package services.goldway;

import services.goldway.api.RealPayRequest;
import services.goldway.api.RealPayeeRequest;
import services.goldway.api.TradeResponse;

import java.util.Map;

/**
 * Created by Yuan on 2015/6/8.
 */
public class DeductService {
    /**
     * ʵʱ����
     *
     * @param parameters
     * @return
     */
    public Map<String, String> deductRealPayee(Map<String, String> parameters) throws Exception {
        RealPayeeRequest realPayeeRequest = new RealPayeeRequest(parameters);
        TradeResponse tradeResponse = new TradeResponse(realPayeeRequest);
        return tradeResponse.response();
    }

    /**
     * ʵʱ����
     *
     * @param parameters
     * @return
     */
    public Map<String, String> deductRealPay(Map<String, String> parameters) throws Exception {
        RealPayRequest realPayRequest = new RealPayRequest(parameters);
        TradeResponse tradeResponse = new TradeResponse(realPayRequest);
        return tradeResponse.response();
    }

}
