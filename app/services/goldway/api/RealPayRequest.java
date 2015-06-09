package services.goldway.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import constants.Constants;
import models.DeductRealPay;
import play.db.jpa.Model;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

/**
 * Created by Yuan on 2015/6/9.
 */
public class RealPayRequest extends TradeRequest {

    public RealPayRequest(Map<String, String> map) {
        super("实时代收", Constants.DEDUCT_REAL_PAY_SERVICE_CODE, map);
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
        sb.append("<BizType>" + map.get("bizType") + "</BizType>");
        sb.append("<BizObjType>" + map.get("bizObjType") + "</BizObjType>");
        sb.append("<PayerAcc>" + map.get("payerAcc") + "</PayerAcc>");
        sb.append("<PayerName>" + map.get("payerName") + "</PayerName>");
        sb.append("<CardType>" + map.get("cardType") + "</CardType>");
        sb.append("<PayerBankCode>" + map.get("payerBankCode") + "</PayerBankCode>");
        sb.append("<PayerBankName>" + map.get("payerBankName") + "</PayerBankName>");
        sb.append("<PayerBankNo>" + map.get("payerBankNo") + "</PayerBankNo>");
        sb.append("<Amt>" + map.get("amt") + "</Amt>");
        sb.append("<CertType>" + map.get("certType") + "</CertType>");
        sb.append("<CertNo>" + map.get("certNo") + "</CertNo>");
        sb.append("<ProvNo>" + map.get("provNo") + "</ProvNo>");
        sb.append("<CityNo>" + map.get("cityNo") + "</CityNo>");
        sb.append("<Purpose>" + map.get("purpose") + "</Purpose>");
        sb.append("<Postscript>" + map.get("postscript") + "</Postscript>");
        sb.append("</Req>");
        sb.append("</package>");
        return sb.toString();
    }

    @Override
    public Model getModel() {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.convertValue(getMap(), DeductRealPay.class);
    }


}
