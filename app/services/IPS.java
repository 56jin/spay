package services;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import play.Logger;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.mvc.Http.Request;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import business.DealDetail;
import business.Information;
import business.Member;
import business.Platform;

import com.shove.security.Encrypt;

import utils.Converter;
import utils.EmailUtil;
import utils.ErrorInfo;
import utils.IPSUtil;
import utils.PaymentUtil;
import utils.WSUtil;
import utils.XmlUtil;
import constants.Constants;
import constants.IPSConstants;
import controllers.Application;

public class IPS {

	/**
	 * 对使用平台的请求信息进行转化处理，转化成能调用第三方支付的请求参数
	 * @param type 请求接口类型
	 * @param platformId 平台id
	 * @param platformMemberId 使用平台的用户的id
	 * @param platformUsername 使用平台的用户的用户名
	 * @param argMerCode
	 * @param arg3DesXmlPara
	 * @param argSign
	 * @return 请求第三方支付需要的参数
	 */
	public static Map<String, String> entrance(int type, int platformId, long platformMemberId, String platformUsername, 
			String argMerCode, String arg3DesXmlPara, String argSign) {
		Logger.info("arg3DesXmlPara:"+arg3DesXmlPara);
		
		JSONObject json = (JSONObject)Converter.xmlToObj(arg3DesXmlPara);
		
		if(null == json){
			Logger.info("参数json解析有误");
			 
			return null;
		}
		
		/*将使用平台的pWebUrl和pS2SUrl等信息放入pMemo1中，将本平台的回调方法替换使用平台的pWebUrl，pS2SUrl*/
		json.put("pMemo1", "<![CDATA["+json.getString("pWebUrl")+";"+type+";"+platformId+";"+platformMemberId+";"+platformUsername+"]]>");
		json.put("pMemo2", "<![CDATA["+json.getString("pS2SUrl")+"]]>");
		json.put("pWebUrl", "<![CDATA["+IPSConstants.IPS_ACCOUNT_WEB+"]]>");
		json.put("pS2SUrl", "<![CDATA["+IPSConstants.IPS_ACCOUNT_S2S+"]]>");
		
		arg3DesXmlPara = Converter.jsonToXml(json.toString(), "pReq", "pRow", null, null);
		
		String serialNumber = json.getString("pMerBillNo");
		
		/*获取请求信息，保存订单号，请求平台，金额等信息*/
		if (!DealDetail.isSerialNumberExist(platformId, serialNumber)) {
			switch (type) {
			case IPSConstants.REGISTER_SUBJECT:
				registerSubject(platformId, platformMemberId, json);
				break;
			case IPSConstants.REGISTER_CREDITOR:
				registerCreditor(platformId, platformMemberId, json);
				break;
			case IPSConstants.REGISTER_GUARANTOR:
				registerGuarantor(platformId, platformMemberId, json);
				break;
			case IPSConstants.REGISTER_CRETANSFER:
				registerCretansfer(platformId, platformMemberId, json);
				break;
			case IPSConstants.RECHARGE:
				doDpTrade(platformId, platformMemberId, json);
				break;
			case IPSConstants.TRANSFER:
				transfer(platformId, platformMemberId, json);
				break;
			case IPSConstants.REPAYMENT:
				repaymentNewTrade(platformId, platformMemberId, json);
				break;
			case IPSConstants.UNFREEZE:
				guaranteeUnfreeze(platformId, platformMemberId, json);
				break;
			case IPSConstants.DEDUCT:
				coDp(platformId, platformMemberId, json);
				break;
			case IPSConstants.WITHDRAWAL:
				doDwTrade(platformId, platformMemberId, json);
				break;
			default:
				break;
			}
		}
		
		/*将使用平台的请求信息转化成第三方支付的请求参数*/
        Map<String, String> argsMap = new HashMap<String, String>();
        argsMap.put("argMerCode", argMerCode);
        Logger.info("--argMerCode:"+argMerCode+"\n--arg3DesXmlPara="+arg3DesXmlPara+"\n--DES_KEY:"+IPSConstants.DES_KEY+"\n--DES_IV:"+IPSConstants.DES_IV+"\n--CERT_MD5:"+IPSConstants.CERT_MD5);
        arg3DesXmlPara = com.ips.security.utility.IpsCrypto.triDesEncrypt(arg3DesXmlPara,IPSConstants.DES_KEY,IPSConstants.DES_IV);
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\r", "");
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\n", "");
        argsMap.put("arg3DesXmlPara", arg3DesXmlPara);
        argsMap.put("argSign", com.ips.security.utility.IpsCrypto.md5Sign(argMerCode+arg3DesXmlPara+IPSConstants.CERT_MD5));
        argsMap.put("url", IPSConstants.IPS_URL[type]);
        Logger.info("--arg3DesXmlPara:"+arg3DesXmlPara+"\n--argSign="+com.ips.security.utility.IpsCrypto.md5Sign(argMerCode+arg3DesXmlPara+IPSConstants.CERT_MD5));
        String prefix = "1";
        
        /*当prefix=0时，请求参数前缀为arg;prefix=1时，请求参数前缀为p*/
        if(type == IPSConstants.CREATE_ACCOUNT ) {
            	prefix = "0";
    	}
        
//        if(type == IPSConstants.REGISTER_SUBJECT || type == IPSConstants.AUTO_SIGING || type == IPSConstants.REPAYMENT_SIGNING
//        	|| type == IPSConstants.RECHARGE || type == IPSConstants.WITHDRAWAL || type == IPSConstants.REGISTER_CREDITOR 
//        	|| type == IPSConstants.REGISTER_GUARANTOR || type == IPSConstants.REPAYMENT || type == IPSConstants.REGISTER_CRETANSFER) {
//        	prefix = "0";
//		}
        
        argsMap.put("prefix", prefix);
        return argsMap;
	}
	
