package services.goldway.xml;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Yuan on 2015/6/10.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JacksonXmlRootElement(localName="package")
public class RequestXml {
    @JacksonXmlProperty(localName="Pub")
    private Map<String,String> pub = new HashMap<String, String>();

    @JacksonXmlProperty(localName="Req")
    private Map<String,String> req = new HashMap<String, String>();

    @JacksonXmlProperty(localName="Ans")
    private Map<String,String> ans = new HashMap<String, String>();

    public Map<String, String> getPub() {
        return pub;
    }

    public void setPub(Map<String, String> pub) {
        this.pub = pub;
    }

    public Map<String, String> getReq() {
        return req;
    }

    public void setReq(Map<String, String> req) {
        this.req = req;
    }

    public Map<String, String> getAns() {
        return ans;
    }

    public void setAns(Map<String, String> ans) {
        this.ans = ans;
    }
}
