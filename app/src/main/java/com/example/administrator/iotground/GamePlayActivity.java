package com.example.administrator.iotground;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;


public class GamePlayActivity extends AppCompatActivity {
    private final int REQUEST_BLUETOOTH_ENABLE = 100;
    static final String GAME_FINISH_ACTION = "GAME_FINISH";
    static final String GAME_START_ACTION ="GAME_START";

    private TextView mConnectionStatus;
    // private EditText mInputEditText;

    GamePlayActivity.ConnectedTask mConnectedTask = null;
    static BluetoothAdapter mBluetoothAdapter;
    private String mConnectedDeviceName = null;
    private ArrayAdapter<String> mConversationArrayAdapter;
    static boolean isConnectionError = false;
    private static final String TAG = "BluetoothClient";
    MqttHelper mqttHelper;
    TextView textView_sub;
    TextView textView_info;

    Context context = this;

    TelephonyManager telManager;
    String PhoneNum;
    // 솔로와 팀모드를 구별하는 것
    int idx;

    int teamNum = 2;
    String teamName;
    String teamPhoneNumber;
    String ip;

    BroadcastReceiver gamePlayReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(intent.getAction().equals(GAME_START_ACTION)) {
                String res = intent.getStringExtra("res");
                if(res.equals("ACCEPT")) {
                    textView_info.setText("게임 중입니다.");
                }

                else if(res.equals("DENY")) {
                    Toast.makeText(context, "서버가 바쁩니다. 게임을 요청할 수 없습니다. ", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            else if(intent.getAction().equals(GAME_FINISH_ACTION)) {
                sendMessage("D");
                String result = intent.getStringExtra("result");
                Log.d("gameplayactivity 메세지 :", result);
                finish();
                Intent goIntent = new Intent(GamePlayActivity.this, GameEndDataActivity.class);
                goIntent.putExtra("result", result);
                startActivity(goIntent);

            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gameplay);

        IntentFilter filter = new IntentFilter();
        filter.addAction(GAME_FINISH_ACTION);
        filter.addAction(GAME_START_ACTION);
        registerReceiver(gamePlayReceiver, filter);

        final Button sendButton = (Button)findViewById(R.id.send_button);
        textView_info = (TextView)findViewById(R.id.textView8);

        Intent get_idx = getIntent();
        idx = get_idx.getIntExtra("idx", 0);
        ip = get_idx.getStringExtra("ip");

        if(idx == 2) {
            teamNum = get_idx.getIntExtra("teamNum", 2);
            teamName = get_idx.getStringExtra("teamName");
            teamPhoneNumber = get_idx.getStringExtra("teamPhoneNum");
        }
        else if(idx == 3){
            sendButton.setText("Ready");
        }

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

        mqttHelper = new MqttHelper(context, PhoneNum, ip);

        sendButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){

                if(idx == 1){
                    sendButton.setEnabled(false);
                    mqttHelper.publish(PhoneNum+"/REQ_GAME_START", "solo", 0);
                    textView_info.setText("게임 시작!");
                    sendMessage("C");
                }
                else if( idx == 2  ){
                    sendButton.setEnabled(false);
                    String message = "team-" + teamName + "-" + teamNum +"-" + teamPhoneNumber;
                    Log.d("GameplayActivity/team", message);
                    mqttHelper.publish(PhoneNum+"/REQ_GAME_START", message.toString(), 0);
                    textView_info.setText("게임 시작!");
                    sendMessage("C");
                }

                else if( idx == 3  ){
                    sendButton.setEnabled(false);
                    textView_info.setText("게임 시작!");
                    sendMessage("C");
                }
                // 서버에서 게임을 시작하라는 메세지를 받음
                // 메세지 subscribe : 게임 종료 문자가 오면 문자를 받아 해당 문자면 finish하고 다음 액티비티 실행
            }
        });
        mConnectionStatus = (TextView)findViewById(R.id.connection_status_textview);

