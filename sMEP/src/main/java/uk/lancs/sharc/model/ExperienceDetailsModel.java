package uk.lancs.sharc.model;

import java.util.ArrayList;
import java.util.List;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.LatLngBounds.Builder;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.location.Location;

import org.json.JSONObject;

import uk.lancs.sharc.R;
import uk.lancs.sharc.service.SharcLibrary;
import uk.lancs.sharc.service.ExperienceDatabaseManager;
/**
 * <p>This class is a model of the Locative media Experience entity</p>
 * <p>An experience may has multiple POIs, EOIs, Routes, Responses </p>
 *
 * Author: Trien Do
 * Date: Feb 2015
 **/
public class ExperienceDetailsModel {
	private List<POIModel> allPOIs;				//All POIs of the experience
	private List<EOIModel> allEOIs;				//All EOIs of the experience
	private List<RouteModel> allRoutes;			//All Routes of the experience
	private List<ResponseModel> myResponses;		//Responses submitted by the current user to the experience - not all responses
	private ExperienceMetaDataModel metaData;			//Metadata about the experience
	private List<Marker> allPoiMarkers;			//A PoI Marker shows thumbnail of the first image of a POI
	private List<Object> allTriggerZoneVizs;		//Visualization for a geo-fence of each PoIs (can be either circle or polygon)
	private Marker startRoute;							//Mark start of a route
	private Marker endRoute;							//Mark end of a route
	private boolean isUpdatedConsumerExperience;		//Mark that an user has consume this experience and submitted responses -> they can use post revisit tool later

	private ExperienceDatabaseManager experienceDatabaseManager;

	/**
	 * @param experienceDatabaseManager: help interact with database
	 * @param isFromOnline: true = loading the experience from server -> delete existing data to have the fresh data
	 */

	public ExperienceDetailsModel(ExperienceDatabaseManager experienceDatabaseManager, boolean isFromOnline)
	{
		allPOIs = new ArrayList<POIModel>();
		allEOIs = new ArrayList<EOIModel>();
		allRoutes = new ArrayList<RouteModel>();
		myResponses = new ArrayList<ResponseModel>();
		allPoiMarkers = new ArrayList<Marker>();
		allTriggerZoneVizs = new ArrayList<Object>();
		isUpdatedConsumerExperience = false;
        //From online --> delete all data of the experience if already downloaded before
        //Else just  create a new database = automatically done
        //experienceDatabaseManager = mExperienceDetailsDB;
		//if (isFromOnline)
		//	experienceDatabaseManager.deleteAllDataInTables();
		this.experienceDatabaseManager = experienceDatabaseManager;
	}

    /**
     * An experience is stored as a json file on Dropbox
     * This function parse a json file to extract info of an experience and store this info in an SQLite db
     * @param jsonExperience
     */
	public void getExperienceFromSnapshotOnCloud(JSONObject jsonExperience) //parse content of an experience from JSON file and download media files
	{
		experienceDatabaseManager.parseJsonAndSaveToDB(jsonExperience);
	}
	
	public void renderAllPOIs(GoogleMap mMap, SMEPAppVariable mySMEPAppVariable)
    {
    	allPOIs = experienceDatabaseManager.getAllPOIs();
        metaData.setPoiCount(allPOIs.size());
        mySMEPAppVariable.setAllPOIs(allPOIs);
        mySMEPAppVariable.setNewExperience(true);
    	for(int i = 0; i < allPOIs.size(); i++)
        {
    		renderPOIandTriggerZone(mMap, allPOIs.get(i), String.valueOf(i));//i = ID of marker -> when marker is tapped -> show Media of POI with that id
    	}
    }
    
