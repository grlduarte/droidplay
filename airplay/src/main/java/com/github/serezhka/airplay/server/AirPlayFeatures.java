package com.github.serezhka.airplay.server;

import java.util.BitSet;

public class AirPlayFeatures {
    private BitSet features = new BitSet(32);

    private static final int BIT_VIDEO = 0;
    private static final int BIT_PHOTO = 1;
    private static final int BIT_VIDEO_FAIRPLAY = 2;
    private static final int BIT_VOLUME_CONTROL = 3;
    private static final int BIT_VIDEO_HTTP_LIVE_STREAM = 4;
    private static final int BIT_SLIDESHOW = 5;
    private static final int BIT_SCREEN = 7;
    private static final int BIT_SCREEN_ROTATE = 8;
    private static final int BIT_AUDIO = 9;
    private static final int BIT_AUDIO_REDUNDANT = 11;
    private static final int BIT_FAIRPLAY_SECURE_AUTH = 12;
    private static final int BIT_PHOTO_CACHING = 13;
    private static final int BIT_AUTHENTICATION_4 = 14;
    private static final int BIT_METADATA_FEATURE_1 = 15;
    private static final int BIT_METADATA_FEATURE_2 = 16;
    private static final int BIT_METADATA_FEATURE_0 = 17;
    private static final int BIT_AUDIO_FORMAT_1 = 18;
    private static final int BIT_AUDIO_FORMAT_2 = 19;
    private static final int BIT_AUDIO_FORMAT_3 = 20;
    private static final int BIT_AUDIO_FORMAT_4 = 21;
    private static final int BIT_AUTHENTICATION_1 = 23;
    private static final int BIT_SUPPORTS_LEGACY_PAIRING = 27;
    private static final int BIT_RAOP = 30;

    private AirPlayFeatures(BitSet features) {
        this.features = features;
    }

    public static AirPlayFeatures getDefaults() {
        BitSet features = new BitSet(32);
        features.clear();
        features.set(BIT_SCREEN);
        features.set(BIT_AUDIO);
        features.set(10);
        features.set(BIT_FAIRPLAY_SECURE_AUTH);
        features.set(BIT_AUTHENTICATION_4);
        features.set(BIT_AUDIO_FORMAT_2);
        features.set(BIT_AUDIO_FORMAT_3);
        features.set(BIT_SUPPORTS_LEGACY_PAIRING);
        features.set(BIT_RAOP);

        return new AirPlayFeatures(features);
    }

    public boolean isAacEldAudioSupported() {
        return features.get(BIT_AUDIO_FORMAT_1) && features.get(BIT_AUDIO_FORMAT_4);
    }

    public void setAacEldAudioSupported(boolean supported) {
        features.set(BIT_AUDIO_FORMAT_1, supported);
        features.set(BIT_AUDIO_FORMAT_4, supported);
    }

    public long toDecimal() {
        return features.toLongArray()[0];
    }

    public String toString() {
        return "0x" + Long.toHexString(toDecimal()).toUpperCase() + ",0x0";
    }
}
