package de.kappa_mm.sitmark.sitmarkaudiobeaconbridge;

import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class SitMarkAudioBeaconReceiver
{
    private static final String LOGTAG = SitMarkAudioBeaconReceiver.class.getSimpleName();

    private SitMarkAudioBeaconCallback callback;

    private final ArrayList<byte[]> bufferQueue;
    private ReceiverThread receiverThread;
    private Handler handler;

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

    public void setCallbackListener(SitMarkAudioBeaconCallback callback)
    {
        this.callback = callback;
    }

    public void onStartListening()
    {
        //openLog();
        openThread();
    }

    public void onStopListening()
    {
        closeThread();
        //closeLog();
    }

    public void pushBuffer(byte[] buffer)
    {
        synchronized (bufferQueue)
        {
            bufferQueue.add(buffer);
        }
    }

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

    private void openLog()
    {
        logFile = "dezibezi";

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

                int samplesRead = readBuffer(thisBuffer, 0, thisBuffer.length);
                Log.d(LOGTAG, "ReceiverThread: samplesRead=" + samplesRead);
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
                        cbuffer[(sample << 1)] = lastBuffer[sinx];
                        cbuffer[(sample << 1) + 1] = lastBuffer[sinx + 1];

                        sinx += (numChannels << 1);
                    }

                    //
                    // Detect watermark.
                    //

                    double confidence = detectors[channel].detectWatermark(cbuffer);

                    Log.d(LOGTAG, "ReceiverThread: channel=" + channel + " confidence=" + confidence);

                    /*
                    //
                    // Read value of sound beacon from detector.
                    //

                    char[] message = new char[32];
                    double acconfidence = detectors[channel].getAccumulatedMessage(message);
                    String beacon = detectors[channel].getDecodedBeacon(message);

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
                    */
                }
            }

            Log.d(LOGTAG, "ReceiverThread: ended.");
        }
    }
}
