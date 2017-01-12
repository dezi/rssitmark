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

    public static int getAVGAmplitude(byte[] audio, int numChannels)
    {
        int[] summs = new int[ 2 ];

        short value;
        int sample = 0;

        while (sample < audio.length)
        {
            for (int channel = 0; channel < numChannels; channel++)
            {
                value = (short) ((audio[ sample ] & 0xff) + ((audio[ sample + 1 ] & 0xff) << 8));

                summs[ channel ] += Math.abs(value);

                sample += 2;
            }
        }

        int samplesPerChannel = audio.length / (2 * numChannels);

        int avg = 0;

        for (int channel = 0; channel < numChannels; channel++)
        {
            avg += summs[ channel ] /  samplesPerChannel;
        }

        return avg / numChannels;
    }

    public static int getMAXAmplitude(byte[] audio, int numChannels)
    {
        int[] maxis = new int[ 2 ];

        short value;
        int sample = 0;

        while (sample < audio.length)
        {
            for (int channel = 0; channel < numChannels; channel++)
            {
                value = (short) ((audio[ sample ] & 0xff) + ((audio[ sample + 1 ] & 0xff) << 8));

                if (Math.abs(value) > maxis[ channel ]) maxis[ channel ] = Math.abs(value);

                sample += 2;
            }
        }

        int samplesPerChannel = audio.length / (2 * numChannels);

        int max = 0;

        for (int channel = 0; channel < numChannels; channel++)
        {
            if (maxis[ channel ] > max) max = maxis[ channel ];
        }

        return max;
    }

    public static int getHFSamples(byte[] audio, int numChannels)
    {
        int[] hfcount = new int[ 2 ];

        short[][] values = new short[ 2 ][ 4 ];

        short value;

        int sample = 0;

        while (sample < audio.length)
        {
            for (int channel = 0; channel < numChannels; channel++)
            {
                value = (short) ((audio[ sample ] & 0xff) + ((audio[ sample + 1 ] & 0xff) << 8));

                values[ channel ][ 0 ] = values[ channel ][ 1 ];
                values[ channel ][ 1 ] = values[ channel ][ 2 ];
                values[ channel ][ 2 ] = values[ channel ][ 3 ];
                values[ channel ][ 3 ] = value;

                if ((values[ channel ][ 0 ] > values[ channel ][ 1 ] + 30) &&
                    (values[ channel ][ 1 ] + 30 < values[ channel ][ 2 ]) &&
                    (values[ channel ][ 2 ] > values[ channel ][ 3 ] + 30))
                {
                    hfcount[ channel ] += 1;
                }

                if ((values[ channel ][ 0 ] + 30 < values[ channel ][ 1 ]) &&
                    (values[ channel ][ 1 ] > values[ channel ][ 2 ] + 30) &&
                    (values[ channel ][ 2 ] + 30 < values[ channel ][ 3 ]))
                {
                    hfcount[ channel ] += 1;
                }

                sample += 2;
            }
        }

        int avg = 0;

        for (int channel = 0; channel < numChannels; channel++)
        {
            avg += hfcount[ channel ];
        }

        return avg / numChannels;
    }

    public static void multiplyAmplitude(byte[] audio, int numChannels, float factor)
    {
        short value;
        int sample = 0;

        while (sample < audio.length)
        {
            for (int channel = 0; channel < numChannels; channel++)
            {
                value = (short) ((audio[ sample ] & 0xff) + ((audio[ sample + 1 ] & 0xff) << 8));

                value = (short) Math.round(value * factor);

                audio[ sample ] = (byte) (value & 0xff);
                audio[ sample + 1 ] = (byte) ((value >> 8) & 0xff);

                sample += 2;
            }
        }
    }

    public static void maskNoiseBits(byte[] audio, int numChannels, int bits)
    {
        if (bits > 0)
        {
            short value;
            int sample = 0;

            while (sample < audio.length)
            {
                for (int channel = 0; channel < numChannels; channel++)
                {
                    value = (short) ((audio[sample] & 0xff) + ((audio[sample + 1] & 0xff) << 8));

                    value = (short) (value >> bits);
                    value = (short) (value << bits);

                    audio[sample] = (byte) (value & 0xff);
                    audio[sample + 1] = (byte) ((value >> 8) & 0xff);

                    sample += 2;
                }
            }
        }
    }
}
