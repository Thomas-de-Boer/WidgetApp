package com.example.widgetapp.recyclers;

import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.widgetapp.MainActivity;
import com.example.widgetapp.R;
import com.example.widgetapp.WidgetSettingsListener;
import com.example.widgetapp.fragments.ImagesSettingsFragment;
import com.example.widgetapp.fragments.QuotesSettingsFragment;

import java.util.HashMap;
import java.util.List;

public class RecyclerItemAdapter extends RecyclerView.Adapter<RecyclerItemAdapter.MyViewHolder> {

    private final List<WidgetItem> widgetItemList;
    public AppWidgetManager appWidgetManager;
    public AppWidgetHost appWidgetHost;
    public Context context;
    public WidgetSettingsListener listener;

    // Constructor
    public RecyclerItemAdapter(List<WidgetItem> widgetItemList, AppWidgetManager appWidgetManager, AppWidgetHost appWidgetHost, Context context, WidgetSettingsListener listener ) {
        this.widgetItemList = widgetItemList;
        this.appWidgetManager = appWidgetManager;
        this.appWidgetHost = appWidgetHost;
        this.context = context;
        this.listener = listener;
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

        viewHolder.widget_item_overlay.setOnClickListener(v -> listener.onWidgetSettingsRequested(widgetItem.get_type()));
    }

    @Override
    public int getItemCount() {
        return widgetItemList.size();
    }

    // ViewHolder class
    public static class MyViewHolder extends RecyclerView.ViewHolder {
        FrameLayout widget_item_widget_view;
        View widget_item_overlay;


        public MyViewHolder(@NonNull View itemView) {

            super(itemView);

            widget_item_widget_view = itemView.findViewById(R.id.widget_item_widget_view);
            widget_item_overlay = itemView.findViewById(R.id.widget_item_overlay);
        }
    }
}