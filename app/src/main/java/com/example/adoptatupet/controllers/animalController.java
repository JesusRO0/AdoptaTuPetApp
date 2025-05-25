package com.example.adoptatupet.controllers;

import android.content.Context;
import android.content.SharedPreferences;

import com.example.adoptatupet.models.Animal;
import com.example.adoptatupet.models.Mensaje;
import com.example.adoptatupet.network.ApiClient;
import com.example.adoptatupet.network.ApiService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * animalController: maneja todas las operaciones relacionadas con animales:
 * - Añadir un animal al backend.
 * - Obtener la lista completa de animales.
 * - Mantener una caché local (SharedPreferences) para acelerar el arranque.
 */
public class animalController {

    // Clave de SharedPreferences donde guardamos el JSON de animales
    private static final String PREFS_NAME        = "animal_cache_prefs";
    private static final String KEY_CACHED_LIST   = "key_cached_animals";

    private static animalController instance;
    private final ApiService api;
    private final SharedPreferences prefs;
    private final Gson gson;

    // Caché en memoria de la última lista obtenida
    private final List<Animal> cachedAnimals = new ArrayList<>();

    /**
     * Constructor privado. Inicializa API, SharedPreferences y Gson,
     * y carga la caché previa (si existe).
     */
    private animalController(Context ctx) {
        this.api   = ApiClient.getClient().create(ApiService.class);
        this.prefs = ctx.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson  = new Gson();
        loadCacheFromPrefs();
    }

    /**
     * Devuelve la instancia singleton del controller.
     */
    public static animalController getInstance(Context ctx) {
        if (instance == null) {
            instance = new animalController(ctx);
        }
        return instance;
    }

    /** Callback para operaciones de añadir animal */
    public interface AnimalCallback {
        void onSuccess();
        void onError(String message);
    }

    /** Callback para operaciones de obtención de lista */
    public interface FetchCallback {
        void onSuccess(List<Animal> animales);
        void onError(String message);
    }

    /**
     * Añade un nuevo animal al servidor.
     * @param animal Objeto a enviar.
     * @param cb     Callback de respuesta.
     */
    public void addAnimal(Animal animal, AnimalCallback cb) {
        api.addAnimal(animal).enqueue(new Callback<Mensaje>() {
            @Override
            public void onResponse(Call<Mensaje> call, Response<Mensaje> resp) {
                if (resp.isSuccessful() && resp.body()!=null && resp.body().isSuccess()) {
                    cb.onSuccess();
                } else {
                    cb.onError(resp.body()!=null
                            ? resp.body().getMessage()
                            : "Error desconocido al añadir animal");
                }
            }
            @Override
            public void onFailure(Call<Mensaje> call, Throwable t) {
                cb.onError("Fallo en servidor: " + t.getMessage());
            }
        });
    }

    /**
     * Obtiene todos los animales del servidor y actualiza la caché.
     * @param cb Callback con la lista o el error.
     */
    public void fetchAllAnimals(FetchCallback cb) {
        api.getAllAnimals().enqueue(new Callback<List<Animal>>() {
            @Override
            public void onResponse(Call<List<Animal>> call, Response<List<Animal>> resp) {
                if (resp.isSuccessful() && resp.body() != null) {
                    // 1) Actualizar caché en memoria
                    cachedAnimals.clear();
                    cachedAnimals.addAll(resp.body());
                    // 2) Persistir en SharedPreferences
                    saveCacheToPrefs(cachedAnimals);
                    // 3) Devolver copia al UI
                    if (cb != null) cb.onSuccess(new ArrayList<>(cachedAnimals));
                } else {
                    if (cb != null) cb.onError("Error al cargar animales");
                }
            }
            @Override
            public void onFailure(Call<List<Animal>> call, Throwable t) {
                if (cb != null) cb.onError("Fallo en servidor: " + t.getMessage());
            }
        });
    }

    /**
     * Devuelve inmediatamente la lista cacheada en memoria.
     * @return Lista de animales (copia).
     */
    public List<Animal> getCachedAnimals() {
        return new ArrayList<>(cachedAnimals);
    }

    /**
     * Guarda la lista en formato JSON en SharedPreferences.
     */
    private void saveCacheToPrefs(List<Animal> list) {
        String json = gson.toJson(list);
        prefs.edit()
                .putString(KEY_CACHED_LIST, json)
                .apply();
    }

    /**
     * Carga la lista cacheada desde SharedPreferences al iniciar.
     * Si el JSON está corrupto o ausente, inicializa como lista vacía.
     */
    private void loadCacheFromPrefs() {
        String json = prefs.getString(KEY_CACHED_LIST, null);
        if (json != null) {
            try {
                Type type = new TypeToken<List<Animal>>(){}.getType();
                List<Animal> stored = gson.fromJson(json, type);
                if (stored != null) {
                    cachedAnimals.clear();
                    cachedAnimals.addAll(stored);
                }
            } catch (Exception e) {
                // Si hay error, limpiar entrada corrupta
                prefs.edit().remove(KEY_CACHED_LIST).apply();
                cachedAnimals.clear();
            }
        }
    }
}
