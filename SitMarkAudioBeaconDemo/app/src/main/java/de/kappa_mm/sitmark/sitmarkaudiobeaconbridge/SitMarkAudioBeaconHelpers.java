package de.kappa_mm.sitmark.sitmarkaudiobeaconbridge;

import android.app.Activity;
import android.content.Context;
import android.net.DhcpInfo;
import android.net.wifi.WifiManager;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

public class SitMarkAudioBeaconHelpers
{
    private static final String LOGTAG = SitMarkAudioBeaconHelpers.class.getSimpleName();

    public static String findLibrariesArchitecture()
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

    public static String getWifiIPAddress(Context context)
    {
        try
        {
            WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);

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

    public static int getScreenWidth(Activity activity)
    {
        DisplayMetrics displaymetrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        return displaymetrics.widthPixels;
    }
}
