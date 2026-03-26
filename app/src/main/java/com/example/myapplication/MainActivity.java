package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.audio.AudioRecorder;
import com.example.myapplication.core.NoteMapper;
import com.example.myapplication.core.TunerEngine;
import com.example.myapplication.core.Yin;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView txtCurrentNote, txtHz, txtCentsRaw, txtDirectionHint;
    private View pointerWrapper, gaugeContainer, bubbleBg;

    private AudioRecorder recorder;
    private Yin yin;
    private NoteMapper mapper;

    private String targetNote = "E2";
    private final List<TextView> noteButtons = new ArrayList<>();

    private float smoothedCents = 0f;
    private String detectedNoteName = "--";

    private static final float AMPLITUDE_THRESHOLD = 0.008f;
    private static final float FILTER_COEFFICIENT = 0.15f;
    private long lastUpdateTime = 0;
    private static final long UI_REFRESH_RATE = 33; // 30 FPS

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // PHẢI CÓ DÒNG NÀY ĐỂ HIỂN THỊ GIAO DIỆN
        setContentView(R.layout.activity_main);

        // Ánh xạ View
        txtCurrentNote = findViewById(R.id.txt_current_note);
        txtHz = findViewById(R.id.txt_hz);
        txtCentsRaw = findViewById(R.id.txt_cents_raw);
        txtDirectionHint = findViewById(R.id.txt_direction_hint);
        pointerWrapper = findViewById(R.id.pointer_wrapper);
        gaugeContainer = findViewById(R.id.gauge_container);
        bubbleBg = findViewById(R.id.txt_bubble_cents);

        setupNoteButtons();

        // Kiểm tra quyền ghi âm
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 200);
        } else {
            startTuner();
        }
    }

    private void setupNoteButtons() {
        int[] ids = {R.id.btn_e2, R.id.btn_a2, R.id.btn_d3, R.id.btn_g3, R.id.btn_b3, R.id.btn_e4};
        String[] noteNames = {"E2", "A2", "D3", "G3", "B3", "E4"};

        for (int i = 0; i < ids.length; i++) {
            TextView btn = findViewById(ids[i]);
            if (btn == null) continue;
            final String name = noteNames[i];
            btn.setOnClickListener(v -> {
                targetNote = name;
                updateButtonStyles(btn);
                smoothedCents = 0;
            });
            noteButtons.add(btn);
            if (name.equals(targetNote)) updateButtonStyles(btn);
        }
    }

    private void updateButtonStyles(TextView selected) {
        for (TextView btn : noteButtons) {
            btn.setSelected(false);
            btn.setTextColor(Color.GRAY);
        }
        selected.setSelected(true);
        selected.setTextColor(Color.WHITE);
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private void startTuner() {
        recorder = new AudioRecorder();
        yin = new Yin(0.15f);
        mapper = new NoteMapper();
        recorder.start();

        new Thread(() -> {
            while (recorder != null && recorder.isRecording()) {
                float[] frame = recorder.getFrame();
                if (frame == null) continue;

                float rms = calculateRMS(frame);
                float pitch = yin.getPitch(frame, 44100);

                if (pitch > 40 && pitch < 1000 && rms > AMPLITUDE_THRESHOLD) {
                    double targetHz = TunerEngine.getTargetFrequency(targetNote);
                    float rawCents = TunerEngine.calculateCents(pitch, targetHz);

                    if (Math.abs(rawCents - smoothedCents) < 40) {
                        smoothedCents = smoothedCents + FILTER_COEFFICIENT * (rawCents - smoothedCents);
                    }

                    detectedNoteName = mapper.mapFrequencyToNote(pitch);

                    long now = System.currentTimeMillis();
                    if (now - lastUpdateTime > UI_REFRESH_RATE) {
                        runOnUiThread(() -> updateTunerUI(detectedNoteName, pitch, smoothedCents));
                        lastUpdateTime = now;
                    }
                } else if (rms < AMPLITUDE_THRESHOLD * 0.5f) {
                    smoothedCents *= 0.8f;
                }
            }
        }).start();
    }

    private void updateTunerUI(String note, float pitch, float cents) {
        // 1. Cập nhật tên nốt và tần số
        txtCurrentNote.setText(note);
        txtHz.setText(String.format("%.1f Hz", pitch));

        // 2. Hiển thị độ lệch Cents (Cái số trong bong bóng)
        // Dùng Math.abs để hiện số dương cho dễ nhìn, hoặc để nguyên nếu muốn hiện dấu -
        int roundedCents = Math.round(cents);

        // Đảm bảo bạn đã ánh xạ txtBubbleNumber trong onCreate
        // Nếu trong XML bạn dùng txtCentsRaw làm số trong bong bóng thì dùng nó
        txtCentsRaw.setText(String.valueOf(Math.abs(roundedCents)));

        // 3. Logic Vùng Chết (Dead Zone) để kim đứng yên khi đã chuẩn
        float pointerCents = cents;
        if (Math.abs(cents) < 1.2f) {
            pointerCents = 0;
        }

        // 4. Di chuyển kim (Pointer)
        float containerWidth = gaugeContainer.getWidth();
        if (containerWidth > 0) {
            float maxRange = (containerWidth / 2f) - 60;
            float targetX = (pointerCents / 50f) * maxRange;

            pointerWrapper.animate()
                    .translationX(targetX)
                    .setDuration(40) // Giảm xuống 40ms để phản hồi nhanh hơn
                    .setInterpolator(new android.view.animation.LinearInterpolator())
                    .start();
        }

        // 5. Cập nhật màu sắc dựa trên độ lệch
        int color;
        if (Math.abs(cents) < 3.0f) {
            color = Color.parseColor("#2ECC71"); // Xanh lá: Chuẩn rồi!
            txtDirectionHint.setVisibility(View.INVISIBLE);
            txtCurrentNote.setTextColor(color);
        } else {
            txtCurrentNote.setTextColor(Color.WHITE);
            txtDirectionHint.setVisibility(View.VISIBLE);
            if (cents > 0) {
                color = Color.parseColor("#E74C3C"); // Đỏ: Quá cao (vặn lỏng ra)
                txtDirectionHint.setText("TUNE DOWN ↓");
            } else {
                color = Color.parseColor("#F1C40F"); // Vàng: Quá thấp (vặn chặt vào)
                txtDirectionHint.setText("TUNE UP ↑");
            }
            txtDirectionHint.setTextColor(color);
        }

        // Đổi màu nền của bong bóng
        if (bubbleBg != null && bubbleBg.getBackground() != null) {
            bubbleBg.getBackground().setTint(color);
        }
    }

    private float calculatePointerX(float cents) {
        float halfWidth = gaugeContainer.getWidth() / 2f;
        float constrainedCents = Math.max(-50, Math.min(50, cents));
        return (constrainedCents / 50f) * (halfWidth - 40);
    }

    private float calculateRMS(float[] audioData) {
        if (audioData == null || audioData.length == 0) return 0;
        float sum = 0;
        for (float f : audioData) sum += f * f;
        return (float) Math.sqrt(sum / audioData.length);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recorder != null) recorder.stop();
    }
}