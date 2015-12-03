package uk.lancs.sharc.controller;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import uk.lancs.sharc.R;
import uk.lancs.sharc.model.MapWindowAdapter;
import uk.lancs.sharc.service.BackgroundService;
import uk.lancs.sharc.service.DBExperienceMetadata;
import uk.lancs.sharc.service.ErrorReporter;
import uk.lancs.sharc.service.SharcLibrary;
import uk.lancs.sharc.model.ExperienceDetailsModel;
import uk.lancs.sharc.model.ExperienceMetaDataModel;
import uk.lancs.sharc.model.InteractionLog;
import uk.lancs.sharc.model.JSONParser;
import uk.lancs.sharc.model.SMEPAppVariable;
import uk.lancs.sharc.model.MediaListAdapter;
import uk.lancs.sharc.model.ResponseListAdapter;
import uk.lancs.sharc.model.SMEPSettings;
import uk.lancs.sharc.model.ResponseModel;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.media.MediaRecorder;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.dropbox.sync.android.DbxAccountInfo;
import com.dropbox.sync.android.DbxAccountManager;
import com.dropbox.sync.android.DbxDatastore;
import com.dropbox.sync.android.DbxDatastoreManager;
import com.dropbox.sync.android.DbxFile;
import com.dropbox.sync.android.DbxFileSystem;
import com.dropbox.sync.android.DbxPath;
import com.dropbox.sync.android.DbxRecord;
import com.dropbox.sync.android.DbxTable;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapClickListener;
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.jeremyfeinstein.slidingmenu.lib.app.SlidingActivity;

/**
 * <p>This class controls the logic of the app and user interaction</p>
 *
 * Author: Trien Do
 * Date: Feb 2014
 **/
public class MainActivity extends SlidingActivity implements OnMapClickListener {

	//constants
	private static final String TAG = "SPET_MAIN";


    private final int LOCATION_INTERVAL = 3;
	private final float LOCATION_DISTANCE = 10f;
	private final float FONT_SIZE = 18f;

	//Google map
	private MapFragment mMapFragment;
	private GoogleMap mMap;		            //Google Maps object
	private Marker currentPosition;         //Mock location -> to simulate a fake current location
	private LatLng initialLocation = null;  //Move to where the current location of the user when starting the app
	private LatLng sessionLocation = null;  //A session of responses for a new location (one or more responses can be added at the same location. They should be treated as all media for a new POI
    private Marker currentPos;              //Real position
    private Circle currentAccuracy;         //accuracy
	private ArrayList<Marker> allExperienceMarkers = new ArrayList<Marker>();
    Button btnResponse;
	private Location lastKnownLocation = null;
	
	//Setting
	SMEPSettings smepSettings = new SMEPSettings();
	AlertDialog adYAH;                      //Dialog box to select YAH marker --> need to close this dialog from other place -> global
    private int selectedLocationIcon = 0;   //id of the selected YAH icon
    private ProgressDialog pDialog;         //dialog shows waiting icon when downloading data

	//Experience
	private List<ExperienceMetaDataModel> allExperienceMetaData = new ArrayList<ExperienceMetaDataModel>();//Store all available experiences (either online or cached)

	public ExperienceDetailsModel getSelectedExperienceDetail() {
		return selectedExperienceDetail;
	}

	private ExperienceDetailsModel selectedExperienceDetail;                                                    //Details of the current experience
    DBExperienceMetadata experienceMetaDB = new DBExperienceMetadata(this);                                     //DB stores metadata of all experiences
	private ArrayList<Integer> nearbyExperiences = new ArrayList<Integer>();                                    //Array of IDs of Experiences within 5 km

	//Tab view - Menu
	private Menu actionbarMenu;	        //tab menu
	int currentTab = 0;			        //which tab is current selected
	int currentPOIIndex = -1;			//id of the current POI displayed in the media pane
    ViewGroup.LayoutParams params;      //Control how to show maps - height = 0 or fill parent
	ListView mediaItemsPresentation;    //List views to render media for tabs POI, EOI, Summary
	ListView responseTab;               //List views to render Response tab

    //Direction sensor
    private static SensorManager sensorService; //To get heading of the device
    private Sensor sensor;                      //Manage all sensors
    private float mDeclination;                 //heading of the device

    //Location service
    LocationListener[] mLocationListeners = new LocationListener[] {
	        new LocationListener(LocationManager.GPS_PROVIDER),
	        new LocationListener(LocationManager.NETWORK_PROVIDER)
	};
	String testingCode;
	
	//Dropbox
	private static final String APP_KEY = "nz8hhultn33wlqu";        //app key genereated from Dropbox App
    private static final String APP_SECRET = "4kldjr3m3330ytn";     //app secret genereated from Dropbox App
    private static final int REQUEST_LINK_TO_DBX = 0;
    private DbxAccountManager mDbxAcctMgr;
    DbxAccountInfo dbUser = null;

    //Response
    private static final int TAKE_PICTURE = 9999;                   //mark if the user is taking a picture
    private static final int CAPTURE_VIDEO = 8888;                  //mark if the user is taking a video
    private Uri fileUri;                                            //file url to store image/video
    private MediaRecorder myAudioRecorder;                          //to record audio
	private static String outputFile = null;                        //path to output file of responses (e.g., photo, video, audio)
	private long startTime = 0L;                                    //Timer showing that voice is being recorded
    private Handler customHandler = new Handler();                  //handle the recording dialog
    long timeInMilliseconds = 0L;
    long updatedTime = 0L;
    private TextView timerValue;                                    //Textview to display recording time
    
    //Logfile
    InteractionLog smepInteractionLog; 
		
