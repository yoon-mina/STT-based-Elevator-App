package com.itda.myapplication;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;


public class IntroActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        MediaPlayer player = MediaPlayer.create(this,R.raw.ddingdong);
        player.start();

        Handler handler = new Handler();
        handler.postDelayed(new Runnable(){
            public void run(){
                Intent intent=new Intent(getApplicationContext(), MainActivity.class);
                startActivity(intent);
            }//run()
        },100);
    }//endonCreate()

    protected void onPause(){
        super.onPause();
        finish();
    }//onPause()
}