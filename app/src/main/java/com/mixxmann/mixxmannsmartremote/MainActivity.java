package com.mixxmann.mixxmannsmartremote;

import androidx.appcompat.app.AppCompatActivity;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.mixxmann.smartcontrol.R;
import com.google.android.material.snackbar.Snackbar;
import com.triggertrap.seekarc.SeekArc;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;



import belka.us.androidtoggleswitch.widgets.ToggleSwitch;

public class MainActivity extends AppCompatActivity {
    SharedPreferences sPref; final String SAVED_BTDEVICE = "";
    boolean mute;
    private SeekArc mSeekArc;    private ToggleSwitch mTogleSwitch;
    private TextView mSeekArcProgress;    private Menu menu;    ArrayAdapter<String>pairedDeviceAdapter;
    ListView deviceList;    TextView textInfo;    TextView textViewRemoteDevice;
    TextView resmessage; TextView notFoundMessage;    FrameLayout mainFrame;    FrameLayout listFrame;
    LinearLayout workframe;    ArrayList<String> pairedDeviceList;    private UUID myUUID;
    ThreadConnectBTdevice myThreadConnectBTdevice;    ThreadConnected myThreadConnected;
    private StringBuilder sb = new StringBuilder();
    private BluetoothSocket btSocket = null;
    byte[] mess = new byte[1];
    boolean datareceived=false;
    TextView intel, outtel;
    int vin,vout,aout,fout,allow = 0;
    boolean havtel=false;
    Handler handler = new Handler();
    Runnable runnable;
    int delay = 10000;
    @Override public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mute=false;
        setContentView(R.layout.activity_main);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        deviceList=findViewById(R.id.deviceList);
        mainFrame=findViewById(R.id.mainframe);
        listFrame=findViewById(R.id.listframe);
        workframe=findViewById(R.id.workframe);
        textInfo=findViewById(R.id.textInfo);
        notFoundMessage=findViewById(R.id.nonfoundmessage);
        intel=findViewById(R.id.telemetryin);
        outtel=findViewById(R.id.telemetryout);
        myUUID = UUID.fromString(constants.UUID_STRING_WELL_KNOWN_SPP);
        SeekArc mSeekArc = findViewById(R.id.seekArc);
        mSeekArcProgress = findViewById(R.id.seekArcProgress);
        mSeekArc.setOnSeekArcChangeListener(new SeekArc.OnSeekArcChangeListener() {
            @Override public void onStopTrackingTouch(SeekArc seekArc) {
                if(myThreadConnected!=null & !mute) {
                    String remotecommand="V";
                    myThreadConnected.write(remotecommand.getBytes());
                    int progr=mSeekArc.getProgress();
                    if (progr<=100 & progr>=0 ) {
                        mess[0]=(byte)(progr & 0xFF);
                        myThreadConnected.write(mess);
                    }
                    remotecommand="~";
                    myThreadConnected.write(remotecommand.getBytes());
                }
            }
            @Override public void onStartTrackingTouch(SeekArc seekArc) { }
            @Override public void onProgressChanged(SeekArc seekArc, int progress,boolean fromUser) {
                if (progress==0) {mSeekArcProgress.setText("MIN");}
                else if (progress==100) {mSeekArcProgress.setText("MAX");}
                else {mSeekArcProgress.setText(String.valueOf(progress)+"%");}
                if (progress<=30) {
                    mSeekArcProgress.setTextColor(getResources().getColor(R.color.pgreen));
                } else if (progress<=70) {
                    mSeekArcProgress.setTextColor(getResources().getColor(R.color.pyellow));
                } else {
                    mSeekArcProgress.setTextColor(getResources().getColor(R.color.pred));
                }
                if(myThreadConnected!=null & !mute) {
                    String remotecommand="V";
                    myThreadConnected.write(remotecommand.getBytes());
                    int progr=mSeekArc.getProgress();
                    if (progr<=100 & progr>=0 ) {
                        mess[0]=(byte)(progr & 0xFF);
                        myThreadConnected.write(mess);
                    }
                    remotecommand="~";
                    myThreadConnected.write(remotecommand.getBytes());
                }
            }
        });
        textViewRemoteDevice=findViewById(R.id.textViewremotedevice);
        mTogleSwitch = findViewById(R.id.mtogleSwitch);
        mTogleSwitch.setCheckedTogglePosition(1,false);
        mTogleSwitch.setActiveBgColor(getResources().getColor(R.color.pgreen));
        mTogleSwitch.setActivated(true);
        mTogleSwitch.setOnToggleSwitchChangeListener(new ToggleSwitch.OnToggleSwitchChangeListener(){
            @Override
            public void onToggleSwitchChangeListener(int position, boolean isChecked) {
                if(myThreadConnected!=null & !mute) {
                    String remotecommand="P";
                    myThreadConnected.write(remotecommand.getBytes());

                    if (position==1) {
                        byte[] val={0x00};
                        myThreadConnected.write(val);
                    }
                    if (position==0) {
                        byte[] val={0x01};
                        myThreadConnected.write(val);
                    }
                    remotecommand="~";
                    myThreadConnected.write(remotecommand.getBytes());
                }

            }
        });
//        h = new Handler() {
//            public void handleMessage(android.os.Message msg) {
//                switch (msg.what) {
//                    case RECIEVE_MESSAGE:                                                   // если приняли сообщение в Handler
//                        byte[] readBuf = (byte[]) msg.obj;
//                        String strIncom = new String(readBuf, 0, msg.arg1);
//                        //Toast.makeText(MainActivity.this,strIncom,Toast.LENGTH_SHORT).show();
//                        sb.append(strIncom);                                                // формируем строку
//                        int endOfLineIndex = sb.indexOf("\r\n");                            // определяем символы конца строки
//                        if (endOfLineIndex > 0) {                                            // если встречаем конец строки,
//                            String sbprint = sb.substring(0, endOfLineIndex);               // то извлекаем строку
//                            sb.delete(0, sb.length());                                      // и очищаем sb
//                            resmessage.setText("m: " + sbprint);             // обновляем TextView
//
//                        }
//                        //Log.d(TAG, "...Строка:"+ sb.toString() +  "Байт:" + msg.arg1 + "...");
//                        break;
//                }
//            };
//        };


    }
    protected void onResume() {
        handler.postDelayed(runnable = new Runnable() {
            public void run() {
                handler.postDelayed(runnable, delay);
                disconectifnodata();
            }
        }, delay);
        super.onResume();
    }
    @Override protected void onPause() {
        handler.removeCallbacks(runnable); //stop handler when activity not visible super.onPause();
        super.onPause();
    }
    @Override public void onStart() {
        super.onStart();

    }
    @Override protected void onDestroy() { // Закрытие приложения
        super.onDestroy();
        if(myThreadConnectBTdevice!=null) myThreadConnectBTdevice.cancel();
    }
    @Override public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mainmenu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    private void savePrefs(String sName, String sVal) { /// зберігаєм останній девайс
        sPref = getSharedPreferences("mixxmann", MODE_PRIVATE);
        SharedPreferences.Editor ed = sPref.edit();
        ed.putString(sName, sVal);
        ed.commit();
    }
    private String loadPrefs(String sName) {
        sPref = getSharedPreferences("mixxmann", MODE_PRIVATE);
        String savedText = sPref.getString(sName, "");
        return savedText;
    }
    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.connectbutton:
                textInfo.setText("Select a device to connect");
                if (btSocket!=null){
                    try {btSocket.close();} catch (IOException e) {}
                }
                    menu.getItem(0).setIcon(R.drawable.bluetooth);
                    workframe.setVisibility(View.GONE);
                    setupbluetooth();
                break;

            case R.id.supportbutton:
                String posted_by = "+380983005305";
                Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel",posted_by,null));
                startActivity(intent);
                break;
            case R.id.aboutbutton:
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://bauservice.com.ua/")));
                break;
        }
        return super.onOptionsItemSelected(item);
    }
    public void bbonClick(View view){
       setupbluetooth();
    }
    public void snonClick(View view){
        openBTSettings();
    }
    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == constants.REQUEST_ENABLE_BT) { // Если разрешили включить Bluetooth, тогда void setup()
            if (resultCode == Activity.RESULT_OK) {
                setupbluetooth();
            } else { // Если не разрешили, тогда закрываем приложение
                View butview = findViewById(R.id.buttonconnect);
                Snackbar sb = Snackbar.make(butview, "Bluetooth activation is not allowed", Snackbar.LENGTH_LONG);
                ;
                sb.setActionTextColor(getResources().getColor(R.color.purple_700));
                sb.setAction("EXIT", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        finishAffinity();
                    }
                });
                sb.show();
                return;
            }
        }
        if (requestCode == constants.SETTINGS_BT) {
            setupbluetooth();
        }
    }
    public void openBTSettings() {
        Intent enableIntent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
        startActivityForResult(enableIntent, constants.SETTINGS_BT);
    }
    public void setupbluetooth() {

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null) {
            View butview=findViewById(R.id.buttonconnect);
            Snackbar sb = Snackbar.make(butview, "Bluetooth is not supported on this hardware platform", Snackbar.LENGTH_LONG);;
            sb.setActionTextColor(getResources().getColor(R.color.purple_700));
            sb.setAction("EXIT", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    finishAffinity();
                }
            });
            sb.show();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, constants.REQUEST_ENABLE_BT);
        }
        Set <BluetoothDevice>pairedDevices = bluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            pairedDeviceList = new ArrayList<>();
            for (BluetoothDevice device : pairedDevices) {
                String deviceName = (String) device.getName();
                String deviceAddress = (String) device.getAddress();
                if ((deviceName.substring(0,2).equals("S3")) || (deviceName.substring(0,2).equals("S8")) || (deviceName.substring(0,2).equals("S5")) ) {
                    pairedDeviceList.add("MIXXMANN "+deviceName+"\n"+deviceAddress);
                }

            }
            pairedDeviceAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, pairedDeviceList);
            deviceList.setAdapter(pairedDeviceAdapter);
            mainFrame.setVisibility(View.GONE);
            listFrame.setVisibility(View.VISIBLE);
            if (pairedDeviceList.size()==0) {
                deviceList.setVisibility(View.GONE);
                notFoundMessage.setVisibility(View.VISIBLE);
            } else {
                deviceList.setVisibility(View.VISIBLE);
                notFoundMessage.setVisibility(View.GONE);
            }
            deviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    String selectedlistitem=(String) deviceList.getItemAtPosition(position);
                    String MAC=selectedlistitem.substring(selectedlistitem.length()-17);
                    BluetoothDevice remotedevice=bluetoothAdapter.getRemoteDevice(MAC);
                    myThreadConnectBTdevice=new ThreadConnectBTdevice(remotedevice);
                    myThreadConnectBTdevice.start();
                    textInfo.setText("CONNECTING...");
                }
            });
            String saveddeaddres=loadPrefs(SAVED_BTDEVICE);
