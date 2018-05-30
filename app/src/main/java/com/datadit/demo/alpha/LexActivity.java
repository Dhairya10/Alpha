package com.datadit.demo.alpha;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.AWSStartupHandler;
import com.amazonaws.mobile.client.AWSStartupResult;
import com.amazonaws.mobileconnectors.lex.interactionkit.Response;
import com.amazonaws.mobileconnectors.lex.interactionkit.config.InteractionConfig;
import com.amazonaws.mobileconnectors.lex.interactionkit.ui.InteractiveVoiceView;

import java.util.Locale;
import java.util.Map;

public class LexActivity extends AppCompatActivity {
    InteractiveVoiceView interactiveVoiceView;

    private String LOG_TAG = LexActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lex);

        AWSMobileClient.getInstance().initialize(this, new AWSStartupHandler() {
            @Override
            public void onComplete(AWSStartupResult awsStartupResult) {
                Log.d("LexActivity", "AWSMobileClient is instantiated and you are connected to AWS!");
            }
        }).execute();

        if (ContextCompat.checkSelfPermission(LexActivity.this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.d(LOG_TAG, "Permission Not Granted");
        } else {

            init();
        }
    }

    public void init() {
        InteractiveVoiceView voiceView =
                (InteractiveVoiceView) findViewById(R.id.voiceInterface);

        voiceView.setInteractiveVoiceListener(
                new InteractiveVoiceView.InteractiveVoiceListener() {

                    @Override
                    public void dialogReadyForFulfillment(Map slots, String intent) {
                        Log.d(LOG_TAG, String.format(
                                Locale.US,
                                "Dialog ready for fulfillment:\n\tIntent: %s\n\tSlots: %s",
                                intent,
                                slots.toString()));
                    }

                    @Override
                    public void onResponse(Response response) {
                        Log.d(LOG_TAG, "Bot response: " + response.getTextResponse());
                    }

                    @Override
                    public void onError(String responseText, Exception e) {
                        Log.e(LOG_TAG, "Error: " + responseText, e);
                    }
                });

        voiceView.getViewAdapter().setCredentialProvider(AWSMobileClient.getInstance().getCredentialsProvider());

        //replace parameters with your botname, bot-alias
        voiceView.getViewAdapter()
                .setInteractionConfig(
                        new InteractionConfig("ScheduleAppointment", "Demo"));

      //  voiceView.getViewAdapter()
        //        .setAwsRegion(String.valueOf(Regions.US_WEST_2));
    }
}
