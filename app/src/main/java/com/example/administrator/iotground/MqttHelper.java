package com.example.administrator.iotground;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;

public class MqttHelper {
    static final String MQTT_CONNECT_SUCCESS_ACTION = "CONNECT SUCCESS";
    //final String serverUri = "tcp://192.168.0.22:1883";
    String serverUri;
    final String clientId = "IoTGrounDAndroidClient";

    final String username = "IoTGroundClient";
    final String password = "czC8iFC0033-aaa";
    Context context;

    String[] subscribes = {"/RES_GAME_START", "/RES_SHOW_GAME_RESULT", "/RES_SHOW_RANKING", "/RES_SHOW_USERINFO","/RES_TEAM_INVITE"};
    MqttAndroidClient mqttAndroidClient;
    private static MqttMessage message;

    String phoneNum = "";
    public MqttHelper(final Context context, final String phoneNum, String ip){
        this.context = context;
        this.phoneNum = phoneNum;

        for(int i = 0; i < subscribes.length; i++) {
            subscribes[i] = phoneNum + subscribes[i];
        }

        //serverUri = "tcp://192.168.1.156:1883";
        serverUri = "tcp://"+ip+":1883";
        mqttAndroidClient = new MqttAndroidClient(context, serverUri, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean b, String s) {
                Log.w("mqtt", s);
            }

            @Override
            public void connectionLost(Throwable throwable) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
                if(topic.equals(subscribes[0])) {
                    Intent intent = new Intent();
                    intent.putExtra("res", mqttMessage.toString());
                    intent.setAction(GamePlayActivity.GAME_START_ACTION);
                    context.sendBroadcast(intent);
                }

                else if(topic.equals(subscribes[1])) {
                    publish(phoneNum+"/RESULT_OK", "OK", 0);
                    String finish_mes = mqttMessage.toString();
                    Log.d("mqtt 받은 메세지 : ", finish_mes);
                    Intent intent = new Intent();
                    intent.setAction(GamePlayActivity.GAME_FINISH_ACTION);
                    intent.putExtra("result", finish_mes);
                    context.sendBroadcast(intent);
                }

                else if(topic.equals(subscribes[2])) {
                    Intent intent = new Intent();
                    intent.putExtra("rankings", mqttMessage.toString());
                    intent.setAction(ShowDataActivity.SHOW_RANKING_ACTION);
                    context.sendBroadcast(intent);
                }
                else if(topic.equals(subscribes[4])){
                    Intent intent = new Intent();
                    publish(phoneNum + "/INVITE_OK", "OK", 0);
                    intent.putExtra("res", mqttMessage.toString());
                    intent.setAction(GamePlayActivity.GAME_START_ACTION);
                    context.sendBroadcast(intent);

                }
                Log.d("Mqtt", mqttMessage.toString());
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {

            }
        });
        connect();
    }

    public void setCallback(MqttCallbackExtended callback) {
        mqttAndroidClient.setCallback(callback);
    }

    private void connect(){
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);
        mqttConnectOptions.setUserName(username);
        mqttConnectOptions.setPassword(password.toCharArray());

        try {

            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    for(int i = 0; i < subscribes.length; i++) {
                        setSubscribe(subscribes[i]);
                        Log.d("Subscribed : ", subscribes[i]);
                    }
                    Intent intent = new Intent();
                    intent.setAction(MQTT_CONNECT_SUCCESS_ACTION);
                    context.sendBroadcast(intent);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w("Mqtt", "Failed to connect to: " + serverUri + exception.toString());
                }
            });


        } catch (MqttException ex){
            ex.printStackTrace();
        }
    }
    public void disconnect() {
        try {
            mqttAndroidClient.unregisterResources();
            mqttAndroidClient.close();
            mqttAndroidClient.disconnect();
        }
        catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void setSubscribe(String topic) {
        try {
            mqttAndroidClient.subscribe(topic, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.w("Mqtt","Subscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.w("Mqtt", "Subscribed fail!");
                }
            });

        } catch (MqttException ex) {
            System.err.println("Exceptionst subscribing");
            ex.printStackTrace();
        }
    }
    public void publish(String topic, String msg, int qos){
        Log.d("Mqtt publish", topic + ":" + msg);
        message = new MqttMessage();
        message.setQos(qos);
        message.setPayload(msg.getBytes());

        try {
            mqttAndroidClient.publish(topic, message);
        } catch (MqttPersistenceException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (MqttException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
