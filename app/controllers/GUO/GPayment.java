package controllers.GUO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import net.sf.json.JSONObject;
import play.Logger;
import play.libs.Codec;
import play.libs.WS;
import services.GUO;
import utils.Converter;
import utils.ErrorInfo;
import utils.PaymentUtil;
import utils.WSUtil;
import business.GuoOrderDetails;

import com.shove.security.Encrypt;

import constants.ChinaPnrConstants;
import constants.Constants;
import constants.GUOConstants;
import controllers.Application;
import controllers.BaseController;

/**
 *  国付宝控制类
 * @author cp
 *	@create 2015年1月7日 下午4:14:28
 */
public class GPayment extends BaseController{
	
	private static final String view = "/GUO/GPayment/guo.html";

	/**
	 * 主入口
	 * @param domain 协议号,p2p通过与spay中对应的约定名称,做校验
	 * @param type 接口类型
	 * @param platform 平台id
	 * @param memberId p2p用户id
	 * @param membername p2p用户名称
	 * @param argMerCode 商户号
	 * @param key 缓存中的arg3DesXmlPara，argeXtraPara的缓存key
	 * @param argSign md5加密之后的校验参数
	 * @param argIpsAccount  第三方客户号
	 */
	public void guo(String domain, int type, int platform, long memberId, String membername, String argMerCode,String arg3DesXmlPara,String argeXtraPara , String argSign, String argIpsAccount) {
		
		Logger.debug("-----------guo ：接口 %s-----------", type);
		
		if(type <= 0 || type > 200) {
			flash.error("传入参数有误");
			Application.error();
		}
		
		String src = "";
		
		if(type == GUOConstants.ACCOUNT_BALANCE) {
			src = argMerCode + argIpsAccount + argeXtraPara;
		}else {
			src = argMerCode + arg3DesXmlPara + argeXtraPara;
		}
				
		
		if(!PaymentUtil.expansionCheckSign(src, argSign)) {
			Logger.debug("------------------------资金托管平台校验失败-------------------------------");
			flash.error("sign校验失败");
			Application.error();
		}
		
		arg3DesXmlPara = Encrypt.decrypt3DES(arg3DesXmlPara, Constants.ENCRYPTION_KEY);
		argeXtraPara = Encrypt.decrypt3DES(argeXtraPara, Constants.ENCRYPTION_KEY);
		
		Logger.debug("~~~~~~~P2P 平台传入参数~~~~~~~~"+"\n~~~~~~~~arg3DesXmlPara = "+arg3DesXmlPara+"\n~~~~~~~~argeXtraPara = "+argeXtraPara+"\n~~~~~~~~type ="+ type+"~~~~~~~P2P 平台传入参数~~~~~~~~");
		
		GUO guo = new GUO();
		
		guo.type = type;
		guo.platform = platform;
		guo.memberId = memberId;
		guo.memberName = membername;
		guo.argMerCode = argMerCode;
		guo.arg3DesXmlPara = arg3DesXmlPara;
		guo.argExtraPara = argeXtraPara;
		
		Map <String, String> args = null;
		if(type == GUOConstants.CREATE_ACCOUNT) {
			args = guo.createAccount();
		}else if(type == GUOConstants.USER_LOGIN) {
			args = guo.login();
		}else if(type == GUOConstants.ACCOUNT_BALANCE) {
			args = guo.queryAccount(argIpsAccount);
			String accountInfo = WSUtil.WSSubmit(GUOConstants.GUO_URL,args);
			if(StringUtils.isBlank(accountInfo)){
				renderText(null);
			}
			JSONObject accountJson = (JSONObject)Converter.xmlToObj(accountInfo);
			accountJson.put("pBalance", accountJson.getString("curBal"));
			accountJson.put("pLock", accountJson.getString("lockBal"));
			renderText(accountJson.toString());
		}else if(type == GUOConstants.REGISTER_SUBJECT) {
			String info = "";
			if("1".equals(guo.desPara.getString("pOperationType"))) {
				args = guo.createBid();
				info = WS.url(GUOConstants.GUO_URL).setParameters(args).post().getString();
				Logger.info("国付宝发标返回xml:\n %s", info);
				args = GUO.createBidCB(info);
				render("@GUO.GPayment.guoCommit", args);
			}else if("2".equals(guo.desPara.getString("pOperationType"))) {
				if(!"flowI".equals(guo.desPara.getString("pMemo3"))){
					args = guo.flowBid();
					info = WSUtil.WSSubmit(GUOConstants.GUO_URL,args);
					args = GUO.flowBidCB(info);
					if(null == args){
						renderText("");	
					}
					render("@GUO.GPayment.guoCommit", args);
				}else{
					
					info = guo.flowBidByWs();
					
					guo.addParamTopDesJson("pMemo1", String.valueOf(memberId));
					excuteFlowBidByWs(info,guo.desPara);
				}
			}
			renderText(info);
		}else if(type == GUOConstants.REGISTER_CREDITOR) {
			args = guo.invest();
		}else if(type == GUOConstants.RECHARGE) {
			args = guo.recharge();
		}else if(type == GUOConstants.WITHDRAWAL) {
			args = guo.withdrawal();
		}else if(type == GUOConstants.TRANSFER) {
			String info = "";
			args = guo.finishBid();
			info = WSUtil.WSSubmit(GUOConstants.GUO_URL,args);
			info = GUO.finishBidCB(info);
			if(null == info){
				flash.error("参数解析有误，请重试");
				Application.error();
			}
			renderText(info);
		}else if(type == GUOConstants.REPAYMENT) {
			args = guo.repayment();
		}else if(type == GUOConstants.QUERY_TRADE){
			String result = guo.analysisQueryTrade();
			if(null == result){
				flash.error("参数解析有误，请重试");
				Application.error();
			}
			renderJSON(result);
		}
		List<String> keys = new ArrayList<String>(args.keySet());
		StringBuffer argsHtml = new StringBuffer();
		
		
		
		argsHtml.append("<form id=\"gopaysubmit\" name=\"gopaysubmit\" action=\"" + GUOConstants.GUO_URL
    			+ "\" method=\"" + "POST"
    			+ "\">");
    	for (int i = 0; i < keys.size(); i++) {
    		String name = (String) keys.get(i);
    		String value = (String) args.get(name);
    		
    		Logger.debug(" %s : %s", name,value);
    		
    		argsHtml.append("<input type=\"hidden\" name=\"" + name + "\" value=\"" + value + "\"/>");
    	}
    	Logger.info(" ReqParams: %s", args.toString());
    	argsHtml.append("<script>document.gopaysubmit.submit();</script>");
    	
    	String argsForm = argsHtml.toString();
    	
    	String tranCode = args.get("tranCode");
    	String cmdName = CommonUtils.getTypeName(tranCode);
    	Logger.info("---------[%s][%s]preSubmit---------", tranCode,cmdName);
    	Logger.info("---------ReqestParams:%s---------", args.toString());
		render(view,argsForm);
	}
	
