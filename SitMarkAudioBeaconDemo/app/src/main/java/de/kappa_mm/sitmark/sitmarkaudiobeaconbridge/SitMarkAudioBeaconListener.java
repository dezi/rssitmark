package de.kappa_mm.sitmark.sitmarkaudiobeaconbridge;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import de.kappa_mm.sitmark.sitmarkaudiobeaconbridge.SitMarkAudioBeaconBridge;

public class SitMarkAudioBeaconListener
{
    private static final String LOGTAG = SitMarkAudioBeaconListener.class.getSimpleName();

    private SitMarkAudioBeaconCallback callback;

    private RecorderThread audioThread;
    private AudioRecord audioRecord;
    private Handler handler;

    private boolean isListening;
    private boolean isRunning;
    private boolean isGranted;

    private SitMarkAudioBeaconDetector[] detectors;
    private int numChannels;
    private int frameSize;

    public SitMarkAudioBeaconListener()
    {
        handler = new Handler();
    }

    public boolean checkAndRequestPermission(Activity activity)
    {
        int permission = ContextCompat.checkSelfPermission(activity,
                Manifest.permission.RECORD_AUDIO);

        isGranted = (permission == PackageManager.PERMISSION_GRANTED);

        if (! isGranted)
        {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.RECORD_AUDIO},
                    4711);
        }

        return isGranted;
    }

    public void setCallbackListener(SitMarkAudioBeaconCallback callback)
    {
        this.callback = callback;
    }

    public void onStartListening()
    {
        openMics();
    }

    public void onStopListening()
    {
        closeMics();
    }

    private void openMics()
    {
        if (isGranted && ! isListening)
        {
            int sampleRateInHz = SitMarkAudioBeaconBridge.getSampleRate();

            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    sampleRateInHz, AudioFormat.CHANNEL_IN_STEREO,
                    AudioFormat.ENCODING_PCM_16BIT, 4 * sampleRateInHz);

            Log.d(LOGTAG, "openMics STEREO=" + audioRecord.getState());

            if (audioRecord.getState() == AudioRecord.STATE_UNINITIALIZED)
            {
                //
                // dual channel not available -> use mono instead
                //
                // NOTE:
                //
                // 	From Android documentation:
                //		- Sample rate of 44100 	is guaranteed
                //		- CHANNEL_IN_MONO 		is guaranteed
                //		- ENCODING_PCM_16BIT	is guaranteed
                //

                audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                        sampleRateInHz, AudioFormat.CHANNEL_IN_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, 2 * sampleRateInHz);

                Log.d(LOGTAG, "openMics MONO=" + audioRecord.getState());

                numChannels = 1;
            }
            else
            {
                numChannels = 2;
            }

            detectors = new SitMarkAudioBeaconDetector[ numChannels ];

            for (int inx = 0; inx < numChannels; inx++)
            {
                detectors[ inx ] = new SitMarkAudioBeaconDetector();
            }

            frameSize = detectors[ 0 ].getFrameSize();

            audioRecord.startRecording();

            audioThread = new RecorderThread();
            audioThread.start();

            isListening = true;
        }
    }

    private void closeMics()
    {
        if (isListening)
        {
            isRunning = false;

            if (audioThread != null)
            {
                try
                {
                    audioThread.join();
                }
                catch (Exception ignore)
                {
                }

                audioThread = null;
            }

            if (audioRecord != null)
            {
                audioRecord.stop();
                audioRecord = null;
            }

            isListening = false;
        }
    }

    private class RecorderThread extends Thread
    {
        @Override
        public void run()
        {
            super.run();

            Log.d(LOGTAG, "RecorderThread: started.");

            isRunning = true;

            //
            // We have always two buffers filled with samples.
            // Only the older buffer is check for sync. If a
            // sync is found, the buffer frame is completed
            // from the current buffer,
            //

            byte[] lastBuffer = new byte[ frameSize * 2 * numChannels ];
            byte[] thisBuffer = new byte[ frameSize * 2 * numChannels ];

            byte[] cbuffer = new byte[ frameSize * 2 ];

            while (isRunning)
            {
                //
                // Switch buffers.
                //

                byte[] tmp = lastBuffer;
                lastBuffer = thisBuffer;
                thisBuffer = tmp;

                int samplesRead = audioRecord.read(thisBuffer, 0, thisBuffer.length);
                //Log.d(LOGTAG, "RecorderThread: samplesRead=" + samplesRead);

                for (int channel = 0; channel < numChannels; channel++)
                {
                    int sinx = channel * 2;

                    for (int sample = 0; sample < frameSize; sample++)
                    {
                        cbuffer[ (sample << 1) ] = lastBuffer[ sinx ];
                        cbuffer[ (sample << 1) + 1 ] = lastBuffer[ sinx + 1 ];

                        sinx += (numChannels << 1);
                    }

                    int sync = detectors[ channel ].searchSync(cbuffer);

                    if (sync > 0)
                    {
                        Log.d(LOGTAG, "RecorderThread: channel=" + channel + " sync=" + sync);

                        if (callback != null)
                        {
                            final int cbchannel = channel;

                            handler.post(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    callback.onSyncDetected(cbchannel);
                                }
                            });
                        }
                    }
                }
            }

            Log.d(LOGTAG, "RecorderThread: ended.");
        }
    }
}
