package com.example.maceradores.maceracion.Fragments;


import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.NotificationCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.CardView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.Chronometer;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.example.maceradores.maceracion.R;
import com.example.maceradores.maceracion.db.DatabaseHelper;
import com.example.maceradores.maceracion.models.MeasureInterval;
import com.example.maceradores.maceracion.models.SensedValues;
import com.example.maceradores.maceracion.utils.Calculos;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class MeasureFragment extends Fragment {
    private CardView cardViewTemp;
    private TextView tvMeasureTemp;
    private TextView tvMeasurePh;
    private TextView tvMeasureEnzyme;
    private TextView tvMeasureEnviroment;
    private TextView tvMeasureSecondMacerator;
    private TextView tvMeasureStage;
    private Chronometer chronometer;

    //---- Config----
    private boolean[] sensoresHabilitados = new boolean[4];

    private int metodoCalculo;
    public static int PROMEDIO = R.id.radiobuttonPromedioConfigTemp;
    public static int MEDIANA = R.id.radiobuttonMedianaConfigTemp;
    public static int PROMEDIO_EXTREMOS = R.id.radiobuttonExtremosConfigTemp;

    //---Handler---
    Handler mHandlerThread;
    Thread thread1;
    private int idMash;
    private int idExp;


    public MeasureFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {


        View view = inflater.inflate(R.layout.fragment_measure, container, false);
        final int idMash = getArguments().getInt("idMash"); //Me traigo el idMash q viene del viewpager
        final int idExp = getArguments().getInt("idExp");
        this.idMash=idMash;

        this.idExp=idExp;


        //chronometer = getView().findViewById(R.id.chronometer);
        chronometer = view.findViewById(R.id.chronometer);
        chronometer.setBase(SystemClock.elapsedRealtime()); //esto debería ser el tiempo en el que hice la inserción o que tuve la primer medida.
        chronometer.start();

        // configuracion de temperatura.
        // TODO traerlo de un SharedPreferences.
        // veamos que sale de esto.
        Arrays.fill(this.sensoresHabilitados, true);
        this.metodoCalculo = PROMEDIO;

        loadSharedPreferences();




        cardViewTemp = view.findViewById(R.id.cardViewMeasureTemperature);
        cardViewTemp.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //Toast.makeText(getContext(), "Anduvo pereeeque", Toast.LENGTH_SHORT).show();
                //aca tengo que mostrar un alert dialog para que me guarde la configuración.
                showAlertDialogConfigTemp(sensoresHabilitados, metodoCalculo);
                return false;
            }
        });


        //---Thread con Handler
        thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                int counter =0;

                int intervaloMedicion = intervaloMedicion(idMash);
                int NumberOfCalls = cantMediciones(idMash, intervaloMedicion);
                int sleep = (intervaloMedicion/2)*1000;
                Log.d("idMash: ",String.valueOf(idMash));
                Log.d("idExp: ",String.valueOf(idExp));
                Log.d("sleep: ",String.valueOf(sleep));

                while (counter < NumberOfCalls) {
                    counter++;

                    try {
                        Thread.sleep(sleep);
                    } catch (InterruptedException ex) {
                        ex.printStackTrace();
                    }
                    SensedValues sensedValues = getLastSensedValues(idExp);
                    if (sensedValues != null) {
                        Bundle bundle = new Bundle();
                        bundle.putBoolean("NewSensedValues",true);
                        bundle.putString("date",sensedValues.getDate());
                        bundle.putFloat("temp1",sensedValues.getTemp1());
                        Log.d("temp1", String.valueOf(sensedValues.getTemp1()));
                        bundle.putFloat("temp2",sensedValues.getTemp2());
                        bundle.putFloat("temp3",sensedValues.getTemp3());
                        bundle.putFloat("temp4",sensedValues.getTemp4());
                        bundle.putFloat("tempSecondary",sensedValues.getTempSecondary());
                        bundle.putFloat("tempPH",sensedValues.getTempPH());
                        bundle.putFloat("humidity",sensedValues.getHumidity());
                        bundle.putFloat("tempEnviroment",sensedValues.getTempEnviroment());
                        bundle.putFloat("pH",sensedValues.getpH());

                        Message message = new Message();
                        message.setData(bundle);

                        mHandlerThread.sendMessage(message);

                        //-------Aca va tmb lo de la verificacion para las NOTIFICACIONES DE DESVIOS!
                        //float temp = validatedTempMean(sensedValues.getTemp1(),sensedValues.getTemp2(),sensedValues.getTemp3(),sensedValues.getTemp4()); //Hardcod
                        float[] t = new float[] {sensedValues.getTemp1(),sensedValues.getTemp2(),sensedValues.getTemp3(),sensedValues.getTemp4()};
                        float temp = theTemp(t, sensoresHabilitados, metodoCalculo);
                        int intervaloDuracion = intervaloMedicion(idMash);
                        List<Integer> ListmedicionesxInter = medicionesPorIntervalo(idMash,intervaloDuracion);
                        int cantSensedValues = amountSensedValue(idExp);
                        Log.d("Cant SensedValues",String.valueOf(cantSensedValues));
                        for (int medicion:ListmedicionesxInter) {
                            Log.d("List Mediciones",String.valueOf(medicion));
                        }

                        int Etapa = getOrderInterval(cantSensedValues,ListmedicionesxInter);
                        MeasureInterval measureInterval = getIntervalByOrder(Etapa,idMash);
                        float tempMin = measureInterval.getMainTemperature() - measureInterval.getMainTemperatureDeviation();//temp objetivo menos desviacion
                        float tempMax = measureInterval.getMainTemperature() + measureInterval.getMainTemperatureDeviation();//temp objetivo mas desviacion
                        float ph = sensedValues.getpH();
                        float phMin = measureInterval.getpH() - measureInterval.getPhDeviation();//pH objetivo menos desviacion
                        float phMax = measureInterval.getpH() + measureInterval.getPhDeviation();//pH objetivo mas desviacion
                        Log.d("temp", String.valueOf(temp));
                        Log.d("tempMin", String.valueOf(tempMin));
                        //TODO acomodar para que al tirar temp=-1000 no pase el temp<tempMin NO ESTA ANDANDO IGUAL...JAJAJ SOLO PARA PH
                        if(temp<tempMin && ph<phMin) {
                            sendNotification("Alerta de desvío de Temperatura y pH ","Temperatura: "+String.valueOf(temp) + " menor al minimo: " + String.valueOf(tempMin)
                                    + "y pH: "+String.valueOf(ph) + " menor al minimo: " + String.valueOf(phMin));
                        }
                        else if(temp<tempMin){ //Si es menor a la minima o mayor a la maxima
                            sendNotification("Alerta de desvío de Temperatura ","Temperatura: "+String.valueOf(temp) + " menor al minimo: " + String.valueOf(tempMin));
                        }else if(ph<phMin){ //Si es menor a la minima o mayor a la maxima
                            sendNotification("Alerta de desvío de pH ","pH: "+String.valueOf(ph) + " menor al minimo: " + String.valueOf(phMin));
                        }
                        if(temp>tempMax){ //Si es menor a la minima o mayor a la maxima
                            sendNotification("Alerta de desvío de Temperatura ","Temperatura: "+String.valueOf(temp) + "  mayor al maximo: " + String.valueOf(tempMax));
                        }
                        if(ph>phMax){ //Si es menor a la minima o mayor a la maxima
                            sendNotification("Alerta de desvío de ph ","pH: "+String.valueOf(ph) + " mayor al maximo: " + String.valueOf(phMax));
                        }

                    }
                    else{
                        Log.d("Sensedvalues esta","vacío");
                        Bundle bundle = new Bundle();
                        bundle.putBoolean("NewSensedValues",false);
                        Message message = new Message();
                        message.setData(bundle);

                        mHandlerThread.sendMessage(message);
                    }
                }
            }
        });
        thread1.start();
        // Inflate the layout for this fragment
        return view;
    }

    private void loadSharedPreferences() {
        SharedPreferences settings = getActivity().getSharedPreferences("Config Temp", 0);
        if(settings.contains("Sensor 1")){
            sensoresHabilitados[0] = settings.getBoolean("Sensor 1", true);
        }

        if(settings.contains("Sensor 2")){
            sensoresHabilitados[1] = settings.getBoolean("Sensor 2", true);
        }

        if(settings.contains("Sensor 3")){
            sensoresHabilitados[2] = settings.getBoolean("Sensor 3", true);
        }

        if(settings.contains("Sensor 4")){
            sensoresHabilitados[3] = settings.getBoolean("Sensor 4", true);
        }

        if(settings.contains("Metodo Calculo")){
            metodoCalculo = settings.getInt("Metodo Calculo", PROMEDIO);
        }

    }

    private void saveSharedPreferences(){
        SharedPreferences settings = getActivity().getSharedPreferences("Config Temp", 0);
        SharedPreferences.Editor editor = settings.edit();

        editor.putBoolean("Sensor 1", sensoresHabilitados[0]);
        editor.putBoolean("Sensor 2", sensoresHabilitados[1]);
        editor.putBoolean("Sensor 3", sensoresHabilitados[2]);
        editor.putBoolean("Sensor 4", sensoresHabilitados[3]);

        editor.putInt("Metodo Calculo", metodoCalculo);

        editor.commit();
    }

    private void showAlertDialogConfigTemp(final boolean[] sensoresHabilitados, final int metodoCalculo) {

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Configuración de Medición");

        View configView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_config_measure_temp, null);
        builder.setView(configView);

        // ahora me tengo que traer todas las referencias.
        // Son 4 chech button de los sensores.
        final CheckBox sensor1 = configView.findViewById(R.id.checkboxSensor1ConfigTemp);
        final CheckBox sensor2 = configView.findViewById(R.id.checkboxSensor2ConfigTemp);
        final CheckBox sensor3 = configView.findViewById(R.id.checkboxSensor3ConfigTemp);
        final CheckBox sensor4 = configView.findViewById(R.id.checkboxSensor4ConfigTemp);

        // son 3 radio button de radio group
        final RadioGroup radioGroup = configView.findViewById(R.id.radiogroupConfigTemp);

        //Inicializo los valores
        sensor1.setChecked( sensoresHabilitados[0]);
        sensor2.setChecked( sensoresHabilitados[1]);
        sensor3.setChecked( sensoresHabilitados[2]);
        sensor4.setChecked( sensoresHabilitados[3]);

        radioGroup.check(metodoCalculo);

        //agrego boton para cerrar el dialogo
        builder.setPositiveButton("ACEPTAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                setSensoresHabilitados(
                        sensor1.isChecked(),
                        sensor2.isChecked(),
                        sensor3.isChecked(),
                        sensor4.isChecked()
                );

                switch (radioGroup.getCheckedRadioButtonId()){
                    case R.id.radiobuttonPromedioConfigTemp:
                        setMetodoCalculo(PROMEDIO);
                        //metodoCalculo = PROMEDIO;
                        break;
                    case R.id.radiobuttonMedianaConfigTemp:
                        setMetodoCalculo(MEDIANA);
                        break;
                    case R.id.radiobuttonExtremosConfigTemp:
                        setMetodoCalculo(PROMEDIO_EXTREMOS);
                        break;
                    default:
                        break;
                }

            }
        });

        builder.setNegativeButton("CANCELAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.create().show();
    } // en Show Alert Dialog Config Temp

    private void setSensoresHabilitados(boolean checked1, boolean checked2, boolean checked3, boolean checked4) {
        this.sensoresHabilitados[0] = checked1;
        this.sensoresHabilitados[1] = checked2;
        this.sensoresHabilitados[2] = checked3;
        this.sensoresHabilitados[3] = checked4;
    }

    public void setMetodoCalculo(int calculo){
        this.metodoCalculo = calculo;
    }

    @Override
    public void onResume() {
        super.onResume();
        if(getView() != null){
            this.tvMeasureTemp = (TextView) getView().findViewById(R.id.textViewMeasureTemp); //podria estar en el oncreate.
            this.tvMeasurePh = (TextView) getView().findViewById(R.id.textViewMeasurePh);
            this.tvMeasureEnzyme = (TextView) getView().findViewById(R.id.textViewMeasureEnzyme);
            this.tvMeasureEnviroment = (TextView) getView().findViewById(R.id.textViewMeasureEnviroment);
            this.tvMeasureSecondMacerator = (TextView) getView().findViewById(R.id.textViewMeasureSecondMaserator);
            this.tvMeasureStage= (TextView) getView().findViewById(R.id.textViewMeasureStage);
        } else
            Log.d("Measure Fragment", "No se cargo el layout correctamente");


        //----Handler para manejo de mensajes con el thread
        mHandlerThread = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                Bundle bundle = msg.getData();
                boolean newSensedValues = bundle.getBoolean("NewSensedValues");
                if(newSensedValues) { //Solo si hay nuevos SensedValues
                    float t1 = bundle.getFloat("temp1");
                    float t2 = bundle.getFloat("temp2");
                    float t3 = bundle.getFloat("temp3");
                    float t4 = bundle.getFloat("temp4");
                    //float tPromedio = validatedTempMean(t1, t2, t3, t4);
                    float[] t = new float[]{t1,t2,t3,t4};
                    float tPromedio = theTemp(t, sensoresHabilitados, metodoCalculo);
                    float ph = bundle.getFloat("pH");
                    float tempPh = bundle.getFloat("tempPH");
                    float tempSecondary =  bundle.getFloat("tempSecondary");

                    int intervaloDuracion = intervaloMedicion(idMash);
                    Log.d("Duration",String.valueOf(intervaloDuracion));
                    List<Integer> ListmedicionesxInter = medicionesPorIntervalo(idMash, intervaloDuracion);
                    for (int medicion:ListmedicionesxInter) {
                        Log.d("List Mediciones",String.valueOf(medicion));
                    }

                    int cantSensedValues = amountSensedValue(idExp);
                    Log.d("Cant SensedValues",String.valueOf(cantSensedValues));
                    int Etapa = getOrderInterval(cantSensedValues, ListmedicionesxInter);
                    MeasureInterval measureInterval = getIntervalByOrder(Etapa, idMash);
                    float desvioTemp = tPromedio - measureInterval.getMainTemperature();
                    float desvioPh = ph - measureInterval.getpH();
                    float desvioTempSecon = tempSecondary - measureInterval.getSecondTemperature();
                    loadTemperatureCardView(tPromedio, desvioTemp, measureInterval.getMainTemperature(), measureInterval.getMainTemperatureDeviation(), t1, t2, t3, t4);
                    if (ph > 0)
                        loadPhCardView(ph, desvioPh, measureInterval.getpH(), measureInterval.getPhDeviation(), tempPh); //Solo actualizo ph si el valor es valido
                    loadStageCardView(Etapa);
                    loadSecondMaceratorCardView(tempSecondary,desvioTempSecon,measureInterval.getSecondTemperature(),measureInterval.getSecondTemperatureDeviation());
                    loadEnviromentCardView(bundle.getFloat("tempEnviroment"),bundle.getFloat("humidity"));
                    loadEnzymeCardView(SensedValues.alphaAmylase(tPromedio,ph),
                            SensedValues.betaAmylase(tPromedio,ph),SensedValues.betaGlucanase(tPromedio,ph),SensedValues.protease(tPromedio,ph));
                }

            }
        };


    }

    private SensedValues getLastSensedValues(int idExp){
        SensedValues sv = null;

        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery("SELECT * FROM SensedValues WHERE id_exp = ? AND id = (SELECT MAX(id) FROM SensedValues)", new String[] {String.valueOf(idExp)});

        if(cursor.moveToFirst()){
            int id = cursor.getInt( cursor.getColumnIndexOrThrow("id"));
            int idRaspi = cursor.getInt( cursor.getColumnIndexOrThrow("idRaspi"));
            String date = cursor.getString(cursor.getColumnIndexOrThrow("fechayhora"));
            float temp1 = cursor.getFloat(cursor.getColumnIndexOrThrow("temp1"));
            float temp2 = cursor.getFloat(cursor.getColumnIndexOrThrow("temp2"));
            float temp3 = cursor.getFloat(cursor.getColumnIndexOrThrow("temp3"));
            float temp4 = cursor.getFloat(cursor.getColumnIndexOrThrow("temp4"));
            float temp5 = cursor.getFloat(cursor.getColumnIndexOrThrow("temp5"));
            float tempPh = cursor.getFloat(cursor.getColumnIndexOrThrow("tempPh"));
            float tempAmb = cursor.getFloat(cursor.getColumnIndexOrThrow("tempAmb"));
            float humidity = cursor.getFloat(cursor.getColumnIndexOrThrow("humity"));
            float pH= cursor.getFloat(cursor.getColumnIndexOrThrow("pH"));

            sv = new SensedValues(id,idRaspi, date, temp1, temp2, temp3, temp4, temp5, tempPh, humidity, tempAmb, pH);
        }
        cursor.close();
        db.close();
        return sv;
    }

    private void loadStageCardView(int stage){
        tvMeasureStage.setText(" Etapa Actual: ");
        tvMeasureStage.append(String.valueOf(stage));
    }

    private void loadSecondMaceratorCardView(float temp, float desvioObtenido, float tempPlanificada, float alerta){
        //android:text="-- "
        tvMeasureSecondMacerator.setText(" Actual: ");
        tvMeasureSecondMacerator.append(String.valueOf(temp));

        tvMeasureSecondMacerator.append(" °C \t\t\t\t\t Desvío: ");
        tvMeasureSecondMacerator.append(String.valueOf(desvioObtenido));

        tvMeasureSecondMacerator.append(" °C \n Planificado: ");
        tvMeasureSecondMacerator.append(String.valueOf(tempPlanificada));

        tvMeasureSecondMacerator.append(" °C \t\t Alerta: ±");
        tvMeasureSecondMacerator.append(String.valueOf(alerta));
        tvMeasureSecondMacerator.append(" °C");
    }

    private void loadEnviromentCardView(float temp, float humidity){
        //android:text="22°C \t Humedad: 67%"
        tvMeasureEnviroment.setText(" Temperatura: ");
        tvMeasureEnviroment.append(String.valueOf(temp));

        tvMeasureEnviroment.append(" °C \t Humedad: ");
        tvMeasureEnviroment.append(String.valueOf(humidity));
        tvMeasureEnviroment.append("%");
    }

    private void loadEnzymeCardView(float alfa, float beta, float glucanasa, float proteasa){
        tvMeasureEnzyme.setText(" Alfa Amilasa: ");
        tvMeasureEnzyme.append(String.valueOf(alfa));

        tvMeasureEnzyme.append("% \t\t Proteasa: ");
        tvMeasureEnzyme.append(String.valueOf(proteasa));

        tvMeasureEnzyme.append("% \n Beta Amilasa: ");
        tvMeasureEnzyme.append(String.valueOf(beta));

        tvMeasureEnzyme.append("% \t\t Beta Glucanasa: ");
        tvMeasureEnzyme.append(String.valueOf(glucanasa));
        tvMeasureEnzyme.append("%");
    }

    private void loadPhCardView(float ph, float desvioObtenido, float phPlanificado, float desvioPlanificado, float tempPh) {
        //android:text=" \n Temperatura de Medición: 25°C"

        tvMeasurePh.setText(" Actual: ");
        tvMeasurePh.append(String.valueOf(ph));

        tvMeasurePh.append(" \t\t\t\t\t\t Desvío: ");
        tvMeasurePh.append(String.valueOf(desvioObtenido));

        tvMeasurePh.append(" \n Planificado: ");
        tvMeasurePh.append(String.valueOf(phPlanificado));

        tvMeasurePh.append(" \t\t\t\t Alerta: ± ");
        tvMeasurePh.append(String.valueOf(desvioPlanificado));

        tvMeasurePh.append(" \n Temperatura de Medición: ");
        tvMeasurePh.append(String.valueOf(tempPh));
        tvMeasurePh.append(" °C");
    }

    private void loadTemperatureCardView(float tPromedio, float desvioObtenido, float tPlanificada, float desvioPlanificado, float t1, float t2, float t3, float t4) {
        tvMeasureTemp.setText(" Calculado: ");
        tvMeasureTemp.append(String.valueOf(tPromedio)); //este debería ser el valor promedio calculado.

        tvMeasureTemp.append(" °C \t\t Desvío: "); //esto es por una cuestión estetica.
        tvMeasureTemp.append(String.valueOf(desvioObtenido)); //valor desviado respecto a lo planificado.

        tvMeasureTemp.append(" °C \n Planificado: ");
        tvMeasureTemp.append(String.valueOf(tPlanificada)); //Aca iría el valor planificado.

        tvMeasureTemp.append(" °C \t\t\t Alerta: ± ");
        tvMeasureTemp.append(String.valueOf(desvioPlanificado)); //aca sería valor de desvio

        tvMeasureTemp.append(" °C \n Sensor 1: ");
        tvMeasureTemp.append(String.valueOf(t1)); //valor del primer sensor.

        tvMeasureTemp.append(" °C \t\t\t Sensor 3: ");
        tvMeasureTemp.append(String.valueOf(t3));

        tvMeasureTemp.append(" °C \n Sensor 2: ");
        tvMeasureTemp.append(String.valueOf(t2));

        tvMeasureTemp.append(" °C \t\t\t Sensor 4: ");
        tvMeasureTemp.append(String.valueOf(t4));
        tvMeasureTemp.append(" °C");
    }

    private int cantMediciones( int idMash, int intervaloMedicion){
        //TODO hacer con medicionesPorIntervalo

        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(duracion) FROM Intervalo WHERE maceracion = ?", new String[]{String.valueOf(idMash)});
        int duracionTotal = 0;
        if(c.moveToFirst()){
            duracionTotal = c.getInt(0);
        }
        c.close();
        db.close();
        return (duracionTotal * 60) / (intervaloMedicion/2);
    }

    private int intervaloMedicion (int idMash){
        int intervaloMedicion = 0;
        // saber el periodo de medicion.
        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] columns = {"intervaloMedTemp"};
        String selection = "id = ?";
        String[] selectionArgs = { String.valueOf(idMash)};

        Cursor cursor = db.query("Maceracion", columns, selection, selectionArgs, null, null, null);
        if(cursor.moveToFirst()){
            intervaloMedicion = cursor.getInt(0); //como tengo una sola columna, devuelvo la primera nomas.
        }
        cursor.close();
        db.close();
        if(intervaloMedicion<30)intervaloMedicion=30; // Minimo Intervalo de Medicion es de 30 seg

        return intervaloMedicion;
    }

    public void sendNotification(String title, String message) {
        NotificationManager notificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
//        Intent intent = new Intent(getContext(), CurrentExperienceActivity.class); // Aca estaria mi duda
//        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//        PendingIntent pendingIntent = PendingIntent.getActivity(getContext(), 0, intent, 0);

        //If on Oreo then notification required a notification channel.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel("default", "Default", NotificationManager.IMPORTANCE_DEFAULT);
            notificationManager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notification = new NotificationCompat.Builder(getContext(), "default")
                .setContentTitle(title)
                //.setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
//                .setContentIntent(pendingIntent)
                .setSmallIcon(R.mipmap.ic_launcher);

        notificationManager.notify(1, notification.build());
    }

    private float theTemp( float[] temps, boolean[] sensAllow, int metodo ){
        // get validated Temps.
        List<Float> t = new ArrayList<>();
        for( int i = 0; i < 4; i++){
            if( sensAllow[i] && temps[i] != -1000 )
                t.add(temps[i]);
        }

        if( ! t.isEmpty()){
            if(metodo == PROMEDIO) return Calculos.promedio(t);
            if(metodo == MEDIANA) return Calculos.mediana(t);
            if(metodo == PROMEDIO_EXTREMOS) return Calculos.promedio_extremos(t);
        }

        return -1000;
    }

    private float validatedTempMean(float t1, float t2, float t3, float t4){
        int divisor=4;
        float dividendo=0;
        if(t1==-1000){
            divisor--;
        }else{dividendo = dividendo + t1;}
        if(t2==-1000){
            divisor--;
        }else{dividendo = dividendo + t2;}
        if(t3==-1000){
            divisor--;
        }else{dividendo = dividendo + t3;}
        if(t4==-1000){
            divisor--;
        }else{dividendo = dividendo + t4;}
        if(dividendo==0){//Asi evito la division por cero
            return -1000;//Devuelvo, una temperatura invalida
        }else {
            float promedio = dividendo / divisor;
            return promedio;
        }

    }

    public List<Integer> medicionesPorIntervalo( int idMash, int intervaloMedicion){
        List<Integer> mediciones = new ArrayList<Integer>();
        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] columns = {"duracion"};
        String selection = "maceracion = ?";
        String[] selectionArgs = { String.valueOf(idMash)};

        Cursor cursor = db.query("Intervalo", columns, selection, selectionArgs, null, null, "orden DESC");
        while(cursor.moveToNext()){
            mediciones.add( cursor.getInt(0) * 60 / (intervaloMedicion/2));
        }
        cursor.close();
        db.close();
        return mediciones;
    }

    public int amountSensedValue(int idExp){
        //int amount = 0;
        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        int amount = (int) DatabaseUtils.queryNumEntries(db, "SensedValues", "id_exp=?", new String[] {String.valueOf(idExp)});
        db.close();
        return amount;
    }

    public int getOrderInterval( int amount, List<Integer> medicionesPorIntervalo){
        //Me devuelve la Etapa/Stage en la q estoy
        // hago los valores acumulados.
        // {10, 30, 20}
        for( int i = medicionesPorIntervalo.size() - 1; i > 0; i--){
            int acumulado = 0;
            // sumo todos los valores antes que i.
            for( int j = 0; j <= i; j++){
                acumulado = acumulado + medicionesPorIntervalo.get(j);
                Log.d("Acumulado",String.valueOf(acumulado));
            }
            medicionesPorIntervalo.set(i, acumulado);
        }
        // {10, 40, 60}

        int orden = 1;
        boolean flag = true;
        while(flag){
            Log.d("getOrderIntervals","");
            Log.d("Cant of SensedValues: ",String.valueOf(amount));

            if( amount <= medicionesPorIntervalo.get(orden-1)){
                flag = false;
            }else{
                orden = orden + 1;
            }
        }
        return orden;

    }

    public MeasureInterval getIntervalByOrder(int order, int idMash){
        MeasureInterval measureInterval = null;
        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        //String[] columns = {"intervaloMedTemp"};
        String selection = "maceracion = ? AND orden = ?";
        String[] selectionArgs = { String.valueOf(idMash), String.valueOf(order)};

        Cursor cursor = db.query("intervalo", null, selection, selectionArgs, null, null, null);
        if(cursor.moveToFirst()){
            float mainTemperature = cursor.getFloat( cursor.getColumnIndexOrThrow("temperatura"));
            float mainTemperatureDeviation = cursor.getFloat(cursor.getColumnIndexOrThrow("desvioTemperatura"));
            float secondTemperature = cursor.getFloat(cursor.getColumnIndexOrThrow("tempDecoccion"));
            float secondTemperatureDeviation = cursor.getFloat(cursor.getColumnIndexOrThrow("desvioTempDecoccion"));
            float pH = cursor.getFloat(cursor.getColumnIndexOrThrow("ph"));
            float phDeviation = cursor.getFloat(cursor.getColumnIndexOrThrow("desvioPh"));
            int duration = cursor.getInt(cursor.getColumnIndexOrThrow("duracion"));

            measureInterval = new MeasureInterval(order,mainTemperature, mainTemperatureDeviation, secondTemperature, secondTemperatureDeviation, pH,  phDeviation,  duration);
        }
        cursor.close();
        db.close();
        return measureInterval;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        saveSharedPreferences();
    }
}
