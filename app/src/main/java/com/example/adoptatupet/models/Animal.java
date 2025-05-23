package com.example.adoptatupet.models;

import com.google.gson.annotations.Expose;

public class Animal {
    @Expose
    private int idAnimal;       // Se ignora al crear (AUTO_INCREMENT)
    @Expose
    private String nombre;
    @Expose
    private String especie;
    @Expose
    private String raza;
    @Expose
    private String edad;
    @Expose
    private String localidad;
    @Expose
    private String sexo;
    @Expose
    private String tamano;
    @Expose
    private String descripcion;
    @Expose
    private String imagen;      // Base64
    @Expose
    private int idUsuario;      // FK con usuario logueado

    public Animal() {}

    /**
     * Constructor completo (para uso interno o si necesitas crear con todos los campos).
     */
    public Animal(int idAnimal,
                  String nombre,
                  String especie,
                  String raza,
                  String edad,
                  String localidad,
                  String sexo,
                  String tamano,
                  String descripcion,
                  String imagen,
                  int idUsuario) {
        this.idAnimal    = idAnimal;
        this.nombre      = nombre;
        this.especie     = especie;
        this.raza        = raza;
        this.edad        = edad;
        this.localidad   = localidad;
        this.sexo        = sexo;
        this.tamano      = tamano;
        this.descripcion = descripcion;
        this.imagen      = imagen;
        this.idUsuario   = idUsuario;
    }

    /**
     * Constructor para crear un animal nuevo (sin idAnimal).
     */
    public Animal(String nombre,
                  String especie,
                  String raza,
                  String edad,
                  String localidad,
                  String sexo,
                  String tamano,
                  String descripcion,
                  String imagen,
                  int idUsuario) {
        this.nombre      = nombre;
        this.especie     = especie;
        this.raza        = raza;
        this.edad        = edad;
        this.localidad   = localidad;
        this.sexo        = sexo;
        this.tamano      = tamano;
        this.descripcion = descripcion;
        this.imagen      = imagen;
        this.idUsuario   = idUsuario;
    }

    // —— Getters ——

    public int getIdAnimal() {
        return idAnimal;
    }

    public String getNombre() {
        return nombre;
    }

    public String getEspecie() {
        return especie;
    }

    public String getRaza() {
        return raza;
    }

    public String getEdad() {
        return edad;
    }

    public String getLocalidad() {
        return localidad;
    }

    public String getSexo() {
        return sexo;
    }

    public String getTamano() {
        return tamano;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public String getImagen() {
        return imagen;
    }

    public int getIdUsuario() {
        return idUsuario;
    }

    // —— Setters ——

    public void setIdAnimal(int idAnimal) {
        this.idAnimal = idAnimal;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public void setEspecie(String especie) {
        this.especie = especie;
    }

    public void setRaza(String raza) {
        this.raza = raza;
    }

    public void setEdad(String edad) {
        this.edad = edad;
    }

    public void setLocalidad(String localidad) {
        this.localidad = localidad;
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

    public void setTamano(String tamano) {
        this.tamano = tamano;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public void setImagen(String imagen) {
        this.imagen = imagen;
    }

    public void setIdUsuario(int idUsuario) {
        this.idUsuario = idUsuario;
    }
}