	/**
	 * 流标异步处理（ws处理方式）
	 * @param result 国付宝结果
	 * @param json  p2p参数,用于返回p2p
	 */
	private static void excuteFlowBidByWs(String result,JSONObject json){
		if(StringUtils.isBlank(result)){
			return;
		}
		
		String jsonValue = Converter.xmlToJson(result);
		JSONObject jsonObj = new JSONObject().fromObject(jsonValue);
		
		String pErrCode = "";
		String pErrMsg = "";
		String pMerCode = jsonObj.getString("merId");
		if("0000".equals(jsonObj.getString("respCode"))) {
			pErrCode = "MG00000F"; //标的结束
			pErrMsg = "流标成功";
		}else {
			pErrCode = "MG00001F";
			pErrMsg = jsonObj.getString("msgExt");
		}
		
		JSONObject returnJson = new JSONObject();
		returnJson.put("pErrCode", pErrCode);
		returnJson.put("pErrMsg", pErrMsg);
		returnJson.put("pMerCode", pMerCode);
		String p3DesXmlPara = buildP3DesXmlPara(json);
		returnJson.put("p3DesXmlPara", p3DesXmlPara);
		String pSign = hasMD5(pMerCode,pErrCode,pErrMsg,p3DesXmlPara);
		returnJson.put("pSign", pSign);
		renderJSON(returnJson.toString());
		
	}
	
	/**
	 * 用于响应p2p中的pSign加密方法
	 * @param args 字符串数组
	 * @return 加密后value
	 */
	private static String hasMD5(String... args){
		if(args != null){
			StringBuffer buffer = new StringBuffer();
			for(String value : args){
				buffer.append(value);
			}
			buffer.append(Constants.ENCRYPTION_KEY);
			return Codec.hexMD5(buffer.toString());
		}
		Logger.error("hasMD5加密时,缺少参数");
		return "";
	}
	
