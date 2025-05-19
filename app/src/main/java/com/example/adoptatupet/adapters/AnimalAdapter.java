package com.example.adoptatupet.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.adoptatupet.R;
import com.example.adoptatupet.models.Animal;
import com.bumptech.glide.Glide;

import java.util.List;

/**
 * Adapter para mostrar una lista de animales en un RecyclerView.
 */
public class AnimalAdapter extends RecyclerView.Adapter<AnimalAdapter.ViewHolder> {

    private final List<Animal> animals;

    public AnimalAdapter(List<Animal> animals) {
        this.animals = animals;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        // Infla el layout item_animal.xml
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_animal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        Animal animal = animals.get(position);

        // Rellena los TextView con los datos del modelo
        holder.name.setText(animal.getNombre());
        holder.species.setText(animal.getEspecie());
        holder.breed.setText(animal.getRaza());
        holder.age.setText(animal.getEdad());
        holder.location.setText(animal.getLocalidad());

        // Carga la imagen: si la tienes en Base64, tendrías que decodificarla aquí;
        // si es URL, puedes usar Glide directamente:
        Glide.with(holder.itemView.getContext())
                .load(animal.getImagen())  // puede ser URL o ruta local
                .placeholder(R.drawable.default_avatar)
                .into(holder.photo);
    }

    @Override
    public int getItemCount() {
        return animals.size();
    }

    /**
     * ViewHolder que referencia las vistas de item_animal.xml
     */
    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView name;
        final TextView species;
        final TextView breed;
        final TextView age;
        final TextView location;
        final ImageView photo;

        ViewHolder(View itemView) {
            super(itemView);
            name     = itemView.findViewById(R.id.tvAnimalNombre);
            species  = itemView.findViewById(R.id.tvAnimalEspecie);
            breed    = itemView.findViewById(R.id.tvAnimalRaza);
            age      = itemView.findViewById(R.id.tvAnimalEdad);
            location = itemView.findViewById(R.id.tvAnimalLocalidad);
            photo    = itemView.findViewById(R.id.ivAnimalFoto);
        }
    }
}
