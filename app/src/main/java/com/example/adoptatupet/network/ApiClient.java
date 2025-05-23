package com.example.adoptatupet.network;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * ApiClient gestiona la instancia singleton de Retrofit para
 * comunicarse con el backend en https://adoptatupetapp-backend.onrender.com/api/.
 * Mantiene la configuración de timeouts, logging y Gson con @Expose.
 */
public class ApiClient {
    // URL base apuntando al directorio /api/ en tu servidor
    private static final String BASE_URL = "https://adoptatupetapp-backend.onrender.com/api/";
    private static Retrofit retrofit;

    private ApiClient() {
        // Evitar instanciación
    }

    /**
     * Devuelve la instancia singleton de Retrofit.
     * Configura:
     *  - OkHttpClient con logging de cuerpo (BODY) y timeouts de 60s
     *  - Gson que incluye solo campos con @Expose
     *  - ConverterFactory para JSON
     */
    public static synchronized Retrofit getClient() {
        if (retrofit == null) {
            // Interceptor para ver peticiones y respuestas en logcat
            HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
            logging.setLevel(HttpLoggingInterceptor.Level.BODY);

            // Cliente HTTP con timeouts razonables
            OkHttpClient client = new OkHttpClient.Builder()
                    .addInterceptor(logging)
                    .connectTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build();

            // Gson que respeta solo campos marcados con @Expose
            Gson gson = new GsonBuilder()
                    .excludeFieldsWithoutExposeAnnotation()
                    .create();

            // Construcción de Retrofit con la configuración anterior
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create(gson))
                    .build();
        }
        return retrofit;
    }
}