	/**
	 * 构造buildP3DesXmlPara
	 * 
	 * @param jsonParams
	 * @return
	 */
	private static String buildP3DesXmlPara(JSONObject jsonParams) {
		String result = "";
		try {

			result = Converter.jsonToXml(jsonParams.toString(), "pReq", null,
					null, null);

		} catch (Exception e) { // 手动捕获异常,可能会存在Xtream转化出现异常

			Logger.error("buildP3DesXmlPara 时 %s", e.getMessage());
			return "{\"RespCode\":\"999\",\"RespDesc\":\"buildP3DesXmlPara异常\"}";
		}
		result = Encrypt.encrypt3DES(result, Constants.ENCRYPTION_KEY);
		return result;
	}
	
	
	/**
	 * 国付宝同步回调
	* @param version 网关版本号
	 * @param charset 字符集
	 * @param signType 加密方式
	 * @param tranCode 交易代码
	 * @param merId 商户代码
	 * @param merName 商户名称
	 * @param tranAmt 投资金额
	 * @param payType 支付方式
	 * @param feeAmt 国付宝手续费
	 * @param feePayer 国付宝手续承担方
	 * @param frontMerUrl 商户前台通知地址
	 * @param tranDateTime 交易时间
	 * @param contractNo 提现专属账户的签约协议号
	 * @param p2pUserId P2p用户在国付宝平台的用户ID
	 * @param virCardNo 国付宝虚拟账号
	 * @param merOrderNum 订单号
	 * @param mercFeeAm P2P平台佣金
	 * @param backgroundMerUrl 商户后台通知地址
	 * @param respCode 响应码
	 * @param msgExt 响应结果信息
	 * @param customerId P2P平台用户ID
	 * @param mobilePhone 开通用户的手机号
	 * @param extantAmt 留存金额
	 * @param orderId 国付宝内部订单号
	 * @param bidId 标号
	 * @param tranFinishTime 交易完成时间
	 * @param mercFeeAmt P2P平台佣金
	 * @param bankPayAmt 银行卡支付金额
	 * @param vcardPayAmt 国付宝虚拟卡支付金额
	 * @param curBal 投资人国付宝虚拟卡可用余额
	 * @param repaymentType 还款类型
	 * @param isInFull 是否全额还款
	 * @param repaymentInfo 还款信息
	 * @param repaymentChargeFeeAmt 还款充值手续费
	 * @param repaymentChargeFeePayer 还款充值手续费承担方
	 * @param tranIP 用户浏览器IP
	 * @param signValue 加密串
	 */
	public static void guoCommit(String version, String charset, String signType, String tranCode, String merId, 
			String merName,String tranAmt, String payType, String feeAmt, String feePayer, String frontMerUrl,
			String tranDateTime, String contractNo, String p2pUserId, String virCardNo, String merOrderNum, String mercFeeAm,
			String backgroundMerUrl, String respCode, String msgExt, String customerId, String mobilePhone, 
			String extantAmt, String orderId, String bidId, String tranFinishTime, String mercFeeAmt, String bankPayAmt,
			String vcardPayAmt, String curBal, String repaymentType, String isInFull, String repaymentInfo, String repaymentChargeFeeAmt,
			String repaymentChargeFeePayer, String tranIP, String signValue) {
		Logger.info("------------------国付宝同步回调参数start------------------");
		
		printParamsByStringArr( version,  charset,  signType,  tranCode,  merId, 
				 merName, tranAmt,  payType,  feeAmt,  feePayer,  frontMerUrl,
				 tranDateTime,  contractNo,  p2pUserId,  virCardNo,  merOrderNum,  mercFeeAm,
				 backgroundMerUrl,  respCode,  msgExt,  customerId,  mobilePhone, 
				 extantAmt,  orderId,  bidId,  tranFinishTime,  mercFeeAmt,  bankPayAmt,
				 vcardPayAmt,  curBal,  repaymentType,  isInFull,  repaymentInfo,  repaymentChargeFeeAmt,
				 repaymentChargeFeePayer,  tranIP,  signValue);
		Logger.info("------------------国付宝同步回调参数start------------------");
		
		GuoOrderDetails guoOrderDetails = new GuoOrderDetails();
		ErrorInfo error = new ErrorInfo();
		
		guoOrderDetails.saveOrder(version,  charset,  signType,  tranCode,  merId, 
				 merName, tranAmt,  payType,  feeAmt,  feePayer,  frontMerUrl,
				 tranDateTime,  contractNo,  p2pUserId,  virCardNo,  merOrderNum,  mercFeeAm,
				 backgroundMerUrl,  respCode,  msgExt,  customerId,  mobilePhone, 
				 extantAmt,  orderId,  bidId,  tranFinishTime,  mercFeeAmt,  bankPayAmt,
				 vcardPayAmt,  curBal,  repaymentType,  isInFull,  repaymentInfo,  repaymentChargeFeeAmt,
				 repaymentChargeFeePayer,  tranIP,  signValue, error);
		
		if(error.code<0){
			Application.error(error.msg);
		}
		
		Logger.debug("------------------------frontUrl响应------------------------------");
		
		Map<String, String> args = null;
		if(GUOConstants.CMD_RECHARGE.equals(tranCode)) {
			
			args = GUO.rechargeCB(version, charset, signType, tranCode,
					merOrderNum, tranDateTime, signValue, respCode, msgExt, merId,
					merName, contractNo, mobilePhone, payType, tranAmt, feeAmt,
					feePayer, frontMerUrl, backgroundMerUrl);
		}else if(GUOConstants.CMD_WITHDRAWAL.equals(tranCode)) {
			
			args = GUO.withdrawalCB(version, charset, signType, tranCode, merOrderNum, tranDateTime, signValue, respCode, msgExt, 
					customerId, merName, contractNo, mobilePhone, tranAmt, mercFeeAm, feeAmt, feePayer, frontMerUrl, backgroundMerUrl);
		}else if(GUOConstants.CMD_INVEST.equals(tranCode)) {
			
			args = GUO.investCB(version, tranCode, signType, merId, merName, contractNo, bidId, mobilePhone, merOrderNum, tranAmt, 
					extantAmt, tranDateTime, tranIP, respCode, msgExt, orderId, tranFinishTime, frontMerUrl, backgroundMerUrl, feeAmt, 
					mercFeeAmt, payType, bankPayAmt, vcardPayAmt, curBal, signValue);
		}else if(GUOConstants.CMD_REPAYMENT.equals(tranCode)) {
			
			args = GUO.repaymentCB(version, charset, signType, tranCode, merOrderNum, tranDateTime, signValue, respCode, msgExt, 
					merId, merName, contractNo, mobilePhone, bidId, tranAmt, repaymentType, mercFeeAmt, isInFull, repaymentInfo, 
					repaymentChargeFeeAmt, repaymentChargeFeePayer, payType, bankPayAmt, vcardPayAmt, curBal);
		}
		
		if(null == args) {
			Logger.info("------------------------支付平台校验失败------------------------------");
			flash.error("sign校验失败");
			Application.error();
		}
		
		Logger.debug("------------------------支付平台校验成功------------------------------");
		
		Logger.info("------------------------中间件同步回调至P2P参数:%s------------------------", args.toString());
		render(args);
	}
	
