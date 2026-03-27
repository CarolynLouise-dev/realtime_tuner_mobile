package com.example.myapplication.core;

public class TunerHelper {

    public static class PitchResult {
        public float cents;
        public String status;

        public PitchResult(float cents, String status) {
            this.cents = cents;
            this.status = status;
        }
    }

    /**
     * Tính độ lệch Cents.
     * Lưu ý: Sử dụng Math.log10 hoặc hằng số log(2) để tối ưu hiệu năng.
     */
    public static float getCentsDifference(float pitch, float targetFreq) {
        if (pitch <= 0 || targetFreq <= 0) return 0f;
        // Công thức chuẩn: 1200 * log2(f1 / f2)
        return (float) (1200 * (Math.log(pitch / targetFreq) / 0.69314718056));
    }

    /**
     * Trả về trạng thái dựa trên độ lệch.
     * GuitarTuna thường để vùng "In Tune" cực hẹp để tạo độ chuyên nghiệp.
     */
    public static String getStatusFromCents(float cents) {
        // Vùng chuẩn (In Tune): Thường là +/- 2.5 đến 3 cents
        if (Math.abs(cents) <= 3.0f) {
            return "in tune";
        } else if (cents > 0) {
            return "tone down"; // Cao quá -> vặn lỏng dây
        } else {
            return "tone up";   // Thấp quá -> vặn chặt dây
        }
    }

    public static PitchResult comparePitch(float pitch, String targetNote) {
        // Tần số mục tiêu lấy từ TunerEngine hoặc NoteMapper của bạn
        float targetFreq = (float) TunerEngine.getTargetFrequency(targetNote);
        float cents = getCentsDifference(pitch, targetFreq);

        // Giới hạn cents trong khoảng -50 đến 50 để tránh kim đo bay mất khỏi thước
        float constrainedCents = Math.max(-50f, Math.min(50f, cents));

        return new PitchResult(constrainedCents, getStatusFromCents(cents));
    }
}