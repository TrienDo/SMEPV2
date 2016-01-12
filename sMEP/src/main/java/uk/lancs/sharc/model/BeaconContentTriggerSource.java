package uk.lancs.sharc.model;

import android.app.Activity;
import android.content.Context;
import android.location.Location;

import java.util.Hashtable;
import java.util.List;

/**
 * Created by SHARC on 08/01/2016.
 * This class may be the same as GPS so it may not be used.
 */
public class BeaconContentTriggerSource extends ContentTriggerSource {

    public BeaconContentTriggerSource(Location currentLocation, List<POIModel> poiModelList, Context context, Activity activity, Hashtable<Integer, Long> shownLocation) {
        super(activity, context, shownLocation);
    }

    @Override
    public Hashtable<Integer, Long> findSelectedContent() {
        return null;
    }
}
