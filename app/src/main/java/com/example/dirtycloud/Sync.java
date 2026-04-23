package com.example.dirtycloud;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

public class Sync extends AppCompatActivity {
    Toolbar toolbar;

    Thread sync_thread;
    boolean stop_thread;
    Process process;
    Button stop_button;

    ArrayList<String> cli_output_lines;

    RecyclerView cli_output_line_list;

    OutputStream sync_log_ostream;

    void stop_sync(){
        if (sync_thread != null && sync_thread.isAlive()){
            stop_thread = true;
            if (process != null){
                process.destroy();
            }
            stop_button.setEnabled(false);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (sync_thread != null && sync_thread.isAlive()) {
                        try {
                            sync_thread.join();
                        }catch(Exception e){
                            System.out.print(String.format("thread join exception %s\n", e.toString()));
                        }
                    }
                    stop_button.post(new Runnable() {
                        @Override
                        public void run() {
                            stop_button.setEnabled(true);
                            finish();
                        }

                    });
                }
            }).start();
        }
    }

    void log(String msg){
        System.out.print(msg);
        String[] lines = msg.split("\n");
        cli_output_line_list.post(new Runnable() {
            @Override
            public void run() {
                for(String line : lines){
                    cli_output_lines.add(line);
                }
                cli_output_line_list.getAdapter().notifyDataSetChanged();
                cli_output_line_list.scrollBy(0, 65535);
            }
        });
    }

    void log_sync(String msg){
        System.out.print(msg);
        try {
            sync_log_ostream.write(msg.getBytes(Charset.forName("UTF-8")));
        }catch(Exception e){
            System.out.print(String.format("failed writing log message, %s", e.toString()));
        }
    }

    public static class CliOutputLine extends RecyclerView.ViewHolder {
        TextView text;

        public CliOutputLine(View view){
            super(view);
            text = view.findViewById(R.id.console_output_line_text);
        }

        public void set_text(String t){
            text.setText(t);
        }
    }

    public static class CliOutputLineListAdapter extends RecyclerView.Adapter<CliOutputLine>{
        private ArrayList<String> console_output_lines;

        public CliOutputLineListAdapter(ArrayList<String> l){
            console_output_lines = l;
        }

        @Override
        public CliOutputLine onCreateViewHolder(ViewGroup viewGroup, int viewType) {
            View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.console_output_line, viewGroup, false);
            return new CliOutputLine(view);
        }

        @Override
        public void onBindViewHolder(CliOutputLine line, final int position) {
            line.set_text(console_output_lines.get(position));
        }

        @Override
        public int getItemCount() {
            return console_output_lines.size();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sync);

        toolbar = findViewById(R.id.toolbar_sync);
        stop_button = findViewById(R.id.stop_button);
        cli_output_line_list = findViewById(R.id.cli_output_line_list);

        SharedPreferences global = getSharedPreferences("global", Context.MODE_PRIVATE);
        String server_name = global.getString("server_name", "");
        String user_name = global.getString("user_name", "");
        String app_password = global.getString("app_password", "");
        String native_dir = getApplicationInfo().nativeLibraryDir;
        Set<String> subfolder_set = global.getStringSet("subfolder_list", new HashSet<String>());
        String external_dir = getExternalFilesDir(null).toString();
        String files_dir = getFilesDir().toString();
        String rootfs_dir = String.format("%s/rootfs", files_dir);
        String nextcloud_dir = String.format("/sdcard/DirtyCloud", external_dir);
        String sync_log_path = String.format("%s/sync_log.txt", nextcloud_dir);

        stop_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stop_sync();
            }
        });

        cli_output_lines = new ArrayList<String>();
        cli_output_line_list.setLayoutManager(new LinearLayoutManager(this));
        cli_output_line_list.setAdapter(new CliOutputLineListAdapter(cli_output_lines));

        setSupportActionBar(toolbar);

        if (subfolder_set.size() == 0){
            log("no subfolder to sync!\n");
            return;
        }

        if (server_name.trim().length() == 0){
            log("server address is empty!\n");
            return;
        }

        if (user_name.trim().length() == 0){
            log("user name is empty\n");
            return;
        }

        if (app_password.trim().length() == 0){
            log("app password is empty!\n");
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if(!Environment.isExternalStorageManager()){
                String package_name = getApplicationContext().getPackageName();
                Intent settings_intent = new Intent();
                settings_intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                settings_intent.addCategory("android.intent.category.DEFAULT");
                settings_intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                startActivity(settings_intent);
                finish();
            }
        }

        try {
            Runtime.getRuntime().exec(String.format("mkdir -p %s", nextcloud_dir)).waitFor();
            sync_log_ostream = new BufferedOutputStream(new FileOutputStream(new File(sync_log_path), false), 4096);
        }catch(Exception e){
            log(String.format("failed creating nextcloud dir and log file, %s\n", e.toString()));
            return;
        }
        log(String.format("sync begin, log can be found at %s\n", sync_log_path));

        sync_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                long begin_ms = (new Date()).getTime();
                for(String path : subfolder_set) {
                    long path_begin_ms = (new Date()).getTime();
                    if (stop_thread){
                        log("sync stopped\n");
                        break;
                    }

                    String nextcloud_subdir = String.format("%s/%s", nextcloud_dir, path);
                    try {
                        Runtime.getRuntime().exec(String.format("mkdir -p %s", nextcloud_subdir)).waitFor();
                    }catch(Exception e){
                        log(String.format("cannot create subdir %s, %s", nextcloud_subdir, e.toString()));
                        break;
                    }

                    LinkedList<String> cmd = new LinkedList<String>();
                    LinkedList<String> env = new LinkedList<String>();

                    env.add(String.format("PROOT_LOADER=%s/loader", native_dir));
                    env.add(String.format("PROOT_LOADER_32=%s/loader32", native_dir));
                    env.add(String.format("PROOT_TMP_DIR=%s/tmp", rootfs_dir));
                    env.add(String.format("LD_LIBRARY_PATH=%s", native_dir));
                    env.add(String.format("PATH=/usr/bin:/usr/sbin"));
                    cmd.add(String.format("%s/proot", native_dir));
                    cmd.add("-r");
                    cmd.add(rootfs_dir);
                    cmd.add("-w");
                    cmd.add("/");
                    cmd.add("--kill-on-exit");
                    cmd.add("-b");
                    cmd.add(nextcloud_dir);

                    /*
                    cmd.add("/usr/bin/uname");
                    cmd.add("-a");
                    */

                    //env.add(String.format("NC_USER=%s", user_name));
                    //env.add(String.format("NC_PASSWORD=%s", app_password));
                    cmd.add("/usr/bin/nextcloudcmd");
                    cmd.add("-u");
                    cmd.add(user_name);
                    cmd.add("-p");
                    cmd.add(app_password);
                    cmd.add("--non-interactive");
                    cmd.add("--path");
                    cmd.add(path);
                    cmd.add(nextcloud_subdir);
                    cmd.add(server_name);

                    /*
                    for(String e : env){
                        System.out.println(e);
                    }
                    for(String c : cmd){
                        System.out.println(c);
                    }
                    */

                    String begin_message = String.format("begin sync of %s\n", path);
                    log(begin_message);
                    log_sync(begin_message);

                    try {
                        process = Runtime.getRuntime().exec(cmd.toArray(new String[0]), env.toArray(new String[0]), getFilesDir());
                        InputStream input_stream = new BufferedInputStream(process.getInputStream(), 4096);
                        InputStream error_stream = new BufferedInputStream(process.getErrorStream(), 4096);
                        while(true){
                            boolean process_done = false;
                            try{
                                process.exitValue();
                                process_done = true;
                            }catch(IllegalThreadStateException e){

                            }

                            for(InputStream stream : new InputStream[]{input_stream, error_stream}) {
                                while(stream.available() != 0) {
                                    byte[] buf = new byte[2049];
                                    int read_result = stream.read(buf, 0, buf.length - 1);
                                    if (read_result == -1) {
                                        break;
                                    }
                                    log_sync(new String(buf, 0, read_result));
                                }
                                Thread.sleep(100);
                            }
                            if (stop_thread){
                                process.destroy();
                                break;
                            }
                            if (process_done){
                                break;
                            }
                        }
                        process.waitFor();
                        long path_time_spent_ms = (new Date()).getTime() - path_begin_ms;
                        String finish_message = String.format("sync of %s finished with %d, took %d ms\n", path, process.exitValue(), path_time_spent_ms);
                        log_sync(finish_message);
                        log(finish_message);
                    }catch(Exception e){
                        String error_message = String.format("sync of %s failed, %s", path, e.toString());
                        log_sync(error_message);
                        log(error_message);
                        break;
                    }
                }
                long time_spent_ms = (new Date()).getTime() - begin_ms;
                String end_message = String.format("sync finished, took %d ms\n", time_spent_ms);
                log_sync(end_message);
                log(end_message);
                try{
                    sync_log_ostream.flush();
                }catch(Exception e){

                }
                stop_button.post(new Runnable() {
                    @Override
                    public void run() {
                        stop_button.setEnabled(false);
                    }
                });
            }
        });
        stop_thread = false;
        sync_thread.start();
    }

    @Override
    protected void onDestroy() {
        stop_sync();
        super.onDestroy();
    }
}