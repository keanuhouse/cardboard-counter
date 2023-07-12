#include "com_example_opencv_my_MainActivity.h"

#include <iostream>
#include <opencv2/opencv.hpp>
#include <jni.h>
#include <android/bitmap.h>

using namespace cv;
using namespace std;

extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_clx_camerademo_myapplication222_my_1MainActivity_detectBoxes(JNIEnv *env, jobject thiz,
                                                                      jobject bitmap) {
    AndroidBitmapInfo info;
    void *pixels;

    CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);
    CV_Assert(info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ||
              info.format == ANDROID_BITMAP_FORMAT_RGB_565);
    CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);
    CV_Assert(pixels);

    Mat temp(info.height, info.width, CV_8UC4, pixels);
    Mat gray;
    cvtColor(temp, gray, COLOR_RGBA2GRAY);
    equalizeHist(gray, gray);
    Mat blur;
    GaussianBlur(gray, blur, Size(5, 5), 0);
    //用大津算法算出纸板箱的大致数量，用以计算block_size
    Mat thresh_OTSU;
    threshold(blur, thresh_OTSU, 0, 255, THRESH_BINARY_INV | THRESH_OTSU);
    Mat kernel = getStructuringElement(MORPH_RECT, Size(5, 5));
    bitwise_not(thresh_OTSU, thresh_OTSU);
    Mat erosion_OTSU;
    erode(thresh_OTSU, erosion_OTSU, kernel, Point(-1, -1), 1);
    vector<vector<Point>> contours_OTSU;
    vector<Vec4i> hierarchy_OTSU;
    findContours(erosion_OTSU, contours_OTSU, hierarchy_OTSU, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

    vector<cv::Rect> boxes_OTSU;
    for (size_t i = 0; i < contours_OTSU.size(); i++) {
        double area = contourArea(contours_OTSU[i]);
        if (500 < area) {
            Rect box = boundingRect(contours_OTSU[i]);
            boxes_OTSU.push_back(box);
        }
    }

    int block_size = static_cast<int>(temp.rows / boxes_OTSU.size() / 2) * 2 + 1;
    Mat thresh;
    adaptiveThreshold(blur, thresh, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, block_size, 0);
    Mat erosion;
    erode(thresh, erosion, kernel, Point(-1, -1), 1);

//    int num_labels;
//    Mat labels, stats, centroids;
//    num_labels = connectedComponentsWithStats(erosion, labels, stats, centroids);
//
//    vector<Rect> boxes;
//    int x, y, w, h, area;
//    for (int i = 0; i < num_labels; i++) {
//        x = stats.at<int>(i, CC_STAT_LEFT);
//        y = stats.at<int>(i, CC_STAT_TOP);
//        w = stats.at<int>(i, CC_STAT_WIDTH);
//        h = stats.at<int>(i, CC_STAT_HEIGHT);
//        area = stats.at<int>(i, CC_STAT_AREA);
//        float bili = static_cast<float>(w) / static_cast<float>(h);
//        if (500 < area && bili >= 4) {
//            Rect rect(x, y, w, h);
//            boxes.push_back(rect);
//        }
//    }

    // 寻找连通区域并筛选目标区域
    vector<vector<Point>> contours;
    vector<Vec4i> hierarchy;
    findContours(erosion, contours, hierarchy, RETR_EXTERNAL, CHAIN_APPROX_SIMPLE);

    vector<Rect> boxes_1;
    // 面积判断纸箱连通域
    for (size_t i = 0; i < contours.size(); i++)
    {
        double area = contourArea(contours[i]);
        if (50 < area)
        {
            Rect box = boundingRect(contours[i]);
            boxes_1.push_back(box);
        }
    }

    int num_box = boxes_1.size();

    // 连通域合并防止多检
    int i_1 = 0;
    while (i_1 < num_box)
    {
        Rect box_i = boxes_1[i_1];
        int j = i_1 + 1;
        while (j < num_box)
        {
            double dist = abs((boxes_1[j].y + boxes_1[j].height / 2) - (boxes_1[i_1].y + boxes_1[i_1].height / 2));
            if (dist < block_size * 0.4)
            {
                Rect box_j = boxes_1[j];
                int xb_i = box_i.x + box_i.width;
                int yb_i = box_i.y + box_i.height;
                int xb_j = box_j.x + box_j.width;
                int yb_j = box_j.y + box_j.height;
                box_i.x = min(box_i.x, box_j.x);
                box_i.y = min(box_i.y, box_j.y);
                box_i.width = max(xb_i, xb_j) - box_i.x;
                box_i.height = max(yb_i, yb_j) - box_i.y;
                boxes_1[j] = box_j;
                boxes_1.erase(boxes_1.begin() + j);
                num_box--;
                j--;
            }
            j++;
        }

        boxes_1[i_1] = box_i;
        i_1++;
    }
    int area_box = static_cast<int>(block_size * 0.1 * temp.cols);
    vector<Rect> boxes;
    for (int i = 0; i < num_box; i++)
    {
        int area = boxes_1[i].width * boxes_1[i].height;
        if (area > area_box)
        {
            boxes.push_back(boxes_1[i]);
        }
    }

//    cvtColor(erosion,temp,COLOR_GRAY2RGBA);
    AndroidBitmap_unlockPixels(env, bitmap);

    // Create a jobjectArray with the same size as boxes
    jclass rectClass = env->FindClass("android/graphics/Rect");
    jobjectArray rectArray = env->NewObjectArray(boxes.size(), rectClass, nullptr);

    // Populate the jobjectArray with Rect objects
    jmethodID rectConstructor = env->GetMethodID(rectClass, "<init>", "(IIII)V");
    for (size_t i = 0; i < boxes.size(); i++) {
        jobject rectObj = env->NewObject(rectClass, rectConstructor, boxes[i].x, boxes[i].y, boxes[i].width,
                                         boxes[i].height);
        env->SetObjectArrayElement(rectArray, i, rectObj);
    }

    return rectArray;
}
extern "C"
JNIEXPORT jintArray JNICALL
Java_com_clx_camerademo_myapplication222_CropImageView_cut_1img(JNIEnv *env, jobject thiz,
                                                                jobject bitmap) {
    AndroidBitmapInfo info;
    void *pixels;

    CV_Assert(AndroidBitmap_getInfo(env, bitmap, &info) >= 0);
    CV_Assert(info.format == ANDROID_BITMAP_FORMAT_RGBA_8888 ||
              info.format == ANDROID_BITMAP_FORMAT_RGB_565);
    CV_Assert(AndroidBitmap_lockPixels(env, bitmap, &pixels) >= 0);
    CV_Assert(pixels);

    Mat img(info.height, info.width, CV_8UC4, pixels);

    //  将Android传过来的RGBA类型的图片转成RGB
    Mat img_rgb;
    cvtColor(img, img_rgb, COLOR_RGBA2RGB);

    // 取黄色掩膜
    Mat img_hsv, mask1, mask2, mask;
    cvtColor(img_rgb, img_hsv, COLOR_RGB2HSV);

    Scalar low_range1(11, 43, 46);
    Scalar high_range1(25, 255, 255);
    inRange(img_hsv, low_range1, high_range1, mask1);

    Scalar low_range2(26, 43, 46);
    Scalar high_range2(34, 255, 255);
    inRange(img_hsv, low_range2, high_range2, mask2);

    mask = mask2 + mask1;
    Mat img_yellow;
    bitwise_and(img_rgb, img_rgb, img_yellow, mask = mask);

    // 二值化
    Mat gray, gray_eq, blur, thresh;
    cvtColor(img_yellow, gray, COLOR_RGB2GRAY);
    equalizeHist(gray, gray_eq);
    GaussianBlur(gray_eq, blur, Size(5, 5), 0);

    adaptiveThreshold(blur, thresh, 255, ADAPTIVE_THRESH_MEAN_C, THRESH_BINARY, 71, 0);

    int num_labels;
    Mat labels, stats, centroids;
    num_labels = connectedComponentsWithStats(thresh, labels, stats, centroids);

    // 拼接前半段和后半段
    Mat new_stats, new_centroids;

    // 连通域合并判断
    int i = 1;
    while (i < num_labels) {
        int x, y, w, h, area;
        x = stats.at<int>(i, CC_STAT_LEFT);
        y = stats.at<int>(i, CC_STAT_TOP);
        w = stats.at<int>(i, CC_STAT_WIDTH);
        h = stats.at<int>(i, CC_STAT_HEIGHT);
        area = stats.at<int>(i, CC_STAT_AREA);
        float bili = static_cast<float>(w) / static_cast<float>(h);

        if (static_cast<float>(w) / static_cast<float>(h) >= 4 && h <= 80)
        {
            int j = i + 1;
            while (j < num_labels)
            {
                int xj = stats.at<int>(j, CC_STAT_LEFT);
                int yj = stats.at<int>(j, CC_STAT_TOP);
                int wj = stats.at<int>(j, CC_STAT_WIDTH);
                int hj = stats.at<int>(j, CC_STAT_HEIGHT);
                int area_j = stats.at<int>(j, CC_STAT_AREA);
                int dist = abs(y + h - yj);

                if (area_j > 0 && static_cast<float>(wj) / static_cast<float>(hj) >= 4 && hj <= 80)
                {
                    if (dist < 40)
                    {
                        // 合并连通域
                        int xb_i = stats.at<int>(i, CC_STAT_LEFT) + stats.at<int>(i, CC_STAT_WIDTH);
                        int yb_i = stats.at<int>(i, CC_STAT_TOP) + stats.at<int>(i, CC_STAT_HEIGHT);
                        int xb_j = stats.at<int>(j, CC_STAT_LEFT) + stats.at<int>(j, CC_STAT_WIDTH);
                        int yb_j = stats.at<int>(j, CC_STAT_TOP) + stats.at<int>(j, CC_STAT_HEIGHT);
                        stats.at<int>(i, CC_STAT_LEFT) = std::min(stats.at<int>(i, CC_STAT_LEFT), stats.at<int>(j, CC_STAT_LEFT));
                        stats.at<int>(i, CC_STAT_TOP) = std::min(stats.at<int>(i, CC_STAT_TOP), stats.at<int>(j, CC_STAT_TOP));
                        stats.at<int>(i, CC_STAT_WIDTH) = std::max(xb_i, xb_j) - stats.at<int>(i, CC_STAT_LEFT);
                        stats.at<int>(i, CC_STAT_HEIGHT) = std::max(yb_i, yb_j) - stats.at<int>(i, CC_STAT_TOP);
                        stats.at<int>(i, CC_STAT_AREA) = stats.at<int>(i, CC_STAT_WIDTH) * stats.at<int>(i, CC_STAT_HEIGHT);
                        centroids.at<double>(i, 0) = (centroids.at<double>(i, 0) * stats.at<int>(i, CC_STAT_AREA) + centroids.at<double>(j, 0) * stats.at<int>(j, CC_STAT_AREA)) / (stats.at<int>(i, CC_STAT_AREA) + stats.at<int>(j, CC_STAT_AREA));
                        centroids.at<double>(i, 1) = (centroids.at<double>(i, 1) * stats.at<int>(i, CC_STAT_AREA) + centroids.at<double>(j, 1) * stats.at<int>(j, CC_STAT_AREA)) / (stats.at<int>(i, CC_STAT_AREA) + stats.at<int>(j, CC_STAT_AREA));

                        // 更新当前匹配
                        x = stats.at<int>(i, CC_STAT_LEFT);
                        y = stats.at<int>(i, CC_STAT_TOP);
                        w = stats.at<int>(i, CC_STAT_WIDTH);
                        h = stats.at<int>(i, CC_STAT_HEIGHT);
                        area = stats.at<int>(i, CC_STAT_AREA);

//                        // 删除被合并的连通域信息
//                        cv::vconcat(stats.rowRange(0, j), stats.rowRange(j + 1, stats.rows), new_stats);
//                        cv::vconcat(centroids.rowRange(0, j), centroids.rowRange(j + 1, centroids.rows), new_centroids);
//
//                        // 替换原始矩阵
//                        stats = new_stats.clone();
//                        centroids = new_centroids.clone();
//                        num_labels -= 1;
//                        j--;
                    }
                    j++;
                }
                else
                {
//                    if (j < num_labels - 1)
//                    {
//                        cv::vconcat(stats.rowRange(0, j), stats.rowRange(j + 1, stats.rows), new_stats);
//                        cv::vconcat(centroids.rowRange(0, j), centroids.rowRange(j + 1, centroids.rows), new_centroids);
//
//                        // 替换原始矩阵
//                        stats = new_stats.clone();
//                        centroids = new_centroids.clone();
//                        num_labels -= 1;
//                    }
//                    else {
//                        j++;
//                    }
                    j++;
                }
            }
            i++;
        }
        else
        {
//            if (i < num_labels - 1) {
//                cv::vconcat(stats.rowRange(0, i), stats.rowRange(i + 1, stats.rows), new_stats);
//                cv::vconcat(centroids.rowRange(0, i), centroids.rowRange(i + 1, centroids.rows), new_centroids);
//
//                // 替换原始矩阵
//                stats = new_stats.clone();
//                centroids = new_centroids.clone();
//                num_labels -= 1;
//            }
//            else
//            {
//                i++;
//            }
            i++;
        }
    }

    // 查找最大合并连通域
    int flag = 1;
    for (int i = 1; i < num_labels; i++)
    {
        if (stats.at<int>(i, CC_STAT_AREA) >= stats.at<int>(flag, CC_STAT_AREA))
        {
            flag = i;
        }
    }

    // 获取裁剪区域左上角在原始图像中的坐标
    int cut_x = stats.at<int>(flag, CC_STAT_LEFT);
    int cut_y = stats.at<int>(flag, CC_STAT_TOP);
    int cut2_x = stats.at<int>(flag, CC_STAT_WIDTH) + cut_x;
    int cut2_y = stats.at<int>(flag, CC_STAT_HEIGHT) + cut_y;

    Mat img_cut = img(Range(cut_y, cut2_y), Range(cut_x, cut2_x));

    // 将左上角和右下角坐标返回给Java代码
    jintArray ret_array = env->NewIntArray(4);
    jint data[4] = {cut_x, cut_y, cut2_x, cut2_y};
    env->SetIntArrayRegion(ret_array, 0, 4, data);

    return ret_array;
}