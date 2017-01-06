package de.kappa_mm.sitmark.sitmarkaudiobeaconbridge;

import android.util.Log;

public class SitMarkAudioBeaconBridge
{
    private static final String LOGTAG = SitMarkAudioBeaconBridge.class.getSimpleName();

    private static final String DETECTOR_KEY = "D8F26BEBD8E901A855689B52097914D8";
    private static final double PLAYLENGTH_2_BIT = 0.00264533d;

    private static final int nettoMessageLen = 24;

    private static final int sampleRateInHz = 44100;
    private static final int minFrequencyHF = 20270;
    private static final int maxFrequencyHF = 21430;

    private static final int useECC = 0;
    private static final int wmRedundancy = 1;

    public static void initializeBridge()
    {
        System.loadLibrary("SitMarkAudio2MDetectorAPI");
        System.loadLibrary("SitMarkAudioBeaconBridge");

        Log.d(LOGTAG, "initializeBridge: SitMarkAudio2MDetectorAPI=" + getVersionString());

        initializeHF(
                nettoMessageLen, minFrequencyHF, maxFrequencyHF,
                useECC, wmRedundancy, sampleRateInHz,
                5, DETECTOR_KEY, true, PLAYLENGTH_2_BIT
                );
    }

    public static native String getVersionString();

    public static native int initializeHF(
            int netMessLen, int freqMin, int freqMax,
            int useEcc, int wmRedundancy, int sampleFreq,
            int ringBufferLength,
            String key,
            boolean hfMode,
            double playlength2BitSequence);
}
