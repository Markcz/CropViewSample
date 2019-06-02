package com.mark.cropviewsample;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import com.mark.cropview.CropView;

public class MainActivity extends AppCompatActivity {

    CropView cropView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();
    }

    private void init() {
        cropView = findViewById(R.id.crop_view);
        cropView.setImageResource(R.drawable.crop);
    }
}
