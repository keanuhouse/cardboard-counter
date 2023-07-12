package com.clx.camerademo.myapplication222;

import static com.clx.camerademo.myapplication222.my_MainActivity.box_count;
import static com.clx.camerademo.myapplication222.my_MainActivity.boxes;
import static com.clx.camerademo.myapplication222.my_MainActivity.origin_bitmap;
import static com.clx.camerademo.myapplication222.my_MainActivity.textView;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;

import java.util.ArrayList;

/**
 * 自定义手势放大  缩小  平移view
 */
@SuppressLint("AppCompatCustomView")
public class ScaleMoveImageViewer extends ImageView implements View.OnTouchListener,ScaleGestureDetector.OnScaleGestureListener {
    private ScaleGestureDetector sgc;
    private GestureDetector gd;
    private float SOURCE_SCALE;
    private Matrix matrix=new Matrix();
    private float[] values=new float[9];
    private boolean once=true;
    private float preX,preY,currentX,currentY;
    private int prePointerCount;

    private static final int REQUESTCODE_BIGER=1;
    private static final int REQUESTCODE_SMALLER=2;
    private static final float BIGER_TMP_SCALE=1.06f;
    private static final float SMALLER_TMP_SCALE=0.94f;
    private static final float MAX_SCALE=4.0F;
    private static final float MIN_SCALE=0.2F;
    private ArrayList<PointF> pointList = new ArrayList<>();

    private Activity imageShower;
    public ScaleMoveImageViewer(Context context) {
        this(context,null);
    }
    public ScaleMoveImageViewer(final Context context, AttributeSet attrs) {
        super(context, attrs);
        this.imageShower= (Activity) context;
        super.setScaleType(ScaleType.MATRIX);
        this.setOnTouchListener(this);
        sgc=new ScaleGestureDetector(context, this);
        gd=new GestureDetector(context, new GestureDetector.SimpleOnGestureListener(){

            @Override
            public boolean onDoubleTap(MotionEvent e) {
                Log.i("TAG","onDoubleTap");
                //处理双击事件
//                float x=e.getX();
//                float y=e.getY();
//                setDoubleTapScale(x, y);
                return true;
            }
            @Override
            public boolean onSingleTapConfirmed(MotionEvent e) {
                Drawable drawable = getDrawable();

                // 获取 Drawable 的边界
                RectF bounds = new RectF(drawable.getBounds());

                // 创建矩阵并设置为当前的 image matrix
                Matrix imageMatrix = getImageMatrix();
                Matrix matrix = new Matrix(imageMatrix);

                // 计算缩放比例
                float[] values = new float[9];
                matrix.getValues(values);
                float scale = values[Matrix.MSCALE_X];

                // 将屏幕坐标转换为图片坐标
                float[] pts = {e.getX(), e.getY()};
                matrix.invert(matrix);
                matrix.mapPoints(pts);

                // 判断点击位置是否在矩形框内
                boolean isInsideBox = false;
                int boxIndex = -1;
                for (int i = 0; i < boxes.length; i++) {
                    if (boxes[i].contains((int)pts[0], (int)pts[1])) {
                        isInsideBox = true;
                        boxIndex = i;
                        break;
                    }
                }

                if (isInsideBox) {
                    // 点击位置在矩形框内，从boxes中移除该矩形框
                    Rect[] newBoxes = new Rect[boxes.length - 1];
                    int j = 0;
                    for (int i = 0; i < boxes.length; i++) {
                        if (i != boxIndex) {
                            newBoxes[j++] = boxes[i];
                        }
                    }
                    boxes = newBoxes;
                    box_count--;
                    textView.setText(Integer.toString(box_count));
                }
                else {
                    // 点击位置在矩形框以外，以点击点为中心，向boxes中添加一个新的矩形框
                    Rect newBox;
                    int newHeight = 0;
                    if (boxes.length > 0) {
                        // 计算其他矩形框的平均宽度
                        int sumHeight = 0;
                        for (Rect box : boxes) {
                            sumHeight += box.height();
                        }
                        int avgWidth = sumHeight / boxes.length;
                        newHeight = avgWidth;
                    } else {
                        // 如果没有已有矩形框，则设置默认宽度
                        newHeight = 100;
                    }
//                    (int)(pts[0] + newWidth / 2)
//                    (int)(pts[1] + origin_bitmap.getWidth() / 2)
//                    (int)(pts[0] - newWidth / 2)
//                    (int)(pts[1] - origin_bitmap.getWidth() / 2)
                    newBox = new Rect((int)(pts[0] - origin_bitmap.getWidth() / 2), (int)(pts[1] - newHeight / 2), (int)(pts[0] + origin_bitmap.getWidth() / 2), (int)(pts[1] + newHeight / 2));

                    Rect[] newBoxes = new Rect[boxes.length + 1];
                    for (int i = 0; i < boxes.length; i++) {
                        newBoxes[i] = boxes[i];
                    }
                    newBoxes[boxes.length] = newBox;
                    boxes = newBoxes;
                    box_count++;
                    textView.setText(Integer.toString(box_count));
                }

                // 创建新的 Bitmap 对象
                Bitmap newBitmap = origin_bitmap.copy(origin_bitmap.getConfig(), true);
                Canvas canvas = new Canvas(newBitmap);
                Paint paint = new Paint();
                paint.setStrokeWidth(4);
                paint.setColor(Color.GREEN);
                paint.setStyle(Paint.Style.STROKE); // 画线模式
                for (Rect box : boxes) {
                    canvas.drawRect(box, paint);
                }
                // 更新 ImageView 显示的 Bitmap
                setImageBitmap(newBitmap);
                return true;
            }

        });

    }

