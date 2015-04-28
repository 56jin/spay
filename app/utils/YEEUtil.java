package utils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.tree.DefaultAttribute;

import play.Logger;
import play.libs.WS;
import play.libs.WS.WSRequest;

public class YEEUtil {

	/** 
	* 发送post请求到直接接口
	* 
	* @param strURL 
	* @param req 
	* @return 
	*/ 
	public static String doPostQueryCmd(String strURL, Map<String, Object> req){ 
        String response = null;
		
		try {
			WSRequest request = null;
			request= WS.url(strURL);
	
			Map<String, String> header = new HashMap<String, String>();
			header.put("Content-Type","application/x-www-form-urlencoded;charset=utf-8");
			request.headers = header;
			
			response = request.params(req).post().getString();
		    
		} catch(Exception e){
			Logger.info("WS请求出现异常url："+strURL+"，description:"+ e.getMessage());
			
		}
		
		return response;
	}
	
	/**
	 * "易宝"接口 XML节点添加属性
	 * @param domain商户号
	 * @param req XML格式的字符串
	 * @return
	 * @throws DocumentException
	 */
	public static String addAttribute(String argMerCode, String req){
        Attribute attribute = new DefaultAttribute("platformNo", argMerCode);
		Document document = null;
		
		try {
			document = (Document) DocumentHelper.parseText(req);
		} catch (DocumentException e1) {
			
			Logger.info("XML节点添加属性异常", e1.getMessage());
		} // 将字符串转为XML   

		Element e = document.getRootElement();
		e.add(attribute);

        return document.asXML();
	}
	
	/**
	 * 生成流水号(最长30位)
	 * @param operation
	 * @return
	 */
	public static String createBillNo(long userId, int operation) {
		
		return "" + operation + new Date().getTime();
	}
}
