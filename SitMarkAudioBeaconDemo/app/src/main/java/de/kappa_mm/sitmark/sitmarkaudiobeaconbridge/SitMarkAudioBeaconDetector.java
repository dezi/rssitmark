package de.kappa_mm.sitmark.sitmarkaudiobeaconbridge;

import android.util.Log;

//
// Simple wrapper java class to use native detectors
// as object instances.
//

@SuppressWarnings("unused")
public class SitMarkAudioBeaconDetector
{
    private static final String LOGTAG = SitMarkAudioBeaconDetector.class.getSimpleName();

    //
    // The underlying native detector id.
    //

    private final int detectorId;

    //
    // Detector is high frequency mode flag.
    //

    private final boolean hf;

    /***
     *
     * Create instance.
     *
     * @param hf    High frequency mode flag.
     *                  true: Beacon detection
     *                  false: Watermark detection
     */

    public SitMarkAudioBeaconDetector(boolean hf)
    {
        this.hf = hf;

        Log.d(LOGTAG, "SitMarkAudioBeaconDetector hf=" + hf);

        detectorId = hf
                    ? SitMarkAudioBeaconBridge.createDetectorHF()
                    : SitMarkAudioBeaconBridge.createDetectorSW()
                    ;
    }

    /***
     *
     * Format binary decoded message into binary coded as decimal string.
     *
     * @param messageBuffer     Binary message buffer
     * @return                  Binary coded as decimal string
     */

    public String getDecodedMessage(char[] messageBuffer)
    {
        int nettoMessageLen = SitMarkAudioBeaconBridge.getMessageLen(hf);

        String idBinary = "";

        for (int inx = 0; inx < nettoMessageLen; inx++)
        {
            idBinary += Character.getNumericValue(messageBuffer[ inx ]);
        }

        return idBinary;
    }

    /***
     *
     * Retrieve detectors current framesize in samples.
     *
     * @return  Framesize in samples
     */

    public int getFrameSize()
    {
        return SitMarkAudioBeaconBridge.getFrameSize(detectorId);
    }

    /***
     * Reset detector state after watermark or beacon detection.
     */

    public void reset()
    {
        SitMarkAudioBeaconBridge.resetDetector(detectorId);
    }

    /***
     * Destroy and deallocate detector.
     */

    public void destroy()
    {
        SitMarkAudioBeaconBridge.destroyDetector(detectorId);
    }

    //
    //
    //

    /***
     *
     * Search for sync in high frequency beacon detect mode.
     *
     * @param audioData     Raw audio data samples in PCM16LE format
     * @return              Sync sample index
     */

    public int searchSync(byte[] audioData)
    {
        return SitMarkAudioBeaconBridge.searchSync(detectorId, audioData);
    }

    /***
     *
     * Detect beacon in high frequency mode after a sync was encountered.
     *
     * @param audioData     Raw audio data samples in PCM16LE format
     * @return              Fraunhofer confidence value
     */

    public double detectBeacon(byte[] audioData)
    {
        return SitMarkAudioBeaconBridge.detectBeacon(detectorId, audioData);
    }

    /***
     *
     * Detect watermark in embedded watermark buffer mode.
     *
     * @param audioData     Raw audio data samples in PCM16LE format
     * @return              Fraunhofer confidence value
     */

    public double detectWatermark(byte[] audioData)
    {
        return SitMarkAudioBeaconBridge.detectWatermark(detectorId, audioData);
    }

    /***
     *
     * Retrieve watermark or beacon with a confidence value.
     *
     * @param message   Preallocated message buffer
     * @return          Fraunhofer confidence value
     */

    public double getAccumulatedMessage(char[] message)
    {
        return SitMarkAudioBeaconBridge.getAccumulatedMessage(detectorId, message);
    }
}
