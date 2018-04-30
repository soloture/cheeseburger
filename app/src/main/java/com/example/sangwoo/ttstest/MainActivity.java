package com.example.sangwoo.ttstest;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.twitter.sdk.android.core.Twitter;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.io.PdAudio;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.PdListener;
import org.puredata.core.utils.IoUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;

import com.twitter.sdk.android.core.Callback;
import com.twitter.sdk.android.core.Result;
import com.twitter.sdk.android.core.TwitterAuthToken;
import com.twitter.sdk.android.core.TwitterCore;
import com.twitter.sdk.android.core.TwitterException;
import com.twitter.sdk.android.core.TwitterSession;
import com.twitter.sdk.android.core.identity.TwitterLoginButton;


public class MainActivity extends AppCompatActivity {
    private PdUiDispatcher dispatcher;

    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };
    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    TextToSpeech tts;
    EditText ed1;
    Button b1;
    Button p1;
    Button play1;
    String file = Environment.getExternalStorageDirectory().getAbsolutePath() + "/test.wav";
//    File file = new File(getApplication().getFilesDir(), filename);
    SeekBar play1RevLiveliness;
    TextView play1RevLivelinessNumber;
    SeekBar play1NormalSpeed;
    TextView play1NormalSpeedNumber;
    SeekBar play1StartPoint;
    TextView play1StartPointNumber;
    SeekBar play1Phase;
    TextView play1PhaseNumber;
    SeekBar play1WindowSize;
    TextView play1WindowSizeNumber;
    MediaPlayer mp;
    TwitterLoginButton loginButton;

    private void initPD() throws IOException {
        int sampleRate = AudioParameters.suggestSampleRate();
        Log.d("ramplerate", String.valueOf(sampleRate));
//        int sampleRate = 37000;
        PdAudio.initAudio(sampleRate, 0, 2, 8, true);

        dispatcher = new PdUiDispatcher();
        PdBase.setReceiver(dispatcher);
        dispatcher.addListener("toAndroid", myListener);

    }

    private void loadPdPatch() throws IOException{
        File dir = getFilesDir();
        Log.d("LibPd", dir.toString());
        IoUtils.extractZipResource(getResources().openRawResource(R.raw.voicepatch),dir, true);
        File pdPatch = new File(dir, "voicepatch.pd");
        PdBase.openPatch(pdPatch.getAbsolutePath());
    }
    private void loadPd() {
        try{
            initPD();
            loadPdPatch();
        }catch(IOException e){
//            finish();
            Log.e("PdTag",e.toString());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Twitter.initialize(this);
        setContentView(R.layout.activity_main);
        ed1=(EditText)findViewById(R.id.editText);
        b1=(Button)findViewById(R.id.button);
        p1=(Button)findViewById(R.id.play);
        play1 =(Button)findViewById(R.id.playone);

        //Reverb UI settings
        play1RevLiveliness = (SeekBar) findViewById(R.id.play1_rev_liveliness);
        play1RevLivelinessNumber = (TextView) findViewById(R.id.play1_rev_liveliness_number);
        play1NormalSpeed = (SeekBar) findViewById(R.id.play1_normal_speed);
        play1NormalSpeedNumber = (TextView) findViewById(R.id.play1_normal_speed_number);
        play1StartPoint = (SeekBar) findViewById(R.id.play1_startpoint);
        play1StartPointNumber = (TextView) findViewById(R.id.play1_startpoint_number);
        play1Phase = (SeekBar) findViewById(R.id.play1_phase);
        play1PhaseNumber = (TextView) findViewById(R.id.play1_phase_number);
        play1WindowSize = (SeekBar) findViewById(R.id.play1_windowsize);
        play1WindowSizeNumber = (TextView) findViewById(R.id.play1_windowsize_number);



        verifyStoragePermissions(this);


        loginButton = (TwitterLoginButton) findViewById(R.id.login_button);
        loginButton.setCallback(new Callback<TwitterSession>() {
            @Override
            public void success(Result<TwitterSession> result) {
                // Do something with result, which provides a TwitterSession for making API calls
                TwitterSession session = TwitterCore.getInstance().getSessionManager().getActiveSession();
                TwitterAuthToken authToken = session.getAuthToken();
                String token = authToken.token;
                String secret = authToken.secret;
                Log.d("Twitter", "Successful");

                login(session);
            }

            @Override
            public void failure(TwitterException exception) {
                // Do something on failure
            }

        });


        tts=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    tts.setLanguage(Locale.US);
                }
            }
        });
        loadPd();

        play1RevLiveliness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                PdBase.sendFloat("play1_rev_liveness",(float)i);
                play1RevLivelinessNumber.setText("Liveness = " + i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        play1NormalSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {

                PdBase.sendFloat("normalspeed", (float) i);
                play1NormalSpeedNumber.setText("Normalspeed = " + i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        play1StartPoint.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                PdBase.sendFloat("startpoint", (float)i);
                play1StartPointNumber.setText("startpoint = " + i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        play1Phase.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                float phaseActual = ((float) i) / 100;
                PdBase.sendFloat("phase", (float) phaseActual);
                play1PhaseNumber.setText("Phase = " + (float) phaseActual);
                Log.d("phase", String.valueOf(phaseActual));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        play1WindowSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                PdBase.sendFloat("windowsize", (float) i);
                play1WindowSizeNumber.setText("Window size = " + i);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        b1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Generate TTS file
                String toSpeak = ed1.getText().toString();
                Toast.makeText(getApplicationContext(), toSpeak,Toast.LENGTH_SHORT).show();
                HashMap<String, String> myHashRender = new HashMap();
                String utteranceID = "wpta";
                myHashRender.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceID);
                tts.synthesizeToFile(toSpeak, myHashRender, file);

                //Load in PD


//                tts.speak(toSpeak, TextToSpeech.QUEUE_FLUSH, null);

            }
        });
        p1.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                //Normal speed play
                Uri uri  = Uri.parse("file://"+file);
                Log.d("Libpd", String.valueOf(uri));

                mp = MediaPlayer.create(getApplicationContext(), uri);

                mp.start();

            }
        });
        play1.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                //Play in PD
                PdBase.sendBang("load1");
                PdBase.sendBang("play1");

