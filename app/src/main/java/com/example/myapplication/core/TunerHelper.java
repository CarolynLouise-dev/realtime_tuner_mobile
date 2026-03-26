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

    public static float getCentsDifference(float pitch, float targetFreq) {
        if (pitch <= 0 || targetFreq <= 0) return 0f;
        return (float) (1200 * (Math.log(pitch / targetFreq) / Math.log(2)));
    }

    public static String getStatusFromCents(float cents) {
        if (cents > 5) {
            return "tone down";
        } else if (cents < -5) {
            return "tone up";
        } else {
            return "in tune";
        }
    }

    public static PitchResult comparePitch(float pitch, String targetNote, NoteMapper mapper) {
        float targetFreq = mapper.getFrequencyForNote(targetNote);
        float cents = getCentsDifference(pitch, targetFreq);
        String status = getStatusFromCents(cents);
        return new PitchResult(cents, status);
    }
}
