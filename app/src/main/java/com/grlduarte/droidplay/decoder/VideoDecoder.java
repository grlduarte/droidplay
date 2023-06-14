package com.grlduarte.droidplay.decoder;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceHolder;

import com.github.serezhka.airplay.server.AirPlayConfig;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// examples:
// https://github.com/taehwandev/MediaCodecExample
// https://github.com/lucribas/AirplayServer/blob/master/app/src/main/java/com/fang/myapplication/player/VideoPlayer.java
public class VideoDecoder extends Thread {
    private static String TAG = "VideoDecoder";

    private final String mimeType = MediaFormat.MIMETYPE_VIDEO_AVC;
    private final int videoWidth;
    private final int videoHeight;

    private MediaCodec decoder = null;
    private MediaFormat mediaFormat = null;
    private SurfaceHolder holder;
    private List<DataPacket> listBuffer = Collections.synchronizedList(new ArrayList<DataPacket>());
    private List<Integer> inputBuffersIndex =
            Collections.synchronizedList(new ArrayList<Integer>());
    private boolean isRunning = false;

    public VideoDecoder(AirPlayConfig config) {
        videoWidth = config.getWidth();
        videoHeight = config.getHeight();

        try {
            decoder = MediaCodec.createDecoderByType(mimeType);
            decoder.setVideoScalingMode(MediaCodec.VIDEO_SCALING_MODE_SCALE_TO_FIT);
            mediaFormat = getMediaFormat();
            super.start();
        } catch (Exception e) {
            Log.e(TAG, "Failed while initializing audio decoder", e);
        }
    }

    public void startDecoder(SurfaceHolder surfaceHolder) {
        this.holder = surfaceHolder;

        if (decoder != null) {
            try {
                decoder.configure(mediaFormat, holder.getSurface(), null, 0);
                decoder.setCallback(mediaCodecCallback);
            } catch (IllegalStateException e) {
                // decoder already configured, just start it
            }

            decoder.start();
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
        MediaFormat mediaFormat = MediaFormat.createVideoFormat(mimeType, videoWidth, videoHeight);
        MediaCodecInfo decoderInfo = decoder.getCodecInfo();
        Log.i(TAG, "DECODER SELECTED = " + decoderInfo.getName());

        if (decoderSupportsAndroidRLowLatency(
                decoderInfo, mediaFormat.getString(MediaFormat.KEY_MIME))) {
            Log.i(TAG, "Using low-latency mode");
            mediaFormat.setInteger("low-latency", 1);
        }

        return mediaFormat;
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
                        decoder.releaseOutputBuffer(index, true);
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
