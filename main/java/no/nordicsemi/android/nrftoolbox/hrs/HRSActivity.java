/*
 * Copyright (c) 2015, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE
 * USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package no.nordicsemi.android.nrftoolbox.hrs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Point;
import android.media.AudioDeviceCallback;
import android.os.Bundle;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;

import com.android.volley.VolleyError;
import com.chaquo.python.PyObject;
import com.srplab.www.starcore.StarCoreFactory;
import com.srplab.www.starcore.StarCoreFactoryPath;
import com.srplab.www.starcore.StarObjectClass;
import com.srplab.www.starcore.StarServiceClass;
import com.srplab.www.starcore.StarSrvGroupClass;

import org.achartengine.GraphicalView;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.UUID;


import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.nrftoolbox.CallUtils;
import no.nordicsemi.android.nrftoolbox.FeaturesActivity;
import no.nordicsemi.android.nrftoolbox.R;
import no.nordicsemi.android.nrftoolbox.hts.HTSActivity;
import no.nordicsemi.android.nrftoolbox.network.VolleyInterface;
import no.nordicsemi.android.nrftoolbox.network.VolleyRequest;
import no.nordicsemi.android.nrftoolbox.profile.BleProfileActivity;
import no.nordicsemi.android.nrftoolbox.profile.BleProfileService;
import no.nordicsemi.android.nrftoolbox.profile.BleProfileServiceReadyActivity;
import no.nordicsemi.android.nrftoolbox.profile.LoggableBleManager;
import no.nordicsemi.android.nrftoolbox.uart.UARTInterface;
import no.nordicsemi.android.nrftoolbox.uart.UARTService;
import no.nordicsemi.android.nrftoolbox.utils.MyTime;
import static android.widget.Toast.LENGTH_LONG;
import static no.nordicsemi.android.nrftoolbox.CallUtils.callSimpleInfo;
import static no.nordicsemi.android.nrftoolbox.CallUtils2.callSimpleInfo2;
import static no.nordicsemi.android.nrftoolbox.IIRFilter.callIIRFilterInfo;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import no.nordicsemi.android.nrftoolbox.uart.UARTActivity;
import no.nordicsemi.android.nrftoolbox.uart.UARTService;

/**
 * HRSActivity is the main Heart rate activity. It implements HRSManagerCallbacks to receive callbacks from HRSManager class. The activity supports portrait and landscape orientations. The activity
 * uses external library AChartEngine to show real time graph of HR values.
 **/

// TODO The HRSActivity should be rewritten to use the service approach, like other do.
public class HRSActivity extends BleProfileServiceReadyActivity<UARTService.UARTBinder> implements HRSManagerCallbacks {
	private UARTService.UARTBinder mServiceBinder;
	@SuppressWarnings("unused")
	private final String TAG = "HRSActivity";
	private static final String TAG2 = "EcgMeasure";
	private final static String GRAPH_STATUS = "graph_status";
	private final static String GRAPH_COUNTER = "graph_counter";
	private final static String HR_VALUE = "hr_value";
	public static String BASE_URL = "http://47.52.98.62:8089/zh";
	private final static int REFRESH_INTERVAL = 1; // 12 ms interval

	private Handler mHandler = new Handler();
	int rececount=0;
	private boolean isGraphInProgress = false;
	int shiftattail = 0;
	private GraphicalView mGraphViewled1,mGraphViewled2,mGraphViewled3,mGraphViewled4,mGraphViewecg,mGraphViewps,mGraphViewGsensor,mGraphViewGsensorX,mGraphViewGsensorY,mGraphViewGsensorZ;
	private LineGraphView mLineGraphled1;
	private LineGraphView_1 mLineGraphled2;
	private LineGraphView_2 mLineGraphled3;
	private LineGraphView_3 mLineGraphled4;
	private LineGraphView_4 mLineGraphecg;
	private LineGraphView_ps mLineGraphps;
	private LineGraphView_x mLineGraphGsensor;
	private LineGraphView_gx mLineGraphGsensorX;
	private LineGraphView_gy mLineGraphGsensorY;
	private LineGraphView_gz mLineGraphGsensorZ;
	int happycount = 0,gsensorcounter = 0;
	boolean checkbt = false;
	int sizer = 0;
	int savePPG1 = 0,PPGdiff=0,PPGdiff2=0,savecount=0;
	String rrS="";
	private TextView mHRSValue, mHRSPosition,mHRSint,mGValue,TestValue;
	private TextView mBatteryLevelView;
	private  List<Integer> rrIntervals_get;
	int sizerget = 0;
	boolean readOnce = true;
	int readOncecount = 0;
	int ninedatacount = 0;
	List<Integer> nine_get;
	String temhex;
	private static final String FILE_NAME = "data.txt";
	private int mHrmValue = 0,mX = 0,mY = 0,mZ = 0;
	String testing = "",testing1 = "",testing2 = "";
	String b1 = "",b2 = "",b3 = "",b4 = "",b5 = "",b6 = "",b7 = "",b8 = "",b9 = "";
	String b2f="",b2b="",b5f="",b5b="",b8f="",b8b="";
	String c1="",c2="",c3="",c4="",c5="",c6="";
	String hex1="",hex2="",hex3="",hex4="",hex5="",hex6="";
	int PPG1=0,PPG2=0,PPG3=0,PPG4=0;
	String fhex="",bhex="",ohex="",Outhex="";
	int Savepos = -1;
	int shiftpos = -1;
	String hex1_2="",hextem="";
	int printcount =0;
	String matchstring ="";
	int dataCountReceive = 0;
	private int mCounter = 0,mCounter1=0,mCounter2=0,mCounter3=0,mCounter4=0,mCounter5=0,mXCounter=0,mYCounter=0,mZCounter=0,mCounterps=0;
	private Button test_bt,upload_bt,record_bt;
	public BluetoothDevice devicesave;
	private BluetoothGatt gattsend;
	String mixhex="";
	int yeahcount=0;
	public String measure_time,c_yearTime,c_monthTime,dayTime,hourTime,minTime,secTime;
	private String c_uid = "00",c_heart="82",c_systolic="22",c_diastolic="",c_year="44";
	private TextView measure_state;
	private int c_sampling_accuracy = 100;
	private int c_frequency = 24;
	private String loutput="/",lh1="/",lh2="",lh3="",lh4="",lh5="",lh6="",ln1="",ln2="",ln3="",ln4="",ln5="",ln6="";
	//MycuteData
	private StringBuilder my_PPG = new StringBuilder("");
	private StringBuilder my_PPGOrg = new StringBuilder("");
	private StringBuilder my_HrmValue = new StringBuilder("");
	private StringBuilder my_mX= new StringBuilder("");
	private StringBuilder my_mY= new StringBuilder("");
	private StringBuilder my_mZ = new StringBuilder("");
	StringBuilder hex_listget = new StringBuilder("");
	String yeahstring="";
	double [] arraytesting = {1000000, 1500000, 3000000, 4000000, 5500000, 6600000,1000000, 1500000, 3000000, 4000000, 5500000, 6600000,1000000, 1500000, 1000000, 1500000, 3000000, 4000000, 5500000, 6600000,1000000, 1500000, 3000000, 4000000, 5500000, 6600000,1000000, 1500000,1000000, 1500000, 3000000, 4000000, 5500000, 6600000,1000000, 1500000, 3000000, 4000000, 5500000, 6600000,1000000, 1500000,1000000, 1500000, 3000000, 4000000, 5500000, 6600000,1000000, 1500000, 3000000, 4000000, 5500000, 6600000,1000000, 1500000,1000000, 1500000, 3000000, 4000000, 5500000, 6600000,1000000, 1500000, 3000000, 4000000, 5500000, 6600000,1000000, 1500000,1000000, 1500000, 3000000, 4000000, 5500000, 6600000,1000000, 1500000, 3000000, 4000000, 5500000, 6600000,1000000, 1500000,1000000, 1500000, 3000000, 4000000, 5500000, 6600000,1000000, 1500000, 3000000, 4000000, 5500000, 6600000,1000000, 1500000,1000000, 1500000, 3000000, 4000000, 5500000, 6600000,1000000, 1500000, 3000000, 4000000, 5500000, 6600000,1000000, 1500000,1000000, 1500000, 3000000, 4000000, 5500000, 6600000,1000000, 1500000, 3000000, 4000000, 5500000, 6600000,1000000, 1500000,1000000, 1500000, 3000000, 4000000, 5500000, 6600000,1000000, 1500000, 3000000, 4000000, 5500000, 6600000,1000000, 1500000,1000000, 1500000, 3000000, 4000000, 5500000, 6600000,1000000, 1500000, 3000000, 4000000, 5500000, 6600000,1000000, 1500000};
	public ArrayList<Integer>DataReceivedArray = new ArrayList<Integer>();

	public double[] myECGArray = new double[5000];
	public double[] myECGArrayFilter = new double[5000];
	public double[] myECGArray100Filter = new double[125];
	public ArrayList<Integer>myECGGraph2500 = new ArrayList<Integer>();

	public double[] myLED1Array100Filter = new double[125];
	public double[] myLED2Array100Filter = new double[125];
	public double[] myLED3Array100Filter = new double[125];
	public double[] myLED4Array100Filter = new double[125];
	public ArrayList<Integer>myECGPeakArray = new ArrayList<Integer>();
	public ArrayList<Integer>myECGValueArray = new ArrayList<Integer>();
	public int ecgvaluecount = 0;

	public ArrayList<Integer>myPSPeakArray = new ArrayList<Integer>();
	public ArrayList<Integer>myPSValueArray = new ArrayList<Integer>();

	public double [] myLED1Array = new double[5000];
	public double[] myLED1ArrayFilter = new double[5000];
	public ArrayList<Double>myLED1FilterUse = new ArrayList<Double>();
	public ArrayList<Double>myLED1Save = new ArrayList<Double>();
	public double[] myLED1FilterUsetem = new double[125];
	public double[] myLED1FilterW = new double[125];
	public ArrayList<Integer>myLED1PeakArray = new ArrayList<Integer>();

	public double[] myLED2Array = new double[5000];
	public double[] myLED2ArrayFilter = new double[5000];
	public double[] myLED3Array = new double[5000];
	public double[] myLED3ArrayFilter = new double[5000];
	public double[] myLED4Array = new double[5000];
	public double[] myLED4ArrayFilter = new double[5000];
	public int myGsensorX = 0,myGsensorY = 0,myGsensorZ = 0;

	public ArrayList<Integer>myGsensorXArray = new ArrayList<Integer>();
	public ArrayList<Integer>myGsensorYArray = new ArrayList<Integer>();
	public ArrayList<Integer>myGsensorZArray = new ArrayList<Integer>();
	final int runtimemax = 50000;
	public List<PyObject> led1_list,led1_listx,ecg_list,ecg_listx,test_list;
	//EM Filtering
	public int datacount = 0;
	final public int windowsize = 5;
	//led1
	public int led1sum = 0;
	public ArrayList<Integer>Window1ValueArray = new ArrayList<Integer>();
	//public ArrayList<Double>Window1ValuedArray = new ArrayList<Double>();
	public ArrayList<Double>Window1ValueArrayF = new ArrayList<Double>();
	//led2
	public int led2sum = 0;
	public ArrayList<Integer>Window2ValueArray = new ArrayList<Integer>();
	//led3
	public int led3sum = 0;
	public ArrayList<Integer>Window3ValueArray = new ArrayList<Integer>();
	//led4
	public int led4sum = 0;
	public ArrayList<Integer>Window4ValueArray = new ArrayList<Integer>();
	//ecg
	public int ecgsum = 0;
	public ArrayList<Integer>WindowecgValueArray = new ArrayList<Integer>();
	//ps
	public int pssum = 0;
	public ArrayList<Integer>WindowpsValueArray = new ArrayList<Integer>();
	public int countecgget = 0;
	final int barhost = 125;
	final int eachhost = 25;
	boolean firstrun = false;
	ViewGroup layoutGsensorX;
	ViewGroup layoutGsensorY;
	ViewGroup layoutGsensorZ;
	ViewGroup layoutecg;
	ViewGroup layoutled1;
	ViewGroup layoutled2;
	ViewGroup layoutled3;
	ViewGroup layoutled4;
	ViewGroup layoutps;
	public UARTActivity uartAct = new UARTActivity();;


	//Below is finding the monmention.
	public ArrayList<Integer>GXArray = new ArrayList<Integer>();
	public ArrayList<Integer>GYArray = new ArrayList<Integer>();
	public ArrayList<Integer>GZArray = new ArrayList<Integer>();


