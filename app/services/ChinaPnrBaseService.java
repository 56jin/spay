package services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import play.Logger;
import play.libs.Codec;
import utils.DateUtil;
import utils.FileUtil;
import chinapnr.SecureLink;

import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import constants.ChinaPnrConstants;
import constants.Constants;

/**
 * 汇付天下支付服务类基类
 * @author yx
 *	@create 2014年12月1日 上午9:59:29
 */
public class ChinaPnrBaseService extends LinkedHashMap<String, String> implements Serializable{
	
	/**
	 * 设值
	 * @param key
	 * @param value
	 * @return
	 */
	public ChinaPnrBaseService putValue(String key,String value){
		put(key, value);
		return this;
	}
	
	/**
	 * 获取key集合
	 * @return
	 */
	private Set<String> getKeys(){
		return keySet();
	}
	
	/**
	 * 校验指令
	 * @return
	 */
	private boolean validateCmdId(){
		String cmdId = get("CmdId");
		if(cmdId==null|"".equals(cmdId))
			return false;
		return true;
	}
	
	/**
	 * 校验必须参数
	 * @return
	 */
	public boolean validateMust(){
		if(validateCmdId()){
			String cmdId = get("CmdId");
			String[] mustKeys = ChinaPnrConfig.getMustKeys(cmdId);
			Set<String> keys = getKeys();
			return keys.containsAll(Arrays.asList(mustKeys));
		}
		return false;
	}
	
	/**
	 * 汇付天下支付接口加密参数
	 * @return
	 */
	public boolean setChkValue(){
		if(!validateCmdId()){
			return false;
		}
		String cmdId = get("CmdId");
		String[] chkKeys = ChinaPnrConfig.getChkValueKeys(cmdId);
		StringBuffer buffer = new StringBuffer();
		if(chkKeys!=null){
			for(String key : chkKeys){
				String value = get(key)==null?"":get(key);
				buffer.append(value);
			}
			
			Logger.debug("---------------ChkValue明文%s---------------", buffer.toString());
			
			SecureLink sl = new SecureLink();
			int ret = 0;
			try {
				
				ret = sl.SignMsg(ChinaPnrConfig.getProperty("chinapnr_merId"), ChinaPnrConfig.getProperty("chinapnr_merKeyFile"), buffer.toString().getBytes("utf-8"));
				
			} catch (UnsupportedEncodingException e) {
				
				Logger.error("ChkValue加密时 %s",e.getMessage());
				
				e.printStackTrace();
			}
			if (ret != 0) {
				return false;
			}
			put("ChkValue", sl.getChkValue());
			
			Logger.debug("---------------ChkValue密文%s---------------", sl.getChkValue());
			
		}
		return true;
	}
	
	/**
	 * 扩展指令(在汇付天下验签回调,可能会一个接口出现多种验签方式,比如交易查询接口中的冻结解冻查询)
	 * @return
	 */
	public String expandCmdId(JsonObject params){
		String cmdId = params.get("CmdId").getAsString();
		if(ChinaPnrConstants.CMD_QUERYTRANSSTAT.equals(cmdId)){
			String queryTransType = params.get("QueryTransType").getAsString();
			if("FREEZE".equals(queryTransType)){
				cmdId = cmdId+"_Freeze";
			}
		}
		return cmdId;
	}
	
