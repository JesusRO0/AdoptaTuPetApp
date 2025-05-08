package com.example.adoptatupet.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.adoptatupet.R;

public class HomeFragment extends Fragment {

    public HomeFragment() {
        // Requiere un constructor vacío
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Infla el layout para este fragmento
        View rootView = inflater.inflate(R.layout.fragment_home, container, false);

        // Encuentra el botón VER PERROS usando su ID
        Button verPerrosButton = rootView.findViewById(R.id.ver_perros_button); // Asegúrate de que este ID sea el correcto

        // Agrega el OnClickListener para que navegue a AdoptaFragment
        verPerrosButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Crea una nueva instancia del fragmento AdoptaFragment
                AdoptaFragment adoptaFragment = new AdoptaFragment();

                // Obtén el FragmentTransaction y realiza el reemplazo
                FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction(); // Usamos requireActivity()

                // Reemplaza el fragmento actual con AdoptaFragment en el contenedor correcto
                transaction.replace(R.id.nav_host_fragment, adoptaFragment); // Usamos nav_host_fragment aquí
                transaction.addToBackStack(null); // Añadir la transacción al back stack para que se pueda volver
                transaction.commit(); // Ejecuta la transacción
            }
        });

        return rootView; // Devuelve la vista inflada
    }
}
