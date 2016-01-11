package uk.lancs.sharc.model;

import android.app.Activity;
import android.content.Context;
import android.location.Location;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import uk.lancs.sharc.controller.MainActivity;
import uk.lancs.sharc.service.SharcLibrary;

/**
 * Created by SHARC on 08/01/2016.
 */
public class GpsLocationSource extends LocationSource {
    private Location currentLocation;
    private List<POIModel> allPOIs;
    private Context context;
    public GpsLocationSource(Location currentLocation, List<POIModel> poiModelList, Context context){
        this.currentLocation = currentLocation;
        this.allPOIs = poiModelList;
        this.context = context;
        locationSourceType = LOCATION_SOURCE_GPS;
    }

    @Override
    public Hashtable<Integer,Long> findSelectedContent(Hashtable<Integer, Long> shownLocation) {
        return findTriggerZone(currentLocation, shownLocation);
    }



    /**
     *This method identify whether the current location is within any trigger zone to push media to the user
     * @param L1: current location
     */
    private Hashtable<Integer,Long> findTriggerZone(Location L1, Hashtable<Integer,Long> shownLocation)
    {
        if(allPOIs.size()>0)
        {
            boolean isWithin = false;
            LatLng tmpPoint;
            for (int i = 0; i < allPOIs.size(); i++)
            {
                isWithin = false;

                if(allPOIs.get(i).getTriggerType().equalsIgnoreCase("circle"))
                {
                    float[] results = new float[1];
                    tmpPoint = allPOIs.get(i).getTriggerZoneCoordinates().get(0);
                    Location.distanceBetween(L1.getLatitude(),L1.getLongitude(), tmpPoint.latitude,tmpPoint.longitude, results);
                    if(results[0] < allPOIs.get(i).getTriggerZoneRadius())//radius of circle
                        isWithin = true;
                }
                else if(allPOIs.get(i).getTriggerType().equalsIgnoreCase("polygon"))
                {
                    List<LatLng> polyPath = allPOIs.get(i).getTriggerZoneCoordinates();
                    isWithin = SharcLibrary.isCurrentPointInsideRegion(new LatLng(L1.getLatitude(), L1.getLongitude()), polyPath);
                }

                if(isWithin)
                {
                    SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) context.getApplicationContext();
                    if(shownLocation.get(i) == null)//If the user has not visited this POI
                    {
                        shownLocation.put(i, new Date().getTime());       //push id in the hashmap to record ID and time that a POI is visited
                        pushMediaForPOI(i);
                       // ((MainActivity)activity).showNewContent();

                    }
                    else if(mySMEPAppVariable.isPushAgain())//The user has already visited this POI but wants media to be push again when revisiting
                    {
                        long period = new Date().getTime() - shownLocation.get(i);//Get milliseconds between last push and current time
                        if(period >= mySMEPAppVariable.getTimeThreshold())        //only push again if the user comes back after time threshold - converted to millisecond
                        {
                            shownLocation.put(i,new Date().getTime());
                            pushMediaForPOI(i);
                        }
                    }
                }
            }
        }
        return shownLocation;
    }

    private void pushMediaForPOI(int i)
    {
        SMEPAppVariable mySMEPAppVariable = (SMEPAppVariable) context.getApplicationContext();
        mySMEPAppVariable.setNewMedia(true);//Mark that there are new media so Main UI can render them
        mySMEPAppVariable.setNewMediaIndex(i);
        if(mySMEPAppVariable.isVibrationNotification())
        {
            Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            v.vibrate(1000);
        }
        if(mySMEPAppVariable.isSoundNotification())
        {
            try {
                Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                Ringtone r = RingtoneManager.getRingtone(context.getApplicationContext(), notification);
                r.play();
            }
            catch (Exception e) {
                Log.e("GPSLocationSource", "Can't play sound: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }
}
