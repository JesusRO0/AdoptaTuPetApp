package com.example.adoptatupet.models;

import com.google.gson.annotations.Expose;

/**
 * Envoltorio genérico para respuestas de servidor:
 * - Usa los getters isSuccess() y getMessage() que ya tenías.
 * - Añadimos un campo Animal para el get_animal_by_id.
 */
public class Mensaje {
    @Expose
    private boolean success;

    @Expose
    private String message;

    /** Para login u otros endpoints que devuelvan usuario */
    @Expose
    private Usuario usuario;

    /** NUEVO: para get_animal_by_id.php */
    @Expose
    private Animal animal;

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    /** NUEVO: */
    public Animal getAnimal() {
        return animal;
    }
}
