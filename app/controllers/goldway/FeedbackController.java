package controllers.goldway;

import controllers.BaseController;
import org.apache.commons.io.IOUtils;
import services.goldway.FeedbackService;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by Yuan on 2015/6/10.
 */
public class FeedbackController extends BaseController {

    private static FeedbackService feedbackService = new FeedbackService();

    public static void callback() {
        InputStream body = request.body;
        String xml = body.toString();
        try {
            xml = IOUtils.toString(request.body, "GBK");
        } catch (IOException e) {
            e.printStackTrace();
        }
        String callBack = feedbackService.callBack(xml);
        renderXml(callBack);
    }

}
