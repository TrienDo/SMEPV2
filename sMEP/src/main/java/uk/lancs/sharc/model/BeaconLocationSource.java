package uk.lancs.sharc.model;

import java.util.Hashtable;

/**
 * Created by SHARC on 08/01/2016.
 */
public class BeaconLocationSource extends LocationSource {

    public BeaconLocationSource() {

    }

    @Override
    public Hashtable<Integer, Long> findSelectedContent(Hashtable<Integer, Long> listOfContentPushedPreviously) {
        return null;
    }
}