	String strLine = "Oh";
	public StarSrvGroupClass SrvGroup;
	public PyObject pyf;
	@Override
	protected void onCreateView(final Bundle savedInstanceState) {
		final UARTInterface uart = uartAct;
		setContentView(R.layout.activity_feature_hrs);
		setGUI();
		if(! Python.isStarted() ){
			Python.start(new AndroidPlatform(getApplicationContext()));
		}

		Python py = Python.getInstance();

		pyf = py.getModule("myscript"); //py name
		loadingdata();

		upload_bt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				//layoutps.setVisibility(View.VISIBLE);
				//uploadHealthyData();
				//saveFile();
				clearGraph();
				//PyObject obj_ledtest = pyf.callAttr("PPGfindPeak");
				//test_list = obj_ledtest.asList();
				//PPGHandler(test_list.toString());


				//Filter all signal
				PyObject obj_led1 = pyf.callAttr("PPGFilter",myLED1ArrayFilter);
				led1_list = obj_led1.asList();
				PyObject obj_ecg = pyf.callAttr("ECGFilter",myECGArrayFilter);
				ecg_list = obj_ecg.asList();

				for(int i=0;i<5000;i++){
					myLED1ArrayFilter[i]=led1_list.get(i).toDouble();
					myECGArrayFilter[i]= ecg_list.get(i).toDouble();
				}

				PyObject obj_peakecg = pyf.callAttr("ECGPeak",myECGArrayFilter);

				PyObject obj_peakppg1 = pyf.callAttr("PPGPeak",myLED1ArrayFilter);
				//PyObject obj_peakppg1 = pyf.callAttr("ECGPeak",myLED1ArrayFilter);

				for(int i=0;i<obj_peakecg.asList().size();i++){
					int tempeak;
					tempeak = Integer.valueOf(obj_peakecg.asList().get(i).toString());
					myECGPeakArray.add(tempeak);
				}
				for(int i=1;i<myECGPeakArray.size();i++){
					myECGValueArray.add(15000/(int)(myECGPeakArray.get(i)-myECGPeakArray.get(i-1)));
				}

				for(int i=0;i<obj_peakppg1.asList().size();i++){
					int tempeak;
					tempeak = Integer.valueOf(obj_peakppg1.asList().get(i).toString());
					myLED1PeakArray.add(tempeak);
				}
				//Count PTP



				//Real Time Testing
				/*
				Thread thread1 = new Thread(new Runnable() {
					public void run() {
				for(int i =0 ;i<200;i++) {
					for (int j = 0; j < 25; j++) {
						if(i<5){
							//just print out
							myLED1FilterUse.add(myLED1ArrayFilter[i*25+j]);
							//updateled1Graph((int)myLED1ArrayFilter[i*25+j]);
							myLED1Save.add(myLED1ArrayFilter[i*25+j]);
                            int temsize = i*25+j;
                            runOnUiThread(() ->mGValue.setText(String.valueOf(temsize)));
						}else{

							//First In
							if(j==0){
							    for(int x=0;x<125;x++){
                                    myLED1FilterUsetem[x] = myLED1FilterUse.get(x);
                                }

							    PyObject obj_led1 = pyf.callAttr("PPGFilter",myLED1FilterUsetem);
								led1_list = obj_led1.asList();
								//Remove Wfilterlist from 125 to 100
								for(int k=0;k<25;k++) {
									myLED1FilterUse.remove(0);
								}
                               // Toast.makeText(getBaseContext(), Integer.toString(myLED1FilterUse.size()), Toast.LENGTH_SHORT).show();

							}

							double temled1data = led1_list.get(99+j).toDouble();
							updateled1Graph((int)temled1data);
							myLED1Save.add(temled1data);
							myLED1FilterUse.add(myLED1ArrayFilter[i*25+j]);
							int temsize = i*25+j;
                            runOnUiThread(() ->mGValue.setText(String.valueOf(temsize)));
							//mGValue.setText(String.valueOf(myLED1ArrayFilter[i*25+j]));
							updateled2Graph((int)myLED1ArrayFilter[i*25+j]);
						}

						try {
							Thread.sleep(4);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}


					}
				}
					}
				});
				thread1.start();

				*/

			}

			//Toast.
			// makeText(getBaseContext(), myLED1PeakArray.toString(), Toast.LENGTH_SHORT).show();
			//Toast.makeText(getBaseContext(), Arrays.toString(myLED1ArrayFilter), Toast.LENGTH_SHORT).show();



			//resetHandler();
			//Toast.makeText(getBaseContext(), appLib, Toast.LENGTH_SHORT).show();
			//System.loadLibrary("use_ndk_build");
			//Toast.makeText(getBaseContext(), callSimpleInfo(), Toast.LENGTH_SHORT).show();
			//Toast.makeText(getBaseContext(), , Toast.LENGTH_SHORT).show();


			//callSimpleInfo2(myECGArrayFilter);
			//callSimpleInfo2(myLED1ArrayFilter);
			//callSimpleInfo2(myLED2ArrayFilter);
			//callSimpleInfo2(myLED3ArrayFilter);
			//callSimpleInfo2(myLED4ArrayFilter);
			//callIIRFilterInfo(myLED1ArrayFilter);
			//startShowGraph();

/*
				Thread thread2 = new Thread(new Runnable() {
					public void run() {
						for (int i = 0; i < runtimemax; i++) {
							try {

								if(myGsensorXArray.size()>i) {
									updateGsensorXGraph(myGsensorXArray.get(i));
									updateGsensorYGraph(myGsensorYArray.get(i));
									updateGsensorZGraph(myGsensorZArray.get(i));
								}else{
									i--;
								}


								Thread.sleep(1);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				});
				thread2.start();
*/
			//Toast.makeText(getBaseContext(), Arrays.toString(arraytesting) , Toast.LENGTH_SHORT).show();
			//callSimpleInfo();

		});

		record_bt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {

				/*
				checkbt = true;


                PyObject obj_led1 = pyf.callAttr("PPGFilter",myLED1ArrayFilter);
                led1_list = obj_led1.asList();

                PyObject obj_ecg = pyf.callAttr("ECGFilter",myECGArrayFilter);
                ecg_list = obj_ecg.asList();
                int barhost = 125;
				int eachhost = 25;
				int resthost = 5000/eachhost;
                Thread thread2 = new Thread(new Runnable() {
                    public void run() {
                    for(int i=0;i<resthost;i++){

                    	for(int j=0;j<eachhost;j++){

							//For Full host
							updateled2Graph((int)myECGArrayFilter[i*eachhost+j]);
							myECGArrayFilter[i*eachhost+j]=ecg_list.get(i*eachhost+j).toDouble();
							updateled3Graph((int)myECGArrayFilter[i*eachhost+j]);


							if(i<barhost/eachhost){
                                    myECGArray100Filter[i * eachhost + j] = myECGArray[i * eachhost + j];
							}
							if(i>=barhost/eachhost) {
								//25 host
								if (j == 0) {
									PyObject obj_ecgx = pyf.callAttr("ECGFilter", myECGArray100Filter);
									ecg_listx = obj_ecgx.asList();

									//shift array to left 25
									for(int x = 0;x<barhost-eachhost;x++){
										myECGArray100Filter[x]=myECGArray100Filter[x+eachhost];
									}
								}
								updateled4Graph((int) ecg_listx.get(j+85).toDouble());
								//add last 25
								myECGArray100Filter[barhost-eachhost+j] = myECGArray[i * eachhost + j];
								try {
									Thread.sleep(1);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}

						}
                    }
                    }
                });
				thread2.start();
				*/

				/*
				Thread thread1 = new Thread(new Runnable() {
					public void run() {
						for (int i = 0; i < 5000; i++) {
							try {
								myECGArrayFilter[i]=ecg_list.get(i).toDouble();
								PPGOrgHandler(String.valueOf(myLED1ArrayFilter[i]));
								PPGHandler(String.valueOf(myECGArrayFilter[i]));
								updateecgGraph((int)myECGArrayFilter[i]);
								updateled1Graph((int)myLED1ArrayFilter[i]);
								//updateled2Graph(myLED2ArrayFilter[i]);
								//updateled3Graph(myLED3ArrayFilter[i]);
								//updateled4Graph(myLED4ArrayFilter[i]);
								Thread.sleep(1);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				});
				thread1.start();
				*/
				//resetHandler();
				//Intent intent = new Intent(HRSActivity.this, HTSActivity.class);
				//intent.putExtra("DataType", 2);
				//startActivity(intent);

			}
		});
/*
		test_bt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				Thread thread1 = new Thread(new Runnable() {
					public void run() {
						for(int i=0;i<5000;i++){

							updateecgGraph((int)myECGArrayFilter[i]);
							updateled1Graph((int)myLED1ArrayFilter[i]);

							try {
								Thread.sleep(5);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}

					}
				});
				thread1.start();

				layoutps.setVisibility(View.GONE);

				byte[] data_to_write = "FUCK".getBytes();
				//BluetoothGatt mBG = mDevice.connectGatt(....);
				//BluetoothGattService mSVC = mBG.getService(service_uuid);



				try {
					File fold=new File("/sdcard/PPGdata.txt");
					fold.delete();
					File myFile = new File("/sdcard/PPGdata.txt");
					myFile.createNewFile();
					FileOutputStream fOut = new FileOutputStream(myFile);
					OutputStreamWriter myOutWriter =
							new OutputStreamWriter(fOut);
					myOutWriter.append(my_PPG.toString());
					myOutWriter.close();
					fOut.close();
					Toast.makeText(getBaseContext(),
							"Done writing SD 'PPGdata.txt'",
							Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					Toast.makeText(getBaseContext(), e.getMessage(),
							Toast.LENGTH_SHORT).show();
				}

				try {
					File fold=new File("/sdcard/OrgPPGdata.txt");
					fold.delete();
					File myFile = new File("/sdcard/OrfPPGdata.txt");
					myFile.createNewFile();
					FileOutputStream fOut = new FileOutputStream(myFile);
					OutputStreamWriter myOutWriter =
							new OutputStreamWriter(fOut);
					myOutWriter.append(my_PPGOrg.toString());
					myOutWriter.close();
					fOut.close();
					Toast.makeText(getBaseContext(),
							"Done writing SD 'OrgPPGdata.txt'",
							Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					Toast.makeText(getBaseContext(), e.getMessage(),
							Toast.LENGTH_SHORT).show();
				}


			}
		});

		*/

		test_bt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				uart.send("yeah");
				uartAct.send("yeah2");
				if (mServiceBinder != null) {
					mServiceBinder.send("yeah3");
				}
			}
		});
	}


	private void setGUI() {
		mLineGraphled1 = LineGraphView.getLineGraphView();
		mLineGraphled2 = LineGraphView_1.getLineGraphView();
		mLineGraphled3 = LineGraphView_2.getLineGraphView();
		mLineGraphled4 = LineGraphView_3.getLineGraphView();
		mLineGraphecg = LineGraphView_4.getLineGraphView();
		mLineGraphps = LineGraphView_ps.getLineGraphView();
		mLineGraphGsensor = LineGraphView_x.getLineGraphView();
		mLineGraphGsensorX = LineGraphView_gx.getLineGraphView();
		mLineGraphGsensorY = LineGraphView_gy.getLineGraphView();
		mLineGraphGsensorZ = LineGraphView_gz.getLineGraphView();

		record_bt = (Button)findViewById(R.id.record_bt2);
		upload_bt = (Button)findViewById(R.id.upload_bt);
		mHRSValue = findViewById(R.id.text_hrs_value);
		TestValue = findViewById(R.id.test_reading);
		mGValue = findViewById(R.id.text_xyz_value);
		mHRSPosition = findViewById(R.id.text_hrs_position);
		//mHRSint = findViewById(R.id.text_hrs_int);
		mBatteryLevelView = findViewById(R.id.battery);
		test_bt = (Button)findViewById(R.id.test_bt);
		//measure_state = (TextView)findViewById(R.id.measure_state);
		showGraph();


	}




	private void showGraph() {
		/*
		mGraphViewGsensor = mLineGraphGsensor.getView(this);
		ViewGroup layoutGsensor = findViewById(R.id.graph_Gsensor);
		layoutGsensor.addView(mGraphViewGsensor);
		*/

		// This is the moving place
		mGraphViewGsensorX = mLineGraphGsensorX.getView(this);
		layoutGsensorX = findViewById(R.id.graph_GsensorX);
		layoutGsensorX.addView(mGraphViewGsensorX);
		//layoutGsensorX.setVisibility(View.GONE);

		mGraphViewGsensorY = mLineGraphGsensorY.getView(this);
		layoutGsensorY = findViewById(R.id.graph_GsensorY);
		layoutGsensorY.addView(mGraphViewGsensorY);
		//layoutGsensorY.setVisibility(View.GONE);

		mGraphViewGsensorZ = mLineGraphGsensorZ.getView(this);
		layoutGsensorZ = findViewById(R.id.graph_GsensorZ);
		layoutGsensorZ.addView(mGraphViewGsensorZ);
		//layoutGsensorZ.setVisibility(View.GONE);

		mGraphViewecg = mLineGraphecg.getView(this);
		layoutecg = findViewById(R.id.graph_ecg);
		layoutecg.addView(mGraphViewecg);
		layoutecg.setVisibility(View.GONE);

		mGraphViewps = mLineGraphps.getView(this);
		layoutps = findViewById(R.id.graph_ps);
		layoutps.addView(mGraphViewps);

		mGraphViewled1 = mLineGraphled1.getView(this);
		layoutled1 = findViewById(R.id.graph_led1);
		layoutled1.addView(mGraphViewled1);
		//layoutled1.setVisibility(View.GONE);

		mGraphViewled2 = mLineGraphled2.getView(this);
		layoutled2 = findViewById(R.id.graph_led2);
		layoutled2.addView(mGraphViewled2);
		//layoutled2.setVisibility(View.GONE);

		mGraphViewled3 = mLineGraphled3.getView(this);
		layoutled3 = findViewById(R.id.graph_led3);
		layoutled3.addView(mGraphViewled3);
		//layoutled3.setVisibility(View.GONE);

		mGraphViewled4 = mLineGraphled4.getView(this);
		layoutled4 = findViewById(R.id.graph_led4);
		layoutled4.addView(mGraphViewled4);
		//layoutled4.setVisibility(View.GONE);

	}

	protected LoggableBleManager<HRSManagerCallbacks> initializeManager() {
		final HRSManager manager = HRSManager.getInstance(getApplicationContext());
		manager.setGattCallbacks(this);
		return manager;
	}

	@Override
	protected void onStart() {
		super.onStart();

		final Intent intent = getIntent();
		if (!isDeviceConnected() && intent.hasExtra(FeaturesActivity.EXTRA_ADDRESS)) {
			final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
			final BluetoothDevice device = bluetoothAdapter.getRemoteDevice(getIntent().getByteArrayExtra(FeaturesActivity.EXTRA_ADDRESS));
			onDeviceSelected(device, device.getName());
			intent.removeExtra(FeaturesActivity.EXTRA_APP);
			intent.removeExtra(FeaturesActivity.EXTRA_ADDRESS);
		}
	}

	@Override
	protected void onRestoreInstanceState(@NonNull final Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);

		isGraphInProgress = savedInstanceState.getBoolean(GRAPH_STATUS);
		mCounter = savedInstanceState.getInt(GRAPH_COUNTER);
		mHrmValue = savedInstanceState.getInt(HR_VALUE);

		//if (isGraphInProgress) startShowGraph();
	}

	@Override
	protected void onSaveInstanceState(final Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putBoolean(GRAPH_STATUS, isGraphInProgress);
		outState.putInt(GRAPH_COUNTER, mCounter);
		outState.putInt(HR_VALUE, mHrmValue);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		//stopShowGraph();
	}


	//Here is the UART


	@Override
	protected void onServiceBound(UARTService.UARTBinder binder) {
		mServiceBinder = binder;
	}

	@Override
	protected void onServiceUnbound() {
		mServiceBinder = null;
	}

	@Override
	protected Class<? extends BleProfileService> getServiceClass() {
		return UARTService.class;
	}



	@Override
	protected int getLoggerProfileTitle() {
		return R.string.hrs_feature_title;
	}

	@Override
	protected int getAboutTextId() {
		return R.string.hrs_about_text;
	}

	@Override
	protected int getDefaultDeviceName() {
		return R.string.hrs_default_name;
	}

	@Override
	protected UUID getFilterUUID() {
		return HRSManager.HR_SERVICE_UUID;
	}
