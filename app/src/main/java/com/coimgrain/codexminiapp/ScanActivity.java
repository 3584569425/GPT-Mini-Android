package com.coimgrain.codexminiapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeCallback;
import com.journeyapps.barcodescanner.BarcodeResult;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;

import java.util.Collections;

public class ScanActivity extends Activity {
    public static final String EXTRA_SCAN_RESULT = "scan_result";

    private DecoratedBarcodeView barcodeView;
    private boolean completed;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            buildScannerUi();
            barcodeView.decodeContinuous(callback);
        } catch (Exception error) {
            Toast.makeText(this, R.string.scan_failed, Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void buildScannerUi() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        barcodeView = new DecoratedBarcodeView(this);
        barcodeView.setStatusText("");
        barcodeView.getBarcodeView().setDecoderFactory(new DefaultDecoderFactory(
                Collections.singletonList(BarcodeFormat.QR_CODE)
        ));
        root.addView(barcodeView, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        ));

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(dp(18), 0, dp(18), 0);
        topBar.setBackground(barBackground());
        FrameLayout.LayoutParams topParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(58),
                Gravity.TOP
        );
        topParams.setMargins(0, dp(22), 0, 0);
        root.addView(topBar, topParams);

        TextView back = new TextView(this);
        back.setText("‹");
        back.setTextSize(42);
        back.setTextColor(Color.rgb(136, 154, 190));
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(view -> finish());
        topBar.addView(back, new LinearLayout.LayoutParams(dp(52), LinearLayout.LayoutParams.MATCH_PARENT));

        TextView prompt = new TextView(this);
        prompt.setText(R.string.scan_prompt_top);
        prompt.setTextSize(18);
        prompt.setTextColor(Color.rgb(196, 205, 224));
        prompt.setGravity(Gravity.CENTER);
        topBar.addView(prompt, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f));

        TextView bottom = new TextView(this);
        bottom.setText(R.string.scan_prompt_bottom);
        bottom.setTextSize(18);
        bottom.setTextColor(Color.WHITE);
        bottom.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams bottomParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                dp(72),
                Gravity.BOTTOM
        );
        bottomParams.setMargins(dp(16), 0, dp(16), dp(24));
        root.addView(bottom, bottomParams);
    }

    private final BarcodeCallback callback = new BarcodeCallback() {
        @Override
        public void barcodeResult(BarcodeResult result) {
            if (completed || result == null || result.getText() == null || result.getText().trim().isEmpty()) return;
            completed = true;
            Intent data = new Intent();
            data.putExtra(EXTRA_SCAN_RESULT, result.getText());
            setResult(RESULT_OK, data);
            finish();
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        if (barcodeView != null) barcodeView.resume();
    }

    @Override
    protected void onPause() {
        if (barcodeView != null) barcodeView.pause();
        super.onPause();
    }

    private GradientDrawable barBackground() {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(Color.argb(218, 2, 16, 47));
        drawable.setCornerRadius(dp(18));
        drawable.setStroke(dp(1), Color.argb(145, 64, 118, 190));
        return drawable;
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }
}
