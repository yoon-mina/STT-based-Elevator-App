package com.itda.myapplication;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.media.Image;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import static android.speech.tts.TextToSpeech.ERROR;

import android.os.Bundle;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Locale;

public class ttsActivity extends AppCompatActivity {
    private TextToSpeech tts;
    private static final int REQUEST_CODE = 1234;
    ArrayList<String> text;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tts);

        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        String ttsGuide="도착할 층수를 말해주세요";

        // TTS를 생성하고 OnInitListener로 초기화 한다.
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != ERROR) {
                    tts.setLanguage(Locale.KOREAN);
                }//if
            }//onInits
        });
        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                tts.speak(ttsGuide,TextToSpeech.QUEUE_FLUSH, null);

            }//run
        },500);

        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(isConnected()){
                    Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                    startActivityForResult(intent, REQUEST_CODE);
                }
                else{
                    Toast.makeText(getApplicationContext(), "Plese Connect to Internet", Toast.LENGTH_LONG).show();
                }
            }//run
        },2700);
    }//onCreate

   @Override
    protected void onDestroy() {
        super.onDestroy();
        // TTS 객체가 남아있다면 실행을 중지하고 메모리에서 제거한다.
        if(tts != null){
            tts.stop();
            tts.shutdown();
            tts = null;
        }
    }

    public  boolean isConnected()
    {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo net = cm.getActiveNetworkInfo();
        if (net!=null && net.isAvailable() && net.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            ((MainActivity)MainActivity.context_main).sendData(text.toString());
            Intent intent = new Intent(
                    getApplicationContext(), // 현재 화면의 제어권자
                    ttsCompleteActivity.class); // 다음 넘어갈 클래스 지정
            intent.putExtra("floor",text.toString());
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}//endclass