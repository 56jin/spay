package controllers.IPS;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import business.DealDetail;
import business.Platform;
import business.Member;

import com.shove.security.Encrypt;

import constants.Constants;
import constants.IPSConstants;
import controllers.Application;
import controllers.BaseController;
import play.Logger;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.mvc.Controller;
import services.IPS;
import services.IPSTest;
import utils.Converter;
import utils.EmailUtil;
import utils.ErrorInfo;
import utils.IPSUtil;
import utils.NumberUtil;
import utils.WSUtil;
import utils.XmlParse;
import utils.PaymentUtil;
import utils.XmlUtil;

public class IPayment  extends BaseController {
	
	/**
	 * 处理请求环迅支付的请求
	 * @param domain 请求平台的域名
	 * @param type 请求接口类型
	 * @param platform 请求平台的id
	 * @param memberId 请求平台的用户的id
	 * @param memberName 请求平台的用户的用户名
	 * @param argMerCode
	 * @param arg3DesXmlPara
	 * @param argSign
	 * @param argIpsAccount
	 * @param merCodeSoap
	 * @param desXmlParaSoap
	 * @param argMemo
	 * @param wsUrl 当第三方支付为ws请求，且没有异步回调地址时，该参数供本平台回调使用平台
	 */
	public static void ips(String domain, int type, int platform, long memberId, String memberName, String argMerCode, String arg3DesXmlPara, 
			String argSign, String argIpsAccount, String argMemo, String flow, String autoInvest, String autoPayment, String wsUrl) {
		ErrorInfo error = new ErrorInfo();
		
		/*接口类型参数限定在1~16*/
		if(type <= 0 || type > 17) {
			flash.error("传入参数有误");
			Application.error();
		}
		
		/*账户余额查询和账户信息查询插入的参数为argIpsAccount，为了适应下面的校验方法，进行了赋值转化*/
		if(type == IPSConstants.ACCOUNT_BALANCE || type == IPSConstants.USER_INFO) {
			arg3DesXmlPara = argIpsAccount;
		}
		
		/*与使用平台的校验*/
		if(!PaymentUtil.checkSign(argMerCode, arg3DesXmlPara, argSign)) {
			Logger.info("------------------------资金托管平台校验失败-------------------------------");
			flash.error("sign校验失败");
			Application.error();
		}
		
		Logger.info("------------------------资金托管平台校验成功-------------------------------");
		
		/*账户余额查询、账户信息查询，WS请求*/
		if(type == IPSConstants.ACCOUNT_BALANCE || type == IPSConstants.USER_INFO) {
			String userInfo = IPS.queryUserInfo(memberId, platform, type, argMerCode, argIpsAccount, argMemo);
			
			if(null == userInfo){
				Logger.info("------------------------账户信息查询--参数解析错误-------------------------------");
				flash.error("参数解析错误，请重试");
				Application.error();
			}
			
			renderText(userInfo);
		}
		
		/*转账、自动代扣充值，包含WS请求和异步回调*/
		if(type == IPSConstants.TRANSFER || type == IPSConstants.DEDUCT) {
			String transferInfo = IPS.transfer(platform, memberId, type, argMerCode, arg3DesXmlPara, error);
			
			if(null == transferInfo){
				Logger.info("------------------------转账、自动代扣充值--参数解析错误-------------------------------");
				flash.error("参数解析错误，请重试");
				Application.error();
			}
			
			renderText(transferInfo);
		}
		
		/*商户端获取银行列表查询，WS请求*/
		if(type == IPSConstants.BANK_LIST) {
			String bankInfo = IPS.queryBankInfo(memberId, platform, type, argMerCode, argIpsAccount, argMemo);
			
			if(null == bankInfo){
				Logger.info("-----------------------商户端获取银行列表查询--参数解析错误-------------------------------");
				flash.error("参数解析错误，请重试");
				Application.error();
			}
			
			renderText(bankInfo);
		}
		
		/*交易记录查询查询，WS请求*/
		if(type == IPSConstants.QUERY_TRADE) {
			String bankInfo = IPS.queryTradeInfo(type, argMerCode, arg3DesXmlPara, argSign);
			
			if(null == bankInfo){
				Logger.info("------------------------交易记录查询查询--参数解析错误-------------------------------");
				flash.error("参数解析错误，请重试");
				Application.error();
			}
			
			renderText(bankInfo);
		}
		
		/*解冻保证金，WS请求*/
		if(type == IPSConstants.UNFREEZE) {
			String reslut = IPS.guaranteeUnfreeze(type, platform, memberId, memberName, argMerCode, arg3DesXmlPara, argSign);
			
			if(null == reslut){
				Logger.info("------------------------解冻保证金--参数解析错误-------------------------------");
				flash.error("参数解析错误，请重试");
				Application.error();
			}
			
			renderText(reslut);
		}
		
		arg3DesXmlPara = Encrypt.decrypt3DES(arg3DesXmlPara, Constants.ENCRYPTION_KEY);
		
		JSONObject json = (JSONObject)Converter.xmlToObj(arg3DesXmlPara);
		
		if(StringUtils.isBlank(arg3DesXmlPara)){
			Logger.info("解析参数arg3DesXmlPara出现空值");
			flash.error("解析参数有误");
			Application.error();
		}
		
		Logger.info("----------------------arg3DesXmlPara = :"+arg3DesXmlPara);
		
		/*判断订单号是否已存在*/
//		if(DealDetail.isSerialNumberExist(platform, json.getString("pMerBillNo"))) {
//			flash.error("已提交处理，请勿重复提交");
//			Application.error();
//		}
		
		/*根据身份证判断是否开户*/
		if(type == IPSConstants.CREATE_ACCOUNT) {
			
			String idNumber = json.getString("pIdentNo");
			boolean flag = true;
			
			/*身份证已存在*/
			if(Member.isCreateAccount(json.getString("pIdentNo"), domain, error)) {
				
				/*用户平台关系表中不存在该身份证会员的信息*/
				if(error.code == 1) {
					Member member = new Member();
					long id = Member.queryIdByIdNumber(json.getString("pIdentNo"));
					member.memberId = id;
					member.platformId = platform;
					member.platformMemberId = memberId;
					member.platformMembername = memberName;
					member.addPlatformmember(error);
				
				/*不同平台，使用相同的支付接口，表示身份证会员已开户，在平台关系表中插入数据*/	
				}else if(error.code == 2) {
					Member member = new Member();
					member.idNumber = idNumber;
					
					member.platformMemberId = memberId;
					member.platformMembername = memberName;
					member.platformMemberAccount = member.queryAccount(idNumber, platform);
					
					member.addPlatformmember(error);
				}else if(error.code == 3) {
					Member.updateAccount(platform, memberId, Member.queryAccount(idNumber, platform));
				}
				
				String ipsAcctNo = Member.queryAccount(idNumber, platform);
				
				if(ipsAcctNo != null) {
					flag = false;
				}
				
				if(!flag) {
					String pErrCode = ipsAcctNo == null ? "MG00001F" : "MY00000F";
					String pErrMsg = ipsAcctNo == null ? "开户失败" : "已开户";
					JSONObject jsonObj = new JSONObject();
					jsonObj.put("pIpsAcctNo", ipsAcctNo);
					jsonObj.put("pStatus", "0");
					jsonObj.put("pMemo1", memberId);
					String strXml = Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null);
					String p3DesXmlPara = Encrypt.encrypt3DES(strXml, Constants.ENCRYPTION_KEY);
					
					Map<String, String> args = new HashMap<String, String>();
					args.put("url", json.getString("pWebUrl"));
					args.put("pMerCode", argMerCode);
					args.put("pErrCode", pErrCode);
					args.put("pErrMsg", pErrMsg);
					args.put("p3DesXmlPara", p3DesXmlPara);
					args.put("pSign", Encrypt.MD5(argMerCode+pErrCode+pErrMsg+p3DesXmlPara+Constants.ENCRYPTION_KEY));
					
					render("@IPS.IPayment.ipsCommit", args);
				}
			}else {
				/*身份证不存在，根据请求在用户表和用户平台关系表中添加记录*/
				Member member = new Member();
				
				member.idNumber = json.getString("pIdentNo");
				member.mobile = json.getString("pMobileNo");
				member.platformId = platform;
				
				member.platformMemberId = memberId;
				member.platformMembername = memberName;
				
				Map<String, String> info = member.add();
				
				String content = "您在资金托管平台注册的用户名：" + info.get("name") + "  密码：" + info.get("password");
				
				EmailUtil.sendEmail(json.getString("pEmail"), "注册信息", content);
			}
		}
		
