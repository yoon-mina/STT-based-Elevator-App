package com.itda.myapplication;

import static android.speech.tts.TextToSpeech.ERROR;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static android.content.ContentValues.TAG;

public class MainActivity extends AppCompatActivity {
    public SQLiteDatabase db;
    private static final int REQUEST_ENABLE_BT = 10; // 블루투스 활성화 상태
    private static final int RESULT_CANCELDE = 9; // 블루투스 비활성화 상태
    private BluetoothAdapter bluetoothAdapter; // 블루투스 어댑터
    private Set<BluetoothDevice> devices; // 블루투스 디바이스 데이터 셋
    private BluetoothDevice bluetoothDevice; // 블루투스 디바이스
    private BluetoothSocket bluetoothSocket = null; // 블루투스 소켓
    private OutputStream outputStream = null; // 블루투스에 데이터를 출력하기 위한 출력 스트림
    private InputStream inputStream = null; // 블루투스에 데이터를 입력하기 위한 입력 스트림
    private Thread workerThread = null; // 문자열 수신에 사용되는 쓰레드
    private byte[] readBuffer; // 수신 된 문자열을 저장하기 위한 버퍼
    private int readBufferPosition; // 버퍼 내 문자 저장 위치
    private TextToSpeech tts; // 음성 인식
    private TextView weightView; // 혼잡도 값
    private View speech; // 음성 입력
    private View input; // 직접 입력
    private View mainView; // 메인
    private TextView weightColor; // 혼잡도 글씨 색
    private View weight; // 혼잡도 음성
    private String weight_speak; // 혼잡도 상태
    private GestureDetector mDetector; // 제스쳐
    public DatabaseHelper databaseHelper;
    public static Context context_main; // main의 모든 context
    long pressedTime = 0; //'뒤로가기' 버튼 클릭했을 때의 시간

