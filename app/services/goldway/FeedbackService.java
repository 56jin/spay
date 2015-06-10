package services.goldway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import constants.Constants;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import play.Logger;
import play.db.jpa.JPA;
import services.goldway.api.FeedbackResultFactory;
import services.goldway.util.RSAHelper;
import services.goldway.xml.RequestXml;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Yuan on 2015/6/10.
 */
public class FeedbackService {

    public String callBack(String xml) {
        Date date = new Date();
        RequestXml requestXml = new RequestXml();
        Map<String, String> pub = requestXml.getPub();
        pub.put("TransDate", DateFormatUtils.ISO_DATE_FORMAT.format(date));
        pub.put("TransTime", DateFormatUtils.ISO_TIME_FORMAT.format(date));
        Map<String, String> ans = requestXml.getAns();
        ans.put("ExecCode", "01");
        ans.put("ExecMsg", "������ʽ����");
        XmlMapper xmlMapper = new XmlMapper();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String resp = new String(xml.getBytes(), "GBK");
            String signStr = resp.substring(35, 379);            // ǩ����ֵ
            String cipertext = resp.substring(379);
            RSAHelper cipher = new RSAHelper();
            cipher.initKey(Constants.GOLD_WAY_PRIVATE_KEY, Constants.GOLD_WAY_PUB_KEY, 2048);
            String plaintext = cipher.decrypt(cipertext);

            //����
            if (cipher.verify(plaintext, signStr)) {
                requestXml = xmlMapper.readValue(plaintext, RequestXml.class);
                pub = requestXml.getPub();
                if (pub != null) {
                    String serviceCode = pub.get("TransCode");

                    Class<?> feedbackResult = FeedbackResultFactory.createFeedbackResult(serviceCode);
                    Map<String, String> requestMap = new HashMap<String, String>();

                    requestMap.putAll(pub);
                    requestMap.putAll(requestXml.getReq());

                    Object o = objectMapper.convertValue(requestMap, feedbackResult);
                    try {
                        JPA.em().persist(o);
                        requestXml.getAns().put("ExecCode", "00");
                        requestXml.getAns().put("ExecMsg", "�ɹ�");
                        Logger.info("[��ͨ�ص��ɹ�] ServiceCode is " + serviceCode);
                    } catch (Exception e) {
                        Logger.error("[��ͨ�ص�,��������ʧ��] ServiceCode is " + serviceCode, e);
                        requestXml.getAns().put("ExecCode", "01");
                        requestXml.getAns().put("ExecMsg", "����ʧ��");
                    }

                } else {
                    requestXml.getAns().put("ExecCode", "01");
                    requestXml.getAns().put("ExecMsg", "�����벻��Ϊ��");
                }
            } else {
                requestXml.getAns().put("ExecCode", "01");
                requestXml.getAns().put("ExecMsg", "����ʧ��");
            }


        } catch (IOException e) {
            Logger.error("[��ͨ�ص�ʧ��]", e);
        }

        StringBuffer result = new StringBuffer();
        result.append("<?xml version=\"1.0\" encoding=\"GBK\"?>");
        try {
            requestXml.getReq().clear();
            result.append(xmlMapper.writeValueAsString(requestXml));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return result.toString();
    }
}
