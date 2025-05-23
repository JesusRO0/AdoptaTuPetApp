package com.example.adoptatupet.views.fragments;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.adoptatupet.R;
import com.example.adoptatupet.models.Mensaje;
import com.example.adoptatupet.network.ApiClient;
import com.example.adoptatupet.network.ApiService;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeleteUserFragment extends Fragment {

    private EditText etEmailEliminar;
    private Button   btnEliminarUsuario, btnVolverEliminar;
    private final Gson gson = new Gson();

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup   container,
                             @Nullable Bundle      savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_delete_user, container, false);

        etEmailEliminar    = view.findViewById(R.id.etEmailEliminar);
        btnEliminarUsuario = view.findViewById(R.id.btnEliminarUsuario);
        btnVolverEliminar  = view.findViewById(R.id.btnVolverEliminar);

        btnEliminarUsuario.setOnClickListener(v -> {
            String email = etEmailEliminar.getText().toString().trim();
            if (TextUtils.isEmpty(email)) {
                etEmailEliminar.setError("Requerido");
                etEmailEliminar.requestFocus();
                return;
            }
            eliminarUsuario(email);
        });

        btnVolverEliminar.setOnClickListener(v ->
                requireActivity().getSupportFragmentManager().popBackStack()
        );

        return view;
    }

    private void eliminarUsuario(String email) {
        ApiService api = ApiClient.getClient().create(ApiService.class);

        Map<String,String> body = Collections.singletonMap("email", email);
        api.deleteUserEmail(body).enqueue(new Callback<Mensaje>() {
            @Override public void onResponse(Call<Mensaje> call, Response<Mensaje> resp) {
                // Si el body viene en 200 OK o en error HTTP, lo intentamos parsear
                if (resp.isSuccessful() && resp.body() != null) {
                    // 200 OK con JSON
                    mostrarMensaje(resp.body());
                } else {
                    // Error HTTP: obtenemos el errorBody y parseamos
                    ResponseBody err = resp.errorBody();
                    if (err != null) {
                        try {
                            String json = err.string();
                            Mensaje m = gson.fromJson(json, Mensaje.class);
                            mostrarMensaje(m);
                        } catch (IOException | JsonSyntaxException e) {
                            Toast.makeText(getContext(),
                                    "Error al procesar la respuesta",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(getContext(),
                                "Error inesperado",
                                Toast.LENGTH_SHORT).show();
                    }
                }
            }
            @Override public void onFailure(Call<Mensaje> call, Throwable t) {
                Toast.makeText(getContext(),
                        "Fallo en servidor: " + t.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarMensaje(Mensaje m) {
        // Mostrar siempre el mensaje
        Toast.makeText(getContext(),
                m.getMessage(),
                Toast.LENGTH_SHORT).show();
        // Si ha sido éxito, volvemos atrás
        if (m.isSuccess()) {
            requireActivity().getSupportFragmentManager().popBackStack();
        }
    }
}
