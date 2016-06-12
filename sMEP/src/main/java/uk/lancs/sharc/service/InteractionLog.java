package uk.lancs.sharc.service;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Resources;
import android.os.Build;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;
import com.google.android.gms.maps.model.LatLng;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;

import uk.lancs.sharc.R;
import uk.lancs.sharc.controller.MainActivity;

/**
 * <p>InteractionLog helps log key user interactions in SMEP </p>
 * <p>It provides the addLog method to log interactions</p>
 *
 * Author: Trien Do
 * Date: May 2015
 */

public class InteractionLog {
	private OutputStreamWriter oswLogWriter; 			//Store log file in the Sharc folder of the External Storage of the device
	private String deviceID;							//An unique ID for each device = Build.SERIAL;
	private Hashtable<String, String> actionNames; 		//A map of ActionID - Action name (human readable format)
	private Activity activity;
	private ArrayList<Integer[]> logStore = null;
	private Comparator<Integer[]> sort = new Comparator<Integer[]>() {
		@Override
		public int compare(Integer[] data1, Integer[] data2) {
			if(data1[0] < data2[0])
				return -1;
			else if(data1[0] == data2[0])
				return 0;
			else if(data1[0] > data2[0])
				return 1;
			return 0;
		}};

	public InteractionLog(Activity activity)
	{
		this.activity = activity;
		actionNames = new Hashtable<String, String>();
		createActionNameHashtable();					//Fill in the map of ActionID - Action name

		//Get log file ready for writing log
		//Logfile path = Sharc/smepLog.csv
		//E.g., in Nexus 7: root/storage/emulated/0/Sharc/smepLog.csv 
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(SharcLibrary.SHARC_LOG_FOLDER + File.separator + "smepLog" + SharcLibrary.getReadableTimeStamp() + ".csv", true);		//true = append
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		oswLogWriter = new OutputStreamWriter(fos);
		
		//Get device id
		deviceID = Build.SERIAL;
		//setup temporary logging if the view log button is enabled
		Resources res = activity.getResources();
		if(res.getBoolean(R.bool.view_logs)){
			logStore = new ArrayList<Integer[]>();
			for(String key : actionNames.keySet()){
				Integer[] data = new Integer[2];
				data[0] = Integer.parseInt(key);
				data[1] = 0;
				logStore.add(data);
			}
		}
		Collections.sort(logStore,sort);
	}
	
