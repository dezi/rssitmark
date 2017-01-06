package de.kappa_mm.sitmark.sitmarkaudiobeaconbridge;

import android.util.Log;

public class SitMarkAudioBeaconBridge
{
    private static final String LOGTAG = SitMarkAudioBeaconBridge.class.getSimpleName();
    private static final SitMarkAudioBeaconBridge instance = new SitMarkAudioBeaconBridge();

    public static void initializeBridge()
    {
        System.loadLibrary("SitMarkAudioBeaconBridge");
        System.loadLibrary("SitMarkAudio2MDetectorAPI");

        Log.d(LOGTAG, "Halloooooooooo:" + instance.initializeDecoder());
        Log.d(LOGTAG, "Halloooooooooo:" + getVersionString());
    }

    public native String initializeDecoder();

    public static native String getVersionString();
}
