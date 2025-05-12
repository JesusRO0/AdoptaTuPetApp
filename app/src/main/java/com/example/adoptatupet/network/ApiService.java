package com.example.adoptatupet.network;

import com.example.adoptatupet.models.Usuario;
import com.example.adoptatupet.models.Mensaje;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Headers;
import retrofit2.http.POST;

public interface ApiService {

    @Headers("Content-Type: application/json")
    @POST("login.php")
    Call<Mensaje> login(@Body Usuario usuario);

    @Headers("Content-Type: application/json")
    @POST("register.php")
    Call<Mensaje> register(@Body Usuario usuario);
}
