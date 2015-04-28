package utils;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

import org.apache.commons.lang.StringUtils;

import play.Logger;
import constants.Constants;
import constants.IPSConstants;

public class IPSUtil {

	/**
	 * webservice
	 * @param Url 请求地址
	 * @param argMerCode 商户号
	 * @param arg3DesXmlPara 参数xml
	 * @param argSign 签名
	 * @return
	 */
	public static String getSoapInputStream(int type, String argMerCode, String argIpsAccount, String argSign,  String argMemo) {  
        try {  
            String soap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"+
            "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"+
            "<soap:Body>"+
            "  <"+IPSConstants.IPS_URL[type]+" xmlns=\"http://tempuri.org/\">"+
            "    <argMerCode>"+argMerCode+"</argMerCode>";
            
            if(StringUtils.isNotBlank(argIpsAccount)) {
            	soap +="    <argIpsAccount>"+argIpsAccount+"</argIpsAccount>";
            }
            
            soap += "    <argSign>"+argSign+"</argSign>";
            
            if(StringUtils.isNotBlank(argMemo)) {
            	soap += "    <argMemo>"+argMemo+"</argMemo>";
            }
            
            soap += "  </"+IPSConstants.IPS_URL[type]+">"+
            "</soap:Body>"+
            "</soap:Envelope>";
            
            String wsUrl = IPSConstants.WS_URL;
            
            if(type == IPSConstants.USER_INFO) {
            	wsUrl = IPSConstants.WS_URL_QUERY;
            }
            URL url = new URL(wsUrl);  
            URLConnection conn = url.openConnection();  
            conn.setUseCaches(false);  
            conn.setDoInput(true);  
            conn.setDoOutput(true);  
            conn.setRequestProperty("Content-Length", Integer.toString(soap  
                    .length()));  
            conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");  
            conn.setRequestProperty("SOAPAction", "http://tempuri.org/"+IPSConstants.IPS_URL[type]);  
  
            OutputStream os = conn.getOutputStream();  
            OutputStreamWriter osw = new OutputStreamWriter(os, "utf-8");  
            osw.write(soap);
            osw.flush();  
            osw.close();  

            InputStream is = conn.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int i = -1;
			
			while ((i = is.read()) != -1) {
				baos.write(i);
			}
			
			return baos.toString("utf-8");
  
        } catch (Exception e) {  
            e.printStackTrace();
            Logger.info("WS调用："+e.getMessage());
            return null;  
        }  
    } 

	public static String getSoapInputStream2(int type, String pMerCode, String p3DesXmlPara, String pSign) {  
        String soap = "";
		if(type == IPSConstants.UNFREEZE || type == IPSConstants.DEDUCT) {
			soap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"+
                    "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"+
                    "<soap:Body>"+
                    "  <"+IPSConstants.IPS_URL[type]+" xmlns=\"http://tempuri.org/\">"+
                    "    <argMerCode>"+pMerCode+"</argMerCode>"+
                    "    <arg3DesXmlPara>"+p3DesXmlPara+"</arg3DesXmlPara>"+
                    "    <argSign>"+pSign+"</argSign>"+
                    "  </"+IPSConstants.IPS_URL[type]+">"+
                    "</soap:Body>"+
                    "</soap:Envelope>";
        }else {
        	soap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"+
                    "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"+
                    "<soap:Body>"+
                    "  <"+IPSConstants.IPS_URL[type]+" xmlns=\"http://tempuri.org/\">"+
                    "    <pMerCode>"+pMerCode+"</pMerCode>"+
                    "    <p3DesXmlPara>"+p3DesXmlPara+"</p3DesXmlPara>"+
                    "    <pSign>"+pSign+"</pSign>"+
                    "  </"+IPSConstants.IPS_URL[type]+">"+
                    "</soap:Body>"+
                    "</soap:Envelope>";
        }
		
		try {
            String wsUrl = IPSConstants.WS_URL;
            
            URL url = new URL(wsUrl);  
            URLConnection conn = url.openConnection();  
            conn.setUseCaches(false);  
            conn.setDoInput(true);  
            conn.setDoOutput(true);  
            conn.setRequestProperty("Content-Length", Integer.toString(soap.length()));  
            conn.setRequestProperty("Content-Type", "text/xml; charset=utf-8");  
            conn.setRequestProperty("SOAPAction", "http://tempuri.org/"+IPSConstants.IPS_URL[type]);
            
            OutputStream os = conn.getOutputStream();
            OutputStreamWriter osw = new OutputStreamWriter(os, "utf-8");  
            osw.write(soap);
            osw.flush();  
            osw.close();  

            InputStream is = conn.getInputStream();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
			int i = -1;
			
			while ((i = is.read()) != -1) {
				baos.write(i);
			}
			
			return baos.toString("utf-8");
  
        } catch (Exception e) {  
            e.printStackTrace();
            Logger.info("WS调用："+e.getMessage());
            return null;  
        }  
    } 
}
