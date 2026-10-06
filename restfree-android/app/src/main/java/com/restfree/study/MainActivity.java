package com.restfree.study;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String PREFS = "restfree_prefs";
    private static final String KEY_TOTAL = "total_study_time";
    private static final int GOAL = 900;
    private static final int STUDY_MINUTES = 60;
    private static final String AUDIO_ASSET = "testcase29.m4a";

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

    private MediaPlayer mediaPlayer;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable progressUpdater = new Runnable() {
        @Override
        public void run() {
            updateAudioTime();
            if (mediaPlayer != null) {
                handler.postDelayed(this, 500);
            }
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

        TextView title = new TextView(this);
        title.setText("레스트프리 1H");
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title, matchWrap(dp(6)));

        TextView subtitle = new TextView(this);
        subtitle.setText("공부 시작 시간 + 1시간 완료 시간 · 33분 오디오");
        subtitle.setTextColor(Color.rgb(170, 178, 189));
        subtitle.setTextSize(14);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, matchWrap(dp(18)));

        Button studyStart = new Button(this);
        studyStart.setText("공부 시작 - 1시간");
        studyStart.setTextSize(21);
        studyStart.setAllCaps(false);
        studyStart.setOnClickListener(v -> showStudyTimes());
        LinearLayout.LayoutParams studyButtonParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(68));
        studyButtonParams.setMargins(0, 0, 0, dp(14));
        root.addView(studyStart, studyButtonParams);

        startTimeView = makeCenteredText("현재 시간: -", 18, Color.WHITE);
        root.addView(startTimeView, matchWrap(dp(5)));

        endTimeView = makeCenteredText("완료 시간: -", 21, Color.rgb(125, 220, 120));
        endTimeView.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(endTimeView, matchWrap(dp(24)));

        TextView audioTitle = makeCenteredText("🎵 testcase29.m4a", 20, Color.WHITE);
        audioTitle.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(audioTitle, matchWrap(dp(4)));

        audioStatusView = makeCenteredText("오디오 준비 중...", 14, Color.rgb(170, 178, 189));
        root.addView(audioStatusView, matchWrap(dp(4)));

        audioTimeView = makeCenteredText("00:00 / 33:23", 16, Color.rgb(200, 205, 212));
        root.addView(audioTimeView, matchWrap(dp(10)));

        LinearLayout audioControls = new LinearLayout(this);
        audioControls.setOrientation(LinearLayout.HORIZONTAL);
        audioControls.setGravity(Gravity.CENTER);

        playButton = new Button(this);
        playButton.setText("▶ 재생");
        playButton.setTextSize(18);
        playButton.setAllCaps(false);
        playButton.setEnabled(false);
        playButton.setOnClickListener(v -> playAudio());

        pauseButton = new Button(this);
        pauseButton.setText("⏸ 일시정지");
        pauseButton.setTextSize(18);
        pauseButton.setAllCaps(false);
        pauseButton.setEnabled(false);
        pauseButton.setOnClickListener(v -> pauseAudio());

        LinearLayout.LayoutParams audioBtn = new LinearLayout.LayoutParams(0, dp(62), 1f);
        audioBtn.setMargins(dp(5), 0, dp(5), 0);
        audioControls.addView(playButton, audioBtn);
        audioControls.addView(pauseButton, audioBtn);
        root.addView(audioControls, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView separator = makeCenteredText("────────────", 14, Color.rgb(70, 75, 82));
        root.addView(separator, matchWrap(dp(12)));

        TextView label = makeCenteredText("총 공부시간", 17, Color.rgb(190, 197, 205));
        root.addView(label, matchWrap(dp(3)));

        totalView = makeCenteredText("0", 54, Color.WHITE);
        totalView.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(totalView, matchWrap(dp(1)));

        TextView unit = makeCenteredText("초", 16, Color.rgb(170, 178, 189));
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

        goalView = makeCenteredText("", 13, Color.rgb(170, 178, 189));
        root.addView(goalView, matchWrap(dp(7)));

        TextView info = makeCenteredText(
                "알림은 사용하지 않습니다.\n오디오는 일시정지한 위치에서 다시 이어서 재생됩니다.",
                13, Color.rgb(150, 158, 168));
        root.addView(info, matchWrap(0));

        setContentView(scrollView);
        saveAndRender();
        prepareAudio();
    }

    private TextView makeCenteredText(String text, int size, int color) {
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
        try {
            File audioFile = new File(getFilesDir(), AUDIO_ASSET);
            if (!audioFile.exists() || audioFile.length() == 0) {
                try (InputStream in = getAssets().open(AUDIO_ASSET);
                     FileOutputStream out = new FileOutputStream(audioFile)) {
                    byte[] buffer = new byte[64 * 1024];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                    }
                }
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(audioFile.getAbsolutePath());
            mediaPlayer.setOnPreparedListener(mp -> {
                audioStatusView.setText("준비 완료 · 약 " + formatTime(mp.getDuration()));
                playButton.setEnabled(true);
                pauseButton.setEnabled(true);
                updateAudioTime();
                handler.post(progressUpdater);
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                audioStatusView.setText("재생 완료");
                mp.seekTo(0);
                updateAudioTime();
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            audioStatusView.setText("오디오를 불러오지 못했습니다.");
            playButton.setEnabled(false);
            pauseButton.setEnabled(false);
        }
    }

    private void playAudio() {
        if (mediaPlayer != null && !mediaPlayer.isPlaying()) {
            mediaPlayer.start();
            audioStatusView.setText("재생 중");
        }
    }

    private void pauseAudio() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            audioStatusView.setText("일시정지");
            updateAudioTime();
        }
    }

    private void updateAudioTime() {
        if (mediaPlayer == null) return;
        try {
            audioTimeView.setText(formatTime(mediaPlayer.getCurrentPosition())
                    + " / " + formatTime(mediaPlayer.getDuration()));
        } catch (IllegalStateException ignored) {
        }
    }

    private String formatTime(int millis) {
        if (millis < 0) millis = 0;
        int totalSeconds = millis / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.KOREA, "%02d:%02d", minutes, seconds);
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
        handler.removeCallbacks(progressUpdater);
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        super.onDestroy();
    }
}