	/**
	 * 校验汇付返回参数,检测是否被篡改
	 * @param params
	 * @return
	 */
	private boolean validResp(JsonObject params){
		Logger.debug("------------------validResp start-------------------");
		String cmdId = expandCmdId(params);
		Logger.debug("------------------CmdId:%s-------------------",cmdId);
		
		String[] keys = ChinaPnrConfig.getRespChkValueKeys(cmdId);
		Logger.debug("------------------ChkValueKeys:%s-------------------",Arrays.toString(keys));
		String value = "";
		StringBuffer buffer = new StringBuffer();
		
		for(String key : keys ){
			Logger.debug("%s : %s", key,params.get(key));
			try {
				//GooleJson可能会解析出JsonNull,该对象不与null相等
				value = "".equals(params.get(key) instanceof JsonNull?"":params.get(key).getAsString())?"":URLDecoder.decode(params.get(key).getAsString(),"utf-8");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
			buffer.append(value);
		}
		
		String chkValue = params.get("ChkValue").getAsString();
		
		boolean flag = false;
		try {
			Logger.debug("---------------validResp chkValue明文：%s", buffer.toString());
			flag = SignUtils.verifyByRSA(buffer.toString(), chkValue);

		} catch (Exception e) {

			e.printStackTrace();

		}
		if (!flag) {

			Logger.error("汇付天下回调签名验证失败");
			return false;

		}
		
		return true;
	}
	
	/**
	 * http请求
	 * @return
	 */
	protected String http(){
		Logger.debug("------------------http start-------------------");
		if(!setChkValue()){
			Logger.error("------------------发送请求签名错误-------------------");
			return "{\"RespCode\":\"999\",\"RespDesc\":\"发送请求签名错误\"}"; 
		}
		
		if(!validateMust()){
			Logger.error("------------------缺少必须参数-------------------");
			return "{\"RespCode\":\"999\",\"RespDesc\":\"缺少必须参数\"}";  
		}
		
		String result = null;
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        Set<Entry<String, String>> entrySet = entrySet();
		
		if (entrySet != null) {
			
				for (Entry<String, String> e : entrySet) {
					nvps.add(new BasicNameValuePair(e.getKey(), e.getValue()));
				}
				
		}
		Logger.info("------------------ReqParams%s-------------------",nvps.toString());
		
		writeReqParams(nvps.toString());
		
        CloseableHttpClient httpclient = HttpClients.createDefault();
        EntityBuilder builder = EntityBuilder.create();
        
        //HttpClient进行ws请求start
        try {
        	
            HttpPost httpPost = new HttpPost(ChinaPnrConstants.DEV_URL);
            builder.setParameters(nvps);
            httpPost.setEntity(builder.build());
            CloseableHttpResponse response = null;
            
			try {
				
				response = httpclient.execute(httpPost);
				
			} catch (ClientProtocolException e) {
				
				Logger.error("HttpClient : %s", e.getMessage());
			} catch (IOException e) {
				
				Logger.error("流解析时 : %s", e.getMessage());
			}

            try {
            	
                HttpEntity entity = response.getEntity();
                
                if (response.getStatusLine().getReasonPhrase().equals("OK")
                    && response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                    try {
                    	
						result = EntityUtils.toString(entity, "UTF-8");
						
					} catch (ParseException e) {
						
						Logger.error("HttpClient : %s", e.getMessage());
					} catch (IOException e) {

						Logger.error("流解析时 : %s", e.getMessage());
					}
                }
                try {
					EntityUtils.consume(entity);
				} catch (IOException e) {

					Logger.error("流解析时 : %s", e.getMessage());
				}
            } finally {
                try {
					response.close();
				} catch (IOException e) {

					Logger.error("关闭流时 : %s", e.getMessage());
				}
            }
        } finally {
            try {
				httpclient.close();
			} catch (IOException e) {

				Logger.error("流解析时 : %s", e.getMessage());
			}
        }
      //HttpClient进行ws请求end
        Logger.info("------------------results %s-------------------",result);
        
        JsonParser parser = new JsonParser();
        JsonObject jsonObj = parser.parse(result).getAsJsonObject();
        if(!validResp(jsonObj)){
        	return "{\"RespCode\":\"999\",\"RespDesc\":\"汇付天下回调签名验证失败\"}";
        }
        
//        writeRespParams(result);
        
        Logger.debug("------------------http end-------------------");
        return result;
	}
	
	/**
	 * 写入请求参数至日志
	 * @param value
	 */
	public static void writeReqParams(String value){
		StringBuffer buffer = new StringBuffer();
		buffer.append("------------------------request params start------------------------");
		buffer.append("\r\n");
		buffer.append(value);
		buffer.append("\r\n");
		buffer.append("------------------------request params end------------------------");
		buffer.append("\r\n");
		writeLogs(buffer.toString());
	}
	
	/**
	 * 写入响应至日志
	 * @param value
	 */
	public static void writeRespParams(String value){
		StringBuffer buffer = new StringBuffer();
		buffer.append("------------------------response params start------------------------");
		buffer.append("\r\n");
		buffer.append(value);
		buffer.append("\r\n");
		buffer.append("------------------------response params end------------------------");
		buffer.append("\r\n");
		writeLogs(buffer.toString());
	}
	
	/**
	 * P2P平台对接加密参数
	 */
	public void setPsign(String[] keysArr){
		String[] keys = keysArr;
		StringBuffer buffer = new StringBuffer();
		String value = "";
		for(String key : keys){
			value = get(key)==null?"":get(key);
			buffer.append(value);
		}
		buffer.append(Constants.ENCRYPTION_KEY);
		
		Logger.debug("------resp Psign明文 :%s", buffer.toString());
		
		String pSign = Codec.hexMD5(buffer.toString());
		put("pSign", pSign);
	}
	
	/**
	 * 记录日志
	 * @param value
	 */
	public static void writeLogs(String value){/*
		String parentPath = ChinaPnrConstants.LOGFILEROOT + FileUtil.getPathByCurrentDate();
		String filePath = parentPath+ File.separator + DateUtil.getDate()+ ".logs";
		Logger.info("writeLogs -->parentPath:%s", parentPath);
		Logger.info("writeLogs -->filePath:%s", filePath);
		try{
			
			FileUtil.mkDir(parentPath);
			
		}catch(Exception e){
			
			Logger.error("创建文件夹时%s", e.getMessage());
			return;
		}
		
		File file = new File(filePath);
		
		Logger.info("file getAbsoluteFile path : %s",file.getAbsoluteFile());
		try {
			IOUtils.write(value, new FileOutputStream(file,true));
		} catch (FileNotFoundException e) {
			
			Logger.error("文件写入时:%s", e.getMessage());
		} catch (IOException e) {
			
			Logger.error("文件写入时:%s", e.getMessage());
		}
	*/}

}
