package com.wally.kissmod;

public final class StressTestState {
    private static int rapidIterations;
    private static int packetCount;

    private StressTestState() {
    }

    public static void recordRapidIterations(int iterations) {
        rapidIterations += iterations;
    }

    public static void recordPackets(int packets) {
        packetCount += packets;
    }

    public static String report() {
        return "rapidIterations=" + rapidIterations + ", packetCount=" + packetCount;
    }

    public static void reset() {
        rapidIterations = 0;
        packetCount = 0;
    }
}
