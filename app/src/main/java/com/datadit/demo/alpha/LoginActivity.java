package com.datadit.demo.alpha;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.IdpResponse;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 123;
    private static final String TAG = "LoginActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        TextView emptyStateTextView = findViewById(R.id.empty_view);


        ConnectivityManager connectivityManager = (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

            if (networkInfo != null && networkInfo.isConnectedOrConnecting()) {
                Log.i(TAG, "User is connected to Internet");

                List<AuthUI.IdpConfig> providers = Arrays.asList(
                        new AuthUI.IdpConfig.GoogleBuilder().build(),
                        new AuthUI.IdpConfig.FacebookBuilder().build());

                startActivityForResult(
                        AuthUI.getInstance()
                                .createSignInIntentBuilder()
                                .setAvailableProviders(providers)
                                .setTheme(R.style.Theme_AppCompat)
                                .setLogo(R.drawable.alpha)
                                .build(),
                        RC_SIGN_IN);
            } else {
                Log.i(TAG, "Internet Connectivity Issue");
                emptyStateTextView.setText(R.string.no_internet);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {


        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);

            if (resultCode == RESULT_OK) {
                Log.d("LoginActivity", "Result code is OK");
                FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
                String name = null;
                if (user != null) {
                    name = user.getDisplayName();
                }
                Intent intent = new Intent(LoginActivity.this, MenuActivity.class);
                intent.putExtra("Name", name);
                startActivity(intent);
                finish();
            } else {
                if (response != null) {
                    Toast.makeText(this, "Sign In Failed" + response.getError(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Response is Null", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}
