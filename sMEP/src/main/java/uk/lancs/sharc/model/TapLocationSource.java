package uk.lancs.sharc.model;

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

import uk.lancs.sharc.service.SharcLibrary;

/**
 * Created by SHARC on 08/01/2016.
 */
public class TapLocationSource extends LocationSource {
    private int selectedPoi;
    private Context context;
    public TapLocationSource(int poiIndex, Context context, int tapType){
        selectedPoi = poiIndex;
        this.context = context;
        locationSourceType = tapType;
    }

    @Override
    public Hashtable<Integer,Long> findSelectedContent(Hashtable<Integer, Long> shownLocation) {
        pushMediaForPOI(selectedPoi);
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
                Log.e("TapLocationSource", "Can't play sound: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }
}
