package uk.lancs.sharc.service;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import org.apache.http.NameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import uk.lancs.sharc.controller.MainActivity;
import uk.lancs.sharc.model.ExperienceMetaDataModel;
import uk.lancs.sharc.model.InteractionLog;
import uk.lancs.sharc.model.JSONParser;

/**
 * Created by SHARC on 11/12/2015.
 */
public class RestfulManager {
    //RESTful APIs
    public static final String api_path = "http://wraydisplay.lancs.ac.uk/SHARC20/api/v1/";
    public static final String api_get_all_published_experiences = api_path + "experiences";
    public static final String api_get_experience_snapshot = api_path + "experienceSnapshot/";
    public static final String url_mockLocation = api_path + "locations/";
    public static final String url_emailDesigner = api_path + "emailDesigner";
    public static final String url_updateConsumerExperience = api_path + "consumerExperience";

    public static final String STATUS_SUCCESS = "success";

    private Activity activity;
    public RestfulManager(Activity activity){
        this.activity = activity;
    }

    public void getPublishedExperience(){
        new GetAllOnlineExperiencesThread().execute();
    }

    public void downloadExperience(Long exprienceId){
        new ExperienceDetailsThread().execute(exprienceId.toString());
    }


    /*
        This inner class helps
            - Get information of all available online experiences
            - Present each experience as a marker on Google Maps
            - Add the Click listener even for each marker
        Note this class needs retrieve information from server so it has to run in background
    */
    class GetAllOnlineExperiencesThread extends AsyncTask<String, String, String>
    {
        //Before starting the background thread -> Show Progress Dialog
        private ProgressDialog pDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(activity);
            pDialog.setMessage("Loading available experiences. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        //Get all available experiences
        protected String doInBackground(String... args)
        {
            try
            {
                // Building Parameters
                JSONParser jParser = new JSONParser();
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                // Getting result in form of a JSON string from a Web RESTful
                JSONObject json = jParser.makeHttpRequest(RestfulManager.api_get_all_published_experiences, "GET", params);

                String ret = json.getString("status");
                if (ret.equalsIgnoreCase(RestfulManager.STATUS_SUCCESS))
                {
                    // Get Array of experiences
                    JSONArray publishedExperiences = json.getJSONArray("data");

                    // Loop through all experiences
                    ExperienceMetaDataModel tmpExperience;
                    String logData = "";
                    for (int i = 0; i < publishedExperiences.length(); i++)
                    {
                        JSONObject objExperience = publishedExperiences.getJSONObject(i);
                        // Storing each json item in variable
                        int id = objExperience.getInt("id");
                        String name = objExperience.getString("name");
                        String description = objExperience.getString("description");
                        if(description.length() > 0 && description.charAt(description.length()-1) != '.')
                            description.concat(".");
                        String createdDate = objExperience.getString("createdDate");
                        String lastPublishedDate = objExperience.getString("lastPublishedDate");
                        int designerId = objExperience.getInt("designerId");
                        boolean isPublished = true;
                        int moderationMode = objExperience.getInt("moderationMode");
                        String latLng = objExperience.getString("latLng");
                        String summary = objExperience.getString("summary");
                        String snapshotPath = objExperience.getString("snapshotPath");
                        String thumbnailPath = objExperience.getString("thumbnailPath");
                        int size = objExperience.getInt("size");
                        String theme = objExperience.getString("theme");
                        tmpExperience = new ExperienceMetaDataModel(id, name, description, createdDate, lastPublishedDate, designerId, isPublished,
                                moderationMode, latLng, summary, snapshotPath, thumbnailPath, size, theme);

                        logData += "#" + tmpExperience.getProName();
                        ((MainActivity) activity).getAllExperienceMetaData().add(tmpExperience);
                    }
                    //smepInteractionLog.addLog(initialLocation, mDbxAcctMgr, InteractionLog.VIEW_ONLINE_EXPERIENCES, logData);
                }
                else
                {
                    Toast.makeText(activity, "No experiences found", Toast.LENGTH_LONG).show();
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return null;
        }

        //After completing background task ->Dismiss the progress dialog
        protected void onPostExecute(String file_url)
        {
            // dismiss the dialog after getting all files
            pDialog.dismiss();
            // updating UI from Background Thread
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    //Updating parsed JSON data into ListView
                    ((MainActivity)activity).displayAllExperienceMetaData(true);
                    ((MainActivity)activity).addOnlineExperienceMarkerListener();
                }
            });
        }
    }

    /*
		This inner class helps
			- Download a snapshot of an experience in form of json object
			- Download all media files from Dropbox
			- Present the experience
		Note this class needs retrieve information from server so it has to run in background
	*/
    class ExperienceDetailsThread extends AsyncTask<String, String, String>
    {
        //Before starting background thread -> Show Progress Dialog
        private ProgressDialog pDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            pDialog = new ProgressDialog(activity);
            pDialog.setMessage("Loading the experience. Please wait...");
            pDialog.setIndeterminate(false);
            pDialog.setCancelable(false);
            pDialog.show();
        }

        protected String doInBackground(String... experienceId)
        {
            try{
                // Building Parameters
                JSONParser jParser = new JSONParser();
                List<NameValuePair> params = new ArrayList<NameValuePair>();
                // Getting result in form of a JSON string from a Web RESTful
                JSONObject json = jParser.makeHttpRequest(RestfulManager.api_get_experience_snapshot.concat(experienceId[0]), "GET", params);
                String ret = json.getString("status");
                if (ret.equalsIgnoreCase(RestfulManager.STATUS_SUCCESS)) {
                    ((MainActivity)activity).getSelectedExperienceDetail().getExperienceFromSnapshotOnDropbox(json.getJSONObject("data"));
                    System.out.println("Experience json:" + json.getJSONObject("data"));
                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            return null;
        }

        //After completing background task -> Dismiss the progress dialog
        protected void onPostExecute(String file_url)
        {
            // dismiss the dialog after getting all files
            pDialog.dismiss();
            // updating UI from Background Thread
            activity.runOnUiThread(new Runnable() {
                public void run() {
                    //Render the experience
                    ((MainActivity) activity).presentExperience();
                }
            });
        }
    }
}
