package services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import play.Logger;

import com.shove.security.Encrypt;

import business.DealDetail;
import business.Member;
import business.MemberOfPlatform;

import constants.Constants;
import constants.IPSConstants;
import constants.YEEConstants;
import constants.YEEConstants.QueryType;
import constants.YEEConstants.Status;
import controllers.YEE.SecureSign.SignUtil;


import models.t_member_events;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import utils.Converter;
import utils.EmailUtil;
import utils.ErrorInfo;
import utils.WSUtil;
import utils.YEEUtil;

/**
 * 易宝支付接口处理
 * @author zhs
 * @date 2014-11-29 上午10:19:36
 */
public class YEE {

	/**
	 * 转化参数提交给第三方
	 * @param type  操作类型
	 * @param platformId  平台id
	 * @param platformMemberId  会员id
	 * @param platformUsername  会员名称
	 * @param argMerCode  商户号
	 * @param json  P2P传过来的数据
	 * @param summary  需要备份到数据库的数据
	 * @param jsonXtraPara  P2P传过来的数据
	 * @param error
	 * @return
	 */
	public static Map<String, String> entrance(int type, int platformId, long platformMemberId, String platformUsername, 
			String argMerCode, JSONObject json, JSONObject jsonMark, JSONObject jsonXtraPara, long memberId, String memberName, String domain, ErrorInfo error) {
		Map<String, String> argsMap = new HashMap<String, String>();
		String req = null;
		String url = null;
		
		//提现之前要先绑卡
		if(YEEConstants.WITHDRAWAL == type){
			int status = MemberOfPlatform.BindCardStatus(platformMemberId, error);
			
			if(status == YEEConstants.CARD_NO_BANG){
				type = YEEConstants.BOUND_CARD;
				jsonMark.put("type", type);
			}
		}
		
		jsonMark.put("pWebUrl", json.getString("pWebUrl"));  
		String summary = jsonMark.toString();
		
		url = YEEConstants.IPS_URL_TEST[type];
		
		switch(type){
		case YEEConstants.CREATE_ACCOUNT:
			req = register(type, platformId, platformMemberId, json, argMerCode, summary, memberId, memberName, domain, error);
		    break;
		case YEEConstants.REGISTER_CREDITOR:
		    req  = investBid(platformId, platformMemberId, json, argMerCode, summary, jsonXtraPara, error);
		    break;
		case YEEConstants.REGISTER_CRETANSFER:
			req = transfer(platformId, platformMemberId, json, argMerCode, summary, error);
			break;
		case YEEConstants.AUTO_SIGING:
			req  = autoBid(type, platformId, platformMemberId, json, argMerCode, summary, error);
			break;
		case YEEConstants.RECHARGE:
		    req  = recharge(platformId, platformMemberId, json, argMerCode, summary, error);
		    break;
		case YEEConstants.REPAYMENT:
			req = repayment(platformId, platformMemberId, json, argMerCode, summary, error);
			break;
		case YEEConstants.WITHDRAWAL:
			req = withdraw(platformId, platformMemberId, json, argMerCode, summary, error);
			break;
		case YEEConstants.BOUND_CARD:
			req = bindBankCard(type, platformId, platformMemberId, json, argMerCode, summary, error);
			break;
		case YEEConstants.TRANSFER_USER_TO_MER:
			req = userToMerchant(platformId, platformMemberId, json, jsonXtraPara, argMerCode, summary, error);
			break;
		}
		
		/*将使用平台的请求信息转化成第三方支付的请求参数*/
        String sign = SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS);
        
        argsMap.put("req", req);
        argsMap.put("sign", sign);
        argsMap.put("url", url);
        argsMap.put("redictMark", "1");  //登记标的接口易宝没有，故用该字段在页面区别提交
        
