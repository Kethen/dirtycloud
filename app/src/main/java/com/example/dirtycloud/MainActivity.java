package com.example.dirtycloud;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.google.android.material.textfield.TextInputEditText;

import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    TextInputEditText server_name;
    TextInputEditText user_name;
    TextInputEditText app_password;
    RecyclerView subfolder_list;

    LinkedList<String> path_list;

    Button subfolder_add_button;
    Button sync_button;

    public static class SubFolderListItem extends RecyclerView.ViewHolder{
        TextInputEditText path;
        Button remove_button;
        int index;

        public SubFolderListItem(View view){
            super(view);

            path = view.findViewById(R.id.path);
            remove_button = view.findViewById(R.id.remove_button);
        }

        public String get_path(){
            return path.toString();
        }

        public Button get_remove_button() {
            return remove_button;
        }

        public TextInputEditText get_edit_text() {
            return path;
        }

        public void set_index(int i){
            index = i;
        }

        public int get_index(){
            return index;
        }
    }
    public static class SubFolderListAdapter extends RecyclerView.Adapter<SubFolderListItem>{
        LinkedList<String> path_list;
        public SubFolderListAdapter(LinkedList<String> p){
            path_list = p;
        }

        @Override
        public SubFolderListItem onCreateViewHolder(ViewGroup viewGroup, int viewType){
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.folder_list_item, viewGroup, false);
            SubFolderListItem item = new SubFolderListItem(view);

            return item;
        }

        @Override
        public void onBindViewHolder(SubFolderListItem list_item, final int position) {
            list_item.get_remove_button().setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    if (path_list.size() == 1){
                        path_list.set(0, "");
                    }else {
                        path_list.remove(position);
                    }
                    notifyDataSetChanged();
                }
            });
            TextInputEditText path = list_item.get_edit_text();
            path.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View view, boolean b) {
                    if (b){
                        return;
                    }
                    if (position >= path_list.size()){
                        return;
                    }
                    path_list.set(position, path.getText().toString());
                }
            });
            if (position < path_list.size()) {
                path.setText(path_list.get(position));
            }
            list_item.set_index(position);
        }

        @Override
        public int getItemCount() {
            return path_list.size();
        }
    }

    void save_input(){
        SharedPreferences global = getSharedPreferences("global", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = global.edit();
        editor.putString("server_name", server_name.getText().toString());
        editor.putString("user_name", user_name.getText().toString());
        editor.putString("app_password", app_password.getText().toString());
        Set<String> subfolder_set = new HashSet<String>();
        for(String item : path_list){
            if (item.trim().length() != 0)
                subfolder_set.add(item);
        }
        editor.putStringSet("subfolder_list", subfolder_set);
        editor.commit();
    }

    void load_input(){
        SharedPreferences global = getSharedPreferences("global", Context.MODE_PRIVATE);
        server_name.setText(global.getString("server_name", ""));
        user_name.setText(global.getString("user_name", ""));
        app_password.setText(global.getString("app_password", ""));
        path_list.clear();
        HashSet<String> default_set = new HashSet<String>();
        default_set.add("");
        Set<String> config_set = global.getStringSet("subfolder_list", default_set);
        if (config_set.size() == 0){
            config_set = default_set;
        }
        for(String item : config_set){
            path_list.add(item);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            path_list.sort(new Comparator<String>() {
                @Override
                public int compare(String lhs, String rhs) {
                    return lhs.compareTo(rhs);
                }
            });
        }
    }

    Toolbar toolbar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        path_list = new LinkedList<String>();

        toolbar = findViewById(R.id.toolbar);
        server_name = findViewById(R.id.server_address);
        user_name = findViewById(R.id.user_name);
        app_password = findViewById(R.id.app_password);
        subfolder_list = findViewById(R.id.subfolder_list);
        subfolder_list.setLayoutManager(new LinearLayoutManager(this));
        subfolder_list.setAdapter(new SubFolderListAdapter(path_list));

        sync_button = findViewById(R.id.sync);
        subfolder_add_button = findViewById(R.id.subfolder_add);

        subfolder_add_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                path_list.add("");
                subfolder_list.getAdapter().notifyDataSetChanged();
            }
        });

        load_input();
        setSupportActionBar(toolbar);
    }

    @Override
    protected void onPause() {
        super.onPause();
        save_input();
    }
}