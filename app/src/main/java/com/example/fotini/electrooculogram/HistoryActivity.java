package com.example.fotini.electrooculogram;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.io.FileOutputStream;
import java.util.ArrayList;

public class HistoryActivity extends AppCompatActivity
{
    private ListView m_listview;

    ArrayList<String> dates_times = new ArrayList<String>();
    ArrayAdapter<String> adapter;
    String[] data = new String[1000];
    int c; //counter

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        m_listview = (ListView) findViewById(R.id.listView);

        updateList();

        saveLogToFile();
    }

    //Update Blinking History list
    private void updateList()   {

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        c = prefs.getInt("number", 0);

        for (int i = 0; i < c; i++)   {
            data[i] = prefs.getString("string_id"+Integer.toString(i+1), "no id"); //default value: no id

            adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, dates_times);
            m_listview.setAdapter(adapter);
            adapter.add((i+1)+". "+data[i]);
            adapter.notifyDataSetChanged();

        }

    }

    //Save history log to csv file
    private void saveLogToFile() {
        String FILENAME = "data.csv";
        String entry = "";
        for (int i = 0; i < c - 1; i++) {
            entry += data[i] + ",";
        }
        entry += data[c - 1] + "\n";

        try {
            FileOutputStream out = openFileOutput(FILENAME, Context.MODE_APPEND);
            out.write(entry.getBytes());
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
