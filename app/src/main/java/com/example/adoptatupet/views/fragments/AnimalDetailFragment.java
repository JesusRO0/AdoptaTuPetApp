package com.example.adoptatupet.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.adoptatupet.R;
import com.example.adoptatupet.models.Animal;

import android.util.Base64;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

/**
 * Fragment que muestra el detalle completo de un Animal.
 */
public class AnimalDetailFragment extends Fragment {

    private static final String ARG_ANIMAL = "animal";

    private Animal animal;

    public AnimalDetailFragment() {
        // Required empty public constructor
    }

    /**
     * Crea una nueva instancia pasando el Animal como Parcelable.
     */
    public static AnimalDetailFragment newInstance(@NonNull Animal animal) {
        AnimalDetailFragment fragment = new AnimalDetailFragment();
        Bundle args = new Bundle();
        // Ahora usamos putParcelable en lugar de putSerializable
        args.putParcelable(ARG_ANIMAL, animal);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Recuperamos el Animal con getParcelable
        if (getArguments() != null) {
            animal = getArguments().getParcelable(ARG_ANIMAL);
        }
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup  container,
                             @Nullable Bundle     savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_animal_detail, container, false);

        // Referencias a las vistas del layout
        ImageView ivFoto       = view.findViewById(R.id.ivDetailFoto);
        TextView tvNombre      = view.findViewById(R.id.tvDetailNombre);
        TextView tvEspecie     = view.findViewById(R.id.tvDetailEspecie);
        TextView tvRaza        = view.findViewById(R.id.tvDetailRaza);
        TextView tvEdad        = view.findViewById(R.id.tvDetailEdad);
        TextView tvLocalidad   = view.findViewById(R.id.tvDetailLocalidad);
        TextView tvSexo        = view.findViewById(R.id.tvDetailSexo);
        TextView tvTamano      = view.findViewById(R.id.tvDetailTamano);
        TextView tvDescripcion = view.findViewById(R.id.tvDetailDescripcion);

        if (animal != null) {
            // Rellenamos los campos
            tvNombre.setText(animal.getNombre());
            tvEspecie.setText("Especie: " + animal.getEspecie());
            tvRaza.setText("Raza: " + animal.getRaza());
            tvEdad.setText("Edad: " + animal.getEdad());
            tvLocalidad.setText("Localidad: " + animal.getLocalidad());
            tvSexo.setText("Sexo: " + animal.getSexo());
            tvTamano.setText("Tamaño: " + animal.getTamano());
            tvDescripcion.setText(animal.getDescripcion());

            // Decodificar imagen Base64
            String b64 = animal.getImagen();
            if (b64 != null && !b64.isEmpty()) {
                byte[] data = Base64.decode(b64, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                ivFoto.setImageBitmap(bmp);
            } else {
                ivFoto.setImageResource(R.drawable.default_avatar);
            }
        }

        return view;
    }
}
