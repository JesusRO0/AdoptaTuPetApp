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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * animalController: maneja todas las operaciones relacionadas con animales:
 * - Añadir un animal al backend.
 * - Obtener la lista completa de animales.
 * - Borrar animal.
 * - Actualizar animal.
 * - Obtener un animal por ID (nuevo).
 * - Mantener una caché local (SharedPreferences) para acelerar el arranque.
 */
public class animalController {

    private static final String PREFS_NAME      = "animal_cache_prefs";
    private static final String KEY_CACHED_LIST  = "key_cached_animals";

    private static animalController instance;
    private final ApiService api;
    private final SharedPreferences prefs;
    private final Gson gson;
    private final List<Animal> cachedAnimals = new ArrayList<>();

    private animalController(Context ctx) {
        this.api   = ApiClient.getClient().create(ApiService.class);
        this.prefs = ctx.getApplicationContext()
                .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson  = new Gson();
        loadCacheFromPrefs();
    }

    public static animalController getInstance(Context ctx) {
        if (instance == null) {
            instance = new animalController(ctx);
        }
        return instance;
    }

    /** Callback para operaciones de añadir, borrar o actualizar animal */
    public interface AnimalCallback {
        void onSuccess();
        void onError(String message);
    }

    /** Callback para operaciones de obtención de lista */
    public interface FetchCallback {
        void onSuccess(List<Animal> animales);
        void onError(String message);
    }

    /** Callback para obtener un solo Animal por ID */
    public interface AnimalByIdCallback {
        void onSuccess(Animal animal);
        void onError(String message);
    }

    public void addAnimal(Animal animal, AnimalCallback cb) {
        api.addAnimal(animal).enqueue(new Callback<Mensaje>() {
            @Override public void onResponse(Call<Mensaje> call, Response<Mensaje> resp) {
                if (resp.isSuccessful() && resp.body()!=null && resp.body().isSuccess()) {
                    cb.onSuccess();
                } else {
                    cb.onError(resp.body()!=null
                            ? resp.body().getMessage()
                            : "Error desconocido al añadir animal");
                }
            }
            @Override public void onFailure(Call<Mensaje> call, Throwable t) {
                cb.onError("Fallo en servidor: " + t.getMessage());
            }
        });
    }

    public void fetchAllAnimals(FetchCallback cb) {
        api.getAllAnimals().enqueue(new Callback<List<Animal>>() {
            @Override public void onResponse(Call<List<Animal>> call, Response<List<Animal>> resp) {
                if (resp.isSuccessful() && resp.body()!=null) {
                    cachedAnimals.clear();
                    cachedAnimals.addAll(resp.body());
                    saveCacheToPrefs(cachedAnimals);
                    if (cb!=null) cb.onSuccess(new ArrayList<>(cachedAnimals));
                } else if (cb!=null) {
                    cb.onError("Error al cargar animales");
                }
            }
            @Override public void onFailure(Call<List<Animal>> call, Throwable t) {
                if (cb!=null) cb.onError("Fallo en servidor: " + t.getMessage());
            }
        });
    }

    /** Obtiene un solo animal por su ID usando tu clase Mensaje como wrapper */
    public void fetchAnimalById(int idAnimal, AnimalByIdCallback cb) {
        api.getAnimalById(idAnimal)
                .enqueue(new Callback<Mensaje>() {
                    @Override
                    public void onResponse(Call<Mensaje> call, Response<Mensaje> resp) {
                        if (resp.isSuccessful() && resp.body() != null) {
                            Mensaje wrapper = resp.body();
                            if (wrapper.isSuccess() && wrapper.getAnimal() != null) {
                                cb.onSuccess(wrapper.getAnimal());
                            } else {
                                // Si el servidor devolvió success=false o no vino el animal
                                cb.onError(wrapper.getMessage() != null
                                        ? wrapper.getMessage()
                                        : "Error al obtener el animal");
                            }
                        } else {
                            cb.onError("Respuesta inválida del servidor");
                        }
                    }

                    @Override
                    public void onFailure(Call<Mensaje> call, Throwable t) {
                        cb.onError("Fallo en servidor: " + t.getMessage());
                    }
                });
    }
    public void deleteAnimal(int idAnimal, AnimalCallback cb) {
        Map<String,String> body = new HashMap<>();
        body.put("idAnimal", String.valueOf(idAnimal));
        api.deleteAnimal(body).enqueue(new Callback<Mensaje>() {
            @Override public void onResponse(Call<Mensaje> call, Response<Mensaje> resp) {
                if (resp.isSuccessful() && resp.body()!=null && resp.body().isSuccess()) {
                    cb.onSuccess();
                } else {
                    cb.onError(resp.body()!=null
                            ? resp.body().getMessage()
                            : "Error al borrar animal");
                }
            }
            @Override public void onFailure(Call<Mensaje> call, Throwable t) {
                cb.onError("Fallo en servidor: " + t.getMessage());
            }
        });
    }

    public void updateAnimal(Animal animal, AnimalCallback cb) {
        api.updateAnimal(animal).enqueue(new Callback<Mensaje>() {
            @Override public void onResponse(Call<Mensaje> call, Response<Mensaje> resp) {
                if (resp.isSuccessful() && resp.body()!=null && resp.body().isSuccess()) {
                    cb.onSuccess();
                } else {
                    cb.onError(resp.body()!=null
                            ? resp.body().getMessage()
                            : "Error desconocido al actualizar animal");
                }
            }
            @Override public void onFailure(Call<Mensaje> call, Throwable t) {
                cb.onError("Fallo en servidor: " + t.getMessage());
            }
        });
    }

    public List<Animal> getCachedAnimals() {
        return new ArrayList<>(cachedAnimals);
    }

    private void saveCacheToPrefs(List<Animal> list) {
        String json = gson.toJson(list);
        prefs.edit().putString(KEY_CACHED_LIST, json).apply();
    }

    private void loadCacheFromPrefs() {
        String json = prefs.getString(KEY_CACHED_LIST, null);
        if (json!=null) {
            try {
                Type type = new TypeToken<List<Animal>>(){}.getType();
                List<Animal> stored = gson.fromJson(json, type);
                if (stored!=null) {
                    cachedAnimals.clear();
                    cachedAnimals.addAll(stored);
                }
            } catch (Exception e) {
                prefs.edit().remove(KEY_CACHED_LIST).apply();
                cachedAnimals.clear();
            }
        }
    }
}
