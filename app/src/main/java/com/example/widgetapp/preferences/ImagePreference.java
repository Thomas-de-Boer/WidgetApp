package com.example.widgetapp.preferences;

import static android.content.Context.MODE_PRIVATE;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.net.Uri;
import android.os.Bundle;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.example.widgetapp.R;
import com.example.widgetapp.SettingsManager;
import com.example.widgetapp.widgets.image.WidgetProviderImages;
import com.google.gson.reflect.TypeToken;
import com.google.gson.Gson;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ImagePreference extends PreferenceFragmentCompat {

    Preference uploadImage;
    AppWidgetManager widgetManager;

    ContentResolver contentResolver;
    List<String> uploadedFileNames;


    @Override
    public void onCreatePreferences(@Nullable Bundle savedInstanceState, @Nullable String rootKey) {
        setPreferencesFromResource(R.xml.preferences_images, rootKey);

        contentResolver = requireContext().getContentResolver();

        uploadedFileNames = new ArrayList<>();

        widgetManager = AppWidgetManager.getInstance(getContext());

        ActivityResultLauncher<PickVisualMediaRequest> pickMultipleMedia =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(5), uris -> {

                for (Uri uri: uris) {

                    String uuid = UUID.randomUUID().toString();

                    String fileName = "UploadedImages" + uuid;

                    try {
                        ByteArrayOutputStream buffer;
                        try (InputStream inputStream = contentResolver.openInputStream(uri); FileOutputStream fileOutputStream = requireContext().openFileOutput(fileName, MODE_PRIVATE)) {

                            buffer = new ByteArrayOutputStream();

                            int nRead;
                            byte[] data = new byte[4096];

                            while (true) {
                                assert inputStream != null;
                                if ((nRead = inputStream.read(data, 0, data.length)) == -1)
                                    break;
                                buffer.write(data, 0, nRead);
                            }

                            buffer.flush();
                            byte[] bytes = buffer.toByteArray();

                            fileOutputStream.write(bytes);

                            uploadedFileNames.add(fileName);
                        }

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }

//                SettingsManager.read(SettingsManager.UPLOADEDIMAGES, null, set -> {
//                    uploadedFileNames.addAll(set);
//                    HashSet<String> uploadedFileNamesSet = new HashSet<String>(uploadedFileNames);
//                    SettingsManager.write(SettingsManager.UPLOADEDIMAGES, uploadedFileNamesSet);
//                });

                SettingsManager.read(SettingsManager.UPLOADEDIMAGES, null, string -> {
                    Gson gson = new Gson();

                    if (string != null) {

                        Type listType = new TypeToken<ArrayList<String>>(){}.getType();
                        List<String> uploadedFileNamesOld = gson.fromJson(string, listType);

                        uploadedFileNames.addAll(uploadedFileNamesOld);
                    }

                    SettingsManager.write(SettingsManager.UPLOADEDIMAGES, gson.toJson(uploadedFileNames), s -> {
                        int[] appWidgetIds = widgetManager.getAppWidgetIds(new ComponentName(requireContext(), WidgetProviderImages.class));

                        WidgetProviderImages.update(getContext(), widgetManager, appWidgetIds);
                    });
                });

            });

        uploadImage = findPreference("upload_image");

        assert uploadImage != null;
        uploadImage.setOnPreferenceClickListener(preference -> {
            pickMultipleMedia.launch(new PickVisualMediaRequest.Builder().build());
            return false;
        });

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
//
//        widgetManager = AppWidgetManager.getInstance(getContext());
//
//        int[] appWidgetIds = widgetManager.getAppWidgetIds(new ComponentName(requireContext(), WidgetProviderImages.class));
//
//        WidgetProviderImages.update(getContext(), widgetManager, appWidgetIds);
    }
}
