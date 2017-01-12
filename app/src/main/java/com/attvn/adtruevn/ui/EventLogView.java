package com.attvn.adtruevn.ui;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.attvn.adtruevn.adapter.LogViewAdapter;

import java.util.ArrayList;

/**
 * Created by app on 12/10/16.
 */

public final class EventLogView extends FrameLayout {

    private Context mContext;

    private RecyclerView mListLogFile;
    private LogViewAdapter mLogViewAdapter;
    private static final int PADDING_PX = 16;

    public EventLogView(Context context) {
        this(context, null, 0);
    }

    public EventLogView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public EventLogView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        this.mContext = context;

        initView();
        addHandleListLog();
    }

    private void initView() {
        mListLogFile = new RecyclerView(mContext);
        FrameLayout.LayoutParams listLogFileParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        listLogFileParams.gravity = Gravity.BOTTOM;
        mListLogFile.setLayoutParams(listLogFileParams);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false);
        mListLogFile.setLayoutManager(layoutManager);
        addView(mListLogFile);
        setPadding(PADDING_PX, PADDING_PX, PADDING_PX, PADDING_PX);
        setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.transparent));
    }

    private void addHandleListLog() {
        if(mListLogFile != null) {
            mLogViewAdapter = new LogViewAdapter(mContext, new ArrayList<String>());
            mListLogFile.setAdapter(mLogViewAdapter);
        }
    }

    public void append(String s) {
        mLogViewAdapter.appendLog(s);
        mListLogFile.scrollToPosition(mLogViewAdapter.getItemCount() - 1);
    }
}
