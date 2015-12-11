package uk.lancs.sharc.service;

import android.app.Activity;
import android.net.Uri;

/**
 * Created by SHARC on 11/12/2015.
 */
public class GoogleDriveCloud extends CloudManager{
    public GoogleDriveCloud(Activity activity) {
        super(activity);
    }

    @Override
    public String[] uploadAndShareFile(String fName, Uri fileUri, String textContent, String mediaType) throws Exception {
        return new String[0];
    }

    @Override
    public void login(int actionCode) {

    }

    @Override
    public boolean checkLoginStatus() {
        return false;
    }

    @Override
    public void getUserDetail() {

    }

    @Override
    public void logout() {

    }

    @Override
    public boolean isLoggedin() {
        return false;
    }
}
