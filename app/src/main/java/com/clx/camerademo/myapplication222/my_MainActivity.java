package com.clx.camerademo.myapplication222;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextClock;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.dothantech.lpapi.LPAPI;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;


public class my_MainActivity extends Activity {


    public static final String KEY_IMAGE_PATH = "imagePath";
    private LPAPI api;

    public static final int REQUEST_ID_MULTIPLE_PERMISSIONS = 1;  // Declare this integer globally
    private Uri mImageUri;                                  //指定的uri

    private String mImageName;                              //保存的图片的名字
    private String cutImagePath;
    public static Uri find_uri;
    private File mImageFile;                                //图片文件

    public static final String SD_APP_DIR_NAME = "TestDir"; //存储程序在外部SD卡上的根目录的名字
    public static final String PHOTO_DIR_NAME = "photo";    //存储照片在根目录下的文件夹名字
    public static final String VOICE_DIR_NAME = "voice";    //存储音频在根目录下的文件夹名字
    public static final String VIDEO_DIR_NAME = "video";    //存储视频在根目录下的文件夹名字

    public static final int PHOTO_RESULT_CODE = 100;        //标志符，图片的结果码，判断是哪一个Intent
    public static final int VOICE_RESULT_CODE = 101;        //标志符，音频的结果码，判断是哪一个Intent
    public static final int VIDEO_RESULT_CODE = 102;        //标志符，视频的结果码，判断是哪一个Intent
    private static final int REQUEST_PHOTO_CUT = 317;

    private static final String TAG = "MainActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;

    private ImageView imageView;
    private Button cameraButton;
    private Button galleryButton;
    private Button IdentifyButton;
    private String mImagePath;                   //用于存储最终目录，即根目录 / 要操作（存储文件）的文件夹
    private EditText editText1;
    private TextClock timeText1;
    private TextClock timeText2;

    private int c;


    private TextView mTextView2;
    public static TextView textView;
    private Boolean IdentifyOnce = true;
    private Bitmap result_bitmap;
    public static Rect[] boxes;
    public static int box_count;
    public static Bitmap origin_bitmap;


    static {//加载so库
        System.loadLibrary("native-lib");
    }





    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.my_activity_main);
        Log.d(TAG, "开始...");
        // android 7.0系统解决拍照的问题
        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());
        builder.detectFileUriExposure();

        imageView = findViewById(R.id.image_view);
        cameraButton = findViewById(R.id.camera_button);
        galleryButton = findViewById(R.id.gallery_button);
        IdentifyButton = findViewById(R.id.identify_button);
        textView = findViewById(R.id.text_view);

        editText1 =findViewById (R.id.text_text);
        timeText1=findViewById(R.id.tc_h_m);
        timeText2=findViewById(R.id.tc_y_m_d_w);
/*
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchTakePictureIntent();
            }
        });*/




        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dispatchPickImageIntent();
            }
        });




        IdentifyButton.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("SetTextI18n")
            @Override
            public void onClick(View view) {
                if (!IdentifyOnce){
                    // 获取ImageView中的Drawable
                    Bitmap original_bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();
                    // 复制原图
                    result_bitmap = original_bitmap.copy(original_bitmap.getConfig(), true);
                    // 进行纸板箱计数处理
                    boxes = detectBoxes(result_bitmap);
                    box_count = boxes.length;
                    Canvas canvas = new Canvas(result_bitmap);
                    Paint paint = new Paint();
                    paint.setStrokeWidth(4);
                    paint.setColor(Color.GREEN);
                    paint.setStyle(Paint.Style.STROKE); // 画线模式
                    for (int i = 0; i < boxes.length; i++) {
                        // 这两步是因为C++中的Rect rect(x, y, w, h) 存的是矩形的左上角坐标的x y和矩形的宽高
                        // 而Android里面绘图的函数canvas.drawRect要的是矩形的左上角坐标x y和右下角坐标x y
                        boxes[i].right = boxes[i].right + boxes[i].left;
                        boxes[i].bottom = boxes[i].bottom + boxes[i].top;
                        canvas.drawRect(boxes[i], paint);
                    }
                    textView.setText(Integer.toString(box_count));

                    // 在ImageView中显示修改后的Bitmap对象
                    imageView.setImageBitmap(result_bitmap);

                    IdentifyOnce = true;
                }
            }
        });


        // 初始化 LPAPI 对象（简单起见，不处理结果通知）
        api = LPAPI.Factory.createInstance();
    }




    @Override
    protected void onDestroy() {
        // 断开蓝牙连接，释放 LPAPI 对象
        api.quit();

        super.onDestroy();
    }

