package com.example.krando67.todolist;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Button;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    //custom class that holds the title and description of a to-do item
    public class ListItem{
        private String title;
        private String description;
        private ListItem(String titl, String desc) {
            this.title = titl;
            this.description = desc;
        }

        private JSONObject getJSONObject() {
            JSONObject obj = new JSONObject();
            try {
                obj.put("Title", title);
                obj.put("Description", description);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return obj;
        }
    }

    //list that holds the to-do items
    ArrayList<ListItem> theList = new ArrayList<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final Context context = this;
        final EditText new_item_title = findViewById(R.id.new_item_title);
        final EditText new_item_desc = findViewById(R.id.new_item_desc);
        Button addButton = findViewById(R.id.add_button);
        ListView listView = findViewById(R.id.the_list_view);

        String jsonString = readFromFile(this);
        Log.d("JSON RECEIVED", jsonString);
        try {
            if (!jsonString.equals("")) {
                theList.clear();
                JSONArray json2 = new JSONArray(jsonString);
                for (int i = 0; i < json2.length(); i++) {
                    JSONObject jsonobject = json2.getJSONObject(i);
                    ListItem it = new ListItem(jsonobject.getString("Title"), jsonobject.getString("Description"));
                    theList.add(it);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (savedInstanceState != null){
            new_item_title.setText((String) savedInstanceState.getSerializable("new_title"));
            new_item_desc.setText((String) savedInstanceState.getSerializable("new_desc"));
        }

        final ArrayAdapter<ListItem> adapter = new ArrayAdapter<ListItem>(this, R.layout.list_item, R.id.item_title, theList){
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                final View view = super.getView(position, convertView, parent);
                TextView text1 = view.findViewById(R.id.item_title);
                TextView text2 = view.findViewById(R.id.item_desc);
                CheckBox check = view.findViewById(R.id.item_check);
                text1.setText(theList.get(position).title);
                text2.setText(theList.get(position).description);
                final int pos = position;
                check.setOnCheckedChangeListener(null);
                check.setChecked(false);
                check.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView,
                                                 boolean isChecked) {
                        final Handler h = new Handler();
                        h.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                              theList.remove(pos);
                              notifyDataSetChanged();
                                //Write to File in case app is terminated
                                writeToFile(context);
                            }
                        }, 200);

                        Toast.makeText(MainActivity.this, "Item Deleted", Toast.LENGTH_SHORT).show();
                    }
                });
                return view;
            }
        };
        listView.setAdapter(adapter);

        listView.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                theList.remove(position);
                adapter.notifyDataSetChanged();
                //Write to File in case app is terminated
                writeToFile(context);

                Toast.makeText(MainActivity.this, "Item Deleted", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ListItem newLI = new ListItem(new_item_title.getText().toString(), new_item_desc.getText().toString());
                if (!new_item_title.getText().toString().equals("") && !new_item_desc.getText().toString().equals("")) {
                    theList.add(newLI);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(MainActivity.this, "Item Added", Toast.LENGTH_SHORT).show();
                    new_item_title.setText("");
                    new_item_desc.setText("");
                    new_item_title.requestFocus();
                    //Write to File in case app is terminated
                    writeToFile(context);
                }
                else
                    Toast.makeText(MainActivity.this, "Title or Description can't be empty", Toast.LENGTH_SHORT).show();
            }
        });
    }

    //Save user input before pressing the add button in case the user rotates the screen
    @Override
    protected void onSaveInstanceState(Bundle state) {
        EditText new_item_title = findViewById(R.id.new_item_title);
        EditText new_item_desc = findViewById(R.id.new_item_desc);
        state.putSerializable("T", new_item_title.getText().toString());
        state.putSerializable("D", new_item_desc.getText().toString());
        super.onSaveInstanceState(state);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("ON DESTROY", "is called");
        writeToFile(this);

    }

    private void writeToFile(Context context) {
        JSONArray json = new JSONArray();
        for (int i = 0; i < theList.size(); i++){
            json.put(theList.get(i).getJSONObject());
        }
        Log.d("JSON", json.toString());
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("list.txt", MODE_PRIVATE));
            outputStreamWriter.write(json.toString());
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }

    private String readFromFile(Context context) {

        String ret = "";

        try {
            InputStream inputStream = context.openFileInput("list.txt");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("FILE ACTIVITY", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("FILE ACTIVITY", "Can not read file: " + e.toString());
        }

        return ret;
    }
}

