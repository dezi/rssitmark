package de.kappa_mm.sitmark.sitmarkaudiobeacondemo;

import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.graphics.Color;
import android.os.Bundle;

import de.kappa_mm.sitmark.sitmarkaudiobeaconbridge.SitMarkAudioBeaconBridge;

public class MainActivity extends AppCompatActivity
{
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
        // Get version string and check for desired version.
        //

        String version = SitMarkAudioBeaconBridge.getVersionString();
        boolean versionok = SitMarkAudioBeaconBridge.isDesiredVersion();

        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setBackgroundColor(versionok ? Color.GREEN : Color.RED);
        tv.setText(version);
    }
}