//            if (saveddeaddres.length()>0) {
//                BluetoothDevice remotedevice=bluetoothAdapter.getRemoteDevice(saveddeaddres);
//                myThreadConnectBTdevice=new ThreadConnectBTdevice(remotedevice);
//                myThreadConnectBTdevice.start();
//                textInfo.setText("CONNECTING...");
//            }
        } else {
            View butview=findViewById(R.id.buttonconnect);
            Snackbar sb = Snackbar.make(butview, "No paired devices found", Snackbar.LENGTH_LONG);;
            sb.setActionTextColor(getResources().getColor(R.color.purple_700));
            sb.setAction("SEARCH", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openBTSettings();
                }
            });
            sb.show();
            return;
        }
    }
    public void disconectifnodata(){
        if (! datareceived & workframe.getVisibility() == View.VISIBLE) {
            if(myThreadConnectBTdevice!=null) myThreadConnectBTdevice.cancel();
            workframe.setVisibility(View.GONE);
            setupbluetooth();
        } else {
            datareceived=false;
        }
    }
    public void updatetelemetry() {
//        StringBuilder tsin = new StringBuilder();
//        tsin.append("Uin:");
//        tsin.append(vin);
//        tsin.append(" V");
//        intel.setText(tsin.toString());
        if (allow==1) {
            mTogleSwitch.setVisibility(View.VISIBLE);
        } else {
            mTogleSwitch.setVisibility(View.GONE);
        }

        StringBuilder tout = new StringBuilder();
        //tout.append("ALLOW: ");
        //tout.append(allow);
        //tout.append("   |   ");
        tout.append("U: ");
        tout.append(vout);
        tout.append("V");
        tout.append("   |   ");
        tout.append("I: ");
        tout.append(aout);
        tout.append("A");
        tout.append("   |   ");
        tout.append("Freq: ");
        tout.append(fout);
        tout.append("Hz");

        outtel.setText(tout.toString());
    }
