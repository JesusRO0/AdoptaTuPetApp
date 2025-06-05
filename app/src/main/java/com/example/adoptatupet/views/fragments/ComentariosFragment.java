package com.example.adoptatupet.views.fragments;

import android.app.AlertDialog;
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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adoptatupet.R;
import com.example.adoptatupet.adapters.CommentsAdapter;
import com.example.adoptatupet.models.Comment;
import com.example.adoptatupet.models.Mensaje;
import com.example.adoptatupet.network.ApiClient;
import com.example.adoptatupet.network.ApiService;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * ComentariosFragment muestra:
 *  1) El post original en la parte superior (usando layout item_mensaje).
 *     Ahora con funcionalidad de “like/unlike”, “commentCount”, “Eliminar post” y “pop-up de comentario”.
 *  2) Un RecyclerView con todos los comentarios asociados a ese post.
 *  3) Flecha de retroceso para volver al foro.
 */
public class ComentariosFragment extends Fragment {

    private static final String ARG_POST = "arg_post";

    private Mensaje postOriginal;

    // ===== Vistas del post original (include @layout/item_mensaje) =====
    private ImageView   ivPostUserProfile;
    private TextView    tvUserName,
            tvFechaMensaje,
            tvTextoMensaje,
            tvLikeCount,
            tvCommentCount;
    private ImageView   ivPostImage;
    private ImageButton btnLike;          // Corazón
    private ImageButton btnComment;       // Bocadillo
    private ImageButton btnDeleteMessage; // Papelera para eliminar el post

    // ===== Vistas del listado de comentarios =====
    private RecyclerView     rvComentarios;
    private CommentsAdapter  commentsAdapter;
    private List<Comment>    commentList = new ArrayList<>();

    // ===== Flecha de retroceso =====
    private ImageView btnAtras;

    private int currentUserId = -1;
    private static final Gson gson = new Gson();

    public ComentariosFragment() {
        // Constructor vacío
    }

    /**
     * Crea una nueva instancia del fragment recibiendo el objeto Mensaje (post original).
     */
    public static ComentariosFragment newInstance(Mensaje post) {
        ComentariosFragment fragment = new ComentariosFragment();
        Bundle args = new Bundle();
        args.putString(ARG_POST, gson.toJson(post));
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Leer el postOriginal de los argumentos
        if (getArguments() != null && getArguments().containsKey(ARG_POST)) {
            String postJson = getArguments().getString(ARG_POST);
            postOriginal = gson.fromJson(postJson, Mensaje.class);
        }
        // Obtener currentUserId de SharedPreferences
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user", Context.MODE_PRIVATE);
        currentUserId = prefs.getInt("idUsuario", -1);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup   container,
                             @Nullable Bundle      savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_comentarios, container, false);

