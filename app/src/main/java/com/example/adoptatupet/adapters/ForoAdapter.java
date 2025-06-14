package com.example.adoptatupet.adapters;

import android.content.Context;
import android.content.SharedPreferences;
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
 * Adaptador que muestra cada Mensaje en un RecyclerView del foro.
 * Incluye:
 *  1) currentUserId: para determinar si el post es propio (mostrar botón eliminar).
 *  2) Seis listeners en el constructor:
 *     - OnUserNameClickListener
 *     - OnCommentClickListener
 *     - OnPostClickListener
 *     - OnDeleteClickListener
 */
public class ForoAdapter extends RecyclerView.Adapter<ForoAdapter.MensajeViewHolder> {

    public interface OnUserNameClickListener {
        void onUserNameClicked(int userId);
    }

    public interface OnCommentClickListener {
        void onCommentClicked(Mensaje mensaje);
    }

    public interface OnPostClickListener {
        void onPostClicked(Mensaje mensaje);
    }

    public interface OnDeleteClickListener {
        void onDeleteClicked(Mensaje mensaje, int position);
    }

    private List<Mensaje> listaMensajes;
    private int currentUserId;  // ID del usuario logueado
    private OnUserNameClickListener userListener;
    private OnCommentClickListener commentListener;
    private OnPostClickListener postListener;
    private OnDeleteClickListener deleteListener;

    public ForoAdapter(
            List<Mensaje> inicialList,
            int currentUserId,
            OnUserNameClickListener uListener,
            OnCommentClickListener cListener,
            OnPostClickListener pListener,
            OnDeleteClickListener dListener
    ) {
        this.listaMensajes   = inicialList;
        this.currentUserId   = currentUserId;
        this.userListener    = uListener;
        this.commentListener = cListener;
        this.postListener    = pListener;
        this.deleteListener  = dListener;
    }

    public void setListaMensajes(List<Mensaje> nuevaLista) {
        this.listaMensajes = nuevaLista;
        notifyDataSetChanged();
    }

    public List<Mensaje> getListaMensajes() {
        return listaMensajes;
    }

    public void setOnUserNameClickListener(OnUserNameClickListener l) {
        this.userListener = l;
    }

    public void setOnCommentClickListener(OnCommentClickListener l) {
        this.commentListener = l;
    }

    public void setOnPostClickListener(OnPostClickListener l) {
        this.postListener = l;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener l) {
        this.deleteListener = l;
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

        // 5) Like: estado inicial
        holder.tvLikeCount.setText(String.valueOf(m.getLikeCount()));
        if (m.isLikedByUser()) {
            holder.btnLike.setImageResource(R.drawable.ic_heart_filled);
            holder.btnLike.setColorFilter(ContextCompat.getColor(ctx, R.color.red_500));
        } else {
            holder.btnLike.setImageResource(R.drawable.ic_heart_outline);
            holder.btnLike.setColorFilter(ContextCompat.getColor(ctx, R.color.gray_500));
        }

        // 6) CommentCount: mostrar el número de comentarios
        holder.tvCommentCount.setText(String.valueOf(m.getCommentCount()));

        // 7) Mostrar u ocultar botón “Eliminar” si:
        //    - es autor o
        //    - el email en SharedPreferences es "admin@gmail.com"
        SharedPreferences prefs = ctx.getSharedPreferences("user", Context.MODE_PRIVATE);
        String userEmail = prefs.getString("email", "");
        if (m.getUsuarioId() == currentUserId || "admin@gmail.com".equals(userEmail)) {
            holder.btnDeleteMessage.setVisibility(View.VISIBLE);
        } else {
            holder.btnDeleteMessage.setVisibility(View.GONE);
        }

        // 8) Click en nombre de usuario → avisar al listener
        holder.tvUserName.setOnClickListener(v -> {
            if (userListener != null) {
                userListener.onUserNameClicked(m.getUsuarioId());
            }
        });

        // 9) Click en “like/unlike” (optimistic UI + backend)
        holder.btnLike.setOnClickListener(v -> {
            boolean currentlyLiked = m.isLikedByUser();
            int currentCount = m.getLikeCount();

            // a) Actualizar UI local (optimistic)
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

            // b) Llamada al backend para persistir el cambio (usando currentUserId)
            Map<String, Integer> body = new HashMap<>();
            body.put("usuarioId", currentUserId);
            body.put("idPost", m.getIdMensaje());

            ApiService api = ApiClient.getClient().create(ApiService.class);
            if (currentlyLiked) {
                Call<Mensaje> callUn = api.unlikePost(body);
                callUn.enqueue(new Callback<Mensaje>() {
                    @Override
                    public void onResponse(Call<Mensaje> call, Response<Mensaje> response) {
                        if (!(response.isSuccessful() && response.body() != null && response.body().isSuccess())) {
                            // Revertir si falla
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
                        // Revertir si falla
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
                            // Revertir si falla
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
                        // Revertir si falla
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

        // 10) Click en icono de comentario → avisar al listener
        holder.btnComment.setOnClickListener(v -> {
            if (commentListener != null) {
                commentListener.onCommentClicked(m);
            }
        });

        // 11) Click en TODO el bloque del post → abrir ComentariosFragment
        holder.itemView.setOnClickListener(v -> {
            if (postListener != null) {
                postListener.onPostClicked(m);
            }
        });

        // 12) Click en icono “Eliminar” → avisar al deleteListener
        holder.btnDeleteMessage.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onDeleteClicked(m, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return listaMensajes != null ? listaMensajes.size() : 0;
    }

    static class MensajeViewHolder extends RecyclerView.ViewHolder {
        ImageView   ivPostUserProfile, ivMensajeImagen;
        TextView    tvUserName, tvFechaMensaje, tvTextoMensaje, tvLikeCount, tvCommentCount;
        ImageButton btnLike, btnComment, btnDeleteMessage;

        public MensajeViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPostUserProfile = itemView.findViewById(R.id.ivPostUserProfile);
            tvUserName        = itemView.findViewById(R.id.tvUserName);
            tvFechaMensaje    = itemView.findViewById(R.id.tvFechaMensaje);
            tvTextoMensaje    = itemView.findViewById(R.id.tvTextoMensaje);
            ivMensajeImagen   = itemView.findViewById(R.id.ivMensajeImagen);
            btnLike           = itemView.findViewById(R.id.btnLike);
            btnComment        = itemView.findViewById(R.id.btnComment);
            tvLikeCount       = itemView.findViewById(R.id.tvLikeCount);
            tvCommentCount    = itemView.findViewById(R.id.tvCommentCount);
            btnDeleteMessage  = itemView.findViewById(R.id.btnDeleteMessage);
        }
    }
}
