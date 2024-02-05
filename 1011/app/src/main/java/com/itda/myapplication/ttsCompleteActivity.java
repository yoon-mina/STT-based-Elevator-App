package com.itda.myapplication;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.TextView;

public class ttsCompleteActivity extends AppCompatActivity {
    String floor;//말한 층수 값
    TextView floor_result;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tts_complete);

        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        floor_result =findViewById(R.id.floor_result);
        Intent intent = getIntent();
        floor=intent.getStringExtra("floor");
        floor_result.setText(floor);

        Handler handler = new Handler();
        handler.postDelayed(new Runnable(){
            public void run(){
                Intent intent=new Intent(getApplicationContext(),MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }//run()
        },100);
    }//onCreate
}//end class