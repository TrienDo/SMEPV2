package uk.lancs.sharc.model;

import java.util.Hashtable;

/**
 * Created by SHARC on 08/01/2016.
 */
public class CommanderLocationSource extends LocationSource {
    private int mediaIndex;//need to get this info from data sent by Commander
    public CommanderLocationSource(){

    }
    @Override
    public Hashtable<Integer, Long> findSelectedContent(Hashtable<Integer, Long> listOfContentPushedPreviously) {
        return null;
    }

    public int getMediaIndex(){
        return mediaIndex;
    }
}
