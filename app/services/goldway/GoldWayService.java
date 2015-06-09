package services.goldway;

import com.fasterxml.jackson.databind.ObjectMapper;
import constants.Constants;
import models.DeductRealPay;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.dom4j.Element;
import services.goldway.api.TradeRequest;
import services.goldway.util.XmlHelper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Yuan on 2015/6/8.
 */
public class GoldWayService {
    /**
     * ʵʱ����
     *
     * @param parameters
     * @return
     */
    public Map<String, String> deductRealPay(Map<String, String> parameters) throws Exception {

        // ������������
        String plaintext = TradeRequest.getSingleWithHoldRequest(parameters);

        // �̻�����
        String merId = parameters.get("merId");

        // ������뵥�ʴ���ΪNCPS0001;
        String serviceCode = Constants.DEDUCT_REAL_SERVICE_CODE;

        // ��װ���ݶ������ڼ��ܽ��ܱ��ģ���ǩ����ȡ����
        Datagram data = new Datagram(merId, serviceCode, plaintext);

        // �̻�˽Կ�ļ���ַ
        String privateKey = Constants.GOLD_WAY_PRIVATE_KEY;
        // ��ͨ��Կ�ļ���ַ
        String pubKey = Constants.GOLD_WAY_PUB_KEY;

        // ��ʼ����Կ�ļ�
        data.initKey(privateKey, pubKey);

        // ���ɼ�������
        String req = data.Encrypt();

        // �������󲢻�ȡӦ����Ϣ
        String resp = null;
        HttpClient httpClient = new HttpClient();
        HttpConnectionManagerParams managerParams = httpClient.getHttpConnectionManager().getParams();
        // �������ӳ�ʱʱ��(��λ����)
        managerParams.setConnectionTimeout(60000);
        // ���ö����ݳ�ʱʱ��(��λ����)
        managerParams.setSoTimeout(120000);
        httpClient.getParams().setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET, "GBK");
        // �ӿڵ�ַ
        PostMethod postMethod = new PostMethod(Constants.GOLD_WAY_SERVICE_URL);
        postMethod.setRequestHeader("Connection", "close");
        // ������ֵ����postMethod��
        postMethod.setRequestBody(req);
        // ִ��postMethod
        int statusCode = 0;
        Map<String, String> result = new HashMap<String, String>();
        result.put("statusCode", "");
        result.put("verify", "0");
        result.put("resultCode", "");
        result.put("resMsg", "");
        result.put("paySerialNo", "");
        try {
            statusCode = httpClient.executeMethod(postMethod);
            result.put("statusCode", statusCode + "");
            // ͨ��״̬�쳣
            if (statusCode != 200) {
                return result;
            }

            // ��ȡ����
            byte[] responseBody = postMethod.getResponseBody();
            // ��������
            resp = new String(responseBody, "GBK");

            // ��ǩ
            boolean verify = data.verifySign(resp);

            // ��ǩʧ��
            if (verify) {
                ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
                DeductRealPay deductRealPay = mapper.convertValue(parameters, DeductRealPay.class);
                deductRealPay.create();

                // ��ȡ������Ӧ��Ϣ
                String plainResult = data.getPlaintext();

                // ����xml���
                Element root = XmlHelper.getField(plainResult);
                Element ans = XmlHelper.child(root, "Ans");
                // ������Ӧ��
                String execCode = XmlHelper.elementAsString(ans, "ExecCode");
                // ��Ӧ����
                String execMsg = XmlHelper.elementAsString(ans, "ExecMsg");
                // ��ͨ���ɵĶ�Ӧ��ˮ��,�̻������ʹ��
                String paySerialNo = XmlHelper.elementAsString(ans, "PaySerialNo");

                result.put("execCode", execCode);
                result.put("execMsg", execMsg);
                result.put("paySerialNo", paySerialNo);

                deductRealPay.setExecCode(execCode);
                deductRealPay.setExecMsg(execMsg);
                deductRealPay.setPaySerialNo(paySerialNo);

                deductRealPay.save();

            }  else {
                result.put("verify", "1");
            }


        } catch (HttpException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

}
