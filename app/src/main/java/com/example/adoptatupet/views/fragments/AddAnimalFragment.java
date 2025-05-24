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
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.adoptatupet.R;
import com.example.adoptatupet.controllers.animalController;
import com.example.adoptatupet.models.Animal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fragment para añadir un nuevo animal.
 * - Especie: Perro / Gato (RadioGroup)
 * - Raza: Spinner dinámico según especie
 * - Edad: Cachorro / Joven / Adulto (RadioGroup)
 * - Localidad: Spinner con placeholder y lista de España
 * - Sexo: Macho / Hembra (RadioGroup)
 * - Tamaño: Pequeño / Mediano / Grande (RadioGroup)
 * - Descripción: EditText
 * - Imagen: galería -> Base64
 * - Validación de todos los campos y envío al servidor
 */
public class AddAnimalFragment extends Fragment {

    private Spinner     spinnerLocalidad, spinnerRaza;
    private RadioGroup  rgEspecie, rgEdad, rgSexo, rgTamano;
    private RadioButton rbPerro, rbGato, rbCachorro, rbJoven, rbAdulto, rbMacho, rbHembra, rbPequeno, rbMediano, rbGrande;
    private EditText    etDescripcion;
    private ImageView   ivFoto;
    private Button      btnSeleccionarImagen, btnEnviarAnimal, btnVolver;
    private String      imagenBase64 = null;

    // Lanzador para seleccionar imagen de la galería
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    ivFoto.setImageURI(uri);
                    convertirImagenABase64(uri);
                }
            });

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_animal, container, false);

        // 1) Referencias a vistas
        spinnerLocalidad      = view.findViewById(R.id.spinnerLocalidadAnimal);
        spinnerRaza           = view.findViewById(R.id.spinnerRaza);

        rgEspecie             = view.findViewById(R.id.rgEspecie);
        rbPerro               = view.findViewById(R.id.rbPerro);
        rbGato                = view.findViewById(R.id.rbGato);

        rgEdad                = view.findViewById(R.id.rgEdad);
        rbCachorro            = view.findViewById(R.id.rbCachorro);
        rbJoven               = view.findViewById(R.id.rbJoven);
        rbAdulto              = view.findViewById(R.id.rbAdulto);

        rgSexo                = view.findViewById(R.id.rgSexo);
        rbMacho               = view.findViewById(R.id.rbMacho);
        rbHembra              = view.findViewById(R.id.rbHembra);

        rgTamano              = view.findViewById(R.id.rgTamano);
        rbPequeno             = view.findViewById(R.id.rbPequeno);
        rbMediano             = view.findViewById(R.id.rbMediano);
        rbGrande              = view.findViewById(R.id.rbGrande);

        etDescripcion         = view.findViewById(R.id.editTextDescripcion);
        ivFoto                = view.findViewById(R.id.imageViewAnimal);

        btnSeleccionarImagen  = view.findViewById(R.id.btnSeleccionarImagenAnimal);
        btnEnviarAnimal       = view.findViewById(R.id.btnEnviarAnimal);
        btnVolver             = view.findViewById(R.id.btnVolver);

        // 2) Spinner Localidad con placeholder
        String[] baseLocs = getResources().getStringArray(R.array.spain_localities);
        List<String> locList = new ArrayList<>();
        locList.add("Selecciona localidad");
        locList.addAll(Arrays.asList(baseLocs));
        ArrayAdapter<String> locAdapter = new ArrayAdapter<String>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                locList
        ) {
            @Override public boolean isEnabled(int position) {
                return position != 0;
            }
            @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
                View v = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView)v;
                tv.setTextColor(position == 0 ? 0xFF888888 : 0xFF000000);
                return v;
            }
        };
        locAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerLocalidad.setAdapter(locAdapter);
        spinnerLocalidad.setSelection(0);

        // 3) Spinner Raza dinámico según especie
        spinnerRaza.setAdapter(new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new String[]{"Selecciona especie primero"}
        ));

        rgEspecie.setOnCheckedChangeListener((group, checkedId) -> {
            String[] base = (checkedId == R.id.rbPerro)
                    ? getResources().getStringArray(R.array.dog_breeds)
                    : getResources().getStringArray(R.array.cat_breeds);

            List<String> razaList = new ArrayList<>();
            razaList.add("Selecciona raza");
            razaList.addAll(Arrays.asList(base));

            ArrayAdapter<String> razaAdapter = new ArrayAdapter<String>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    razaList
            ) {
                @Override public boolean isEnabled(int position) {
                    return position != 0;
                }
                @Override public View getDropDownView(int position, View convertView, ViewGroup parent) {
                    View v = super.getDropDownView(position, convertView, parent);
                    TextView tv = (TextView)v;
                    tv.setTextColor(position == 0 ? 0xFF888888 : 0xFF000000);
                    return v;
                }
            };
            razaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerRaza.setAdapter(razaAdapter);
            spinnerRaza.setSelection(0);
        });

        // 4) Listeners de botones
        btnSeleccionarImagen.setOnClickListener(v ->
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

    /** Convierte URI a Base64 (sin saltos de línea) */
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

    /** Valida todos los campos y envía el objeto Animal al servidor */
    private void validarYEnviar() {
        // Especie
        int selEsp = rgEspecie.getCheckedRadioButtonId();
        if (selEsp == -1) {
            Toast.makeText(getContext(), "Selecciona la especie", Toast.LENGTH_SHORT).show();
            return;
        }
        String especie = (selEsp == R.id.rbPerro) ? "Perro" : "Gato";

        // Raza
        if (spinnerRaza.getSelectedItemPosition() == 0) {
            Toast.makeText(getContext(), "Selecciona la raza", Toast.LENGTH_SHORT).show();
            return;
        }
        String raza = spinnerRaza.getSelectedItem().toString();

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
        if (spinnerLocalidad.getSelectedItemPosition() == 0) {
            Toast.makeText(getContext(), "Selecciona la localidad", Toast.LENGTH_SHORT).show();
            return;
        }
        String localidad = spinnerLocalidad.getSelectedItem().toString();

        // Sexo
        int selSexo = rgSexo.getCheckedRadioButtonId();
        if (selSexo == -1) {
            Toast.makeText(getContext(), "Selecciona el sexo", Toast.LENGTH_SHORT).show();
            return;
        }
        String sexo = (selSexo == R.id.rbMacho) ? "Macho" : "Hembra";

        // Tamaño
        int selTam = rgTamano.getCheckedRadioButtonId();
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

        // Construir Animal (usamos raza también como nombre)
        Animal animal = new Animal(
                raza,
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

        // Enviar al servidor con animalController
        animalController.getInstance(requireContext())
                .addAnimal(animal, new animalController.AnimalCallback() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getContext(),
                                "Animal añadido con éxito", Toast.LENGTH_SHORT).show();
                        requireActivity().getSupportFragmentManager().popBackStack();
                    }
                    @Override
                    public void onError(String message) {
                        Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
