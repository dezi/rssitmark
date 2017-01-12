package de.kappa_mm.sitmark.sitmarkaudiobeaconbridge;

import android.util.Log;

public class SitMarkAudioBeaconBridge
{
    private static final String LOGTAG = SitMarkAudioBeaconBridge.class.getSimpleName();
    private static final String VERSION = "09.01.2017:0";

    //region Constants.

    private static final String desiredVersion = "Release (Full) - 04.01.2017:0 - Licensed";

    private static final String DETECTOR_KEY = "D8F26BEBD8E901A855689B52097914D8";
    private static final double PLAYLENGTH_2_BIT = 0.00264533d;

    private static final int nettoMessageLen = 24;

    private static final int sampleRateInHz = 44100;
    private static final int minFrequencyHF = 20270;
    private static final int maxFrequencyHF = 21430;

    private static final int useECC = 0;
    private static final int wmRedundancy = 1;

    //endregion Constants

    public static String getBridgeVersionString()
    {
        return VERSION;
    }

    public static int getSampleRate()
    {
        return sampleRateInHz;
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

    public static int createDetector()
    {
        return createDetector(
                nettoMessageLen, minFrequencyHF, maxFrequencyHF,
                useECC, wmRedundancy, sampleRateInHz,
                5, DETECTOR_KEY, true, PLAYLENGTH_2_BIT
        );
    }

    public static String getDecodedBeacon(char[] messageBuffer)
    {
        String idBinary = "";

        for (int inx = 0; inx < nettoMessageLen; inx++)
        {
            idBinary += Character.getNumericValue(messageBuffer[ inx ]);
        }

        return idBinary;
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

    public static native double getAccumulatedMessage(int detectorId, char[] messageBuffer);

    //endregion Native detector methods.
}
