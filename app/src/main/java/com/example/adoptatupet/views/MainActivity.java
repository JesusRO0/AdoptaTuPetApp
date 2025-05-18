package com.example.adoptatupet.views;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
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
import com.example.adoptatupet.network.ApiClient;
import com.example.adoptatupet.network.ApiService;
import com.example.adoptatupet.views.fragments.AdoptaFragment;
import com.example.adoptatupet.views.fragments.ContactoFragment;
import com.example.adoptatupet.views.fragments.ForoFragment;
import com.example.adoptatupet.views.fragments.HomeFragment;
import com.example.adoptatupet.views.fragments.PerfilFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private Toolbar topAppBar;
    private TextView navUserName;
    private ImageView navUserImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        topAppBar = findViewById(R.id.topAppBar);
        setSupportActionBar(topAppBar);

        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, topAppBar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        View headerView = navigationView.getHeaderView(0);
        navUserName = headerView.findViewById(R.id.nav_user_name);
        navUserImage = headerView.findViewById(R.id.nav_user_image);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_login) {
                showLoginDialog();
            } else if (id == R.id.nav_profile) {
                loadFragment(new PerfilFragment());
            } else if (id == R.id.nav_logout) {
                cerrarSesion();
                return true;
            } else if (id == R.id.nav_exit) {
                finishAffinity();
            }

            drawerLayout.closeDrawers();
            return true;
        });

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
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
        });

        if (savedInstanceState == null) {
            bottomNavigationView.setSelectedItemId(R.id.nav_home);
        }

        // Al iniciar la app, carga usuario y actualiza UI (nombre + foto)
        cargarUsuarioYActualizarUI();
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commit();
    }

    private void showLoginDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_login, null);

        EditText nameEditText = dialogView.findViewById(R.id.nameEditText);
        EditText emailEditText = dialogView.findViewById(R.id.emailEditText);
        EditText passwordEditText = dialogView.findViewById(R.id.passwordEditText);
        TextView toggleTextView = dialogView.findViewById(R.id.toggleTextView);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        final boolean[] isLoginMode = {true};

        builder.setPositiveButton("Entrar", null);
        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.setTitle("Iniciar Sesión");

        dialog.setOnShowListener(dialogInterface -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            ApiService apiService = ApiClient.getClient().create(ApiService.class);

            positiveButton.setOnClickListener(v -> {
                String email = emailEditText.getText().toString().trim();
                String password = passwordEditText.getText().toString().trim();

                if (isLoginMode[0]) {
                    // Modo LOGIN
                    Log.d("LoginEmail", "Email enviado: " + email);

                    Usuario usuario = new Usuario(email, password);

                    apiService.login(usuario).enqueue(new Callback<Mensaje>() {
                        @Override
                        public void onResponse(Call<Mensaje> call, Response<Mensaje> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                Usuario usuario = response.body().getUsuario();

                                String fotoPerfilRaw = usuario.getFotoPerfil();
                                if (fotoPerfilRaw != null) {
                                    Log.d("DEBUG_LOGIN_FULL", "FotoPerfil completa (primeros 500 chars): " + fotoPerfilRaw.substring(0, Math.min(500, fotoPerfilRaw.length())));
                                } else {
                                    Log.d("DEBUG_LOGIN_FULL", "FotoPerfil es null");
                                }

                                // Limpieza y formateo de la foto de perfil base64
                                String fotoPerfil = usuario.getFotoPerfil();
                                if (fotoPerfil != null) {
                                    if (fotoPerfil.contains(",")) {
                                        fotoPerfil = fotoPerfil.substring(fotoPerfil.indexOf(",") + 1);
                                    }
                                    fotoPerfil = fotoPerfil.replaceAll("\\s+", "");
                                    Log.d("FotoPerfilClean", "Longitud fotoPerfil limpia: " + fotoPerfil.length());
                                }

                                // Guardar datos en SharedPreferences
                                SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putBoolean("isLoggedIn", true);
                                editor.putInt("idUsuario", usuario.getIdUsuario());
                                editor.putString("usuario", usuario.getUsuario());
                                editor.putString("localidad", usuario.getLocalidad());
                                editor.putString("fotoPerfil", fotoPerfil); // Guardar la foto limpia
                                editor.putString("email", usuario.getEmail());
                                editor.putString("contrasena", usuario.getContrasena());
                                editor.apply();
                                Log.d("DEBUG_LOGIN", "FotoPerfil guardada en SharedPreferences");

                                // Actualizar UI del drawer
                                navUserName.setText("¡Bienvenido " + usuario.getUsuario() + "!");
                                actualizarFotoDrawer(fotoPerfil);
                                actualizarOpcionesMenu(true);

                                // Actualizar perfil si está visible
                                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
                                if (currentFragment instanceof PerfilFragment) {
                                    ((PerfilFragment) currentFragment).actualizarImagenPerfilDesdePrefs();
                                }

                                // Dentro de onResponse, tras limpiar fotoPerfil:
                                Log.d("DEBUG_LOGIN", "FotoPerfil recibida (limpia), longitud: " + (fotoPerfil != null ? fotoPerfil.length() : "null"));
                                Log.d("DEBUG_LOGIN", "FotoPerfil primeros 30 chars: " + (fotoPerfil != null && fotoPerfil.length() > 30 ? fotoPerfil.substring(0,30) : fotoPerfil));


                                Toast.makeText(MainActivity.this, "Bienvenido " + usuario.getUsuario(), Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            } else {
                                String mensaje = response.body() != null ? response.body().getMessage() : "Respuesta inválida";
                                Toast.makeText(MainActivity.this, mensaje, Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Mensaje> call, Throwable t) {
                            Toast.makeText(MainActivity.this, "Fallo en servidor: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });

                } else {
                    // Modo REGISTRO
                    String nombre = nameEditText.getText().toString().trim();
                    if (nombre.isEmpty()) {
                        Toast.makeText(MainActivity.this, "Por favor ingresa un nombre", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Usuario usuario = new Usuario(email, nombre, "Sin localidad", password);

                    apiService.register(usuario).enqueue(new Callback<Mensaje>() {
                        @Override
                        public void onResponse(Call<Mensaje> call, Response<Mensaje> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                Toast.makeText(MainActivity.this, "Registro exitoso", Toast.LENGTH_SHORT).show();
                                dialog.dismiss();
                            } else {
                                String mensaje = response.body() != null ? response.body().getMessage() : "Respuesta inválida";
                                Toast.makeText(MainActivity.this, mensaje, Toast.LENGTH_SHORT).show();
                            }
                        }

                        @Override
                        public void onFailure(Call<Mensaje> call, Throwable t) {
                            Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });

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


    private void actualizarOpcionesMenu(boolean estaLogueado) {
        Menu menu = navigationView.getMenu();

        menu.findItem(R.id.nav_profile).setVisible(estaLogueado);
        menu.findItem(R.id.nav_logout).setVisible(estaLogueado);
        menu.findItem(R.id.nav_login).setVisible(!estaLogueado);

        navigationView.invalidate();
    }

    private void cerrarSesion() {
        SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
        prefs.edit().clear().apply();

        Menu menu = navigationView.getMenu();
        menu.clear();
        navigationView.inflateMenu(R.menu.drawer_menu);

        actualizarOpcionesMenu(false);

        navUserName.setText("¡Bienvenido!");
        navUserImage.setImageResource(R.drawable.default_avatar);

        getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
        loadFragment(new HomeFragment());

        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();

        drawerLayout.closeDrawers();
    }

    public void actualizarFotoDrawer(String base64Foto) {
        View headerView = navigationView.getHeaderView(0);
        ImageView navUserImage = headerView.findViewById(R.id.nav_user_image);

        Log.d("DEBUG_DRAWER", "actualizarFotoDrawer llamada");
        Log.d("DEBUG_DRAWER", "Base64 recibido (longitud): " +
                (base64Foto != null ? base64Foto.length() : "null"));

        if (base64Foto == null || base64Foto.trim().isEmpty()) {
            navUserImage.setImageResource(R.drawable.default_avatar);
            Log.d("DEBUG_DRAWER", "Cadena vacía o null, pongo avatar por defecto.");
            return;
        }

        // 1) Limpieza: solo si realmente trae un prefijo data:, y
        //    eliminamos posibles saltos de línea o espacios.
        if (base64Foto.contains("base64,")) {
            base64Foto = base64Foto.substring(base64Foto.indexOf("base64,") + 7);
        }
        base64Foto = base64Foto.replaceAll("\\s+", "");

        // 2) Decodificar con NO_WRAP (mantiene la cadena intacta)
        byte[] decodedBytes;
        try {
            decodedBytes = android.util.Base64.decode(base64Foto, android.util.Base64.NO_WRAP);
        } catch (IllegalArgumentException e) {
            Log.e("DEBUG_DRAWER", "Error al decodificar Base64", e);
            navUserImage.setImageResource(R.drawable.default_avatar);
            return;
        }

        Log.d("DEBUG_DRAWER", "Bytes decodificados: " + decodedBytes.length);

        // 3) Imprimimos los primeros 4 bytes en hexadecimal para comprobar el 'magic number'
        if (decodedBytes.length >= 4) {
            StringBuilder magic = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                magic.append(String.format("%02X ", decodedBytes[i]));
            }
            Log.d("DEBUG_DRAWER", "Magic bytes: " + magic.toString().trim());
            // PNG debería empezar: 89 50 4E 47
            // JPEG debería empezar: FF D8 FF E0 (o similar)
        } else {
            Log.e("DEBUG_DRAWER", "decodedBytes demasiado corto para ser imagen válida");
        }

        // 4) Intentamos convertir a Bitmap
        Bitmap bmp = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        if (bmp == null) {
            Log.e("DEBUG_DRAWER", "BitmapFactory.decodeByteArray devolvió null");
            navUserImage.setImageResource(R.drawable.default_avatar);
        } else {
            navUserImage.setImageBitmap(bmp);
            Log.d("DEBUG_DRAWER", "Bitmap cargado y aplicado correctamente.");
        }
    }


    private void cargarUsuarioYActualizarUI() {
        SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);
        actualizarOpcionesMenu(isLoggedIn);

        if (isLoggedIn) {
            String usuario = prefs.getString("usuario", "Usuario");
            String fotoPerfil = prefs.getString("fotoPerfil", null);

            navUserName.setText("¡Bienvenido " + usuario + "!");

            actualizarFotoDrawer(fotoPerfil);

        } else {
            navUserName.setText("¡Bienvenido!");
            navUserImage.setImageResource(R.drawable.default_avatar);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarUsuarioYActualizarUI();
    }
}