/*
//Example of Multi Graph on 1 diagram
	private void updateGraph(final int hrmValue,int mXget,int mYget,int mZget) {
		mCounter++;
		//HRM Graph
		mLineGraph.addValue(new Point(mCounter, hrmValue));
		mGraphView.repaint();
		//G Sensor Graph
		mLineGraphxyz.addValue(new Point(mCounter,mXget));
		mGraphViewxyz.repaint();

		mLineGraphxyz2.addValue(new Point(mCounter,mYget));
		mGraphViewxyz2.repaint();

		mLineGraphled4.addValue(new Point(mCounter,mZget));
		mGraphViewled4.repaint();

		mLineGraphecg.addValue(new Point(mCounter,mZget));
		mGraphViewecg.repaint();
	}
	*/

	private void updateGsensorGraph(final int mXget,int mYget,int mZget) {
		mCounter++;
		//G Sensor Graph
		mLineGraphGsensor.addValue(new Point(mCounter,mXget),new Point(mCounter,mYget),new Point(mCounter,mZget));
		mGraphViewGsensor.repaint();
	}

	private void updateGsensorXGraph(final int mXValue){
		mXCounter++;
		mLineGraphGsensorX.addValue(new Point (mXCounter,mXValue));
		mGraphViewGsensorX.repaint();

	}

	private void updateGsensorYGraph(final int mYValue){
		mYCounter++;
		mLineGraphGsensorY.addValue(new Point (mYCounter,mYValue));
		mGraphViewGsensorY.repaint();

	}
	private void updateGsensorZGraph(final int mZValue){
		mZCounter++;
		mLineGraphGsensorZ.addValue(new Point (mZCounter,mZValue));
		mGraphViewGsensorZ.repaint();

	}


	private void updateecgGraph(final int ecgValue) {
		mCounter1++;
		//ECG Graph
		boolean vaild = false;
		if(myECGPeakArray.contains(mCounter1)){
			vaild = true;
			if(ecgvaluecount< myECGValueArray.size()-1) {
				runOnUiThread(() -> mHRSValue.setText(String.valueOf(myECGValueArray.get(ecgvaluecount))+"/bpm"));
			}
			ecgvaluecount++;
		}
		mLineGraphecg.addValue(new Point(mCounter1, ecgValue),vaild);
		mGraphViewecg.repaint();
	}


	private void updatepsGraph(final int psValue) {
		mCounterps++;
		//PS Graph
		boolean vaild = false;
		mLineGraphps.addValue(new Point(mCounterps, psValue),vaild);
		mGraphViewps.repaint();
	}


	private void updateled1Graph(final int led1Value) {
		mCounter2++;
		//GPPG Graph
		boolean vaild = false;
		if(myLED1PeakArray.contains(mCounter2)){
			vaild = true;
		}
		mLineGraphled1.addValue(new Point(mCounter2, led1Value),vaild);
		mGraphViewled1.repaint();
	}


	private void updateled2Graph(final int led2Value) {
		mCounter3++;
		//BPPG Graph
		mLineGraphled2.addValue(new Point(mCounter3, led2Value));
		mGraphViewled2.repaint();
	}


	private void updateled3Graph(final int led3Value) {
		mCounter4++;
		//IPPG Graph
		mLineGraphled3.addValue(new Point(mCounter4, led3Value));
		mGraphViewled3.repaint();
	}
	private void updateled4Graph(final int led4Value) {
		mCounter5++;
		//YPPG Graph
		mLineGraphled4.addValue(new Point(mCounter5, led4Value));
		mGraphViewled4.repaint();
	}



	/*
        private Runnable mRepeatTask = new Runnable() {
            @Override
            public void run() {
                    PPGdiff = savePPG1 - PPG1;
                    if(PPGdiff<0){
                        PPGdiff = - PPGdiff;
                    }
                    if(PPGdiff<500) {
                        if(PPG1>0) {
                            updateGraph(PPG1, PPG2, PPG3, PPG4);
                        }
                    }
                    savePPG1 = PPG1;
                if (isGraphInProgress)
                    mHandler.postDelayed(mRepeatTask, REFRESH_INTERVAL);
            }
        };

        void startShowGraph() {
            isGraphInProgress = true;
            mRepeatTask.run();
        }

        void stopShowGraph() {
            isGraphInProgress = false;
            mHandler.removeCallbacks(mRepeatTask);
        }
    */

	@Override
	public void onServicesDiscovered(@NonNull final BluetoothDevice device, final boolean optionalServicesFound) {
		// this may notify user or show some views
	}

	@Override
	public void onDeviceReady(@NonNull final BluetoothDevice device) {
		devicesave = device;

		//startShowGraph();
	}

	@Override
	public void onBatteryLevelChanged(@NonNull final BluetoothDevice device, final int batteryLevel) {
		runOnUiThread(() -> mBatteryLevelView.setText(getString(R.string.battery, batteryLevel)));
	}

	@Override
	public void onBodySensorLocationReceived(@NonNull final BluetoothDevice device, final int sensorLocation) {
		runOnUiThread(() -> {
			if (sensorLocation >= SENSOR_LOCATION_FIRST && sensorLocation <= SENSOR_LOCATION_LAST) {
				mHRSPosition.setText(getResources().getStringArray(R.array.hrs_locations)[sensorLocation]);
			} else {
				mHRSPosition.setText(R.string.hrs_location_other);
			}
		});
	}



	@Override
	public void onHeartRateMeasurementReceived(@NonNull final BluetoothDevice device, final int heartRate,
											   @Nullable final Boolean contactDetected,
											   @Nullable final Integer energyExpanded,
											   @Nullable final List<Integer> rrIntervals) {


		String test, output = "/", h1 = "/", h2 = "", h3 = "", h4 = "", h5 = "", h6 = "", n1 = "", n2 = "", n3 = "", n4 = "", n5 = "", n6 = "";
		String outputprint;
		b1 = "";
		b2 = "";
		b2f = "";
		b2b = "";
		b3 = "";
		b4 = "";
		b5 = "";
		b5f = "";
		b5b = "";
		b6 = "";
		b7 = "";
		b8 = "";
		b8f = "";
		b8b = "";
		b9 = "";
		c1 = "";
		c2 = "";
		c3 = "";
		c4 = "";
		c5 = "";
		c6 = "";
		hex1 = "";
		hex2 = "";
		hex3 = "";
		hex4 = "";
		hex5 = "";
		hex6 = "";
		mHrmValue = heartRate;
		rrIntervals_get = rrIntervals;

		if(rrIntervals_get!=null) {
			/*
			sizer = rrIntervals_get.size();
			runOnUiThread(() -> mHRSValue.setText(String.valueOf(sizer)));
			runOnUiThread(() -> TestValue.setText(String.valueOf(rrIntervals_get)));
			PPGOrgHandler(String.valueOf(rrIntervals_get));
			//Get X,Y,Z
*/
			/*
			for(int i=0;i<120;i+=15){
				myGsensorX = dataprocess(rrIntervals_get.get(i));
			}

			myGsensorX = dataprocess(rrIntervals_get.get(0));
			updateGsensorXGraph(myGsensorX);
			myGsensorY =  dataprocess(rrIntervals_get.get(1));
			updateGsensorYGraph(myGsensorY);
			myGsensorZ =  dataprocess(rrIntervals_get.get(2));
			updateGsensorZGraph(myGsensorZ);


			//Get GPPG
			String TemStringGPPG = String.valueOf(rrIntervals_get.get(3))+String.valueOf(rrIntervals_get.get(4))+String.valueOf(rrIntervals_get.get(5));
			updateled1Graph(Integer.valueOf(TemStringGPPG));

			// Get BPPG
			String TemStringBPPG = String.valueOf(rrIntervals_get.get(6))+String.valueOf(rrIntervals_get.get(7))+String.valueOf(rrIntervals_get.get(8));
			updateled2Graph(Integer.valueOf(TemStringBPPG));

			// Get IPPG
			String TemStringIPPG = String.valueOf(rrIntervals_get.get(9))+String.valueOf(rrIntervals_get.get(10))+String.valueOf(rrIntervals_get.get(11));
			updateled3Graph(Integer.valueOf(TemStringIPPG));

			// Get YPPG
			String TemStringYPPG = String.valueOf(rrIntervals_get.get(12))+String.valueOf(rrIntervals_get.get(13))+String.valueOf(rrIntervals_get.get(14));
			updateled4Graph(Integer.valueOf(TemStringYPPG));


			//Get X,Y,Z
			myGsensorX =  dataprocess(rrIntervals_get.get(15));
			updateGsensorXGraph(myGsensorX);
			myGsensorY =  dataprocess(rrIntervals_get.get(16));
			updateGsensorYGraph(myGsensorY);
			myGsensorZ =  dataprocess(rrIntervals_get.get(17));
			updateGsensorZGraph(myGsensorZ);


			//Get GPPG
			TemStringGPPG = String.valueOf(rrIntervals_get.get(18))+String.valueOf(rrIntervals_get.get(19))+String.valueOf(rrIntervals_get.get(20));
			updateled1Graph(Integer.valueOf(TemStringGPPG));

			// Get BPPG
			TemStringBPPG = String.valueOf(rrIntervals_get.get(21))+String.valueOf(rrIntervals_get.get(22))+String.valueOf(rrIntervals_get.get(23));
			updateled2Graph(Integer.valueOf(TemStringBPPG));

			// Get IPPG
			TemStringIPPG = String.valueOf(rrIntervals_get.get(24))+String.valueOf(rrIntervals_get.get(25))+String.valueOf(rrIntervals_get.get(26));
			updateled3Graph(Integer.valueOf(TemStringIPPG));

			// Get YPPG
			TemStringYPPG = String.valueOf(rrIntervals_get.get(27))+String.valueOf(rrIntervals_get.get(28))+String.valueOf(rrIntervals_get.get(29));
			updateled4Graph(Integer.valueOf(TemStringYPPG));


			//Get X,Y,Z
			myGsensorX =  dataprocess(rrIntervals_get.get(30));
			updateGsensorXGraph(myGsensorX);
			myGsensorY =  dataprocess(rrIntervals_get.get(31));
			updateGsensorYGraph(myGsensorY);
			myGsensorZ =  dataprocess(rrIntervals_get.get(32));
			updateGsensorZGraph(myGsensorZ);


			//Get GPPG
			TemStringGPPG = String.valueOf(rrIntervals_get.get(33))+String.valueOf(rrIntervals_get.get(34))+String.valueOf(rrIntervals_get.get(35));
			updateled1Graph(Integer.valueOf(TemStringGPPG));

			// Get BPPG
			TemStringBPPG = String.valueOf(rrIntervals_get.get(36))+String.valueOf(rrIntervals_get.get(37))+String.valueOf(rrIntervals_get.get(38));
			updateled2Graph(Integer.valueOf(TemStringBPPG));

			// Get IPPG
			TemStringIPPG = String.valueOf(rrIntervals_get.get(39))+String.valueOf(rrIntervals_get.get(40))+String.valueOf(rrIntervals_get.get(41));
			updateled3Graph(Integer.valueOf(TemStringIPPG));

			// Get YPPG
			TemStringYPPG = String.valueOf(rrIntervals_get.get(42))+String.valueOf(rrIntervals_get.get(43))+String.valueOf(rrIntervals_get.get(44));
			updateled4Graph(Integer.valueOf(TemStringYPPG));

			//Get X,Y,Z
			myGsensorX =  dataprocess(rrIntervals_get.get(45));
			updateGsensorXGraph(myGsensorX);
			myGsensorY =  dataprocess(rrIntervals_get.get(46));
			updateGsensorYGraph(myGsensorY);
			myGsensorZ =  dataprocess(rrIntervals_get.get(47));
			updateGsensorZGraph(myGsensorZ);


			//Get GPPG
			TemStringGPPG = String.valueOf(rrIntervals_get.get(48))+String.valueOf(rrIntervals_get.get(49))+String.valueOf(rrIntervals_get.get(50));
			updateled1Graph(Integer.valueOf(TemStringGPPG));

			// Get BPPG
			TemStringBPPG = String.valueOf(rrIntervals_get.get(51))+String.valueOf(rrIntervals_get.get(52))+String.valueOf(rrIntervals_get.get(53));
			updateled2Graph(Integer.valueOf(TemStringBPPG));

			// Get IPPG
			TemStringIPPG = String.valueOf(rrIntervals_get.get(54))+String.valueOf(rrIntervals_get.get(55))+String.valueOf(rrIntervals_get.get(56));
			updateled3Graph(Integer.valueOf(TemStringIPPG));

			// Get YPPG
			TemStringYPPG = String.valueOf(rrIntervals_get.get(57))+String.valueOf(rrIntervals_get.get(58))+String.valueOf(rrIntervals_get.get(59));
			updateled4Graph(Integer.valueOf(TemStringYPPG));

			//Get X,Y,Z
			myGsensorX =  dataprocess(rrIntervals_get.get(60));
			updateGsensorXGraph(myGsensorX);
			myGsensorY =  dataprocess(rrIntervals_get.get(61));
			updateGsensorYGraph(myGsensorY);
			myGsensorZ =  dataprocess(rrIntervals_get.get(62));
			updateGsensorZGraph(myGsensorZ);


			//Get GPPG
			TemStringGPPG = String.valueOf(rrIntervals_get.get(63))+String.valueOf(rrIntervals_get.get(64))+String.valueOf(rrIntervals_get.get(65));
			updateled1Graph(Integer.valueOf(TemStringGPPG));

			// Get BPPG
			TemStringBPPG = String.valueOf(rrIntervals_get.get(66))+String.valueOf(rrIntervals_get.get(67))+String.valueOf(rrIntervals_get.get(68));
			updateled2Graph(Integer.valueOf(TemStringBPPG));

			// Get IPPG
			TemStringIPPG = String.valueOf(rrIntervals_get.get(69))+String.valueOf(rrIntervals_get.get(70))+String.valueOf(rrIntervals_get.get(71));
			updateled3Graph(Integer.valueOf(TemStringIPPG));

			// Get YPPG
			TemStringYPPG = String.valueOf(rrIntervals_get.get(72))+String.valueOf(rrIntervals_get.get(73))+String.valueOf(rrIntervals_get.get(74));
			updateled4Graph(Integer.valueOf(TemStringYPPG));


			//Get X,Y,Z
			myGsensorX =  dataprocess(rrIntervals_get.get(75));
			updateGsensorXGraph(myGsensorX);
			myGsensorY =  dataprocess(rrIntervals_get.get(76));
			updateGsensorYGraph(myGsensorY);
			myGsensorZ =  dataprocess(rrIntervals_get.get(77));
			updateGsensorZGraph(myGsensorZ);


			//Get GPPG
			TemStringGPPG = String.valueOf(rrIntervals_get.get(78))+String.valueOf(rrIntervals_get.get(79))+String.valueOf(rrIntervals_get.get(80));
			updateled1Graph(Integer.valueOf(TemStringGPPG));

			// Get BPPG
			TemStringBPPG = String.valueOf(rrIntervals_get.get(81))+String.valueOf(rrIntervals_get.get(82))+String.valueOf(rrIntervals_get.get(83));
			updateled2Graph(Integer.valueOf(TemStringBPPG));

			// Get IPPG
			TemStringIPPG = String.valueOf(rrIntervals_get.get(84))+String.valueOf(rrIntervals_get.get(85))+String.valueOf(rrIntervals_get.get(86));
			updateled3Graph(Integer.valueOf(TemStringIPPG));

			// Get YPPG
			TemStringYPPG = String.valueOf(rrIntervals_get.get(87))+String.valueOf(rrIntervals_get.get(88))+String.valueOf(rrIntervals_get.get(89));
			updateled4Graph(Integer.valueOf(TemStringYPPG));
			*/


			/*
			for(int count = 0; count < 30;count=count+3) {

				if (rrIntervals_get.get(count) != null && rrIntervals_get.get(count+1) != null && rrIntervals_get.get(count+2) != null) {


					b1 = Integer.toBinaryString(0x10000 |rrIntervals_get.get(count)).substring(1);
					if (b1.charAt(0) == '1') {
						String result = b1;
						result = result.replace("0", " "); //temp replace 0s
						result = result.replace("1", "0"); //replace 1s with 0s
						result = result.replace(" ", "1"); //put the 1s back in
						//Change this to decimal format.
						int decimalValue = Integer.parseInt(result, 2);
						//Add 1 to the curernt decimal and multiply it by -1
						//because we know it's a negative number
						myGsensorX = (decimalValue + 1) * -1;
					}else{
						myGsensorX = Integer.parseInt(b1, 2);
					}

					updateGsensorXGraph(myGsensorX);



					b2 = Integer.toBinaryString(0x10000 |rrIntervals_get.get(count+1)).substring(1);

					if (b2.charAt(0) == '1') {
						String result = b2;
						result = result.replace("0", " "); //temp replace 0s
						result = result.replace("1", "0"); //replace 1s with 0s
						result = result.replace(" ", "1"); //put the 1s back in
						//Change this to decimal format.
						int decimalValue = Integer.parseInt(result, 2);
						//Add 1 to the curernt decimal and multiply it by -1
						//because we know it's a negative number
						myGsensorY = (decimalValue + 1) * -1;
					}else{
						myGsensorY = Integer.parseInt(b2, 2);
					}

					updateGsensorYGraph(myGsensorY);

					b3 = Integer.toBinaryString(0x10000 |rrIntervals_get.get(count+2)).substring(1);
					if (b3.charAt(0) == '1') {
						String result = b3;
						result = result.replace("0", " "); //temp replace 0s
						result = result.replace("1", "0"); //replace 1s with 0s
						result = result.replace(" ", "1"); //put the 1s back in
						//Change this to decimal format.
						int decimalValue = Integer.parseInt(result, 2);
						//Add 1 to the curernt decimal and multiply it by -1
						//because we know it's a negative number
						myGsensorZ = (decimalValue + 1) * -1;
					}else{
						myGsensorZ = Integer.parseInt(b3, 2);
					}
					updateGsensorZGraph(myGsensorZ);

					yeahcount++;

					runOnUiThread(() -> mGValue.setText(String.valueOf(yeahcount)));
					runOnUiThread(() -> TestValue.setText(String.valueOf(myGsensorX)+","+String.valueOf(myGsensorY)+","+String.valueOf(myGsensorZ)));
				}


			}

			*/

			//updateGsensorGraph(rrIntervals_get.get(0),rrIntervals_get.get(1),rrIntervals_get.get(2));
			//runOnUiThread(() -> mHRSValue.setText(String.valueOf(heartRate)));
			/*
			for(int x = 0;x<210;x=x+3) {
				updateGsensorXGraph(rrIntervals_get.get(x));
				updateGsensorYGraph(rrIntervals_get.get(x+1));
				updateGsensorZGraph(rrIntervals_get.get(x+2));
			}
			*/
			//updateGsensorXGraph(rrIntervals_get.get(0));
			//updateGsensorYGraph(rrIntervals_get.get(1));
			//updateGsensorZGraph(rrIntervals_get.get(2));

			/*
			b1 = Integer.toBinaryString(0x10000 |rrIntervals_get.get(0)).substring(1);
			//int i = (int) Long.parseLong(b1,2);
			//String s1 = String.format("%d", i & 0xFFFFFFFFL);

			if (b1.charAt(0) == '1') {
				String result = b1;
				result = result.replace("0", " "); //temp replace 0s
				result = result.replace("1", "0"); //replace 1s with 0s
				result = result.replace(" ", "1"); //put the 1s back in
				//Change this to decimal format.
				int decimalValue = Integer.parseInt(result, 2);
				//Add 1 to the curernt decimal and multiply it by -1
				//because we know it's a negative number
				myGsensorX = (decimalValue + 1) * -1;
			}else{
				myGsensorX = Integer.parseInt(b1, 2);
			}



			b2 = Integer.toBinaryString(0x10000 |rrIntervals_get.get(1)).substring(1);
			myGsensorY = Integer.parseInt(b2,2);

			if (b2.charAt(0) == '1') {
				String result = b2;
				result = result.replace("0", " "); //temp replace 0s
				result = result.replace("1", "0"); //replace 1s with 0s
				result = result.replace(" ", "1"); //put the 1s back in
				//Change this to decimal format.
				int decimalValue = Integer.parseInt(result, 2);
				//Add 1 to the curernt decimal and multiply it by -1
				//because we know it's a negative number
				myGsensorY = (decimalValue + 1) * -1;
			}else{
				myGsensorY = Integer.parseInt(b2, 2);
			}




			b3 = Integer.toBinaryString(0x10000 |rrIntervals_get.get(2)).substring(1);
			myGsensorZ = Integer.parseInt(b3,2);


			if (b3.charAt(0) == '1') {
				String result = b3;
				result = result.replace("0", " "); //temp replace 0s
				result = result.replace("1", "0"); //replace 1s with 0s
				result = result.replace(" ", "1"); //put the 1s back in
				//Change this to decimal format.
				int decimalValue = Integer.parseInt(result, 2);
				//Add 1 to the curernt decimal and multiply it by -1
				//because we know it's a negative number
				myGsensorZ = (decimalValue + 1) * -1;
			}else{
				myGsensorZ = Integer.parseInt(b3, 2);
			}

			//updateGsensorXGraph(myGsensorX);
			//updateGsensorYGraph(myGsensorY);
			//updateGsensorZGraph(myGsensorZ);

			runOnUiThread(() -> TestValue.setText(String.valueOf(myGsensorX)+","+String.valueOf(myGsensorY)+","+String.valueOf(myGsensorZ)));
			//runOnUiThread(() -> TestValue.setText(rrIntervals_get.get(0)+","+b1+","+String.valueOf(myGsensorX)));
			//Log.d(TAG, "The Gx Value: " + rrIntervals_get.get(0));

			//runOnUiThread(() -> TestValue.setText(rrIntervals_get.get(1)+","+b2+""));
			*/

		}
		/*
		// For testing Heart Rate
				runOnUiThread(() -> TestValue.setText(String.valueOf(mHrmValue)));
				runOnUiThread(() -> mHRSValue.setText(String.valueOf(rececount)));
				rececount++;
				if(mHRSValue!=null) {
					PPGHandler(String.valueOf(mHrmValue));
					nine_get.add(mHrmValue);
					ninedatacount++;
				}
		*/

		/*
		For Getting one by one(total six times)
		if(ninedatacount==9){
				b1 = Integer.toBinaryString(0x10000 | nine_get.get(0)).substring(1);
				b2 = Integer.toBinaryString(0x10000 | nine_get.get(1)).substring(1);
				b2f = b2.substring(0, 8);
				b2b = b2.substring(8, 16);
				c1 = b1 + b2f;
				hex1 = new BigInteger(c1, 2).toString(16);
				b3 = Integer.toBinaryString(0x10000 | nine_get.get(2)).substring(1);
				c2 = b2b + b3;
				hex2 = new BigInteger(c2, 2).toString(16);
				b4 = Integer.toBinaryString(0x10000 | nine_get.get(3)).substring(1);
				b5 = Integer.toBinaryString(0x10000 | nine_get.get(4)).substring(1);
				b5f = b5.substring(0, 8);
				b5b = b5.substring(8, 16);
				c3 = b4 + b5f;
				hex3 = new BigInteger(c3, 2).toString(16);
				b6 = Integer.toBinaryString(0x10000 | nine_get.get(5)).substring(1);
				c4 = b5b + b6;
				hex4 = new BigInteger(c4, 2).toString(16);
				b7 = Integer.toBinaryString(0x10000 | nine_get.get(6)).substring(1);
				b8 = Integer.toBinaryString(0x10000 | nine_get.get(7)).substring(1);
				b8f = b8.substring(0, 8);
				b8b = b8.substring(8, 16);
				c5 = b7 + b8f;
				hex5 = new BigInteger(c5, 2).toString(16);
				b9 = Integer.toBinaryString(0x10000 | nine_get.get(8)).substring(1);
				c6 = b8b + b9;
				hex6 = new BigInteger(c6, 2).toString(16);

				// fill 0
			if(hex1.length( )==5){
				hex1 = "0"+hex1;
			}
			if(hex2.length( )==5){
				hex2 = "0"+hex2;
			}
			if(hex3.length( )==5){
				hex3 = "0"+hex3;
			}
			if(hex4.length( )==5){
				hex4 = "0"+hex4;
			}
			if(hex5.length( )==5){
				hex5 = "0"+hex5;
			}
			if(hex6.length( )==5){
				hex6 = "0"+hex6;
			}
			if(hex1.length( )==4){
				hex1 = "00"+hex1;
			}
			if(hex2.length( )==4){
				hex2 = "00"+hex2;
			}
			if(hex3.length( )==4){
				hex3 = "00"+hex3;
			}
			if(hex4.length( )==4){
				hex4 = "00"+hex4;
			}
			if(hex5.length( )==4){
				hex5 = "00"+hex5;
			}
			if(hex6.length( )==4){
				hex6 = "00"+hex6;
			}
			if(hex1.length( )==3){
				hex1 = "000"+hex1;
			}
			if(hex2.length( )==3){
				hex2 = "000"+hex2;
			}
			if(hex3.length( )==3){
				hex3 = "000"+hex3;
			}
			if(hex4.length( )==3){
				hex4 = "000"+hex4;
			}
			if(hex5.length( )==3){
				hex5 = "000"+hex5;
			}
			if(hex6.length( )==3){
				hex6 = "000"+hex6;
			}

			if(hex1.length( )==2){
				hex1 = "0000"+hex1;
			}
			if(hex2.length( )==2){
				hex2 = "0000"+hex2;
			}
			if(hex3.length( )==2){
				hex3 = "0000"+hex3;
			}
			if(hex4.length( )==2){
				hex4 = "0000"+hex4;
			}
			if(hex5.length( )==2){
				hex5 = "0000"+hex5;
			}
			if(hex6.length( )==2){
				hex6 = "0000"+hex6;
			}
			if(hex1.length( )==1){
				hex1 = "00000"+hex1;
			}
			if(hex2.length( )==1){
				hex2 = "00000"+hex2;
			}
			if(hex3.length( )==1){
				hex3 = "00000"+hex3;
			}
			if(hex4.length( )==1){
				hex4 = "00000"+hex4;
			}
			if(hex5.length( )==1){
				hex5 = "00000"+hex5;
			}
			if(hex6.length( )==1){
				hex6 = "00000"+hex6;
			}
			hextem = hex1+hex2+hex3+hex4+hex5+hex6;
			PPGHandler(hextem);

			if(hextem.length()==36) {
				if(shiftpos>0) {
					fhex = hextem.substring(0, shiftpos);
					if(Savepos==shiftpos) {
						if (bhex != "") {
							Outhex = bhex + fhex;
							PPG1 = Integer.valueOf(new BigInteger(Outhex.substring(6, 12), 16).toString(10));
							PPG2 = Integer.valueOf(new BigInteger(Outhex.substring(12,18), 16).toString(10));
							PPG3 = Integer.valueOf(new BigInteger(Outhex.substring(18,24), 16).toString(10));
							PPG4 = Integer.valueOf(new BigInteger(Outhex.substring(24,30), 16).toString(10));
						}
						if (Outhex.length() == 36) {

							Outhex = Outhex.substring(0, 6) + "," + Outhex.substring(6, 12) + "," + Outhex.substring(12,18) + "," + Outhex.substring(18,24) + "," + Outhex.substring(24,30) + "," + Outhex.substring(30, 36);

						}
						PPGHandler(Outhex);
						runOnUiThread(() -> TestValue.setText(Outhex));
					}else{
						PPGHandler("unmatch");
					}
					bhex = hextem.substring(shiftpos, 36);
					Savepos = shiftpos;
				}else if(shiftpos==0){
					Outhex = hextem;
					PPG1 = Integer.valueOf(new BigInteger(Outhex.substring(6, 12), 16).toString(10));
					PPG2 = Integer.valueOf(new BigInteger(Outhex.substring(12,18), 16).toString(10));
					PPG3 = Integer.valueOf(new BigInteger(Outhex.substring(18,24), 16).toString(10));
					PPG4 = Integer.valueOf(new BigInteger(Outhex.substring(24,30), 16).toString(10));
					Outhex = Outhex.substring(0, 6) + "," + Outhex.substring(6, 12) + "," + Outhex.substring(12,18) + "," + Outhex.substring(18,24) + "," + Outhex.substring(24,30) + "," + Outhex.substring(30, 36);

					PPGHandler(Outhex);
				}else if (shiftpos ==-1){
					//can't found, 0d11 change to 0d1;1 0d;d1 0;d11; for n
					if(hextem.indexOf("0d1")>0){
						//find at the end
						shiftattail = 1;
					}
					if(hextem.indexOf("0d")>0){
						//find at the end
						shiftattail = 2;
					}
					//n+1
					if(hextem.indexOf("d11")>0){
						//For 0;d11
						Outhex = "0"+Outhex.substring(0, 35);
						PPG1 = Integer.valueOf(new BigInteger(Outhex.substring(6, 12), 16).toString(10));
						PPG2 = Integer.valueOf(new BigInteger(Outhex.substring(12,18), 16).toString(10));
						PPG3 = Integer.valueOf(new BigInteger(Outhex.substring(18,24), 16).toString(10));
						PPG4 = Integer.valueOf(new BigInteger(Outhex.substring(24,30), 16).toString(10));
						PPGHandler(Outhex);
					}
					if(hextem.indexOf(11)==0&&shiftattail==2){
						//For 0d;11
						Outhex = "0d"+Outhex.substring(0, 34);
						PPG1 = Integer.valueOf(new BigInteger(Outhex.substring(6, 12), 16).toString(10));
						PPG2 = Integer.valueOf(new BigInteger(Outhex.substring(12,18), 16).toString(10));
						PPG3 = Integer.valueOf(new BigInteger(Outhex.substring(18,24), 16).toString(10));
						PPG4 = Integer.valueOf(new BigInteger(Outhex.substring(24,30), 16).toString(10));
						PPGHandler(Outhex);
					}
					if(hextem.indexOf(0)==0&&shiftattail==1){
						//For 0d;11
						Outhex = "0d1"+Outhex.substring(0, 33);
						PPG1 = Integer.valueOf(new BigInteger(Outhex.substring(6, 12), 16).toString(10));
						PPG2 = Integer.valueOf(new BigInteger(Outhex.substring(12,18), 16).toString(10));
						PPG3 = Integer.valueOf(new BigInteger(Outhex.substring(18,24), 16).toString(10));
						PPG4 = Integer.valueOf(new BigInteger(Outhex.substring(24,30), 16).toString(10));
						PPGHandler(Outhex);
					}
				}
			}else{
				// not have 36 digit
				PPGHandler("Miss Data");
			}

			//reset
			nine_get.clear();
			ninedatacount=0;
		}

 		*/


		//For Testing orginal
		/*
				if(rrIntervals_get!=null){
					sizerget = rrIntervals_get.size();
					//Make Sure is
					for(int i=0;i<sizerget;i++){
						temhex = Integer.toHexString(rrIntervals_get.get(i));

						if(temhex.length()==0){
							temhex="000000";
						}
						if(temhex.length()==1){
							temhex="00000"+temhex;
						}
						if(temhex.length()==2){
							temhex="0000"+temhex;
						}
						if(temhex.length()==3){
							temhex="000"+temhex;
						}
						if(temhex.length()==4){
							temhex="00"+temhex;
						}
						if(temhex.length()==5){
							temhex="0"+temhex;
						}
						hex_listget.append(temhex);
						hex_listget.append(",");
					}

					yeahstring = hex_listget.toString();
					runOnUiThread(() -> TestValue.setText(yeahstring));
					runOnUiThread(() -> mHRSValue.setText(String.valueOf(rececount)));
					PPGHandler(yeahstring);
					PPGOrgHandler(String.valueOf(rrIntervals_get));
					rececount++;
					hex_listget.setLength(0);
				}
		*/





		/*
		CREATE TABLE face_detect(
				Date DATE,
				 datatype,
				column3 datatype,

		columnN datatype,
		PRIMARY KEY( one or more columns )
		);

		//For getting two by two(total 3 time)
		if(rrIntervals_get!=null){
			sizer = rrIntervals_get.size();
			//runOnUiThread(() -> mGValue.setText(String.valueOf(sizer)));
			//runOnUiThread(() -> TestValue.setText(rrIntervals_get.toString()));
			//PPGHandler(rrIntervals_get.toString());
			b1 = "";b2 = "";b2f = "";b2b = "";b3 = "";b4 = "";b5 = "";b5f = "";b5b = "";b6 = "";b7 = "";b8 = "";b8f = "";b8b = "";b9 = "";
			c1 = "";c2 = "";c3 = "";c4 = "";c5 = "";c6 = "";
			hex1 = "";hex2 = "";hex3 = "";hex4 = "";hex5 = "";hex6 = "";
			dataCountReceive++;
			sizer = rrIntervals_get.size();
			int count = sizer;


			if(sizer>=1){
				b1 = Integer.toBinaryString(0x10000 | rrIntervals_get.get(0)).substring(1);
			}
			if(sizer>=2){
				b2 = Integer.toBinaryString(0x10000 | rrIntervals_get.get(1)).substring(1);
				b2f = b2.substring(0, 8);
				b2b = b2.substring(8, 16);
				c1 = b1 + b2f;
				hex1 = new BigInteger(c1, 2).toString(16);
			}
			if(sizer>=3){
				b3 = Integer.toBinaryString(0x10000 | rrIntervals_get.get(2)).substring(1);
				c2 = b2b + b3;
				hex2 = new BigInteger(c2, 2).toString(16);
			}
			if(sizer>=4){
				b4 = Integer.toBinaryString(0x10000 | rrIntervals_get.get(3)).substring(1);
			}
			if(sizer>=5){
				b5 = Integer.toBinaryString(0x10000 | rrIntervals_get.get(4)).substring(1);
				b5f = b5.substring(0, 8);
				b5b = b5.substring(8, 16);
				c3 = b4 + b5f;
				hex3 = new BigInteger(c3, 2).toString(16);
			}
			if(sizer>=6){
				b6 = Integer.toBinaryString(0x10000 | rrIntervals_get.get(5)).substring(1);
				c4 = b5b + b6;
				hex4 = new BigInteger(c4, 2).toString(16);
			}
			if(sizer>=7){
				b7 = Integer.toBinaryString(0x10000 | rrIntervals_get.get(6)).substring(1);
			}
			if(sizer>=8){
				b8 = Integer.toBinaryString(0x10000 | rrIntervals_get.get(7)).substring(1);
				b8f = b8.substring(0, 8);
				b8b = b8.substring(8, 16);
				c5 = b7 + b8f;
				hex5 = new BigInteger(c5, 2).toString(16);
			}
			if(sizer>=9) {
				b9 = Integer.toBinaryString(0x10000 | rrIntervals_get.get(8)).substring(1);
				c6 = b8b + b9;
				hex6 = new BigInteger(c6, 2).toString(16);
			}

				//Fill the 0
			if(hex1.length( )==5){
				hex1 = "0"+hex1;
			}
			if(hex2.length( )==5){
				hex2 = "0"+hex2;
			}
			if(hex1.length( )==4){
				hex1 = "00"+hex1;
			}
			if(hex2.length( )==4){
				hex2 = "00"+hex2;
			}
			if(hex1.length( )==3){
				hex1 = "000"+hex1;
			}
			if(hex2.length( )==3){
				hex2 = "000"+hex2;
			}
			if(hex1.length( )==2){
				hex1 = "0000"+hex1;
			}
			if(hex2.length( )==2){
				hex2 = "0000"+hex2;
			}
			if(hex1.length( )==1){
				hex1 = "00000"+hex1;
			}
			if(hex2.length( )==1){
				hex2 = "00000"+hex2;
			}

			//yeahcount++;
			//runOnUiThread(() -> mHRSValue.setText(String.valueOf(yeahcount)));
			//runOnUiThread(() -> TestValue.setText(rrIntervals_get.toString()));
			//PPGHandler(hex1+","+hex2+","+hex3+","+hex4+","+hex5+","+hex6);
			sepprint(hex1+","+hex2);

			if(savecount<2){
				mixhex = mixhex+hex1+hex2;
				savecount++;
			}else if(savecount==2){
				mixhex = mixhex+hex1+hex2;
				hextem = mixhex;
				mixhex = "";
				savecount=0;
				PPGHandler(hextem);
				shiftpos = hextem.indexOf("0411");

				yeahcount++;
				runOnUiThread(() -> mHRSValue.setText(String.valueOf(yeahcount)));
				runOnUiThread(() -> mGValue.setText(String.valueOf(shiftpos)+","+Savepos+","+String.valueOf(hextem.length())));
				if(hextem.length()==36) {
					if(shiftpos>0) {
						fhex = hextem.substring(0, shiftpos);
						if(Savepos==shiftpos) {
							if (bhex != "") {
								Outhex = bhex + fhex;
								PPG1 = Integer.valueOf(new BigInteger(Outhex.substring(6, 12), 16).toString(10));
								PPG2 = Integer.valueOf(new BigInteger(Outhex.substring(12,18), 16).toString(10));
								PPG3 = Integer.valueOf(new BigInteger(Outhex.substring(18,24), 16).toString(10));
								PPG4 = Integer.valueOf(new BigInteger(Outhex.substring(24,30), 16).toString(10));
							}
							if (Outhex.length() == 36) {

								Outhex = Outhex.substring(0, 6) + "," + Outhex.substring(6, 12) + "," + Outhex.substring(12,18) + "," + Outhex.substring(18,24) + "," + Outhex.substring(24,30) + "," + Outhex.substring(30, 36);

							}
							PPGHandler(Outhex);
							runOnUiThread(() -> TestValue.setText(Outhex));
						}else{
							PPGHandler("unmatch");
						}
						bhex = hextem.substring(shiftpos, 36);
						Savepos = shiftpos;
					}else if(shiftpos==0){
						Outhex = hextem;
						PPG1 = Integer.valueOf(new BigInteger(Outhex.substring(6, 12), 16).toString(10));
						PPG2 = Integer.valueOf(new BigInteger(Outhex.substring(12,18), 16).toString(10));
						PPG3 = Integer.valueOf(new BigInteger(Outhex.substring(18,24), 16).toString(10));
						PPG4 = Integer.valueOf(new BigInteger(Outhex.substring(24,30), 16).toString(10));
						Outhex = Outhex.substring(0, 6) + "," + Outhex.substring(6, 12) + "," + Outhex.substring(12,18) + "," + Outhex.substring(18,24) + "," + Outhex.substring(24,30) + "," + Outhex.substring(30, 36);

						PPGHandler(Outhex);
					}else if (shiftpos ==-1){
						//can't found, 0d11 change to 0d1;1 0d;d1 0;d11; for n
						if(hextem.indexOf("041")>0){
							//find at the end
							shiftattail = 1;
						}
						if(hextem.indexOf("04")>0){
							//find at the end
							shiftattail = 2;
						}
						//n+1
						if(hextem.indexOf("411")>0){
							//For 0;d11
							Outhex = "0"+Outhex.substring(0, 35);
							PPG1 = Integer.valueOf(new BigInteger(Outhex.substring(6, 12), 16).toString(10));
							PPG2 = Integer.valueOf(new BigInteger(Outhex.substring(12,18), 16).toString(10));
							PPG3 = Integer.valueOf(new BigInteger(Outhex.substring(18,24), 16).toString(10));
							PPG4 = Integer.valueOf(new BigInteger(Outhex.substring(24,30), 16).toString(10));
							PPGHandler(Outhex);
						}
						if(hextem.indexOf(11)==0&&shiftattail==2){
							//For 0d;11
							Outhex = "04"+Outhex.substring(0, 34);
							PPG1 = Integer.valueOf(new BigInteger(Outhex.substring(6, 12), 16).toString(10));
							PPG2 = Integer.valueOf(new BigInteger(Outhex.substring(12,18), 16).toString(10));
							PPG3 = Integer.valueOf(new BigInteger(Outhex.substring(18,24), 16).toString(10));
							PPG4 = Integer.valueOf(new BigInteger(Outhex.substring(24,30), 16).toString(10));
							PPGHandler(Outhex);
						}
						if(hextem.indexOf(0)==0&&shiftattail==1){
							//For 0d;11
							Outhex = "041"+Outhex.substring(0, 33);
							PPG1 = Integer.valueOf(new BigInteger(Outhex.substring(6, 12), 16).toString(10));
							PPG2 = Integer.valueOf(new BigInteger(Outhex.substring(12,18), 16).toString(10));
							PPG3 = Integer.valueOf(new BigInteger(Outhex.substring(18,24), 16).toString(10));
							PPG4 = Integer.valueOf(new BigInteger(Outhex.substring(24,30), 16).toString(10));
							PPGHandler(Outhex);
						}
					}
				}else{
					// not have 36 digit
					PPGHandler("Miss Data");
				}
			}
		}

		*/


		//6 set data
		/*
		if (rrIntervals_get != null) {
			sizer = rrIntervals_get.size();
			//runOnUiThread(() -> mGValue.setText(String.valueOf(sizer)));
			//runOnUiThread(() -> TestValue.setText(rrIntervals_get.toString()));
			//PPGHandler(rrIntervals_get.toString());
			b1 = "";
			b2 = "";
			b2f = "";
			b2b = "";
			b3 = "";
			b4 = "";
			b5 = "";
			b5f = "";
			b5b = "";
			b6 = "";
			b7 = "";
			b8 = "";
			b8f = "";
			b8b = "";
			b9 = "";
			c1 = "";
			c2 = "";
			c3 = "";
			c4 = "";
			c5 = "";
			c6 = "";
			hex1 = "";
			hex2 = "";
			hex3 = "";
			hex4 = "";
			hex5 = "";
			hex6 = "";
			dataCountReceive++;
			sizer = rrIntervals_get.size();
			int count = sizer;


			if (sizer >= 1) {
				b1 = Integer.toBinaryString(0x10000 | rrIntervals_get.get(0)).substring(1);
			}
			if (sizer >= 2) {
				b2 = Integer.toBinaryString(0x10000 | rrIntervals_get.get(1)).substring(1);
				b2f = b2.substring(0, 8);
				b2b = b2.substring(8, 16);
				c1 = b1 + b2f;
				hex1 = new BigInteger(c1, 2).toString(16);
			}
			if (sizer >= 3) {
				b3 = Integer.toBinaryString(0x10000 | rrIntervals_get.get(2)).substring(1);
				c2 = b2b + b3;
				hex2 = new BigInteger(c2, 2).toString(16);
			}
			if (sizer >= 4) {
				b4 = Integer.toBinaryString(0x10000 | rrIntervals_get.get(3)).substring(1);
			}
			if (sizer >= 5) {
				b5 = Integer.toBinaryString(0x10000 | rrIntervals_get.get(4)).substring(1);
				b5f = b5.substring(0, 8);
				b5b = b5.substring(8, 16);
				c3 = b4 + b5f;
				hex3 = new BigInteger(c3, 2).toString(16);
			}
			if (sizer >= 6) {
				b6 = Integer.toBinaryString(0x10000 | rrIntervals_get.get(5)).substring(1);
				c4 = b5b + b6;
				hex4 = new BigInteger(c4, 2).toString(16);
			}
			if (sizer >= 7) {
				b7 = Integer.toBinaryString(0x10000 | rrIntervals_get.get(6)).substring(1);
			}
			if (sizer >= 8) {
				b8 = Integer.toBinaryString(0x10000 | rrIntervals_get.get(7)).substring(1);
				b8f = b8.substring(0, 8);
				b8b = b8.substring(8, 16);
				c5 = b7 + b8f;
				hex5 = new BigInteger(c5, 2).toString(16);
			}
			if (sizer >= 9) {
				b9 = Integer.toBinaryString(0x10000 | rrIntervals_get.get(8)).substring(1);
				c6 = b8b + b9;
				hex6 = new BigInteger(c6, 2).toString(16);
			}

			// fill 0
			if (hex1.length() == 5) {
				hex1 = "0" + hex1;
			}
			if (hex2.length() == 5) {
				hex2 = "0" + hex2;
			}
			if (hex3.length() == 5) {
				hex3 = "0" + hex3;
			}
			if (hex4.length() == 5) {
				hex4 = "0" + hex4;
			}
			if (hex5.length() == 5) {
				hex5 = "0" + hex5;
			}
			if (hex6.length() == 5) {
				hex6 = "0" + hex6;
			}
			if (hex1.length() == 4) {
				hex1 = "00" + hex1;
			}
			if (hex2.length() == 4) {
				hex2 = "00" + hex2;
			}
			if (hex3.length() == 4) {
				hex3 = "00" + hex3;
			}
			if (hex4.length() == 4) {
				hex4 = "00" + hex4;
			}
			if (hex5.length() == 4) {
				hex5 = "00" + hex5;
			}
			if (hex6.length() == 4) {
				hex6 = "00" + hex6;
			}
			if (hex1.length() == 3) {
				hex1 = "000" + hex1;
			}
			if (hex2.length() == 3) {
				hex2 = "000" + hex2;
			}
			if (hex3.length() == 3) {
				hex3 = "000" + hex3;
			}
			if (hex4.length() == 3) {
				hex4 = "000" + hex4;
			}
			if (hex5.length() == 3) {
				hex5 = "000" + hex5;
			}
			if (hex6.length() == 3) {
				hex6 = "000" + hex6;
			}

			if (hex1.length() == 2) {
				hex1 = "0000" + hex1;
			}
			if (hex2.length() == 2) {
				hex2 = "0000" + hex2;
			}
			if (hex3.length() == 2) {
				hex3 = "0000" + hex3;
			}
			if (hex4.length() == 2) {
				hex4 = "0000" + hex4;
			}
			if (hex5.length() == 2) {
				hex5 = "0000" + hex5;
			}
			if (hex6.length() == 2) {
				hex6 = "0000" + hex6;
			}
			if (hex1.length() == 1) {
				hex1 = "00000" + hex1;
			}
			if (hex2.length() == 1) {
				hex2 = "00000" + hex2;
			}
			if (hex3.length() == 1) {
				hex3 = "00000" + hex3;
			}
			if (hex4.length() == 1) {
				hex4 = "00000" + hex4;
			}
			if (hex5.length() == 1) {
				hex5 = "00000" + hex5;
			}
			if (hex6.length() == 1) {
				hex6 = "00000" + hex6;
			}
			if (hex1.length() == 0) {
				hex1 = "000000";
			}
			if (hex2.length() == 0) {
				hex2 = "000000";
			}
			if (hex3.length() == 0) {
				hex3 = "000000";
			}
			if (hex4.length() == 0) {
				hex4 = "000000";
			}
			if (hex5.length() == 0) {
				hex5 = "000000";
			}
			if (hex6.length() == 0) {
				hex6 = "000000";
			}

			hextem = hex1 + hex2 + hex3 + hex4 + hex5 + hex6;
			Outhex = hextem;
			PPG1 = Integer.valueOf(new BigInteger(Outhex.substring(6, 12), 16).toString(10));
			PPG2 = Integer.valueOf(new BigInteger(Outhex.substring(12, 18), 16).toString(10));
			PPG3 = Integer.valueOf(new BigInteger(Outhex.substring(18, 24), 16).toString(10));
			PPG4 = Integer.valueOf(new BigInteger(Outhex.substring(24, 30), 16).toString(10));
			PPGHandler(hex1 + "," + hex2 + "," + hex3 + "," + hex4 + "," + hex5 + "," + hex6);
			PPGOrgHandler(String.valueOf(rrIntervals_get));
			yeahcount++;
			runOnUiThread(() -> mGValue.setText(String.valueOf(yeahcount)));
			runOnUiThread(() -> mHRSValue.setText(String.valueOf(sizer)));
			runOnUiThread(() -> TestValue.setText(hex1 + "," + hex2 + "," + hex3 + "," + hex4 + "," + hex5 + "," + hex6));

		}
		/*
			/*
			shiftpos = hextem.indexOf("0b11");

			runOnUiThread(() -> mGValue.setText(String.valueOf(shiftpos)+","+Savepos+","+String.valueOf(hextem.length())));
				if(hextem.length()==36) {
					if(shiftpos>0) {
						fhex = hextem.substring(0, shiftpos);
						if(Savepos==shiftpos) {
							if (bhex != "") {
								Outhex = bhex + fhex;
								PPG1 = Integer.valueOf(new BigInteger(Outhex.substring(6, 12), 16).toString(10));
								PPG2 = Integer.valueOf(new BigInteger(Outhex.substring(12,18), 16).toString(10));
								PPG3 = Integer.valueOf(new BigInteger(Outhex.substring(18,24), 16).toString(10));
								PPG4 = Integer.valueOf(new BigInteger(Outhex.substring(24,30), 16).toString(10));
							}
							if (Outhex.length() == 36) {

								Outhex = Outhex.substring(0, 6) + "," + Outhex.substring(6, 12) + "," + Outhex.substring(12,18) + "," + Outhex.substring(18,24) + "," + Outhex.substring(24,30) + "," + Outhex.substring(30, 36);

							}
							PPGHandler(Outhex);

						}else{
							PPGHandler("unmatch");
						}
						bhex = hextem.substring(shiftpos, 36);
						Savepos = shiftpos;
					}else if(shiftpos==0){
						Outhex = hextem;
						PPG1 = Integer.valueOf(new BigInteger(Outhex.substring(6, 12), 16).toString(10));
						PPG2 = Integer.valueOf(new BigInteger(Outhex.substring(12,18), 16).toString(10));
						PPG3 = Integer.valueOf(new BigInteger(Outhex.substring(18,24), 16).toString(10));
						PPG4 = Integer.valueOf(new BigInteger(Outhex.substring(24,30), 16).toString(10));
						Outhex = Outhex.substring(0, 6) + "," + Outhex.substring(6, 12) + "," + Outhex.substring(12,18) + "," + Outhex.substring(18,24) + "," + Outhex.substring(24,30) + "," + Outhex.substring(30, 36);
						runOnUiThread(() -> TestValue.setText(Outhex));
						PPGHandler(Outhex);
					}else if (shiftpos ==-1){
						//can't found, 0d11 change to 0d1;1 0d;d1 0;d11; for n
						if(hextem.indexOf("041")>0){
							//find at the end
							shiftattail = 1;
						}
						if(hextem.indexOf("04")>0){
							//find at the end
							shiftattail = 2;
						}
						//n+1
						if(hextem.indexOf("411")>0){
							//For 0;d11
							Outhex = "0"+Outhex.substring(0, 35);
							PPG1 = Integer.valueOf(new BigInteger(Outhex.substring(6, 12), 16).toString(10));
							PPG2 = Integer.valueOf(new BigInteger(Outhex.substring(12,18), 16).toString(10));
							PPG3 = Integer.valueOf(new BigInteger(Outhex.substring(18,24), 16).toString(10));
							PPG4 = Integer.valueOf(new BigInteger(Outhex.substring(24,30), 16).toString(10));
							PPGHandler(Outhex);
							runOnUiThread(() -> TestValue.setText(Outhex));
						}
						if(hextem.indexOf(11)==0&&shiftattail==2){
							//For 0d;11
							Outhex = "04"+Outhex.substring(0, 34);
							PPG1 = Integer.valueOf(new BigInteger(Outhex.substring(6, 12), 16).toString(10));
							PPG2 = Integer.valueOf(new BigInteger(Outhex.substring(12,18), 16).toString(10));
							PPG3 = Integer.valueOf(new BigInteger(Outhex.substring(18,24), 16).toString(10));
							PPG4 = Integer.valueOf(new BigInteger(Outhex.substring(24,30), 16).toString(10));
							PPGHandler(Outhex);
							runOnUiThread(() -> TestValue.setText(Outhex));
						}
						if(hextem.indexOf(0)==0&&shiftattail==1){
							//For 0d;11
							Outhex = "041"+Outhex.substring(0, 33);
							PPG1 = Integer.valueOf(new BigInteger(Outhex.substring(6, 12), 16).toString(10));
							PPG2 = Integer.valueOf(new BigInteger(Outhex.substring(12,18), 16).toString(10));
							PPG3 = Integer.valueOf(new BigInteger(Outhex.substring(18,24), 16).toString(10));
							PPG4 = Integer.valueOf(new BigInteger(Outhex.substring(24,30), 16).toString(10));
							PPGHandler(Outhex);
							runOnUiThread(() -> TestValue.setText(Outhex));
						}
					}
				}else{
					// not have 36 digit
					PPGHandler("Miss Data");
				}
			}
			*/

	}

	@Override
	public void onDeviceDisconnected(@NonNull final BluetoothDevice device) {
		super.onDeviceDisconnected(device);
		runOnUiThread(() -> {
			mHRSValue.setText(R.string.not_available_value);
			mHRSPosition.setText(R.string.not_available);
			mBatteryLevelView.setText(R.string.not_available);
			//stopShowGraph();
		});
	}

	@Override
	protected void setDefaultUI() {
		mHRSValue.setText(R.string.not_available_value);
		mHRSPosition.setText(R.string.not_available);
		mBatteryLevelView.setText(R.string.not_available);
		clearGraph();
	}

	private void clearGraph() {

		mLineGraphecg.clearGraph();
		mGraphViewecg.repaint();
		mLineGraphps.clearGraph();
		mGraphViewps.repaint();
		mCounterps = 0;
		mCounter = 0;
		mCounter1 =0;


		mLineGraphled1.clearGraph();
		mGraphViewled1.repaint();
		mLineGraphled2.clearGraph();
		mGraphViewled2.repaint();
		mLineGraphled3.clearGraph();
		mGraphViewled3.repaint();
		mLineGraphled4.clearGraph();
		mGraphViewled4.repaint();
		mCounter2 =0;
		mCounter3 =0;
		mCounter4 =0;
		mCounter5 =0;

		mHrmValue = 0;
		mX = 0;
		mY = 0;
		mZ = 0;
	}

	void resetHandler(){
		my_HrmValue =  new StringBuilder("");
		my_mX = new StringBuilder("");
		my_mY = new StringBuilder("");
		my_mZ = new StringBuilder("");
	}

	void sepprint(String data){
		printcount++;
		if(data==","){
			data = "{"+rrIntervals_get.toString()+"}";
		}
		if(printcount==1){
			matchstring = matchstring+data;
		}
		if(printcount>1) {
			matchstring = matchstring + "," + data;
		}
		if(printcount==3){
			//runOnUiThread(() -> TestValue.setText(matchstring));
			PPGOrgHandler(matchstring);
			printcount = 0;
			matchstring = "";
		}
	}

	void sepprint2(String data){
		printcount++;
		if(data==","){
			data = "{"+rrIntervals_get.toString()+"}";
		}
		if(printcount==1){
			matchstring = matchstring+data;
		}
		if(printcount==2){
			//runOnUiThread(() -> TestValue.setText(matchstring));
			PPGOrgHandler(matchstring);
			printcount = 0;
			matchstring = "";
		}
	}

	void saveorg(String data){

	}
	@Override
	public void onHRSensorWrite(boolean vaild) {

	}

	@Override
	public void onHRSensorgatt(BluetoothGatt gatt) {
		this.gattsend = gatt;
	}


	int ppgfwindowsize = 100;
	boolean vaild = false;
	boolean firstin90 = true;
	boolean firstin40 = true;
	@Override
	public void onHRValueReceived(final BluetoothDevice device, ArrayList<Integer> rrIntervals_rece,int flags) {
		if(rrIntervals_rece!=null) {
			sizer = rrIntervals_rece.size();
			runOnUiThread(() -> mHRSValue.setText(String.valueOf(sizer)));
			runOnUiThread(() -> TestValue.setText(String.valueOf(rrIntervals_rece)));
			//runOnUiThread(() -> TestValue.setText("1:"+String.valueOf(motion & 0x01)+","+"2:"+String.valueOf(motion>>1 & 0x01)+","+"3:"+String.valueOf(motion>>2 & 0x01)+","+"4:"+String.valueOf(motion>>3 & 0x01)+","+"5:"+String.valueOf(motion>>4 & 0x01)+","+"6:"+String.valueOf(motion>>5 & 0x01)+","+"7:"+String.valueOf(motion>>6 & 0x01)+","+"8:"+String.valueOf(motion>>7 & 0x01)));
			happycount++;
			PPGOrgHandler(happycount + String.valueOf(rrIntervals_rece));
			runOnUiThread(() -> mGValue.setText(String.valueOf(happycount)));
			int motionvaild = flags>>1 & 0x01;

			if(motionvaild==1) {



				firstin40=true;
				if(firstin90==true){
				//set ps invisible
				layoutps.setVisibility(View.GONE);
				layoutled1.setVisibility(View.VISIBLE);
				layoutled2.setVisibility(View.VISIBLE);
				layoutled3.setVisibility(View.VISIBLE);
				layoutled4.setVisibility(View.VISIBLE);
				//clear the ps data
					mLineGraphps.clearGraph();
					mGraphViewps.repaint();
					mCounterps = 0;
				firstin90 = false;
				}


				for (int i = 0; i < 90; i += 9) {
					if (sizer == 90) {

						updateGsensorXGraph(rrIntervals_rece.get(i));
						updateGsensorYGraph(rrIntervals_rece.get(i + 1));
						updateGsensorZGraph(rrIntervals_rece.get(i + 2));
						gsensorcounter++;
						//250-500dp


					/*
					Window1ValuedArray.add(Double.valueOf(rrIntervals_rece.get(i + 4))); //LED1
					//Try New

					if(Window1ValuedArray.size()==ppgfwindowsize){
						vaild = true;
						final double[] led1arr = new double[Window1ValuedArray.size()];
						int index = 0;
						for (final Double value : Window1ValuedArray) {
							led1arr[index++] = value;
						}
						PyObject obj_led1 = pyf.callAttr("PPGFilter",led1arr);
						led1_list = obj_led1.asList();
						Window1ValueArrayF.clear();
						for(int ii =0;ii<ppgfwindowsize;ii++){
							double ppg1f = led1_list.get(ii).toDouble();
							Window1ValueArrayF.add(ppg1f);
						}

						Window1ValuedArray.clear();
					}

					if(Window1ValuedArray.size()<ppgfwindowsize&&vaild==true){
						double ppg = Window1ValueArrayF.get(Window1ValuedArray.size());
						updateled1Graph((int)ppg);
					}

					*/
						int led1 = rrIntervals_rece.get(i + 3) / windowsize;
						int led2 = rrIntervals_rece.get(i + 4) / windowsize;
						int led3 = rrIntervals_rece.get(i + 5) / windowsize;
						int led4 = rrIntervals_rece.get(i + 6) / windowsize;
						int ecg = rrIntervals_rece.get(i + 7) / windowsize;
						int ps = rrIntervals_rece.get(i + 8) / windowsize;
						if (myECGGraph2500.size() > 2500) {
							myECGGraph2500.remove(0);
						}
						myECGGraph2500.add(ecg);
						//Find Largest One and the Smallest One
						int ECGmax = Collections.max(myECGGraph2500);
						int ECGmin = Collections.min(myECGGraph2500);
						//int led1get = rrIntervals_rece.get(i + 3);
						//int led2get = rrIntervals_rece.get(i + 4);
						//int led3get = rrIntervals_rece.get(i + 5);
						//int led4get = rrIntervals_rece.get(i + 6);
						int ecgget = rrIntervals_rece.get(i + 7);

						PPGHandler(String.valueOf(ecgget) + ",");


						if (countecgget < barhost) {
							myECGArray100Filter[countecgget] = ecgget;
							countecgget++;
						}
						if (countecgget >= barhost) {
							//25 host
							//ecg filter
							PyObject obj_ecgx = pyf.callAttr("ECGFilter", myECGArray100Filter);
							ecg_listx = obj_ecgx.asList();
							firstrun = true;
							//shift array to left 25
							for (int x = 0; x < barhost - eachhost; x++) {
								myECGArray100Filter[x] = myECGArray100Filter[x + eachhost];
								//myLED1Array100Filter[x]=myLED1Array100Filter[x+eachhost];
							}

							//count-25
							countecgget = countecgget - 25;
						}
						if (firstrun == true) {
							//updateecgGraph((int) ecg_listx.get(countecgget - 25).toDouble());
							//updateled1Graph((int) led1_listx.get(countecgget - 25).toDouble());
						}

						led1sum = led1sum + led1;
						led2sum = led2sum + led2;
						led3sum = led3sum + led3;
						led4sum = led4sum + led4;
						ecgsum = ecgsum + ecg;
						//pssum = pssum + ps;


						Window1ValueArray.add(led1);
						Window2ValueArray.add(led2);
						Window3ValueArray.add(led3);
						Window4ValueArray.add(led4);
						WindowecgValueArray.add(ecg);
						//WindowpsValueArray.add(ps);

						datacount++;
						if (datacount == windowsize) {
							updateled1Graph(led1sum);
							updateled2Graph(led2sum);
							updateled3Graph(led3sum);
							updateled4Graph(led4sum);
							//updateecgGraph(ecgsum);
							//updatepsGraph(pssum);


						} else if (datacount > windowsize) {
							led1sum = led1sum - Window1ValueArray.get(0);
							led2sum = led2sum - Window2ValueArray.get(0);
							led3sum = led3sum - Window3ValueArray.get(0);
							led4sum = led4sum - Window4ValueArray.get(0);
							ecgsum = ecgsum - WindowecgValueArray.get(0);
							//pssum = pssum - WindowpsValueArray.get(0);

							Window1ValueArray.remove(0);
							Window2ValueArray.remove(0);
							Window3ValueArray.remove(0);
							Window4ValueArray.remove(0);
							WindowecgValueArray.remove(0);
							//WindowpsValueArray.remove(0);

							updateled1Graph(led1sum);
							updateled2Graph(led2sum);
							updateled3Graph(led3sum);
							updateled4Graph(led4sum);
							//updateecgGraph(ecgsum);
							//updatepsGraph(rrIntervals_rece.get(i + 8));
						}

					}
				}
			}
			if(motionvaild==0){
				firstin90=true;
				if(firstin40==true) {
					//leds invisible
					layoutled1.setVisibility(View.GONE);
					layoutled2.setVisibility(View.GONE);
					layoutled3.setVisibility(View.GONE);
					layoutled4.setVisibility(View.GONE);
					//clear the leds data
					mLineGraphled1.clearGraph();
					mGraphViewled1.repaint();
					mLineGraphled2.clearGraph();
					mGraphViewled2.repaint();
					mLineGraphled3.clearGraph();
					mGraphViewled3.repaint();
					mLineGraphled4.clearGraph();
					mGraphViewled4.repaint();
					mCounter2 =0;
					mCounter3 =0;
					mCounter4 =0;
					mCounter5 =0;
					layoutps.setVisibility(View.VISIBLE);
					firstin40=false;
				}

				for (int i = 0; i < 40; i += 4) {
					if (sizer == 40) {
						updateGsensorXGraph(rrIntervals_rece.get(i));
						updateGsensorYGraph(rrIntervals_rece.get(i + 1));
						updateGsensorZGraph(rrIntervals_rece.get(i + 2));
						gsensorcounter++;
						int ps = rrIntervals_rece.get(i + 3);
						updatepsGraph(ps);
					}
				}
			}

		}
	}

	void signalHandler(int mHrmValue, int mX, int mY, int mZ) {
		my_HrmValue.append(String.valueOf(mHrmValue) + ",");
		my_mX.append(String.valueOf(mX) + ",");
		my_mY.append(String.valueOf(mY) + ",");
		my_mZ.append(String.valueOf(mZ) + ",");
	}

	void PPGHandler(String PPGdata){
		my_PPG.append(String.valueOf(PPGdata+"\n"));
	}

	void PPGOrgHandler(String PPGOrgdata){
		my_PPGOrg.append(String.valueOf(PPGOrgdata+"\n"));
	}

	private void uploadHealthyData(){

		measure_time = MyTime.geTime();

		String[] parts = measure_time.split(",");
		int i = parts.length;
		int sum = 0;
		while(i > 0){
			sum = sum + Integer.parseInt(parts[i-1]);
			i--;
		}
		sum = sum/(parts.length);
		c_yearTime = parts[0];
		c_monthTime = parts[1];
		dayTime = parts[2];
		hourTime = parts[3];
		minTime = parts[4];
		secTime = parts[5];
		//measure_state.setText("狀態" + "上傳緊,請等等...ฅ●ω●ฅ");

		measure_state.setText(c_yearTime);
		Random rand = new Random();
		String teststring = "1,2,3,4,5,6,7,8,9";
		//c_systolic = Integer.toString(rand.nextInt(50));
		//c_heart = Integer.toString(rand.nextInt(100 - 70) + 70);
		//整左個HashMap用黎包data，format分別係String同String
		HashMap<String, String> data = new HashMap<String, String>();

		data.put("hrmValue", my_HrmValue.toString());
		//data.put("c_xValue", my_mX.toString());
		data.put("c_xValue", teststring);
		data.put("c_yValue", my_mY.toString());
		data.put("c_zValue", my_mZ.toString());
		data.put("type","2");
		data.put("c_uid", c_uid);
		//data.put("c_heart", c_heart);
		//data.put("c_diastolic", c_diastolic);
		data.put("c_systolic", c_systolic);
		data.put("c_frequency", String.valueOf(c_frequency));
		data.put("c_sampling_accuracy", String.valueOf(c_sampling_accuracy));
		data.put("c_yearTime",c_yearTime);
		data.put("c_monthTime",c_monthTime);
		data.put("c_dayTime",dayTime);
		data.put("c_hourTime",hourTime);
		data.put("c_minTime",minTime);
		data.put("c_secTime",secTime);

		JSONObject EcgMeasureObject = new JSONObject();
		JSONObject data_json = new JSONObject(data);
		try {
			EcgMeasureObject.put("c", "ctl000021");
			EcgMeasureObject.put("m", "saveHealthHK");
			EcgMeasureObject.put("data", data_json.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}


		VolleyRequest.RequestPost(HRSActivity.this, BASE_URL, TAG2,
				EcgMeasureObject, new VolleyInterface(HRSActivity.this,
						VolleyInterface.mListener,
						VolleyInterface.mErrorListener) {
					public void onMySuccess(JSONObject result) {

						// TODO Auto-generated method stub
						if ("1".equals(result.optString("result"))) {
							Toast.makeText(HRSActivity.this, "upload success！", Toast.LENGTH_SHORT).show();
							measure_state.setText("state:" + "upload success");
						} else {
							Toast.makeText(HRSActivity.this, "upload fail！", Toast.LENGTH_SHORT).show();
							measure_state.setText("state:" + "Sorry,Fail....Work hard");
						}
					}
					public void onMyError(VolleyError arg0) {
						// TODO Auto-generated method stub
						measure_state.setText("State:" + "Time out");
						Log.e(TAG, arg0.toString());
						Toast.makeText(HRSActivity.this, "Upload data timed out!", Toast.LENGTH_SHORT).show();
					}
				});



	}

	public void loadingdata(){
		clearGraph();


		//ECG
		InputStream Is = getResources().openRawResource(R.raw.ecgselect);
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(Is, Charset.forName("UTF-8"))
		);

		String line = "";
		String linesave = "";
		int count = 0;
		try{
			while((line = reader.readLine())!=null){
				linesave = line;
			}

			String[] tokens = linesave.split(",");
			for (String token:tokens) {
				if(count<5000) {
					double value = (double)Double.valueOf(token).longValue();
					myECGArray[count] = value;
					myECGArrayFilter[count] = value;
					count++;
				}
			}

			//Toast.makeText(getBaseContext(), linesave , Toast.LENGTH_SHORT).show();
			Is.close();

		}catch (IOException e){
			e.printStackTrace();
		}




		//LED1 GPPG
		Is = getResources().openRawResource(R.raw.led1gselect);
		reader = new BufferedReader(
				new InputStreamReader(Is, Charset.forName("UTF-8"))
		);

		line = "";
		linesave = "";
		count = 0;
		try{
			while((line = reader.readLine())!=null){
				linesave = line;
			}

			String[] tokens = linesave.split(",");
			for (String token:tokens) {
				if(count<5000) {
					double value = (double)Double.valueOf(token).longValue();
					myLED1Array[count] = value;
					myLED1ArrayFilter[count] = value;
					count++;

				}
			}

			//Toast.makeText(getBaseContext(), linesave , Toast.LENGTH_SHORT).show();
			Is.close();

		}catch (IOException e){
			e.printStackTrace();
		}



		//LED2 BPPG
		Is = getResources().openRawResource(R.raw.led2bselect);
		reader = new BufferedReader(
				new InputStreamReader(Is, Charset.forName("UTF-8"))
		);

		line = "";
		linesave = "";
		count = 0;
		try{
			while((line = reader.readLine())!=null){
				linesave = line;
			}

			String[] tokens = linesave.split(",");
			for (String token:tokens) {
				if(count<5000) {
					double value = (double)Double.valueOf(token).longValue();
					//updateled1Graph(value);
					myLED2Array[count] = value;
					myLED2ArrayFilter[count] = value;
					count++;

				}
			}

			//Toast.makeText(getBaseContext(), linesave , Toast.LENGTH_SHORT).show();
			Is.close();

		}catch (IOException e){
			e.printStackTrace();
		}



		//LED3 IPPG
		Is = getResources().openRawResource(R.raw.led3iselect);
		reader = new BufferedReader(
				new InputStreamReader(Is, Charset.forName("UTF-8"))
		);

		line = "";
		linesave = "";
		count = 0;
		try{
			while((line = reader.readLine())!=null){
				linesave = line;
			}

			String[] tokens = linesave.split(",");
			for (String token:tokens) {
				if(count<5000) {
					double value = (double)Double.valueOf(token).longValue();
					//updateled1Graph(value);
					myLED3Array[count] = value;
					myLED3ArrayFilter[count] = value;
					count++;

				}
			}

			//Toast.makeText(getBaseContext(), linesave , Toast.LENGTH_SHORT).show();
			Is.close();

		}catch (IOException e){
			e.printStackTrace();
		}


		//LED4 YPPG
		Is = getResources().openRawResource(R.raw.led4yselect);
		reader = new BufferedReader(
				new InputStreamReader(Is, Charset.forName("UTF-8"))
		);

		line = "";
		linesave = "";
		count = 0;
		try{
			while((line = reader.readLine())!=null){
				linesave = line;
			}

			String[] tokens = linesave.split(",");
			for (String token:tokens) {
				if(count<5000) {
					double value = (double)Double.valueOf(token).longValue();
					//updateled1Graph(value);
					myLED4Array[count] = value;
					myLED4ArrayFilter[count] = value;
					count++;

				}
			}

			//Toast.makeText(getBaseContext(), linesave , Toast.LENGTH_SHORT).show();
			Is.close();

		}catch (IOException e){
			e.printStackTrace();
		}



	}



}


