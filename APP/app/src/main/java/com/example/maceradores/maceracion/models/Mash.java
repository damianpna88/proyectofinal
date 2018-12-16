package com.example.maceradores.maceracion.models;

import com.example.maceradores.maceracion.utils.Calculos;

import java.util.ArrayList;
import java.util.List;

public class Mash {
    private int id;
    private String name;
    private String tipo; //TODO change variable name to Type
    private List<MeasureInterval> plan;
    private List<Grain> grains;
    private List<Experiment> experiments;
    private float volumen; //TODO change variable name to volume
    private float densidadObjetivo; //TODO change variable name to objetiveDensity
    private int periodMeasureTemperature;
    private int periodMeasurePh;

    // Constructors

    //Empty Constructor.
    public Mash(){}

    public Mash(int id, String name, String type) {
        this.id = id;
        this.name = name;
        this.tipo = type;
    }

    public Mash(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public Mash(int id, String name, List<MeasureInterval> plan, List<Experiment> experiments) {
        this.id = id;
        this.name = name;
        this.plan = plan;
        this.experiments = experiments;
    }

    public Mash(int id, String name, List<MeasureInterval> plan) {
        this.id = id;
        this.name = name;
        this.plan = plan;
    }

    public Mash(int id, String name, List<MeasureInterval> plan, List<Grain> grains, List<Experiment> experiments, int periodMeasureTemperature, int periodMeasurePh) {
        this.id = id;
        this.name = name;
        this.plan = plan;
        this.grains = grains;
        this.experiments = experiments;
        this.periodMeasureTemperature = periodMeasureTemperature;
        this.periodMeasurePh = periodMeasurePh;
    }

    //Own methods
    public void addMeasureInterval( MeasureInterval i){
        // I supose that this interval doesn't have the field order.
        // then i assigned the length of the list plus one.
        i.setOrder( this.plan.size() + 1 );
        plan.add(i);
    }

    public void removeMeasureInterval(int position){
        // i need to reset the order and delete interval.
        if( position > plan.size() || position < 0){
            //si le erro con los numeritos que no haga nada.
            return;
        }
        boolean finded = false;
        int it = plan.size() - 1; // arranco por el utlimo
        while(finded){
            if( it == position){
                // lo encontre y lo elimino.
                plan.remove(it);
                finded = true;
            }
            else{
                //agarro lo que esta en it y le resto uno al orden.
                plan.get(it).decrementOrder();
                // y sigo con el proximo.
                it --;
            }
        } //end while

    }

    // Getters & Setters

    public float getDensidadObjetivo() {
        return densidadObjetivo;
    }

    public void setDensidadObjetivo(float densidadObjetivo) {
        this.densidadObjetivo = densidadObjetivo;
    }

    public List<Experiment> getExperiments() {
        return experiments;
    }

    public void setExperiments(List<Experiment> experiments) {
        this.experiments = experiments;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<MeasureInterval> getPlan() {
        return plan;
    }

    public void setPlan(List<MeasureInterval> plan) {
        // setteo los ordenes de la lista para que queden de manera creciente.

        this.plan = new ArrayList<MeasureInterval>();

        for(int i= 0; i < plan.size(); i++){
            addMeasureInterval(plan.get(i));
        }
        //this.plan = plan;
    }

    public List<Grain> getGrains() {
        return grains;
    }

    public void setGrains(List<Grain> grains) {
        this.grains = grains;
    }

    public void addGrain(Grain grain){
        this.grains.add(grain);
    }

    public void removeGrain(int index){
        if( index >= 0 && index < grains.size())
            grains.remove(index);
    }

    public int getPeriodMeasureTemperature() {
        return periodMeasureTemperature;
    }

    public void setPeriodMeasureTemperature(int periodMeasureTemperature) {
        this.periodMeasureTemperature = periodMeasureTemperature;
    }

    public int getPeriodMeasurePh() {
        return periodMeasurePh;
    }

    public void setPeriodMeasurePh(int periodMeasurePh) {
        this.periodMeasurePh = periodMeasurePh;
    }

    public float getVolumen() {
        return volumen;
    }

    public void setVolumen(float volumen) {
        this.volumen = volumen;
    }

    public String getTipo() {
        return tipo;
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public String getPlanning(int position){
        // Necesito saber la cantidad de grano que tengo que usaer.
        double cantMalta = 0;
        for( int i = 0; i < this.getGrains().size(); i++){
            //TODO calcular el rendimiento a partir de la base de datos.
            cantMalta = cantMalta + Calculos.calcCantInsumoTeoRayDaniels(this.densidadObjetivo, this.volumen, this.grains.get(i), 0.7f);
        }


        if( this.tipo.equals( "Simple") ){
            // aca retorno la termperatura de inicio nomas.
            double tempInicio = Calculos.temperaturaAguaInicial(this.getPlan().get(0).getMainTemperature(), 20f, this.volumen, cantMalta );
            tempInicio = tempInicio - tempInicio % 0.01;
            return "Temperatura de agua: " + String.valueOf(tempInicio) + " °C \n" +
                    "Cantidad de agua: " + String.valueOf(this.volumen) + "litros \n";
        }

        if(this.tipo.equals("Escalonada")){
            // que comience la fiesta. la cuestion aca es que calculo
            // la cosa es asi el primer intervalo es como una maceracion simple.
            // lo que si tengo que conocer el volumen de agua para ese primer calculo.
            ArrayList<Float> temperaturas = new ArrayList<Float>();
            for(int i = 0; i < getPlan().size(); i++){
                temperaturas.add(plan.get(i).getMainTemperature());
            }
            double volAguaPrimerEscalon = Calculos.cantAguaPrimerEscalon( this.volumen, cantMalta, temperaturas);
            if( position == 0){
                // como maceracion simple pero cambio el volumen de agua.
                double tempInicio = Calculos.temperaturaAguaInicial(this.getPlan().get(0).getMainTemperature(), 20f, volAguaPrimerEscalon, cantMalta );
                tempInicio = tempInicio - tempInicio % 0.01;
                return "Temperatura de agua: " + String.valueOf(tempInicio) + " °C \n" +
                        "Cantidad de agua: " + String.valueOf(volAguaPrimerEscalon) + "litros \n";
            } else{
                // aca hay que hacer algo mas jugoso
                // la temperatura siempre es 100°C -> agua hirviendo.
                double cantAgua = Calculos.cantAguaEscalon(volAguaPrimerEscalon, cantMalta, temperaturas.subList(0, position + 1));
                cantAgua = cantAgua - cantAgua % 0.01;
                return "Temperatura de agua: " + String.valueOf(100) + " °C \n" +
                        "Cantidad de agua: " + String.valueOf(cantAgua) + "litros \n";

            }
        }


        return "";
    }




}
