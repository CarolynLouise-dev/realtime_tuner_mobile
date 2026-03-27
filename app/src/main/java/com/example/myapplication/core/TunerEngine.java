package com.example.myapplication.core;

public class TunerEngine {
    public static double getTargetFrequency(String noteName) {
        switch (noteName) {
            case "E2": return 82.41;
            case "A2": return 110.00;
            case "D3": return 146.83;
            case "G3": return 196.00;
            case "B3": return 246.94;
            case "E4": return 329.63;
            default: return 440.0;
        }
    }

    // Công thức tính Cents chuẩn: 1200 * log2(f_detected / f_target)
    public static float calculateCents(float currentHz, double targetHz) {
        if (currentHz <= 0 || targetHz <= 0) return 0;
        return (float) (1200 * Math.log(currentHz / targetHz) / Math.log(2));
    }
}