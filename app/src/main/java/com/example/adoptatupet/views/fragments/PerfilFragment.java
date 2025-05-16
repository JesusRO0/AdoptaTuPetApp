package com.example.adoptatupet.views.fragments;

import android.content.Intent;
import android.graphics.Bitmap;
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
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.adoptatupet.R;
import com.example.adoptatupet.models.Usuario;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class PerfilFragment extends Fragment {

    private ImageView imageViewPerfil;
    private EditText editTextNombre, editTextLocalidad;
    private Button btnGuardar, btnCambiarFoto;
    private Usuario usuarioLogueado;
    private String imagenBase64 = null;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == getActivity().RESULT_OK && result.getData() != null) {
                    Uri selectedImage = result.getData().getData();
                    imageViewPerfil.setImageURI(selectedImage);
                    convertirImagenABase64(selectedImage);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_perfil, container, false);

        imageViewPerfil = view.findViewById(R.id.imageViewPerfil);
        editTextNombre = view.findViewById(R.id.editTextNombre);
        editTextLocalidad = view.findViewById(R.id.editTextLocalidad);
        btnGuardar = view.findViewById(R.id.btnGuardarCambios);
        btnCambiarFoto = view.findViewById(R.id.btnCambiarFoto);

        cargarDatosUsuario();

        btnCambiarFoto.setOnClickListener(v -> seleccionarImagen());
        btnGuardar.setOnClickListener(v -> guardarCambios());

        return view;
    }

    private void cargarDatosUsuario() {
        // Aquí deberías recuperar desde SharedPreferences u otro método real
        // Este es solo un ejemplo con valores por defecto
        usuarioLogueado = new Usuario();
        usuarioLogueado.setUsuario("NombreEjemplo");
        usuarioLogueado.setLocalidad("LocalidadEjemplo");

        editTextNombre.setText(usuarioLogueado.getUsuario());
        editTextLocalidad.setText(usuarioLogueado.getLocalidad());
        imageViewPerfil.setImageResource(R.drawable.default_avatar); // Puedes cargar desde Base64
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
        String nombre = editTextNombre.getText().toString().trim();
        String localidad = editTextLocalidad.getText().toString().trim();

        if (nombre.isEmpty() || localidad.isEmpty()) {
            Toast.makeText(getContext(), "Completa todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        usuarioLogueado.setUsuario(nombre);
        usuarioLogueado.setLocalidad(localidad);

        // Aquí puedes hacer una llamada con Retrofit a update_usuario.php
        // Si quieres, te ayudo a conectarlo ahora mismo

        Toast.makeText(getContext(), "Cambios guardados localmente", Toast.LENGTH_SHORT).show();
    }
}
