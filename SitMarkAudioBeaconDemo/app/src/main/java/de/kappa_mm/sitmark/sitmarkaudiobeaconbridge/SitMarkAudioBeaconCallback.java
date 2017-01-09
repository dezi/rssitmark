package de.kappa_mm.sitmark.sitmarkaudiobeaconbridge;

public interface SitMarkAudioBeaconCallback
{
    void onSyncDetected(Object sender, int channel);

    void onBeaconDetected(Object sender, int channel, double confidence, String beacon);
}
