/**
 * Created by koosh on 26/6/17.
 */
public class metaLink {
    private String finalLink;
    private String contentType;

    public metaLink(String finalLink,String contentType){
        this.finalLink = finalLink;
        this.contentType = contentType;
    }

    public String getContentType() {
        return contentType;
    }

    public String getFinalLink() {
        return finalLink;
    }
}
