package com.rck.fiftygram;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Transformation;
import com.bumptech.glide.request.RequestOptions;

import java.io.FileDescriptor;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import jp.wasabeef.glide.transformations.gpu.InvertFilterTransformation;
import jp.wasabeef.glide.transformations.gpu.SepiaFilterTransformation;
import jp.wasabeef.glide.transformations.gpu.SketchFilterTransformation;
import jp.wasabeef.glide.transformations.gpu.ToonFilterTransformation;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, ActivityCompat.OnRequestPermissionsResultCallback {
    private ImageView imageView;
    private Button btn_save;
    private Bitmap image;
    private String nameExtension;
    private String description;
    private final String[] filters = {"Sepia Filter", "Toon Filter", "Sketch Filter", "Invert Filter", "Remover Filter"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
        getSupportActionBar().setCustomView(R.layout.action_bar_layout);
        requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);

        imageView = findViewById(R.id.image_view);
        Button btn_choose_photo = findViewById(R.id.btn_choose_photo_id);
        btn_save = findViewById(R.id.btn_save);
        btn_choose_photo.setOnClickListener(v -> choosePhoto());
        btn_save.setOnClickListener(v -> beginSave());
        Spinner spinner = (Spinner) findViewById(R.id.filters_spinner);

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.filters_options, android.R.layout.simple_spinner_item);

        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(this);

    }

    // Request permission to user to access external storage of the device
    // If the request is granted the user can save the image later, otherwise the button to save will stay disabled
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == -1) {
            Button btnSave = findViewById(R.id.btn_save);
            btnSave.setEnabled(false);
            Toast.makeText(getApplicationContext(), "PERMISSION DENIED - You still can apply the filter but won't be able to save the image.", Toast.LENGTH_LONG).show();
        }
    }

    private void choosePhoto() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("image/*");
        startActivityForResult(intent, 1);
    }

    // Apply the filter passed by argument to the image opened
    private void apply(Transformation<Bitmap> filter) {
        Glide.with(this)
                .load(image)
                .apply(RequestOptions.bitmapTransform(filter))
                .into(imageView);
    }

    // Call apply with the Sepia filter and give the file name and a description
    private void applySepia() {
        apply(new SepiaFilterTransformation());
        nameExtension = "_sepia";
        description = "Image with Sepia filter";
    }

    // Call apply with the Toon filter and give the file name and a description
    private void applyToon() {
        apply(new ToonFilterTransformation());
        nameExtension = "_toon";
        description = "Image with Toon filter";
    }

    // Call apply with the Sketch filter and give the file name and a description
    private void applySketch() {
        apply(new SketchFilterTransformation());
        nameExtension = "_sketch";
        description = "Image with Sketch filter";
    }

    // Call apply with the Invert filter and give the file name and a description
    private void applyInvert() {
        apply(new InvertFilterTransformation());
        nameExtension = "_invert";
        description = "Image with Invert filter";
    }

    // Show original picture again
    private void removeFilter() {
        imageView.setImageBitmap(image);
        nameExtension = "_original";
        description = "Original Image, no filters applied";
    }

    // Call async function to save image on background and set button disabled until it finishes
    private void beginSave() {
        btn_save.setEnabled(false);
        new ApplySave().execute();
    }

    // Save Image to Media store of the user
    public boolean savePicture () {
        Date now = new Date();
        String fileName = new SimpleDateFormat("ddMMyyyyHHmm", Locale.ENGLISH).format(now);
        String nameFinal = fileName + nameExtension;
        BitmapDrawable changed = (BitmapDrawable) imageView.getDrawable();
        Bitmap filtered = changed.getBitmap();

        try {
            Uri.parse(MediaStore.Images.Media.insertImage(getContentResolver(), filtered, nameFinal, description));
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == Activity.RESULT_OK && data != null){
            try {
                Uri uri = data.getData();
                ParcelFileDescriptor parcelFileDescriptor = getContentResolver().openFileDescriptor(uri, "r");
                FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
                image = BitmapFactory.decodeFileDescriptor(fileDescriptor);
                parcelFileDescriptor.close();
                imageView.setImageBitmap(image);
            } catch (IOException exception) {
                exception.printStackTrace();
            }

        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // An item was selected. You can retrieve the selected item using
        // parent.getItemAtPosition(pos)

        if(parent.getItemAtPosition(position).equals(filters[0])) {
            applySepia();
        }
        if(parent.getItemAtPosition(position).equals(filters[1])) {
            applyToon();
        }
        if(parent.getItemAtPosition(position).equals(filters[2])) {
            applySketch();
        }
        if(parent.getItemAtPosition(position).equals(filters[3])) {
            applyInvert();
        }
        if(parent.getItemAtPosition(position).equals(filters[4])) {
            removeFilter();
        }

    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        // Another interface callback
    }

    // Background function to call savePicture
    private class ApplySave extends AsyncTask {
        private Boolean result;
        @Override
        protected Boolean doInBackground(Object[] objects) {
            if( savePicture()) {
                result = true;
            } else {
                result = false;
            }
            return result;
        }

        @Override
        protected void onPostExecute(Object o) {
            if (result) {
                Toast.makeText(getApplicationContext(),
                        "Image Saved Successfully!",
                        Toast.LENGTH_LONG).show();
                Button btnSave = findViewById(R.id.btn_save);
                btnSave.setEnabled(true);
            } else {
                Toast.makeText(getApplicationContext(),
                        "Error saving the image!",
                        Toast.LENGTH_LONG).show();
            }
        }
    }
}