package com.example.adoptatupet.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.example.adoptatupet.adapters.animalAdapter;
import com.example.adoptatupet.models.Animal;
import com.example.adoptatupet.R;

import java.util.ArrayList;
import java.util.List;

public class AdoptaFragment extends Fragment {

    private RecyclerView recyclerView;
    private animalAdapter adapter;
    private List<Animal> animalList = new ArrayList<>();

    public AdoptaFragment() {
        // Requiere un constructor vacío
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootView = inflater.inflate(R.layout.fragment_adopta, container, false);

        recyclerView = rootView.findViewById(R.id.adopta_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new animalAdapter(animalList);
        recyclerView.setAdapter(adapter);

        // Agregar algunos animales de ejemplo
        loadAnimals();

        return rootView;
    }

    private void loadAnimals() {

        adapter.notifyDataSetChanged();
    }
}