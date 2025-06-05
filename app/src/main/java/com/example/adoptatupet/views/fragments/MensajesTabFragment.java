package com.example.adoptatupet.views.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adoptatupet.R;
import com.example.adoptatupet.adapters.ForoAdapter;
import com.example.adoptatupet.models.Mensaje;
import com.example.adoptatupet.network.ApiClient;
import com.example.adoptatupet.network.ApiService;
import com.example.adoptatupet.views.MainActivity;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * MensajesTabFragment: muestra la lista de mensajes del foro
 * y permite postear nuevos.
 * Ahora incluye opción para eliminar cada post (con confirmación)
 * y funcionalidad para adjuntar una imagen al post.
 */
public class MensajesTabFragment extends Fragment {

    private static final String CACHE_PREFS = "foro_cache";
    private static final String KEY_CACHED_MENSAJES = "cachedMensajeList";
    private static final int PICK_IMAGE_REQUEST = 100;

    private ImageView    ivUserProfile;
    private EditText     etPostContent;
    private ImageButton  btnAttachImage;
    private ImageButton  btnPostear;
    private RecyclerView rvMensajesTab;
    private ForoAdapter  foroAdapter;

    // Para almacenar imagen seleccionada en Base64
    private String attachedImageBase64 = null;

    private final Gson gson = new Gson();

    /** Listener para clicks en nombre de usuario (inyectado desde ForoFragment) */
    private ForoAdapter.OnUserNameClickListener listenerUsuario;

    // Lanzador de actividad para seleccionar imagen
    private ActivityResultLauncher<String> pickImageLauncher;

    public MensajesTabFragment() {
        // Constructor vacío
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup   container,
                             @Nullable Bundle      savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mensajes_tab, container, false);

        // 1) Inicializar vistas del composer
        ivUserProfile    = view.findViewById(R.id.ivUserProfile);
        etPostContent    = view.findViewById(R.id.etPostContent);
        btnAttachImage   = view.findViewById(R.id.btnAttachImage);
        btnPostear       = view.findViewById(R.id.btnPostear);

        // 1a) Cargar la imagen de perfil actual desde SharedPreferences
        cargarImagenPerfil();

        // 1b) Al hacer clic sobre la imagen de perfil propia, mostrar perfil del usuario actual
        ivUserProfile.setOnClickListener(v -> {
            SharedPreferences prefs = requireActivity()
                    .getSharedPreferences("user", Context.MODE_PRIVATE);
            int myId = prefs.getInt("idUsuario", -1);
            if (myId != -1 && listenerUsuario != null) {
                listenerUsuario.onUserNameClicked(myId);
            }
        });

        // 1c) Configurar “Adjuntar imagen”
        registerImagePicker();
        btnAttachImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        // 1d) Configurar botón “Postear”
        btnPostear.setOnClickListener(v -> {
            String texto = etPostContent.getText().toString().trim();
            if (TextUtils.isEmpty(texto)) {
                Toast.makeText(getContext(), "Escribe algo antes de postear", Toast.LENGTH_SHORT).show();
                return;
            }
            enviarMensaje(texto, attachedImageBase64);
        });

        // 2) Inicializar RecyclerView para mostrar mensajes
        rvMensajesTab = view.findViewById(R.id.rvForo);
        rvMensajesTab.setLayoutManager(new LinearLayoutManager(getContext()));

        // ====== CAMBIO PRINCIPAL AQUI: Leemos el ID del usuario logueado ======
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user", Context.MODE_PRIVATE);
        int userIdActual = prefs.getInt("idUsuario", -1);

