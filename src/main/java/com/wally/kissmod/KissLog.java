package com.wally.kissmod;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public final class KissLog {
    public static final Marker DETECTION = MarkerFactory.getMarker("KissCraft.Detection");
    public static final Marker NETWORK = MarkerFactory.getMarker("KissCraft.Networking");
    public static final Marker ANIMATION = MarkerFactory.getMarker("KissCraft.Animation");
    public static final Marker CAPABILITY = MarkerFactory.getMarker("KissCraft.Capability");
    public static final Marker CONFIG = MarkerFactory.getMarker("KissCraft.Config");
    public static final Marker DEBUG = MarkerFactory.getMarker("KissCraft.Debug");

    private KissLog() {
    }

    public static void detection(String message, Object... args) {
        if (Config.DEBUG_MODE.get() || Boolean.getBoolean("kissmod.log.detection")) {
            kissmod.LOGGER.info(DETECTION, "[KissCraft][Detection] " + message, args);
        }
    }

    public static void networking(String message, Object... args) {
        if (Config.DEBUG_MODE.get() || Boolean.getBoolean("kissmod.log.networking")) {
            kissmod.LOGGER.info(NETWORK, "[KissCraft][Networking] " + message, args);
        }
    }

    public static void animation(String message, Object... args) {
        if (Config.DEBUG_MODE.get() || Boolean.getBoolean("kissmod.log.animation")) {
            kissmod.LOGGER.info(ANIMATION, "[KissCraft][Animation] " + message, args);
        }
    }

    public static void capability(String message, Object... args) {
        if (Config.DEBUG_MODE.get() || Boolean.getBoolean("kissmod.log.capability")) {
            kissmod.LOGGER.info(CAPABILITY, "[KissCraft][Capability] " + message, args);
        }
    }

    public static void config(String message, Object... args) {
        kissmod.LOGGER.info(CONFIG, "[KissCraft][Config] " + message, args);
    }

    public static void debug(String message, Object... args) {
        if (Config.DEBUG_MODE.get()) {
            kissmod.LOGGER.info(DEBUG, "[KissCraft][Debug] " + message, args);
        }
    }
}