////////////////////работа з блютузом
private class ThreadConnectBTdevice extends Thread { // Поток для коннекта с Bluetooth
  //  public BluetoothSocket bluetoothSocket = null;
    public BluetoothDevice remotedevice;
    private ThreadConnectBTdevice(BluetoothDevice device) {
        try {
            remotedevice=device;
            btSocket = device.createRfcommSocketToServiceRecord(myUUID);
        }
        catch (IOException e) {
            e.printStackTrace();
            textInfo.setText("Select a device to connect");
        }
    }
    @Override
    public void run() { // Коннект
        boolean success = false;
        try {
            btSocket.connect();
            success = true;
        }
        catch (IOException e) {
            e.printStackTrace();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
//                    View butview=findViewById(R.id.buttonconnect);
//                    Snackbar sb = Snackbar.make(butview, "Connection closed.", Snackbar.LENGTH_LONG);;
//                    sb.setActionTextColor(getResources().getColor(R.color.purple_700));
//                    sb.setAction("Try again", new View.OnClickListener() {
//                        @Override
//                        public void onClick(View v) {
//                            setupbluetooth();
//                        }
//                    });
                    //sb.show();
                    textInfo.setText("Select a device to connect");
                    return;

                }
            });
            try {
                btSocket.close();
                textInfo.setText("Select a device to connect");
            }
            catch (IOException e1) {
                e1.printStackTrace();
                textInfo.setText("Select a device to connect");
            }
        }
        if(success) {  // Если законнектились, тогда открываем панель с кнопками и запускаем поток приёма и отправки данных
            myThreadConnected = new ThreadConnected(btSocket);
            myThreadConnected.start(); // запуск потока приёма и отправки данных

            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                   mainFrame.setVisibility(View.GONE);
                   listFrame.setVisibility(View.GONE);
                   workframe.setVisibility(View.VISIBLE);
                   textViewRemoteDevice.setText(remotedevice.getName());
                   menu.getItem(0).setIcon(R.drawable.bluetoothtblue);
                   savePrefs(SAVED_BTDEVICE,remotedevice.getAddress());
                    String remotecommand = "R0~";
                    myThreadConnected.write(remotecommand.getBytes());
                    
                }
            });


            if(myThreadConnected!=null) {

            }
        }
    }
    public void cancel() {
        //Toast.makeText(getApplicationContext(), "Close - BluetoothSocket", Toast.LENGTH_LONG).show();
        try {
            btSocket.close();
            textInfo.setText("Select a device to connect");
        }
        catch (IOException e) {
            e.printStackTrace();
            textInfo.setText("Select a device to connect");
        }
    }

} // END ThreadConnectBTdevice:
private class ThreadConnected extends Thread {    // Поток - приём и отправка данных
        private final InputStream connectedInputStream;
        private final OutputStream connectedOutputStream;
        private final BluetoothSocket socket;
        private String sbprint;
        
