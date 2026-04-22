package com.example.dirtycloud;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class Sync extends AppCompatActivity {
    Toolbar toolbar;
    EditText cli_output;
    Thread sync_thread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);

        toolbar = findViewById(R.id.toolbar_sync);
        cli_output = findViewById(R.id.cli_output);

        SharedPreferences global = getSharedPreferences("global", Context.MODE_PRIVATE);
        String server_name = global.getString("server_name", "");
        String user_name = global.getString("user_name", "");
        String app_password = global.getString("app_password", "");
        String native_dir = getApplicationInfo().nativeLibraryDir;
        Set<String> subfolder_set = global.getStringSet("subfolder_list", new HashSet<String>());

        if (subfolder_set.size() != 0){
            sync_thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    for(String path : subfolder_set) {
                        LinkedList<String> cmd = new LinkedList<String>();
                        LinkedList<String> env = new LinkedList<String>();
                        env.add(String.format("NC_USER=%s", user_name));
                        env.add(String.format("NC_PASSWORD=%s", app_password));
                        // TODO this can be made a bit smarter to support more architectures
                        env.add(String.format("LD_LIBRARY_PATH=%s/usr/lib/aarch64-linux-gnu", native_dir));
                        cmd.add(String.format("%s/usr/bin/nextcloudcmd", native_dir));
                        cmd.add("--non-interactive");
                        cmd.add("--path");
                        cmd.add(path);
                        cmd.add(server_name);

                        Process p = new Process();
                        Runtime.exec(cmd.toArray(new String[0]), env.toArray(new String[0]));
                    }




                }
            });
            sync_thread.start();
        }else{

        }

        setSupportActionBar(toolbar);
    }
}