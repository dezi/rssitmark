package de.kappa_mm.sitmark.sitmarkaudiobeaconbridge;

import android.support.v4.content.ContextCompat;
import android.support.v4.app.ActivityCompat;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.app.Activity;
import android.Manifest;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;

//import android.media.audiofx.AcousticEchoCanceler;
//import android.media.audiofx.AutomaticGainControl;
//import android.media.audiofx.NoiseSuppressor;

//
// High level beacon detection interface via microphone.
//
// Starts a worker thread for beacon recognition and
// utilizes callback interface for beacon delivery.
//

@SuppressWarnings({"WeakerAccess", "unused"})
public class SitMarkAudioBeaconListener
{
    private static final String LOGTAG = SitMarkAudioBeaconListener.class.getSimpleName();

    private final Handler handler;

    private SitMarkAudioBeaconCallback callback;

    private RecorderThread audioThread;
    private AudioRecord audioRecord;

    private boolean isListening;
    private boolean isRunning;
    private boolean isGranted;

    private SitMarkAudioBeaconDetector[] detectors;
    private int numChannels;
    private int frameSize;

    private String logFile;
    private FileOutputStream logStream;

    public SitMarkAudioBeaconListener()
    {
        handler = new Handler();
    }

    /***
     *
     * Utility method to request audio record permission.
     *
     * @param activity  Current activity.
     * @return          True if permission is granted.
     */

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

    /***
     *
     * Register a callback instance.
     *
     * @param callback  The instance to be called.
     */

    public void setCallbackListener(SitMarkAudioBeaconCallback callback)
    {
        this.callback = callback;
    }

    /***
     *
     * Register a log file.
     *
     * @param logFile   Log file name.
     */

    public void setLogFile(String logFile)
    {
        this.logFile = logFile;
    }

    /***
     * Start listening thread.
     */

    public void onStartListening()
    {
        openLog();
        openMics();
    }

    /***
     * Stop listening thread.
     */

    public void onStopListening()
    {
        closeMics();
        closeLog();
    }

    /***
     * Open log.
     */

    private void openLog()
    {
        if ((logFile != null) && ! logFile.isEmpty())
        {
            String realFile = logFile + ".s16le";

            File file = new File(Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DOWNLOADS), realFile);

            Log.d(LOGTAG,"openLog: file=" + file.toString());

            try
            {
                logStream = new FileOutputStream(file);

                Log.d(LOGTAG,"openLog: is open file=" + file.toString());
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
        }
    }

    /***
     * Close log.
     */

    private void closeLog()
    {
        if (logStream != null)
        {
            try
            {
                logStream.close();
            }
            catch (Exception ignore)
            {
            }

            logStream = null;
            logFile = null;
        }
    }

    /***
     * Open microphone setup.
     */

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
                // Dual channel not available -> use mono instead.
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
            }

            numChannels = audioRecord.getChannelCount();

            //AutomaticGainControl agc = AutomaticGainControl.create(audioRecord.getAudioSessionId());
            //Log.d(LOGTAG, "AutomaticGainControl=" + agc.getEnabled());

            //AcousticEchoCanceler aec = AcousticEchoCanceler.create(audioRecord.getAudioSessionId());
            //Log.d(LOGTAG, "AcousticEchoCanceler=" + aec.getEnabled());

            //NoiseSuppressor ns = NoiseSuppressor.create(audioRecord.getAudioSessionId());
            //Log.d(LOGTAG, "NoiseSuppressor=" + ns.getEnabled());

            detectors = new SitMarkAudioBeaconDetector[ numChannels ];

            for (int inx = 0; inx < numChannels; inx++)
            {
                detectors[ inx ] = new SitMarkAudioBeaconDetector(true);
            }

            frameSize = detectors[ 0 ].getFrameSize();

            audioRecord.startRecording();

            audioThread = new RecorderThread();
            audioThread.start();

            isListening = true;
        }
    }

    /***
     * Close microphone setup.
     */

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

    /***
     * Recorder thread working class.
     */

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
            // Only the older buffer is checked for sync. If a
            // sync is found, the buffer frame is completed
            // from the current buffer.
            //

            byte[] lastBuffer = new byte[ frameSize * 2 * numChannels ];
            byte[] thisBuffer = new byte[ frameSize * 2 * numChannels ];

            byte[] cbuffer = new byte[ frameSize * 2 ];

            long collect = 0;

            while (isRunning)
            {
                if ((collect++ % 100) == 0) Runtime.getRuntime().gc();

                //
                // Switch buffers.
                //

                byte[] tmp = lastBuffer;
                lastBuffer = thisBuffer;
                thisBuffer = tmp;

                int samplesRead = audioRecord.read(thisBuffer, 0, thisBuffer.length);
                //Log.d(LOGTAG, "RecorderThread: samplesRead=" + samplesRead);
                if (samplesRead < thisBuffer.length) break;

                if (logStream != null)
                {
                    try
                    {
                        logStream.write(thisBuffer);
                    }
                    catch (Exception ignore)
                    {
                    }
                }

                //SitMarkAudioBeaconHelpers.maskNoiseBits(thisBuffer, numChannels, 2);
                //SitMarkAudioBeaconHelpers.multiplyAmplitude(thisBuffer, numChannels, 0.1f);

                //int avgAmp = SitMarkAudioBeaconHelpers.getAVGAmplitude(thisBuffer, numChannels);
                //int maxAmp = SitMarkAudioBeaconHelpers.getMAXAmplitude(thisBuffer, numChannels);
                //int HFsamp = SitMarkAudioBeaconHelpers.getNUMHFSamples(thisBuffer, numChannels);

                //Log.d(LOGTAG, "RecorderThread: avgAmp=" + avgAmp + " maxAmp=" + maxAmp + " HFsamp=" + HFsamp);

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
                                    callback.onSyncDetected(SitMarkAudioBeaconListener.this, cbchannel);
                                }
                            });
                        }

                        //
                        // Shift down current buffer.
                        //

                        int newpos = 0;
                        int oldpos = sync * 2;

                        while (oldpos < cbuffer.length)
                        {
                            cbuffer[ newpos++ ] = cbuffer[ oldpos++ ];
                            cbuffer[ newpos++ ] = cbuffer[ oldpos++ ];
                        }

                        //
                        // Complete buffer from actual frame.
                        //

                        sinx = channel * 2;

                        while (newpos < cbuffer.length)
                        {
                            cbuffer[ newpos++ ] = thisBuffer[ sinx ];
                            cbuffer[ newpos++ ] = thisBuffer[ sinx + 1 ];

                            sinx += (numChannels << 1);
                        }

                        //
                        // Detect sound beacon from now complete frame.
                        //

                        double confidence = detectors[ channel ].detectBeacon(cbuffer);

                        Log.d(LOGTAG, "RecorderThread: channel=" + channel + " confidence=" + confidence);

                        //
                        // Read value of sound beacon from detector.
                        //

                        char[] message = new char[ 32 ];
                        double acconfidence = detectors[ channel ].getAccumulatedMessage(message);
                        String beacon = detectors[ channel ].getDecodedMessage(message);

                        Log.d(LOGTAG, "RecorderThread: channel=" + channel + " beacon=" + beacon);

                        if (callback != null)
                        {
                            final int cbchannel = channel;
                            final String cbbeacon = beacon;
                            final double cbconfidence = acconfidence;

                            handler.post(new Runnable()
                            {
                                @Override
                                public void run()
                                {
                                    callback.onBeaconDetected(SitMarkAudioBeaconListener.this, cbchannel, cbconfidence, cbbeacon);
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