	public void renderPOIandTriggerZone(GoogleMap mMap, POIModel curPOI, String markerID)
	{
		Object shape = null;		
		//Get icon for marker to present the POI
		Marker tmpMarker = mMap.addMarker(new MarkerOptions()        	
	    	.title(markerID)
	    	.anchor(0.5f, 0.5f)
			.position(curPOI.getLocation())	        
			.visible(true)
		);

		//Check if POI is polygon or polyline to render
		if(curPOI.getPoiViz().size() > 0)
		{
			mMap.addPolyline((new PolylineOptions()
					.width(5)
					.color(SharcLibrary.hex2rgb("#FF0000"))
					.visible(true)
			)).setPoints(curPOI.getPoiViz());
		}
		String firstImage = curPOI.getThumbnailPath();

		if (firstImage.equalsIgnoreCase(""))
			tmpMarker.setIcon(BitmapDescriptorFactory.fromResource(R.raw.poi));
		else
		{
			Bitmap bitmap = SharcLibrary.getThumbnail(firstImage);
			if(bitmap != null)	//First available image		      	
				tmpMarker.setIcon(BitmapDescriptorFactory.fromBitmap(bitmap));
			else
				tmpMarker.setIcon(BitmapDescriptorFactory.fromResource(R.raw.poi));
		}

		if(curPOI.getType().equalsIgnoreCase(SharcLibrary.SHARC_POI_ACCESSIBILITY))
			tmpMarker.setIcon(BitmapDescriptorFactory.fromResource(R.raw.access));
        allPoiMarkers.add(tmpMarker);

		//Draw trigger zone
		int alpha = 47; //Set alpha fill colour		
		if(curPOI.getTriggerType().equalsIgnoreCase("circle"))
		{
			shape =   mMap.addCircle(new CircleOptions()
			     .center(curPOI.getTriggerZoneCoordinates().get(0))
			     .radius(curPOI.getTriggerZoneRadius())
			     .strokeWidth(2)
			     .strokeColor(Color.parseColor(curPOI.getTriggerZoneColour()))
			     .fillColor(SharcLibrary.hex2Argb(alpha, curPOI.getTriggerZoneColour()))
							.visible(true)
			);						
		}
		else if(curPOI.getTriggerType().equals("polygon"))
		{	
			shape =   mMap.addPolygon(new PolygonOptions()
			     .addAll(curPOI.getTriggerZoneCoordinates())
			     .strokeWidth(2)
			     .strokeColor(Color.parseColor(curPOI.getTriggerZoneColour()))
			     .fillColor(SharcLibrary.hex2Argb(alpha, curPOI.getTriggerZoneColour()))
			     .visible(true)
			);						
		}								
		allTriggerZoneVizs.add(shape);		
	}

	public void renderAllEOIs()
	{
		allEOIs = experienceDatabaseManager.getAllEOIs();
		metaData.setEoiCount(allEOIs.size());
		for (int i = 0; i < allEOIs.size(); i++)
		{
			List<MediaModel> mediaList = experienceDatabaseManager.getMediaForEntity(allEOIs.get(i).getId(), "EOI");
			String content = allEOIs.get(i).getHTMLPresentation(mediaList);
			allEOIs.get(i).setMediaHTMLCode(content);
		}
	}
	
    public void renderAllRoutes(GoogleMap mMap)
    {
    	allRoutes = experienceDatabaseManager.getAllRoutes();
        metaData.setRouteCount(allRoutes.size());
		String routeInfo = "";
    	for (int i = 0; i < allRoutes.size(); i++)
    	{
			routeInfo += "<div> - Route name: " + allRoutes.get(i).getName() + " (" +   String.format("%.2f", allRoutes.get(i).getDistance()) + " km). Description: " + allRoutes.get(i).getDescription() +"</div>";
			mMap.addPolyline((new PolylineOptions()
                .width(5)
                .color(SharcLibrary.hex2rgb(allRoutes.get(i).getColour()))
                    .visible(true)
                )).setPoints(allRoutes.get(i).getPath());
            if(allRoutes.get(i).getDirected())
            {
                startRoute = mMap.addMarker(new MarkerOptions()
                                .anchor(0.5f, 1.0f)
                                .title("START")
                                .position(allRoutes.get(i).getPath().get(0))
                                .icon(BitmapDescriptorFactory.fromResource(R.raw.start))
                                .visible(true)
                );
                endRoute = mMap.addMarker(new MarkerOptions()
                                .title("END")
                                .anchor(0.5f, 0.0f)
                                .position(allRoutes.get(i).getPath().get(allRoutes.get(i).getPath().size()-1))
                                .icon(BitmapDescriptorFactory.fromResource(R.raw.end))
                                .visible(true)
                );
            }
            //metaData.setDifficultLevel(tmpRoute.getDescription());
    	}
		metaData.setRouteInfo(routeInfo);
    }
    
