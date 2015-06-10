package services.goldway.xml;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import models.AccountValidate;

import java.io.IOException;
import java.util.Map;

/**
 * Created by Yuan on 2015/6/8.
 */
public class DeductRealPayTest {

    public static void main(String[] args) {
//        readDeductRealPayTest();
        convertValue();
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

}