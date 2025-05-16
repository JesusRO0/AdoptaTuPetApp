package com.example.adoptatupet.models;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Usuario {

    @Expose
    private int idUsuario;

    @Expose
    private String email;

    @Expose
    private String usuario;

    @Expose
    private String localidad;

    @Expose
    private String contrasena;

    @Expose
    @SerializedName("fotoPerfil")
    private String fotoPerfil;

    // Constructores
    public Usuario() {
    }

    public Usuario(String email, String contrasena) {
        this.email = email;
        this.contrasena = contrasena;
    }

    public Usuario(String email, String usuario, String localidad, String contrasena) {
        this.email = email;
        this.usuario = usuario;
        this.localidad = localidad;
        this.contrasena = contrasena;
    }

    // Getters
    public int getIdUsuario() {
        return idUsuario;
    }

    public String getEmail() {
        return email;
    }

    public String getUsuario() {
        return usuario;
    }

    public String getLocalidad() {
        return localidad;
    }

    public String getContrasena() {
        return contrasena;
    }

    public String getFotoPerfil() {
        return fotoPerfil;
    }

    // Setters
    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public void setLocalidad(String localidad) {
        this.localidad = localidad;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public void setFotoPerfil(String fotoPerfil) {
        this.fotoPerfil = fotoPerfil;
    }
}
