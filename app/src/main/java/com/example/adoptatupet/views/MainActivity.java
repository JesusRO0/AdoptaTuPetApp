package com.example.adoptatupet.views;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;

import com.example.adoptatupet.R;
import com.example.adoptatupet.controllers.UsuarioController;
import com.example.adoptatupet.models.Mensaje;
import com.example.adoptatupet.models.Usuario;
import com.example.adoptatupet.views.fragments.AdoptaFragment;
import com.example.adoptatupet.views.fragments.ContactoFragment;
import com.example.adoptatupet.views.fragments.ForoFragment;
import com.example.adoptatupet.views.fragments.HomeFragment;
import com.example.adoptatupet.views.fragments.PerfilFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import retrofit2.Call;
import retrofit2.Response;

/**
 * MainActivity: gestiona el menú lateral, el login/logout y la navegación inferior.
 * Toda la lógica de usuario se delega a UsuarioController.
 */
public class MainActivity extends AppCompatActivity {

    private DrawerLayout      drawerLayout;
    private NavigationView    navigationView;
    private Toolbar           topAppBar;
    private TextView          navUserName;
    private ImageView         navUserImage;
    private UsuarioController usuarioController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Instancia singleton del controller de usuario
        usuarioController = UsuarioController.getInstance(this);

        // Configura la barra superior y el drawer
        topAppBar     = findViewById(R.id.topAppBar);
        drawerLayout  = findViewById(R.id.drawer_layout);
        navigationView= findViewById(R.id.nav_view);
        setSupportActionBar(topAppBar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, topAppBar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Obtiene referencias al header (avatar y nombre)
        View headerView = navigationView.getHeaderView(0);
        navUserName  = headerView.findViewById(R.id.nav_user_name);
        navUserImage = headerView.findViewById(R.id.nav_user_image);

        // Listener para clicks en el menú lateral
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_login) {
                showLoginDialog();
            }
            else if (id == R.id.nav_profile) {
                loadFragment(new PerfilFragment());
            }
            else if (id == R.id.nav_logout) {
                cerrarSesion();
                return true;  // ya cerramos el drawer dentro de cerrarSesion()
            }
            else if (id == R.id.nav_exit) {
                finishAffinity();
            }

