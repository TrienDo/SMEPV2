package uk.lancs.sharc.model;

import android.app.Activity;
import android.content.Context;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;
import android.util.Log;

import java.util.Hashtable;

/**
 * Created by SHARC on 08/01/2016.
 */
public class CommanderContentTriggerSource extends ContentTriggerSource {
    private int mediaIndex;//need to get this info from data sent by Commander
    private String commandFromCommader;
    private Context context;

    /*
    @context: application context
    @activity: MainActivity
     */
    public CommanderContentTriggerSource(String commandFromCommader, Context context, Activity activity){
        super(activity, context, null);
        //commandFromCommader = "3#2"; split
        this.context = context;
        this.commandFromCommader = commandFromCommader;
    }

    @Override
    public Hashtable<Integer, Long> findSelectedContent() {
        //This function need to pass
        String[] decodedCommand = this.commandFromCommader.split("#");//decodedCommand[0] = "3"; decodedCommand[1] = "2"
        mediaIndex = Integer.parseInt(decodedCommand[1]);
        markSelectedPoi(Integer.parseInt(decodedCommand[0]));
        return null;
    }

    public int getMediaIndex(){
        return mediaIndex;
    }
}