    public LatLngBounds getGeographicalBoundary()//Get boundary of an experience to move and zoom to suitable location on maps
    {
    	boolean empty = true;
    	Builder boundsBuilder = new LatLngBounds.Builder();
    	//All POI
    	int i;
    	for (i = 0; i < allPoiMarkers.size(); i++)
    		boundsBuilder.include(allPoiMarkers.get(i).getPosition());
    	if(i>0)
    		empty = false;
    	//All routes
    	for(int k = 0; k < allRoutes.size(); k++)
    	{
    		List<LatLng> path = allRoutes.get(k).getPath();
    		for (i = 0; i < path.size(); i++)
        		boundsBuilder.include(path.get(i));
    		if(i>0)
        		empty = false;
    	}
    	
    	if(!empty)
    		return boundsBuilder.build();
    	else
    		return null;
    }


    /**
     *
     * @param index: index of selected/pushed POI
     * @param currentLocation: help identify whether the user is physically at the selected/pushed POI
     * @return a list of html items. An item can be a media item, a response, overview info. Each html item will then be rendered in a webview later
     */
    public List<String> getPOIHtmlListItems(int index, LatLng currentLocation)//get data to display in the listview of POI Media
    {
        String state = "";
        if(currentLocation == null)
            state = SMEPAppVariable.getResource().getString(R.string.message_no_gps);
        else
        {
            if(this.isLocationWithinTrigerzone(currentLocation, index))
                state = SMEPAppVariable.getResource().getString(R.string.location_state_inside);
            else
                state = SMEPAppVariable.getResource().getString(R.string.location_state_outside);
        }
        if(index >=0 && index < allPOIs.size())
            return allPOIs.get(index).getHtmlListItems(this.experienceDatabaseManager, state);
        else
            return null;
    }

    public List<String> getAllEOIMediaListItems() //get data to display in the listview of EOI tab
    {
        ArrayList<String> mediaList = new ArrayList<String>();
        mediaList.add("<h3>This experience comprises " + allEOIs.size() + " Events of Interest </h3>");
        for (int i = 0; i < allEOIs.size(); i++)
        {
            String htmlCode = "<p><b> " + (i+1) + ". " + allEOIs.get(i).getName() + "</b></p>";
            htmlCode += "<blockquote> " + allEOIs.get(i).getDescription() + "</blockquote>";
            htmlCode += allEOIs.get(i).getMediaHTMLCode();
            mediaList.add(htmlCode);
        }
        //get responses for EOI tab
        //Get responses
        List<ResponseModel> responseList = experienceDatabaseManager.getResponsesForTab("EOI");
        for(int i = 0; i< responseList.size(); i++ )
            mediaList.add(responseList.get(i).getHTMLCodeForResponse(false));
        return mediaList;
    }

	public String[] getHTMLCodeForEOI(Long id)
	{
		for(int i = 0; i < allEOIs.size(); i++)
		{
			if(allEOIs.get(i).getId() == id)
			{
				return new String[]{allEOIs.get(i).getName(), allEOIs.get(i).getMediaHTMLCode()};
			}
		}
		return null;
	}

