package services.goldway.api;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by Yuan on 2015/6/9.
 */
public abstract class PayRequest {
    private String name;
    private String serviceCode;
    private Map<String, String> map = new HashMap<String, String>();

    public PayRequest(String name, String serviceCode, Map<String, String> map) {
        this.name = name;
        this.serviceCode = serviceCode;
        this.map = map;
    }

    public abstract String getXml();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public Map<String, String> getMap() {
        return map;
    }

    public void setMap(Map<String, String> map) {
        this.map = map;
    }

    public String getMerId() {
        return map.get("merId");
    }


}
