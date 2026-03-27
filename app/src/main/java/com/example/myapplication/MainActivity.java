package com.example.myapplication;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.audio.AudioRecorder;
import com.example.myapplication.core.AudioPreprocessor;
import com.example.myapplication.core.NoteMapper;
import com.example.myapplication.core.TunerEngine;
import com.example.myapplication.core.Yin;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    // UI Components
    private TextView txtCurrentNote, txtHz, txtCentsRaw, txtDirectionHint;
    private View pointerWrapper, gaugeContainer, bubbleBg;
    private final List<TextView> noteButtons = new ArrayList<>();

    // Audio Logic
    private AudioRecorder recorder;
    private Yin yin;
    private NoteMapper mapper;
    private AudioPreprocessor preprocessor;
    private String targetNote = "E2";

    // DSP & Smoothing
    private float smoothedCents = 0f;
    private static final float AMPLITUDE_THRESHOLD = 0.015f; // Ngưỡng đủ lớn để bắt nốt
    private static final float SMOOTHING_FACTOR = 0.15f;    // Alpha cho Low-pass filter (0.1 - 0.2)

    // UI Throttling (Chống quá tải CPU)
    private long lastUiUpdateTime = 0;
    private static final long UI_UPDATE_MS = 50; // Cập nhật UI mỗi 50ms (~20 FPS)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupNoteButtons();

        // Xin quyền ghi âm
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 200);
        } else {
            startTuner();
        }
    }

    private void initViews() {
        txtCurrentNote = findViewById(R.id.txt_current_note);
        txtHz = findViewById(R.id.txt_hz);
        txtCentsRaw = findViewById(R.id.txt_cents_raw); // Số trong bong bóng
        txtDirectionHint = findViewById(R.id.txt_direction_hint);
        pointerWrapper = findViewById(R.id.pointer_wrapper);
        gaugeContainer = findViewById(R.id.gauge_container);
        bubbleBg = findViewById(R.id.txt_bubble_cents); // View hình giọt nước
    }

    private void setupNoteButtons() {
        int[] ids = {R.id.btn_e2, R.id.btn_a2, R.id.btn_d3, R.id.btn_g3, R.id.btn_b3, R.id.btn_e4};
        String[] names = {"E2", "A2", "D3", "G3", "B3", "E4"};

        for (int i = 0; i < ids.length; i++) {
            TextView btn = findViewById(ids[i]);
            if (btn == null) continue;
            final String noteName = names[i];
            btn.setOnClickListener(v -> {
                targetNote = noteName;
                updateButtonStyles(btn);
                smoothedCents = 0; // Reset kim khi đổi dây
            });
            noteButtons.add(btn);
            if (noteName.equals(targetNote)) updateButtonStyles(btn);
        }
    }

    private void updateButtonStyles(TextView selected) {
        for (TextView btn : noteButtons) {
            btn.setSelected(false);
            btn.setTextColor(Color.parseColor("#888888"));
        }
        selected.setSelected(true);
        selected.setTextColor(Color.WHITE);
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private void startTuner() {
        recorder = new AudioRecorder();
        yin = new Yin(0.12f); // Ngưỡng Yin (0.1 - 0.15 là chuẩn)
        mapper = new NoteMapper();
        preprocessor = new AudioPreprocessor();
        recorder.start();

        new Thread(() -> {
            while (recorder != null && recorder.isRecording()) {
                float[] rawFrame = recorder.getFrame();
                if (rawFrame == null) continue;

                // 1. Tiền xử lý để làm sạch sóng âm
                float[] processedFrame = preprocessor.preprocess(rawFrame);
                float rms = calculateRMS(rawFrame);

                // 2. Chỉ xử lý khi có tiếng đàn (RMS đủ lớn)
                if (processedFrame != null && rms > AMPLITUDE_THRESHOLD) {
                    float pitch = yin.getPitch(processedFrame, 44100);

                    if (pitch > 40 && pitch < 1000) {
                        // TỰ ĐỘNG NHẬN DIỆN NỐT ĐANG GẢY (Quan trọng nhất!)
                        String detectedNote = mapper.mapFrequencyToNote(pitch);

                        // Nếu bạn đang để chế độ AUTO, hãy cập nhật targetNote theo nốt máy nhận được
                        // targetNote = detectedNote; // <--- Mở dòng này nếu muốn Auto hoàn toàn

                        double targetHz = TunerEngine.getTargetFrequency(targetNote);
                        float rawCents = TunerEngine.calculateCents(pitch, targetHz);

                        // LÀM MƯỢT NHẸ NHÀNG (Tăng factor lên 0.4 để nhạy hơn)
                        smoothedCents = smoothedCents + 0.4f * (rawCents - smoothedCents);

                        // CẬP NHẬT UI
                        long now = System.currentTimeMillis();
                        if (now - lastUiUpdateTime > UI_UPDATE_MS) {
                            runOnUiThread(() -> updateTunerUI(detectedNote, pitch, smoothedCents));
                            lastUiUpdateTime = now;
                        }
                    }
                } else {
                    // Khi im lặng, kim trôi về 0 nhanh hơn
                    smoothedCents *= 0.5f;
                }
            }
        }).start();
    }

    private void updateTunerUI(String note, float pitch, float cents) {
        txtCurrentNote.setText(note);
        txtHz.setText(String.format("%.1f Hz", pitch));
        txtCentsRaw.setText(String.valueOf(Math.abs(Math.round(cents))));

        gaugeContainer.post(() -> {
            float width = gaugeContainer.getWidth();
            if (width > 0) {
                // Tính toán biên độ di chuyển (85% của nửa thanh đo)
                float maxRange = (width / 2f) * 0.85f;
                float constrainedCents = Math.max(-50, Math.min(50, cents));

                // Deadzone: Dưới 1.2 cents thì coi như hoàn hảo
                if (Math.abs(constrainedCents) < 1.2f) constrainedCents = 0;

                float finalX = (constrainedCents / 50f) * maxRange;

                pointerWrapper.animate()
                        .translationX(finalX)
                        .setDuration(UI_UPDATE_MS + 20) // Thời gian trượt khớp với tốc độ lấy mẫu
                        .setInterpolator(new DecelerateInterpolator())
                        .start();
            }
        });

        // Màu sắc và Cảnh báo
        int color;
        if (Math.abs(cents) < 3.0f) {
            color = Color.parseColor("#1ABC9C"); // Green: Chuẩn
            txtDirectionHint.setVisibility(View.INVISIBLE);
            txtCurrentNote.setTextColor(color);
        } else {
            txtCurrentNote.setTextColor(Color.WHITE);
            txtDirectionHint.setVisibility(View.VISIBLE);
            if (cents > 0) {
                color = Color.parseColor("#E74C3C"); // Red: Quá cao
                txtDirectionHint.setText("TUNE DOWN ↓");
            } else {
                color = Color.parseColor("#F1C40F"); // Yellow: Quá thấp
                txtDirectionHint.setText("TUNE UP ↑");
            }
            txtDirectionHint.setTextColor(color);
        }

        if (bubbleBg != null && bubbleBg.getBackground() != null) {
            bubbleBg.getBackground().setTint(color);
        }
    }

    private float calculateRMS(float[] audioData) {
        float sum = 0;
        for (float f : audioData) sum += f * f;
        return (float) Math.sqrt(sum / audioData.length);
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 200 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startTuner();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (recorder != null) recorder.stop();
    }
}