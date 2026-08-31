package com.example.widgetapp.recyclers;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.widgetapp.R;

import java.util.List;

public class RecyclerItemAdapter extends RecyclerView.Adapter<RecyclerItemAdapter.MyViewHolder> {

    private final List<WidgetItem> widgetItemList;
    public AppWidgetManager appWidgetManager;
    public AppWidgetHost appWidgetHost;
    public Context context;

    // Constructor
    public RecyclerItemAdapter(List<WidgetItem> widgetItemList, AppWidgetManager appWidgetManager, AppWidgetHost appWidgetHost, Context context) {
        this.widgetItemList = widgetItemList;
        this.appWidgetManager = appWidgetManager;
        this.appWidgetHost = appWidgetHost;
        this.context = context;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.widget_item, parent, false);
        return new MyViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull MyViewHolder viewHolder, int position) {
        WidgetItem widgetItem = widgetItemList.get(position);

//        if (widgetItem.widgetType == WidgetItem.widgetTypes.QUOTES) {
//            viewHolder.widget_item_text.setText("Quote");
//        }
//        else if (widgetItem.widgetType == WidgetItem.widgetTypes.IMAGES) {
//            viewHolder.widget_item_text.setText("Image");
//        }
//        else if (widgetItem.widgetType == WidgetItem.widgetTypes.IMAGES_SMALL) {
//            viewHolder.widget_item_text.setText("Image small");
//        }


        View view = appWidgetHost.createView(context, widgetItem.get_id(), appWidgetManager.getAppWidgetInfo(widgetItem.get_id()));

        view.setPadding(0, 0, 0, 0);

        viewHolder.widget_item_widget_view.removeAllViews();
        viewHolder.widget_item_widget_view.addView(view);


//        viewHolder.widget_item_AppWidgetHostView.setAppWidget(widgetItem.get_id(), appWidgetManager.getAppWidgetInfo(widgetItem.get_id()));
    }

    @Override
    public int getItemCount() {
        return widgetItemList.size();
    }

    // ViewHolder class
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        FrameLayout widget_item_widget_view;

        public MyViewHolder(@NonNull View itemView) {

            super(itemView);

            widget_item_widget_view = itemView.findViewById(R.id.widget_item_widget_view);
        }
    }
}