package com.attvn.adtruevn.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.StateListDrawable;
import android.support.annotation.ColorRes;
import android.support.annotation.DrawableRes;
import android.support.annotation.IntRange;
import android.support.v4.content.ContextCompat;

/**
 * Created by app on 1/3/17.
 */

public class DrawableUtil {

    public static StateListDrawable getStateListDrawable(Context context, @DrawableRes int imageResource, @ColorRes int desiredColor, @IntRange(from = 0, to = 255) int disableAlpha) {

        // Create the colorized image (pressed state)
        Bitmap one = BitmapFactory.decodeResource(context.getResources(), imageResource);
        Bitmap oneCopy = Bitmap.createBitmap(one.getWidth(), one.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas c = new Canvas(oneCopy);
        Paint p = new Paint();
        int color = context.getResources().getColor(desiredColor);
        p.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        c.drawBitmap(one, 0, 0, p);

        // Create the disabled bitmap for the disabled state
        Bitmap disabled = BitmapFactory.decodeResource(context.getResources(),imageResource);
        Bitmap disabledCopy = Bitmap.createBitmap(disabled.getWidth(), disabled.getHeight(), Bitmap.Config.ARGB_8888);

        Canvas disabledCanvas = new Canvas(disabledCopy);;
        Paint alphaPaint = new Paint();
        alphaPaint.setAlpha(disableAlpha);
        disabledCanvas.drawBitmap(disabled, 0, 0, alphaPaint);

        StateListDrawable stateListDrawable = new StateListDrawable();

        // Pressed State
        stateListDrawable.addState(new int[]{android.R.attr.state_pressed}, new BitmapDrawable(context.getResources(), oneCopy));

        // Disabled State
        stateListDrawable.addState(new int[]{-android.R.attr.state_enabled}, new BitmapDrawable(context.getResources(), disabledCopy) );  // - symbol means opposite, in this case "disabled"

        // Default State
        stateListDrawable.addState(new int[]{}, ContextCompat.getDrawable(context, imageResource));

        return stateListDrawable;
    }

}