    float distanceBetweenPoints(PointF p1, PointF p2) {
        float dx = p1.x - p2.x;
        float dy = p1.y - p2.y;
        return (float) Math.sqrt(dx * dx + dy * dy);
    }

    //手指缩放
    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        float scaleFactor=detector.getScaleFactor();
        float currentScale=getScale();//相对原图的缩放比例
        if(currentScale>MAX_SCALE && scaleFactor<1.0f || currentScale<MIN_SCALE
                && scaleFactor>1.0f || currentScale<MAX_SCALE && currentScale>MIN_SCALE){
            matrix.postScale(scaleFactor, scaleFactor, detector.getFocusX(), detector.getFocusY());
        }
        ImagePositionManager.setShowPosition(getDrawable(), matrix, getWidth(), getHeight());
        setImageMatrix(matrix);
        return true;
    }

    //移动
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        currentX=0;currentY=0;
        int pointerCount=event.getPointerCount();
        for(int i=0;i<pointerCount;i++){
            currentX+=event.getX();
            currentY+=event.getY();
        }
        currentX/=pointerCount;
        currentY/=pointerCount;
        if (pointerCount!=prePointerCount) {
            preX=currentX;
            preY=currentY;
            prePointerCount=pointerCount;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                float dx=currentX-preX;
                float dy=currentY-preY;
                ImagePositionManager.setMovePosition(getDrawable(), matrix, dx, dy, getWidth(), getHeight());
                setImageMatrix(matrix);
                preX=currentX;
                preY=currentY;
                break;
            case MotionEvent.ACTION_UP://有多根手指触摸屏幕时，只有当所有的手指抬起时这里才执行
                prePointerCount=0;
                break;
        }
        gd.onTouchEvent(event);
        return sgc.onTouchEvent(event);
    }
    //双击缩放
    public void setDoubleTapScale(float px, float py){
        float currectScale=getScale();
        if(currectScale<SOURCE_SCALE){
            ScaleMoveImageViewer.this.postDelayed(new AutoScaleRunnable(SOURCE_SCALE, px, py,REQUESTCODE_BIGER), 10);
        }
        if(currectScale==SOURCE_SCALE){
            ScaleMoveImageViewer.this.postDelayed(new AutoScaleRunnable(MAX_SCALE-1, px, py,REQUESTCODE_BIGER), 10);
        }
        if(currectScale>SOURCE_SCALE){
            ScaleMoveImageViewer.this.postDelayed(new AutoScaleRunnable(SOURCE_SCALE, px, py,REQUESTCODE_SMALLER), 10);
        }
        ImagePositionManager.setShowPosition(getDrawable(), matrix, getWidth(), getHeight());
        setImageMatrix(matrix);
    }

    private class AutoScaleRunnable implements Runnable{

        float targetScale=0;
        float px=0;
        float py=0;
        int requestCode=0;
        public AutoScaleRunnable(float targetScale,float px,float py,int requestCode){
            this.targetScale=targetScale;
            this.px=px;
            this.py=py;
            this.requestCode=requestCode;
        }

        @Override
        public void run() {
            if(requestCode==REQUESTCODE_BIGER){
                matrix.postScale(BIGER_TMP_SCALE, BIGER_TMP_SCALE, px, py);
                ImagePositionManager.setShowPosition(getDrawable(), matrix, getWidth(), getHeight());
                setImageMatrix(matrix);
                float currentScale = getScale();
                if (currentScale<targetScale) {
                    ScaleMoveImageViewer.this.postDelayed(this, 10);
                }else {
                    while(getScale()!=targetScale){
                        matrix.postScale(targetScale/getScale(), targetScale/getScale(), px, py);
                        ImagePositionManager.setShowPosition(getDrawable(), matrix, getWidth(), getHeight());
                        setImageMatrix(matrix);
                    }
                }
            }
            else if (requestCode==REQUESTCODE_SMALLER) {
                matrix.postScale(SMALLER_TMP_SCALE, SMALLER_TMP_SCALE, px, py);
                ImagePositionManager.setShowPosition(getDrawable(), matrix, getWidth(), getHeight());
                setImageMatrix(matrix);
                float currentScale = getScale();
                if(currentScale>targetScale){
                    ScaleMoveImageViewer.this.postDelayed(this, 10);
                }else {
                    while(getScale()!=targetScale){
                        matrix.postScale(targetScale/getScale(), targetScale/getScale(), px, py);
                        ImagePositionManager.setShowPosition(getDrawable(), matrix, getWidth(), getHeight());
                        setImageMatrix(matrix);
                    }
                }
            }
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if(once){
            if (getDrawable()==null){
                return;
            }
            matrix=getImageMatrix();
            once=false;
            Drawable drawable=getDrawable();
            //获取图片的宽和高
            int dw=drawable.getIntrinsicWidth();
            int dh=drawable.getIntrinsicHeight();
            int w=getWidth();
            int h=getHeight();
            float scale=Math.min(1.0f*w/dw, 1.0f*h/dh);
            SOURCE_SCALE=scale;
            matrix.postTranslate(w/2-dw/2, h/2-dh/2);
            matrix.postScale(scale, scale, w/2, h/2);
            setImageMatrix(matrix);
        }
        super.onDraw(canvas);
    }

    private float getScale(){
        matrix.getValues(values);
        return values[Matrix.MSCALE_X];
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        return true;

    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {

    }
}
