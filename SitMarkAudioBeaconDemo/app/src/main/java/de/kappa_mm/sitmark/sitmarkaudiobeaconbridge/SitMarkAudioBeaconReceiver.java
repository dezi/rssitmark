package de.kappa_mm.sitmark.sitmarkaudiobeaconbridge;

import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

//
// High level watermark interface via audio samples push.
//
// Starts a worker thread for beacon recognition and
// utilizes callback interface for beacon delivery.
//

@SuppressWarnings({"WeakerAccess", "unused"})
public class SitMarkAudioBeaconReceiver
{
    private static final String LOGTAG = SitMarkAudioBeaconReceiver.class.getSimpleName();

    private final Handler handler;

    private SitMarkAudioBeaconCallback callback;

    private final ArrayList<byte[]> bufferQueue;
    private ReceiverThread receiverThread;

    private boolean isListening;
    private boolean isRunning;

    private SitMarkAudioBeaconDetector[] detectors;
    private int numChannels;
    private int frameSize;

    private String logFile;
    private FileOutputStream logStream;

    public SitMarkAudioBeaconReceiver()
    {
        bufferQueue = new ArrayList<>();
        handler = new Handler();
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
        openThread();
    }

    /***
     * Stop listening thread.
     */

    public void onStopListening()
    {
        closeThread();
        closeLog();
    }

    /***
     *
     * Push a raw sample buffer into queue.
     *
     * @param buffer Raw audio samples in PCM16LE format.
     */

    public void pushBuffer(byte[] buffer)
    {
        synchronized (bufferQueue)
        {
            bufferQueue.add(buffer);
        }
    }

    /***
     * Read buffer from queue.
     *
     * @param buffer    Buffer to read into.
     * @param offset    Buffer offset.
     * @param size      Buffer size.
     * @return          Number of bytes read.
     */

    private int readBuffer(byte[] buffer, int offset, int size)
    {
        int read = 0;

        while (isRunning && (read < size))
        {
            byte[] chunk = null;

            synchronized (bufferQueue)
            {
                if (bufferQueue.size() > 0) chunk = bufferQueue.remove(0);
            }

            if (chunk == null)
            {
                try
                {
                    Thread.sleep(10);
                }
                catch (Exception ignore)
                {
                }

                continue;
            }

            if (chunk.length <= (size - read))
            {
                System.arraycopy(chunk, 0, buffer, offset, chunk.length);
                offset += chunk.length;
                read += chunk.length;
            }
            else
            {
                int rest = size - read;

                System.arraycopy(chunk, 0, buffer, offset, rest);
                offset += rest;
                read += rest;

                byte[] restBuffer = new byte[ chunk.length - rest ];
                System.arraycopy(chunk, rest, restBuffer, 0, restBuffer.length);

                synchronized (bufferQueue)
                {
                    bufferQueue.add(0, restBuffer);
                }
            }
        }

        return read;
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
     * Open consumer thread.
     */

    private void openThread()
    {
        if (! isListening)
        {
            numChannels = 2; //todo

            detectors = new SitMarkAudioBeaconDetector[ numChannels ];

            for (int inx = 0; inx < numChannels; inx++)
            {
                detectors[ inx ] = new SitMarkAudioBeaconDetector(false);
            }

            frameSize = detectors[ 0 ].getFrameSize();

            receiverThread = new ReceiverThread();
            receiverThread.start();

            isListening = true;
        }
    }

    /***
     * Close consumer thread.
     */

    private void closeThread()
    {
        if (isListening)
        {
            isRunning = false;

            if (receiverThread != null)
            {
                try
                {
                    receiverThread.join();
                }
                catch (Exception ignore)
                {
                }

                receiverThread = null;
            }

            isListening = false;
        }
    }

    /***
     * Receiver thread working class.
     */

    private class ReceiverThread extends Thread
    {
        @Override
        public void run()
        {
            super.run();

            Log.d(LOGTAG, "ReceiverThread: started.");

            isRunning = true;

            //
            // We have always two buffers filled with samples.
            // Only the older buffer is checked for sync. If a
            // sync is found, the buffer frame is completed
            // from the current buffer.
            //

            byte[] thisBuffer = new byte[ frameSize * 2 * numChannels ];

            byte[] cbuffer = new byte[ frameSize * 2 ];

            long collect = 0;

            while (isRunning)
            {
                if ((collect++ % 100) == 0) Runtime.getRuntime().gc();

                //
                // Switch buffers.
                //

                int samplesRead = readBuffer(thisBuffer, 0, thisBuffer.length);
                Log.d(LOGTAG, "ReceiverThread: samplesRead=" + samplesRead + " framesize=" + frameSize);
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

                for (int channel = 0; channel < numChannels; channel++)
                {
                    int sinx = channel * 2;

                    for (int sample = 0; sample < frameSize; sample++)
                    {
                        cbuffer[(sample << 1)] = thisBuffer[sinx];
                        cbuffer[(sample << 1) + 1] = thisBuffer[sinx + 1];

                        sinx += (numChannels << 1);
                    }

                    //
                    // Detect watermark.
                    //

                    double confidence = detectors[channel].detectWatermark(cbuffer);

                    Log.d(LOGTAG, "ReceiverThread: channel=" + channel + " confidence=" + confidence);

                    if ((collect % 20) == 0)
                    {
                        //
                        // Read value of sound beacon from detector.
                        //

                        char[] message = new char[32];
                        double acconfidence = detectors[channel].getAccumulatedMessage(message);

                        if (acconfidence >= 0)
                        {
                            detectors[channel].reset();

                            String beacon = detectors[channel].getDecodedMessage(message);

                            Log.d(LOGTAG, "ReceiverThread: channel=" + channel + " beacon=" + beacon + " acconf=" + acconfidence);

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
                                        callback.onBeaconDetected(SitMarkAudioBeaconReceiver.this, cbchannel, cbconfidence, cbbeacon);
                                    }
                                });
                            }
                        }
                    }
                }
            }

            Log.d(LOGTAG, "ReceiverThread: ended.");
        }
    }
}
