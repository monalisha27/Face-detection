

package com.camerafacedetection.camerafacedetection.Activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.media.Image;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.camerafacedetection.camerafacedetection.Helper.BitmapUtils;
import com.camerafacedetection.camerafacedetection.Helper.CapturePhotoUtils;
import com.camerafacedetection.camerafacedetection.R;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;

public class CamraXCaptureActivity extends AppCompatActivity {
    final int CARMERA_PERMISSION_CODE = 24;
    Button capture_btn;
    ImageView camera_switch_btn, flash_light_btn;
    PreviewView previewView;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private CameraSelector cameraSelector;
    private Preview preview;
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private ImageAnalysis imageAnalysis;
    FaceDetector detector;
    ImageCapture imageCapture;
    ExecutorService executorService;
    Bitmap mainBitmap;
    Face face;
    int countSuc = 0, count = 10, prog_count = 0;
    ProgressBar pb;
    Runnable runnable;
    private ProgressBar progress_bar;
    ArrayList<String> imageUriArrayList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camra_xcapture);
        capture_btn = findViewById(R.id.capture_btn);
        camera_switch_btn = findViewById(R.id.camera_switch_btn);
        flash_light_btn = findViewById(R.id.flash_light_btn);
        previewView = findViewById(R.id.cameraPreview);
        progress_bar = findViewById(R.id.progress_bar);
        progress_bar.setMax(100);
        progress_bar.setProgress(0);
        executorService = Executors.newSingleThreadExecutor();
        imageUriArrayList = new ArrayList<>();
        initializeFun();
    }

    private void initializeFun() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PERMISSION_GRANTED) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE}, CARMERA_PERMISSION_CODE);
        }

        runnable = new Runnable() {
            @Override
            public void run() {
                Rect rect = face.getBoundingBox();
                Bitmap faceCrop = Bitmap.createBitmap(mainBitmap, rect.left, rect.top, rect.width(), rect.height());
                String uri = CapturePhotoUtils.insertImage(CamraXCaptureActivity.this.getContentResolver(), faceCrop, "Face_" + countSuc);
                imageUriArrayList.add(uri);
                capture_btn.setText(String.valueOf(--count));
                prog_count = prog_count + 10;
                progress_bar.setProgress(prog_count);
            }
        };
        camera_switch_btn.setOnClickListener(new View.OnClickListener() {
            @SuppressLint("RestrictedApi")
            @Override
            public void onClick(View view) {
                if (cameraSelector.getLensFacing() == CameraSelector.LENS_FACING_BACK) {
                    cameraSelector = new CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                            .build();
                    bindPreview(cameraProvider);
                } else {
                    cameraSelector = new CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build();
                    bindPreview(cameraProvider);
                }
            }
        });
        flash_light_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(CamraXCaptureActivity.this, "clickckc flash", Toast.LENGTH_SHORT).show();
                if (camera.getCameraInfo().getTorchState().getValue() > 0) {
                    camera.getCameraControl().enableTorch(false);
                } else {
                    camera.getCameraControl().enableTorch(true);
                }

            }
        });
    }

    private void tackPhoto() {

       /* imageView.setImageBitmap(previewView.getBitmap());
        imageView.setVisibility(View.VISIBLE);
        File file = new File(Environment.getExternalStorageDirectory(), "${System.currentTimeMillis()}.jpg");
        if (!file.exists()) {
            file.mkdir();
        }
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "NEW_IMAGE");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        ImageCapture.OutputFileOptions options = new ImageCapture.OutputFileOptions.Builder(
                getContentResolver(),
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues).build();

        imageCapture.takePicture(options, executorService, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull @NotNull ImageCapture.OutputFileResults outputFileResults) {

                Thread thread = new Thread() {
                    public void run() {
                        Looper.prepare();

                        final Handler handler = new Handler();
                        handler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                // Do Work
                                Uri uri = outputFileResults.getSavedUri();
                                try {
                                    if (uri != null) {
                                        Intent intent = new Intent(CamraXCaptureActivity.this, MainActivity.class);
                                        intent.putExtra("bit", faceCrop );
                                        startActivity(intent);
                                        finish();
                                    }
                                } catch (Exception e) {
                                    //handle exception
                                }
                                Log.e("ssss", uri.getPath());
                                Toast.makeText(CamraXCaptureActivity.this, "uri.getPath()", Toast.LENGTH_SHORT).show();
                                handler.removeCallbacks(this);
                                Looper.myLooper().quit();
                            }
                        }, 2000);

                        Looper.loop();
                    }
                };
                thread.start();
            }

            @Override
            public void onError(@NonNull @NotNull ImageCaptureException exception) {
                Toast.makeText(CamraXCaptureActivity.this, "not Saved", Toast.LENGTH_SHORT).show();
            }
        });*/
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                preview = new Preview.Builder().build();
                imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();
                // Real-time contour detection
                FaceDetectorOptions realTimeOpts = new FaceDetectorOptions.Builder()
                        .setContourMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
                        .build();
                detector = FaceDetection.getClient(realTimeOpts);
                imageAnalysis.setAnalyzer(executorService, new YourAnalyzer());
                cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                        .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                        .setTargetName("CameraConference")
                        .build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageAnalysis, imageCapture);
        if (camera.getCameraInfo().hasFlashUnit()) {
            flash_light_btn.setVisibility(View.VISIBLE);
        } else {
            flash_light_btn.setVisibility(View.GONE);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @org.jetbrains.annotations.NotNull String[] permissions, @NonNull @org.jetbrains.annotations.NotNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PERMISSION_GRANTED) {
            startCamera();
        } else {
            Toast.makeText(this, "Please Accept the Camera Permission", Toast.LENGTH_SHORT).show();
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CARMERA_PERMISSION_CODE);
        }
    }

    private class YourAnalyzer implements ImageAnalysis.Analyzer {
        @Override
        public void analyze(ImageProxy imageProxy) {
            @SuppressLint("UnsafeOptInUsageError") Image mediaImage = imageProxy.getImage();
            if (mediaImage != null) {
                InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());
                Task<List<Face>> result = detector.process(image).addOnSuccessListener(new OnSuccessListener<List<Face>>() {
                    @SuppressLint("UnsafeOptInUsageError")
                    @Override
                    public void onSuccess(List<Face> faces) {
                        countSuc = countSuc + 1;
                        for (Face f : faces) {
                            if (countSuc % 10 == 0) {
                                if (count > 0) {
                                    face = f;
                                    mainBitmap = BitmapUtils.getBitmap(imageProxy); //bytebuffer, framedata
                                    runnable.run();
                                }
                                if (count == 0) {
                                    convertImageToBase64();
                                    count = -1;
                                    Dialog dialog = new Dialog(CamraXCaptureActivity.this);
                                    dialog.setContentView(R.layout.ten_pics_store_done);
                                    dialog.setCancelable(false);
                                    Window window = dialog.getWindow();
                                    window.setLayout(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                                    Button closeBtn = dialog.findViewById(R.id.closeBtn);
                                    closeBtn.setOnClickListener(new View.OnClickListener() {
                                        @Override
                                        public void onClick(View view) {
                                            dialog.dismiss();
                                            finish();
                                        }
                                    });
                                    dialog.show();
                                }
                            }
                        }
                        imageProxy.close();
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        imageProxy.close();
                    }
                });

            }
        }
    }

    private void convertImageToBase64(){
        int imageExistCount=0;
        for (int i = 0; i < imageUriArrayList.size(); i++) {
            String imagePath=getPath(Uri.parse(imageUriArrayList.get(i)));
            if (!Objects.equals(imagePath, "") && !Objects.equals(imagePath, null)) {
                Bitmap bm = BitmapFactory.decodeFile(imagePath);
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.JPEG, 100, baos); // bm is the bitmap object
                byte[] byteArrayImage = baos.toByteArray();
                String encodedImage = Base64.encodeToString(byteArrayImage, Base64.DEFAULT);
                Log.e("Base64 image " + i + "=", encodedImage);
                Toast.makeText(this, encodedImage, Toast.LENGTH_SHORT).show();
                imageExistCount = imageExistCount + 1;
            }
            if (i==9){
                Log.e("Toatal Images = ", String.valueOf(imageExistCount));
                Toast.makeText(this, String.valueOf(imageExistCount), Toast.LENGTH_SHORT).show();
            }
        }
    }

    public String getPath(Uri uri) {
        String path="";
        try{
            String[] projection = { MediaStore.Images.Media.DATA };
            Cursor cursor = managedQuery(uri, projection, null, null, null);
            startManagingCursor(cursor);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            path= cursor.getString(column_index);
        }catch (Exception exception){
            Log.e("Image "+uri,exception.toString());
        }
       return path;
    }

}//end main Class