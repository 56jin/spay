package services.goldway.api;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import constants.Constants;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.dom4j.Element;
import play.Logger;
import services.goldway.Datagram;
import services.goldway.util.XmlHelper;

import java.io.IOException;
import java.util.Date;
import java.util.Map;

/**
 * Created by Yuan on 2015/6/9.
 */
public class PayResponse {
    // �̻�˽Կ�ļ���ַ
    public static final String PRIVATE_KEY = Constants.GOLD_WAY_PRIVATE_KEY;
    // ��ͨ��Կ�ļ���ַ
    public static final String PUB_KEY = Constants.GOLD_WAY_PUB_KEY;
    // �������
    public static final String SERVICE_URL = Constants.GOLD_WAY_SERVICE_URL;

    private PayRequest payRequest;

    public PayResponse(PayRequest payRequest) {
        this.payRequest = payRequest;
    }

    public Map<String, String> response() throws Exception {
        Logger.info("[" + payRequest.getName() + "] request begin at " + new Date());
        Map<String, String> result = payRequest.getMap();

        Datagram data = DatagramFactory.create(payRequest);

        data.initKey(PRIVATE_KEY, PUB_KEY);

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
        PostMethod postMethod = new PostMethod(SERVICE_URL);
        postMethod.setRequestHeader("Connection", "close");
        // ������ֵ����postMethod��
        postMethod.setRequestBody(req);
        // ִ��postMethod
        int statusCode = 0;
        try {
            statusCode = httpClient.executeMethod(postMethod);
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
                // ��ȡ������Ӧ��Ϣ
                String plainResult = data.getPlaintext();

                // ����xml���
                Element root = XmlHelper.getField(plainResult);
                Element ans = XmlHelper.child(root, "Ans");
                String text = ans.getText();

                XmlMapper xmlMapper = new XmlMapper();
                Map<String, String> map = xmlMapper.readValue(text, Map.class);
                for (String key : map.keySet()) {
                    result.put(key, map.get(key));
                }

            }

        } catch (HttpException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        Logger.info("[" + payRequest.getName() + "] response end at " + new Date());
        return result;
    }
}
