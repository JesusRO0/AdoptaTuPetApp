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

    // Constructor para login (solo email y contraseña)
    public Usuario(String email, String contrasena) {
        this.email = email;
        this.contrasena = contrasena;
    }

    // Constructor para registro (todos los campos menos id)
    public Usuario(String email, String usuario, String localidad, String contrasena) {
        this.email = email;
        this.usuario = usuario;
        this.localidad = localidad;
        this.contrasena = contrasena;
    }

    // Getters
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

    // Setters si los necesitas
}
