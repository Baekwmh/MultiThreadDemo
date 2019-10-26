package com.example.multithreaddemo;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private static final int START_NUM = 1;
    private static final int ADDING_NUM = 2;
    private static final int ENDING_NUM = 3;
    private static final int CANCEL_NUM = 4;

    private Button other;
    private Button duo;
    private Button yibu;
    private Button handler;
    private Button task;
    private ImageView dog;
    private TextView time;
    private ProgressBar progressBar;
    private MyUIHandler uiHandler = new MyUIHandler(this);
    private static final int MSG_SHOW_PROGRESS = 11;
    private static final int MSG_SHOW_IMAGE = 12;
    private static final String DOWNLOAD_URL = "https://desk-fd.zol-img.com.cn/t_s1920x1080c5/g5/M00/07/07/ChMkJlXw8QmIO6kEABYKy-RYbJ4AACddwM0pT0AFgrj303.jpg";


    static class MyUIHandler extends  Handler{
        private  WeakReference<Activity> ref;
        public  MyUIHandler(Activity activity){
            this.ref = new WeakReference<>(activity);

        }
        public void handleMessage(Message msg){
            super.handleMessage(msg);
            MainActivity activity = (MainActivity) ref.get();
            if (activity == null) {
                return;
            }
            switch (msg.what){
                case MSG_SHOW_PROGRESS:
                    activity.progressBar.setVisibility(View.VISIBLE);

                    break;
                case MSG_SHOW_IMAGE:
                    activity.progressBar.setVisibility(View.GONE);
                    activity.dog.setImageBitmap((Bitmap)msg.obj);
                    break;
            }
        }
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        dog = findViewById(R.id.img);
        time = findViewById(R.id.tv_time);
        progressBar = findViewById(R.id.pb_bar);
        other = findViewById(R.id.btn_other);
        duo = findViewById(R.id.btn_duo);
        yibu = findViewById(R.id.btn_yibu);
        handler = findViewById(R.id.btn_handler);
        task = findViewById(R.id.btn_task);
        progressBar.setOnClickListener(this);
        other.setOnClickListener(this);
        duo.setOnClickListener(this);
        yibu.setOnClickListener(this);
        handler.setOnClickListener(this);
        task.setOnClickListener(this);
        dog.setOnClickListener(this);

    }

    private CalculateThread calculateThread;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_duo:
                calculateThread = new CalculateThread();
                calculateThread.start();
                break;
            case R.id.btn_yibu:
                new MyAsyncTask(this).execute(100);
                break;

            case R.id.btn_handler:
                new Thread(new DownloadImageFetcher(DOWNLOAD_URL)).start();
                break;


            case R.id.btn_task:
                new DownloadImage(this).execute(DOWNLOAD_URL);
                break;

            case R.id.btn_other:
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        other.setText("runOnUiThread方式更新");
                        time.setText("runOnUiThread方式更新TextView的内容");
                    }
                });
                break;

        }
    }

    class CalculateThread extends Thread {
        @Override
        public void run() {
            int result = 0;
            boolean isCancel = false;
            myHandler.sendEmptyMessage(START_NUM);

            for (int i = 0; i < 101; i++) {
                try {
                    Thread.sleep(100);
                    result += i;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    isCancel = true;
                    break;
                }
                if (i % 5 == 0) {
                    Message msg = Message.obtain();
                    msg.what = ADDING_NUM;
                    msg.arg1 = i;
                    myHandler.sendMessage(msg);
                }
            }
            if (!isCancel) {
                Message msg = myHandler.obtainMessage();
                msg.what = ENDING_NUM;
                msg.arg1 = result;
                myHandler.sendMessage(msg);
            }
        }
    }

    private MyHandler myHandler = new MyHandler(this);

    static class MyHandler extends Handler {
        private WeakReference<Activity> ref;

        public MyHandler(Activity activity) {
            this.ref = new WeakReference<>(activity);
        }

        public void handleMessage(Message msg) {
            super.handleMessage(msg);

            MainActivity activity = (MainActivity) ref.get();
            if (activity == null) {
                return;
            }
            switch (msg.what) {
                case START_NUM:
                    activity.progressBar.setVisibility(View.VISIBLE);
                    break;
                case ADDING_NUM:
                    activity.progressBar.setProgress(msg.arg1);
                    activity.time.setText("计算已完成" + msg.arg1 + "%");
                    break;
                case ENDING_NUM:
                    activity.progressBar.setVisibility(View.GONE);
                    activity.time.setText("计算已完成，结果为：" + msg.arg1);
                    activity.myHandler.removeCallbacks(activity.calculateThread);
                    break;
                case CANCEL_NUM:
                    activity.progressBar.setProgress(0);
                    activity.progressBar.setVisibility(View.GONE);
                    activity.time.setText("计算已取消");
                    break;
            }
        }
    }

