package com.attvn.adtruevn.ui;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.attvn.adtruevn.R;
import com.attvn.adtruevn.util.Logging;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;

/**
 * Created by app on 12/23/16.
 */

public final class CustomProgressBarView extends FrameLayout {
    private ProgressBar progressBar;
    private TextView tvPercentage;

    public CustomProgressBarView(Context context) {
        this(context, null, 0);
    }

    public CustomProgressBarView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CustomProgressBarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(R.layout.progress_bar_view, this);
        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        tvPercentage = (TextView) findViewById(R.id.percentage);

        progressBar.setMax(100);
        progressBar.setProgress(0);
        tvPercentage.setVisibility(INVISIBLE);
    }

    public void setProgress(final int progress){
        progressBar.setProgress(progress);

        Subscription subscription = Observable.just(progress)
            .subscribe(new Subscriber<Integer>() {
                @Override
                public void onCompleted() {

                }

                @Override
                public void onError(Throwable e) {
                    Logging.log("[e] " + e.getMessage());
                }

                @Override
                public void onNext(Integer progress) {
                    tvPercentage.setText(progress + "%");
                    int position = progressBar.getMeasuredWidth() * progress / 100;
                    FrameLayout.LayoutParams params = (LayoutParams) tvPercentage.getLayoutParams();
                    params.width = position;

                    if(tvPercentage.getVisibility() == INVISIBLE && progress >= 5) {
                        tvPercentage.setVisibility(VISIBLE);
                    }
                    tvPercentage.requestLayout();
                }
            });
        subscription.unsubscribe();
        invalidate();
    }

    public void setProgressBarColor(int resColorId) {
        progressBar.setBackgroundColor(ContextCompat.getColor(getContext(), resColorId));
    }
}