    public void getMediaStatFromDB()
    {    	
    	experienceDatabaseManager.getMediaStat(metaData);
    }  
    
    public List<String> getSumaryInfo()
    {
		ArrayList<String> mediaList = new ArrayList<String>();
		if(metaData!=null)
			mediaList.add(metaData.getExperienceStats());
    	else
			mediaList.add(SMEPAppVariable.getResource().getString(R.string.message_no_experience));
		//Get responses
		List<ResponseModel> responseList = experienceDatabaseManager.getResponsesForTab(ResponseModel.FOR_ROUTE);
		for(int i = 0; i< responseList.size(); i++ )
			mediaList.add(responseList.get(i).getHTMLCodeForResponse(false));
		return mediaList;
    }
	
    public void showTriggerZones(boolean visible)
    {
    	for(int i = 0; i < allTriggerZoneVizs.size(); i++)
	    {
            if(allTriggerZoneVizs.get(i) instanceof Circle)
            {
                Circle tmp = (Circle)allTriggerZoneVizs.get(i);
                tmp.setVisible(visible);
            }
            else if(allTriggerZoneVizs.get(i) instanceof Polygon)
            {
                Polygon tmp = (Polygon)allTriggerZoneVizs.get(i);
                tmp.setVisible(visible);
            }
	    }
    }
    
    public void showPOIThumbnails(boolean visible)
    {
    	for(int i = 0; i < allPoiMarkers.size(); i++)
    	{
    		allPoiMarkers.get(i).setVisible(visible);
    	}
    }

    /**
     * @param mLocation: location on map where the user taps on to select a POI
     * @return: index of the trigger zone else -1
     */
    public int getTriggerZoneIndexFromLocation(LatLng mLocation)//get ID of the trigger zone touched by users
    {
    	if(allTriggerZoneVizs.size()>0)
		{
			for (int i = 0; i < allTriggerZoneVizs.size(); i++)
			{
				if(isLocationWithinTrigerzone(mLocation, i))
					return i;
			}
		}
    	return -1;
    }

    /**
     *
     * @param mLocation: current location of the user
     * @param poiIndex: index of the selected/pushed POI
     * @return true if the user is withing the trigger zone of the POI
     */
    public boolean isLocationWithinTrigerzone(LatLng mLocation, int poiIndex)
	{
		if(poiIndex < 0 || poiIndex > allTriggerZoneVizs.size()-1)
			return false;
		if(allTriggerZoneVizs.get(poiIndex).getClass().equals(Circle.class))//Trigger zone is a circle
		{
			float[] results = new float[1];
			Circle tmpZone = (Circle)allTriggerZoneVizs.get(poiIndex);
			LatLng tmpPoint = tmpZone.getCenter();
			Location.distanceBetween(mLocation.latitude, mLocation.longitude, tmpPoint.latitude,tmpPoint.longitude, results);
			if(results[0] < tmpZone.getRadius())//radius of circle
				return true;
		}
		else if(allTriggerZoneVizs.get(poiIndex).getClass().equals(Polygon.class))//Trigger zone is a polygon
		{
			List<LatLng> polyPath = ((Polygon)allTriggerZoneVizs.get(poiIndex)).getPoints();
			if(SharcLibrary.isCurrentPointInsideRegion(new LatLng(mLocation.latitude, mLocation.longitude), polyPath))
				return true;
		}
		return false;
	}
    
	public void clearExperience()
	{
		allPOIs.clear();
		allEOIs.clear();
		allRoutes.clear();
		allPoiMarkers.clear();
		allTriggerZoneVizs.clear();		
	}

    //This list is displayed in the responses tab
	public ArrayList<String> getMyResponsesList()
	{
		getMyResponsesFromDatabase();
		ArrayList<String> resList = new ArrayList<String>();
		for (int i = 0; i < myResponses.size(); i++) {
            resList.add(this.getMyResponsePresentationName(i));
		}
		return resList;
	}

