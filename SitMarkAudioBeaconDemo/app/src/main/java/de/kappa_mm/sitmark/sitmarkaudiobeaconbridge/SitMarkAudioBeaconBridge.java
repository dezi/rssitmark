package de.kappa_mm.sitmark.sitmarkaudiobeaconbridge;

import android.util.Log;

public class SitMarkAudioBeaconBridge
{
    private static final String LOGTAG = SitMarkAudioBeaconBridge.class.getSimpleName();
    private static final String VERSION = "20.02.2017:0";

    //region Constants.

    private static final String desiredVersion = "Release (Full) - 15.02.2017:0 - Licensed";

    private static final String DETECTOR_KEY = "D8F26BEBD8E901A855689B52097914D8";
    private static final double PLAYLENGTH_2_BIT = 0.00264533d;

    private static final int sampleRateInHz = 44100;

    //
    // High frequency beacons parameters.
    //

    private static final int nettoMessageLenHF = 24;
    private static final int minFrequencyHF = 20270;
    private static final int maxFrequencyHF = 21430;
    private static final int wmRedundancyHF = 1;
    private static final int useECCHF = 0;

    //
    // Stream watermark parameters.
    //

    private static final int nettoMessageLenSW = 12;
    private static final int minFrequencySW = 1000;
    private static final int maxFrequencySW = 10000;
    private static final int wmRedundancySW = 2;
    private static final int useECCSW = 1;

    //endregion Constants

    public static String getBridgeVersionString()
    {
        return VERSION;
    }

    public static int getSampleRate()
    {
        return sampleRateInHz;
    }

    public static int getMessageLen(boolean hf)
    {
        return hf ? nettoMessageLenHF : nettoMessageLenSW;
    }

    public static void initializeBridge()
    {
        System.loadLibrary("SitMarkAudio2MDetectorAPI");
        System.loadLibrary("SitMarkAudioBeaconBridge");

        Log.d(LOGTAG, "initializeBridge: SitMarkAudio2MDetectorAPI=" + getVersionString());
    }

    public static boolean isDesiredVersion()
    {
        String version = getVersionString();
        return desiredVersion.equals(version);
    }

    public static int createDetectorSW()
    {
        return createDetector(
                nettoMessageLenSW, minFrequencySW, maxFrequencySW,
                useECCSW, wmRedundancySW, sampleRateInHz,
                5, DETECTOR_KEY, false, PLAYLENGTH_2_BIT
        );
    }

    public static int createDetectorHF()
    {
        return createDetector(
                nettoMessageLenHF, minFrequencyHF, maxFrequencyHF,
                useECCHF, wmRedundancyHF, sampleRateInHz,
                5, DETECTOR_KEY, true, PLAYLENGTH_2_BIT
        );
    }

    //region Native static methods.

    public static native String getVersionString();

    public static native int createDetector(
            int netMessLen, int freqMin, int freqMax,
            int useEcc, int wmRedundancy, int sampleFreq,
            int ringBufferLength,
            String key,
            boolean hfMode,
            double playlength2BitSequence);

    //endregion Native static methods.

    //region Native detector methods.

    public static native int getFrameSize(int detectorId);

    public static native int resetDetector(int detectorId);

    public static native int destroyDetector(int detectorId);

    public static native int searchSync(int detectorId, byte[] audioData);

    public static native double detectBeacon(int detectorId, byte[] audioData);

    public static native double detectWatermark(int detectorId, byte[] audioData);

    public static native double getAccumulatedMessage(int detectorId, char[] messageBuffer);

    //endregion Native detector methods.
}
