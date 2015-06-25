package com.xiuxiu_zhang.cast_time;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class TimerService extends Service {
	
	//控制线程的开关
	boolean flag = true;
	
	//代表时间
	long mTime = 0;
	
	Intent intent = new Intent();

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	/**
	 * 这个方法是当我们第一次开启服务的时候走过这个方法
	 */
	@Override
	public void onCreate() {
		//用来计时的线程
		new Thread(){
			public void run() {
				while(flag){
					try {
						sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					mTime++;
					intent.setAction("s.s.s.s");
					intent.putExtra("data", mTime);
					sendBroadcast(intent);
				}
			};
		}.start();
		super.onCreate();
	}
	
	/**
	 * 当开启的方式是StartService 走过这个方法
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//参数中的intent就是Activity中启动服务是用到的intent，所以这个intent中
		//携带的flag数据就是Activity中的flag，然后赋值到Service中的flag，使得
		//Activity和Service中两个flag的值保持一致
		flag = intent.getBooleanExtra("flag", false);
		if(flag == false){
			stopSelf();
		}
		return super.onStartCommand(intent, flags, startId);
	}
	
	/**
	 * 当我们停止服务的时候系统走过这个方法
	 */
	@Override
	public void onDestroy() {
		Log.i("123","onDestroy");
		super.onDestroy();
	}

}
