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
import android.text.TextUtils;
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
 * - Si no hay localidad, muestra "Sin localidad".
 * - Asegura que tras guardar siempre se vea el TextView correspondiente.
 * - Añade ProgressDialog al guardar cambios y al cambiar foto.
 */
public class PerfilFragment extends Fragment {

    private ImageView imageViewPerfil;
    private TextView  tvNombre, tvEmail, tvPassword, tvLocalidad;
    private EditText  editTextNombre, editTextEmail, editTextPassword, editTextLocalidad;
    private Button    btnGuardar, btnCambiarFoto;
    private Button    btnEditarNombre, btnEditarEmail, btnEditarPassword, btnEditarLocalidad;
    private Button    btnAñadirAnimal, btnEliminarUsuario, btnBorrarCuenta;

    // Cadena Base64 de la imagen actual
    private String imagenBase64 = null;

    // Lanzador para seleccionar foto de galería
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == requireActivity().RESULT_OK &&
                                result.getData() != null) {
                            Uri uri = result.getData().getData();
                            imageViewPerfil.setImageURI(uri);
                            convertirImagenABase64(uri);
                            // mostramos la rueda mientras subimos la nueva foto
                            ((MainActivity) requireActivity()).showLoading();
                            guardarFotoDirecta();
                        }
                    }
            );

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup   container,
                             @Nullable Bundle      savedInstanceState) {
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

        // Listeners para editar y guardar
        btnCambiarFoto.setOnClickListener(v -> seleccionarImagen());
        btnEditarNombre.setOnClickListener(v -> toggleEditCancel(tvNombre, editTextNombre, btnEditarNombre));
        btnEditarEmail.setOnClickListener(v -> toggleEditCancel(tvEmail, editTextEmail, btnEditarEmail));
        btnEditarPassword.setOnClickListener(v -> toggleEditCancel(tvPassword, editTextPassword, btnEditarPassword));
        btnEditarLocalidad.setOnClickListener(v -> toggleEditCancel(tvLocalidad, editTextLocalidad, btnEditarLocalidad));
        btnGuardar.setOnClickListener(v -> guardarCambios());

        // Botones de admin vs usuario normal
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user", Context.MODE_PRIVATE);
        String email = prefs.getString("email", "");
        if ("admin@gmail.com".equalsIgnoreCase(email)) {
            btnAñadirAnimal.setVisibility(View.VISIBLE);
            btnEliminarUsuario.setVisibility(View.VISIBLE);
            btnAñadirAnimal.setOnClickListener(v ->
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.nav_host_fragment, new AddAnimalFragment())
                            .addToBackStack(null)
                            .commit()
            );
            btnEliminarUsuario.setOnClickListener(v ->
                    requireActivity().getSupportFragmentManager()
                            .beginTransaction()
                            .replace(R.id.nav_host_fragment, new DeleteUserFragment())
                            .addToBackStack(null)
                            .commit()
            );
        } else {
            btnAñadirAnimal.setVisibility(View.GONE);
            btnEliminarUsuario.setVisibility(View.GONE);
        }

        // referencia a “Cerrar sesión”
        Button btnCerrarSesion = view.findViewById(R.id.btnCerrarSesion);
        btnCerrarSesion.setOnClickListener(v -> {
            // Limpiamos el mismo SharedPreferences que usas al cargar datos
            requireActivity().getSharedPreferences("user", Context.MODE_PRIVATE)
                    .edit().clear().apply();
            // Si el ProgressDialog sigue abierto, lo ocultamos
            ((MainActivity) requireActivity()).hideLoading();
            // 2) Restablecer la imagen de perfil en este fragment
            imageViewPerfil.setImageResource(R.drawable.default_avatar);

            // 3) Si hay botón de “Borrar cuenta” o guardados visibles, ocultarlos
            btnGuardar.setVisibility(View.GONE);
            btnCambiarFoto.setVisibility(View.GONE);
            btnEditarNombre.setVisibility(View.GONE);
            btnEditarEmail.setVisibility(View.GONE);
            btnEditarPassword.setVisibility(View.GONE);
            btnEditarLocalidad.setVisibility(View.GONE);
            btnAñadirAnimal.setVisibility(View.GONE);
            btnEliminarUsuario.setVisibility(View.GONE);
            btnBorrarCuenta.setVisibility(View.GONE);

            // 4) Volver al HomeFragment
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.nav_host_fragment, new HomeFragment())
                    .commit();
        });

        // Borrar cuenta
        btnBorrarCuenta.setOnClickListener(v ->
                new AlertDialog.Builder(requireContext())
                        .setTitle("Eliminar cuenta")
                        .setMessage("¿Estás seguro? Acción irreversible.")
                        .setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface dialog, int which) {
                                borrarCuenta();
                            }
                        })
                        .setNegativeButton("Cancelar", null)
                        .show()
        );

        return view;
    }

    /** Alterna entre ver y editar un campo concreto. */
    private void toggleEditCancel(TextView tv, EditText et, Button btn) {
        if (et.getVisibility() == View.GONE) {
            // Entrar a edición
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
            // Si ningún campo queda en edición, ocultar Guardar
            if (editTextNombre.getVisibility()==View.GONE &&
                    editTextEmail.getVisibility()==View.GONE &&
                    editTextPassword.getVisibility()==View.GONE &&
                    editTextLocalidad.getVisibility()==View.GONE) {
                btnGuardar.setVisibility(View.GONE);
            }
        }
    }

    /**
     * Carga datos de SharedPreferences y actualiza la UI:
     * - Siempre muestra los TextView
     * - Si no hay localidad, pone "Sin localidad"
     */
    public void cargarDatosUsuario() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user", Context.MODE_PRIVATE);

        // Aseguramos que todos los TextView estén visibles
        tvNombre.setVisibility(View.VISIBLE);
        tvEmail.setVisibility(View.VISIBLE);
        tvPassword.setVisibility(View.VISIBLE);
        tvLocalidad.setVisibility(View.VISIBLE);

        // Nombre
        String nombre = prefs.getString("usuario", "");
        tvNombre.setText("Nombre: " + nombre);
        editTextNombre.setText(nombre);

        // Email
        String mail = prefs.getString("email", "");
        tvEmail.setText("Email: " + mail);
        editTextEmail.setText(mail);

        // Contraseña (oculta)
        tvPassword.setText("Contraseña: ********");
        editTextPassword.setText(prefs.getString("contrasena", ""));

        // Localidad
        String loc = prefs.getString("localidad", "");
        if (TextUtils.isEmpty(loc)) loc = "Sin localidad";
        tvLocalidad.setText("Localidad: " + loc);
        editTextLocalidad.setText(prefs.getString("localidad", ""));

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

        // Ocultar campos de edición y botón Guardar
        editTextNombre.setVisibility(View.GONE);
        editTextEmail.setVisibility(View.GONE);
        editTextPassword.setVisibility(View.GONE);
        editTextLocalidad.setVisibility(View.GONE);
        btnGuardar.setVisibility(View.GONE);

        // Reset texto de botones "Editar"
        btnEditarNombre.setText("Editar");
        btnEditarEmail.setText("Editar");
        btnEditarPassword.setText("Editar");
        btnEditarLocalidad.setText("Editar");
    }

    /** Abre la galería para escoger imagen. */
    private void seleccionarImagen() {
        Intent intent = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    /** Convierte URI a Base64 (sin saltos) y guarda en imagenBase64. */
    private void convertirImagenABase64(Uri uri) {
        try {
            Bitmap bmp = MediaStore.Images.Media.getBitmap(
                    requireActivity().getContentResolver(), uri
            );
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, baos);
            imagenBase64 = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP);
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error al cargar imagen", Toast.LENGTH_SHORT).show();
        }
    }

    /** Envía los cambios al servidor mostrando la ProgressDialog de MainActivity. */
    private void guardarCambios() {
        // Muestra la rueda
        ((MainActivity) requireActivity()).showLoading();

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user", Context.MODE_PRIVATE);
        int idUsuario = prefs.getInt("idUsuario", -1);
        if (idUsuario == -1) {
            ((MainActivity) requireActivity()).hideLoading();
            Toast.makeText(getContext(), "Sesión no válida", Toast.LENGTH_SHORT).show();
            return;
        }

        String nombre = editTextNombre.getVisibility()==View.VISIBLE
                ? editTextNombre.getText().toString().trim()
                : prefs.getString("usuario", "");
        String mail = editTextEmail.getVisibility()==View.VISIBLE
                ? editTextEmail.getText().toString().trim()
                : prefs.getString("email", "");
        String pass = editTextPassword.getVisibility()==View.VISIBLE
                ? editTextPassword.getText().toString().trim()
                : prefs.getString("contrasena", "");
        String loc  = editTextLocalidad.getVisibility()==View.VISIBLE
                ? editTextLocalidad.getText().toString().trim()
                : prefs.getString("localidad", "");

        if (nombre.isEmpty() || mail.isEmpty() || pass.isEmpty() || loc.isEmpty()) {
            ((MainActivity) requireActivity()).hideLoading();
            Toast.makeText(getContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        Usuario u = new Usuario();
        u.setIdUsuario(idUsuario);
        u.setUsuario(nombre);
        u.setEmail(mail);
        u.setContrasena(pass);
        u.setLocalidad(loc);
        u.setFotoPerfil(imagenBase64);

        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.updateUsuario(u).enqueue(new Callback<Mensaje>() {
            @Override public void onResponse(Call<Mensaje> c, Response<Mensaje> r) {
                // Oculta la rueda
                ((MainActivity) requireActivity()).hideLoading();
                if (r.isSuccessful() && r.body()!=null && r.body().isSuccess()) {
                    // Actualiza prefs con valores nuevos
                    prefs.edit()
                            .putString("usuario",   nombre)
                            .putString("email",     mail)
                            .putString("contrasena",pass)
                            .putString("localidad", loc)
                            .putString("fotoPerfil",imagenBase64.replaceAll("\\s+",""))
                            .apply();
                    Toast.makeText(getContext(), "Datos actualizados", Toast.LENGTH_SHORT).show();
                    // Recarga UI mostrando siempre los TextView
                    cargarDatosUsuario();
                } else {
                    Toast.makeText(getContext(), "Error al actualizar", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Mensaje> c, Throwable t) {
                ((MainActivity) requireActivity()).hideLoading();
                Toast.makeText(getContext(), "Fallo en servidor: "+t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Envía nueva foto al servidor y actualiza el drawer. */
    private void guardarFotoDirecta() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user", Context.MODE_PRIVATE);
        int idUsuario = prefs.getInt("idUsuario",-1);
        if (idUsuario==-1) {
            ((MainActivity) requireActivity()).hideLoading();
            Toast.makeText(getContext(),"Sesión no válida",Toast.LENGTH_SHORT).show();
            return;
        }
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
                ((MainActivity) requireActivity()).hideLoading();
                if (r.isSuccessful() && r.body()!=null && r.body().isSuccess()) {
                    // Guardamos la foto en prefs y actualizamos drawer
                    prefs.edit().putString("fotoPerfil",imagenBase64).apply();
                    ((MainActivity) requireActivity()).actualizarFotoDrawer(imagenBase64);
                    Toast.makeText(getContext(),"Foto actualizada",Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getContext(),"Error al actualizar foto",Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Mensaje> call, Throwable t) {
                ((MainActivity) requireActivity()).hideLoading();
                Toast.makeText(getContext(),"Fallo en servidor: "+t.getMessage(),Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Llama a delete_user.php y vuelve a HomeFragment. */
    private void borrarCuenta() {
        // mostramos rueda
        ((MainActivity) requireActivity()).showLoading();

        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user", Context.MODE_PRIVATE);
        String email = prefs.getString("email", "");

        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.deleteUserEmail(Collections.singletonMap("email", email))
                .enqueue(new Callback<Mensaje>() {
                    @Override public void onResponse(Call<Mensaje> c, Response<Mensaje> r) {
                        // ocultar rueda
                        ((MainActivity) requireActivity()).hideLoading();

                        if (r.isSuccessful() && r.body()!=null && r.body().isSuccess()) {
                            // en lugar de limpiar y navegar manualmente, llamamos cerrarSesion()
                            ((MainActivity) requireActivity()).cerrarSesion();
                        } else {
                            String msg = r.body()!=null
                                    ? r.body().getMessage()
                                    : "Error al borrar cuenta";
                            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onFailure(Call<Mensaje> c, Throwable t) {
                        ((MainActivity) requireActivity()).hideLoading();
                        Toast.makeText(getContext(),"Error de red: "+t.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });
    }

}
