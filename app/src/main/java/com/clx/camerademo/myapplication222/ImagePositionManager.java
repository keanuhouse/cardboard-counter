package com.clx.camerademo.myapplication222;

import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;

public class ImagePositionManager {

    //缩放图片时控制其显示位置
    public static void setShowPosition(Drawable drawable, Matrix matrix, int w, int h){
        RectF rectF=new RectF(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        matrix.mapRect(rectF);
        float rw=rectF.width();
        float rh=rectF.height();
        float moveX=0,moveY=0;
        if(rw<=w){
            moveX=w/2-rw/2-rectF.left;
        }
        if (rh<=h) {
            moveY=h/2-rh/2-rectF.top;
        }
        if(rw>w && rectF.left>0){
            moveX=-rectF.left;
        }
        if(rw>w && rectF.right<w){
            moveX=w-rectF.right;
        }
        if(rh>h && rectF.top>0){
            moveY=-rectF.top;
        }
        if(rh>h && rectF.bottom<h){
            moveY=h-rectF.bottom;
        }
        matrix.postTranslate(moveX, moveY);
    }

    //移动图片时控制显示位置
    public static void setMovePosition(Drawable drawable, Matrix matrix, float dx, float dy, int w, int h){
        RectF rectF=new RectF(0,0,drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
        matrix.mapRect(rectF);
        float rw=rectF.width();
        float rh=rectF.height();
        if(rw>w && rectF.left+dx<=0 && rectF.right+dx>=w){
            matrix.postTranslate(dx, 0);
        }
        if(rh>h && rectF.top+dy<=0 && rectF.bottom+dy>=h){
            matrix.postTranslate(0, dy);
        }
    }

}


