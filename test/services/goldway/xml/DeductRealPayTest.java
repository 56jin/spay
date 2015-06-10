package services.goldway.xml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import models.AccountValidate;
import org.apache.commons.lang.time.DateFormatUtils;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

/**
 * Created by Yuan on 2015/6/8.
 */
public class DeductRealPayTest {

    public static void main(String[] args) {
//        readDeductRealPayTest();
//        convertValue();
//        toXml();
        readeXml();
    }

    //    @Test
    public static void readDeductRealPayTest() {
        DeductRealPayVo deductRealPay = new DeductRealPayVo();
        DeductRealPayVo.Pub pub = new DeductRealPayVo.Pub();
        pub.setSerialNo("2015042000000001");
        DeductRealPayVo.Req req = new DeductRealPayVo.Req();
        req.setMerId("631000000000001");
        req.setMerName("金通测试用户");
        req.setTransType("0402");
        deductRealPay.setPub(pub);
        deductRealPay.setReq(req);

        JacksonXmlModule module = new JacksonXmlModule();

        ObjectMapper xmlMapper = new XmlMapper(module);


        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"GBK\"?>");
        try {
            xml.append(xmlMapper.writeValueAsString(deductRealPay));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        System.out.println(xml.toString());
    }

    public static void convertValue() {
        StringBuffer xml = new StringBuffer();
        xml.append("<Ans>");
        xml.append("<Return_Code>1</Return_Code>");
        xml.append("<Return_Message>1</Return_Message>");
        xml.append("<SerialNo>1</SerialNo>");
        xml.append("<TradeDesc>1</TradeDesc>");
        xml.append("</Ans>");

        XmlMapper xmlMapper = new XmlMapper();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            Map map = xmlMapper.readValue(xml.toString(), Map.class);
            AccountValidate accountValidate = objectMapper.convertValue(map, AccountValidate.class);
            System.out.println(accountValidate.getReturnCode());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void toXml() {
        Date date = new Date();
        RequestXml requestXml = new RequestXml();
        requestXml.getPub().put("Version", "1.0");
        requestXml.getPub().put("TransCode", "NCPS1002");
        requestXml.getPub().put("TransDate", DateFormatUtils.ISO_DATE_FORMAT.format(date));
        requestXml.getPub().put("TransTime", DateFormatUtils.ISO_TIME_FORMAT.format(date));
        requestXml.getPub().put("SerialNo", "20150619871");

        requestXml.getReq().put("MerId", "20150619871");
        requestXml.getReq().put("OriPaySerialNo", "20150619871");
        requestXml.getReq().put("OriTransDate", "20150619871");
        requestXml.getReq().put("TransType", "20150619871");
        requestXml.getReq().put("Amt", "20150619871");
        requestXml.getReq().put("Status", "20150619871");
        requestXml.getReq().put("Description", "20150619871");
        XmlMapper xmlMapper = new XmlMapper();
        try {
            String xml = xmlMapper.writeValueAsString(requestXml);
            System.out.println("[xml]" + xml);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public static void readeXml() {
        Date date = new Date();
        StringBuffer reqXml = new StringBuffer();
        reqXml.append("<?xml version=\"1.0\" encoding=\"GBK\"?>");
        reqXml.append("<package>");
        StringBuffer pub = new StringBuffer();
        pub.append("<Pub>");
        pub.append("<TransCode>NCPS1002</TransCode>");
        pub.append("<TransDate>" + DateFormatUtils.ISO_DATE_FORMAT.format(date) + "</TransDate>");
        pub.append("<TransTime>" + DateFormatUtils.ISO_TIME_FORMAT.format(date) + "</TransTime>");
        pub.append("<SerialNo>20150619871</SerialNo>");
        pub.append("</Pub>");
        reqXml.append(pub);

        reqXml.append("<Req>");
        reqXml.append("<MerId>NCPS1002</MerId>");
        reqXml.append("<OriPaySerialNo>20150619871</OriPaySerialNo>");
        reqXml.append("<OriTransDate>" + DateFormatUtils.ISO_DATE_FORMAT.format(date) + "</OriTransDate>");
        reqXml.append("<TransType>0401</TransType>");
        reqXml.append("<Amt>10000</Amt>");
        reqXml.append("<Status>01</Status>");
        reqXml.append("<Description>成功</Description>");
        reqXml.append("</Req>");
        reqXml.append("</package>");

        XmlMapper xmlMapper = new XmlMapper();
        try {
            RequestXml requestXml = xmlMapper.readValue(reqXml.toString(), RequestXml.class);
            System.out.println("[pub]" + requestXml.getPub());
            System.out.println("[req]" + requestXml.getReq());
            requestXml.getReq().clear();

            // .....
            // .....

            requestXml.getAns().put("ExecCode","00");
            requestXml.getAns().put("ExecMsg","成功");

            String result = xmlMapper.writeValueAsString(requestXml);

            System.out.println(result);

        } catch (IOException e) {
            e.printStackTrace();
        }



    }

}