		/*接口请求入口*/
		Map<String, String> args = IPS.entrance(type, platform, memberId, memberName, argMerCode, arg3DesXmlPara, argSign);
		
		if(null == args){
			flash.error("解析参数有误，请重试");
			Application.error();
		}
		
		/*流标*/
		if(IPSConstants.BID_FLOWS.equals(flow)) {
			String bidFlowInfo = IPS.bidFlow(args, memberId, error);
			
			if(error.code < 0) {
				flash.error(error.msg);
				Application.error();
			}
			
			renderText(bidFlowInfo);
		}
		
		/*自动投标*/
		if(IPSConstants.AUTO_INVEST.equals(autoInvest)) {
			String autoInvestInfo = IPS.autoInvest(args, error);
			
			if(error.code < 0) {
				flash.error(error.msg);
				Application.error();
			}
			Logger.info("---------------autoInvestInfo:"+autoInvestInfo+"-----------");
			renderText(autoInvestInfo);
		}
		
//		/*自动还款*/
//		if(IPSConstants.AUTO_PAYMENT.equals(autoPayment)) {
//			String autoPaymentInfo = IPS.autoPayment(args, error);
//			
//			if(error.code < 0) {
//				flash.error(error.msg);
//				Application.error();
//			}
//			Logger.info("---------------autoInvestInfo:"+autoPaymentInfo+"-----------");
//			renderText(autoPaymentInfo);
//		}
		
