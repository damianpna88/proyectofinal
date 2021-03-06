package com.example.maceradores.maceracion.Fragments;


import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.maceradores.maceracion.R;
import com.example.maceradores.maceracion.adapters.FragmentChartAdapter;
import com.example.maceradores.maceracion.db.DatabaseHelper;
import com.example.maceradores.maceracion.models.Experiment;
import com.example.maceradores.maceracion.models.SensedValues;
import com.example.maceradores.maceracion.utils.Calculos;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple {@link Fragment} subclass.
 */
public class ChartExperimentsFragment extends Fragment {
    //UI
    private RecyclerView recyclerView;
    private RecyclerView.Adapter rvAdapter;
    private RecyclerView.LayoutManager layoutManager;

    private List<List<Float>> listSensedValues=new ArrayList<>();
    private int idMash;
    private float intervaloMedicion;
    private List<String> listDates;

    public ChartExperimentsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_chart_experiments, container, false);
        final int idMash = getArguments().getInt("idMash");
        this.idMash=idMash;


        List <Integer> listExp = getAllExperiments(this.idMash);
        Log.d("listExp size",String.valueOf(listExp.size()));

        //Cargo la Lista de listas de SensedValues ya promediados
        for(int i =0; i<listExp.size();i++){
            this.listSensedValues.add(getSensedValues(listExp.get(i)));
            Log.d("listSensedValues size",String.valueOf(listSensedValues.size()));
        }

        Log.d("listSensedValues size",String.valueOf(listSensedValues.size()));
        this.listDates = getListDates(idMash);


        // Inflate the layout for this fragment
        recyclerView = (RecyclerView) view.findViewById(R.id.recyclerViewChartExperiments);
        this.intervaloMedicion= intervaloMedicion(idMash)/60;
        rvAdapter =  new FragmentChartAdapter(this.listSensedValues,this.listDates,R.layout.item_list_chart_experiments,this.intervaloMedicion);

        layoutManager = new LinearLayoutManager(getActivity());
        recyclerView.setAdapter(rvAdapter);
        recyclerView.setLayoutManager(layoutManager);

        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        return view;
    }

    private List<Integer> getAllExperiments(int idMash) {
        List<Integer> resultados = new ArrayList<>();

        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] columns = {"id"};
        String selection = "maceracion = ? AND densidad IS NOT NULL";
        String[] selectionArgs = { String.valueOf(idMash)};

        Cursor cursor = db.query("Experimento", columns, selection, selectionArgs, null, null, null);
        List<Float> yieldList = new ArrayList<>();

        //List itemNames = new ArrayList<>();
        while(cursor.moveToNext()) {
            int id = cursor.getInt(
                    cursor.getColumnIndexOrThrow("id")
            );

            resultados.add(id);
        } // end while
        cursor.close();
        dbHelper.close();
        return resultados;
    } //end getAllExperiments

    private List<Float> getSensedValues(int idExp){

        List<Float> listSensedValues = new ArrayList<>();
        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        //TODO CAMBIAR RAWQUERY POR QUERY COMUN
        Cursor cursor = db.rawQuery("SELECT * FROM SensedValues WHERE id_exp = ? ", new String[] {String.valueOf(idExp)});
        while(cursor.moveToNext()){
            //if(cursor.moveToFirst()){


            float temp1 = cursor.getFloat(cursor.getColumnIndexOrThrow("temp1"));
            float temp2 = cursor.getFloat(cursor.getColumnIndexOrThrow("temp2"));
            float temp3 = cursor.getFloat(cursor.getColumnIndexOrThrow("temp3"));
            float temp4 = cursor.getFloat(cursor.getColumnIndexOrThrow("temp4"));

            Float TempMean= validatedTempMean(temp1,temp2,temp3,temp4);
//            Log.d("TempMean",String.valueOf(TempMean));

            listSensedValues.add(TempMean);
        }
        cursor.close();
        db.close();
        return listSensedValues;
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
        if(intervaloMedicion<60)intervaloMedicion=60; // Minimo Intervalo de Medicion es de 30 seg

        return intervaloMedicion;
    }//Si lo divido por 2 tengo el int de Med de Temo en Seg!

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

    private List<String> getListDates(int idMash){
        List<String> resultados = new ArrayList<>();
        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        // Filter results WHERE "title" = 'My Title'
        //String selection = "id = ?";
        String[] selectionArgs = { String.valueOf(idMash)};

        Cursor cursor = db.rawQuery("SELECT E.id AS 'id', strftime('%d/%m/%Y %H:%M', E.fecha) AS 'fecha' FROM Experimento AS E INNER JOIN Maceracion AS M ON E.maceracion = M.id WHERE M.id = ? ORDER BY E.fecha DESC", selectionArgs);

        //List itemNames = new ArrayList<>();
        while(cursor.moveToNext()) {
            String fecha = cursor.getString(
                    cursor.getColumnIndexOrThrow("fecha")
            );

            resultados.add(fecha);
        } // end while
        cursor.close();
        dbHelper.close();
        return resultados;
    }

}
