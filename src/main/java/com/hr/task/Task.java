package com.hr.task;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import com.hr.server.JobServer;

public class Task {

	/**
	 * 定时自动投简历
	 */
	public static void autoExe() {
		// 7天总毫秒数
		long daySpan = 7 * 24 * 60 * 60 * 1000;
		// 规定运行时间
		final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// 首次运行时间
		final Date startTime;
		try {
			startTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(sdf.format(new Date()));
			// 如果今天的已经过了 首次运行时间就改为下次
			// if (System.currentTimeMillis() > startTime.getTime()) {
			// startTime = new Date(startTime.getTime() + daySpan);
			// }
			Timer t = new Timer();
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
					try {
						new JobServer().start();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			};
			long delay = 5 * 60 * 1000;// 延迟
			t.scheduleAtFixedRate(task, startTime, daySpan + delay);// 51Job投递频率为一周，故延迟五分钟。
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		autoExe();
	}
}
