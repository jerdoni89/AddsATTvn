package com.attvn.adtruevn.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.attvn.adtruevn.R;
import com.attvn.adtruevn.util.ClickListener;
import com.attvn.adtruevn.util.Global;
import com.attvn.adtruevn.util.Logging;
import com.attvn.adtruevn.util.RecyclerTouchListener;

import java.util.ArrayList;
import java.util.List;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

/**
 * Created by app on 12/30/16.
 */

public class OptionsView extends FrameLayout {
    public static final int VERTICAL = 0;
    public static final int HORIZONTAL = 1;

    private int orientation;
    private boolean isDownloadScreen = true;

    private Context mContext;
    private RecyclerView mListViewOptions;
    private RecyclerView.LayoutManager layoutManager;
    private OptionsAdapter mOptionsAdapter;

    private Handler handler;
    private Runnable hideAction = new Runnable() {
        @Override
        public void run() {
            setVisibility(GONE);
        }
    };

    public OptionsView(Context context) {
        this(context, null, 0);
    }

    public OptionsView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public OptionsView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        this.mContext = context;
        if(attrs != null) {
            TypedArray ar = context.getTheme().obtainStyledAttributes(attrs, R.styleable.OptionsView, 0, 0);
            try {
                orientation = ar.getInt(R.styleable.OptionsView_orientation, VERTICAL);
                isDownloadScreen = ar.getBoolean(R.styleable.OptionsView_is_downloading_screen, true);
            } finally {
                ar.recycle();
            }
        }
        handler = new Handler();
        mListViewOptions = new RecyclerView(context);
        mListViewOptions.setFocusable(true);
        mListViewOptions.requestFocus();

