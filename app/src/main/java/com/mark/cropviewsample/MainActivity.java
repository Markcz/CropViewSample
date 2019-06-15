package com.mark.cropviewsample;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import com.mark.cropview.CropView;
import java.io.File;

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

        cropView.setImageResource(R.drawable.crop_2048x1242);
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
            new CropAsyncTask().execute(cropView.crop());
        } else {
            PermissionHelper.requestStoragePermission(this, REQUEST_PERMISSION_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_PERMISSION_CODE
                && grantResults.length > 1
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            new CropAsyncTask().execute(cropView.crop());
        } else {
            Toast.makeText(this, "缺少存储权限", Toast.LENGTH_LONG);
        }
    }


    class CropAsyncTask extends AsyncTask<Bitmap, Void, File> {

        @Override
        protected File doInBackground(Bitmap... bitmaps) {
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).getAbsolutePath()
                    , System.currentTimeMillis() + ".jpg");
            cropView.flushToFile(bitmaps[0], file, 100, Bitmap.CompressFormat.JPEG);
            return file;
        }

        @Override
        protected void onPostExecute(File file) {
            super.onPostExecute(file);
            if (!isFinishing() && file != null && file.exists()){
                Intent intent = new Intent(MainActivity.this,PreviewActivity.class);
                intent.putExtra("preview",file.getAbsolutePath());
                startActivity(intent);
            }
        }
    }
}
