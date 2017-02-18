package com.example.samue.plabarduinobluetoothcontroller;

import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.support.v7.widget.CardView;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.AsyncTask;
import java.io.IOException;
import java.util.UUID;

import static com.example.samue.plabarduinobluetoothcontroller.R.id.btnOn;
import static com.example.samue.plabarduinobluetoothcontroller.R.layout.dialog;

public class LedControl extends AppCompatActivity {
    Button btnOn, btnOff, btnDisconnect, btnSendCommand;
    SeekBar brightness;
    CardView cardView;
    TextView progressTxt, cardStatus;
    String address, deviceName;
    Intent newInt;

    private ProgressDialog progress;
    BluetoothAdapter myBlueTooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_led_control);

        newInt = getIntent();
        address = newInt.getStringExtra(MainActivity.EXTRA_ADDRESS);
        deviceName = newInt.getStringExtra(MainActivity.EXTRA_DEVICE_NAME);

        cardStatus = (TextView) findViewById(R.id.card_control_panel_text_view);
        cardStatus.setText(getString(R.string.status_card_view_cp, deviceName));

        cardView = (CardView) findViewById(R.id.card_control_panel_card_view);
        cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                renameDevice();
            }
        });

        btnSendCommand = (Button) findViewById(R.id.btn_send_command);
        btnSendCommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendCommand();
            }
        });

        btnOn = (Button) findViewById(R.id.btnOn);
        btnOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnOnLed();
            }
        });

        btnOff = (Button) findViewById(R.id.btnOff);
        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                turnOffLed();
            }
        });

        btnDisconnect = (Button) findViewById(R.id.btn_disconnect);
        btnDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disconnect();
            }
        });

        progressTxt = (TextView) findViewById(R.id.progress_txt);
        brightness = (SeekBar) findViewById(R.id.seekBar_brightness);
        brightness.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    progressTxt.setText(String.valueOf(progress));
                    try {
                        btSocket.getOutputStream().write(String.valueOf(progress).getBytes());
                    } catch (IOException e) {
                    }
                }
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        new ConnectBT().execute(); //Call the class to connect

    }

    private class ConnectBT extends AsyncTask<Void, Void, Void> {

        private boolean connectSuccess = true;

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(LedControl.this, getString(R.string.progress_dialog_connecting, deviceName), getString(R.string.progress_dialog_please_wait));
        }

        @Override
        protected Void doInBackground(Void... devices) {
            try {
                if (btSocket == null || !isBtConnected) {
                    myBlueTooth = BluetoothAdapter.getDefaultAdapter(); // get the mobile bt device
                    BluetoothDevice connectedDevice = myBlueTooth.getRemoteDevice(address); //connects to the devices address and checks if it's available
                    //deviceName = device.getName(); // Necessary?...
                    btSocket = connectedDevice.createInsecureRfcommSocketToServiceRecord(myUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();

                    btSocket.connect(); // Error here when failing to connect

                }
            } catch (IOException e) {
                connectSuccess = false;
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
            if (!connectSuccess) {
                msg("Connection failed");
                finish();
            } else {
                msg("Connected");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }

    public void renameDevice() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(dialog, null);
        dialogBuilder.setView(dialogView);

        final EditText deviceNameEditText = (EditText) dialogView.findViewById(R.id.alert_dialog_name_text_view);
        dialogBuilder.setTitle(getString(R.string.alert_dialog_header_device));
        dialogBuilder.setPositiveButton(getString(R.string.alert_dialog_done), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if (btSocket != null) {
                    try {
                        btSocket.getOutputStream().write(("AT+NAME" + (deviceNameEditText.getText()).toString()).getBytes());
                        deviceName = deviceNameEditText.getText().toString();
                    } catch (IOException e) {
                        msg("Error. Cannot communicate with bluetooth socket");
                    }
                }
                cardStatus = (TextView) findViewById(R.id.card_control_panel_text_view);
                cardStatus.setText(getString(R.string.status_card_view_cp, deviceName));  // Or use startActivityForResult
            }
        });

        dialogBuilder.setNegativeButton(getString(R.string.alert_dialog_cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //pass
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }

    public void sendCommand() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(dialog, null);
        dialogBuilder.setView(dialogView);

        final EditText deviceNameEditText = (EditText) dialogView.findViewById(R.id.alert_dialog_name_text_view);
        dialogBuilder.setTitle(getString(R.string.alert_dialog_header_command));
        dialogBuilder.setMessage(getString(R.string.alert_dialog_message_command));
        dialogBuilder.setPositiveButton(getString(R.string.alert_dialog_done), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if (btSocket != null) {
                    try {
                        btSocket.getOutputStream().write((deviceNameEditText.getText().toString().toUpperCase()).getBytes());
                    } catch (IOException e) {
                        msg("Error. Cannot communicate with bluetooth socket");
                    }
                }
            }
        });
        dialogBuilder.setNegativeButton(getString(R.string.alert_dialog_cancel), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                //pass
            }
        });
        AlertDialog b = dialogBuilder.create();
        b.show();
    }


    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    public void disconnect() {
        if (btSocket != null) {
            try {
                btSocket.close();
            } catch (IOException e) {
                msg("Error. Cannot communicate with bluetooth socket");
            }
        }
        finish(); // Return to the first layout
    }

    public void turnOffLed() {
        if (btSocket != null) {
            try {
                btSocket.getOutputStream().write("LEDOFF".toString().getBytes());
            } catch (IOException e) {
                msg("Error. Cannot communicate with bluetooth socket");
            }
        }
    }

    public void turnOnLed() {
        if (btSocket != null) {
            try {
                btSocket.getOutputStream().write("LEDON".toString().getBytes());
            } catch (IOException e) {
                msg("Error. Cannot communicate with bluetooth socket");
            }
        }
    }
}





