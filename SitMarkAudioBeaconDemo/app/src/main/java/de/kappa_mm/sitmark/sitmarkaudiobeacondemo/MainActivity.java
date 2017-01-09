package de.kappa_mm.sitmark.sitmarkaudiobeacondemo;

import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ToggleButton;
import android.widget.TextView;
import android.graphics.Color;
import android.os.Handler;
import android.os.Bundle;
import android.view.View;
import android.util.Log;

import de.kappa_mm.sitmark.sitmarkaudiobeaconbridge.SitMarkAudioBeaconBridge;
import de.kappa_mm.sitmark.sitmarkaudiobeaconbridge.SitMarkAudioBeaconCallback;
import de.kappa_mm.sitmark.sitmarkaudiobeaconbridge.SitMarkAudioBeaconHelpers;
import de.kappa_mm.sitmark.sitmarkaudiobeaconbridge.SitMarkAudioBeaconListener;
import de.kappa_mm.sitmark.sitmarkaudiobeaconbridge.SitMarkAudioBeaconRemote;

public class MainActivity extends AppCompatActivity implements SitMarkAudioBeaconCallback
{
    private static final String LOGTAG = MainActivity.class.getSimpleName();

    private final Handler handler = new Handler();
    private SitMarkAudioBeaconListener listener;
    private SitMarkAudioBeaconRemote remote;

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

        int screenWidth = SitMarkAudioBeaconHelpers.getScreenWidth(this);

        //
        // Display current IP address.
        //

        String myIP = SitMarkAudioBeaconHelpers.getWifiIPAddress(this);
        TextView ip = (TextView) findViewById(R.id.ipAddress);
        ip.setText(myIP);

        if (screenWidth < 500) ip.setTextSize(ip.getTextSize() / 2f);

        //
        // Dezi's shortcut setup...
        //

        String targetIP = null;

        if ((myIP != null) && myIP.equals("192.168.2.107")) targetIP = "192.168.2.101";
        if ((myIP != null) && myIP.equals("192.168.2.101")) targetIP = "192.168.2.107";

        EditText et = (EditText) findViewById(R.id.targetIP);
        et.setText(targetIP);

        if (screenWidth < 500) et.setTextSize(et.getTextSize() / 3f);

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
        // Setup beacon listener and callback.
        //

        listener = new SitMarkAudioBeaconListener();
        listener.checkAndRequestPermission(this);
        listener.setCallbackListener(this);

        //
        // Setup start/stop listener button.
        //

        ToggleButton ltb = (ToggleButton) findViewById(R.id.listenToggle);

        ltb.setText("LOCAL MIC AUS");
        ltb.setTextOff("LOCAL MIC AUS");
        ltb.setTextOn("LOCAL MIC EIN");

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
                        EditText et = (EditText) findViewById(R.id.targetIP);

                        String editTargetIP = et.getText().toString();
                        listener.setRemoteListener(editTargetIP);
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
        // Setup remote listener and callback.
        //

        remote = new SitMarkAudioBeaconRemote();
        remote.checkAndRequestPermission(this);
        remote.setCallbackListener(this);

        //
        // Setup start/stop remote listener button.
        //

        ToggleButton rtb = (ToggleButton) findViewById(R.id.remoteToggle);

        rtb.setText("REMOTE UDP AUS");
        rtb.setTextOff("REMOTE UDP AUS");
        rtb.setTextOn("REMOTE UDP EIN");

        rtb.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                boolean on = ((ToggleButton) view).isChecked();

                Log.d(LOGTAG, "remoteToggle=" + on);

