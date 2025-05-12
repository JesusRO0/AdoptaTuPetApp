package com.example.adoptatupet.views;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.adoptatupet.R;
import com.example.adoptatupet.views.fragments.AdoptaFragment;
import com.example.adoptatupet.views.fragments.ContactoFragment;
import com.example.adoptatupet.views.fragments.ForoFragment;
import com.example.adoptatupet.views.fragments.HomeFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout; // Menú lateral
    private NavigationView navigationView; // Vista del menú
    private Toolbar topAppBar; // Barra superior

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Inicializa Toolbar
        topAppBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topAppBar);

        // Inicializa DrawerLayout y NavigationView
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        // Crea el botón hamburguesa y sincroniza el estado
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, topAppBar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Escucha clicks en los ítems del menú lateral
        navigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.nav_login) {
                    showLoginDialog();
                    drawerLayout.closeDrawers(); // Cierra el menú lateral
                    return true;
                }
                return false;
            }
        });

        // Instancia BottomNavigationView
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Establecer listener para el BottomNavigationView
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                if (item.getItemId() == R.id.nav_home) {
                    loadFragment(new HomeFragment());
                    return true;
                } else if (item.getItemId() == R.id.nav_adopta) {
                    loadFragment(new AdoptaFragment());
                    return true;
                } else if (item.getItemId() == R.id.nav_foro) {
                    loadFragment(new ForoFragment());
                    return true;
                } else if (item.getItemId() == R.id.nav_contacto) {
                    loadFragment(new ContactoFragment());
                    return true;
                }
                return false;
            }
        });

        // Fragmento por defecto
        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }
    }

    // Método para cargar los fragmentos
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commit();
    }

    // Método para el menu de inicio de sesion y registro
    private void showLoginDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_login, null);

        EditText nameEditText = dialogView.findViewById(R.id.nameEditText);
        EditText emailEditText = dialogView.findViewById(R.id.emailEditText);
        EditText passwordEditText = dialogView.findViewById(R.id.passwordEditText);
        TextView toggleTextView = dialogView.findViewById(R.id.toggleTextView);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        final boolean[] isLoginMode = {true}; // Estado del modo actual

        builder.setPositiveButton("Entrar", null); // Asignaremos manualmente luego
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.setTitle("Iniciar Sesión");

        dialog.setOnShowListener(dialogInterface -> {
            // Obtener botón Entrar/Registrar
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

            // Acción al pulsar el botón principal
            positiveButton.setOnClickListener(v -> {
                String email = emailEditText.getText().toString();
                String password = passwordEditText.getText().toString();

                if (isLoginMode[0]) {
                    // Lógica de login
                    Toast.makeText(this, "Iniciando sesión con " + email, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    String name = nameEditText.getText().toString();
                    // Lógica de registro
                    Toast.makeText(this, "Registrando: " + name + ", " + email, Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }
            });

            // Alternar entre login y registro
            toggleTextView.setOnClickListener(v -> {
                isLoginMode[0] = !isLoginMode[0];
                if (isLoginMode[0]) {
                    nameEditText.setVisibility(View.GONE);
                    dialog.setTitle("Iniciar Sesión");
                    positiveButton.setText("Entrar");
                    toggleTextView.setText("¿No tienes cuenta? Regístrate");
                } else {
                    nameEditText.setVisibility(View.VISIBLE);
                    dialog.setTitle("Registro");
                    positiveButton.setText("Registrarse");
                    toggleTextView.setText("¿Ya tienes cuenta? Inicia sesión");
                }
            });
        });

        dialog.show();
    }

}
