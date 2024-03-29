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
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import static android.support.v4.graphics.TypefaceCompatUtil.closeQuietly;

public class CropView extends AppCompatImageView {

    final static String TAG = "CropView";

    final static int DEFAULT_VIEW_PORT_BORDER_COLOR = Color.WHITE;//默认裁剪框颜色
    final static int DEFAULT_OVERLAY_COLOR = Color.parseColor("#99000000");//默认裁剪框颜色
    //final static int DEFAULT_OVERLAY_COLOR = Color.WHITE;//默认裁剪框颜色


    static final float DEFAULT_MAX_SCALE = 8.0f;
    static final float DEFAULT_MID_SCALE = 4.0f;
    static final float DEFAULT_MIN_SCALE = 1.0f;


    float minScale = DEFAULT_MIN_SCALE;
    float middleScale = DEFAULT_MID_SCALE;
    float maxScale = DEFAULT_MAX_SCALE;


    int viewPortBorderColor = DEFAULT_VIEW_PORT_BORDER_COLOR;//裁剪框颜色
    int viewPortBorderWidth = dp2px(1);//裁剪框宽度

    int overlayColor = DEFAULT_OVERLAY_COLOR;

    float viewPortLeftAndRightMargin = dp2px(25);
    float viewPortTopAndBottomMargin = dp2px(10);


    float[] matrixValues = new float[9];


    float cropRadio = 16f / 9f;//默认裁剪高宽比例: H : W = 1：1
    //float cropRadio = 1f / 1f;//默认裁剪高宽比例: H : W = 1：1

    boolean init = true;

    Bitmap bitmap;
    RectF viewportRectF = new RectF();
    Paint viewportBorderPaint = new Paint();
    Paint overlayPaint = new Paint();

    Matrix matrix = new Matrix();


    GestureDetector simpleDetector;
    ScaleGestureDetector scaleDetector;

