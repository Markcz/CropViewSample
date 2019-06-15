package com.mark.cropviewsample;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;

public class PreviewActivity extends AppCompatActivity {

    ImageView ivPreview;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);
        String path = getIntent().getStringExtra("preview");
        if (path == null){
            return;
        }
        ivPreview = findViewById(R.id.iv_preview);
        Bitmap bitmap = BitmapFactory.decodeFile(path);
        ivPreview.setImageBitmap(bitmap);
    }
}
