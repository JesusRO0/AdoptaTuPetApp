package com.example.adoptatupet.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adoptatupet.R;
import com.example.adoptatupet.models.Animal;

import java.util.List;

public class AnimalSliderAdapter extends RecyclerView.Adapter<AnimalSliderAdapter.SliderViewHolder> {

    private Context context;
    private List<Animal> animalList;

    /**
     * Constructor: recibe contexto y lista de Animal.
     * Inicialmente puedes pasar una lista vacía, luego llamas a setAnimalList().
     */
    public AnimalSliderAdapter(Context context, List<Animal> animalList) {
        this.context = context;
        this.animalList = animalList;
    }

    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_animal_slider, parent, false);
        return new SliderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
        Animal a = animalList.get(position);

        // Si la imagen viene en Base64, la decodificamos a Bitmap
        if (a.getImagen() != null) {
            try {
                byte[] decodedBytes = Base64.decode(a.getImagen(), Base64.NO_WRAP);
                Bitmap bmp = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.ivAnimalImagen.setImageBitmap(bmp);
            } catch (IllegalArgumentException e) {
                // Si falla la decodificación, mostramos un placeholder
                holder.ivAnimalImagen.setImageResource(R.drawable.gato_1);
            }
        } else {
            holder.ivAnimalImagen.setImageResource(R.drawable.perro_1);
        }
    }

    @Override
    public int getItemCount() {
        return animalList != null ? animalList.size() : 0;
    }

    /**
     * Permite actualizar la lista de animales cuando recibamos datos de la API.
     */
    public void setAnimalList(List<Animal> nuevaLista) {
        this.animalList = nuevaLista;
        notifyDataSetChanged();
    }

    static class SliderViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAnimalImagen;
        public SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAnimalImagen = itemView.findViewById(R.id.ivAnimalImagenSlider);
        }
    }
}
