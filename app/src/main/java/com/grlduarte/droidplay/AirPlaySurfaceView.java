package com.grlduarte.droidplay;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import com.github.serezhka.airplay.lib.AudioStreamInfo;
import com.github.serezhka.airplay.lib.VideoStreamInfo;
import com.github.serezhka.airplay.server.AirPlayConfig;
import com.github.serezhka.airplay.server.AirPlayConsumer;
import com.github.serezhka.airplay.server.AirPlayServer;
import com.grlduarte.droidplay.decoder.DataPacket;
import com.grlduarte.droidplay.decoder.VideoDecoder;

public class AirPlaySurfaceView extends SurfaceView {
    private static final String TAG = "AirPlaySurfaceView";

    private Context context;
    private VisibilityCallback visibilityCallback;
    private SurfaceHolder surfaceHolder;

    private AirPlayServer airPlayServer;
    private AirPlayConfig airPlayConfig;
    private String serverName;

    private VideoDecoder videoDecoder;

    public AirPlaySurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public AirPlaySurfaceView(Context context) {
        this(context, null);
    }

    public void init(VisibilityCallback callback) {
        this.visibilityCallback = callback;

        try {
            this.airPlayConfig = AirPlayConfig.DISPLAY_METRICS_30FPS;
            airPlayConfig.setAacEldAudioSupported(
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU);

            airPlayServer = new AirPlayServer(context, airPlayConfig, airPlayConsumer);
            airPlayServer.start();

            SurfaceHolder holder = getHolder();
            holder.addCallback(surfaceHolderCallback);
        } catch (Exception e) {
            Log.e(TAG, "Failed to start AirPlay server", e);
        }
    }

    public void stop() {
        airPlayServer.stop();
    }

    private AirPlayConsumer airPlayConsumer =
            new AirPlayConsumer() {
                @Override
                public void onVideoFormat(VideoStreamInfo videoStreamInfo) {
                    videoDecoder = new VideoDecoder(airPlayConfig);

                    visibilityCallback.showSurface();
                }

                @Override
                public void onVideo(byte[] packet) {
                    if (videoDecoder != null) videoDecoder.addToBuffer(new DataPacket(packet));
                }

                @Override
                public void onVideoSrcDisconnect() {
                    visibilityCallback.hideSurface();

                    if (videoDecoder != null) {
                        videoDecoder.releaseDecoder();
                        videoDecoder = null;
                    }
                }

                @Override
                public void onAudioFormat(AudioStreamInfo audioStreamInfo) {}

                @Override
                public void onAudio(byte[] packet) {}

                @Override
                public void onAudioSrcDisconnect() {}
            };

    private SurfaceHolder.Callback surfaceHolderCallback =
            new SurfaceHolder.Callback() {
                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    videoDecoder.startDecoder(holder);
                }

                @Override
                public void surfaceChanged(
                        SurfaceHolder holder, int format, int width, int height) {}

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {}
            };

    public abstract static class VisibilityCallback {
        abstract void showSurface();

        abstract void hideSurface();
    }
}
