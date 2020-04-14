package no.nordicsemi.android.nrftoolbox.network;

import android.content.Context;

import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;

import org.json.JSONObject;

import no.nordicsemi.android.nrftoolbox.utils.AESUtils;

public abstract class VolleyInterface {

	public Context mContext;
	public static Listener<JSONObject> mListener;
	public static ErrorListener mErrorListener;

	public VolleyInterface(Context context, Listener<JSONObject> listener,
						   ErrorListener errorListener) {
		this.mContext = context;
		this.mListener = listener;
		this.mErrorListener = errorListener;
	}

	public Listener<JSONObject> loadingListener() {
		mListener = arg0 -> {
            // TODO Auto-generated method stub
            try {
                onMySuccess(new JSONObject(AESUtils.decrypt(
                        arg0.optString("result"), "wo.szzhkjyxgs.20")));
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        };
		//呢道係成功傳送既地方，over!!
		return mListener;

	}

	public ErrorListener errorListener() {
		mErrorListener = new ErrorListener() {

			@Override
			public void onErrorResponse(VolleyError arg0) {
				// TODO Auto-generated method stub
				onMyError(arg0);
			}
		};
		return mErrorListener;

	}

	public abstract void onMySuccess(JSONObject result);

	public abstract void onMyError(VolleyError arg0);
}
