package com.example.adoptatupet.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adoptatupet.R;
import com.example.adoptatupet.models.Mensaje;
import com.example.adoptatupet.network.ApiClient;
import com.example.adoptatupet.network.ApiService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Adaptador para los mensajes del foro.
 * Permite:
 *   • Mostrar foto, usuario, fecha, texto, imagen adjunta, contador de likes y corazón de like.
 *   • Botón de like/unlike (optimistic UI y persistencia en back).
 *   • Botón de comentario (invoca OnCommentClickListener).
 *   • Click en nombre de usuario (invoca OnUserNameClickListener).
 */
public class ForoAdapter extends RecyclerView.Adapter<ForoAdapter.MensajeViewHolder> {

    /** Llamado al hacer clic en el nombre de usuario de un post */
    public interface OnUserNameClickListener {
        void onUserNameClicked(int userId);
    }

    /** Llamado al hacer clic en el botón de comentar de un post */
    public interface OnCommentClickListener {
        /**
         * @param postId        ID del post en el que se quiere comentar
         * @param usuarioNombre Nombre del autor original (para mostrar en diálogo)
         * @param fotoPerfil    Avatar del autor original en Base64 (para mostrar en diálogo)
         */
        void onCommentClicked(int postId, String usuarioNombre, String fotoPerfil);
    }

    private List<Mensaje> listaMensajes;
    private OnUserNameClickListener listenerUsuario;
    private OnCommentClickListener listenerComentario;

    /**
     * @param inicialList       Lista inicial de mensajes (puede venir vacía)
     * @param lUsuario          Listener para clicks en nombre de usuario
     * @param lComentario       Listener para clicks en botón de comentario
     */
    public ForoAdapter(List<Mensaje> inicialList,
                       OnUserNameClickListener lUsuario,
                       OnCommentClickListener lComentario) {
        this.listaMensajes = inicialList;
        this.listenerUsuario = lUsuario;
        this.listenerComentario = lComentario;
    }

    /** Reemplaza la lista entera y refresca */
    public void setListaMensajes(List<Mensaje> nuevaLista) {
        this.listaMensajes = nuevaLista;
        notifyDataSetChanged();
    }

    public List<Mensaje> getListaMensajes() {
        return listaMensajes;
    }

    /** Permite cambiar el listener de usuario en tiempo de ejecución */
    public void setOnUserNameClickListener(OnUserNameClickListener l) {
        this.listenerUsuario = l;
    }

    /** Permite cambiar el listener de comentario en tiempo de ejecución */
    public void setOnCommentClickListener(OnCommentClickListener l) {
        this.listenerComentario = l;
    }

