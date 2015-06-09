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
    // 商户私钥文件地址
    public static final String PRIVATE_KEY = Constants.GOLD_WAY_PRIVATE_KEY;
    // 金通公钥文件地址
    public static final String PUB_KEY = Constants.GOLD_WAY_PUB_KEY;

    private TradeRequest tradeRequest;

    public TradeResponse(TradeRequest tradeRequest) {
        this.tradeRequest = tradeRequest;
    }

    public Map<String, String> response() throws Exception {

        Datagram data = DatagramFactory.create(tradeRequest);

        data.initKey(PRIVATE_KEY, PUB_KEY);

        // 生成加密请求
        String req = data.Encrypt();

        // 发送请求并获取应答信息
        String resp = null;
        HttpClient httpClient = new HttpClient();
        HttpConnectionManagerParams managerParams = httpClient.getHttpConnectionManager().getParams();
        // 设置连接超时时间(单位毫秒)
        managerParams.setConnectionTimeout(60000);
        // 设置读数据超时时间(单位毫秒)
        managerParams.setSoTimeout(120000);
        httpClient.getParams().setParameter(HttpMethodParams.HTTP_CONTENT_CHARSET, "GBK");
        // 接口地址
        PostMethod postMethod = new PostMethod(Constants.GOLD_WAY_SERVICE_URL);
        postMethod.setRequestHeader("Connection", "close");
        // 将表单的值放入postMethod中
        postMethod.setRequestBody(req);
        // 执行postMethod
        int statusCode = 0;
        Map<String, String> result = new HashMap<String, String>();
        result.put("statusCode", "");
        result.put("verify", "0");

        try {
            statusCode = httpClient.executeMethod(postMethod);
            result.put("statusCode", statusCode + "");
            // 通信状态异常
            if (statusCode != 200) {
                return result;
            }

            // 读取内容
            byte[] responseBody = postMethod.getResponseBody();
            // 处理内容
            resp = new String(responseBody, "GBK");

            // 验签
            boolean verify = data.verifySign(resp);

            // 验签失败
            if (verify) {
                Model model = tradeRequest.getModel();
                model.create();

                // 获取明文响应信息
                String plainResult = data.getPlaintext();

                // 解析xml结果
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
