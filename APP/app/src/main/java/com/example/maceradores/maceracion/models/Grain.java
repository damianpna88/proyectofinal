package com.example.maceradores.maceracion.models;

public class Grain {
    private String name;
    private float quantity;
    private float extractPotential;

    public Grain(String name, float quantity, float extractPotential) {
        this.name = name;
        this.quantity = quantity;
        this.extractPotential = extractPotential;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public float getQuantity() {
        return quantity;
    }

    public void setQuantity(float quantity) {
        this.quantity = quantity;
    }

    public float getExtractPotential() {
        return extractPotential;
    }

    public void setExtractPotential(float extractPotential) {
        this.extractPotential = extractPotential;
    }

    public String getStringPlanning(){
        return  getName() +
                "\t porcentaje: " + String.valueOf(getQuantity()) +
                "\t Extracto: " + String.valueOf(getExtractPotential());
    }

    public String getStringPlanned(float rendimiento){
        String detail = getName() +
                "\t Cantidad: " + String.valueOf(1) + " kg ";
        return detail;
    }
}
