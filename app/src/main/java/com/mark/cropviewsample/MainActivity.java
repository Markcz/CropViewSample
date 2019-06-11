package com.mark.cropviewsample;

import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.mark.cropview.CropView;

import java.io.File;
import java.io.OutputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    static final int REQUEST_PERMISSION_CODE = 1;
    CropView cropView;
    TextView tvCrop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {

        tvCrop = findViewById(R.id.tv_crop);
        cropView = findViewById(R.id.crop_view);

        cropView.setImageResource(R.drawable.crop_720x1280);
        tvCrop.setOnClickListener(this);

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.tv_crop:
                crop();
                break;
        }
    }

    private void crop() {
        if (PermissionHelper.checkStoragePermission(this)) {
            flushToFile(cropView.crop(),
                    Bitmap.CompressFormat.JPEG,
                    100,
                    new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath()
                            , "crop_"+System.currentTimeMillis() + ".jpg"));
        }else {
            PermissionHelper.requestStoragePermission(this,REQUEST_PERMISSION_CODE);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_CODE
                && grantResults.length > 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            flushToFile(cropView.crop(),
                    Bitmap.CompressFormat.JPEG,
                    100,
                    new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath()
                            , System.currentTimeMillis() + ".jpg"));
        }else {
            Toast.makeText(this,"缺少存储权限",Toast.LENGTH_LONG);
        }
    }



    private final static ExecutorService EXECUTOR_SERVICE = Executors.newCachedThreadPool();

    public Future<Void> flushToFile(final Bitmap bitmap,
                                    final Bitmap.CompressFormat format,
                                    final int quality,
                                    final File file) {

        return EXECUTOR_SERVICE.submit(new Runnable() {
            @Override
            public void run() {
                cropView.flushToFile(bitmap, file, quality, format);
            }
        }, null);
    }
}
