package controllers.goldway;

import controllers.BaseController;
import services.goldway.GoldWayService;

import java.util.Map;

/**
 * Created by Yuan on 2015/6/8.
 */
public class GoldWayPayment extends BaseController {

    private static GoldWayService goldWayService = new GoldWayService();

    public static void deductRealPayIndex() {
        render();
    }

    public static void deductRealPay() {
        Map<String, String> parameters = params.allSimple();
        try {
            Map<String, String> map = goldWayService.deductRealPay(parameters);
            renderJSON(map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
