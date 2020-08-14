package com.example.administrator.iotground;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.StringTokenizer;

/**
 * Created by Administrator on 2018-05-02.
 */

public class ShowDataActivity extends AppCompatActivity {
    static final int SHOW_TEAM_RANKING = 1;
    static final int SHOW_SOLO_RANKING = 0;
    static final String SHOW_RANKING_ACTION ="SHOW_RANKING";

    MqttHelper mqttHelper;
    Context context = this;
    TelephonyManager telManager;
    String phoneNum;
    int flag = 0;
    String ip;

    TextView[] infos;
    BroadcastReceiver rankingReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(SHOW_RANKING_ACTION)) {
                String rankings = intent.getStringExtra("rankings");
                setRankings(rankings);
            }

            if(intent.getAction().equals(MqttHelper.MQTT_CONNECT_SUCCESS_ACTION)) {
                if(flag == SHOW_SOLO_RANKING) {
                    mqttHelper.publish(phoneNum + "/REQ_SHOW_RANKING", "solo", 0);
                }

                else if(flag == SHOW_TEAM_RANKING) {
                    mqttHelper.publish(phoneNum + "/REQ_SHOW_RANKING", "team", 0);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show);
        infos = new TextView[7];
        infos[0] = (TextView)findViewById(R.id.RANK);
        infos[1] = (TextView)findViewById(R.id.Name);
        infos[2] = (TextView)findViewById(R.id.Date);
        infos[3] = (TextView)findViewById(R.id.ACC);
        infos[4] = (TextView)findViewById(R.id.MAX);
        infos[5] = (TextView)findViewById(R.id.MIN);
        infos[6] = (TextView)findViewById(R.id.AVG);

        IntentFilter filter = new IntentFilter();
        filter.addAction(SHOW_RANKING_ACTION);
        filter.addAction(MqttHelper.MQTT_CONNECT_SUCCESS_ACTION);
        registerReceiver(rankingReceiver, filter);

        Intent intent = getIntent();
        flag = intent.getIntExtra("flag", 0);
        ip = intent.getStringExtra("ip");

        // 핸드폰번호 가져오기
        telManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        try{
            phoneNum = telManager.getLine1Number();
            if(phoneNum.startsWith("+82")){
                phoneNum = phoneNum.replace("+82", "0");
            }
        } catch (SecurityException e){
            Toast.makeText(this, "핸드폰번호를 가져올 수 없음 ",Toast.LENGTH_SHORT).show();
        }

        mqttHelper = new MqttHelper(context, phoneNum, ip);
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mqttHelper.disconnect();
    }

    public void setRankings(String rankings) {
        StringTokenizer tokenizer = new StringTokenizer(rankings, "-");
        int i = 0;
        int rank = 1;
        while(tokenizer.hasMoreTokens()) {
            if(i == 1) {
                infos[0].append("[" + String.valueOf(rank)+ "] \n" );
                rank++;
            }
            String info = tokenizer.nextToken();
            if(i > 0) infos[i].append(info+ "\n");
            i++;

            i = i%7;
        }
    }
}
