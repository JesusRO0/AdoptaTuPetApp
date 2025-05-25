package com.example.adoptatupet.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.adoptatupet.R;
import com.example.adoptatupet.adapters.animalAdapter;
import com.example.adoptatupet.controllers.animalController;
import com.example.adoptatupet.models.Animal;

import java.util.ArrayList;
import java.util.List;

/**
 * AdoptaFragment: muestra la lista de animales disponibles.
 *
 * Flujo:
 * 1) Configura el RecyclerView con LinearLayoutManager.
 * 2) Crea el adapter y le inyecta un listener para reaccionar al click en cada tarjeta.
 * 3) Intenta rellenar la lista desde la caché local (animalController.getCachedAnimals()).
 * 4) Si no hay caché, hace fetchAllAnimals() al servidor y actualiza la lista.
 * 5) Al hacer click en un animal, se abre AnimalDetailFragment pasando el objeto Parcelable.
 */
public class AdoptaFragment extends Fragment {

    // RecyclerView que muestra las tarjetas
    private RecyclerView  rv;
    // Adapter que enlace la lista de Animal con el RecyclerView
    private animalAdapter adapter;
    // Lista interna que alimenta el adapter
    private final List<Animal> lista = new ArrayList<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup  container,
                             @Nullable Bundle     savedInstanceState) {
        // 1) Inflar layout y referenciar RecyclerView
        View v = inflater.inflate(R.layout.fragment_adopta, container, false);
        rv = v.findViewById(R.id.adopta_recycler_view);

        // 2) Configurar LayoutManager vertical
        rv.setLayoutManager(new LinearLayoutManager(getContext()));

        // 3) Crear adapter con la lista inicial vacía
        adapter = new animalAdapter(lista);

        // 4) Inyectar listener para clicks en cada tarjeta del adaptador
        adapter.setOnItemClickListener(animal -> {
            // Al hacer click, construimos el detail fragment y pasamos el Animal
            AnimalDetailFragment detail = AnimalDetailFragment.newInstance(animal);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, detail)
                    .addToBackStack(null)
                    .commit();
        });

        // 5) Asignar el adapter al RecyclerView
        rv.setAdapter(adapter);

        // 6) Intentar cargar animales desde caché local
        List<Animal> cache = animalController.getInstance(requireContext())
                .getCachedAnimals();
        if (cache != null && !cache.isEmpty()) {
            // Si hay caché, rellenar inmediatamente la lista
            lista.clear();
            lista.addAll(cache);
            adapter.notifyDataSetChanged();
        } else {
            // Si no hay caché, solicitar al servidor
            animalController.getInstance(requireContext())
                    .fetchAllAnimals(new animalController.FetchCallback() {
                        @Override
                        public void onSuccess(List<Animal> animales) {
                            // Actualizar lista y refrescar UI
                            lista.clear();
                            lista.addAll(animales);
                            adapter.notifyDataSetChanged();
                        }
                        @Override
                        public void onError(String msg) {
                            // Mostrar error con Toast
                            Toast.makeText(getContext(),
                                    msg,
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        }

        return v;
    }
    @Override
    public void onResume() {
        super.onResume();
        // Re-cargar siempre al volver a primer plano
        animalController.getInstance(requireContext())
                .fetchAllAnimals(new animalController.FetchCallback() {
                    @Override
                    public void onSuccess(List<Animal> animales) {
                        lista.clear();
                        lista.addAll(animales);
                        adapter.notifyDataSetChanged();
                    }
                    @Override
                    public void onError(String msg) {
                        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
