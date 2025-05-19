package com.example.adoptatupet.controllers;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.adoptatupet.models.Mensaje;
import com.example.adoptatupet.models.Usuario;
import com.example.adoptatupet.network.ApiClient;
import com.example.adoptatupet.network.ApiService;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Controller que centraliza toda la lógica de usuario:
 * login, register, updateProfile, carga/guardado en prefs y logout.
 */
public class UsuarioController {
    private static final String PREFS_NAME = "user";
    private static UsuarioController instance;
    private final SharedPreferences prefs;
    private final ApiService api;

    private UsuarioController(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        api   = ApiClient.getClient().create(ApiService.class);
    }

    /**
     * Devuelve la instancia singleton del controller.
     */
    public static UsuarioController getInstance(Context ctx) {
        if (instance == null) {
            instance = new UsuarioController(ctx.getApplicationContext());
        }
        return instance;
    }

    /**
     * Intenta iniciar sesión con email+password.
     * - Si el login es exitoso, guarda el Usuario en SharedPreferences.
     * - Reenvía la respuesta (Callback<Mensaje>) al caller.
     */
    public void login(String email,
                      String password,
                      Callback<Mensaje> callback) {

        Usuario req = new Usuario(email, password);
        Call<Mensaje> call = api.login(req);

        call.enqueue(new Callback<Mensaje>() {
            @Override
            public void onResponse(Call<Mensaje> call, Response<Mensaje> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Guardar usuario en prefs tras login exitoso
                    saveToPrefs(response.body().getUsuario());
                }
                // Reenvía la respuesta original (éxito o fallo lógico)
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<Mensaje> call, Throwable t) {
                // Reenvía fallo de red o servidor
                callback.onFailure(call, t);
            }
        });
    }

    /**
     * Registra un nuevo usuario.
     * - No inicia sesión automáticamente.
     * - Reenvía el Callback<Mensaje> al caller.
     */
    public void register(String email,
                         String nombreUsuario,
                         String localidad,
                         String password,
                         String fotoPerfilBase64,
                         Callback<Mensaje> callback) {

        // Construye el objeto Usuario para el POST
        Usuario nuevo = new Usuario(email, nombreUsuario, localidad, password);
        nuevo.setFotoPerfil(fotoPerfilBase64);

        Call<Mensaje> call = api.register(nuevo);
        call.enqueue(new Callback<Mensaje>() {
            @Override
            public void onResponse(Call<Mensaje> call, Response<Mensaje> response) {
                // Simplemente reenvía la respuesta (éxito o mensaje de error)
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<Mensaje> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    /**
     * Actualiza el perfil del usuario ya logueado.
     * - Si la actualización es exitosa, actualiza SharedPreferences.
     * - Reenvía el Callback<Mensaje> al caller.
     */
    public void updateProfile(String nombreUsuario,
                              String localidad,
                              String email,
                              String password,            // Puede ser hash o texto plano
                              String fotoPerfilBase64,
                              Callback<Mensaje> callback) {

        // Construye el objeto Usuario con los campos nuevos
        Usuario u = new Usuario();
        u.setIdUsuario(prefs.getInt("idUsuario", -1));
        u.setUsuario(nombreUsuario);
        u.setLocalidad(localidad);
        u.setEmail(email);
        u.setContrasena(password);
        u.setFotoPerfil(fotoPerfilBase64);

        Call<Mensaje> call = api.updateUsuario(u);
        call.enqueue(new Callback<Mensaje>() {
            @Override
            public void onResponse(Call<Mensaje> call, Response<Mensaje> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Si la actualización en servidor fue exitosa, refresca prefs
                    saveToPrefs(u);
                }
                // Reenvía la respuesta original
                callback.onResponse(call, response);
            }

            @Override
            public void onFailure(Call<Mensaje> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }

    /**
     * Carga el Usuario almacenado en SharedPreferences.
     * @return Usuario o null si no hay sesión activa.
     */
    public Usuario loadFromPrefs() {
        if (!prefs.getBoolean("isLoggedIn", false)) {
            return null;
        }
        Usuario u = new Usuario();
        u.setIdUsuario (prefs.getInt   ("idUsuario", -1));
        u.setUsuario   (prefs.getString("usuario",   ""));
        u.setEmail     (prefs.getString("email",     ""));
        u.setLocalidad (prefs.getString("localidad", ""));
        u.setContrasena(prefs.getString("contrasena",""));
        u.setFotoPerfil(prefs.getString("fotoPerfil", null));
        return u;
    }

    /**
     * Cierra la sesión borrando SharedPreferences.
     */
    public void logout() {
        prefs.edit().clear().apply();
    }

    /**
     * Guarda en SharedPreferences los campos del Usuario dado.
     */
    private void saveToPrefs(Usuario u) {
        SharedPreferences.Editor e = prefs.edit();
        e.putBoolean("isLoggedIn", true);
        e.putInt   ("idUsuario",   u.getIdUsuario());
        e.putString("usuario",     u.getUsuario());
        e.putString("email",       u.getEmail());
        e.putString("contrasena",  u.getContrasena());
        e.putString("localidad",   u.getLocalidad());
        e.putString("fotoPerfil",  u.getFotoPerfil());
        e.apply();
    }
    public void register(String email,
                         String nombre,
                         String localidad,
                         String password,
                         retrofit2.Callback<Mensaje> callback) {
        Usuario req = new Usuario(email, nombre, localidad, password);
        api.register(req).enqueue(new Callback<Mensaje>() {
            @Override
            public void onResponse(Call<Mensaje> call, Response<Mensaje> response) {
                if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                    // Opcional: guardar en prefs tal cual lo haces en login
                    // saveToPrefs(response.body().getUsuario());
                    callback.onResponse(call, response);
                } else {
                    callback.onFailure(call, new Exception(
                            response.body() != null
                                    ? response.body().getMessage()
                                    : "Error en registro"
                    ));
                }
            }
            @Override
            public void onFailure(Call<Mensaje> call, Throwable t) {
                callback.onFailure(call, t);
            }
        });
    }
}
