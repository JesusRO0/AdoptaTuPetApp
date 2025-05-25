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
import com.example.adoptatupet.views.MainActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Fragment para añadir un nuevo animal.
 * Tras un ADD exitoso, reemplaza el fragmento actual por AdoptaFragment,
 * lo que dispara su carga (caché/servidor) y muestra el nuevo animal.
 */
public class AddAnimalFragment extends Fragment {

    private EditText    etNombre, etDescripcion;
    private Spinner     spinnerLocalidad, spinnerRaza;
    private RadioGroup  rgEspecie, rgEdad, rgSexo, rgTamano;
    private RadioButton rbPerro, rbGato, rbCachorro, rbJoven, rbAdulto, rbMacho, rbHembra, rbPequeno, rbMediano, rbGrande;
    private ImageView   ivFoto;
    private Button      btnSeleccionarImagen, btnEnviarAnimal, btnVolver;
    private String      imagenBase64 = null;

    // Lanzador para escoger imagen de la galería
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

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_animal, container, false);

        // 1) Referencias
        etNombre             = view.findViewById(R.id.editTextAnimalNombre);
        spinnerLocalidad     = view.findViewById(R.id.spinnerLocalidadAnimal);
        spinnerRaza          = view.findViewById(R.id.spinnerRaza);
        rgEspecie            = view.findViewById(R.id.rgEspecie);
        rbPerro              = view.findViewById(R.id.rbPerro);
        rbGato               = view.findViewById(R.id.rbGato);
        rgEdad               = view.findViewById(R.id.rgEdad);
        rbCachorro           = view.findViewById(R.id.rbCachorro);
        rbJoven              = view.findViewById(R.id.rbJoven);
        rbAdulto             = view.findViewById(R.id.rbAdulto);
        rgSexo               = view.findViewById(R.id.rgSexo);
        rbMacho              = view.findViewById(R.id.rbMacho);
        rbHembra             = view.findViewById(R.id.rbHembra);
        rgTamano             = view.findViewById(R.id.rgTamano);
        rbPequeno            = view.findViewById(R.id.rbPequeno);
        rbMediano            = view.findViewById(R.id.rbMediano);
        rbGrande             = view.findViewById(R.id.rbGrande);
        etDescripcion        = view.findViewById(R.id.editTextDescripcion);
        ivFoto               = view.findViewById(R.id.imageViewAnimal);
        btnSeleccionarImagen = view.findViewById(R.id.btnSeleccionarImagenAnimal);
        btnEnviarAnimal      = view.findViewById(R.id.btnEnviarAnimal);
        btnVolver            = view.findViewById(R.id.btnVolver);

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
            @Override public boolean isEnabled(int pos) { return pos != 0; }
            @Override public View getDropDownView(int pos, View cv, ViewGroup p) {
                View v = super.getDropDownView(pos, cv, p);
                ((TextView)v).setTextColor(pos == 0 ? 0xFF888888 : 0xFF000000);
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
        rgEspecie.setOnCheckedChangeListener((g, id) -> {
            String[] breeds = id == R.id.rbPerro
                    ? getResources().getStringArray(R.array.dog_breeds)
                    : getResources().getStringArray(R.array.cat_breeds);
            List<String> razaList = new ArrayList<>();
            razaList.add("Selecciona raza");
            razaList.addAll(Arrays.asList(breeds));
            ArrayAdapter<String> razaAdapter = new ArrayAdapter<String>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    razaList
            ) {
                @Override public boolean isEnabled(int pos) { return pos != 0; }
                @Override public View getDropDownView(int pos, View cv, ViewGroup p) {
                    View v = super.getDropDownView(pos, cv, p);
                    ((TextView)v).setTextColor(pos == 0 ? 0xFF888888 : 0xFF000000);
                    return v;
                }
            };
            razaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerRaza.setAdapter(razaAdapter);
            spinnerRaza.setSelection(0);
        });

        // 4) Botones
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

    /** Convierte URI a Base64 (sin saltos) */
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
     * Valida todos los campos y envía al servidor mostrando rueda de carga.
     * Al éxito, navegamos a AdoptaFragment para recargar la lista.
     */
    private void validarYEnviar() {
        // — Validaciones abreviadas…
        String nombre = etNombre.getText().toString().trim();
        if (TextUtils.isEmpty(nombre)) {
            etNombre.setError("Requerido");
            return;
        }
        if (rgEspecie.getCheckedRadioButtonId() == -1) {
            Toast.makeText(getContext(), "Selecciona especie", Toast.LENGTH_SHORT).show();
            return;
        }
        String especie = rbPerro.isChecked() ? "Perro" : "Gato";
        if (spinnerRaza.getSelectedItemPosition() == 0) {
            Toast.makeText(getContext(), "Selecciona raza", Toast.LENGTH_SHORT).show();
            return;
        }
        String raza = spinnerRaza.getSelectedItem().toString();
        if (rgEdad.getCheckedRadioButtonId() == -1) {
            Toast.makeText(getContext(), "Selecciona edad", Toast.LENGTH_SHORT).show();
            return;
        }
        String edad = rbCachorro.isChecked() ? "Cachorro"
                : rbJoven.isChecked()   ? "Joven"
                : "Adulto";
        if (spinnerLocalidad.getSelectedItemPosition() == 0) {
            Toast.makeText(getContext(), "Selecciona localidad", Toast.LENGTH_SHORT).show();
            return;
        }
        String localidad = spinnerLocalidad.getSelectedItem().toString();
        if (rgSexo.getCheckedRadioButtonId() == -1) {
            Toast.makeText(getContext(), "Selecciona sexo", Toast.LENGTH_SHORT).show();
            return;
        }
        String sexo = rbMacho.isChecked() ? "Macho" : "Hembra";
        if (rgTamano.getCheckedRadioButtonId() == -1) {
            Toast.makeText(getContext(), "Selecciona tamaño", Toast.LENGTH_SHORT).show();
            return;
        }
        String tamano = rbPequeno.isChecked() ? "Pequeño"
                : rbMediano.isChecked() ? "Mediano"
                : "Grande";
        String desc = etDescripcion.getText().toString().trim();
        if (TextUtils.isEmpty(desc)) {
            etDescripcion.setError("Requerido");
            return;
        }
        if (imagenBase64 == null) {
            Toast.makeText(getContext(), "Selecciona imagen", Toast.LENGTH_SHORT).show();
            return;
        }
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user", Context.MODE_PRIVATE);
        int idU = prefs.getInt("idUsuario", -1);
        if (idU == -1) {
            Toast.makeText(getContext(), "Error de sesión", Toast.LENGTH_SHORT).show();
            return;
        }

        // Construimos el objeto Animal
        Animal nuevo = new Animal(
                nombre, especie, raza, edad,
                localidad, sexo, tamano, desc,
                imagenBase64, idU
        );

        // 1) Rueda de carga
        ((MainActivity) requireActivity()).showLoading();

        // 2) Llamada al controller
        animalController.getInstance(requireContext())
                .addAnimal(nuevo, new animalController.AnimalCallback() {
                    @Override
                    public void onSuccess() {
                        // 3) Ocultamos rueda
                        ((MainActivity) requireActivity()).hideLoading();
                        Toast.makeText(getContext(),
                                "Animal añadido con éxito",
                                Toast.LENGTH_SHORT).show();

                        // 4) Navegar a AdoptaFragment para refrescar lista
                        requireActivity().getSupportFragmentManager()
                                .beginTransaction()
                                .replace(R.id.nav_host_fragment, new AdoptaFragment())
                                .commit();
                    }
                    @Override
                    public void onError(String msg) {
                        ((MainActivity) requireActivity()).hideLoading();
                        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
