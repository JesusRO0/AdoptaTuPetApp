package com.example.adoptatupet.models;

import com.google.gson.annotations.Expose;

public class Mensaje {
    @Expose
    private boolean success;

    @Expose
    private String message;

    @Expose
    private Usuario usuario;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Usuario getUsuario() {
        return usuario;
    }
}
