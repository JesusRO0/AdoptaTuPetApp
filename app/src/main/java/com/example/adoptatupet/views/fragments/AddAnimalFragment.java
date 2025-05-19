package com.example.adoptatupet.views.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
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
import com.example.adoptatupet.models.Animal;
import com.example.adoptatupet.models.Mensaje;
import com.example.adoptatupet.network.ApiClient;
import com.example.adoptatupet.network.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class AddAnimalFragment extends Fragment {

    private EditText etNombre, etEspecie, etRaza, etEdad, etLocalidad, etSexo, etTamano, etDescripcion;
    private ImageView ivFoto;
    private Button btnSeleccionarImagen, btnEnviarAnimal, btnVolver;
    private String imagenBase64 = null;

    // Lanzador para seleccionar imagen
    private final ActivityResultLauncher<Intent> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == requireActivity().RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    ivFoto.setImageURI(uri);
                    convertirImagenABase64(uri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_animal, container, false);

        // Referencias a vistas
        etNombre       = view.findViewById(R.id.editTextAnimalNombre);
        etEspecie      = view.findViewById(R.id.editTextEspecie);
        etRaza         = view.findViewById(R.id.editTextRaza);
        etEdad         = view.findViewById(R.id.editTextEdad);
        etLocalidad    = view.findViewById(R.id.editTextLocalidadAnimal);
        etSexo         = view.findViewById(R.id.editTextSexo);
        etTamano       = view.findViewById(R.id.editTextTamano);
        etDescripcion  = view.findViewById(R.id.editTextDescripcion);
        ivFoto         = view.findViewById(R.id.imageViewAnimal);
        btnSeleccionarImagen = view.findViewById(R.id.btnSeleccionarImagenAnimal);
        btnEnviarAnimal      = view.findViewById(R.id.btnEnviarAnimal);
        btnVolver            = view.findViewById(R.id.btnVolver);

        // Selección de imagen
        btnSeleccionarImagen.setOnClickListener(v ->
                imagePickerLauncher.launch(
                        new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                )
        );

        // Envío de datos
        btnEnviarAnimal.setOnClickListener(v -> validarYEnviar());

        // Volver al perfil
        btnVolver.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager()
                        .popBackStack()
        );

        return view;
    }

    /** Convierte URI a Base64 */
    private void convertirImagenABase64(Uri uri) {
        try {
            Bitmap bmp = MediaStore.Images.Media.getBitmap(
                    requireActivity().getContentResolver(), uri
            );
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.JPEG, 90, baos);
            imagenBase64 = android.util.Base64.encodeToString(baos.toByteArray(),
                    android.util.Base64.NO_WRAP);
        } catch (IOException e) {
            Toast.makeText(getContext(), "Error al cargar imagen", Toast.LENGTH_SHORT).show();
            imagenBase64 = null;
        }
    }

    /** Valida campos y, si todo ok, hace la llamada al API */
    private void validarYEnviar() {
        // 1) Validar que no estén vacíos
        if (TextUtils.isEmpty(etNombre.getText().toString().trim())) {
            etNombre.setError("Requerido"); etNombre.requestFocus(); return;
        }
        if (TextUtils.isEmpty(etEspecie.getText().toString().trim())) {
            etEspecie.setError("Requerido"); etEspecie.requestFocus(); return;
        }
        if (TextUtils.isEmpty(etRaza.getText().toString().trim())) {
            etRaza.setError("Requerido"); etRaza.requestFocus(); return;
        }
        if (TextUtils.isEmpty(etEdad.getText().toString().trim())) {
            etEdad.setError("Requerido"); etEdad.requestFocus(); return;
        }
        if (TextUtils.isEmpty(etLocalidad.getText().toString().trim())) {
            etLocalidad.setError("Requerido"); etLocalidad.requestFocus(); return;
        }
        if (TextUtils.isEmpty(etSexo.getText().toString().trim())) {
            etSexo.setError("Requerido"); etSexo.requestFocus(); return;
        }
        if (TextUtils.isEmpty(etTamano.getText().toString().trim())) {
            etTamano.setError("Requerido"); etTamano.requestFocus(); return;
        }
        if (TextUtils.isEmpty(etDescripcion.getText().toString().trim())) {
            etDescripcion.setError("Requerido"); etDescripcion.requestFocus(); return;
        }
        if (imagenBase64 == null) {
            Toast.makeText(getContext(), "Selecciona una imagen", Toast.LENGTH_SHORT).show();
            return;
        }

        // 2) Construir objeto Animal
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences("user", Context.MODE_PRIVATE);
        int idUsuario = prefs.getInt("idUsuario", -1);

        Animal animal = new Animal();
        animal.setNombre     (etNombre.getText().toString().trim());
        animal.setEspecie    (etEspecie.getText().toString().trim());
        animal.setRaza       (etRaza.getText().toString().trim());
        animal.setEdad       (etEdad.getText().toString().trim());
        animal.setLocalidad  (etLocalidad.getText().toString().trim());
        animal.setSexo       (etSexo.getText().toString().trim());
        animal.setTamano     (etTamano.getText().toString().trim());
        animal.setDescripcion(etDescripcion.getText().toString().trim());
        animal.setImagen     (imagenBase64);
        animal.setIdUsuario  (idUsuario);

        // 3) Llamada al API
        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.addAnimal(animal).enqueue(new Callback<Mensaje>() {
            @Override
            public void onResponse(Call<Mensaje> call, Response<Mensaje> resp) {
                if (resp.isSuccessful() && resp.body()!=null && resp.body().isSuccess()) {
                    Toast.makeText(getContext(),
                            "Animal añadido con éxito",
                            Toast.LENGTH_SHORT).show();
                    // Volver al perfil automáticamente
                    requireActivity().getSupportFragmentManager().popBackStack();
                } else {
                    Toast.makeText(getContext(),
                            "Error al añadir animal",
                            Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Mensaje> call, Throwable t) {
                Toast.makeText(getContext(),
                        "Fallo en servidor: "+t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }
}
