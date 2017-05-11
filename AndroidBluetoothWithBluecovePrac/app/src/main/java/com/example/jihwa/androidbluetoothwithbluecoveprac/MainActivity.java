package com.example.jihwa.androidbluetoothwithbluecoveprac;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

/*
 * 본 예제에서 activity_main.xml 은 분석하지않고 그냥 복붙함
 * 원예제
 * http://webnautes.tistory.com/849
 * 원예제 - 참고
 * https://github.com/googlesamples/android-BluetoothChat
 * http://www.kotemaru.org/2013/10/30/android-bluetooth-sample.html
 */
public class MainActivity extends AppCompatActivity {

    private final int REQUEST_BLUETOOTH_ENABLE = 100;
    private TextView mConnectionStatus;
    private EditText mInputEditText;

    ConnectedTask mConnectedTask = null;
    static BluetoothAdapter mBluetoothAdapter;
    private String mConnectedDeviceName = null;
    private ArrayAdapter<String> mConversationArrayAdapter;
    static boolean isConnectionError = false;
    private static final String TAG = "BluetoothClient";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button sendButton = (Button) findViewById(R.id.send_button);
        sendButton.setOnClickListener(new View.OnClickListener(){

            @Override
            public void onClick(View v) {
                String sendMessage = mInputEditText.getText().toString();
                if(sendMessage.length()>0)
                    sendMessage(sendMessage);
            }
        });
        mConnectionStatus = (TextView) findViewById(R.id.connection_status_textview);
        mInputEditText = (EditText) findViewById(R.id.input_string_edittext);
        ListView mMessageListView = (ListView) findViewById(R.id.message_listview);

        mConversationArrayAdapter = new ArrayAdapter<>(this,android.R.layout.simple_list_item_activated_1);
        mMessageListView.setAdapter(mConversationArrayAdapter);

        Log.d(TAG,"Initializing Bluetooth adapter..");

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(mBluetoothAdapter == null){
            showErrorDialog("This device is not implement Bluetooth.");
            return;
        }

