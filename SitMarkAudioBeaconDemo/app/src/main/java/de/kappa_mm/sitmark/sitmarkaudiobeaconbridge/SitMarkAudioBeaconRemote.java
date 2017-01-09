package de.kappa_mm.sitmark.sitmarkaudiobeaconbridge;

import android.app.Activity;
import android.os.Handler;
import android.util.Log;

import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class SitMarkAudioBeaconRemote
{
    private static final String LOGTAG = SitMarkAudioBeaconRemote.class.getSimpleName();

    private SitMarkAudioBeaconCallback callback;

    private DatagramSocket datagramSocket;
    private DatagramPacket datagramPacket;
    private RemoteThread remoteThread;
    private Handler handler;

    private boolean isListening;
    private boolean isRunning;

    private SitMarkAudioBeaconDetector[] detectors;
    private int numChannels;
    private int frameSize;

    public SitMarkAudioBeaconRemote()
    {
        handler = new Handler();
    }

    public boolean checkAndRequestPermission(Activity activity)
    {
        return true;
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
        if (! isListening)
        {
            numChannels = 2;

            detectors = new SitMarkAudioBeaconDetector[ numChannels ];

            for (int inx = 0; inx < numChannels; inx++)
            {
                detectors[ inx ] = new SitMarkAudioBeaconDetector();
            }

            frameSize = detectors[ 0 ].getFrameSize();

            try
            {
                byte[] buffer = new byte[ frameSize * 2 * numChannels ];

                datagramSocket = new DatagramSocket(47474);
                datagramPacket = new DatagramPacket(buffer, buffer.length);
            }
            catch (Exception ignore)
            {
                ignore.printStackTrace();
            }

            remoteThread = new RemoteThread();
            remoteThread.start();

            isListening = true;
        }
    }

    private void closeMics()
    {
        if (isListening)
        {
            isRunning = false;

            if (remoteThread != null)
            {
                try
                {
                    remoteThread.join();
                }
                catch (Exception ignore)
                {
                }

                remoteThread = null;
            }

            if (datagramSocket != null)
            {
                datagramSocket.close();
                datagramSocket = null;
                datagramPacket = null;
            }

            isListening = false;
        }
    }

    private class RemoteThread extends Thread
    {
        @Override
        public void run()
        {
            super.run();

            Log.d(LOGTAG, "RemoteThread: started.");

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

                try
                {
                    datagramSocket.receive(datagramPacket);

                    int samplesRead = datagramPacket.getLength();
                    byte[] data = datagramPacket.getData();
                    System.arraycopy(data, 0, thisBuffer, 0, samplesRead);

                    Log.d(LOGTAG, "RemoteThread: receive length=" + samplesRead);

                    for (int channel = 0; channel < numChannels; channel++)
                    {
                        int sinx = channel * 2;

                        for (int sample = 0; sample < frameSize; sample++)
                        {
                            cbuffer[(sample << 1)] = lastBuffer[sinx];
                            cbuffer[(sample << 1) + 1] = lastBuffer[sinx + 1];

                            sinx += (numChannels << 1);
                        }

                        int sync = detectors[channel].searchSync(cbuffer);

                        if (sync > 0)
                        {
                            Log.d(LOGTAG, "RemoteThread: channel=" + channel + " sync=" + sync);

                            if (callback != null)
                            {
                                final int cbchannel = channel;

                                handler.post(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        callback.onSyncDetected(SitMarkAudioBeaconRemote.this, cbchannel);
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

                            double confidence = detectors[ channel ].detectBeacon(cbuffer);
                            Log.d(LOGTAG, "RecorderThread: channel=" + channel + " confidence=" + confidence);

                            char[] message = new char[ 32 ];

                            double acconfidence = detectors[ channel ].getAccumulatedMessage(message);
                            String beacon = SitMarkAudioBeaconBridge.getDecodedBeacon(message);

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
                                        callback.onBeaconDetected(SitMarkAudioBeaconRemote.this, cbchannel, cbconfidence, cbbeacon);
                                    }
                                });
                            }
                        }
                    }
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }

            Log.d(LOGTAG, "RemoteThread: ended.");
        }
    }
}
