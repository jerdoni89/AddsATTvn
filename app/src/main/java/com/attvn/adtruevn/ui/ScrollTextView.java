package com.attvn.adtruevn.ui;

import android.content.Context;
import android.graphics.Rect;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.animation.LinearInterpolator;
import android.widget.Scroller;
import android.widget.TextView;

/**
 * Created by app on 12/16/16.
 */

public final class ScrollTextView extends TextView {
    private static final float DEFAULT_SCROLL_SPEED = 0.15f; // {DEFAULT_SCROLL_SPEED} px/sec

    private Scroller mScroller;

    private int mXPaused;
    private float mScrollSpeed;

    private boolean mPaused = false;

    public ScrollTextView(Context context) {
        this(context, null, android.R.attr.textViewStyle);
    }

    public ScrollTextView(Context context, AttributeSet attrs) {
        this(context, attrs, android.R.attr.textViewStyle);
    }

    public ScrollTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        setSingleLine();
        setEllipsize(null);
        mScrollSpeed = DEFAULT_SCROLL_SPEED;
    }

    public void startScroll() {
        mXPaused = -1 * getWidth();
        mPaused = true;

        resumeScroll();
    }

    private void resumeScroll() {
        if(!mPaused) return;

        setHorizontallyScrolling(true);

        mScroller = new Scroller(this.getContext(), new LinearInterpolator());
        setScroller(mScroller);

        int distance = calculateScrollingLen() - (getWidth() + mXPaused);
        int duration = (int) (distance * 1.0f / mScrollSpeed);

        mScroller.startScroll(mXPaused, 0, distance, 0, duration);
        invalidate();
        mPaused = false;
    }

    private int calculateScrollingLen() {
        TextPaint tp = getPaint();
        Rect rect = new Rect();
        String txt = getText().toString();
        tp.getTextBounds(txt, 0, txt.length(), rect);
        int scrollingLength = rect.width() + getWidth();
        rect = null;
        return scrollingLength;
    }

    // set scroll speed
    public void setScrollSpeed(int scrollSpeed) {
        mScrollSpeed = scrollSpeed;
    }

    @Override
    public void computeScroll() {
        super.computeScroll();

        if(null == mScroller) return;

        if(mScroller.isFinished() && (!mPaused)) {
            startScroll();
        }
    }
}
