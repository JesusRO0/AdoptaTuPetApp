package com.example.adoptatupet.views.fragments;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adoptatupet.R;
import com.example.adoptatupet.adapters.ForoAdapter;
import com.example.adoptatupet.models.Mensaje;
import com.example.adoptatupet.models.Usuario;
import com.example.adoptatupet.network.ApiClient;
import com.example.adoptatupet.network.ApiService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * UserProfileTabFragment muestra la información del usuario y su historial de posts.
 * Ahora, al cargar los posts, respetamos el campo likeCount y el flag likedByUser
 * para que el corazón aparezca relleno si corresponde.
 */
public class UserProfileTabFragment extends Fragment {

    private ImageView    ivProfileAvatarTab;
    private TextView     tvProfileNameTab;
    private TextView     tvProfileEmailTab;
    private RecyclerView rvUserPostsTab;
    private ForoAdapter  postsAdapter;
    private int currentUserId = -1;

    public UserProfileTabFragment() {
        // Constructor vacío requerido
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup   container,
                             @Nullable Bundle      savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_profile_tab, container, false);

        // 1) Referencias a vistas (mismos IDs que en tu XML)
        ivProfileAvatarTab = view.findViewById(R.id.ivProfileAvatarTab);
        tvProfileNameTab   = view.findViewById(R.id.tvProfileNameTab);
        tvProfileEmailTab  = view.findViewById(R.id.tvProfileEmailTab);
        rvUserPostsTab     = view.findViewById(R.id.rvUserPostsTab);

        // 2) Configuramos el RecyclerView para mostrar historial de posts:
        rvUserPostsTab.setLayoutManager(new LinearLayoutManager(getContext()));
        // Pasamos listener=null porque no necesitamos click en nombre aquí
        postsAdapter = new ForoAdapter(new ArrayList<>(), /* listenerUsuario= */ null);
        rvUserPostsTab.setAdapter(postsAdapter);

        // 3) Obtenemos el userId de los argumentos o, si no viene, de SharedPreferences
        Bundle args = getArguments();
        if (args != null && args.containsKey("userId")) {
            currentUserId = args.getInt("userId", -1);
        }
        if (currentUserId == -1) {
            currentUserId = requireActivity()
                    .getSharedPreferences("user", Context.MODE_PRIVATE)
                    .getInt("idUsuario", -1);
        }

        // 4) Si tenemos un userId válido, cargamos datos y posts
        if (currentUserId != -1) {
            cargarDatosUsuario(currentUserId);
            cargarPostsUsuario(currentUserId);
        } else {
            Toast.makeText(getContext(), "Usuario inválido para mostrar perfil", Toast.LENGTH_SHORT).show();
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Cada vez que el fragment se reanude, recargamos los posts del usuario
        // para que se actualicen automáticamente los “likes” si han cambiado.
        if (currentUserId != -1) {
            cargarPostsUsuario(currentUserId);
        }
    }

    /**
     * Llama a get_usuario_by_id.php para obtener nombre, email y foto de perfil.
     */
    private void cargarDatosUsuario(int userId) {
        ApiService api = ApiClient.getClient().create(ApiService.class);
        Call<Usuario> callUser = api.getUsuarioById(userId);
        callUser.enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                // Salvaguarda: si el fragment ya no está adjunto, salimos
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    Usuario user = response.body();
                    // 1) Nombre y correo
                    tvProfileNameTab.setText(user.getUsuario());
                    tvProfileEmailTab.setText(user.getEmail());

                    // 2) Foto de perfil (Base64 -> Bitmap)
                    String foto64 = user.getFotoPerfil();
                    if (!TextUtils.isEmpty(foto64)) {
                        try {
                            byte[] decoded = Base64.decode(foto64, Base64.NO_WRAP);
                            Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                            ivProfileAvatarTab.setImageBitmap(bmp);
                        } catch (IllegalArgumentException e) {
                            ivProfileAvatarTab.setImageResource(R.drawable.default_avatar);
                        }
                    } else {
                        ivProfileAvatarTab.setImageResource(R.drawable.default_avatar);
                    }
                } else {
                    Toast.makeText(getContext(), "No se pudo cargar info de usuario", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Usuario> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Error de red al cargar usuario", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Llama a get_mensajes_usuario.php para obtener sólo los posts de este usuario.
     * Ahora, el objeto Mensaje debe traer en cada elemento:
     *   • likeCount        (cantidad de “me gusta”)
     *   • likedByUser      (si este usuario ya lo marcó como “me gusta”)
     *
     * Una vez obtenido, se asigna directamente al adaptador:
     * postsAdapter.setListaMensajes(lista);
     * y el propio ForoAdapter se encarga de mostrar el corazón relleno/contorno.
     */
    private void cargarPostsUsuario(int userId) {
        ApiService api = ApiClient.getClient().create(ApiService.class);
        Call<List<Mensaje>> callPosts = api.getMensajesUsuario(userId);
        callPosts.enqueue(new Callback<List<Mensaje>>() {
            @Override
            public void onResponse(Call<List<Mensaje>> call, Response<List<Mensaje>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<Mensaje> lista = response.body();

                    // IMPORTANTE: aquí cada Mensaje debe traer:
                    //   • m.getLikeCount()    → número de likes
                    //   • m.isLikedByUser()   → true/false
                    // Si la API no te está devolviendo estos campos, tendrás que
                    // ajustarla para que incluy a “likeCount” y “likedByUser” en el JSON.

                    postsAdapter.setListaMensajes(lista);
                } else {
                    Toast.makeText(getContext(), "No se pudo cargar historial", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Mensaje>> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Error de red al cargar historial", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Permite a ForoFragment invocar manualmente la recarga si se necesita.
     */
    public void actualizarUsuario(int userId) {
        currentUserId = userId;
        cargarDatosUsuario(userId);
        cargarPostsUsuario(userId);
    }
}
