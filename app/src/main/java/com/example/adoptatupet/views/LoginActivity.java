package com.example.adoptatupet.views;

import com.example.adoptatupet.R;
import com.example.adoptatupet.controllers.usuarioController;
import com.example.adoptatupet.models.Mensaje;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import retrofit2.Call;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "user";

    private EditText nameET, emailET, passET;
    private TextView toggleTV;
    private Button btnLoginRegister;
    private ProgressDialog loadingDialog;
    private boolean isLoginMode = true;
    private usuarioController usuarioController;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 0) Si ya hay sesión, saltamos al Main y cerramos este Activity
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        if (prefs.getBoolean("loggedIn", false)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        // Inflamos tu dialog_login.xml como layout completo
        setContentView(R.layout.activity_login);

        usuarioController = usuarioController.getInstance(this);

        nameET   = findViewById(R.id.nameEditText);
        emailET  = findViewById(R.id.emailEditText);
        passET   = findViewById(R.id.passwordEditText);
        toggleTV = findViewById(R.id.toggleTextView);
        btnLoginRegister = findViewById(R.id.btnLoginRegister);

        // Al pulsar Entrar / Registrar
        btnLoginRegister.setOnClickListener(v -> {
            String email = emailET.getText().toString().trim();
            String pass  = passET.getText().toString().trim();
            if (email.isEmpty() || pass.isEmpty() ||
                    (!isLoginMode && nameET.getText().toString().trim().isEmpty())) {
                Toast.makeText(this, "Completa todos los campos", Toast.LENGTH_SHORT).show();
                return;
            }

            showLoading();
            if (isLoginMode) {
                // —— LOGIN ——
                usuarioController.login(email, pass, new retrofit2.Callback<Mensaje>() {
                    @Override public void onResponse(Call<Mensaje> call, Response<Mensaje> resp) {
                        hideLoading();
                        if (resp.isSuccessful() && resp.body()!=null && resp.body().isSuccess()) {
                            // Marcamos sesión iniciada
                            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                                    .edit()
                                    .putBoolean("loggedIn", true)
                                    .apply();
                            startMainActivity();
                        } else {
                            Toast.makeText(LoginActivity.this, "Credenciales inválidas", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onFailure(Call<Mensaje> call, Throwable t) {
                        hideLoading();
                        Toast.makeText(LoginActivity.this, "Error en servidor", Toast.LENGTH_SHORT).show();
                    }
                });
            } else {
                // —— REGISTRO ——
                String nombre = nameET.getText().toString().trim();
                usuarioController.register(email, nombre, "Sin localidad", pass,
                        new retrofit2.Callback<Mensaje>() {
                            @Override public void onResponse(Call<Mensaje> call, Response<Mensaje> resp) {
                                hideLoading();
                                if (resp.isSuccessful() && resp.body()!=null && resp.body().isSuccess()) {
                                    Toast.makeText(LoginActivity.this, "Registro exitoso, inicia sesión", Toast.LENGTH_SHORT).show();
                                    switchMode(true);
                                } else {
                                    Toast.makeText(LoginActivity.this, "Error al registrar", Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override public void onFailure(Call<Mensaje> call, Throwable t) {
                                hideLoading();
                                Toast.makeText(LoginActivity.this, "Error en servidor", Toast.LENGTH_SHORT).show();
                            }
                        }
                );
            }
        });

        // Alternar entre Login y Registro
        toggleTV.setOnClickListener(v -> switchMode(!isLoginMode));
    }

    private void switchMode(boolean toLogin) {
        isLoginMode = toLogin;
        nameET.setVisibility(toLogin ? View.GONE : View.VISIBLE);
        btnLoginRegister.setText(toLogin ? "Entrar" : "Registrar");
        toggleTV.setText(toLogin
                ? "¿No tienes cuenta? Regístrate"
                : "¿Ya tienes cuenta? Inicia sesión"
        );
    }

    private void showLoading() {
        if (loadingDialog == null) {
            loadingDialog = new ProgressDialog(this);
            loadingDialog.setMessage("Cargando...");
            loadingDialog.setCancelable(false);
        }
        loadingDialog.show();
    }
    private void hideLoading() {
        if (loadingDialog != null && loadingDialog.isShowing())
            loadingDialog.dismiss();
    }

    private void startMainActivity() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