private  class DownloadImageFetcher implements Runnable{
private String img;
public DownloadImageFetcher(String strUrl){
    this.img = strUrl;
}
    @Override
    public void run() {
        InputStream in = null;

        uiHandler.obtainMessage(MSG_SHOW_PROGRESS).sendToTarget();
        try{
            URL url = new URL(img);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            in = connection.getInputStream();
            Bitmap bitmap = BitmapFactory.decodeStream(in);
            Message msg = uiHandler.obtainMessage();
            msg.what = MSG_SHOW_IMAGE;
            msg.obj = bitmap;
            uiHandler.sendMessage(msg);

        }catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (in != null){
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }
    }

}
    static class MyAsyncTask extends AsyncTask<Integer, Integer, Integer> {
        private WeakReference<AppCompatActivity> ref;

        public MyAsyncTask(AppCompatActivity activity) {
            this.ref = new WeakReference<>(activity);
        }

        // 执行线程任务前的操作
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            MainActivity activity = (MainActivity) this.ref.get();
            activity.progressBar.setVisibility(View.VISIBLE);
        }

        // 接收输入参数、执行任务中的耗时操作、返回线程任务执行的结果
        @Override
        protected Integer doInBackground(Integer... params) {
            int sleep = params[0];
            int result = 0;

            for (int i = 0; i < 101; i++) {
                try {
                    Thread.sleep(sleep);
                    result += i;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                if (i % 5 == 0) {
                    publishProgress(i);
                }

                if (isCancelled()) {
                    break;
                }
            }
            return result;
        }

        // 在主线程中显示线程任务执行的进度
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);

            MainActivity activity = (MainActivity) this.ref.get();
            activity.progressBar.setProgress(values[0]);
            activity.time.setText("计算已完成" + values[0] + "%");
        }

        // 接收线程任务执行结果、将执行结果显示到UI组件
        @Override
        protected void onPostExecute(Integer result) {
            super.onPostExecute(result);
            MainActivity activity = (MainActivity) this.ref.get();
            activity.time.setText("已计算完成，结果为：" + result);
            activity.progressBar.setVisibility(View.GONE);
        }

        // 将异步任务设置为：取消状态
        @Override
        protected void onCancelled() {
            super.onCancelled();

            MainActivity activity = (MainActivity) this.ref.get();
            activity.time.setText("计算已取消");

            activity.progressBar.setProgress(0);
            activity.progressBar.setVisibility(View.GONE);
        }
    }

    static class DownloadImage extends AsyncTask<String, Bitmap, Bitmap> {
        private WeakReference<AppCompatActivity> ref;

        public DownloadImage(AppCompatActivity activity) {
            this.ref = new WeakReference<>(activity);
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            MainActivity activity = (MainActivity) this.ref.get();
        }

        @Override
        protected Bitmap doInBackground(String... params) {
            String url = params[0];
            return downloadImage(url);
        }

        private Bitmap downloadImage(String strUrl) {
            InputStream stream = null;
            Bitmap bitmap = null;

            MainActivity activity = (MainActivity) this.ref.get();
            ByteArrayOutputStream bos = new ByteArrayOutputStream();

            try {
                URL url = new URL(strUrl);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                int totalLen = connection.getContentLength();
                if (totalLen == 0) {
                    activity.progressBar.setProgress(0);
                }

                if (connection.getResponseCode() == 200) {
                    stream = connection.getInputStream();
//                    bitmap = BitmapFactory.decodeStream(stream);

                    int len = -1;
                    int progress = 0;
                    byte[] tmps = new byte[1024];
                    while ((len = stream.read(tmps)) != -1) {
                        progress += len;
                        activity.progressBar.setProgress(progress);
                        bos.write(tmps, 0, len);
                    }
                    bitmap = BitmapFactory.decodeByteArray(bos.toByteArray(), 0, bos.size());
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
            return bitmap;
        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);

            MainActivity activity = (MainActivity) this.ref.get();
            if (bitmap != null) {
                activity.dog.setImageBitmap(bitmap);
            }
        }
    }
}

