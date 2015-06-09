package services.goldway.api;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import constants.Constants;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpConnectionManagerParams;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.dom4j.Element;
import play.db.jpa.Model;
import services.goldway.Datagram;
import services.goldway.util.XmlHelper;
import services.goldway.xml.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Yuan on 2015/6/9.
 */
public class TradeResponse {
    // �̻�˽Կ�ļ���ַ
    public static final String PRIVATE_KEY = Constants.GOLD_WAY_PRIVATE_KEY;
    // ��ͨ��Կ�ļ���ַ
    public static final String PUB_KEY = Constants.GOLD_WAY_PUB_KEY;

    private TradeRequest tradeRequest;

    public TradeResponse(TradeRequest tradeRequest) {
        this.tradeRequest = tradeRequest;
    }

    public Map<String, String> response() throws Exception {

        Datagram data = DatagramFactory.create(tradeRequest);

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
        PostMethod postMethod = new PostMethod(Constants.GOLD_WAY_SERVICE_URL);
        postMethod.setRequestHeader("Connection", "close");
        // ������ֵ����postMethod��
        postMethod.setRequestBody(req);
        // ִ��postMethod
        int statusCode = 0;
        Map<String, String> result = new HashMap<String, String>();
        result.put("statusCode", "");
        result.put("verify", "0");

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
                Model model = tradeRequest.getModel();
                model.create();

                // ��ȡ������Ӧ��Ϣ
                String plainResult = data.getPlaintext();

                // ����xml���
                Element root = XmlHelper.getField(plainResult);
                Element ans = XmlHelper.child(root, "Ans");
                String text = ans.getText();

                XmlMapper xmlMapper = new XmlMapper();
                Ans ans1 = xmlMapper.readValue(text, Ans.class);
                Map<String, String> map = ans1.getMap();
                for (String key : map.keySet()) {
                    result.put(key, map.get(key));
                    BeanUtils.setProperty(model, key, map.get(key));
                }
                model.save();

            } else {
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
