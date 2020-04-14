package no.nordicsemi.android.nrftoolbox.utils;

import android.util.Log;

/**
 * 
 * log管理类
 *
 */
public class LogUtil {

	/**
	 * 是否为开发者模式(开发模式打印LOG,非开发模式不打印LOG)
	 */
	private static boolean mDebug = true;

	private LogUtil() {
	}

	/**
	 * 打印info级别的log
	 * 
	 * @param msg
	 */
	public static void i(String tag, String msg) {
		if (mDebug) {
			Log.i(tag, msg);
		}
	}

	/**
	 * 打印error级别的log
	 * 
	 * @param msg
	 */
	public static void e(String tag, String msg) {
		if (mDebug) {
			Log.e(tag, msg);
		}
	}
}
