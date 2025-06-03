package com.example.adoptatupet.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.adoptatupet.R;
import com.example.adoptatupet.models.Comment;

import java.util.List;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    private final List<Comment> comments;

    public CommentsAdapter(List<Comment> comments) {
        this.comments = comments;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comentario, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment c = comments.get(position);
        Context ctx = holder.itemView.getContext();

        // Nombre y fecha
        holder.tvCommentUser.setText(c.getUsuarioNombre());
        holder.tvCommentFecha.setText(c.getFecha());

        // Texto del comentario
        holder.tvCommentTexto.setText(c.getTexto());

        // Foto de perfil del autor del comentario (Base64 → Bitmap)
        if (!TextUtils.isEmpty(c.getFotoPerfil())) {
            try {
                byte[] decodedBytes = Base64.decode(c.getFotoPerfil(), Base64.NO_WRAP);
                Bitmap bmp = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.ivCommentUserProfile.setImageBitmap(bmp);
            } catch (IllegalArgumentException e) {
                holder.ivCommentUserProfile.setImageResource(R.drawable.default_avatar);
            }
        } else {
            holder.ivCommentUserProfile.setImageResource(R.drawable.default_avatar);
        }
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCommentUserProfile;
        TextView tvCommentUser, tvCommentFecha, tvCommentTexto;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCommentUserProfile = itemView.findViewById(R.id.ivCommentUserProfile);
            tvCommentUser        = itemView.findViewById(R.id.tvCommentUser);
            tvCommentFecha       = itemView.findViewById(R.id.tvCommentFecha);
            tvCommentTexto       = itemView.findViewById(R.id.tvCommentTexto);
        }
    }
}