	/**
	 * 国付宝异步回调
	 * @param version 网关版本号
	 * @param charset 字符集
	 * @param signType 加密方式
	 * @param tranCode 交易代码
	 * @param merId 商户代码
	 * @param merName 商户名称
	 * @param tranAmt 投资金额
	 * @param payType 支付方式
	 * @param feeAmt 国付宝手续费
	 * @param feePayer 国付宝手续承担方
	 * @param frontMerUrl 商户前台通知地址
	 * @param tranDateTime 交易时间
	 * @param contractNo 提现专属账户的签约协议号
	 * @param p2pUserId P2p用户在国付宝平台的用户ID
	 * @param virCardNo 国付宝虚拟账号
	 * @param merOrderNum 订单号
	 * @param mercFeeAm P2P平台佣金
	 * @param backgroundMerUrl 商户后台通知地址
	 * @param respCode 响应码
	 * @param msgExt 响应结果信息
	 * @param customerId P2P平台用户ID
	 * @param mobilePhone 开通用户的手机号
	 * @param extantAmt 留存金额
	 * @param orderId 国付宝内部订单号
	 * @param bidId 标号
	 * @param tranFinishTime 交易完成时间
	 * @param mercFeeAmt P2P平台佣金
	 * @param bankPayAmt 银行卡支付金额
	 * @param vcardPayAmt 国付宝虚拟卡支付金额
	 * @param curBal 投资人国付宝虚拟卡可用余额
	 * @param repaymentType 还款类型
	 * @param isInFull 是否全额还款
	 * @param repaymentInfo 还款信息
	 * @param repaymentChargeFeeAmt 还款充值手续费
	 * @param repaymentChargeFeePayer 还款充值手续费承担方
	 * @param tranIP 用户浏览器IP
	 * @param signValue 加密串
	 */
	public static void guoCommitAsynch(String version, String charset, String signType, String tranCode, String merId, 
			String merName,String tranAmt, String payType, String feeAmt, String feePayer, String frontMerUrl,
			String tranDateTime, String contractNo, String p2pUserId, String virCardNo, String merOrderNum, String mercFeeAm,
			String backgroundMerUrl, String respCode, String msgExt, String customerId, String mobilePhone, 
			String extantAmt, String orderId, String bidId, String tranFinishTime, String mercFeeAmt, String bankPayAmt,
			String vcardPayAmt, String curBal, String repaymentType, String isInFull, String repaymentInfo, String repaymentChargeFeeAmt,
			String repaymentChargeFeePayer, String tranIP, String signValue) {
		Logger.info("------------------------国付宝异步回调参数start------------------------");
		
		printParamsByStringArr( version,  charset,  signType,  tranCode,  merId, 
				 merName, tranAmt,  payType,  feeAmt,  feePayer,  frontMerUrl,
				 tranDateTime,  contractNo,  p2pUserId,  virCardNo,  merOrderNum,  mercFeeAm,
				 backgroundMerUrl,  respCode,  msgExt,  customerId,  mobilePhone, 
				 extantAmt,  orderId,  bidId,  tranFinishTime,  mercFeeAmt,  bankPayAmt,
				 vcardPayAmt,  curBal,  repaymentType,  isInFull,  repaymentInfo,  repaymentChargeFeeAmt,
				 repaymentChargeFeePayer,  tranIP,  signValue);
		Logger.info("------------------------国付宝异步回调参数end------------------------");
		GuoOrderDetails guoOrderDetails = new GuoOrderDetails();
		ErrorInfo error = new ErrorInfo();
		
		guoOrderDetails.saveOrder(version,  charset,  signType,  tranCode,  merId, 
				 merName, tranAmt,  payType,  feeAmt,  feePayer,  frontMerUrl,
				 tranDateTime,  contractNo,  p2pUserId,  virCardNo,  merOrderNum,  mercFeeAm,
				 backgroundMerUrl,  respCode,  msgExt,  customerId,  mobilePhone, 
				 extantAmt,  orderId,  bidId,  tranFinishTime,  mercFeeAmt,  bankPayAmt,
				 vcardPayAmt,  curBal,  repaymentType,  isInFull,  repaymentInfo,  repaymentChargeFeeAmt,
				 repaymentChargeFeePayer,  tranIP,  signValue, error);
		
		if(error.code<0){
			Application.error(error.msg);
		}
		
		Map<String, String> args = null;
		
		if(GUOConstants.CMD_RECHARGE.equals(tranCode)) {
			
			args = GUO.rechargeCB(version, charset, signType, tranCode,
					merOrderNum, tranDateTime, signValue, respCode, msgExt, merId,
					merName, contractNo, mobilePhone, payType, tranAmt, feeAmt,
					feePayer, frontMerUrl, backgroundMerUrl);
		}else if(GUOConstants.CMD_WITHDRAWAL.equals(tranCode)) {
			
			args = GUO.withdrawalCB(version, charset, signType, tranCode, merOrderNum, tranDateTime, signValue,
					respCode, msgExt, merId, merName, contractNo, mobilePhone, tranAmt, mercFeeAmt, 
					repaymentChargeFeeAmt, repaymentChargeFeePayer, frontMerUrl, backgroundMerUrl);
		}else if(GUOConstants.CMD_INVEST.equals(tranCode)) {
			
			args = GUO.investCB(version, tranCode, signType, merId, merName, contractNo, bidId, mobilePhone, merOrderNum, tranAmt, 
					extantAmt, tranDateTime, tranIP, respCode, msgExt, orderId, tranFinishTime, frontMerUrl, backgroundMerUrl, feeAmt, 
					mercFeeAmt, payType, bankPayAmt, vcardPayAmt, curBal, signValue);
		}else if(GUOConstants.CMD_REPAYMENT.equals(tranCode)) {
			
			args = GUO.repaymentCB(version, charset, signType, tranCode, merOrderNum, tranDateTime, signValue, respCode, msgExt, 
					merId, merName, contractNo, mobilePhone, bidId, tranAmt, repaymentType, mercFeeAmt, isInFull, repaymentInfo, 
					repaymentChargeFeeAmt, repaymentChargeFeePayer, payType, bankPayAmt, vcardPayAmt, curBal);
		}else if(GUOConstants.CMD_CREATE_ACCOUNT.equals(tranCode)){
			
			args = GUO.createAccountCB(version, tranCode, p2pUserId, virCardNo, merOrderNum, 
					tranDateTime, signValue, respCode, msgExt, merId, merName, contractNo, mobilePhone,
					customerId, tranIP, backgroundMerUrl);
		}
		
		if(args!=null){
			
			Logger.debug("------------------------国付宝异步回调参数start------------------------");
			printParams(args);
			Logger.info("------------------中间件异步回调至P2P参数:%s------------------", args.toString());
			Logger.debug("------------------------guoCommitAsynch 异步参数end------------------------");
		}
		
		if(null == args) {
			
			Logger.info("------------------------支付平台校验失败------------------------------");
			flash.error("sign校验失败");
			Application.error();
		}
		
		Logger.debug("------------支付平台校验成功------------backgroundUrl响应end------------------------------");
		
		if(P2pCommonUtils.isAsynchSoapNames(tranCode)){
			
		}
		WS.url(args.get("backgroundUrl")).setParameters(args).post();
	}
	
