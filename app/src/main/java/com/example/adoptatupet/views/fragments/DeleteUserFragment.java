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
import com.example.adoptatupet.views.MainActivity;

import java.util.Collections;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DeleteUserFragment extends Fragment {

    private EditText etEmailEliminar;
    private Button   btnEliminarUsuario, btnVolverEliminar;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
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
        // mostrar rueda
        ((MainActivity) requireActivity()).showLoading();

        ApiService api = ApiClient.getClient().create(ApiService.class);
        api.deleteUserEmail(Collections.singletonMap("email", email))
                .enqueue(new Callback<Mensaje>() {
                    @Override public void onResponse(Call<Mensaje> call, Response<Mensaje> resp) {
                        // ocultar rueda
                        ((MainActivity) requireActivity()).hideLoading();
                        if (resp.isSuccessful() && resp.body() != null && resp.body().isSuccess()) {
                            Toast.makeText(getContext(),
                                    "Usuario eliminado", Toast.LENGTH_SHORT).show();
                            requireActivity().getSupportFragmentManager().popBackStack();
                        } else {
                            String msg = resp.body()!=null
                                    ? resp.body().getMessage()
                                    : "Error al eliminar";
                            Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onFailure(Call<Mensaje> call, Throwable t) {
                        ((MainActivity) requireActivity()).hideLoading();
                        Toast.makeText(getContext(),
                                "Fallo en servidor: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
