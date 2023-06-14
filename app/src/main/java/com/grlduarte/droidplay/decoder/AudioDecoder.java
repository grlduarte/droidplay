package com.grlduarte.droidplay.decoder;

import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

import com.github.serezhka.airplay.server.AirPlayConfig;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// examples:
// https://github.com/taehwandev/MediaCodecExample
public class AudioDecoder extends Thread {
    private static String TAG = "AudioDecoder";

    private String mimeType;
    private int audioProfile;
    private final int sampleRate = 44100;
    private final int channelCount = 2;

    private MediaCodec decoder = null;
    private MediaFormat mediaFormat = null;
    private AudioTrack audioTrack = null;
    private List<DataPacket> listBuffer = Collections.synchronizedList(new ArrayList<DataPacket>());
    private List<Integer> inputBuffersIndex =
            Collections.synchronizedList(new ArrayList<Integer>());
    private boolean isRunning = false;

    public AudioDecoder(AirPlayConfig config) {
        if (config.isAacEldAudioSupported()) {
            mimeType = MediaFormat.MIMETYPE_AUDIO_AAC_ELD;
            audioProfile = MediaCodecInfo.CodecProfileLevel.AACObjectELD;
        } else {
            mimeType = MediaFormat.MIMETYPE_AUDIO_AAC;
            audioProfile = MediaCodecInfo.CodecProfileLevel.AACObjectLC;
        }

        try {
            decoder = MediaCodec.createDecoderByType(mimeType);
            decoder.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            mediaFormat = getMediaFormat();
            audioTrack = getAudioTrack();
            super.start();
        } catch (Exception e) {
            Log.e(TAG, "Failed while initializing audio decoder", e);
        }
    }

    public void startDecoder() {
        if (decoder != null) {
            try {
                decoder.configure(mediaFormat, null, null, 0);
                decoder.setCallback(mediaCodecCallback);
            } catch (IllegalStateException e) {
                // decoder already configured, just start it
            }

            decoder.start();
            audioTrack.play();
            isRunning = true;
        }
    }

    public void releaseDecoder() {
        super.interrupt();

        isRunning = false;
        listBuffer.clear();
        inputBuffersIndex.clear();

        if (decoder != null) {
            decoder.stop();
            decoder.release();
            decoder = null;
        }

        if (audioTrack != null) {
            audioTrack.stop();
            audioTrack.release();
            audioTrack = null;
        }
    }

    public void addToBuffer(DataPacket packet) {
        listBuffer.add(packet);
    }

    @Override
    public void run() {
        super.run();

        try {
            while (true) {
                if (!isRunning | (listBuffer.size() == 0) | (inputBuffersIndex.size() == 0)) {
                    sleep(1);
                } else {
                    Log.d("DEBUG", inputBuffersIndex.size() + " available buffers");
                    Log.d("DEBUG", listBuffer.size() + " packets waiting");
                    Log.d("DEBUG", "");
                    try {
                        int index = inputBuffersIndex.remove(0);
                        byte[] packet = listBuffer.remove(0).data;
                        ByteBuffer inputBuf = decoder.getInputBuffer(index);
                        inputBuf.put(packet, 0, packet.length);
                        decoder.queueInputBuffer(index, 0, packet.length, 0, 0);
                    } catch (IllegalStateException e) {
                        Log.e(TAG, "Not in executing state, skipping");
                    }
                }
            }
        } catch (InterruptedException e) {
            Log.d("DEBUG", "thread stopped");
            return;
        }
    }

    private boolean decoderSupportsAndroidRLowLatency(MediaCodecInfo decoderInfo, String mimeType) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                if (decoderInfo
                        .getCapabilitiesForType(mimeType)
                        .isFeatureSupported(MediaCodecInfo.CodecCapabilities.FEATURE_LowLatency)) {
                    return true;
                }
            } catch (Exception e) {
                // Tolerate buggy codecs
                e.printStackTrace();
            }
        }

        Log.d(TAG, "Low latency decoding mode NOT supported (FEATURE_LowLatency)");
        return false;
    }

    private MediaFormat getMediaFormat() {
        // index of the sample rate in the array:
        // [ 96000, 88200, 64000, 48000, 44100, 32000,
        //   24000, 22050, 16000, 12000, 11025, 8000 ]
        int frequencyIndex = 4;

        // create the 2 bytes length header for AAC
        ByteBuffer csd = ByteBuffer.allocate(4);
        csd.put((byte) (audioProfile << 3 | frequencyIndex >> 1));
        csd.position(1);
        csd.put((byte) (frequencyIndex << 7 & 0x80 | channelCount << 3));
        csd.flip();

        MediaFormat mediaFormat = MediaFormat.createAudioFormat(mimeType, sampleRate, channelCount);
        mediaFormat.setByteBuffer("csd-0", csd);

        if (decoderSupportsAndroidRLowLatency(
                decoder.getCodecInfo(), mediaFormat.getString(MediaFormat.KEY_MIME))) {
            Log.i(TAG, "Using low-latency mode");
            mediaFormat.setInteger("low-latency", 1);
        }

        return mediaFormat;
    }

    private AudioTrack getAudioTrack() {
        final AudioFormat audioFormat =
                new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .build();

        final AudioAttributes audioAttributes =
                new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build();

        final int bufferSize =
                AudioTrack.getMinBufferSize(
                        sampleRate, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT);

        return new AudioTrack(
                audioAttributes,
                audioFormat,
                bufferSize,
                AudioTrack.MODE_STREAM,
                AudioManager.AUDIO_SESSION_ID_GENERATE);
    }

    private MediaCodec.Callback mediaCodecCallback =
            new MediaCodec.Callback() {
                @Override
                public void onInputBufferAvailable(MediaCodec codec, int index) {
                    inputBuffersIndex.add(index);
                }

                @Override
                public void onOutputBufferAvailable(
                        MediaCodec codec, int index, MediaCodec.BufferInfo info) {
                    try {
                        ByteBuffer outputBuf = decoder.getOutputBuffer(index);
                        byte[] chunk = new byte[info.size];
                        outputBuf.get(chunk);
                        outputBuf.clear();
                        audioTrack.write(
                                chunk,
                                info.offset,
                                info.offset + info.size,
                                AudioTrack.WRITE_NON_BLOCKING);
                        decoder.releaseOutputBuffer(index, false);
                    } catch (IllegalStateException e) {
                        Log.e(TAG, "Not in executing state, skipping");
                    }
                }

                @Override
                public void onError(MediaCodec codec, MediaCodec.CodecException e) {
                    Log.e(TAG, "Error while decoding packet", e);
                }

                @Override
                public void onOutputFormatChanged(MediaCodec codec, MediaFormat format) {
                    Log.d(TAG, "Output format changed: " + format.toString());
                }
            };
}
