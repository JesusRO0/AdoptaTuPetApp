package com.example.adoptatupet.views.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.example.adoptatupet.R;
import com.example.adoptatupet.adapters.AnimalSliderAdapter;
import com.example.adoptatupet.models.Animal;
import com.example.adoptatupet.network.ApiClient;
import com.example.adoptatupet.network.ApiService;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * HomeFragment muestra la pantalla principal, incluyendo:
 *  - Slider (ViewPager2) con últimos 5 animales añadidos (clicable).
 *  - Botones "CONÓCENOS", "VER PERROS" y "VER GATOS" que cambian pestañas.
 *
 * Incorpora cache (SharedPreferences) y autoplay (cambia slide cada AUTO_PLAY_DELAY_MS).
 */
public class HomeFragment extends Fragment {
    private static final String TAG = "HomeFragment";

    // SharedPreferences para cachear lista de animales
    private static final String CACHE_PREFS        = "home_cache";
    private static final String KEY_CACHED_ANIMALS = "cachedAnimals";

    private ViewPager2 sliderViewPager;
    private AnimalSliderAdapter sliderAdapter;
    private Gson gson = new Gson();

    // Handler y Runnable para autoplay
    private Handler autoplayHandler;
    private Runnable autoplayRunnable;
    private static final long AUTO_PLAY_DELAY_MS = 5000; // 5 segundos

    public HomeFragment() {
        /* Constructor vacío requerido */
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup   container,
                             @Nullable Bundle      savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // 1) Referencia al ViewPager2
        sliderViewPager = view.findViewById(R.id.viewPagerAnimals);

        // 2) Crear adapter inicialmente con lista vacía
        sliderAdapter = new AnimalSliderAdapter(requireContext(), /* inicialList= */ null);
        sliderViewPager.setAdapter(sliderAdapter);

        // 3) Configurar clic en cada slide para ir a pestaña "Adopta"
        sliderAdapter.setOnItemClickListener(new AnimalSliderAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Animal clickedAnimal) {
                BottomNavigationView bottomNav = requireActivity()
                        .findViewById(R.id.bottomNavigationView);
                // Asumiendo que el ID de la pestaña "Adopta" es nav_adopta
                bottomNav.setSelectedItemId(R.id.nav_adopta);
            }
        });

        // 4) Cargar lista de animales desde cache y mostrarla inmediatamente
        cargarAnimalesDesdeCache();

        // 5) Luego, en segundo plano, obtener últimos animales del servidor
        refrescarAnimalesDesdeServidor();

        // 6) Configurar autoplay: cambia de página cada AUTO_PLAY_DELAY_MS ms
        autoplayHandler = new Handler(Looper.getMainLooper());
        autoplayRunnable = new Runnable() {
            @Override
            public void run() {
                int itemCount = sliderAdapter.getItemCount();
                if (itemCount > 1 && isAdded()) {
                    int nextItem = (sliderViewPager.getCurrentItem() + 1) % itemCount;
                    sliderViewPager.setCurrentItem(nextItem, true);
                    autoplayHandler.postDelayed(this, AUTO_PLAY_DELAY_MS);
                }
            }
        };

        // 7) Botón "CONÓCENOS" → pestaña "Contacto"
        Button conocenosButton = view.findViewById(R.id.conocenos_button);
        conocenosButton.setOnClickListener(v -> {
            BottomNavigationView bottomNav = requireActivity()
                    .findViewById(R.id.bottomNavigationView);
            bottomNav.setSelectedItemId(R.id.nav_contacto);
        });

        // 8) Botón "VER PERROS" → pestaña "Adopta"
        Button verPerrosButton = view.findViewById(R.id.ver_perros_button);
        verPerrosButton.setOnClickListener(v -> {
            BottomNavigationView bottomNav = requireActivity()
                    .findViewById(R.id.bottomNavigationView);
            bottomNav.setSelectedItemId(R.id.nav_adopta);
        });

        // 9) Botón "VER GATOS" → pestaña "Adopta"
        Button verGatosButton = view.findViewById(R.id.ver_gatos_button);
        verGatosButton.setOnClickListener(v -> {
            BottomNavigationView bottomNav = requireActivity()
                    .findViewById(R.id.bottomNavigationView);
            bottomNav.setSelectedItemId(R.id.nav_adopta);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Iniciar autoplay cuando el fragment esté visible
        autoplayHandler.postDelayed(autoplayRunnable, AUTO_PLAY_DELAY_MS);
    }

    @Override
    public void onPause() {
        super.onPause();
        // Detener autoplay al pausar el fragment
        autoplayHandler.removeCallbacks(autoplayRunnable);
    }

    /**
     * Lee del SharedPreferences la última lista de animales (JSON).
     * Si existe, convierte a List<Animal> y actualiza el adapter.
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
     * Hace la llamada a la API para obtener los 5 últimos animales.
     * Actualiza el adapter y guarda en cache. Si falla, mantiene cache anterior.
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

                    // 1) Actualizar adapter con datos recientes
                    sliderAdapter.setAnimalList(lista);

                    // 2) Guardar lista en cache (SharedPreferences)
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
     * Guarda la lista de animales en SharedPreferences como JSON
     * para que la próxima carga use la versión cacheada primero.
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