                if (on)
                {
                    if (! remote.checkAndRequestPermission(MainActivity.this))
                    {
                        ((ToggleButton) view).setChecked(false);
                    }
                    else
                    {
                        remote.onStartListening();
                    }
                }
                else
                {
                    remote.onStopListening();
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
            tv.setTextSize(tv.getTextSize() / 4f);

            tv = (TextView) findViewById(R.id.lSync);
            tv.setTextSize(tv.getTextSize() / 4f);

            tv = (TextView) findViewById(R.id.lBeacon);
            tv.setTextSize(tv.getTextSize() / 4f);

            tv = (TextView) findViewById(R.id.rChannel);
            tv.setTextSize(tv.getTextSize() / 4f);

            tv = (TextView) findViewById(R.id.rSync);
            tv.setTextSize(tv.getTextSize() / 4f);

            tv = (TextView) findViewById(R.id.rBeacon);
            tv.setTextSize(tv.getTextSize() / 4f);

            tv = (TextView) findViewById(R.id.remlChannel);
            tv.setTextSize(tv.getTextSize() / 4f);

            tv = (TextView) findViewById(R.id.remlSync);
            tv.setTextSize(tv.getTextSize() / 4f);

            tv = (TextView) findViewById(R.id.remlBeacon);
            tv.setTextSize(tv.getTextSize() / 4f);

            tv = (TextView) findViewById(R.id.remrChannel);
            tv.setTextSize(tv.getTextSize() / 4f);

            tv = (TextView) findViewById(R.id.remrSync);
            tv.setTextSize(tv.getTextSize() / 4f);

            tv = (TextView) findViewById(R.id.remrBeacon);
            tv.setTextSize(tv.getTextSize() / 4f);
        }
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
            }

            if (channel == 1)
            {
                TextView rSync = (TextView) findViewById(R.id.rSync);
                rSync.setBackgroundColor(Color.GREEN);
                rSync.setText("SYNC");
            }

            handler.removeCallbacks(resetLocalSyncs);
            handler.postDelayed(resetLocalSyncs, 1000);
        }

        if (sender == remote)
        {
            Log.d(LOGTAG, "onSyncDetected: remote channel=" + channel);

            if (channel == 0)
            {
                TextView lSync = (TextView) findViewById(R.id.remlSync);
                lSync.setBackgroundColor(Color.GREEN);
                lSync.setText("SYNC");
            }

            if (channel == 1)
            {
                TextView rSync = (TextView) findViewById(R.id.remrSync);
                rSync.setBackgroundColor(Color.GREEN);
                rSync.setText("SYNC");
            }

            handler.removeCallbacks(resetRemoteSyncs);
            handler.postDelayed(resetRemoteSyncs, 1000);
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
            }

            if (channel == 1)
            {
                TextView rBeacon = (TextView) findViewById(R.id.rBeacon);
                rBeacon.setBackgroundColor(confidence >= 0.0 ? Color.GREEN : Color.RED);
                rBeacon.setText(beacon);
            }

            handler.removeCallbacks(resetLocalSyncs);
            handler.postDelayed(resetLocalSyncs, 1000);
        }

        if (sender == remote)
        {
            Log.d(LOGTAG, "onBeaconDetected: remote channel=" + channel);

            if (channel == 0)
            {
                TextView lBeacon = (TextView) findViewById(R.id.remlBeacon);
                lBeacon.setBackgroundColor(confidence >= 0.0 ? Color.GREEN : Color.RED);
                lBeacon.setText(beacon);
            }

            if (channel == 1)
            {
                TextView rBeacon = (TextView) findViewById(R.id.remrBeacon);
                rBeacon.setBackgroundColor(confidence >= 0.0 ? Color.GREEN : Color.RED);
                rBeacon.setText(beacon);
            }

            handler.removeCallbacks(resetRemoteSyncs);
            handler.postDelayed(resetRemoteSyncs, 1000);
        }
    }

    //endregion SitMarkAudioBeaconCallback implementation.

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

    private final Runnable resetRemoteSyncs = new Runnable()
    {
        @Override
        public void run()
        {
            TextView lSync = (TextView) findViewById(R.id.remlSync);
            lSync.setBackgroundColor(Color.TRANSPARENT);
            lSync.setText("");

            TextView lBeacon = (TextView) findViewById(R.id.remlBeacon);
            lBeacon.setBackgroundColor(Color.TRANSPARENT);

            TextView rSync = (TextView) findViewById(R.id.remrSync);
            rSync.setBackgroundColor(Color.TRANSPARENT);
            rSync.setText("");

            TextView rBeacon = (TextView) findViewById(R.id.remrBeacon);
            rBeacon.setBackgroundColor(Color.TRANSPARENT);
        }
    };


}
