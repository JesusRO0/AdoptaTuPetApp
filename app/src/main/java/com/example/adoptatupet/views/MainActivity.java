package com.example.adoptatupet.views;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
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

public class MainActivity extends AppCompatActivity {

    private DrawerLayout      drawerLayout;
    private NavigationView    navigationView;
    private Toolbar           topAppBar;
    private TextView          navUserName;
    private ImageView         navUserImage;
    private com.example.adoptatupet.controllers.usuarioController usuarioController;

    // ProgressDialog para mostrar rueda de carga
    private ProgressDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Instancia singleton del controller de usuario
        usuarioController = com.example.adoptatupet.controllers.usuarioController.getInstance(this);

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

        // Header del drawer
        View headerView = navigationView.getHeaderView(0);
        navUserName  = headerView.findViewById(R.id.nav_user_name);
        navUserImage = headerView.findViewById(R.id.nav_user_image);

        // Listener menú lateral usando if en lugar de switch
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            drawerLayout.closeDrawers();

            if (id == R.id.nav_login) {
                showLoginDialog(); return true;
            }
            if (id == R.id.nav_profile) {
                loadFragment(new PerfilFragment()); return true;
            }
            if (id == R.id.nav_logout) {
                cerrarSesion(); return true;
            }
            if (id == R.id.nav_exit) {
                finishAffinity(); return true;
            }
            return false;
        });

        // Navegación inferior
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            Fragment destino;
            if (item.getItemId() == R.id.nav_home) {
                destino = new HomeFragment();
            } else if (item.getItemId() == R.id.nav_adopta) {
                destino = new AdoptaFragment();
            } else if (item.getItemId() == R.id.nav_foro) {
                destino = new ForoFragment();
            } else if (item.getItemId() == R.id.nav_contacto) {
                destino = new ContactoFragment();
            } else {
                return false;
            }
            loadFragment(destino);
            return true;
        });

        // Selección inicial
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }

        // Carga usuario en header
        cargarUsuarioYActualizarUI();
    }

    /** Reemplaza el frame con el fragmento dado. */
    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commit();
    }

    /**
     * Muestra un diálogo de login/registro con rueda de carga.
     */
    private void showLoginDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_login, null);
        EditText nameET  = dialogView.findViewById(R.id.nameEditText);
        EditText emailET = dialogView.findViewById(R.id.emailEditText);
        EditText passET  = dialogView.findViewById(R.id.passwordEditText);
        TextView toggle  = dialogView.findViewById(R.id.toggleTextView);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setPositiveButton("Entrar", null)
                .setNegativeButton("Cancelar", (d, w) -> d.dismiss())
                .create();
        dialog.setTitle("Iniciar Sesión");

        dialog.setOnShowListener(dlg -> {
            Button btnOK = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnOK.setOnClickListener(v -> {
                String email = emailET.getText().toString().trim();
                String pass  = passET.getText().toString().trim();

                if (nameET.getVisibility() == View.GONE) {
                    // —— LOGIN ——
                    showLoading();
                    usuarioController.login(email, pass, new retrofit2.Callback<Mensaje>() {
                        @Override
                        public void onResponse(Call<Mensaje> call, Response<Mensaje> resp) {
                            hideLoading();
                            if (resp.isSuccessful() && resp.body()!=null && resp.body().isSuccess()) {
                                Usuario u = resp.body().getUsuario();
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
                            hideLoading();
                            Toast.makeText(MainActivity.this,
                                    "Error en servidor",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // —— REGISTRO ——
                    String nombre = nameET.getText().toString().trim();
                    if (nombre.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                        Toast.makeText(MainActivity.this,
                                "Completa todos los campos",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showLoading();
                    usuarioController.register(
                            email, nombre, "Sin localidad", pass,
                            new retrofit2.Callback<Mensaje>() {
                                @Override
                                public void onResponse(Call<Mensaje> call, Response<Mensaje> resp) {
                                    hideLoading();
                                    if (resp.isSuccessful() && resp.body()!=null && resp.body().isSuccess()) {
                                        Toast.makeText(MainActivity.this,
                                                "Registro exitoso, inicie sesión",
                                                Toast.LENGTH_SHORT).show();
                                        nameET.setVisibility(View.GONE);
                                        btnOK.setText("Entrar");
                                        dialog.setTitle("Iniciar Sesión");
                                        toggle.setText("¿No tienes cuenta? Regístrate");
                                    } else {
                                        Toast.makeText(MainActivity.this,
                                                resp.body()!=null?resp.body().getMessage():"Error en registro",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                }
                                @Override
                                public void onFailure(Call<Mensaje> call, Throwable t) {
                                    hideLoading();
                                    Toast.makeText(MainActivity.this,
                                            "Error en servidor durante registro",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                }
            });

            // Alternar Login / Registro
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

        dialog.show();
    }

    /** Muestra la rueda de carga. */
    public void showLoading() {
        if (loadingDialog == null) {
            loadingDialog = new ProgressDialog(this);
            loadingDialog.setMessage("Cargando...");
            loadingDialog.setCancelable(false);
        }
        loadingDialog.show();
    }

    /** Oculta la rueda de carga. */
    public void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    /** Actualiza visibilidad de opciones del drawer. */
    private void actualizarOpcionesMenu(boolean loggedIn) {
        Menu menu = navigationView.getMenu();
        menu.findItem(R.id.nav_profile).setVisible(loggedIn);
        menu.findItem(R.id.nav_logout).setVisible(loggedIn);
        menu.findItem(R.id.nav_login).setVisible(!loggedIn);
        navigationView.invalidate();
        for (int i = 0; i < menu.size(); i++) {
            MenuItem mi = menu.getItem(i);
            Log.d("DEBUG_MENU", "Item " + mi.getTitle() + " visible=" + mi.isVisible());
        }
    }

    /** Cierra sesión y vuelve a HomeFragment. */
    private void cerrarSesion() {
        usuarioController.logout();
        navigationView.getMenu().clear();
        navigationView.inflateMenu(R.menu.drawer_menu);
        actualizarOpcionesMenu(false);
        navUserName.setText("¡Bienvenido!");
        navUserImage.setImageResource(R.drawable.default_avatar);
        loadFragment(new HomeFragment());
        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();
        drawerLayout.closeDrawers();
    }

    /** Actualiza avatar en el drawer a partir de Base64. */
    public void actualizarFotoDrawer(String base64Foto) {
        View header = navigationView.getHeaderView(0);
        ImageView avatar = header.findViewById(R.id.nav_user_image);
        if (base64Foto == null || base64Foto.isEmpty()) {
            avatar.setImageResource(R.drawable.default_avatar);
            return;
        }
        if (base64Foto.contains(",")) {
            base64Foto = base64Foto.substring(base64Foto.indexOf(",")+1);
        }
        base64Foto = base64Foto.replaceAll("\\s+","");
        byte[] bytes = Base64.decode(base64Foto, Base64.NO_WRAP);
        Bitmap bmp = BitmapFactory.decodeByteArray(bytes,0,bytes.length);
        avatar.setImageBitmap(bmp!=null?bmp:BitmapFactory.decodeResource(getResources(), R.drawable.default_avatar));
    }

    /** Carga usuario de prefs y actualiza header y menú. */
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
        cargarUsuarioYActualizarUI();
    }
}
