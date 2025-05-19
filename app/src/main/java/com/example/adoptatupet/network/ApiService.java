package com.example.adoptatupet.network;

import com.example.adoptatupet.models.Animal;
import com.example.adoptatupet.models.Usuario;
import com.example.adoptatupet.models.Mensaje;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

public interface ApiService {

    @POST("register.php")
    Call<Mensaje> register(@Body Usuario usuario);

    @POST("login.php")
    Call<Mensaje> login(@Body Usuario usuario);

    @POST("update_usuario.php")
    Call<Mensaje> updateUsuario(@Body Usuario usuario);

    @POST("add_animal.php")
    Call<Mensaje> addAnimal(@Body Animal animal);
}
