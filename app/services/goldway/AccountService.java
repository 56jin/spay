package services.goldway;

import services.goldway.api.AccountValidateRequest;
import services.goldway.api.PayResponse;

import java.util.Map;

/**
 * Created by Yuan on 2015/6/10.
 */
public class AccountService {

    public Map<String, String> validate(Map<String, String> parameter) throws Exception {
        AccountValidateRequest accountValidateRequest = new AccountValidateRequest(parameter);
        PayResponse payResponse = new PayResponse(accountValidateRequest);
        return payResponse.response();
    }
}
