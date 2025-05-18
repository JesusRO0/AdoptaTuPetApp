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

        SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
        boolean isLoggedIn = prefs.getBoolean("isLoggedIn", false);

        if (isLoggedIn) {
            actualizarOpcionesMenu(true);
            String usuario = prefs.getString("usuario", "Usuario");
            navUserName.setText("¡Bienvenido " + usuario + "!");

            // Cargar imagen base64 de SharedPreferences y mostrarla en el drawer
            String base64Foto = prefs.getString("fotoPerfil", null);
            if (base64Foto != null && !base64Foto.isEmpty()) {
                try {
                    byte[] decodedBytes = android.util.Base64.decode(base64Foto, android.util.Base64.DEFAULT);
                    Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                    navUserImage.setImageBitmap(decodedBitmap);
                } catch (Exception e) {
                    navUserImage.setImageResource(R.drawable.default_avatar);
                }
            } else {
                navUserImage.setImageResource(R.drawable.default_avatar);
            }

        } else {
            actualizarOpcionesMenu(false);
            navUserName.setText("¡Bienvenido!");
            navUserImage.setImageResource(R.drawable.default_avatar);
        }
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
                    Log.d("LoginEmail", "Email enviado: " + email);  // Log correcto

                    Usuario usuario = new Usuario(email, password);

                    apiService.login(usuario).enqueue(new Callback<Mensaje>() {
                        @Override
                        public void onResponse(Call<Mensaje> call, Response<Mensaje> response) {
                            if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                                Usuario usuario = response.body().getUsuario();

                                SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
                                SharedPreferences.Editor editor = prefs.edit();
                                editor.putBoolean("isLoggedIn", true);
                                editor.putInt("idUsuario", usuario.getIdUsuario());
                                editor.putString("usuario", usuario.getUsuario());
                                editor.putString("localidad", usuario.getLocalidad());
                                editor.putString("fotoPerfil", usuario.getFotoPerfil()); // fotoBase64 del backend
                                editor.putString("email", usuario.getEmail());
                                editor.putString("contrasena", usuario.getContrasena());
                                editor.apply();

                                navUserName.setText("¡Bienvenido " + usuario.getUsuario() + "!");
                                navUserImage.setImageResource(R.drawable.default_avatar);

                                actualizarOpcionesMenu(true);

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
                    // Registro (sin cambios)
                    String nombre = nameEditText.getText().toString().trim();
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

        // 👇 Fuerza el redibujado del NavigationView
        navigationView.invalidate();
    }

    private void cerrarSesion() {
        navUserName.setText("¡Bienvenido!");
        navUserImage.setImageResource(R.drawable.default_avatar);
        actualizarOpcionesMenu(false);

        // Limpia SharedPreferences (sesión)
        SharedPreferences prefs = getSharedPreferences("user", MODE_PRIVATE);
        prefs.edit().clear().apply();

        // Limpia el back stack para evitar volver al perfil
        getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);

        // Cambia a HomeFragment
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, new HomeFragment())
                .commit();

        Toast.makeText(this, "Sesión cerrada", Toast.LENGTH_SHORT).show();

        // Cierra el drawer
        drawerLayout.closeDrawers();
    }
    public void actualizarFotoDrawer(String base64Foto) {
        if (base64Foto != null && !base64Foto.isEmpty()) {
            try {
                byte[] decodedBytes = android.util.Base64.decode(base64Foto, android.util.Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                navUserImage.setImageBitmap(decodedBitmap);
            } catch (Exception e) {
                navUserImage.setImageResource(R.drawable.default_avatar);
            }
        } else {
            navUserImage.setImageResource(R.drawable.default_avatar);
        }
    }

}
