package com.example.adoptatupet.models;

import android.os.Parcel;
import android.os.Parcelable;
import com.google.gson.annotations.Expose;

/**
 * Modelo Animal con implementación de Parcelable
 * para poder pasarlo entre Fragments/Activities de forma eficiente.
 */
public class Animal implements Parcelable {
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

    /**
     * Constructor vacío requerido por Gson y frameworks.
     */
    public Animal() {}

    /**
     * Constructor completo con todos los campos.
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

    // —— Parcelable implementation ——
    protected Animal(Parcel in) {
        idAnimal    = in.readInt();
        nombre      = in.readString();
        especie     = in.readString();
        raza        = in.readString();
        edad        = in.readString();
        localidad   = in.readString();
        sexo        = in.readString();
        tamano      = in.readString();
        descripcion = in.readString();
        imagen      = in.readString();
        idUsuario   = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(idAnimal);
        dest.writeString(nombre);
        dest.writeString(especie);
        dest.writeString(raza);
        dest.writeString(edad);
        dest.writeString(localidad);
        dest.writeString(sexo);
        dest.writeString(tamano);
        dest.writeString(descripcion);
        dest.writeString(imagen);
        dest.writeInt(idUsuario);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Parcelable.Creator<Animal> CREATOR = new Parcelable.Creator<Animal>() {
        @Override
        public Animal createFromParcel(Parcel in) {
            return new Animal(in);
        }

        @Override
        public Animal[] newArray(int size) {
            return new Animal[size];
        }
    };

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
