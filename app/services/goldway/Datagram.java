package services.goldway;

import services.goldway.util.FileUtil;
import services.goldway.util.RSAHelper;
import services.goldway.util.StringUtil;

/**
 * ������ Datagram
 * ���ܣ���װ���ġ����ļӽ��ܡ���ǩ
 * ŵ�ǽ�ͨ  ��Ȩ����.
 * @author zyc    2014-12-27
 * @version 1.0
 */
public class Datagram {
	
	/** 15λ���������*/
	private String merId;
	
	/** 8λ������*/
	private String serverCode;
	
	/**������������*/
	private String reqXml;
	
	/** �ӽ��ܶ���*/
	private RSAHelper cipher;
	
	/** ������������*/
	private String plaintext;
	
	/**
	 * ������
	 * @param merId			�̻���
	 * @param serverCode	��������
	 * @param reqXml		����XML����
	 */
	public Datagram(String merId, String serverCode, String reqXml) {
		this.merId = merId;
		this.serverCode = serverCode;
		this.reqXml = reqXml;
	}
	
	/**
	 * ��ʼ�����ܶ���
	 * @param merKeyPath	�̻�˽Կ
	 * @param pubKeyPath	ϵͳ��Կ
	 * ��ע�� ϵͳ��Կ->���� 	�̻�˽Կ->ǩ��
	 * @throws Exception 
	 */
    public void initKey(String merKeyPath, String pubKeyPath) throws Exception {
    	cipher = new RSAHelper();	
    	String merKey = FileUtil.loadKey(merKeyPath);
    	String pubKey = FileUtil.loadKey(pubKeyPath);
		cipher.initKey(merKey, pubKey, 2048);
    }
	
	/**
	 * ���ܣ����ļ���
	 * @return
	 */
	public String Encrypt() {
		/*
		 *                                    ����ͷ
		 *                  ____________________________________________
		 * ���Ľṹ��8λ�����ܳ���	15λ���������	 8λ������	  4λǩ���򳤶�	  344λǩ����ֵ	   nλXML����������������
		 *        |       | |        |   |    |   |       |   |        |   |            |
		 *        0       7 8        22  23   30  31      34  35       378 379          n
		 */
		try {
			String data = cipher.encrypt(reqXml);
			String sign = cipher.sign(reqXml);
			
			String signLen = StringUtil.leftAppendZero(sign.length() + "", 4);
			int gramaLen = merId.length() + serverCode.length() + signLen.length() + sign.length() + data.length();
			String gramaLenStr = StringUtil.leftAppendZero(gramaLen + "", 8);
			
			// ��װ����
			return new StringBuilder("")			
			.append(gramaLenStr)	// 8λ�����ܳ���
			.append(merId)			// 15λ���������
			.append(serverCode)	    // 8λ������
			.append(signLen)		// 4λǩ���򳤶�
			.append(sign)			// ǩ����ֵ
			.append(data)			// XML����������������
			.toString();
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("��error�������̣����ױ��ļ���ǩ��������δ�����쳣");
		}
		return null;
	}
	
	/**
	 * ���ܣ�������ǩ
	 * @param data	��ǩ����
	 * @return
	 * @throws Exception
	 */
	public boolean verifySign(String data) throws Exception {
		// ϵͳ��Կ����ǩ 	�̻�˽Կ������
		String signStr =  data.substring(35, 379);			// ǩ����ֵ
		String cipertext = data.substring(379);				// ����
		plaintext = cipher.decrypt(cipertext);				// ����->����
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
