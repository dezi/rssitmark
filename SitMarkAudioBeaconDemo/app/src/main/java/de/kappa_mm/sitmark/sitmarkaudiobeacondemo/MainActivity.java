package de.kappa_mm.sitmark.sitmarkaudiobeacondemo;

import android.Manifest;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.RadioButton;
import android.widget.ToggleButton;
import android.widget.TextView;
import android.graphics.Color;
import android.os.Handler;
import android.os.Bundle;
import android.view.View;
import android.util.Log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.kappa_mm.sitmark.sitmarkaudiobeaconbridge.SitMarkAudioBeaconBridge;
import de.kappa_mm.sitmark.sitmarkaudiobeaconbridge.SitMarkAudioBeaconListener;
import de.kappa_mm.sitmark.sitmarkaudiobeaconbridge.SitMarkAudioBeaconCallback;
import de.kappa_mm.sitmark.sitmarkaudiobeaconbridge.SitMarkAudioBeaconHelpers;
import de.kappa_mm.sitmark.sitmarkaudiobeaconbridge.SitMarkAudioBeaconPusher;
import de.kappa_mm.sitmark.sitmarkaudiobeaconbridge.SitMarkAudioBeaconReceiver;

//
// Hallo bollo! Version vom 02.02.2017 15:40...
//

public class MainActivity extends AppCompatActivity implements SitMarkAudioBeaconCallback
{
    private static final String LOGTAG = MainActivity.class.getSimpleName();

    private final Handler handler = new Handler();
    private SitMarkAudioBeaconListener listener;

    private long stopTime;

    private int syncFoundLeft;
    private int syncFoundRight;
    private int codeFoundLeft;
    private int codeFoundRight;

    private RadioButton um1, um3, um5;
    private RadioButton om1, om3, om5;

    static
    {
        //
        // Initialize SitMarkAudioBeaconBridge
        // library at application startup.
        //

        SitMarkAudioBeaconBridge.initializeBridge();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);

        int screenWidth = SitMarkAudioBeaconHelpers.getScreenWidth(this);

        //
        // Get bridge version string and architecture.
        //

        String bversion = SitMarkAudioBeaconBridge.getBridgeVersionString();
        String archcpu = System.getProperty("os.arch");
        String archlib = SitMarkAudioBeaconHelpers.findLibrariesArchitecture();

        setTitle("RS " + bversion + " " + archcpu + " " + archlib);

        //
        // Get version string and check for desired version.
        //

        String version = SitMarkAudioBeaconBridge.getVersionString();
        boolean versionok = SitMarkAudioBeaconBridge.isDesiredVersion();

        TextView vtv = (TextView) findViewById(R.id.version);
        vtv.setBackgroundColor(versionok ? Color.GREEN : Color.RED);
        vtv.setText(version);

        if (screenWidth < 500) vtv.setTextSize(vtv.getTextSize() / 4f);

        //
        // Get model name.
        //

        String manufacturer = Build.MANUFACTURER.toUpperCase().replace(" ","_");
        String model = Build.MODEL.toUpperCase().replace(" ","_");

        TextView mtv = (TextView) findViewById(R.id.model);
        mtv.setText(manufacturer + "-" + model);

        if (screenWidth < 500) mtv.setTextSize(mtv.getTextSize() / 4f);

        um1 = (RadioButton) findViewById(R.id.um1);
        um3 = (RadioButton) findViewById(R.id.um3);
        um5 = (RadioButton) findViewById(R.id.um5);
        om1 = (RadioButton) findViewById(R.id.om1);
        om3 = (RadioButton) findViewById(R.id.om3);
        om5 = (RadioButton) findViewById(R.id.om5);

        um1.setOnClickListener(onRadioClick);
        um3.setOnClickListener(onRadioClick);
        um5.setOnClickListener(onRadioClick);
        om1.setOnClickListener(onRadioClick);
        om3.setOnClickListener(onRadioClick);
        om5.setOnClickListener(onRadioClick);

        //
        // Setup beacon listener and callback.
        //

        listener = new SitMarkAudioBeaconListener();
        listener.checkAndRequestPermission(this);
        listener.setCallbackListener(this);

