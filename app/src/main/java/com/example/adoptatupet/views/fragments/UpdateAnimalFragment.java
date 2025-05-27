package com.example.adoptatupet.views.fragments;

import android.content.Intent;
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
 * - Rellena el formulario con datos de caché (instantáneo).
 * - Luego refresca desde servidor y actualiza los campos.
 */
public class UpdateAnimalFragment extends Fragment {

    private static final String ARG_ID = "animal_id";
    private int animalId;
    private Animal animal;                  // Animal actual (cacheado o fetch)

    // Vistas del formulario
    private EditText    etNombre, etDescripcion;
    private Spinner     spinnerLocalidad, spinnerRaza;
    private RadioGroup  rgEspecie, rgEdad, rgSexo, rgTamano;
    private RadioButton rbPerro, rbGato, rbCachorro, rbJoven,
            rbAdulto, rbMacho, rbHembra,
            rbPequeno, rbMediano, rbGrande;
    private ImageView   ivFoto;
    private Button      btnSeleccionarImagen, btnGuardar, btnCancelar;

    // Solo se llenará al seleccionar una nueva imagen
    private String      imagenBase64 = null;

    // Launcher para la galería
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

    public UpdateAnimalFragment() {
        // Required empty constructor
    }

    /** Crea instancia pasando solo el ID para evitar TransactionTooLarge */
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

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_update_animal, container, false);

        // 1) Referencias a vistas
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

        // 2) Prefill inmediato desde caché
        animalController controller = animalController.getInstance(requireContext());
        List<Animal> cache = controller.getCachedAnimals();
        for (Animal a : cache) {
            if (a.getIdAnimal() == animalId) {
                animal = a;
                break;
            }
        }
        if (animal != null) {
            populateFields(animal);
        }

        // 3) Refrescar desde servidor
        controller.fetchAnimalById(animalId, new animalController.AnimalByIdCallback() {
            @Override
            public void onSuccess(Animal a) {
                animal = a;
                populateFields(a);
            }
            @Override
            public void onError(String msg) {
                Toast.makeText(requireContext(),
                        "Error cargando datos actualizados: " + msg,
                        Toast.LENGTH_LONG).show();
            }
        });

        // 4) Cuando cambie especie, recargar raza
        rgEspecie.setOnCheckedChangeListener((grp, checkedId) -> {
            String[] newBreeds = (checkedId == R.id.rbPerroEdit)
                    ? getResources().getStringArray(R.array.dog_breeds)
                    : getResources().getStringArray(R.array.cat_breeds);
            ArrayAdapter<String> razaAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    Arrays.asList(newBreeds)
            );
            razaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerRaza.setAdapter(razaAdapter);
        });

        // 5) Selector de imagen
        btnSeleccionarImagen.setOnClickListener(v ->
                imagePickerLauncher.launch(
                        new Intent(Intent.ACTION_PICK,
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                )
        );

        // 6) Botón Guardar cambios
        btnGuardar.setOnClickListener(v -> {
            // Validaciones básicas
            String nombre = etNombre.getText().toString().trim();
            if (TextUtils.isEmpty(nombre)) {
                etNombre.setError("Requerido");
                return;
            }
            String especie = rbPerro.isChecked() ? "Perro" : "Gato";
            String raza    = spinnerRaza.getSelectedItem().toString();
            String edad    = ((RadioButton) requireView()
                    .findViewById(rgEdad.getCheckedRadioButtonId()))
                    .getText().toString();
            String localidad = spinnerLocalidad.getSelectedItem().toString();
            String sexo      = ((RadioButton) requireView()
                    .findViewById(rgSexo.getCheckedRadioButtonId()))
                    .getText().toString();
            String tamano    = ((RadioButton) requireView()
                    .findViewById(rgTamano.getCheckedRadioButtonId()))
                    .getText().toString();
            String desc = etDescripcion.getText().toString().trim();
            if (TextUtils.isEmpty(desc)) {
                etDescripcion.setError("Requerido");
                return;
            }

            // Si no cambió la imagen, usar la original en 'animal'
            String fotoBase64 = (imagenBase64 != null)
                    ? imagenBase64
                    : (animal != null ? animal.getImagen() : null);

            // Construir Animal actualizado
            Animal actualizado = new Animal(
                    animal.getIdAnimal(),
                    nombre,
                    especie,
                    raza,
                    edad,
                    localidad,
                    sexo,
                    tamano,
                    desc,
                    fotoBase64,
                    animal.getIdUsuario()
            );

            // Mostrar loader
            ((MainActivity) requireActivity()).showLoading();

            // Llamada a updateAnimal
            controller.updateAnimal(actualizado, new animalController.AnimalCallback() {
                @Override
                public void onSuccess() {
                    ((MainActivity) requireActivity()).hideLoading();
                    Toast.makeText(getContext(),
                            "Animal actualizado", Toast.LENGTH_SHORT).show();
                    requireActivity().getSupportFragmentManager().popBackStack();
                }
                @Override
                public void onError(String msg) {
                    ((MainActivity) requireActivity()).hideLoading();
                    Toast.makeText(getContext(),
                            "Error: " + msg, Toast.LENGTH_LONG).show();
                }
            });
        });

        // 7) Cancelar edición
        btnCancelar.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        return view;
    }

    /**
     * Helper para rellenar **todos** los campos del formulario
     * con los datos del objeto Animal dado.
     */
    private void populateFields(@NonNull Animal a) {
        // Texto
        etNombre.setText(a.getNombre());
        etDescripcion.setText(a.getDescripcion());

        // Especie y recarga Raza
        boolean isDog = "Perro".equalsIgnoreCase(a.getEspecie());
        rbPerro.setChecked(isDog);
        rbGato .setChecked(!isDog);
        String[] breeds = isDog
                ? getResources().getStringArray(R.array.dog_breeds)
                : getResources().getStringArray(R.array.cat_breeds);
        ArrayAdapter<String> razaAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                Arrays.asList(breeds)
        );
        razaAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerRaza.setAdapter(razaAdapter);
        spinnerRaza.setSelection(Arrays.asList(breeds).indexOf(a.getRaza()));

        // Edad
        selectRadio(rgEdad, a.getEdad());
        // Localidad (XML usa entries="@array/spain_localities")
        List<String> locs = Arrays.asList(
                getResources().getStringArray(R.array.spain_localities)
        );
        spinnerLocalidad.setSelection(locs.indexOf(a.getLocalidad()));
        // Sexo
        selectRadio(rgSexo, a.getSexo());
        // Tamaño
        selectRadio(rgTamano, a.getTamano());

        // Imagen Base64 → Bitmap
        String b64 = a.getImagen();
        if (b64 != null && !b64.isEmpty()) {
            byte[] data = Base64.decode(b64, Base64.DEFAULT);
            ivFoto.setImageBitmap(
                    BitmapFactory.decodeByteArray(data, 0, data.length)
            );
        } else {
            ivFoto.setImageResource(R.drawable.default_avatar);
        }

        // Reiniciar bandera de nueva imagen para que solo cambie al seleccionar
        imagenBase64 = null;
    }

    /** Convierte URI a Base64 sin saltos */
    private void convertirImagenABase64(@NonNull Uri uri) {
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
            Toast.makeText(getContext(),
                    "Error al cargar imagen", Toast.LENGTH_SHORT
            ).show();
            imagenBase64 = null;
        }
    }

    /** Marca el RadioButton dentro de rg cuyo texto coincide con val */
    private void selectRadio(@NonNull RadioGroup rg, @NonNull String val) {
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
