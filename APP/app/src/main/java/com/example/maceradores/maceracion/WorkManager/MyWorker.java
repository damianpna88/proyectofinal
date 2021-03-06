package com.example.maceradores.maceracion.WorkManager;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.NonNull;
import android.util.Log;

import com.example.maceradores.maceracion.RetrofitGsonContainer.SensedValuesContainer;
import com.example.maceradores.maceracion.db.DatabaseHelper;
import com.example.maceradores.maceracion.retrofitInterface.Api;
import com.google.gson.JsonObject;

import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.work.Worker;
import androidx.work.WorkerParameters;
import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MyWorker extends Worker {
    public static final String IDEXP = "-1";


    public MyWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {

        String IdExp = getInputData().getString(IDEXP);

        Log.d("El idExp es:",IdExp);
        int IdExp_int = Integer.valueOf(IdExp);

        int idMash = getIdMashByIdExp(IdExp_int);
        if(idMash != -1 && IdExp_int != -1) {
            int intervaloMedicion = intervaloMedicion(idMash); //intervalo de medicion en segundos! o segun convengamos
            Log.d("El idMash es:", String.valueOf(idMash));
            Log.d("El interv de med es:", String.valueOf(intervaloMedicion));


            int NumberOfCalls = cantMediciones(idMash, intervaloMedicion);
            Log.d("La duración es:", String.valueOf((intervaloMedicion / 2) * NumberOfCalls));
            int counter = 0;
            int sleep = (intervaloMedicion / 2) * 1000;
            while (counter < NumberOfCalls) {

                String AppList = getListIdInsertedSensedValue(IdExp_int); // Get the sensed values in the APP DB
                Log.d("ListInsertedValues",AppList);
                try {
                    getSensedValues(IdExp_int, AppList); // Call to API to get the Sensed Values
                } catch (Exception e) {
                    e.printStackTrace();
                    Log.d("Se rompió la ", "llamada RETROFIT new Exp");
                }

                counter++;
                try {
                    Thread.sleep(sleep);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

            }
        }
        return Result.SUCCESS;
    }

    private int getIdMashByIdExp( int idExp){
        int idMash = -1;

        DatabaseHelper dbHelper = new DatabaseHelper(getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] columns = {"maceracion"};
        String selection = "id = ?";
        String[] selectionArgs = { String.valueOf(idExp)};

        Cursor cursor = db.query("Experimento", columns, selection, selectionArgs, null, null, null);
        if(cursor.moveToFirst()){
            idMash = cursor.getInt(0); //como tengo una sola columna, devuelvo la primera nomas.
        }
        cursor.close();
        db.close();

        return idMash;
    }

    private int intervaloMedicion (int idMash){
        int intervaloMedicion = 0;
        // saber el periodo de medicion.
        DatabaseHelper dbHelper = new DatabaseHelper(getApplicationContext());
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
        if(intervaloMedicion<60)intervaloMedicion=60; // Minimo Intervalo de Medicion es de 60 seg

        return intervaloMedicion;
    }

    private int cantMediciones( int idMash, int intervaloMedicion){
        DatabaseHelper dbHelper = new DatabaseHelper(getApplicationContext());
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

    private void getSensedValues(int idExp, String IdList) {
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.MINUTES)
                .readTimeout(240, TimeUnit.SECONDS)
                .writeTimeout(240, TimeUnit.SECONDS)
                .build();

        //Luego lo agrego a la llamada de Retrofit

        final Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Api.BASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();


        //------Build new JsonObject with Experiment to be send
        //{ "idExp": "31", "ArrayID": "" }
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("idExp", String.valueOf(idExp));
        jsonObject.addProperty("ArrayID", IdList);

        Api api = retrofit.create(Api.class);
        Call<List<SensedValuesContainer>> call = api.getSensedValues(jsonObject);
//        final List<SensedValuesContainer> Lista = new ArrayList<SensedValuesContainer>();
        call.enqueue(new Callback<List<SensedValuesContainer>>() {
            @Override
            public void onResponse(Call<List<SensedValuesContainer>> call, Response<List<SensedValuesContainer>> response) {
                List<SensedValuesContainer> values = response.body();
                if (!values.isEmpty()) { // Only makes Insertions if the response is not empty
                    for (SensedValuesContainer value : values) {
                        Log.d("Un valor...", value.getTemp1());
                        Long flag = insertSensedValue(value); // Here we insert the values
                        if(flag==-1)Log.d("Error en","Inserción SensedValue");
                    }
                }
            }
            @Override
            public void onFailure(Call call, Throwable t) {
                t.printStackTrace();
            }
        });

    }

    private long insertSensedValue(SensedValuesContainer svc){
        DatabaseHelper dbHelperTest = new DatabaseHelper(getApplicationContext());
        SQLiteDatabase dbTest = dbHelperTest.getReadableDatabase();

        //String[] columns = new String[]{"id"};
        String selection = "idRaspi = ?";
        String[] selectionArgs = new String[] {String.valueOf(svc.getId())};
        Cursor c = dbTest.query("SensedValues", null, selection, selectionArgs, null, null, null);
        if(c.moveToFirst()){
            String idExp = c.getString(c.getColumnIndexOrThrow("id_exp"));
            String idSV = c.getString(c.getColumnIndexOrThrow("idRaspi"));
            Log.d("Insertar Sensed Values", "el sensed value " + idSV + "ya existe en el experimento " + idExp);
            c.close();
            dbTest.close();
            return -1;
        }
        c.close();
        dbTest.close();

// creo la instancia de basede datos para insertar.
        DatabaseHelper dbHelper = new DatabaseHelper(getApplicationContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put("id_exp", Integer.valueOf(svc.getId_exp()));
        values.put("idRaspi", Integer.valueOf(svc.getId()));
        values.put("fechayhora", svc.getFechayhora());  //Aca no se que movida. Es tipo fecha.
        values.put("temp1", Float.valueOf(svc.getTemp1()));
        values.put("temp2", Float.valueOf(svc.getTemp2()));
        values.put("temp3", Float.valueOf(svc.getTemp3()));
        values.put("temp4", Float.valueOf(svc.getTemp4()));
        values.put("temp5", Float.valueOf(svc.getTemp5()));
        values.put("tempPh", Float.valueOf(svc.getTempPh()));
        values.put("tempAmb", Float.valueOf(svc.getTempAmb()));
        values.put("pH", Float.valueOf(svc.getpH()));

        long newSensedValueId = db.insert("SensedValues", null, values);
        dbHelper.close();
        return newSensedValueId; //si devuelve -1 es porque no pudo insertar
    }

    private String getListIdInsertedSensedValue(int idExp){
        StringBuilder buffer = new StringBuilder();

        DatabaseHelper dbHelper = new DatabaseHelper(getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] columns = {"idRaspi"};
        String selection = "id_exp = ?";
        String[] selectionArgs = { String.valueOf(idExp)};

        Cursor cursor = db.query("SensedValues", columns, selection, selectionArgs, null, null, null);
        if(cursor.moveToFirst()){
            buffer.append( cursor.getString(0)); // checkear si la columna es 0 o 1
            while(cursor.moveToNext()){
                buffer.append(",");
                buffer.append(cursor.getString(0));
            }
        }
        cursor.close();
        db.close();
        return buffer.toString();
    }

}