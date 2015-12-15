package uk.lancs.sharc.service;

import android.app.Activity;
import android.net.Uri;

/**
 * Created by SHARC on 11/12/2015.
 */
public abstract class CloudManager {
    public static final String TYPE_DROPBOX = "Dropbox";
    public static final String TYPE_GOOGLE_DRIVE = "Google Drive";
    private Long userId;
    private String userName;
    private String userEmail;
    private String cloudType;
    private String apiKey;
    private String cloudAccountId;



    protected Activity activity;


    public CloudManager(Activity activity){
        this.activity = activity;
    }

    public abstract String[] uploadAndShareFile(String fName, Uri fileUri, String textContent, String mediaType) throws Exception;
    public abstract void login(int actionCode);
    public abstract boolean checkLoginStatus();
    public abstract void getUserDetail();
    public abstract void logout();
    public abstract boolean isCloudServiceReady();
    public abstract void setDefaultUser(String user);
    public abstract boolean isLoggedin();
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getCloudType() {
        return cloudType;
    }

    public void setCloudType(String cloudType) {
        this.cloudType = cloudType;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getCloudAccountId() {
        return cloudAccountId;
    }

    public void setCloudAccountId(String cloudAccountId) {
        this.cloudAccountId = cloudAccountId;
    }
}
