package com.xiuxiu_zhang.cast_time;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class TimerService extends Service {
	
	//�����̵߳Ŀ���
	boolean flag = true;
	
	//����ʱ��
	long mTime = 0;
	
	Intent intent = new Intent();

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	/**
	 * ��������ǵ����ǵ�һ�ο��������ʱ���߹��������
	 */
	@Override
	public void onCreate() {
		//������ʱ���߳�
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
	 * �������ķ�ʽ��StartService �߹��������
	 */
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		//�����е�intent����Activity�������������õ���intent���������intent��
		//Я����flag���ݾ���Activity�е�flag��Ȼ��ֵ��Service�е�flag��ʹ��
		//Activity��Service������flag��ֵ����һ��
		flag = intent.getBooleanExtra("flag", false);
		if(flag == false){
			stopSelf();
		}
		return super.onStartCommand(intent, flags, startId);
	}
	
	/**
	 * ������ֹͣ�����ʱ��ϵͳ�߹��������
	 */
	@Override
	public void onDestroy() {
		Log.i("123","onDestroy");
		super.onDestroy();
	}

}
