package utils;


import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;   
import java.util.Iterator;   
import java.util.List;
import java.util.Map;   
import java.util.Map.Entry;
import java.util.Set;

import net.sf.json.JSONObject;

import org.dom4j.Attribute;
import org.dom4j.Document;   
import org.dom4j.DocumentException;   
import org.dom4j.DocumentHelper;   
import org.dom4j.Element;
import org.dom4j.tree.DefaultAttribute;
import org.jdom.input.SAXBuilder;
import org.xml.sax.InputSource;

import play.Logger;
import play.libs.XML;
import play.libs.XPath;

import com.ips.security.utility.IpsCrypto;

public class XmlParse {
	
	public static void parseXml(String xml) {
		Document document = null;  
		Element root = null;
		
        try { 
        	document = (Document) DocumentHelper.parseText(xml); // 将字符串转为         
            root = document.getRootElement(); // 获取根节点   
            System.out.println("根节点：" + root.getName()); 
            
        } catch (Exception e) {
        	e.printStackTrace();
        	Logger.info("解析xml字符串时：" + e.getMessage());
        }   
        String iter = root.elementTextTrim("pStatus");    
        System.out.println("pStatus：" + iter);
        String pMerBillNo = root.elementTextTrim("pMerBillNo");     
        System.out.println("pMerBillNo：" + pMerBillNo);
        String pIdentNo = root.elementTextTrim("pIdentNo");    
        System.out.println("pIdentNo：" + pIdentNo);
        String pRealName = root.elementTextTrim("pRealName");     
        System.out.println("pRealName：" + pRealName);
        String pIpsAcctNo = root.elementTextTrim("pIpsAcctDate");   
        System.out.println("pIpsAcctNo：" + pIpsAcctNo);
        String pIpsAcctDate = root.elementTextTrim("pIpsAcctDate");    
        System.out.println("pIpsAcctDate：" + pIpsAcctDate);
        String pMemo1 = root.elementTextTrim("pMemo1");     
        System.out.println("pMemo1：" + pMemo1);
        String pMemo2 = root.elementTextTrim("pMemo2");     
        System.out.println("pMemo2：" + pMemo2);
        String pMemo3 = root.elementTextTrim("pMemo3");    
        System.out.println("pMemo3：" + pMemo3);

	}
	
	public static Map<String, String> parseXml2(String xml) {
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
	
	public static void main(String[] args) {
//		parseXml();
//		String xml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><pReq><pStatus>0000</pStatus><pMerBillNo>430426199005271392</pMerBillNo><pIdentNo>430426199005271392</pIdentNo><pRealName>环迅</pRealName><pIpsAcctNo>4021000029908014</pIpsAcctNo><pIpsAcctDate>20140724</pIpsAcctDate><pMemo1>pMemo1</pMemo1><pMemo2>pMemo2</pMemo2><pMemo3>pMemo3</pMemo3></pReq>";
//		Map<String, String> map = parseXml2(xml);
//		Set<Entry<String, String>> entry= map.entrySet();
//		
//		Iterator<Entry<String, String>> iterator = entry.iterator();
//		
//		while(iterator.hasNext()) {
//			Entry<String, String> trys = iterator.next();
//			System.out.println(trys.getKey()+":"+trys.getValue());
//		}
		
		String json = "{\"CERT_MD5\":\"GPhKt7sh4dxQQZZkINGFtefRKNPyAj8S00cgAwtRyy0ufD7alNC28xCBKpa6IU7u54zzWSAv4PqUDKMgpOnM7fucO1wuwMi4RgPAnietmqYIhHXZ3TqTGKNzkxA55qYH\","
				+ " \"PUB_KEY\":\"-----BEGIN PUBLIC KEY-----#MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCMWwKD0u90z1K8WvtG6cZ3SXHL#"
				+ "UqmQCWxbT6JURy5BVwgsTdsaGmr22HT4jfEBQHEjmTtyUWC5Ag9Cwgef0VFrDB7T#qyhWfVA7n8SvV6b1eDbQlY/qhUb50+3SCpN7HxdPzdMDkJjy6i6syh7RtH0QfoAp#"
				+ "HS6TLY4DjPvbGgdXhwIDAQAB#-----END PUBLIC KEY-----\","
				+ " \"DES_KEY\":\"ICHuQplJ0YR9l7XeVNKi6FMn\","
				+ " \"DES_IV\":\"2EDxsEfp\"}";
		System.out.println(json);
		JSONObject jsonobject = JSONObject.fromObject(json);
		System.out.println(jsonobject.get("CERT_MD5"));

		
		
	}
}
