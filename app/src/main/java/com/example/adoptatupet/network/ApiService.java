package com.example.adoptatupet.network;

import com.example.adoptatupet.models.Animal;
import com.example.adoptatupet.models.Usuario;
import com.example.adoptatupet.models.Mensaje;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

/**
 * ApiService define los endpoints de tu API REST.
 * Cada método mapea una llamada HTTP al backend bajo /api/.
 */
public interface ApiService {

    /**
     * Registro de un nuevo usuario.
     * POST https://…/api/register.php
     */
    @POST("register.php")
    Call<Mensaje> register(@Body Usuario usuario);

    /**
     * Login de un usuario existente.
     * POST https://…/api/login.php
     */
    @POST("login.php")
    Call<Mensaje> login(@Body Usuario usuario);

    /**
     * Actualiza los datos del usuario (incluida la foto).
     * POST https://…/api/update_usuario.php
     */
    @POST("update_usuario.php")
    Call<Mensaje> updateUsuario(@Body Usuario usuario);

    /**
     * Añade un nuevo animal a la base de datos.
     * POST https://…/api/add_animal.php
     */
    @POST("add_animal.php")
    Call<Mensaje> addAnimal(@Body Animal animal);

    /**
     * Borra un usuario dado su email.
     * POST https://…/api/delete_user.php
     */
    @POST("delete_user.php")
    Call<Mensaje> deleteUserEmail(@Body Map<String, String> body);

    /**
     * Obtiene todos los animales.
     * GET https://…/api/get_animals.php
     */
    @GET("get_animals.php")
    Call<List<Animal>> getAllAnimals();

    /**
     * Borra un animal dado su id.
     * POST https://…/api/delete_animal.php
     */
    @POST("delete_animal.php")
    Call<Mensaje> deleteAnimal(@Body Map<String, String> body);


    /** Actualiza un animal existente */
    @POST("update_animal.php")
    Call<Mensaje> updateAnimal(@Body Animal animal);
}
