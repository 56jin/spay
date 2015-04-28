package utils;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;

import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.jdom.input.SAXBuilder;
import org.xml.sax.InputSource;

import com.shove.security.Encrypt;

import constants.Constants;
import play.Logger;
import play.cache.Cache;

public class PaymentUtil {

	public static String createXml(Map<String, String> xmlMap) {
		
		String strxml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><pReq>";
		
		for(Map.Entry<String, String> entry : xmlMap.entrySet()){
			
			String key = entry.getKey();
			String value = "";
			
			if(entry.getValue().getClass().isAssignableFrom(String.class)){
				value = entry.getValue().toString();
			}
			
			strxml = strxml + "<" + key + ">" + value + "</" + key + ">";
		}
		strxml = strxml + "</pReq>";
		Logger.info(strxml);
		return strxml;
	}
	
	/**
	 * 将json转化成xml
	 * @param jsonXml
	 * @return
	 */
	public static String jsonToXml(String jsonXml) {
		StringBuffer strXml = new StringBuffer("<?xml version=\"1.0\" encoding=\"utf-8\"?><pReq>");
		
		JSONObject json = JSONObject.fromObject(jsonXml);
		
		Iterator<String> iterator = json.keys();
		
		while(iterator.hasNext()) {
			String key =  iterator.next();
			String value = json.getString(key);
			
			strXml.append("<" + key + ">" + value + "</" + key + ">");
		}
		
		strXml.append("</pReq>");
		Logger.info(strXml.toString());
		
		return strXml.toString();
	}
	
	/**
	 * 将xml转化成map
	 * @param xml
	 * @return
	 */
	public static Map<String, String> parseXml(String xml) {
		 StringReader xmlString = new StringReader(xml);     
	    InputSource source = new InputSource(xmlString);
	    SAXBuilder sax = new SAXBuilder();     
	    org.jdom.Document document = null;
	    
	    try {    
	    	document = sax.build(source);
	    } catch (Exception e) {
       	e.printStackTrace();
       	Logger.info("解析xml字符串时：" + e.getMessage());
       	return null;
       }
	    
	    org.jdom.Element root = document.getRootElement();      
       List node = root.getChildren();     
       org.jdom.Element element = null;
       Map<String,String> result = new HashMap<String, String>(); 
       
       for (int i = 0; i < node.size(); i++) {     
       	element = (org.jdom.Element) node.get(i);
       	result.put(element.getName(), element.getText());
           System.out.println(element.getName()+":"+element.getText());  
       }    
       
       return result;
	}
	
	/**
	 * 将xml转化成json字符串
	 * @param xml
	 * @return
	 */
	public static String parseXmlToJson(String xml) {
		StringReader xmlString = new StringReader(xml);     
	    InputSource source = new InputSource(xmlString);
	    SAXBuilder sax = new SAXBuilder();     
	    org.jdom.Document document = null;
	    
	    try {    
	    	document = sax.build(source);
	    } catch (Exception e) {
       	e.printStackTrace();
       	Logger.info("解析xml字符串时：" + e.getMessage());
       	return null;
       }
	    
	    org.jdom.Element root = document.getRootElement();      
       List node = root.getChildren();     
       org.jdom.Element element = null;
       JSONObject json = new JSONObject();
       
       for (int i = 0; i < node.size(); i++) {     
	       	element = (org.jdom.Element) node.get(i);
	       	json.put(element.getName(), element.getText());
            System.out.println(element.getName()+":"+element.getText());  
       }    
       
       return json.toString();
	}
	
	public static boolean checkSign(String argMerCode, String arg3DesXmlPara, String argSign) {
		if(StringUtils.isBlank(argMerCode) || StringUtils.isBlank(argSign)) {
			return false;
		}
		
		if(StringUtils.isBlank(arg3DesXmlPara)) {
			return argSign.equals(Encrypt.MD5(argMerCode+Constants.ENCRYPTION_KEY));
		}
		
		return argSign.equals(Encrypt.MD5(argMerCode+arg3DesXmlPara+Constants.ENCRYPTION_KEY));
	}
	
	public static boolean expansionCheckSign(String src, String sign) {
		if (StringUtils.isBlank(src) || StringUtils.isBlank(sign)) {
			return false;
		}
		return sign.equals(Encrypt.MD5(src+Constants.ENCRYPTION_KEY));
	}
	
	public static void main(String[] args) {
//		parseXml();
		String xml = "<?xml version='1.0' encoding='utf-8'?><pReq><pStatus>0000</pStatus><pMerBillNo>430426199005271392</pMerBillNo><pIdentNo>430426199005271392</pIdentNo><pRealName>环迅</pRealName><pIpsAcctNo>4021000029908014</pIpsAcctNo><pIpsAcctDate>20140724</pIpsAcctDate><pMemo1>pMemo1</pMemo1><pMemo2>pMemo2</pMemo2><pMemo3>pMemo3</pMemo3></pReq>";
		Map<String, String> map = parseXml(xml);
		Set<Entry<String, String>> entry= map.entrySet();
		
		Iterator<Entry<String, String>> iterator = entry.iterator();
		
		while(iterator.hasNext()) {
			Entry<String, String> trys = iterator.next();
			System.out.println(trys.getKey()+":"+trys.getValue());
		}
		
//		String xml2 = createXml(map);
//		
//		System.out.println(createXml(map));
		System.out.println(xml);
		
//		System.out.println(xml.equals(xml2));
	}
	
	/**
	 * 将给定参数放入缓存中，返回key
	 * @param arg3DesXmlPara
	 * @param argeXtraPara
	 * @return
	 */
	public static String cacheParam(String arg3DesXmlPara, String argeXtraPara){
		String uuid = UUID.randomUUID().toString();
		Map<String,String> map = new HashMap<String,String>();
		map.put("arg3DesXmlPara", arg3DesXmlPara==null?"":arg3DesXmlPara);
		map.put("argeXtraPara", argeXtraPara==null?"":argeXtraPara);
		Cache.set(uuid, map, "10mn");
		
		return uuid;
	}
	
	public static Map getCacheParam(String key){
		Map map = (Map)Cache.get(key);
		if(map == null){
			Logger.info("提取缓存中的资金托管参数（key=%s）时，参数丢失", key);
		}
		Cache.delete(key);
		
		return map;
	}
}
