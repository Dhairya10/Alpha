package com.datadit.demo.alpha;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;


import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.rekognition.AmazonRekognitionClient;
import com.amazonaws.services.rekognition.model.AgeRange;
import com.amazonaws.services.rekognition.model.Attribute;
import com.amazonaws.services.rekognition.model.DetectFacesRequest;
import com.amazonaws.services.rekognition.model.DetectFacesResult;
import com.amazonaws.services.rekognition.model.FaceDetail;
import com.amazonaws.services.rekognition.model.Image;
import com.amazonaws.util.IOUtils;
import com.nbsp.materialfilepicker.MaterialFilePicker;
import com.nbsp.materialfilepicker.ui.FilePickerActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.List;

public class RekognitionActivity extends AppCompatActivity {

    AmazonRekognitionClient amazonRekognitionClient;
    DetectFacesRequest detectFacesRequest;
    DetectFacesResult detectFacesResult;
    TextView textViewDetails;
    Image image;
    Button buttonChoose;
    File file;
    String low;
    String high;
    String details;
    ProgressBar progressBar;
    private static final String TAG = "RekognitionActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rekognition);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(RekognitionActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1001);
        }

        ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

            if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
                Log.i(TAG, "User is connected to Internet");
            } else {
                Toast.makeText(this, "Please check your internet connection", Toast.LENGTH_LONG).show();
                Log.i(TAG, "Internet Connectivity Issue");
            }
        }


        image = new Image();
        progressBar = findViewById(R.id.loading_spinner);
        progressBar.setVisibility(View.INVISIBLE);
        textViewDetails = findViewById(R.id.textViewDetails);
        buttonChoose = findViewById(R.id.buttonChoose);
        buttonChoose.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new MaterialFilePicker()
                        .withActivity(RekognitionActivity.this)
                        .withRequestCode(1)
                        .withFilterDirectories(true)
                        .withHiddenFiles(true)
                        .start();
            }
        });

        String filePath = Environment.getExternalStorageDirectory() + "/logcatRekognition.txt";
        try {
            Runtime.getRuntime().exec(new String[]{"logcat", "-f", filePath, TAG, "*:S"});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK) {
            String filePath = data.getStringExtra(FilePickerActivity.RESULT_FILE_PATH);
            file = new File(String.valueOf(filePath));

            if (file.exists() && file != null) {
                Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
                ImageView imageView = findViewById(R.id.imageView);
                imageView.setImageBitmap(bitmap);

            } else {
                Log.e(TAG, "File is null");
            }
        }

        AWSCallTask awsCallTask = new AWSCallTask();
        awsCallTask.execute();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case 1001: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permission not granted", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    public void testRekognition() {

        ByteBuffer imageBytes;
        try {
            InputStream inputStream = new FileInputStream(file.getAbsolutePath());
            imageBytes = ByteBuffer.wrap(IOUtils.toByteArray(inputStream));
            Log.e("InputStream: ", "" + inputStream);
            Log.e("imageBytes: ", "");
            image.withBytes(imageBytes);

            CognitoCachingCredentialsProvider credentialsProvider = new CognitoCachingCredentialsProvider(
                    getApplicationContext(),
                    "us-east-1:2f6277d3-2e5a-4463-b0e3-3279cd5f8096",
                    Regions.US_EAST_1

            );
            amazonRekognitionClient = new AmazonRekognitionClient(credentialsProvider);

            detectFacesRequest = new DetectFacesRequest()
                    .withAttributes(Attribute.ALL.toString())
                    .withImage(image);


            detectFacesResult = amazonRekognitionClient.detectFaces(detectFacesRequest);
            List<FaceDetail> faceDetails = detectFacesResult.getFaceDetails();

            Log.i(TAG, String.valueOf(detectFacesResult.getFaceDetails()));


            for (FaceDetail face : faceDetails) {
                AgeRange ageRange = face.getAgeRange();
                Log.i(TAG, "The detected face is estimated to be between" + ageRange.getLow().toString() + "and"
                        + ageRange.getHigh().toString() + "years old");
                low = ageRange.getLow().toString();
                high = ageRange.getHigh().toString();
            }

            details = "The Age of the user is between : " + low + " and " + high;

        } catch (Exception e) {
            Log.e("Error in Rekognition:", e.getMessage());
        }
    }

    public class AWSCallTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(View.VISIBLE);
        }

        @Override
        protected Void doInBackground(Void... voids) {
            testRekognition();
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            progressBar.setVisibility(View.GONE);
            if (low == null || high == null) {
                textViewDetails.setText(R.string.invalid_image);
            } else {
                textViewDetails.setText(details);
            }
        }
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, MenuActivity.class));
        finish();
    }
}