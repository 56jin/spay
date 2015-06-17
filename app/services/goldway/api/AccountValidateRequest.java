package services.goldway.api;

import constants.Constants;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Created by Yuan on 2015/6/10.
 */
public class AccountValidateRequest extends PayRequest {

    public AccountValidateRequest(Map<String, String> map) {
        super("实名认证", Constants.ACCOUNT_VALIDATE_SERVICE_CODE, map);
    }

    @Override
    public String getXml() {
        Map<String, String> map = getMap();
        map.put("merId", Constants.GOLD_WAY_VALIDATE_KEY);
        Date transTime = new Date();
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"GBK\"?>");
        sb.append("<package>");
        sb.append("<Pub>");
        sb.append("<Version>" + "1.0" + "</Version>");
        sb.append("<TransCode>" + getServiceCode() + "</TransCode>");
        sb.append("<TransDate>" + new SimpleDateFormat("yyyyMMdd").format(transTime) + "</TransDate>");
        sb.append("<TransTime>" + new SimpleDateFormat("HHmmss").format(transTime) + "</TransTime>");
        sb.append("<SerialNo>" + map.get("serialNo") + "</SerialNo>");
        sb.append("</Pub>");
        sb.append("<Req>");
        sb.append("<MerId>" + map.get("merId") + "</MerId>");
        sb.append("<MerName>" + map.get("merName") + "</MerName>");
        sb.append("<AccNo>" + map.get("accNo") + "</AccNo>");
        sb.append("<AccName>" + map.get("accName") + "</AccName>");
        sb.append("<CertType>" + map.get("certType") + "</CertType>");
        sb.append("<CertNo>" + map.get("certNo") + "</CertNo>");
        sb.append("<Mobile>" + map.get("mobile") + "</Mobile>");
        sb.append("<BankNo>" + map.get("bankNo") + "</BankNo>");
        sb.append("</Req>");
        sb.append("</package>");
        return sb.toString();
    }
}
