package de.kappa_mm.sitmark.sitmarkaudiobeaconbridge;

import android.util.Log;

public class SitMarkAudioBeaconDetector
{
    private static final String LOGTAG = SitMarkAudioBeaconDetector.class.getSimpleName();

    private final int detectorId;
    private final boolean hf;

    public SitMarkAudioBeaconDetector(boolean hf)
    {
        this.hf = hf;

        Log.d(LOGTAG, "SitMarkAudioBeaconDetector hf=" + hf);

        detectorId = hf
                    ? SitMarkAudioBeaconBridge.createDetectorHF()
                    : SitMarkAudioBeaconBridge.createDetectorSW()
                    ;
    }

    public String getDecodedBeacon(char[] messageBuffer)
    {
        int nettoMessageLen = SitMarkAudioBeaconBridge.getMessageLen(hf);

        String idBinary = "";

        for (int inx = 0; inx < nettoMessageLen; inx++)
        {
            idBinary += Character.getNumericValue(messageBuffer[ inx ]);
        }

        return idBinary;
    }

    public void destroy()
    {
        SitMarkAudioBeaconBridge.destroyDetector(detectorId);
    }

    public void reset()
    {
        SitMarkAudioBeaconBridge.resetDetector(detectorId);
    }

    public int getFrameSize()
    {
        return SitMarkAudioBeaconBridge.getFrameSize(detectorId);
    }

    public int searchSync(byte[] audioData)
    {
        return SitMarkAudioBeaconBridge.searchSync(detectorId, audioData);
    }

    public double detectBeacon(byte[] audioData)
    {
        return SitMarkAudioBeaconBridge.detectBeacon(detectorId, audioData);
    }

    public double detectWatermark(byte[] audioData)
    {
        return SitMarkAudioBeaconBridge.detectWatermark(detectorId, audioData);
    }

    public double getAccumulatedMessage(char[] message)
    {
        return SitMarkAudioBeaconBridge.getAccumulatedMessage(detectorId, message);
    }
}