        public ThreadConnected(BluetoothSocket socket) {
            this.socket=socket;
            InputStream in = null;
            OutputStream out = null;
            try {
                in = socket.getInputStream();
                out = socket.getOutputStream();
            }
            catch (IOException e) {
                e.printStackTrace();
                textInfo.setText("Select a device to connect");
            }
            connectedInputStream = in;
            connectedOutputStream = out;
        }

        @Override
        public void run() { // Приём данных
            byte[] buffer = new byte[1];
            while (true) {
                try {

                    connectedInputStream.read(buffer);
                    String strIncom = new String(buffer, 0, 1);
                    sb.append(strIncom); // собираем символы в строку
                    int endOfLineIndex = sb.indexOf("~"); // определяем конец строки
                    if (endOfLineIndex > 0) {
                        sbprint = sb.substring(0, endOfLineIndex);
                        sb.delete(0, sb.length());
                        runOnUiThread(new Runnable() { // Вывод данных

                            @Override
                            public void run() {
                                datareceived=true;
                                String command=sbprint.substring(0,1);
                                String sval=sbprint.substring(1,sbprint.length());
//                                TextView t1 = findViewById(R.id.telemetryin);
//                                TextView t2 = findViewById(R.id.telemetryout);
//                                t1.setText(command);
//                                t2.setText(sval);
                                mute=true;
                                if (command.equals("V")) {
                                    int value=Integer.parseInt(sval);
                                    SeekArc mSeekArc = findViewById(R.id.seekArc);
                                    mSeekArc.setProgress(value);

                                }
                                if (command.equals("P")) {
                                    int value = Integer.parseInt(sval);
                                    ToggleSwitch ts = findViewById(R.id.mtogleSwitch);
                                    if (value==0) {ts.setCheckedTogglePosition(1);}
                                    if (value==1) {ts.setCheckedTogglePosition(0);}
                                }
                                if (command.equals("F")) {
                                    fout = Integer.parseInt(sval);
                                }
                                if (command.equals("U")) {
                                    vout = Integer.parseInt(sval);
                                }
                                if (command.equals("I")) {
                                    aout = Integer.parseInt(sval);
                                }
                                if (command.equals("u")) {
                                    vin = Integer.parseInt(sval);
                                }
                                if (command.equals("A")) {
                                    allow = Integer.parseInt(sval);
                                }
                                 updatetelemetry();
                                mute=false;
                            }
                        });
                    }
                } catch (IOException e) {
                    textInfo.setText("Select a device to connect");
                    break;
                }
//                try {
//                    bytes = connectedInputStream.read(buffer);
//                    h.obtainMessage(RECIEVE_MESSAGE, bytes, -1, buffer).sendToTarget();
//
//                } catch (IOException e) {
//                    break;
//                }

            }
        }
        public void write(byte[] buffer) {
            try {
                connectedOutputStream.write(buffer);
            } catch (IOException e) {
                textInfo.setText("Select a device to connect");
                e.printStackTrace();
            }
        }
        public void cancel() {
        try {
            connectedInputStream.close();
            connectedOutputStream.close();
            textInfo.setText("Select a device to connect");
        } catch (IOException e) { }
    }
    }


}