    @NonNull
    @Override
    public MensajeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_mensaje, parent, false);
        return new MensajeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MensajeViewHolder holder, int position) {
        Mensaje m = listaMensajes.get(position);
        Context ctx = holder.itemView.getContext();

        // 1) Nombre y fecha
        holder.tvUserName.setText(m.getUsuarioNombre());
        holder.tvFechaMensaje.setText(m.getFechaPublicacion());

        // 2) Texto del mensaje
        holder.tvTextoMensaje.setText(m.getTexto());

        // 3) Foto de perfil (Base64 → Bitmap)
        if (!TextUtils.isEmpty(m.getFotoPerfil())) {
            try {
                byte[] decodedBytes = Base64.decode(m.getFotoPerfil(), Base64.NO_WRAP);
                Bitmap bmp = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.ivPostUserProfile.setImageBitmap(bmp);
            } catch (IllegalArgumentException e) {
                holder.ivPostUserProfile.setImageResource(R.drawable.default_avatar);
            }
        } else {
            holder.ivPostUserProfile.setImageResource(R.drawable.default_avatar);
        }

        // 4) Imagen adjunta (si existe)
        if (!TextUtils.isEmpty(m.getImagenMensaje())) {
            holder.ivMensajeImagen.setVisibility(View.VISIBLE);
            try {
                byte[] imgBytes = Base64.decode(m.getImagenMensaje(), Base64.NO_WRAP);
                Bitmap bmp2 = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.length);
                holder.ivMensajeImagen.setImageBitmap(bmp2);
            } catch (IllegalArgumentException e) {
                holder.ivMensajeImagen.setVisibility(View.GONE);
            }
        } else {
            holder.ivMensajeImagen.setVisibility(View.GONE);
        }

        // 5) Like: contador y estado según 'likedByUser'
        holder.tvLikeCount.setText(String.valueOf(m.getLikeCount()));
        if (m.isLikedByUser()) {
            holder.btnLike.setImageResource(R.drawable.ic_heart_filled);
            holder.btnLike.setColorFilter(ContextCompat.getColor(ctx, R.color.red_500));
        } else {
            holder.btnLike.setImageResource(R.drawable.ic_heart_outline);
            holder.btnLike.setColorFilter(ContextCompat.getColor(ctx, R.color.gray_500));
        }

        // 6) Click en nombre de usuario → avisa al listenerUsuario
        holder.tvUserName.setOnClickListener(v -> {
            if (listenerUsuario != null) {
                listenerUsuario.onUserNameClicked(m.getUsuarioId());
            }
        });

        // 7) Click en botón de comentario → avisa al listenerComentario
        holder.btnComment.setOnClickListener(v -> {
            if (listenerComentario != null) {
                listenerComentario.onCommentClicked(
                        m.getIdMensaje(),
                        m.getUsuarioNombre(),
                        m.getFotoPerfil()
                );
            }
        });

        // 8) Click en “like/unlike” (optimistic UI + persistencia en back)
        holder.btnLike.setOnClickListener(v -> {
            boolean currentlyLiked = m.isLikedByUser();
            int currentCount = m.getLikeCount();

            // a) Optimistic UI: actualizar contador local y corazón
            if (currentlyLiked) {
                m.setLikedByUser(false);
                m.setLikeCount(currentCount - 1);
                holder.btnLike.setImageResource(R.drawable.ic_heart_outline);
                holder.btnLike.setColorFilter(ContextCompat.getColor(ctx, R.color.gray_500));
                holder.tvLikeCount.setText(String.valueOf(m.getLikeCount()));
            } else {
                m.setLikedByUser(true);
                m.setLikeCount(currentCount + 1);
                holder.btnLike.setImageResource(R.drawable.ic_heart_filled);
                holder.btnLike.setColorFilter(ContextCompat.getColor(ctx, R.color.red_500));
                holder.tvLikeCount.setText(String.valueOf(m.getLikeCount()));
            }

            // b) Llamada al backend para persistir el cambio
            Map<String, Integer> body = new HashMap<>();
            body.put("usuarioId", m.getUsuarioId());
            body.put("idPost", m.getIdMensaje());

            ApiService api = ApiClient.getClient().create(ApiService.class);
            if (currentlyLiked) {
                Call<Mensaje> callUn = api.unlikePost(body);
                callUn.enqueue(new Callback<Mensaje>() {
                    @Override
                    public void onResponse(Call<Mensaje> call, Response<Mensaje> response) {
                        if (!(response.isSuccessful() && response.body() != null && response.body().isSuccess())) {
                            // Si falla, revertir UI
                            m.setLikedByUser(true);
                            m.setLikeCount(m.getLikeCount() + 1);
                            holder.btnLike.setImageResource(R.drawable.ic_heart_filled);
                            holder.btnLike.setColorFilter(ContextCompat.getColor(ctx, R.color.red_500));
                            holder.tvLikeCount.setText(String.valueOf(m.getLikeCount()));
                            Toast.makeText(ctx, "No se pudo quitar el Like", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<Mensaje> call, Throwable t) {
                        // Si falla, revertir UI
                        m.setLikedByUser(true);
                        m.setLikeCount(m.getLikeCount() + 1);
                        holder.btnLike.setImageResource(R.drawable.ic_heart_filled);
                        holder.btnLike.setColorFilter(ContextCompat.getColor(ctx, R.color.red_500));
                        holder.tvLikeCount.setText(String.valueOf(m.getLikeCount()));
                        Toast.makeText(ctx, "Error de red al quitar Like", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                Call<Mensaje> callLike = api.likePost(body);
                callLike.enqueue(new Callback<Mensaje>() {
                    @Override
                    public void onResponse(Call<Mensaje> call, Response<Mensaje> response) {
                        if (!(response.isSuccessful() && response.body() != null && response.body().isSuccess())) {
                            // Si falla, revertir UI
                            m.setLikedByUser(false);
                            m.setLikeCount(m.getLikeCount() - 1);
                            holder.btnLike.setImageResource(R.drawable.ic_heart_outline);
                            holder.btnLike.setColorFilter(ContextCompat.getColor(ctx, R.color.gray_500));
                            holder.tvLikeCount.setText(String.valueOf(m.getLikeCount()));
                            Toast.makeText(ctx, "No se pudo dar Like", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override
                    public void onFailure(Call<Mensaje> call, Throwable t) {
                        // Si falla, revertir UI
                        m.setLikedByUser(false);
                        m.setLikeCount(m.getLikeCount() - 1);
                        holder.btnLike.setImageResource(R.drawable.ic_heart_outline);
                        holder.btnLike.setColorFilter(ContextCompat.getColor(ctx, R.color.gray_500));
                        holder.tvLikeCount.setText(String.valueOf(m.getLikeCount()));
                        Toast.makeText(ctx, "Error de red al dar Like", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaMensajes != null ? listaMensajes.size() : 0;
    }

    /** ViewHolder que contiene todos los views de un ítem de mensaje */
    static class MensajeViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPostUserProfile, ivMensajeImagen;
        TextView tvUserName, tvFechaMensaje, tvTextoMensaje, tvLikeCount;
        ImageButton btnLike, btnComment;

        public MensajeViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPostUserProfile = itemView.findViewById(R.id.ivPostUserProfile);
            tvUserName        = itemView.findViewById(R.id.tvUserName);
            tvFechaMensaje    = itemView.findViewById(R.id.tvFechaMensaje);
            tvTextoMensaje    = itemView.findViewById(R.id.tvTextoMensaje);
            ivMensajeImagen   = itemView.findViewById(R.id.ivMensajeImagen);
            btnLike           = itemView.findViewById(R.id.btnLike);
            tvLikeCount       = itemView.findViewById(R.id.tvLikeCount);
            // “Comentario” debe estar definido en item_mensaje.xml con id btnComment
            btnComment        = itemView.findViewById(R.id.btnComment);
        }
    }
}
