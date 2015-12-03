package uk.lancs.sharc.model;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;

import uk.lancs.sharc.service.SharcLibrary;
import android.net.Uri;

import com.orm.SugarRecord;

/**
 * <p>This class is a model of the response entity</p>
 *
 * Author: Trien Do
 * Date: Feb 2015
 */

public class ResponseModel extends SugarRecord {
	private Long id;
	private String type;
	private String desc;
	private String content;
	private String noOfLike;

	private String noOfComment;
	private String entityType;
	private String entityID;
	private String conName;
	private String conEmail;
	private String status;
	private Uri fileUri;

	public static final String FOR_POI = "POI";
	public static final String FOR_EOI = "EOI";
	public static final String FOR_ROUTE = "ROUTE";
	public static final String FOR_NEW_POI = "NEW";
	public static final String FOR_MEDIA = "MEDIA";
	public static final String FOR_RESPONSE = "RESPONSES";

	public static final String STATUS_ACCEPTED = "accepted";
	public static final String STATUS_FOR_UPLOAD = "uploading";

	
	public ResponseModel(String mID, String mStatus, String mType, String mDesc, String mContent, String mEntityType, String mEntityID, String mNoOfLike, String mConName, String mConEmail)
	{
		this.id = Long.getLong(mID);
	    this.type = mType;//(Text/Image/Audio/Video)
	    this.desc = mDesc;
	    this.content = mContent; // Content (Text vs. path to media)
	    this.noOfLike = mNoOfLike;
	    this.entityType = mEntityType;
	    this.entityID = mEntityID;
	    this.conName = mConName;
	    this.conEmail = mConEmail;
	    this.status = mStatus;
	}

	public String getEntityID() {
		return entityID;
	}

	public void setEntityID(String entityID) {
		this.entityID = entityID;
	}

	public String getDesc() {
        try {
            return URLDecoder.decode(desc, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return desc;
	}

	public void setDesc(String desc) {
		this.desc = desc;
	}

	public String getContent() {
        if(type.equalsIgnoreCase("text"))
        {
            try {
                return URLDecoder.decode(content,"UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
        return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getNoOfLike() {
		return noOfLike;
	}

	public void setNoOfLike(String noOfLike) {
		this.noOfLike = noOfLike;
	}

	public String getNoOfComment() {
		return noOfComment;
	}

	public void setNoOfComment(String noOfComment) {
		this.noOfComment = noOfComment;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public String getConName() {
        try {
            return URLDecoder.decode(conName,"UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return conName;
	}

	public void setConName(String conName) {
		this.conName = conName;
	}

	public String getConEmail() {
		return conEmail;
	}

	public void setConEmail(String conEmail) {
		this.conEmail = conEmail;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getResponseId() {
		return id.toString();
	}

	public void setId(String id) {
		this.id = Long.getLong(id);
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Uri getFileUri() {
		//return fileUri;
		File mediaFile = new File(this.getContent());
		return Uri.fromFile(mediaFile);
	}

	public void setFileUri(Uri fileUri) {
		this.fileUri = fileUri;
	}

	public String getHTMLCodeForResponse(boolean isLocal)//Two types of responses: local added by the current user, online submitted by other users
	{
		String responseHeader = "<div style='background-color:#AAEEFF;'><p style='margin-left:30px;font-weight:bold;'> A response added by ";
		if(isLocal)
			responseHeader += "you at " + new Date(this.getId()).toString() + "</p>";
		else
			responseHeader += this.getConName() + " at " + new Date(this.getId()).toString() + "</p>";
		return responseHeader + SharcLibrary.getHTMLCodeForMedia(this.getId().toString(),"Responses", this.getNoOfLike(), this.getNoOfComment(), this.getType(), this.getContent(), this.getDesc(), isLocal) + "</div>";
	}
}