	/**
	 * 对第三方支付回调的信息进行转化处理，转化成能回调使用平台的请求参数
	 * @param pMerCode
	 * @param pErrCode
	 * @param pErrMsg
	 * @param p3DesXmlPara
	 * @param pSign
	 * @return 回调使用平台所需的参数
	 */
	public static Map<String, String> exit(String pMerCode, String pErrCode, String pErrMsg, String p3DesXmlPara, String pSign) {
		p3DesXmlPara = com.ips.security.utility.IpsCrypto.triDesDecrypt(p3DesXmlPara,IPSConstants.DES_KEY,IPSConstants.DES_IV);
		Logger.info("------------------------p3DesXmlPara：----------"+p3DesXmlPara);
		/*处理第三方支付返回的xml信息中的bom*/
		byte[] bs = null;
		try {
			bs = p3DesXmlPara.getBytes("UTF-8");
			if(bs[0] == -17 && bs[1] == -69 && bs[2] == -65) {
				p3DesXmlPara = new String(bs,3,bs.length-3,"UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}   

		
		String jsonStr = Converter.xmlToJson(p3DesXmlPara);
		
		if(StringUtils.isBlank(jsonStr)){
			Logger.info("jsonStr参数解析有误");
			return null;
		}
		
		
		JSONObject json = JSONObject.fromObject(jsonStr);
		/*获得pMemo1中存入的信息，将其中的信息赋给相应的变量*/
		String[] remarks = json.getString("pMemo1").split(";");
		
		if(null == remarks){
			Logger.info("remarks参数解析有误");
			return null;
		}
		
		String pS2SUrl = json.getString("pMemo2");
		Logger.info("---pMemo1中的信息：---"+json.getString("pMemo1")+"\n---pMemo2中的信息：---"+json.getString("pMemo2"));
		int type = Integer.parseInt(remarks[1]);
		String url = remarks[0];
		int platformId = Integer.parseInt(remarks[2]);
		long platformMemberId = Long.parseLong(remarks[3]);
		String platformUsername = remarks[4];
		
		json.put("pWebUrl", url);
		json.put("pS2SUrl", pS2SUrl);
		json.put("pMemo1", platformMemberId+"");
		json.put("pMemo2", "");
		
//		boolean flag = false;
		
		DealDetail.addEvent(platformMemberId, type+200, platformId, null, null, null, null, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		/*更新请求之前的保存的信息的状态*/
		switch (type) {
		case IPSConstants.CREATE_ACCOUNT:
			if("10".equals(json.get("pStatus"))) {
				Logger.info("------------------------开户成功，资金托管平台添加支付账号------------------------------");
				createAccount(platformId, platformMemberId, platformUsername, json);
			}
			
			break;
		case IPSConstants.REGISTER_SUBJECT:
			if("1".equals(json.get("pOperationType")) && (pErrCode.equals("MG02500F") || pErrCode.equals("MG00000F"))) {
				detailCommit(platformId, json);
			}
			
			break;
		case IPSConstants.REGISTER_CREDITOR:
		case IPSConstants.REGISTER_GUARANTOR:
		case IPSConstants.REGISTER_CRETANSFER:
		case IPSConstants.UNFREEZE:
		case IPSConstants.DEDUCT:
		case IPSConstants.WITHDRAWAL:
			if("MG00000F".equals(pErrCode)) {
				detailCommit(platformId, json);
			}
			
			break;
		case IPSConstants.RECHARGE:
			if("MG00000F".equals(pErrCode)) {
				detailCommit(platformId, json);
				Platform.updateDealStatus(platformId);
			}
			
			break;
		case IPSConstants.REPAYMENT:
			/*MG00008F表示受理中*/
			if("MG00000F".equals(pErrCode) || "MG00008F".equals(pErrCode)) {
				detailCommit(platformId, json);
			}
			
			break;
		case IPSConstants.TRANSFER:
			if("MG00000F".equals(pErrCode)) {
				transferCommit(platformId, json);
			}
			
			break;
		case IPSConstants.AUTO_SIGING:
			if("MG00000F".equals(pErrCode)) {
				authInvestNumberCommit(platformId, platformMemberId, json);
			}
			
			break;
		case IPSConstants.REPAYMENT_SIGNING:
			if("MG00000F".equals(pErrCode)) {
				authPaymentNumberCommit(platformId, platformMemberId, json);
			}
			
			break;
		default:
			break;
		
		}
		
		p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(json.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		
		pSign = Encrypt.MD5(pMerCode+pErrCode+pErrMsg+p3DesXmlPara+Constants.ENCRYPTION_KEY);
	    
		System.out.println(url +"pWebUrl"+json.getString("pWebUrl"));
		
		/*如果第三方成功，而本资金托管平台操作失败，此方法是为了记录当前操作的信息，目前不需要*/
//		if(!flag) {
//			JSONObject json2 = new JSONObject();
//			
//			json2.put("url", url);
//			json2.put("pMerCode", pMerCode);
//			json2.put("pErrCode", pErrCode);
//			json2.put("pErrMsg", pErrMsg);
//			json2.put("p3DesXmlPara", p3DesXmlPara);
//			json2.put("pSign", pSign);
//			
//			Information info = new Information();
//			
//			info.information = json2.toString();
//			
//			info.add();
//			
//			url = remarks[1];
//		}
		
		Map<String, String> args = new HashMap<String, String>();
		
		args.put("url", url);
		args.put("pS2SUrl", pS2SUrl);
		args.put("pMerCode", pMerCode);
		args.put("pErrCode", pErrCode);
		args.put("pErrMsg", pErrMsg);
		args.put("p3DesXmlPara", p3DesXmlPara);
		args.put("pSign", pSign);
		
		return args;
	}
	
	/**
	 * 开户
	 * @param json
	 */
	public static boolean createAccount(int platformId, long platformMemberId, String platformUsername, JSONObject json) {
		Member.updateStatus(json.getString("pIdentNo"));
		Member.updateAccount(platformId, platformMemberId, json.getString("pIpsAcctNo"));
		
		return true;
	}
	
	/**
	 * 标的登记，保存的金额为借款金额
	 * @param platformId
	 * @param platformMemberId
	 * @param json
	 * @return
	 */
	public static boolean registerSubject(int platformId, long platformMemberId, JSONObject json) {
//		if(!"1".equals(json.get("pOperationType"))) {
//			return true;
//		}
		
		DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				IPSConstants.REGISTER_SUBJECT, json.getDouble("pLendAmt"), false, "标的登记");
		
		return detail.addDealDetail();
	}
	
	/**
	 * 登记债权人（投标），保存的金额为投标金额+手续费
	 * @param platformId
	 * @param platformMemberId
	 * @param json
	 * @return
	 */
	public static boolean registerCreditor(int platformId, long platformMemberId, JSONObject json) {
		DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				IPSConstants.REGISTER_CREDITOR, json.getDouble("pTrdAmt")+json.getDouble("pFee"), false, "登记债权人");
		
		return detail.addDealDetail();
	}
	
	/**
	 * 登记担保方，保存的金额为担保保证金
	 * @param json
	 * @return
	 */
	public static boolean registerGuarantor(int platformId, long platformMemberId, JSONObject json) {
		DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				IPSConstants.REGISTER_GUARANTOR, json.getDouble("pAmount"), false, "登记担保方");
		
		return detail.addDealDetail();
	}
	
	/**
	 * 登记债权转让，保存的金额为支付金额
	 * @param json
	 * @return
	 */
	public static boolean registerCretansfer(int platformId, long platformMemberId, JSONObject json) {
		DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				IPSConstants.REGISTER_CRETANSFER, json.getDouble("pPayAmt"), false, "登记债权转让");
		
		return detail.addDealDetail();
	}
	
	/**
	 * 充值，保存的金额为充值金额
	 * @param json
	 * @return
	 */
	public static boolean doDpTrade(int platformId, long platformMemberId, JSONObject json) {
		DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				IPSConstants.RECHARGE, json.getDouble("pTrdAmt"), false, "充值");
		
		return detail.addDealDetail();
	}
	
