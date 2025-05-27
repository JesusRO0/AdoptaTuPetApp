package com.example.adoptatupet.views.fragments;

import android.os.Bundle;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.example.adoptatupet.R;
import com.example.adoptatupet.controllers.animalController;
import com.example.adoptatupet.controllers.usuarioController;
import com.example.adoptatupet.models.Animal;
import com.google.android.material.button.MaterialButton;

/**
 * Fragment que muestra el detalle completo de un Animal.
 * Incluye botones de BORRAR y EDITAR, visibles solo para admin.
 */
public class AnimalDetailFragment extends Fragment {

    private static final String ARG_ANIMAL = "animal";
    private Animal animal;

    public AnimalDetailFragment() {
        // Required empty public constructor
    }

    /**
     * Crea instancia con el objeto Animal.
     */
    public static AnimalDetailFragment newInstance(@NonNull Animal animal) {
        AnimalDetailFragment fragment = new AnimalDetailFragment();
        Bundle args = new Bundle();
        args.putParcelable(ARG_ANIMAL, animal);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Recuperar el Animal desde los argumentos
        if (getArguments() != null) {
            animal = getArguments().getParcelable(ARG_ANIMAL);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflar layout
        View view = inflater.inflate(R.layout.fragment_animal_detail, container, false);

        // Bind de vistas
        ImageView ivFoto        = view.findViewById(R.id.ivDetailFoto);
        TextView  tvNombre      = view.findViewById(R.id.tvDetailNombre);
        TextView  tvEspecie     = view.findViewById(R.id.tvDetailEspecie);
        TextView  tvRaza        = view.findViewById(R.id.tvDetailRaza);
        TextView  tvEdad        = view.findViewById(R.id.tvDetailEdad);
        TextView  tvLocalidad   = view.findViewById(R.id.tvDetailLocalidad);
        TextView  tvSexo        = view.findViewById(R.id.tvDetailSexo);
        TextView  tvTamano      = view.findViewById(R.id.tvDetailTamano);
        TextView  tvDescripcion = view.findViewById(R.id.tvDetailDescripcion);

        // Mostrar datos del animal
        if (animal != null) {
            tvNombre.setText(animal.getNombre());
            tvEspecie.setText(animal.getEspecie());
            tvRaza.setText(animal.getRaza());
            tvEdad.setText(animal.getEdad());
            tvLocalidad.setText(animal.getLocalidad());
            tvSexo.setText(animal.getSexo());
            tvTamano.setText(animal.getTamano());
            tvDescripcion.setText(animal.getDescripcion());

            // Cargar imagen en Base64
            String b64 = animal.getImagen();
            if (b64 != null && !b64.isEmpty()) {
                byte[] data = Base64.decode(b64, Base64.DEFAULT);
                ivFoto.setImageBitmap(
                        android.graphics.BitmapFactory.decodeByteArray(data, 0, data.length)
                );
            } else {
                ivFoto.setImageResource(R.drawable.default_avatar);
            }
        }

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1) Desactivar overscroll en scroll y root
        NestedScrollView scroll = view.findViewById(R.id.scrollAnimalDetail);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        CoordinatorLayout root = view.findViewById(R.id.detail_root);
        root.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // 2) Configurar flecha de atrás en la toolbar
        Toolbar toolbar = view.findViewById(R.id.toolbar_detail);
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        // 3) Botones (solo admin)
        MaterialButton btnBorrar = view.findViewById(R.id.btnBorrarAnimal);
        MaterialButton btnEditar = view.findViewById(R.id.btnEditarAnimal);
        String email = usuarioController
                .getInstance(requireContext())
                .loadFromPrefs()
                .getEmail();

        if ("admin@gmail.com".equalsIgnoreCase(email)) {
            // Mostrar Borrar
            btnBorrar.setVisibility(View.VISIBLE);
            btnBorrar.setOnClickListener(v -> {
                animalController.getInstance(requireContext())
                        .deleteAnimal(animal.getIdAnimal(), new animalController.AnimalCallback() {
                            @Override
                            public void onSuccess() {
                                Toast.makeText(getContext(), "Animal borrado", Toast.LENGTH_SHORT).show();
                                requireActivity().onBackPressed();
                            }
                            @Override
                            public void onError(String message) {
                                Toast.makeText(getContext(), "Error: " + message, Toast.LENGTH_LONG).show();
                            }
                        });
            });
            // Mostrar Editar
            btnEditar.setVisibility(View.VISIBLE);
            btnEditar.setOnClickListener(v -> {
                // Abrir fragment de edición
                // Abrir fragment de edición pasándole el objeto Animal
                UpdateAnimalFragment editFrag = UpdateAnimalFragment.newInstance(animal);
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.nav_host_fragment, editFrag)
                        .addToBackStack(null)
                        .commit();
            });
        } else {
            btnBorrar.setVisibility(View.GONE);
            btnEditar.setVisibility(View.GONE);
        }
    }
}
