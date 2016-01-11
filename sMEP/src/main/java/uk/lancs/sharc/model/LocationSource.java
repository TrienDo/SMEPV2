package uk.lancs.sharc.model;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.util.Hashtable;
import java.util.List;

/**
 * Created by SHARC on 08/01/2016.
 */
public abstract class LocationSource {
    public static final int LOCATION_SOURCE_GPS = 0;
    public static final int LOCATION_SOURCE_POI_TAP = 1;
    public static final int LOCATION_SOURCE_MAP_TOUCH = 2;
    public static final int LOCATION_SOURCE_BEACON = 3;
    public static final int LOCATION_SOURCE_COMMANDER = 4;

    protected int locationSourceType;

    public abstract Hashtable<Integer,Long> findSelectedContent(Hashtable<Integer, Long> listOfContentPushedPreviously);//to deal with push media again or not when revisiting

    public int getLocationSourceType() {
        return locationSourceType;
    }
}
