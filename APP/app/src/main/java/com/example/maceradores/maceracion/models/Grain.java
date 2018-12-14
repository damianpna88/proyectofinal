package com.example.maceradores.maceracion.models;

import com.example.maceradores.maceracion.utils.Calculos;

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
                "\t Porcentaje: " + String.valueOf(getQuantity()) +
                "\t Extracto: " + String.valueOf(getExtractPotential());
    }

    public String getStringPlanned(float densEspecif, float volLitros, float rendEquipoTeorico){
        double kg = Calculos.calcCantInsumoTeoRayDaniels(densEspecif, volLitros, this, rendEquipoTeorico);
        return getName() +
                "\t Cantidad: " + String.valueOf(kg) + " kg ";
    }

    public String getStringPlanned(float densEspecif, float volLitros, float rendEquipoTeorico, float rendEquipoPractico){
        double kg = Calculos.calcCantInsumoTeoRayDaniels(densEspecif, volLitros, this, rendEquipoTeorico);
        double kgPractico = Calculos.calcCantInsumoTeoRayDaniels(densEspecif, volLitros, this, rendEquipoPractico);
        return getName() +
                "\t Cantidad Teórica: " + String.valueOf(kg) + " kg " +
                "\t Cantidad Práctica: " + String.valueOf(kgPractico) + " kg";
    }

}
