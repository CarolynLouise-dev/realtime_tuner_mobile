package com.example.myapplication.audio;

import android.Manifest;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.annotation.RequiresPermission;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class AudioRecorder {
    private static final int SAMPLE_RATE = 44100;
    private static final int FRAME_SIZE = 8192;
    private static final String TAG = "AudioRecorder";

    private AudioRecord audioRecord;
    private int bufferSize;
    private boolean isRecording = false;
    private BlockingQueue<float[]> audioQueue = new LinkedBlockingQueue<>();

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    public AudioRecorder() {
        try {
            bufferSize = Math.max(AudioRecord.getMinBufferSize(SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT), FRAME_SIZE * 2);

            audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                    SAMPLE_RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize);

            if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord not initialized, device may not support mic!");
            }

        } catch (Exception e) {
            Log.e(TAG, "Error initializing AudioRecord", e);
        }
    }

    public void start() {
        if (isRecording || audioRecord == null) return;

        try {
            audioRecord.startRecording();
        } catch (Exception e) {
            Log.e(TAG, "Failed to start recording", e);
            return;
        }

        isRecording = true;

        new Thread(() -> {
            short[] buffer = new short[FRAME_SIZE];
            while (isRecording) {
                try {
                    int read = audioRecord.read(buffer, 0, buffer.length);
                    if (read <= 0) {
                        Log.w(TAG, "No audio captured! Microphone may not be working.");
                        continue;
                    }
                    else if (read > 0) {
                        float[] floatBuffer = new float[read];
                        for (int i = 0; i < read; i++) floatBuffer[i] = buffer[i] / 32768f;
                        audioQueue.offer(floatBuffer);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error reading audio", e);
                }
            }
        }).start();
    }

    public void stop() {
        isRecording = false;
        if (audioRecord != null) {
            try {
                audioRecord.stop();
                audioRecord.release();
            } catch (Exception e) {
                Log.e(TAG, "Error stopping AudioRecord", e);
            }
        }
    }

    public float[] getFrame() {
        return audioQueue.poll();
    }

    public boolean isRecording() {
        return isRecording;
    }
}