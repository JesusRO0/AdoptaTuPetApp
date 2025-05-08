package com.example.adoptatupet;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.adoptatupet.fragments.AdoptaFragment;
import com.example.adoptatupet.fragments.ContactoFragment;
import com.example.adoptatupet.fragments.ForoFragment;
import com.example.adoptatupet.fragments.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Instancia BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Establecer listener para el BottomNavigationView
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                // Reemplazamos el switch por if-else
                if (item.getItemId() == R.id.nav_home) {
                    // Código para mostrar el fragmento de inicio
                    loadFragment(new HomeFragment());
                    return true;
                } else if (item.getItemId() == R.id.nav_adopta) {
                    // Código para mostrar el fragmento de adopción
                    loadFragment(new AdoptaFragment());
                    return true;
                } else if (item.getItemId() == R.id.nav_foro) {
                    // Código para mostrar el fragmento de foro
                    loadFragment(new ForoFragment());
                    return true;
                } else if (item.getItemId() == R.id.nav_contacto) {
                    // Código para mostrar el fragmento de contacto
                    loadFragment(new ContactoFragment());
                    return true;
                }
                return false; // Devuelve false si el ID no coincide con ningún caso
            }
        });

        // Si es la primera vez que se lanza la app, seleccionamos el HomeFragment por defecto
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home); // Selecciona Home al inicio
        }
    }

    // Método para cargar los fragmentos
    private void loadFragment(Fragment fragment) {
        // Obtén el FragmentTransaction y realiza el reemplazo
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, fragment) // Usa el ID del FrameLayout donde los fragmentos se reemplazan
                .commit(); // Ejecuta la transacción
    }
}
