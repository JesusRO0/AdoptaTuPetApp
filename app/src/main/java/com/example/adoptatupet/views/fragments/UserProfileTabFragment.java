package com.example.adoptatupet.views.fragments;

import android.content.Context;
import android.content.SharedPreferences;
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
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * UserProfileTabFragment muestra la información del usuario y su historial de posts.
 * Guarda en caché (SharedPreferences) tanto los posts como los datos básicos de usuario
 * (nombre, email, foto) para que al cerrar/reabrir la app se vean de inmediato.
 */
public class UserProfileTabFragment extends Fragment {

    private static final String CACHE_PREFS           = "perfil_cache";
    private static final String KEY_CACHED_POSTS_PRE  = "cachedPerfilPosts_";
    private static final String KEY_CACHED_NAME       = "cachedUserName_";
    private static final String KEY_CACHED_EMAIL      = "cachedUserEmail_";
    private static final String KEY_CACHED_PHOTO_BASE64 = "cachedUserPhoto_";

    private ImageView    ivProfileAvatarTab;
    private TextView     tvProfileNameTab;
    private TextView     tvProfileEmailTab;
    private RecyclerView rvUserPostsTab;
    private ForoAdapter  postsAdapter;
    private int currentUserId = -1;

    private final Gson gson = new Gson();

    public UserProfileTabFragment() {
        // Constructor vacío requerido
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup   container,
                             @Nullable Bundle      savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_user_profile_tab, container, false);

        // 1) Referencias a vistas
        ivProfileAvatarTab = view.findViewById(R.id.ivProfileAvatarTab);
        tvProfileNameTab   = view.findViewById(R.id.tvProfileNameTab);
        tvProfileEmailTab  = view.findViewById(R.id.tvProfileEmailTab);
        rvUserPostsTab     = view.findViewById(R.id.rvUserPostsTab);

        // 2) Configurar RecyclerView
        rvUserPostsTab.setLayoutManager(new LinearLayoutManager(getContext()));

        // 3) Obtener el userId (args o SharedPreferences)
        Bundle args = getArguments();
        if (args != null && args.containsKey("userId")) {
            currentUserId = args.getInt("userId", -1);
        }
        if (currentUserId == -1) {
            SharedPreferences prefs = requireActivity()
                    .getSharedPreferences("user", Context.MODE_PRIVATE);
            currentUserId = prefs.getInt("idUsuario", -1);
        }

        if (currentUserId != -1) {
            // 4) Primero, mostrar datos básicos desde caché (si existen)
            mostrarDatosUsuarioDesdeCache(currentUserId);
            // 5) Después, instanciar adapter y cargar posts cacheados
            postsAdapter = new ForoAdapter(
                    new ArrayList<>(),
                    currentUserId,
                    /* user listener */      null,
                    /* comment listener */   null,
                    /* post listener */      null,
                    /* delete listener */    (mensaje, position) -> {
                new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                        .setTitle("Eliminar publicación")
                        .setMessage("¿Estás seguro de que deseas eliminar esta publicación?")
                        .setPositiveButton("Eliminar", (dialog, which) -> {
                            deletePostFromServer(mensaje.getIdMensaje(), position);
                        })
                        .setNegativeButton("Cancelar", null)
                        .show();
            }
            );
            rvUserPostsTab.setAdapter(postsAdapter);

            cargarPostsDesdeCache(currentUserId);
            // 6) Luego, refrescar datos desde servidor
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
        // Refrescar posts al volver al fragment
        if (currentUserId != -1) {
            cargarPostsUsuario(currentUserId);
        }
    }

    /**
     * Intenta mostrar inmediatamente (sin esperar red) nombre, email y foto
     * leyendo desde SharedPreferences.
     */
    private void mostrarDatosUsuarioDesdeCache(int userId) {
        SharedPreferences cache = requireActivity()
                .getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);

        String cachedName   = cache.getString(KEY_CACHED_NAME + userId, null);
        String cachedEmail  = cache.getString(KEY_CACHED_EMAIL + userId, null);
        String cachedPhoto  = cache.getString(KEY_CACHED_PHOTO_BASE64 + userId, null);

