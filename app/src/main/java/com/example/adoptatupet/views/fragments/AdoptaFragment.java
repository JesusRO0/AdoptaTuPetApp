package com.example.adoptatupet.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
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

public class AdoptaFragment extends Fragment {

    private RecyclerView rv;
    private animalAdapter adapter;
    // lista filtrada que muestra el RecyclerView
    private final List<Animal> lista = new ArrayList<>();
    // lista completa para aplicar filtros
    private final List<Animal> fullList = new ArrayList<>();

    private Spinner spinnerLocation, spinnerAge, spinnerSpecies;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup  container,
                             @Nullable Bundle     savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_adopta, container, false);

        // Inicializar spinners de filtro
        spinnerLocation = v.findViewById(R.id.spinner_location);
        spinnerAge      = v.findViewById(R.id.spinner_age);
        spinnerSpecies  = v.findViewById(R.id.spinner_species);

        // Listener único para todos los spinners
        AdapterView.OnItemSelectedListener filterListener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterAnimals();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) { }
        };
        spinnerLocation.setOnItemSelectedListener(filterListener);
        spinnerAge.setOnItemSelectedListener(filterListener);
        spinnerSpecies.setOnItemSelectedListener(filterListener);

        // Configurar RecyclerView y adapter
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
            fullList.clear();
            fullList.addAll(cache);
            filterAnimals();
        } else {
            animalController.getInstance(requireContext())
                    .fetchAllAnimals(new animalController.FetchCallback() {
                        @Override
                        public void onSuccess(List<Animal> animales) {
                            fullList.clear();
                            fullList.addAll(animales);
                            filterAnimals();
                        }
                        @Override
                        public void onError(String msg) {
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
        // Refrescar datos completos al volver al fragment
        animalController.getInstance(requireContext())
                .fetchAllAnimals(new animalController.FetchCallback() {
                    @Override public void onSuccess(List<Animal> animales) {
                        fullList.clear();
                        fullList.addAll(animales);
                        filterAnimals();
                    }
                    @Override public void onError(String msg) {
                        if (isAdded()) {
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    /**
     * Aplica los filtros seleccionados y actualiza la lista mostrada.
     */
    private void filterAnimals() {
        lista.clear();
        String loc     = spinnerLocation.getSelectedItem().toString();
        String age     = spinnerAge.getSelectedItem().toString();
        String species = spinnerSpecies.getSelectedItem().toString();

        for (Animal a : fullList) {
            if (!"Todos".equals(loc) && !a.getLocalidad().equals(loc)) continue;
            if (!"Todos".equals(age) && !a.getEdad().equals(age)) continue;
            if (!"Todos".equals(species) && !a.getEspecie().equals(species)) continue;
            lista.add(a);
        }
        adapter.notifyDataSetChanged();
    }
}