    @Override
    public void onBackPressed() {
        if ( pressedTime == 0 ) {
            Toast.makeText(MainActivity.this, " 한 번 더 누르면 종료됩니다." , Toast.LENGTH_LONG).show();
            pressedTime = System.currentTimeMillis();
        }
        else {
            int seconds = (int) (System.currentTimeMillis() - pressedTime);

            if ( seconds > 2000 ) {
                Toast.makeText(MainActivity.this, " 한 번 더 누르면 종료됩니다." , Toast.LENGTH_LONG).show();
                pressedTime = 0 ;
            }
            else {
                super.onBackPressed();
                finish(); // app 종료 시키기
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // guide DB
        databaseHelper = new DatabaseHelper(this, "DB", null, 1);
        // 쓰기 가능한 SQLiteDatabase 인스턴스 구함
        db = databaseHelper.getWritableDatabase();

        // db insert
        dbInsert("guide", "음성입력", "음성입력 안내입니다. 화면 상단부를 클릭하면 음성인식이 시작됩니다. 도착할 층수를 말해주세요.");
        dbInsert("guide", "혼잡도", "혼잡도 안내입니다. 화면 좌측 하단을 클릭하면 혼잡도 안내 음성이 나옵니다.");
        dbInsert("guide", "직접입력", "직접입력 안내입니다. 화면 우측 하단을 클릭하면 층수를 직접 입력할 수 있습니다.");

        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        // 각 컨테이너들의 id를 매인 xml과 맞춰준다.
        weightView = (TextView) findViewById(R.id.weight_value); // 혼잡도 값
        speech = (View) findViewById(R.id.speech_click); // 음성 입력 화면
        input = (View) findViewById(R.id.input_click); // 직접 입력 화면
        weight = (View) findViewById(R.id.weight_click); // 혼잡도 음성 화면
        mainView = (View) findViewById(R.id.main); // 메인(큰) 화면
        weightColor = (TextView) findViewById(R.id.main_weight); // 혼잡도 글씨 색

        String guide = "음성안내를 듣고싶다면 화면을 왼쪽으로 넘기세요";

        //최초 실행 여부 판단하는 구문
        SharedPreferences pref = getSharedPreferences("isFirst", MainActivity.MODE_PRIVATE);
        boolean first = pref.getBoolean("isFirst", false);
        if (!first) {
            Log.d("Is first Time?", "first");
            SharedPreferences.Editor editor = pref.edit();
            editor.putBoolean("isFirst", true);
            editor.commit();

            //앱 최초 실행시 하고 싶은 작업
            // TTS를 생성하고 OnInitListener로 초기화 한다.
            tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
                @Override
                public void onInit(int status) {
                    if(status != ERROR) {
                        // 언어를 선택한다.d
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
            Log.d(TAG, "tts실행중입니다.");
        } else {
            Log.d("Is first Time?", "not first");
        }

        mDetector = new GestureDetector(this, new GestureDetector.OnGestureListener() {
            private static final int SWIPE_THRESHOLD = 100;
            private static final int SWIPE_VELOCITY_THRESHOLD = 100;

            public void onSwipeRight() {

            }

            public void onSwipeLeft() {
                Intent intent = new Intent(getApplicationContext(), ArsGuide.class);
                startActivity(intent);
            }

            public void onSwipeTop() {
            }

            public void onSwipeBottom() {
            }

            @Override
            public boolean onDown(MotionEvent motionEvent) {
                return false;
            }

            @Override
            public void onShowPress(MotionEvent motionEvent) {

            }

            @Override
            public boolean onSingleTapUp(MotionEvent motionEvent) {
                return false;
            }

            @Override
            public boolean onScroll(MotionEvent e1, MotionEvent e2, float v, float v1) {
                boolean result = false;
                try {
                    float diffY = e2.getY() - e1.getY();
                    float diffX = e2.getX() - e1.getX();
                    if (Math.abs(diffX) > Math.abs(diffY)) {
                        if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(v) > SWIPE_VELOCITY_THRESHOLD) {
                            if (diffX > 0) {
                                onSwipeRight();
                            } else {
                                onSwipeLeft();
                            }
                        }
                        result = true;
                    } else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(v1) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            onSwipeBottom();
                        } else {
                            onSwipeTop();
                        }
                    }
                    result = true;

                } catch (Exception exception) {
                    exception.printStackTrace();
                }
                return result;
            }

            @Override
            public void onLongPress(MotionEvent motionEvent) {

            }

            @Override
            public boolean onFling(MotionEvent motionEvent, MotionEvent motionEvent1, float v, float v1) {
                return false;
            }
        });

        mainView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mDetector.onTouchEvent(motionEvent);
                return false;
            }
        });

        speech.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mDetector.onTouchEvent(motionEvent);
                return false;
            }
        });

        input.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mDetector.onTouchEvent(motionEvent);
                return false;
            }
        });

        weight.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                mDetector.onTouchEvent(motionEvent);
                return false;
            }
        });

        // 블루투스 활성화하기
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter(); // 블루투스 어댑터를 디폴트 어댑터로 설정
        if (bluetoothAdapter == null) { // 디바이스가 블루투스를 지원하지 않을 때
            Toast.makeText(getApplicationContext(), "Plese Connect to Blutooth", Toast.LENGTH_LONG).show();
        } else { // 디바이스가 블루투스를 지원 할 때
            if (bluetoothAdapter.isEnabled()) { // 블루투스가 활성화 상태 (기기에 블루투스가 켜져있음)
                selectBluetoothDevice(); // 블루투스 디바이스 선택 함수 호출
            } else { // 블루투스가 비 활성화 상태 (기기에 블루투스가 꺼져있음)
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                // 선택한 값이 onActivityResult 함수에서 콜백됨
                startActivityForResult(intent, REQUEST_ENABLE_BT);
            }
        }

        // 음성입력화면이동
        speech.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), ttsActivity.class);
                startActivity(intent);
            }
        });

        // 직접입력화면이동
        input.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), input.class);
                startActivity(intent);
            }
        });

        // 혼잡도 클릭 음성
        weight_speak ="혼잡도 안내 음성입니다.";
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != ERROR) {
                    // 언어를 선택한다.
                    tts.setLanguage(Locale.KOREAN);
                }//if
            }//onInits
        });

        weight.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                tts.speak(weight_speak,TextToSpeech.QUEUE_FLUSH, null);
            }
        });//혼잡도 클릭시
        context_main = this;
    }//end onCreate

    public void dbInsert(String tableName, String title, String content) {
        String query = "select id from " + tableName
                + " where title" + "= '"+ title +"'";
        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst(); // Cursor를 제일 첫행으로 이동
        if(cursor.getCount() == 0) {  // 중복이 없으면 저장
            ContentValues contentValues = new ContentValues();
            contentValues.put("TITLE", title);
            contentValues.put("CONTENT", content);
            // 리턴값: 생성된 데이터의 id
            long id = db.insert(tableName, null, contentValues);
            Log.d(TAG, "id : " + id);
        }
    }//테이블 삽입

    public void selectBluetoothDevice() {
        // 이미 페어링 되어있는 블루투스 기기를 찾습니다.
        devices = bluetoothAdapter.getBondedDevices();
        // 페어링 된 디바이스의 크기를 저장
        int pariedDeviceCount = devices.size();
        // 페어링 되어있는 장치가 없는 경우
        if (pariedDeviceCount == 0) {
            Toast.makeText(getApplicationContext(),"장치를 페어링 해주세요",Toast.LENGTH_SHORT).show();
        }//if
        // 페어링 되어있는 장치가 있는 경우
        else {
            // 페어링 된 각각의 디바이스의 이름과 주소를 저장
            List<String> list = new ArrayList<>();
            // idta 디바이스의 이름을 리스트에 추가
            for (BluetoothDevice bluetoothDevice : devices) {
                if (bluetoothDevice.getName().equals("itda3")) {
                    list.add(bluetoothDevice.getName());
                }//if
            }//for
            // List를 CharSequence 배열로 변경
            final CharSequence[] charSequences = list.toArray(new CharSequence[list.size()]);
            list.toArray(new CharSequence[list.size()]);
            connectDevice(charSequences[0].toString()); // itda인 디바이스만 추가했으니 [0]번째 : itda
        }//else
    }//selectDevice

    // 장치 연결
    public void connectDevice(String deviceName)
    {
        bluetoothSocket=null;
        // 페어링 된 디바이스들을 모두 탐색
        for (BluetoothDevice tempDevice : devices) {
            // 사용자가 선택한 이름과 같은 디바이스로 설정하고 반복문 종료
            if (deviceName.equals(tempDevice.getName())) {
                bluetoothDevice = tempDevice;
                break;
            }
        }

        // UUID 생성
        UUID uuid = java.util.UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
        // Rfcomm 채널을 통해 블루투스 디바이스와 통신하는 소켓 생성
        try {
            bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(uuid);
            bluetoothSocket.connect();
            // 데이터 송,수신 스트림을 얻어옵니다.
            outputStream = bluetoothSocket.getOutputStream();
            inputStream = bluetoothSocket.getInputStream();
            // 데이터 수신 함수 호출
            receiveData();
        } catch (IOException e) {
            e.printStackTrace();

        }
    }//connectDevice()

    public void receiveData() {
        final Handler handler = new Handler();
        // 데이터를 수신하기 위한 버퍼를 생성
        readBufferPosition = 0;
        readBuffer = new byte[1024];

        // 데이터를 수신하기 위한 쓰레드 생성
        workerThread = new Thread(new Runnable()
        {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        // 데이터를 수신했는지 확인
                        int byteAvailable = inputStream.available();
                        // 데이터가 수신 된 경우
                        if (byteAvailable > 0) {
                            // 입력 스트림에서 바이트 단위로 읽어옴
                            byte[] bytes = new byte[byteAvailable];
                            inputStream.read(bytes);
                            // 입력 스트림 바이트를 한 바이트씩 읽어옴
                            for (int i = 0; i < byteAvailable; i++) {
                                byte tempByte = bytes[i];
                                // 개행문자를 기준으로 받음(한줄)
                                if (tempByte == '\n') {
                                    // readBuffer 배열을 encodedBytes로 복사
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    // 인코딩 된 바이트 배열을 문자열로 변환
                                    final String text = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    if (text.equals("red")) {
                                        weightColor.setTextColor(Color.parseColor("#d32f2f"));
                                        weight_speak ="혼잡 상태입니다.";
                                        weightColor.setText("혼잡");
                                    } else if(text.equals("yellow")) {
                                        weightColor.setTextColor(Color.parseColor("#ffd400"));
                                        weight_speak ="보통 상태입니다.";
                                        weightColor.setText("보통");
                                    } else if(text.equals("green")) {
                                        weightColor.setTextColor(Color.parseColor("#008000"));
                                        weight_speak ="원활 상태입니다.";
                                        weightColor.setText("원활");
                                    } else if(text.equals("danger")){
                                        weightColor.setTextColor(Color.parseColor("#ff0000"));
                                        weight_speak ="정원초과 입니다.";
                                        weightColor.setText("정원초과");
                                    }
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            // 텍스트 뷰에 출력
                                            weightView.append(text + "\n");
                                        }
                                    });
                                } // 개행 문자가 아닐 경우
                                else {
                                    readBuffer[readBufferPosition++] = tempByte;
                                }//else
                            }//for
                        }//if(수신이 된경우)
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        // 1초마다 받아옴
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        workerThread.start();
    }//receiveData()

    public void sendData(String text) {
        // 문자열에 개행문자("\n")를 추가
        text += "\n";
        try {
            // 데이터 송신
            outputStream.write(text.getBytes());
            outputStream.close();
            System.out.println(text);
        } catch (Exception e) {
            System.out.println("error");
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case REQUEST_ENABLE_BT:
                if(resultCode == RESULT_OK) {
                    selectBluetoothDevice();
                }
                else if(resultCode == RESULT_CANCELDE) {
                    // 블루투스 비활성
                    finish();  //  어플리케이션 종료
                }
                break;
        } // 블투 연결
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        try {
            workerThread.interrupt();   // 데이터 수신 쓰레드 종료
            inputStream.close();
            outputStream.close();
            bluetoothSocket.close();
            db.close();
            databaseHelper.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
        }
        super.onDestroy();
    }

}//end class