//    EditText editText1 =(EditText) findViewById (R.id.text_text);
//    String content = editText1.getText().toString();

    public void printTextOnClick(View view) {
        String content = editText1.getText().toString();
        String content1 = timeText1.getText().toString();
        String content2 = timeText2.getText().toString();


        printText(content2+content1,content,"        识别数量为" + c);
    }

    public void print2RcodeOnClick(View view) {
        printQRcode("http://www.detonger.com");
    }

    private void printText(String content1,String content2,String content3) {
        // 连接配对的第一个打印机对象
        api.openPrinter("");

        // 开始绘图任务，传入参数(页面宽度, 页面高度)
        api.startJob(50, 30, 0);

        // 开始一个页面的绘制，绘制文本字符串
        api.drawText(content1, 11, 1, 39, 30, 4);
        api.drawText(content2, 11, 11, 39, 30, 4);
        api.drawText(content3, 11, 23, 39, 30, 4);

        // 结束绘图任务提交打印
        api.commitJob();
    }

    private void printQRcode(String content) {
        // 连接配对的第一个打印机对象
        api.openPrinter("");

        // 开始绘图任务，传入参数(页面宽度, 页面高度)
        api.startJob(40, 30, 0);

        // 开始一个页面的绘制，绘制二维码
        api.draw2DQRCode(content, 4, 5, 20);

        // 结束绘图任务提交打印
        api.commitJob();
    }




    /*//拍照
    private void dispatchTakePictureIntent() {


        List<String> permissionList = ApplicationConfig.checkAndRequestPermissions(this);

        if (permissions(permissionList)) {
            //检查是否获得写入权限，未获得则向用户请求
            if (ActivityCompat.checkSelfPermission(my_MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                //未获得，向用户请求
                Log.d(TAG, "无读写权限，开始请求权限。");
                ActivityCompat.requestPermissions(my_MainActivity.this, new String[]
                        {Manifest.permission.WRITE_EXTERNAL_STORAGE}, 200);
            } else {
                Log.d(TAG, "有读写权限，准备启动相机。");
                //启动照相机
                startCamera();
            }
        }
    }*/

    //选照片
    private void dispatchPickImageIntent() {
        //动态申请获取访问 读写磁盘的权限
        if (ContextCompat.checkSelfPermission(my_MainActivity.this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(my_MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 101);
        } else {
            Intent pickImageIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(pickImageIntent, REQUEST_IMAGE_PICK);
        }
    }



    /**
     * 返回用户是否允许权限的结果，并处理
     */
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResult) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResult);
        if (requestCode == 200) {
            //用户允许权限
            if (grantResult[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "用户已允许权限，准备启动相机。");
                //启动照相机
                startCamera();
            } else {  //用户拒绝
                Log.d(TAG, "用户已拒绝权限，程序终止。");
                Toast.makeText(this, "程序需要写入权限才能运行", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * 启动相机，创建文件，并要求返回uri
     */
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void startCamera() {
        // 步骤一：创建存储照片的文件
        String path = getFilesDir() + File.separator + "images" + File.separator;
        mImageName = Calendar.getInstance().getTimeInMillis() + ".jpg";
        Log.d(TAG, "设置图片文件的名称为："+mImageName);
        mImageFile = new File(path, mImageName);

        mImagePath = mImageFile.getAbsolutePath();

        if(!mImageFile.getParentFile().exists())
            mImageFile.getParentFile().mkdirs();
        mImageFile.setWritable(true);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            //步骤二：Android 7.0及以上获取文件 Uri
            mImageUri = FileProvider.getUriForFile(this, "com.example.opencv.provider", mImageFile);
        } else {
            //步骤三：获取文件Uri
            mImageUri = Uri.fromFile(mImageFile);
        }

        //步骤四：调取系统拍照
        Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
        intent.putExtra(MediaStore.EXTRA_OUTPUT, mImageUri);
        //启动相机并要求返回结果
        //原版相机的话就是这里启动然后PHOTO_RESULT_CODE
        startActivityForResult(intent, PHOTO_RESULT_CODE);
    }






    /**
     * 创建图片文件
     */
    private void createImageFile(){
        Log.d(TAG, "开始创建图片文件...");
        //设置图片文件名（含后缀），以当前时间的毫秒值为名称
       // String path = getFilesDir() + File.separator + "images" + File.separator;



        mImageName = Calendar.getInstance().getTimeInMillis() + ".jpg";
        Log.d(TAG, "设置图片文件的名称为："+mImageName);
        //创建图片文件
        mImageFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/" + SD_APP_DIR_NAME + "/" + PHOTO_DIR_NAME + "/", mImageName);

        //将图片的绝对路径设置给mImagePath，后面会用到
        mImagePath = mImageFile.getAbsolutePath();
        //按设置好的目录层级创建
        mImageFile.getParentFile().mkdirs();
        Log.d(TAG, "按设置的目录层级创建图片文件，路径："+mImagePath);
        //不加这句会报Read-only警告。且无法写入SD
        mImageFile.setWritable(true);
        Log.d(TAG, "将图片文件设置可写。");
    }

    /**
     * 处理返回结果。
     * 1、图片
     * 2、音频
     * 3、视频
     *
     * @param requestCode 请求码
     * @param resultCode  结果码 成功 -1 失败 0
     * @param data        返回的数据
     */
    private static final int REQUEST_CODE_CROP_PHOTO = 200;
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == Activity.RESULT_OK) {
            switch (requestCode) {
                case PHOTO_RESULT_CODE: { // 这里接收结果
                    Bitmap bitmap = null;
                    try {
                        //根据uri设置bitmap

                        bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), mImageUri);// 然后我这里把原版拍摄的照片传到裁剪界面
                        // 这个mImageUri会不会没有初始化到，因为我是给裁剪界面传递mImageUri，然后通过mImageUri读取到图片的
                        // 这个mImageUri的初始化在startCamera里面，原版相机肯定是用到这个方法了，自定义相机我就不知道了
                        find_uri = mImageUri;
                        //将拍摄图片传递给裁剪界面
                        Intent intent = new Intent(this, cut_Activity.class);
                        intent.putExtra("mImageUri", mImageUri);

                        startActivityForResult(intent,REQUEST_PHOTO_CUT);

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //将图片保存到SD的指定位置
                    savePhotoToSD(bitmap);
                    //更新系统图库
                    updateSystemGallery();
                    Log.d(TAG, "结束。");
                    break;
                }
                case REQUEST_IMAGE_PICK:{
                    try {
                        handleImageOnKitKat(data);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    break;
                }
                case REQUEST_PHOTO_CUT:{
                    textView.setText("");
                    cutImagePath = data.getStringExtra("image_path");
                    Bitmap bitmap = BitmapFactory.decodeFile(cutImagePath);
                    // 获取屏幕宽度和高度
                    DisplayMetrics metrics = new DisplayMetrics();
                    getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    int screenWidth = metrics.widthPixels;
                    int screenHeight = metrics.heightPixels;

                    // 获取图像的宽度和高度
                    int imageWidth = bitmap.getWidth();
                    int imageHeight = bitmap.getHeight();

                    // 计算缩放比例
                    float scale = Math.min((float) screenWidth / imageWidth, (float) screenHeight / imageHeight);

                    // 缩放图像
                    Matrix matrix = new Matrix();
                    matrix.postScale(scale, scale);
                    origin_bitmap = Bitmap.createBitmap(bitmap, 0, 0, imageWidth, imageHeight, matrix, true);
                    imageView.setImageBitmap(origin_bitmap);
                    IdentifyOnce = false;
                    break;
                }
                case 2005:{
                    if (data != null) {
                        Uri uri = Uri.fromFile(new File(data.getStringExtra(KEY_IMAGE_PATH)));
                        Intent intent = new Intent(this, cut_Activity.class);
                        intent.putExtra("mImageUri", uri);

                        startActivityForResult(intent,REQUEST_PHOTO_CUT);
                    }
                    break;
                }

            }
        }
    }

    /**
     * 保存照片到SD卡的指定位置
     */
    private void savePhotoToSD(Bitmap bitmap) {
        Log.d(TAG, "将图片保存到指定位置。");
        //创建输出流缓冲区
        BufferedOutputStream os = null;
        try {
            //设置输出流
            os = new BufferedOutputStream(new FileOutputStream(mImageFile));
            Log.d(TAG, "设置输出流。");
            //压缩图片，100表示不压缩
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os);
            Log.d(TAG, "保存照片完成。");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (os != null) {
                try {
                    //不管是否出现异常，都要关闭流
                    os.flush();
                    os.close();
                    Log.d(TAG, "刷新、关闭流");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 更新系统图库
     */
    private void updateSystemGallery() {
        //把文件插入到系统图库
        try {
            MediaStore.Images.Media.insertImage(this.getContentResolver(),
                    mImageFile.getAbsolutePath(), mImageName, null);
            Log.d(TAG, "将图片文件插入系统图库。");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // 最后通知图库更新
        this.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + mImagePath)));
        Log.d(TAG, "通知系统图库更新。");
    }

    private boolean permissions(List<String> listPermissionsNeeded) {
        if (!listPermissionsNeeded.isEmpty()) {
            ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray
                    (new String[listPermissionsNeeded.size()]), REQUEST_ID_MULTIPLE_PERMISSIONS);
            return false;
        }
        return true;
    }

    private void handleImageOnKitKat(Intent data) throws IOException {
        Uri uri = data.getData();
        find_uri = uri;
        //将拍摄图片传递给裁剪界面
        Intent intent = new Intent(this, cut_Activity.class);
        intent.putExtra("mImageUri", uri);
        startActivityForResult(intent,REQUEST_PHOTO_CUT);
    }

    // 声明native方法
    public native Rect[] detectBoxes(Bitmap bitmap);

    public void gotoCamare(View view) {
        CameraActivity.startMe(this, 2005, CameraActivity.MongolianLayerType.IDCARD_POSITIVE);
    }
}

