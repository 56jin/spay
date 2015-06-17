package controllers.goldway;

import com.fasterxml.jackson.databind.ObjectMapper;
import controllers.BaseController;
import models.AccountValidate;
import services.goldway.AccountService;

import java.util.Map;

/**
 * Created by Yuan on 2015/6/10.
 */
public class AccountController extends BaseController {

    private static AccountService accountService = new AccountService();

    public static void validate() {

        Map<String, String> parameters = params.allSimple();
        parameters.remove("body");
        parameters.remove("authenticityToken");
        try {
            Map<String, String> map = accountService.validate(parameters);
            ObjectMapper objectMapper = new ObjectMapper();
            AccountValidate accountValidate = objectMapper.convertValue(map, AccountValidate.class);
            accountValidate.save();
            renderJSON(accountValidate);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void validateIndex() {
        render();
    }
}
