package de.kappa_mm.sitmark.sitmarkaudiobeaconbridge;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.util.Log;

import java.io.File;
import java.nio.ByteBuffer;

public class SitMarkAudioBeaconFile
{
    private static final String LOGTAG = SitMarkAudioBeaconFile.class.getSimpleName();

    private String mediaFile;
    private MediaExtractor extractor;
    private MediaCodec codec;

    public SitMarkAudioBeaconFile()
    {
    }

    public void dodat(String mediaFile)
    {
        this.mediaFile = mediaFile;

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
                    ByteBuffer inputBuffer = codec.getInputBuffer(inputBufferId);

                    int xfer = extractor.readSampleData(inputBuffer, 0);
                    long presentationTimeUs = extractor.getSampleTime();

                    if (xfer < 0) break;
                    Log.d(LOGTAG, "xfer=" + xfer);
                    extractor.advance();

                    codec.queueInputBuffer(inputBufferId, 0, xfer, presentationTimeUs, 0);
                }

                MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

                int outputBufferId = codec.dequeueOutputBuffer(info, 1000);

                if (outputBufferId >= 0)
                {
                    ByteBuffer outputBuffer = codec.getOutputBuffer(outputBufferId);

                    //Log.d(LOGTAG, "outputsize=" + outputBuffer.remaining());

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