//                PdBase.sendFloat("onOff", 1.0f);
            }

        });
        Switch onOffSwitch = (Switch) findViewById(R.id.switch1);
        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                float val = (b) ? 1.0f : 0.0f;
                PdBase.sendFloat("onOff", val);

            }
        });
    }

    private final PdListener myListener = new PdListener() {
        @Override
        public void receiveMessage(String source, String symbol, Object... args) {
            Log.i("receiveMessage symbol:", symbol);
            for (Object arg: args) {
                Log.i("receiveMessage atom:", arg.toString());
            }
        }

        /* What to do when we receive a list from Pd. In this example
           we're collecting the list from Pd and outputting each atom */
        @Override
        public void receiveList(String source, Object... args) {
            for (Object arg: args) {
                Log.i("receiveList atom:", arg.toString());
            }
        }

        /* When we receive a symbol from Pd */
        @Override public void receiveSymbol(String source, String symbol) {
            Log.i("receiveSymbol", symbol);
        }
        /* When we receive a float from Pd */
        @Override public void receiveFloat(String source, float x) {
            Log.i("receiveFloat", String.valueOf(x));
        }
        /* When we receive a bang from Pd */
        @Override public void receiveBang(String source) {
            Log.i("receiveBang", "bang!");
        }
    };

    protected void onResume(){
        super.onResume();
        PdAudio.startAudio(this);

    }
    public void onPause(){
        super.onPause();
        PdAudio.stopAudio();
        if(tts !=null){
            tts.stop();
            tts.shutdown();
        }

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Pass the activity result to the login button.
        loginButton.onActivityResult(requestCode, resultCode, data);
    }

    public void login(TwitterSession session){
        String username= session.getUserName();
        Log.d("Twitter" , username);
    }


}