        // ======================
        // 0) Flecha de retroceso
        // ======================
        btnAtras = view.findViewById(R.id.atras);
        btnAtras.setOnClickListener(v -> {
            // Volver al foro
            getActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment,
                            new com.example.adoptatupet.views.fragments.ForoFragment())
                    .commit();
        });

        // ======================
        // 1) Setear datos del post original en include (item_mensaje)
        // ======================
        ivPostUserProfile = view.findViewById(R.id.ivPostUserProfile);
        tvUserName        = view.findViewById(R.id.tvUserName);
        tvFechaMensaje    = view.findViewById(R.id.tvFechaMensaje);
        tvTextoMensaje    = view.findViewById(R.id.tvTextoMensaje);
        ivPostImage       = view.findViewById(R.id.ivMensajeImagen);
        tvLikeCount       = view.findViewById(R.id.tvLikeCount);
        tvCommentCount    = view.findViewById(R.id.tvCommentCount);

        btnLike           = view.findViewById(R.id.btnLike);
        btnComment        = view.findViewById(R.id.btnComment);
        btnDeleteMessage  = view.findViewById(R.id.btnDeleteMessage);

        if (postOriginal != null) {
            // –– Nombre y fecha
            tvUserName.setText(postOriginal.getUsuarioNombre());
            tvFechaMensaje.setText(postOriginal.getFechaPublicacion());

            // –– Texto del post
            tvTextoMensaje.setText(postOriginal.getTexto());

            // –– Foto de perfil (Base64 → Bitmap)
            String fotoPerfil64 = postOriginal.getFotoPerfil();
            if (!TextUtils.isEmpty(fotoPerfil64)) {
                try {
                    byte[] decodedBytes = Base64.decode(fotoPerfil64, Base64.NO_WRAP);
                    Bitmap bmp = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    ivPostUserProfile.setImageBitmap(bmp);
                } catch (IllegalArgumentException e) {
                    ivPostUserProfile.setImageResource(R.drawable.default_avatar);
                }
            } else {
                ivPostUserProfile.setImageResource(R.drawable.default_avatar);
            }

            // –– Imagen del post (si existe)
            String imgPost64 = postOriginal.getImagenMensaje();
            if (!TextUtils.isEmpty(imgPost64)) {
                ivPostImage.setVisibility(View.VISIBLE);
                try {
                    byte[] imgBytes = Base64.decode(imgPost64, Base64.NO_WRAP);
                    Bitmap bmp2 = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
                    ivPostImage.setImageBitmap(bmp2);
                } catch (IllegalArgumentException e) {
                    ivPostImage.setVisibility(View.GONE);
                }
            } else {
                ivPostImage.setVisibility(View.GONE);
            }

            // –– Contador de likes
            tvLikeCount.setText(String.valueOf(postOriginal.getLikeCount()));

            // –– Contador de comentarios
            tvCommentCount.setText(String.valueOf(postOriginal.getCommentCount()));

            // –– Estado inicial del icono “like”
            Context ctx = getContext();
            if (postOriginal.isLikedByUser()) {
                btnLike.setImageResource(R.drawable.ic_heart_filled);
                btnLike.setColorFilter(ContextCompat.getColor(ctx, R.color.red_500));
            } else {
                btnLike.setImageResource(R.drawable.ic_heart_outline);
                btnLike.setColorFilter(ContextCompat.getColor(ctx, R.color.gray_500));
            }

            // --------- LISTENER “LIKE / UNLIKE” EN EL POST ORIGINAL -----------
            btnLike.setOnClickListener(v -> {
                boolean currentlyLiked = postOriginal.isLikedByUser();
                int currentCount = postOriginal.getLikeCount();

                // a) Actualizar UI local (optimistic)
                if (currentlyLiked) {
                    postOriginal.setLikedByUser(false);
                    postOriginal.setLikeCount(Math.max(0, currentCount - 1));
                    btnLike.setImageResource(R.drawable.ic_heart_outline);
                    btnLike.setColorFilter(ContextCompat.getColor(ctx, R.color.gray_500));
                    tvLikeCount.setText(String.valueOf(postOriginal.getLikeCount()));
                } else {
                    postOriginal.setLikedByUser(true);
                    postOriginal.setLikeCount(currentCount + 1);
                    btnLike.setImageResource(R.drawable.ic_heart_filled);
                    btnLike.setColorFilter(ContextCompat.getColor(ctx, R.color.red_500));
                    tvLikeCount.setText(String.valueOf(postOriginal.getLikeCount()));
                }

                // b) Llamar a backend para persistir
                Map<String, Integer> body = new HashMap<>();
                body.put("usuarioId", currentUserId);
                body.put("idPost", postOriginal.getIdMensaje());

                ApiService api = ApiClient.getClient().create(ApiService.class);
                if (currentlyLiked) {
                    Call<Mensaje> callUn = api.unlikePost(body);
                    callUn.enqueue(new Callback<Mensaje>() {
                        @Override
                        public void onResponse(Call<Mensaje> call, Response<Mensaje> response) {
                            if (!(response.isSuccessful() &&
                                    response.body() != null &&
                                    response.body().isSuccess())) {
                                // Revertir si falla
                                postOriginal.setLikedByUser(true);
                                postOriginal.setLikeCount(postOriginal.getLikeCount() + 1);
                                btnLike.setImageResource(R.drawable.ic_heart_filled);
                                btnLike.setColorFilter(ContextCompat.getColor(ctx, R.color.red_500));
                                tvLikeCount.setText(String.valueOf(postOriginal.getLikeCount()));
                                Toast.makeText(ctx, "No se pudo quitar el Like", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<Mensaje> call, Throwable t) {
                            // Revertir si falla
                            postOriginal.setLikedByUser(true);
                            postOriginal.setLikeCount(postOriginal.getLikeCount() + 1);
                            btnLike.setImageResource(R.drawable.ic_heart_filled);
                            btnLike.setColorFilter(ContextCompat.getColor(ctx, R.color.red_500));
                            tvLikeCount.setText(String.valueOf(postOriginal.getLikeCount()));
                            Toast.makeText(ctx, "Error de red al quitar Like", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    Call<Mensaje> callLike = api.likePost(body);
                    callLike.enqueue(new Callback<Mensaje>() {
                        @Override
                        public void onResponse(Call<Mensaje> call, Response<Mensaje> response) {
                            if (!(response.isSuccessful() &&
                                    response.body() != null &&
                                    response.body().isSuccess())) {
                                // Revertir si falla
                                postOriginal.setLikedByUser(false);
                                postOriginal.setLikeCount(Math.max(0, postOriginal.getLikeCount() - 1));
                                btnLike.setImageResource(R.drawable.ic_heart_outline);
                                btnLike.setColorFilter(ContextCompat.getColor(ctx, R.color.gray_500));
                                tvLikeCount.setText(String.valueOf(postOriginal.getLikeCount()));
                                Toast.makeText(ctx, "No se pudo dar Like", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<Mensaje> call, Throwable t) {
                            // Revertir si falla
                            postOriginal.setLikedByUser(false);
                            postOriginal.setLikeCount(Math.max(0, postOriginal.getLikeCount() - 1));
                            btnLike.setImageResource(R.drawable.ic_heart_outline);
                            btnLike.setColorFilter(ContextCompat.getColor(ctx, R.color.gray_500));
                            tvLikeCount.setText(String.valueOf(postOriginal.getLikeCount()));
                            Toast.makeText(ctx, "Error de red al dar Like", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

            // --------- LISTENER “POP-UP DE COMENTARIO” EN EL POST ORIGINAL -----------
            btnComment.setOnClickListener(v -> {
                mostrarDialogoComentario(
                        postOriginal.getIdMensaje(),
                        postOriginal.getUsuarioNombre(),
                        postOriginal.getFotoPerfil()
                );
            });

            // --------- Mostrar el botón de Eliminar POST si el usuario es el autor -----------
            if (postOriginal.getUsuarioId() == currentUserId) {
                btnDeleteMessage.setVisibility(View.VISIBLE);
                btnDeleteMessage.setOnClickListener(v -> {
                    new AlertDialog.Builder(requireContext())
                            .setTitle("Eliminar post")
                            .setMessage("¿Estás seguro de que deseas eliminar este post?")
                            .setPositiveButton("Eliminar", (dialogInt, which) -> {
                                deletePostFromServer(postOriginal.getIdMensaje());
                            })
                            .setNegativeButton("Cancelar", null)
                            .show();
                });
            } else {
                btnDeleteMessage.setVisibility(View.GONE);
            }
        }

        // ======================
        // 2) RecyclerView para comentarios
        // ======================
        rvComentarios = view.findViewById(R.id.rvComentarios);
        rvComentarios.setLayoutManager(new LinearLayoutManager(getContext()));
        commentsAdapter = new CommentsAdapter(commentList);
        rvComentarios.setAdapter(commentsAdapter);

        // ======================
        // 3) Cargar comentarios del servidor
        // ======================
        if (postOriginal != null) {
            cargarComentariosDesdeServidor(postOriginal.getIdMensaje());
        }

        return view;
    }

    /**
     * Carga los comentarios desde el servidor vía API:
     * GET …/get_comentarios.php?idPost=…
     */
    private void cargarComentariosDesdeServidor(int postId) {
        ApiService api = ApiClient.getClient().create(ApiService.class);
        Call<List<Comment>> call = api.getComentarios(postId);
        call.enqueue(new Callback<List<Comment>>() {
            @Override
            public void onResponse(Call<List<Comment>> call, Response<List<Comment>> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null) {
                    commentList.clear();
                    commentList.addAll(response.body());
                    commentsAdapter.notifyDataSetChanged();
                    rvComentarios.scrollToPosition(commentList.size() - 1);
                } else {
                    Toast.makeText(getContext(), "Error al cargar comentarios", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<List<Comment>> call, Throwable t) {
                if (!isAdded()) return;
                Toast.makeText(getContext(), "Fallo de red al cargar comentarios", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * Muestra un AlertDialog con un EditText para que el usuario escriba su comentario
     * al post original.
     */
    private void mostrarDialogoComentario(int postId, String usuarioNombre, String fotoPerfilBase64) {
        View dialogView = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_comentar, null);

        ImageView ivAvatarDestino  = dialogView.findViewById(R.id.ivAvatarDestino);
        TextView  tvDestinoNombre  = dialogView.findViewById(R.id.tvDestinoNombre);
        TextView  tvDestinoEmail   = dialogView.findViewById(R.id.tvDestinoEmail);
        EditText  etComentario     = dialogView.findViewById(R.id.etComentario);
        Button    btnResponder     = dialogView.findViewById(R.id.btnResponderDialog);

        tvDestinoNombre.setText(usuarioNombre);
        tvDestinoEmail.setText("");

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

        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(dialogView)
                .setCancelable(true)
                .create();

        btnResponder.setOnClickListener(v -> {
            String textoComent = etComentario.getText().toString().trim();
            if (TextUtils.isEmpty(textoComent)) {
                Toast.makeText(getContext(), "Escribe algo antes de enviar", Toast.LENGTH_SHORT).show();
                return;
            }
            SharedPreferences prefs = requireActivity()
                    .getSharedPreferences("user", Context.MODE_PRIVATE);
            int myUserId = prefs.getInt("idUsuario", -1);

            Map<String, Object> body = new HashMap<>();
            body.put("idUsuario", myUserId);
            body.put("idPost", postId);
            body.put("texto", textoComent);

            ApiService api = ApiClient.getClient().create(ApiService.class);
            Call<Mensaje> callComentario = api.postComentario(body);
            callComentario.enqueue(new Callback<Mensaje>() {
                @Override
                public void onResponse(Call<Mensaje> call, Response<Mensaje> response) {
                    if (!isAdded()) return;
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        Toast.makeText(getContext(), "Comentario enviado", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();

                        postOriginal.setCommentCount(postOriginal.getCommentCount() + 1);
                        tvCommentCount.setText(String.valueOf(postOriginal.getCommentCount()));
                        cargarComentariosDesdeServidor(postId);
                    } else {
                        Toast.makeText(getContext(), "Error al enviar comentario", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onFailure(Call<Mensaje> call, Throwable t) {
                    if (!isAdded()) return;
                    Toast.makeText(getContext(), "Fallo de red al enviar comentario", Toast.LENGTH_SHORT).show();
                }
            });
        });

        dialog.show();
    }

    /**
     * Llama a delete_post.php para eliminar el post actual. Al confirmarlo, vuelve al foro.
     */
    private void deletePostFromServer(int postId) {
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
                    Toast.makeText(getContext(), "Post eliminado", Toast.LENGTH_SHORT).show();
                    // Volver automáticamente al foro
                    getActivity()
                            .getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.nav_host_fragment,
                                    new com.example.adoptatupet.views.fragments.ForoFragment())
                            .commit();
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
}
