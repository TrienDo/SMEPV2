package uk.lancs.sharc.model;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;

import com.google.android.gms.maps.model.LatLng;
import com.orm.SugarRecord;
import com.orm.dsl.Ignore;

/**
 * <p>This class is a reduced model of the route entity</p>
 * <p>It can be changed later depending on future work </p>
 *
 * Author: Trien Do
 * Date: Feb 2015
 */

public class RouteModel extends SugarRecord {
	private Long id;
	private Long designerId;
	private Long experienceId;
	private String name;
	private String description;
	private boolean directed;
	private String colour;
	private String path;
	private String poiList;
	private String eoiList;

	@Ignore
	private float distance; //the length of the route --> summary info

	public RouteModel(){

	}

	public RouteModel(Long id, Long designerId, Long experienceId, String name, String description, boolean directed, String colour, String path, String poiList, String eoiList){
		this.id = id;
		this.designerId = designerId;
		this.experienceId = experienceId;
		this.name = name;
		this.description = description;
		this.directed = directed;
		this.colour = colour;
		this.path = path;
		this.poiList = poiList;
		this.eoiList = eoiList;
	}

	public List<LatLng> getPath() {
		List<LatLng> latLngPath = new ArrayList<LatLng>();
		String[]latLngInfo = this.path.split(" ");
		if(latLngInfo.length > 2)
		{
			int i = 0;
			while (i < latLngInfo.length)
			{
				latLngPath.add(new LatLng(Float.parseFloat(latLngInfo[i]), Float.parseFloat(latLngInfo[i+1])));
				i+=2;
			}
		}
		return latLngPath;
	}
	public void setPath(String path) {
		this.path = path;
	}
	public float getDistance() {
		return distance;
	}
	public void setDistance(float distance) {
		this.distance = distance;
	}

	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getColour() {
		return colour;
	}

	public void setColour(String colour) {
		this.colour = colour;
	}

	public boolean getDirected() {
		return directed;
	}

	public void setDirected(boolean directed) {
		this.directed = directed;
	}
}
