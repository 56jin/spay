package utils;

import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import play.Logger;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

public class Converter {
	
	/**
	 * xml字符串转json字符串
	 * @param xml
	 * @return
	 */
	public static String xmlToJson(String xml){ 
		String json = null;
		try {
			json = new XMLSerializer().read(xml).toString();
		} catch(Exception e){
			Logger.error("xml转json出现异常");
			return "";
		}
		
        return json;  
    }
	
	/**
	 * json字符串转xml字符串
	 * @param json
	 * @return
	 */
    public static String jsonToXml(String json){  
        XMLSerializer xmlSerializer = new XMLSerializer();  
        xmlSerializer.setTypeHintsEnabled(false);      
        
        String xml = null;
		try {
			xml = xmlSerializer.write(JSONSerializer.toJSON(json));
		} catch(Exception e){
			Logger.error("json转xml出现异常");
			return "";
		}
		
        return xml; 
    }
    
	/**
	 * json字符串转xml字符串
	 * @param json
	 * @param rootName
	 * @param elementName
	 * @param objectName
	 * @param arrayName
	 * @return
	 */
    public static String jsonToXml(String json, String rootName, String elementName, String objectName, String arrayName){  
        XMLSerializer xmlSerializer = new XMLSerializer();  
        xmlSerializer.setTypeHintsEnabled(false);
        
        if (StringUtils.isNotBlank(rootName)) {
        	xmlSerializer.setRootName(rootName);
		}
        
        if (StringUtils.isNotBlank(elementName)) {
        	xmlSerializer.setElementName(elementName);
		}
        
        if (StringUtils.isNotBlank(objectName)) {
        	xmlSerializer.setObjectName(objectName);
        }
        
        if (StringUtils.isNotBlank(arrayName)) {
        	xmlSerializer.setArrayName(arrayName);
        }
        
        String xml = null;
		try {
			xml = xmlSerializer.write(JSONSerializer.toJSON(json));
		} catch(Exception e){
			Logger.error("json转xml出现异常");
			return "";
		}
		
        return xml; 
    }
    
	/**
	 * xml字符串转json对象/数组
	 * @param xml
	 * @return
	 */
	public static JSON xmlToObj(String xml) {
		JSON json = null;
		try {
			json = new XMLSerializer().read(xml);
		}catch(Exception e){
			Logger.error("xml转json对象/数组出现异常");
			return null;
		}
		return json;  
	}
	
	public static String parseMapToXml(LinkedHashMap<String, String> xmlMap){

		String strxml = "<?xml version=\"1.0\" encoding=\"utf-8\"?><pReq>";
		try {
			
			for (Map.Entry<String, String> entry : xmlMap.entrySet()) {
				
				String key = entry.getKey();
				String value = "";
				if (entry.getValue().getClass().isAssignableFrom(String.class)) {
					value = entry.getValue().toString();
				}
				if(value == null){
					new Exception("参数" + key + "不能为null!");
					
						throw new Exception("参数" + key + "不能为null!");
					
				}
				if(value.equals("")){				
					throw new Exception("参数" + key + "不能为null!");
				}
				strxml = strxml + "<" + key + ">" + value + "</" + key + ">";
			}
			
		} catch (Exception e) {
			Logger.error("xml转json出现异常");
			return "";
		}
		
		strxml = strxml + "</pReq>";
		Logger.info(strxml);
		return strxml;
	}
}
