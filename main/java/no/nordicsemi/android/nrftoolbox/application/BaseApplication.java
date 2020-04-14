package no.nordicsemi.android.nrftoolbox.application;

import android.app.Application;
import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;


public class BaseApplication extends Application {

    private static Context mContext;
    public static RequestQueue queue;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
        // initial Volley
        queue = Volley.newRequestQueue(mContext);
    }
    public static RequestQueue getHttpQueue() {
        return queue;
    }
}
