package com.example.dirtycloud;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.InputStream;
import java.util.ArrayList;
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
                    while (sync_thread.isAlive()) {
                        try {
                            sync_thread.join();
                        }catch(Exception e){
                        }
                    }
                    stop_button.post(new Runnable() {
                        @Override
                        public void run() {
                            stop_button.setEnabled(true);
                        }
                    });
                }
            }).start();
        }
    }

    void log(String msg){
        System.out.print(msg);
        cli_output_line_list.post(new Runnable() {
            @Override
            public void run() {
                for(String line : msg.split("\n")){
                    cli_output_lines.add(line);
                }
                cli_output_line_list.getAdapter().notifyDataSetChanged();
                cli_output_line_list.scrollBy(0, 65535);

                System.out.print(msg);
            }
        });
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
        String nextcloud_dir = String.format("%s/Nextcloud", external_dir);

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

        sync_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                for(String path : subfolder_set) {
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

                    try {
                        process = Runtime.getRuntime().exec(cmd.toArray(new String[0]), env.toArray(new String[0]), getFilesDir());
                        InputStream input_stream = process.getInputStream();
                        InputStream error_stream = process.getErrorStream();
                        while(true){
                            boolean process_done = false;
                            try{
                                process.exitValue();
                                process_done = true;
                            }catch(IllegalThreadStateException e){

                            }

                            for(InputStream stream : new InputStream[]{input_stream, error_stream}) {
                                byte[] buf = new byte[2049];
                                if (stream.available() == 0){
                                    Thread.sleep(100);
                                    continue;
                                }
                                int read_result = stream.read(buf, 0, buf.length - 1);
                                if (read_result == -1) {
                                    continue;
                                }
                                log(new String(buf, 0, read_result));
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
                        log(String.format("process finished with %d\n", process.exitValue()));
                    }catch(Exception e){
                        log(String.format("sync process failed, %s\n", e.toString()));
                        break;
                    }
                }
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