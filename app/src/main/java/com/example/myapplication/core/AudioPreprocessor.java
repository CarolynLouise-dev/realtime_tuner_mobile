package com.example.myapplication.core;

public class AudioPreprocessor {
    private static final float NOISE_THRESHOLD = 0.015f; // Ngưỡng đủ để loại bỏ tiếng xì nền

    public float[] preprocess(float[] frame) {
        if (frame == null || frame.length == 0) return null;

        // 1. Kiểm tra năng lượng (Noise Gate thực thụ)
        float rms = 0f;
        for (float v : frame) rms += v * v;
        rms = (float) Math.sqrt(rms / frame.length);

        if (rms < NOISE_THRESHOLD) {
            return null; // Trả về null để MainActivity biết là im lặng, không cần tính Yin
        }
        // 2. Normalize (Cực kỳ quan trọng cho Yin)
        // Giúp app bắt nốt cực nhạy dù bạn gảy rất khẽ
        float[] normalizedFrame = normalize(frame);
        // 3. Loại bỏ thành phần DC (DC Offset Removal)
        // Đảm bảo sóng âm dao động cân bằng quanh trục 0, giúp Yin chuẩn hơn
        return removeDCLine(normalizedFrame);
    }

    private float[] normalize(float[] frame) {
        float maxVal = 0f;
        for (float v : frame) maxVal = Math.max(maxVal, Math.abs(v));
        if (maxVal > 0.0001f) {
            float[] result = new float[frame.length];
            for (int i = 0; i < frame.length; i++) result[i] = frame[i] / maxVal;
            return result;
        }
        return frame;
    }

    private float[] removeDCLine(float[] frame) {
        float sum = 0;
        for (float v : frame) sum += v;
        float avg = sum / frame.length;
        for (int i = 0; i < frame.length; i++) frame[i] -= avg;
        return frame;
    }
}