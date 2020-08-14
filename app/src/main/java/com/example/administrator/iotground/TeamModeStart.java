package com.example.administrator.iotground;

import android.content.Context;
import android.content.Intent;
import android.inputmethodservice.ExtractEditText;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

public class TeamModeStart extends AppCompatActivity {
    EditText[] teams;
    EditText teamName;
    int teamNum = 2;
    String ip;
    MqttHelper mqttHelper;

    Context context = this;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activty_teamstart);

        Intent intent_ip = getIntent();
        ip = intent_ip.getStringExtra("ip");

        teamName = (EditText)findViewById(R.id.teamname);
        teams = new EditText[3];
        teams[0] = (EditText)findViewById(R.id.team1);
        teams[1] = (EditText)findViewById(R.id.team2);
        teams[2] = (EditText)findViewById(R.id.team3);

        mqttHelper = new MqttHelper(context, "", ip);

        String[] nums = {"2", "3", "4"};
        Spinner spinner = (Spinner)findViewById(R.id.teamnum);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, nums);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
            public void onItemSelected(AdapterView<?> parent , View view, int pos ,long id) {
                teamNum = pos+1;
                for(int i = 0; i < 3; i++) {
                    if(i <= pos) teams[i].setVisibility(View.VISIBLE);
                    else teams[i].setVisibility(View.INVISIBLE);
                }
            }
            public void onNothingSelected(AdapterView<?> arg0) {

            }
        } );

    }
    public void onClick(View view){

        String phones = "";
        String name = "";

        for (int i = 0; i < teamNum; i++) {
            phones += teams[i].getText().toString() + "-";
            mqttHelper.publish(teams[i].getText().toString() + "/REQ_TEAM_INVITE", "team", 0);
        }


        name = teamName.getText().toString();

        Intent intent = new Intent(this, GamePlayActivity.class);
        intent.putExtra("idx", 2);
        intent.putExtra("teamNum", teamNum);
        intent.putExtra("teamName", name);
        intent.putExtra("teamPhoneNum", phones);
        intent.putExtra("ip",ip);
        startActivity(intent);
        finish();
    }
}