package uk.lancs.sharc.model;

import android.app.Activity;

import java.util.Hashtable;

import uk.lancs.sharc.controller.MainActivity;

/**
 * Created by SHARC on 08/01/2016.
 */
public class MediaContentManager {
    public static final int RENDER_MODE_POI = 1;
    public static final int RENDER_MODE_MEDIA = 2;
    LocationSource locationSource;
    Activity activity;
    Hashtable<Integer,Long> shownLocation;
    public MediaContentManager(LocationSource locationSource, Activity activity, Hashtable<Integer,Long> shownLocation){
        this.locationSource = locationSource;
        this.activity = activity;
        this.shownLocation = shownLocation;
    }

    public Hashtable<Integer,Long> renderContent(){
        shownLocation = locationSource.findSelectedContent(shownLocation);
        switch(locationSource.getLocationSourceType()){
            case LocationSource.LOCATION_SOURCE_GPS:
                ((MainActivity)activity).renderMedia(MediaContentManager.RENDER_MODE_POI,-1);//as mediaIndex is not required
                break;
            case LocationSource.LOCATION_SOURCE_POI_TAP:
                ((MainActivity)activity).renderMedia(MediaContentManager.RENDER_MODE_POI,-1);//as mediaIndex is not required
                break;
            case LocationSource.LOCATION_SOURCE_MAP_TOUCH:
                ((MainActivity)activity).renderMedia(MediaContentManager.RENDER_MODE_POI,-1);//as mediaIndex is not required
                break;
            case LocationSource.LOCATION_SOURCE_BEACON:
                ((MainActivity)activity).renderMedia(MediaContentManager.RENDER_MODE_POI,-1);//as mediaIndex is not required
                break;
            case LocationSource.LOCATION_SOURCE_COMMANDER:
                ((MainActivity)activity).renderMedia(MediaContentManager.RENDER_MODE_POI, ((CommanderLocationSource)locationSource).getMediaIndex());//as mediaIndex is REQUIRED
                break;
        }
        return shownLocation;
    }
}
