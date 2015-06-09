package controllers.goldway;

import controllers.BaseController;
import services.goldway.DeductService;

import java.util.Map;

/**
 * Created by Yuan on 2015/6/8.
 */
public class DeductController extends BaseController {

    private static DeductService deductService = new DeductService();

    public static void deductRealPayIndex() {
        render();
    }

    public static void deductRealPay() {
        Map<String, String> parameters = params.allSimple();
        try {
            Map<String, String> map = deductService.deductRealPay(parameters);
            renderJSON(map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void deductRealPayeeIndex() {
        render();
    }

    public static void deductRealPayee() {
        Map<String, String> parameters = params.allSimple();
        try {
            Map<String, String> map = deductService.deductRealPayee(parameters);
            renderJSON(map);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
