package uk.lancs.sharc.service;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import uk.lancs.sharc.model.EOIModel;
import uk.lancs.sharc.model.ExperienceMetaDataModel;
import uk.lancs.sharc.model.MediaModel;
import uk.lancs.sharc.model.POIModel;
import uk.lancs.sharc.model.ResponseModel;
import uk.lancs.sharc.model.RouteModel;


public class ExperienceDatabaseManager
{
	private Long experienceId;
	public ExperienceDatabaseManager() {
        experienceId = Long.valueOf(-1);
    }

    public void setSelectedExperience(Long experienceId){
        this.experienceId = experienceId;
    }

    //select all experiences to present them as markers on Google Maps
    public List<ExperienceMetaDataModel> getExperiences()
    {
        return ExperienceMetaDataModel.listAll(ExperienceMetaDataModel.class);
    }

    public void parseJsonAndSaveToDB(JSONObject jsonExperience) //parse content of an experience from JSON file and download media files
    {
        //parse entities such as POIs, EOIs
        try
        {
            //Gets all links to download media files
            JSONArray jsonEntityList;

            jsonEntityList = jsonExperience.getJSONArray("allPois");
            for(int i = 0; i < jsonEntityList.length(); i++){
                JSONObject jsonEntity = jsonEntityList.getJSONObject(i);
                JSONObject jsonEntityDesigner = jsonEntity.getJSONObject("poiDesigner");
                this.insertPOI(jsonEntity.getLong("id"),jsonEntityDesigner.getString("name"), jsonEntity.getString("description"), jsonEntityDesigner.getString("coordinate"),
                        jsonEntityDesigner.getString("triggerZone"),jsonEntityDesigner.getLong("designerId"), jsonEntity.getLong("experienceId"), jsonEntity.getString("typeList"),
                        jsonEntity.getString("eoiList"), jsonEntity.getString("routeList"), jsonEntity.getString("thumbnail"), jsonEntity.getInt("mediaCount"), jsonEntity.getInt("responseCount"));
            }

            jsonEntityList = jsonExperience.getJSONArray("allEois");
            for(int i = 0; i < jsonEntityList.length(); i++){
                JSONObject jsonEntity = jsonEntityList.getJSONObject(i);
                JSONObject jsonEntityDesigner = jsonEntity.getJSONObject("eoiDesigner");
                this.insertEOI(jsonEntity.getLong("id"), jsonEntityDesigner.getLong("designerId"), jsonEntity.getLong("experienceId"), jsonEntityDesigner.getString("name"),
                        jsonEntityDesigner.getString("description"), jsonEntity.getString("poiList"), jsonEntity.getString("routeList"));
            }

            jsonEntityList = jsonExperience.getJSONArray("allRoutes");
            for(int i = 0; i < jsonEntityList.length(); i++){
                JSONObject jsonEntity = jsonEntityList.getJSONObject(i);
                JSONObject jsonEntityDesigner = jsonEntity.getJSONObject("routeDesigner");
                this.insertROUTE(jsonEntity.getLong("id"), jsonEntityDesigner.getLong("designerId"), jsonEntity.getLong("experienceId"), jsonEntityDesigner.getString("name"),
                        jsonEntity.getString("description"), jsonEntityDesigner.getInt("directed")  == 1 ? true: false, jsonEntityDesigner.getString("colour"), jsonEntityDesigner.getString("path"),
                        jsonEntity.getString("poiList"), jsonEntity.getString("eoiList"));
            }

            jsonEntityList = jsonExperience.getJSONArray("allMedia");
            for(int i = 0; i < jsonEntityList.length(); i++){
                JSONObject jsonEntity = jsonEntityList.getJSONObject(i);
                JSONObject jsonEntityDesigner = jsonEntity.getJSONObject("mediaDesigner");

                this.insertMEDIA(jsonEntity.getLong("id"), jsonEntityDesigner.getLong("designerId"), jsonEntity.getLong("experienceId"), jsonEntityDesigner.getString("contentType"),
                        jsonEntityDesigner.getString("content"), jsonEntity.getString("context"), jsonEntityDesigner.getString("name"),
                        jsonEntity.getString("caption"), jsonEntity.getString("entityType"), jsonEntity.getLong("entityId"),
                        jsonEntityDesigner.getInt("size"), jsonEntity.getInt("mainMedia") == 1 ? true: false, jsonEntity.getInt("visible") == 1 ? true : false, jsonEntity.getInt("order"));
                //Download media
                donwloadMediaFile(jsonEntityDesigner.getString("content") );
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

    }

    public void donwloadMediaFile(String onlinePath){
        String mName = onlinePath.substring(onlinePath.lastIndexOf("/"));
        String localPath = SharcLibrary.SHARC_MEDIA_FOLDER + mName;

        HttpURLConnection inConection = null;
        File localFile = new File(localPath);
        if(!localFile.exists()) {
            try {
                URL inUrl = new URL(onlinePath);
                inConection = (HttpURLConnection) inUrl.openConnection();
                inConection.connect();
                // download the file
                InputStream mInput = new BufferedInputStream(inUrl.openStream(), 8192);
                OutputStream mOutput = new FileOutputStream(localPath);
                System.out.println(" .Downloading:" + onlinePath);
                byte mData[] = new byte[1024];
                int count;
                while ((count = mInput.read(mData)) != -1) {
                    // writing data to file
                    mOutput.write(mData, 0, count);
                }
                // flushing output
                mOutput.flush();
                mOutput.close();
                mInput.close();
                inConection.disconnect();
            } catch (IOException e) {
                e.printStackTrace();
                if(inConection != null)
                    inConection.disconnect();
            } finally {
                if(inConection != null)
                    inConection.disconnect();
            }
        }
    }
    public void getMediaStat(ExperienceMetaDataModel metaData)
    {
        //Cursor statRet = this.getDataSQL("select type, count(*) as total from MEDIA group by type", null);
        //Can optimise later
        List<MediaModel> mediaList = MediaModel.find(MediaModel.class, "experience_Id = ? and content_Type = ?", String.valueOf(this.experienceId), MediaModel.TYPE_TEXT);
        metaData.setTextCount(mediaList.size());
        mediaList = MediaModel.find(MediaModel.class, "experience_Id = ? and content_Type = ?", String.valueOf(this.experienceId), MediaModel.TYPE_IMAGE);
        metaData.setImageCount(mediaList.size());
        mediaList = MediaModel.find(MediaModel.class, "experience_Id = ? and content_Type = ?", String.valueOf(this.experienceId), MediaModel.TYPE_AUDIO);
        metaData.setAudioCount(mediaList.size());
        mediaList = MediaModel.find(MediaModel.class, "experience_Id = ? and content_Type = ?", String.valueOf(this.experienceId), MediaModel.TYPE_VIDEO);
        metaData.setVideoCount(mediaList.size());
    }

    public List<MediaModel> getMediaForEntity(Long entityId, String entityType)
    {
    	return MediaModel.find(MediaModel.class, "experience_Id = ? and entity_Id = ? and entity_Type = ?", String.valueOf(this.experienceId), String.valueOf(entityId) , entityType);
    }

	public List<ResponseModel> getCommentsForEntity(Long entityId)
	{
		return ResponseModel.find(ResponseModel.class, "experience_Id = ? and entity_Id = ? and entity_Type = 'MEDIA' and status = ?",
                String.valueOf(this.experienceId), entityId.toString(), ResponseModel.STATUS_ACCEPTED);
	}

    public List<ResponseModel> getResponsesForEntity(Long entityId, String entityType)
    {
        return ResponseModel.find(ResponseModel.class, "experience_Id = ? and entity_Id = ? and entity_Type = ? and status = ?",
                String.valueOf(this.experienceId), String.valueOf(entityId), entityType, ResponseModel.STATUS_ACCEPTED);
    }

    public List<ResponseModel> getMyResponses()
    {
        return ResponseModel.find(ResponseModel.class, "experience_Id = ? and status = ?",
                String.valueOf(this.experienceId), ResponseModel.STATUS_FOR_UPLOAD);
    }

    public List<POIModel> getAllPOIs()
    {
        return POIModel.find(POIModel.class, "experience_Id = ?", this.experienceId.toString());
    }

    public List<EOIModel> getAllEOIs()
    {
        return EOIModel.find(EOIModel.class, "experience_Id = ?", this.experienceId.toString());
    }

    public List<RouteModel> getAllRoutes()
    {
        return RouteModel.find(RouteModel.class, "experience_Id = ?", this.experienceId.toString());
    }

	public List<ResponseModel> getResponsesForTab(String tabName)//EOI and Summary tabs
	{
		List<ResponseModel> responseList = ResponseModel.find(ResponseModel.class, "experience_Id = ? and entity_Type = ? and status = ?",
            String.valueOf(this.experienceId), tabName, ResponseModel.STATUS_ACCEPTED);
		for (int i = 0; i < responseList.size(); i++) {
			List<ResponseModel> comments = this.getCommentsForEntity(responseList.get(i).getId());
            //responseList.get(i).setNoOfComment(String.valueOf(comments.size()));
		}
		return responseList;
	}

	/*
	public String getRepresentativePhoto(Long id)
    {
        List<MediaModel> mainMedia =  MediaModel.find(MediaModel.class, "experienceID = ? and entityId = ? and entityType = ? and mainMedia = 1",
                String.valueOf(this.experienceId), id.toString(), "POI");
        if(mainMedia != null && mainMedia.size() > 0)
            return mainMedia.get(0).getContent();
        else
            return "";
    }
    */


	public String[] getEOIFromID(String eoiId)
    {
        List<EOIModel> objEoi =  EOIModel.find(EOIModel.class, "experience_Id = ? and id = ?", String.valueOf(this.experienceId), eoiId);
        if(objEoi != null && objEoi.size() > 0){
            return new String[]{objEoi.get(0).getName(), objEoi.get(0).getDescription()};
    	}
    	else
    		return null;
    }


	//insert
	public void insertPOI(Long id, String name, String description, String coordinate, String triggerZone, Long designerId, Long experienceId, String typeList, String eoiList, String routeList, String thumbnailPath, int mediaCount, int responseCount)
	{
		POIModel objPOI = new POIModel(id, name, description, coordinate, triggerZone, designerId, experienceId, typeList, eoiList, routeList, thumbnailPath, mediaCount, responseCount);
		objPOI.save();
	}
	
	public void insertEOI(Long id, Long designerId, Long experienceId, String name, String description, String poiList, String routeList)
	{
		EOIModel objEOI = new EOIModel(id, designerId, experienceId, name, description, poiList, routeList);
        objEOI.save();
	}
	
	public void insertROUTE(Long id, Long designerId, Long experienceId, String name, String description,boolean directed, String colour, String path, String poiList, String eoiList)
	{
        RouteModel objRoute = new RouteModel(id, designerId, experienceId, name, description, directed, colour, path, poiList, eoiList);
        objRoute.save();
	}
	
	public void insertMEDIA(Long id, Long designerId, Long experienceId, String contentType, String content, String context, String name, String caption,
                                      String entityType, Long entityID, int size, boolean mainMedia, boolean visible, int order)
	{
        MediaModel objMedia = new MediaModel(id, designerId, experienceId, contentType, content, context, name, caption, entityType, entityID, size, mainMedia, visible, order);
        objMedia.save();
	}
	
	public void insertResponse(Long id, Long experienceId, Long userId, String contentType, String content, String description,
                               String entityType, Long entityId, String status, int size, String submittedDate)
	{
        ResponseModel objResponse = new ResponseModel(id, experienceId, userId, contentType, content, description, entityType, entityId, status, size, submittedDate);
        objResponse.save();
	}

    public void insertResponse(ResponseModel responseModel)
    {
        responseModel.save();
    }

	public void deleteMyResponse(Long resID)
	{
        ResponseModel res = ResponseModel.findById(ResponseModel.class, resID);
        if(res != null)
            res.delete();
	}

	//select
	public Cursor getDataSQL(String sql, String[] param)
	{
		// Gets the data repository in write mode
		//rawQuery("SELECT id, name FROM people WHERE name = ? AND id = ?", new String[] {"David", "2"});
		//SQLiteDatabase db = this.getReadableDatabase();
		//Cursor results = db.rawQuery(sql, param);
		//return results;
        return null;
	}
	
	public void addOrUpdateExperience(ExperienceMetaDataModel experienceMetaDataModel){
        ExperienceMetaDataModel tmp = ExperienceMetaDataModel.findById(ExperienceMetaDataModel.class, experienceMetaDataModel.getId());
        if(tmp != null) {//already there -> delete all data
            this.deleteExperience(experienceMetaDataModel.getId());
            tmp.delete();
        }
        experienceMetaDataModel.save();
    }
    public void deleteExperience(Long experienceId)
	{
		POIModel.deleteAll(POIModel.class,"experience_Id = ?", experienceId.toString());
        EOIModel.deleteAll(EOIModel.class,"experience_Id = ?", experienceId.toString());
        RouteModel.deleteAll(RouteModel.class, "experience_Id = ?", experienceId.toString());
        MediaModel.deleteAll(MediaModel.class, "experience_Id = ?", experienceId.toString());
        ResponseModel.deleteAll(ResponseModel.class, "experience_Id = ?", experienceId.toString());
        ExperienceMetaDataModel experienceMetaDataModel = ExperienceMetaDataModel.findById(ExperienceMetaDataModel.class, experienceId);
        if(experienceMetaDataModel != null)
            experienceMetaDataModel.delete();
	}
}