        if (cachedName != null) {
            tvProfileNameTab.setText(cachedName);
        }
        if (cachedEmail != null) {
            tvProfileEmailTab.setText(cachedEmail);
        }
        if (!TextUtils.isEmpty(cachedPhoto)) {
            try {
                byte[] decoded = Base64.decode(cachedPhoto, Base64.NO_WRAP);
                Bitmap bmp = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                ivProfileAvatarTab.setImageBitmap(bmp);
            } catch (IllegalArgumentException e) {
                ivProfileAvatarTab.setImageResource(R.drawable.default_avatar);
            }
        }
    }

    /**
     * Llama a get_usuario_by_id.php para cargar nombre, email, foto en tiempo real.
     * También sobreescribe los valores cacheados en SharedPreferences.
     */
    private void cargarDatosUsuario(int userId) {
        ApiService api = ApiClient.getClient().create(ApiService.class);
        Call<Usuario> callUser = api.getUsuarioById(userId);
        callUser.enqueue(new Callback<Usuario>() {
            @Override
            public void onResponse(Call<Usuario> call, Response<Usuario> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    Usuario user = response.body();

                    // 1) Mostrar en UI
                    tvProfileNameTab.setText(user.getUsuario());
                    tvProfileEmailTab.setText(user.getEmail());

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

                    // 2) Guardar en caché para persistir entre cierres
                    SharedPreferences cache = requireActivity()
                            .getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
                    cache.edit()
                            .putString(KEY_CACHED_NAME + userId, user.getUsuario())
                            .putString(KEY_CACHED_EMAIL + userId, user.getEmail())
                            .putString(KEY_CACHED_PHOTO_BASE64 + userId, foto64 == null ? "" : foto64)
                            .apply();
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
     * Intenta cargar posts del usuario desde SharedPreferences (caché) para mostrar
     * inmediatamente aunque la app se haya cerrado.
     */
    private void cargarPostsDesdeCache(int userId) {
        SharedPreferences cache = requireActivity()
                .getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        String jsonCache = cache.getString(KEY_CACHED_POSTS_PRE + userId, null);
        if (jsonCache != null) {
            Type listType = new TypeToken<List<Mensaje>>() {}.getType();
            List<Mensaje> listaCache = gson.fromJson(jsonCache, listType);
            if (listaCache != null && !listaCache.isEmpty()) {
                postsAdapter.setListaMensajes(listaCache);
            }
        }
    }

    /**
     * Hace GET al servidor para traer los posts actualizados.
     * Actualiza adapter y sobrescribe caché.
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
                    postsAdapter.setListaMensajes(lista);
                    guardarPostsEnCache(userId, lista);
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
     * Guarda la lista de posts del usuario en SharedPreferences como JSON,
     * para que persista aunque la app se cierre.
     */
    private void guardarPostsEnCache(int userId, List<Mensaje> lista) {
        String jsonParaGuardar = gson.toJson(lista);
        SharedPreferences cache = requireActivity()
                .getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        cache.edit()
                .putString(KEY_CACHED_POSTS_PRE + userId, jsonParaGuardar)
                .apply();
    }

    /**
     * Llama a delete_post.php (POST) para eliminar el post y, tras confirmar,
     * lo quita de la lista local y notifica al adapter.
     */
    private void deletePostFromServer(int postId, int position) {
        Map<String, Integer> body = new HashMap<>();
        body.put("usuarioId", currentUserId); // si el endpoint lo requiere
        body.put("idPost", postId);

        ApiService api = ApiClient.getClient().create(ApiService.class);
        Call<Mensaje> callDelete = api.deletePost(body);  // Asegúrate de tener este endpoint en ApiService
        callDelete.enqueue(new Callback<Mensaje>() {
            @Override
            public void onResponse(Call<Mensaje> call, Response<Mensaje> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Borrado exitoso en backend: quitar de la lista local
                    postsAdapter.getListaMensajes().remove(position);
                    postsAdapter.notifyItemRemoved(position);
                    Toast.makeText(getContext(), "Post eliminado", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(), "No se pudo eliminar el post", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Mensaje> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Error de red al eliminar post", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Permite a ForoFragment actualizar el userId y recargar datos+posts.
     */
    public void actualizarUsuario(int userId) {
        currentUserId = userId;
        cargarDatosUsuario(userId);
        cargarPostsDesdeCache(userId);
        cargarPostsUsuario(userId);
    }
}
