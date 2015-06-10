package services.goldway;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.commons.lang.time.DateFormatUtils;
import play.Logger;
import play.db.jpa.JPA;
import services.goldway.api.FeedbackResultFactory;
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
        RequestXml requestXml;
        XmlMapper xmlMapper = new XmlMapper();
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            requestXml = xmlMapper.readValue(xml, RequestXml.class);
            Map<String, String> pub = requestXml.getPub();
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

        } catch (IOException e) {
            Logger.error("[��ͨ�ص�ʧ��]", e);
            Date date = new Date();
            requestXml = new RequestXml();
            Map<String, String> pub = requestXml.getPub();
            pub.put("TransDate", DateFormatUtils.ISO_DATE_FORMAT.format(date));
            pub.put("TransTime", DateFormatUtils.ISO_TIME_FORMAT.format(date));
            Map<String, String> ans = requestXml.getAns();
            ans.put("ExecCode", "01");
            ans.put("ExecMsg", "������ʽ����");
        }

        StringBuffer result = new StringBuffer();
        result.append("<?xml version=\"1.0\" encoding=\"GBK\"?>");
        try {
            result.append(xmlMapper.writeValueAsString(requestXml));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return result.toString();
    }
}
