package com.attvn.adtruevn.ui;

import android.content.Context;
import android.os.CountDownTimer;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.attvn.adtruevn.R;

/**
 * Created by app on 1/6/17.
 */

public class SkipableView extends FrameLayout {

    private final static int DEFAULT_TIME_COUNTER = 4;

    private TextView skipableCounter, guidedText;

    private int mTimeCounter = DEFAULT_TIME_COUNTER;

    private String guidedTextOpen, guidedTextClose;

    public SkipableView(Context context) {
        this(context, null, 0);
    }

    public SkipableView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public SkipableView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(R.layout.skipable_layout_view, this);

        skipableCounter = (TextView) findViewById(R.id.label_skipable);
        guidedText = (TextView) findViewById(R.id.guided_text);

        guidedTextOpen = context.getString(R.string.ads_open_guided);
        guidedTextClose = context.getString(R.string.ads_close_guided);
    }

    public void countTime(boolean isOpen) {
        if(isOpen) {
            guidedText.setText(guidedTextOpen);
        } else {
            guidedText.setText(guidedTextClose);
        }
        CountDownTimer countDownTimer = new CountDownTimer(mTimeCounter*1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                skipableCounter.setText(String.valueOf(--mTimeCounter));
            }

            @Override
            public void onFinish() {
                setVisibility(GONE);
                mTimeCounter = DEFAULT_TIME_COUNTER;
            }
        };
        countDownTimer.start();
    }
}
