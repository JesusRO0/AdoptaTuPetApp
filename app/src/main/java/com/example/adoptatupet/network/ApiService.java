package com.example.adoptatupet.network;

import com.example.adoptatupet.models.Animal;
import com.example.adoptatupet.models.Comment;
import com.example.adoptatupet.models.Usuario;
import com.example.adoptatupet.models.Mensaje;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * ApiService define los endpoints de tu API REST.
 * Cada método mapea una llamada HTTP al backend.
 */
public interface ApiService {

    /**
     * Registro de un nuevo usuario.
     * POST https://…/api/register.php
     */
    @POST("api/register.php")
    Call<Mensaje> register(@Body Usuario usuario);

    /**
     * Login de un usuario existente.
     * POST https://…/api/login.php
     */
    @POST("api/login.php")
    Call<Mensaje> login(@Body Usuario usuario);

    /**
     * Actualiza los datos del usuario (incluida la foto).
     * POST https://…/api/update_usuario.php
     */
    @POST("api/update_usuario.php")
    Call<Mensaje> updateUsuario(@Body Usuario usuario);

    /**
     * Añade un nuevo animal a la base de datos.
     * POST https://…/api/add_animal.php
     */
    @POST("api/add_animal.php")
    Call<Mensaje> addAnimal(@Body Animal animal);

    /**
     * Borra un usuario dado su email.
     * POST https://…/api/delete_user.php
     */
    @POST("api/delete_user.php")
    Call<Mensaje> deleteUserEmail(@Body Map<String, String> body);

    /**
     * Obtiene todos los animales.
     * GET https://…/api/get_animals.php
     */
    @GET("api/get_animals.php")
    Call<List<Animal>> getAllAnimals();

    /**
     * Borra un animal dado su id.
     * POST https://…/api/delete_animal.php
     */
    @POST("api/delete_animal.php")
    Call<Mensaje> deleteAnimal(@Body Map<String, String> body);

    /**
     * Actualiza un animal existente.
     * POST https://…/api/update_animal.php
     */
    @POST("api/update_animal.php")
    Call<Mensaje> updateAnimal(@Body Animal animal);

    /**
     * Obtiene un animal por su id.
     * GET https://…/api/get_animal_by_id.php?idAnimal=…
     */
    @GET("api/get_animal_by_id.php")
    Call<Mensaje> getAnimalById(@Query("idAnimal") int idAnimal);

    /**
     * Obtiene todos los mensajes del foro.
     * GET https://…/api/get_mensajes.php?idUsuario=…
     */
    @GET("api/get_mensajes.php")
    Call<List<Mensaje>> getMensajesForo(@Query("idUsuario") Integer idUsuario);

    /**
     * Crea un nuevo mensaje en el foro.
     * POST https://…/api/post_mensaje.php
     */
    @POST("api/post_mensaje.php")
    Call<Mensaje> postMensaje(@Body Mensaje nuevo);

    /**
     * Agrega un like a un post.
     * POST https://…/api/like_post.php
     */
    @POST("api/like_post.php")
    Call<Mensaje> likePost(@Body Map<String, Integer> body);

    /**
     * Quita un like a un post.
     * POST https://…/api/unlike_post.php
     */
    @POST("api/unlike_post.php")
    Call<Mensaje> unlikePost(@Body Map<String, Integer> body);

    /**
     * Borra un comentario de un post.
     * POST https://…/api/delete_comentario.php
     */
    @POST("api/delete_comentario.php")
    Call<Mensaje> deleteComentario(@Body Map<String, Integer> body);

    /**
     * Obtiene los datos de un usuario dado su ID.
     * GET https://…/api/get_usuario_by_id.php?idUsuario=…
     */
    @GET("api/get_usuario_by_id.php")
    Call<Usuario> getUsuarioById(@Query("idUsuario") int idUsuario);

    /**
     * Obtiene todos los mensajes de un usuario concreto.
     * GET https://…/api/get_mensajes_usuario.php?idUsuario=…
     */
    @GET("api/get_mensajes_usuario.php")
    Call<List<Mensaje>> getMensajesUsuario(@Query("idUsuario") int idUsuario);

    /**
     * Envía un comentario a un post existente.
     * POST https://…/api/post_comentario.php
     */
    @POST("api/post_comentario.php")
    Call<Mensaje> postComentario(@Body Map<String, Object> body);

    /**
     * Obtiene todos los comentarios de un post dado su id.
     * GET https://…/api/get_comentarios.php?idPost=…
     */
    @GET("api/get_comentarios.php")
    Call<List<Comment>> getComentarios(@Query("idPost") int idPost);

    @POST("api/delete_post.php")
    Call<Mensaje> deletePost(@Body Map<String, Integer> body);

    /**
     * Obtiene los últimos N animales (se pasará limit=5 desde Home).
     * Ejemplo de llamada: GET /api/get_latest_animals.php?limit=5
     */
    @GET("api/get_latest_animals.php")
    Call<List<Animal>> getLatestAnimals();
}
