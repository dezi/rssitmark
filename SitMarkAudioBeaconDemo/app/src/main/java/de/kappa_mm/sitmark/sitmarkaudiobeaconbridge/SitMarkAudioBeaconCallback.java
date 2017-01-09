package de.kappa_mm.sitmark.sitmarkaudiobeaconbridge;

public interface SitMarkAudioBeaconCallback
{
    void onSyncDetected(Object instance, int channel);

    void onBeaconDetected(Object instance, int channel, int beacon);
}