	//////////////////////////////////////////////////////////////////////////////
	// INIT - ACTIVITY
	//////////////////////////////////////////////////////////////////////////////	
    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		init();
		//checkAndReportCrash();
	}

    @Override
	public boolean onCreateOptionsMenu(Menu menu) {
		//getMenuInflater().inflate(R.menu.action_bar_button, menu);
		actionbarMenu = menu;
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		//getActionBar().hide();
		actionBar.setDisplayShowHomeEnabled(false);//Hide home button
		getActionBar().setDisplayShowTitleEnabled(false);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle("GPS is not available yet. Please wait...");
		//Replace the up icon of app with menu icon
        ViewGroup home = (ViewGroup) findViewById(android.R.id.home).getParent();
		// get the first child (up imageview)
		((ImageView)home.getChildAt(0)).setImageResource(R.drawable.ic_drawer);
		getActionBar().setHomeButtonEnabled(true);

		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

		ActionBar.Tab tab = actionBar.newTab()
				.setText("MAP")
				.setTabListener(new SharcTabListener(0));
		actionBar.addTab(tab);

		tab = actionBar.newTab()
				.setText("MEDIA")
				.setTabListener(new SharcTabListener(1));
		actionBar.addTab(tab);

        tab = actionBar.newTab()
                .setText("EVENTS")
                .setTabListener(new SharcTabListener(2));
        actionBar.addTab(tab);

		tab = actionBar.newTab()
				.setText("SUMMARY")
				.setTabListener(new SharcTabListener(3));
		actionBar.addTab(tab);

		tab = actionBar.newTab()
				.setText("    UPLOAD  \nRESPONSES")
				.setTabListener(new SharcTabListener(4));
		actionBar.addTab(tab);
		return true;
	}

	class SharcTabListener implements ActionBar.TabListener {
		private int tabID;

		public SharcTabListener(int id)
		{
			tabID = id;
		}
		@Override
		public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
			processTab(tabID);
		}

		@Override
		public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {

		}

		@Override
		public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {

		}
	}

	public void processTab(int tabID)
	{
		switch(tabID)
		{
			case android.R.id.home:
				getSlidingMenu().showMenu(true);
				break;
			case 0:
				displayMapTab();
				break;
			case 1:
				switchToPOIMediaTab("FROM_TAB");
				break;
			case 2:
				displayEOIMediaTab();
				break;
			case 3:
				displayInfoTab();
				break;
			case 4:
				displayResponseTab();
				break;
		}
	}

    @Override
	public void onDestroy()
	{
		Log.d(TAG, "Stopping service");
		stopService(new Intent(this, BackgroundService.class));
	    super.onDestroy();
	    if (sensor != null) {
			sensorService.unregisterListener(mySensorEventListener);
		}
	    smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.EXIT_APP, "exit");
	}
	
    public void startBackgroundService()
    {
    	//Start tracking service
		Log.e(TAG, "Starting service");
		Intent trackingIntent = new Intent(this, BackgroundService.class);			
	    startService(trackingIntent);
    }
    
	@Override
    public boolean onKeyDown(int keyCode, KeyEvent event) 
	{
		if(keyCode==KeyEvent.KEYCODE_BACK)
		{
			smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_BACK_BUTTON, "from tab " + currentTab);
			displayMapTab();			
			return true;
		}
		else if(keyCode==KeyEvent.KEYCODE_HOME)
		{
			getSlidingMenu().showMenu(true);
			//smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_HOME_BUTTON, "open sliding menu");
			return true;
		}
		else
			return super.onKeyDown(keyCode, event); 
	}

	public void createActionListenerForSlideMenu()
    {
		//SMEP version
		TextView txtVersion = (TextView) findViewById(R.id.txtSetting);
		txtVersion.setText("SMEP Settings (Version " + getString(R.string.app_version) + ")");
		smepSettings.setAppVersion(getString(R.string.app_version));

		//Three buttons for login/out
		ImageButton imgBtnUser = (ImageButton) findViewById(R.id.imgBtnUser);
		imgBtnUser.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				loginOrLogout();
			}
		});
		
		TextView txtUserName = (TextView) findViewById(R.id.txtUsername);
		txtUserName.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				loginOrLogout();
			}
		});

		TextView txtUseremail = (TextView) findViewById(R.id.txtUseremail);
		txtUseremail.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				loginOrLogout();
			}
		});
		//Done login/out

		Button btnShowHelp = (Button) findViewById(R.id.btnShowHelp);
		btnShowHelp.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				showHelp();
			}
		});

        Button btnLoadFile = (Button) findViewById(R.id.btnLoadProject);
		btnLoadFile.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				presentDownloadedExperiences();
			}
		});
		
		Button btnExploreProject = (Button) findViewById(R.id.btnExploreProject);
		btnExploreProject.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				loadOnlineExperiences();
			}
		});
		
		Button btnSelectYAH = (Button) findViewById(R.id.btnSelectYAH);
		btnSelectYAH.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				selectYAHMarker();
			}
		});

		Switch switchShowGPS = (Switch) findViewById(R.id.switchShowGPS);
		switchShowGPS.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				smepSettings.setIsShowingGPS(isChecked);
				if (!isChecked)
					getActionBar().setTitle("[" + getString(R.string.app_name) + " - V" + getString(R.string.app_version) + "]");
				else {
					if (lastKnownLocation != null)
						getActionBar().setTitle("GPS Accuracy: " + String.format("%.1f", lastKnownLocation.getAccuracy()) + " (m)");
					else
						getActionBar().setTitle("GPS is not available");
				}
				smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SHOW_GPS_INFO, String.valueOf(isChecked));
			}
		});

		Switch switchPushMedia = (Switch) findViewById(R.id.switchPushMedia);
		switchPushMedia.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				smepSettings.setPushingMedia(isChecked);
				smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_PUSH, String.valueOf(isChecked));
			}
		});

		Switch switchPushMediaAgain = (Switch) findViewById(R.id.switchPushMediaAgain);
		switchPushMediaAgain.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
                SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) getApplicationContext();
                mySMEPAppVariable.setIsPushAgain(isChecked);
				smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_PUSH_AGAIN, String.valueOf(isChecked));
			}
		});
		

		Switch switchSoundNotification = (Switch) findViewById(R.id.switchSoundNotification);
		switchSoundNotification.setOnCheckedChangeListener(new OnCheckedChangeListener() {
		   @Override
		   public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) 
		   {
			   smepSettings.setSoundNotification(isChecked);
			   SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) getApplicationContext();
			   mySMEPAppVariable.setSoundNotification(isChecked);
			   smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_SOUND, String.valueOf(isChecked));
		   }
		});
		
		Switch switchVibrationNotification = (Switch) findViewById(R.id.switchVibrationNotification);
		switchVibrationNotification.setOnCheckedChangeListener(new OnCheckedChangeListener() {
		 
		   @Override
		   public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) 
		   {
			   smepSettings.setVibrationNotification(isChecked);
			   SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) getApplicationContext();
			   mySMEPAppVariable.setVibrationNotification(isChecked);
			   smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_VIBRATION, String.valueOf(isChecked));
		   }
		});
		
		Switch switchMapType = (Switch) findViewById(R.id.switchMapType);
		switchMapType.setOnCheckedChangeListener(new OnCheckedChangeListener() {
		 
		   @Override
		   public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) 
		   {
			   smepSettings.setSatellite(isChecked);	
			   if(isChecked)
		    		mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
			   else
		    		mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
			   smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_SETELLITE, String.valueOf(isChecked));
		   }
		});
		
		Switch switchMapRotate = (Switch) findViewById(R.id.switchMapRotate);
		switchMapRotate.setOnCheckedChangeListener(new OnCheckedChangeListener() {
		 
		   @Override
		   public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) 
		   {
			   smepSettings.setRotating(isChecked);
			   if(isChecked)
		    		sensorService.registerListener(mySensorEventListener, sensor,SensorManager.SENSOR_DELAY_NORMAL);
			   else
		    		sensorService.unregisterListener(mySensorEventListener);
			   smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_ROTATION, String.valueOf(isChecked));
		   }
		});
		
		Switch switchMapCentre = (Switch) findViewById(R.id.switchMapCentre);
		switchMapCentre.setOnCheckedChangeListener(new OnCheckedChangeListener() {
		 
		   @Override
		   public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) 
		   {
			   smepSettings.setYAHCentred(isChecked);	
			   smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_YAH_CENTRED, String.valueOf(isChecked));
		   }
		});	
		
		Switch switchTestMode = (Switch) findViewById(R.id.switchTestMode);
		switchTestMode.setOnCheckedChangeListener(new OnCheckedChangeListener() {
		 
		   @Override
		   public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) 
		   {
			   	final boolean selection = isChecked;
			   	smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_TEST, String.valueOf(isChecked));
			   	if(isChecked)
			   	{
				    AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
					alert.setTitle("Please enter the testing code:");				
					final EditText testCode = new EditText(MainActivity.this);
					alert.setView(testCode);				
					
					alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            testingCode = testCode.getText().toString();
                            smepSettings.setTestMode(selection);
                            //Global variable for the whole application
                            SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) getApplicationContext();
                            currentPosition.setVisible(selection);
                            mySMEPAppVariable.setTestMode(selection);
                        }
                    });
	
					alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Canceled.
                            Switch switchTest = (Switch) findViewById(R.id.switchTestMode);
                            switchTest.setSelected(false);
                        }
                    });
                    setDialogFontSizeAndShow(alert, FONT_SIZE);
					//alert.show();
			   	}
			   	else
			   	{
			   		smepSettings.setTestMode(selection);
					//Global variable for the whole application
					SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) getApplicationContext();
					currentPosition.setVisible(selection);
					mySMEPAppVariable.setTestMode(selection);
			   	}
		   }
		});
		
		Switch switchTriggerZone = (Switch) findViewById(R.id.switchTriggerZone);
		switchTriggerZone.setOnCheckedChangeListener(new OnCheckedChangeListener() {
		   @Override
		   public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) 
		   {
			   smepSettings.setShowingTriggers(isChecked);
			   smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_SHOW_TRIGGER_ZONE, String.valueOf(isChecked));
			   selectedExperienceDetail.showTriggerZones(isChecked);
		   }
		});
		
		Switch switchShowPOI = (Switch) findViewById(R.id.switchShowPOI);
		switchShowPOI.setOnCheckedChangeListener(new OnCheckedChangeListener() {
		   @Override
		   public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) 
		   {
			   smepSettings.setShowingThumbnails(isChecked);
			   smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_SHOW_POI_THUMBS, String.valueOf(isChecked));
			   selectedExperienceDetail.showPOIThumbnails(isChecked);
		   }
		});
		
		Button btnResetPOIs = (Button) findViewById(R.id.btnResetPOIs);
		btnResetPOIs.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
		    	//Global variable for the whole application
			    SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) getApplicationContext();
			    mySMEPAppVariable.setResetPOI(true);
			    getSlidingMenu().showContent(false);
			    smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_RESET_POI, "resetPOI");
			}
		});
    }

	public void showHelp()
	{
		Intent help = new Intent(MainActivity.this, Usermanual.class);
		startActivity(help);
	}

    public void loadOnlineExperiences()
    {
        if(SharcLibrary.isNetworkAvailable(this))
        {
            gotoExperiencesBrowsingMapMode();
            new GetAllOnlineExperiencesThread().execute();
        }
        else
            Toast.makeText(this, getString(R.string.message_wifiConnection), Toast.LENGTH_LONG).show();
    }

    public void loginOrLogout()
    {
        if(mDbxAcctMgr.hasLinkedAccount())
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setTitle("Authentication")
                    .setMessage("Are you sure that you want to log out?")
                    .setIcon(android.R.drawable.ic_dialog_alert)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            mDbxAcctMgr.getLinkedAccount().unlink();
                            displayDropboxUser(null);
                            smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_LOGOUT, "logout");
                        }
                    })
                    .setNegativeButton("No", null)	//Do nothing on no
                    .show();
        }
        else
        {
            if(SharcLibrary.isNetworkAvailable(MainActivity.this))
                mDbxAcctMgr.startLink(MainActivity.this, REQUEST_LINK_TO_DBX);
            else
                Toast.makeText(MainActivity.this, getString(R.string.message_wifiConnection), Toast.LENGTH_LONG).show();

        }
    }

    public void presentDownloadedExperiences()
    {
        gotoExperiencesBrowsingMapMode();
        getAllExperienceMetaDataFromLocalDatabase();
        addDBExperienceMarkerListener();
    }

    public void gotoExperiencesBrowsingMapMode()
    {
    	getSlidingMenu().showContent(false);
		displayMapTab();				
		clearMap();
		moveAndZoomToLocation(initialLocation, 10);
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
        if(selectedExperienceDetail == null && item.getItemId() != android.R.id.home) {
            Toast.makeText(this, getString(R.string.message_no_experience), Toast.LENGTH_LONG).show();
            return true;
        }
		else
			processTab(item.getItemId());
	    return true;
	}
	
	public void init()
	{		
		try
	    {
            btnResponse = (Button) findViewById(R.id.btnAddResponse);
            setBehindContentView(R.layout.sliding_menu); //https://www.youtube.com/watch?v=vmiUh0RQ7QY --> Sliding menu tutorial
			//getSlidingMenu().setBehindWidth(630);
			DisplayMetrics metrics = getResources().getDisplayMetrics();		
			final float menu_width = 315.0f;
			getSlidingMenu().setBehindWidth((int) (metrics.density * menu_width));
			createActionListenerForSlideMenu();
			mMapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.myMap);
        	mMap = mMapFragment.getMap();
        	
	        if (mMap != null) {
	        	mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);       	//mMap.control
	        	//mMap.setMyLocationEnabled(true);
	        	mMap.getUiSettings().setZoomControlsEnabled(true); 	//mMap.setPadding(0, 0, 0, 70);
	        }
	        mMap.setOnMapClickListener(this);
            mMap.setInfoWindowAdapter(new MapWindowAdapter(this));
	        //Make sharc folder
	        SharcLibrary.createFolder(SharcLibrary.SHARC_FOLDER);	        
  	        //Make media folder to store media files
	        SharcLibrary.createFolder(SharcLibrary.SHARC_MEDIA_FOLDER);
			//Create log folder
			SharcLibrary.createFolder(SharcLibrary.SHARC_LOG_FOLDER);
	       
	        //Current mock position
	        currentPosition = mMap.addMarker(new MarkerOptions()
	        	.position(new LatLng(0,0))	        
	        	.icon(BitmapDescriptorFactory.fromResource(R.raw.location)) 
	        	.anchor(0.5f, 0.5f)
	        );
	        currentPosition.setVisible(false);
	        
	        //Current real position
	        selectedLocationIcon = R.raw.yahred24;
	        createCurrentLocationMarker();
	        
	        setUpLocationService();
	        //Start sensor
	        sensorService = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
			sensor = sensorService.getDefaultSensor(Sensor.TYPE_ORIENTATION);
			if (sensor != null) 
				sensorService.registerListener(mySensorEventListener, sensor,SensorManager.SENSOR_DELAY_NORMAL);
			params = mMapFragment.getView().getLayoutParams();
			setupListView();
			startBackgroundService();
			checkDropboxLogin();
			smepInteractionLog = new InteractionLog();
			smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.START_APP, smepSettings.getAppVersion());
			//showTermsAndConditions();
	    } 
	    catch (Exception e) 
	    {
	        Log.e(TAG, "Error: " + e.toString());	        
	    }
	}

	public void showTermsAndConditions()
	{
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		//alert.setCancelable(false);
		alert.setTitle("Terms and conditions");
		alert.setMessage(getString(R.string.terms_and_conditions));
		alert.setPositiveButton("I agree", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				selectYAHMarker();
				showHelp();
				//smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.ADD_RESPONSE_TEXT,  entity[0] + "#" + entity[1]);
			}
		});
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				finish();
				//smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.ADD_RESPONSE_TEXT,  entity[0] + "#" + entity[1]);
			}
		});
		setDialogFontSizeAndShow(alert, FONT_SIZE);
	}

	public void downloadOrOpen()
	{
		AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Starting SMEP");
		//alert.setCancelable(false);
		LayoutInflater factory = LayoutInflater.from(this);
		alert.setMessage("How would you like to start?");
		alert.setPositiveButton("Download an experience", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                loadOnlineExperiences();
                //smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.ADD_RESPONSE_TEXT,  entity[0] + "#" + entity[1]);
            }
        });
		alert.setNeutralButton("Play an experience", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				presentDownloadedExperiences();
				//smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.ADD_RESPONSE_TEXT,  entity[0] + "#" + entity[1]);
			}
		});

		//alert.setNegativeButton("Cancel", null);
        setDialogFontSizeAndShow(alert, FONT_SIZE);
		//alert.show();
	}
	public void createCurrentLocationMarker()//SMEP doesn't use a default YAH marker of Google Maps. currentPos is used for this purpose
	{
		currentPos = mMap.addMarker(new MarkerOptions()
			.position(initialLocation!=null? initialLocation : new LatLng(54.101519,-2.604666))
	        .icon(BitmapDescriptorFactory
                    .fromResource(selectedLocationIcon))
	        //.anchor(0.5f, 0.71f)
	        .anchor(0.5f, 0.5f)
	        .title("YAH")
	        );
        currentPos.showInfoWindow();
		currentAccuracy = mMap.addCircle(new CircleOptions().center(currentPos.getPosition()).radius(10f).strokeColor(Color.argb(100, 93, 188, 210)).strokeWidth(1).fillColor(Color.argb(30, 93, 188, 210)));
	}
			
	public void clearMap()
	{
		//Data
		allExperienceMetaData.clear();	
		//Viz
		allExperienceMarkers.clear();
		if(selectedExperienceDetail != null)
			selectedExperienceDetail.clearExperience();
		mMap.clear();
		//Add current location again
		currentPosition = mMap.addMarker(new MarkerOptions()
						.position(new LatLng(54.101519, -2.604666))
						.icon(BitmapDescriptorFactory.fromResource(R.raw.location))
						.anchor(0.5f, 0.5f)
		);
		currentPosition.setVisible(smepSettings.isTestMode());
        btnResponse.setVisibility(View.GONE);
		createCurrentLocationMarker();
        selectedExperienceDetail = null;
        currentPOIIndex = -1;
	}
	
	public void selectYAHMarker()
	{	
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		adYAH = alert.create();
		adYAH.setTitle("Please select a You-Are-Here (YAH) marker");
		adYAH.setCancelable(false);
		LayoutInflater factory = LayoutInflater.from(this);
		final View textEntryView = factory.inflate(R.layout.select_yah_dialog, null);
		adYAH.setView(textEntryView);
		adYAH.show();		
	}
	
	public void setBlueYAH(View v)
	{
		setYAHMarker(R.raw.yahblue24, "SmallBlue");
	}
	
	public void setRedYAH(View v)
	{
		setYAHMarker(R.raw.yahred24, "SmallRed");
	}
	
	public void setBlueYAHBig(View v)
	{
		setYAHMarker(R.raw.yahblue32, "BigBlue");
	}
	
	public void setRedYAHBig(View v)
	{
		setYAHMarker(R.raw.yahred32, "BigRed");
	}

	public void setYAHMarker(int iconID, String iconText)
	{
		adYAH.dismiss();
		selectedLocationIcon = iconID;
		currentPos.setIcon(BitmapDescriptorFactory.fromResource(selectedLocationIcon));
		smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_YAH, iconText);
		if(selectedExperienceDetail == null) {
			downloadOrOpen();
			getSlidingMenu().showMenu(true);
		}
		else
			getSlidingMenu().toggle();
	}
	//////////////////////////////////////////////////////////////////////////////
	// TABS
	//////////////////////////////////////////////////////////////////////////////	
	public void setSelectedTabIcons(int index)
	{
		currentTab = index;
        if(index == 4)//Change icon
            btnResponse.setCompoundDrawablesWithIntrinsicBounds(R.drawable.upload, 0, 0, 0);
        else
            btnResponse.setCompoundDrawablesWithIntrinsicBounds(R.drawable.addnew, 0, 0, 0);

         if(selectedExperienceDetail != null)
            btnResponse.setVisibility(View.VISIBLE);
         else
            btnResponse.setVisibility(View.GONE);

		 switch (index)
		 {
			 case 0:
			 	btnResponse.setText("Add response for your current location");
			 	break;
			 case 1:
				btnResponse.setText("Add your response for this POI");
				break; 
			 case 2:
				btnResponse.setText("Add your response for EOIs");
			 	break;
			 case 3:
				btnResponse.setText("Add your response for this experience");
				break;
			 case 4:
				btnResponse.setText("Upload all responses");
				break;
		 }
		getActionBar().setSelectedNavigationItem(index);
	}
	
	public void displayMapTab()
	{
		setSelectedTabIcons(0);
		mediaItemsPresentation.setVisibility(View.GONE);
		showMap(true);
		smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_MAP_TAB, mMap.getCameraPosition().target.latitude + " " + mMap.getCameraPosition().target.longitude + "#" + mMap.getCameraPosition().zoom);
	}
	
	public void switchToPOIMediaTab(String type)//type = 0: selected by user from tab, 1: selected by user from POI marker, 2: pushed by location service 
	{
		setSelectedTabIcons(1);
        responseTab.setVisibility(View.GONE);
        showMap(false);
        mediaItemsPresentation.setVisibility(View.VISIBLE);
		if(currentPOIIndex >=0)
		{
            btnResponse.setVisibility(View.VISIBLE);
            displayMediaItems(selectedExperienceDetail.getPOIHtmlListItems(currentPOIIndex, initialLocation), 0);
			smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_POI_TAB, type + "#" + selectedExperienceDetail.getPOIName(currentPOIIndex));
		}
		else
		{
            btnResponse.setVisibility(View.GONE);
            displayMediaItems(new ArrayList<String>(){{add("No media has been pushed/pulled yet");}}, 0);
			smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_POI_TAB, type + "#" + "No POI selected");
		}
	}

	public void displayMediaTab(int poiID,String type)
	{	
		currentPOIIndex = poiID;
		ArrayList<String> mediaList = selectedExperienceDetail.getPOIHtmlListItems(poiID, initialLocation);
		displayMediaItems(mediaList, 0);
				
	    if(smepSettings.isPushingMedia())   //Push -> Go to the POI Media tab
	    {
	    	switchToPOIMediaTab(type);
	    }
	    else                                //Pull -> Show notification icon for POI Media tab
	    {
	    	//actionbarMenu.getItem(1).setIcon(R.drawable.tab_new);
	    }
	}


	public void displayEOIMediaTab()
	{
		setSelectedTabIcons(2);
		mediaItemsPresentation.setVisibility(View.VISIBLE);
		responseTab.setVisibility(View.GONE);
		showMap(false);
				
		if(selectedExperienceDetail!=null)
		{
			List eoiList = selectedExperienceDetail.getAllEOIMediaListItems();
            displayMediaItems(eoiList, 1);
            if(eoiList.size() > 1)//header row so >=1
                btnResponse.setVisibility(View.VISIBLE);
            else
                btnResponse.setVisibility(View.GONE);
			smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_EOI_TAB, "All EOIs info");
		}
		else
		{
			displayMediaItems(new ArrayList<String>(){{add(getString(R.string.message_no_experience));}}, 1);
			smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_EOI_TAB, "No EOIs info");
		}
		
	}

	//Show info of an EOI when the user clicks on a button in the Point of Interest's media tab
	public void showSelectedEOI(final String EoiID)
	{
		//Work around error "Calling View methods on another thread than the UI thread"
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
				String[] eoiContent = selectedExperienceDetail.getHTMLCodeForEOI(EoiID);
				if (eoiContent != null) {
					alert.setTitle("Event: " + eoiContent[0]);
					WebView wv = new WebView(MainActivity.this);
					String base = "file://" + SharcLibrary.SHARC_MEDIA_FOLDER + "/";
					wv.loadDataWithBaseURL(base, eoiContent[1], "text/html", "utf-8", null);
					alert.setView(wv);
					//alert.setCancelable(false);
					alert.setNegativeButton("Close", null);    //Do nothing
				} else
					alert.setMessage("No information found for this Event of Interest");
				setDialogFontSizeAndShow(alert, FONT_SIZE);
				//alert.show();
				//smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_VIEW_RESPONSE, index + "#" + selectedExperienceDetail.getMyResponsePresentationName(index));
			}
		});
	}
	public void displayInfoTab()
	{
		setSelectedTabIcons(3);
		mediaItemsPresentation.setVisibility(View.VISIBLE);
		responseTab.setVisibility(View.GONE);
		showMap(false);
		if(selectedExperienceDetail!=null)
		{
			displayMediaItems(selectedExperienceDetail.getSumaryInfo(),2);
			smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_SUMMARY_TAB, "All summary info");
		}
		else
		{
			displayMediaItems(new ArrayList<String>(){{add(getString(R.string.message_no_experience));}}, 2);
			smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_EOI_TAB, "No summary info");
		}
		
	}
	
	public void displayResponseTab()
	{
		setSelectedTabIcons(4);
		mediaItemsPresentation.setVisibility(View.GONE);
		responseTab.setVisibility(View.VISIBLE);
		showMap(false);		
		ArrayList<String> responseList = new ArrayList<String>();
		if(selectedExperienceDetail!=null) {
			responseList.addAll(selectedExperienceDetail.getMyResponsesList());

			String connection = "";
			if (SharcLibrary.isNetworkAvailable(this) && dbUser != null) {
				connection = " Tap the yellow icon to view, blue icon to upload, and red icon to delete a response. You can also tap the 'Upload all responses' button at bottom of the screen to upload all of your responses.";
				if (!selectedExperienceDetail.isUpdatedConsumerExperience())
					keepTrackConsumerExperience();
			}
			if (SharcLibrary.isNetworkAvailable(this) && dbUser == null)
				Toast.makeText(this, getString(R.string.message_dropboxConnection), Toast.LENGTH_LONG).show();
			if (!SharcLibrary.isNetworkAvailable(this) && dbUser != null)
				Toast.makeText(this, getString(R.string.message_wifiConnection), Toast.LENGTH_LONG).show();
			String htmlCode = "Here you can review and upload your reponses for the experience: '" + selectedExperienceDetail.getMetaData().getProName() + "'.";
			if (responseList.size() > 0) {
				htmlCode += connection;
				btnResponse.setVisibility(View.VISIBLE);
			} else {
				htmlCode += " Currently there are no responses to review/upload.";
				btnResponse.setVisibility(View.GONE);
			}
			responseList.add(0,htmlCode);
		}
		else
			responseList.add(0, getString(R.string.message_no_experience));
		smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_RESPONSE_TAB, TextUtils.join("#", responseList));
	  	ResponseListAdapter adapter = new ResponseListAdapter(MainActivity.this, responseList);
		ListView mLv = (ListView)findViewById(R.id.responseTab);				
		mLv.setAdapter(adapter);
	}
	
	public void showMap(boolean isFull)
	{
		ImageButton goCurLocation = (ImageButton) findViewById(R.id.btnCurrentLocation);
		if(isFull)
		{
			params.height = params.MATCH_PARENT;
			goCurLocation.setVisibility(View.VISIBLE);
			//params.width = params.MATCH_PARENT;
		}
		else
		{
			params.height = 0;
			goCurLocation.setVisibility(View.GONE);
			//params.width = 350;
		}
		mMapFragment.getView().setLayoutParams(params);
	}
	
	public void displayMediaItems(List<String> mediaList, int type)
	{
		MediaListAdapter adapter = new MediaListAdapter(MainActivity.this, mediaList, type);
		//CustomList adapter = new CustomList(MediaReviewActivity.this, type, web, imageId);
		ListView mLv = (ListView)findViewById(R.id.webViewTab);				
		mLv.setAdapter(adapter);			   
	}
	
	public void setupListView()
	{		 
		ArrayList<String> data = new ArrayList<String>();
		data.add("No data available");
		MediaListAdapter adapter = new MediaListAdapter(MainActivity.this, data, 0);
		mediaItemsPresentation = (ListView)findViewById(R.id.webViewTab);
		mediaItemsPresentation.setAdapter(adapter);
		
		ResponseListAdapter resAdapter = new ResponseListAdapter(MainActivity.this, data);
		responseTab = (ListView)findViewById(R.id.responseTab);				
		responseTab.setAdapter(resAdapter);		
	}

	public void setDialogFontSizeAndShow(AlertDialog.Builder alert, final float fontSize)
	{
		AlertDialog alertDialog = alert.create();
		alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
			@Override
			public void onShow(DialogInterface dialog) {
				AlertDialog alertDialog = (AlertDialog) dialog;
				Button button = alertDialog.getButton(DialogInterface.BUTTON_POSITIVE);
				//button.setTypeface(Typeface.DEFAULT, Typeface.BOLD | Typeface.ITALIC);
				button.setTextSize(fontSize);
				button = alertDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
				button.setTextSize(fontSize);
				button = alertDialog.getButton(DialogInterface.BUTTON_NEUTRAL);
				button.setTextSize(fontSize);
			}
		});

		alertDialog.show();
	}

	//////////////////////////////////////////////////////////////////////////////
	// EXPERIENCE METADATA: ONLINE BROWSING - LOCAL BROWSING - RENDERING
	//////////////////////////////////////////////////////////////////////////////
    
	/*
		This inner class helps 
			- Get information of all available online experiences
			- Present each experience as a marker on Google Maps
			- Add the Click listener even for each marker
		Note this class needs retrieve information from server so it has to run in background
	*/
	class GetAllOnlineExperiencesThread extends AsyncTask<String, String, String> 
    { 
        //Before starting the background thread -> Show Progress Dialog        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Loading available experiences. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }
 
      //Get all available experiences       
        protected String doInBackground(String... args) 
        {            
            try 
            {
            	// Building Parameters
            	JSONParser jParser = new JSONParser();
            	List<NameValuePair> params = new ArrayList<NameValuePair>();
            	// Getting result in form of a JSON string from a Web RESTful
                JSONObject json = jParser.makeHttpRequest(SharcLibrary.url_all_experiences, "GET", params);
      
                String ret = json.getString("status");
                if (ret.equalsIgnoreCase("success"))
                {
                    // Get Array of experiences
                	JSONArray publishedExperiences = json.getJSONArray("data");
 
                    // Loop through all experiences
                	ExperienceMetaDataModel tmpExperience;   
                	String logData = "";
                    for (int i = 0; i < publishedExperiences.length(); i++)
                    {
                        JSONObject objExperience = publishedExperiences.getJSONObject(i);
                        // Storing each json item in variable
						int id = objExperience.getInt("id");
						String name = objExperience.getString("name");
						String description = objExperience.getString("description");
                        if(description.length() > 0 && description.charAt(description.length()-1) != '.')
							description.concat(".");
						String createdDate = objExperience.getString("createdDate");
                        String lastPublishedDate = objExperience.getString("lastPublishedDate");
						int designerId = objExperience.getInt("designerId");
						boolean isPublished = true;
						int moderationMode = objExperience.getInt("moderationMode");
						String latLng = objExperience.getString("latLng");
						String summary = objExperience.getString("summary");
						String snapshotPath = objExperience.getString("snapshotPath");
                        String thumbnailPath = objExperience.getString("thumbnailPath");
						int size = objExperience.getInt("size");
						String theme = objExperience.getString("theme");
                        tmpExperience = new ExperienceMetaDataModel(id, name, description, createdDate, lastPublishedDate, designerId, isPublished,
								moderationMode, latLng, summary, snapshotPath, thumbnailPath, size, theme);

                        logData += "#" + tmpExperience.getProName();
                        allExperienceMetaData.add(tmpExperience);                        
                    }
                	smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.VIEW_ONLINE_EXPERIENCES, logData);
                } 
                else 
                {
                    Toast.makeText(getApplicationContext(), "No experiences found",Toast.LENGTH_LONG).show();
                }
            } 
            catch (Exception e) 
            {
                e.printStackTrace();
            }
            return null;
        }
 
        //After completing background task ->Dismiss the progress dialog         
        protected void onPostExecute(String file_url) 
        {
            // dismiss the dialog after getting all files
            pDialog.dismiss();
            // updating UI from Background Thread
            runOnUiThread(new Runnable() 
            {
                public void run() 
                {
                	//Updating parsed JSON data into ListView
                	displayAllExperienceMetaData(true);
                	addOnlineExperienceMarkerListener();
                }
            });            
        } 
    }

	/*
		This inner class helps
			- Download a snapshot of an experience in form of json object
			- Download all media files from Dropbox
			- Present the experience
		Note this class needs retrieve information from server so it has to run in background
	*/
	class ExperienceDetailsThread extends AsyncTask<String, String, String>
	{
		//Before starting background thread -> Show Progress Dialog
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(MainActivity.this);
			pDialog.setMessage("Loading the experience. Please wait...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(false);
			pDialog.show();
		}

		protected String doInBackground(String... experienceId)
		{
			try{
				// Building Parameters
				JSONParser jParser = new JSONParser();
				List<NameValuePair> params = new ArrayList<NameValuePair>();
				// Getting result in form of a JSON string from a Web RESTful
				JSONObject json = jParser.makeHttpRequest(SharcLibrary.url_experience_snapshot + experienceId, "GET", params);
				String ret = json.getString("status");
				if (ret.equalsIgnoreCase("success")) {
					selectedExperienceDetail.getExperienceFromSnapshotOnDropbox(json.getJSONObject("data"));
					System.out.println("Experience json:" + json.getJSONObject("data"));
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			return null;
		}

		//After completing background task -> Dismiss the progress dialog
		protected void onPostExecute(String file_url)
		{
			// dismiss the dialog after getting all files
			pDialog.dismiss();
			// updating UI from Background Thread
			runOnUiThread(new Runnable()
			{
				public void run()
				{
					//Render the experience
					presentExperience();
				}
			});
		}
	}

	public void getAllExperienceMetaDataFromLocalDatabase()
    {
    	clearMap();
		allExperienceMetaData = experienceMetaDB.getExperiences();
		String logData = allExperienceMetaData.toString();
    	smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.VIEW_CACHED_EXPERIENCES, logData);
    	displayAllExperienceMetaData(false);
    }
    
    public void displayAllExperienceMetaData(boolean isOnline)
    {
    	Marker tmpMarker = null;
    	nearbyExperiences.clear();
    	ArrayList<String> nearbyExperienceName = new ArrayList<String>();
    	for (int i = 0; i < allExperienceMetaData.size() ; i++) {
    		LatLng exLocation = allExperienceMetaData.get(i).getLocation();
        	tmpMarker = mMap.addMarker(new MarkerOptions()
            	.title(String.valueOf(i))
            	.anchor(0, 0)
        		.position(exLocation)	        
        		.icon(BitmapDescriptorFactory.fromResource(R.raw.experience))	 
        		.visible(true)
            );
			//All experiences
			nearbyExperienceName.add(allExperienceMetaData.get(i).getProName());
			nearbyExperiences.add(i);//key = index of current list, value = index of marker --> reuse marker event
			//Only experiences around 5 km
			/*
            if(initialLocation != null)
            {
                //Get near by experience 5000m
                float[] results = new float[1];
                Location.distanceBetween(exLocation.latitude,exLocation.longitude, initialLocation.latitude,initialLocation.longitude, results);
                if(results[0] < 5000)//radius of circle
                {
                    nearbyExperienceName.add(allExperienceMetaData.get(i).getProName());
                    nearbyExperiences.add(i);//key = index of current list, value = index of marker --> reuse marker event
                }
            }
            else
                Toast.makeText(this, R.string.message_gps, Toast.LENGTH_LONG).show();
            */

    	}
        allExperienceMarkers.add(tmpMarker);                
        showNearByExperienceList(nearbyExperienceName.toArray(new CharSequence[nearbyExperienceName.size()]), isOnline);
    }
	
    public void addOnlineExperienceMarkerListener()
    {
    	mMap.setOnMarkerClickListener(new OnMarkerClickListener() 
        {				
			@Override
			public boolean onMarkerClick(Marker arg0) 
			{				
				return markerClick(arg0.getTitle(),true);
            }
        });
    }
    
    public void addDBExperienceMarkerListener()
    {
    	mMap.setOnMarkerClickListener(new OnMarkerClickListener() 
        {				
			@Override
			public boolean onMarkerClick(Marker arg0) 
			{				
				return markerClick(arg0.getTitle(),false);				
            }
        });
    }
    
    public boolean markerClick(String markerTitle, boolean isOnline)
    {
    	if(markerTitle.equalsIgnoreCase("YAH"))//current location marker
        {
            Toast.makeText(getApplicationContext(), "This circle shows your current location", Toast.LENGTH_LONG).show();
            currentPos.showInfoWindow();
            return true;
        }
    	final ExperienceMetaDataModel selectedExperienceMeta = allExperienceMetaData.get(Integer.parseInt(markerTitle));
    	if(isOnline)
    	{
			AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
	    	builder.setTitle(selectedExperienceMeta.getProName()  + " (" + selectedExperienceMeta.getProSize() + " MB)")
	    	.setMessage(selectedExperienceMeta.getProDesc() + " This experience is designed by " + selectedExperienceMeta.getProAuthID() + " on " + selectedExperienceMeta.getProDate() + ".")
					.setIcon(android.R.drawable.ic_dialog_alert)
	    	.setPositiveButton("Download", new DialogInterface.OnClickListener() {
	    	    public void onClick(DialogInterface dialog, int which) {			      	
	    	    	//experienceMetaDB.insertExperience(selectedExperienceMeta.getProName(), selectedExperienceMeta.getProPath(), selectedExperienceMeta.getProDesc(), selectedExperienceMeta.getProDate(), selectedExperienceMeta.getProAuthID(), selectedExperienceMeta.getProPublicURL(), selectedExperienceMeta.getProLocation());
					selectedExperienceMeta.save();
					clearMap();
                    selectedExperienceDetail = new ExperienceDetailsModel(selectedExperienceMeta.getId().intValue(), true);
					selectedExperienceDetail.setMetaData(selectedExperienceMeta);
					smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.DOWNLOAD_ONLINE_EXPERIENCE, selectedExperienceMeta.getProName());
					new ExperienceDetailsThread().execute(selectedExperienceMeta.getId().toString());
					setSelectedTabIcons(0);
	    	    }
	    	})
	    	.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
	    	    public void onClick(DialogInterface dialog, int which) {
	    	    	smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.CANCEL_DOWNLOAD_EXPERIENCE, selectedExperienceMeta.getProName());
	    	    }
	    	})
	    	.show();
    	}
    	else
    	{
			AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);
            alert.setTitle(selectedExperienceMeta.getProName())
	    	.setMessage(selectedExperienceMeta.getProDesc() + " This experience is designed by " + selectedExperienceMeta.getProAuthName() + " on " + selectedExperienceMeta.getProDate() + ".")
	    	.setIcon(android.R.drawable.ic_dialog_alert)
	    	.setPositiveButton("Play", new DialogInterface.OnClickListener() {
	    	    public void onClick(DialogInterface dialog, int which) {			      	
	    	    	clearMap();
                    selectedExperienceDetail = new ExperienceDetailsModel(selectedExperienceMeta.getId().intValue(), false);
					selectedExperienceDetail.setMetaData(selectedExperienceMeta);
					presentExperience();
					setSelectedTabIcons(0);
					smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.PLAY_EXPERIENCE, selectedExperienceMeta.getProName());
	    	    }
	    	})
	    	.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.CANCEL_PLAY_EXPERIENCE, selectedExperienceMeta.getProName());
				}
	    	})
	    	.setNeutralButton("Delete", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int which) {
					//Delete db
					smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.DELETE_EXPERIENCE, selectedExperienceMeta.getProName());
					//Delete entry
					experienceMetaDB.deleteExperience(selectedExperienceMeta.getId().intValue());
					//Reload map
					presentDownloadedExperiences();
				}
			});
            setDialogFontSizeAndShow(alert, FONT_SIZE);

    	}
        if(currentPos != null)
            currentPos.showInfoWindow();
    	return true;
    }
    
    public void showNearByExperienceList(final CharSequence[] items, final boolean isOnline)
    {
    	AlertDialog.Builder alert = new AlertDialog.Builder(this);		
		//alert.setCancelable(false);
		if(items.length > 0)
		{
			//alert.setTitle("Experiences within 5 km");
			alert.setTitle("Please select an experience");
			alert.setItems(items, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int index) {
		            // Do something with the selection
		            //Toast.makeText(getApplicationContext(), items[item], Toast.LENGTH_LONG).show();
		            markerClick(String.valueOf(nearbyExperiences.get(index)), isOnline);
		        }
		    });
		}
		else
			alert.setTitle("There are no available experiences");
			//alert.setTitle("There are no experiences within 5 km");
		alert.setNegativeButton("Close to browse on map", null);	//Do nothing on no
        setDialogFontSizeAndShow(alert, FONT_SIZE);
		//alert.show();
    }

	//////////////////////////////////////////////////////////////////////////////
	// EXPERIENCE DETAIL: DOWNLOADING FROM SERVER - 
	//////////////////////////////////////////////////////////////////////////////

    public void presentExperience()
    {
        btnResponse.setVisibility(View.VISIBLE);
    	selectedExperienceDetail.renderAllPOIs(mMap, (SMEPAppVariable) getApplicationContext());
    	addPOIMarkerListener();
    	selectedExperienceDetail.renderAllRoutes(mMap);
    	selectedExperienceDetail.renderAllEOIs();
    	selectedExperienceDetail.getMediaStatFromDB();
    	//bound for the whole experience
    	LatLngBounds bounds = selectedExperienceDetail.getGeographicalBoundary();
    	if(bounds != null)
    		mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100));//50 = margin ~ geofence
    	if(mMap.getCameraPosition().zoom > 19)// && allPOIs.size() < 3)
    		mMap.animateCamera(CameraUpdateFactory.zoomTo(19), 2000, null);
		if(dbUser != null)
		{
			keepTrackConsumerExperience();
		}
    }   
    
    public void addPOIMarkerListener()
    {
    	
    	mMap.setOnMarkerClickListener(new OnMarkerClickListener() 
        {				
			@Override
			public boolean onMarkerClick(Marker arg0) 
			{				
				if(arg0.getTitle().equalsIgnoreCase("START"))
					Toast.makeText(getApplicationContext(), "This marker indicates the starting point of the route", Toast.LENGTH_LONG).show();
				else if(arg0.getTitle().equalsIgnoreCase("END"))
					Toast.makeText(getApplicationContext(), "This marker indicates the end point of the route", Toast.LENGTH_LONG).show();
				else if(arg0.getTitle().equalsIgnoreCase("YAH"))//current location marker
                {
                    Toast.makeText(getApplicationContext(), "This circle shows your current location", Toast.LENGTH_LONG).show();
                }
                else
					pushMediaToUser(Integer.valueOf(arg0.getTitle()));
                if(currentPos != null)
                    currentPos.showInfoWindow();
            	return true;
            }
        });
    }
	
    @Override
	public void onMapClick(LatLng arg0) {
		// Click on a trigger zone 
    	if(selectedExperienceDetail != null)
    		pushMediaToUser(selectedExperienceDetail.getTriggerZoneIndexFromLocation(arg0));
	}
    
    public void pushMediaToUser(int poiID)
    {
    	try 
		{
    		//currentPOIIndex = Integer.valueOf(arg0.getTitle());    		
        	displayMediaTab(poiID, "FROM_MAP_PULL");            		
        	if(smepSettings.isSoundNotification())
        	{
			    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
			    Ringtone r = RingtoneManager.getRingtone(getApplicationContext(), notification);
			    r.play();
        	}
		} 
		catch (Exception e) {Log.e(TAG, "Error when playing sound: " + e.getMessage());}
    }

	//////////////////////////////////////////////////////////////////////////////
	// LOCATION CHANGE SERVICE
	//////////////////////////////////////////////////////////////////////////////

    /**
     * <p>This class tracks the current location of the user</p>
     **/
    private class LocationListener implements android.location.LocationListener
	{
	    Location mLastLocation;	    
	    
	    public LocationListener(String provider)
	    {
	        //Log.e(TAG, "LocationListener " + provider);
	        mLastLocation = new Location(provider);
	    }
	    @Override
	    public void onLocationChanged(Location location)
	    {
	        //Log.e(TAG, "onLocationChanged..................................: " + location);
	    	mLastLocation.set(location);		   
	    	updateSMEPWhenLocationChange(location);
	    }
	    	    
	    @Override
	    public void onProviderDisabled(String provider)
	    {
	        //Log.e(TAG, "onProviderDisabled: " + provider);            
	    }
	    @Override
	    public void onProviderEnabled(String provider)
	    {
	        //Log.e(TAG, "onProviderEnabled: " + provider);
	    }
	    @Override
	    public void onStatusChanged(String provider, int status, Bundle extras)
	    {
	        //Log.e(TAG, "onStatusChanged: " + provider);
	    }
	}	
	
	public void updateSMEPWhenLocationChange(Location location) {
		try {
			//Log.d("Mock Location:","Location changed:" + "Test mode: " + isTestMode);
			lastKnownLocation = location;
			GeomagneticField field = new GeomagneticField(
					(float) location.getLatitude(),
					(float) location.getLongitude(),
					(float) location.getAltitude(), System.currentTimeMillis());
			// getDeclination returns degrees
			mDeclination = field.getDeclination();
			LatLng curPos = new LatLng(location.getLatitude(),location.getLongitude());
			currentPos.setPosition(curPos);
            //currentPos.
			currentAccuracy.setCenter(curPos);
			currentAccuracy.setRadius(location.getAccuracy());
			initialLocation = new LatLng(location.getLatitude(),location.getLongitude());
			if(smepSettings.isShowingGPS())
				getActionBar().setTitle("GPS Accuracy: " + String.format("%.1f", location.getAccuracy()) + " (m)");

            if (smepSettings.isYAHCentred()) {
				mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentPos.getPosition(), mMap.getCameraPosition().zoom));
			}
			
			SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) getApplicationContext();
			if(mySMEPAppVariable.isNewMedia())
			{
				mySMEPAppVariable.setNewMedia(false);
				currentPOIIndex = mySMEPAppVariable.getNewMediaIndex();
				displayMediaTab(currentPOIIndex, "FROM_LOCATION_SERVICE");
			}
			
			if (smepSettings.isTestMode()) {
				new MockLocationService().execute();				
			}
		} catch (Exception e) {
            e.printStackTrace();
		}
	}
	
	private void setUpLocationService()	{
		LocationManager mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
		try	{
	        mLocationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, mLocationListeners[1]);
	    } 
		catch (java.lang.SecurityException ex) {
	        Log.i(TAG, "fail to request location update, ignore", ex);
	    } 
		catch (IllegalArgumentException ex) {
	        Log.d(TAG, "network provider does not exist, " + ex.getMessage());
	    }
	    try {
	        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE, mLocationListeners[0]);
	    } 
		catch (java.lang.SecurityException ex) {
	        Log.i(TAG, "fail to request location update, ignore", ex);
	    } catch (IllegalArgumentException ex) {
	        Log.d(TAG, "gps provider does not exist " + ex.getMessage());
	    }
	}

    /**
     * <p>This class is another background thread which simulates the fake current location</p>
     **/
    class MockLocationService extends AsyncTask<String, String, String>
    { 
        //Before starting background thread Show Progress Dialog        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();            
        }
 
        protected String doInBackground(String... args) 
        {        	             
            try 
            {
            	JSONParser jParser = new JSONParser();
            	// Building Parameters
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                params.add(new BasicNameValuePair("locationID",testingCode));
                // getting JSON string from URL
                JSONObject json = jParser.makeHttpRequest(SharcLibrary.url_mockLocation, "POST", params);
     
                // Check your log cat for JSON reponse
                Log.d("Mock Location:", json.toString());
            	// Checking for SUCCESS TAG
                int success = json.getInt("success"); 
                if (success == 1) 
                {
                	JSONArray mockLocations = json.getJSONArray("location");
                    // looping through All files
                    for (int i = 0; i < mockLocations.length(); i++) 
                    {
                        JSONObject c = mockLocations.getJSONObject(i);
                        double lat = c.getDouble("lat");
                        double lng = c.getDouble("lng");
                        SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) getApplicationContext();
                        Location mockLoc = new Location("");//provider name is necessary
                        mockLoc.setLatitude(lat);//your coords of course
                        mockLoc.setLongitude(lng);
        		    	mySMEPAppVariable.setMockLocation(mockLoc);
        		    	System.out.println("Lat x Lng:" + lat + " x " + lng);
                    }
                } 
                else 
                {
                    // no file found
                    Toast.makeText(getApplicationContext(), "No mock location found",Toast.LENGTH_LONG).show();                  
                }
            } 
            catch (JSONException e) 
            {
                e.printStackTrace();
            } 
            return null;
        }
 
        //After completing background task Dismiss the progress dialog         
        protected void onPostExecute(String file_url) 
        {
            // updating UI from Background Thread
            runOnUiThread(new Runnable() 
            {
                public void run() 
                {
                	//Updating parsed JSON data into ListView      
                	SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) getApplicationContext();
                	currentPosition.setPosition(new LatLng(mySMEPAppVariable.getMockLocation().getLatitude(), mySMEPAppVariable.getMockLocation().getLongitude()));
                }
            }); 
        } 
    }

	public void gotoCurrentLocation(View v)
	{
		moveAndZoomToLocation(initialLocation, 16);
	}
	
    public void moveAndZoomToLocation(LatLng location, int zoomLevel)
    {
		if(location == null)
        {
            Toast.makeText(this, R.string.message_no_gps, Toast.LENGTH_LONG).show();
        }
        else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(location, zoomLevel - 1));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(zoomLevel), 2000, null);
        }
    }
    
	//////////////////////////////////////////////////////////////////////////////
	// DIRECTION SENSOR
	//////////////////////////////////////////////////////////////////////////////
    private SensorEventListener mySensorEventListener = new SensorEventListener(){
	    @Override
	    public void onAccuracyChanged(Sensor sensor, int accuracy) {
	    }

		@Override
		public void onSensorChanged(SensorEvent event) {
		    // angle between the magnetic north direction
			float bearing = (float) (event.values[0] + mDeclination);
			currentPos.setRotation(bearing - mMap.getCameraPosition().bearing);	        
	        if(smepSettings.isRotating()) 
	        	updateCamera(bearing);		   
		}
	};

	private void updateCamera(float bearing) {
	    CameraPosition oldPos = mMap.getCameraPosition();
	    CameraPosition pos = CameraPosition.builder(oldPos).bearing(bearing).build();/**/
	    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(pos));
	}
   
	//////////////////////////////////////////////////////////////////////////////
	// RESPONSE
	//////////////////////////////////////////////////////////////////////////////
    protected void onActivityResult(int requestCode, int resultCode, Intent data){
    	if(requestCode == TAKE_PICTURE)
    	{
			if (resultCode == RESULT_OK) {
				String[] entity = getAssociatedEntity();
				ResponseModel res = new ResponseModel(String.valueOf((new Date()).getTime()), "Waiting", "image", "", outputFile, entity[0], entity[1], "", "NEW", "NEW");
				smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.ADD_RESPONSE_IMAGE, entity[0] + "#" + entity[1]);
				//res.setFileUri(fileUri);
				addDescription(res);
			}
		}
		else if(requestCode == CAPTURE_VIDEO) {
			if (resultCode == RESULT_OK) {
				String[] entity = getAssociatedEntity();
				ResponseModel res = new ResponseModel(String.valueOf((new Date()).getTime()), "Waiting", "video", "", outputFile, entity[0], entity[1], "", "NEW", "NEW");
				smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.ADD_RESPONSE_VIDEO, entity[0] + "#" + entity[1]);
				addDescription(res);
			}
	    }		
 	   	else if (requestCode == REQUEST_LINK_TO_DBX) 
 	   	{
 	   		if (resultCode == RESULT_OK) {
 	   			if(mDbxAcctMgr.hasLinkedAccount())
		    	{
 				   	dbUser = null;
					new GetUserDetailsService().execute();
		    	}
 	   		}
 	   		else {
 			   //... Link failed or was cancelled by the user.
 	   			smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_LOGIN, "failed");
 	   			Toast.makeText(this, "Link to Dropbox failed.", Toast.LENGTH_LONG).show();
 	   		}
 	   	} 
 	   	else {
 		   super.onActivityResult(requestCode, resultCode, data);
 	   	}
 	}
	/**
	 * <p>This class is another background thread which get user details when logging into SMEP</p>
	 **/
	class GetUserDetailsService extends AsyncTask<String, String, String>
	{
		//Before starting background thread Show Progress Dialog
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(MainActivity.this);
			pDialog.setMessage("Getting user information. Please wait...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(false);
			pDialog.show();
		}

		protected String doInBackground(String... args)
		{
			try
			{
				while (dbUser == null)
				{
					if(mDbxAcctMgr.hasLinkedAccount())
					{
						dbUser = mDbxAcctMgr.getLinkedAccount().getAccountInfo();
					}
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			return null;
		}

		//After completing background task Dismiss the progress dialog
		protected void onPostExecute(String file_url)
		{
			// updating UI from Background Thread
			runOnUiThread(new Runnable()
			{
				public void run()
				{
					pDialog.dismiss();
					if(dbUser != null ) {
						smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_LOGIN, mDbxAcctMgr.getLinkedAccount().getUserId());
					}
					displayDropboxUser(dbUser);
				}
			});
		}
	}

    public void addResponse(View v)
	{
		if(currentTab != 4)
    	{
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle("Add a response");
			//alert.setCancelable(false);
			LayoutInflater factory = LayoutInflater.from(this);
			final View textEntryView = factory.inflate(R.layout.response_select_type_dialog, null);
			sessionLocation = initialLocation;//start a new session -> all responses will have the same LatLng
			alert.setView(textEntryView);
			alert.setNegativeButton("Close", null);	//Do nothing on no
            setDialogFontSizeAndShow(alert, FONT_SIZE);
			//alert.show();
			smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.OPEN_RESPONSE_DIALOG, String.valueOf(currentTab));
    	}
		else
		{			
			new UploadAllToDropboxThread().execute();
			/*File mPath = new File("/data/data/uk.lancs.sharc/databases");
		    if(mPath.exists()) 
		    {
		    	File[] fList = mPath.listFiles();
		    	for (File file : fList) {
		    	    if (file.isFile()) {
		    	        System.out.println(file.getAbsolutePath());
		    	    }
		    	}
		    }*/
		}
	}
    
    public void addTextResponse(View v)
    {
    	AlertDialog.Builder alert = new AlertDialog.Builder(this);

		alert.setTitle("Add text");
		//alert.setCancelable(false);
		LayoutInflater factory = LayoutInflater.from(this);
		final View textEntryView = factory.inflate(R.layout.response_add_text_dialog, null);
		alert.setView(textEntryView);
		
		alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                EditText content = (EditText) textEntryView.findViewById(R.id.editTextMediaContentD);
                EditText title = (EditText) textEntryView.findViewById(R.id.editTextTitleD);
                String id = String.valueOf((new Date()).getTime());
                String[] entity = getAssociatedEntity();
                ResponseModel res = new ResponseModel(id, "Waiting", "text", title.getText().toString(), content.getText().toString(), entity[0], entity[1], "", "NEW", "NEW");
                selectedExperienceDetail.addMyResponse(res);
                showResponseDone(res);
                smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.ADD_RESPONSE_TEXT, entity[0] + "#" + entity[1]);
            }
        });

		alert.setNegativeButton("Cancel", null);
        setDialogFontSizeAndShow(alert, FONT_SIZE);
		//alert.show();
    }
    
    public String[] getAssociatedEntity()
    {
    	String mEntityType = "";
		String mEntityID = "";
		switch (currentTab)
		{
			case 0:
				mEntityType = "NEW";
				mEntityID = sessionLocation.latitude + " " + sessionLocation.longitude;
				break;
			case 1:
				mEntityType = "POI";
				mEntityID = selectedExperienceDetail.getPOIID(currentPOIIndex);
				break;
			case 2:
				mEntityType = "EOI";
				mEntityID = "";
				break;
			case 3:
				mEntityType = "ROUTE";
				mEntityID = "";
				break;
			default:
				break;
		}
		return new String[]{mEntityType, mEntityID};
    }

    public void addPhotoResponse(View v)
    {
    	Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);		 
        fileUri = getOutputMediaFileUri(TAKE_PICTURE); 
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); 
        // start the image capture Intent
        startActivityForResult(intent, TAKE_PICTURE);
    }
    
    public Uri getOutputMediaFileUri(int type) 
    {
        // External sdcard location
        File mediaStorageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM).getAbsoluteFile();
        //Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.getDefault()).format(new Date());
        File mediaFile;
        if (type == TAKE_PICTURE)
        {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "Camera" + File.separator + "IMG_" + timeStamp + ".jpg");
        }
        else if (type == CAPTURE_VIDEO)
        {
            mediaFile = new File(mediaStorageDir.getPath() + File.separator + "Camera" + File.separator + "VID_" + timeStamp + ".mp4");
        }
        else
        {
            return null;
        }
        outputFile = mediaFile.getAbsolutePath();
        return Uri.fromFile(mediaFile);
	}
	 
	public void addDescription(final ResponseModel curRes)
	{
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Please enter title for the media (optional)");
		//alert.setCancelable(false);
		// Set an EditText view to get user input 
		final EditText input = new EditText(this);
		alert.setView(input);
		alert.setPositiveButton("Add", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
			curRes.setDesc(input.getText().toString());
			selectedExperienceDetail.addMyResponse(curRes);
			smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.ADD_RESPONSE_DESC, curRes.getDesc());
			showResponseDone(curRes);
		  }
		});

		alert.setNegativeButton("Skip", new DialogInterface.OnClickListener() {
		  public void onClick(DialogInterface dialog, int whichButton) {
		    // Canceled.
			  curRes.setDesc("");
			  selectedExperienceDetail.addMyResponse(curRes);
			  showResponseDone(curRes);
		  }
		});
        setDialogFontSizeAndShow(alert, FONT_SIZE);
		//alert.show();
		// see http://androidsnippets.com/prompt-user-input-with-an-alertdialog
	}
    
    public void addAudioResponse(View v)
    {
    	AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Record audio");
		//alert.setCancelable(false);
		LayoutInflater factory = LayoutInflater.from(this);
		final View textEntryView = factory.inflate(R.layout.response_add_audio_dialog, null);
		alert.setView(textEntryView);
		timerValue = (TextView)textEntryView.findViewById(R.id.viewRecording);
		timerValue.setText("00:00:00");
		Button record = (Button)textEntryView.findViewById(R.id.btnAudioRecording);
		record.setText("Start recording");
		outputFile = null;
		alert.setPositiveButton("Save", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				if (outputFile == null) {
					Toast.makeText(MainActivity.this, "Please tap the 'Start recording' button to record an audio clip first", Toast.LENGTH_LONG).show();
					addAudioResponse(null);
				} else {
					stopRecording();
					String id = outputFile.substring(outputFile.lastIndexOf(File.separator) + 1, outputFile.lastIndexOf("."));
					String[] entity = getAssociatedEntity();
					ResponseModel res = new ResponseModel(id, "Waiting", "audio", "", outputFile, entity[0], entity[1], "", "NEW", "NEW");
					smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.ADD_RESPONSE_AUDIO, entity[0] + "#" + entity[1]);
				addDescription(res);
				}
			}
		});

		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
				stopRecording();
			}
		});
		setDialogFontSizeAndShow(alert, FONT_SIZE);
		//alert.show();
    }
   
    public void recordAudio (View v)
    {
    	Button record = (Button)v;//findViewById(R.id.btnAudioRecording);
    	if(record.getText().toString().contains("Start"))
    	{
    		startTime = SystemClock.uptimeMillis();
    		customHandler.postDelayed(updateTimerThread, 0);

    		String id = String.valueOf((new Date()).getTime());
    		outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + id + ".mp3";
    		myAudioRecorder = new MediaRecorder();
            myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);            
    		myAudioRecorder.setOutputFile(outputFile);
    		try {
	   	         myAudioRecorder.prepare();
	   	         myAudioRecorder.start();	   	    
	   	    } catch (Exception e) {
	   	         e.printStackTrace();
	   	    }
    		record.setText("STOP");
			record.setBackgroundColor(Color.argb(255, 60, 190, 255));
    	}
    	else if(record.getText().toString().contains("STOP"))
    	{
    		//timeSwapBuff += timeInMilliseconds;
    		customHandler.removeCallbacks(updateTimerThread);

    		try {
    			myAudioRecorder.stop();
    		}
    		catch(Exception e)
    		{
    			 e.printStackTrace();
    		}
    		finally
    		{
    			myAudioRecorder.release();
                myAudioRecorder  = null;
    			record.setText("Start recording");
                record.setBackgroundColor(Color.argb(255, 173, 13, 6));
    		}
    	}
    }
    
    private void stopRecording()
    {
    	if(myAudioRecorder!=null)
    	{
			customHandler.removeCallbacks(updateTimerThread);
	
			try {
				myAudioRecorder.stop();
			}
			catch(Exception e)
			{
				 e.printStackTrace();
			}
			finally
			{
				myAudioRecorder.release();
				myAudioRecorder  = null;
			}
    	}
    }
    
    private Runnable updateTimerThread = new Runnable() {
		public void run() {

			timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
			//updatedTime = timeSwapBuff + timeInMilliseconds;
			updatedTime =  timeInMilliseconds;

			int secs = (int) (updatedTime / 1000);
			int mins = secs / 60;
			secs = secs % 60;
			int milliseconds = (int) (updatedTime % 1000);
			timerValue.setText("" + String.format("%02d", mins) + ":"
					+ String.format("%02d", secs) + ":"
					+ String.format("%03d", milliseconds));
			customHandler.postDelayed(this, 0);
		}
	};
    public void addVideoResponse(View v)
    {
    	Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);		 
        fileUri = getOutputMediaFileUri(CAPTURE_VIDEO); 
        // set video quality
        intent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); 
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fileUri); // set the image file name 
        // start the video capture Intent
        startActivityForResult(intent, CAPTURE_VIDEO);
    }
    
    public void showResponseDone(ResponseModel res)
    {
		if(res.getEntityType().equalsIgnoreCase("NEW"))
		{
			Toast.makeText(this, "Your response for this location has been submitted and will be added as a new POI (upon moderation). When you have Wi-Fi/Data, please go to the RESPONSE tab to upload it", Toast.LENGTH_LONG).show();
		}
		else
			Toast.makeText(this, "Your response has been submitted. When you have Wi-Fi/Data, please go to the RESPONSE tab to upload it", Toast.LENGTH_LONG).show();
    }

    public void uploadResponse(int index)
    {    	
    	if(dbUser == null)
    	{
    		Toast.makeText(this, getString(R.string.message_dropboxConnection), Toast.LENGTH_LONG).show();
    		return;
    	}
    	else if(!SharcLibrary.isNetworkAvailable(this))
    	{
    		Toast.makeText(this, getString(R.string.message_wifiConnection), Toast.LENGTH_LONG).show();
    		return;
    	}  
    	smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_UPLOAD_RESPONSE, index + "#" + selectedExperienceDetail.getMyResponsePresentationName(index));
    	new UploadToDropboxThread().execute(String.valueOf(index));
    }
    
    public void viewResponse(final int index)
    {    	    	
    	AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("View a response");
		//alert.setCancelable(false);
		LayoutInflater factory = LayoutInflater.from(this);
		final View textEntryView = factory.inflate(R.layout.response_preview_dialog, null);
		alert.setView(textEntryView);		
		alert.setNegativeButton("Close", null);	//Do nothing on no
		//alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
    	//    public void onClick(DialogInterface dialog, int which) {			      	
    	    	String responseContent = selectedExperienceDetail.getMyResponseContentAt(index);
    	    	WebView wv = (WebView) textEntryView.findViewById(R.id.webViewContent);
    			String base = "file://" + SharcLibrary.SHARC_MEDIA_FOLDER + "/";
    			wv.loadDataWithBaseURL(base, responseContent, "text/html", "utf-8",null);
    	//    }
    	//});
        setDialogFontSizeAndShow(alert, FONT_SIZE);
        //alert.show();
		smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_VIEW_RESPONSE, index + "#" + selectedExperienceDetail.getMyResponsePresentationName(index));
    }
    
    public void deleteResponse(final int index)
    {
		AlertDialog.Builder alert = new AlertDialog.Builder(this);
		alert.setTitle("Delete a response");
		//alert.setCancelable(false);
		alert.setMessage("Are you sure you want to delete this response?");
		alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                selectedExperienceDetail.deleteMyResponseAt(index);
                smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.SELECT_UPLOAD_RESPONSE, index + "#" + selectedExperienceDetail.getMyResponsePresentationName(index));
                displayResponseTab();
            }
        });
		alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                stopRecording();
            }
        });
        setDialogFontSizeAndShow(alert, FONT_SIZE);
        //alert.show();
    }
	//////////////////////////////////////////////////////////////////////////////
	// RESPONSE - DROPBOX
	//////////////////////////////////////////////////////////////////////////////
    public void checkDropboxLogin()
    {
    	mDbxAcctMgr = DbxAccountManager.getInstance(getApplicationContext(), APP_KEY, APP_SECRET);
    	if(mDbxAcctMgr.hasLinkedAccount())
    	{
    		dbUser = mDbxAcctMgr.getLinkedAccount().getAccountInfo();
    	}
    	displayDropboxUser(dbUser);    	    	
    }
    
    public void displayDropboxUser(DbxAccountInfo dbUser)
    {    	
    	TextView txtUsername = (TextView) findViewById(R.id.txtUsername);
    	TextView txtUseremail = (TextView) findViewById(R.id.txtUseremail);

		if(dbUser!=null)
		{
			//Log.d(TAG, dbUser.toString());
			try {
				JSONObject jsonUser = new JSONObject(dbUser.toString());
				JSONObject jsonUserInfo = new JSONObject(jsonUser.getString("rawJson"));
				txtUseremail.setText(jsonUserInfo.getString("email"));
				txtUsername.setText(dbUser.displayName);
				//track which users consume which experiences
				if(selectedExperienceDetail != null)
				{
					keepTrackConsumerExperience();
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		else
		{
			//Prompt to login
			txtUsername.setText("Log in to submit responses");
			txtUseremail.setText("(Dropbox account is required)");
		}
    }

	public void keepTrackConsumerExperience()
	{
		//Only do when internet is available
		if(SharcLibrary.isNetworkAvailable(this))
			new UpdateConsumersExperiencesThread().execute();
	}

	/*
		This inner class helps
			- Submit info about which users consume which experiences
		Note this class needs retrieve information from server so it has to run in background
	*/
	class UpdateConsumersExperiencesThread extends AsyncTask<String, String, String>
	{
		//Before starting the background thread -> Show Progress Dialog
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(MainActivity.this);
			pDialog.setMessage("Tracking consumer vs. experiences. Please wait...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(false);
			pDialog.show();
		}

		//Update designer and experience info
		protected String doInBackground(String... args)
		{
			try
			{
				JSONParser jParser = new JSONParser();
				List<NameValuePair> params = new ArrayList<NameValuePair>();
				//User info
				params.add(new BasicNameValuePair("userID", mDbxAcctMgr.getLinkedAccount().getUserId()));
				params.add(new BasicNameValuePair("projectID",selectedExperienceDetail.getMetaData().getId().toString()));
				//update MySQL data
				JSONObject json = jParser.makeHttpRequest(SharcLibrary.url_updateConsumerExperience, "POST", params);
				selectedExperienceDetail.setIsUpdatedConsumerExperience(true);
				//smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.VIEW_ONLINE_EXPERIENCES, logData);
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			return null;
		}
		//After completing background task ->Dismiss the progress dialog
		protected void onPostExecute(String file_url)
		{
			// dismiss the dialog after getting all files
			pDialog.dismiss();
			// updating UI from Background Thread
			runOnUiThread(new Runnable()
			{
				public void run()
				{
					//
				}
			});
		}
	}

    //This inner class (thread) enable uploading a media file and get public URL
    class UploadToDropboxThread extends AsyncTask<String, String, String>
    {
        private boolean isError = false;
        //Before starting background thread Show Progress Dialog
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(MainActivity.this);
            pDialog.setMessage("Uploading response(s). Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }
 
        protected String doInBackground(String... args)
        {
			DbxDatastore mDatastore = null;
			try {
				//Connect to datastore
				DbxDatastoreManager mDatastoreManager = DbxDatastoreManager.forAccount(mDbxAcctMgr.getLinkedAccount());
				//mDatastore  = mDatastoreManager.openDatastore(selectedExperienceDetail.getMetaData().getProPath());
				DbxTable responseTable;
				responseTable = mDatastore.getTable("Responses");
        		//Get the index of the response
				int index = Integer.parseInt(args[0]);
        		ResponseModel response = selectedExperienceDetail.getMyResponseAt(index);
				uploadOneResponse(response, responseTable);
				mDatastore.sync();
				selectedExperienceDetail.deleteMyResponseAt(index);
				//displayResponseTab();
            } 
            catch (Exception e) 
            {
				e.printStackTrace();
                isError = true;
            }
			finally
			{
				mDatastore.close();
			}
            return null;
        }
 
        //After completing background task Dismiss the progress dialog         
        protected void onPostExecute(String file_url) 
        {
            // dismiss the dialog after getting all files
            pDialog.dismiss();
            // updating UI from Background Thread
            runOnUiThread(new Runnable() 
            {
                public void run() 
                {
                	displayResponseTab();//To view new list after delete the submitted response
                    if(isError)
                        Toast.makeText(MainActivity.this, getString(R.string.message_upload_error), Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(MainActivity.this,"The response has been uploaded successfully and is waiting for the designer to approve", Toast.LENGTH_LONG).show();
                }
            }); 
        }    
    }

	//This inner class (thread) enable uploading all responses
	class UploadAllToDropboxThread extends AsyncTask<String, String, String>
	{
		//Before starting background thread Show Progress Dialog
        private boolean isError = false;
		@Override
		protected void onPreExecute() {
			super.onPreExecute();
			pDialog = new ProgressDialog(MainActivity.this);
			pDialog.setMessage("Uploading all response(s). Please wait...");
			pDialog.setIndeterminate(false);
			pDialog.setCancelable(false);
			pDialog.show();
		}

		protected String doInBackground(String... args)
		{
			DbxDatastore mDatastore = null;
			try {
				//Connect to datastore
				DbxDatastoreManager mDatastoreManager = DbxDatastoreManager.forAccount(mDbxAcctMgr.getLinkedAccount());
				//mDatastore  = mDatastoreManager.openDatastore(selectedExperienceDetail.getMetaData().getProPath());
				DbxTable responseTable;
				responseTable = mDatastore.getTable("Responses");
				//Get the index of the response
				for(int index = 0; index < selectedExperienceDetail.getMyResponses().size(); index++) {
					ResponseModel response = selectedExperienceDetail.getMyResponseAt(index);
					uploadOneResponse(response, responseTable);
					mDatastore.sync();
					selectedExperienceDetail.deleteMyResponseAt(index);
				}
				//displayResponseTab();
			}
			catch (Exception e)
			{
				e.printStackTrace();
                isError = true;
			}
			finally
			{
				mDatastore.close();
			}
			return null;
		}

		//After completing background task Dismiss the progress dialog
		protected void onPostExecute(String file_url)
		{
			// dismiss the dialog after getting all files
			pDialog.dismiss();
			// updating UI from Background Thread
			runOnUiThread(new Runnable()
			{
				public void run()
				{
					displayResponseTab();//To view new list after delete the submitted response
                    if(isError)
                        Toast.makeText(MainActivity.this, getString(R.string.message_upload_error), Toast.LENGTH_LONG).show();
                    else
                        Toast.makeText(MainActivity.this,"All responses have been uploaded successfully and is waiting for the designer to approve", Toast.LENGTH_LONG).show();
				}
			});
		}
	}

	public void uploadOneResponse(ResponseModel response, DbxTable responseTable) throws Exception {
		String[] ret = new String[]{"0",""};
		String publicURL = "";
		if (response.getType().equalsIgnoreCase("text"))
			ret[0] = String.valueOf(response.getContent().length());
		else if (response.getType().equalsIgnoreCase("image"))
		{
			//Get file and upload
			ret = uploadAndShareFile(response.getId().concat(".jpg"), response.getFileUri(), true);
			publicURL = ret[1];
			//Get direct link
			publicURL = publicURL.replace("https://www.drop","https://dl.drop");
			publicURL = publicURL.substring(0,publicURL.indexOf("?"));
			response.setContent(publicURL);
		}
		else if(response.getType().equalsIgnoreCase("video"))
		{
			//Get file and upload
			ret = uploadAndShareFile(response.getId().concat(".mp4"), response.getFileUri(), false);
			publicURL = ret[1];
			//Get direct link
			publicURL = publicURL.replace("https://www.drop","https://dl.drop");
			publicURL = publicURL.substring(0,publicURL.indexOf("?"));
			response.setContent(publicURL);
		}
		else if(response.getType().equalsIgnoreCase("audio"))
		{
			//Get file and upload
			ret = uploadAndShareFile(response.getId().concat(".mp3"), response.getFileUri(), false);
			publicURL = ret[1];
			//Get direct link
			publicURL = publicURL.replace("https://www.drop","https://dl.drop");
			publicURL = publicURL.substring(0,publicURL.indexOf("?"));
			response.setContent(publicURL);
		}
		//Prepare info for a new response
		String consumerName = dbUser.displayName;
		response.setConName(consumerName);
		TextView txtUseremail = (TextView) findViewById(R.id.txtUseremail);
		String consumerEmail = txtUseremail.getText().toString();
		response.setConEmail(consumerEmail);

		//Insert a new row for the new response to the Responses table
		DbxRecord responseMedia = responseTable.getOrInsert(response.getId());
		responseMedia.set("type",response.getType()).set("content", response.getContent()).set("entityType", response.getEntityType()).set("entityID", response.getEntityID())
				.set("desc", response.getDesc()).set("noOfLike", 0).set("consumerName", response.getConName()).set("consumerEmail", response.getConEmail()).set("status", response.getStatus()).set("size",ret[0]);

		//Send email
		// Building Parameters
		JSONParser jParser = new JSONParser();
		List<NameValuePair> params = new ArrayList<NameValuePair>();
		// getting JSON string from URL
		//params.add(new BasicNameValuePair("authEmail",selectedExperienceDetail.getMetaData().getProAuthEmail()));
		params.add(new BasicNameValuePair("authName",selectedExperienceDetail.getMetaData().getProAuthName()));
		params.add(new BasicNameValuePair("proName",selectedExperienceDetail.getMetaData().getProName()));
		params.add(new BasicNameValuePair("conName",dbUser.displayName));
		params.add(new BasicNameValuePair("conEmail",consumerEmail));
		params.add(new BasicNameValuePair("proAuthID",mDbxAcctMgr.getLinkedAccount().getUserId()));
		// getting JSON string from URL
		JSONObject json = jParser.makeHttpRequest(SharcLibrary.url_emailDesigner, "POST", params);
	}

	public String[] uploadAndShareFile(String fName, Uri fileUri, boolean isImage) throws Exception {
		//Dropbox file
		DbxFile mFile = null;
		FileOutputStream out = null;
		FileInputStream in = null;
		try {

			DbxPath path = new DbxPath("/" + fName);
			DbxFileSystem dbxFs = DbxFileSystem.forAccount(mDbxAcctMgr.getLinkedAccount());

			if(dbxFs.isFile(path))
				mFile = dbxFs.open(path);
			else
				mFile = dbxFs.create(path);
			//mFile = dbxFs.create(path);//normally try to open before creating //mFile = dbxFs.open(path);
			String fileSize = "0";
			out = mFile.getWriteStream();
			if(isImage)
			{
				//Bitmap bmp = BitmapFactory.decodeFile(fileUri.getPath());
				Bitmap bmp = MediaStore.Images.Media.getBitmap(this.getContentResolver(), fileUri);
				ByteArrayOutputStream bos = new ByteArrayOutputStream();
				bmp.compress(Bitmap.CompressFormat.JPEG, 70, bos);
				FileOutputStream fos = new FileOutputStream (new File(SharcLibrary.SHARC_MEDIA_FOLDER + "/sharc.tmp"));
				bos.writeTo(fos);
				fos.flush();
				fos.close();
				fileSize = String.valueOf(bos.size());
				in = new FileInputStream(SharcLibrary.SHARC_MEDIA_FOLDER + "/sharc.tmp");
			}
			else {
				in = (FileInputStream) getContentResolver().openInputStream(fileUri);
				fileSize = String.valueOf(in.available());
			}
			SharcLibrary.copyFile(in, out);
			out.close();
			in.close();
			mFile.close();
			//Share and get public link
			return new String[]{fileSize, dbxFs.fetchShareLink(path, false).toString()};
		}
		catch (Exception e) {
			e.printStackTrace();
			try {
				out.close();
				in.close();
				mFile.close();
				throw e;
			}
			catch (Exception ex)
			{
				throw ex;
			}
		}
	}
	//////////////////////////////////////////////////////////////////////////////
	// COMMENTS - DROPBOX
	//////////////////////////////////////////////////////////////////////////////
	public void showCommentDialogForMediaItem(int mediaIndex)
	{
		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        final AlertDialog alert = dialog.create();
		//alert.setTitle("Add a comment");
        //alert.requestWindowFeature(Window.FEATURE_NO_TITLE);
		//alert.setCancelable(false);
        alert.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
		LayoutInflater factory = LayoutInflater.from(this);
		final View commentView = factory.inflate(R.layout.comment_for_media_item_dialog, null);
		//Close button
		Button closeButton = (Button)commentView.findViewById(R.id.btnClose);
		closeButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				alert.dismiss();
			}
		});
		String htmlMediaItem = selectedExperienceDetail.getPOIHtmlListItems(currentPOIIndex, initialLocation).get(mediaIndex);
		final String[] entity = getAssociatedEntity(htmlMediaItem);
		final WebView webviewMedia = (WebView)commentView.findViewById(R.id.mediaItemContent);
		//get  comments info
		htmlMediaItem += "<hr/>";
		List<ResponseModel> mediaComment = selectedExperienceDetail.getCommentsForEntity(entity[1]);
		for(int i = 0; i < mediaComment.size(); i++)
		{
			htmlMediaItem += "<div style='text-align:left;margin-left:10;font-weight:bold;'>" + mediaComment.get(i).getConName() + "</div>";
			htmlMediaItem += "<div style='text-align:left;margin-left:10;'>" + mediaComment.get(i).getContent() + "</div>";
			htmlMediaItem += "<div style='text-align:right;margin-right:10;color:gray;'>" + new Date(Long.parseLong(mediaComment.get(i).getId())) + "</div>";
		}
		final String newHTMLContent = htmlMediaItem;
		String base = "file://" + SharcLibrary.SHARC_MEDIA_FOLDER + "/";
		SharcLibrary.setupWebView(webviewMedia, MainActivity.this);
		webviewMedia.loadDataWithBaseURL(base, htmlMediaItem, "text/html", "utf-8", null);
		final Button btnPost = (Button)commentView.findViewById(R.id.btnPost);
		final EditText etComment = (EditText) commentView.findViewById(R.id.editTextComment);
        etComment.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                if (etComment.getText().toString().equalsIgnoreCase(""))
                    btnPost.setEnabled(false);
                else
                    btnPost.setEnabled(true);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {}
		});

		btnPost.setEnabled(false);
        btnPost.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {

                String id = String.valueOf((new Date()).getTime());

                ResponseModel res = new ResponseModel(id, "Waiting", "text", "", etComment.getText().toString(), entity[0], entity[1], "", "NEW", "NEW");
                selectedExperienceDetail.addMyResponse(res);
                String newContent = newHTMLContent;
                newContent += "<div style='text-align:left;margin-left:10;font-weight:bold;'>Your comment is pending moderation by the creator of this experience</div>";
                newContent += "<div style='text-align:left;margin-left:10;'>" + res.getContent() + "</div>";
                newContent += "<div style='text-align:right;margin-right:10;color:gray;'>" + new Date(Long.parseLong(res.getId())) + "</div>";
                webviewMedia.loadDataWithBaseURL("file://" + SharcLibrary.SHARC_MEDIA_FOLDER + "/", newContent, "text/html", "utf-8", null);
                webviewMedia.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        super.onPageFinished(view, url);
                        Handler lHandler = new Handler();
                        lHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                webviewMedia.scrollTo(0, 1000000000);
                            }
                        }, 200);
                    }
                });
                etComment.setText("");
                //showResponseDone();
                //smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.ADD_RESPONSE_TEXT,  entity[0] + "#" + entity[1]);
                //alert.dismiss();
            }
		});
		alert.setView(commentView);
        alert.show();
	}

	public String[] getAssociatedEntity(String htmlCode)//Media - Response
	{
		htmlCode = htmlCode.substring(htmlCode.indexOf("<span"),htmlCode.indexOf("</span>"));////000#id#1111#type#noLike#noComments#therest
		String[] params = htmlCode.split("#");
		return new String[]{params[3],params[1]};
	}
	public  void checkAndReportCrash() {
		final ErrorReporter reporter = ErrorReporter.getInstance();
		reporter.init(this);
		if (reporter.isThereAnyErrorFile()) {
			AlertDialog.Builder alert = new AlertDialog.Builder(this);
			alert.setTitle("Send Error Log");
			alert.setMessage("A previous crash was reported. Would you like to send the developer the error log to fix this issue in the future?");
			alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int whichButton) {
					reporter.sendReportEmail();
				}
			});
			alert.setNegativeButton("No", null);
			setDialogFontSizeAndShow(alert, FONT_SIZE);
		}
	}
}