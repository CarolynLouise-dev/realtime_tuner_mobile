package com.example.myapplication.core;

public class Yin {

    private final float threshold;

    public Yin(float threshold) {
        this.threshold = threshold;
    }

    public float getPitch(float[] frame, int sampleRate) {
        int N = frame.length;
        int halfN = N / 2;
        float[] diff = new float[halfN];
        float[] cumulative = new float[halfN];
        for (int tau = 1; tau < halfN; tau++) {
            float sum = 0;
            for (int i = 0; i < halfN; i++) {
                float delta = frame[i] - frame[i + tau];
                sum += delta * delta;
            }
            diff[tau] = sum;
        }
        cumulative[0] = 1;
        float runningSum = 0;
        for (int tau = 1; tau < halfN; tau++) {
            runningSum += diff[tau];
            if (runningSum != 0) {
                cumulative[tau] = diff[tau] * tau / runningSum;
            } else {
                cumulative[tau] = 1;
            }
        }

        // Absolute threshold
        int tauEstimate = -1;
        for (int tau = 1; tau < halfN; tau++) {
            if (cumulative[tau] < threshold) {
                tauEstimate = tau;
                break;
            }
        }

        // Nếu không tìm thấy dưới ngưỡng, chọn giá trị nhỏ nhất toàn cục (giúp nhận diện nốt thấp tốt hơn)
        if (tauEstimate == -1) {
            float minVal = Float.MAX_VALUE;
            for (int tau = 1; tau < halfN; tau++) {
                if (cumulative[tau] < minVal) {
                    minVal = cumulative[tau];
                    tauEstimate = tau;
                }
            }
        }

        if (tauEstimate <= 0) return -1f;
        float betterTau = (float) tauEstimate;
        if (tauEstimate > 0 && tauEstimate < halfN - 1) {
            float s0 = cumulative[tauEstimate - 1];
            float s1 = cumulative[tauEstimate];
            float s2 = cumulative[tauEstimate + 1];
            float denominator = 2 * (2 * s1 - s0 - s2);
            if (Math.abs(denominator) > 1e-6) {
                betterTau = tauEstimate + (s2 - s0) / denominator;
            }
        }

        return sampleRate / betterTau;
    }
}
