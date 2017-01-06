package de.kappa_mm.sitmark.sitmarkaudiobeaconbridge;

public class SitMarkAudioBeaconDetector
{
    private static final String LOGTAG = SitMarkAudioBeaconDetector.class.getSimpleName();

    private final int detectorId;

    public SitMarkAudioBeaconDetector()
    {
        detectorId = SitMarkAudioBeaconBridge.createDetector();
    }

    public int getFrameSize()
    {
        return SitMarkAudioBeaconBridge.getFrameSize(detectorId);
    }
}
