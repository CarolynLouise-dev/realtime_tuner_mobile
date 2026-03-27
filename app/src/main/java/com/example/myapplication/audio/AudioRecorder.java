package com.example.myapplication.audio;

import android.Manifest;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import androidx.annotation.RequiresPermission;
import java.util.concurrent.ArrayBlockingQueue;

public class AudioRecorder {
    private static final int SAMPLE_RATE = 44100;
    // 4096 là con số vàng cho Guitar: Đủ để bắt nốt E2 (82Hz) và đủ nhanh (<100ms)
    private static final int FRAME_SIZE = 4096;

    private AudioRecord audioRecord;
    private boolean isRecording = false;
    private ArrayBlockingQueue<float[]> audioQueue = new ArrayBlockingQueue<>(5);

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public AudioRecorder() {
        int minBufSize = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);

        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT, Math.max(minBufSize, FRAME_SIZE * 4));
    }

    public void start() {
        if (isRecording || audioRecord == null) return;
        audioRecord.startRecording();
        isRecording = true;

        new Thread(() -> {
            short[] buffer = new short[FRAME_SIZE];
            while (isRecording) {
                int read = audioRecord.read(buffer, 0, FRAME_SIZE);
                if (read > 0) {
                    float[] floatBuffer = new float[read];
                    for (int i = 0; i < read; i++) {
                        floatBuffer[i] = buffer[i] / 32768f;
                    }

                    // Nếu Queue đầy (MainActivity xử lý không kịp),
                    // ta xóa bớt cái cũ nhất để nhường chỗ cho cái mới nhất (Real-time)
                    if (audioQueue.size() >= 5) {
                        audioQueue.poll();
                    }
                    audioQueue.offer(floatBuffer);
                }
            }
        }).start();
    }

    public float[] getFrame() {
        return audioQueue.poll();
    }

    public void stop() {
        isRecording = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
        }
    }

    public boolean isRecording() { return isRecording; }
}