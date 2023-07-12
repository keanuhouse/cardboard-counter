package com.clx.camerademo.myapplication222;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class cut_Activity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "MainActivity";
    private Uri mImageUri;                                  //指定的uri
    private CropImageView mCropImageView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cut_activity);

        mCropImageView = findViewById(R.id.crop_image);
        Button mBtnCut = findViewById(R.id.cut);

        mBtnCut.setOnClickListener(this);

        // 获取传递过来的信息，将图片显示在控件上
        mImageUri = getIntent().getParcelableExtra("mImageUri");

        Bitmap bitmap = null;
        try {
            bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), mImageUri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        mCropImageView.setDrawable(bitmap,200,300);

        mCropImageView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onClick(View v) {
        Bitmap bitmap;
        // 这里设置visibility为GONE 是为了触发CropImageView里面的onMeasure来重新设置控件大小
        mCropImageView.setVisibility(View.GONE);
        switch (v.getId()){
            case R.id.cut:
                bitmap = mCropImageView.getCropImage();
                mCropImageView.setVisibility(View.VISIBLE);

                // 将 Bitmap 转换成文件
                File file = new File(getExternalFilesDir(null), "cropped.jpg");
                OutputStream os = null;
                try {
                    os = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
                    os.flush();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (os != null) {
                            os.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                // 将处理结果返回
                Intent resultIntent = new Intent();
                resultIntent.putExtra("image_path", file.getAbsolutePath());
                setResult(Activity.RESULT_OK, resultIntent);
                finish();
                break;
            default:
                break;
        }
    }

}