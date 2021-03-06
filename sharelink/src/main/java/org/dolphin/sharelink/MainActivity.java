package org.dolphin.sharelink;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.dolphin.http.server.FileBean;
import org.dolphin.http.server.Main;
import org.dolphin.job.Job;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private ListView listView;
    private TextView tv;
    FileAdapter fileAdapter;
//    private SharedDiskManager sharedDiskManager;
    private SharedDisk currSharedDisk = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.listView = (ListView) findViewById(R.id.list_view);
        tv = (TextView) findViewById(R.id.empty_tip);

        listView.setEmptyView(tv);
        fileAdapter = new FileAdapter(null);
        listView.setAdapter(fileAdapter);
        fileAdapter.notifyDataSetChanged();

//        sharedDiskManager = SharedDiskManager.instance();
//        sharedDiskManager.addObserver(sharedDiskChangedObserver);
//        sharedDiskManager.start();
        SharedDisk disk = new SharedDisk("192.168.1.107", "" + Main.PORT);
        visite(disk);
        listView.setOnItemClickListener(onItemClickListener);
    }

    private Job.Callback1<List<SharedDisk>> sharedDiskChangedObserver = new Job.Callback1<List<SharedDisk>>() {
        @Override
        public void call(List<SharedDisk> result) {
            if (null == currSharedDisk && result.size() > 0) {
                currSharedDisk = result.get(0);
                visite(currSharedDisk);
            }
        }
    };

    private void visite(SharedDisk sharedDisk) {
        Log.d(TAG, "Visit disk " + sharedDisk.toString());
        sharedDisk.queryFileList("video", fileListQueryObservre);
    }

    private Job.Callback1<List<FileBean>> fileListQueryObservre = new Job.Callback1<List<FileBean>>() {
        @Override
        public void call(List<FileBean> result) {
            fileAdapter.setFiles(result.toArray(new FileBean[0]));
            fileAdapter.notifyDataSetChanged();
        }
    };

    private AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            FileBean fileBean = (FileBean) fileAdapter.getItem(i);
            String url = fileBean.url;
            Log.d(TAG, "Open file " + url);
            Uri uri = Uri.parse(url);
            Intent intent1 = new Intent(Intent.ACTION_VIEW);
            android.util.Log.v("URI:::::::::", uri.toString());
            intent1.setDataAndType(uri, fileBean.type);
            startActivity(intent1);
        }
    };


    private class FileAdapter extends BaseAdapter {
        private FileBean[] files;

        private FileAdapter(FileBean[] files) {
            this.files = files;
        }

        public void setFiles(FileBean[] files) {
            this.files = files;
        }

        @Override
        public int getCount() {
            return null != files ? files.length : 0;
        }

        @Override
        public Object getItem(int i) {
            return files[i];
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            TextView tv = new TextView(MainActivity.this);
            tv.setText("" + files[i].name);
            tv.setTextSize(20);
            return tv;
        }
    }
}
