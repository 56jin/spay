package services.goldway.api;

import services.goldway.Datagram;

/**
 * Created by Yuan on 2015/6/9.
 */
public class DatagramFactory {

    private DatagramFactory() {
    }

    public static Datagram create(PayRequest payRequest) {
        String serviceCode = payRequest.getServiceCode();
        String merId = payRequest.getMerId();
        String xml = payRequest.getXml();
        return new Datagram(serviceCode, merId, xml);
    }
}
