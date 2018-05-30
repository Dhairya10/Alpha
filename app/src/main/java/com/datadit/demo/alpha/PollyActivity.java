/*
 * Copyright 2010-2018 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.datadit.demo.alpha;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.polly.AmazonPollyPresigningClient;
import com.amazonaws.services.polly.model.DescribeVoicesRequest;
import com.amazonaws.services.polly.model.DescribeVoicesResult;
import com.amazonaws.services.polly.model.OutputFormat;
import com.amazonaws.services.polly.model.SynthesizeSpeechPresignRequest;
import com.amazonaws.services.polly.model.Voice;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.util.List;

public class PollyActivity extends Activity {
    private static final String TAG = "PollyActivity";

    private static final String KEY_SELECTED_VOICE_POSITION = "SelectedVoicePosition";
    private static final String KEY_VOICES = "Voices";
    private static final String KEY_SAMPLE_TEXT = "SampleText";

    private static final String COGNITO_POOL_ID = "us-east-2:cc3e128b-d79b-41b0-93bd-aa1b0cfcad53";

    private static final Regions MY_REGION = Regions.US_EAST_2;

    CognitoCachingCredentialsProvider credentialsProvider;

    private AmazonPollyPresigningClient client;
    private List<Voice> voices;

    private Spinner voicesSpinner;
    private EditText sampleTextEditText;
    private Button playButton;
    private ImageButton defaultTextButton;

    private int selectedPosition;

    MediaPlayer mediaPlayer;

    private class SpinnerVoiceAdapter extends BaseAdapter {
        private LayoutInflater inflater;
        private List<Voice> voices;

        SpinnerVoiceAdapter(Context ctx, List<Voice> voices) {
            this.inflater = LayoutInflater.from(ctx);
            this.voices = voices;
        }

        @Override
        public int getCount() {
            return voices.size();
        }

        @Override
        public Object getItem(int position) {
            return voices.get(position);
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.voice_spinner_row, parent, false);
            }
            Voice voice = voices.get(position);

            TextView nameTextView = (TextView) convertView.findViewById(R.id.voiceName);
            nameTextView.setText(voice.getName());

            TextView languageCodeTextView = (TextView) convertView.findViewById(R.id.voiceLanguageCode);
            languageCodeTextView.setText(voice.getLanguageName() + " (" + voice.getLanguageCode() + ")");

            return convertView;
        }
    }

    private class GetPollyVoices extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            if (voices != null) {
                return null;
            }

            DescribeVoicesRequest describeVoicesRequest = new DescribeVoicesRequest();

            DescribeVoicesResult describeVoicesResult;
            try {
                describeVoicesResult = client.describeVoices(describeVoicesRequest);
            } catch (RuntimeException e) {
                Log.e(TAG, "Unable to get available voices. " + e.getMessage());
                return null;
            }

            voices = describeVoicesResult.getVoices();

            Log.i(TAG, "Available Polly voices: " + voices);

            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            if (voices == null) {
                return;
            }

            voicesSpinner.setAdapter(new SpinnerVoiceAdapter(PollyActivity.this, voices));

            findViewById(R.id.voicesProgressBar).setVisibility(View.INVISIBLE);
            voicesSpinner.setVisibility(View.VISIBLE);

            voicesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (view == null) {
                        return;
                    }

                    setDefaultTextForSelectedVoice();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            });
            voicesSpinner.setSelection(selectedPosition);

            playButton.setEnabled(true);
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

        setupVoicesSpinner();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_polly);

        TextView textViewName = findViewById(R.id.textViewName);
        String userName = getIntent().getStringExtra("Name");
        String name = "Hello " + userName;

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
        if (userName == null) {
            String hello = "Hello";
            textViewName.setText(hello);
        } else {
            textViewName.setText(name);
        }
        initPollyClient();

        setupNewMediaPlayer();
        setupSampleTextEditText();
        setupPlayButton();
        setupDefaultTextButton();

        String filePath = Environment.getExternalStorageDirectory() + "/logcatPolly.txt";
        try {
            Runtime.getRuntime().exec(new String[]{"logcat", "-f", filePath, TAG, "*:S"});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(KEY_SELECTED_VOICE_POSITION, voicesSpinner.getSelectedItemPosition());
        outState.putSerializable(KEY_VOICES, (Serializable) voices);
        outState.putString(KEY_SAMPLE_TEXT, sampleTextEditText.getText().toString());

        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        selectedPosition = savedInstanceState.getInt(KEY_SELECTED_VOICE_POSITION);
        voices = (List<Voice>) savedInstanceState.getSerializable(KEY_VOICES);

        String sampleText = savedInstanceState.getString(KEY_SAMPLE_TEXT);
        if (sampleText.isEmpty()) {
            defaultTextButton.setVisibility(View.GONE);
        } else {
            sampleTextEditText.setText(sampleText);
            defaultTextButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    void initPollyClient() {
        credentialsProvider = new CognitoCachingCredentialsProvider(
                getApplicationContext(),
                COGNITO_POOL_ID,
                MY_REGION
        );

        client = new AmazonPollyPresigningClient(credentialsProvider);
    }

    void setupSampleTextEditText() {
        sampleTextEditText = findViewById(R.id.sampleText);

        sampleTextEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                defaultTextButton.setVisibility(sampleTextEditText.getText().toString().isEmpty() ?
                        View.GONE : View.VISIBLE);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        sampleTextEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                playButton.performClick();
                return false;
            }
        });
    }

    void setupNewMediaPlayer() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.release();
                setupNewMediaPlayer();
            }
        });
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                mp.start();
                playButton.setEnabled(true);
            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                playButton.setEnabled(true);
                return false;
            }
        });
    }

    void setupVoicesSpinner() {
        voicesSpinner = (Spinner) findViewById(R.id.voicesSpinner);
        findViewById(R.id.voicesProgressBar).setVisibility(View.VISIBLE);
        new GetPollyVoices().execute();
    }

    void setupPlayButton() {
        playButton = (Button) findViewById(R.id.readButton);
        playButton.setEnabled(false);
        playButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playButton.setEnabled(false);

                Voice selectedVoice = (Voice) voicesSpinner.getSelectedItem();

                String textToRead = sampleTextEditText.getText().toString();

                if (textToRead.trim().isEmpty()) {
                    textToRead = getSampleText(selectedVoice);
                }

                SynthesizeSpeechPresignRequest synthesizeSpeechPresignRequest =
                        new SynthesizeSpeechPresignRequest()
                                .withText(textToRead)
                                .withVoiceId(selectedVoice.getId())
                                .withOutputFormat(OutputFormat.Mp3);
                URL presignedSynthesizeSpeechUrl =
                        client.getPresignedSynthesizeSpeechUrl(synthesizeSpeechPresignRequest);

                Log.i(TAG, "Playing speech from presigned URL: " + presignedSynthesizeSpeechUrl);

                if (mediaPlayer.isPlaying()) {
                    setupNewMediaPlayer();
                }
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);

                try {
                    mediaPlayer.setDataSource(presignedSynthesizeSpeechUrl.toString());
                } catch (IOException e) {
                    Log.e(TAG, "Unable to set data source for the media player! " + e.getMessage());
                }
                mediaPlayer.prepareAsync();
            }
        });
    }

    String getSampleText(Voice voice) {
        if (voice == null) {
            return "";
        }

        String resourceName = "sample_" +
                voice.getLanguageCode().replace("-", "_").toLowerCase() + "_" +
                voice.getId().toLowerCase();
        int sampleTextResourceId =
                getResources().getIdentifier(resourceName, "string", getPackageName());
        if (sampleTextResourceId == 0)
            return "";

        return getString(sampleTextResourceId);
    }

    void setDefaultTextForSelectedVoice() {
        Voice selectedVoice = (Voice) voicesSpinner.getSelectedItem();
        if (selectedVoice == null) {
            return;
        }

        String sampleText = getSampleText(selectedVoice);

        sampleTextEditText.setHint(sampleText);
    }

    void setupDefaultTextButton() {
        defaultTextButton = findViewById(R.id.defaultTextButton);
        defaultTextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sampleTextEditText.setText(null);
            }
        });
    }

    private void logout() {
        AuthUI.getInstance()
                .signOut(PollyActivity.this)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(PollyActivity.this, "Successfully Logged Out", Toast.LENGTH_SHORT).show();
                        startActivity(new Intent(PollyActivity.this, LoginActivity.class));
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

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, MenuActivity.class));
        finish();
    }
}
