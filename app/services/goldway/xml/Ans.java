package services.goldway.xml;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Yuan on 2015/6/9.
 */
@JacksonXmlRootElement(localName = "ans")
public class Ans {
    private Map<String, String> map = new HashMap<String, String>();

    @JsonAnyGetter
    public Map<String, String> getMap() {
        return map;
    }

    @JsonAnySetter
    public void setMap(Map<String, String> map) {
        this.map = map;
    }
}
