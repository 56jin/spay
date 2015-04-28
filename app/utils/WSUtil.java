package utils;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import play.Logger;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.libs.WS.WSRequest;

/**
 * WS提交报异常，重试3次
 * @author zhs
 * @date 2015-1-21 下午11:53:51
 */
public class WSUtil {

	/**
	 * WS提交方法封装
	 * @param url
	 * @param args
	 * @return
	 */
	public static String WSSubmit(String url, Map<String, String> args){
		String result = null;
		
		try {
			result = WS.url(url).setParameters(args).post().getString();
		    
		} catch(Exception e){
			Logger.info("WS请求出现异常url："+url+"--description:"+ e.getMessage());
		}
				
		return result;
	}
	
	/**
	 * WS提交方法封装
	 * @param url
	 * @param args
	 * @return
	 */
	public static HttpResponse WSSubmitResponse(String url){
		return WSSubmitResponse(url,null);
	}
	
	/**
	 * WS提交方法封装
	 * @param url
	 * @param args
	 * @return
	 */
	public static HttpResponse WSSubmitResponse(String url, Map<String, String> args){
		HttpResponse result = null;
		String timer = "60s";
		try {
			WSRequest request = WS.url(url);
			if(args!=null){
				request.setParameters(args);
			}
			
			result= request.timeout(timer).post();	
		} catch(Exception e){
			if(e instanceof RuntimeException){
				Logger.info("WS请求%s超时异常：%s",timer,e.getMessage());
			}
				Logger.info("WS请求出现异常  url："+url+"+\n   description:"+ e.getMessage());
			
		}
		
		return result;
	}
	
	/**
	 * WS提交方法封装
	 * @param url
	 * @param args
	 * @return
	 */
	public static HttpResponse WSSubmitRequest(String url, Map<String, Object> args){
		HttpResponse response = null;
		
		
		try {
			WSRequest request = null;
			request= WS.url(url);
	
			Map<String, String> header = new HashMap<String, String>();
			header.put("Content-Type","application/x-www-form-urlencoded;charset=utf-8");
			request.headers = header;
			
			response = request.params(args).post();
		    
		} catch(Exception e){
			Logger.info("WS请求出现异常", "url："+url+"，description:"+ e.getMessage());
			
		}
		
		return response;
	}
}
