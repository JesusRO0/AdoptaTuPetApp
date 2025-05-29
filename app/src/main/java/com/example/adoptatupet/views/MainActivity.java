package com.example.adoptatupet.views;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.adoptatupet.R;
import com.example.adoptatupet.controllers.animalController;
import com.example.adoptatupet.controllers.usuarioController;
import com.example.adoptatupet.models.Mensaje;
import com.example.adoptatupet.models.Usuario;
import com.example.adoptatupet.views.fragments.AdoptaFragment;
import com.example.adoptatupet.views.fragments.ContactoFragment;
import com.example.adoptatupet.views.fragments.ForoFragment;
import com.example.adoptatupet.views.fragments.HomeFragment;
import com.example.adoptatupet.views.fragments.PerfilFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import retrofit2.Call;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private Toolbar topAppBar;
    private ImageView toolbarProfileImage;
    private TextView toolbarTitle;
    private usuarioController usuarioController;
    private ProgressDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Aplica layout /activity_main que usa toolbar_main para el AppBar
        setContentView(R.layout.activity_main);

        // ——— Forzar color de status bar igual que colorPrimary ———
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));
        // ————————————————————————————————————————————————

        // Instancia singleton del controller de usuario
        usuarioController = usuarioController.getInstance(this);

        // Referencias del Toolbar
        topAppBar = findViewById(R.id.topAppBar);
        toolbarTitle = topAppBar.findViewById(R.id.toolbar_title);
        toolbarProfileImage = topAppBar.findViewById(R.id.toolbar_profile_image);
        setSupportActionBar(topAppBar);
        // Quita el título por defecto para usar el TextView custom
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Cuando pulsen la imagen de perfil:
        toolbarProfileImage.setOnClickListener(v -> {
            Usuario u = usuarioController.loadFromPrefs();
            if (u == null) {
                // Si no hay usuario logueado, abrimos diálogo de login/registro
                showLoginDialog();
            } else {
                // Si ya está logueado, cargamos su perfil
                loadFragment(new PerfilFragment());
            }
        });

        // Configuramos la navegación inferior
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

        // Selección inicial de pestaña
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }

        // Precarga de animales en caché
        animalController.getInstance(this)
                .fetchAllAnimals(null);
    }

    /** Reemplaza el contenedor principal con el fragmento indicado */
    private void loadFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commit();
    }

    /** Diálogo para login / registro */
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
            // Botón “Entrar” / “Registrar”
            View btnOK = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            btnOK.setOnClickListener(v -> {
                String email = emailET.getText().toString().trim();
                String pass  = passET.getText().toString().trim();

                if (nameET.getVisibility() == View.GONE) {
                    // —— LOGIN ——
                    if (email.isEmpty() || pass.isEmpty()) {
                        Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showLoading();
                    usuarioController.login(email, pass, new retrofit2.Callback<Mensaje>() {
                        @Override
                        public void onResponse(Call<Mensaje> call, Response<Mensaje> resp) {
                            hideLoading();
                            if (resp.isSuccessful() && resp.body()!=null && resp.body().isSuccess()) {
                                Usuario u = resp.body().getUsuario();
                                // Actualiza imagen y título
                                toolbarTitle.setText("¡Bienvenido " + u.getUsuario() + "!");
                                actualizarPerfilImage(u.getFotoPerfil());
                                dialog.dismiss();
                            } else {
                                Toast.makeText(MainActivity.this, "Credenciales inválidas", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<Mensaje> call, Throwable t) {
                            hideLoading();
                            Toast.makeText(MainActivity.this, "Error en servidor", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // —— REGISTRO ——
                    String nombre     = nameET.getText().toString().trim();
                    String emailReg    = emailET.getText().toString().trim();
                    String passwordReg= passET.getText().toString().trim();
                    if (nombre.isEmpty() || emailReg.isEmpty() || passwordReg.isEmpty()) {
                        Toast.makeText(MainActivity.this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    showLoading();
                    usuarioController.register(
                            emailReg, nombre, "Sin localidad", passwordReg,
                            new retrofit2.Callback<Mensaje>() {
                                @Override
                                public void onResponse(Call<Mensaje> call, Response<Mensaje> resp) {
                                    hideLoading();
                                    if (resp.isSuccessful() && resp.body()!=null && resp.body().isSuccess()) {
                                        Toast.makeText(MainActivity.this,
                                                "Registro exitoso, inicie sesión",
                                                Toast.LENGTH_SHORT).show();
                                        nameET.setVisibility(View.GONE);
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
                            }
                    );
                }

            });

            // Alternar entre login y registro
            toggle.setOnClickListener(v -> {
                if (nameET.getVisibility() == View.GONE) {
                    nameET.setVisibility(View.VISIBLE);
                    dialog.setTitle("Registro");
                    toggle.setText("¿Ya tienes cuenta? Inicia sesión");
                } else {
                    nameET.setVisibility(View.GONE);
                    dialog.setTitle("Iniciar Sesión");
                    toggle.setText("¿No tienes cuenta? Regístrate");
                }
            });
        });

        dialog.show();
    }

    /** Muestra el ProgressDialog de carga */
    public void showLoading() {
        if (loadingDialog == null) {
            loadingDialog = new ProgressDialog(this);
            loadingDialog.setMessage("Cargando...");
            loadingDialog.setCancelable(false);
        }
        loadingDialog.show();
    }

    /** Oculta el ProgressDialog */
    public void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    /**
     * Cierra la sesión del usuario y vuelve al HomeFragment.
     * Desde PerfilFragment invocan ((MainActivity) requireActivity()).cerrarSesion();
     */
    public void cerrarSesion() {
        // Limpia datos de sesión
        usuarioController.logout();
        // Restablece toolbar
        toolbarTitle.setText("AdoptaTuPet");
        toolbarProfileImage.setImageResource(R.drawable.default_avatar);
        // Vuelve a la pestaña Home
        loadFragment(new HomeFragment());
    }

    /** Actualiza la imagen de perfil del toolbar a partir de Base64 */
    private void actualizarPerfilImage(String base64Foto) {
        if (base64Foto == null || base64Foto.isEmpty()) {
            toolbarProfileImage.setImageResource(R.drawable.default_avatar);
            return;
        }
        // Decodifica y ajusta
        if (base64Foto.contains(",")) {
            base64Foto = base64Foto.substring(base64Foto.indexOf(",") + 1);
        }
        base64Foto = base64Foto.replaceAll("\\s+", "");
        byte[] bytes = Base64.decode(base64Foto, Base64.NO_WRAP);
        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        toolbarProfileImage.setImageBitmap(bmp != null
                ? bmp
                : BitmapFactory.decodeResource(getResources(), R.drawable.default_avatar));
    }

    /**
     * Método público que permite a PerfilFragment llamar
     * a la actualización de la foto desde el toolbar.
     */
    public void actualizarFotoDrawer(String base64Foto) {
        actualizarPerfilImage(base64Foto);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Al volver, refresca datos del usuario en toolbar
        Usuario u = usuarioController.loadFromPrefs();
        toolbarTitle.setText("AdoptaTuPet");
        if (u != null) {
            actualizarPerfilImage(u.getFotoPerfil());
        } else {
            toolbarProfileImage.setImageResource(R.drawable.default_avatar);
        }
    }
}
