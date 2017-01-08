package de.kappa_mm.sitmark.sitmarkaudiobeacondemo;

import android.support.v7.app.AppCompatActivity;
import android.widget.ToggleButton;
import android.widget.TextView;
import android.graphics.Color;
import android.os.Handler;
import android.os.Bundle;
import android.view.View;
import android.util.Log;

import de.kappa_mm.sitmark.sitmarkaudiobeaconbridge.SitMarkAudioBeaconBridge;
import de.kappa_mm.sitmark.sitmarkaudiobeaconbridge.SitMarkAudioBeaconCallback;
import de.kappa_mm.sitmark.sitmarkaudiobeaconbridge.SitMarkAudioBeaconListener;

public class MainActivity extends AppCompatActivity implements SitMarkAudioBeaconCallback
{
    private static final String LOGTAG = MainActivity.class.getSimpleName();

    private final Handler handler = new Handler();
    private SitMarkAudioBeaconListener listener;

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

        //
        // Get bridge version string.
        //

        String bversion = SitMarkAudioBeaconBridge.getBridgeVersionString();
        setTitle("RS - Demo - " + bversion);

        //
        // Get version string and check for desired version.
        //

        String version = SitMarkAudioBeaconBridge.getVersionString();
        boolean versionok = SitMarkAudioBeaconBridge.isDesiredVersion();

        TextView tv = (TextView) findViewById(R.id.version);
        tv.setBackgroundColor(versionok ? Color.GREEN : Color.RED);
        tv.setText(version);

        //
        // Setup beacon listener and callback.
        //

        listener = new SitMarkAudioBeaconListener();
        listener.checkAndRequestPermission(this);
        listener.setCallbackListener(this);

        //
        // Setup start/stop button.
        //

        ToggleButton tb = (ToggleButton) findViewById(R.id.toggleButton);

        tb.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view)
            {
                boolean on = ((ToggleButton) view).isChecked();

                Log.d(LOGTAG, "toggleButton=" + on);

                if (on)
                {
                    if (! listener.checkAndRequestPermission(MainActivity.this))
                    {
                        ((ToggleButton) view).setChecked(false);
                    }
                    else
                    {
                        listener.onStartListening();
                    }
                }
                else
                {
                    listener.onStopListening();
                }
            }
        });
    }

    //region SitMarkAudioBeaconCallback implementation.

    @Override
    public void onSyncDetected(int channel)
    {
        Log.d(LOGTAG, "onSyncDetected: channel=" + channel);

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

        handler.removeCallbacks(resetSyncs);
        handler.postDelayed(resetSyncs, 1000);
    }

    @Override
    public void onBeaconDetected(int channel, int beacon)
    {

    }

    //endregion SitMarkAudioBeaconCallback implementation.

    private final Runnable resetSyncs = new Runnable()
    {
        @Override
        public void run()
        {
            TextView lSync = (TextView) findViewById(R.id.lSync);
            lSync.setBackgroundColor(Color.TRANSPARENT);
            lSync.setText("");

            TextView rSync = (TextView) findViewById(R.id.rSync);
            rSync.setBackgroundColor(Color.TRANSPARENT);
            rSync.setText("");
        }
    };
}
