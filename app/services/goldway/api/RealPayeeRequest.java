package services.goldway.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import constants.Constants;
import models.DeductRealPayee;
import play.db.jpa.Model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Created by Yuan on 2015/6/9.
 */
public class RealPayeeRequest extends TradeRequest {

    public RealPayeeRequest(Map<String, String> map) {
        super("ʵʱ����", Constants.DEDUCT_REAL_PAYEE_SERVICE_CODE, map);
    }

    @Override
    public String getXml() {
        Map<String, String> map = getMap();
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
        sb.append("<TransType>" + map.get("transType") + "</TransType>");
//		sb.append("<BizType>"+map.get("bizType")+"</BizType>");
        sb.append("<BizObjType>" + map.get("bizObjType") + "</BizObjType>");
        sb.append("<PayeeAcc>" + map.get("payeeAcc") + "</PayeeAcc>");
        sb.append("<PayeeName>" + map.get("payeeName") + "</PayeeName>");
        sb.append("<BankAccType>" + map.get("bankAccType") + "</BankAccType>");
        sb.append("<PayeeBankCode>" + map.get("payeeBankCode") + "</PayeeBankCode>");
        sb.append("<PayeeBankName>" + map.get("payeeBankName") + "</PayeeBankName>");
        sb.append("<PayeeBankNo>" + map.get("payeeBankNo") + "</PayeeBankNo>");
        sb.append("<Amt>" + map.get("amt") + "</Amt>");
        sb.append("<CertType>" + map.get("certType") + "</CertType>");
        sb.append("<CertNo>" + map.get("certNo") + "</CertNo>");
        sb.append("<Mobile>" + map.get("mobile") + "</Mobile>");
        sb.append("<ProvNo>" + map.get("provNo") + "</ProvNo>");
        sb.append("<CityNo>" + map.get("cityNo") + "</CityNo>");
        sb.append("<Purpose>" + map.get("purpose") + "</Purpose>");
        sb.append("<Postscript>" + map.get("postscript") + "</Postscript>");
        sb.append("</Req>");
        sb.append("</package>");
        return sb.toString();

    }

}
