import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;
import java.util.*;
/**
 * Created by koosh on 26/6/17.
 */
public class docFrame {
    private String docno;
    private String HTTPheader;
    private String title;
    private String html_Source;
    private List<String> out_links;
    private String text;
    private String author;
    private Integer depth;
    private String url;

    public docFrame(){

    }


    public String getDocno() {
        return docno;
    }

    public void setDocno(String docno) {
        this.docno = docno;
    }

    @JsonProperty("HTTPheader")
    public String getHTTPheader() {
        return HTTPheader;
    }

    @JsonProperty("HTTPheader")
    public void setHTTPheader(String HTTPheader) {
        this.HTTPheader = HTTPheader;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setHtml_Source(String html_Source) {
        this.html_Source = html_Source;
    }

    public String getHtml_Source() {
        return html_Source;
    }

    public List<String> getOut_links() {
        return out_links;
    }

    public void setOut_links(List<String> out_links) {
        this.out_links = out_links;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public Integer getDepth() {
        return depth;
    }

    public void setDepth(Integer depth) {
        this.depth = depth;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
