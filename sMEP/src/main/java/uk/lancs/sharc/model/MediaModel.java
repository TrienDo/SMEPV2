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
    private Long id;
    private Long designerId;
    private Long experienceId;
    private String contentType;
    private String content;
    private String context;
    private String name;
    private String caption;
    private String entityType;
    private Long entityId;
    private int size;
    private boolean mainMedia;
    private boolean visible;
    private int order;

    public static final String TYPE_TEXT = "text";
    public static final String TYPE_IMAGE = "image";
    public static final String TYPE_AUDIO = "audio";
    public static final String TYPE_VIDEO = "video";

    public MediaModel(Long id, Long designerId, Long experienceId, String contentType, String content, String context, String name, String caption,
                      String entityType, Long entityID, int size, boolean mainMedia, boolean visible, int order) {
        this.id = id;
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
        this.order = order;
    }

    public String getHTMLPresentation()
    {
        return SharcLibrary.getHTMLCodeForMedia(this.getId().toString(), "media", this.getNoOfLike(), this.getNoOfComment(),this.getContentType(), this.getContent(), this.getName(),false);
    }
    public Long getId() {
        return id;
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

    public int getNoOfComment() {
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
    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }
}