	/**
	 * 打印参数
	 * @param args 国付宝返回参数数组
	 */
	private static void printParamsByStringArr(String ... args){
		String[] keys = {"version","  charset","  signType","  tranCode","  merId"," merName"," tranAmt","  payType","  feeAmt","  feePayer","  frontMerUrl",
				" tranDateTime","  contractNo","  p2pUserId","  virCardNo","  merOrderNum","  mercFeeAm","backgroundMerUrl","  respCode","  msgExt",
				"  customerId","  mobilePhone"," extantAmt","  orderId","  bidId","  tranFinishTime","  mercFeeAmt","  bankPayAmt"," vcardPayAmt",
				"  curBal","  repaymentType","  isInFull","  repaymentInfo","  repaymentChargeFeeAmt"," repaymentChargeFeePayer","  tranIP","  signValue"};
		for(int i = 0 ;i<args.length;i++){
			Logger.info("%s : %s", keys[i],args[i]);
		}
	}
	
	/**
	 * 打印参数,用于spay请求国付宝
	 * @param maps
	 */
	private static void printParams(Map<String,String> maps){
		Logger.debug("------------------printParams-------------------");
		Set<Entry<String, String>> set = maps.entrySet();
		for(Entry<String, String> entry : set){
			Logger.debug("%s : %s", entry.getKey(),entry.getValue());
		}
		Logger.debug("------------------printParams-------------------");
	}
	
	
public static class P2pCommonUtils{
		
