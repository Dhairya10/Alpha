package com.datadit.demo.alpha;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

public class MenuActivity extends AppCompatActivity implements View.OnClickListener {
    String userName;
    TextView textViewPolly, textViewRekognition;
    private static final String TAG = "MenuActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_menu);

        textViewPolly = findViewById(R.id.textViewPolly);
        textViewRekognition = findViewById(R.id.textViewRekognition);


        ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

            if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
                Log.i(TAG, "User is connected to Internet");
            } else {
                Toast.makeText(this, "You are not connected to Internet", Toast.LENGTH_LONG).show();
                Log.i(TAG, "Internet Connectivity Issue");
            }
        }

        textViewPolly.setOnClickListener(this);
        textViewRekognition.setOnClickListener(this);

        userName = getIntent().getStringExtra("Name");
        if (userName != null) {
            Toast.makeText(this, "Welcome " + userName, Toast.LENGTH_SHORT).show();
            Log.i(TAG, "User Logged in Successfully");
        }
    }

    @Override
    public void onClick(View view) {

        if (view == textViewPolly) {
            Intent intent = new Intent(MenuActivity.this, PollyActivity.class);
            if (userName != null) {
                intent.putExtra("Name", userName);
            }
            startActivity(intent);
            Log.i(TAG, "User navigated to Polly Activity");
            finish();

        } else if (view == textViewRekognition) {
            startActivity(new Intent(MenuActivity.this, RekognitionActivity.class));
            Log.i(TAG, "User Navigated to rekognition Activity");
            finish();

        }

    }

    private void logout() {
        AuthUI.getInstance()
                .signOut(MenuActivity.this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(MenuActivity.this, "Successfully Logged Out", Toast.LENGTH_SHORT).show();
                        Log.i(TAG, "User successfully logged out");
                        startActivity(new Intent(MenuActivity.this, LoginActivity.class));
                    }
                });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_item, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout:
                logout();

        }
        return super.onOptionsItemSelected(item);
    }
}

