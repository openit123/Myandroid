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
package no.nordicsemi.android.nrftoolbox.bpm;

import android.bluetooth.BluetoothDevice;
import android.graphics.Point;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import org.achartengine.GraphicalView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.Buffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

import no.nordicsemi.android.nrftoolbox.R;
import no.nordicsemi.android.nrftoolbox.hrs.LineGraphView;
import no.nordicsemi.android.nrftoolbox.hrs.LineGraphView_4;
import no.nordicsemi.android.nrftoolbox.hrs.LineGraphView_bp;
import no.nordicsemi.android.nrftoolbox.hrs.LineGraphView_heartrate;
import no.nordicsemi.android.nrftoolbox.hrs.LineGraphView_ppt;
import no.nordicsemi.android.nrftoolbox.profile.BleProfileActivity;
import no.nordicsemi.android.nrftoolbox.profile.LoggableBleManager;
import no.nordicsemi.android.nrftoolbox.uart.UARTActivity;
import no.nordicsemi.android.nrftoolbox.uart.UARTInterface;


// TODO The BPMActivity should be rewritten to use the service approach, like other do.
public class BPMActivity extends BleProfileActivity implements BPMManagerCallbacks {


	private TextView mECGValue,mHRValue,mLED1Value,mPPTValue,mDBPValue,mSBPValue;

	private LineGraphView_4 mLineGraphecg;
	private LineGraphView mLineGraphled1;
	private LineGraphView_ppt mLineGraphppt;
	private LineGraphView_heartrate mLineGraphhr;
	private LineGraphView_bp mLineGraphbp;
	private UARTActivity uartAct;
	private GraphicalView mGraphViewecg,mGraphViewled1,mGraphViewppt,mGraphViewhr,mGraphViewbp;

	ViewGroup layoutecg,layoutled1,layoutppt,layouthr,layoutbp;

	private Button upload_bt,display_bt;
	public PyObject pyf;
	public List<PyObject> led1_list,ecg_list,ppt_list,hr_list;

	private int mCounterecg = 0,mCounterled1=0,mCounterppt=0,mCounterhr=0,mCounterbp=0;
	public int ecgvaluecount = 0,led1valuecount = 0, printcount=0;

	public double [] myLED1Array = new double[5000]; //G
	public double[] myLED1ArrayFilter = new double[5000];
	public double[] myLED2Array = new double[5000]; //B
	public double[] myLED2ArrayFilter = new double[5000];
	public double[] myLED3Array = new double[5000]; //I
	public double[] myLED3ArrayFilter = new double[5000];
	public double[] myLED4Array = new double[5000]; //Y
	public double[] myLED4ArrayFilter = new double[5000];

	public double [] myECGArray = new double[5000];
	public double[] myECGArrayFilter = new double[5000];

	//New Raw data
	public ArrayList<Double>ALLECGArrayList = new ArrayList<Double>();
	public ArrayList<Double>ALLLED1ArrayList = new ArrayList<Double>(); //B
	public ArrayList<Double>ALLLED2ArrayList = new ArrayList<Double>(); //I
	public ArrayList<Double>ALLLED3ArrayList = new ArrayList<Double>(); //G
	public ArrayList<Double>ALLLED4ArrayList = new ArrayList<Double>(); //Y

	public double[] ALLECGArray;
	public double[] ALLLED1Array;
	public double[] ALLLED2Array;
	public double[] ALLLED3Array;
	public double[] ALLLED4Array;

	public double[] ForPeakLED1Array;
	public double[] ForPeakLED2Array;
	public double[] ForPeakLED3Array;
	public double[] ForPeakLED4Array;
	public double[] ForPeakECGArray;

	public ArrayList<Double>ALLLED1ArrayFilter = new ArrayList<Double>(); //B
	public ArrayList<Double>ALLLED2ArrayFilter = new ArrayList<Double>(); //I
	public ArrayList<Double>ALLLED3ArrayFilter = new ArrayList<Double>(); //G
	public ArrayList<Double>ALLLED4ArrayFilter = new ArrayList<Double>(); //Y


	public ArrayList<Integer>myECGPeakArray = new ArrayList<Integer>();
	public ArrayList<Integer>myECGValueArray = new ArrayList<Integer>(); //It is Heart Rate
	public ArrayList<Integer>myLED1PeakArray = new ArrayList<Integer>();
	public ArrayList<Integer>myPPTArray = new ArrayList<Integer>();
	public int PPTvaluesave=550,HRvaluesave=50;

