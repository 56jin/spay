package utils;

import org.apache.commons.lang3.StringUtils;
import play.Logger;
import play.Play;
import play.mvc.Http;

/**
 * <p>Project: com.shovesoft.sp2p</p>
 * <p>Title: ParseClientUtil.java</p>
 * <p>Description: </p>
 * <p>Copyright (c) 2014 Sunlights.cc</p>
 * <p>All Rights Reserved.</p>
 *
 * @author <a href="mailto:jiaming.wang@sunlights.cc">wangJiaMing</a>
 */
public class ParseClientUtil {
    public static final String PC = "0";
    public static final String APP = "1";
    public static final String H5 = "2";
    
    public static final String APP_AGENT_NAMES = Play.configuration.getProperty("app.agent.names");

    public static String parseClient(Http.Request request){
        Http.Header userAgentHeader = request.headers.get("User-Agent");
        if (userAgentHeader == null) {
            userAgentHeader = request.headers.get("user-agent");
        }

        if (userAgentHeader != null && StringUtils.isNotEmpty(userAgentHeader.value())){
            String userAgent = userAgentHeader.value();
            Logger.info("User-Agent:" + userAgent);
            if (userAgent.contains("Mobile") || userAgent.contains("mobile")) {
                String appNames = APP_AGENT_NAMES;
                if (StringUtils.isNotEmpty(appNames)) {
                    String[] appAgents = appNames.split(";");
                    for (String appAgent : appAgents) {
                        if (userAgent.contains(appAgent)) {
                            return APP;
                        }
                    }
                }

                return H5;
            }
        }

        return PC;
    }


}
