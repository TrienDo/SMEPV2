package uk.lancs.sharc.service;

import java.util.Hashtable;
import java.util.List;

import uk.lancs.sharc.controller.MainActivity;
import uk.lancs.sharc.model.ContentTriggerSource;
import uk.lancs.sharc.model.GpsContentTriggerSource;
import uk.lancs.sharc.model.SMEPAppVariable;
import uk.lancs.sharc.model.POIModel;

import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

/**
 * <p>This class is a background service which enables SMEP to keep tracking the user current location
 * even when the user switch to another app.</p>
 *
 * Author: Trien Do
 * Date: Feb 2014
 */
public class BackgroundService extends Service
{
	private static final String TAG = "SMEP_SERVICE";
	private LocationManager mLocationManager = null;
	private List<POIModel> allPOIs;
    private Hashtable<Integer,Long> shownLocation;

	private class LocationListener implements android.location.LocationListener
    {
	    Location mCurrentLocation;//Store current location

	    public LocationListener(String provider)
        {
	        mCurrentLocation = new Location(provider);
	    }
	    
	    @Override
	    public void onLocationChanged(Location location)
        {
	    	SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) getApplicationContext();//Get the global settings of SMEP
	    	//Update screen
			((MainActivity)mySMEPAppVariable.getActivity()).updateSMEPWhenLocationChange(location);
			if(mySMEPAppVariable.isNewExperience())
            {
	    		allPOIs = mySMEPAppVariable.getAllPOIs();
	    		shownLocation.clear();
	    		mySMEPAppVariable.setNewExperience(false);
	    	}

            if(mySMEPAppVariable.isResetPOI())
		    {
		    	//Clear array of pushed media
		    	if(!shownLocation.isEmpty())
		    		shownLocation.clear();
		    	mySMEPAppVariable.setResetPOI(false);
		    }

            if(allPOIs != null)
		    {	
			    if(mySMEPAppVariable.isTestMode())
			    {
			    	mCurrentLocation.set(mySMEPAppVariable.getMockLocation());
					checkAndRenderContent(mySMEPAppVariable.getMockLocation(), mySMEPAppVariable.getActivity());
			    }
			    else
			    {
			    	if(location.getAccuracy() <= 100)//Only use GPS with high accuracy as GPS may jump
					{
						mCurrentLocation.set(location);
						checkAndRenderContent(location, mySMEPAppVariable.getActivity());
					}
			    }
		    }
	    }

		private void checkAndRenderContent(Location location, Activity activity){
			ContentTriggerSource contentTriggerSource = new GpsContentTriggerSource(location, allPOIs, getApplicationContext(),activity, shownLocation);
			shownLocation = contentTriggerSource.renderContent();
		}

	    @Override
	    public void onProviderDisabled(String provider)
	    {
	        Log.e(TAG, "onProviderDisabled: " + provider);            
	    }
	    @Override
	    public void onProviderEnabled(String provider)
	    {
	        Log.e(TAG, "onProviderEnabled: " + provider);
	    }
	    @Override
	    public void onStatusChanged(String provider, int status, Bundle extras)
	    {
	        Log.e(TAG, "onStatusChanged: " + provider);
	    }
	}
	
	LocationListener[] mLocationListeners = new LocationListener[] {
	        new LocationListener(LocationManager.GPS_PROVIDER),
	        new LocationListener(LocationManager.NETWORK_PROVIDER)
	};
	
	@Override
	public IBinder onBind(Intent arg0){
        return null;
	}
	@Override
	public int onStartCommand(Intent intent, int flags, int startId){
	    Log.e(TAG, "onStartCommand");
	    allPOIs = null;
	    shownLocation = new Hashtable<Integer, Long>();
	    super.onStartCommand(intent, flags, startId);       
	    return START_STICKY;
	}
	@Override
	public void onCreate(){
	    Log.e(TAG, "onCreate");
	    initializeLocationManager();
        final int LOCATION_INTERVAL = 0;
        final float LOCATION_DISTANCE = 0f;
        try {
	        mLocationManager.requestLocationUpdates(
	                LocationManager.NETWORK_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
	                mLocationListeners[1]);
	    } 
		catch (java.lang.SecurityException ex) {
	        Log.i(TAG, "Fail to request location update, ignore", ex);
	    } catch (IllegalArgumentException ex) {
	        Log.d(TAG, "Network provider does not exist, " + ex.getMessage());
	    }
	    try {
	        mLocationManager.requestLocationUpdates(
	                LocationManager.GPS_PROVIDER, LOCATION_INTERVAL, LOCATION_DISTANCE,
	                mLocationListeners[0]);
	    } 
		catch (java.lang.SecurityException ex) {
	        Log.i(TAG, "Fail to request location update, ignore", ex);
	    } catch (IllegalArgumentException ex) {
	        Log.d(TAG, "Gps provider does not exist " + ex.getMessage());
	    }
	}
	@Override
	public void onDestroy()	{
	    super.onDestroy();
	    if (mLocationManager != null) {
	        for (int i = 0; i < mLocationListeners.length; i++) {
	            try {
	                mLocationManager.removeUpdates(mLocationListeners[i]);
	            } catch (Exception ex) {
	                ex.printStackTrace();
	            }
	        }
	    }
	}

	private void initializeLocationManager() {
	    if (mLocationManager == null) {
	        mLocationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
	    }
	}
}