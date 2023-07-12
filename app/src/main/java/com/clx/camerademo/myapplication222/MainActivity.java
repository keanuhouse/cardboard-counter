package com.clx.camerademo.myapplication222;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private ImageView imageView;
    private TextView textView;
    public static final String KEY_IMAGE_PATH = "imagePath";
    private ImageView resultImg;

    private TextClock mHourMinuteTC;
    private TextClock mYearMonthDayWeekTC;

    static {//加载so库
        System.loadLibrary("native-lib");
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.imageView);
        textView = findViewById(R.id.count);
        findViewById(R.id.show).setOnClickListener(this);
        findViewById(R.id.process).setOnClickListener(this);


        initView();
        setTime();
    }

//    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void initView(){
        mHourMinuteTC = (TextClock) findViewById(R.id.tc_h_m);
        mYearMonthDayWeekTC = (TextClock) findViewById(R.id.tc_y_m_d_w);
    }
//    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR1)
    private void setTime(){
        String timeZone = "GMT+08:00";
        mHourMinuteTC.setTimeZone(timeZone);
        mHourMinuteTC.setFormat24Hour("HH:mm");
        mYearMonthDayWeekTC.setTimeZone(timeZone);
        mYearMonthDayWeekTC.setFormat24Hour("yyyy年MM月dd日 EEEE");
    }


    @Override
    public void onClick(View v) {
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.img3);
        if (v.getId() == R.id.show) {
            imageView.setImageBitmap(bitmap);
            textView.setText("");
        } else {
            int c = countCardboardBoxes(bitmap);
            imageView.setImageBitmap(bitmap);
            textView.setText(Integer.toString(c));

        }
    }

    public void gotoCamare(View view) {
        CameraActivity.startMe(this, 2005, CameraActivity.MongolianLayerType.IDCARD_POSITIVE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null) {
            resultImg.setImageURI(Uri.fromFile(new File(data.getStringExtra(KEY_IMAGE_PATH))));
        }
    }

    native int countCardboardBoxes(Object bitmap);

}