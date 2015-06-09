package services.goldway.api;

import services.goldway.Datagram;

/**
 * Created by Yuan on 2015/6/9.
 */
public class DatagramFactory {

    public static Datagram create(TradeRequest tradeRequest) {
        String serviceCode = tradeRequest.getServiceCode();
        String merId = tradeRequest.getMerId();
        String xml = tradeRequest.getXml();
        return new Datagram(serviceCode, merId, xml);
    }
}
