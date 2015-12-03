package uk.lancs.sharc.model;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import uk.lancs.sharc.service.ExperienceDatabaseManager;

import com.google.android.gms.maps.model.LatLng;
import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

/**
 * <p>This class is a model of the POI entity</p>
 * <p>It can be changed later depending on future work </p>
 *
 * Author: Trien Do
 * Date: Feb 2015
 **/

public class POIModel extends SugarRecord{
	
	private Long id;
	private Long designerId;
	private Long experienceId;

	private String name;
	private String description;
	private String coordinate;//Can be a point or a polyline
	private String triggerZone;
	private String typeList;
	private String eoiList;
	private String routeList;

	//Info of trigger zone -> extracted from triggerZone string
	@Ignore
	private String triggerType;
	@Ignore
	private String triggerZoneColour;
	@Ignore
	private float triggerZoneRadius;
	@Ignore
	private ArrayList<LatLng> triggerZoneCoordinates = new ArrayList<LatLng>();

	public POIModel(){

	}
	public POIModel(Long id, String name, String description, String coordinate, String triggerZone, Long designerId, Long experienceId, String typeList, String eoiList, String routeList)
	{
		this.id = id;
		this.name = name;
		this.description = description;
		this.coordinate = coordinate;
		this.triggerZone = triggerZone;
		this.designerId = designerId;
		this.experienceId = experienceId;
		this.typeList = typeList;
		this.eoiList = eoiList;
		this.routeList = routeList;

		//Extract information for trigger zone
		String[] triggerInfo = triggerZone.split(" ");
		this.triggerType = triggerInfo[0];
		this.triggerZoneColour = "#" + triggerInfo[1];
		if(this.triggerType.equalsIgnoreCase("circle")) //triggerZoneString of circle: type[space]triggerZoneColour[space]triggerZoneRadius[space]lat[space]lng
		{
			triggerZoneRadius = Float.parseFloat(triggerInfo[2]);
			triggerZoneCoordinates.add(new LatLng(Float.parseFloat(triggerInfo[3]), Float.parseFloat(triggerInfo[4])));
		}
		else //triggerZoneString of polygon: type[space]triggerZoneColour[space]lat1[space]lng1---latN[space]lngN
		{
			triggerZoneRadius = 0;
			int k = 2;
			while (k < triggerInfo.length)
			{
				triggerZoneCoordinates.add(new LatLng(Float.parseFloat(triggerInfo[k]), Float.parseFloat(triggerInfo[k + 1])));
				k+=2;
			}
		}
	}	 
	
	public Long getId(){
		return id;
	}
	
	public String getName()
	{
        return name;
	}
	
	public String getDescription()
	{
        return description;
	}

	public List<LatLng> getPoiViz() {
		List<LatLng> poiViz = new ArrayList<LatLng>();
		//Get POI Viz
		String[] locationInfo = this.coordinate.split(" ");      //locationString is in the format:Lat[space]Lng
		if(locationInfo.length > 2) //polyline or polygon
		{
			int i = 0;
			while (i < locationInfo.length)
			{
				poiViz.add(new LatLng(Float.parseFloat(locationInfo[i]), Float.parseFloat(locationInfo[i+1])));
				i+=2;
			}
		}
		return poiViz;
	}

	public String getTriggerZoneColour()
	{
		return triggerZoneColour;
	}
	
	public ArrayList<LatLng> getTriggerZoneCoordinates()
	{
		return triggerZoneCoordinates;
	}
		
	public float getTriggerZoneRadius()
	{
		return triggerZoneRadius;
	}

	public String getType() {
		return typeList;
	}

	public String getTriggerType() {
		return triggerType;
	}

	public LatLng getLocation() {
		String[] locationInfo = this.coordinate.split(" ");      //locationString is in the format:Lat[space]Lng
		return new LatLng(Double.parseDouble(locationInfo[0]), Double.parseDouble(locationInfo[1]));
	}



	/**
	 * Get data related to a POI to render (EOIs - Media - Responses. All EOIs is a list item, each media and response is a separated item
	 * @param db: help interact with database
	 * @param state: whether the user is physically at the POI
	 * @return
	 */
	public List<String> getHtmlListItems(ExperienceDatabaseManager db, String state)
	{		
		List<String> strHtmlListItems = new ArrayList<String>();
		//Header = EOI info
		String header = "";
		//header = "<h3>" + this.getName() + "</h3><div style='color:gray;'>["  + state + "]</div><br/>";
		header = "<h3>" + this.getName() + "</h3>";
		//Get media
		List<MediaModel> mediaList = db.getMediaForEntity(this.id, "POI");
		int totalMedia = mediaList.size();
		//Get responses
		List<ResponseModel> responseList = db.getResponsesForEntity(this.id,"POI");
		//Get EOIs
		//responseList = null;
		String[] relatedEOIs;
		if(eoiList.equalsIgnoreCase(""))
			relatedEOIs = null;
		else
			relatedEOIs = eoiList.split(" ");

		if(relatedEOIs!=null)
		{
			if(relatedEOIs.length > 1)
				header += "<div> This Point of Interest has " + relatedEOIs.length + " related events.</div>";
			else
				header += "<div> This Point of Interest has " + relatedEOIs.length + " related event.</div>";
			for (int i = 0; i < relatedEOIs.length; i++)			
			{
				String[] eoiInfo = db.getEOIFromID(relatedEOIs[i]);
				//header += "<blockquote><button style='width:95%;height:40px;' onclick='alert(\""+ eoiInfo[1] + " Please go to Events of OI Media tab for more information!\")'>" + eoiInfo[0] + "</button></blockquote>";
				//Call a function bound in the AndroidWebViewInterface when an EOI button is clicked
				header += "<blockquote><button style='width:95%;height:40px;font-size:20px;' onclick='Android.showEOIInfo(\"" + relatedEOIs[i]   + "\")' >" + eoiInfo[0] + "</button></blockquote>";
			}
		}
		else
			header += "<div> This Point of Interest does not have any related events.</div>";
		header += "<div>It contains " +  totalMedia + " media " + (totalMedia > 1 ? "items" : "item") + " submitted by the designer";
		if(responseList !=null && responseList.size() > 0)
			header += " and " + responseList.size() + (responseList.size() > 1 ? " media items submitted in responses.</div>" : " media item submitted in a response.</div>") ;
		else
			header += ".</div>";

		strHtmlListItems.add(header);
		//Add media list
		strHtmlListItems.addAll(this.getHTMLMediaListItems(mediaList));
        //Add response list
		strHtmlListItems.addAll(this.getHTMLResponseListItems(responseList));
		return strHtmlListItems ; //EOI + Media + Response
	}

	public List<String> getHTMLMediaListItems(List<MediaModel> mediaList)
	{
		List<String> htmlMediaArray = new ArrayList<String>();
		if(mediaList != null && mediaList.size() > 0){
            for (MediaModel media : mediaList){
                String strMedia = media.getHTMLPresentation();
				htmlMediaArray.add(strMedia);
			}
		}
		return htmlMediaArray;
	}

    public List<String> getHTMLResponseListItems(List<ResponseModel> responseList)
    {
        ArrayList<String> htmlResponseArray = new ArrayList<String>();
        if(responseList != null && responseList.size() > 0)
        {
            for (ResponseModel response : responseList)
            {
                String strResponse = response.getHTMLCodeForResponse(false);
                htmlResponseArray.add(strResponse);
            }
        }
        return htmlResponseArray;
    }
}