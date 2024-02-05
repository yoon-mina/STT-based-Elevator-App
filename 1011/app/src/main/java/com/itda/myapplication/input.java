package com.itda.myapplication;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class input extends AppCompatActivity {
    private EditText input_floor; // 직접 입력
    private Button input_button;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_input);
        ActionBar actionBar = getSupportActionBar();
        actionBar.hide();

        input_floor=(EditText)findViewById(R.id.input_floor);
        input_button=(Button)findViewById(R.id.input_button);

        input_button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                ((MainActivity)MainActivity.context_main).sendData(input_floor.getText().toString());
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
            }
        });

    }//onCreate
}//end class