		return argsMap;
	}
	
	/**
	 * 处理第三方传过来的参数，然后处理并请求给P2P（异步回调）
	 * @param resp
	 * @param sign
	 * @return
	 */
	public static Map<String, String> notifyExit(String resp, ErrorInfo error){
		error.clear();
		JSONObject json = (JSONObject)Converter.xmlToObj(resp);
		Map<String, String> args = new HashMap<String, String>();
		Map<String, String> remarkMap = null;
		
		if(json.containsKey("@platformNo")){
			json.put("platformNo", json.getString("@platformNo"));
		}
		
		//查询备份数据库的数据
		if(null != DealDetail.queryEvents(json.getString("requestNo"), error)){
			remarkMap = DealDetail.queryEvents(json.getString("requestNo"), error);
			
		}else{
			remarkMap = DealDetail.queryDetails(json.getString("requestNo"), error);
			
		}
		
		int type = Integer.parseInt(remarkMap.get("type"));
		boolean BcarStatus = false;
		
		switch(type){
		case YEEConstants.CREATE_ACCOUNT:
			args = registerCall(json, remarkMap);
			break;
		case YEEConstants.REGISTER_CREDITOR:
			args = investBidCall(json, remarkMap);
			break;
		case YEEConstants.REGISTER_CRETANSFER:
			args = transferCall(json, remarkMap);
			break;
		case YEEConstants.AUTO_SIGING:
			args = autoInvestBidCall(json, remarkMap);
			break;
		case YEEConstants.REPAYMENT_SIGNING:
			args = autoPaymentCall(json, remarkMap);
			break;
		case YEEConstants.RECHARGE:
			args = rechargeCall(json, remarkMap.get("amount"));
			break;
		case YEEConstants.REPAYMENT:
			args = PaymentCall(json, remarkMap);
			break;
		case YEEConstants.WITHDRAWAL:
			args = withdrawCall(json, remarkMap);
			break;
		case YEEConstants.BOUND_CARD:
			BcarStatus = updateCardStatus(json,remarkMap, error);
		    break;
		case YEEConstants.TRANSFER_USER_TO_MER:
			args = userToMerCall(json, remarkMap);
		    break;
		case YEEConstants.TRANSFER_MER_TO_USERS:
			args = merToUserCall(json, remarkMap);
		    break;
		case YEEConstants.TRANSFER_MER_TO_USER:
			args = merToUserCall(json, remarkMap);
		    break;
		case YEEConstants.UNFREZZ_AMOUNT:
			args = unfrezzCall(json, remarkMap);
		    break;
		}
		
		//绑卡后返回一个单独页面
		if(type == YEEConstants.BOUND_CARD){
			if(BcarStatus){
				args.put("BcarStatus", "1");
			}else{
				args.put("BcarStatus", "2");
			}
			
			return args;
		}
		
		if(null == args){
			return null;
		}
		
		String result = null;
		JSONObject jsonReturn = null;
		String returnResult = null; 
		
		//还款，登记债权人，充值，提现先WS后post提交
		if(type == YEEConstants.REPAYMENT || type == YEEConstants.WITHDRAWAL || type == YEEConstants.REGISTER_CREDITOR || type == YEEConstants.RECHARGE){
			result = WSUtil.WSSubmit(remarkMap.get("pS2SUrl"),args);
			
			jsonReturn = JSONObject.fromObject(result);
			returnResult = Encrypt.encrypt3DES(Converter.jsonToXml(jsonReturn.toString(), null, null, null, null), Constants.ENCRYPTION_KEY);
			
			//判断是否满标，如满标，解冻资金
			if(type == YEEConstants.REGISTER_CREDITOR){
				if(jsonReturn.getString("code").equals("-10")){
					String revocationJson = revocationTransfer(json.getInt("platformId"), json.getString("platformNo"), jsonReturn, remarkMap);
					returnResult = Encrypt.encrypt3DES(Converter.jsonToXml(revocationJson, null, null, null, null), Constants.ENCRYPTION_KEY);
					
				}
			}
			
			args.clear();
			args.put("postMark", "true");
			args.put("returnMark", "false");
			args.put("url", jsonReturn.getString("pPostUrl"));
			args.put("result", returnResult);
		
	  }else{
		  result = WSUtil.WSSubmit(remarkMap.get("pS2SUrl"),args);
//		  JSONObject resultJson = JSONObject.fromObject(result);
//		  
//		  Logger.info("---------P2P回调中间件处理结果  "+ resultJson.toString());
		  
//		  if(resultJson.getInt("code") < 0){
//			  args.put("returnMark", "false");
//			  
//		  }else{
//			  args.put("returnMark", "true");
//		  }
		  args.put("returnMark", "true");
		  args.put("postMark", "false");
	  }
		
		return args;
	}
	
	/**
	 * 处理第三方传过来的参数，然后处理并请求给P2P（同步处理 ）
	 * @param resp
	 * @param sign
	 * @return
	 */
	public static Map<String, String> exit(String resp, ErrorInfo error){
		error.clear();
		JSONObject json = (JSONObject)Converter.xmlToObj(resp);
		Map<String, String> args = new HashMap<String, String>();
		Map<String, String> remarkMap = null;
		
		if(json.containsKey("@platformNo")){
			json.put("platformNo", json.getString("@platformNo"));
		}
		
		//查询备份数据库的数据
		if(null != DealDetail.queryEvents(json.getString("requestNo"), error)){
			remarkMap = DealDetail.queryEvents(json.getString("requestNo"), error);
			
		}else{
			remarkMap = DealDetail.queryDetails(json.getString("requestNo"), error);
		}
		
		int type = Integer.parseInt(remarkMap.get("type"));
		boolean BcarStatus = false;
		
		switch(type){
		case YEEConstants.CREATE_ACCOUNT:
			args = registerCall(json, remarkMap);
			break;
		case YEEConstants.REGISTER_CREDITOR:
			args = investBidCall(json, remarkMap);
			break;
		case YEEConstants.REGISTER_CRETANSFER:
			args = transferCall(json, remarkMap);
			break;
		case YEEConstants.AUTO_SIGING:
			args = autoInvestBidCall(json, remarkMap);
			break;
		case YEEConstants.REPAYMENT_SIGNING:
			args = autoPaymentCall(json, remarkMap);
			break;
		case YEEConstants.RECHARGE:
			args = rechargeCall(json, remarkMap.get("amount"));
			break;
		case YEEConstants.REPAYMENT:
			args = PaymentCall(json, remarkMap);
			break;
		case YEEConstants.WITHDRAWAL:
			args = withdrawCall(json, remarkMap);
			break;
		case YEEConstants.BOUND_CARD:
			BcarStatus = updateCardStatus(json, remarkMap, error);
		    break;
		case YEEConstants.TRANSFER_USER_TO_MER:
			args = userToMerCall(json, remarkMap);
		    break;
		case YEEConstants.TRANSFER_MER_TO_USERS:
			args = merToUserCall(json, remarkMap);
		    break;
		case YEEConstants.TRANSFER_MER_TO_USER:
			args = merToUserCall(json, remarkMap);
		    break;
		case YEEConstants.UNFREZZ_AMOUNT:
			args = unfrezzCall(json, remarkMap);
		    break;
		}
		
		//绑卡后返回一个单独页面
		if(type == YEEConstants.BOUND_CARD){
			if(BcarStatus){
				args.put("BcarStatus", "1");
			}else{
				args.put("BcarStatus", "2");
			}
			
			return args;
		}
		
		if(null == args){
			return null;
		}
		
		//还款，登记债权人，充值，提现先WS后post提交
		if(type == YEEConstants.REPAYMENT || type == YEEConstants.WITHDRAWAL || type == YEEConstants.REGISTER_CREDITOR){
			String result = null;
			JSONObject jsonReturn = null;
			String returnResult = null; 
			
			result = WSUtil.WSSubmit(remarkMap.get("pS2SUrl"),args);
			jsonReturn = JSONObject.fromObject(result);
			returnResult = Encrypt.encrypt3DES(Converter.jsonToXml(jsonReturn.toString(), null, null, null, null), Constants.ENCRYPTION_KEY);
			
			//判断是否满标，如满标，解冻资金
			if(type == YEEConstants.REGISTER_CREDITOR){
				if(jsonReturn.getString("code").equals("-10")){
					String revocationJson = revocationTransfer(json.getInt("platformId"), json.getString("platformNo"), jsonReturn, remarkMap);
					returnResult = Encrypt.encrypt3DES(Converter.jsonToXml(revocationJson, null, null, null, null), Constants.ENCRYPTION_KEY);
					
				}
			}
			
			args.clear();
			args.put("postMark", "true");
			args.put("url", jsonReturn.getString("pPostUrl"));
			args.put("result", returnResult);
			
			return args;
		}
		
		args.put("url", remarkMap.get("pWebUrl"));
		args.put("redictMark", "false");
		
		return args;
	}
	
	/**
	 * 修改成绑卡的状态
	 * @param remarkMap
	 * @param error
	 */
	public static boolean updateCardStatus(JSONObject json, Map<String, String> remarkMap, ErrorInfo error){
		boolean rusult = false;
		
		if(json.getInt("code") == 1){
			
			rusult = MemberOfPlatform.updateCardStatus(Long.parseLong(remarkMap.get("memberId")), error);
		}
		
		Logger.info("------------------------修改成绑卡的状态返回结果==:"+rusult+"-------------------------------");
		
		return rusult;
	}
	
	/**
	 * 提现回调P2P
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> withdrawCall(JSONObject json, Map<String, String> remarkMap ){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		Logger.info("------------------------提现回调-------------------------------");
		
		if(json.getString("code").equals("1")){
		    pErrCode = "MG00000F";
            DealDetail.updateStatus(json.getString("requestNo"));
			
		}else{
			pErrCode = "";
		}
		
		jsonOb.put("pMerBillNo", json.getString("requestNo"));
		jsonOb.put("pMemo1", remarkMap.get("memberId"));
		jsonOb.put("pMemo3", remarkMap.get("pMemo3"));
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.info("------------------------提现回调返回结果==:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 * 用户转商户回调P2P
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> userToMerCall(JSONObject json, Map<String, String> remarkMap ){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		Map<String, Object> userMap = new HashMap<String, Object>();
		String pErrCode = "";
		JSONArray jsonArr = null;
		String resultarr = null;
		
		Logger.info("------------------------用户转商户回调json---------"+json.toString());
		
//		if(json.getString("code").equals("1")){
//			Map<String, Object> mapObj = new HashMap<String, Object>();
//        	
//        	jsonOb.put("requestNo", json.getString("requestNo"));
//        	jsonOb.put("mode", "CONFIRM");
//        	jsonOb.put("notifyUrl", Constants.BASE_URL + "/yee/notifys");
//        	
//        	String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
//        	
//        	Logger.info("------------------------用户转商户回调---转账到易宝确认--请求参数：---"+jsonOb.toString());
//        	
//    		req = YEEUtil.addAttribute(json.getString("platformNo"), req);
//    		
//    		mapObj.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
//    		mapObj.put("req", req);
//    		mapObj.put("service", "COMPLETE_TRANSACTION");
//    		
//    		//WS请求直接返回处理结果
//    		resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, mapObj);
//    		
//    		Logger.info("------------------------用户转商户回调---转账到易宝确认--易宝返回结果：---"+resultarr.toString());
//    		
//    		JSONObject jsonResult = (JSONObject)Converter.xmlToObj(resultarr);
//    		
//    		if(jsonResult.getInt("code") == 1){
//    			pErrCode = "MG00000F";
//    			
//    		}
//		}
		
		if(json.getString("code").equals("1")){
			userMap.clear();
	        userMap.put("requestNo", json.getString("requestNo"));  
			
	        jsonOb.clear();
			userMap.put("mode", "CP_TRANSACTION");  //查询模式
			jsonOb.putAll(userMap);
			
			String ueryReq = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
			ueryReq = YEEUtil.addAttribute(json.getString("platformNo"), ueryReq);
			
			userMap.clear();
			userMap.put("sign",SignUtil.sign(ueryReq, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
			userMap.put("req", ueryReq);
			userMap.put("service", "QUERY");
			
			resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, userMap);
			Logger.info("-------------查询用户转商户ws易宝，易宝返回处理结果resultarr------"+resultarr.toString()+"\n ");
			
			/*----------------------------处理返回的数据回调P2P--------------------------------------------------------------*///系统异常，异常编号
			JSONObject queryResult = (JSONObject)Converter.xmlToObj(resultarr);
			Object pDetails = queryResult.get("records");//节点数组
			
			if(!pDetails.toString().equals("[]")){
				if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
					JSONObject pDetail = (JSONObject)pDetails; 
					JSONObject record = pDetail.getJSONObject("record"); 
			
					jsonArr = new JSONArray(); 
					jsonArr.add(record); 
				} else {
					jsonArr = queryResult.getJSONArray("records");
					
				} 
				JSONObject pRow = jsonArr.getJSONObject(0);
				
				if(pRow.getString("status").equals("PREAUTH")){
					Logger.info("------------------------用户转商户回调---转账到易宝确认-------------------------------");
		        	Map<String, Object> mapObj = new HashMap<String, Object>();
		        	
		        	jsonOb.put("requestNo", json.getString("requestNo"));
		        	jsonOb.put("mode", "CONFIRM");
		        	jsonOb.put("notifyUrl", Constants.BASE_URL + "/yee/notifys");
		        	
		        	String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
		        	
		        	Logger.info("------------------------用户转商户回调---转账到易宝确认--请求参数：---"+jsonOb.toString());
		        	
		    		req = YEEUtil.addAttribute(json.getString("platformNo"), req);
		    		
		    		mapObj.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
		    		mapObj.put("req", req);
		    		mapObj.put("service", "COMPLETE_TRANSACTION");
		    		
		    		//WS请求直接返回处理结果
		    		resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, mapObj);
		    		
		    		Logger.info("------------------------用户转商户回调---转账到易宝确认--易宝返回结果：---"+resultarr.toString());
		    		
		    		JSONObject jsonResult = (JSONObject)Converter.xmlToObj(resultarr);
		    		
		    		if(jsonResult.getInt("code") == 1){
		    			pErrCode = "MG00000F";
		    			
		    		}
				}else if(pRow.getString("status").equals("CONFIRM")){  //已确认出款
					pErrCode = "MG00000F";
				}
			}
		}
		
		jsonOb.clear();
		jsonOb.put("pMerBillNo", json.getString("requestNo"));  //商户流水号
		jsonOb.put("TransAmt", remarkMap.get("amount"));  //交易金额
		jsonOb.put("pMemo1", remarkMap.get("pMemo1"));  
		jsonOb.put("pMemo3", remarkMap.get("pMemo3"));  
		jsonOb.put("UsrCustId", json.getString("requestNo"));  //入账客户号
		jsonOb.put("pTransferType", String.valueOf(YEEConstants.COMPENSATE_REPAYMENT));  //入账客户号
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.info("------------------------用户转商户回调返回结果==:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 * 取消投标--解冻资金回调P2P
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> unfrezzCall(JSONObject json, Map<String, String> remarkMap ){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		Logger.info("------------------------取消投标解冻资金回调------------------------------");
		
		if(json.getString("code").equals("1")){
		    pErrCode = "MG00000F";
		    DealDetail.updateStatus(json.getString("requestNo"));
			
		}else{
			pErrCode = "";
			 
		}
		
		jsonOb.put("pMerBillNo", json.getString("requestNo"));  //商户流水号
		jsonOb.put("TransAmt", remarkMap.get("amount"));  //交易金额
		jsonOb.put("UsrCustId", json.getString("requestNo"));  //入账客户号
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.info("------------------------取消投标解冻资金回调返回结果==:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 * 商户转用户回调P2P
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> merToUserCall(JSONObject json, Map<String, String> remarkMap ){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		Logger.info("------------------------商户转用户回调-------------------------------");
		
		if(json.getString("code").equals("1")){
		    pErrCode = "MG00000F";
		    DealDetail.updateStatus(json.getString("requestNo"));
			
		}else{
			pErrCode = "";
			 
		}
		
		jsonOb.put("pMerBillNo", json.getString("requestNo"));  //商户流水号
		jsonOb.put("pMemo1", remarkMap.get("pMemo1"));  //交易金额
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.info("------------------------商户转用户回调返回结果==:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 * 开户回调P2P
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> registerCall(JSONObject json, Map<String, String> remarkMap ){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		Logger.info("------------------------开户回调-------------------------------");
		
		if(json.getString("code").equals("1") || json.getString("code").equals("101")){
			jsonOb.put("pStatus", "10");
		    pErrCode = "MG00000F";
		    
		    t_member_events event = DealDetail.updateEvent(json.getString("requestNo"), "pErrCode:"+json.getString("code"));
			
			if(null == event) {
				return null;
			}
			
		}else{
			jsonOb.put("pStatus", "9");
			pErrCode = "";
			 
		}
		
		jsonOb.put("pMerBillNo", json.getString("requestNo"));
		jsonOb.put("pIpsAcctNo", remarkMap.get("memberId"));
		jsonOb.put("pMemo1", remarkMap.get("memberId"));
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.info("------------------------开户回调返回结果==:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 *标的登记回调P2P
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> bidCall(String argMerCode, JSONObject json, JSONObject jsonXtraPara, long menberId){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		jsonOb.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonOb.put("pBidNo", json.getString("pBidNo"));
		jsonOb.put("pOperationType", json.getString("pOperationType"));
		jsonOb.put("pMemo3", json.getString("pMemo3"));
		jsonOb.put("pMemo1", menberId+"");
		jsonOb.put("pIpsBillNo", "");
		
		Logger.info("------------------------标的登记回调-------------------------------");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", argMerCode);
		map.put("pErrMsg", "");
		map.put("pErrCode", "MG00000F");
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( argMerCode+"MG00000F"+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.info("------------------------标的登记回调==:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 *充值回调P2P（pd测试的时候用）
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> rechargeCallTest(String argMerCode, JSONObject json, JSONObject jsonXtraPara, long menberId){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		jsonOb.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonOb.put("pTrdAmt", json.getString("pTrdAmt"));
		jsonOb.put("pMemo1", menberId+"");
		
		Logger.info("------------------------充值（pd测试的时候用）回调-------------------------------");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", argMerCode);
		map.put("pErrMsg", "");
		map.put("pErrCode", "MG00000F");
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( argMerCode+"MG00000F"+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.info("------------------------充值（pd测试的时候用）回调==:"+map+"-------------------------------");
		
//		String jsonResult = WSUtil.WSSubmit(jsonXtraPara.getString("pWSUrl"),map);
//		JSONObject jsonReturn = JSONObject.fromObject(jsonResult);
//		
//		Logger.info("------------------------充值WS回调P2P获取返回信息jsonReturn=:"+jsonReturn.toString()+"-------------------------------");
//		
//		String result = Encrypt.encrypt3DES(Converter.jsonToXml(jsonReturn.toString(), null, null, null, null), Constants.ENCRYPTION_KEY);
//		
//		map.clear();
		map.put("url", json.getString("pWebUrl"));
//		map.put("result", result);
		map.put("redictMark", "2");
		
		return map;
	}
	
	/**
	 *流标（WS）
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> flowCall(String argMerCode, int platformId, int type, JSONObject json, long menberId,
			JSONObject jsonXtraPara, String summary){
		JSONObject jsonOb = new JSONObject();
		Map<String, String> map = new HashMap<String, String>();
		Map<String, Object> userMap = new HashMap<String, Object>();
		String pErrCode = "";
		int hasPublished = 0;
		JSONArray jsonArr = null;
		String resultarr = null;
		
		
        Object pDetails = jsonXtraPara.get("pDetails");
        
        Logger.info("-------------流标请求易宝参数json=:"+json.toString()+"\n-------------流标请求易宝额外参数json=:"+jsonXtraPara.toString());
        
		if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
			JSONObject pDetail = (JSONObject)pDetails; 
			JSONObject pRow = pDetail.getJSONObject("pRow"); 
	
			jsonArr = new JSONArray(); 
			jsonArr.add(pRow); 
		} else {
			jsonArr = jsonXtraPara.getJSONArray("pDetails");
		} 
		
		int size = jsonArr.size();
		
		//遍历数组jsonArr再重新赋值到一个新的数组jsonArry
		for (Object obj : jsonArr) {
			JSONObject pRow = (JSONObject)obj;
			JSONObject jsonObj = new JSONObject();
        	Map<String, Object> mapObj = new HashMap<String, Object>();
        	                                         
        	jsonObj.put("requestNo", pRow.getString("ipsBillNo"));
        	jsonObj.put("platformUserNo", pRow.getString("investUserId"));
        	
        	String req = Converter.jsonToXml(jsonObj.toString(), "request", null, null, null);
    		req = YEEUtil.addAttribute(argMerCode, req);
    		
    		mapObj.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
    		mapObj.put("req", req);
    		mapObj.put("service", "REVOCATION_TRANSFER");
    		
    		//WS请求直接返回处理结果
    		resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, mapObj);
    		
    		//判断通信是否有异常，如果有异常，回调P2P失败值
    		if(null == resultarr){
    			String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
    			map.put("pMerCode", argMerCode);
    			map.put("pErrMsg", "");
    			map.put("pErrCode", "MG02035F");
    			map.put("p3DesXmlPara", p3DesXmlPara);
    			map.put("pSign", Encrypt.MD5( argMerCode+"MG02035F"+p3DesXmlPara+Constants.ENCRYPTION_KEY));
    			
    			Logger.info("------------------------流标请求易宝异常-------------------------------");
    			
    			return map;
    		}
    		
    		DealDetail.addEvent(pRow.getLong("investUserId"), type+200, (long)platformId, pRow.getString("ipsBillNo"), null, null, summary, "流标");
    		
    		JSONObject jsonResult = (JSONObject)Converter.xmlToObj(resultarr);
    		
    		Logger.info("------------------------流标第三方返回结果jsonResult=:"+jsonResult+"-------------------------------");
    		
			if(jsonResult.containsKey("@platformNo")){
				jsonResult.put("platformNo", jsonResult.getString("@platformNo"));
			}
    		
    		if(jsonResult.getString("code").equals("1")){
    			hasPublished++;
    			
    		 }else{
				userMap.clear();
		        userMap.put("requestNo", pRow.getString("ipsBillNo"));  //需要投标流水号中一个
				
		        jsonOb.clear();
				userMap.put("mode", "PAYMENT_RECORD");  //查询模式
				jsonOb.putAll(userMap);
				
				String ueryReq = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
				ueryReq = YEEUtil.addAttribute(argMerCode, ueryReq);
				
				userMap.clear();
				userMap.put("sign",SignUtil.sign(ueryReq, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
				userMap.put("req", ueryReq);
				userMap.put("service", "QUERY");
				
				resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, userMap);
				
				/*----------------------------处理返回的数据回调P2P--------------------------------------------------------------*///系统异常，异常编号
				JSONObject queryResult = (JSONObject)Converter.xmlToObj(resultarr);
				pDetails = queryResult.get("records");//节点数组
				
				if(!pDetails.toString().equals("[]")){
					if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
						JSONObject pDetail = (JSONObject)pDetails; 
						JSONObject record = pDetail.getJSONObject("record"); 
				
						jsonArr = new JSONArray(); 
						jsonArr.add(record); 
					} else {
						jsonArr = queryResult.getJSONArray("records");
						
					} 
					pRow = jsonArr.getJSONObject(0);
					
					if(pRow.getString("status").equals("CANCEL")){
						hasPublished++;
					}
				}
			 }
		}
		
        if(hasPublished == size){
        	pErrCode = "MG00000F";
        	
        }else{
        	pErrCode = "MG02035F";
        }
        
        jsonOb.put("pOperationType", json.get("pOperationType"));
        jsonOb.put("pMerBillNo", json.get("pMerBillNo"));
        jsonOb.put("pIpsBillNo", jsonXtraPara.getString("freezeTrxId"));
        jsonOb.put("pMemo3", json.get("pMemo3"));
        jsonOb.put("pBidNo", json.get("pBidNo"));
        jsonOb.put("pMemo1", menberId+"");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.clear();
		map.put("pMerCode", argMerCode);
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( argMerCode+pErrCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.info("------------------------流标回调P2P参数map=:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 * 登记债权人回调P2P
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> investBidCall(JSONObject json, Map<String, String> remarkMap ){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = "MG02504F";
		
		Logger.info("------------------------登记债权人回调-------------------------------");
		
		if(json.getString("code").equals("1")){
		    pErrCode = "MG00000F";
		    DealDetail.updateStatus(json.getString("requestNo"));
			
		}
		
		jsonOb.put("pMerBillNo", json.getString("requestNo"));
		jsonOb.put("pP2PBillNo", json.getString("requestNo"));//该字段在P2P端需要用到，易宝本身没有用到
		jsonOb.put("oldMerBillNo", json.getString("requestNo"));
		jsonOb.put("pMemo1", remarkMap.get("memberId"));
		jsonOb.put("pFee", "0");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.info("------------------------登记债权人回调map=:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 * 还款回调P2P
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> PaymentCall(JSONObject json, Map<String, String> remarkMap){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		Map<String, Object> userMap = new HashMap<String, Object>();
		JSONArray jsonArr = null;
		String resultarr = null;
		
		String pErrCode = "";
		
		Logger.info("------------------------还款回调-------------------------------");
		
		if(json.getString("code").equals("1")){
		    pErrCode = "MG00000F";
		    
		}else{
			userMap.clear();
			JSONObject pRow = new JSONObject();
	        userMap.put("requestNo", remarkMap.get("pMerBillNo"));  
			
	        jsonOb.clear();
			userMap.put("mode", "REPAYMENT_RECORD");  //查询模式
			jsonOb.putAll(userMap);
			
			String ueryReq = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
			ueryReq = YEEUtil.addAttribute(json.getString("platformNo"), ueryReq);
			
			userMap.clear();
			userMap.put("sign",SignUtil.sign(ueryReq, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
			userMap.put("req", ueryReq);
			userMap.put("service", "QUERY");
			
			resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, userMap);
			
			/*----------------------------处理返回的数据回调P2P--------------------------------------------------------------*///系统异常，异常编号
			JSONObject queryResult = (JSONObject)Converter.xmlToObj(resultarr);
			Object pDetails = queryResult.get("records");//节点数组
			
			if(!pDetails.toString().equals("[]")){
				if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
					JSONObject pDetail = (JSONObject)pDetails; 
					JSONObject record = pDetail.getJSONObject("record"); 
			
					jsonArr = new JSONArray(); 
					jsonArr.add(record); 
				} else {
					jsonArr = queryResult.getJSONArray("records");
					
				} 
				pRow = jsonArr.getJSONObject(0);
				
				if(pRow.getString("status").equals("SUCCESS")){
					pErrCode = "MG00000F";
				}
			}
		 }
		
		jsonOb.put("pMemo1", remarkMap.get("memberId"));//还款人id
		jsonOb.put("pMemo3",remarkMap.get("pMemo3"));
		jsonOb.put("pMerBillNo",json.getString("requestNo"));
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.info("------------------------还款回调P2P参数map=:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 * “自动投标授权”回调P2P
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> autoInvestBidCall(JSONObject json, Map<String, String> remarkMap ){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		Logger.info("------------------------自动投标授权p3DesXmlPara=:"+jsonOb.toString()+"-------------------------------");
		
		if(json.getString("code").equals("1")){
		    pErrCode = "MG00000F";
            t_member_events event = DealDetail.updateEvent(json.getString("requestNo"), "pErrCode:"+json.getString("code"));
			
			if(null == event) {
				return null;
			}
		    
		}else{
			pErrCode = "";
			 
		}
		
		jsonOb.put("pMerBillNo", json.getString("requestNo"));
		jsonOb.put("pIpsAuthNo", json.getString("requestNo"));//本字段是p2p方用来标记“环迅”接口，此处用流水号代替
		jsonOb.put("pMemo1", remarkMap.get("memberId"));
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.info("------------------------自动投标授权map=:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 * “自动还款签约”回调P2P
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> autoPaymentCall(JSONObject json, Map<String, String> remarkMap ){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		if(json.getString("code").equals("1")){
		    pErrCode = "MG00000F";
			
		}else{
			pErrCode = "";
			 
		}
		
		jsonOb.put("pMerBillNo", json.getString("requestNo"));
		jsonOb.put("pIpsAuthNo", json.getString("requestNo"));//本字段是p2p方用来标记“环迅”接口，此处用流水号代替
		jsonOb.put("pMemo1", remarkMap.get("memberId"));
		
		Logger.info("------------------------自动投标授权回调-------------------------------");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.info("------------------------自动投标授权回调map=:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 * 登记债权转让回调P2P
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> transferCall(JSONObject json, Map<String, String> remarkMap){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		Logger.info("------------------------登记债权转让回调-------------------------------");
		
		if(json.getString("code").equals("1")){
		    pErrCode = "MG00000F";
		    DealDetail.updateStatus(json.getString("requestNo"));
		}else{
			pErrCode = "";
			 
		}
		
		jsonOb.put("pMerBillNo", json.getString("requestNo"));
		jsonOb.put("pMemo1", remarkMap.get("memberId"));
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.info("------------------------登记债权转让回调map=:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 * 充值回调P2P
	 * @param json
	 * @param remarkMap
	 * @return
	 */
	public static Map<String, String> rechargeCall(JSONObject json, String amount){
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		String pErrCode = null;
		
		if(json.getString("code").equals("1")){
		    pErrCode = "MG00000F";
		    DealDetail.updateStatus(json.getString("requestNo"));
		}else{
			pErrCode = "";
			 
		}
		
		jsonOb.put("pMerBillNo", json.getString("requestNo"));
		jsonOb.put("pTrdAmt", amount);
		
		Logger.info("------------------------充值回调-------------------------------");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		map.put("pMerCode", json.getString("platformNo"));
		map.put("pErrMsg", "");
		map.put("pErrCode", pErrCode);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", Encrypt.MD5( json.getString("platformNo")+pErrCode+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.info("------------------------充值回调map=:"+map+"-------------------------------");
		
		return map;
	}
	
	/**
	 * 用户注册
	 * @param platformMemberId 会员在P2P平台的唯一标识 
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String register(int type, int platformId, long platformMemberId, JSONObject json, String argMerCode,
			String summary, long memberId, String memberName, String domain, ErrorInfo error){
		error.clear();
		Map<String, String> userMap = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject(); 
		
		isRegister(json, platformId, memberId, memberName, domain, error);
		
		if(error.code == -1){
			return null;
		}
		
		Logger.info("------------------------用户注册P2P参数json=:"+json.toString()+"-------------------------------");
		
		userMap.put("platformUserNo", Long.toString(platformMemberId));
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("realName", json.getString("pRealName"));
		userMap.put("idCardType", "G2_IDCARD");
		userMap.put("idCardNo", json.getString("pIdentNo"));
		userMap.put("mobile", json.getString("pMobileNo"));
		userMap.put("email", json.getString("pEmail"));      
		userMap.put("callbackUrl", Constants.BASE_URL + "/yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "/yee/notifys");
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		DealDetail.addEvent(platformMemberId, type+200, platformId, json.getString("pMerBillNo"), null, null, summary, "开户");
		
		Logger.info("------------------------用户注册请求易宝参数req=:"+req+"-------------------------------");
		
		return req;
	}
	
	/**
	 * 用户充值
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String recharge(int platformId, long platformMemberId, JSONObject json,
			String argMerCode, String summary, ErrorInfo error){
		Map<String, String> userMap = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject();
		
		Logger.info("------------------------用户充值参数json=:"+json.toString()+"-------------------------------");
		
		userMap.put("platformUserNo", Long.toString(platformMemberId));
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("amount", json.getString("pTrdAmt"));
		userMap.put("feeMode", "PLATFORM");  //费率模式
		userMap.put("callbackUrl", Constants.BASE_URL + "/yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "/yee/notifys");
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				YEEConstants.RECHARGE, json.getDouble("pTrdAmt"), false, summary);
		
		if(!detail.addDealDetail()){
			error.code = -1;
			error.msg = "数据库异常，导致添加充值交易记录失败";
			
			return null;
		}
		
		Logger.info("------------------------用户充值请求易宝req=:"+req+"-------------------------------");
		
		return req;
	}
	
	/**
	 * 绑卡
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String bindBankCard(int type, int platformId, long platformMemberId, JSONObject json, String argMerCode,
			String summary, ErrorInfo error){
		error.clear();
		Map<String, String> userMap = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject(); 
		
		Logger.info("------------------------绑卡P2P参数json=:"+json.toString()+"-------------------------------");
		
		userMap.put("platformUserNo", Long.toString(platformMemberId));
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("callbackUrl", Constants.BASE_URL + "/yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "/yee/notifys");
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		DealDetail.addEvent(platformMemberId, type+200, platformId, json.getString("pMerBillNo"), null, null, summary, "绑卡");
		
		Logger.info("------------------------绑卡请求易宝参数req=:"+req+"-------------------------------");
		
		return req;
	}
	
	/**
	 * 取消绑卡
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String unBindBankCard(long platformMemberId, JSONObject json, String argMerCode){
		Map<String, String> userMap = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject(); 
		
		userMap.put("platformUserNo", Long.toString(platformMemberId));
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("callbackUrl", Constants.BASE_URL + "/yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "/yee/notifys");
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		return req;
	}
	
	/**
	 * 债权转让(转让成功)--对应P2P的登记债权转让接口
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String transfer(int platformId, long platformMemberId, JSONObject json,
			String argMerCode, String summary, ErrorInfo error){
		Map<String, String> userMap = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject(); 
		
		Logger.info("------------------------债权转让P2P参数json=:"+json.toString()+"-------------------------------");
		
		userMap.put("platformUserNo", Long.toString(platformMemberId));
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("amount", json.getString("pPayAmt"));  //债权购买人出资的金额
		userMap.put("orderNo", json.getString("pBidNo"));  //标的号
		userMap.put("paymentRequestNo", json.getString("pCreMerBillNo"));  //投标请求流水号
		userMap.put("fee", json.getString("pFromFee"));  //债权转让管理费
		
		userMap.put("callbackUrl", Constants.BASE_URL + "/yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "/yee/notifys");
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		if (!DealDetail.isSerialNumberExist(platformId, json.getString("pMerBillNo"))) {
			DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
					YEEConstants.REGISTER_CRETANSFER, json.getDouble("pCretAmt"), false, summary);
			
			if(!detail.addDealDetail()){
				error.code = -1;
				error.msg = "数据库异常，导致添加债权转让交易记录失败";
				
				return null;
			}
		}
		
		Logger.info("------------------------债权转让请求易宝参数req=:"+req+"-------------------------------");
		
		return req;
	}
	
	/**
	 * 登记债权人
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @jsonXtraPara P2P在环迅之后添加的参数
	 * @return
	 */
	public static String investBid(int platformId, long platformMemberId, JSONObject json,
			String argMerCode, String summary, JSONObject jsonXtraPara, ErrorInfo error){
		Map<String, String> userMap = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject(); 
		
		Logger.info("-----------登记债权人P2P参数json=:"+json.toString()+"\n-----------登记债权人P2P参数jsonXtraPara=:"+jsonXtraPara.toString());
		
		userMap.put("platformUserNo", Long.toString(platformMemberId));
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("orderNo", json.getString("pBidNo"));  //订单号
		userMap.put("transferAmount", jsonXtraPara.getString("transferAmount"));//标的借款额
		userMap.put("targetPlatformUserNo", jsonXtraPara.getString("loanerId"));//借款人编号
		userMap.put("paymentAmount", json.getString("pAuthAmt"));//冻结金额（至少一元）
		userMap.put("callbackUrl", Constants.BASE_URL + "/yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "/yee/notifys");
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
        req = YEEUtil.addAttribute(argMerCode, req);
		
		DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				YEEConstants.REGISTER_CREDITOR, json.getDouble("pAuthAmt"), false, summary);
        
		if(!detail.addDealDetail()){
			error.code = -1;
			error.msg = "数据库异常，导致添加投标交易记录失败";
			
			return null;
		}
		
		Logger.info("------------------------登记债权人请求易宝参数req=:"+req+"-------------------------------");
		
		return req;
	}
	
	/**
	 * 自动投标授权
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String autoBid(int type,int platformId, long platformMemberId, JSONObject json,
			String argMerCode, String summary, ErrorInfo error){
		Map<String, String> userMap = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject(); 
		
		Logger.info("------------------------自动投标授权json=:"+json.toString()+"-------------------------------");
		
		userMap.put("platformUserNo", Long.toString(platformMemberId));
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("callbackUrl", Constants.BASE_URL + "/yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "/yee/notifys");
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		DealDetail.addEvent(platformMemberId, type+200, platformId, json.getString("pMerBillNo"), null, null, summary, "");
		
		Logger.info("------------------------自动投标授权请求易宝参数req=:"+req+"-------------------------------");
		
		return req;
	}
	
	/**
	 * 还款
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String repayment(int platformId, long platformMemberId, JSONObject json,
			String argMerCode, String summary, ErrorInfo error){
		Map<String, Object> userMap = new HashMap<String, Object>();
		JSONObject jsonOb = new JSONObject(); 
		
		Logger.info("------------------------还款json=:"+json.toString()+"-------------------------------");
		
		userMap.put("platformUserNo", Long.toString(platformMemberId));
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("orderNo", json.getString("pBidNo"));
		userMap.put("callbackUrl", Constants.BASE_URL + "/yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "/yee/notifys");
		
		JSONArray jsonArr = null; 

		Object pDetails = json.get("pDetails");
		
		if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
			JSONObject pDetail = (JSONObject)pDetails; 
			JSONObject pRow = pDetail.getJSONObject("pRow"); 
	
			jsonArr = new JSONArray(); 
			jsonArr.add(pRow); 
		} else {
			jsonArr = json.getJSONArray("pDetails");
		} 
		List<Map<String, String>> arrJson = new ArrayList<Map<String, String>>();
		
		//遍历数组jsonArr再重新赋值到一个新的数组jsonArry
		for (Object obj : jsonArr) {
			JSONObject pRow = (JSONObject)obj;
			Map<String, String> properties = new HashMap<String, String>();
			
			properties.put("paymentRequestNo", pRow.getString("pCreMerBillNo"));  //转账请求流水号
			properties.put("targetUserNo", pRow.getString("pInAcctNo"));  //投资人会员编号
			properties.put("amount", pRow.getString("pInAmt"));  //还款金额(投资人到账金额=还款金额-还款平台提成，至少0.01 元)
			properties.put("fee", pRow.getString("pInFee"));  //还款平台提成
			arrJson.add(properties);
			
			String serialNumber = json.getString("pMerBillNo")+"_"+pRow.getString("pCreMerBillNo");
			
			if (!DealDetail.isSerialNumberExist(platformId, serialNumber)) {
				DealDetail detail = new DealDetail(platformId, Member.queryPlatMemberId(pRow.getString("pInAcctNo"), platformId), serialNumber, 
						YEEConstants.REPAYMENT, pRow.getDouble("pInAmt"), false, summary);
				
				if (!detail.addDealDetail()){
					error.code = -1;
					error.msg = "数据库异常，导致还款失败";
					
					return null;
				}
			}
		}
		
		if (!DealDetail.isSerialNumberExist(platformId, json.getString("pMerBillNo"))) {
			DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
					YEEConstants.REPAYMENT, json.getDouble("pOutAmt"), false, summary);
			
			if (!detail.addDealDetail()){
				error.code = -1;
				error.msg = "数据库异常，导致还款失败";
				
				return null;
			}
		}
		
		userMap.put("repayments", arrJson);
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", "repayment", null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		Logger.info("------------------------还款请求易宝参数req=:"+req+"-------------------------------");
		
		return req;
	}
	
	/**
	 * 用户转商户(本金垫付后借款人还款时转商户)
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static Map<String, String> offerUserToMerchant(int platformId, long platformMemberId, JSONObject json,
			String argMerCode, String summary, JSONObject jsonXtraPara, ErrorInfo error){
		Map<String, Object> userMap = new HashMap<String, Object>();
		Map<String, String> map = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject(); 
		
		Logger.info("------------------------用户转商户json=:"+json.toString()+"-------------------------------");
		
		userMap.put("platformUserNo", Long.toString(platformMemberId));  //出款人平台用户编号
		userMap.put("requestNo", json.getString("pMerBillNo"));  //请求流水号
		userMap.put("userType", "MEMBER");  //出款人类型
		userMap.put("bizType", "TRANSFER");  //固定值TRANSFER
		userMap.put("callbackUrl", Constants.BASE_URL + "/yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "/yee/notifys");
		
		List<Map<String, String>> arrJson = new ArrayList<Map<String, String>>();
		Map<String, String> properties = new HashMap<String, String>();
		
		properties.put("targetUserType", "MERCHANT");  //收款人用户类型
		properties.put("bizType", "TRANSFER");  //固定值TRANSFER
		properties.put("amount", jsonXtraPara.getString("amount"));  //转入金额
		properties.put("targetPlatformUserNo", argMerCode);  //商户编号
		arrJson.add(properties);
		
		if(!DealDetail.isSerialNumberExist(platformId, json.getString("pMerBillNo"))){

			DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
					YEEConstants.TRANSFER_USER_TO_MER, jsonXtraPara.getDouble("amount"), false, summary);
			
			if (!detail.addDealDetail()){
				error.code = -1;
				error.msg = "数据库异常，导致用户转商户失败";
				
				return null;
			}
		}
		
		userMap.put("details", arrJson);
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", "detail", null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		String sign = SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS);
	        
		map.put("req", req);
		map.put("sign", sign);
		map.put("url", YEEConstants.IPS_URL_TEST[34]);
		map.put("redictMark", "1");
		
		Logger.info("------------------------用户转商户(本金垫付后借款人还款时转商户)请求易宝参数req=:"+map.toString()+"-------------------------------");
		
		return map;
	}
	
	/**
	 * 用户转商户(发标时借款人把投标奖励转给平台)
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String userToMerchant(int platformId, long platformMemberId, JSONObject json, JSONObject extrajson,
			String argMerCode, String summary, ErrorInfo error){
		Map<String, Object> userMap = new HashMap<String, Object>();
		JSONObject jsonOb = new JSONObject(); 
		
		Logger.info("------------------------用户转商户json=:"+json.toString()+"-------------------------------");
		
		userMap.put("platformUserNo", Long.toString(platformMemberId));  //出款人平台用户编号
		userMap.put("requestNo", json.getString("pMerBillNo"));  //请求流水号
		userMap.put("userType", "MEMBER");  //出款人类型
		userMap.put("bizType", "TRANSFER");  //固定值TRANSFER
		userMap.put("callbackUrl", Constants.BASE_URL + "/yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "/yee/notifys");
		
		List<Map<String, String>> arrJson = new ArrayList<Map<String, String>>();
		Map<String, String> properties = new HashMap<String, String>();
		
		properties.put("targetUserType", "MERCHANT");  //收款人用户类型
		properties.put("amount", extrajson.getString("TransAmt"));  //转入金额
		properties.put("bizType", "TRANSFER");  //固定值TRANSFER
		properties.put("targetPlatformUserNo", argMerCode);  //商户编号
		arrJson.add(properties);
		
		DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				YEEConstants.TRANSFER_USER_TO_MER, extrajson.getDouble("TransAmt"), false, summary);
		
		if (!detail.addDealDetail()){
			error.code = -1;
			error.msg = "数据库异常，导致用户转商户失败";
			
			return null;
		}
		
		userMap.put("details", arrJson);
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", "detail", null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		Logger.info("------------------------用户转商户(发标时借款人把投标奖励转给平台)请求易宝参数req=:"+req+"-------------------------------");
		
		return req;
	}
	
	/**
	 * 提现 
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String withdraw(int platformId, long platformMemberId, JSONObject json,
			String argMerCode, String summary, ErrorInfo error){
		Map<String, String> userMap = new HashMap<String, String>();
		JSONObject jsonOb = new JSONObject(); 
		
		Logger.info("------------------------提现json=:"+json.toString()+"-------------------------------");
		
		userMap.put("platformUserNo", Long.toString(platformMemberId));
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("amount", json.getString("pTrdAmt"));
		userMap.put("callbackUrl", Constants.BASE_URL + "/yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "/yee/notifys");
		
		//1为扣取平台  2为扣取用户
		if(json.getString("pIpsFeeType").equals("1")){
			userMap.put("feeMode", "PLATFORM");
			
		}else{
			userMap.put("feeMode", "USER");
			
		}
		
		jsonOb.putAll(userMap);
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				IPSConstants.WITHDRAWAL, json.getDouble("pTrdAmt"), false, summary);
		
		if (!detail.addDealDetail()){
			error.code = -1;
			error.msg = "数据库异常，导致提现失败";
			
			return null;
		}
		
		Logger.info("------------------------提现请求易宝参数req=:"+req+"-------------------------------");
		
		return req;
	}
	
	/**
	 * 自动投标（WS）
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String antoTransfer(int platformId, long platformMemberId, JSONObject json,
			JSONObject jsonXtraPara, String argMerCode, String summary, ErrorInfo error){
		Map<String, String> userMap = new HashMap<String, String>();
		Map<String, Object> map = new HashMap<String, Object>();
		JSONObject jsonOb = new JSONObject(); 
		String resultarr = null;
		JSONObject jsonObj = new JSONObject();
		
		Logger.info("---------------自动投标json=:"+json.toString()+"\n---------------自动投标jsonXtraPara=:"+jsonXtraPara.toString());
		
		userMap.put("platformUserNo", Long.toString(platformMemberId));
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("orderNo", json.getString("pBidNo"));  //订单号
		userMap.put("transferAmount", jsonXtraPara.getString("transferAmount"));//标的借款额
		userMap.put("targetPlatformUserNo", jsonXtraPara.getString("loanerId"));//借款人编号
		userMap.put("paymentAmount", json.getString("pAuthAmt"));//冻结金额（至少一元）
		userMap.put("callbackUrl", Constants.BASE_URL + "/yee/callBack");  
		userMap.put("notifyUrl", Constants.BASE_URL + "/yee/notifys");  
		jsonOb.putAll(userMap);
		
		DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				YEEConstants.REGISTER_CREDITOR, json.getDouble("pAuthAmt"), false, summary);
		
		if(!detail.addDealDetail()){
			error.code = -1;
			error.msg = "数据库异常，导致解冻失败";
			
			return null;
		}
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
        req = YEEUtil.addAttribute(argMerCode, req);
        
		map.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
		map.put("service", "AUTO_TRANSFER");
		map.put("req", req);
		
		Logger.info("------------------------自动投标WS  map=:"+map+"-------------------------------");
		
		resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, map);
		
		//判断通信是否有异常，如果有异常，回调P2P失败值
		if(null == resultarr){
			String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
			jsonObj.put("pMerCode", argMerCode);
			jsonObj.put("pErrMsg", "");
			jsonObj.put("pErrCode", "");
			jsonObj.put("p3DesXmlPara", p3DesXmlPara);
			jsonObj.put("pSign", Encrypt.MD5( argMerCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
			
			Logger.info("------------------------自动投标请求易宝异常-------------------------------");
			
			return jsonObj.toString();
		}
		
		/*------------------------------处理返回数据------------------------------------------------*/
		JSONObject jsonResult = (JSONObject)Converter.xmlToObj(resultarr);
		String pErrCode = "MG00000F";
		
		Logger.info("------------------------自动投标易宝返回结果WS  jsonResult=:"+jsonResult+"-------------------------------");
		if(!jsonResult.get("code").equals("1")){
			pErrCode = "";
		}
		
		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonObj.put("pMemo1", platformMemberId+"");
		jsonOb.put("pP2PBillNo",json.getString("pMerBillNo"));//加该字段只是因为P2P接口需要用到“环迅”接口传过来的参数，这里用请求流水号代替
		jsonOb.put("pFee", "0");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		jsonObj.put("pMerCode", argMerCode);
		jsonObj.put("pErrMsg", "");
		jsonObj.put("pErrCode", pErrCode);
		jsonObj.put("p3DesXmlPara", p3DesXmlPara);
		jsonObj.put("pSign", Encrypt.MD5( argMerCode+pErrCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.info("------------------------自动投标回调参数=:"+jsonObj.toString()+"-------------------------------");
		
		return jsonObj.toString();
	}
	
	/**
	 * 自动还款（WS）
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String antoRepayment(int platformId, long platformMemberId, JSONObject json, String argMerCode){
		Map<String, Object> userMap = new HashMap<String, Object>();
		Map<String, Object> map = new HashMap<String, Object>();
		JSONObject jsonOb = new JSONObject(); 
		String resultarr = null;
		JSONArray jsonArr = null; 
		JSONObject jsonObj = new JSONObject();
		
		Object pDetails = json.get("pDetails");//节点数组
		
		userMap.put("platformUserNo", Long.toString(platformMemberId));
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("callbackUrl", Constants.BASE_URL + "/yee/callBack");
		userMap.put("notifyUrl", Constants.BASE_URL + "/yee/notifys");
		
		if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
			JSONObject pDetail = (JSONObject)pDetails; 
			JSONObject pRow = pDetail.getJSONObject("pRow"); 
	
			jsonArr = new JSONArray(); 
			jsonArr.add(pRow); 
		} else {
			jsonArr = json.getJSONArray("pDetails");
			
		} 
		
		JSONObject arrJson = new JSONObject(); 
		
		//遍历数组jsonArr再重新赋值到一个新的数组jsonArry
		for (Object obj : jsonArr) {
			JSONObject pRow = (JSONObject)obj;
			Map<String, String> properties = new HashMap<String, String>();
			
			properties.put("paymentRequestNo", pRow.getString("pCreMerBillNo"));  //转账请求流水号
			properties.put("targetUserNo", pRow.getString("pInAcctNo"));  //投资人会员编号
			properties.put("amount", pRow.getString("pInAmt"));  //还款金额(投资人到账金额=还款金额-还款平台提成，至少1 元)
			properties.put("fee", pRow.getString("pInFee"));  //还款平台提成
			arrJson.putAll(properties);
		}
		
		userMap.put("properties", arrJson);
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", "repayment", null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		map.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
		map.put("req", req);
		map.put("service", "AUTO_REPAYMENT");
		
		resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, map);
		
		//判断通信是否有异常，如果有异常，回调P2P失败值
		if(null == resultarr){
			String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
			jsonObj.put("pMerCode", argMerCode);
			jsonObj.put("pErrMsg", "");
			jsonObj.put("pErrCode", "");
			jsonObj.put("p3DesXmlPara", p3DesXmlPara);
			jsonObj.put("pSign", Encrypt.MD5( argMerCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
			
			Logger.info("------------------------自动投标请求易宝异常-------------------------------");
			
			return jsonObj.toString();
		}
		
		/*------------------------------处理返回数据------------------------------------------------*/
		JSONObject jsonResult = (JSONObject)Converter.xmlToObj(resultarr);
		String pErrCode = "MG00000F";
		
		if(!jsonResult.get("code").equals("1")){
			pErrCode = "";
		}
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		jsonObj.put("pMerCode", argMerCode);
		jsonObj.put("pErrMsg", "");
		jsonObj.put("pErrCode", pErrCode);
		jsonObj.put("p3DesXmlPara", p3DesXmlPara);
		jsonObj.put("pSign", Encrypt.MD5( argMerCode+pErrCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		return jsonObj.toString();
	}
	
	/**
	 * 平台划款--线下收款，本金垫付（WS商户转用户）
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String offerLinePayment(int platformId, long platformMemberId, JSONObject json, JSONObject jsonXtraPara,
			 String argMerCode, String summary, ErrorInfo error){
		Map<String, String> userMap = null;
		Map<String, Object> map = null;
		JSONObject jsonOb = null; 
		String resultarr = null;
		JSONObject jsonObj = new JSONObject();
		
		String periods = jsonXtraPara.getString("periods");
        Object pDetails = json.get("pDetails");//节点数组 
        JSONArray jsonArr = null; 
        
        if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
			JSONObject pDetail = (JSONObject)pDetails; 
			JSONObject pRow = pDetail.getJSONObject("pRow"); 
	
			jsonArr = new JSONArray(); 
			jsonArr.add(pRow); 
		} else {
			jsonArr = json.getJSONArray("pDetails");
			
		} 
		
		int count = 0;
		String pMerBillNo = null;
		JSONObject jsonResult = null;
		
		//遍历数组jsonArr
		for (Object obj : jsonArr) {
			JSONObject pRow = (JSONObject)obj;
			userMap = new HashMap<String, String>();
			jsonOb = new JSONObject();
			map = new HashMap<String, Object>();
			double amount = Double.parseDouble(pRow.getString("pTrdAmt")) - Double.parseDouble(pRow.getString("pTTrdFee"));
			
			pMerBillNo = pRow.getString("pOriMerBillNo") + "b" + periods;  //登记债权人提交的订单号，这里在后面加上9
			userMap.put("requestNo", pMerBillNo+"");
			userMap.put("sourceUserType", "MERCHANT");  //出款人类型
			userMap.put("sourcePlatformUserNo", argMerCode);  //商编号
			userMap.put("amount", amount+"");  //划款金额
			userMap.put("targetUserType", "MEMBER");  //收款人类型
			userMap.put("targetPlatformUserNo", pRow.getString("pTIpsAcctNo"));  //收款人编号
			userMap.put("callbackUrl", Constants.BASE_URL + "/yee/callBack");  //
			userMap.put("notifyUrl", Constants.BASE_URL + "/yee/notifys");  //
			jsonOb.putAll(userMap);
			
			if(!DealDetail.isSerialNumberExist(platformId, pMerBillNo)){
				DealDetail laondetails = new DealDetail(platformId,  Long.parseLong(pRow.getString("pTIpsAcctNo")), pMerBillNo+"", 
						YEEConstants.TRANSFER_MER_TO_USERS, amount, false, summary);
				
				if (!laondetails.addDealDetail()){
					error.code = -1;
					error.msg = "数据库异常，导致平台划款失败";
					
					return null;
				}
			}
			
			String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
	        req = YEEUtil.addAttribute(argMerCode, req);
	        
			map.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
			map.put("service", "PLATFORM_TRANSFER");
			map.put("req", req);
			
			resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, map);
			
			//判断通信是否有异常，如果有异常，回调P2P失败值
			if(null == resultarr){
				String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
				jsonObj.clear();
				jsonObj.put("pMerCode", argMerCode);
				jsonObj.put("pErrMsg", "");
				jsonObj.put("pErrCode", "");
				jsonObj.put("p3DesXmlPara", p3DesXmlPara);
				jsonObj.put("pSign", Encrypt.MD5( argMerCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
				
				Logger.info("------------------------平台划款(线下收款，本金垫付)请求易宝异常-------------------------------");
				
				return jsonObj.toString();
			}
			
			jsonResult = (JSONObject)Converter.xmlToObj(resultarr);
			
			if(jsonResult.get("code").equals("1")){
				count++;
			}
		}
		
		String pErrCode = "";
		if(jsonArr.size() == count){
			pErrCode = "MG00000F";
		}
		
		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonObj.put("pMemo1", platformMemberId+"");
		jsonObj.put("pMemo3", json.getString("pMemo3"));
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		
		jsonObj.clear();
		jsonObj.put("pMerCode", argMerCode);
		jsonObj.put("pErrMsg", "");
		jsonObj.put("pErrCode", pErrCode);
		jsonObj.put("p3DesXmlPara", p3DesXmlPara);
		jsonObj.put("pSign", Encrypt.MD5(argMerCode+pErrCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.info("------------------------平台划款(线下收款，本金垫付)回调P2P=:"+jsonObj.toString()+"-------------------------------");
		
		return jsonObj.toString();
	}
	
	/**
	 * 平台划款--多笔--投标奖励发放（WS商户转用户）
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static Map<String, String> plateToTransfers(int platformId, long platformMemberId, JSONObject json, 
			JSONObject extraJson, String argMerCode, String summary, ErrorInfo error){
		Map<String, String> userMap = null;
		Map<String, Object> map = null;
		JSONObject jsonOb = null; 
		String resultarr = null;
		JSONObject jsonObj = new JSONObject();
		
        Object pDetails = extraJson.get("pDetails");//节点数组 
        JSONArray jsonArr = null; 
        
        if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
			JSONObject pDetail = (JSONObject)pDetails; 
			JSONObject pRow = pDetail.getJSONObject("pRow"); 
	
			jsonArr = new JSONArray(); 
			jsonArr.add(pRow); 
		} else {
			jsonArr = extraJson.getJSONArray("pDetails");
			
		} 
		
		int count = 0;
		String pMerBillNo = null;
		JSONObject jsonResult = null;
		int size = 0;
		
		//遍历数组jsonArr再重新赋值到一个新的数组jsonArry
		for (Object obj : jsonArr) {
			JSONObject pRow = (JSONObject)obj;
			userMap = new HashMap<String, String>();
			jsonOb = new JSONObject();
			map = new HashMap<String, Object>();
			size = size + 1;
			
			pMerBillNo = json.getString("pMerBillNo")+ "a" +String.valueOf(size);
			userMap.put("requestNo", pMerBillNo+"");
			userMap.put("sourceUserType", "MERCHANT");  //出款人类型
			userMap.put("sourcePlatformUserNo", argMerCode);  //出款人编号
			userMap.put("amount", pRow.getString("transAmt"));  //划款金额
			userMap.put("targetUserType", "MEMBER");  //收款人类型
			userMap.put("targetPlatformUserNo", pRow.getString("inCustId"));  //收款人编号
			userMap.put("callbackUrl", Constants.BASE_URL + "/yee/callBack");  //
			userMap.put("notifyUrl", Constants.BASE_URL + "/yee/notifys");  //
			jsonOb.putAll(userMap);
			
			if (!DealDetail.isSerialNumberExist(platformId, pMerBillNo+"")) {
				DealDetail laondetails = new DealDetail(platformId,  Long.parseLong(pRow.getString("inCustId")), pMerBillNo+"", 
						YEEConstants.TRANSFER_MER_TO_USERS, Double.parseDouble(pRow.getString("transAmt")), false, summary);
				
				if (!laondetails.addDealDetail()){
					error.code = -1;
					error.msg = "数据库异常，导致平台划款失败";
					
					return null;
				}
			}
			
			String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
	        req = YEEUtil.addAttribute(argMerCode, req);
	        
			map.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
			map.put("service", "PLATFORM_TRANSFER");
			map.put("req", req);
			
			resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, map);
			
			//判断通信是否有异常，如果有异常，回调P2P失败值
			if(null == resultarr){
				userMap.clear();
				String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
				userMap.put("pMerCode", argMerCode);
				userMap.put("pErrMsg", "");
				userMap.put("pErrCode", "");
				userMap.put("p3DesXmlPara", p3DesXmlPara);
				userMap.put("pSign", Encrypt.MD5( argMerCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
				userMap.put("url", json.getString("pWebUrl"));
				userMap.put("redictMark", "2");
				Logger.info("------------------------平台划款(投标奖励)请求易宝异常-------------------------------");
				
				return userMap;
			}
			
			jsonResult = (JSONObject)Converter.xmlToObj(resultarr);
			
			if(jsonResult.get("code").equals("1")){
				count++;
			}
		}
		
		String pErrCode = "";
		if(jsonArr.size() == count){
			pErrCode = "MG00000F";
		}
		
		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonObj.put("pMemo1", json.getString("pMemo1"));
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		
		userMap.clear();
		userMap.put("pMerCode", argMerCode);
		userMap.put("pErrMsg", "");
		userMap.put("pErrCode", pErrCode);
		userMap.put("p3DesXmlPara", p3DesXmlPara);
		userMap.put("pSign", Encrypt.MD5(argMerCode+pErrCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		userMap.put("url", json.getString("pWebUrl"));
		userMap.put("redictMark", "2");
		Logger.info("------------------------平台划款(投标奖励)回调P2P=:"+userMap.toString()+"-------------------------------");
		
		return userMap;
	}
	
	/**
	 * 平台划款--单笔--cps发放（WS商户转用户）
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String plateToTransfer(int platformId, long platformMemberId, JSONObject json, 
			JSONObject extraJson, String argMerCode, String summary, ErrorInfo error){
		Map<String, String> userMap = new HashMap<String, String>();
		Map<String, Object> map = new HashMap<String, Object>();
		JSONObject jsonOb = new JSONObject(); 
		String resultarr = null;
		JSONObject jsonObj = new JSONObject();
		
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("sourceUserType", "MERCHANT");  //出款人类型
		userMap.put("sourcePlatformUserNo", argMerCode);  //出款人编号
		userMap.put("amount", extraJson.getString("transAmt"));  //划款金额
		userMap.put("targetUserType", "MEMBER");  //收款人类型
		userMap.put("targetPlatformUserNo", extraJson.getString("inCustId"));  //收款人编号
		userMap.put("callbackUrl", Constants.BASE_URL + "/yee/callBack");  //
		userMap.put("notifyUrl", Constants.BASE_URL + "/yee/notifys");  //
		jsonOb.putAll(userMap);
		
		DealDetail laondetails = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				YEEConstants.TRANSFER_MER_TO_USER, Double.parseDouble(extraJson.getString("transAmt")), false, summary);
		
		if (!laondetails.addDealDetail()){
			error.code = -1;
			error.msg = "数据库异常，导致平台划款失败";
			
			return null;
		}
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
        req = YEEUtil.addAttribute(argMerCode, req);
        
		map.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
		map.put("service", "PLATFORM_TRANSFER");
		map.put("req", req);
		
		resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, map);
		
		//判断通信是否有异常，如果有异常，回调P2P失败值
		if(null == resultarr){
			String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
			jsonObj.put("pMerCode", argMerCode);
			jsonObj.put("pErrMsg", "");
			jsonObj.put("pErrCode", "");
			jsonObj.put("p3DesXmlPara", p3DesXmlPara);
			jsonObj.put("pSign", Encrypt.MD5( argMerCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
			
			Logger.info("-----------------------平台划款--单笔--cps发放请求易宝异常-------------------------------");
			
			return jsonObj.toString();
		}
		
		JSONObject jsonResult = (JSONObject)Converter.xmlToObj(resultarr);
		String pErrCode = "MG00000F";
		
		Logger.info("------------------------平台划款(cps奖励)WS返回结果=:"+jsonResult.toString()+"-------------------------------");
		
		if(!jsonResult.get("code").equals("1")){
			pErrCode = "";
		}
		
		jsonObj.clear();
		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonObj.put("pMemo1", json.getString("pMemo1"));
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		
		jsonObj.clear();
		jsonObj.put("pMerCode", argMerCode);
		jsonObj.put("pErrMsg", "");
		jsonObj.put("pErrCode", pErrCode);
		jsonObj.put("p3DesXmlPara", p3DesXmlPara);
		jsonObj.put("pSign", Encrypt.MD5(argMerCode+pErrCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.info("------------------------平台划款(cps奖励)回调P2P=:"+jsonObj.toString()+"-------------------------------");
		
		return jsonObj.toString();
	}
	
	/**
	 * 查询用户信息(WS)
	 * @param platformMemberId 会员在P2P平台的唯一标识 
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String queryUserInfo(String argMerCode, long memberId){
		Map<String, Object> map = new HashMap<String, Object>();
		JSONObject jsonOb = new JSONObject(); 
		Map<String, String> userMap = new HashMap<String, String>();
		String resultarr = null;
		JSONObject jsonObj = new JSONObject();
		
		Logger.info("------------------------查询用户信息argMerCode=:"+memberId+"-------------------------------");
		
		userMap.put("platformUserNo", memberId+"");
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		map.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
		map.put("req", req);
		map.put("service", "ACCOUNT_INFO");
		
		resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, map);
		
		//判断通信是否有异常，如果有异常，回调P2P失败值
		if(null == resultarr){
			Logger.info("-----------------------查询用户信息请求易宝异常-------------------------------");
			
			return jsonObj.toString();
		}
		
		/*----------------------------处理返回的数据回调P2P--------------------------------------------------------------*/
		JSONObject jsonResult = (JSONObject)Converter.xmlToObj(resultarr);
		
		Logger.info("------------------------查询用户信息第三方返回结果jsonResult=:"+jsonResult+"-------------------------------");
		
		jsonObj.put("pBalance", jsonResult.getString("availableAmount"));  //可用余额
		jsonObj.put("pLock", jsonResult.getString("freezeAmount"));  //冻结金额
//		jsonObj.put("pBankName", jsonResult.getString("bank"));
//		jsonObj.put("pBankCard", jsonResult.getString("cardNo"));
	    jsonObj.put("pBankName", "未知");
		jsonObj.put("pBankCard", "未知");
		jsonObj.put("pBCardStatus", "未知");
		jsonObj.put("memberId", memberId+"");
		
		if(jsonResult.getString("activeStatus").equals(YEEConstants.USER_ACTIVATED)){
			jsonObj.put("pStatus","已激活");
			
		}else{
			jsonObj.put("pStatus", "未激活");
			
		}
		
//		if(jsonResult.getString("cardStatus").equals(YEEConstants.CARD_HANDDLE)){
//			jsonObj.put("pBCardStatus","认证中");
//			
//		}else if(jsonResult.getString("cardStatus").equals(YEEConstants.CARD_SUCCESS)){
//			jsonObj.put("pBCardStatus", "已认证");
//			
//		}else{
//			jsonObj.put("pBCardStatus", "还未绑定");
//			
//		}
		
		Logger.info("------------------------查询用户信息 返回结果:"+jsonObj.toString()+"-------------------------------");
		
		return jsonObj.toString();
	}
	
	/**
	 * 放款WS（对应P2P转账）
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String loanWS(int platformId, long platformMemberId, JSONObject json, JSONObject jsonXtraPara,
			String argMerCode, String summary, ErrorInfo error){
		Map<String, Object> userMap = new HashMap<String, Object>();
		JSONArray jsonArr = null;
		String resultarr = null;
		JSONObject jsonObj = new JSONObject();
		JSONObject pRow = null;
			
		double amount = jsonXtraPara.getDouble("amount");
		
		Logger.info("------------------------放款P2P参数 json=:"+json.toString()+"-------------------------------");
		
		List<Map<String, String>> arrJson = new ArrayList<Map<String, String>>();
		Object pDetails = json.get("pDetails");//节点数组
		
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("fee", jsonXtraPara.getString("serviceFees"));
		userMap.put("orderNo", json.getString("pBidNo"));
		
		userMap.put("notifyUrl", Constants.BASE_URL + "/yee/notifys");
		
		if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
			JSONObject pDetail = (JSONObject)pDetails; 
			pRow = pDetail.getJSONObject("pRow"); 
	
			jsonArr = new JSONArray(); 
			jsonArr.add(pRow); 
		} else {
			jsonArr = json.getJSONArray("pDetails");
			
		} 
		
		//遍历数组jsonArr再重新赋值到一个新的数组jsonArry
		for(int i=0;i<jsonArr.size();i++){
			Map<String, String> properties = new HashMap<String, String>();
			pRow = (JSONObject)jsonArr.get(i);
			
			properties.put("requestNo", pRow.getString("pOriMerBillNo"));  //投标请求流水号
			properties.put("transferAmount", pRow.getString("pTrdAmt"));  //转账请求转账金额
			properties.put("sourceUserType", "MEMBER");  //投资人会员类型
			properties.put("targetUserType", "MEMBER");  //借款人会员类型
			properties.put("targetPlatformUserNo", jsonXtraPara.getString("loanerId"));  //借款人会员编号
			properties.put("sourcePlatformUserNo", pRow.getString("pFIpsAcctNo"));  //投资人会员编号
			
			arrJson.add(properties);
			
            String serialNumber = json.getString("pMerBillNo")+"_"+pRow.getString("pOriMerBillNo");
			
			if (!DealDetail.isSerialNumberExist(platformId, serialNumber)) {
				DealDetail detail = new DealDetail(platformId, Member.queryPlatMemberId(pRow.getString("pFIpsAcctNo"), platformId), serialNumber, 
						YEEConstants.UNFREZZ_AMOUNT, pRow.getDouble("pTrdAmt"), false, summary);
				
				if (!detail.addDealDetail()){
					error.code = -1;
					error.msg = "数据库异常，导致放款失败";
					
					return null;
				}
		      }
		   }
		
		if (!DealDetail.isSerialNumberExist(platformId, json.getString("pMerBillNo"))) {
			DealDetail laondetails = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
					YEEConstants.TRANSFER, amount, false, summary);
			
			if (!laondetails.addDealDetail()){
				error.code = -1;
				error.msg = "数据库异常，导致放款失败";
				
				return null;
			}
		}
		
		userMap.put("transfers", arrJson);
		jsonObj.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonObj.toString(), "request", "transfer", null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		userMap.clear();
		userMap.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
		userMap.put("req", req);
		userMap.put("service", "LOAN");
		
		//WS请求直接返回处理结果
		resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, userMap);

		//判断通信是否有异常，如果有异常，回调P2P失败值
		if(null == resultarr){
			jsonObj.clear();
			jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
			jsonObj.put("pTransferType", json.getString("pTransferType"));
			jsonObj.put("pMemo1", platformMemberId+"");
			
			String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
			
			jsonObj.clear();
			jsonObj.put("pMerCode", argMerCode);
			jsonObj.put("pErrMsg", "");
			jsonObj.put("pErrCode", "");
			jsonObj.put("p3DesXmlPara", p3DesXmlPara);
			jsonObj.put("pSign", Encrypt.MD5( argMerCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
			
			Logger.info("------------------------放款回调出现异常，回调P2P失败值-------------------------------");
			
			return jsonObj.toString();
		}
		
		JSONObject jsonResult = (JSONObject)Converter.xmlToObj(resultarr);
		
		String pErrCode = "";
		
		Logger.info("------------------------放款WS返回结果=:"+jsonResult.toString()+"-------------------------------");
		
		if(jsonResult.get("code").equals("1")){
			pErrCode = "MG00000F";
			DealDetail.updateStatus(json.getString("pMerBillNo"));
			
		}else{
			userMap.clear();
			jsonObj.clear();
			jsonObj = (JSONObject)jsonArr.get(0);
	        userMap.put("requestNo", jsonObj.getString("pOriMerBillNo"));  //需要投标流水号中一个
			
	        jsonObj.clear();
			userMap.put("mode", "PAYMENT_RECORD");  //查询模式
			jsonObj.putAll(userMap);
			
			String ueryReq = Converter.jsonToXml(jsonObj.toString(), "request", null, null, null);
			ueryReq = YEEUtil.addAttribute(argMerCode, ueryReq);
			
			userMap.clear();
			userMap.put("sign",SignUtil.sign(ueryReq, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
			userMap.put("req", ueryReq);
			userMap.put("service", "QUERY");
			
			resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, userMap);
			
			/*----------------------------处理返回的数据回调P2P--------------------------------------------------------------*///系统异常，异常编号
			JSONObject queryResult = (JSONObject)Converter.xmlToObj(resultarr);
			pDetails = queryResult.get("records");//节点数组
			
			if(!pDetails.toString().equals("[]")){
				if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
					JSONObject pDetail = (JSONObject)pDetails; 
					JSONObject record = pDetail.getJSONObject("record"); 
			
					jsonArr = new JSONArray(); 
					jsonArr.add(record); 
				} else {
					jsonArr = queryResult.getJSONArray("records");
					
				} 
				pRow = jsonArr.getJSONObject(0);
				
				if(pRow.getString("status").equals("LOANED")){
					pErrCode = "MG00000F";
					DealDetail.updateStatus(json.getString("pMerBillNo"));
				}
			}
		}
		
		jsonObj.clear();
		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonObj.put("pTransferType", json.getString("pTransferType"));
		jsonObj.put("pMemo1", platformMemberId+"");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		
		jsonObj.clear();
		jsonObj.put("pMerCode", argMerCode);
		jsonObj.put("pErrMsg", "");
		jsonObj.put("pErrCode", pErrCode);
		jsonObj.put("p3DesXmlPara", p3DesXmlPara);
		jsonObj.put("pSign", Encrypt.MD5( argMerCode+pErrCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.info("------------------------放款回调P2P=:"+jsonObj.toString()+"-------------------------------");
		
		return jsonObj.toString();
	}
	
	/**
	 * 债权转让WS（对应P2P转账）
	 * @param platformMemberId 会员在P2P平台的唯一标识
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String transferWS(int platformId, long platformMemberId, JSONObject json, JSONObject jsonXtraPara,
			String argMerCode, String summary, ErrorInfo error){
		Map<String, Object> userMap = new HashMap<String, Object>();
		JSONObject jsonOb = new JSONObject(); 
		String resultarr = null;
		JSONObject jsonObj = new JSONObject();
		JSONArray jsonArr = null;
		double amount = 0;
		
		Logger.info("------------------------债权转让P2P参数json=:"+json.toString()+"-------------------------------");
		
		Object pDetails = json.get("pDetails");//节点数组
		
		if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
			JSONObject pDetail = (JSONObject)pDetails; 
			JSONObject pRow = pDetail.getJSONObject("pRow"); 
	
			jsonArr = new JSONArray(); 
			jsonArr.add(pRow); 
		} else {
			jsonArr = json.getJSONArray("pDetails");
			
		} 
		
		//遍历数组jsonArr再重新赋值到一个新的数组jsonArry
		for (Object obj : jsonArr) {
			JSONObject pRow = (JSONObject)obj;
			
			userMap.put("requestNo", pRow.getString("pOriMerBillNo"));  //投标请求流水号
			userMap.put("amount", pRow.getString("pTrdAmt"));  //划款金额
			userMap.put("sourceUserType", "MERCHANT");  //出款人类型
			userMap.put("sourcePlatformUserNo", pRow.getString("fromUserId"));  //出款人编号
			userMap.put("targetUserType", "MERCHANT");  //收款人类型
			userMap.put("targetPlatformUserNo", pRow.getString("toUserId"));  //收款人编号
	    }
		
		userMap.put("requestNo", json.getString("pMerBillNo"));
		userMap.put("notifyUrl", Constants.BASE_URL + "/yee/notifys");
		jsonOb.putAll(userMap);
		
		if (!DealDetail.isSerialNumberExist(platformId, json.getString("pMerBillNo"))) {
			DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
					YEEConstants.TRANSFER, amount, false, summary);
			
			if (!detail.addDealDetail()){
				error.code = -1;
				error.msg = "数据库异常，导致债权转让失败";
				
				return null;
			}
		}
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		userMap.clear();
		userMap.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
		userMap.put("req", req);
		userMap.put("service", "PLATFORM_TRANSFER");
		
		//WS请求直接返回处理结果
		resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, userMap);

		//判断通信是否有异常，如果有异常，回调P2P失败值
		if(null == resultarr){
			String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
			jsonObj.put("pMerCode", argMerCode);
			jsonObj.put("pErrMsg", "");
			jsonObj.put("pErrCode", "");
			jsonObj.put("p3DesXmlPara", p3DesXmlPara);
			jsonObj.put("pSign", Encrypt.MD5( argMerCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
			
			Logger.info("-----------------------债权转让请求易宝异常-------------------------------");
			
			return jsonObj.toString();
		}
		
		JSONObject jsonResult = (JSONObject)Converter.xmlToObj(resultarr);
		String pErrCode = "";
		
		Logger.info("------------------------债权转让WS易宝处理结果 jsonResult=:"+jsonResult.toString()+"-------------------------------");
		
		if(jsonResult.get("code").equals("1")){
			pErrCode = "MG00000F";
		    DealDetail.updateStatus(json.getString("pMerBillNo"));
		}
		
		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonObj.put("pTransferType", json.getString("pTransferType"));
		jsonObj.put("pMemo1", platformMemberId+"");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		
		jsonObj.clear();
		jsonObj.put("pMerCode", argMerCode);
		jsonObj.put("pErrMsg", "");
		jsonObj.put("pErrCode", pErrCode);
		jsonObj.put("p3DesXmlPara", p3DesXmlPara);
		jsonObj.put("pSign", Encrypt.MD5( argMerCode+pErrCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.info("------------------------债权转让回调P2P jsonResult=:"+jsonResult.toString()+"-------------------------------");
		
		return jsonObj.toString();
	}
	
	/**
	 * 取消投标--解冻资金(WS)
	 * @param platformMemberId 会员在P2P平台的唯一标识 
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String revocationTransfer(int platformId, String argMerCode, JSONObject json, Map<String, String> remarkMap){
		ErrorInfo error = new ErrorInfo();
		JSONObject jsonOb = new JSONObject(); 
		Map<String, Object> userMap = new HashMap<String, Object>();
		String resultarr = null;
		JSONObject jsonObj = new JSONObject();
		
		userMap.put("requestNo", json.getString("oldMerBillNo"));  //之前投标的请求流水号
		userMap.put("platformUserNo",remarkMap.get("memberId"));  //用户编号
		jsonOb.putAll(userMap);
		
		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
		req = YEEUtil.addAttribute(argMerCode, req);
		
		userMap.clear();
		userMap.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
		userMap.put("req", req);
		userMap.put("service", "REVOCATION_TRANSFER");
		
		if (!DealDetail.isSerialNumberExist(platformId, json.getString("pMerBillNo"))) {
			DealDetail laondetails = new DealDetail(Integer.parseInt(remarkMap.get("platformId")),  Long.parseLong(remarkMap.get("memberId")), json.getString("oldMerBillNo")+"9", 
					YEEConstants.UNFREZZ_AMOUNT, 0, false, remarkMap.toString());
			
			if (!laondetails.addDealDetail()){
				error.code = -1;
				error.msg = "数据库异常，导致平台划款失败";
				
				return null;
			}
		}
		
		resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, userMap);
		
		//判断通信是否有异常，如果有异常，回调P2P失败值
		if(null == resultarr){
			String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
			jsonObj.put("pMerCode", argMerCode);
			jsonObj.put("pErrMsg", "");
			jsonObj.put("pErrCode", "");
			jsonObj.put("p3DesXmlPara", p3DesXmlPara);
			jsonObj.put("pSign", Encrypt.MD5( argMerCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
			
			Logger.info("-----------------------取消投标请求易宝异常-------------------------------");
			
			return jsonObj.toString();
		}
		
		JSONObject jsonResult = (JSONObject)Converter.xmlToObj(resultarr);
		String pErrCode = "";
		
		Logger.info("------------------------取消投标WS易宝处理结果 jsonResult=:"+jsonResult.toString()+"-------------------------------");
		
		if(jsonResult.get("code").equals("1")){
			pErrCode = "MG00000F";
			DealDetail.updateStatus(json.getString("oldMerBillNo")+"9");
		}
		
		jsonObj.put("pMerBillNo", json.getString("oldMerBillNo"));
		jsonObj.put("pMemo2", "Y");
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		
		jsonObj.clear();
		jsonObj.put("pMerCode", argMerCode);
		jsonObj.put("pErrMsg", "");
		jsonObj.put("pErrCode", pErrCode);
		jsonObj.put("p3DesXmlPara", p3DesXmlPara);
		jsonObj.put("oldMerBillNo", json.getString("oldMerBillNo"));
		jsonObj.put("pSign", Encrypt.MD5( argMerCode+pErrCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.info("------------------------取消投标回调P2P jsonResult=:"+jsonResult.toString()+"-------------------------------");
		
		return jsonObj.toString();
	}
	
	/**
	 * 单笔业务查询WS(PAYMENT_RECORD：标的投资放款记录
       REPAYMENT_RECORD：标的还款记录
	   WITHDRAW_RECORD：提现记录
	   RECHARGE_RECORD：充值记录)
	 * @param platformMemberId 会员在P2P平台的唯一标识 
	 * @param json 解析xml出来的数据
	 * @param argMerCode 商户号
	 * @return
	 */
	public static String query(String argMerCode, JSONObject json, JSONObject jsonXtraPara){
		Map<String, Object> map = new HashMap<String, Object>();
		JSONObject jsonOb = new JSONObject(); 
		Map<String, String> userMap = new HashMap<String, String>();
		String resultarr = null;
		JSONObject jsonObj = new JSONObject();
		 String pErrCode = "MG00000F";
		 String status = null;
		
		Logger.info("------------------------单笔业务查询json=:"+json.toString()+"-------------------------------");
		
		int type = json.getInt("pTradeType");
		String mode = null;
		
       if(type != QueryType.QUERY_CREATE_BID){
    	   switch(type){
	   		case QueryType.QUERY_INVEST_TYPE:
	   			mode = "PAYMENT_RECORD";
	   			break;
	   		case QueryType.QUERY_RECAHARGE_TYPE:
	   			mode = "RECHARGE_RECORD";
	   			break;
	   		case QueryType.QUERY_WITHDRAW_TYPE:
	   			mode = "WITHDRAW_RECORD";
	   			break;
	   		case QueryType.QUERY_REPAYMENT_TYPE:
	   			mode = "REPAYMENT_RECORD";
	   			break;
	   		case QueryType.QUERY_MONEY_TRANSFER_TYPE:
	   			mode = "PAYMENT_RECORD";
	   			break;
	   		}
	   		
	   		//放款的时候需要用到投资流水号的其中一个
	   		if(type == QueryType.QUERY_MONEY_TRANSFER_TYPE){
	   			userMap.put("requestNo", jsonXtraPara.getString("investMenberNo"));  //补单的请求流水号
	   			
	   		}else{
	   			userMap.put("requestNo", json.getString("pMerBillNo"));  //补单的请求流水号
	   			
	   		}
	   		
	   		userMap.put("mode", mode);  //查询模式
	   		jsonOb.putAll(userMap);
	   		
	   		String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
	   		req = YEEUtil.addAttribute(argMerCode, req);
	   		map.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
	   		map.put("req", req);
	   		map.put("service", "QUERY");
	   		
	   		resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, map);
	   		
	   		//判断通信是否有异常，如果有异常，回调P2P失败值
	   		if(null == resultarr){
	   			String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
	   			jsonObj.put("pMerCode", argMerCode);
	   			jsonObj.put("pErrMsg", "");
	   			jsonObj.put("pErrCode", "");
	   			jsonObj.put("p3DesXmlPara", p3DesXmlPara);
	   			jsonObj.put("pSign", Encrypt.MD5( argMerCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
	   			
	   			Logger.info("-----------------------单笔业务查询请求易宝异常-------------------------------");
	   			
	   			return jsonObj.toString();
	   		}
	   		
	   		JSONObject jsonResult = (JSONObject)Converter.xmlToObj(resultarr);
	   		
	   		Logger.info("------------------------单笔业务查询第三方返回结果jsonResult=:"+jsonResult+"-------------------------------");
	   		JSONArray jsonArr = null;
	   		Object pDetails = jsonResult.get("records");  //节点数组
	   		
	   		if(null == pDetails || pDetails.toString().equals("[]")){
	   			status = Status.FAIL;
	   			
	   		}else{
	   			if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
	   				JSONObject pDetail = (JSONObject)pDetails; 
	   				JSONObject record = pDetail.getJSONObject("record"); 
	   		
	   				jsonArr = new JSONArray(); 
	   				jsonArr.add(record); 
	   			} else {
	   				jsonArr = jsonResult.getJSONArray("records");
	   				
	   			} 
	   			JSONObject pRow = jsonArr.getJSONObject(0);
	   			
	   			//不同的查询类型返回的数据不一样
	   			switch(type){
	   			case QueryType.QUERY_INVEST_TYPE:
	   				if(pRow.getString("status").equals("FREEZED")){
	   					status = Status.SUCCESS;
	   				}else if(pRow.getString("status").equals("CANCEL")){
	   					status = Status.FAIL;
	   				}
	   				break;
	   			case QueryType.QUERY_RECAHARGE_TYPE:
	   				if(pRow.getString("status").equals("SUCCESS")){
	   					status = Status.SUCCESS;
	   				}else if(pRow.getString("status").equals("INIT")){
	   					status = Status.FAIL;
	   				}
	   				break;
	   			case QueryType.QUERY_WITHDRAW_TYPE:
	   				if(pRow.getString("status").equals("SUCCESS") && pRow.getString("remitStatus").equals("REMIT_SUCCESS")){
	   					status = Status.SUCCESS;
	   				}else if(pRow.getString("status").equals("SUCCESS") && pRow.getString("remitStatus").equals("REMITING")){
	   					status = Status.HANDLING;
	   				}else{
	   					status = Status.FAIL;
	   				}
	   				break;
	   			case QueryType.QUERY_REPAYMENT_TYPE:
	   				if(pRow.getString("status").equals("SUCCESS")){
	   					status = Status.SUCCESS;
	   				}else if(pRow.getString("status").equals("INIT")){
	   					status = Status.FAIL;
	   				}
	   				break;
	   			case QueryType.QUERY_MONEY_TRANSFER_TYPE:
	   				if(pRow.getString("status").equals("LOANED")){
	   					status = Status.SUCCESS;
	   				}else if(pRow.getString("status").equals("CANCEL")){
	   					status = Status.FAIL;
	   				}
	   				break;
	   			}
	   		}
	   		
	   		if(!jsonResult.get("code").equals("1")){
				pErrCode = "";
			}
	   }else{
		   status = Status.SUCCESS;
	   }
		
		jsonObj.put("pTradeStatue", status);
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null), Constants.ENCRYPTION_KEY);
		
		jsonObj.clear();
		jsonObj.put("pMerCode", argMerCode);
		jsonObj.put("pErrMsg", "");
		jsonObj.put("pErrCode", pErrCode);
		jsonObj.put("p3DesXmlPara", p3DesXmlPara);
		jsonObj.put("pSign", Encrypt.MD5( argMerCode+pErrCode+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		Logger.info("------------------------单笔业务查询第三方返回结果后回调P2P数据jsonObj=:"+jsonObj.toString()+"-------------------------------");
		
		return jsonObj.toString();
	}
	
	/**
	 * 标的登记和流标
	 * @param argMerCode
	 * @param json
	 * @param memberId
	 * @param jsonXtraPara
	 * @param summary
	 * @return
	 */
	public static Map<String, String> registerBidContr(String argMerCode, int platformId, int type, JSONObject json, Long memberId, JSONObject jsonXtraPara, String summary){
		Map<String, String> args =null;
		
	    //流标
		if(json.getString("pOperationType").equals(YEEConstants.BID_OPERRATION_TYPE)){
			args = flowCall(argMerCode, platformId, type, json, memberId, jsonXtraPara, summary);
			
		}else{ //标的登记
			args = bidCall(argMerCode, json, jsonXtraPara, memberId);
		}
		
		args.put("redictMark", "2");
		args.put("url", json.getString("pWebUrl"));
		
		return args;
	}
	
	/**
	 * 转账
	 * @param platformId
	 * @param memberId
	 * @param json
	 * @param jsonXtraPara
	 * @param argMerCode
	 * @param summary
	 * @param error
	 * @return
	 */
	public static String transferContr(int platformId, long memberId, JSONObject json,
			JSONObject jsonXtraPara, String argMerCode, String summary, ErrorInfo error){
		int transferType = json.getInt("pTransferType");
		String args = null;
		
		switch(transferType){
		case YEEConstants.P2P_LOAN:  //放款
			args = loanWS(platformId, memberId, json, jsonXtraPara, argMerCode, summary, error);
			break;
		case YEEConstants.P2P_TRANSFER:  //债权转让
//			args = transferWS(platformId, memberId, json, jsonXtraPara, argMerCode, summary, error);
//			break;
//		case YEEConstants.COMPENSATE_REPAYMENT: //代偿还款（本金垫付后借款人还款 -- 用户转商户）
//			args = offerUserToMerchant(platformId, memberId, json, argMerCode, summary, isWs, error);
//			break;
		case YEEConstants.COMPENSATE:  //（线下收款，本金垫付--商户转用户）
			args = offerLinePayment(platformId, memberId, json, jsonXtraPara, argMerCode, summary, error);
			break;
		}
		
		return args;
	}
	
	/**
	 * 根据身份证判断是否开户
	 * @param json
	 * @param platformId
	 * @param memberId
	 * @param memberName
	 * @param domain
	 * @param error
	 */
	public static void isRegister(JSONObject json, int platformId, long memberId, String memberName, String domain, ErrorInfo error){
		String idNumber = json.getString("pIdentNo");
		
		/*身份证已存在*/
		if(Member.isCreateAccount(json.getString("pIdentNo"), domain, error)) {
			
			/*用户平台关系表中不存在该身份证会员的信息*/
			if(error.code == 1) {
				Member member = new Member();
				long id = Member.queryIdByIdNumber(json.getString("pIdentNo"));
				member.memberId = id;
				member.platformId = platformId;
				member.memberId = platformId;
				member.platformMembername = memberName;
				member.addPlatformmember(error);
			
			/*不同平台，使用相同的支付接口，表示身份证会员已开户，在平台关系表中插入数据*/	
			}else if(error.code == 2) {
				Member member = new Member();
				member.idNumber = idNumber;
				
				member.memberId = platformId;
				member.platformMembername = memberName;
				member.platformMemberAccount = member.queryAccount(idNumber, platformId);
				
				member.addPlatformmember(error);
				error.code = -1;
				
			}else if(error.code == 3) {
				
				Member.updateAccount(platformId, platformId, Member.queryAccount(idNumber, platformId));
				error.code = -1;
				error.msg = "已开户";
				
			}else if(error.code == 4) {
				
			}else {
				error.code = -1;
			}
		}else {
			/*身份证不存在，根据请求在用户表和用户平台关系表中添加记录*/
			Member member = new Member();
			
			member.idNumber = json.getString("pIdentNo");
			member.mobile = json.getString("pMobileNo");
			member.platformId = platformId;
			member.platformMemberId = memberId;
			member.memberId = platformId;
			member.platformMembername = memberName;
			
			Map<String, String> info = member.add();
			
			String content = "您在资金托管平台注册的用户名：" + info.get("name") + "  密码：" + info.get("password");
			
			EmailUtil.sendEmail(json.getString("pEmail"), "注册信息", content);
		}
	}
}
