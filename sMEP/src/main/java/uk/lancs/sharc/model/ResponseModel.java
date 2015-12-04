package uk.lancs.sharc.model;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Date;

import uk.lancs.sharc.service.SharcLibrary;
import android.net.Uri;

import com.orm.SugarRecord;
import com.orm.dsl.Ignore;
import com.orm.dsl.Unique;

/**
 * <p>This class is a model of the response entity</p>
 *
 * Author: Trien Do
 * Date: Feb 2015
 */

public class ResponseModel extends SugarRecord {
	//@Unique
	private Long id;
	private Long experienceId;
	private Long userId;
	private String contentType;
	private String content;
	private String description;
	private String entityType;
	private Long entityId;
	private String status;
	private int size;
	private String submittedDate;

	@Ignore
	private Uri fileUri;

	public static final String FOR_POI = "POI";
	public static final String FOR_EOI = "EOI";
	public static final String FOR_ROUTE = "ROUTE";
	public static final String FOR_NEW_POI = "NEW";
	public static final String FOR_MEDIA = "MEDIA";
	public static final String FOR_RESPONSE = "RESPONSES";

	public static final String STATUS_ACCEPTED = "accepted";
	public static final String STATUS_FOR_UPLOAD = "uploading";

	public ResponseModel(){

	}
	public ResponseModel(Long id, Long experienceId, Long userId, String contentType, String content, String description,
						 String entityType, Long entityId, String status, int size, String submittedDate)
	{
		this.id = id;
		this.experienceId = experienceId;
		this.userId = userId;
	    this.contentType = contentType;//(Text/Image/Audio/Video)
		this.content = content; // Content (Path to media)
	    this.description = description;
		this.entityType = entityType;
		this.entityId = entityId;
	    this.status = status;
	    this.size = size;
	    this.submittedDate = submittedDate;
	}

	public Long getEntityId() {
		return entityId;
	}

	public void setEntityID(Long entityId) {
		this.entityId = entityId;
	}

	public String getDescription() {
        return description;
	}

	public void setDescription(String desc) {
		this.description = desc;
	}

	public String getContent() {
        return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public int getNoOfLike() {
		return 0;
	}

	public int getNoOfComment() {
		return 0;
	}

	public String getEntityType() {
		return entityType;
	}

	public void setEntityType(String entityType) {
		this.entityType = entityType;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String type) {
		this.contentType = type;
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
			responseHeader += "you at " + this.submittedDate + "</p>";
		else
			responseHeader += this.userId + " at " + this.submittedDate + "</p>";
		return responseHeader + SharcLibrary.getHTMLCodeForMedia(this.getId().toString(),"Responses", this.getNoOfLike(), this.getNoOfComment(), this.getContentType(),
				this.getContent(), this.getDescription(), isLocal) + "</div>";
	}

	public String getSubmittedDate(){
		return submittedDate;
	}

	public Long getUserId(){
		return  userId;
	}

}