	/**
	 * 转账，保存的金额为转账金额
	 * @param json
	 * @return
	 */
	public static boolean transfer(int platformId, long platformMemberId, JSONObject json) {
		String pMerBillNo = json.getString("pMerBillNo");
		double amount = 0;
		
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
		
		for (Object obj : jsonArr) {
			JSONObject pRow = (JSONObject)obj;
			System.out.println();
			String serialNumber = pMerBillNo+"_"+pRow.getString("pOriMerBillNo");
			System.out.println(pRow.getString("pTrdAmt"));
			DealDetail detail = new DealDetail(platformId, Member.queryPlatMemberId(pRow.getString("pTIpsAcctNo"), platformId), serialNumber, 
					IPSConstants.REPAYMENT, pRow.getDouble("pTrdAmt"), false, "转入");
			amount += pRow.getDouble("pTrdAmt");
			
			if (!detail.addDealDetail()){
				return false;
			}
		}
		
		DealDetail detail1 = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				IPSConstants.RECHARGE, amount, false, "转账");
		
		detail1.addDealDetail();
		
		return true;
	}
	
	/**
	 * 还款，保存的金额为还款总额
	 * @param json
	 * @return
	 */
	public static boolean repaymentNewTrade(int platformId, long platformMemberId, JSONObject json) {
		Logger.info(json.toString());
		String pMerBillNo = json.getString("pMerBillNo");
		DealDetail detail1 = new DealDetail(platformId, platformMemberId, pMerBillNo, 
				IPSConstants.REPAYMENT, json.getDouble("pOutAmt"), false, "还款");
		
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
		
		for (Object obj : jsonArr) {
			JSONObject pRow = (JSONObject)obj;
			String serialNumber = pMerBillNo+"_"+pRow.getString("pCreMerBillNo");
			System.out.println(pRow.getString("pInAmt"));
			if (!DealDetail.isSerialNumberExist(platformId, serialNumber)) {
				DealDetail detail = new DealDetail(platformId, Member.queryPlatMemberId(pRow.getString("pInAcctNo"), platformId), serialNumber, 
						IPSConstants.REPAYMENT, pRow.getDouble("pInAmt"), false, "收到还款");
				
				if (!detail.addDealDetail()){
					return false;
				}
			}
		}
		
		return detail1.addDealDetail();
	}
	
	/**
	 * 解冻保证金，保存的金额为解冻金额
	 * @param json
	 * @return
	 */
	public static boolean guaranteeUnfreeze(int platformId, long platformMemberId, JSONObject json) {
		DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				IPSConstants.UNFREEZE, json.getDouble("pUnfreezeAmt"), false, "解冻保证金");
		
		return detail.addDealDetail();
	}
	
	/**
	 * 自动代扣充值(WS)，保存的金额为充值金额
	 * @param json
	 * @return
	 */
	public static boolean coDp(int platformId, long platformMemberId, JSONObject json) {
		DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				IPSConstants.DEDUCT, json.getDouble("pTrdAmt"), false, "自动代扣充值");
		
		return detail.addDealDetail();
	}
	
	/**
	 * 提现，保存的金额为提现金额
	 * @param json
	 * @return
	 */
	public static boolean doDwTrade(int platformId, long platformMemberId, JSONObject json) {
		DealDetail detail = new DealDetail(platformId, platformMemberId, json.getString("pMerBillNo"), 
				IPSConstants.WITHDRAWAL, json.getDouble("pTrdAmt"), false, "提现");
		
		return detail.addDealDetail();
	}
	
	/**
	 * 第三方返回结果后进行交易记录的状态更新
	 * @param platformId
	 * @param json
	 * @return
	 */
	public static boolean detailCommit(int platformId, JSONObject json) {
		String serialNumber = json.getString("pMerBillNo");
		
		return DealDetail.updateStatus(platformId, serialNumber);
	}
	
	/**
	 * 第三方返回结果后进行交易记录的状态更新（转账）
	 * @param platformId
	 * @param json
	 * @return
	 */
	public static boolean transferCommit(int platformId, JSONObject json) {
//		JSONArray jsonArr = null; 
//
//		Object pDetails = json.get("pDetails");
//		
//		if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
//			JSONObject pDetail = (JSONObject)pDetails; 
//			JSONObject pRow = pDetail.getJSONObject("pRow"); 
//	
//			jsonArr = new JSONArray(); 
//			jsonArr.add(pRow); 
//		} else {
//			jsonArr = json.getJSONArray("pDetails"); 
//		} 
//		
//		for (Object obj : jsonArr) {
//			JSONObject pRow = (JSONObject)obj;
//			String serialNumber = pRow.getString("pOriMerBillNo");
//			
//			return DealDetail.updateStatus(platformId, serialNumber);
//		}
		
		DealDetail.updateStatus(platformId, json.getString("pMerBillNo"));
		
		return true;
	}
	
	/**
	 * 第三方返回结果后进行交易记录的状态更新（还款，因为只记录还款的总信息，所以下面的方法不需要，保留着，如果以后更改可方便使用）
	 * @param platformId
	 * @param json
	 * @return
	 */
