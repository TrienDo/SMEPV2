package uk.lancs.sharc.service;

/**
 * Created by SHARC on 11/12/2015.
 */
public abstract class CloudManager {
    public static final String TYPE_DROPBOX = "Dropbox";
    public static final String TYPE_GOOGLE_DRIVE = "Google Drive";
    private String userName;
    private String userEmail;
    private String cloudType;
    private String apiKey;
    private String cloudAccountId;

    public abstract void uploadFile();
    public abstract void logIn();
}
