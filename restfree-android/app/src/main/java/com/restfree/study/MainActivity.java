package com.restfree.study;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String PREFS = "restfree_prefs";
    private static final String KEY_TOTAL = "total_study_time";
    private static final int GOAL = 900;

    private int totalStudyTime = 0;
    private TextView totalView;
    private TextView goalView;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        totalStudyTime = Math.max(0, prefs.getInt(KEY_TOTAL, 0));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(40), dp(24), dp(24));
        root.setBackgroundColor(Color.rgb(17, 19, 24));

        TextView title = new TextView(this);
        title.setText("레스트프리");
        title.setTextColor(Color.WHITE);
        title.setTextSize(30);
        title.setGravity(Gravity.CENTER);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(title, matchWrap(dp(12)));

        TextView subtitle = new TextView(this);
        subtitle.setText("모바일 공부시간 카운터");
        subtitle.setTextColor(Color.rgb(170, 178, 189));
        subtitle.setTextSize(16);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle, matchWrap(dp(28)));

        TextView label = new TextView(this);
        label.setText("총 공부시간");
        label.setTextColor(Color.rgb(190, 197, 205));
        label.setTextSize(18);
        label.setGravity(Gravity.CENTER);
        root.addView(label, matchWrap(dp(8)));

        totalView = new TextView(this);
        totalView.setTextColor(Color.WHITE);
        totalView.setTextSize(72);
        totalView.setGravity(Gravity.CENTER);
        totalView.setTypeface(null, android.graphics.Typeface.BOLD);
        root.addView(totalView, matchWrap(dp(4)));

        TextView unit = new TextView(this);
        unit.setText("초");
        unit.setTextColor(Color.rgb(170, 178, 189));
        unit.setTextSize(18);
        unit.setGravity(Gravity.CENTER);
        root.addView(unit, matchWrap(dp(26)));

        LinearLayout controls = new LinearLayout(this);
        controls.setOrientation(LinearLayout.HORIZONTAL);
        controls.setGravity(Gravity.CENTER);

        Button minus = new Button(this);
        minus.setText("-1");
        minus.setTextSize(26);
        minus.setAllCaps(false);
        minus.setOnClickListener(v -> {
            totalStudyTime = Math.max(0, totalStudyTime - 1);
            saveAndRender();
        });

        Button plus = new Button(this);
        plus.setText("+1");
        plus.setTextSize(26);
        plus.setAllCaps(false);
        plus.setOnClickListener(v -> {
            totalStudyTime += 1;
            saveAndRender();
        });

        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(0, dp(72), 1f);
        buttonParams.setMargins(dp(6), 0, dp(6), 0);
        controls.addView(minus, buttonParams);
        controls.addView(plus, buttonParams);
        root.addView(controls, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        Button reset = new Button(this);
        reset.setText("0으로 초기화");
        reset.setTextSize(17);
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
                ViewGroup.LayoutParams.MATCH_PARENT, dp(60));
        resetParams.setMargins(0, dp(16), 0, 0);
        root.addView(reset, resetParams);

        goalView = new TextView(this);
        goalView.setTextColor(Color.rgb(170, 178, 189));
        goalView.setTextSize(15);
        goalView.setGravity(Gravity.CENTER);
        root.addView(goalView, matchWrap(dp(8)));

        TextView info = new TextView(this);
        info.setText("앱을 닫았다 다시 열어도 시간이 자동 저장됩니다.\n인터넷 연결 없이 사용할 수 있습니다.");
        info.setTextColor(Color.rgb(150, 158, 168));
        info.setTextSize(14);
        info.setGravity(Gravity.CENTER);
        root.addView(info, matchWrap(0));

        setContentView(root);
        saveAndRender();
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
}
