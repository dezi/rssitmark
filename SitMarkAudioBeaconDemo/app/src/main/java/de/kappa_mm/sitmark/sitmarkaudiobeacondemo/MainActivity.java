package de.kappa_mm.sitmark.sitmarkaudiobeacondemo;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import de.kappa_mm.sitmark.sitmarkaudiobeaconbridge.SitMarkAudioBeaconBridge;

public class MainActivity extends AppCompatActivity
{
    static
    {
        //
        // Load SitMarkAudioBeaconBridge library at application startup.
        //

        SitMarkAudioBeaconBridge.initializeBridge();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Example of a call to a native method
        TextView tv = (TextView) findViewById(R.id.sample_text);
        tv.setText(stringFromJNI());
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
}