        //
        // Setup start/stop listener button.
        //

        ToggleButton ltb = (ToggleButton) findViewById(R.id.listenToggle);

        ltb.setText("MIC AUS");
        ltb.setTextOff("MIC AUS");
        ltb.setTextOn("MIC EIN");

        ltb.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                boolean on = ((ToggleButton) view).isChecked();

                Log.d(LOGTAG, "listenToggle=" + on);

                if (on)
                {
                    if (! listener.checkAndRequestPermission(MainActivity.this))
                    {
                        ((ToggleButton) view).setChecked(false);
                    }
                    else
                    {
                        syncFoundLeft = 0;
                        syncFoundRight = 0;
                        codeFoundLeft = 0;
                        codeFoundRight = 0;

                        stopTime = 0;

                        displayCounts();

                        listener.onStartListening();
                    }
                }
                else
                {
                    listener.onStopListening();
                }
            }
        });

        //
        // Setup timer listener button.
        //

        ToggleButton ttb = (ToggleButton) findViewById(R.id.timerButton);

        ttb.setText("TIMER AUS");
        ttb.setTextOff("TIMER AUS");
        ttb.setTextOn("TIMER EIN");

        ttb.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                boolean on = ((ToggleButton) view).isChecked();

                Log.d(LOGTAG, "timerToggle=" + on);

                if (on)
                {
                    if (! listener.checkAndRequestPermission(MainActivity.this))
                    {
                        ((ToggleButton) view).setChecked(false);
                    }
                    else
                    {
                        syncFoundLeft = 0;
                        syncFoundRight = 0;
                        codeFoundLeft = 0;
                        codeFoundRight = 0;

                        stopTime = (System.currentTimeMillis() / 1000) + 60;

                        displayCounts();

                        listener.setLogFile(getLogFile());
                        listener.onStartListening();

                        handler.post(timerCheck);
                    }
                }
                else
                {
                    listener.onStopListening();
                }
            }
        });

        //
        // Adjust all text sizes for smaller screens.
        //

        if (screenWidth < 500)
        {
            TextView tv;

            tv = (TextView) findViewById(R.id.lChannel);
            tv.setTextSize(tv.getTextSize() / 5f);

            tv = (TextView) findViewById(R.id.lSync);
            tv.setTextSize(tv.getTextSize() / 5f);

            tv = (TextView) findViewById(R.id.lBeacon);
            tv.setTextSize(tv.getTextSize() / 5f);

            tv = (TextView) findViewById(R.id.rChannel);
            tv.setTextSize(tv.getTextSize() / 5f);

            tv = (TextView) findViewById(R.id.rSync);
            tv.setTextSize(tv.getTextSize() / 5f);

            tv = (TextView) findViewById(R.id.rBeacon);
            tv.setTextSize(tv.getTextSize() / 5f);
        }

        checkAndRequestExternalWrite(this);

        File file = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS),
                "Watermark/RadioScreenStream.mp3");

        SitMarkAudioBeaconPusher watermark = new SitMarkAudioBeaconPusher();

        SitMarkAudioBeaconReceiver receiver = new SitMarkAudioBeaconReceiver();

        receiver.onStartListening();
        watermark.dodat(file.toString(), receiver);
        receiver.onStopListening();
    }

    //region SitMarkAudioBeaconCallback implementation.

    @Override
    public void onSyncDetected(Object sender, int channel)
    {
        if (sender == listener)
        {
            Log.d(LOGTAG, "onSyncDetected: listener channel=" + channel);

            if (channel == 0)
            {
                TextView lSync = (TextView) findViewById(R.id.lSync);
                lSync.setBackgroundColor(Color.GREEN);
                lSync.setText("SYNC");
                syncFoundLeft++;
            }

            if (channel == 1)
            {
                TextView rSync = (TextView) findViewById(R.id.rSync);
                rSync.setBackgroundColor(Color.GREEN);
                rSync.setText("SYNC");
                syncFoundRight++;
            }

            displayCounts();

            handler.removeCallbacks(resetLocalSyncs);
            handler.postDelayed(resetLocalSyncs, 1000);
        }
    }

    @Override
    public void onBeaconDetected(Object sender, int channel, double confidence, String beacon)
    {
        if (sender == listener)
        {
            Log.d(LOGTAG, "onBeaconDetected: listener channel=" + channel);

            if (channel == 0)
            {
                TextView lBeacon = (TextView) findViewById(R.id.lBeacon);
                lBeacon.setBackgroundColor(confidence >= 0.0 ? Color.GREEN : Color.RED);
                lBeacon.setText(beacon);

                if (confidence >= 0.0) codeFoundLeft++;
            }

            if (channel == 1)
            {
                TextView rBeacon = (TextView) findViewById(R.id.rBeacon);
                rBeacon.setBackgroundColor(confidence >= 0.0 ? Color.GREEN : Color.RED);
                rBeacon.setText(beacon);

                if (confidence >= 0.0) codeFoundRight++;
            }

            displayCounts();

            handler.removeCallbacks(resetLocalSyncs);
            handler.postDelayed(resetLocalSyncs, 1000);
        }
    }

    //endregion SitMarkAudioBeaconCallback implementation.

    private void displayCounts()
    {
        TextView tv;

        tv = (TextView) findViewById(R.id.lChannel);
        tv.setText("L:" + syncFoundLeft + ":" + codeFoundLeft);

        tv = (TextView) findViewById(R.id.rChannel);
        tv.setText("R:" + syncFoundRight + ":" + codeFoundRight);
    }

    private final Runnable timerCheck = new Runnable()
    {
        @Override
        public void run()
        {
            if (stopTime > 0)
            {
                long currentTime = System.currentTimeMillis() / 1000;
                int rest = (int) (stopTime - currentTime);

                ToggleButton ttb = (ToggleButton) findViewById(R.id.timerButton);

                if (currentTime >= stopTime)
                {
                    ttb.setChecked(false);

                    listener.onStopListening();
                }
                else
                {
                    ttb.setText("TIMER " + rest);

                    handler.postDelayed(timerCheck, 500);
                }
            }
        }
    };

    private final Runnable resetLocalSyncs = new Runnable()
    {
        @Override
        public void run()
        {
            TextView lSync = (TextView) findViewById(R.id.lSync);
            lSync.setBackgroundColor(Color.TRANSPARENT);
            lSync.setText("");

            TextView lBeacon = (TextView) findViewById(R.id.lBeacon);
            lBeacon.setBackgroundColor(Color.TRANSPARENT);

            TextView rSync = (TextView) findViewById(R.id.rSync);
            rSync.setBackgroundColor(Color.TRANSPARENT);
            rSync.setText("");

            TextView rBeacon = (TextView) findViewById(R.id.rBeacon);
            rBeacon.setBackgroundColor(Color.TRANSPARENT);
        }
    };

    private final View.OnClickListener onRadioClick = new View.OnClickListener()
    {
        @Override
        public void onClick(View view)
        {
            RadioButton rb = (RadioButton) view;

            um1.setChecked(rb == um1);
            um3.setChecked(rb == um3);
            um5.setChecked(rb == um5);
            om1.setChecked(rb == om1);
            om3.setChecked(rb == om3);
            om5.setChecked(rb == om5);
        }
    };

    private String getLogFile()
    {
        String which = "";

        if (um1.isChecked()) which = "UM-1.5";
        if (um3.isChecked()) which = "UM-3";
        if (um5.isChecked()) which = "UM-5";

        if (om1.isChecked()) which = "OM-1.5";
        if (om3.isChecked()) which = "OM-3";
        if (om5.isChecked()) which = "OM-5";

        String manufacturer = Build.MANUFACTURER.toUpperCase().replace(" ","_");
        String model = Build.MODEL.toUpperCase().replace(" ","_");

        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.getDefault());
        String date = sdf.format(new Date());

        return "RS-" + manufacturer + "-" + model + "-" + which + "-" + date;
    }

    public void checkAndRequestExternalWrite(Activity activity)
    {
        int permission = ContextCompat.checkSelfPermission(activity,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    4711);
        }
    }
}
