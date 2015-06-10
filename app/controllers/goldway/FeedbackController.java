package controllers.goldway;

import controllers.BaseController;
import services.goldway.FeedbackService;

/**
 * Created by Yuan on 2015/6/10.
 */
public class FeedbackController extends BaseController {

    private static FeedbackService feedbackService = new FeedbackService();

    public static void callback() {
        String xml = request.body.toString();
        response.setHeader("Content-Type","text/xml;charset=GBK");
        String callBack = feedbackService.callBack(xml);
        renderXml(callBack);
    }

}
