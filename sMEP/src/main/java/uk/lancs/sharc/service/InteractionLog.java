package uk.lancs.sharc.service;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Hashtable;

import com.dropbox.sync.android.DbxAccountManager;
import com.google.android.gms.maps.model.LatLng;

import android.app.Activity;
import android.os.Build;
import android.text.TextUtils;

import uk.lancs.sharc.controller.MainActivity;
import uk.lancs.sharc.service.SharcLibrary;

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
		
		try {
			oswLogWriter.append(TextUtils.join(",", logData.toArray()));//Separate fields by comma 
			oswLogWriter.append(System.getProperty("line.separator"));
			oswLogWriter.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	}
	
	public static final String START_APP 						= "00";
	public static final String EXIT_APP 						= "01";
	public static final String SELECT_YAH 						= "02"; //Sliding menu: Set You-Are-Here (YAH) marker
	public static final String SELECT_LOGIN 					= "03"; //Sliding menu:
	public static final String SELECT_LOGOUT 					= "04"; //Sliding menu:
	public static final String VIEW_CACHED_EXPERIENCES 			= "05"; //Sliding menu: Play a downloaded experience 
	public static final String VIEW_ONLINE_EXPERIENCES 			= "06"; //Sliding menu: Download experiences	
	
	public static final String DOWNLOAD_ONLINE_EXPERIENCE		= "07"; //A dialog shows when an online experience marker on map is selected: Download 
	public static final String CANCEL_DOWNLOAD_EXPERIENCE 		= "08"; //A dialog shows when an online experience marker on map is selected: Cancel
	public static final String PLAY_EXPERIENCE 					= "09"; //A dialog shows when a cached experience marker on map is selected: Play
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
	public static final String SHOW_GPS_INFO					= "48"; //Show GPS in title bar

	
	private void createActionNameHashtable()
	{
		actionNames.put("00", "START_APP");
		actionNames.put("01", "EXIT_APP");
		actionNames.put("02", "SELECT_YAH");
		actionNames.put("03", "SELECT_LOGIN");
		actionNames.put("04", "SELECT_LOGOUT");
		actionNames.put("05", "VIEW_CACHED_EXPERIENCES");
		actionNames.put("06", "VIEW_ONLINE_EXPERIENCES");
		
		actionNames.put("07", "DOWNLOAD_ONLINE_EXPERIENCE");
		actionNames.put("08", "CANCEL_DOWNLOAD_EXPERIENCE");
		actionNames.put("09", "PLAY_EXPERIENCE");
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
		actionNames.put("48", "SHOW_GPS_INFO");
	}
}