    public void getMyResponsesFromDatabase()
    {
        myResponses.clear();
        myResponses = experienceDatabaseManager.getMyResponses();
    }

    /**
     *
     * @param i: index of the response
     * @return: a string describing info about the response for displaying in the responses tab
     */
    public String getMyResponsePresentationName(int i)
	{
		String resName = "";
        ResponseModel res = myResponses.get(i);
        String responseForType = res.getEntityType().toUpperCase();
        String responseType = res.getContentType().toUpperCase();
		if(responseForType.equalsIgnoreCase(ResponseModel.FOR_POI))
		{
			String enName = getPOINameFromID(Long.valueOf(res.getEntityId()));
			if(enName!=null)
				resName =  responseType + " response for POI named " + enName;
		}
		else if(responseForType.equalsIgnoreCase(ResponseModel.FOR_EOI))
		{
            String enName = getEOINameFromId(Long.valueOf(res.getEntityId()));
			if(enName!=null)
				resName =  responseType + " response for EOI named " + enName;
			else
				resName =  responseType + " response for all EOIs";
		}
		else if(responseForType.equalsIgnoreCase(ResponseModel.FOR_ROUTE))
		{
			resName =  responseType + " response for the whole experience";
		}
		else if(responseForType.equalsIgnoreCase(ResponseModel.FOR_NEW_POI))
		{
			resName = responseType + " response for an undefined location";
		}
		else if(responseForType.equalsIgnoreCase(ResponseModel.FOR_MEDIA))
		{
			resName = responseType + " comment on a media item";
		}
		else if(responseForType.equalsIgnoreCase(ResponseModel.FOR_RESPONSE))
		{
			resName = responseType + " comment on a response";
		}
		return  resName + SharcLibrary.getResponseSize(myResponses.get(i).getContentType(), myResponses.get(i).getContent());
	}

	public void deleteMyResponseAt(int index)
	{
		experienceDatabaseManager.deleteMyResponse(myResponses.get(index).getId());
	}
	
	public void addMyResponse(ResponseModel res)
	{
		myResponses.add(res);
		experienceDatabaseManager.insertResponse(res);
	}

    /**********************************
    //Getters and setters for the class
     **********************************/

    public ExperienceMetaDataModel getMetaData() {
        return metaData;
    }

    public void setMetaData(ExperienceMetaDataModel metaData) {
        this.metaData = metaData;
    }

    public boolean isUpdatedConsumerExperience() {
        return isUpdatedConsumerExperience;
    }

    public void setIsUpdatedConsumerExperience(boolean isUpdatedConsumerExperience) {
        this.isUpdatedConsumerExperience = isUpdatedConsumerExperience;
    }

    public List<ResponseModel> getMyResponses() {
        return myResponses;
    }


    /**********************************
     //Other getters and setters
     **********************************/

    public String getPOIName(int index)
    {
        return allPOIs.get(index).getName();
    }

    public Long getPOIID(int index)
    {
        return allPOIs.get(index).getId();
    }

    public List<ResponseModel> getCommentsForEntity(Long id)
    {
        return experienceDatabaseManager.getCommentsForEntity(id);
    }

    public ResponseModel getMyResponseAt(int index)
    {
        return myResponses.get(index);
    }

    public String getMyResponseContentAt(int index)
    {
        return myResponses.get(index).getHTMLCodeForResponse(true);
    }

    public String getPOINameFromID(Long id)
    {
        for (int i = 0; i < allPOIs.size(); i++)
        {
            if(allPOIs.get(i).getId() == id)
                return allPOIs.get(i).getName();
        }
        return null;
    }

    public String getEOINameFromId(Long id)
    {
        for (int i = 0; i < allEOIs.size(); i++)
        {
            if(allEOIs.get(i).getId() == id)
                return allEOIs.get(i).getName();
        }
        return null;
    }
}
