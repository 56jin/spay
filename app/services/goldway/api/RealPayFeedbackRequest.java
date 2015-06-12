package services.goldway.api;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Yuan on 2015/6/9.
 */
public class RealPayFeedbackRequest extends PayRequest {

    public RealPayFeedbackRequest() {
        super("交易结果反馈", "NCPS1002", new HashMap<String, String>());
    }

    @Override
    public String getXml() {
        Date transTime = new Date();
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"GBK\"?>");
        sb.append("<package>");
        sb.append("<Pub>");
        sb.append("<Version>" + "1.0" + "</Version>");
        sb.append("<TransCode>" + getServiceCode() + "</TransCode>");
        sb.append("<TransDate>" + new SimpleDateFormat("yyyyMMdd").format(transTime) + "</TransDate>");
        sb.append("<TransTime>" + new SimpleDateFormat("HHmmss").format(transTime) + "</TransTime>");
        sb.append("<SerialNo>2012025897411111</SerialNo>");
        sb.append("</Pub>");
        sb.append("<Req>");
        sb.append("<MerId>201202589741111</MerId>");
        sb.append("<OriTransDate>20120205</OriTransDate>");
        sb.append("<TransType>0401</TransType>");
        sb.append("<Amt>5000</Amt>");
        sb.append("<Status>00</Status>");
        sb.append("<Description>成功</Description>");
        sb.append("</Req>");
        sb.append("</package>");
        return sb.toString();
    }

}
