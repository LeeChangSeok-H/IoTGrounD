package com.example.administrator.iotground;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.view.View;
import android.widget.Toast;



/**
 * Created by Administrator on 2018-05-02.
 */


public class MainActivity extends AppCompatActivity {
    ///////////// 블루투스  //////////

    MqttHelper mqttHelper;
    Context context = this;
    String ip;
    TelephonyManager telManager;
    String PhoneNum;
    BroadcastReceiver gamePlayReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            if(intent.getAction().equals(GamePlayActivity.GAME_START_ACTION)) {
                Intent intent_team = new Intent(context, GamePlayActivity.class);
                intent_team.putExtra("idx", 3);
                intent_team.putExtra("ip", ip);
                startActivity(intent_team);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        ip = intent.getStringExtra("firstip");

        IntentFilter filter = new IntentFilter();
        filter.addAction(GamePlayActivity.GAME_START_ACTION);
        filter.addAction(MqttHelper.MQTT_CONNECT_SUCCESS_ACTION);
        registerReceiver(gamePlayReceiver, filter);

        // 핸드폰번호 가져오기
        telManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        try{
            PhoneNum = telManager.getLine1Number();
            if(PhoneNum.startsWith("+82")){
                PhoneNum = PhoneNum.replace("+82", "0");
            }
        } catch (SecurityException e){
            Toast.makeText(this, "핸드폰번호를 가져올 수 없음 ",Toast.LENGTH_SHORT).show();
        }

        mqttHelper = new MqttHelper(this, PhoneNum, ip);
    }

    public void onClick(View view) {
        // 솔로모드 버튼을 눌렀을 때
        if(view.getId() == R.id.solomode) {
            Intent intent = new Intent(this, GamePlayActivity.class);
            intent.putExtra("idx", 1);
            intent.putExtra("ip", ip);
            //mqttHelper.disconnect();
            startActivity(intent);
        }
        // 팀모드 버튼을 눌렀을 때
        else if(view.getId() == R.id.teammode) {
            Intent intent = new Intent(this, TeamModeStart.class);
            intent.putExtra("ip", ip);
            //mqttHelper.disconnect();
            startActivity(intent);
        }
        // 개인 랭킹 버튼을 눌렀을 때
        else if(view.getId() == R.id.solorank) {
            Intent intent = new Intent(this, ShowDataActivity.class);
            intent.putExtra("flag", ShowDataActivity.SHOW_SOLO_RANKING);
            intent.putExtra("ip", ip);
            //mqttHelper.disconnect();
            startActivity(intent);
        }
        // 팀 랭킹 버튼을 눌렀을 때
        else if(view.getId() == R.id.teamrank) {
            Intent intent = new Intent(this, ShowDataActivity.class);
            intent.putExtra("flag", ShowDataActivity.SHOW_TEAM_RANKING);
            intent.putExtra("ip", ip);
            //mqttHelper.disconnect();
            startActivity(intent);
        }
    }
}
