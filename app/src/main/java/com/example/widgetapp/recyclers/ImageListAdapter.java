package com.example.widgetapp.recyclers;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.widgetapp.R;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

public class ImageListAdapter extends RecyclerView.Adapter<ImageListAdapter.MyViewHolder>{

    Context context;
    List<String> uploadedFileNames;
    Callback<String> callback;

    public interface Callback<T>{
        void onResult(T value);
    }

    public ImageListAdapter(Context context, List<String> uploadedFileNames, Callback<String> callback) {
        this.context = context;
        this.uploadedFileNames = uploadedFileNames;
        this.callback = callback;
    }


    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.images_list_preference_card, parent, false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        String uploadedFileName = uploadedFileNames.get(position);

        FileInputStream fileInputStream;
        try {
            fileInputStream = context.openFileInput(uploadedFileName);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        Bitmap bitmap = BitmapFactory.decodeStream(fileInputStream);
        try {
            fileInputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        holder.images_list_preference_card_image.setImageBitmap(bitmap);


        holder.images_list_preference_card_button.setOnClickListener(v -> {
            callback.onResult(uploadedFileName);
        });
    }

    @Override
    public int getItemCount() { return uploadedFileNames.size(); }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView images_list_preference_card_image;
        Button images_list_preference_card_button;

        public MyViewHolder(@NonNull View itemView) {

            super(itemView);

            images_list_preference_card_image = itemView.findViewById(R.id.image_list_preference_card_image);
            images_list_preference_card_button = itemView.findViewById(R.id.image_list_preference_card_button);
        }
    }
}
