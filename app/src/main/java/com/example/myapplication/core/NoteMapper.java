package com.example.myapplication.core;

public class NoteMapper {

    private static final String[] NOTE_NAMES = {
            "C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B"
    };

    private static final float REFERENCE_A4 = 440.0f;

    public NoteMapper() {
    }

    public String mapFrequencyToNote(float freq) {
        if (freq < 20.0f || freq > 3000f) return "";

        double midiNote = 12 * (Math.log(freq / REFERENCE_A4) / Math.log(2)) + 69;
        int roundedMidiNote = (int) Math.round(midiNote);

        int noteIndex = roundedMidiNote % 12;
        int octave = (roundedMidiNote / 12) - 1;

        if (noteIndex < 0) noteIndex += 12;

        if (octave < 0 || octave > 8) return "";

        return NOTE_NAMES[noteIndex] + octave;
    }

    public float getFrequencyForNote(String noteName) {
        if (noteName == null || noteName.isEmpty()) return 0f;

        try {
            String namePart;
            int octavePart;

            if (noteName.contains("#")) {
                namePart = noteName.substring(0, 2);
                octavePart = Integer.parseInt(noteName.substring(2));
            } else {
                namePart = noteName.substring(0, 1);
                octavePart = Integer.parseInt(noteName.substring(1));
            }

            int noteIndex = -1;
            for (int i = 0; i < NOTE_NAMES.length; i++) {
                if (NOTE_NAMES[i].equalsIgnoreCase(namePart)) {
                    noteIndex = i;
                    break;
                }
            }

            if (noteIndex == -1) return 0f;

            int midiNote = (octavePart + 1) * 12 + noteIndex;
            return (float) (REFERENCE_A4 * Math.pow(2, (midiNote - 69) / 12.0));

        } catch (Exception e) {
            return 0f;
        }
    }
}
