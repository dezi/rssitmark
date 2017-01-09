package de.kappa_mm.sitmark.sitmarkaudiobeacondemo;

import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;
import android.widget.ToggleButton;
import android.widget.TextView;
import android.graphics.Color;
import android.os.Handler;
import android.os.Bundle;
import android.view.View;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

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

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //
        // Display current IP address.
        //

        TextView ip = (TextView) findViewById(R.id.ipAddress);
        ip.setText(getWifiIPAddress());

        //
        // Get bridge version string and architecture.
        //

        String bversion = SitMarkAudioBeaconBridge.getBridgeVersionString();
        String archcpu = System.getProperty("os.arch");
        String archlib = findLibrariesArchitecture();

        setTitle("RS " + bversion + " " + archcpu + " " + archlib);

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
    public void onSyncDetected(Object instance, int channel)
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
    public void onBeaconDetected(Object instance, int channel, int beacon)
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

    private static String findLibrariesArchitecture()
    {
        String arch = "n.n.";

        try
        {
            Set<String> libs = new HashSet<String>();
            String mapsFile = "/proc/" + android.os.Process.myPid() + "/maps";
            BufferedReader reader = new BufferedReader(new FileReader(mapsFile));
            String line;

            while ((line = reader.readLine()) != null)
            {
                if (line.endsWith(".so") && line.contains("libSitMark"))
                {
                    String lib = line.substring(line.lastIndexOf(" ") + 1);

                    if (! libs.contains(lib))
                    {
                        Log.d(LOGTAG, "findLibraries: " + lib);
                        libs.add(lib);

                        String[] parts = lib.split("/");
                        if (parts.length > 2) arch = parts[ parts.length - 2 ];
                    }
                }
            }
        }
        catch (Exception ignore)
        {
        }

        return arch;
    }

    public String getWifiIPAddress()
    {
        try
        {
            WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);

            DhcpInfo dhcpInfo = wifiManager.getDhcpInfo();

            int ip = dhcpInfo.ipAddress;

            if (ip != 0)
            {
                return (ip & 0xff) + "." + ((ip >> 8) & 0xff) + "." +
                        ((ip >> 16) & 0xff) + "." + ((ip >> 24) & 0xff);
            }
        }
        catch (Exception ignore)
        {
        }

        return null;
    }

}
