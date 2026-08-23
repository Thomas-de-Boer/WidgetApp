package com.example.widgetapp;

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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.widgetapp.widgets.image.WidgetProviderImages;
import com.example.widgetapp.widgets.image.WidgetProviderImagesSmall;
import com.example.widgetapp.widgets.quote.WidgetProviderQuotes;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class WidgetFragment extends Fragment {
    Context context;
    AppWidgetManager appWidgetManager;
    RecyclerAdapter adapter;
    View text;
    AppWidgetHost appWidgetHost;


    HashMap<Class<? extends AppWidgetProvider>, WidgetItem.widgetTypes> classAndEnums = new HashMap<>();
    List<WidgetItem> widgetList = new ArrayList<>();

    public WidgetFragment() {
        classAndEnums.put(WidgetProviderQuotes.class, WidgetItem.widgetTypes.QUOTES);
        classAndEnums.put(WidgetProviderImages.class, WidgetItem.widgetTypes.IMAGES);
        classAndEnums.put(WidgetProviderImagesSmall.class, WidgetItem.widgetTypes.IMAGES_SMALL);
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

        context = requireActivity().getApplicationContext();
        appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetHost = new AppWidgetHost(context, 1);

        adapter = new RecyclerAdapter(widgetList, appWidgetManager, appWidgetHost , context);

        text = view.findViewById(R.id.fragment_widgets_text);

        appWidgetHost.startListening();


        RecyclerView recyclerView = view.findViewById(R.id.fragment_widgets_recyclerview);

        widgetList = buildWidgetList();

        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        recyclerView.setAdapter(adapter);


        if (adapter.getItemCount() != 0) {
            text.setVisibility(View.INVISIBLE);
        }
        else { text.setVisibility(View.VISIBLE); }
    }

    @Override
    public void onResume() {
        super.onResume();

        widgetList = buildWidgetList();

        adapter.notifyDataSetChanged();

        if (adapter.getItemCount() != 0) {
            text.setVisibility(View.INVISIBLE);
        }
        else { text.setVisibility(View.VISIBLE); }
    }

    @Override
    public void onStart() {
        super.onStart();


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
}