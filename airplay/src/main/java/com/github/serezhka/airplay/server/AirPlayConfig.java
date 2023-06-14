package com.github.serezhka.airplay.server;

import android.content.res.Resources;

import lombok.Getter;

@Getter
public class AirPlayConfig {
    private static final String DEFAULT_SERVER_NAME = "DroidPlay";

    private String serverName;
    private final int width;
    private final int height;
    private final int fps;
    private final AirPlayFeatures features = AirPlayFeatures.getDefaults();

    public AirPlayConfig(String serverName, int height, int width, int fps) {
        this.serverName = serverName;
        this.height = height;
        this.width = width;
        this.fps = fps;
    }

    public static final AirPlayConfig FULL_HD_60_FPS =
            new AirPlayConfig(DEFAULT_SERVER_NAME, 1920, 1080, 60);

    public static final AirPlayConfig FULL_HD_30_FPS =
            new AirPlayConfig(DEFAULT_SERVER_NAME, 1920, 1080, 30);

    public static final AirPlayConfig DISPLAY_METRICS_60FPS =
            new AirPlayConfig(
                    DEFAULT_SERVER_NAME,
                    Resources.getSystem().getDisplayMetrics().widthPixels,
                    Resources.getSystem().getDisplayMetrics().heightPixels,
                    60);

    public static final AirPlayConfig DISPLAY_METRICS_30FPS =
            new AirPlayConfig(
                    DEFAULT_SERVER_NAME,
                    Resources.getSystem().getDisplayMetrics().widthPixels,
                    Resources.getSystem().getDisplayMetrics().heightPixels,
                    30);

    public long getDecimalFeatures() {
        return features.toDecimal();
    }

    public String getStringFeatures() {
        return features.toString();
    }

    public void setAacEldAudioSupported(boolean supported) {
        features.setAacEldAudioSupported(supported);
    }
}
