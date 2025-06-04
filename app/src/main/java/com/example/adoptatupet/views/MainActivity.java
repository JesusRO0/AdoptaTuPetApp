package com.example.adoptatupet.views;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.adoptatupet.R;
import com.example.adoptatupet.controllers.animalController;
import com.example.adoptatupet.controllers.usuarioController;
import com.example.adoptatupet.models.Usuario;
import com.example.adoptatupet.views.LoginActivity;
import com.example.adoptatupet.views.fragments.AdoptaFragment;
import com.example.adoptatupet.views.fragments.ContactoFragment;
import com.example.adoptatupet.views.fragments.ForoFragment;
import com.example.adoptatupet.views.fragments.HomeFragment;
import com.example.adoptatupet.views.fragments.PerfilFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private Toolbar topAppBar;
    private de.hdodenhof.circleimageview.CircleImageView toolbarProfileImage;
    private TextView toolbarTitle;
    private usuarioController usuarioController;
    private ProgressDialog loadingDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // ——— Instancia del controller ANTES de cualquier UI ———
        usuarioController = usuarioController.getInstance(this);

        // Si no hay usuario en prefs, saltar a LoginActivity y limpiar la pila
        if (usuarioController.loadFromPrefs() == null) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return; // importante para que el resto no se ejecute
        }

        // Ahora sí: inflar el layout principal
        setContentView(R.layout.activity_main);

        // Forzar color de status bar
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimary));

        // Referencias del Toolbar
        topAppBar = findViewById(R.id.topAppBar);
        toolbarTitle = topAppBar.findViewById(R.id.toolbar_title);
        toolbarProfileImage = topAppBar.findViewById(R.id.toolbar_profile_image);
        setSupportActionBar(topAppBar);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        // Click en avatar: si ya había usuario (siempre que llegaste aquí),
        // abre Perfil; si en algún momento se desloguea, cerrarSesion()
        toolbarProfileImage.setOnClickListener(v -> {
            Usuario u = usuarioController.loadFromPrefs();
            if (u == null) {
                cerrarSesion();
            } else {
                loadFragment(new PerfilFragment());
            }
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

        // Precarga de animales en caché
        animalController.getInstance(this).fetchAllAnimals(null);
    }

    /**
     * Cambié este método de `private` a `public` para que pueda usarse desde otros fragmentos.
     * Reemplaza el contenedor principal con el fragmento indicado
     */
    public void loadFragment(@NonNull Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.nav_host_fragment, fragment)
                .commit();
    }

    /**
     * Cierra la sesión del usuario y vuelve a LoginActivity.
     */
    public void cerrarSesion() {
        usuarioController.logout();

        // Reinicia en LoginActivity y limpia la pila de actividades
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    /** Actualiza la imagen de perfil en el toolbar a partir de Base64 */
    public void actualizarFotoDrawer(String base64Foto) {
        if (base64Foto == null || base64Foto.isEmpty()) {
            toolbarProfileImage.setImageResource(R.drawable.default_avatar);
            return;
        }
        if (base64Foto.contains(",")) {
            base64Foto = base64Foto.substring(base64Foto.indexOf(",") + 1);
        }
        base64Foto = base64Foto.replaceAll("\\s+", "");
        byte[] bytes = Base64.decode(base64Foto, Base64.NO_WRAP);
        Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        toolbarProfileImage.setImageBitmap(
                bmp != null
                        ? bmp
                        : BitmapFactory.decodeResource(getResources(), R.drawable.default_avatar)
        );
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

    /** Oculta el ProgressDialog de carga */
    public void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing()) {
            loadingDialog.dismiss();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Si en algún momento vuelves y no hay usuario, cierras sesión
        if (usuarioController.loadFromPrefs() == null) {
            cerrarSesion();
            return;
        }
        // Actualiza avatar
        actualizarFotoDrawer(usuarioController.loadFromPrefs().getFotoPerfil());
    }
}
