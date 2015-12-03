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

import com.dropbox.sync.android.CoreHttpRequestor;
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
	private int experienceId;
	public ExperienceDatabaseManager(int experienceId) {
        this.experienceId = experienceId;
    }

	private static final String TABLE_POI  = "POIS";
	private static final String TABLE_EOI  = "EOIS";
	private static final String TABLE_ROUTE  = "ROUTES";
	private static final String TABLE_MEDIA  = "MEDIA";
	private static final String TABLE_RESPONSE  = "RESPONSES";
	private static final String TABLE_MYRESPONSE  = "MYRESPONSES";
	// Database creation sql statement
	private static final String TABLE_POI_CREATE = "create table " + TABLE_POI  + " (id varchar(15) primary key, name varchar(256), type varchar(256), desc text, latLng varchar(50), mediaOrder text, associatedEOI text, associatedRoute text, triggerZone text)";
	private static final String TABLE_EOI_CREATE = "create table " + TABLE_EOI  + " (id varchar(15) primary key, name varchar(256), desc text, startDate varchar(50), endDate varchar(50), associatedPOI text, associatedRoute text, mediaOrder text)";
	private static final String TABLE_ROUTE_CREATE = "create table " + TABLE_ROUTE  + " (id varchar(15) primary key, name varchar(256), desc text, colour varchar(10), polygon text, associatedPOI text, associatedEOI text, directed varchar(6))";//, mediaOrder text)";
	private static final String TABLE_MEDIA_CREATE = "create table " + TABLE_MEDIA  + " (id varchar(15) primary key, name varchar(256), desc text, attachedTo varchar(10), content varchar(50), context varchar(300), noOfLike int, type varchar(20), EntityID varchar(15))";
	private static final String TABLE_RESPONSE_CREATE = "create table " + TABLE_RESPONSE  +   " (id varchar(15) primary key, status varchar(10), type varchar(10), desc text, content text, entityType varchar(10), entityID varchar(15), noOfLike text, consumerName varchar(300), consumerEmail varchar(300))";
	private static final String TABLE_MYRESPONSE_CREATE = "create table " + TABLE_MYRESPONSE  +   " (id varchar(15) primary key, status varchar(10), type varchar(10), desc text, content text, entityType varchar(10), entityID varchar(15), noOfLike text, consumerName varchar(300), consumerEmail varchar(300))";


    public void parseJsonAndSaveToDB(JSONObject jsonExperience) //parse content of an experience from JSON file and download media files
    {
        //Gets all links to download media files
        String jsonString = "dgfd";

        ArrayList<String> allMediaLinks = SharcLibrary.extractLinksFromText(jsonString);
        for(int m = 0; m < allMediaLinks.size(); m++)
        {
            HttpURLConnection inConection = null;
            // Output stream
            String mName =allMediaLinks.get(m);
            mName = mName.substring(mName.lastIndexOf("/"));
            String localPath = SharcLibrary.SHARC_MEDIA_FOLDER + mName;
            File localFile = new File(localPath);
            if(!localFile.exists()) {
                try {
                    URL inUrl = new URL(allMediaLinks.get(m));
                    inConection = (HttpURLConnection) inUrl.openConnection();
                    inConection.connect();
                    // download the file
                    InputStream mInput = new BufferedInputStream(inUrl.openStream(), 8192);
                    OutputStream mOutput = new FileOutputStream(localPath);
                    System.out.println(String.valueOf(m) + " .Downloading:" + mName);
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
                    inConection.disconnect();
                    continue;
                } finally {
                    inConection.disconnect();
                }
            }
        }
        //parse entities such as POIs, EOIs
        try
        {
            JSONObject json = new JSONObject(jsonString);
            JSONArray jObject = json.getJSONArray("rows");
            for (int i = 0; i < jObject.length(); i++)
            {
                //Storing each json object in a variable
                JSONObject c = jObject.getJSONObject(i);
                String name = c.getString("tid");               //name of the table
                JSONObject tmpData = c.getJSONObject("data");
                String mediaOrder = "";
                try
                {
                    mediaOrder = tmpData.getString("mediaOrder");
                }
                catch(JSONException je){je.printStackTrace();}

                if(name.equalsIgnoreCase("POIs"))
                {
                    //this.insertPOI(c.getString("rowid"),tmpData.getString("name"), tmpData.getString("type"), tmpData.getString("desc"), tmpData.getString("latLng"), mediaOrder,tmpData.getString("associatedEOI"), tmpData.getString("associatedRoute"), tmpData.getString("triggerZone"));
                }
                else if(name.equalsIgnoreCase("EOIs"))
                {
                    //this.insertEOI(c.getString("rowid"),tmpData.getString("name"), tmpData.getString("desc"), tmpData.getString("startDate"), tmpData.getString("endDate"), tmpData.getString("associatedPOI"), tmpData.getString("associatedRoute"), mediaOrder);
                }
                else if(name.equalsIgnoreCase("Routes"))
                {
                    //this.insertROUTE(c.getString("rowid"),tmpData.getString("name"), tmpData.getString("desc"), tmpData.getString("colour"), tmpData.getString("polygon"), tmpData.getString("associatedPOI"), tmpData.getString("associatedEOI"),tmpData.getString("directed"));//, tmpData.getString("mediaOrder"));
                }
                else if(name.equalsIgnoreCase("Media"))
                {
                    //this.insertMEDIA(c.getString("rowid"),tmpData.getString("name"), tmpData.getString("type"), tmpData.getString("desc"), tmpData.getString("attachedTo"), tmpData.getString("content"), tmpData.getString("context"), 0, tmpData.getString("PoIID"));
                    //experienceDetailsDB.insertMEDIA(c.getString("rowid"),tmpData.getString("name"), tmpData.getString("type"), tmpData.getString("desc"), tmpData.getString("attachedTo"), tmpData.getString("content"), tmpData.getString("context"), tmpData.getInt("noOfLike"), tmpData.getString("PoIID"));
                }
                else if(name.equalsIgnoreCase("Responses"))
                {
                    //experienceDetailsDB.insertResponse(c.getString("rowid"), tmpData.getString("status"), tmpData.getString("type"), tmpData.getString("desc"), tmpData.getString("entityType"), tmpData.getString("content"), tmpData.getInt("noOfLike"),tmpData.getString("entityID"), tmpData.getString("consumerName"), tmpData.getString("consumerEmail"));
                    JSONObject objCount = tmpData.getJSONObject("noOfLike");
                   // this.insertResponse(c.getString("rowid"), tmpData.getString("status"), tmpData.getString("type"), tmpData.getString("desc"), tmpData.getString("entityType"), tmpData.getString("content"), objCount.getInt("I"), tmpData.getString("entityID"), tmpData.getString("consumerName"), tmpData.getString("consumerEmail"));
                }
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }

    }


    public void getMediaStat(ExperienceMetaDataModel metaData)
    {
        //Cursor statRet = this.getDataSQL("select type, count(*) as total from MEDIA group by type", null);
        //Can optimise later
        List<MediaModel> mediaList = MediaModel.find(MediaModel.class, "experienceID = ? and contentType = ?", String.valueOf(this.experienceId), MediaModel.TYPE_TEXT);
        metaData.setTextCount(mediaList.size());
        mediaList = MediaModel.find(MediaModel.class, "experienceID = ? and contentType = ?", String.valueOf(this.experienceId), MediaModel.TYPE_IMAGE);
        metaData.setImageCount(mediaList.size());
        mediaList = MediaModel.find(MediaModel.class, "experienceID = ? and contentType = ?", String.valueOf(this.experienceId), MediaModel.TYPE_AUDIO);
        metaData.setAudioCount(mediaList.size());
        mediaList = MediaModel.find(MediaModel.class, "experienceID = ? and contentType = ?", String.valueOf(this.experienceId), MediaModel.TYPE_VIDEO);
        metaData.setVideoCount(mediaList.size());
    }

    public List<MediaModel> getMediaForEntity(Long entityId, String entityType)
    {
    	return MediaModel.find(MediaModel.class, "experienceID = ? and entityId = ? and entityType = ?", String.valueOf(this.experienceId), String.valueOf(entityId) , entityType);
    }

	public List<ResponseModel> getCommentsForEntity(int entityId)
	{
		return ResponseModel.find(ResponseModel.class, "experienceID = ? and entityId = ? and entityType = 'MEDIA' and status = ?",
                                                String.valueOf(this.experienceId), String.valueOf(entityId), ResponseModel.STATUS_ACCEPTED);
	}

    public List<ResponseModel> getResponsesForEntity(Long entityId, String entityType)
    {
        return ResponseModel.find(ResponseModel.class, "experienceID = ? and entityId = ? and entityType = ? and status = ?",
                String.valueOf(this.experienceId), String.valueOf(entityId), entityType, ResponseModel.STATUS_ACCEPTED);
    }

    public List<ResponseModel> getMyResponses()
    {
        return ResponseModel.find(ResponseModel.class, "experienceID = ? and status = ?",
                String.valueOf(this.experienceId), ResponseModel.STATUS_FOR_UPLOAD);
    }

    public List<POIModel> getAllPOIs()
    {
        return POIModel.listAll(POIModel.class);
    }

    public List<EOIModel> getAllEOIs()
    {
        return EOIModel.listAll(EOIModel.class);
    }

    public List<RouteModel> getAllRoutes()
    {
        ArrayList<RouteModel> allRoutes = new ArrayList<RouteModel>();
        /*Cursor routeRet = this.getDataSQL("select * from ROUTES", null);
        if(routeRet.getCount() > 0)
        {
            routeRet.moveToFirst();
            do
            {
                ArrayList<LatLng> path = new ArrayList<LatLng>();
                if(routeRet.getString(4).trim().length() <=0)
                    continue;
                String[] pathInfo = routeRet.getString(4).split(" ");
                int k = 0;
                while (k < pathInfo.length)
                {
                    path.add(new LatLng(Float.parseFloat(pathInfo[k]), Float.parseFloat(pathInfo[k+1])));
                    k+=2;
                }
                //Calculate distance
                float distance = 0.0f;
                float[] results = new float[1];
                for (int i=1; i < path.size(); i++)
                {
                    Location.distanceBetween(path.get(i - 1).latitude, path.get(i - 1).longitude, path.get(i).latitude, path.get(i).longitude, results);
                    distance += results[0];
                }

                RouteModel tmpRoute = new RouteModel();
                tmpRoute.setDistance(distance / 1000);
                tmpRoute.setPath(path);
                tmpRoute.setName(routeRet.getString(1));
                tmpRoute.setDescription(routeRet.getString(2));
                tmpRoute.setColour(routeRet.getString(3));
                tmpRoute.setDirected(routeRet.getString(7));
                allRoutes.add(tmpRoute);
            }
            while (routeRet.moveToNext());
        }*/
        return allRoutes;
    }

	public List<ResponseModel> getResponsesForTab(String tabName)//EOI and Summary tabs
	{
		List<ResponseModel> responseList = ResponseModel.find(ResponseModel.class, "experienceID = ? and entityType = ? and status = ?",
            String.valueOf(this.experienceId), tabName, ResponseModel.STATUS_ACCEPTED);
		for (int i = 0; i < responseList.size(); i++) {
			List<ResponseModel> comments = this.getCommentsForEntity(responseList.get(i).getId().intValue());
            responseList.get(i).setNoOfComment(String.valueOf(comments.size()));
		}
		return responseList;
	}

	public String getRepresentativePhoto(String poiId)
    {
        List<MediaModel> mainMedia =  MediaModel.find(MediaModel.class, "experienceID = ? and entityId = ? and entityType = ? and mainMedia = 1",
                String.valueOf(this.experienceId), poiId, "POI");
        if(mainMedia != null && mainMedia.size() > 0)
            return mainMedia.get(0).getContent();
        else
            return "";
    }


	public String[] getEOIFromID(String eoiId)
    {
        List<EOIModel> objEoi =  EOIModel.find(EOIModel.class, "experienceID = ? and id = ?", String.valueOf(this.experienceId), eoiId);
        if(objEoi != null && objEoi.size() > 0){
            return new String[]{objEoi.get(0).getName(), objEoi.get(0).getDescription()};
    	}
    	else
    		return null;
    }


	//insert
	public void insertPOI(Long id, String name, String description, String coordinate, String triggerZone, Long designerId, Long experienceId, String typeList, String eoiList, String routeList)
	{
		POIModel objPOI = new POIModel(id, name, description, coordinate, triggerZone, designerId, experienceId, typeList, eoiList, routeList);
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
	
	public void insertResponse(String id, String status, String type, String desc, String entityType, String content, int noOfLike, String entityID, String consumerName, String consumerEmail)
	{

	}
	
	public void insertMyResponse(ResponseModel res)
	{

	}

	public void deleteMyResponse(String resID)
	{

	}

	//select
	public Cursor getDataSQL(String sql, String[] param)
	{
		// Gets the data repository in write mode
		//rawQuery("SELECT id, name FROM people WHERE name = ? AND id = ?", new String[] {"David", "2"});
		SQLiteDatabase db = this.getReadableDatabase();
		Cursor results = db.rawQuery(sql, param);
		return results;
	}
	
	public void deleteAllDataInTables()
	{
		SQLiteDatabase db = this.getWritableDatabase();
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_EOI);
		db.execSQL(TABLE_EOI_CREATE);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_ROUTE);
		db.execSQL(TABLE_ROUTE_CREATE);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_RESPONSE);
		db.execSQL(TABLE_RESPONSE_CREATE);
		db.execSQL("DROP TABLE IF EXISTS " + TABLE_MYRESPONSE);
		db.execSQL(TABLE_MYRESPONSE_CREATE);
		db.delete(TABLE_POI, null, null);
		db.delete(TABLE_EOI, null, null);
		db.delete(TABLE_ROUTE, null, null);
		db.delete(TABLE_MEDIA, null, null);
		db.delete(TABLE_MYRESPONSE, null, null);
	}
}