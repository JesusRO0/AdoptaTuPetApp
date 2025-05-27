package com.example.adoptatupet.views.fragments;

import android.content.Intent;
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
import com.example.adoptatupet.controllers.animalController;
import com.example.adoptatupet.models.Animal;
import com.example.adoptatupet.views.MainActivity;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Fragment para editar un Animal existente.
 * Recibe solo el ID (ARG_ID) para evitar TransactionTooLargeException.
 * Al guardar cambios valida campos y llama a updateAnimal.
 */
public class UpdateAnimalFragment extends Fragment {

    private static final String ARG_ID = "animal_id";
    private int animalId;
    private Animal animal;

    private EditText    etNombre, etDescripcion;
    private Spinner     spinnerLocalidad, spinnerRaza;
    private RadioGroup  rgEspecie, rgEdad, rgSexo, rgTamano;
    private RadioButton rbPerro, rbGato, rbCachorro, rbJoven, rbAdulto, rbMacho, rbHembra, rbPequeno, rbMediano, rbGrande;
    private ImageView   ivFoto;
    private Button      btnSeleccionarImagen, btnGuardar, btnCancelar;
    private String      imagenBase64 = null;

    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(), result -> {
                        if (result.getResultCode() == requireActivity().RESULT_OK
                                && result.getData() != null) {
                            Uri uri = result.getData().getData();
                            ivFoto.setImageURI(uri);
                            convertirImagenABase64(uri);
                        }
                    }
            );

    public UpdateAnimalFragment() {
        // Required empty constructor
    }

    /**
     * Instancia pasando solo el ID del Animal.
     */
    public static UpdateAnimalFragment newInstance(int idAnimal) {
        UpdateAnimalFragment f = new UpdateAnimalFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ID, idAnimal);
        f.setArguments(args);
        return f;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            animalId = getArguments().getInt(ARG_ID, -1);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_update_animal, container, false);

        // Bind vistas
        etNombre             = view.findViewById(R.id.edit2TextAnimalNombre);
        spinnerLocalidad     = view.findViewById(R.id.editTextAnimalLocalidad);
        spinnerRaza          = view.findViewById(R.id.editTextAnimalRaza);
        rgEspecie            = view.findViewById(R.id.editTextAnimalEspecie);
        rbPerro              = view.findViewById(R.id.rbPerroEdit);
        rbGato               = view.findViewById(R.id.rbGatoEdit);
        rgEdad               = view.findViewById(R.id.editTextAnimalEdad);
        rbCachorro           = view.findViewById(R.id.rbCachorroEdit);
        rbJoven              = view.findViewById(R.id.rbJovenEdit);
        rbAdulto             = view.findViewById(R.id.rbAdultoEdit);
        rgSexo               = view.findViewById(R.id.editTextAnimalSexo);
        rbMacho              = view.findViewById(R.id.rbMachoEdit);
        rbHembra             = view.findViewById(R.id.rbHembraEdit);
        rgTamano             = view.findViewById(R.id.editTextAnimalTamano);
        rbPequeno            = view.findViewById(R.id.rbPequenoEdit);
        rbMediano            = view.findViewById(R.id.rbMedianoEdit);
        rbGrande             = view.findViewById(R.id.rbGrandeEdit);
        etDescripcion        = view.findViewById(R.id.editTextAnimalDescripcion);
        ivFoto               = view.findViewById(R.id.editimageViewAnimal);
        btnSeleccionarImagen = view.findViewById(R.id.btnSeleccionarImagen);
        btnGuardar           = view.findViewById(R.id.btnEditGuardarCambios);
        btnCancelar          = view.findViewById(R.id.btnCancelarEdicion);

        // TODO: fetch animal details by ID and then prefill fields
        // Por ejemplo:
        // animalController.getInstance(requireContext())
        //   .fetchAnimalById(animalId, new animalController.AnimalByIdCallback() { ... });

        // Inicia imagen default
        ivFoto.setImageResource(R.drawable.default_avatar);

        // Escucha cambios especie para recargar razas
        rgEspecie.setOnCheckedChangeListener((g, id) -> {
            String[] newBreeds = (id == R.id.rbPerroEdit)
                    ? getResources().getStringArray(R.array.dog_breeds)
                    : getResources().getStringArray(R.array.cat_breeds);
            ArrayAdapter<String> newAdapter = new ArrayAdapter<>(
                    requireContext(), android.R.layout.simple_spinner_item,
                    Arrays.asList(newBreeds)
            );
            newAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerRaza.setAdapter(newAdapter);
        });

        // Selección de imagen
        btnSeleccionarImagen.setOnClickListener(v ->
                imagePickerLauncher.launch(
                        new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                )
        );

        // Guardar
        btnGuardar.setOnClickListener(v -> {
            // Validaciones...
            String nombre = etNombre.getText().toString().trim();
            if (TextUtils.isEmpty(nombre)) { etNombre.setError("Requerido"); return; }
            // Resto de validaciones

            // Construir Animal actualizado (usar imagenBase64 o existente)
            // TODO: asegurarse de que 'animal' no sea null si no se ha fetcheado
            Animal actualizado = new Animal(
                    animalId,
                    nombre,
                    rbPerro.isChecked() ? "Perro" : "Gato",
                    spinnerRaza.getSelectedItem().toString(),
                    ((RadioButton) requireView().findViewById(rgEdad.getCheckedRadioButtonId())).getText().toString(),
                    spinnerLocalidad.getSelectedItem().toString(),
                    ((RadioButton) requireView().findViewById(rgSexo.getCheckedRadioButtonId())).getText().toString(),
                    ((RadioButton) requireView().findViewById(rgTamano.getCheckedRadioButtonId())).getText().toString(),
                    etDescripcion.getText().toString().trim(),
                    imagenBase64 != null ? imagenBase64 : (animal != null ? animal.getImagen() : null),
                    animal != null ? animal.getIdUsuario() : -1
            );

            ((MainActivity) requireActivity()).showLoading();
            animalController.getInstance(requireContext())
                    .updateAnimal(actualizado, new animalController.AnimalCallback() {
                        @Override public void onSuccess() {
                            ((MainActivity) requireActivity()).hideLoading();
                            Toast.makeText(getContext(), "Animal actualizado", Toast.LENGTH_SHORT).show();
                            requireActivity().getSupportFragmentManager().popBackStack();
                        }
                        @Override public void onError(String msg) {
                            ((MainActivity) requireActivity()).hideLoading();
                            Toast.makeText(getContext(), "Error: " + msg, Toast.LENGTH_LONG).show();
                        }
                    });
        });

        btnCancelar.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        return view;
    }

    private void convertirImagenABase64(Uri uri) {
        try {
            Bitmap bmp = MediaStore.Images.Media.getBitmap(
                    requireActivity().getContentResolver(), uri);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, baos);
            imagenBase64 = android.util.Base64.encodeToString(
                    baos.toByteArray(), android.util.Base64.NO_WRAP);
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error al cargar imagen", Toast.LENGTH_SHORT).show();
            imagenBase64 = null;
        }
    }

    private void selectRadio(RadioGroup rg, String val) {
        for (int i = 0; i < rg.getChildCount(); i++) {
            if (rg.getChildAt(i) instanceof RadioButton) {
                RadioButton rb = (RadioButton) rg.getChildAt(i);
                if (rb.getText().toString().equalsIgnoreCase(val)) {
                    rb.setChecked(true);
                    return;
                }
            }
        }
    }
}