//	public static boolean repaymentCommit(int platformId, JSONObject json) {
//		JSONArray jsonArr = json.getJSONArray("pDetails");
//		
//		for (Object obj : jsonArr) {
//			JSONObject pRow = (JSONObject)obj;
//			String serialNumber = pRow.getString("pOriMerBillNo");
//			
//			return DealDetail.updateStatus(platformId, serialNumber);
//		}
//		
//		return true;
//	}
	
	/**
	 * 第三方返回结果后进行自动还款授权号添加
	 * @param platformId
	 * @param platformMemberId
	 * @param json
	 * @return
	 */
	public static boolean authPaymentNumberCommit(int platformId, long platformMemberId, JSONObject json) {
		String authPaymentNumber = json.getString("pIpsAuthNo");
		
		Member.updateAuthPaymentNumber(platformId, platformMemberId, authPaymentNumber);
		
		return true;
	}
	
	/**
	 * 第三方返回结果后进行自动投标授权号添加
	 * @param platformId
	 * @param platformMemberId
	 * @param json
	 * @return
	 */
	public static boolean authInvestNumberCommit(int platformId, long platformMemberId, JSONObject json) {
		String authInvestNumber = json.getString("pIpsAuthNo");
		
		Member.updateAuthPaymentNumber(platformId, platformMemberId, authInvestNumber);
		
		return true;
	}
	
	/**
	 * 账户余额查询、账户信息查询
	 * @param memberId 金融平台的用户id
	 * @param platformId 金融平台id
	 * @param type
	 * @param argMerCode
	 * @param argIpsAccount
	 * @param argMemo
	 * @return
	 */
	public static String queryUserInfo(long memberId, long platformId, int type, String argMerCode, String argIpsAccount, String argMemo) {
		String argSign = com.ips.security.utility.IpsCrypto.md5Sign(argMerCode+argIpsAccount+IPSConstants.CERT_MD5);
		String argXmlPara = IPSUtil.getSoapInputStream(type, argMerCode, argIpsAccount, argSign, argMemo);
		Logger.info("------------------------argXmlPara:-------------------------------"+argXmlPara);
		String xml = new XmlUtil().setDocument(argXmlPara).getNodeValue(type == IPSConstants.ACCOUNT_BALANCE ? "QueryForAccBalanceResult" : "QueryMerUserInfoResult");		
		
		if(StringUtils.isBlank(xml)){
			Logger.info("xml出现空值");
			return null;
		}
		
		JSONObject jsonObj = (JSONObject) Converter.xmlToObj(xml);
		String userIfo = jsonObj.toString();
		Logger.info(userIfo);
		
		String pErrMsg = type == IPSConstants.ACCOUNT_BALANCE ? ";pErrMsg:"+jsonObj.getString("pErrMsg") : "";
		DealDetail.addEvent(memberId, type, platformId, null, null, null, null, "pErrCode:"+jsonObj.getString("pErrCode")+ pErrMsg);
		
		return userIfo;
	}
	
	/**
	 * 商户端获取银行列表查询
	 * @param memberId 金融平台的用户id
	 * @param platformId 金融平台id
	 * @param type
	 * @param argMerCode
	 * @param argIpsAccount
	 * @param argMemo
	 * @return
	 */
	public static String queryBankInfo(long memberId, long platformId, int type, String argMerCode, String argIpsAccount, String argMemo) {
		String argSign = com.ips.security.utility.IpsCrypto.md5Sign(argMerCode+IPSConstants.CERT_MD5);
		String argXmlPara = IPSUtil.getSoapInputStream(type, argMerCode, argIpsAccount, argSign, argMemo);
		Logger.info("------------------------argXmlPara:-------------------------------"+argXmlPara);
		
		String xml = new XmlUtil().setDocument(argXmlPara).getNodeValue("GetBankListResult");		
		
		JSONObject jsonObj = (JSONObject) Converter.xmlToObj(xml);
		
		if(StringUtils.isBlank(xml)){
			Logger.info("xml出现空值");
			return null;
		}
		
		String pMerCode = jsonObj.getString("pMerCode");
		String pErrCode = jsonObj.getString("pErrCode");
		String pErrMsg = jsonObj.getString("pErrMsg");
		String pBankList = jsonObj.getString("pBankList");
//		String pSign = jsonObj.getString("pSign");
		
		String src = "<pMerCode>"+pMerCode+"</pMerCode>" +"<pErrCode>"+ pErrCode + "</pErrCode>"
				+"<pErrMsg>"+pErrMsg +"</pErrMsg>"+"<pBankList>"+ pBankList+"</pBankList>";
		
//		String localSign = com.ips.security.utility.IpsCrypto.md5Sign(src + IPSConstants.CERT_MD5);
		
//		if(pSign == null || localSign == null || !pSign.equals(localSign)){
//			flash.error("sign校验失败");
//			Application.error();
//		}
		
		jsonObj.put("pSign", Encrypt.MD5(src+Constants.ENCRYPTION_KEY));
		String bankInfo = jsonObj.toString();
		Logger.info(bankInfo);
		
		return bankInfo;
	}
	
	public static String queryTradeInfo(int type, String argMerCode, String arg3DesXmlPara, String argSign) {
		arg3DesXmlPara = Encrypt.decrypt3DES(arg3DesXmlPara, Constants.ENCRYPTION_KEY);
		arg3DesXmlPara = com.ips.security.utility.IpsCrypto.triDesEncrypt(arg3DesXmlPara,IPSConstants.DES_KEY,IPSConstants.DES_IV);
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\r", "");
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\n", "");
		argSign = com.ips.security.utility.IpsCrypto.md5Sign(argMerCode+arg3DesXmlPara+IPSConstants.CERT_MD5);
		String argXmlPara = IPSUtil.getSoapInputStream2(type, argMerCode, arg3DesXmlPara, argSign);
		
		if(StringUtils.isBlank(argXmlPara)){
			Logger.info("argXmlPara出现空值");
			return null;
		}
		
		Logger.info("------------------------argXmlPara:-------------------------------"+argXmlPara);
		
		String xml = new XmlUtil().setDocument(argXmlPara).getNodeValue("QueryTradeResult");		
		
		JSONObject jsonObj = (JSONObject) Converter.xmlToObj(xml);
		
		if(null == jsonObj){
			Logger.info("jsonObj出现空值");
			return null;
		}
		
		String pMerCode = jsonObj.getString("pMerCode");
		String pErrCode = jsonObj.getString("pErrCode");
		String pErrMsg = jsonObj.getString("pErrMsg");
		String p3DesXmlPara = jsonObj.getString("p3DesXmlPara");
		p3DesXmlPara = com.ips.security.utility.IpsCrypto.triDesDecrypt(p3DesXmlPara,IPSConstants.DES_KEY,IPSConstants.DES_IV);
		
		/*处理第三方支付返回的xml信息中的bom*/
		byte[] bs = null;
		
		try {
			bs = p3DesXmlPara.getBytes("UTF-8");
			if(bs[0] == -17 && bs[1] == -69 && bs[2] == -65) {
				p3DesXmlPara = new String(bs,3,bs.length-3,"UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			Logger.info(argXmlPara);
		}
		
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara, Constants.ENCRYPTION_KEY);
		
//		String pSign = jsonObj.getString("pSign");
		
		String src = "<pMerCode>"+pMerCode+"</pMerCode>" +"<pErrCode>"+ pErrCode + "</pErrCode>"
				+"<pErrMsg>"+pErrMsg +"</pErrMsg>"+"<p3DesXmlPara>"+ p3DesXmlPara+"</p3DesXmlPara>";
		
//		String localSign = com.ips.security.utility.IpsCrypto.md5Sign(src + IPSConstants.CERT_MD5);
		
//		if(pSign == null || localSign == null || !pSign.equals(localSign)){
//			flash.error("sign校验失败");
//			Application.error();
//		}
		
		jsonObj.put("p3DesXmlPara", p3DesXmlPara);
		jsonObj.put("pSign", Encrypt.MD5(src+Constants.ENCRYPTION_KEY));
		String bankInfo = jsonObj.toString();
		Logger.info(bankInfo);
		
		return bankInfo;
	}
	
	/**
	 * 解栋保证金
	 * @param type
	 * @param argMerCode
	 * @param arg3DesXmlPara
	 * @param argSign
	 * @return
	 */
	public static String guaranteeUnfreeze(int type, int platformId, long platformMemberId, String platformUsername, 
			String argMerCode, String arg3DesXmlPara, String argSign) {
		arg3DesXmlPara = Encrypt.decrypt3DES(arg3DesXmlPara, Constants.ENCRYPTION_KEY);

		JSONObject json = (JSONObject)Converter.xmlToObj(arg3DesXmlPara);
		
		if(StringUtils.isBlank(arg3DesXmlPara)){
			Logger.info("arg3DesXmlPara出现空值");
			return null;
		}
		
		/*将使用平台的pWebUrl和pS2SUrl等信息放入pMemo1中，将本平台的回调方法替换使用平台的pWebUrl，pS2SUrl*/
		json.put("pMemo1", "<![CDATA["+json.getString("pS2SUrl")+";"+type+";"+platformId+";"+platformMemberId+";"+platformUsername+"]]>");
		json.put("pMemo2", "<![CDATA["+json.getString("pS2SUrl")+"]]>");
		json.put("pS2SUrl", "<![CDATA["+IPSConstants.IPS_ACCOUNT_S2S+"]]>");
		
		arg3DesXmlPara = Converter.jsonToXml(json.toString(), "pReq", "pRow", null, null);

		arg3DesXmlPara = com.ips.security.utility.IpsCrypto.triDesEncrypt(arg3DesXmlPara,IPSConstants.DES_KEY,IPSConstants.DES_IV);
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\r", "");
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\n", "");
		argSign = com.ips.security.utility.IpsCrypto.md5Sign(argMerCode+arg3DesXmlPara+IPSConstants.CERT_MD5);
		String argXmlPara = IPSUtil.getSoapInputStream2(type, argMerCode, arg3DesXmlPara, argSign);
		Logger.info("------------------------第三方响应数据:------------------------\n%s",argXmlPara);
		
		String xml = new XmlUtil().setDocument(argXmlPara).getNodeValue("GuaranteeUnfreezeResult");		
		
		if(StringUtils.isBlank(xml)){
			Logger.info("xml出现空值");
			return null;
		}
		
		JSONObject jsonObj = (JSONObject) Converter.xmlToObj(xml);
		String pMerCode = jsonObj.getString("pMerCode");
		String pErrCode = jsonObj.getString("pErrCode");
		String pErrMsg = jsonObj.getString("pErrMsg");
		String p3DesXmlPara = jsonObj.getString("p3DesXmlPara");
		p3DesXmlPara = com.ips.security.utility.IpsCrypto.triDesDecrypt(p3DesXmlPara,IPSConstants.DES_KEY,IPSConstants.DES_IV);
		
		/*处理第三方支付返回的xml信息中的bom*/
		byte[] bs = null;
		
		try {
			bs = p3DesXmlPara.getBytes("UTF-8");
			if(bs[0] == -17 && bs[1] == -69 && bs[2] == -65) {
				p3DesXmlPara = new String(bs,3,bs.length-3,"UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			Logger.info(argXmlPara);
		}
		
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara, Constants.ENCRYPTION_KEY);
		
		String pSign = jsonObj.getString("pSign");
		String localSign = com.ips.security.utility.IpsCrypto.md5Sign(pMerCode + pErrCode + pErrMsg + jsonObj.getString("p3DesXmlPara") + IPSConstants.CERT_MD5);
		
		if(pSign == null || localSign == null || !pSign.equals(localSign)){
			jsonObj.put("pErrCode", "-1");
			jsonObj.put("pErrMsg", "sign校验失败");
		}
		
		jsonObj.put("p3DesXmlPara", p3DesXmlPara);
		jsonObj.put("pSign", Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY));
		String result = jsonObj.toString();
		Logger.info("------------------------spay响应数据:------------------------\n%s",result);
		
		return result;
	}
	
	/**
	 * 转账，WS请求
	 * @param platform
	 * @param memberId
	 * @param type
	 * @param argMerCode
	 * @param arg3DesXmlPara
	 * @param error
	 * @return
	 */
	public static String transfer(int platform, long memberId, int type, String argMerCode, String arg3DesXmlPara, ErrorInfo error) {
		error.clear();
		
		arg3DesXmlPara = Encrypt.decrypt3DES(arg3DesXmlPara, Constants.ENCRYPTION_KEY);
		
		JSONObject transferJson = (JSONObject)Converter.xmlToObj(arg3DesXmlPara);
		
		if(StringUtils.isBlank(arg3DesXmlPara)){
			Logger.info("转账时：arg3DesXmlPara出现空值");
			return null;
		}
		
		Logger.info(transferJson.toString());
		/*判断订单号是否已存在*/
//		if(DealDetail.isSerialNumberExist(platform, transferJson.getString("pMerBillNo"))) {
//			error.code = -1;
//			error.msg = "已提交处理，请勿重复提交";
//			
//			return null;
//		}
		
		transfer(platform, memberId, transferJson);
		
		transferJson.put("pMemo1",transferJson.getString("pS2SUrl")+";"+type+";"+platform+";"+memberId+";"+null);
		transferJson.put("pS2SUrl", IPSConstants.IPS_ACCOUNT_WEB);
		arg3DesXmlPara = Converter.jsonToXml(transferJson.toString(), "pReq", "pRow", null, null);
		
		if(null == arg3DesXmlPara){
			Logger.info("转账时：arg3DesXmlPara--json转xml出现空值");
			return null;
		}
		
		Logger.info("arg3DesXmlPara:"+arg3DesXmlPara+"/n arg3DesXmlPara:"+arg3DesXmlPara);
		arg3DesXmlPara = com.ips.security.utility.IpsCrypto.triDesEncrypt(arg3DesXmlPara,IPSConstants.DES_KEY,IPSConstants.DES_IV);
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\r", "");
		arg3DesXmlPara = arg3DesXmlPara.replaceAll("\n", "");
		String argSign = com.ips.security.utility.IpsCrypto.md5Sign(argMerCode+arg3DesXmlPara+IPSConstants.CERT_MD5);
		String argXmlPara = IPSUtil.getSoapInputStream2(type, argMerCode, arg3DesXmlPara, argSign);
		Logger.info("----------argXmlPara:-----"+argXmlPara+"\n---返回的地址："+transferJson.getString("pS2SUrl"));
		
		String xml = new XmlUtil().setDocument(argXmlPara).getNodeValue("TransferResult");	
		
		JSONObject jsonObj = (JSONObject) Converter.xmlToObj(xml);
		
		if(StringUtils.isBlank(xml)){
			Logger.info("xml出现空值");
			return null;
		}
		
		String pMerCode = jsonObj.getString("pMerCode");
		String pErrCode = jsonObj.getString("pErrCode");
		String pErrMsg = jsonObj.getString("pErrMsg");
		String p3DesXmlPara = jsonObj.getString("p3DesXmlPara");
		String pSign = jsonObj.getString("pSign");
		
		String localSign = com.ips.security.utility.IpsCrypto.md5Sign(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + IPSConstants.CERT_MD5);
		
		if(pSign == null || localSign == null || !pSign.equals(localSign)){
			error.code = -1;
			error.msg = "sign校验失败";
			
			return null;
		}
		
		p3DesXmlPara = com.ips.security.utility.IpsCrypto.triDesDecrypt(p3DesXmlPara,IPSConstants.DES_KEY,IPSConstants.DES_IV);
		Logger.info("p3DesXmlPara 1\n"+p3DesXmlPara);
		
		/*处理第三方支付返回的xml信息中的bom*/
		byte[] bs = null;
		try {
			bs = p3DesXmlPara.getBytes("UTF-8");
			if(bs[0] == -17 && bs[1] == -69 && bs[2] == -65) {
				p3DesXmlPara = new String(bs,3,bs.length-3,"UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		String jsonStr = Converter.xmlToJson(p3DesXmlPara);
		JSONObject json = JSONObject.fromObject(jsonStr);
		json.put("pMemo1", memberId+"");
		
		if("MG00000F".equals(pErrCode) || "MG00008F".equals(pErrCode)) {
			transferCommit(platform, json);
		}
		
		p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(json.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		Logger.info("p3DesXmlPara 2\n"+Encrypt.decrypt3DES(p3DesXmlPara, Constants.ENCRYPTION_KEY));
		
		pSign = Encrypt.MD5(pMerCode+pErrCode+pErrMsg+p3DesXmlPara+Constants.ENCRYPTION_KEY);
		jsonObj.put("pMerCode", pMerCode);
		jsonObj.put("pErrCode", pErrCode);
		jsonObj.put("pErrMsg", pErrMsg);
		jsonObj.put("p3DesXmlPara", p3DesXmlPara);
		jsonObj.put("pSign", pSign);
		String transferInfo = jsonObj.toString();
		
		error.code = 0;
		Logger.info(transferInfo);
		
		return transferInfo;
	}
	
	/**
	 * 流标
	 * @param args
	 * @param error
	 * @return
	 */
	public static String bidFlow(Map<String, String> args, long memberId, ErrorInfo error) {
		error.clear();
		
		Map<String, String> args2 = new HashMap<String, String>();
		args2.put("pMerCode", args.get("argMerCode"));
		args2.put("p3DesXmlPara", args.get("arg3DesXmlPara"));
		args2.put("pSign", args.get("argSign"));
		String data = WSUtil.WSSubmit(args.get("url"),args2);
		Logger.info(data);
		if(StringUtils.isBlank(data)){
			Logger.info("data出现空值");
			return null;
		}
		data = data.split("</form>")[1] + "</form>";
		JSONObject jsonObj = (JSONObject)Converter.xmlToObj(data);
		JSONArray inputs = (JSONArray) jsonObj.get("input");	
		LinkedHashMap<String, String> returnMap = new LinkedHashMap<String, String>();
		JSONObject toP2p = new JSONObject();
		for(int i=0; i < inputs.size(); i++){
			JSONObject obj = (JSONObject) inputs.get(i);
			String name = obj.getString("@name");
			String value = obj.getString("@value");
			returnMap.put(name, value);
			toP2p.put(name, value);
		}
		
		String localSign = com.ips.security.utility.IpsCrypto.md5Sign(toP2p.getString("pMerCode") + toP2p.getString("pErrCode") + toP2p.getString("pErrMsg") + toP2p.getString("p3DesXmlPara") + IPSConstants.CERT_MD5);
		
		if(toP2p.getString("pSign") == null || localSign == null || !toP2p.getString("pSign").equals(localSign)){
			error.code = -1;
			error.msg = "sign校验失败";
			
			return null;
		}
		String p3DesXmlPara = com.ips.security.utility.IpsCrypto.triDesDecrypt(toP2p.getString("p3DesXmlPara"),IPSConstants.DES_KEY,IPSConstants.DES_IV);
		String jsonStr = Converter.xmlToJson(p3DesXmlPara);
		JSONObject json = JSONObject.fromObject(jsonStr);
		json.put("pMemo1", memberId+"");
		p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(json.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		Logger.info("--------------------------p3DesXmlPara-------------------"+p3DesXmlPara);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara, Constants.ENCRYPTION_KEY);
		Logger.info("--------------------------p3DesXmlPara-------------------"+p3DesXmlPara);
		jsonObj.put("pMerCode", toP2p.getString("pMerCode"));
		jsonObj.put("pErrCode", toP2p.getString("pErrCode"));
		jsonObj.put("pErrMsg", toP2p.getString("pErrMsg"));
		jsonObj.put("p3DesXmlPara", p3DesXmlPara);
		jsonObj.put("pSign", Encrypt.MD5(toP2p.getString("pMerCode") + toP2p.getString("pErrCode") + toP2p.getString("pErrMsg") + p3DesXmlPara +Constants.ENCRYPTION_KEY));
		
		error.code = 0;
		
		return jsonObj.toString();
	}
	
	/**
	 * 自动投标
	 * @param args
	 * @param error
	 * @return
	 */
	public static String autoInvest(Map<String, String> args, ErrorInfo error) {
		error.clear();
		
		Map<String, String> args2 = new HashMap<String, String>();
		args2.put("pMerCode", args.get("argMerCode"));
		args2.put("p3DesXmlPara", args.get("arg3DesXmlPara"));
		args2.put("pSign", args.get("argSign"));
		String data = WSUtil.WSSubmit(args.get("url"),args2);
		Logger.info("----------------data:"+data+"------------------");
		data = data.split("</form>")[1] + "</form>";
		JSONObject jsonObj = (JSONObject)Converter.xmlToObj(data);
		
		if(null == jsonObj){
			error.code = -1;
			error.msg = "参数解析有误";
			return null;
		}
		
		JSONArray inputs = (JSONArray) jsonObj.get("input");
		Logger.info("-------------inputs:"+inputs+"-----------------");
//		LinkedHashMap<String, String> returnMap = new LinkedHashMap<String, String>();
		JSONObject toP2p = new JSONObject();
		for(int i=0; i < inputs.size(); i++){
			JSONObject obj = (JSONObject) inputs.get(i);
			String name = obj.getString("@name");
			String value = obj.getString("@value");
//			returnMap.put(name, value);\
			toP2p.put(name, value);
		}
		
		Logger.info("-----------toP2p:"+toP2p+"---------------------");
		String localSign = com.ips.security.utility.IpsCrypto.md5Sign(toP2p.getString("pMerCode") + toP2p.getString("pErrCode") + toP2p.getString("pErrMsg") + toP2p.getString("p3DesXmlPara") + IPSConstants.CERT_MD5);
		
		if(toP2p.getString("pSign") == null || localSign == null || !toP2p.getString("pSign").equals(localSign)){
			error.code = -1;
			error.msg = "sign校验失败";
			
			return null;
		}
		String p3DesXmlPara = com.ips.security.utility.IpsCrypto.triDesDecrypt(toP2p.getString("p3DesXmlPara"),IPSConstants.DES_KEY,IPSConstants.DES_IV);
		Logger.info("--------------------------p3DesXmlPara-------------------"+p3DesXmlPara);
		
		/*处理第三方支付返回的xml信息中的bom*/
		byte[] bs = null;
		try {
			bs = p3DesXmlPara.getBytes("UTF-8");
			if(bs[0] == -17 && bs[1] == -69 && bs[2] == -65) {
				p3DesXmlPara = new String(bs,3,bs.length-3,"UTF-8");
			}
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara, Constants.ENCRYPTION_KEY);
		Logger.info("--------------------------p3DesXmlPara-------------------"+p3DesXmlPara);
		toP2p.put("p3DesXmlPara", p3DesXmlPara);
		toP2p.put("pSign", Encrypt.MD5(toP2p.getString("pMerCode") + toP2p.getString("pErrCode") + toP2p.getString("pErrMsg") + p3DesXmlPara +Constants.ENCRYPTION_KEY));
		
		error.code = 0;
		
		return toP2p.toString();
	}
	
	/**
	 * 自动投标
	 * @param args
	 * @param error
	 * @return
	 */
	public static String autoPayment(Map<String, String> args, ErrorInfo error) {
		error.clear();
		
		Map<String, String> args2 = new HashMap<String, String>();
		args2.put("pMerCode", args.get("argMerCode"));
		args2.put("p3DesXmlPara", args.get("arg3DesXmlPara"));
		args2.put("pSign", args.get("argSign"));
		String data = WSUtil.WSSubmit(args.get("url"),args2);
		Logger.info("----------------data:"+data+"------------------");
		if(StringUtils.isBlank(data)){
			Logger.info("data出现空值");
			return null;
		}
		data = data.split("</form>")[0] + "</form>";
		JSONObject jsonObj = (JSONObject)Converter.xmlToObj(data);
		JSONArray inputs = (JSONArray) jsonObj.get("input");
		Logger.info("-------------inputs:"+inputs+"-----------------");
//		LinkedHashMap<String, String> returnMap = new LinkedHashMap<String, String>();
		JSONObject toP2p = new JSONObject();
		for(int i=0; i < inputs.size(); i++){
			JSONObject obj = (JSONObject) inputs.get(i);
			String name = obj.getString("@name");
			String value = obj.getString("@value");
//			returnMap.put(name, value);
			toP2p.put(name, value);
		}
		
		Logger.info("-----------toP2p:"+toP2p+"---------------------");
		String localSign = com.ips.security.utility.IpsCrypto.md5Sign(toP2p.getString("pMerCode") + toP2p.getString("pErrCode") + toP2p.getString("pErrMsg") + toP2p.getString("p3DesXmlPara") + IPSConstants.CERT_MD5);
		
		if(toP2p.getString("pSign") == null || localSign == null || !toP2p.getString("pSign").equals(localSign)){
			error.code = -1;
			error.msg = "sign校验失败";
			
			return null;
		}
		String p3DesXmlPara = com.ips.security.utility.IpsCrypto.triDesDecrypt(toP2p.getString("p3DesXmlPara"),IPSConstants.DES_KEY,IPSConstants.DES_IV);
		Logger.info("--------------------------p3DesXmlPara-------------------"+p3DesXmlPara);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara, Constants.ENCRYPTION_KEY);
		Logger.info("--------------------------p3DesXmlPara-------------------"+p3DesXmlPara);
		p3DesXmlPara = Encrypt.decrypt3DES(p3DesXmlPara, Constants.ENCRYPTION_KEY);
		Logger.info("--------------------------p3DesXmlPara-------------------"+p3DesXmlPara);
		p3DesXmlPara = p3DesXmlPara.replace("?<", "<");
		toP2p.put("p3DesXmlPara", p3DesXmlPara);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara, Constants.ENCRYPTION_KEY);
		Logger.info("--------------------------p3DesXmlPara-------------------"+p3DesXmlPara);
		toP2p.put("p3DesXmlPara", p3DesXmlPara);
		toP2p.put("pSign", Encrypt.MD5(toP2p.getString("pMerCode") + toP2p.getString("pErrCode") + toP2p.getString("pErrMsg") + p3DesXmlPara +Constants.ENCRYPTION_KEY));
		
		error.code = 0;
		
		return toP2p.toString();
	}
}