        if(!mBluetoothAdapter.isEnabled()){
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(intent,REQUEST_BLUETOOTH_ENABLE);
        }else{
            Log.d(TAG,"Initialisation successful.");

            // 2. 페어링 되어 있는 블루투스 장치들의 목록을 보여줌
            // 3. 목록에서 블루투스 장치를 선택하면 선택한 디바이스를
            // 인자로 하여 doConnect 함수가 호출.
            showPairedDevicesListDialog();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mConnectedTask!= null){
            mConnectedTask.cancel(true);
        }
    }

    private class ConnectTask extends AsyncTask<Void,Void,Boolean>{
        private BluetoothSocket mBluetoothSocket = null;
        private BluetoothDevice mBluetoothDevice = null;

        public ConnectTask(BluetoothDevice bluetoothDevice) {

            UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
            mBluetoothDevice = bluetoothDevice;
            mConnectedDeviceName = mBluetoothDevice.getName();
            //get a bluetoothSocket for a connection with the
            // given bluetoothDevice
            try{
                mBluetoothSocket = mBluetoothDevice.createRfcommSocketToServiceRecord(uuid);

                Log.d(TAG,"create socket for "+mConnectedDeviceName);
            } catch (IOException e) {
                Log.d(TAG,"socket create failed " + e.getMessage());
            }
            mConnectionStatus.setText("Connecting...");
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // Always cancel discovery because it will slow down a connection
            // 연결이 느려지기 때문에, 항상 발견하던것을 취소함.
            mBluetoothAdapter.cancelDiscovery();
            try{
                mBluetoothSocket.connect();
                Log.e(TAG,"mBluetoothSocket connect");
            } catch (IOException e) {
                try{
                    mBluetoothSocket.close();
                } catch (IOException e1) {
                    Log.e(TAG,"unable to close()" + "socket during connection failure",e1);
                    return false;
                }
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean isSuccess) {
            if(isSuccess){
                connected(mBluetoothSocket);
            }else{
                isConnectionError = true;
                Log.d(TAG,"Unable to connect device");
                showErrorDialog("Unable to connect device");
            }
        }
    }

    private void connected(BluetoothSocket mBluetoothSocket) {
        mConnectedTask = new ConnectedTask(mBluetoothSocket);
        mConnectedTask.execute();
    }


    // this thread runs during a connection with a remote device.
    // it handles all incoming and outgoing transmissions.
    private class ConnectedTask extends  AsyncTask<Void,String,Boolean>{
        private InputStream mInputStream = null;
        private OutputStream mOutputStream = null;
        private BluetoothSocket mBluetoothSocket = null;

        public ConnectedTask(BluetoothSocket socket) {
            mBluetoothSocket = socket;
            try{
                mInputStream = mBluetoothSocket.getInputStream();
                mOutputStream = mBluetoothSocket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG,"socket not created",e);
            }

            Log.d(TAG,"connected to " + mConnectedDeviceName);
            mConnectionStatus.setText("connected to " + mConnectedDeviceName);
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            byte[] readBuffer = new byte[1024];
            int readBufferPosition = 0;
            while(true){
                if(isCancelled()) return false;

                try{
                    // java.io.IOException: socket closed
                    // 계속 disconnected 됨
                    Log.e(TAG,"mInpustStream = " + mInputStream.markSupported());
                    int bytesAvailable = mInputStream.available();
                    if(bytesAvailable>0){
                        byte[] packetBytes = new byte[bytesAvailable];
                        //read from the inputStream
                        mInputStream.read(packetBytes);

                        for(int i = 0 ; i<bytesAvailable; i++){
                            byte b = packetBytes[i];
                            if(b == '\n'){
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer,0,encodedBytes,0,encodedBytes.length);
                                String recvMessage = new String(encodedBytes,"UTF-8");

                                readBufferPosition = 0;
                                Log.d(TAG,"recv message: " + recvMessage);
                                publishProgress(recvMessage);
                            }else{
                                readBuffer[readBufferPosition++] = b;
                            }
                        }
                    }
                } catch (IOException e) {
                    Log.d(TAG,"disconnected",e);
                    return false;
                }
            }
        }

        @Override
        protected void onProgressUpdate(String... recvMassage) {
            mConversationArrayAdapter.insert(mConnectedDeviceName + ": " + recvMassage[0],0);
        }

        @Override
        protected void onPostExecute(Boolean isSuccess) {
            super.onPostExecute(isSuccess);
            if(!isSuccess){
                closeSocket();
                Log.d(TAG,"Device connection was lost");
                isConnectionError = true;
                showErrorDialog("Device connection was lost");
            }
        }

        @Override
        protected void onCancelled(Boolean aBoolean) {
            super.onCancelled(aBoolean);
            closeSocket();
        }

        void closeSocket() {
            try{
                mBluetoothSocket.close();
                Log.d(TAG,"close socket()");
            } catch (IOException e) {
                Log.e(TAG,"unable to close()"
                        + "socket during connection failure",e);
            }
        }
        void write(String msg){
            msg += "\n";
            try{
                mOutputStream.write(msg.getBytes());
                mOutputStream.flush();
            } catch (IOException e) {
                Log.e(TAG,"Exception during send",e);
            }
            mInputEditText.setText(" ");
        }
    }


    private void showPairedDevicesListDialog() {
        Set<BluetoothDevice> devices = mBluetoothAdapter.getBondedDevices();
        
        final BluetoothDevice[] pairedDevices = devices.toArray(new BluetoothDevice[0]);
        if(pairedDevices.length == 0){
            showQuitDialog("NO device have been paired.\n" +
                    "You must pair it with another device.");
            return;
        }
        String[] items;
        items = new String[pairedDevices.length];
        for(int i = 0 ; i < pairedDevices.length ; i++){
            items[i] = pairedDevices[i].getName();
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select device");
        builder.setCancelable(false);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

                ConnectTask task = new ConnectTask(pairedDevices[which]);
                task.execute();
            }
        });
        builder.create().show();
    }

    private void showErrorDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quit");
        builder.setCancelable(false);
        builder.setMessage(message);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                if(isConnectionError){
                    isConnectionError = false;
                    finish();
                }
            }
        });
        builder.create().show();
    }

    private void showQuitDialog(String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Quit");
        builder.setCancelable(false);
        builder.setMessage(message);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                finish();
            }
        });
        builder.create().show();
    }

    private void sendMessage(String msg) {
        if(mConnectedTask != null){
            mConnectedTask.write(msg);
            Log.d(TAG,"send message: " + msg);
            mConversationArrayAdapter.insert("Me: " + msg,0);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == REQUEST_BLUETOOTH_ENABLE){
            if(requestCode == RESULT_OK){
                showPairedDevicesListDialog();
            }
            if(requestCode == RESULT_CANCELED){
                showQuitDialog("You need to enable bluetooth");
            }
        }
    }
}
