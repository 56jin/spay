package controllers.YEE;

import java.io.File;
import java.io.FileInputStream;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import play.Logger;

import net.sf.json.JSONObject;

import com.shove.security.Encrypt;

import services.YEE;
import utils.Converter;
import utils.ErrorInfo;
import utils.PaymentUtil;
import business.DealDetail;
import business.GateWay;
import constants.Constants;
import constants.YEEConstants;
import controllers.Application;
import controllers.BaseController;

public class YEEPayment extends BaseController {
	
	private static YEEPayment instance = null; 

	public static YEEPayment getInstance() {
		if (instance == null) {
			synchronized (YEEPayment.class) {
				if (instance == null) {
					instance = new YEEPayment();
				}
			}
		}
		return instance;
	}

    /**
     * 易宝业务入口(放入正式环境前要去掉的两个方法，一个是日志，一个是充值（pd测试）用的)
     * @param getWayId  第三方接口id
     * @param domain  商户号
     * @param type  操作类型
     * @param platformId  平台的唯一id
     * @param memberId  会员id
     * @param memberName  会员名称
     * @param arg3DesXmlPara  P2P传过来的的解密内容
     * @param argSign  
     * @param argIpsAccount
     * @param autoInvest  自动投标标识
     * @param wsUrl
     * @param argeXtraPara  P2P传过来的的解密内容
     */
	public void transfer(int getWayId, String domain, int type, int platformId, long memberId, String memberName, 
			String argSign, String argIpsAccount, String autoInvest, String wsUrl, String arg3DesXmlPara, String argeXtraPara, String isWs) {
        ErrorInfo error = new ErrorInfo();
        
        Logger.info("------------------------易宝调试-------------------------------");
		
		//通过getWayId得到商户号
		GateWay gateWay = new GateWay();
		gateWay.id = (long)getWayId;
		String argMerCode = gateWay.pid;
		
		JSONObject jsonMark = new JSONObject();
		JSONObject json = new JSONObject();
		JSONObject jsonXtraPara = null;  
		String summary = null;
		
		//用户查询不需要下面数据
		if(type != YEEConstants.ACCOUNT_BALANCE && type != YEEConstants.USER_INFO){
			//与使用平台的校验
//			if(!PaymentUtil.expansionCheckSign(argMerCode+arg3DesXmlPara+argeXtraPara, argSign)) {
//				Logger.info("------------------------资金托管平台校验失败-------------------------------");
//				flash.error("sign校验失败");
//				Application.error();
//			}
			
			//3DES解密得到json值
			arg3DesXmlPara = Encrypt.decrypt3DES(arg3DesXmlPara, Constants.ENCRYPTION_KEY);
			
			json = (JSONObject)Converter.xmlToObj(arg3DesXmlPara);
			
			if(null == json){
				Logger.info("argeXtraPara出现空值");
				flash.error("解析参数有误");
				Application.error();
			}
			
			/*判断订单号是否已存在*/
			if(type == YEEConstants.TRANSFER && type != YEEConstants.REPAYMENT && type == YEEConstants.TRANSFER_USER_TO_MER
					&& type != YEEConstants.TRANSFER_MER_TO_USERS && type != YEEConstants.TRANSFER_MER_TO_USER 
					&& type != YEEConstants.REGISTER_CRETANSFER){
				if(DealDetail.isSerialNumberExist(platformId, json.getString("pMerBillNo")) || 
						DealDetail.isExistOfEvent(platformId, json.getString("pMerBillNo"))) {
					flash.error("已提交处理，请勿重复提交");
					
					Application.error();
				}
			}
			
			if(!StringUtils.isBlank(argeXtraPara)){
				argeXtraPara = Encrypt.decrypt3DES(argeXtraPara, Constants.ENCRYPTION_KEY);
				jsonXtraPara = (JSONObject)Converter.xmlToObj(argeXtraPara);
				
				if(null == jsonXtraPara){
					Logger.info("argeXtraPara出现空值");
					flash.error("解析参数有误");
					Application.error();
				}
			}
			
			//jsonMark用来封装下面信息保存到交易记录或事件中
			jsonMark.put("pWebUrl", ""); //后面有用到的接口会覆盖这个属性值，最开始给个空值
			jsonMark.put("type", type);
			jsonMark.put("memberId", memberId+"");
			jsonMark.put("pMerBillNo", json.getString("pMerBillNo"));
			jsonMark.put("platformId", platformId+"");
			jsonMark.put("domain", domain);
			jsonMark.put("pMemo1", "");
			jsonMark.put("pMemo2", "");
			jsonMark.put("pMemo3", "");

			if(json.containsKey("pMemo1")){
				jsonMark.put("pMemo1", json.get("pMemo1"));
			}
			
			if(json.containsKey("pMemo2")){
				jsonMark.put("pMemo2", json.get("pMemo2"));
			}
			
			if(json.containsKey("pMemo3")){
				jsonMark.put("pMemo3", json.get("pMemo3"));
			}
			
			if(type != YEEConstants.QUERY_TRADE){
				jsonMark.put("pS2SUrl", json.getString("pS2SUrl"));
			}
			
			//解除冻结资金没有这些字段
			if(type != YEEConstants.UNFREZZ_AMOUNT && type != YEEConstants.QUERY_TRADE){
				jsonMark.put("pMemo1", json.getString("pMemo1"));
				
//				//商户转用户没有传这两个值
//				if(type != YEEConstants.TRANSFER_MER_TO_USER && type != YEEConstants.TRANSFER_MER_TO_USERS){
//					jsonMark.put("pMemo2", json.getString("pMemo2"));
//					jsonMark.put("pMemo3", json.getString("pMemo3"));
//				}
				
				//充值操作添加此字段
				if(YEEConstants.RECHARGE == type){
					jsonMark.put("amount", json.getString("pTrdAmt"));
				}
				
				//用户转商户操作添加此字段
				if(YEEConstants.TRANSFER_USER_TO_MER == type){
					jsonMark.put("amount", jsonXtraPara.getString("TransAmt"));
				}
			}
		}
		
		summary = jsonMark.toString();
		
		Map<String, String> args = null;
		String result = null;
		
		/*自动投标*/
		if(YEEConstants.AUTO_INVEST.equals(autoInvest)) {
			type = YEEConstants.AUTO_INVEST_BID;
		}
		
		switch(type){
		case YEEConstants.USER_INFO:  //用户信息查询
			result =YEE.queryUserInfo(argMerCode, memberId);
			renderText(result);
			break;
		case YEEConstants.ACCOUNT_BALANCE:  //用户信息查询
			result =YEE.queryUserInfo(argMerCode, memberId);
			renderText(result);
			break;
		case YEEConstants.QUERY_TRADE:  //交易记录查询 
			result = YEE.query(argMerCode, json, jsonXtraPara);
			renderText(result);
			break;
		case YEEConstants.UNFREZZ_AMOUNT:  //解冻资金
			json.put("oldMerBillNo", json.getString("pP2PBillNo"));
			result = YEE.revocationTransfer(platformId, argMerCode, json, jsonMark);
			renderText(result);
			break;
		case YEEConstants.RECHARGE:  //充值（只在测试环境调用,正式环境用YEE.rechargeCall()方法）
			args = YEE.rechargeCallTest(argMerCode, json, jsonXtraPara, memberId);
			break;
		case YEEConstants.REGISTER_SUBJECT:  //标的登记和流标
			args = YEE.registerBidContr(argMerCode, platformId, type, json, memberId, jsonXtraPara, summary);
			break;
		case YEEConstants.TRANSFER:  //转账
			if(isWs.equals("Y") && json.getString("pTransferType").equals("3")){
				renderText("form_post");
				
			}else if(json.getString("pTransferType").equals("3")){  //代偿还款
				jsonMark.put("pWebUrl", jsonXtraPara.getString("pWebUrl"));  
				jsonMark.put("type", YEEConstants.TRANSFER_USER_TO_MER);
				jsonMark.put("pMemo1", json.getString("pMemo1"));
				jsonMark.put("pMemo3", json.getString("pMemo3"));
				summary = jsonMark.toString();
				args = YEE.offerUserToMerchant(platformId, memberId, json, argMerCode, summary, jsonXtraPara, error);
				render("YEE/YEEPayment/transfer.html",args);
				
			}
			
			if(json.getString("pTransferType").equals("2")){
				jsonMark.put("type", YEEConstants.TRANSFER_MER_TO_USERS);
				summary = jsonMark.toString();
			}
			
			result = YEE.transferContr(platformId, memberId, json, jsonXtraPara, argMerCode, summary, error);
			renderText(result);
			break;
		case YEEConstants.TRANSFER_MER_TO_USERS:  //商户转用户(发送投标奖励)
			args = YEE.plateToTransfers(platformId, memberId, json, jsonXtraPara, argMerCode, summary, error);
			break;
		case YEEConstants.TRANSFER_MER_TO_USER:  //平台划款（WS商户转用户）
			result = YEE.plateToTransfer(platformId, memberId, json, jsonXtraPara, argMerCode, summary, error);
			renderText(result);
			break;
		case YEEConstants.AUTO_INVEST_BID:  //自动投标
			result = YEE.antoTransfer(platformId, memberId, json, jsonXtraPara, argMerCode, summary, error);
			renderText(result);
			break;
		default:  //post请求
			args = YEE.entrance(type, platformId, memberId, memberName, argMerCode, json, jsonMark, jsonXtraPara, memberId, memberName, domain, error);
		}
		
		if(error.code < 0){
			flash.error(error.msg);
			Application.error();
		}
		
		Logger.info("------------------------WEB请求参数到易宝==:"+args+"-------------------------------");
		
		render("YEE/YEEPayment/transfer.html",args);
	}
	
