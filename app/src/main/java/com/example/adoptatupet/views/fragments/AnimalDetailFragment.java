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
 * Ahora recibe solo el ID para evitar TransactionTooLargeException.
 * Incluye botones BORRAR y EDITAR solo para admin.
 */
public class AnimalDetailFragment extends Fragment {

    private static final String ARG_ID = "animal_id";
    private int animalId;
    private Animal animal;

    public AnimalDetailFragment() {
        // Required empty public constructor
    }

    /**
     * Instancia pasando solo el ID del Animal.
     */
    public static AnimalDetailFragment newInstance(int idAnimal) {
        AnimalDetailFragment fragment = new AnimalDetailFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ID, idAnimal);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Recuperar solo el ID desde los argumentos
        if (getArguments() != null) {
            animalId = getArguments().getInt(ARG_ID, -1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
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

        // 1) Recuperar el animal por ID
        animalController.getInstance(requireContext())
                .fetchAnimalById(animalId, new animalController.AnimalByIdCallback() {
                    @Override
                    public void onSuccess(Animal a) {
                        animal = a;
                        // 2) Mostrar datos
                        tvNombre.setText(a.getNombre());
                        tvEspecie.setText(a.getEspecie());
                        tvRaza.setText(a.getRaza());
                        tvEdad.setText(a.getEdad());
                        tvLocalidad.setText(a.getLocalidad());
                        tvSexo.setText(a.getSexo());
                        tvTamano.setText(a.getTamano());
                        tvDescripcion.setText(a.getDescripcion());
                        String b64 = a.getImagen();
                        if (b64 != null && !b64.isEmpty()) {
                            byte[] data = Base64.decode(b64, Base64.DEFAULT);
                            ivFoto.setImageBitmap(
                                    android.graphics.BitmapFactory.decodeByteArray(data, 0, data.length)
                            );
                        } else {
                            ivFoto.setImageResource(R.drawable.default_avatar);
                        }
                    }
                    @Override
                    public void onError(String msg) {
                        Toast.makeText(requireContext(),
                                        "Error cargando animal: " + msg,
                                        Toast.LENGTH_LONG)
                                .show();
                    }
                });

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Desactivar overscroll
        NestedScrollView scroll = view.findViewById(R.id.scrollAnimalDetail);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        CoordinatorLayout root = view.findViewById(R.id.detail_root);
        root.setOverScrollMode(View.OVER_SCROLL_NEVER);

        // Toolbar atrás
        Toolbar toolbar = view.findViewById(R.id.toolbar_detail);
        toolbar.setNavigationOnClickListener(v -> requireActivity().onBackPressed());

        // Botones admin
        MaterialButton btnBorrar = view.findViewById(R.id.btnBorrarAnimal);
        MaterialButton btnEditar = view.findViewById(R.id.btnEditarAnimal);
        String email = usuarioController
                .getInstance(requireContext())
                .loadFromPrefs()
                .getEmail();

        if ("admin@gmail.com".equalsIgnoreCase(email)) {
            btnBorrar.setVisibility(View.VISIBLE);
            btnEditar.setVisibility(View.VISIBLE);

            // Borrar
            btnBorrar.setOnClickListener(v ->
                    animalController.getInstance(requireContext())
                            .deleteAnimal(animalId, new animalController.AnimalCallback() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(getContext(),
                                                    "Animal borrado",
                                                    Toast.LENGTH_SHORT)
                                            .show();
                                    requireActivity().onBackPressed();
                                }
                                @Override
                                public void onError(String message) {
                                    Toast.makeText(getContext(),
                                                    "Error: " + message,
                                                    Toast.LENGTH_LONG)
                                            .show();
                                }
                            })
            );

            // Editar → lanzamos UpdateAnimalFragment con el mismo ID
            btnEditar.setOnClickListener(v -> {
                UpdateAnimalFragment editFrag = UpdateAnimalFragment.newInstance(animalId);
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
