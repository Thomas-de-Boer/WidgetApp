package com.example.widgetapp;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class RecyclerRowAdapter extends RecyclerView.Adapter<RecyclerRowAdapter.MyViewHolder> {

    private final List<WidgetItem> widgetItemList;
    private final List<WidgetItem> widgetItemListType;
    private final List<WidgetItem.widgetTypes> widgetTypes;
    public AppWidgetManager appWidgetManager;
    public AppWidgetHost appWidgetHost;
    public Context context;

    // Constructor
    public RecyclerRowAdapter(List<WidgetItem.widgetTypes> widgetTypes, List<WidgetItem> widgetItemList, AppWidgetManager appWidgetManager, AppWidgetHost appWidgetHost, Context context) {
        this.widgetItemList = widgetItemList;
        this.appWidgetManager = appWidgetManager;
        this.appWidgetHost = appWidgetHost;
        this.context = context;
        this.widgetTypes = widgetTypes;
        widgetItemListType = new ArrayList<>();
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.widget_item_row, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder viewHolder, int position) {
        WidgetItem.widgetTypes widgetType = widgetTypes.get(position);

        if (widgetType == WidgetItem.widgetTypes.QUOTES) {
            viewHolder.widget_item_row_text.setText("Quote");
        }
        else if (widgetType == WidgetItem.widgetTypes.IMAGES) {
            viewHolder.widget_item_row_text.setText("Image");
        }
        else if (widgetType == WidgetItem.widgetTypes.IMAGES_SMALL) {
            viewHolder.widget_item_row_text.setText("Image small");
        }


        widgetItemListType.clear();
        for (WidgetItem widgetItem : widgetItemList) {
            if (widgetItem.get_type() == widgetType) {
                widgetItemListType.add(widgetItem);
            }
        }


        RecyclerItemAdapter adapter = new RecyclerItemAdapter(widgetItemListType, appWidgetManager, appWidgetHost , context);


        viewHolder.widget_item_row_recycler.setLayoutManager(new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
        viewHolder.widget_item_row_recycler.setAdapter(adapter);


//        View view = appWidgetHost.createView(context, widgetItem.get_id(), appWidgetManager.getAppWidgetInfo(widgetItem.get_id()));
//
//        viewHolder.widget_item_widget_view.removeAllViews();
//        viewHolder.widget_item_widget_view.addView(view);


//        viewHolder.widget_item_AppWidgetHostView.setAppWidget(widgetItem.get_id(), appWidgetManager.getAppWidgetInfo(widgetItem.get_id()));
    }

    @Override
    public int getItemCount() {
        return widgetTypes.size();
    }

    // ViewHolder class
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView widget_item_row_text;
        RecyclerView widget_item_row_recycler;

        public MyViewHolder(@NonNull View itemView) {

            super(itemView);

            widget_item_row_text = itemView.findViewById(R.id.widget_item_row_text);
            widget_item_row_recycler = itemView.findViewById(R.id.widget_item_row_recycler);
        }
    }
}