package com.example.lab3;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageDecoder;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;



public class MainActivity extends AppCompatActivity {
    ImageView selectedImage;
    Button cameraBtn, galleryBtn;
    static final int REQUEST_IMAGE_CAPTURE = 1;
    Mat m;
    private static final String    TAG                 = "OCVSample::Activity";
    static{ System.loadLibrary("opencv_java3"); }

    public MainActivity()
    {
        if (!OpenCVLoader.initDebug())
        {
            System.out.println("GG");
        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        OpenCVLoader.initDebug();

        selectedImage =findViewById(R.id.displayImageView);
        cameraBtn =findViewById(R.id.cameraBtn);
        galleryBtn =findViewById(R.id.galleryBtn);

        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                askCameraPermissions();
            }
        });

        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Gallery bnt is clicked", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void askCameraPermissions() {
        if(ContextCompat.checkSelfPermission(this,  Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, 101);
        }else {
            openCamera();
        }
    }


    public void  onRequestPermissionResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        if(requestCode == 101 ){
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                openCamera();
            }else{
                Toast.makeText(this,"Camera Permission is required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openCamera() {

        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }
    public static Bitmap overlayBitmapToCenter(Bitmap bitmap1, Bitmap bitmap2) {
        int bitmap1Width = bitmap1.getWidth();
        int bitmap1Height = bitmap1.getHeight();
        int bitmap2Width = bitmap2.getWidth();
        int bitmap2Height = bitmap2.getHeight();

        float marginLeft = (float) (bitmap1Width * 0.5 - bitmap2Width * 0.5);
        float marginTop = (float) (bitmap1Height * 0.5 - bitmap2Height * 0.5);

        //создаем пустой битмап с размерами как 1-й битмап
        Bitmap overlayBitmap = Bitmap.createBitmap(bitmap1Width, bitmap1Height, bitmap1.getConfig());
        //создаем canvas
        Canvas canvas = new Canvas(overlayBitmap);
        //наносим на canvas 1-й битмап
        canvas.drawBitmap(bitmap1, new Matrix(), null);
        //сверху наносим 2-й битмап (по центру)
        canvas.drawBitmap(bitmap2, marginLeft, marginTop, null);
        //возвращаем итоговый битмап
        return overlayBitmap;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        System.out.println("onActivityResult");
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap image = (Bitmap) extras.get("data");
            int width = image.getWidth();
            int height = image.getHeight();
            Bitmap overlayBitmap = Bitmap.createBitmap(width, height, image.getConfig());

            selectedImage.setImageBitmap(image);


            //int x = image.getHeight();
            //int y = image.getWidth();

            //int color = image.getPixel(x-1,y-1);
            //int color = image.getPixel(x, y);
            //int a = Color.alpha(color);
            //int r = Color.red(color);
            //int g = Color.green(color);
            //int b = Color.blue(color);
            //float[] hsv = new float[3];
            //Color.RGBToHSV(r, g, b, hsv);
            //color = Color.HSVToColor(a, hsv);//перевод в HSV руками

            Mat roi = new Mat();
            Mat dst = new Mat();
            Utils.bitmapToMat(image, roi);
            Mat roiTmp = roi.clone();
            Mat mask=roi.clone();

            Mat mask1=roi.clone();
            Mat mask2=roi.clone();

            Imgproc.cvtColor(roi, roiTmp, Imgproc.COLOR_RGB2HSV);

            Scalar lower1 = new Scalar(40, 40, 40);
            Scalar upper1 = new Scalar(70, 255, 255);
            Scalar lower2 = new Scalar(36, 25, 25);
            Scalar upper2 = new Scalar(70, 255,255);

            Core.inRange(roiTmp,lower1,upper1,mask1);
            Core.inRange(roiTmp,lower2,upper2,mask2);
            Core.addWeighted( mask1, 1.0, mask2, 1.0, 0.0, dst);
            Utils.matToBitmap(dst,overlayBitmap);

            Bitmap result = Bitmap.createBitmap(overlayBitmap.getWidth(), overlayBitmap.getHeight(), Bitmap.Config.ARGB_8888);
            Canvas tempCanvas = new Canvas(result);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DARKEN));
            tempCanvas.drawBitmap(image, 0, 0, null);
            tempCanvas.drawBitmap(overlayBitmap, 0, 0, paint);

            //Canvas canvas = new Canvas(overlayBitmap);
            //float marginLeft = (float) (width * 0.5 - width * 0.5);
            //float marginTop = (float) (height * 0.5 - height * 0.5);
            //canvas.drawBitmap(image, new Matrix(), null);
            //canvas.drawBitmap(overlayBitmap, marginLeft, marginTop, null);


            //roi.copyTo(dst,dst);
            //Core.inRange(roiTmp, new Scalar(40, 40, 40), new Scalar(70, 255, 255), roiTmp);
            //Imgproc.cvtColor(roiTmp, roiTmp, Imgproc.COLOR_RGB2HSV);

            //Core.addWeighted( mask, 0.5, roiTmp, 0.5, 0.0, dst);
            //double grayThres = Imgproc.threshold(roiTmp, mask, 0, 255, Imgproc.THRESH_BINARY_INV | Imgproc.THRESH_OTSU);
            //roiTmp.setTo(new Scalar(0,0,255), mask);

            //Imgproc.cvtColor(roiTmp, roi, Imgproc.COLOR_HSV2RGB);
            //Utils.matToBitmap(dst,image);
            ////////overlayBitmap=overlayBitmapToCenter(overlayBitmap,image);

            selectedImage.setImageBitmap(result);
        }
    }
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    m=new Mat();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };




}
