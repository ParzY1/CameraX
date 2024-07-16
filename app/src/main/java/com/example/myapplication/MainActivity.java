package com.example.myapplication;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.CameraXConfig;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity implements CameraXConfig.Provider {
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private static final int MY_CAMERA_PERMISSION_CODE = 2;
    private ImageCapture imageCapture = null;
    private Camera camera;
    public static final @NonNull CameraSelector DEFAULT_FRONT_CAMERA = CameraSelector.DEFAULT_FRONT_CAMERA;
    private File outputDirectory;
    private ExecutorService cameraExecutor;
    Button camera_capture_button;
    private final String TAG = "CameraXBasic";
    private final String FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS";
    private final int REQUEST_CODE_PERMISSIONS = 10;
    private String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};
    PreviewView previewView;
    Button rotate_button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        camera_capture_button = findViewById(R.id.camera_capture_button);
        rotate_button = findViewById(R.id.camera_rotate_button);
        previewView = findViewById(R.id.previewView);
        if (allPermissionsGranted()){
            startCamera();
        }else {
            ActivityCompat.requestPermissions(this,REQUIRED_PERMISSIONS,REQUEST_CODE_PERMISSIONS);
        }
        camera_capture_button.setOnClickListener(view -> takePhoto());

        outputDirectory = getOutputDirectory();
        cameraExecutor = Executors.newSingleThreadExecutor();
        if (rotate_button != null){
            rotate_button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    facingFront();
                }
            });
        }
    }
    private boolean allPermissionsGranted(){
        if (getApplicationContext().checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.CAMERA},MY_CAMERA_PERMISSION_CODE);
        }
        return true;
    }
    private void facingFront(){
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_FRONT).build();

        Toast.makeText(this, "done", Toast.LENGTH_SHORT).show();
    }
    private void facingBack(){
        CameraSelector rotate = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();
    }

    private void startCamera(){

        cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder()
                        .build();


                CameraSelector rotate = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK).build();


                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();
                Log.v(TAG,"startCamera");

                try {
                    cameraProvider.unbindAll();
                    camera = cameraProvider.bindToLifecycle((LifecycleOwner) this,rotate,preview,imageCapture);

                }catch (Exception e){
                    Log.e(TAG, "Use case binding failed 1" + e);
                }
            }catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Use case binding failed" + e);
            }
        }, ContextCompat.getMainExecutor(this));
    }
    private void takePhoto(){
        ImageCapture imageCapture = this.imageCapture;
        File file = null;
        try{
            file = File.createTempFile(
                    new SimpleDateFormat(FILENAME_FORMAT, Locale.US
                    ).format(System.currentTimeMillis()), ".jpg", this.outputDirectory);
            Log.v(TAG,"nazwa pliku: " + file.toString());
        }catch (IOException e){
            e.printStackTrace();
        }
        Log.v(TAG,"start takePhoto()");
        File photoFile = file;
        ImageCapture.OutputFileOptions outputFileOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();
        Log.v(TAG,"output" + outputFileOptions);
        imageCapture.takePicture(outputFileOptions,ContextCompat.getMainExecutor((Context) this),
                (new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri savedUri = Uri.fromFile(photoFile);
                        String msg = "Photo capture succeeded"+savedUri;
                        Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_SHORT).show();
                        Log.d(TAG,msg);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG,"Photo capture failed" + exception.getMessage());
                    }
                }
                ));
    }
    private File getOutputDirectory(){
        return getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        //executorService.shutdown();
    }
    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        File storageDir = Environment.getExternalStorageDirectory();
        File image = File.createTempFile(
                "example",  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }
    private void galleryAddPic() {
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File f = new File(mCurrentPhotoPath);
        Uri contentUri = Uri.fromFile(f);
        mediaScanIntent.setData(contentUri);
        this.sendBroadcast(mediaScanIntent);
    }
    @NonNull
    @Override
    public CameraXConfig getCameraXConfig() {
        return null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_CAMERA_PERMISSION_CODE){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                Toast.makeText(getApplicationContext(),"camera permission granted", Toast.LENGTH_LONG).show();
                startCamera();
            }else {
                Toast.makeText(getApplicationContext(),"camera permission denied", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }
}