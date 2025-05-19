package com.example.adoptatupet.views.fragments;

import android.content.Context;
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
import androidx.fragment.app.Fragment;

import com.example.adoptatupet.R;
import com.example.adoptatupet.models.Mensaje;
import com.example.adoptatupet.models.Usuario;
import com.example.adoptatupet.network.ApiClient;
import com.example.adoptatupet.network.ApiService;
import com.example.adoptatupet.views.MainActivity;
import com.example.adoptatupet.views.fragments.AddAnimalFragment;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * PerfilFragment: muestra y permite editar los datos del usuario.
 * Si el email guardado es admin@gmail.com, muestra un botón "AÑADIR ANIMAL"
 * que navega a AddAnimalFragment.
 */
public class PerfilFragment extends Fragment {

    // -- Vistas del layout --
    private ImageView imageViewPerfil;
    private TextView  tvNombre, tvEmail, tvPassword, tvLocalidad;
    private EditText  editTextNombre, editTextEmail, editTextPassword, editTextLocalidad;
    private Button    btnGuardar, btnCambiarFoto;
    private Button    btnEditarNombre, btnEditarEmail, btnEditarPassword, btnEditarLocalidad;
    private Button    btnAñadirAnimal;

    // Cadena Base64 de la imagen actual
    private String imagenBase64 = null;

