package com.travis.listviewwithlrucache;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

/**
 * Created by yutao on 2018/8/14.
 */

public class BigImageActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 使状态栏透明，产生沉浸式状态栏的效果
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);

        setContentView(R.layout.big_image);

        Intent intent = getIntent();
        String url = intent.getStringExtra("image");

        //Bitmap bitmap = BitmapFactory.decodeFile(url);

        ImageView imageView = findViewById(R.id.big_image);
        //imageView.setImageBitmap(bitmap);

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ActivityCompat.finishAfterTransition(BigImageActivity.this);
            }
        });

        Glide.with(this).load(url).into(imageView);
    }
}
