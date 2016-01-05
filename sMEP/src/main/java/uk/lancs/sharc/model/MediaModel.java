package uk.lancs.sharc.model;

import com.orm.SugarRecord;

import uk.lancs.sharc.service.SharcLibrary;

/**
 * <p>This class is a model of the Media entity</p>
 *
 * Created by SHARC
 * Date: May 2015.
 */
public class MediaModel extends SugarRecord {
    private String mid;
    private String designerId;
    private String experienceId;
    private String contentType;
    private String content;
    private String context;
    private String name;
    private String caption;
    private String entityType;
    private String entityId;
    private int size;
    private boolean mainMedia;
    private boolean visible;
    private int mediaOrder;
    private int commentCount;

    public static final String TYPE_TEXT = "text";
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_AUDIO = "audio";
    public static final String TYPE_VIDEO = "video";

    public MediaModel(){
    }
    public MediaModel(String id, String designerId, String experienceId, String contentType, String content, String context, String name, String caption,
                      String entityType, String entityID, int size, boolean mainMedia, boolean visible, int order, int commentCount) {
        this.mid = id;
        this.designerId = designerId;
        this.experienceId = experienceId;
        this.contentType = contentType;
        this.content = content;
        this.context = context;
        this.name = name;
        this.caption = caption;
        this.name = name;
        this.caption = caption;
        this.entityType = entityType;
        this.entityId = entityID;
        this.size = size;
        this.mainMedia = mainMedia;
        this.visible = visible;
        this.mediaOrder = order;
        this.commentCount = commentCount;
    }

    public String getHTMLPresentation()
    {
        return SharcLibrary.getHTMLCodeForMedia(this.getMediaId().toString(), "media", this.getNoOfLike(), this.commentCount,this.getContentType(), this.getContent(), this.getCaption(),false);
    }
    public String getMediaId() {
        return mid;
    }

    public void setId(String id) {
        this.mid = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    public int getNoOfLike() {
        return 0;
    }

    public void setNoOfComment(String noOfComment) {
        //this.noOfComment = noOfComment;
    }

    public void setNoOfLike(String noOfLike) {
        //this.noOfLike = noOfLike;
    }

    public String getContentType(){
        return contentType;
    }
    public String getEntityId() {
        return entityId;
    }

    public void setEntityId(String entityId) {
        this.entityId = entityId;
    }

    public String getDesignerId() {
        return designerId;
    }

    public void setDesignerId(String designerId) {
        this.designerId = designerId;
    }

    public String getExperienceId() {
        return experienceId;
    }

    public void setExperienceId(String experienceId) {
        this.experienceId = experienceId;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public boolean isMainMedia() {
        return mainMedia;
    }

    public void setMainMedia(boolean mainMedia) {
        this.mainMedia = mainMedia;
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public int getOrder() {
        return mediaOrder;
    }

    public void setOrder(int order) {
        this.mediaOrder = order;
    }


}
