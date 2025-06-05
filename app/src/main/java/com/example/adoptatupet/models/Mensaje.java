package com.example.adoptatupet.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Modelo que agrupa:
 * 1) Campos genéricos de respuesta de servidor (success, message, usuario, animal).
 * 2) Campos específicos de un mensaje de foro.
 */
public class Mensaje {

    // ----- Campos genéricos de respuesta -----
    @Expose
    private boolean success;

    @Expose
    private String message;

    /** Para endpoints que devuelvan un usuario. */
    @Expose
    private Usuario usuario;

    /** Para get_animal_by_id.php */
    @Expose
    private Animal animal;

    // ----- Campos específicos de un mensaje del foro -----
    @Expose
    @SerializedName("idMensaje")
    private int idMensaje;

    @Expose
    @SerializedName("usuarioId")
    private int usuarioId;

    @Expose
    @SerializedName("usuarioNombre")
    private String usuarioNombre;

    /** Foto de perfil en Base64 */
    @Expose
    @SerializedName("fotoPerfil")
    private String fotoPerfil;

    @Expose
    @SerializedName("texto")
    private String texto;

    @Expose
    @SerializedName("fechaPublicacion")
    private String fechaPublicacion;

    /** Imagen adjunta en Base64 (puede ser null o cadena vacía) */
    @Expose
    @SerializedName("imagenMensaje")
    private String imagenMensaje;

    @SerializedName("likeCount")
    @Expose
    private int likeCount;

    @SerializedName("likedByUser")
    @Expose
    private boolean likedByUser;

    // ----- NUEVO CAMPO: contador de comentarios -----
    @SerializedName("commentCount")
    @Expose
    private int commentCount;

    // ----- NUEVO CAMPO: controla visibilidad del botón “eliminar” -----
    private boolean mostrarBotonEliminar = false;

    // ----------------- Getters y Setters -----------------

    // Campos genéricos
    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public void setUsuario(Usuario usuario) {
        this.usuario = usuario;
    }

    public Animal getAnimal() {
        return animal;
    }

    public void setAnimal(Animal animal) {
        this.animal = animal;
    }

    // Campos de foro
    public int getIdMensaje() {
        return idMensaje;
    }

    public void setIdMensaje(int idMensaje) {
        this.idMensaje = idMensaje;
    }

    public int getUsuarioId() {
        return usuarioId;
    }

    public void setUsuarioId(int usuarioId) {
        this.usuarioId = usuarioId;
    }

    public String getUsuarioNombre() {
        return usuarioNombre;
    }

    public void setUsuarioNombre(String usuarioNombre) {
        this.usuarioNombre = usuarioNombre;
    }

    public String getFotoPerfil() {
        return fotoPerfil;
    }

    public void setFotoPerfil(String fotoPerfil) {
        this.fotoPerfil = fotoPerfil;
    }

    public String getTexto() {
        return texto;
    }

    public void setTexto(String texto) {
        this.texto = texto;
    }

    public String getFechaPublicacion() {
        return fechaPublicacion;
    }

    public void setFechaPublicacion(String fechaPublicacion) {
        this.fechaPublicacion = fechaPublicacion;
    }

    public String getImagenMensaje() {
        return imagenMensaje;
    }

    public void setImagenMensaje(String imagenMensaje) {
        this.imagenMensaje = imagenMensaje;
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public boolean isLikedByUser() {
        return likedByUser;
    }

    public void setLikedByUser(boolean likedByUser) {
        this.likedByUser = likedByUser;
    }

    // Getter y Setter para el nuevo campo commentCount
    public int getCommentCount() {
        return commentCount;
    }

    public void setCommentCount(int commentCount) {
        this.commentCount = commentCount;
    }

    // Getter y Setter para controlar visibilidad del botón “eliminar”
    public boolean isMostrarBotonEliminar() {
        return mostrarBotonEliminar;
    }

    public void setMostrarBotonEliminar(boolean mostrarBotonEliminar) {
        this.mostrarBotonEliminar = mostrarBotonEliminar;
    }
}