		render(args);
    }
	
	/**
	 * 环迅同步回调方法
	 * @param pMerCode
	 * @param pErrCode
	 * @param pErrMsg
	 * @param p3DesXmlPara
	 * @param pSign
	 */
	public static void ipsCommit(String pMerCode, String pErrCode, String pErrMsg, String p3DesXmlPara, String pSign) {
		
		Logger.info("----------返回结果-------"+"\n----------pErrCode = "+pErrCode+"\n----------pErrMsg"+pErrMsg);
		String localSign = com.ips.security.utility.IpsCrypto.md5Sign(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + IPSConstants.CERT_MD5);
		
		if(pSign == null || localSign == null || !pSign.equals(localSign)){
			Logger.info("------------------------支付平台校验失败------------------------------");
			flash.error("sign校验失败");
			Application.error();
		}
		
		Logger.info("------------------------支付平台校验成功------------------------------");
		
		Map<String, String> args = IPS.exit(pMerCode, pErrCode, pErrMsg, p3DesXmlPara, pSign);
		
		if(null == args){
			renderText("");
		}
		
        render(args);
	}
	
	/**
	 * 环迅异步回调方法
	 * @param pMerCode
	 * @param pErrCode
	 * @param pErrMsg
	 * @param p3DesXmlPara
	 * @param pSign
	 */
	public static void ipsCommitAsynch(String pMerCode, String pErrCode, String pErrMsg, String p3DesXmlPara, String pSign) {
		Logger.info("----------异步返回结果-------"+"\n----------pErrCode = "+pErrCode+"\n----------pErrMsg"+pErrMsg);
		String localSign = com.ips.security.utility.IpsCrypto.md5Sign(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + IPSConstants.CERT_MD5);
		
		if(pSign == null || localSign == null || !pSign.equals(localSign)){
			flash.error("sign校验失败");
			Application.error();
		}
		
		Logger.info("------------------------支付平台校验成功------------------------------");
		
		Map<String, String> args = IPS.exit(pMerCode, pErrCode, pErrMsg, p3DesXmlPara, pSign);
		
		WS.url(args.get("pS2SUrl")).setParameters(args).postAsync();
	}
	
	/**
	 * 内部测试方法，测试使用平台请求本平台，本平台不请求第三方支付
	 * @param type
	 * @param platform
	 * @param memberId
	 * @param membername
	 * @param argMerCode
	 * @param arg3DesXmlPara
	 * @param argSign
	 * @param argIpsAccount
	 * @param argMemo
	 */
	public static void ipsTest(int type, int platform, long memberId, String membername, String argMerCode, String arg3DesXmlPara,
			String argSign, String argIpsAccount, String argMemo) {
		if(type <= 0 || type > 16) {
			flash.error("传入参数有误");
			Application.error();
		}
		
		if(type == IPSConstants.ACCOUNT_BALANCE) {
			
			if(!PaymentUtil.checkSign(argMerCode, argIpsAccount, argSign)) {
				flash.error("sign校验失败");
				Application.error();
			}
			
			String argXmlPara = IPSTest.queryBalance(argMerCode, argIpsAccount);
			render("@queryBalance", argXmlPara);
		}
		
		if(type == IPSConstants.BANK_LIST) {
			
			if(!PaymentUtil.checkSign(argMerCode, "", argSign)) {
				flash.error("sign校验失败");
				Application.error();
			}
			
			String argXmlPara = IPSTest.bankList(argMerCode);
			
			render("@queryBalance", argXmlPara);
		}
		
		if(type == IPSConstants.USER_INFO) {
			
			if(!PaymentUtil.checkSign(argMerCode, argIpsAccount, argSign)) {
				flash.error("sign校验失败");
				Application.error();
			}
			
			String argXmlPara = IPSTest.queryAccount(argMerCode, argIpsAccount, argMemo);
			
			render("@queryBalance", argXmlPara);
		}
		
		if(!PaymentUtil.checkSign(argMerCode, arg3DesXmlPara, argSign)) {
			flash.error("sign校验失败");
			Application.error();
		}
		
		arg3DesXmlPara = Encrypt.decrypt3DES(arg3DesXmlPara, Constants.ENCRYPTION_KEY);
		
		Map<String, String> args = IPSTest.entrance(type, platform, memberId, membername, argMerCode, arg3DesXmlPara, argSign);
		
		render(args);
	}
	
	/**
	 * ws调用测试
	 */
	public static void queryBalance() {
		String arg3DesXmlPara = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><pReq><pMerBillNo>1x9x1410526977254</pMerBillNo><pBidNo>1410493822176</pBidNo><pDate>20140912</pDate><pTransferType>1</pTransferType><pTransferMode>1</pTransferMode><pS2SUrl>http://172.16.6.171:9000/IPSCallBack</pS2SUrl><pDetails><pRow><pOriMerBillNo>3x3x1410508252636</pOriMerBillNo><pTrdAmt>99.00</pTrdAmt><pFAcctType>1</pFAcctType><pFIpsAcctNo>4021000030258728</pFIpsAcctNo><pFTrdFee>0.99</pFTrdFee><pTAcctType>1</pTAcctType><pTIpsAcctNo>4021000030239302</pTIpsAcctNo><pTTrdFee>0</pTTrdFee></pRow></pDetails><pMemo1>pMemo1</pMemo1><pMemo2>pMemo2</pMemo2><pMemo3>pMemo3</pMemo3></pReq>"; 
		arg3DesXmlPara = Encrypt.encrypt3DES(arg3DesXmlPara, Constants.ENCRYPTION_KEY);
		String argMerCode = "808801";
		String argSign = Encrypt.MD5(argMerCode+arg3DesXmlPara+Constants.ENCRYPTION_KEY);
		render(argMerCode, arg3DesXmlPara, argSign);
	}
}
