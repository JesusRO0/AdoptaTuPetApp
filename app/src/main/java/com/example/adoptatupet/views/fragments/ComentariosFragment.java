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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
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
 *  2) Un RecyclerView con todos los comentarios asociados a ese post.
 *  3) Zona inferior para escribir un nuevo comentario y enviarlo.
 *  4) Flecha de retroceso para volver al foro.
 */
public class ComentariosFragment extends Fragment {

    private static final String ARG_POST = "arg_post";

    private Mensaje postOriginal;
    private ImageView ivPostUserProfile;
    private TextView tvUserName, tvFechaMensaje, tvTextoMensaje, tvLikeCount;
    private ImageView ivPostImage;
    private RecyclerView rvComentarios;
    private CommentsAdapter commentsAdapter;
    private List<Comment> commentList = new ArrayList<>();

    private ImageView ivMyProfileForComment;
    private EditText etNewComment;
    private Button btnEnviarComentario;
    private ImageView btnAtras; // Flecha de retroceso

    private static final Gson gson = new Gson();

    public ComentariosFragment() {
        // Constructor vacío requerido
    }

    /**
     * Instancia el fragment recibiendo el objeto Mensaje (post original).
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
        // Recuperar el post original de los argumentos
        if (getArguments() != null && getArguments().containsKey(ARG_POST)) {
            String postJson = getArguments().getString(ARG_POST);
            postOriginal = gson.fromJson(postJson, Mensaje.class);
        }
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
            // Reemplaza el contenedor principal con el fragmento ForoFragment
            getActivity()
                    .getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, new com.example.adoptatupet.views.fragments.ForoFragment())
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

        if (postOriginal != null) {
            // Nombre y fecha
            tvUserName.setText(postOriginal.getUsuarioNombre());
            tvFechaMensaje.setText(postOriginal.getFechaPublicacion());
            // Texto
            tvTextoMensaje.setText(postOriginal.getTexto());
            // Foto de perfil
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
            // Imagen del post (si existe)
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
            // Contador de likes
            tvLikeCount.setText(String.valueOf(postOriginal.getLikeCount()));
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

        // ======================
        // 4) Zona para enviar comentario nuevo
        // ======================
        ivMyProfileForComment = view.findViewById(R.id.ivMyProfileForComment);
        etNewComment          = view.findViewById(R.id.etNewComment);
        btnEnviarComentario   = view.findViewById(R.id.btnEnviarComentario);

        // 4a) Cargar avatar propio
        cargarMiAvatarEnComentario();

        // 4b) Enviar comentario
        btnEnviarComentario.setOnClickListener(v -> {
            String textoComent = etNewComment.getText().toString().trim();
            if (TextUtils.isEmpty(textoComent)) {
                Toast.makeText(getContext(), "Escribe algo antes de enviar", Toast.LENGTH_SHORT).show();
                return;
            }
            if (postOriginal != null) {
                enviarNuevoComentario(postOriginal.getIdMensaje(), textoComent);
            }
        });

        return view;
    }

    /**
     * Carga los comentarios desde el servidor vía API:
     * GET https://…/api/get_comentarios.php?idPost=…
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

    /** Carga el avatar propio en ivMyProfileForComment */
    private void cargarMiAvatarEnComentario() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user", Context.MODE_PRIVATE);
        String fotoPerfilBase64 = prefs.getString("fotoPerfil", null);
        if (fotoPerfilBase64 != null && !fotoPerfilBase64.isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(fotoPerfilBase64, Base64.NO_WRAP);
                Bitmap bmp = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                ivMyProfileForComment.setImageBitmap(bmp);
            } catch (IllegalArgumentException e) {
                ivMyProfileForComment.setImageResource(R.drawable.default_avatar);
            }
        } else {
            ivMyProfileForComment.setImageResource(R.drawable.default_avatar);
        }
    }

    /**
     * Envía un comentario nuevo al servidor:
     * POST https://…/api/post_comentario.php
     * body: { "idUsuario":…, "idPost":…, "texto":… }
     */
    private void enviarNuevoComentario(int postId, String textoComent) {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user", Context.MODE_PRIVATE);
        int myUserId = prefs.getInt("idUsuario", -1);

        Map<String, Object> body = new HashMap<>();
        body.put("idUsuario", myUserId);
        body.put("idPost", postId);
        body.put("texto", textoComent);

        ApiService api = ApiClient.getClient().create(ApiService.class);
        Call<Mensaje> call = api.postComentario(body);
        call.enqueue(new Callback<Mensaje>() {
            @Override
            public void onResponse(Call<Mensaje> call, Response<Mensaje> response) {
                if (!isAdded()) return;
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    etNewComment.setText("");
                    // Refrescar lista de comentarios
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
    }
}
