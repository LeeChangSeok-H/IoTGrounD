package com.example.administrator.iotground;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.w3c.dom.Text;

/**
 * Created by AICT on 2018-05-20.
 */

public class GameEndDataActivity extends AppCompatActivity {
    MqttHelper mqttHelper;
    TextView text_Accuracy;
    TextView text_Max;
    TextView text_Min;
    TextView text_Avg;
    String result;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enddata);
        text_Accuracy = (TextView)findViewById(R.id.textView7);
        text_Max = (TextView)findViewById(R.id.textView6);
        text_Min = (TextView)findViewById(R.id.textView9);
        text_Avg = (TextView)findViewById(R.id.textView11);
        // 7 6 9 11
        Intent intent = getIntent();
        result = intent.getStringExtra("result");
        String[] data = result.split("-");
        String acc  = data[2];
        String max  = data[3];
        String min  = data[4];
        String avg  = data[5];

        text_Accuracy.setText(acc);
        text_Max.setText(max);
        text_Min.setText(min);
        text_Avg.setText(avg);
    }
}
