package com.mhack.professordoodle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

public class roomActivity extends AppCompatActivity {

    private CardView card1;
    private CardView card2;
    private EditText editText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_room);
        editText = findViewById(R.id.room_id);
        card1 = findViewById(R.id.card_view);
        card2 = findViewById(R.id.card_view2);
        card1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //request
            }
        });
        card2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(editText.getVisibility()==View.VISIBLE){
                 //request
                } else {
                    editText.setVisibility(View.VISIBLE);
                }
            }
        });
    }


}
