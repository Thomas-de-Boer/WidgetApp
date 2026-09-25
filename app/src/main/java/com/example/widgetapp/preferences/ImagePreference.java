package com.example.widgetapp.preferences;

import static android.content.Context.MODE_PRIVATE;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

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
import java.util.Set;
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
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inJustDecodeBounds = true;

                            InputStream condenseInputStream = contentResolver.openInputStream(uri);

                            BitmapFactory.decodeStream(condenseInputStream, null, options);

                            int width = options.outWidth;
                            int height = options.outHeight;
                            int tarWidth = 1440;
                            int tarHeight = 3200;

                            int sampleSize = calculateSampleSize(height, width, tarHeight, tarWidth);

                            assert condenseInputStream != null;
                            condenseInputStream.close();

                            options.inJustDecodeBounds = false;
                            options.inSampleSize = sampleSize;

                            Bitmap bitmap = BitmapFactory.decodeStream(inputStream, null, options);

                            InputStream exifIS = contentResolver.openInputStream(uri);

                            assert exifIS != null;
                            ExifInterface exifInterface = new ExifInterface(exifIS);

                            int orientationtag = exifInterface.getAttributeInt(
                                    ExifInterface.TAG_ORIENTATION,
                                    ExifInterface.ORIENTATION_NORMAL
                            );

                            int orientation = 0;
                            switch (orientationtag) {
                                case (ExifInterface.ORIENTATION_NORMAL):
                                    break;
                                case (ExifInterface.ORIENTATION_ROTATE_90):
                                    orientation = 90;
                                    break;
                                case (ExifInterface.ORIENTATION_ROTATE_180):
                                    orientation = 180;
                                    break;
                                case (ExifInterface.ORIENTATION_ROTATE_270):
                                    orientation = 270;
                                    break;
                            }

                            Matrix matrix = new Matrix();
                            matrix.setRotate(orientation);

                            assert bitmap != null;
                            Bitmap newBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                            exifIS.close();


//                            buffer = new ByteArrayOutputStream();
//
//                            int nRead;
//                            byte[] data = new byte[4096];
//
//                            while (true) {
//                                assert inputStream != null;
//                                if ((nRead = inputStream.read(data, 0, data.length)) == -1)
//                                    break;
//                                buffer.write(data, 0, nRead);
//                            }
//
//                            buffer.flush();
//                            byte[] bytes = buffer.toByteArray();

                            newBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fileOutputStream);

//                            fileOutputStream.write(bytes);

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

                    uploadedFileNames.addAll(SettingsManager.deserializeString(string));

                    Log.d("ImagePreference", "writing = " + gson.toJson(uploadedFileNames));


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

    public int calculateSampleSize(int currHeight, int currWidth, int tarHeight, int tarWidth) {
        int sampleSize = 1;

        while (currHeight / sampleSize >= tarHeight || currWidth / sampleSize >= tarWidth) {
            sampleSize *= 2;
        }

        return sampleSize;
    }
}
