package com.example.adoptatupet.views.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.fragment.app.Fragment;
import com.example.adoptatupet.R;

public class HomeFragment extends Fragment {

    public HomeFragment() {
        // Constructor vacío
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Infla el layout para este fragmento
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        Button conocenosButton = rootView.findViewById(R.id.conocenos_button);

        conocenosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Cambia manualmente el ítem seleccionado del BottomNavigationView
                BottomNavigationView bottomNavigationView = requireActivity().findViewById(R.id.bottomNavigationView);
                bottomNavigationView.setSelectedItemId(R.id.nav_contacto);
            }
        });


        // Agrega el OnClickListener para que navegue a AdoptaFragment
        Button verPerrosButton = rootView.findViewById(R.id.ver_perros_button);

        verPerrosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Cambia manualmente el item seleccionado del BottomNavigationView
                BottomNavigationView bottomNavigationView = requireActivity().findViewById(R.id.bottomNavigationView);
                bottomNavigationView.setSelectedItemId(R.id.nav_adopta);
            }
        });
        return rootView; // Devuelve la vista inflada
    }
}
