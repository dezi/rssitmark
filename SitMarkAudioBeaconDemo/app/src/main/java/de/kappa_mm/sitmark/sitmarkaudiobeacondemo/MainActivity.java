package de.kappa_mm.sitmark.sitmarkaudiobeacondemo;

import android.support.v7.app.AppCompatActivity;
import android.widget.ToggleButton;
import android.widget.TextView;
import android.graphics.Color;
import android.view.View;
import android.os.Bundle;
import android.util.Log;

import de.kappa_mm.sitmark.sitmarkaudiobeaconbridge.SitMarkAudioBeaconBridge;
import de.kappa_mm.sitmark.sitmarkaudiobeaconbridge.SitMarkAudioBeaconListener;

public class MainActivity extends AppCompatActivity
{
    private static final String LOGTAG = MainActivity.class.getSimpleName();
    private static final String VERSION = "07.01.2017:0";

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

        setTitle("RS - Demo - " + VERSION);

        //
        // Get version string and check for desired version.
        //

        String version = SitMarkAudioBeaconBridge.getVersionString();
        boolean versionok = SitMarkAudioBeaconBridge.isDesiredVersion();

        TextView tv = (TextView) findViewById(R.id.version);
        tv.setBackgroundColor(versionok ? Color.GREEN : Color.RED);
        tv.setText(version);

        //
        // Setup beacon listener.
        //

        listener = new SitMarkAudioBeaconListener();

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
                    listener.onStartListening();
                }
                else
                {
                    listener.onStopListening();
                }
            }
        });
    }
}