        FrameLayout.LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        mListViewOptions.setLayoutParams(params);
        this.addView(mListViewOptions);
        setLayoutManager();
        initAdapter();
    }

    private void setLayoutManager() {
        layoutManager = null;
        switch (orientation) {
            case HORIZONTAL:
                layoutManager = new LinearLayoutManager(mContext, LinearLayoutManager.HORIZONTAL, false);
                break;
            case VERTICAL:
            default:
                layoutManager = new LinearLayoutManager(mContext, LinearLayoutManager.VERTICAL, false);
                break;
        }
        mListViewOptions.setLayoutManager(layoutManager);
        mListViewOptions.setHasFixedSize(false);
    }

    private void initAdapter(){
        Options options = new Options();
        mOptionsAdapter = new OptionsAdapter(mContext, options);
        mListViewOptions.setAdapter(mOptionsAdapter);
        mOptionsAdapter.notifyItemChanged(0);
    }

    public void setOrientation(int type) {
        if(type > HORIZONTAL || type < VERTICAL) return;

        orientation = type;
        setLayoutManager();
    }

    public void setIsDownloadScreen(boolean isDownloadScreen) {
        this.isDownloadScreen = isDownloadScreen;
        mOptionsAdapter.notifyDataSetChanged();
    }

    public void showWithTimeout(int second) {
        if(getVisibility() == GONE) {
            handler.removeCallbacks(hideAction);
            setVisibility(VISIBLE);
            handler.postDelayed(hideAction, second * 1000);
        } else if(getVisibility() == VISIBLE) {
            setVisibility(GONE);
        }
    }

    public class Options {
        List<Option> optionList;

        public Options() {
            String [] options = mContext.getResources().getStringArray(R.array.options_name);
            TypedArray array = mContext.getResources().obtainTypedArray(R.array.options_icon);
            TypedArray array1 = mContext.getResources().obtainTypedArray(R.array.options_icon_selected);

            optionList = new ArrayList<>();
            int i = 0;
            while (i < options.length) {
                optionList.add(new Option(
                        array.getResourceId(i, R.drawable.ic_settings_black_48dp),
                        array1.getResourceId(i, R.drawable.ic_settings_white_48dp),
                        options[i]
                ));
                i++;
            }

            array.recycle();
            array1.recycle();
        }

        public Option getPosition(int position){
            return optionList.get(position);
        }

        public int getSize() {
            return optionList.size();
        }
    }

    private class Option {
        int icon, iconSelected;
        String optionTitle;

        private Option(int icon, int iconSelected, String optionTitle) {
            this.icon = icon;
            this.iconSelected = iconSelected;
            this.optionTitle = optionTitle;
        }
    }

    public class OptionsAdapter extends RecyclerView.Adapter<OptionsAdapter.OptionsHolder> {
        private Context mContext;
        private OptionsView.Options mOptions;
        private RecyclerView container;

        OptionsAdapter(Context context, OptionsView.Options options) {
            this.mContext = context;
            this.mOptions = options;
        }

        private int focusedItem = 0;

        @Override
        public void onAttachedToRecyclerView(final RecyclerView recyclerView) {
            super.onAttachedToRecyclerView(recyclerView);
            container = recyclerView;

            // Handle key up and key down and attempt to move selection
            recyclerView.setOnKeyListener(new View.OnKeyListener() {
                @Override
                public boolean onKey(View v, int keyCode, KeyEvent event) {
                    Logging.log("[a] key code change focused item: " + keyCode);
                    RecyclerView.LayoutManager lm = recyclerView.getLayoutManager();

                    // Return false if scrolled to the bounds and allow focus to move off the list
                    if (event.getAction() == KeyEvent.ACTION_DOWN) {
                        if(orientation == VERTICAL) {
                            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
                                return tryMoveSelection(lm, 1);
                            } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
                                return tryMoveSelection(lm, -1);
                            }
                        } else {
                            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) {
                                return tryMoveSelection(lm, 1);
                            } else if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                                return tryMoveSelection(lm, -1);
                            }
                        }
                        switch (keyCode) {
                            case KeyEvent.KEYCODE_DPAD_CENTER:
                            case KeyEvent.KEYCODE_ENTER:
                            case KeyEvent.KEYCODE_NUMPAD_ENTER:
                                if(onItemClickListener != null) {
                                    onItemClickListener.onClickOptionItem(focusedItem);
                                }
                                break;
                        }
                    }
                    return false;
                }
            });

            recyclerView.addOnItemTouchListener(recyclerTouchListener);
        }

        private RecyclerTouchListener recyclerTouchListener = new RecyclerTouchListener(mContext, container, new ClickListener() {
            @Override
            public void onClick(View view, int position) {
                RecyclerView.LayoutManager lm = container.getLayoutManager();
                lm.getChildAt(focusedItem).setBackgroundColor(ContextCompat.getColor(mContext,
                        isDownloadScreen ? android.R.color.white : android.R.color.black));
                lm.getChildAt(position).setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.darker_gray));
                focusedItem = position;
                if(onItemClickListener != null) {
                    onItemClickListener.onClickOptionItem(position);
                }
            }

            @Override
            public void onLongClick(View view, int position) {
                Log.d("OptionsView", String.valueOf(position));
            }
        });

        private boolean tryMoveSelection(RecyclerView.LayoutManager lm, int direction) {
            if(focusedItem + direction >= 0 && focusedItem + direction < getItemCount()) {
                lm.getChildAt(focusedItem).setBackgroundColor(ContextCompat.getColor(mContext,
                        isDownloadScreen ? android.R.color.white : android.R.color.black));
            }
            int tryFocusItem = focusedItem + direction;

            // If still within valid bounds, move the selection, notify to redraw, and scroll
            if (tryFocusItem >= 0 && tryFocusItem < getItemCount()) {
//                notifyItemChanged(focusedItem);
                focusedItem = tryFocusItem;
                notifyItemChanged(focusedItem);
                lm.scrollToPosition(focusedItem);
                lm.getChildAt(focusedItem).setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.darker_gray));
                return true;
            }

            return false;
        }

        @Override
        public OptionsHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(mContext).inflate(R.layout.item_option_views, parent, false);
            return new OptionsAdapter.OptionsHolder(v);
        }

        @Override
        public void onBindViewHolder(OptionsAdapter.OptionsHolder holder, int position) {
//            if(position == 0) {
//                holder.itemView.setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.darker_gray));
//            }
            holder.itemView.setSelected(focusedItem == position);
            holder.bind(mOptions.getPosition(position));
        }

        @Override
        public int getItemCount() {
            return mOptions.getSize();
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public int getItemViewType(int position) {
            return position;
        }

        private void setAnimation(View viewToAnimate, boolean selected) {
            Animation animation = AnimationUtils.loadAnimation(mContext,
                    selected ? R.anim.right_to_left : R.anim.left_to_right);
            viewToAnimate.startAnimation(animation);
        }

        class OptionsHolder extends RecyclerView.ViewHolder {

            private LinearLayout lnrContainer;
            private ImageView icOptions;
            private TextView tvOptions;

            private OnClickListener onClickListener = new OnClickListener() {
                @Override
                public void onClick(View v) {
//                    notifyItemChanged(focusedItem);
                    focusedItem = getLayoutPosition();
                    notifyItemChanged(focusedItem);
                }
            };

            OptionsHolder(View itemView) {
                super(itemView);
                lnrContainer = (LinearLayout) itemView.findViewById(R.id.lnr_container);
                icOptions = (ImageView) itemView.findViewById(R.id.ic_option);
                tvOptions = (TextView) itemView.findViewById(R.id.title_option);

                if(isDownloadScreen) {
                    lnrContainer.getLayoutParams().width = (int) Global.convertDpToPixel(60);
                } else {
                    lnrContainer.getLayoutParams().width = WRAP_CONTENT;
                    itemView.setPadding(16, 16, 16, 16);
                    itemView.setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.black));
                }
                itemView.setOnClickListener(onClickListener);
            }

            void bind(Option option) {
                icOptions.setImageResource(isDownloadScreen ? option.icon : option.iconSelected);
//                icOptions.setBackground(DrawableUtil.getStateListDrawable(
//                        mContext,
//                        isDownloadScreen ? option.icon : option.iconSelected,
//                        isDownloadScreen ? android.R.color.white : android.R.color.black,
//                        127
//                ));
//
                tvOptions.setText(option.optionTitle);
                tvOptions.setVisibility(GONE);

                if(itemView.isSelected()) {
                    itemView.setBackgroundColor(ContextCompat.getColor(mContext, android.R.color.darker_gray));
                } else {
                    itemView.setBackgroundColor(ContextCompat.getColor(mContext,
                            isDownloadScreen ? android.R.color.white : android.R.color.black));
                }
            }
        }
    }

    private OnItemClickListener onItemClickListener;

    public void setOnItemClickListener(OnItemClickListener onItemClickListener) {
        this.onItemClickListener = onItemClickListener;
    }

    public interface OnItemClickListener {
        void onClickOptionItem(int position);
    }
}