    GestureDetector.SimpleOnGestureListener simpleListener = new GestureDetector.SimpleOnGestureListener() {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (e1.getPointerCount() > 1 || e2.getPointerCount() > 1) {
                return false;
            } else {
                matrix.postTranslate(-distanceX, -distanceY);
                postInvalidate();
                return true;
            }
        }
    };

    ScaleGestureDetector.SimpleOnScaleGestureListener scaleListener = new ScaleGestureDetector.SimpleOnScaleGestureListener() {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            float focusX = detector.getFocusX();
            float focusY = detector.getFocusY();
            imageScale(scaleFactor, focusX, focusY);
            return true;
        }
    };

    void imageScale(float scaleFactor, float px, float py) {
        float scale = getScale();
        if (scale >= middleScale) {
            postInvalidate();
        } else {
            postInvalidate();
        }
        if ((scale < maxScale || scaleFactor < 1f) && (scale > minScale || scaleFactor > 1f)) {
            matrix.postScale(scaleFactor, scaleFactor, px, py);
            postInvalidate();
        }
    }

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
        viewportBorderPaint.setAntiAlias(true);
        viewportBorderPaint.setDither(true);
        viewportBorderPaint.setFilterBitmap(true);
        viewportBorderPaint.setColor(viewPortBorderColor);
        viewportBorderPaint.setStrokeWidth(viewPortBorderWidth);
        viewportBorderPaint.setStyle(Paint.Style.STROKE);

        overlayPaint.setAntiAlias(true);
        overlayPaint.setDither(true);
        overlayPaint.setFilterBitmap(true);
        overlayPaint.setColor(overlayColor);
        overlayPaint.setStyle(Paint.Style.FILL);

        simpleDetector = new GestureDetector(getContext(), simpleListener);
        scaleDetector = new ScaleGestureDetector(getContext(), scaleListener);


    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        boolean handle = false;
        if (null != scaleDetector) {
            //缩放
            handle = scaleDetector.onTouchEvent(event);
        }
        if (null != simpleDetector && simpleDetector.onTouchEvent(event) && event.getPointerCount() == 1) {
            //拖拽
            handle = true;
        }
        return handle;

    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int halfWidth = getWidth() / 2;
        int halfHeight = getHeight() / 2;
        if (init) {
            //首次
            canvas.save();
            canvas.translate(halfWidth, halfHeight);
            setViewport(halfWidth, halfHeight);
            drawBitmap(canvas);
            drawViewPort(canvas);
            drawOverlay(canvas);
            canvas.restore();
            init = false;
        } else {
            //执行变幻时
            drawTransformBitmap(canvas);
            canvas.save();
            canvas.translate(halfWidth, halfHeight);
            setViewport(halfWidth, halfHeight);
            drawViewPort(canvas);
            drawOverlay(canvas);
            canvas.restore();
        }
    }

    /**
     * 设置 viewport
     *
     * @param halfWidth
     * @param halfHeight
     */
    private void setViewport(int halfWidth, int halfHeight) {
        float right, bottom;
        right = halfWidth - viewPortLeftAndRightMargin;
        bottom = right * cropRadio;
        float distanceV = halfHeight - viewPortTopAndBottomMargin;
        if (bottom >= distanceV) {
            bottom = distanceV;
            right = distanceV / cropRadio;
        }
        viewportRectF.set(-right, -bottom, right, bottom);
    }

    /**
     * 绘制自适应的bitmap
     *
     * @param canvas
     */
    private void drawBitmap(Canvas canvas) {
        bitmap = optimizeBitmap(bitmap);
        if (bitmap == null) {
            Log.e(TAG, "drawBitmap bitmap == null");
        }
        float bw = bitmap.getWidth();
        float bh = bitmap.getHeight();
        if (bh > bw) {
            float halfVh = (bh - viewportRectF.height()) / 2;
            float left = viewportRectF.left;
            float top = -halfVh - (viewportRectF.height() / 2);
            canvas.drawBitmap(bitmap, left, top, null);
        } else {
            float halfVw = (bw - viewportRectF.width()) / 2;
            float left = -halfVw - (viewportRectF.width() / 2);
            float top = viewportRectF.top;
            canvas.drawBitmap(bitmap, left, top, null);
        }
    }

    /**
     * 变幻时 绘制bitmap
     *
     * @param canvas
     */
    private void drawTransformBitmap(Canvas canvas) {
        if (bitmap == null) {
            return;
        }

        int bw = bitmap.getWidth();
        int bh = bitmap.getHeight();

        float tx = getValue(matrix, Matrix.MTRANS_X);
        float ty = getValue(matrix, Matrix.MTRANS_Y);

        float sx = bw * getValue(matrix, Matrix.MSCALE_X);
        float sy = bh * getValue(matrix, Matrix.MSCALE_Y);

        Log.e(TAG, String.format("tx = %f, ty = %f", tx, ty));
        Log.e(TAG, String.format("sx = %f, sy = %f", sx, sy));

        float halfWidth = getWidth() / 2;
        float halfHeight = getHeight() / 2;

        float left = viewportRectF.left + halfWidth;
        float top = viewportRectF.top + halfHeight;
        float right = -tx + left + viewportRectF.width();
        float bottom = -ty - viewportRectF.top + viewportRectF.height();
        if (tx >= left) {
            float distanceX = tx - left;
            matrix.postTranslate(-distanceX, 0);
        }
        if (ty >= top) {
            float distanceY = ty - top;
            matrix.postTranslate(0, -distanceY);
        }
        if (right >= sx) {
            float distanceX = tx - left;
            matrix.postTranslate(-distanceX, 0);
        }
        if (bottom >= sy){
            float distanceY = ty - top;
            matrix.postTranslate(0, -distanceY);
        }


        Log.e(TAG, String.format("left = %f, top = %f, right = %f, bottom = %f",
                viewportRectF.left + halfWidth, viewportRectF.top + halfHeight, halfWidth - viewportRectF.right, halfHeight - viewportRectF.bottom));
        checkBound();
        canvas.drawBitmap(bitmap, matrix, null);
    }

    private void checkBound() {

    }

    /**
     * 绘制 viewport
     *
     * @param canvas
     */
    private void drawViewPort(Canvas canvas) {
        canvas.drawRect(viewportRectF, viewportBorderPaint);
    }

    /**
     * 绘制矩形半透明层
     *
     * @param canvas
     */
    private void drawOverlay(Canvas canvas) {
        int halfWidth = getWidth() / 2;
        int halfHeight = getHeight() / 2;
        //left
        canvas.drawRect(-halfWidth, -halfHeight, viewportRectF.left, halfHeight, overlayPaint);
        //top
        canvas.drawRect(viewportRectF.left, -halfHeight, viewportRectF.right, viewportRectF.top, overlayPaint);
        //right
        canvas.drawRect(viewportRectF.right, -halfHeight, halfWidth, halfHeight, overlayPaint);
        //bottom
        canvas.drawRect(viewportRectF.left, viewportRectF.bottom, viewportRectF.right, halfHeight, overlayPaint);

    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        this.bitmap = bm;
        invalidate();
    }


    /**
     * 图片适配
     *
     * @param bitmap
     * @return
     */
    Bitmap optimizeBitmap(Bitmap bitmap) {
        if (bitmap != null) {
            float viewPortWidth = viewportRectF.width();
            float viewPortHeight = viewportRectF.height();
            Log.e(TAG, "v " + viewPortWidth + ":" + viewPortWidth);
            int bw = bitmap.getWidth();
            int bh = bitmap.getHeight();
            Log.e(TAG, "b " + bw + ":" + bh);
            Bitmap optimizedBitmap;
            if (bh >= bw) {
                float scaleRadio = bw / viewPortWidth;
                int dstW = (int) viewPortWidth;
                int dstH = (int) (bh / scaleRadio);
                Log.e(TAG, "dst " + dstW + ":" + dstH);
                optimizedBitmap = Bitmap.createScaledBitmap(bitmap, dstW, dstH, false);
            } else {
                float scaleRadio = bh / viewPortHeight;
                int dstW = (int) (bw / scaleRadio);
                int dstH = (int) viewPortHeight;
                Log.e(TAG, "dst " + dstW + ":" + dstH);
                optimizedBitmap = Bitmap.createScaledBitmap(bitmap, dstW, dstH, false);
            }
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
            return optimizedBitmap;
        }
        return null;
    }

    public Bitmap crop() {
        if (bitmap == null) {
            return null;
        }
        final Bitmap.Config srcConfig = bitmap.getConfig();
        final Bitmap.Config config = srcConfig == null ? Bitmap.Config.ARGB_8888 : srcConfig;
        final int viewportWidth = (int) viewportRectF.width();
        final int viewportHeight = (int) viewportRectF.height();
        Bitmap dst = Bitmap.createBitmap(viewportWidth, viewportHeight, config);
        Canvas canvas = new Canvas(dst);
        final int left = (getWidth() - viewportWidth) / 2;
        final int top = (getHeight() - viewportHeight) / 2;
        canvas.translate(-left, -top);
        drawTransformBitmap(canvas);
        return dst;
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


    private float getScale() {
        return (float) Math.sqrt((float) Math.pow(getValue(matrix, Matrix.MSCALE_X), 2) + (float) Math.pow(getValue(matrix, Matrix.MSKEW_Y), 2));
    }

    private float getValue(Matrix m, int whichValue) {
        m.getValues(matrixValues);
        return matrixValues[whichValue];
    }

    public void flushToFile(Bitmap bitmap, File dstFile, int quality, Bitmap.CompressFormat format) {
        OutputStream outputStream = null;
        try {
            dstFile.getParentFile().mkdirs();
            outputStream = new FileOutputStream(dstFile);
            bitmap.compress(format, quality, outputStream);
            outputStream.flush();
        } catch (final Throwable throwable) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error attempting to save bitmap.", throwable);
            }
        } finally {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
            closeQuietly(outputStream);
        }
    }

    private static void closeQuietly(@Nullable OutputStream outputStream) {
        try {
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error attempting to close stream.", e);
        }
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
