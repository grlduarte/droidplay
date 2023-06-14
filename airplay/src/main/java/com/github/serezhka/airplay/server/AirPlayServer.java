package com.github.serezhka.airplay.server;

import android.content.Context;

import com.github.serezhka.airplay.lib.AirPlayBonjour;
import com.github.serezhka.airplay.server.internal.ControlServer;

public class AirPlayServer {

    private final AirPlayBonjour airPlayBonjour;
    private final ControlServer controlServer;

    public AirPlayServer(
            Context context, AirPlayConfig airPlayConfig, AirPlayConsumer airPlayConsumer) {
        AirPlayFeatures features = AirPlayFeatures.getDefaults();
        airPlayBonjour = new AirPlayBonjour(context, airPlayConfig);
        controlServer = new ControlServer(airPlayConfig, airPlayConsumer);
    }

    public void start() throws Exception {
        controlServer.start();
        airPlayBonjour.start(controlServer.getPort());
    }

    public void stop() {
        airPlayBonjour.stop();
        controlServer.stop();
    }
}
