package de.kappa_mm.sitmark.sitmarkaudiobeaconbridge;

public interface SitMarkAudioBeaconCallback
{
    void onSyncDetected(int channel);

    void onBeaconDetected(int channel, int beacon);
}
