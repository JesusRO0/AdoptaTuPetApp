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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PerfilFragment extends Fragment {

    private ImageView imageViewPerfil;
    private TextView tvNombre, tvEmail, tvPassword, tvLocalidad;
    private EditText editTextNombre, editTextEmail, editTextPassword, editTextLocalidad;
    private Button btnGuardar, btnCambiarFoto;
    private Button btnEditarNombre, btnEditarEmail, btnEditarPassword, btnEditarLocalidad;

    // Guarda la imagen actual codificada en Base64 para enviar al backend
    private String imagenBase64 = null;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                    Uri selectedImage = result.getData().getData();
                    imageViewPerfil.setImageURI(selectedImage);
                    convertirImagenABase64(selectedImage);

                    // Aquí llama a método para guardar cambios con la nueva foto
                    guardarFotoDirecta();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        imageViewPerfil = view.findViewById(R.id.imageViewPerfil);
        tvNombre = view.findViewById(R.id.tvNombre);
        tvEmail = view.findViewById(R.id.tvEmail);
        tvPassword = view.findViewById(R.id.tvPassword);
        tvLocalidad = view.findViewById(R.id.tvLocalidad);

        editTextNombre = view.findViewById(R.id.editTextNombre);
        editTextEmail = view.findViewById(R.id.editTextEmail);
        editTextPassword = view.findViewById(R.id.editTextPassword);
        editTextLocalidad = view.findViewById(R.id.editTextLocalidad);

        btnGuardar = view.findViewById(R.id.btnGuardarCambios);
        btnCambiarFoto = view.findViewById(R.id.btnCambiarFoto);

        btnEditarNombre = view.findViewById(R.id.btnEditarNombre);
        btnEditarEmail = view.findViewById(R.id.btnEditarEmail);
        btnEditarPassword = view.findViewById(R.id.btnEditarPassword);
        btnEditarLocalidad = view.findViewById(R.id.btnEditarLocalidad);

        cargarDatosUsuario();

        btnCambiarFoto.setOnClickListener(v -> seleccionarImagen());

        btnEditarNombre.setOnClickListener(v -> toggleEditCancel(tvNombre, editTextNombre, btnEditarNombre));
        btnEditarEmail.setOnClickListener(v -> toggleEditCancel(tvEmail, editTextEmail, btnEditarEmail));
        btnEditarPassword.setOnClickListener(v -> toggleEditCancel(tvPassword, editTextPassword, btnEditarPassword));
        btnEditarLocalidad.setOnClickListener(v -> toggleEditCancel(tvLocalidad, editTextLocalidad, btnEditarLocalidad));

        btnGuardar.setOnClickListener(v -> guardarCambios());

        return view;
    }

    private void toggleEditCancel(TextView tv, EditText et, Button btn) {
        if (et.getVisibility() == View.GONE) {
            tv.setVisibility(View.GONE);
            et.setVisibility(View.VISIBLE);
            et.setText(tv.getText().toString().replaceFirst("^[^:]+: ?", ""));
            btn.setText("Cancelar");
            btnGuardar.setVisibility(View.VISIBLE);
        } else {
            et.setVisibility(View.GONE);
            tv.setVisibility(View.VISIBLE);
            btn.setText("Editar");

            if (editTextNombre.getVisibility() == View.GONE &&
                    editTextEmail.getVisibility() == View.GONE &&
                    editTextPassword.getVisibility() == View.GONE &&
                    editTextLocalidad.getVisibility() == View.GONE) {
                btnGuardar.setVisibility(View.GONE);
            }
        }
    }

    private void cargarDatosUsuario() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("user", Context.MODE_PRIVATE);

        String nombre = prefs.getString("usuario", "");
        tvNombre.setText("Nombre: " + nombre);
        editTextNombre.setText(nombre);

        String email = prefs.getString("email", "");
        tvEmail.setText("Email: " + email);
        editTextEmail.setText(email);

        tvPassword.setText("Contraseña: ********");
        editTextPassword.setText(prefs.getString("contrasena", ""));

        String localidad = prefs.getString("localidad", "");
        tvLocalidad.setText("Localidad: " + localidad);
        editTextLocalidad.setText(localidad);

        String base64Foto = prefs.getString("fotoPerfil", null);
        if (base64Foto != null && !base64Foto.isEmpty()) {
            try {
                byte[] decodedBytes = Base64.decode(base64Foto, Base64.DEFAULT);
                Bitmap decodedBitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                imageViewPerfil.setImageBitmap(decodedBitmap);
                imagenBase64 = base64Foto;  // Importante: inicializar con la foto guardada
            } catch (Exception e) {
                imageViewPerfil.setImageResource(R.drawable.default_avatar);
                imagenBase64 = null;
            }
        } else {
            imageViewPerfil.setImageResource(R.drawable.default_avatar);
            imagenBase64 = null;
        }

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

    private void seleccionarImagen() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(intent);
    }

    private void convertirImagenABase64(Uri uri) {
        try {
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), uri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
            byte[] imageBytes = baos.toByteArray();
            imagenBase64 = Base64.encodeToString(imageBytes, Base64.DEFAULT);
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error al cargar la imagen", Toast.LENGTH_SHORT).show();
        }
    }

    private void guardarCambios() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("user", Context.MODE_PRIVATE);
        int idUsuario = prefs.getInt("idUsuario", -1);
        if (idUsuario == -1) {
            Toast.makeText(getContext(), "Error: sesión no válida", Toast.LENGTH_SHORT).show();
            return;
        }

        String nombre = (editTextNombre.getVisibility() == View.VISIBLE) ? editTextNombre.getText().toString().trim() : prefs.getString("usuario", "");
        String email = (editTextEmail.getVisibility() == View.VISIBLE) ? editTextEmail.getText().toString().trim() : prefs.getString("email", "");
        String password = (editTextPassword.getVisibility() == View.VISIBLE) ? editTextPassword.getText().toString().trim() : prefs.getString("contrasena", "");
        String localidad = (editTextLocalidad.getVisibility() == View.VISIBLE) ? editTextLocalidad.getText().toString().trim() : prefs.getString("localidad", "");

        if (nombre.isEmpty() || email.isEmpty() || password.isEmpty() || localidad.isEmpty()) {
            Toast.makeText(getContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        Usuario usuario = new Usuario();
        usuario.setIdUsuario(idUsuario);
        usuario.setUsuario(nombre);
        usuario.setEmail(email);
        usuario.setContrasena(password);
        usuario.setLocalidad(localidad);
        usuario.setFotoPerfil(imagenBase64); // Usar la imagen actualizada o la que venga

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.updateUsuario(usuario).enqueue(new Callback<Mensaje>() {
            @Override
            public void onResponse(Call<Mensaje> call, Response<Mensaje> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("usuario", nombre);
                    editor.putString("email", email);
                    editor.putString("contrasena", password);
                    editor.putString("localidad", localidad);
                    editor.putString("fotoPerfil", imagenBase64); // Actualizamos la foto guardada localmente
                    editor.apply();

                    Toast.makeText(getContext(), "Datos actualizados", Toast.LENGTH_SHORT).show();

                    // Ocultar inputs y mostrar TextViews actualizados
                    editTextNombre.setVisibility(View.GONE);
                    editTextEmail.setVisibility(View.GONE);
                    editTextPassword.setVisibility(View.GONE);
                    editTextLocalidad.setVisibility(View.GONE);

                    tvNombre.setVisibility(View.VISIBLE);
                    tvEmail.setVisibility(View.VISIBLE);
                    tvPassword.setVisibility(View.VISIBLE);
                    tvLocalidad.setVisibility(View.VISIBLE);

                    // Reset botones editar a "Editar"
                    btnEditarNombre.setText("Editar");
                    btnEditarEmail.setText("Editar");
                    btnEditarPassword.setText("Editar");
                    btnEditarLocalidad.setText("Editar");

                    btnGuardar.setVisibility(View.GONE);

                    cargarDatosUsuario();
                } else {
                    Toast.makeText(getContext(), "Error al actualizar", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Mensaje> call, Throwable t) {
                Toast.makeText(getContext(), "Fallo en servidor: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void guardarFotoDirecta() {
        SharedPreferences prefs = requireActivity().getSharedPreferences("user", Context.MODE_PRIVATE);
        int idUsuario = prefs.getInt("idUsuario", -1);
        if (idUsuario == -1) {
            Toast.makeText(getContext(), "Error: sesión no válida", Toast.LENGTH_SHORT).show();
            return;
        }

        // Obtén los valores actuales guardados, para enviarlos junto a la foto
        String nombre = prefs.getString("usuario", "");
        String email = prefs.getString("email", "");
        String password = prefs.getString("contrasena", "");
        String localidad = prefs.getString("localidad", "");

        Usuario usuario = new Usuario();
        usuario.setIdUsuario(idUsuario);
        usuario.setUsuario(nombre);
        usuario.setEmail(email);
        usuario.setContrasena(password);
        usuario.setLocalidad(localidad);
        usuario.setFotoPerfil(imagenBase64); // La nueva imagen en base64

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.updateUsuario(usuario).enqueue(new Callback<Mensaje>() {
            @Override
            public void onResponse(Call<Mensaje> call, Response<Mensaje> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString("fotoPerfil", imagenBase64);
                    editor.apply();

                    Toast.makeText(getContext(), "Foto actualizada", Toast.LENGTH_SHORT).show();
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).actualizarFotoDrawer(imagenBase64);
                    }

                } else {
                    Toast.makeText(getContext(), "Error al actualizar la foto", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Mensaje> call, Throwable t) {
                Toast.makeText(getContext(), "Fallo en servidor: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}
