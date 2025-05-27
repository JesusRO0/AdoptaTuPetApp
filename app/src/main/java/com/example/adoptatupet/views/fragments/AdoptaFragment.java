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
 * 5) Al hacer click en un animal, se abre AnimalDetailFragment pasando solo el ID del Animal.
 */
public class AdoptaFragment extends Fragment {

    private RecyclerView rv;
    private animalAdapter adapter;
    private final List<Animal> lista = new ArrayList<>();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup  container,
                             @Nullable Bundle     savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_adopta, container, false);

        rv = v.findViewById(R.id.adopta_recycler_view);
        rv.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new animalAdapter(lista);
        adapter.setOnItemClickListener(animal -> {
            AnimalDetailFragment detail = AnimalDetailFragment.newInstance(animal.getIdAnimal());
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, detail)
                    .addToBackStack(null)
                    .commit();
        });
        rv.setAdapter(adapter);

        // Carga inicial desde caché o servidor
        List<Animal> cache = animalController.getInstance(requireContext()).getCachedAnimals();
        if (cache != null && !cache.isEmpty()) {
            lista.clear();
            lista.addAll(cache);
            adapter.notifyDataSetChanged();
        } else {
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
                            // Sólo mostrar si el fragment sigue añadido
                            if (isAdded()) {
                                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
        }

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refrescar siempre al volver al fragment
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
                        if (isAdded()) {
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
}
