package services.goldway;

import services.goldway.util.FileUtil;
import services.goldway.util.RSAHelper;
import services.goldway.util.StringUtil;

/**
 * 类名： Datagram
 * 功能：组装报文、报文加解密、验签
 * 诺亚金通  版权所有.
 * @author zyc    2014-12-27
 * @version 1.0
 */
public class Datagram {
	
	/** 15位发起机构号*/
	private String merId;
	
	/** 8位交易码*/
	private String serverCode;
	
	/**交易请求报文体*/
	private String reqXml;
	
	/** 加解密对象*/
	private RSAHelper cipher;
	
	/** 报文主体明文*/
	private String plaintext;
	
	/**
	 * 构造器
	 * @param merId			商户号
	 * @param serverCode	服务交易码
	 * @param reqXml		请求XML报文
	 */
	public Datagram(String merId, String serverCode, String reqXml) {
		this.merId = merId;
		this.serverCode = serverCode;
		this.reqXml = reqXml;
	}
	
	/**
	 * 初始化加密对象
	 * @param merKeyPath	商户私钥
	 * @param pubKeyPath	系统公钥
	 * 备注： 系统公钥->加密 	商户私钥->签名
	 * @throws Exception 
	 */
    public void initKey(String merKeyPath, String pubKeyPath) throws Exception {
    	cipher = new RSAHelper();	
    	String merKey = FileUtil.loadKey(merKeyPath);
    	String pubKey = FileUtil.loadKey(pubKeyPath);
		cipher.initKey(merKey, pubKey, 2048);
    }
	
	/**
	 * 功能：报文加密
	 * @return
	 */
	public String Encrypt() {
		/*
		 *                                    报文头
		 *                  ____________________________________________
		 * 报文结构：8位报文总长度	15位发起机构号	 8位交易码	  4位签名域长度	  344位签名域值	   n位XML报文数据主体密文
		 *        |       | |        |   |    |   |       |   |        |   |            |
		 *        0       7 8        22  23   30  31      34  35       378 379          n
		 */
		try {
			String data = cipher.encrypt(reqXml);
			String sign = cipher.sign(reqXml);
			
			String signLen = StringUtil.leftAppendZero(sign.length() + "", 4);
			int gramaLen = merId.length() + serverCode.length() + signLen.length() + sign.length() + data.length();
			String gramaLenStr = StringUtil.leftAppendZero(gramaLen + "", 8);
			
			// 组装报文
			return new StringBuilder("")			
			.append(gramaLenStr)	// 8位报文总长度
			.append(merId)			// 15位发起机构号
			.append(serverCode)	    // 8位交易码
			.append(signLen)		// 4位签名域长度
			.append(sign)			// 签名域值
			.append(data)			// XML报文数据主体密文
			.toString();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("【error】【流程：交易报文加密签名】出现未处理异常");
		}
		return null;
	}
	
	/**
	 * 功能：报文验签
	 * @param data	验签报文
	 * @return
	 * @throws Exception
	 */
	public boolean verifySign(String data) throws Exception {
		// 系统公钥：验签 	商户私钥：解密
		String signStr =  data.substring(35, 379);			// 签名域值
		String cipertext = data.substring(379);				// 密文
		plaintext = cipher.decrypt(cipertext);				// 解密->明文
		return cipher.verify(plaintext, signStr);
	}
	
	public String getMerId() {
		return merId;
	}

	public void setMerId(String merId) {
		this.merId = merId;
	}

	public String getServerCode() {
		return serverCode;
	}

	public void setServerCode(String serverCode) {
		this.serverCode = serverCode;
	}

	public String getReqXml() {
		return reqXml;
	}

	public void setReqXml(String reqXml) {
		this.reqXml = reqXml;
	}

	public String getPlaintext() {
		return plaintext;
	}

	public void setPlaintext(String plaintext) {
		this.plaintext = plaintext;
	}

}
