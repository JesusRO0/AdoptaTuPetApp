package com.example.adoptatupet.models;

public class Mensaje {
    private boolean success;
    private String message;
    private Usuario usuario;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Usuario getUsuario() { return usuario; }
}