	/**
	 * 同步通知
	 * @param resp xml格式的资金托管方返回数据
	 * @param sign 签名
	 */
    public static void callBack(String resp, String sign){
    	ErrorInfo error = new ErrorInfo();
		Logger.info("---------同步通知返回结果:"+resp+"\n---------同步通知返回校验字符串:"+sign);
		
//		boolean result = SignUtil.verifySign(resp, sign, "CUSTOMER_TXP;10000365642;hk1001001@test.com");
//		
//		if(!result){
//			Logger.info("------------------------支付平台校验失败------------------------------");
//			flash.error("sign校验失败");
//			Application.error();
//		}
		
		Map<String, String> args = YEE.exit(resp, error);
		
		if(args.containsKey("BcarStatus")){
        	if(args.get("BcarStatus").equals("1")){
        		renderText("绑卡处理成功，等待托管方认证");
        	}else{
        		renderText("绑卡不成功，请重新操作");
        	}
        }
		
		if(null == args){
        	renderText("");
		}
		
		render(args);
	}
    
    /**
     * 异步回调
     * @param notify xml格式的资金托管方返回数据
     * @param sign 签名
     */
    public static void notifys(String notify, String sign){
    	ErrorInfo error = new ErrorInfo();
		Logger.info("---------异步通知返回结果:"+notify+"\n---------异步通知返回校验字符串:"+sign);
		
//		boolean result = SignUtil.verifySign(resp, sign, "CUSTOMER_TXP;10000365642;hk1001001@test.com");
//		
//		if(!result){
//			Logger.info("------------------------支付平台校验失败------------------------------");
//			flash.error("sign校验失败");
//			Application.error();
//		}
		
        Map<String, String> args = YEE.notifyExit(notify, error);
        
        Logger.info("---------处理易宝异步回调结果"+args);
		
        if(args.containsKey("BcarStatus")){
        	if(args.get("BcarStatus").equals("1")){
        		renderText("绑卡处理成功，等待托管方认证");
        	}else{
        		renderText("绑卡不成功，请重新操作");
        	}
        }
        
        if(null == args){
        	renderText("");
		}
		
        //根据P2P返回到的情况判断出是否需要返回SUCCESS到易宝
		if("false".equals(args.get("postMark"))){
//			Logger.info("---------返回值"+args.get("postMark"));
//			if(args.get("postMark").equals("true")){
//				renderText("SUCCESS");
//				
//			}else{
//				renderText("");
//			}
			renderText("");
		}
		
		render(args);
    }
    
