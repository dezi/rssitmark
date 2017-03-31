package de.kappa_mm.sitmark.sitmarkaudiobeaconbridge;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.nio.ByteBuffer;

//
// Simple debugging class to stream an
// audio file into watermark detection.
//

@SuppressWarnings({"FieldCanBeLocal", "WeakerAccess", "unused"})
public class SitMarkAudioBeaconPusher
{
    private static final String LOGTAG = SitMarkAudioBeaconPusher.class.getSimpleName();

    private MediaExtractor extractor;
    private MediaCodec codec;

    public void dodat(String mediaFile, SitMarkAudioBeaconReceiver receiver)
    {
        try
        {
            extractor = new MediaExtractor();
            extractor.setDataSource(mediaFile);

            Log.d(LOGTAG, String.format("TRACKS #: %d", extractor.getTrackCount()));
            MediaFormat format = extractor.getTrackFormat(0);
            String mime = format.getString(MediaFormat.KEY_MIME);
            Log.d(LOGTAG, String.format("MIME TYPE: %s", mime));

            extractor.selectTrack(0);

            codec = MediaCodec.createDecoderByType(mime);
            codec.configure(format, null /* surface */, null /* crypto */, 0 /* flags */);
            codec.start();

            MediaFormat outputFormat = codec.getOutputFormat();
            Log.d(LOGTAG, "orig outputFormat=" + outputFormat);

            for (;;)
            {
                int inputBufferId = codec.dequeueInputBuffer(1000);
                if (inputBufferId >= 0)
                {
                    ByteBuffer inputBuffer = null;

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
                    {
                        inputBuffer = codec.getInputBuffer(inputBufferId);
                    }

                    if (inputBuffer != null)
                    {
                        int xfer = extractor.readSampleData(inputBuffer, 0);
                        if (xfer < 0) break;
                        Log.d(LOGTAG, "xfer=" + xfer);

                        long presentationTimeUs = extractor.getSampleTime();
                        codec.queueInputBuffer(inputBufferId, 0, xfer, presentationTimeUs, 0);
                    }

                    extractor.advance();
                }

                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

                int outputBufferId = codec.dequeueOutputBuffer(info, 1000);

                if (outputBufferId >= 0)
                {
                    ByteBuffer outputBuffer = null;

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
                    {
                        outputBuffer = codec.getOutputBuffer(outputBufferId);
                    }

                    if (outputBuffer != null)
                    {
                        Log.d(LOGTAG, "outputsize=" + outputBuffer.remaining());

                        byte[] buffer = new byte[outputBuffer.remaining()];
                        outputBuffer.get(buffer, 0, buffer.length);

                        receiver.pushBuffer(buffer);
                    }

                    codec.releaseOutputBuffer(outputBufferId, false);
                }
                else
                if (outputBufferId == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED)
                {
                    outputFormat = codec.getOutputFormat();

                    Log.d(LOGTAG, "outputFormat=" + outputFormat);
                }
            }

            codec.stop();
            codec.release();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }
}
