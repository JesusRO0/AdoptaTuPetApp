package com.example.adoptatupet.views.fragments;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import android.widget.TextView;

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
 * Fragment para la pestaña “Perfil de usuario”:
 * - Muestra foto circular, nombre y email
 * - Debajo, RecyclerView con los posts de ese usuario
 */
public class UserProfileTabFragment extends Fragment {

    private ImageView ivProfileAvatarTab;
    private TextView tvProfileNameTab, tvProfileEmailTab;
    private RecyclerView rvUserPostsTab;
    private ForoAdapter postsAdapter;
    private int currentUserId = -1;

    public UserProfileTabFragment() { /* constructor vacío */ }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup   container,
                             @Nullable Bundle      savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_profile_tab, container, false);

        ivProfileAvatarTab = view.findViewById(R.id.ivProfileAvatarTab);
        tvProfileNameTab   = view.findViewById(R.id.tvProfileNameTab);
        tvProfileEmailTab  = view.findViewById(R.id.tvProfileEmailTab);
        rvUserPostsTab     = view.findViewById(R.id.rvUserPostsTab);

        rvUserPostsTab.setLayoutManager(new LinearLayoutManager(getContext()));
        postsAdapter = new ForoAdapter(new ArrayList<>(), null);
        rvUserPostsTab.setAdapter(postsAdapter);

        // Si el contenedor le pasa userId vía argumento, úsalo:
        Bundle args = getArguments();
        if (args != null && args.containsKey("userId")) {
            currentUserId = args.getInt("userId", -1);
        }
        if (currentUserId != -1) {
            cargarDatosUsuario(currentUserId);
            cargarPostsUsuario(currentUserId);
        }
        return view;
    }

    private void cargarDatosUsuario(int userId) {
        ApiService api = ApiClient.getClient().create(ApiService.class);
        Call<Usuario> callUser = api.getUsuarioById(userId);
        callUser.enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    Usuario user = response.body();
                    tvProfileNameTab.setText(user.getUsuario());
                    tvProfileEmailTab.setText(user.getEmail());

                    // Cargar fotoBase64
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

    private void cargarPostsUsuario(int userId) {
        ApiService api = ApiClient.getClient().create(ApiService.class);
        Call<List<Mensaje>> callPosts = api.getMensajesUsuario(userId);
        callPosts.enqueue(new Callback<List<Mensaje>>() {
            @Override
            public void onResponse(Call<List<Mensaje>> call, Response<List<Mensaje>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<Mensaje> lista = response.body();
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
     * Para que ForoFragment invoque este método cuando el usuario pulsa un nombre:
     */
    public void actualizarUsuario(int userId) {
        currentUserId = userId;
        cargarDatosUsuario(userId);
        cargarPostsUsuario(userId);
    }
}
