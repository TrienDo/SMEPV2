package uk.lancs.sharc.model;

import android.content.Context;
import android.webkit.JavascriptInterface;
import uk.lancs.sharc.controller.MainActivity;

/**
 * This class enables buttons in javascript to interact with Android native code
 * Created by SHARC on 17/06/2015.
 */

public class AndroidWebViewInterface {
    private Context mContext;

    AndroidWebViewInterface(Context c) {
        mContext = c;
    }

    @JavascriptInterface
    public void showEOIInfo(String EoiID) {
        ((MainActivity)mContext).showSelectedEOI(EoiID);
    }
}
