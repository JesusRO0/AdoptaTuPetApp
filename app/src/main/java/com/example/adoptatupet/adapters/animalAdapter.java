package com.example.adoptatupet.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.adoptatupet.R;
import com.example.adoptatupet.models.Animal;
import java.util.List;

public class animalAdapter extends RecyclerView.Adapter<animalAdapter.VH> {

    /** Listener para clicks en el item */
    public interface OnItemClickListener {
        void onItemClick(Animal a);
    }

    private final List<Animal> lista;
    private OnItemClickListener listener;

    public animalAdapter(List<Animal> lista) {
        this.lista = lista;
    }

    /** Permite inyectar el listener desde el Fragment */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_animal, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        Animal a = lista.get(position);
        h.tvNombre.setText(a.getNombre());
        h.tvEspecie.setText(a.getEspecie());
        h.tvRaza.setText(a.getRaza());
        h.tvEdad.setText(a.getEdad());
        h.tvLocalidad.setText(a.getLocalidad());

        // Carga la foto
        if (a.getImagen() != null && !a.getImagen().isEmpty()) {
            byte[] data = Base64.decode(a.getImagen(), Base64.DEFAULT);
            Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
            h.ivFoto.setImageBitmap(bmp);
        } else {
            h.ivFoto.setImageResource(R.drawable.default_avatar);
        }

        // Click con pequeño retraso para que se vea el ripple
        h.container.setOnClickListener(v -> {
            v.setPressed(true);
            v.postDelayed(() -> {
                if (listener != null) {
                    listener.onItemClick(a);
                }
            }, 200);
        });
    }

    @Override public int getItemCount() {
        return lista.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        View container;
        TextView tvNombre, tvEspecie, tvRaza, tvEdad, tvLocalidad;
        ImageView ivFoto;

        VH(@NonNull View item) {
            super(item);
            // Este es el FrameLayout que tiene el ripple
            container   = item.findViewById(R.id.cardAnimalContainer);
            tvNombre    = item.findViewById(R.id.tvAnimalNombre);
            tvEspecie   = item.findViewById(R.id.tvAnimalEspecie);
            tvRaza      = item.findViewById(R.id.tvAnimalRaza);
            tvEdad      = item.findViewById(R.id.tvAnimalEdad);
            tvLocalidad = item.findViewById(R.id.tvAnimalLocalidad);
            ivFoto      = item.findViewById(R.id.ivAnimalFoto);
        }
    }
}