    //打印日志用
	public static void logFileList(String path)throws Exception{
	    path = path.replace("~", File.separator);
	    System.out.println(path);
	    File file = new File(path);
	    String[] list = null;
	    String result = "";
	    if(file.isFile()){
	    System.out.println("file");
	    result = IOUtils.toString(new FileInputStream(file),"utf-8");
	    renderText(result);
	    }else{
	    System.out.println("dir");
	    list = file.list();
	    }
	    /*File children = null;
	    for(String l : list){
	    children = new File(path+l);
	    if(children.isFile()){
	    System.out.println("isFile:"+children.getAbsolutePath());
	    }else{
	    System.out.println("isDir:"+children.getAbsolutePath());
	    }
	    }*/
	    renderJSON(list);
	   
	    }
	
	public static void testSpayCall(){
    	ErrorInfo error = new ErrorInfo();
    	
		String ser = "888658889999";
		JSONObject json = new JSONObject();
    	json.put("platformNo", "10040011137");
    	json.put("requestNo", ser);
    	json.put("code", "1");
    	
    	String resp = Converter.jsonToXml(json.toString(), "pReq", null, null, null);
		
		Logger.info("------------------------同步通知返回结果:"+resp+"------------------------------");
//		Logger.info("------------------------同步通知返回校验字符串:"+sign+"------------------------------");
		
//		boolean result = SignUtil.verifySign(resp, sign, "CUSTOMER_TXP;10000365642;hk1001001@test.com");
//		
//		if(!result){
//			Logger.info("------------------------支付平台校验失败------------------------------");
//			flash.error("sign校验失败");
//			Application.error();
//		}
		
		Logger.info("------------------------支付平台校验成功------------------------------");
		
		Map<String, String> args = YEE.exit(resp, error);
		
		if(null == args){
			if(error.code == 0){
				renderText("绑卡处理成功，等待托管方认证");
				
			}else{
				renderText("绑卡不成功，请重新操作");
			}
		}
		
		renderText("ttt");
	}
}