        // 2a) Instanciar ForoAdapter con SEIS parámetros:
        //     -> lista vacía, userIdActual, OnUserNameClickListener, OnCommentClickListener, OnPostClickListener, OnDeleteClickListener
        foroAdapter = new ForoAdapter(
                new ArrayList<>(),
                userIdActual,   // <-- Pasamos aquí el ID real del usuario que da “like”
                // OnUserNameClickListener
                usuarioId -> {
                    if (listenerUsuario != null) {
                        listenerUsuario.onUserNameClicked(usuarioId);
                    }
                },
                // OnCommentClickListener: abre pop-up
                mensaje -> mostrarDialogoComentario(
                        mensaje.getIdMensaje(),
                        mensaje.getUsuarioNombre(),
                        mensaje.getFotoPerfil()
                ),
                // OnPostClickListener: abre ComentariosFragment
                mensaje -> {
                    ComentariosFragment comentariosFragment = ComentariosFragment.newInstance(mensaje);
                    FragmentTransaction ft = requireActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.nav_host_fragment, comentariosFragment)
                            .addToBackStack(null);
                    ft.commit();
                },
                // OnDeleteClickListener: confirma y elimina post
                (mensaje, position) -> {
                    new AlertDialog.Builder(getContext())
                            .setTitle("Eliminar post")
                            .setMessage("¿Estás seguro de que quieres eliminar este post?")
                            .setPositiveButton("Sí", (dialog, which) -> {
                                deletePostFromServer(mensaje.getIdMensaje(), position);
                            })
                            .setNegativeButton("No", null)
                            .show();
                }
        );
        rvMensajesTab.setAdapter(foroAdapter);
        // ====== FIN CAMBIO ======

        // 3) Cargar mensajes desde caché y luego refrescar desde servidor
        cargarMensajesDesdeCache();
        refrescarMensajesDesdeServidor();

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Recargar foto de perfil si cambió en PerfilUsuario
        cargarImagenPerfil();
    }

    /** Inyecta listener desde ForoFragment para clicks en nombre de usuario */
    public void setOnUserNameClickListener(ForoAdapter.OnUserNameClickListener l) {
        this.listenerUsuario = l;
        if (foroAdapter != null) {
            foroAdapter.setOnUserNameClickListener(l);
        }
    }

    /**
     * Carga la imagen de perfil del usuario desde SharedPreferences en ivUserProfile.
     */
    private void cargarImagenPerfil() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user", Context.MODE_PRIVATE);
        String fotoPerfilBase64 = prefs.getString("fotoPerfil", null);
        if (!TextUtils.isEmpty(fotoPerfilBase64)) {
            try {
                byte[] decodedBytes = Base64.decode(fotoPerfilBase64, Base64.NO_WRAP);
                Bitmap bmp = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                ivUserProfile.setImageBitmap(bmp);
            } catch (IllegalArgumentException e) {
                ivUserProfile.setImageResource(R.drawable.default_avatar);
            }
        } else {
            ivUserProfile.setImageResource(R.drawable.default_avatar);
        }
    }

    /**
     * Carga lista de Mensaje desde SharedPreferences (caché) y la muestra en el adapter.
     */
    private void cargarMensajesDesdeCache() {
        SharedPreferences cachePrefs = requireActivity()
                .getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        String jsonCache = cachePrefs.getString(KEY_CACHED_MENSAJES, null);
        if (jsonCache != null) {
            Type listType = new TypeToken<List<Mensaje>>() {}.getType();
            List<Mensaje> listaCache = gson.fromJson(jsonCache, listType);
            if (listaCache != null && !listaCache.isEmpty()) {
                foroAdapter.setListaMensajes(listaCache);
                rvMensajesTab.scrollToPosition(listaCache.size() - 1);
            }
        }
    }

    /**
     * Hace GET al servidor para traer los mensajes actualizados.
     * Actualiza adapter y sobrescribe caché.
     */
    private void refrescarMensajesDesdeServidor() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user", Context.MODE_PRIVATE);
        int userId = prefs.getInt("idUsuario", -1);

        ApiService api = ApiClient.getClient().create(ApiService.class);
        Call<List<Mensaje>> call = api.getMensajesForo(userId);
        call.enqueue(new Callback<List<Mensaje>>() {
            @Override
            public void onResponse(Call<List<Mensaje>> call, Response<List<Mensaje>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    List<Mensaje> lista = response.body();
                    // 1) Actualizar adapter
                    foroAdapter.setListaMensajes(lista);
                    rvMensajesTab.scrollToPosition(lista.size() - 1);
                    // 2) Guardar en caché
                    guardarMensajesEnCache(lista);
                } else {
                    Toast.makeText(getContext(), "Error al cargar mensajes", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<Mensaje>> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Fallo de red al cargar mensajes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Guarda lista de Mensaje en SharedPreferences como JSON.
     */
    private void guardarMensajesEnCache(List<Mensaje> lista) {
        String jsonParaGuardar = gson.toJson(lista);
        SharedPreferences cachePrefs = requireActivity()
                .getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        cachePrefs.edit()
                .putString(KEY_CACHED_MENSAJES, jsonParaGuardar)
                .apply();
    }

    /**
     * Registra el lanzador de ActivityResult para seleccionar una imagen del dispositivo.
     */
    private void registerImagePicker() {
        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                new ActivityResultCallback<Uri>() {
                    @Override
                    public void onActivityResult(Uri uri) {
                        if (uri != null) {
                            try {
                                InputStream inputStream = requireContext()
                                        .getContentResolver()
                                        .openInputStream(uri);
                                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                // Convertir Bitmap a Base64
                                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
                                byte[] imageBytes = baos.toByteArray();
                                attachedImageBase64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP);
                                Toast.makeText(getContext(), "Imagen adjuntada", Toast.LENGTH_SHORT).show();
                            } catch (Exception e) {
                                Toast.makeText(getContext(), "Error al procesar imagen", Toast.LENGTH_SHORT).show();
                                attachedImageBase64 = null;
                            }
                        }
                    }
                }
        );
    }

    /**
     * Envía un nuevo mensaje al servidor a través del endpoint POST.
     * Incluye campo "imagenMensaje" con Base64 si se adjuntó imagen.
     * Muestra un indicador de "Cargando..." durante la operación.
     * Al completarse, refresca la lista de mensajes.
     *
     * @param texto  texto del mensaje
     * @param imagenBase64 Base64 de la imagen adjunta (puede ser null)
     */
    private void enviarMensaje(String texto, @Nullable String imagenBase64) {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user", Context.MODE_PRIVATE);
        int userId = prefs.getInt("idUsuario", -1);

        Mensaje nuevo = new Mensaje();
        nuevo.setUsuarioId(userId);
        nuevo.setTexto(texto);
        // Asignar imagen adjunta si existe
        if (imagenBase64 != null) {
            nuevo.setImagenMensaje(imagenBase64);
        }

        // Mostrar indicador de loading en MainActivity
        if (getActivity() != null) {
            ((MainActivity) getActivity()).showLoading();
        }

        ApiService api = ApiClient.getClient().create(ApiService.class);
        Call<Mensaje> call = api.postMensaje(nuevo);
        call.enqueue(new Callback<Mensaje>() {
            @Override
            public void onResponse(Call<Mensaje> call, Response<Mensaje> response) {
                // Ocultar loading
                if (getActivity() != null) {
                    ((MainActivity) getActivity()).hideLoading();
                }
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    etPostContent.setText("");
                    attachedImageBase64 = null;
                    refrescarMensajesDesdeServidor();
                } else {
                    Toast.makeText(getContext(), "No se pudo enviar el mensaje", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Mensaje> call, Throwable t) {
                // Ocultar loading
                if (getActivity() != null) {
                    ((MainActivity) getActivity()).hideLoading();
                }
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Error de red al enviar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Elimina un post en el servidor (requiere usuarioId + idPost).
     * Si la operación es exitosa, busca el índice real del post en la lista
     * y lo quita. De esta forma evitamos el IndexOutOfBoundsException.
     *
     * @param postId   ID del post a eliminar
     * @param position Posición propuesta en el RecyclerView (puede estar desfasada)
     */
    private void deletePostFromServer(int postId, int position) {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user", Context.MODE_PRIVATE);
        int currentUserId = prefs.getInt("idUsuario", -1);

        Map<String, Integer> body = new HashMap<>();
        body.put("usuarioId", currentUserId);
        body.put("idPost", postId);

        ApiService api = ApiClient.getClient().create(ApiService.class);
        Call<Mensaje> callDelete = api.deletePost(body);
        callDelete.enqueue(new Callback<Mensaje>() {
            @Override
            public void onResponse(Call<Mensaje> call, Response<Mensaje> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Buscamos el índice real del post en 'listaMensajes', comparando idMensaje
                    List<Mensaje> lista = foroAdapter.getListaMensajes();
                    int indexToRemove = -1;
                    for (int i = 0; i < lista.size(); i++) {
                        if (lista.get(i).getIdMensaje() == postId) {
                            indexToRemove = i;
                            break;
                        }
                    }
                    if (indexToRemove != -1) {
                        lista.remove(indexToRemove);
                        foroAdapter.notifyItemRemoved(indexToRemove);
                        Toast.makeText(getContext(), "Post eliminado", Toast.LENGTH_SHORT).show();
                    } else {
                        // Si no se encontró (posiblemente ya se refrescó la lista), simplemente refrescamos
                        Toast.makeText(getContext(), "Post eliminado (refrescando lista)", Toast.LENGTH_SHORT).show();
                        refrescarMensajesDesdeServidor();
                    }
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
     * Muestra un AlertDialog con un EditText para que el usuario escriba su comentario.
     *
     * @param postId             El ID del post al que se responde.
     * @param usuarioNombre      El nombre del autor original.
     * @param fotoPerfilBase64   El Base64 del avatar del autor original.
     */
    private void mostrarDialogoComentario(int postId, String usuarioNombre, String fotoPerfilBase64) {
        // Inflamos el layout dialog_comentar.xml
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_comentar, null);

        ImageView ivAvatarDestino  = dialogView.findViewById(R.id.ivAvatarDestino);
        TextView tvDestinoNombre  = dialogView.findViewById(R.id.tvDestinoNombre);
        TextView tvDestinoEmail   = dialogView.findViewById(R.id.tvDestinoEmail);
        EditText  etComentario     = dialogView.findViewById(R.id.etComentario);
        Button    btnResponder     = dialogView.findViewById(R.id.btnResponderDialog);

        // 1) Poner nombre del usuario al que se responde
        tvDestinoNombre.setText(usuarioNombre);
        // 2) No mostramos email en esta versión
        tvDestinoEmail.setText("");

        // 3) Cargar avatar del destinatario
        if (!TextUtils.isEmpty(fotoPerfilBase64)) {
            try {
                byte[] decoded = Base64.decode(fotoPerfilBase64, Base64.NO_WRAP);
                Bitmap bmpAvatar = BitmapFactory.decodeByteArray(decoded, 0, decoded.length);
                ivAvatarDestino.setImageBitmap(bmpAvatar);
            } catch (IllegalArgumentException e) {
                ivAvatarDestino.setImageResource(R.drawable.default_avatar);
            }
        } else {
            ivAvatarDestino.setImageResource(R.drawable.default_avatar);
        }

        // 4) Construir y mostrar el AlertDialog con dicho layout
        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // 5) Al pulsar “Responder” dentro del diálogo:
        btnResponder.setOnClickListener(v -> {
            String textoComent = etComentario.getText().toString().trim();
            if (TextUtils.isEmpty(textoComent)) {
                Toast.makeText(getContext(), "Escribe algo antes de enviar", Toast.LENGTH_SHORT).show();
                return;
            }
            // Construir body para enviar a la API
            SharedPreferences prefs = requireActivity()
                    .getSharedPreferences("user", Context.MODE_PRIVATE);
            int myUserId = prefs.getInt("idUsuario", -1);

            Map<String, Object> body = new HashMap<>();
            body.put("idUsuario", myUserId);
            body.put("idPost", postId);
            body.put("texto", textoComent);

            // Mostrar loading en MainActivity mientras se envía
            if (getActivity() != null) {
                ((MainActivity) getActivity()).showLoading();
            }

            ApiService api = ApiClient.getClient().create(ApiService.class);
            Call<Mensaje> callComentario = api.postComentario(body);
            callComentario.enqueue(new Callback<Mensaje>() {
                @Override
                public void onResponse(Call<Mensaje> call, Response<Mensaje> response) {
                    // Ocultar loading
                    if (getActivity() != null) {
                        ((MainActivity) getActivity()).hideLoading();
                    }
                    if (!isAdded()) return;
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Toast.makeText(getContext(), "Comentario enviado", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    } else {
                        Toast.makeText(getContext(), "Error al enviar comentario", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<Mensaje> call, Throwable t) {
                    // Ocultar loading
                    if (getActivity() != null) {
                        ((MainActivity) getActivity()).hideLoading();
                    }
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Fallo de red al enviar comentario", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }
}
