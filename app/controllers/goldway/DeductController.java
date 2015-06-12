package controllers.goldway;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import controllers.BaseController;
import models.DeductRealPay;
import models.DeductRealPayee;
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
        parameters.remove("body");
        parameters.remove("authenticityToken");
        try {
            Map<String, String> map = deductService.deductRealPay(parameters);
            ObjectMapper objectMapper = new ObjectMapper();
            DeductRealPay deductRealPay = objectMapper.convertValue(map, DeductRealPay.class);
            deductRealPay.save();
            renderJSON(deductRealPay);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void deductRealPayeeIndex() {
        render();
    }

    public static void deductRealPayee() {
        Map<String, String> parameters = params.allSimple();
        parameters.remove("body");
        try {
            Map<String, String> map = deductService.deductRealPayee(parameters);
            ObjectMapper objectMapper = new ObjectMapper();
            DeductRealPayee deductRealPayee = objectMapper.convertValue(map, DeductRealPayee.class);
            deductRealPayee.save();
            renderJSON(deductRealPayee);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void  deductRealPayFeedback() {
        try {
            Map<String, String> stringStringMap = deductService.deductRealPayBack();
            renderJSON(stringStringMap);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
