package com.example.maceradores.maceracion.activities;

import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.example.maceradores.maceracion.R;
import com.example.maceradores.maceracion.adapters.GrainListAdapter;
import com.example.maceradores.maceracion.adapters.IntervalListAdapter;
import com.example.maceradores.maceracion.db.DatabaseHelper;
import com.example.maceradores.maceracion.models.Grain;
import com.example.maceradores.maceracion.models.Mash;
import com.example.maceradores.maceracion.models.MeasureInterval;
import com.example.maceradores.maceracion.utils.Calculos;

import java.util.ArrayList;

import java.util.Arrays;
import java.util.List;


public class PlanningActivity extends AppCompatActivity {
    //Buttons
    private Button buttonAddGrain;
    private FloatingActionButton fab;

    //flag.
    private boolean planned = false;


    //Container
    Spinner spinner;
    ArrayAdapter<CharSequence> adapterSpinner;

    private ListView listGrains;
    private GrainListAdapter grainListAdapter;

    private RecyclerView listsIntervals;
    private RecyclerView.LayoutManager layoutManager;
    private IntervalListAdapter intervalListAdapter;

    //Data - Fields to create the new mash.
    private Mash mash;
    private float rendimientoPractico = -1;

    // LifeCycle functions.
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_planning);

        this.mash = new Mash();
        Intent intent = getIntent();
        if(intent.hasExtra("idMash")){
            this.planned = true;
            int idMash = intent.getIntExtra("idMash", -1);
            mash.setId(idMash);

        }

        chargeUI();

        if(planned){
            fillUI(mash.getId());
            // tengo que deshabilitar el boton del action bar.
            blockUI();
        }


    } //end onCreate

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if( !this.planned){
            getMenuInflater().inflate(R.menu.action_bar_planning_activity, menu);
            return super.onCreateOptionsMenu(menu);
        }
        return false;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()){
            case R.id.acceptPlannification:
                //Me robo los valores de los editText correspondientes a volumen y densidad.
                EditText volumePlanning = findViewById(R.id.editTextPlanningVolumen);
                if( volumePlanning.getText().toString().isEmpty()){
                    Toast.makeText(this, "No se insertó el volumen de maceración", Toast.LENGTH_SHORT).show();
                    return false;
                }
                EditText densityPlanning = findViewById(R.id.editTextPlanningDensidad);
                if( densityPlanning.getText().toString().isEmpty()){
                    Toast.makeText(this, "No se insertó la densidad deseada de maceración", Toast.LENGTH_SHORT).show();
                    return false;
                }
                //this.volume = Float.valueOf(volumePlanning.getText().toString().trim());
                mash.setVolumen(Float.valueOf(volumePlanning.getText().toString().trim()));
                //this.density = Float.valueOf(densityPlanning.getText().toString().trim());
                mash.setDensidadObjetivo(Float.valueOf(densityPlanning.getText().toString().trim()));
                //y el correspondiente al tipo de maceracion.
                Spinner spinner = findViewById(R.id.spinnerTiposMaceracion);
                //this.type = spinner.getSelectedItem().toString().trim();
                mash.setTipo(spinner.getSelectedItem().toString().trim());

                //válido los granos y los intervalos.
                //if(grains.isEmpty()){
                if(mash.getGrains().isEmpty()){
                    Toast.makeText(this, "No se insertó ningun grano para la maceración", Toast.LENGTH_SHORT).show();
                    return false;
                }
                if(mash.getPlan().isEmpty()){
                    Toast.makeText(this, "No se insertó ningun intervalo de medición para la maceración", Toast.LENGTH_SHORT).show();
                    return false;
                }
                showAlertDialogFinishPlanning();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;
        //menu.setHeaderTitle( this.grains.get(info.position).getName());
        menu.setHeaderTitle( mash.getGrains().get(info.position).getName());
        inflater.inflate(R.menu.context_menu_grains, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        //manejamos los eventos
        switch (item.getItemId()){
            case R.id.deleteGrainsContextMenu:
                // aprete en el context menu la opcion eliminar.
                //tengo que obtener la posicion en la que estoy y eliminarla.
                //this.grains.remove(info.position);
                mash.removeGrain(info.position);
                //notifico a los adaptadores
                this.grainListAdapter.notifyDataSetChanged();
                Toast.makeText(PlanningActivity.this, "Grano eliminado", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    //User interface functions
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void chargeUI(){

        spinner = (Spinner) findViewById(R.id.spinnerTiposMaceracion);
        // Create an ArrayAdapter using the string array and a default spinner layout
        adapterSpinner = ArrayAdapter.createFromResource(this,
                R.array.tiposMaceracion, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapterSpinner);

        // List of Grains
        //grains = new ArrayList<Grain>();
        //mash.setGrains(grains);
        mash.setGrains(new ArrayList<Grain>());
        listGrains = (ListView) findViewById(R.id.listViewPlanningGrains);
        //grainListAdapter = new GrainListAdapter(this, grains, R.layout.item_list_grain);
        grainListAdapter = new GrainListAdapter(this, mash, planned, R.layout.item_list_grain, this.rendimientoPractico);
        listGrains.setAdapter(grainListAdapter);
        registerForContextMenu(this.listGrains);

        // List of intervals
        mash.setPlan(new ArrayList<MeasureInterval>());
        layoutManager = new LinearLayoutManager(this);
        if(planned){
            intervalListAdapter = new IntervalListAdapter(mash, planned, R.layout.item_list_interval, null);
        } else {
            intervalListAdapter = new IntervalListAdapter(mash, planned, R.layout.item_list_interval, new IntervalListAdapter.onItemClickListener() {
                @Override
                public void onItemClick(MeasureInterval interval, int position) {
                    Toast.makeText(PlanningActivity.this, "Intervalo Borrado", Toast.LENGTH_SHORT).show();
                    mash.removeMeasureInterval(position);
                    //intervals.remove(position);
                    //intervalListAdapter.notifyItemRemoved(position);
                    intervalListAdapter.notifyDataSetChanged();
                }
            });
        }

        listsIntervals = (RecyclerView) findViewById(R.id.recyclerViewIntervalPlanning);
        listsIntervals.setAdapter(intervalListAdapter);
        listsIntervals.setLayoutManager(layoutManager);
        //listsIntervals.setHasFixedSize(true);

        //Add grain
        buttonAddGrain = findViewById(R.id.buttonPlanningAddGrain);
        buttonAddGrain.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAlertDialogAddGrain();
            }
        });

        //Add measure Interval
        fab = (FloatingActionButton) findViewById(R.id.fabAddMeasureInterval);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String tipo = spinner.getSelectedItem().toString();
                showAlertDialogAddMeasureInterval(tipo);
                //intervalListAdapter.notifyDataSetChanged();
            }
        });

        //Ocultar el teclado cuando arranca el activity... es bastante molesto
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        setToolbar();
    }

    private float getRendimientoPractico(int idMash) {
        //hago la consulta de la base de datos.
        // me traigo la lista de id de experiencias.
        DatabaseHelper dbHelper = new DatabaseHelper(getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] columns = {"densidad"};
        String selection = "maceracion = ? AND densidad IS NOT NULL";
        String[] selectionArgs = { String.valueOf(idMash)};

        Cursor cursor = db.query("Experimento", columns, selection, selectionArgs, null, null, null);
        List<Float> yieldList = new ArrayList<>();
        float volMosto = mash.getVolumen();
        double kgMalta = mash.kgMalta();

        while( cursor.moveToNext()){
            double yield = Calculos.calcRendimiento(volMosto, cursor.getFloat(0), kgMalta)[2]; //este dos es porque el tercer valor es el rendimiento
            yieldList.add( (float) yield);
        }
        cursor.close();
        dbHelper.close();

        if(yieldList.size() < 3){
            return -1;
        } else {
            //devuelvo el promedio.
            float acumulado = 0;
            for( int i = 0; i < yieldList.size(); i++){
                acumulado = acumulado + yieldList.get(i);
            }
            return acumulado / yieldList.size();
        }


    }

    private void blockUI() {
        // I need to block all elements. or can i block the complete activity
        FrameLayout frameLayout = (FrameLayout) findViewById(R.id.framePlanning);
        blockView(frameLayout);
        // necesito bloquear el menu tambien...
    }

    private void blockView(View view){
        if(view instanceof ViewGroup){
            ViewGroup v = (ViewGroup) view;
            for( int i=0; i < v.getChildCount(); i++){
                if(v.getChildAt(i) instanceof Button || v.getChildAt(i) instanceof FloatingActionButton){
                    v.removeView(v.getChildAt(i));
                }else {
                    blockView(v.getChildAt(i));
                }
            }
            v.setEnabled(false);
        } else {
            //es un view pelado. Lo bloqueo a lo pampa-
            //view.setEnabled(false);
            view.setFocusable(false);
            view.setClickable(false);
        }
    }

    private void fillUI(int idMash) {
        DatabaseHelper dbHelper = new DatabaseHelper(getApplicationContext());
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        fillUIMash( idMash, db );
        fillUIGrain(idMash, db);
        fillUIInterval(idMash, db);

        dbHelper.close();
    }

    private void fillUIMash(int idMash, SQLiteDatabase db ){
        // Filter results WHERE "title" = 'My Title'
        String selection = "id = ?";
        String[] selectionArgs = { String.valueOf(idMash)};

        Cursor cursor = db.query("Maceracion", null, selection, selectionArgs, null, null, null);
        if( cursor.moveToFirst()){
            // primero pongo el titulo con el nombre de la maceracion
            String nameMash = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
            setTitle("Planificación " + nameMash);

            //tipo de maceracion : spinner.
            mash.setTipo(cursor.getString(cursor.getColumnIndexOrThrow("tipo")));
            //type = cursor.getString(cursor.getColumnIndexOrThrow("tipo"));
            int spinnerPosition = adapterSpinner.getPosition(mash.getTipo());
            spinner.setSelection(spinnerPosition);

            // volumen
            mash.setVolumen(cursor.getFloat(cursor.getColumnIndexOrThrow("volumen")));
            //volume = cursor.getFloat(cursor.getColumnIndexOrThrow("volumen"));
            EditText volumePlanning = findViewById(R.id.editTextPlanningVolumen);
            volumePlanning.setText(String.valueOf(mash.getVolumen()));

            //densidad
            mash.setDensidadObjetivo(cursor.getFloat(cursor.getColumnIndexOrThrow("densidadObjetivo")));
            //density = cursor.getFloat(cursor.getColumnIndexOrThrow("densidadObjetivo"));
            EditText densityPlanning = findViewById(R.id.editTextPlanningDensidad);
            densityPlanning.setText(String.valueOf(mash.getDensidadObjetivo()));

            // A los intervalos de medicion mandale saludos a cagaste. De la forma que lo plantemaos.
            // no es facil mostrarlo.
            // TODO Hacer algo para mostrar los intervalos de medición.

            /*intervaloMedTemp INTEGER, " +
            "intervaloMedPh INTEGER)");*/
        }
        cursor.close();
    }

    private void fillUIGrain(int idMash, SQLiteDatabase db){
        // Granos
        String selection = "maceracion = ?";
        String [] selectionArgs = new String[] { String.valueOf(idMash)};
        Cursor cursor = db.query("Grano", null, selection, selectionArgs, null, null, null);
        // Puedo y seguramente voy a tener mas de un grano.
        while(cursor.moveToNext()){
            /* db.execSQL("CREATE TABLE Grano(" +
                "id INTEGER PRIMARY KEY, " +
                "nombre VARCHAR(190), " +
                "cantidad FLOAT, " +
                "extractoPotencial FLOAT, " +
                "maceracion INTEGER," +
                "FOREIGN KEY (maceracion) REFERENCES Maceracion(id))");*/
            String name = cursor.getString(cursor.getColumnIndexOrThrow("nombre"));
            Float quantity = cursor.getFloat(cursor.getColumnIndexOrThrow("cantidad"));
            Float extract = cursor.getFloat(cursor.getColumnIndexOrThrow("extractoPotencial"));
             //con estos tres valores puedo crear el grano y agregarlo.
            Grain grain = new Grain(name, quantity, extract);
            //grains.add(grain);
            mash.addGrain(grain);
        }//end while

        this.rendimientoPractico = getRendimientoPractico(this.mash.getId());
        if(rendimientoPractico != -1){
            Log.d("PlanningActivity", "el rendimiento practico es: " + rendimientoPractico);
            grainListAdapter = new GrainListAdapter(this, this.mash, this.planned,R.layout.item_list_grain, this.rendimientoPractico );
            listGrains.setAdapter(grainListAdapter);
        } else{
            grainListAdapter.notifyDataSetChanged();
        }





        cursor.close();
    }

    private void fillUIInterval( int idMash, SQLiteDatabase db){
        //Intervalos.
        String selection = "maceracion = ?";
        String [] selectionArgs = new String[] { String.valueOf(idMash)};
        Cursor cursor = db.query("Intervalo", null, selection, selectionArgs, null, null, "orden ASC");
        // Puedo tener mas de un intervalo. Hacemos un while.
        while(cursor.moveToNext()){
            /*         db.execSQL("CREATE TABLE Intervalo(" +
                "id INTEGER PRIMARY KEY, " +
                "orden INTEGER,"+
                "duracion INTEGER," +  //minutos. deberia ser un flotante?
                "temperatura FLOAT, " +
                "desvioTemperatura FLOAT,"+
                "ph FLOAT," +
                "desvioPh FLOAT,"+
                "tempDecoccion FLOAT, " +
                "desvioTempDecoccion FLOAT,"+
                "maceracion INTEGER, " +
                "FOREIGN KEY (maceracion) REFERENCES Maceracion(id))");*/
            int order = cursor.getInt(cursor.getColumnIndexOrThrow("orden"));
            int duration = cursor.getInt(cursor.getColumnIndexOrThrow("duracion"));

            float temperature = cursor.getFloat(cursor.getColumnIndexOrThrow("temperatura"));
            float temperatureDeviation = cursor.getFloat(cursor.getColumnIndexOrThrow("desvioTemperatura"));

            float ph = cursor.getFloat(cursor.getColumnIndexOrThrow("ph"));
            float phDeviation = cursor.getFloat(cursor.getColumnIndexOrThrow("desvioPh"));

            float temperatureDecoccion = cursor.getFloat(cursor.getColumnIndexOrThrow("tempDecoccion"));
            float temperatureDecoccionDeviation = cursor.getFloat(cursor.getColumnIndexOrThrow("desvioTempDecoccion"));

            //con estos tres valores puedo crear el intervalo y agregarlo.
            MeasureInterval interval = new MeasureInterval(order, temperature, temperatureDeviation, temperatureDecoccion, temperatureDecoccionDeviation, ph, phDeviation, duration);
            //intervals.add(interval);
            mash.addMeasureInterval(interval);
        }//end while
        intervalListAdapter.notifyDataSetChanged();

        cursor.close();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)//Esto es para que me deje usar el Toolbar q empieza e la APU 24
    private void setToolbar(){
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_planning);
        setSupportActionBar(toolbar);
        getSupportActionBar().show();
    }

    private void showAlertDialogFinishPlanning() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Guardar Maceración");

        View finishPlanningView = LayoutInflater.from(this).inflate(R.layout.dialog_finish_planning, null);
        builder.setView(finishPlanningView);
        // Necesito las referecias a los editText
        final EditText nombre = (EditText) finishPlanningView.findViewById(R.id.editTextFinishPlanningName);
        final EditText medTemp = (EditText) finishPlanningView.findViewById(R.id.editTextFinishPlanningMedTemp);
        final EditText medPh = (EditText) finishPlanningView.findViewById(R.id.editTextFinishPlanningMedPh);

        builder.setPositiveButton("ACEPTAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                // Este es el momento donde debería crear el mash.
                // Me robo los valores de los edit text
                if(     nombre.getText().toString().isEmpty() ||
                        medTemp.getText().toString().isEmpty() ||
                        medPh.getText().toString().isEmpty()){
                    Toast.makeText(PlanningActivity.this, "No se guardo. Hay algun campo incompleto", Toast.LENGTH_SHORT).show();
                    //dialog.cancel();
                } else {
                    //save values. Note the beautiful development without any violation
                    //PlanningActivity.this.nameMash = nombre.getText().toString().trim();
                    PlanningActivity.this.mash.setName(nombre.getText().toString().trim());
                    //PlanningActivity.this.periodoMedicionTemp = Integer.valueOf(medTemp.getText().toString().trim());
                    PlanningActivity.this.mash.setPeriodMeasureTemperature(Integer.valueOf(medTemp.getText().toString().trim()));
                    //PlanningActivity.this.periodoMedicionPh = Integer.valueOf(medPh.getText().toString().trim());
                    PlanningActivity.this.mash.setPeriodMeasurePh(Integer.valueOf(medPh.getText().toString().trim()));

                    if(mash.validateMash()){

                        mash.setTipo(spinner.getSelectedItem().toString());

                        insertNewPlanning();
                        //startActivity(new Intent(PlanningActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(PlanningActivity.this, "Los intervalos de medicion insertados no son válidos", Toast.LENGTH_SHORT).show();
                    }

                } //end if validate planning
            }
        }); //end Accept Button

        builder.setNegativeButton("CANCELAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.create().show();

    } //end finish alert dialog

    private void showAlertDialogAddMeasureInterval(final String tipo) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Nuevo Intervalo");
        //builder.setTitle(tipo);

        View addIntervalView = LayoutInflater.from(this).inflate(R.layout.dialog_add_measure_interval, null);

        //le pongo el numerito de etapa que iria a agregar.
        TextView numberInterval = (TextView) addIntervalView.findViewById(R.id.textViewNumberInterval);
        //numberInterval.append(String.valueOf(intervals.size() + 1));
        numberInterval.append(String.valueOf(mash.getPlan().size() + 1));

        //Necesito todas las referencias a los editText.
        final EditText duration = (EditText) addIntervalView.findViewById(R.id.editTextAddIntervalDuration);
        final EditText temperature = (EditText) addIntervalView.findViewById(R.id.editTextAddIntervalTemperature);
        final EditText temperatureDeviation = (EditText) addIntervalView.findViewById(R.id.editTextAddIntervalTemperatureDeviation);
        final EditText ph = (EditText) addIntervalView.findViewById(R.id.editTextAddIntervalPh);
        final EditText phDeviation = (EditText) addIntervalView.findViewById(R.id.editTextAddIntervalPhDeviation);
        final EditText tempDecoccion = (EditText) addIntervalView.findViewById(R.id.editTextAddIntervalTemperatureDecoccion);
        final EditText tempDecoccionDeviation = (EditText) addIntervalView.findViewById(R.id.editTextAddIntervalTemperatureDecoccionDeviation);


        if( ! tipo.equals("Decocción")){
            LinearLayout linearLayoutDecoccion = addIntervalView.findViewById(R.id.linearLayoutDecoccion);
            LinearLayout linearLayoutDecoccionDeviation = addIntervalView.findViewById(R.id.linearLayoutDecoccionDeviation);
            LinearLayout linearLayoutPlanning = addIntervalView.findViewById(R.id.linearLayoutPlanningMash);
            linearLayoutPlanning.removeView(linearLayoutDecoccion);
            linearLayoutPlanning.removeView(linearLayoutDecoccionDeviation);
        }

        builder.setView(addIntervalView);

        builder.setPositiveButton("ACEPTAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //Aca debo tomar los valores y usarlos para llenar un IntervalMeasure.
                if(     temperature.getText().toString().isEmpty() ||
                        temperatureDeviation.getText().toString().isEmpty() ||
                        //tempDecoccion.getText().toString().isEmpty() ||
                        //tempDecoccionDeviation.getText().toString().isEmpty() ||
                        ph.getText().toString().isEmpty() ||
                        phDeviation.getText().toString().isEmpty()){
                    Toast.makeText(PlanningActivity.this, "No se insertó intervalo. Algun campo vacío", Toast.LENGTH_SHORT).show();
                } else{
                    if(tipo.equals("Decocción")){
                        MeasureInterval interval = new MeasureInterval(
                                Float.valueOf(temperature.getText().toString().trim()),
                                Float.valueOf(temperatureDeviation.getText().toString().trim()),
                                Float.valueOf(tempDecoccion.getText().toString().trim()),
                                Float.valueOf(tempDecoccionDeviation.getText().toString().trim()),
                                Float.valueOf(ph.getText().toString().trim()),
                                Float.valueOf(phDeviation.getText().toString().trim()),
                                Integer.valueOf(duration.getText().toString().trim())
                        );

                        //Ahora tengo que agregarlo a la lista de intervalos
                        addInterval(interval);
                    } else {
                        MeasureInterval interval = new MeasureInterval(
                                Float.valueOf(temperature.getText().toString().trim()),
                                Float.valueOf(temperatureDeviation.getText().toString().trim()),
                                Float.valueOf(ph.getText().toString().trim()),
                                Float.valueOf(phDeviation.getText().toString().trim()),
                                Integer.valueOf(duration.getText().toString().trim())
                        );

                        //Ahora tengo que agregarlo a la lista de intervalos
                        addInterval(interval);
                    }


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

    } //end Add Measure alert dialog

    private void showAlertDialogAddGrain() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Agregar Grano");

        View addGrainView = LayoutInflater.from(this).inflate(R.layout.dialog_add_grain, null);
        builder.setView(addGrainView);

        final EditText grainName = (EditText) addGrainView.findViewById(R.id.editTextGrainName);
        final EditText grainQuantity = (EditText) addGrainView.findViewById(R.id.editTextGrainQuantity);
        final EditText grainExtractPotential = (EditText) addGrainView.findViewById(R.id.editTextGrainExtractPotential);

        builder.setPositiveButton("AGREGAR", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if(     grainName.getText().toString().isEmpty() ||
                        grainQuantity.getText().toString().isEmpty() ||
                        grainExtractPotential.getText().toString().isEmpty()
                        ) {
                    Toast.makeText(PlanningActivity.this, "No se pudo insertar el grano porque faltaron completar campos", Toast.LENGTH_SHORT).show();
                } else {
                    String name = grainName.getText().toString().trim();
                    float quantity = Float.valueOf(grainQuantity.getText().toString());
                    float extractPotential = Float.valueOf(grainExtractPotential.getText().toString());

                    Grain grain = new Grain(name, quantity, extractPotential);
                    //grains.add(grain);
                    mash.addGrain(grain);

                    grainListAdapter.notifyDataSetChanged();
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
    } //end Add Grain alert dialog

    //BD functions
    private void addInterval(MeasureInterval interval){
        //this.intervals.add(interval);
        mash.addMeasureInterval(interval);
        this.intervalListAdapter.notifyDataSetChanged();
    }

    private void insertNewPlanning(){
        DatabaseHelper dbHelper = new DatabaseHelper(getApplicationContext());
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        //Ahora puedo escribir en la base de datos,
        ContentValues mashValues = new ContentValues();
        //mashValues.put( "nombre", nameMash); //el nombre tiene la clausula unique
        mashValues.put( "nombre", mash.getName()); //el nombre tiene la clausula unique
        //mashValues.put( "tipo", PlanningActivity.this.type);
        mashValues.put( "tipo", PlanningActivity.this.mash.getTipo());
        //mashValues.put( "volumen", PlanningActivity.this.volume);
        mashValues.put( "volumen", PlanningActivity.this.mash.getVolumen());
        //mashValues.put( "densidadObjetivo", PlanningActivity.this.density);
        mashValues.put( "densidadObjetivo", PlanningActivity.this.mash.getDensidadObjetivo());
        //mashValues.put( "intervaloMedTemp", periodoMedicionTemp);
        mashValues.put( "intervaloMedTemp", PlanningActivity.this.mash.getPeriodMeasureTemperature());
        //mashValues.put( "intervaloMedPh", periodoMedicionPh);
        mashValues.put( "intervaloMedPh", mash.getPeriodMeasurePh());


        long newMashId = db.insert("Maceracion", null, mashValues);

        // cuenta la leyenda que en newRowId tengo el id del ultimo valor insertado.
        if( newMashId != -1){
            //Toast.makeText(PlanningActivity.this, "Inserto sin problemas", Toast.LENGTH_SHORT).show();
            // Si inserto la maceración, tengo que insertar ademas los granos y las etapas de medicion.
            // Comencemos por los granos.
            ContentValues grainValues;
            //for(int i = 0; i < grains.size(); i++){
            for(int i = 0; i < mash.getGrains().size(); i++){
                grainValues = new ContentValues();
                // TODO hacerlo sin violacion de clases
                //grainValues.put("nombre", grains.get(i).getName());
                grainValues.put("nombre", mash.getGrains().get(i).getName());
                //grainValues.put("cantidad", grains.get(i).getQuantity());
                grainValues.put("cantidad", mash.getGrains().get(i).getQuantity());
                //grainValues.put("extractoPotencial", grains.get(i).getExtractPotential());
                grainValues.put("extractoPotencial", mash.getGrains().get(i).getExtractPotential());
                grainValues.put("maceracion", newMashId);

                long newGrainId = db.insert("Grano",null, grainValues );
                if(newGrainId == -1){
                    Toast.makeText(PlanningActivity.this, "Hubo problemas insertando este grano", Toast.LENGTH_SHORT).show();
                }
            } // end fir agregado de granos.

            //Ahora agregamos los intervalos de medicion.
            ContentValues intervalValues;
            //for( int i = 0; i < intervals.size(); i++){
            for( int i = 0; i < mash.getPlan().size(); i++){
                intervalValues = new ContentValues();

                intervalValues.put("orden", i+1); // como le quedo definido, los pongo en ese orden
                //intervalValues.put("duracion", intervals.get(i).getDuration());
                intervalValues.put("duracion", mash.getPlan().get(i).getDuration());
                intervalValues.put("temperatura", mash.getPlan().get(i).getMainTemperature());
                intervalValues.put("desvioTemperatura", mash.getPlan().get(i).getMainTemperatureDeviation());
                intervalValues.put("ph", mash.getPlan().get(i).getpH());
                intervalValues.put("desvioPh", mash.getPlan().get(i).getPhDeviation());
                intervalValues.put("tempDecoccion", mash.getPlan().get(i).getSecondTemperature());
                intervalValues.put("desvioTempDecoccion", mash.getPlan().get(i).getSecondTemperatureDeviation());
                intervalValues.put("maceracion", newMashId);

                long newIntervalId = db.insert("Intervalo", null, intervalValues);
                if(newIntervalId == -1)
                    Toast.makeText(PlanningActivity.this, "Problemas insertando intervalo", Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(PlanningActivity.this, "Problemas al insertar", Toast.LENGTH_SHORT).show();
        }
        // chequeamos si me toma los cambios.
        dbHelper.close();
        Toast.makeText(this, "Maceración correctamente planificada", Toast.LENGTH_SHORT).show();
    }
} //end PlanningActivity