    // Lanzador para seleccionar una foto de la galería
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
                             @Nullable ViewGroup   container,
                             @Nullable Bundle      savedInstanceState) {
        // 1) Inflar el XML
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        // 2) Referencias a las vistas
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

        // 3) Cargar datos del usuario (SharedPreferences) y actualizar UI
        cargarDatosUsuario();

        // 4) Listeners de edición
        btnCambiarFoto.setOnClickListener(v -> seleccionarImagen());
        btnEditarNombre.   setOnClickListener(v -> toggleEditCancel(tvNombre,    editTextNombre,    btnEditarNombre));
        btnEditarEmail.    setOnClickListener(v -> toggleEditCancel(tvEmail,     editTextEmail,     btnEditarEmail));
        btnEditarPassword. setOnClickListener(v -> toggleEditCancel(tvPassword,  editTextPassword,  btnEditarPassword));
        btnEditarLocalidad.setOnClickListener(v -> toggleEditCancel(tvLocalidad, editTextLocalidad, btnEditarLocalidad));
        btnGuardar.        setOnClickListener(v -> guardarCambios());

        // 5) Mostrar “AÑADIR ANIMAL” si es admin
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user", Context.MODE_PRIVATE);
        String email = prefs.getString("email", "");
        if ("admin@gmail.com".equalsIgnoreCase(email)) {
            btnAñadirAnimal.setVisibility(View.VISIBLE);
            btnAñadirAnimal.setText("AÑADIR ANIMAL");
            btnAñadirAnimal.setOnClickListener(v -> {
                // Navegar al formulario de administración
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.nav_host_fragment, new AddAnimalFragment())
                        .addToBackStack(null)
                        .commit();
            });
        } else {
            btnAñadirAnimal.setVisibility(View.GONE);
        }

        return view;
    }

    /** Alterna entre ver y editar un campo concreto. */
    private void toggleEditCancel(TextView tv, EditText et, Button btn) {
        if (et.getVisibility() == View.GONE) {
            // Pasar a edición
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
            // Ocultar Guardar si no hay campos en edición
            if (editTextNombre.getVisibility()==View.GONE &&
                    editTextEmail.getVisibility()==View.GONE &&
                    editTextPassword.getVisibility()==View.GONE &&
                    editTextLocalidad.getVisibility()==View.GONE) {
                btnGuardar.setVisibility(View.GONE);
            }
        }
    }

    /** Carga todos los datos del usuario (incluida la foto) desde SharedPreferences. */
    public void cargarDatosUsuario() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user", Context.MODE_PRIVATE);

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
        tvLocalidad.setText("Localidad: " + loc);
        editTextLocalidad.setText(loc);

        // Foto (Base64)
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

        // Restablecer vistas de edición
        editTextNombre.setVisibility(View.GONE);
        editTextEmail.setVisibility(View.GONE);
        editTextPassword.setVisibility(View.GONE);
        editTextLocalidad.setVisibility(View.GONE);
        btnGuardar.setVisibility(View.GONE);
        btnEditarNombre.setText("Editar");
        btnEditarEmail.setText("Editar");
        btnEditarPassword.setText("Editar");
        btnEditarLocalidad.setText("Editar");
    }

    /** Abre la galería para seleccionar imagen. */
    private void seleccionarImagen() {
        Intent i = new Intent(Intent.ACTION_PICK,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(i);
    }

    /** Convierte una URI de imagen a Base64. */
    private void convertirImagenABase64(Uri uri) {
        try {
            Bitmap bmp = MediaStore.Images.Media.getBitmap(
                    requireActivity().getContentResolver(), uri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, baos);
            imagenBase64 = Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT);
        } catch (IOException e) {
            Toast.makeText(getContext(),
                    "Error al cargar la imagen", Toast.LENGTH_SHORT).show();
        }
    }

    /** Envía al servidor todos los cambios y actualiza SharedPreferences + drawer. */
    private void guardarCambios() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user", Context.MODE_PRIVATE);
        int idUsuario = prefs.getInt("idUsuario", -1);
        if (idUsuario == -1) {
            Toast.makeText(getContext(),
                    "Error: sesión no válida", Toast.LENGTH_SHORT).show();
            return;
        }

        // Recoger valores editados o de prefs
        String nombre =  editTextNombre.getVisibility()==View.VISIBLE
                ? editTextNombre.getText().toString().trim()
                : prefs.getString("usuario", "");
        String mail =     editTextEmail.getVisibility()==View.VISIBLE
                ? editTextEmail.getText().toString().trim()
                : prefs.getString("email", "");
        String pass =     editTextPassword.getVisibility()==View.VISIBLE
                ? editTextPassword.getText().toString().trim()
                : prefs.getString("contrasena", "");
        String loc  =     editTextLocalidad.getVisibility()==View.VISIBLE
                ? editTextLocalidad.getText().toString().trim()
                : prefs.getString("localidad", "");

        if (nombre.isEmpty()||mail.isEmpty()||pass.isEmpty()||loc.isEmpty()) {
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
            @Override public void onResponse(Call<Mensaje> c, Response<Mensaje> r) {
                if (r.isSuccessful() && r.body()!=null && r.body().isSuccess()) {
                    // Guardar foto limpia en prefs
                    prefs.edit()
                            .putString("fotoPerfil", imagenBase64.replaceAll("\\s+",""))
                            .apply();
                    // Actualizar drawer
                    ((MainActivity)getActivity())
                            .actualizarFotoDrawer(imagenBase64);
                    Toast.makeText(getContext(),
                            "Datos actualizados", Toast.LENGTH_SHORT).show();
                    cargarDatosUsuario();
                } else {
                    Toast.makeText(getContext(),
                            "Error al actualizar", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Mensaje> c, Throwable t) {
                Toast.makeText(getContext(),
                        "Fallo en servidor: "+t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Envía inmediatamente la nueva foto al servidor tras seleccionarla. */
    private void guardarFotoDirecta() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user", Context.MODE_PRIVATE);
        int idUsuario = prefs.getInt("idUsuario", -1);
        if (idUsuario == -1) {
            Toast.makeText(getContext(),
                    "Error: sesión no válida", Toast.LENGTH_SHORT).show();
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
            @Override public void onResponse(Call<Mensaje> c, Response<Mensaje> r) {
                if (r.isSuccessful() && r.body()!=null && r.body().isSuccess()) {
                    prefs.edit().putString("fotoPerfil", imagenBase64).apply();
                    Toast.makeText(getContext(),
                            "Foto actualizada", Toast.LENGTH_SHORT).show();
                    ((MainActivity)getActivity())
                            .actualizarFotoDrawer(imagenBase64);
                } else {
                    Toast.makeText(getContext(),
                            "Error al actualizar foto", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Mensaje> c, Throwable t) {
                Toast.makeText(getContext(),
                        "Fallo en servidor: "+t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    /** Si este fragmento está activo tras login, actualiza sólo la imagen. */
    public void actualizarImagenPerfilDesdePrefs() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user", Context.MODE_PRIVATE);
        String b64 = prefs.getString("fotoPerfil", null);
        if (b64 != null && !b64.isEmpty()) {
            b64 = b64.replaceAll("\\s+","");
            try {
                byte[] data = Base64.decode(b64, Base64.DEFAULT);
                Bitmap bmp = BitmapFactory.decodeByteArray(data,0,data.length);
                imageViewPerfil.setImageBitmap(bmp);
            } catch (Exception e) {
                imageViewPerfil.setImageResource(R.drawable.default_avatar);
            }
        } else {
            imageViewPerfil.setImageResource(R.drawable.default_avatar);
        }
    }
}
