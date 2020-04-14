package no.nordicsemi.android.nrftoolbox.network;

import android.content.Context;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;

import org.json.JSONObject;

import no.nordicsemi.android.nrftoolbox.application.BaseApplication;
import no.nordicsemi.android.nrftoolbox.utils.AESUtils;

public class VolleyRequest {

	public static Context context;
	public static JsonObjectRequest objectRequest;
	public static String AESContent = "";
	private static JSONObject postObject;

	public static void RequestPost(Context mContext, String url, String tag,
                                   JSONObject jsonObject, VolleyInterface vif) {
		//歡迎黎到VolleyRequest,首先要加密 (๑╹ᆺ╹)

		try {
			AESContent = AESUtils.encrypt(jsonObject.toString(), "wo.szzhkjyxgs.20");
			//加密左之後,要再次放入去新既JSONObject
			postObject = new JSONObject();
			//將個\n轉左做""
			postObject.put("body", AESContent.replace("\n", ""));

		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



		//用JsonObjectRequest 儲存傳送Data喇 ღවꇳවღ
		//要提供5樣野，1）POST/GET  2）IP同你個Port  3）D Data  4）Vif既"解密"處理  5）Vif既error處理
		objectRequest = new JsonObjectRequest(Method.POST, url, postObject,
				vif.loadingListener(), vif.errorListener());

		//將JsonObjectRequest加埋Tag
		objectRequest.setTag(tag);

		//將JsonObjectRequest加重試既機制
		objectRequest.setRetryPolicy(new DefaultRetryPolicy(300*1000, 0, 1.0f));
		//用之前import左既BaseApplication既getHTTPQueue ADD 入去 RequestQueue
		RequestQueue queue = BaseApplication.getHttpQueue();
		queue.add(objectRequest);
		//BaseApplication.getHttpQueue().add(objectRequest);
		//開始傳送啦 (∩｀-´)⊃━✿✿✿✿✿✿
		BaseApplication.getHttpQueue().start();


	}
}
