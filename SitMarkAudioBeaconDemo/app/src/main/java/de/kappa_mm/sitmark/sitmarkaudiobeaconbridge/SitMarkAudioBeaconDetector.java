package de.kappa_mm.sitmark.sitmarkaudiobeaconbridge;

public class SitMarkAudioBeaconDetector
{
    private static final String LOGTAG = SitMarkAudioBeaconDetector.class.getSimpleName();

    private final int detectorId;

    public SitMarkAudioBeaconDetector()
    {
        detectorId = SitMarkAudioBeaconBridge.createDetector();
    }

    public void destroy()
    {
        SitMarkAudioBeaconBridge.destroyDetector(detectorId);
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

    public double getAccumulatedMessage(char[] message)
    {
        return SitMarkAudioBeaconBridge.getAccumulatedMessage(detectorId, message);
    }
}