	public int countecgget = 0,countecgpeakget = 0;
	final int barhost = 250;
	final int eachhost = 25;
	final int peakhost = 250;
	final int shifthost = 26;
	final int middlehost = 198;
	int sizecount = 0;
	int sizecount2 = 0;
	int count3 = 0;
	public double[] myECGArray100Filter = new double[barhost];
	public double[] myLED1Array100Filter = new double[barhost];
	public double[] myECGPeak100Filter = new double[peakhost];
	public double[] myLED1Peak100Filter = new double[peakhost];

	public ArrayList<Integer>myECGPrint = new ArrayList<Integer>();
	public ArrayList<Integer>myLED1Print = new ArrayList<Integer>();

	public List<PyObject> ecg_listx,led1_listx,test_list,ecgx_list,led1x_list,led2x_list,led3x_list,led4x_list;
	boolean firstrun = false;
	int datacount = 0;
	int led1sum = 0;
	public ArrayList<Integer>Window1ValueArray = new ArrayList<Integer>();
	double DBP = 70,DBP0 = 70,SBP = 110,SBP0 = 110,A = 0.02,PTT = 0,PTT0 = 0;

	@Override
	protected void onCreateView(final Bundle savedInstanceState) {
		uartAct = new UARTActivity();
		final UARTInterface uart = uartAct;
		setContentView(R.layout.activity_feature_bpm);
		setGUI();
		if(! Python.isStarted() ){
			Python.start(new AndroidPlatform(getApplicationContext()));
		}
		Python py = Python.getInstance();
		pyf = py.getModule("myscript"); //py name
		//loadingdata();
		loadingdataAll();

		//Load Large data use
		upload_bt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sizecount=0;
				sizecount2=0;
				//ALLECGArray = new double[ALLECGArrayList.size()];
				ALLLED1Array = new double[5000];
				ALLLED2Array = new double[5000];
				ALLLED3Array = new double[5000];
				ALLLED4Array = new double[5000];

				ForPeakECGArray = new double[250];
				ForPeakLED1Array = new double[250];

				//Arraylist to array
				Thread thread1 = new Thread(new Runnable() {
					public void run() {
						for (int i = 0; i < ALLLED1ArrayList.size(); i++) {

							ALLLED1Array[sizecount] = ALLLED1ArrayList.get(i);
							ALLLED2Array[sizecount] = ALLLED2ArrayList.get(i);
							ALLLED3Array[sizecount] = ALLLED3ArrayList.get(i);
							ALLLED4Array[sizecount] = ALLLED4ArrayList.get(i);
							sizecount++;

							ForPeakECGArray[sizecount2] = ALLECGArrayList.get(i);
							ForPeakLED1Array[sizecount2] = ALLLED1ArrayList.get(i);
							sizecount2++;

							if (sizecount == 5000) {
								//Find BP
								PyObject BP_result = pyf.callAttr("BPEstimation", ALLLED1Array, ALLLED2Array, ALLLED3Array, ALLLED4Array, ALLECGArray);
								Log.d("Output", String.valueOf(BP_result));
								sizecount = 0;
								try {
									Thread.sleep(1000);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}
							if(sizecount2 == 250){
								//Find PeaktoPeak
								PyObject obj_peakecg = pyf.callAttr("ECGPeak",ForPeakECGArray);
								PyObject obj_peakppg1 = pyf.callAttr("PPGPeak",ForPeakLED1Array);
								for(int j=0;j<obj_peakecg.asList().size();j++){
									int tempeak;
									tempeak = Integer.valueOf(obj_peakecg.asList().get(j).toString());
									myECGPeakArray.add(tempeak);
								}

								for(int j=0;j<obj_peakppg1.asList().size();j++){
									int tempeak;
									tempeak = Integer.valueOf(obj_peakppg1.asList().get(j).toString());
									myLED1PeakArray.add(tempeak);
								}
								//PPG-ECG
								int max = myECGPeakArray.size();
								if(myLED1PeakArray.size()<max){
									max = myLED1PeakArray.size();
								}
								for(int ic=0;ic<max;ic++){
									double value =  myLED1PeakArray.get(ic)-myECGPeakArray.get(ic);
									double value2 = value * 4;
									myPPTArray.add((int)value2);
									//Log.d("PTP:",String.valueOf( (int)value2));
									count3++;
								}
								sizecount2 = 0;
								try {
									Thread.sleep(250);
								} catch (InterruptedException e) {
									e.printStackTrace();
								}
							}

							//ALLECGArray[i] = ALLECGArrayList.get(i);
						}
					}
				});
				thread1.start();

				//PyObject BP_result = pyf.callAttr("testimport",1);

				/*
				PyObject obj_peakecg = pyf.callAttr("ECGPeak",myECGArrayFilter);
				PyObject obj_peakppg1 = pyf.callAttr("PPGPeak",myLED1ArrayFilter);
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
				//PPG-ECG
				int max = myECGPeakArray.size();
				if(myLED1PeakArray.size()<max){
					max = myLED1PeakArray.size();
				}
				for(int ic=0;ic<max;ic++){
					double value =  myLED1PeakArray.get(ic)-myECGPeakArray.get(ic);
					double value2 = value * 4;
					myPPTArray.add((int)value2);
				}
				*/
			}
		});

		/*
		//load 5000 data use
		upload_bt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				PyObject obj_ecgx = pyf.callAttr("ECGFilter", myECGArrayFilter);
				PyObject obj_led1 = pyf.callAttr("PPGFilter",myLED1ArrayFilter);
				ecgx_list = obj_ecgx.asList();
				led1x_list = obj_led1.asList();

				for(int i=0;i<5000;i++){
					myLED1ArrayFilter[i]=led1x_list.get(i).toDouble();
					myECGArrayFilter[i]= ecgx_list.get(i).toDouble();
				}
				PyObject obj_peakecg = pyf.callAttr("ECGPeak",myECGArrayFilter);
				PyObject obj_peakppg1 = pyf.callAttr("PPGPeak",myLED1ArrayFilter);
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
				//PPG-ECG
				int max = myECGPeakArray.size();
				if(myLED1PeakArray.size()<max){
					max = myLED1PeakArray.size();
				}
				for(int ic=0;ic<max;ic++){
					double value =  myLED1PeakArray.get(ic)-myECGPeakArray.get(ic);
					double value2 = value * 4;
					myPPTArray.add((int)value2);
				}

			}
		});
		*/

		/*
		upload_bt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				Thread thread2 = new Thread(new Runnable() {
					public void run() {
						for (int i = 0; i < 5000; i++) {

							if (countecgget < barhost) {
								myECGArray100Filter[countecgget] = myECGArray[i];
								myLED1Array100Filter[countecgget] = myLED1Array[i];
								myECGPeak100Filter[countecgpeakget] = myECGArray[i];
								myLED1Peak100Filter[countecgpeakget] = myLED1Array[i];
								//updateecgGraph((int)myECGArrayFilter[i]);
								//updateled1Graph((int)myLED1ArrayFilter[i]);
								countecgget++;
								countecgpeakget++;
							}
							if (countecgget >= barhost) {
								//25 host
								//First filter
								PyObject obj_ecgx = pyf.callAttr("ECGFilter", myECGArray100Filter);
								PyObject obj_ppgx = pyf.callAttr("PPGFilter",myLED1Array100Filter);

								ecg_listx = obj_ecgx.asList();
								led1_listx = obj_ppgx.asList();
								firstrun = true;
								//shift array to left 25
								for (int x = 0; x < barhost - eachhost; x++) {
									myECGArray100Filter[x] = myECGArray100Filter[x +eachhost];
									myLED1Array100Filter[x]=myLED1Array100Filter[x+eachhost];
								}
								//count-25
								countecgget = countecgget - eachhost;
							}
							if (firstrun == true) {
								//Average of LED1
								int led1 = (int) led1_listx.get(countecgget - eachhost).toDouble() / 5;
								led1sum = led1sum + led1;
								Window1ValueArray.add(led1);
								datacount++;
								if(datacount==5) {
									updateled1Graph(led1sum);
								} else if (datacount > 5) {
									led1sum = led1sum - Window1ValueArray.get(0);
									Window1ValueArray.remove(0);
									updateled1Graph(led1sum);

								}
								updateecgGraph((int) ecg_listx.get(countecgget - eachhost).toDouble());
							}
							if(countecgpeakget==peakhost){
								//find peak
								countecgpeakget = 0;
								PyObject obj_peakecg = pyf.callAttr("ECGPeak",myECGPeak100Filter);
								PyObject obj_peakppg1 = pyf.callAttr("PPGPeak",myLED1Peak100Filter);

							}
							try {
								Thread.sleep(5);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				});
				thread2.start();
				//below is the //
			}
		});
		*/
		/*
		upload_bt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {

				Thread thread2 = new Thread(new Runnable() {
					public void run() {
						for (int i = 0; i < 5000; i++) {

							//If ecg is not 125, add data into it. (0-124)
							//Second time: 26-124
							if (countecgget < barhost) {
								myECGArray100Filter[countecgget] = myECGArray[i];
								myLED1Array100Filter[countecgget] = myLED1Array[i];
								//myECGPeak100Filter[countecgpeakget] = myECGArray[i];
								//myLED1Peak100Filter[countecgpeakget] = myLED1Array[i];
								//updateecgGraph((int)myECGArrayFilter[i]);
								//updateled1Graph((int)myLED1ArrayFilter[i]);
								countecgget++;
								//countecgpeakget++;
							}
							//if ecg data is full, filter. and reindex the 100filter
							if (countecgget >= barhost) {
								//25 host
								//First filter
								PyObject obj_ecgx = pyf.callAttr("ECGFilter", myECGArray100Filter);
								PyObject obj_ppgx = pyf.callAttr("PPGFilter",myLED1Array100Filter);

								ecg_listx = obj_ecgx.asList();
								led1_listx = obj_ppgx.asList();


								//clear
								myLED1Print.clear();
								myLED1Print.clear();
								//get middle 99 data
								// x = 13, (13-111) x<112
								for (int x = shifthost;x < barhost-shifthost ;x++){
									myECGPrint.add((int)ecg_listx.get(x-shifthost).toDouble());
									myLED1Print.add((int)led1_listx.get(x-shifthost).toDouble());
								}

								firstrun = true;
								//Ready the next array
								//shift array to left 99 by format : 13/99/13 total 125, 1-13/14-112/113-125
								//shifthost*2 = 26, barhost = 125
								//start (99to124) x(0to25) 99+(0to25)
								for (int x = 0; x < (shifthost*2); x++) {
									myECGArray100Filter[x] = myECGArray100Filter[middlehost+x];
									myLED1Array100Filter[x]=myLED1Array100Filter[middlehost+x];
								}
								//125-99 = 26(start point) 26-124
								countecgget = countecgget - middlehost;
							}

							if(firstrun ==true){
								//0-99
								updateecgGraph(myECGPrint.get(printcount));
								updateled1Graph(myLED1Print.get(printcount));
								printcount++;
								if(printcount>=middlehost){
									printcount = 0;
								}
							}


							try {
								Thread.sleep(4);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}
				});
				thread2.start();
				//below is the //
			}
		});
		*/
		/*
		display_bt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Thread thread1 = new Thread(new Runnable() {
					public void run() {
						for(int i=0;i<ALLLED1ArrayFilter.size();i++){
							//updatebpGraph(120,80);
							//updateecgGraph(ALLECGArray.get(i));

							updateled1Graph((int)Math.floor(ALLLED1ArrayFilter.get(i)));
							try {
								Thread.sleep(4);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}

					}
				});
				thread1.start();

			}
		});
		*/

		display_bt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				//Realtime display data (Test for 20000 data)
				//B(ALLLED1ArrayList) I(ALLLED2ArrayList) G(ALLLED3ArrayList) Y(ALLLED4ArrayList) ECG(ALLECGArrayList)
				//Format is <Double> Arraylist

				//First is display the array one by one normally with move average recursive (led1,led2,led3,led4,ecg)
				//Second is Add a Window Call (display the Peak Position) Python: ECGPeak, PPGPeak



			}
		});

		/*
		//Display old data
		display_bt.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Thread thread1 = new Thread(new Runnable() {
					public void run() {
						for(int i=0;i<5000;i++){
							//updatebpGraph(120,80);
							updateecgGraph((int)myECGArrayFilter[i]);
							updateled1Graph((int)myLED1ArrayFilter[i]);
							updatepptGraph(PPTvaluesave);
							updatehrGraph(HRvaluesave);
							PTT = PPTvaluesave;
							if(PTT!=PTT0) {
								if (i > 0) {
									DBP = (SBP0 / 3) + (2 * DBP0 / 3) + (A * (Math.log(PTT0 / PTT))) - (((SBP0 - DBP0) / 3) * ((PTT0 * PTT0) / (PTT * PTT)));
									SBP = DBP + ((SBP0 - DBP0) * (PTT0 * PTT0) / (PTT * PTT));
									updatebpGraph((int) SBP, (int) DBP);
								}
								PTT0 = PTT;
							}

							updatebpGraph((int) SBP, (int) DBP);


							try {
								Thread.sleep(4);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}

					}
				});
				thread1.start();

			}
		});
		*/

	}

	private void setGUI() {
		mLineGraphecg = LineGraphView_4.getLineGraphView();
		mLineGraphled1 = LineGraphView.getLineGraphView();
		mLineGraphppt = LineGraphView_ppt.getLineGraphView();
		mLineGraphhr = LineGraphView_heartrate.getLineGraphView();
		mLineGraphbp = LineGraphView_bp.getLineGraphView();

		display_bt = (Button)findViewById(R.id.display_bt);
		upload_bt = (Button)findViewById(R.id.upload_bt);

		mECGValue = findViewById(R.id.text_ecg_value);
		mHRValue = findViewById(R.id.text_hr_value);
		mLED1Value = findViewById(R.id.text_ppg_value);
		mPPTValue = findViewById(R.id.text_ppt_value);
		mSBPValue = findViewById(R.id.text_sbp_value);
		mDBPValue = findViewById(R.id.text_dbp_value);

		showGraph();
	}

	private void showGraph() {
		mGraphViewecg = mLineGraphecg.getView(this);
		layoutecg = findViewById(R.id.graph_ecg);
		layoutecg.addView(mGraphViewecg);

		mGraphViewled1 = mLineGraphled1.getView(this);
		layoutled1 = findViewById(R.id.graph_led1);
		layoutled1.addView(mGraphViewled1);

		mGraphViewppt = mLineGraphppt.getView(this);
		layoutppt = findViewById(R.id.graph_ppt);
		layoutppt.addView(mGraphViewppt);

		mGraphViewhr = mLineGraphhr.getView(this);
		layouthr = findViewById(R.id.graph_heartrate);
		layouthr.addView(mGraphViewhr);

		mGraphViewbp = mLineGraphbp.getView(this);
		layoutbp = findViewById(R.id.graph_bp);
		layoutbp.addView(mGraphViewbp);


	}


	@Override
	protected int getLoggerProfileTitle() {
		return R.string.bpm_feature_title;
	}

	@Override
	protected int getDefaultDeviceName() {
		return R.string.bpm_default_name;
	}

	@Override
	protected int getAboutTextId() {
		return R.string.bpm_about_text;
	}

	@Override
	protected UUID getFilterUUID() {
		return BPMManager.BP_SERVICE_UUID;
	}

	@Override
	protected LoggableBleManager<BPMManagerCallbacks> initializeManager() {
		final BPMManager manager = BPMManager.getBPMManager(getApplicationContext());
		manager.setGattCallbacks(this);
		return manager;
	}

	@Override
	protected void setDefaultUI() {


	}

	@Override
	public void onServicesDiscovered(@NonNull final BluetoothDevice device, final boolean optionalServicesFound) {
		// this may notify user or show some views
	}

	@Override
	public void onDeviceReady(@NonNull final BluetoothDevice device) {
		// this may notify user
	}

	@Override
	public void onHRValueReceived(final BluetoothDevice device, ArrayList<Integer> rrIntervals, int motion) {
		/*
		if(end_flag == 1){
			stopShowGraph();
		}
		if(new_flag == 1){
			clearGraph();
			mCounter = 0;
		}
		mHrmValue = 850-hrt_value;

		signalHandler(mHrmValue,mX,mY,mZ);
		//setHRSValueOnView(mHrmValue);
		//footstep_Process(z);
*/
	}

	@Override
	public void onDeviceDisconnected(@NonNull final BluetoothDevice device) {
		super.onDeviceDisconnected(device);

	}

	@Override
	public void onBloodPressureMeasurementReceived(@NonNull final BluetoothDevice device,
												   final float systolic, final float diastolic, final float meanArterialPressure, final int unit,
												   @Nullable final Float pulseRate, @Nullable final Integer userID,
												   @Nullable final BPMStatus status, @Nullable final Calendar calendar) {

	}

	@Override
	public void onIntermediateCuffPressureReceived(@NonNull final BluetoothDevice device, final float cuffPressure, final int unit,
												   @Nullable final Float pulseRate, @Nullable final Integer userID,
												   @Nullable final BPMStatus status, @Nullable final Calendar calendar) {

	}

	@Override
	public void onBatteryLevelChanged(@NonNull final BluetoothDevice device, final int batteryLevel) {

	}


	public void loadingdataAll(){
		InputStream Is = getResources().openRawResource(R.raw.rawall);
		BufferedReader reader = new BufferedReader(
				new InputStreamReader(Is, Charset.forName("UTF-8"))
		);
		String line = "";
		String linesave = "";
		int count = 0;

		try{
			while((line = reader.readLine())!=null){
				linesave = line;
				String[] tokens = linesave.split(",");
				for (String token:tokens) {
					if(count<10) {
						double value = (double)Double.valueOf(token).longValue();
						//B(ALLLED1ArrayList) I(ALLLED2ArrayList) G(ALLLED3ArrayList) Y(ALLLED4ArrayList) ECG(ALLECGArrayList)
						if(ALLECGArrayList.size()<=20000) {
							if (count == 0) {
								ALLLED1ArrayList.add(value);
							} //B
							if (count == 1) {
								ALLLED2ArrayList.add(value);
							} //I
							if (count == 2) {
								ALLLED3ArrayList.add(value);
							} //G
							if (count == 3) {
								ALLLED4ArrayList.add(value);
							} //Y
							if (count == 4) {
								ALLECGArrayList.add(value);
							} //ECG
							//Log.d("GETALL"+count, String.valueOf(value));
						}
						count++;
					}
				}
				count=0;
			}

			//Toast.makeText(getBaseContext(), linesave , Toast.LENGTH_SHORT).show();
			Is.close();

		}catch (IOException e){
			e.printStackTrace();
		}

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


	private void updateecgGraph(final int ecgValue) {
		mCounterecg++;
		runOnUiThread(() -> mECGValue.setText(String.valueOf(ecgValue)));
		//ECG Graph
		boolean vaild = false;
		if(myECGPeakArray.contains(mCounterecg)){
			vaild = true;
			if(ecgvaluecount< myECGValueArray.size()-1) {
				runOnUiThread(() -> mHRValue.setText(String.valueOf(myECGValueArray.get(ecgvaluecount))));
				runOnUiThread(() -> mPPTValue.setText(String.valueOf(myPPTArray.get(ecgvaluecount))));
			}
			ecgvaluecount++;
		}
		mLineGraphecg.addValue(new Point(mCounterecg, ecgValue),vaild);
		mGraphViewecg.repaint();
	}

	private void updateled1Graph(final int led1Value) {
		mCounterled1++;
		runOnUiThread(() -> mLED1Value.setText(String.valueOf(led1Value)));
		//led1 Graph
		boolean vaild = false;
		if(myLED1PeakArray.contains(mCounterled1)){
			vaild = true;
			//Here
			if(led1valuecount< myPPTArray.size()-1) {
				PPTvaluesave = (int)myPPTArray.get(led1valuecount);
			}
			if(led1valuecount< myECGValueArray.size()-1) {
				HRvaluesave = (int)myECGValueArray.get(led1valuecount);
			}
			led1valuecount++;
		}
		mLineGraphled1.addValue(new Point(mCounterled1, led1Value),vaild);
		mGraphViewled1.repaint();
	}

	private void updatehrGraph(final int hrValue) {

		//HR Graph
		boolean vaild = false;
		mLineGraphhr.addValue(new Point(mCounterhr, hrValue),vaild);
		mCounterhr++;
		mGraphViewhr.repaint();

	}

	private void updatepptGraph(final int pptValue) {

		//PPT Graph
		boolean vaild = false;
		mLineGraphppt.addValue(new Point(mCounterppt, pptValue),vaild);
		mCounterppt++;
		mGraphViewppt.repaint();

	}

	private void updatebpGraph(final int sbpValue,final int dbpValue) {

		runOnUiThread(() -> mSBPValue.setText(String.valueOf(sbpValue)));
		runOnUiThread(() -> mDBPValue.setText(String.valueOf(dbpValue)));
		//BP Graph 120/80 mmHg
		boolean vaild = false;
		mLineGraphbp.addValue(new Point(mCounterbp, sbpValue),new Point(mCounterbp, dbpValue),new Point(mCounterbp, 0));
		mCounterbp++;
		mGraphViewbp.repaint();

	}


	private void clearGraph() {

		mLineGraphecg.clearGraph();
		mGraphViewecg.repaint();
		mCounterecg = 0;

		mLineGraphled1.clearGraph();
		mGraphViewled1.repaint();
		mCounterled1 = 0;

		mLineGraphhr.clearGraph();
		mGraphViewhr.repaint();
		mCounterhr = 0;

		mLineGraphppt.clearGraph();
		mGraphViewppt.repaint();
		mCounterppt = 0;

		mLineGraphbp.clearGraph();
		mGraphViewbp.repaint();
		mCounterbp = 0;


	}

}
