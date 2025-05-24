package com.example.adoptatupet.views.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.adoptatupet.R;
import com.example.adoptatupet.models.Mensaje;
import com.example.adoptatupet.models.Usuario;
import com.example.adoptatupet.network.ApiClient;
import com.example.adoptatupet.network.ApiService;
import com.example.adoptatupet.views.MainActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Collections;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * PerfilFragment: muestra y permite editar los datos del usuario.
 * - Localidad nunca desaparece: muestra "Sin localidad" si está vacía.
 * - Tras guardar, todos los botones "Editar" siguen visibles.
 */
public class PerfilFragment extends Fragment {

    private ImageView imageViewPerfil;
    private TextView  tvNombre, tvEmail, tvPassword, tvLocalidad;
    private EditText  editTextNombre, editTextEmail, editTextPassword, editTextLocalidad;
    private Button    btnGuardar, btnCambiarFoto;
    private Button    btnEditarNombre, btnEditarEmail, btnEditarPassword, btnEditarLocalidad;
    private Button    btnAñadirAnimal, btnEliminarUsuario, btnBorrarCuenta;

    private String imagenBase64 = null;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == requireActivity().RESULT_OK &&
                                result.getData() != null) {

                            Uri uri = result.getData().getData();
                            imageViewPerfil.setImageURI(uri);
                            convertirImagenABase64(uri);
                            guardarFotoDirecta();
                        }
                    }
            );

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        // Referencias a vistas
        imageViewPerfil    = view.findViewById(R.id.imageViewPerfil);
        tvNombre           = view.findViewById(R.id.tvNombre);
        tvEmail            = view.findViewById(R.id.tvEmail);
        tvPassword         = view.findViewById(R.id.tvPassword);
        tvLocalidad        = view.findViewById(R.id.tvLocalidad);
        editTextNombre     = view.findViewById(R.id.editTextNombre);
        editTextEmail      = view.findViewById(R.id.editTextEmail);
        editTextPassword   = view.findViewById(R.id.editTextPassword);
        editTextLocalidad  = view.findViewById(R.id.editTextLocalidad);
        btnGuardar         = view.findViewById(R.id.btnGuardarCambios);
        btnCambiarFoto     = view.findViewById(R.id.btnCambiarFoto);
        btnEditarNombre    = view.findViewById(R.id.btnEditarNombre);
        btnEditarEmail     = view.findViewById(R.id.btnEditarEmail);
        btnEditarPassword  = view.findViewById(R.id.btnEditarPassword);
        btnEditarLocalidad = view.findViewById(R.id.btnEditarLocalidad);
        btnAñadirAnimal    = view.findViewById(R.id.btnAñadirAnimal);
        btnEliminarUsuario = view.findViewById(R.id.btnEliminarUsuario);
        btnBorrarCuenta    = view.findViewById(R.id.btnBorrarCuenta);

        // Carga inicial de datos
        cargarDatosUsuario();

        // Listeners de edición
        btnCambiarFoto.setOnClickListener(v -> seleccionarImagen());
        btnEditarNombre   .setOnClickListener(v -> toggleEditCancel(tvNombre,    editTextNombre,    btnEditarNombre));
        btnEditarEmail    .setOnClickListener(v -> toggleEditCancel(tvEmail,     editTextEmail,     btnEditarEmail));
        btnEditarPassword .setOnClickListener(v -> toggleEditCancel(tvPassword,  editTextPassword,  btnEditarPassword));
        btnEditarLocalidad.setOnClickListener(v -> toggleEditCancel(tvLocalidad, editTextLocalidad, btnEditarLocalidad));
        btnGuardar        .setOnClickListener(v -> guardarCambios());

        // Botones admin vs usuario normal
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user", Context.MODE_PRIVATE);
        String email = prefs.getString("email","");

        if ("admin@gmail.com".equalsIgnoreCase(email)) {
            btnAñadirAnimal.setVisibility(View.VISIBLE);
            btnEliminarUsuario.setVisibility(View.VISIBLE);
            btnAñadirAnimal.setOnClickListener(v ->
                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.nav_host_fragment, new AddAnimalFragment())
                            .addToBackStack(null)
                            .commit()
            );
            btnEliminarUsuario.setOnClickListener(v ->
                    requireActivity().getSupportFragmentManager().beginTransaction()
                            .replace(R.id.nav_host_fragment, new DeleteUserFragment())
                            .addToBackStack(null)
                            .commit()
            );
        } else {
            btnAñadirAnimal.setVisibility(View.GONE);
            btnEliminarUsuario.setVisibility(View.GONE);
        }

        // Borrar cuenta (todos los usuarios)
        btnBorrarCuenta.setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Eliminar cuenta")
                        .setMessage("¿Estás seguro? Acción irreversible.")
                        .setPositiveButton("Aceptar", (d,w) -> borrarCuenta())
                        .setNegativeButton("Cancelar", null)
                        .show()
        );

        return view;
    }

    /** Alterna entre modo ver (TextView) y editar (EditText). */
    private void toggleEditCancel(TextView tv, EditText et, Button btn) {
        if (et.getVisibility() == View.GONE) {
            // Entrar en edición
            tv.setVisibility(View.GONE);
            et.setVisibility(View.VISIBLE);
            et.setText(tv.getText().toString().replaceFirst("^[^:]+: ?", ""));
            btn.setText("Cancelar");
            btnGuardar.setVisibility(View.VISIBLE);
        } else {
            // Cancelar edición
            et.setVisibility(View.GONE);
            tv.setVisibility(View.VISIBLE);
            btn.setText("Editar");
            // Si ningún campo está en edición, ocultar Guardar
            if (editTextNombre.getVisibility()==View.GONE &&
                    editTextEmail.getVisibility()==View.GONE &&
                    editTextPassword.getVisibility()==View.GONE &&
                    editTextLocalidad.getVisibility()==View.GONE) {
                btnGuardar.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Carga datos desde SharedPreferences y actualiza la UI:
     * - Nombre, Email, Contraseña, Localidad (o "Sin localidad")
     * - Foto de perfil
     * - Oculta todos los EditText y el botón Guardar
     */
    public void cargarDatosUsuario() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user", Context.MODE_PRIVATE);

        // Nombre
        String nombre = prefs.getString("usuario","");
        tvNombre.setText("Nombre: " + nombre);
        editTextNombre.setText(nombre);
        tvNombre.setVisibility(View.VISIBLE);
        btnEditarNombre.setVisibility(View.VISIBLE);

        // Email
        String mail = prefs.getString("email","");
        tvEmail.setText("Email: " + mail);
        editTextEmail.setText(mail);
        tvEmail.setVisibility(View.VISIBLE);
        btnEditarEmail.setVisibility(View.VISIBLE);

        // Contraseña (siempre oculta)
        tvPassword.setText("Contraseña: ********");
        editTextPassword.setText(prefs.getString("contrasena",""));
        tvPassword.setVisibility(View.VISIBLE);
        btnEditarPassword.setVisibility(View.VISIBLE);

        // Localidad (si está vacía, mostramos "Sin localidad")
        String loc = prefs.getString("localidad","");
        if (loc.isEmpty()) loc = "Sin localidad";  // ** siempre hay texto
        tvLocalidad.setText("Localidad: " + loc);
        editTextLocalidad.setText(loc);
        tvLocalidad.setVisibility(View.VISIBLE);
        btnEditarLocalidad.setVisibility(View.VISIBLE);

        // Foto de perfil
        String b64 = prefs.getString("fotoPerfil", null);
        if (b64 != null && !b64.isEmpty()) {
            try {
                byte[] data = Base64.decode(b64, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length);
                imageViewPerfil.setImageBitmap(bmp);
                imagenBase64 = b64;
            } catch (Exception e) {
                imageViewPerfil.setImageResource(R.drawable.default_avatar);
                imagenBase64 = null;
            }
        } else {
            imageViewPerfil.setImageResource(R.drawable.default_avatar);
            imagenBase64 = null;
        }

        // Ocultar todos los EditText y botón Guardar
        editTextNombre.setVisibility(View.GONE);
        editTextEmail.setVisibility(View.GONE);
        editTextPassword.setVisibility(View.GONE);
        editTextLocalidad.setVisibility(View.GONE);
        btnGuardar.setVisibility(View.GONE);

        // Reset textos de botones
        btnEditarNombre.setText("Editar");
        btnEditarEmail.setText("Editar");
        btnEditarPassword.setText("Editar");
        btnEditarLocalidad.setText("Editar");
    }

    /** Abre la galería para seleccionar una nueva foto. */
    private void seleccionarImagen() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    /** Convierte URI de imagen a Base64 (NO_WRAP). */
    private void convertirImagenABase64(Uri uri) {
        try {
            Bitmap bmp = MediaStore.Images.Media.getBitmap(
                    requireActivity().getContentResolver(), uri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, baos);
            imagenBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
        } catch (IOException e) {
            Toast.makeText(getContext(),
                    "Error al cargar imagen", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Envía al servidor los cambios de perfil y, si tienen éxito,
     * los guarda en prefs y recarga la UI (de nuevo incluyendo localidad).
     */
    private void guardarCambios() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user", Context.MODE_PRIVATE);
        int idUsuario = prefs.getInt("idUsuario", -1);
        if (idUsuario == -1) {
            Toast.makeText(getContext(),
                    "Sesión no válida", Toast.LENGTH_SHORT).show();
            return;
        }

        // Recoger valores editados o los guardados en prefs
        String nombre = editTextNombre.getVisibility()==View.VISIBLE
                ? editTextNombre.getText().toString().trim()
                : prefs.getString("usuario","");
        String mail = editTextEmail.getVisibility()==View.VISIBLE
                ? editTextEmail.getText().toString().trim()
                : prefs.getString("email","");
        String pass = editTextPassword.getVisibility()==View.VISIBLE
                ? editTextPassword.getText().toString().trim()
                : prefs.getString("contrasena","");
        String loc  = editTextLocalidad.getVisibility()==View.VISIBLE
                ? editTextLocalidad.getText().toString().trim()
                : prefs.getString("localidad","");

        if (nombre.isEmpty()|| mail.isEmpty()|| pass.isEmpty()|| loc.isEmpty()) {
            Toast.makeText(getContext(),
                    "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        // Construir objeto Usuario
        Usuario u = new Usuario();
        u.setIdUsuario(idUsuario);
        u.setUsuario(nombre);
        u.setEmail(mail);
        u.setContrasena(pass);
        u.setLocalidad(loc);
        u.setFotoPerfil(imagenBase64);

        // Llamada al API
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.updateUsuario(u).enqueue(new Callback<Mensaje>() {
            @Override public void onResponse(Call<Mensaje> call, Response<Mensaje> r) {
                if (r.isSuccessful() && r.body()!=null && r.body().isSuccess()) {
                    // Guardar TODO en prefs
                    prefs.edit()
                            .putString("usuario",   nombre)
                            .putString("email",     mail)
                            .putString("contrasena",pass)
                            .putString("localidad", loc)
                            .putString("fotoPerfil",imagenBase64.replaceAll("\\s+",""))
                            .apply();

                    Toast.makeText(getContext(),
                            "Datos actualizados", Toast.LENGTH_SHORT).show();

                    // Recargar la UI (localidad siempre visible)
                    cargarDatosUsuario();
                } else {
                    Toast.makeText(getContext(),
                            "Error al actualizar", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Mensaje> call, Throwable t) {
                Toast.makeText(getContext(),
                        "Fallo en servidor: "+t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Borra la cuenta y lleva al HomeFragment. */
    private void borrarCuenta() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user", Context.MODE_PRIVATE);
        String email = prefs.getString("email","");

        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.deleteUserEmail(Collections.singletonMap("email",email))
                .enqueue(new Callback<Mensaje>() {
                    @Override public void onResponse(Call<Mensaje> call, Response<Mensaje> r) {
                        if (r.isSuccessful() && r.body()!=null && r.body().isSuccess()) {
                            prefs.edit().clear().apply();
                            Toast.makeText(getContext(),
                                    "Cuenta eliminada", Toast.LENGTH_SHORT).show();
                            requireActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .replace(R.id.nav_host_fragment, new HomeFragment())
                                    .commit();
                        } else {
                            String msg = r.body()!=null
                                    ? r.body().getMessage()
                                    : "Error al borrar cuenta";
                            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onFailure(Call<Mensaje> call, Throwable t) {
                        Toast.makeText(getContext(),
                                "Error de red: "+t.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    /** Envía inmediatamente la nueva foto al servidor y actualiza el drawer. */
    private void guardarFotoDirecta() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user", Context.MODE_PRIVATE);
        int idUsuario = prefs.getInt("idUsuario", -1);
        if (idUsuario == -1) return;

        Usuario u = new Usuario();
        u.setIdUsuario(idUsuario);
        u.setUsuario(prefs.getString("usuario",""));
        u.setEmail(prefs.getString("email",""));
        u.setContrasena(prefs.getString("contrasena",""));
        u.setLocalidad(prefs.getString("localidad",""));
        u.setFotoPerfil(imagenBase64);

        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.updateUsuario(u).enqueue(new Callback<Mensaje>() {
            @Override public void onResponse(Call<Mensaje> call, Response<Mensaje> r) {
                if (r.isSuccessful() && r.body()!=null && r.body().isSuccess()) {
                    prefs.edit().putString("fotoPerfil", imagenBase64).apply();
                    // Actualizar imagen en el drawer inmediatamente:
                    ((MainActivity) requireActivity())
                            .actualizarFotoDrawer(imagenBase64);
                    Toast.makeText(getContext(),
                            "Foto actualizada", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Mensaje> call, Throwable t) { }
        });
    }
}
