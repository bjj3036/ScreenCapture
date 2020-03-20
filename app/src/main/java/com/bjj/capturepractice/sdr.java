//package com.bjj.capturepractice;
//
//import android.app.ActivityManager;
//import android.content.Context;
//import android.content.Intent;
//import android.graphics.Bitmap;
//import android.graphics.PixelFormat;
//import android.graphics.Point;
//import android.hardware.display.DisplayManager;
//import android.hardware.display.VirtualDisplay;
//import android.media.Image;
//import android.media.ImageReader;
//import android.media.projection.MediaProjection;
//import android.media.projection.MediaProjectionManager;
//import android.os.Environment;
//import android.os.Handler;
//import android.os.Looper;
//import android.os.Bundle;
//import android.util.DisplayMetrics;
//import android.view.Display;
//import android.view.OrientationEventListener;
//import android.view.Surface;
//import android.view.SurfaceHolder;
//import android.view.SurfaceView;
//import android.view.View;
//import android.widget.Button;
//import android.widget.Toast;
//
//import androidx.appcompat.app.AppCompatActivity;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.nio.ByteBuffer;
//
//public class MainActivity extends AppCompatActivity {
//    private static final String TAG = "ScreenTestCapture";
//    private static final int REQUEST_CODE = 100;
//    private static String STORE_DIRECTORY;
//    private static int IMAGES_PRODUCED;
//    private static final String SCREENCAP_NAME = "screencap";
//    private static final int VIRTUAL_DISPLAY_FLAGS = DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY | DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
//    private static MediaProjection sMediaProjection;
//
//    private MediaProjectionManager mProjectionManager;
//    private ImageReader mImageReader;
//    private Handler mHandler;
//    private Display mDisplay;
//    private VirtualDisplay mVirtualDisplay;
//    private int mDensity;
//    private int mWidth;
//    private int mHeight;
//    private int mRotation;
//    private OrientationChangeCallback mOrientationChangeCallback;
//
//    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
//        @Override
//        public void onImageAvailable(ImageReader imageReader) {
//            Image image = null;
//            FileOutputStream fos = null;
//            Bitmap bitmap = null;
//
//            try {
//                //가장 최신 이미지를 가져 옵니다. image 객체로
//                image = mImageReader.acquireLatestImage();
//                if (image != null) {
//                    Image.Plane[] planes = image.getPlanes();
//                    ByteBuffer buffer = planes[0].getBuffer();
//                    int pixelStride = planes[0].getPixelStride();
//                    int rowStride = planes[0].getRowStride();
//                    int rowPadding = rowStride - pixelStride * mWidth;
//
//                    //쨋든 createBitmap으로 bitmap파일 만들고 위의 이미지 buffer로 이미지를 가져옵니다.
//                    bitmap = Bitmap.createBitmap(mWidth + rowPadding / pixelStride, mHeight, Bitmap.Config.ARGB_8888);
//                    bitmap.copyPixelsFromBuffer(buffer);
//
//                    //그 다음 저장하는 부분
//                    fos = new FileOutputStream(STORE_DIRECTORY + "/myscreen_" + IMAGES_PRODUCED + ".jpg");
//                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
//
//                    IMAGES_PRODUCED++;
//                }
//            } catch (Exception e) {
//                e.printStackTrace();
//            } finally {
//                if (fos != null) {
//                    try {
//                        fos.close();
//                    } catch (IOException ioe) {
//                        ioe.printStackTrace();
//                    }
//                }
//
//                if (bitmap != null) {
//                    bitmap.recycle();
//                }
//
//                if (image != null) {
//                    image.close();
//                }
//            }
//        }
//    }
//
//    /*
//    제가보기엔 rotation시 사용할 클래스 인것 같습니다.말고도 추가적으로 change 되는 부분이 있나
//    정위로..?
//    */
//    private class OrientationChangeCallback extends OrientationEventListener {
//        //생성자가 필히 요구 됩니다.
//        public OrientationChangeCallback(Context context) {
//            super(context);
//        }
//
//        @Override
//        public void onOrientationChanged(int orientation) {
//            synchronized (this) {
//                //화면 전환으로 인해서 virtualdisplay를 새로만드는 과정이니 동기화 시켜주고
//                final int rotation = mDisplay.getRotation();
//
//                //rotation값이다르다면
//                if (rotation != mRotation) {
//                    mRotation = rotation;
//                    try {
//                        if (mVirtualDisplay != null) mVirtualDisplay.release();
//                        if (mImageReader != null)
//                            mImageReader.setOnImageAvailableListener(null, null);
//
//                        createVirtualDisplay();
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }
//    }
//
//    private class MediaProjectionStopCallback extends MediaProjection.Callback {
//        @Override
//        public void onStop() {
//            mHandler.post(new Runnable() {
//                @Override
//                public void run() {
//                    if (mVirtualDisplay != null) mVirtualDisplay.release();
//                    if (mImageReader != null) mImageReader.setOnImageAvailableListener(null, null);
//                    if (mOrientationChangeCallback != null) mOrientationChangeCallback.disable();
//                    sMediaProjection.unregisterCallback(MediaProjectionStopCallback.this);
//                }
//            });
//        }
//    }
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_main);
//
//        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
//        Button startButton;
//        startButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                startProjection();
//            }
//        });
//
//        Button stopButton;
//        stopButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                stopProjection();
//            }
//        });
//        new Thread() {
//            @Override
//            public void run() {
//                Looper.prepare();
//                mHandler = new Handler();
//                Looper.loop();
//            }
//        }.start();
//    }
//
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        if (requestCode == REQUEST_CODE) {
//            sMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
//
//            if (sMediaProjection != null) {
//                STORE_DIRECTORY = Environment.getExternalStorageDirectory() + "/data/capturetest/";
//                File storeDirectory = new File(STORE_DIRECTORY);
//                if (!storeDirectory.exists()) {
//                    boolean success = storeDirectory.mkdirs();
//                    if (!success) {
//                        return;
//                    }
//                }
//            }
//
///*
//현재 디스플레이의 density dpi 가져 옵니다.
//*/
//            DisplayMetrics metrics = getResources().getDisplayMetrics();
//            mDensity = metrics.densityDpi;
//            mDisplay = getWindowManager().getDefaultDisplay();
//
///*
//그리고 나서 createVirtualDisplay() 호출해서 virtualdisplay를 만듭니다.
//*/
//            createVirtualDisplay();
//
//            //getMdiaProjection으로 가져온 object에 이제register 등록을 해줍니다. 핸들러와 함께
//            //흠... mHandler를 여기서등록 시켜주면, 흠..null값을 줘도 된다고 developer에 나와있습니다.
//            //looper를 호출 할 필요가 있다면 핸들러를 넣으려는데 아직도 handler를 쓴 이유를 잘 모르겠네요.
//            sMediaProjection.registerCallback(new MediaProjectionStopCallback(), mHandler);
//        }
//    }
//
//    private void startProjection() {
//        //사용자 허가 요청!
//        startActivityForResult(mProjectionManager.createScreenCaptureIntent(), REQUEST_CODE);
//    }
//
//    private void stopProjection() {
//        //projection을 종료 합니다. stop()! mediaprojection callback의
//        //onstop method가 호출 되겠네요.
//        mHandler.post(new Runnable() {
//            @Override
//            public void run() {
//                if (sMediaProjection != null) {
//                    sMediaProjection.stop();
//                }
//            }
//        });
//    }
//
//    //가상 디스플레이를 만듭니다.
//    private void createVirtualDisplay() {
//        //가로,세로 고려 사이즈는 다시 설정하고
//        Point size = new Point();
//        mDisplay.getSize(size);
//        mWidth = size.x;
//        mHeight = size.y;
//
//        //ImageReader 새로운 사이즈의 인스턴스 만들고, createVirtualDisplay로 생ㅇ성 합니다.
//        //하고 이미지 처리할 ImageAvailableListener를 등록 해줍니다.
//        mImageReader = ImageReader.newInstance(mWidth, mHeight, PixelFormat.RGBA_8888, 2);
//        mVirtualDisplay = sMediaProjection.createVirtualDisplay(SCREENCAP_NAME, mWidth, mHeight, mDensity, VIRTUAL_DISPLAY_FLAGS, mImageReader.getSurface(), null, mHandler);
//        mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), mHandler);
//    }
//}