            drawerLayout.closeDrawers();
            return true;
        });


        // Configura la navegación inferior
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            Fragment destino;
            if (id == R.id.nav_home) {
                destino = new HomeFragment();
            }
            else if (id == R.id.nav_adopta) {
                destino = new AdoptaFragment();
            }
            else if (id == R.id.nav_foro) {
                destino = new ForoFragment();
            }
            else if (id == R.id.nav_contacto) {
                destino = new ContactoFragment();
            }
            else {
                return false;
            }
            loadFragment(destino);
            return true;
        });


        // Selecciona Home por defecto
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }

        // Al iniciar, carga usuario y actualiza UI del drawer
        cargarUsuarioYActualizarUI();
    }

    /**
     * Reemplaza el contenedor de fragmentos con el fragmento dado.
     */
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commit();
    }

    /**
     * Muestra un diálogo para login o registro.
     * Usa UsuarioController para la lógica de red y prefs.
     */
    private void showLoginDialog() {
        // 1) Inflamos el layout personalizado
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_login, null);
        EditText nameET  = dialogView.findViewById(R.id.nameEditText);
        EditText emailET = dialogView.findViewById(R.id.emailEditText);
        EditText passET  = dialogView.findViewById(R.id.passwordEditText);
        TextView toggle  = dialogView.findViewById(R.id.toggleTextView);

        // 2) Construimos el AlertDialog añadiendo ya el botón POSITIVO ("Entrar")
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Entrar", null)              // ← aquí creamos el botón
                .setNegativeButton("Cancelar", (d, w) -> d.dismiss())
                .create();

        dialog.setTitle("Iniciar Sesión");

        // 3) Al mostrarse, sobreescribimos el listener del botón Entrar
        dialog.setOnShowListener(dlg -> {
            Button btnOK = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnOK.setOnClickListener(v -> {
                String email = emailET.getText().toString().trim();
                String pass  = passET.getText().toString().trim();

                if (nameET.getVisibility() == View.GONE) {
                    // ——— MODO LOGIN ———
                    usuarioController.login(email, pass, new retrofit2.Callback<Mensaje>() {
                        @Override
                        public void onResponse(Call<Mensaje> call, Response<Mensaje> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                Usuario u = response.body().getUsuario();
                                // Actualiza el header
                                navUserName.setText("¡Bienvenido " + u.getUsuario() + "!");
                                actualizarFotoDrawer(u.getFotoPerfil());
                                actualizarOpcionesMenu(true);
                                Toast.makeText(MainActivity.this,
                                        "Bienvenido " + u.getUsuario(),
                                        Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            } else {
                                Toast.makeText(MainActivity.this,
                                        "Credenciales inválidas",
                                        Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<Mensaje> call, Throwable t) {
                            Toast.makeText(MainActivity.this,
                                    "Error en servidor",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // ——— MODO REGISTRO ———
                    String nombre = nameET.getText().toString().trim();
                    if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                        Toast.makeText(MainActivity.this,
                                "Completa todos los campos",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    // Llamada al register del controller
                    usuarioController.register(
                            email,
                            nombre,
                            "Sin localidad",  // o pide un campo adicional si lo deseas
                            pass,
                            new retrofit2.Callback<Mensaje>() {
                                @Override
                                public void onResponse(Call<Mensaje> call, Response<Mensaje> resp) {
                                    if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
                                        Toast.makeText(MainActivity.this,
                                                "Registro exitoso, ya puedes iniciar sesión",
                                                Toast.LENGTH_SHORT).show();
                                        // Volver a modo login automáticamente
                                        nameET.setVisibility(View.GONE);
                                        btnOK.setText("Entrar");
                                        dialog.setTitle("Iniciar Sesión");
                                        toggle.setText("¿No tienes cuenta? Regístrate");
                                    } else {
                                        Toast.makeText(MainActivity.this,
                                                resp.body() != null
                                                        ? resp.body().getMessage()
                                                        : "Error en registro",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                                @Override
                                public void onFailure(Call<Mensaje> call, Throwable t) {
                                    Toast.makeText(MainActivity.this,
                                            "Error en servidor durante registro",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                    );
                }
            });

            // 4) Toggle entre Login y Registro
            toggle.setOnClickListener(v -> {
                if (nameET.getVisibility() == View.GONE) {
                    nameET.setVisibility(View.VISIBLE);
                    btnOK.setText("Registrar");
                    dialog.setTitle("Registro");
                    toggle.setText("¿Ya tienes cuenta? Inicia sesión");
                } else {
                    nameET.setVisibility(View.GONE);
                    btnOK.setText("Entrar");
                    dialog.setTitle("Iniciar Sesión");
                    toggle.setText("¿No tienes cuenta? Regístrate");
                }
            });
        });

        // 5) ¡Por fin, lo mostramos!
        dialog.show();
    }


    /**
     * Actualiza visibilidad de items de menú según si está logueado.
     */
    private void actualizarOpcionesMenu(boolean loggedIn) {
        Menu menu = navigationView.getMenu();
        menu.findItem(R.id.nav_profile).setVisible(loggedIn);
        menu.findItem(R.id.nav_logout).setVisible(loggedIn);
        menu.findItem(R.id.nav_login).setVisible(!loggedIn);
        navigationView.invalidate();

        // DEBUG — imprime el estado de cada ítem
        for (int i = 0; i < menu.size(); i++) {
            MenuItem mi = menu.getItem(i);
            Log.d("DEBUG_MENU", "Item " + mi.getTitle() + " visible=" + mi.isVisible());
        }
    }

    /**
     * Cierra la sesión del usuario:
     * 1) Borra prefs
     * 2) Reinflar el menú (vuelve a cargar drawer_menu.xml)
     * 3) Oculta/mostrar items según estado
     * 4) Resetea header
     * 5) Vuelve al HomeFragment
     */
    private void cerrarSesion() {
        // 1) Borra todos los datos de sesión
        usuarioController.logout();

        // 2) Reinflar el menú para forzar que 'Entrar' vuelva a añadirse
        navigationView.getMenu().clear();
        navigationView.inflateMenu(R.menu.drawer_menu);

        // 3) Actualiza visibilidad de items (solo nav_login y nav_exit visible)
        actualizarOpcionesMenu(false);

        // 4) Restablece el header a estado "sin sesión"
        navUserName.setText("¡Bienvenido!");
        navUserImage.setImageResource(R.drawable.default_avatar);

        // 5) Navega al HomeFragment
        loadFragment(new HomeFragment());

        // Mensaje al usuario
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();

        // Cierra el drawer
        drawerLayout.closeDrawers();
    }

    /**
     * Decodifica una cadena Base64 (sin prefijos) y la muestra en el avatar del drawer.
     */
    public void actualizarFotoDrawer(String base64Foto) {
        View header = navigationView.getHeaderView(0);
        ImageView avatar = header.findViewById(R.id.nav_user_image);
        if (base64Foto == null || base64Foto.isEmpty()) {
            avatar.setImageResource(R.drawable.default_avatar);
            return;
        }
        if (base64Foto.contains(",")) {
            base64Foto = base64Foto.substring(base64Foto.indexOf(",") + 1);
        }
        base64Foto = base64Foto.replaceAll("\\s+", "");
        byte[] bytes = android.util.Base64.decode(base64Foto, android.util.Base64.NO_WRAP);
        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (bmp != null) avatar.setImageBitmap(bmp);
        else           avatar.setImageResource(R.drawable.default_avatar);
    }

    /**
     * Carga el usuario desde prefs y actualiza el drawer (nombre + foto).
     */
    public void cargarUsuarioYActualizarUI() {
        Usuario u = usuarioController.loadFromPrefs();
        if (u != null) {
            navUserName.setText("¡Bienvenido " + u.getUsuario() + "!");
            actualizarFotoDrawer(u.getFotoPerfil());
            actualizarOpcionesMenu(true);
        } else {
            navUserName.setText("¡Bienvenido!");
            navUserImage.setImageResource(R.drawable.default_avatar);
            actualizarOpcionesMenu(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresca cada vez que vuelve a primer plano
        cargarUsuarioYActualizarUI();
    }
}
