package de.kappa_mm.sitmark.sitmarkaudiobeaconbridge;

//
// Interface for beacon detection call.
//

public interface SitMarkAudioBeaconCallback
{
    /***
     *
     * A sync was detected.
     *
     * @param sender    The SitMarkAudioBeaconListener instance
     * @param channel   The channel number on which it was detected
     */

    void onSyncDetected(Object sender, int channel);

    /***
     *
     * A beacon was detected.
     *
     * @param sender        The SitMarkAudioBeaconListener instance
     * @param channel       The channel number on which it was detected
     * @param confidence    Fraunhofer confidence value
     * @param beacon        The detected beacon as a binary coded decimal string.
     */

    void onBeaconDetected(Object sender, int channel, double confidence, String beacon);
}
