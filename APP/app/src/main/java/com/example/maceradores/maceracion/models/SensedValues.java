package com.example.maceradores.maceracion.models;

public class SensedValues {

    private int id;
    private int idRaspi;
    private String date; //the format YYYY MM DD hh:mm
    //temperatures from sensors
    private float temp1;
    private float temp2;
    private float temp3;
    private float temp4;
    private float tempSecondary;//decoccion
    private float tempPH;
    // DHT11/22
    private float humidity;
    private float tempEnviroment;
    //ph sensor
    private float pH;

    public SensedValues(int id, int idRaspi, String date, float temp1, float temp2, float temp3, float temp4, float tempSecondary, float tempPH, float humidity, float tempEnviroment, float pH) {
        this.id = id;
        this.idRaspi = idRaspi;
        this.date = date;
        this.temp1 = temp1;
        this.temp2 = temp2;
        this.temp3 = temp3;
        this.temp4 = temp4;
        this.tempSecondary = tempSecondary;
        this.tempPH = tempPH;
        this.humidity = humidity;
        this.tempEnviroment = tempEnviroment;
        this.pH = pH;
    }


    public SensedValues(int id, String date, float temp1, float temp2, float temp3, float temp4, float tempSecondary, float tempPH, float humidity, float tempEnviroment, float pH) {
        this.id = id;
        this.date = date;
        this.temp1 = temp1;
        this.temp2 = temp2;
        this.temp3 = temp3;
        this.temp4 = temp4;
        this.tempSecondary = tempSecondary;
        this.tempPH = tempPH;
        this.humidity = humidity;
        this.tempEnviroment = tempEnviroment;
        this.pH = pH;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public float getTemp1() {
        return temp1;
    }

    public void setTemp1(float temp1) {
        this.temp1 = temp1;
    }

    public float getTemp2() {
        return temp2;
    }

    public void setTemp2(float temp2) {
        this.temp2 = temp2;
    }

    public float getTemp3() {
        return temp3;
    }

    public void setTemp3(float temp3) {
        this.temp3 = temp3;
    }

    public float getTemp4() {
        return temp4;
    }

    public void setTemp4(float temp4) {
        this.temp4 = temp4;
    }

    public float getTempSecondary() {
        return tempSecondary;
    }

    public void setTempSecondary(float tempSecondary) {
        this.tempSecondary = tempSecondary;
    }

    public float getTempPH() {
        return tempPH;
    }

    public void setTempPH(float tempPH) {
        this.tempPH = tempPH;
    }

    public float getHumidity() {
        return humidity;
    }

    public void setHumidity(float humidity) {
        this.humidity = humidity;
    }

    public float getTempEnviroment() {
        return tempEnviroment;
    }

    public void setTempEnviroment(float tempEnviroment) {
        this.tempEnviroment = tempEnviroment;
    }

    public float getpH() {
        return pH;
    }

    public void setpH(float pH) {
        this.pH = pH;
    }

    public int getIdRaspi() {
        return idRaspi;
    }

    public void setIdRaspi(int idRaspi) {
        this.idRaspi = idRaspi;
    }


    private static float enzymeParabolicActivation( float t, float minT, float maxT, float ph, float minPh, float maxPh ){
        //devuelo porcentaje. osea si 70% entonces devuleve 70
        // devuleve 100 en el centro del intervalo
        if( t >= minT && t <= maxT && ph >= minPh && ph <= maxPh ){
            float dividendo = 1600 * (t - minT) * (t - maxT) * (ph - minPh) * (ph - maxPh);
            float divisor = (maxT - minT) * (maxT - minT) * (maxPh - minPh) * (maxPh - minPh) ;

            if( divisor != 0) return dividendo / divisor;
            else return 0;
        }
        else return 0;
    }

    private static float enzymeNormalActivation( float t, float minT, float maxT, float ph, float minPh, float maxPh ){
        if( t < maxT){
        float desvio = 64; // PARAMETRO DE AJUSTE, Mientras mas grande, mas valores abarca.
        float medioT = (minT + maxT)/2;
        float medioPh = (minPh + maxPh) / 2;
        double exponenteT = - ((t - medioT) * (t - medioT) / desvio );
        double exponentePh = - ((ph - medioPh) * (ph - medioPh) / desvio );

        double porcentaje =  Math.exp(exponenteT) * Math.exp(exponentePh);

        return 100 * (float)porcentaje;
        } else
            return 0;
    }

    public static float alphaAmylase (float t, float ph){
        //return enzymeParabolicActivation(t, 70, 75, ph, 5.3f, 5.7f);
        return enzymeNormalActivation(t, 70, 75, ph, 5.3f, 5.7f);
    }

    public static float betaAmylase( float t, float ph){
        return enzymeNormalActivation(t, 62, 65, ph, 5f, 5.5f);
    }

    public static float betaGlucanase(float t, float ph){
        return enzymeNormalActivation(t, 35, 45, ph, 4.5f, 5.5f);
    }

    public static float protease (float t, float ph){
        return enzymeNormalActivation(t, 45, 55, ph, 4.6f, 5.3f);
    }
}
