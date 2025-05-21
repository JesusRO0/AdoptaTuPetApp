package com.example.adoptatupet.views.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.adoptatupet.R;
import com.example.adoptatupet.models.Animal;
import com.example.adoptatupet.models.Mensaje;
import com.example.adoptatupet.network.ApiClient;
import com.example.adoptatupet.network.ApiService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Fragment para añadir un nuevo animal.
 * Incluye selección de especie, edad, sexo y tamaño con RadioGroups,
 * validación de campos y envío al servidor.
 */
public class AddAnimalFragment extends Fragment {

    private EditText etNombre, etRaza, etLocalidad, etDescripcion;
    private RadioGroup rgEspecie, rgEdad, rgSexo, rgTamanoGroup;
    private Spinner spinnerLocalidad;
    private RadioButton rbPerro, rbGato, rbCachorro, rbJoven, rbAdulto, rbMacho, rbHembra, rbPequeno, rbMediano, rbGrande;
    private ImageView ivFoto;
    private Button btnSeleccionarImagenAnimal, btnEnviarAnimal, btnVolver;
    private String imagenBase64 = null;

    // Lanzador para seleccionar imagen de la galería
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == requireActivity().RESULT_OK
                                && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            ivFoto.setImageURI(uri);
                            convertirImagenABase64(uri);
                        }
                    }
            );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // 1) Inflar layout
        View view = inflater.inflate(R.layout.fragment_add_animal, container, false);

        // 2) Inicializar vistas y variables
        etNombre = view.findViewById(R.id.editTextAnimalNombre);
        rgEspecie = view.findViewById(R.id.rgEspecie);
        rbPerro = view.findViewById(R.id.rbPerro);
        rbGato  = view.findViewById(R.id.rbGato);

        etRaza = view.findViewById(R.id.editTextRaza);

        rgEdad = view.findViewById(R.id.rgEdad);
        rbCachorro = view.findViewById(R.id.rbCachorro);
        rbJoven    = view.findViewById(R.id.rbJoven);
        rbAdulto   = view.findViewById(R.id.rbAdulto);

        // 1) Referencias a vistas...
        spinnerLocalidad = view.findViewById(R.id.spinnerLocalidadAnimal);

        // 2) Configuramos el adapter
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                requireContext(),
                R.array.spain_localities,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocalidad.setAdapter(adapter);

        rgSexo = view.findViewById(R.id.rgSexo);
        rbMacho  = view.findViewById(R.id.rbMacho);
        rbHembra = view.findViewById(R.id.rbHembra);

        rgTamanoGroup = view.findViewById(R.id.rgTamano);
        rbPequeno = view.findViewById(R.id.rbPequeno);
        rbMediano = view.findViewById(R.id.rbMediano);
        rbGrande  = view.findViewById(R.id.rbGrande);

        etDescripcion = view.findViewById(R.id.editTextDescripcion);

        ivFoto = view.findViewById(R.id.imageViewAnimal);
        btnSeleccionarImagenAnimal = view.findViewById(R.id.btnSeleccionarImagenAnimal);
        btnEnviarAnimal = view.findViewById(R.id.btnEnviarAnimal);
        btnVolver      = view.findViewById(R.id.btnVolver);

        // 3) Configurar listeners
        btnSeleccionarImagenAnimal.setOnClickListener(v ->
                imagePickerLauncher.launch(
                        new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                )
        );

        btnEnviarAnimal.setOnClickListener(v -> validarYEnviar());
        btnVolver.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        return view;
    }

    /**
     * Convierte la URI seleccionada a Base64 (sin saltos de línea).
     */
    private void convertirImagenABase64(Uri uri) {
        try {
            Bitmap bmp = MediaStore.Images.Media.getBitmap(
                    requireActivity().getContentResolver(), uri
            );
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, baos);
            imagenBase64 = android.util.Base64.encodeToString(
                    baos.toByteArray(), android.util.Base64.NO_WRAP
            );
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error al cargar imagen", Toast.LENGTH_SHORT).show();
            imagenBase64 = null;
        }
    }

    /**
     * Valida todos los campos y envía el objeto Animal al servidor.
     */
    private void validarYEnviar() {
        // Nombre
        String nombre = etNombre.getText().toString().trim();
        if (TextUtils.isEmpty(nombre)) {
            etNombre.setError("Requerido");
            etNombre.requestFocus();
            return;
        }

        // Especie
        int selEsp = rgEspecie.getCheckedRadioButtonId();
        if (selEsp == -1) {
            Toast.makeText(getContext(), "Selecciona la especie", Toast.LENGTH_SHORT).show();
            return;
        }
        String especie = selEsp == R.id.rbPerro ? "Perro" : "Gato";

        // Raza
        String raza = etRaza.getText().toString().trim();
        if (TextUtils.isEmpty(raza)) {
            etRaza.setError("Requerido");
            etRaza.requestFocus();
            return;
        }

        // Edad
        int selEdad = rgEdad.getCheckedRadioButtonId();
        if (selEdad == -1) {
            Toast.makeText(getContext(), "Selecciona la edad", Toast.LENGTH_SHORT).show();
            return;
        }
        String edad = selEdad == R.id.rbCachorro ? "Cachorro"
                : selEdad == R.id.rbJoven    ? "Joven"
                : "Adulto";

        // Localidad
        String localidad = spinnerLocalidad.getSelectedItem().toString();
        if (TextUtils.isEmpty(localidad)) {
            Toast.makeText(getContext(),"Selecciona la localidad",Toast.LENGTH_SHORT).show();
            return;
        }

        // Sexo
        int selSexo = rgSexo.getCheckedRadioButtonId();
        if (selSexo == -1) {
            Toast.makeText(getContext(), "Selecciona el sexo", Toast.LENGTH_SHORT).show();
            return;
        }
        String sexo = selSexo == R.id.rbMacho ? "Macho" : "Hembra";

        // Tamaño
        int selTam = rgTamanoGroup.getCheckedRadioButtonId();
        if (selTam == -1) {
            Toast.makeText(getContext(), "Selecciona el tamaño", Toast.LENGTH_SHORT).show();
            return;
        }
        String tamano = selTam == R.id.rbPequeno  ? "Pequeño"
                : selTam == R.id.rbMediano  ? "Mediano"
                : "Grande";

        // Descripción
        String descripcion = etDescripcion.getText().toString().trim();
        if (TextUtils.isEmpty(descripcion)) {
            etDescripcion.setError("Requerido");
            etDescripcion.requestFocus();
            return;
        }

        // Imagen
        if (imagenBase64 == null) {
            Toast.makeText(getContext(), "Selecciona una imagen", Toast.LENGTH_SHORT).show();
            return;
        }

        // ID Usuario
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user", Context.MODE_PRIVATE);
        int idUsuario = prefs.getInt("idUsuario", -1);
        if (idUsuario == -1) {
            Toast.makeText(getContext(), "Error de sesión", Toast.LENGTH_SHORT).show();
            return;
        }

        // Construir Animal
        Animal animal = new Animal(
                nombre,
                especie,
                raza,
                edad,
                localidad,
                sexo,
                tamano,
                descripcion,
                imagenBase64,
                idUsuario
        );

        // Enviar al servidor
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.addAnimal(animal).enqueue(new Callback<Mensaje>() {
            @Override
            public void onResponse(Call<Mensaje> call, Response<Mensaje> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    Toast.makeText(getContext(),
                            "Animal añadido con éxito", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                } else {
                    Toast.makeText(getContext(),
                            "Error al añadir animal", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Mensaje> call, Throwable t) {
                Toast.makeText(getContext(),
                        "Fallo en servidor: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
