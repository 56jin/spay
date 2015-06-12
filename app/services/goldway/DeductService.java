package services.goldway;

import services.goldway.api.RealPayFeedbackRequest;
import services.goldway.api.RealPayRequest;
import services.goldway.api.RealPayeeRequest;
import services.goldway.api.PayResponse;

import java.util.Map;

/**
 * Created by Yuan on 2015/6/8.
 */
public class DeductService {
    /**
     * 实时代付
     *
     * @param parameters
     * @return
     */
    public Map<String, String> deductRealPayee(Map<String, String> parameters) throws Exception {
        RealPayeeRequest realPayeeRequest = new RealPayeeRequest(parameters);
        PayResponse payResponse = new PayResponse(realPayeeRequest);
        return payResponse.response();
    }

    /**
     * 实时代收
     *
     * @param parameters
     * @return
     */
    public Map<String, String> deductRealPay(Map<String, String> parameters) throws Exception {
        RealPayRequest realPayRequest = new RealPayRequest(parameters);
        PayResponse payResponse = new PayResponse(realPayRequest);
        return payResponse.response();
    }

    /**
     * 交易结果反馈
     * @return
     * @throws Exception
     */
    public Map<String, String> deductRealPayBack() throws Exception {
        RealPayFeedbackRequest realPayFeedbackRequest = new RealPayFeedbackRequest();
        PayResponse payResponse = new PayResponse(realPayFeedbackRequest);
        return payResponse.response();
    }

}
