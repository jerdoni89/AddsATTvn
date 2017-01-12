package com.attvn.adtruevn.adapter;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

/**
 * Created by app on 12/10/16.
 */

public class LogViewAdapter extends RecyclerView.Adapter<LogViewAdapter.ViewHolder> {

    private Context context;
    private List<String> mLogs;
    private boolean onBind;

    public LogViewAdapter(Context context, List<String> logs) {
        this.context = context;
        this.mLogs = logs;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TextView tvLogDetail = new TextView(context);
        ViewGroup.LayoutParams params = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        tvLogDetail.setLayoutParams(params);
        tvLogDetail.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        tvLogDetail.setTextColor(ContextCompat.getColor(context, android.R.color.black));
        parent.addView(tvLogDetail);
        return new ViewHolder(tvLogDetail);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        onBind = true;
        final String log = mLogs.get(position);
        holder.bindData(log);
        onBind = false;
    }

    @Override
    public int getItemCount() {
        return mLogs == null? 0 : mLogs.size();
    }

    public void appendLog(String s) {
        if(mLogs != null) {
            mLogs.add(s);
//            notifyItemInserted(getItemCount()-1);
        }
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView;
        }

        void bindData(String s) {
            textView.setText(s);
        }
    }
}
