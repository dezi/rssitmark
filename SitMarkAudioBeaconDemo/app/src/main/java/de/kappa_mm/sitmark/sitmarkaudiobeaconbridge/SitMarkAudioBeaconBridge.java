package de.kappa_mm.sitmark.sitmarkaudiobeaconbridge;

import android.util.Log;

//
// This java cvode has a CPP counterpart in SitMarkAudioBeaconBridge.cpp
//
// It implements the low level interface to Fraunhofer watermark and
// beacon detection.
//

public class SitMarkAudioBeaconBridge
{
    private static final String LOGTAG = SitMarkAudioBeaconBridge.class.getSimpleName();
    private static final String VERSION = "27.03.2017:0";

    //region Constants.

    //
    // The version expected in the binary Fraunhofer libraries.
    //

    private static final String desiredVersion = "Release (Full) - 15.02.2017:0 - Licensed";

    //
    // Some required constants from Fraunhofer.
    //

    private static final String DETECTOR_KEY = "D8F26BEBD8E901A855689B52097914D8";
    private static final double PLAYLENGTH_2_BIT = 0.00264533d;

    //
    // Default sample rate for all audio stream.
    //

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

    //
    // Preload shared libraries and print version string.
    // This call must be done once before all other calls.
    //

    public static void initializeBridge()
    {
        System.loadLibrary("SitMarkAudio2MDetectorAPI");
        System.loadLibrary("SitMarkAudioBeaconBridge");

        Log.d(LOGTAG, "initializeBridge: SitMarkAudio2MDetectorAPI=" + getVersionString());
    }

    //
    // Helper methode for current version check.
    //

    public static boolean isDesiredVersion()
    {
        String version = getVersionString();
        return desiredVersion.equals(version);
    }

    //
    // Create a stream watermark detector and return
    // id reference number.
    //

    public static int createDetectorSW()
    {
        return createDetector(
                nettoMessageLenSW, minFrequencySW, maxFrequencySW,
                useECCSW, wmRedundancySW, sampleRateInHz,
                5, null, false, -1.0
        );
    }

    //
    // Create a high frequency beacon detector and return
    // id reference number.
    //

    public static int createDetectorHF()
    {
        return createDetector(
                nettoMessageLenHF, minFrequencyHF, maxFrequencyHF,
                useECCHF, wmRedundancyHF, sampleRateInHz,
                5, DETECTOR_KEY, true, PLAYLENGTH_2_BIT
        );
    }

    //region Native static methods.

    //
    // Retrieve version string of binary libraries.
    //

    public static native String getVersionString();

    //
    // Create detector instance. Parameters follow Fraunhofer documentation.
    //

    public static native int createDetector(
            int netMessLen, int freqMin, int freqMax,
            int useEcc, int wmRedundancy, int sampleFreq,
            int ringBufferLength,
            String key,
            boolean hfMode,
            double playlength2BitSequence);

    //endregion Native static methods.

    //region Native detector methods.

    //
    // Retrieve detectors current framesize in samples.
    //

    public static native int getFrameSize(int detectorId);

    //
    // Reset detector state after watermark or beacon detection.
    //

    public static native int resetDetector(int detectorId);

    //
    // Destroy and deallocate detector.
    //

    public static native int destroyDetector(int detectorId);

    //
    // Search for sync in high frequency beacon detect mode.
    //

    public static native int searchSync(int detectorId, byte[] audioData);

    //
    // Detect beacon in high frequency mode after a sync was encountered.
    //

    public static native double detectBeacon(int detectorId, byte[] audioData);

    //
    // Detect watermark in embedded watermark buffer mode.
    //

    public static native double detectWatermark(int detectorId, byte[] audioData);

    //
    // Retrieve watermark or beacon with a confidence value.
    //

    public static native double getAccumulatedMessage(int detectorId, char[] messageBuffer);

    //endregion Native detector methods.
}
