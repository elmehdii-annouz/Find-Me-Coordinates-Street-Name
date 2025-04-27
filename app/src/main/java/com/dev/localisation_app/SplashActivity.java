package com.dev.localisation_app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Find the ImageView
        ImageView splashLogo = findViewById(R.id.splash_logo);

        // Load the animation
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.splash_logo_animation);

        // Start the animation
        splashLogo.startAnimation(animation);

        // Delay for the splash screen before transitioning to the next activity
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(SplashActivity.this, SplashActivity.class);
            startActivity(intent);
            finish();
        }, 90000); // 2500 milliseconds = 2.5 seconds to allow full animation
    }
}

