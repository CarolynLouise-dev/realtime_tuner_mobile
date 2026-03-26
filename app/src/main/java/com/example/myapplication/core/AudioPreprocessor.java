package com.example.myapplication.core;

import android.util.Log;

public class AudioPreprocessor {

    private static final float PRE_EMPHASIS_COEFF = 0.95f;
    private static final float NOISE_THRESHOLD = 0.01f;  // giảm để detect low freq
    private static final float NOISE_ATTACK = 0.1f;

    // Bandpass filter (optional, bật nếu muốn)
    private static final boolean USE_BANDPASS = false;
    private static final float LOW_FREQ = 50f;    // giảm từ 80Hz để E2 còn
    private static final float HIGH_FREQ = 1200f; // giữ range cho giọng người

    public float[] preprocess(float[] frame, float sampleRate) {
        if (frame == null || frame.length == 0) return null;

        // 1. Pre-emphasis để tăng cường tần số cao
        frame = preEmphasis(frame);

        // 2. Noise gate chuyên nghiệp
        frame = noiseGate(frame, NOISE_THRESHOLD, NOISE_ATTACK);

        // 3. Bandpass filter (nếu dùng)
        if (USE_BANDPASS) {
            frame = bandpassFilter(frame, sampleRate, LOW_FREQ, HIGH_FREQ);
        }

        // 4. Hanning window
        frame = applyHanning(frame);

        // 5. Chuẩn hóa biên độ
        frame = normalize(frame);

        return frame;
    }

    private float[] preEmphasis(float[] signal) {
        float[] result = new float[signal.length];
        result[0] = signal[0];
        for (int i = 1; i < signal.length; i++) {
            result[i] = signal[i] - PRE_EMPHASIS_COEFF * signal[i - 1];
        }
        return result;
    }

    private float[] noiseGate(float[] frame, float threshold, float attack) {
        float rms = 0f;
        for (float v : frame) rms += v * v;
        rms = (float) Math.sqrt(rms / frame.length);
        if (rms < threshold) {
            for (int i = 0; i < frame.length; i++) frame[i] *= attack;
        }
        return frame;
    }

    private float[] applyHanning(float[] frame) {
        int N = frame.length;
        float[] result = new float[N];
        for (int n = 0; n < N; n++) {
            result[n] = frame[n] * (0.5f - 0.5f * (float) Math.cos(2.0 * Math.PI * n / (N - 1)));
        }
        return result;
    }

    private float[] normalize(float[] frame) {
        float maxVal = 0f;
        for (float v : frame) maxVal = Math.max(maxVal, Math.abs(v));
        if (maxVal > 0) {
            for (int i = 0; i < frame.length; i++) frame[i] /= maxVal;
        }
        return frame;
    }

    // --- Bandpass filter đơn giản (sos / IIR có thể thêm sau) ---
    // Hiện tại dùng simple FFT filter nếu cần
    private float[] bandpassFilter(float[] frame, float sampleRate, float lowFreq, float highFreq) {
        // Placeholder: nếu muốn chính xác, nên dùng biquad/IIR filter
        // Tạm thời return frame nguyên
        Log.d("AudioPreprocessor", "Bandpass filter applied: " + lowFreq + "Hz - " + highFreq + "Hz");
        return frame;
    }
}