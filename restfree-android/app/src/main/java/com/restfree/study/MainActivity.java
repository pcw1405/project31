package com.restfree.study;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS = "restfree_prefs";
    private static final String KEY_TOTAL = "total_study_time";
    private static final int GOAL = 900;
    private static final int STUDY_MINUTES = 60;
    private static final int AUDIO_DURATION_SECONDS = 30 * 60;
    private static final int SAMPLE_RATE = 16000;

    private int totalStudyTime = 0;
    private TextView totalView;
    private TextView goalView;
    private TextView startTimeView;
    private TextView endTimeView;
    private TextView audioStatusView;
    private TextView audioTimeView;
    private Button playButton;
    private Button pauseButton;
    private SharedPreferences prefs;

    private AudioTrack audioTrack;
    private Thread audioThread;
    private volatile boolean audioThreadRunning = false;
    private volatile boolean audioPlaying = false;
    private long playedSamples = 0;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable uiUpdater = new Runnable() {
        @Override
        public void run() {
            updateAudioTime();
            handler.postDelayed(this, 500);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        totalStudyTime = Math.max(0, prefs.getInt(KEY_TOTAL, 0));

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.rgb(17, 19, 24));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(22), dp(30), dp(22), dp(28));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView title = centered("레스트프리 1H", 30, Color.WHITE);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title, matchWrap(dp(6)));

        TextView subtitle = centered("1시간 공부 시각 + 30분 집중 사운드", 14, Color.rgb(170, 178, 189));
        root.addView(subtitle, matchWrap(dp(18)));

        Button studyStart = new Button(this);
        studyStart.setText("공부 시작 - 1시간");
        studyStart.setTextSize(21);
        studyStart.setAllCaps(false);
        studyStart.setOnClickListener(v -> showStudyTimes());
        LinearLayout.LayoutParams studyParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(68));
        studyParams.setMargins(0, 0, 0, dp(14));
        root.addView(studyStart, studyParams);

        startTimeView = centered("현재 시간: -", 18, Color.WHITE);
        root.addView(startTimeView, matchWrap(dp(5)));

        endTimeView = centered("완료 시간: -", 21, Color.rgb(125, 220, 120));
        endTimeView.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(endTimeView, matchWrap(dp(24)));

        TextView audioTitle = centered("🎵 30분 집중 사운드", 20, Color.WHITE);
        audioTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(audioTitle, matchWrap(dp(4)));

        audioStatusView = centered("준비 완료", 14, Color.rgb(170, 178, 189));
        root.addView(audioStatusView, matchWrap(dp(4)));

        audioTimeView = centered("00:00 / 30:00", 16, Color.rgb(200, 205, 212));
        root.addView(audioTimeView, matchWrap(dp(10)));

        LinearLayout audioControls = new LinearLayout(this);
        audioControls.setOrientation(LinearLayout.HORIZONTAL);
        audioControls.setGravity(Gravity.CENTER);

        playButton = new Button(this);
        playButton.setText("▶ 재생");
        playButton.setTextSize(18);
        playButton.setAllCaps(false);
        playButton.setOnClickListener(v -> playAudio());

        pauseButton = new Button(this);
        pauseButton.setText("⏸ 일시정지");
        pauseButton.setTextSize(18);
        pauseButton.setAllCaps(false);
        pauseButton.setOnClickListener(v -> pauseAudio());

        LinearLayout.LayoutParams audioBtn = new LinearLayout.LayoutParams(0, dp(62), 1f);
        audioBtn.setMargins(dp(5), 0, dp(5), 0);
        audioControls.addView(playButton, audioBtn);
        audioControls.addView(pauseButton, audioBtn);
        root.addView(audioControls, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView note = centered("잔잔한 합성 집중음입니다. 일시정지 후 같은 위치에서 이어집니다.",
                13, Color.rgb(150, 158, 168));
        root.addView(note, matchWrap(dp(18)));

        TextView separator = centered("────────────", 14, Color.rgb(70, 75, 82));
        root.addView(separator, matchWrap(dp(12)));

        TextView label = centered("총 공부시간", 17, Color.rgb(190, 197, 205));
        root.addView(label, matchWrap(dp(3)));

        totalView = centered("0", 54, Color.WHITE);
        totalView.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(totalView, matchWrap(dp(1)));

        TextView unit = centered("초", 16, Color.rgb(170, 178, 189));
        root.addView(unit, matchWrap(dp(14)));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);

        Button minus = new Button(this);
        minus.setText("-1");
        minus.setTextSize(24);
        minus.setAllCaps(false);
        minus.setOnClickListener(v -> {
            totalStudyTime = Math.max(0, totalStudyTime - 1);
            saveAndRender();
        });

        Button plus = new Button(this);
        plus.setText("+1");
        plus.setTextSize(24);
        plus.setAllCaps(false);
        plus.setOnClickListener(v -> {
            totalStudyTime += 1;
            saveAndRender();
        });

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(0, dp(62), 1f);
        buttonParams.setMargins(dp(5), 0, dp(5), 0);
        controls.addView(minus, buttonParams);
        controls.addView(plus, buttonParams);
        root.addView(controls, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        Button reset = new Button(this);
        reset.setText("0으로 초기화");
        reset.setTextSize(15);
        reset.setAllCaps(false);
        reset.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("초기화")
                .setMessage("총 공부시간을 0으로 초기화할까요?")
                .setNegativeButton("취소", null)
                .setPositiveButton("초기화", (dialog, which) -> {
                    totalStudyTime = 0;
                    saveAndRender();
                })
                .show());
        LinearLayout.LayoutParams resetParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(52));
        resetParams.setMargins(0, dp(12), 0, 0);
        root.addView(reset, resetParams);

        goalView = centered("", 13, Color.rgb(170, 178, 189));
        root.addView(goalView, matchWrap(dp(7)));

        TextView info = centered("알림은 사용하지 않습니다. 공부시간은 자동 저장됩니다.",
                13, Color.rgb(150, 158, 168));
        root.addView(info, matchWrap(0));

        setContentView(scrollView);
        saveAndRender();
        prepareAudio();
        handler.post(uiUpdater);
    }

    private TextView centered(String text, int size, int color) {
        TextView v = new TextView(this);
        v.setText(text);
        v.setTextColor(color);
        v.setTextSize(size);
        v.setGravity(Gravity.CENTER);
        return v;
    }

    private void showStudyTimes() {
        Calendar now = Calendar.getInstance();
        Calendar end = (Calendar) now.clone();
        end.add(Calendar.MINUTE, STUDY_MINUTES);

        SimpleDateFormat formatter = new SimpleDateFormat("a h:mm:ss", Locale.KOREA);
        startTimeView.setText("현재 시간: " + formatter.format(now.getTime()));
        endTimeView.setText("완료 시간: " + formatter.format(end.getTime()));
    }

    private void prepareAudio() {
        int minBuffer = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT);
        int bufferSize = Math.max(minBuffer, 4096);

        audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build())
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();

        audioThreadRunning = true;
        audioThread = new Thread(() -> {
            short[] buffer = new short[1024];
            double phase1 = 0.0;
            double phase2 = 0.0;
            long totalSamples = (long) AUDIO_DURATION_SECONDS * SAMPLE_RATE;

            while (audioThreadRunning) {
                if (!audioPlaying) {
                    SystemClock.sleep(20);
                    continue;
                }

                if (playedSamples >= totalSamples) {
                    audioPlaying = false;
                    try { audioTrack.pause(); } catch (Exception ignored) {}
                    runOnUiThread(() -> audioStatusView.setText("재생 완료"));
                    continue;
                }

                int count = (int) Math.min(buffer.length, totalSamples - playedSamples);
                for (int i = 0; i < count; i++) {
                    double slow = Math.sin((playedSamples + i) * 2.0 * Math.PI * 0.08 / SAMPLE_RATE);
                    double s1 = Math.sin(phase1);
                    double s2 = Math.sin(phase2);
                    double sample = (s1 * 0.55 + s2 * 0.25) * (0.55 + 0.12 * slow);

                    phase1 += 2.0 * Math.PI * 174.61 / SAMPLE_RATE;
                    phase2 += 2.0 * Math.PI * 261.63 / SAMPLE_RATE;
                    if (phase1 > Math.PI * 2) phase1 -= Math.PI * 2;
                    if (phase2 > Math.PI * 2) phase2 -= Math.PI * 2;

                    buffer[i] = (short) (sample * 2200);
                }

                int written = audioTrack.write(buffer, 0, count, AudioTrack.WRITE_BLOCKING);
                if (written > 0) playedSamples += written;
            }
        }, "RestFreeAudio");
        audioThread.start();
    }

    private void playAudio() {
        if (audioTrack == null) return;

        long totalSamples = (long) AUDIO_DURATION_SECONDS * SAMPLE_RATE;
        if (playedSamples >= totalSamples) {
            playedSamples = 0;
            audioTrack.flush();
        }

        try {
            audioTrack.play();
            audioPlaying = true;
            audioStatusView.setText("재생 중");
        } catch (Exception e) {
            audioStatusView.setText("재생할 수 없습니다.");
        }
    }

    private void pauseAudio() {
        if (audioTrack == null) return;
        audioPlaying = false;
        try {
            audioTrack.pause();
            audioStatusView.setText("일시정지");
        } catch (Exception ignored) {
        }
        updateAudioTime();
    }

    private void updateAudioTime() {
        long seconds = playedSamples / SAMPLE_RATE;
        if (seconds < 0) seconds = 0;
        if (seconds > AUDIO_DURATION_SECONDS) seconds = AUDIO_DURATION_SECONDS;
        audioTimeView.setText(formatTime(seconds) + " / 30:00");
    }

    private String formatTime(long seconds) {
        long minutes = seconds / 60;
        long remain = seconds % 60;
        return String.format(Locale.KOREA, "%02d:%02d", minutes, remain);
    }

    private void saveAndRender() {
        prefs.edit().putInt(KEY_TOTAL, totalStudyTime).apply();
        totalView.setText(String.valueOf(totalStudyTime));
        goalView.setText("목표 " + GOAL + "초 중 " + totalStudyTime + "초");
    }

    private LinearLayout.LayoutParams matchWrap(int bottomMargin) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        p.setMargins(0, 0, 0, bottomMargin);
        return p;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacks(uiUpdater);
        audioPlaying = false;
        audioThreadRunning = false;

        if (audioTrack != null) {
            try {
                audioTrack.pause();
                audioTrack.flush();
                audioTrack.release();
            } catch (Exception ignored) {
            }
            audioTrack = null;
        }

        super.onDestroy();
    }
}
