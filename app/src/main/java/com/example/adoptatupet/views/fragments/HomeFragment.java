package com.example.adoptatupet.views.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.adoptatupet.R;
import com.example.adoptatupet.adapters.AnimalSliderAdapter;
import com.example.adoptatupet.models.Animal;
import com.example.adoptatupet.network.ApiClient;
import com.example.adoptatupet.network.ApiService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * HomeFragment muestra la pantalla principal, incluyendo:
 *  - Imagen de bienvenida
 *  - Texto descriptivo
 *  - Un slider (ViewPager2) que muestra los últimos 5 animales añadidos.
 *
 * Ahora incluye lógica de cacheo de la respuesta (SharedPreferences),
 * para que al abrir la aplicación se cargue primero la versión cacheada
 * y luego se actualice en segundo plano con los datos más recientes.
 */
public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";

    // Nombre del SharedPreferences y clave para guardar la lista de animales
    private static final String CACHE_PREFS       = "home_cache";
    private static final String KEY_CACHED_ANIMALS = "cachedAnimals";

    private ViewPager2 sliderViewPager;
    private AnimalSliderAdapter sliderAdapter;
    private Gson gson = new Gson();

    public HomeFragment() {
        // Constructor vacío requerido
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup   container,
                             @Nullable Bundle      savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 1) Referencia al ViewPager2 definido en fragment_home.xml
        sliderViewPager = view.findViewById(R.id.viewPagerAnimals);

        // 2) Crear adapter inicialmente con lista vacía.
        sliderAdapter = new AnimalSliderAdapter(requireContext(), /* inicialList= */ null);
        sliderViewPager.setAdapter(sliderAdapter);

        // 3) Cargar lista de animales desde cache y mostrarla inmediatamente.
        cargarAnimalesDesdeCache();

        // 4) Luego, en segundo plano, obtener los últimos animales del servidor
        refrescarAnimalesDesdeServidor();

        return view;
    }

    /**
     * Lee del SharedPreferences la última lista de animales guardada (JSON).
     * Si existe y no está vacía, la convierte a List<Animal> y la pasa al adapter.
     */
    private void cargarAnimalesDesdeCache() {
        SharedPreferences prefs = requireActivity()
                .getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        String jsonCache = prefs.getString(KEY_CACHED_ANIMALS, null);
        if (jsonCache != null && !jsonCache.isEmpty()) {
            Type listType = new TypeToken<List<Animal>>() {}.getType();
            List<Animal> listaCache = gson.fromJson(jsonCache, listType);
            if (listaCache != null && !listaCache.isEmpty()) {
                sliderAdapter.setAnimalList(listaCache);
            }
        }
    }

    /**
     * Realiza la llamada a la API para obtener los últimos 5 animales.
     * Si la llamada es exitosa, actualiza el adapter y guarda también la respuesta en cache.
     * En caso de error, se deja la lista cacheada que ya se haya cargado.
     */
    private void refrescarAnimalesDesdeServidor() {
        ApiService api = ApiClient.getClient().create(ApiService.class);
        Call<List<Animal>> call = api.getLatestAnimals(); // Debe corresponder a get_latest_animals.php
        call.enqueue(new Callback<List<Animal>>() {
            @Override
            public void onResponse(Call<List<Animal>> call, Response<List<Animal>> response) {
                if (!isAdded()) return;

                if (response.isSuccessful() && response.body() != null) {
                    List<Animal> lista = response.body();

                    // 1) Actualizar adapter con los datos más recientes
                    sliderAdapter.setAnimalList(lista);

                    // 2) Guardar la lista en cache (SharedPreferences)
                    guardarAnimalesEnCache(lista);
                } else {
                    Log.e(TAG, "Error al cargar animales: respuesta no exitosa");
                }
            }

            @Override
            public void onFailure(Call<List<Animal>> call, Throwable t) {
                if (!isAdded()) return;
                Log.e(TAG, "Error de red al obtener animales", t);
            }
        });
    }

    /**
     * Guarda la lista de animales en SharedPreferences como JSON,
     * para que las siguientes veces se cargue primero la versión cacheada.
     */
    private void guardarAnimalesEnCache(List<Animal> lista) {
        String jsonParaGuardar = gson.toJson(lista);
        SharedPreferences cachePrefs = requireActivity()
                .getSharedPreferences(CACHE_PREFS, Context.MODE_PRIVATE);
        cachePrefs.edit()
                .putString(KEY_CACHED_ANIMALS, jsonParaGuardar)
                .apply();
    }
}