        Log.d( TAG, "Initalizing Bluetooth adapter...");

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            showErrorDialog("This device is not implement Bluetooth.");
            return;
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent, REQUEST_BLUETOOTH_ENABLE);
        }
        else {
            Log.d(TAG, "Initialisation successful.");

            showPairedDevicesListDialog();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mqttHelper.disconnect();
        if ( mConnectedTask != null ) {
            mConnectedTask.cancel(true);
        }
    }

    private class ConnectTask extends AsyncTask<Void, Void, Boolean> {

        private BluetoothSocket mBluetoothSocket = null;
        private BluetoothDevice mBluetoothDevice = null;

        ConnectTask(BluetoothDevice bluetoothDevice) {
            mBluetoothDevice = bluetoothDevice;
            mConnectedDeviceName = bluetoothDevice.getName();

            //SPP
            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

            try {
                mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(uuid);
                Log.d( TAG, "create socket for "+mConnectedDeviceName);

            } catch (IOException e) {
                Log.e( TAG, "socket create failed " + e.getMessage());
            }

            mConnectionStatus.setText("connecting...");
        }


        @Override
        protected Boolean doInBackground(Void... params) {

            // Always cancel discovery because it will slow down a connection
            mBluetoothAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mBluetoothSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mBluetoothSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() " +
                            " socket during connection failure", e2);
                }

                return false;
            }

            return true;
        }

        @Override
        protected void onPostExecute(Boolean isSucess) {

            if ( isSucess ) {
                connected(mBluetoothSocket);
            }
            else{

                isConnectionError = true;
                Log.d( TAG,  "Unable to connect device");
                showErrorDialog("Unable to connect device");
            }
        }
    }

    public void connected( BluetoothSocket socket ) {
        mConnectedTask = new GamePlayActivity.ConnectedTask(socket);
        mConnectedTask.execute();
    }

    private class ConnectedTask extends AsyncTask<Void, String, Boolean> {

        private InputStream mInputStream = null;
        private OutputStream mOutputStream = null;
        private BluetoothSocket mBluetoothSocket = null;

        ConnectedTask(BluetoothSocket socket){

            mBluetoothSocket = socket;
            try {
                mInputStream = mBluetoothSocket.getInputStream();
                mOutputStream = mBluetoothSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "socket not created", e );
            }

            Log.d( TAG, "connected to "+mConnectedDeviceName);
            mConnectionStatus.setText( "connected to "+mConnectedDeviceName);
            textView_info.setText("총 연결 완료.");
            sendMessage("C");
        }


        @Override
        protected Boolean doInBackground(Void... params) {
            byte [] readBuffer = new byte[1024];
            int readBufferPosition = 0;

            while (true) {
                if ( isCancelled() ) return false;
                try {
                    int bytesAvailable = mInputStream.available();
                    if(bytesAvailable > 0) {
                        byte[] packetBytes = new byte[bytesAvailable];
                        mInputStream.read(packetBytes);

                        for(int i=0;i<bytesAvailable;i++) {
                            byte b = packetBytes[i];
                            if(b == '\n')
                            {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0,
                                        encodedBytes.length);
                                String recvMessage = new String(encodedBytes, "UTF-8");

                                readBufferPosition = 0;

                                Log.d(TAG, "recv message: " + recvMessage);
                                publishProgress(recvMessage);
                                //mConversationArrayAdapter.insert("You:  " + recvMessage, 0);
                            }
                            else
                            {
                                readBuffer[readBufferPosition++] = b;
                            }
                        }
                    }
                } catch (IOException e) {

                    Log.e(TAG, "disconnected", e);
                    return false;
                }
            }

        }

        @Override
        protected void onProgressUpdate(String... recvMessage) {

            //mConversationArrayAdapter.insert(mConnectedDeviceName + ": " + recvMessage[0], 0);
        }

        @Override
        protected void onPostExecute(Boolean isSucess) {
            super.onPostExecute(isSucess);

            if ( !isSucess ) {
                closeSocket();
                Log.d(TAG, "Device connection was lost");
                isConnectionError = true;
                showErrorDialog("Device connection was lost");
            }
        }

        @Override
        protected void onCancelled(Boolean aBoolean) {
            super.onCancelled(aBoolean);
            closeSocket();
        }

        void closeSocket(){
            try {
                mBluetoothSocket.close();
                Log.d(TAG, "close socket()");

            } catch (IOException e2) {
                Log.e(TAG, "unable to close() " +
                        " socket during connection failure", e2);
            }
        }

        void write(String msg){
            msg += "\n";
            try {
                mOutputStream.write(msg.getBytes());
                mOutputStream.flush();
            } catch (IOException e) {
                Log.e(TAG, "Exception during send", e );
            }
            //mInputEditText.setText(" ");
        }
    }


    public void showPairedDevicesListDialog()
    {
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        final BluetoothDevice[] pairedDevices = devices.toArray(new BluetoothDevice[0]);

        if ( pairedDevices.length == 0 ){
            showQuitDialog( "No devices have been paired.\n"
                    +"You must pair it with another device.");
            return;
        }

        String[] items;
        items = new String[pairedDevices.length];
        for (int i=0;i<pairedDevices.length;i++) {
            items[i] = pairedDevices[i].getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("사용할 무기를 선택해주세요");
        builder.setCancelable(false);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                GamePlayActivity.ConnectTask task = new GamePlayActivity.ConnectTask(pairedDevices[which]);
                task.execute();
            }
        });
        builder.create().show();
    }

    public void showErrorDialog(String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quit");
        builder.setCancelable(false);
        builder.setMessage(message);
        builder.setPositiveButton("OK",  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if ( isConnectionError  ) {
                    isConnectionError = false;
                    finish();
                }
            }
        });
        builder.create().show();
    }


    public void showQuitDialog(String message)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quit");
        builder.setCancelable(false);
        builder.setMessage(message);
        builder.setPositiveButton("OK",  new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        builder.create().show();
    }

    void sendMessage(String msg){
        if(mConnectedTask == null) Log.d(TAG, "mConnectedTast null");
        else if ( mConnectedTask != null ) {
            mConnectedTask.write(msg);
            Log.d(TAG, "send message: " + msg);
            // mConversationArrayAdapter.insert("Me:  " + msg, 0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if(requestCode == REQUEST_BLUETOOTH_ENABLE){
            if (resultCode == RESULT_OK){
                //BlueTooth is now Enabled
                showPairedDevicesListDialog();
            }
            if(resultCode == RESULT_CANCELED){
                showQuitDialog( "You need to enable bluetooth");
            }
        }
    }
}

