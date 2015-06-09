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
     * 实时代付
     *
     * @param parameters
     * @return
     */
    public Map<String, String> deductRealPay(Map<String, String> parameters) throws Exception {

        // 生成请求明文
        String plaintext = TradeRequest.getSingleWithHoldRequest(parameters);

        // 商户代码
        String merId = parameters.get("merId");

        // 服务代码单笔代扣为NCPS0001;
        String serviceCode = Constants.DEDUCT_REAL_SERVICE_CODE;

        // 封装数据对象，用于加密解密报文，验签，获取明文
        Datagram data = new Datagram(merId, serviceCode, plaintext);

        // 商户私钥文件地址
        String privateKey = Constants.GOLD_WAY_PRIVATE_KEY;
        // 金通公钥文件地址
        String pubKey = Constants.GOLD_WAY_PUB_KEY;

        // 初始化密钥文件
        data.initKey(privateKey, pubKey);

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
        result.put("resultCode", "");
        result.put("resMsg", "");
        result.put("paySerialNo", "");
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
                ObjectMapper mapper = new ObjectMapper(); // can reuse, share globally
                DeductRealPay deductRealPay = mapper.convertValue(parameters, DeductRealPay.class);
                deductRealPay.create();

                // 获取明文响应信息
                String plainResult = data.getPlaintext();

                // 解析xml结果
                Element root = XmlHelper.getField(plainResult);
                Element ans = XmlHelper.child(root, "Ans");
                // 交易响应码
                String execCode = XmlHelper.elementAsString(ans, "ExecCode");
                // 响应描述
                String execMsg = XmlHelper.elementAsString(ans, "ExecMsg");
                // 金通生成的对应流水号,商户看情况使用
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
