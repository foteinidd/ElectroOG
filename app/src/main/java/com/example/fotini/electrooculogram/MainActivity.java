package com.example.fotini.electrooculogram;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.SharedPreferences;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.androidplot.util.Redrawer;
import com.androidplot.xy.AdvancedLineAndPointRenderer;
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.XYPlot;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.WeakReference;
import java.util.Calendar;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    //Blinking counter & notifier
    static int count = 0;
    static boolean flag = false;

    //Bluetooth
    static String value = null;

    private BluetoothSocket bt_socket;
    private OutputStream out_stream;
    private InputStream in_stream;
    boolean kill_worker = false;
    Handler handler = new Handler();

    TextView text; //Displays value received from Bluetooth module

    static boolean f = true; //used to avoid counting one eye-blink incident twice

    //Plot
    XYPlot plot;
    /**
     * Uses a separate thread to modulate redraw frequency.
     */
    Redrawer redrawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        text = (TextView) findViewById(R.id.textView6);  //Displays value received from Bluetooth module

        //Bluetooth
        BluetoothAdapter bt_adapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice bt_device = bt_adapter.getRemoteDevice("98:D3:31:20:60:CB");

        bt_adapter.cancelDiscovery();
        try {
            bt_socket = bt_device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            bt_socket.connect();
            out_stream = bt_socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        startListeningThread();
        startTimer();

        //Plot
        //Initialize our XYPlot reference
        plot = (XYPlot) findViewById(R.id.plot);

        ECGModel ecgSeries = new ECGModel(100, 2);

        //Add a new series to the XYPlot
        MyFadeFormatter formatter =new MyFadeFormatter(100); //trailSize: number of samples per screen
        formatter.setLegendIconEnabled(false);
        plot.addSeries(ecgSeries, formatter);
        plot.setRangeBoundaries(20, 500, BoundaryMode.FIXED);
        plot.setDomainBoundaries(0, 100, BoundaryMode.FIXED);


        //Reduce the number of range labels
        plot.setLinesPerRangeLabel(3);

        //Start generating ECG data in the background:
        ecgSeries.start(new WeakReference<>(plot.getRenderer(AdvancedLineAndPointRenderer.class)));

        //Set a redraw rate of 30hz and start immediately:
        redrawer = new Redrawer(plot, 30, true);

        //Add History button
        Button button = (Button)findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener()    {
            @Override
            public void onClick(View v) {
                goToHistoryActivity();
            }
        });

        //Update Blinking TextViews (counter & date of last eye-blink)
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //updateTextViews(flag);
                            if (flag) {
                                TextView text = (TextView) findViewById(R.id.textView);
                                text.setText(Integer.toString(count));// view in the text

                                //Print current date and time - USED FOR TESTING
                                TextView textView = (TextView) findViewById(R.id.textView4);
                                textView.setText(getDateTime());

                                share();
                            }
                        }
                    });
                } catch (Exception e)    {
                    e.printStackTrace();
                }
            }
        }, 0, 100);

        /*
        //Increment button - USED FOR TESTING
        Button button2 = (Button)findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener()    {
            @Override
            public void onClick(View v) {

                count++;
                TextView text = (TextView)findViewById(R.id.textView);
                text.setText(Integer.toString(count));// view in the text

                //Print current date and time - USED FOR TESTING
                TextView textView = (TextView)findViewById(R.id.textView4);
                textView.setText(getDateTime());

                share();
            }
        });
        */
    }

    //Send data to ECGModel (to plot)
    protected static Integer sendData()    {
        if (value != null) {
            Integer integ = Integer.parseInt(value);
            if (integ < 90)
            {
                f = true;
            }
            if (integ != null) {
                if (integ > 100)    {
                    if (f == true) {
                        count++;
                        f = false;
                        flag = true;
                    }
                }
                else    {
                    flag = false;
                }
                return integ;
            } else {
                flag = false;
                return 0;
            }
        }
        else    {
            flag = false;
            return 0;
        }
    }

    //Get incoming data from Bluetooth module
    public void startListeningThread() {
        try {
            in_stream = bt_socket.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Thread worker = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted() && !kill_worker) {
                    try {
                        //Read the incoming response
                        while (in_stream.available() > 0) {
                            final char command = (char) in_stream.read();
                            char next;
                            String value = "";
                            next = (char) in_stream.read();
                            while (next != command) {
                                value += next;
                                next = (char) in_stream.read();
                            }

                            //Send command back to the main thread
                            final String finalValue = value;
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    updateText(finalValue);
                                }
                            });
                        }
                    } catch (IOException e) {
                        kill_worker = true;
                    }
                }
            }
        });

        worker.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            bt_socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Store new incoming data & show new value on screen
    public void updateText(String data) {
        if (data != null) {
            value = data;
            text.setText(data);
        }
    }

    //Take samples every 500 milliseconds
    private void startTimer() {
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                getValue();
            }
        }, 0, 500);
    }

    //Send character 'g' to request new value from Bluetooth module
    public void getValue() {
        try {
            out_stream.write('g');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Get blinking date and time
    private static String getDateTime()   {
        return java.text.DateFormat.getDateTimeInstance().format(Calendar.getInstance().getTime());
    }

    //Share blinking times - To be inserted to history list
    private void share() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("string_id"+Integer.toString(count), getDateTime());
        editor.putInt("number", count);
        editor.apply();
    }

    //Button - BLINKING HISTORY
    private void goToHistoryActivity()  {
        Intent showHistory = new Intent(this, HistoryActivity.class);
        startActivity(showHistory);
    }
}
