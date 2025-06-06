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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adoptatupet.R;
import com.example.adoptatupet.models.Animal;

import java.util.ArrayList;
import java.util.List;

/**
 * Adaptador para el slider de animales en HomeFragment.
 * Se muestra una imagen por cada Animal en ViewPager2.
 *
 * Ahora incluye:
 *  - Interfaz OnItemClickListener para detectar clics en cada slide.
 *  - Método setOnItemClickListener(...) para registrar el listener.
 */
public class AnimalSliderAdapter extends RecyclerView.Adapter<AnimalSliderAdapter.AnimalViewHolder> {

    /**
     * Interfaz para notificar clic en un Animal del slider.
     */
    public interface OnItemClickListener {
        void onItemClick(Animal animal);
    }

    private Context context;
    private List<Animal> animalList;
    private OnItemClickListener itemClickListener;

    /**
     * Constructor: recibe un Context y una lista de animales (puede ser null inicialmente).
     *
     * @param context     contexto
     * @param inicialList lista inicial de animales (puede ser null)
     */
    public AnimalSliderAdapter(Context context, List<Animal> inicialList) {
        this.context = context;
        this.animalList = (inicialList != null) ? inicialList : new ArrayList<>();
    }

    /**
     * Registra el listener para detectar clics en cada slide.
     *
     * @param listener instancia de OnItemClickListener
     */
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    /**
     * Actualiza la lista de animales y notifica cambios.
     *
     * @param nuevaLista lista de animales
     */
    public void setAnimalList(List<Animal> nuevaLista) {
        this.animalList = (nuevaLista != null) ? nuevaLista : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public AnimalViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_animal_slider, parent, false);
        return new AnimalViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AnimalViewHolder holder, int position) {
        Animal animal = animalList.get(position);

        // Si existe imagen en Base64, convertir a Bitmap y mostrar
        String imagenBase64 = animal.getImagen();
        if (!TextUtils.isEmpty(imagenBase64)) {
            try {
                byte[] decodedBytes = Base64.decode(imagenBase64, Base64.NO_WRAP);
                Bitmap bmp = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                holder.ivAnimal.setImageBitmap(bmp);
            } catch (IllegalArgumentException e) {
                // Si falla conversion, dejar recurso por defecto
                holder.ivAnimal.setImageResource(R.drawable.gato_1);
            }
        } else {
            // Si no hay imagen, usar placeholder
            holder.ivAnimal.setImageResource(R.drawable.perro_1);
        }

        // Configurar clic en el item completo
        holder.itemView.setOnClickListener(v -> {
            if (itemClickListener != null && animal != null) {
                itemClickListener.onItemClick(animal);
            }
        });
    }

    @Override
    public int getItemCount() {
        return (animalList != null) ? animalList.size() : 0;
    }

    /**
     * ViewHolder para cada slide (item_animal_slider.xml).
     */
    static class AnimalViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAnimal;

        public AnimalViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAnimal = itemView.findViewById(R.id.ivAnimalImagenSlider);
        }
    }
}
