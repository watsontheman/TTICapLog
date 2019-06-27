package com.tti.tticaplog.service;

import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.io.File;
import java.io.IOException;

public class CaptureLogService extends Service {

    private final static String TAG = "===CAPCAP===";
    private CapLogThread cpthread;
    private CheckFileThread cfthread;
    private static String fileName = "";
    private Process capProcess = null;
    private File logFile = null;
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        cpthread = new CapLogThread();
        cfthread = new CheckFileThread();
        cfthread.start();
        cpthread.start();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        while(cfthread.CFHandler==null){
            Log.e(TAG,"wait for the CFHandler to be created..");
        }
        cfthread.CFHandler.sendEmptyMessage(100);
        return Service.START_STICKY;
    }


    private class CheckFileThread extends Thread{
        public Handler CFHandler = null;

        @Override
        public void run() {
            Looper.prepare();
            CFHandler = new Handler(Looper.myLooper()){
                @Override
                public void handleMessage(Message msg) {
                    switch(msg.what){
                        case 100:
                            //msg1 : check if the log file exist, if not then create one;
                            File filedir = new File(Environment.getExternalStorageDirectory()+"/test");
                            //check if the local ditectory exist?
                            if(!filedir.exists()){
                                filedir.mkdir();
                            }
                            File logfile = new File(filedir,"logcat_tti"+".txt");

                            if(!logfile.exists()){
                                Log.e(TAG,"File not exist,create new one");
                                try {
                                    logfile.createNewFile();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }else{
                                Log.e(TAG,"yeeeee, file exist");
                            }
                            logFile = logfile;

                            //send msg to start capture log
                            cpthread.CLHandler.sendEmptyMessage(200);

                            //msg2 : check the file size, if is reach the maximum
                            //printFilePath();
                            break;
                        case 101:
                            long templength = logFile.length();
                            long maxsize = (templength/(1024*1024));
                            if(maxsize<20){
                                Log.e(TAG,"MAXSIZE: "+maxsize+" MB");
                                //Log.e(TAG,"File size : "+ templength);
                                //Log.e(TAG,"==check file size in next 10 sec==");
                                cfthread.CFHandler.sendEmptyMessageDelayed(101,10000);
                            }else{
                                Log.e(TAG,"==file reach maximum size ");
                                cpthread.CLHandler.sendEmptyMessage(201);
                            }
                            break;
                        case 102:
                            //recreate an logFile
                            cfthread.CFHandler.removeMessages(101);
                            if(logFile.exists()){
                                Log.e(TAG,"delete the old one....");

                                {
                                    //backup old one
                                    File oldfile = logFile;
                                    oldfile.renameTo(new File(Environment.getExternalStorageDirectory()+"/test/"+
                                            "logcat_tti_old.txt"));
                                    if(!logFile.exists()){
                                        try {
                                            logFile.createNewFile();
                                        } catch (IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                }

                                if(logFile.delete()){
                                    Log.e(TAG,"Create new Log file");
                                    try {
                                        logFile.createNewFile();
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }
                            cpthread.CLHandler.sendEmptyMessage(200);

                    }

                }
            };
            Looper.loop();
        }
    }

    private class CapLogThread extends Thread{
        public Handler CLHandler = null;

        @Override
        public void run() {
            Looper.prepare();
            CLHandler = new Handler(Looper.myLooper()){
                @Override
                public void handleMessage(Message msg) {
                    //msg1 : capture log to file
                    switch (msg.what){
                        case 200:
                            Log.e(TAG,"---Logcat start--- ");
                            Log.e(TAG,"logfile Name:"+logFile.getName());
                            Log.e(TAG,"Logfile to string: "+ logFile);
                            try {
                                //capProcess = Runtime.getRuntime().exec("logcat -c");
                                capProcess = Runtime.getRuntime().exec("logcat -f "+ logFile);

                                //start to check file size
                                cfthread.CFHandler.sendEmptyMessage(101);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            break;
                        case 201:
                                Log.e(TAG,"cap process STOP, kill process");
                                capProcess.destroy();

                                cfthread.CFHandler.sendEmptyMessage(102);
                            break;

                    }
                    //msg2 : stop capturing, kill the process
                }
            };
            Looper.loop();
        }
    }
    private void printFilePath(){
        //File file = new File(Environment.getExternalStorageDirectory().toString());
        Log.e(TAG,Environment.getExternalStorageDirectory().getAbsolutePath());
    }

}
