package com.itda.myapplication;

import static android.content.ContentValues.TAG;
import static android.speech.tts.TextToSpeech.ERROR;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;


import com.itda.myapplication.DatabaseHelper;
import com.itda.myapplication.MainActivity;
import com.itda.myapplication.R;

import java.util.ArrayList;
import java.util.Locale;

public class ArsGuide extends AppCompatActivity {
    SQLiteDatabase db;
    private String content;
    private TextToSpeech tts;
    private static final int REQUEST_CODE = 1234;
    ArrayList<String> text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ars_guide);

        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        DatabaseHelper databaseHelper = new DatabaseHelper(this, "DB", null, 1);
        db = databaseHelper.getWritableDatabase();

        String guide="음성안내 페이지입니다. 일번은 음성입력 이번은 혼잡도안내 삼번은 직접입력안내입니다." +
                " 원하시는 번호를 말해주세요.";

        // TTS를 생성하고 OnInitListener로 초기화 한다.
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != ERROR) {
                    // 언어를 선택한다.
                    tts.setLanguage(Locale.KOREAN);
                }//if
            }//onInits
        });

        new android.os.Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                tts.speak(guide,TextToSpeech.QUEUE_FLUSH, null);
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
        },11000);
    }//onCreate

   protected void onDestroy() {
        super.onDestroy();
        // TTS 객체가 남아있다면 실행을 중지, 메모리에서 제거
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

    @SuppressLint("Range")
    String getContent(String tableName, Integer id) {
        Cursor cursor = null;
        String content = null;
        try {
            String query = "select content from " + tableName
                    + " where id" + "= '"+ id +"'";
            cursor = db.rawQuery(query, null);
            cursor.moveToFirst(); // Cursor를 제일 첫행으로 이동
            content = cursor.getString(cursor.getColumnIndex("CONTENT"));
            return content;
        } finally {
            if (cursor != null) {
                cursor.close();
            }//if
        }//finally
    }//DBSearchs

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        int error=0;
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK)
        {
            text = null;
            text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            String text2=text.get(0);
            if (text2.equals("1번")||text2.equals("일번")||text2.equals("1")||text2.equals("일")){
                content = getContent("guide", 1);

            }
            else if (text2.equals("2번")||text2.equals("이번")||text2.equals("2")||text2.equals("이")) {
                content = getContent("guide", 2);
            } else if (text2.equals("3번")||text2.equals("삼번")||text2.equals("3")||text2.equals("삼")) {
                content = getContent("guide", 3);
            }
            else {
                content = "입력오류, 일이삼번중에 다시한번 말씀해주세요.";
                error=1;//입력오류
            }
            Log.d(TAG, "content:"+content);
            System.out.println(content);
            tts.speak(content,TextToSpeech.QUEUE_FLUSH, null);
        }//end if

        while(true)
        {
            if(!tts.isSpeaking())
            {
                if (error == 0)
                {
                    Intent intent = new Intent(
                            getApplicationContext(), // 현재 화면의 제어권자
                            MainActivity.class); // 다음 넘어갈 클래스 지정
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent); // 다음 화면으로 넘어간다
                    break;
                }//if
                else
                {
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
                    },100);
                }//입력오류
                break;
            }//말하는 게 끝나면
        }//반복문
        super.onActivityResult(requestCode, resultCode, data);
    }//onActivityResult

}//end class