		/**
		 * P2p支持的ws回调接口名称
		 */
		private static List<String> AsynchSoapNames = null;
		
		static {
			if(AsynchSoapNames == null)
				initAsynchSoapNames();
		}
		
		private static void initAsynchSoapNames(){
			AsynchSoapNames = new ArrayList<String>();
			AsynchSoapNames.add(GUOConstants.CMD_INVEST);
			AsynchSoapNames.add(GUOConstants.CMD_REPAYMENT);
			AsynchSoapNames.add(GUOConstants.CMD_RECHARGE);
			AsynchSoapNames.add(GUOConstants.CMD_WITHDRAWAL);
		}
		
		/**
		 * 是否支持ws异步接口
		 * @param cmdId
		 * @return
		 */
		public static boolean isAsynchSoapNames(String cmdId){
			return AsynchSoapNames.contains(cmdId);
		}
		
	}

/**
 * 通用工具类
 * @author lian
 *
 */
	public static class CommonUtils{
		
		private static Map<String,String> maps = null;
		
		static{
			if(maps==null){
				initMaps();
			}
		}
		
		private static void initMaps(){
			maps = new HashMap<String, String>();
			maps.put("P007", "开户");
			maps.put("P008", "登入");
			maps.put("P006", "专属账户余额查询");
			maps.put("P009", "专属账户充值");
			maps.put("P010", "专属账户提现");
			maps.put("P001", "投标接口");
			maps.put("P000", "发标");
			maps.put("P002", "流标");
			maps.put("P003", "投标完成");
			maps.put("P004", "还款");
			maps.put("P013", "查询单笔交易状态");
		}
		
		public static String getTypeName(String tranCode){
			return maps.get(tranCode);
		}
		
	}
}