	public void addLog(String actionID, String actionData)
	{
		//Add a log line to the log file
		//A log line format: DateAndTime,LatLng,DeviceID,UserID,ActionID,ActionName,ActionData
		// - DateAndTime: Local time e.g., Thu May 07 13:44:29 BST 2015
		// - LatLng: LAT LNG (space to separate them
		// - UserID: = Dropbox account ID if the user logs into SMEP with their dropbox account else = anonymous
		// - ActionID and ActionName: see the table below
		// - ActionData: depends on the ActionID		
		//Example of a log line: Thu May 07 13:44:29 BST 2015,54.00594448 -2.78566378,0a282ca7,387643271,02,SELECT_YAH,SmallRed
		LatLng location = ((MainActivity)activity).getInitialLocation();
		CloudManager cloudManager = ((MainActivity)activity).getCloudManager();

		ArrayList<String> logData = new ArrayList<String>();
		String timeStamp = (new Date()).toString();
		
		logData.add(timeStamp);
		
		if(location == null)
			logData.add("undefined");
		else
			logData.add(location.latitude + " " + location.longitude);
		
		logData.add(deviceID);
		
		if(cloudManager == null)
			logData.add("anonymous");
		else
			logData.add(cloudManager.getCloudAccountId());
			
		
		logData.add(actionID);
		logData.add(actionNames.get(actionID));
		logData.add(actionData);
		if(logStore != null){
			Integer actionInt = Integer.parseInt(actionID);
			Integer[] data = logStore.get(actionInt);
			data[1]++;
			logStore.set(data[0],data);
		}
		try {
			oswLogWriter.append(TextUtils.join(",", logData.toArray()));//Separate fields by comma 
			oswLogWriter.append(System.getProperty("line.separator"));
			oswLogWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public static final String START_APP 						= "0";
	public static final String EXIT_APP 						= "1";
	public static final String SELECT_YAH 						= "2"; //Sliding menu: Set You-Are-Here (YAH) marker
	public static final String SELECT_LOGIN 					= "3"; //Sliding menu:
	public static final String SELECT_LOGOUT 					= "4"; //Sliding menu:
	public static final String VIEW_CACHED_EXPERIENCES 			= "5"; //Sliding menu: Play a downloaded experience
	public static final String VIEW_ONLINE_EXPERIENCES 			= "6"; //Sliding menu: Download experiences
	
	public static final String DOWNLOAD_ONLINE_EXPERIENCE		= "7"; //A dialog shows when an online experience marker on map is selected: Download
	public static final String CANCEL_DOWNLOAD_EXPERIENCE 		= "8"; //A dialog shows when an online experience marker on map is selected: Cancel
	public static final String PLAY_EXPERIENCE 					= "9"; //A dialog shows when a cached experience marker on map is selected: Play
	public static final String CANCEL_PLAY_EXPERIENCE 			= "10"; //A dialog shows when a cached experience marker on map is selected: Cancel
	public static final String DELETE_EXPERIENCE 				= "11"; //A dialog shows when a cached experience marker on map is selected: Delete
	
	public static final String SELECT_PUSH		 				= "12"; //Sliding menu: Push Media
	public static final String SELECT_SOUND		 				= "13"; //Sliding menu: Sound Notification
	public static final String SELECT_VIBRATION	 				= "14"; //Sliding menu: Vibration Notification
	public static final String SELECT_SETELLITE	 				= "15"; //Sliding menu: Satellite Maps
	public static final String SELECT_ROTATION	 				= "16"; //Sliding menu: Auto-roate Maps
	public static final String SELECT_YAH_CENTRED 				= "17"; //Sliding menu: Keep YAH marker centred on Maps
	public static final String SELECT_TEST		 				= "18"; //Sliding menu: Test Mode
	public static final String SELECT_SHOW_TRIGGER_ZONE 		= "19"; //Sliding menu: Show Trigger Zones
	public static final String SELECT_SHOW_POI_THUMBS 			= "20"; //Sliding menu: Show POIs
	public static final String SELECT_RESET_POI	 				= "21"; //Sliding menu: Reset POIs
	
	public static final String SELECT_MAP_TAB	 				= "22"; //Tab view: Map View
	public static final String SELECT_POI_TAB	 				= "23"; //Tab view: POI Media
	public static final String SELECT_EOI_TAB	 				= "24"; //Tab view: EOI Media
	public static final String SELECT_SUMMARY_TAB 				= "25"; //Tab view: Summary Info
	public static final String SELECT_RESPONSE_TAB 				= "26"; //Tab view: My Response
	
	public static final String OPEN_RESPONSE_DIALOG				= "27"; //Button: Add response with 1 OF 4 value: 0 - NEW (new POI), 1 - POI, 2 - EOI, 3 - ROUTE -> Just open, not added yet
	public static final String ADD_RESPONSE_TEXT 				= "28"; //Dialog Add response -> Button: Add Text -> Save
	public static final String ADD_RESPONSE_IMAGE				= "29"; //Dialog Add response -> Button: Add Image -> Save
	public static final String ADD_RESPONSE_AUDIO				= "30"; //Dialog Add response -> Button: Add Audio -> Save
	public static final String ADD_RESPONSE_VIDEO				= "31"; //Dialog Add response -> Button: Add Video -> Save
	public static final String ADD_RESPONSE_DESC				= "32"; //Dialog Add response -> Button: Add Video -> Save
	
	public static final String SELECT_UPLOAD_RESPONSE			= "33"; //Response Tab -> Button: Upload
	public static final String SELECT_VIEW_RESPONSE				= "34"; //Response Tab -> Button: View
	public static final String SELECT_DELETE_RESPONSE			= "35"; //Response Tab -> Button: Delete
	
	public static final String SELECT_BACK_BUTTON				= "36"; //Button Back of Android
	public static final String SELECT_PUSH_AGAIN				= "37"; //Button Back of Android
	public static final String SHOW_GPS_INFO					= "38"; //Show GPS in title bar

	
	private void createActionNameHashtable()
	{
		actionNames.put("0", "START_APP");
		actionNames.put("1", "EXIT_APP");
		actionNames.put("2", "SELECT_YAH");
		actionNames.put("3", "SELECT_LOGIN");
		actionNames.put("4", "SELECT_LOGOUT");
		actionNames.put("5", "VIEW_CACHED_EXPERIENCES");
		actionNames.put("6", "VIEW_ONLINE_EXPERIENCES");
		
		actionNames.put("7", "DOWNLOAD_ONLINE_EXPERIENCE");
		actionNames.put("8", "CANCEL_DOWNLOAD_EXPERIENCE");
		actionNames.put("9", "PLAY_EXPERIENCE");
		actionNames.put("10", "CANCEL_PLAY_EXPERIENCE");
		actionNames.put("11", "DELETE_EXPERIENCE");
		
		actionNames.put("12", "SELECT_PUSH");
		actionNames.put("13", "SELECT_SOUND");
		actionNames.put("14", "SELECT_VIBRATION");
		actionNames.put("15", "SELECT_SETELLITE");
		actionNames.put("16", "SELECT_ROTATION");
		actionNames.put("17", "SELECT_YAH_CENTRED");
		actionNames.put("18", "SELECT_TEST");
		actionNames.put("19", "SELECT_TRIGGER_ZONE");
		actionNames.put("20", "SELECT_POI_THUMBS");
		actionNames.put("21", "SELECT_RESET_POI");
		
		actionNames.put("22", "SELECT_MAP_TAB");
		actionNames.put("23", "SELECT_POI_TAB");
		actionNames.put("24", "SELECT_EOI_TAB");
		actionNames.put("25", "SELECT_SUMMARY_TAB");
		actionNames.put("26", "SELECT_RESPONSE_TAB");
		
		actionNames.put("27", "OPEN_RESPONSE_DIALOG");
		actionNames.put("28", "ADD_RESPONSE_TEXT");
		actionNames.put("29", "ADD_RESPONSE_IMAGE");
		actionNames.put("30", "ADD_RESPONSE_AUDIO");
		actionNames.put("31", "ADD_RESPONSE_VIDEO");
		actionNames.put("32", "ADD_RESPONSE_DESC");
		
		actionNames.put("33", "SELECT_UPLOAD_RESPONSE");
		actionNames.put("34", "SELECT_VIEW_RESPONSE");
		actionNames.put("35", "SELECT_DELETE_RESPONSE");
		
		actionNames.put("36", "SELECT_BACK_BUTTON");
		actionNames.put("37", "SELECT_PUSH_AGAIN");
		actionNames.put("38", "SHOW_GPS_INFO");
	}
	//display a graph
	public void showGraph() {
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				MainActivity mActivity = (MainActivity)activity;
				AlertDialog.Builder alert = new AlertDialog.Builder(mActivity);
				if (logStore != null && !logStore.isEmpty()) {
					alert.setTitle("Graph Of Logs");

					LayoutInflater inflater = mActivity.getLayoutInflater();
					View view = inflater.inflate(R.layout.graph_view,null);
					BarChart chart = (BarChart) view.findViewById(R.id.chart );
					chart.setDragEnabled(true);
					chart.setMaxVisibleValueCount(5);
					chart.setDescription("");

					//xAxis
					XAxis xAxis = chart.getXAxis();
					xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
					xAxis.setDrawGridLines(false);
					xAxis.setSpaceBetweenLabels(1);

					YAxis leftAxis = chart.getAxisLeft();
					leftAxis.setPosition(YAxis.YAxisLabelPosition.OUTSIDE_CHART);
					leftAxis.setSpaceTop(15);
					leftAxis.setLabelCount(8, false);
					//leftAxis.setValueFormatter(custom);
					leftAxis.setSpaceTop(15f);
					leftAxis.setAxisMinValue(0f); // this rep


					Collections.sort(logStore,sort);

					ArrayList<BarEntry>yVals = new ArrayList<BarEntry>();
					for(Integer[] i : logStore){
						BarEntry bar = new BarEntry(i[1],i[0]);
						yVals.add(bar);
					}
					ArrayList<String> xVals = new ArrayList<String>();
					for(Integer[] i : logStore){
						String s = actionNames.get(i[0].toString());
						xVals.add(s);
					}
					BarDataSet data;
					if (chart.getData() != null &&
							chart.getData().getDataSetCount() > 0) {
						data = (BarDataSet) chart.getData().getDataSetByIndex(0);
						data.setYVals(yVals);
						chart.getData().setXVals(xVals);
					}else{
						data = new BarDataSet(yVals,"DataSet");
						ArrayList<IBarDataSet> dataSets = new ArrayList<IBarDataSet>();
						dataSets.add(data);
						BarData addData = new BarData(xVals,dataSets);
						chart.setData(addData);
					}

					chart.setVisibleXRangeMinimum(2);
					chart.setVisibleXRangeMaximum(3);
					chart.setMinimumHeight((int)(mActivity.getResources().getDisplayMetrics().heightPixels*0.90)-100);

					alert.setNegativeButton("Close", null);    //Do nothing
					alert.setView(view);
				} else
					alert.setMessage("No information found In This Log");
				mActivity.setDialogFontSizeAndShow(alert, 18f);

			}
		});
	}
}
