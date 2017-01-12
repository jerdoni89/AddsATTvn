package com.attvn.adtruevn.ui;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextClock;

import com.attvn.adtruevn.R;


/**
 * Created by app on 12/16/16.
 */

public final class AdsScrollableView extends FrameLayout {

    private ScrollTextView scrollTextView;
    private TextClock textClock;
    private ImageView iconAds;

    public AdsScrollableView(Context context) {
        this(context, null, 0);
    }

    public AdsScrollableView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public AdsScrollableView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        LayoutInflater.from(context).inflate(R.layout.scrollable_ads_view, this);
        textClock = (TextClock) findViewById(R.id.text_time);
        textClock.setTimeZone("GMT+0700");
        textClock.setFormat24Hour(null);
        textClock.setFormat12Hour("hh:mm a");

        scrollTextView = (ScrollTextView) findViewById(R.id.text_scrollable_content);
        scrollTextView.setSelected(true);
        scrollTextView.startScroll();

        iconAds = (ImageView) findViewById(R.id.icon_ads);
    }

    public void setIconAdsResource(int resId) {
        Drawable drawableRes = ContextCompat.getDrawable(getContext(), resId);
        iconAds.setImageDrawable(drawableRes);
    }

    public void setScrollableMessageText(String message) {
        scrollTextView.setText(message);
    }
}
