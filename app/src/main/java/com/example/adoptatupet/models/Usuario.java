package com.example.adoptatupet.models;

public class Usuario {
    private String email;
    private String usuario;
    private String localidad;
    private String contrasena;

    // Constructor vacío necesario
    public Usuario() {}

    // Constructor para login
    public Usuario(String email, String contrasena) {
        this.email = email;
        this.contrasena = contrasena;
    }

    // Constructor completo
    public Usuario(String email, String usuario, String localidad, String contrasena) {
        this.email = email;
        this.usuario = usuario;
        this.localidad = localidad;
        this.contrasena = contrasena;
    }

    // Getters y setters
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getLocalidad() { return localidad; }
    public void setLocalidad(String localidad) { this.localidad = localidad; }

    public String getContrasena() { return contrasena; }
    public void setContrasena(String contrasena) { this.contrasena = contrasena; }
}
