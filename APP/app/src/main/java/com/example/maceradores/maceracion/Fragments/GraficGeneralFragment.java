package com.example.maceradores.maceracion.Fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.maceradores.maceracion.R;

/**
 * A simple {@link Fragment} subclass.
 */
public class GraficGeneralFragment extends Fragment {


    public GraficGeneralFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_grafic_general, container, false);
    }

}
