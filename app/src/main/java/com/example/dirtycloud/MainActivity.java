package com.example.dirtycloud;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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
    Button refresh_rootfs_button;

    void extract_rootfs(){
        String files_dir = String.format("%s/rootfs", getFilesDir().toString());
        File file_check = new File(files_dir);
        if (file_check.exists()){
            System.out.print("rootfs exists, skipping extraction\n");
            return;
        }
        String cpu = "";
        try {
            Process p = Runtime.getRuntime().exec("getprop ro.product.cpu.abi");
            p.waitFor();
            byte[] buf = new byte[1024];
            int read_status = p.getInputStream().read(buf, 0, buf.length);
            cpu = new String(buf, 0, read_status - 1);
        }catch(Exception e){
            System.out.print(String.format("failed fetching cpu architecture, %s", e.toString()));
            return;
        }
        String filename = "i386.tar";
        if (cpu.equals("armeabi-v7a") || cpu.equals("arm64-v8a")) {
            filename = "armhf.tar";
        }else if (cpu.equals("x86_64") || cpu.equals("x86")){

        }else{
            System.out.print(String.format("got unexpected cpu model %s\n", cpu));
            return;
        }
        System.out.print(String.format("extracting %s for %s\n", filename, cpu));
        try {
            InputStream rootfs_istream = getAssets().open(filename);
            File temp_file = File.createTempFile("rootfs", null, getCacheDir());
            temp_file.deleteOnExit();
            OutputStream temp_ostream = new FileOutputStream(temp_file);
            while(true){
                byte[] buf = new byte[2048];
                int read_status = rootfs_istream.read(buf);
                if (read_status == -1){
                    break;
                }
                temp_ostream.write(buf, 0, read_status);
            }
            temp_ostream.close();


            // need somewhat modern java nio to make directory otherwise
            Runtime.getRuntime().exec(String.format("mkdir -p %s", files_dir)).waitFor();

            LinkedList<String> cmd = new LinkedList<>();
            cmd.add("/system/bin/tar");
            cmd.add("-C");
            cmd.add(files_dir);
            cmd.add("-xvf");
            cmd.add(temp_file.getPath());

            LinkedList<String> env = new LinkedList<>();

            for(String s : cmd){
                System.out.println(s);
            }

            Process p = Runtime.getRuntime().exec(cmd.toArray(new String[0]), env.toArray(new String[0]), getFilesDir());
            InputStream input_stream = p.getInputStream();
            InputStream error_stream = p.getErrorStream();

            for(InputStream stream : new InputStream[]{input_stream, error_stream}) {
                while (true) {
                    byte[] buf = new byte[2049];
                    int read_status = stream.read(buf, 0, buf.length - 1);
                    if (read_status == -1) {
                        break;
                    }
                    System.out.print(new String(buf, 0, read_status));
                }
            }
            p.waitFor();
            System.out.print(String.format("extraction from %s to %s finished with %d\n", temp_file.getPath().toString(), getFilesDir().toString(), p.exitValue()));
            temp_file.delete();

            String native_dir = getApplicationInfo().nativeLibraryDir;
        }catch(Exception e){
            System.out.print(String.format("failed extracting system root, %s\n", e.toString()));
            System.out.print(e.getStackTrace().toString());
            return;
        }

    }
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
            int index = position;
            list_item.get_remove_button().setOnClickListener(new View.OnClickListener(){
                @Override
                public void onClick(View v){
                    if (path_list.size() == 1){
                        path_list.set(0, "");
                    }else {
                        path_list.remove(index);
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
            path.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                @Override
                public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                    if (position >= path_list.size()){
                        return false;
                    }
                    path_list.set(position, path.getText().toString());
                    return false;
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

    void refresh_rootfs(){
        try {
            Runtime.getRuntime().exec(String.format("rm -rf %s/rootfs", getFilesDir())).waitFor();
        }catch(Exception e){

        }
        Runtime.getRuntime().exit(0);
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
        refresh_rootfs_button = findViewById(R.id.refresh_rootfs);

        subfolder_add_button.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                path_list.add("");
                subfolder_list.getAdapter().notifyDataSetChanged();
            }
        });

        MainActivity current_activity = this;
        sync_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                save_input();
                Intent sync_intent = new Intent(current_activity, Sync.class);
                current_activity.startActivity(sync_intent);
            }
        });

        refresh_rootfs_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                refresh_rootfs();
            }
        });

        load_input();
        setSupportActionBar(toolbar);

        extract_rootfs();
    }

    @Override
    protected void onPause() {
        super.onPause();
        save_input();
    }
}