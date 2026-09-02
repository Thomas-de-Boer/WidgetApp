package com.example.widgetapp.fragments;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.widgetapp.R;
import com.example.widgetapp.WidgetSettingsListener;
import com.example.widgetapp.recyclers.RecyclerRowAdapter;
import com.example.widgetapp.recyclers.WidgetItem;
import com.example.widgetapp.widgets.image.WidgetProviderImages;
import com.example.widgetapp.widgets.quote.WidgetProviderQuotes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WidgetFragment extends Fragment {
    Context context;
    AppWidgetManager appWidgetManager;
    RecyclerRowAdapter adapter;
    View text;
    RecyclerView recyclerView;
    AppWidgetHost appWidgetHost;
    FragmentManager fragmentManager;


    HashMap<Class<? extends AppWidgetProvider>, WidgetItem.widgetTypes> classAndEnums = new HashMap<>();
    List<WidgetItem> widgetList = new ArrayList<>();
    List<WidgetItem.widgetTypes> widgetTypeList = new ArrayList<>();

    public WidgetFragment() {
        classAndEnums.put(WidgetProviderQuotes.class, WidgetItem.widgetTypes.QUOTES);
        classAndEnums.put(WidgetProviderImages.class, WidgetItem.widgetTypes.IMAGES);
    }


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

//          Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_widgets, container, false);
    }

    @MainThread
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

//        initiate a bunch of variables
        context = requireActivity().getApplicationContext();
        appWidgetManager = AppWidgetManager.getInstance(context);
//        haha 67
        appWidgetHost = new AppWidgetHost(context, 67);
        fragmentManager = requireActivity().getSupportFragmentManager();

        adapter = new RecyclerRowAdapter(widgetTypeList, widgetList, appWidgetManager, appWidgetHost , context, fragmentManager, (WidgetSettingsListener) getActivity());

        text = view.findViewById(R.id.fragment_widgets_text);
        recyclerView = view.findViewById(R.id.fragment_widgets_recyclerview);

//        start listening for widget updates
        appWidgetHost.startListening();

//        fill lists with WidgetItems and widgetTypes
        widgetList = buildWidgetList();
        widgetTypeList = buildWidgetTypeList(widgetList);

//        activate the recyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(context));
        recyclerView.setAdapter(adapter);

//        if the adapter is empty/filled make the explain text visible/invisible
        checkListFilled(adapter, text);
    }

    @Override
    public void onResume() {
        super.onResume();

//        build lists again to check for updates
        widgetList = buildWidgetList();
        widgetTypeList = buildWidgetTypeList(widgetList);

//        tell the adapter that dataset has changed so it updates the displayed widgets
        adapter.notifyDataSetChanged();

//        same as above... makes text visible/invisible
        checkListFilled(adapter, text);
    }

    @Override
    public void onStop() {
        super.onStop();

        appWidgetHost.stopListening();
    }

    public int[] getWidgetIds(Class<? extends AppWidgetProvider> widgetProviderClass) {
        ComponentName componentName = new ComponentName(context, widgetProviderClass);

//        return a list of widget ids int[]
        return appWidgetManager.getAppWidgetIds(componentName);
    }

    public List<WidgetItem> buildWidgetList() {
        widgetList.clear();

        for (Class<? extends AppWidgetProvider> provider: classAndEnums.keySet()) {
            int[] widgetIds = getWidgetIds(provider);

            for (int widgetId: widgetIds) {
                WidgetItem widgetItem = new WidgetItem(widgetId, classAndEnums.get(provider));

                widgetList.add(widgetItem);
            }
        }
        return widgetList;
    }

    public void checkListFilled(RecyclerRowAdapter adapter, View text) {
        if (adapter.getItemCount() != 0) {
            text.setVisibility(View.INVISIBLE);
        }
        else { text.setVisibility(View.VISIBLE); }
    }

    public List<WidgetItem.widgetTypes> buildWidgetTypeList(List<WidgetItem> widgetList) {
        widgetTypeList.clear();

        for (WidgetItem widgetItem: widgetList) {
            if (widgetTypeList.contains(widgetItem.widgetType)) {
                continue;
            }
            widgetTypeList.add(widgetItem.widgetType);
        }
        return widgetTypeList;
    }
}