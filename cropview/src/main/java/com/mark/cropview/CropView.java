package com.mark.cropview;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.DrawableRes;
import android.support.annotation.Nullable;
import android.support.v7.widget.AppCompatImageView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;

public class CropView extends AppCompatImageView {

    final static String TAG = "CropView";

    final static int DEFAULT_VIEW_PORT_BORDER_COLOR = Color.WHITE;//默认裁剪框颜色

    int viewPortBorderColor = DEFAULT_VIEW_PORT_BORDER_COLOR;//裁剪框颜色
    int viewPortBorderWidth = dp2px(1);//裁剪框宽度

    float viewPortLeftAndRightMargin = dp2px(25);
    float viewPortTopAndBottomMargin = dp2px(10);


    float cropRadio = 16f / 9f;//默认裁剪高宽比例: H : W = 1：1
//    float cropRadio = 1f / 1f;//默认裁剪高宽比例: H : W = 1：1

    boolean init = true;

    Bitmap bitmap;

    RectF viewPortRectF = new RectF();
    Paint viewPortPaint = new Paint();
    Matrix matrix = new Matrix();


    public CropView(Context context) {
        this(context, null);
    }

    public CropView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CropView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    void init() {
        viewPortPaint.setAntiAlias(true);
        viewPortPaint.setColor(viewPortBorderColor);
        viewPortPaint.setStrokeWidth(viewPortBorderWidth);
        viewPortPaint.setStyle(Paint.Style.STROKE);
    }


    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();
        int halfWidth = getWidth() / 2;
        int halfHeight = getHeight() / 2;
        canvas.translate(halfWidth, halfHeight);
        if (init) {
            //首次
            float right, bottom;
            right = halfWidth - viewPortLeftAndRightMargin;
            bottom = right * cropRadio;
            float distanceV = halfHeight - viewPortTopAndBottomMargin;
            if (bottom >= distanceV) {
                bottom = distanceV;
                right = distanceV / cropRadio;
            }
            viewPortRectF.set(-right, -bottom, right, bottom);
            drawBitmap(canvas);
            drawViewPort(canvas);
            init = false;
        } else {
            //执行变幻时
            // TODO: 2019/6/2 变换时 绘制图片
        }
        canvas.restore();
    }

    /**
     * 绘制自适应的bitmap
     *
     * @param canvas
     */
    private void drawBitmap(Canvas canvas) {
        if (bitmap == null) {
            Log.e(TAG, "drawBitmap bitmap == null");
        }
        float viewPortWidth = viewPortRectF.width();
        float viewPortHeight = viewPortRectF.height();
        int bw = bitmap.getWidth();
        int bh = bitmap.getHeight();
        if (bw >= bh) {
            float halfBh = viewPortHeight / 2;
            float halfBw = halfBh / bh * bw;
            int right = (int) halfBw;
            int bottom = (int) halfBh;
            Rect dst = new Rect(-right, -bottom, right, bottom);
            canvas.drawBitmap(bitmap,dst, dst, null);
        } else {
            float halfBw = viewPortWidth / 2;
            float halfBh = halfBw / bw * bh;
            int right = (int) halfBw;
            int bottom = (int) halfBh;
            Rect dst = new Rect(-right, -bottom, right, bottom);
            canvas.drawBitmap(bitmap, dst, dst, null);
        }

    }

    /**
     * 绘制 viewport
     *
     * @param canvas
     */
    private void drawViewPort(Canvas canvas) {
        canvas.drawRect(viewPortRectF, viewPortPaint);
    }


    @Override
    public void setImageBitmap(Bitmap bm) {
        this.bitmap = bm;
        invalidate();
    }

    @Override
    public void setImageResource(@DrawableRes int resId) {
        final Bitmap bitmap = BitmapFactory.decodeResource(getResources(), resId);
        setImageBitmap(bitmap);
    }

    @Override
    public void setImageDrawable(@Nullable Drawable drawable) {
        final Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            bitmap = bitmapDrawable.getBitmap();
        } else if (drawable != null) {
            bitmap = asBitmap(drawable, getWidth(), getHeight());
        } else {
            bitmap = null;
        }
        setImageBitmap(bitmap);
    }

    public float getCropRadio() {
        return cropRadio;
    }

    public void setCropRadio(float radio) {
        this.cropRadio = radio;
        invalidate();
    }

    @Nullable
    private Bitmap getImageBitmap() {
        return bitmap;
    }


    private Bitmap asBitmap(Drawable drawable, int minWidth, int minHeight) {
        final Rect tmpRect = new Rect();
        drawable.copyBounds(tmpRect);
        if (tmpRect.isEmpty()) {
            tmpRect.set(0, 0, Math.max(minWidth, drawable.getIntrinsicWidth()), Math.max(minHeight, drawable.getIntrinsicHeight()));
            drawable.setBounds(tmpRect);
        }
        Bitmap bitmap = Bitmap.createBitmap(tmpRect.width(), tmpRect.height(), Bitmap.Config.ARGB_8888);
        drawable.draw(new Canvas(bitmap));
        return bitmap;
    }

    private int dp2px(float dp) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

}
