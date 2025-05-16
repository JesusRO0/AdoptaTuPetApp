package com.example.adoptatupet.models;

import com.google.gson.annotations.Expose;

public class Usuario {

    private int idUsuario;

    @Expose
    private String email;

    @Expose
    private String usuario;

    @Expose
    private String localidad;

    @Expose
    private String contrasena;

    // 🔧 Constructor vacío necesario para Retrofit, Firebase o uso manual
    public Usuario() {
    }

    // Constructor para login
    public Usuario(String email, String contrasena) {
        this.email = email;
        this.contrasena = contrasena;
    }

    // Constructor para registro
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

    // ✅ Setters
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
}
