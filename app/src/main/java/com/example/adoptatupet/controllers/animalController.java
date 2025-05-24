package com.example.adoptatupet.controllers;

import android.content.Context;

import com.example.adoptatupet.models.Animal;
import com.example.adoptatupet.models.Mensaje;
import com.example.adoptatupet.network.ApiClient;
import com.example.adoptatupet.network.ApiService;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class animalController {
    private static animalController instance;
    private final ApiService api;
    private final Context ctx;

    private animalController(Context ctx) {
        this.ctx = ctx.getApplicationContext();
        this.api = ApiClient.getClient().create(ApiService.class);
    }

    public static animalController getInstance(Context ctx) {
        if (instance == null) {
            instance = new animalController(ctx);
        }
        return instance;
    }

    public interface AnimalCallback {
        void onSuccess();
        void onError(String message);
    }

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
}
