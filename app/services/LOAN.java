package services;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import models.t_member_of_platforms;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;

import play.Logger;
import play.libs.Codec;
import utils.Converter;
import utils.ErrorInfo;
import utils.loan.LoanUtil;
import utils.loan.RsaHelper;
import business.Bid;
import business.DealDetail;
import business.TransferBatches;

import com.shove.Convert;
import com.shove.security.Encrypt;

import constants.Constants;
import constants.LoanConstants;
import constants.LoanConstants.LoanNotifyURL;
import constants.LoanConstants.LoanReturnURL;
import constants.LoanConstants.RepairOperation;
import controllers.loan.LoanController;

/**
 * 资金托管-双乾，核心业务类
 *
 * @author hys
 * @createDate  2015年1月5日 下午3:42:52
 *
 */
public class LOAN {

	/**
	 * 开户参数转化
	 * 
	 * @param error
	 * @param platformId
	 * @param platformMemberId
	 * @param memberName
	 * @param argMerCode
	 * @param jsonArg3DesXmlPara
	 * @return
	 */
	public static Map<String, Object> convertCreateAccountParams(
			ErrorInfo error, long platformId, long platformMemberId,
			String memberName, String argMerCode,
			JSONObject jsonArg3DesXmlPara, JSONObject jsonArgeXtraPara) {
		error.clear();

		// 必传字段校验
		String[] needsParams = {"pMerBillNo","pIdentNo", "pRealName", "pMobileNo","pEmail", "pWebUrl", "pS2SUrl" };
		String result = checkNeedsParams(needsParams, jsonArg3DesXmlPara,
				jsonArgeXtraPara, memberName);
		if (result != null) {
			error.code = -1;
			error.msg = "参数" + result + "为必传字段";

			return null;
		}

		String RegisterType = LoanConstants.REGISTER_TYPE;
		String AccountType = LoanConstants.ACCOUNT_TYPE;
		String Mobile = jsonArg3DesXmlPara.getString("pMobileNo");
		String Email = jsonArg3DesXmlPara.getString("pEmail");
		String RealName = jsonArg3DesXmlPara.getString("pRealName");
		String IdentificationNo = jsonArg3DesXmlPara.getString("pIdentNo");
		String LoanPlatformAccount = memberName;
		String Image1 = LoanConstants.IMAGE1;
		String Image2 = LoanConstants.IMAGE2;
		String PlatformMoneymoremore = argMerCode;
		String RandomTimeStamp = LoanConstants.RANDOM_TIME_STAMP;

		String Remark1 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pWebUrl"));
		String Remark2 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pS2SUrl"));
		String Remark3 = Codec.encodeBASE64(platformId 
				+ LoanConstants.TOKEN + platformMemberId
				+ LoanConstants.TOKEN + jsonArg3DesXmlPara.getString("pMerBillNo")
				);
		Logger.info("Remark.length = %s", Remark1.length() + "," + Remark2.length() + "," + Remark3.length());

		String SubmitURL = LoanConstants.LOAN_URL[1]; // 请求双乾开户接口
		String ReturnURL = LoanReturnURL.CREATE_ACCOUNT;
		String NotifyURL = LoanNotifyURL.CREATE_ACCOUNT;
		String privatekey = LoanConstants.privateKeyPKCS8;

		String dataStr = RegisterType + AccountType + Mobile + Email + RealName
				+ IdentificationNo + Image1 + Image2 + LoanPlatformAccount
				+ PlatformMoneymoremore + RandomTimeStamp + Remark1 + Remark2
				+ Remark3 + ReturnURL + NotifyURL;

		// 签名
		RsaHelper rsa = RsaHelper.getInstance();
		String SignInfo = rsa.signData(dataStr, privatekey);

		Map<String, Object> args = new HashMap<String, Object>();
		args.put("RegisterType", RegisterType);
		args.put("AccountType", AccountType);
		args.put("Mobile", Mobile);
		args.put("Email", Email);
		args.put("RealName", RealName);
		args.put("IdentificationNo", IdentificationNo);
		args.put("Image1", Image1);
		args.put("Image2", Image2);
		args.put("LoanPlatformAccount", LoanPlatformAccount);
		args.put("PlatformMoneymoremore", PlatformMoneymoremore);
		args.put("Remark1", Remark1);
		args.put("Remark2", Remark2);
		args.put("Remark3", Remark3);
		args.put("ReturnURL", ReturnURL);
		args.put("NotifyURL", NotifyURL);
		args.put("SignInfo", SignInfo);
		args.put("SubmitURL", SubmitURL);

		LoanUtil.printLoggerToLoan(args);
		
		//添加用户事件
		String serialNumber = jsonArg3DesXmlPara.getString("pMerBillNo");
		String frontUrl = jsonArg3DesXmlPara.getString("pWebUrl");
		String backgroundUrl = jsonArg3DesXmlPara.getString("pS2SUrl");
		DealDetail.addEvent(platformMemberId, LoanConstants.CREATE_ACCOUNT + 200, platformId, serialNumber, frontUrl, backgroundUrl, "", "");

		return args;
	}

	/**
	 * 二次分配审核授权,参数转化（授权：type为3，授权类型为二次分配审核）
	 * 
	 * @param error
	 * @param platformId
	 * @param platformMemberId
	 * @param memberName
	 * @param argMerCode
	 * @param jsonArg3DesXmlPara
	 * @return
	 */
	public static Map<String, Object> convertAuthorizeSecondaryParams(
			ErrorInfo error, String type, long platformId,
			long platformMemberId, String memberName, String argMerCode,
			JSONObject jsonArg3DesXmlPara, JSONObject jsonArgeXtraPara, String MoneymoremoreId) {
		Logger.info("======请求中间件%s接口，执行开始========", type);
		
		error.clear();
		
		// 必传字段校验
				String[] needsParams = {"pIpsAcctNo","IdentificationNo", "pMerBillNo", "pWebUrl","pS2SUrl" };
				String result = checkNeedsParams(needsParams, jsonArg3DesXmlPara,jsonArgeXtraPara, memberName);
				if (result != null) {
					error.code = -1;
					error.msg = "参数" + result + "为必传字段";

					return null;
				}

		String PlatformMoneymoremore = argMerCode;
		String AuthorizeTypeOpen = type;
		String AuthorizeTypeClose = "";
		String RandomTimeStamp = LoanConstants.RANDOM_TIME_STAMP;

		String Remark1 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pWebUrl"));
		String Remark2 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pS2SUrl"));
		String Remark3 = Codec.encodeBASE64(platformId 
				+ LoanConstants.TOKEN + platformMemberId 
				+ LoanConstants.TOKEN + jsonArg3DesXmlPara.getString("pIpsAcctNo")
				+ LoanConstants.TOKEN + jsonArg3DesXmlPara.getString("IdentificationNo")
				+ LoanConstants.TOKEN + jsonArg3DesXmlPara.getString("pMerBillNo")
				);
		Logger.info("Remark.length = %s", Remark1.length() + "," + Remark2.length() + "," + Remark3.length());

		String SubmitURL = LoanConstants.LOAN_URL[7]; // 双乾授权接口
		String ReturnURL = LoanReturnURL.AUTHORIZE_SECONDARY;
		String NotifyURL = LoanNotifyURL.AUTHORIZE_SECONDARY;
		String privatekey = LoanConstants.privateKeyPKCS8;

		String dataStr = MoneymoremoreId + PlatformMoneymoremore
				+ AuthorizeTypeOpen + AuthorizeTypeClose + RandomTimeStamp
				+ Remark1 + Remark2 + Remark3 + ReturnURL + NotifyURL;
		// 签名
		RsaHelper rsa = RsaHelper.getInstance();
		String SignInfo = rsa.signData(dataStr, privatekey);

		Map<String, Object> args = new HashMap<String, Object>();
		args.put("MoneymoremoreId", MoneymoremoreId);
		args.put("PlatformMoneymoremore", PlatformMoneymoremore);
		args.put("AuthorizeTypeOpen", AuthorizeTypeOpen);
		args.put("AuthorizeTypeClose", AuthorizeTypeClose);
		args.put("RandomTimeStamp", RandomTimeStamp);
		args.put("Remark1", Remark1);
		args.put("Remark2", Remark2);
		args.put("Remark3", Remark3);
		args.put("ReturnURL", ReturnURL);
		args.put("NotifyURL", NotifyURL);
		args.put("SignInfo", SignInfo);
		args.put("SubmitURL", SubmitURL);

		LoanUtil.printLoggerToLoan(args);

		//添加用户事件
		String serialNumber = jsonArg3DesXmlPara.getString("pMerBillNo");
		String frontUrl = jsonArg3DesXmlPara.getString("pWebUrl");
		String backgroundUrl = jsonArg3DesXmlPara.getString("pS2SUrl");
		DealDetail.addEvent(platformMemberId, LoanConstants.AUTHORIZE_SECONDARY + 200, platformId, serialNumber, frontUrl, backgroundUrl, "", "");
		
		return args;
	}
	
	/**
	 * 标的登记,新增,参数转化（转账：单笔，其他，无二次分配，手动，需审核，冻结保证金）
	 * 
	 * @param error
	 * @param platformId
	 * @param platformMemberId
	 * @param memberName
	 * @param argMerCode
	 * @param jsonArg3DesXmlPara
	 * @return
	 */
	public static Map<String, Object> convertAddRegisterSubjectParams(
			ErrorInfo error, long platformId, long platformMemberId,
			String memberName, String argMerCode,
			JSONObject jsonArg3DesXmlPara, JSONObject jsonArgeXtraPara) {

		error.clear();

		// 必传字段校验
		String[] needsParams = { "pIpsAcctNo", "pMerBillNo", "pBidNo",
				"pGuaranteesAmt", "pLendFee","pLendAmt", "pWebUrl", "pMemo3", "pS2SUrl" };
		String result = checkNeedsParams(needsParams, jsonArg3DesXmlPara,
				jsonArgeXtraPara);
		if (result != null) {
			error.code = -1;
			error.msg = "参数" + result + "为必传字段";

			return null;
		}

		String loanAccount = jsonArg3DesXmlPara.getString("pIpsAcctNo");
		String LoanOutMoneymoremore = findMoneymoremoreIdByAccount(error,platformId, loanAccount);

		List<Map<String, Object>> listmlib = new ArrayList<Map<String, Object>>();

		Map<String, Object> loanInfoBean = new HashMap<String, Object>();
		loanInfoBean.put("LoanOutMoneymoremore", LoanOutMoneymoremore);
		loanInfoBean.put("LoanInMoneymoremore", argMerCode);
		loanInfoBean.put("OrderNo", jsonArg3DesXmlPara.getString("pMerBillNo"));
		loanInfoBean.put("BatchNo", jsonArg3DesXmlPara.getString("pBidNo"));
		loanInfoBean.put("ExchangeBatchNo", "");
		loanInfoBean.put("AdvanceBatchNo", "");
		loanInfoBean.put("Amount", jsonArg3DesXmlPara.getString("pGuaranteesAmt"));
		loanInfoBean.put("FullAmount", "");
		loanInfoBean.put("TransferName", "发标");
		loanInfoBean.put("Remark", "冻结保证金");
		loanInfoBean.put("SecondaryJsonList", "");
		listmlib.add(loanInfoBean);

		String LoanJsonList = LoanUtil.toJson(listmlib);

		String PlatformMoneymoremore = argMerCode;
		String TransferAction = "3"; // 其他
		String Action = "1"; // 手动转账
		String TransferType = "2"; // 直连
		String NeedAudit = ""; // 需要审核
		String RandomTimeStamp = LoanConstants.RANDOM_TIME_STAMP;

		String Remark1 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pWebUrl"));
		String Remark2 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pS2SUrl"));
		String Remark3 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pOperationType")
				+ LoanConstants.TOKEN + platformMemberId
				+ LoanConstants.TOKEN + jsonArg3DesXmlPara.getString("pMemo3")
				);
		Logger.info("Remark.length = %s", Remark1.length() + "," + Remark2.length() + "," + Remark3.length());

		String SubmitURL = LoanConstants.LOAN_URL[4]; // 请求双乾转账接口
		String ReturnURL = LoanReturnURL.ADD_REGISTER_SUBJECT;
		String NotifyURL = LoanNotifyURL.ADD_REGISTER_SUBJECT;
		String privatekey = LoanConstants.privateKeyPKCS8;

		String dataStr = LoanJsonList + PlatformMoneymoremore + TransferAction
				+ Action + TransferType + NeedAudit + RandomTimeStamp + Remark1
				+ Remark2 + Remark3 + ReturnURL + NotifyURL;
		// 签名
		RsaHelper rsa = RsaHelper.getInstance();
		String SignInfo = rsa.signData(dataStr, privatekey);

		LoanJsonList = LoanUtil.UrlEncoder(LoanJsonList, "utf-8");

		Map<String, Object> args = new HashMap<String, Object>();
		args.put("LoanJsonList", LoanJsonList);
		args.put("PlatformMoneymoremore", PlatformMoneymoremore);
		args.put("TransferAction", TransferAction);
		args.put("Action", Action);
		args.put("TransferType", TransferType);
		args.put("NeedAudit", NeedAudit);
		args.put("RandomTimeStamp", RandomTimeStamp);
		args.put("Remark1", Remark1);
		args.put("Remark2", Remark2);
		args.put("Remark3", Remark3);
		args.put("ReturnURL", ReturnURL);
		args.put("NotifyURL", NotifyURL);
		args.put("SignInfo", SignInfo);
		args.put("SubmitURL", SubmitURL);

		LoanUtil.printLoggerToLoan(args);
		
		// 保存标的对应借款管理费信息
		Bid.create(jsonArg3DesXmlPara.getString("pBidNo"),Convert.strToDouble(jsonArg3DesXmlPara.getString("pLendFee").trim(), 0),error);
		
		//添加用户事件
		String serialNumber = jsonArg3DesXmlPara.getString("pMerBillNo");
		String frontUrl = jsonArg3DesXmlPara.getString("pWebUrl");
		String backgroundUrl = jsonArg3DesXmlPara.getString("pS2SUrl");
		DealDetail.addEvent(platformMemberId, LoanConstants.REGISTER_SUBJECT + 200, platformId, serialNumber, frontUrl, backgroundUrl, "", "");

		//添加交易记录
		double amount = Convert.strToDouble(jsonArg3DesXmlPara.getString("pGuaranteesAmt").trim(), 0);
		String summary = "标的登记（新增），冻结保证金";
		DealDetail.addDealDetail(platformMemberId, (int)platformId, serialNumber, LoanConstants.REGISTER_SUBJECT, amount, false, summary);
		
		return args;
	}

	/**
	 * 标的登记,结束,参数转化（审核：退回冻结保证金，退回投资金额）
	 * 
	 * @param error
	 * @param platformId
	 * @param platformMemberId
	 * @param memberName
	 * @param argMerCode
	 * @param jsonArg3DesXmlPara
	 * @return
	 */
	public static Map<String, Object> convertEndRegisterSubjectParams(
			ErrorInfo error, long platformId, long platformMemberId,
			String memberName, String argMerCode,
			JSONObject jsonArg3DesXmlPara, JSONObject jsonArgeXtraPara) {
		error.clear();

		// 必传字段校验
		String[] needsParams = {"pMerBillNo","freezeTrxId","investInfo","pWebUrl", "pS2SUrl" };
		String result = checkNeedsParams(needsParams, jsonArg3DesXmlPara,jsonArgeXtraPara);
		if (result != null) {
			error.code = -1;
			error.msg = "参数" + result + "为必传字段";

			return null;
		}
		
		String LoanNoList = "";
		String investInfo = jsonArgeXtraPara.getString("investInfo");  //双乾返回投资流水号
		if(StringUtils.isBlank(investInfo) || "[]".equals(investInfo)){
			LoanNoList =  jsonArgeXtraPara.getString("freezeTrxId");  //双乾返回发标流水号
		}else{
			LoanNoList = investInfo + "," +  jsonArgeXtraPara.getString("freezeTrxId");
		}
		
		String PlatformMoneymoremore = argMerCode;
		String AuditType = "2";  //退回
		String RandomTimeStamp = LoanConstants.RANDOM_TIME_STAMP;

		String Remark1 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pWebUrl"));
		String Remark2 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pS2SUrl"));
		String Remark3 = Codec.encodeBASE64(LoanConstants.END_REGISTER_SUBJECT 
				+ LoanConstants.TOKEN + jsonArg3DesXmlPara.getString("pMerBillNo")
				+ LoanConstants.TOKEN + jsonArgeXtraPara.getString("freezeTrxId") 
				+ LoanConstants.TOKEN + jsonArg3DesXmlPara.getString("pBidNo") 
				+ LoanConstants.TOKEN + jsonArg3DesXmlPara.getString("pMemo3")
				);
		Logger.info("Remark.length = %s", Remark1.length() + "," + Remark2.length() + "," + Remark3.length());

		String SubmitURL = LoanConstants.LOAN_URL[8]; // 审核退回
		String ReturnURL = LoanReturnURL.END_REGISTER_SUBJECT;
		String NotifyURL = LoanNotifyURL.END_REGISTER_SUBJECT;
		String privatekey = LoanConstants.privateKeyPKCS8;

		String dataStr = LoanNoList + PlatformMoneymoremore + AuditType
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ReturnURL
				+ NotifyURL;
		// 签名
		RsaHelper rsa = RsaHelper.getInstance();
		String SignInfo = rsa.signData(dataStr, privatekey);

		Map<String, Object> args = new HashMap<String, Object>();
		args.put("LoanNoList", LoanNoList);
		args.put("PlatformMoneymoremore", PlatformMoneymoremore);
		args.put("AuditType", AuditType);
		args.put("RandomTimeStamp", RandomTimeStamp);
		args.put("Remark1", Remark1);
		args.put("Remark2", Remark2);
		args.put("Remark3", Remark3);
		args.put("ReturnURL", ReturnURL);
		args.put("NotifyURL", NotifyURL);
		args.put("SignInfo", SignInfo);
		args.put("SubmitURL", SubmitURL);

		LoanUtil.printLoggerToLoan(args);

		//添加用户事件
		String serialNumber = jsonArg3DesXmlPara.getString("pMerBillNo");
		String frontUrl = jsonArg3DesXmlPara.getString("pWebUrl");
		String backgroundUrl = jsonArg3DesXmlPara.getString("pS2SUrl");
		DealDetail.addEvent(platformMemberId, LoanConstants.REGISTER_SUBJECT + 200, platformId, serialNumber, frontUrl, backgroundUrl, "", "");
		
		return args;
	}

	/**
	 * 登记债权人,参数转化（转账：单笔,投标，有二次分配，手动/或自动，需审核，冻结投资金额）
	 * 
	 * @param jsonArg3DesXmlPara
	 * @param argMerCode
	 * @param memberName
	 * @param platformMemberId
	 * @param platformId
	 * @param error
	 */
	public static Map<String, Object> convertRegisterCreditorParams(
			ErrorInfo error, long platformId, long platformMemberId,
			String memberName, String argMerCode,
			JSONObject jsonArg3DesXmlPara, JSONObject jsonArgeXtraPara) {
		error.clear();

		// 必传字段校验
		String[] needsParams = { "pAccount", "bidContractNo", "pMerBillNo",
				"pBidNo", "pTrdAmt", "pFee", "serviceFee", "isFull", "pWSUrl",
				"pS2SUrl" };
		String result = checkNeedsParams(needsParams, jsonArg3DesXmlPara,
				jsonArgeXtraPara);
		if (result != null) {
			error.code = -1;
			error.msg = "参数" + result + "为必传字段";

			return null;
		}

		String SecondaryJsonList = "";
		String serviceFee = jsonArgeXtraPara.getString("serviceFee");
		double remainFee = Bid.findRemainFee(jsonArg3DesXmlPara.getString("pBidNo"));
		if ("Y".equals(jsonArgeXtraPara.getString("isFull"))) {
			serviceFee = String.valueOf(remainFee);
		}
 
		remainFee = remainFee - Convert.strToDouble(serviceFee.trim(), 0);
		remainFee = remainFee < 0 ? 0 : remainFee;  //剩余管理费不能为负数
		
		if(!(StringUtils.isBlank(serviceFee) || "[]".equals(serviceFee)) && Convert.strToDouble(serviceFee, 0) != 0){
			// 二次分配，分摊到每笔投资的借款管理费
			List<Map<String, String>> loanInfoSecondaryBeans = new ArrayList<Map<String, String>>();
			Map<String, String> loanInfoSecondaryBean = new HashMap<String, String>();
			loanInfoSecondaryBean.put("LoanInMoneymoremore", argMerCode);
			loanInfoSecondaryBean.put("Amount", serviceFee);
			loanInfoSecondaryBean.put("TransferName", "借款管理费");
			loanInfoSecondaryBean.put("Remark", "借款管理费");
			loanInfoSecondaryBeans.add(loanInfoSecondaryBean);
			
			SecondaryJsonList = LoanUtil.toJson(loanInfoSecondaryBeans);
		}

		Logger.info("SecondaryJsonList = %s", SecondaryJsonList);

		String loanOutAccount = jsonArg3DesXmlPara.get("pAccount").toString();
		String loanInAccount = jsonArgeXtraPara.get("bidContractNo").toString();
		String LoanOutMoneymoremore = findMoneymoremoreIdByAccount(error,platformId, loanOutAccount);
		String LoanInMoneymoremore = findMoneymoremoreIdByAccount(error,platformId, loanInAccount);

		List<Map<String, Object>> listmlib = new ArrayList<Map<String, Object>>();
		Map<String, Object> loanInfoBean = new HashMap<String, Object>();
		loanInfoBean.put("LoanOutMoneymoremore", LoanOutMoneymoremore);
		loanInfoBean.put("LoanInMoneymoremore", LoanInMoneymoremore);
		loanInfoBean.put("OrderNo", jsonArg3DesXmlPara.getString("pMerBillNo"));
		loanInfoBean.put("BatchNo", jsonArg3DesXmlPara.getString("pBidNo"));
		loanInfoBean.put("ExchangeBatchNo", "");
		loanInfoBean.put("AdvanceBatchNo", "");
		loanInfoBean.put("Amount", jsonArg3DesXmlPara.getString("pTrdAmt"));
		loanInfoBean.put("FullAmount", jsonArgeXtraPara.getString("fullAmount"));  //满标控制
		loanInfoBean.put("TransferName", "投标");
		loanInfoBean.put("Remark", "冻结投资金额");
		loanInfoBean.put("SecondaryJsonList", SecondaryJsonList);
		listmlib.add(loanInfoBean);

		String LoanJsonList = LoanUtil.toJson(listmlib);

		Logger.info("LoanJsonList = %s", LoanJsonList);

		String PlatformMoneymoremore = argMerCode;
		String TransferAction = "1"; // 投标

		String Action = "1".equals(jsonArg3DesXmlPara.getString("pRegType"))?"1":"2" ; //1 手动转账，2自动转账
		
		String TransferType = "2"; // 直连
		String NeedAudit = ""; // 需要审核，冻结资金
		String RandomTimeStamp = LoanConstants.RANDOM_TIME_STAMP;

//		String Remark1 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pWebUrl"));
		String Remark1 = Codec.encodeBASE64(jsonArgeXtraPara.getString("pWSUrl"));
		String Remark2 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pS2SUrl"));
		String Remark3 = Codec.encodeBASE64(platformMemberId 
				+ LoanConstants.TOKEN + jsonArg3DesXmlPara.getString("pFee")
				+ LoanConstants.TOKEN + remainFee
				);
		Logger.info("Remark.length = %s", Remark1.length() + "," + Remark2.length() + "," + Remark3.length());

		String SubmitURL = LoanConstants.LOAN_URL[4]; // 双乾转账接口
		String ReturnURL = LoanReturnURL.REGISTER_CREDITOR;
		String NotifyURL = LoanNotifyURL.REGISTER_CREDITOR;
		String privatekey = LoanConstants.privateKeyPKCS8;

		String dataStr = LoanJsonList + PlatformMoneymoremore
				+ TransferAction + Action + TransferType + NeedAudit
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ReturnURL
				+ NotifyURL;
		// 签名
		RsaHelper rsa = RsaHelper.getInstance();
		String SignInfo = rsa.signData(dataStr, privatekey);

		LoanJsonList = LoanUtil.UrlEncoder(LoanJsonList, "utf-8");

		Map<String, Object> args = new HashMap<String, Object>();
		args.put("LoanJsonList", LoanJsonList);
		args.put("PlatformMoneymoremore", PlatformMoneymoremore);
		args.put("TransferAction", TransferAction);
		args.put("Action", Action);
		args.put("TransferType", TransferType);
		args.put("NeedAudit", NeedAudit);
		args.put("RandomTimeStamp", RandomTimeStamp);
		args.put("Remark1", Remark1);
		args.put("Remark2", Remark2);
		args.put("Remark3", Remark3);
		args.put("ReturnURL", ReturnURL);
		args.put("NotifyURL", NotifyURL);
		args.put("SignInfo", SignInfo);
		args.put("SubmitURL", SubmitURL);

		LoanUtil.printLoggerToLoan(args);

		//添加用户事件
		String serialNumber = jsonArg3DesXmlPara.getString("pMerBillNo");
		String frontUrl = jsonArgeXtraPara.getString("pWSUrl");
		String backgroundUrl = jsonArg3DesXmlPara.getString("pS2SUrl");
		DealDetail.addEvent(platformMemberId, LoanConstants.REGISTER_CREDITOR + 200, platformId, serialNumber, frontUrl, backgroundUrl, "", "");

		//添加交易记录
		double amount = Convert.strToDouble(jsonArg3DesXmlPara.getString("pTrdAmt").trim(), 0);
		String summary = "投标，冻结转账到借款人";
		DealDetail.addDealDetail(platformMemberId, (int)platformId, serialNumber, LoanConstants.REGISTER_CREDITOR, amount, false, summary);
		
		return args;
	}

	/**
	 * 登记债权转让,参数转化（转账：单笔，其他，有二次分配，手动，无需审核，成交汇款）
	 * 
	 * @param error
	 * @param platformId
	 * @param platformMemberId
	 * @param memberName
	 * @param argMerCode
	 * @param jsonArg3DesXmlPara
	 * @return
	 */
	public static Map<String, Object> convertRegisterCretansferParams(
			ErrorInfo error, long platformId, long platformMemberId,
			String memberName, String argMerCode,
			JSONObject jsonArg3DesXmlPara, JSONObject jsonArgeXtraPara) {
		error.clear();

		// 必传字段校验
		String[] needsParams = { "pToAccount","pFromAccount","pFromFee","pMerBillNo","pBidNo","pPayAmt","pWebUrl", "pS2SUrl" };
		String result = checkNeedsParams(needsParams, jsonArg3DesXmlPara,
				jsonArgeXtraPara);
		if (result != null) {
			error.code = -1;
			error.msg = "参数" + result + "为必传字段";

			return null;
		}

		String SecondaryJsonList = "";
		String pFromFee = jsonArg3DesXmlPara.getString("pFromFee");
		if(!(StringUtils.isBlank(pFromFee) || "[]".equals(pFromFee)) && Convert.strToDouble(pFromFee, 0) != 0){
			List<Map<String, String>> loanInfoSecondaryBeans = new ArrayList<Map<String, String>>();
			Map<String, String> loanInfoSecondaryBean = new HashMap<String, String>();
			loanInfoSecondaryBean.put("LoanInMoneymoremore", argMerCode);
			loanInfoSecondaryBean.put("Amount",pFromFee);
			loanInfoSecondaryBean.put("TransferName", "债权转让手续费");
			loanInfoSecondaryBean.put("Remark", "债权转让手续费");
			loanInfoSecondaryBeans.add(loanInfoSecondaryBean);
			
			SecondaryJsonList = LoanUtil.toJson(loanInfoSecondaryBeans);
		}

		String loanOutAccount = jsonArg3DesXmlPara.getString("pToAccount");
		String LoanOutMoneymoremore = findMoneymoremoreIdByAccount(error,platformId, loanOutAccount);

		String loanInAccount = jsonArg3DesXmlPara.getString("pFromAccount");
		String LoanInMoneymoremore = findMoneymoremoreIdByAccount(error,platformId, loanInAccount);
		
		List<Map<String, Object>> loanInfoBeans = new ArrayList<Map<String, Object>>();
		Map<String, Object> loanInfoBean = new HashMap<String, Object>();
		loanInfoBean.put("LoanOutMoneymoremore",LoanOutMoneymoremore);
		loanInfoBean.put("LoanInMoneymoremore",LoanInMoneymoremore);
		loanInfoBean.put("OrderNo", jsonArg3DesXmlPara.getString("pMerBillNo"));
		loanInfoBean.put("BatchNo", jsonArg3DesXmlPara.getString("pBidNo"));
		loanInfoBean.put("ExchangeBatchNo", "");
		loanInfoBean.put("AdvanceBatchNo", "");
		loanInfoBean.put("Amount", jsonArg3DesXmlPara.getString("pPayAmt"));
		loanInfoBean.put("FullAmount", "");
		loanInfoBean.put("TransferName", "债权转让");
		loanInfoBean.put("Remark", "债权转让");
		loanInfoBean.put("SecondaryJsonList", SecondaryJsonList);
		loanInfoBeans.add(loanInfoBean);

		String LoanJsonList = LoanUtil.toJson(loanInfoBeans);

		String PlatformMoneymoremore = argMerCode;
		String TransferAction = "3"; // 其他
		String Action = "1"; // 手动转账
		String TransferType = "2"; // 直连
		String NeedAudit = "1"; // 无需审核，直接转账
		String RandomTimeStamp = LoanConstants.RANDOM_TIME_STAMP;

		String Remark1 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pWebUrl"));
		String Remark2 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pS2SUrl"));
		String Remark3 = Codec.encodeBASE64(platformMemberId + "");
		Logger.info("Remark.length = %s", Remark1.length() + "," + Remark2.length() + "," + Remark3.length());

		String SubmitURL = LoanConstants.LOAN_URL[4]; // 双乾转账接口
		String ReturnURL = LoanReturnURL.REGISTER_CRETANSFER;
		String NotifyURL = LoanNotifyURL.REGISTER_CRETANSFER;
		String privatekey = LoanConstants.privateKeyPKCS8;

		String dataStr = LoanJsonList + PlatformMoneymoremore + TransferAction
				+ Action + TransferType + NeedAudit + RandomTimeStamp + Remark1
				+ Remark2 + Remark3 + ReturnURL + NotifyURL;
		// 签名
		RsaHelper rsa = RsaHelper.getInstance();
		String SignInfo = rsa.signData(dataStr, privatekey);

		LoanJsonList = LoanUtil.UrlEncoder(LoanJsonList, "utf-8");

		Map<String, Object> args = new HashMap<String, Object>();
		args.put("LoanJsonList", LoanJsonList);
		args.put("PlatformMoneymoremore", PlatformMoneymoremore);
		args.put("TransferAction", TransferAction);
		args.put("Action", Action);
		args.put("TransferType", TransferType);
		args.put("NeedAudit", NeedAudit);
		args.put("RandomTimeStamp", RandomTimeStamp);
		args.put("Remark1", Remark1);
		args.put("Remark2", Remark2);
		args.put("Remark3", Remark3);
		args.put("ReturnURL", ReturnURL);
		args.put("NotifyURL", NotifyURL);
		args.put("SignInfo", SignInfo);
		args.put("SubmitURL", SubmitURL);

		LoanUtil.printLoggerToLoan(args);

		//添加用户事件
		String serialNumber = jsonArg3DesXmlPara.getString("pMerBillNo");
		String frontUrl = jsonArg3DesXmlPara.getString("pWebUrl");
		String backgroundUrl = jsonArg3DesXmlPara.getString("pS2SUrl");
		DealDetail.addEvent(platformMemberId, LoanConstants.REGISTER_CRETANSFER + 200, platformId, serialNumber, frontUrl, backgroundUrl, "", "");

		//添加交易记录
		double amount = Convert.strToDouble(jsonArg3DesXmlPara.getString("pPayAmt").trim(), 0);
		String summary = "债权转让，转账到转让人";
		DealDetail.addDealDetail(platformMemberId, (int)platformId, serialNumber, LoanConstants.REGISTER_CRETANSFER, amount, false, summary);
		
		return args;
	}
	
	/**
	 * 自动投标/还款签约,参数转化（授权：type为1时，授权类型为投标；type为2时，授权类型为还款）
	 * 
	 * @param error
	 * @param platformId
	 * @param platformMemberId
	 * @param memberName
	 * @param argMerCode
	 * @param jsonArg3DesXmlPara
	 * @return
	 */
	public static Map<String, Object> convertSignParams(ErrorInfo error,
			String type, long platformId, long platformMemberId,
			String memberName, String argMerCode,
			JSONObject jsonArg3DesXmlPara, JSONObject jsonArgeXtraPara) {
		error.clear();

		// 必传字段校验
		String[] needsParams = {"pMerBillNo", "pIpsAcctNo", "pWebUrl", "pS2SUrl" };
		String result = checkNeedsParams(needsParams, jsonArg3DesXmlPara,
				jsonArgeXtraPara);
		if (result != null) {
			error.code = -1;
			error.msg = "参数" + result + "为必传字段";

			return null;
		}

		String loanAccount = jsonArg3DesXmlPara.get("pIpsAcctNo").toString();
		String MoneymoremoreId = findMoneymoremoreIdByAccount(error,
				platformId, loanAccount);

		String PlatformMoneymoremore = argMerCode;

		String AuthorizeTypeOpen = type;

		String AuthorizeTypeClose = "";
		String RandomTimeStamp = LoanConstants.RANDOM_TIME_STAMP;

		String Remark1 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pWebUrl"));
		String Remark2 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pS2SUrl"));
		String Remark3 = Codec.encodeBASE64(platformMemberId
				+ LoanConstants.TOKEN + jsonArg3DesXmlPara.getString("pMerBillNo"));
		Logger.info("Remark.length = %s", Remark1.length() + "," + Remark2.length() + "," + Remark3.length());

		String SubmitURL = LoanConstants.LOAN_URL[7]; // 双乾授权接口
		String ReturnURL = LoanReturnURL.SIGING;
		String NotifyURL = LoanNotifyURL.SIGING;
		String privatekey = LoanConstants.privateKeyPKCS8;

		String dataStr = MoneymoremoreId + PlatformMoneymoremore
				+ AuthorizeTypeOpen + AuthorizeTypeClose + RandomTimeStamp
				+ Remark1 + Remark2 + Remark3 + ReturnURL + NotifyURL;
		// 签名
		RsaHelper rsa = RsaHelper.getInstance();
		String SignInfo = rsa.signData(dataStr, privatekey);

		Map<String, Object> args = new HashMap<String, Object>();
		args.put("MoneymoremoreId", MoneymoremoreId);
		args.put("PlatformMoneymoremore", PlatformMoneymoremore);
		args.put("AuthorizeTypeOpen", AuthorizeTypeOpen);
		args.put("AuthorizeTypeClose", AuthorizeTypeClose);
		args.put("RandomTimeStamp", RandomTimeStamp);
		args.put("Remark1", Remark1);
		args.put("Remark2", Remark2);
		args.put("Remark3", Remark3);
		args.put("ReturnURL", ReturnURL);
		args.put("NotifyURL", NotifyURL);
		args.put("SignInfo", SignInfo);
		args.put("SubmitURL", SubmitURL);

		LoanUtil.printLoggerToLoan(args);

		//添加用户事件
		String serialNumber = jsonArg3DesXmlPara.getString("pMerBillNo");
		String frontUrl = jsonArg3DesXmlPara.getString("pWebUrl");
		String backgroundUrl = jsonArg3DesXmlPara.getString("pS2SUrl");
		DealDetail.addEvent(platformMemberId, LoanConstants.REPAYMENT_SIGNING + 200, platformId, serialNumber, frontUrl, backgroundUrl, "", "");
		
		return args;
	}

	/**
	 * 充值,参数转化
	 * 
	 * @param error
	 * @param platformId
	 * @param platformMemberId
	 * @param memberName
	 * @param argMerCode
	 * @param jsonArg3DesXmlPara
	 * @return
	 */
	public static Map<String, Object> convertRechargeParams(ErrorInfo error,
			long platformId, long platformMemberId, String memberName,
			String argMerCode, JSONObject jsonArg3DesXmlPara,
			JSONObject jsonArgeXtraPara) {
		error.clear();

		// 必传字段校验
		String[] needsParams = {"pMerBillNo", "pIpsAcctNo","pTrdAmt", "pWSUrl", "pS2SUrl" };
		String result = checkNeedsParams(needsParams, jsonArg3DesXmlPara,
				jsonArgeXtraPara);
		if (result != null) {
			error.code = -1;
			error.msg = "参数" + result + "为必传字段";

			return null;
		}

		String loanAccount = jsonArg3DesXmlPara.getString("pIpsAcctNo");
		String RechargeMoneymoremore = findMoneymoremoreIdByAccount(error,platformId, loanAccount);
		if (StringUtils.isBlank(RechargeMoneymoremore)) { // 只支持个人充值
			return null;
		}

		String PlatformMoneymoremore = argMerCode;
		String OrderNo = jsonArg3DesXmlPara.getString("pMerBillNo");
		String Amount = jsonArg3DesXmlPara.getString("pTrdAmt");
		String RechargeType = LoanConstants.RECHARGE_TYPE;
		String FeeType = LoanConstants.FEE_TYPE;
		String CardNo = LoanConstants.CARD_NO;
		String RandomTimeStamp = LoanConstants.RANDOM_TIME_STAMP;

//		String Remark1 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pWebUrl"));
		String Remark1 = Codec.encodeBASE64(jsonArgeXtraPara.getString("pWSUrl"));
		String Remark2 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pS2SUrl"));
		String Remark3 = Codec.encodeBASE64(platformMemberId
				+ LoanConstants.TOKEN + jsonArg3DesXmlPara.getString("pMerBillNo"));
		Logger.info("Remark.length = %s", Remark1.length() + "," + Remark2.length() + "," + Remark3.length());
		
		String SubmitURL = LoanConstants.LOAN_URL[3];
		String ReturnURL = LoanReturnURL.RECHARGE;
		String NotifyURL = LoanNotifyURL.RECHARGE;
		String privatekey = LoanConstants.privateKeyPKCS8;
		String publickey = LoanConstants.publicKey;

		String dataStr = RechargeMoneymoremore + PlatformMoneymoremore
				+ OrderNo + Amount + RechargeType + FeeType + CardNo
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ReturnURL
				+ NotifyURL;

		// 签名
		RsaHelper rsa = RsaHelper.getInstance();
		String SignInfo = rsa.signData(dataStr, privatekey);

		if (StringUtils.isNotBlank(CardNo)) {
			CardNo = rsa.encryptData(CardNo, publickey);
		}

		Map<String, Object> args = new HashMap<String, Object>();
		args.put("RechargeMoneymoremore", RechargeMoneymoremore);
		args.put("PlatformMoneymoremore", PlatformMoneymoremore);
		args.put("OrderNo", OrderNo);
		args.put("Amount", Amount);
		args.put("RechargeType", RechargeType);
		args.put("FeeType", FeeType);
		args.put("CardNo", CardNo);
		args.put("RandomTimeStamp", RandomTimeStamp);
		args.put("Remark1", Remark1);
		args.put("Remark2", Remark2);
		args.put("Remark3", Remark3);
		args.put("ReturnURL", ReturnURL);
		args.put("NotifyURL", NotifyURL);
		args.put("SignInfo", SignInfo);
		args.put("SubmitURL", SubmitURL);

		LoanUtil.printLoggerToLoan(args);
		
		//添加用户事件
		String serialNumber = jsonArg3DesXmlPara.getString("pMerBillNo");
		String frontUrl = jsonArgeXtraPara.getString("pWSUrl");
		String backgroundUrl = jsonArg3DesXmlPara.getString("pS2SUrl");
		DealDetail.addEvent(platformMemberId, LoanConstants.RECHARGE + 200, platformId, serialNumber, frontUrl, backgroundUrl, "", "");
		
		//添加交易记录
		double amount = Convert.strToDouble(Amount.trim(), 0);
		String summary = "充值";
		DealDetail.addDealDetail(platformMemberId, (int)platformId, serialNumber, LoanConstants.RECHARGE, amount, false, summary);
				
		return args;
	}

	/**
	 * 转账：投资,参数转化（审核，通过，放款到借款人，二次分配到平台）
	 * 
	 * @param error
	 * @param platformId
	 * @param platformMemberId
	 * @param memberName
	 * @param argMerCode
	 * @param jsonArg3DesXmlPara
	 * @return
	 */
	public static Map<String, Object> convertTransferParams(ErrorInfo error,
			long platformId, long platformMemberId, String memberName,
			String argMerCode, JSONObject jsonArg3DesXmlPara,
			JSONObject jsonArgeXtraPara) {
		error.clear();

		// 必传字段校验
		String[] needsParams = {"pMerBillNo", "pBillNos","pBidBillNo","pWebUrl", "pS2SUrl" };
		String result = checkNeedsParams(needsParams, jsonArg3DesXmlPara,jsonArgeXtraPara);
		if (result != null) {
			error.code = -1;
			error.msg = "参数" + result + "为必传字段";

			return null;
		}

		String bidBillNo = jsonArgeXtraPara.getString("pBidBillNo");  //发标流水号
		
		String billNos = jsonArgeXtraPara.getString("pBillNos");  //投标流水号
		String[] billArary = billNos.split(",");

		Map<String, String> map = new HashMap<String, String>();
		String transferBillNos = "";  //本次处理转账
		String batchId = jsonArgeXtraPara.containsKey("batchId")?jsonArgeXtraPara.getString("batchId"):"-1";  //本次处理批次id,-1表示无分批
		map.put("transferBillNos", transferBillNos);
		map.put("batchId", batchId);
		
		if(billArary.length > LoanConstants.TRANSFER_MAX_BILL){  //超过200笔交易，分批审核
			map = batchTransferAudit(error, bidBillNo, billArary, billNos, map);

			if(error.code < 0){
				return null;
			}
		}else{
			map.put("transferBillNos", billNos);
			if(!"-1".equals(batchId)){  //分批审核时，修改状态为处理中
				TransferBatches.updateStatus(Long.parseLong(batchId),1,error);  //处理中
			}else{  //无需分批时，对账
				String LoanNo = billNos.split(",")[0].trim();
				String resultStr = loanOrderQuery(LoanConstants.queryTransfer, LoanNo,null)[1];
				if(StringUtils.isNotBlank(resultStr)&&(resultStr.startsWith("[") || resultStr.startsWith("{"))){
					String actState = getLoanMap(resultStr).get("ActState").toString(); // 0.未操作1.已通过2.已退回3.自动通过
					if (!"0".equals(actState)) {
						//已审核
						error.code = LoanConstants.TRANSFER_REPAIR_SUCCESS;
						return null;
					}
				}
			}
		}
		
		transferBillNos = map.get("transferBillNos");
		batchId = map.get("batchId");
		
		String LoanNoList = transferBillNos;
		String PlatformMoneymoremore = argMerCode;
		String AuditType = "1"; // 通过
		String RandomTimeStamp = LoanConstants.RANDOM_TIME_STAMP;

		String Remark1 = Codec.encodeBASE64(jsonArgeXtraPara.getString("pWebUrl"));
		String Remark2 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pS2SUrl"));
		String Remark3 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pMerBillNo")
						+ LoanConstants.TOKEN + LoanConstants.INVEST
						+ LoanConstants.TOKEN + platformMemberId
						+ LoanConstants.TOKEN + bidBillNo
						+ LoanConstants.TOKEN + batchId
						);
		Logger.info("Remark.length = %s", Remark1.length() + "," + Remark2.length() + "," + Remark3.length());
		
		String SubmitURL = LoanConstants.LOAN_URL[8]; // 审核
		String ReturnURL = LoanReturnURL.TRANSFER;
		String NotifyURL = LoanNotifyURL.TRANSFER;
		String privatekey = LoanConstants.privateKeyPKCS8;

		String dataStr = LoanNoList + PlatformMoneymoremore + AuditType
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ReturnURL
				+ NotifyURL;
		// 签名
		RsaHelper rsa = RsaHelper.getInstance();
		String SignInfo = rsa.signData(dataStr, privatekey);

		Map<String, Object> args = new HashMap<String, Object>();
		args.put("LoanNoList", LoanNoList);
		args.put("PlatformMoneymoremore", PlatformMoneymoremore);
		args.put("AuditType", AuditType);
		args.put("RandomTimeStamp", RandomTimeStamp);
		args.put("Remark1", Remark1);
		args.put("Remark2", Remark2);
		args.put("Remark3", Remark3);
		args.put("ReturnURL", ReturnURL);
		args.put("NotifyURL", NotifyURL);
		args.put("SignInfo", SignInfo);
		args.put("SubmitURL", SubmitURL);

		LoanUtil.printLoggerToLoan(args);

		//添加用户事件
		String serialNumber = jsonArg3DesXmlPara.getString("pMerBillNo");
		String frontUrl = jsonArgeXtraPara.getString("pWebUrl");
		String backgroundUrl = jsonArg3DesXmlPara.getString("pS2SUrl");
		String remark = "分批号:" + batchId;
		DealDetail.addEvent(platformMemberId, LoanConstants.TRANSFER + 200, platformId, serialNumber, frontUrl, backgroundUrl, "", remark);
		
		return args;
	}
	
	/**
	 * 审核超过200笔时，分批审核
	 * @param error
	 * @param bidBillNo
	 * @param billArary
	 * @param billNos
	 * @param map
	 * @return
	 */
	private static Map<String, String> batchTransferAudit(ErrorInfo error, String bidBillNo, String[] billArary, String billNos, Map<String, String> map) {
		String batchId = "";
		String transferBillNos = "";

		List<TransferBatches> list =  TransferBatches.queryByBidBillNo(bidBillNo);
		if(list != null){  //已存在审核记录
			boolean flag = true;  //是否完成所有审核
			for(TransferBatches tb : list){
				if(tb.status == 0){  //有未审核转账，继续处理
					batchId = tb.id + "";
					transferBillNos = tb.transferBillNos;
					TransferBatches.updateStatus(tb.id, 1, error);
					flag = false;
					break;
				}
			}
			
			if(flag){  //审核完成
				error.code = LoanConstants.TRANSFER_EXECUTED;
				return map;
			}
			
		}else{
			//无分批审核信息，保存分批信息
			int batches = (int) Math.ceil(1.0 * billArary.length / LoanConstants.TRANSFER_MAX_BILL);
			for(int i=1;i<=batches;i++){
				int begin = (i-1)*LoanConstants.TRANSFER_MAX_BILL;
				int end = i*LoanConstants.TRANSFER_MAX_BILL;
				end = end>billArary.length?-1:end;
				
				transferBillNos = LoanUtil.subString(billNos, begin,end,",");
				
				if(i==1){
					//保存并审核第一批
					TransferBatches tansferBaches = new TransferBatches(i,bidBillNo, transferBillNos, 1,1);
					batchId = tansferBaches.create(error)+"";
					if(error.code < 0){
						error.code = LoanConstants.TRANSFER_ERROR;
						return map;
					}
					
				}else{
					//保存其他批次
					TransferBatches tansferBaches = new TransferBatches(i,bidBillNo, transferBillNos, 1,0);
					tansferBaches.create(error);
					if(error.code < 0){
						error.code = LoanConstants.TRANSFER_ERROR;
						return map;
					}
				}
			}
			//处理第一批
			transferBillNos = LoanUtil.subString(billNos, 0,LoanConstants.TRANSFER_MAX_BILL,",");
		}
		
		map.put("batchId", batchId);
		map.put("transferBillNos", transferBillNos);
		
		return map;
	}

	/**
	 * 转账：代偿
	 * 
	 * @param error
	 * @param platformId
	 * @param platformMemberId
	 * @param memberName
	 * @param argMerCode
	 * @param jsonArg3DesXmlPara
	 * @return
	 */
	public static Map<String, Object> convertCompensateParams(ErrorInfo error,
			long platformId, long platformMemberId, String memberName,
			String argMerCode, JSONObject jsonArg3DesXmlPara,
			JSONObject jsonArgeXtraPara) {

		error.clear();

		// 必传字段校验
		String[] needsParams = {"pMerBillNo","pBidNo","periods","pTransferType","pDetails","pMemo1","pMemo3","pWebUrl", "pS2SUrl"};
		String result = checkNeedsParams(needsParams, jsonArg3DesXmlPara,
				jsonArgeXtraPara);
		if (result != null) {
			error.code = -1;
			error.msg = "参数" + result + "为必传字段";

			return null;
		}

		JSONArray jsonArr = null;

		Object pDetails = jsonArg3DesXmlPara.get("pDetails");
		
		if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
			JSONObject pDetail = (JSONObject)pDetails; 
			JSONObject pRow = pDetail.getJSONObject("pRow"); 
	
			jsonArr = new JSONArray(); 
			jsonArr.add(pRow); 
		} else {
			jsonArr = jsonArg3DesXmlPara.getJSONArray("pDetails");
		} 
		
		JSONArray transfers = null;
		String period = jsonArgeXtraPara.getString("periods");
		String bidNo = jsonArg3DesXmlPara.getString("pBidNo");
		//分批操作：bidBillNo为每一批的唯一标识，生成法则; 标的号  + 分隔符  + 还款期号
		String bidBillNo = bidNo + LoanConstants.BILLTOKEN + period;
		
		Map<String, Object> map = new HashMap<String, Object>();
		String batchId = jsonArg3DesXmlPara.containsKey("batchId")?jsonArg3DesXmlPara.getString("batchId"):"-1";
		int batchNo = jsonArg3DesXmlPara.containsKey("batchNo")?Integer.parseInt(jsonArg3DesXmlPara.getString("batchNo")):0;  //批次编号，0表示无分批
		map.put("batchId", batchId);
		map.put("batchNo", batchNo);
		map.put("transfers", transfers);
		
		//转账超过200笔处理
		if(jsonArr.size() > LoanConstants.TRANSFER_MAX_BILL){
			map = batchTransfer(error, bidBillNo, jsonArg3DesXmlPara, jsonArgeXtraPara, jsonArr, map);
			if(error.code < 0){
				return null;
			}
		}else{
			map.put("transfers", jsonArr);
			if(!"-1".equals(batchId)){  //修改分批处理状态
				TransferBatches.updateStatus(Long.parseLong(batchId),1,error);  //处理中
			}else{  //无需分批时，对账
				String OrderNo =  jsonArg3DesXmlPara.getString("pMerBillNo") + LoanConstants.BILLTOKEN + "0";
				String resultStr = loanOrderQuery(LoanConstants.queryTransfer, "",OrderNo)[1];
				if(StringUtils.isNotBlank(resultStr)&&(resultStr.startsWith("[") || resultStr.startsWith("{"))){
					String actState = getLoanMap(resultStr).get("ActState").toString(); // 0.未操作1.已通过2.已退回3.自动通过
					if (!"0".equals(actState)) {
						// 还款成功
						error.code = LoanConstants.TRANSFER_EXECUTED;
						return null;
					}
				}
			}
		}
		
		batchId = (String) map.get("batchId");
		batchNo = (Integer) map.get("batchNo");
		transfers = (JSONArray) map.get("transfers");
		
		List<Map<String, Object>> loanInfoBeans = new ArrayList<Map<String, Object>>();
		JSONObject pRow = null;

		String loanInAccount = "";
		String LoanInMoneymoremore = "";
		
		double amount = 0;
		for (int i = 0; i < transfers.size(); i++) {
			pRow = transfers.getJSONObject(i);
			
			loanInAccount = pRow.getString("pTIpsAcctNo").trim();
			LoanInMoneymoremore = findMoneymoremoreIdByAccount(error,platformId, loanInAccount);

			double pTrdAmt = Convert.strToDouble(pRow.getString("pTrdAmt").trim(), 0);
			double pTTrdFee = Convert.strToDouble(pRow.getString("pTTrdFee").trim(), 0);
			double transferAmount = pTrdAmt - pTTrdFee;
			
			Map<String, Object> loanInfoBean = new HashMap<String, Object>();
			loanInfoBean.put("LoanOutMoneymoremore",argMerCode);
			loanInfoBean.put("LoanInMoneymoremore",LoanInMoneymoremore);
			//每一笔转账流水号生成法则：① 分批转账：还款流水号  + 分隔符 + 还款期号  + 批号 + 循环i；②一次转账：还款流水号  + 分隔符 + 循环i
			loanInfoBean.put("OrderNo", jsonArg3DesXmlPara.getString("pMerBillNo") + LoanConstants.BILLTOKEN + ("-1".equals(batchId)?"":(period + batchNo)) + i);
			loanInfoBean.put("BatchNo", jsonArg3DesXmlPara.getString("pBidNo"));
			loanInfoBean.put("ExchangeBatchNo", "");
			loanInfoBean.put("AdvanceBatchNo", "");
			loanInfoBean.put("Amount", String.valueOf(transferAmount));
			loanInfoBean.put("FullAmount", "");
			loanInfoBean.put("TransferName", "代偿");
			loanInfoBean.put("Remark", "本金垫付/线下收款");
			loanInfoBean.put("SecondaryJsonList", "");
			loanInfoBeans.add(loanInfoBean);
			
			amount = amount + Convert.strToDouble(String.valueOf(transferAmount), 0);
		}

		String LoanJsonList = LoanUtil.toJson(loanInfoBeans);
		String PlatformMoneymoremore = argMerCode;
		String TransferAction = "3"; //其他
		String Action = "1"; // 1手动转账
		String TransferType = "2"; // 直连
		String NeedAudit = "1"; // 无需审核，直接转账
		String RandomTimeStamp = LoanConstants.RANDOM_TIME_STAMP;

		String Remark1 = Codec.encodeBASE64(jsonArgeXtraPara.getString("pWebUrl"));
		String Remark2 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pS2SUrl"));
		String Remark3 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pMerBillNo")
				+ LoanConstants.TOKEN + jsonArg3DesXmlPara.getString("pMemo1")
				+ LoanConstants.TOKEN + jsonArg3DesXmlPara.getString("pMemo3")
				+ LoanConstants.TOKEN + map.get("batchId")
				+ LoanConstants.TOKEN + platformId
				+ LoanConstants.TOKEN + bidNo
				+ LoanConstants.TOKEN + period
				+ LoanConstants.TOKEN + map.get("batchNo")
				);
		Logger.info("Remark.length = %s", Remark1.length() + "," + Remark2.length() + "," + Remark3.length());

		String SubmitURL = LoanConstants.LOAN_URL[4]; // 双乾转账接口
		String ReturnURL = LoanReturnURL.COMPENSATER;
		String NotifyURL = LoanNotifyURL.COMPENSATER;

		String privatekey = LoanConstants.privateKeyPKCS8;

		String dataStr = LoanJsonList + PlatformMoneymoremore + TransferAction
				+ Action + TransferType + NeedAudit + RandomTimeStamp + Remark1
				+ Remark2 + Remark3 + ReturnURL + NotifyURL;
		// 签名
		RsaHelper rsa = RsaHelper.getInstance();
		String SignInfo = rsa.signData(dataStr, privatekey);

		LoanJsonList = LoanUtil.UrlEncoder(LoanJsonList, "utf-8");

		Map<String, Object> args = new HashMap<String, Object>();
		args.put("LoanJsonList", LoanJsonList);
		args.put("PlatformMoneymoremore", PlatformMoneymoremore);
		args.put("TransferAction", TransferAction);
		args.put("Action", Action);
		args.put("TransferType", TransferType);
		args.put("NeedAudit", NeedAudit);
		args.put("RandomTimeStamp", RandomTimeStamp);
		args.put("Remark1", Remark1);
		args.put("Remark2", Remark2);
		args.put("Remark3", Remark3);
		args.put("ReturnURL", ReturnURL);
		args.put("NotifyURL", NotifyURL);
		args.put("SignInfo", SignInfo);
		args.put("SubmitURL", SubmitURL);

		LoanUtil.printLoggerToLoan(args);

		//添加用户事件
		String serialNumber = jsonArg3DesXmlPara.getString("pMerBillNo") + LoanConstants.BILLTOKEN + ("-1".equals(batchId)?"":batchId);
		String frontUrl = jsonArgeXtraPara.getString("pWebUrl");
		String backgroundUrl = jsonArg3DesXmlPara.getString("pS2SUrl");
		DealDetail.addEvent(platformMemberId, LoanConstants.TRANSFER + 200, platformId, serialNumber, frontUrl, backgroundUrl, "", "");
		
		//添加交易记录
		String summary = "代偿";
		DealDetail.addDealDetail(platformMemberId, (int)platformId, serialNumber, LoanConstants.TRANSFER, amount, false, summary);
						
		
		return args;
	}
	
	/**
	 * 转账-代偿还款：
	 * @param error
	 * @param platformId
	 * @param platformMemberId
	 * @param memberName
	 * @param argMerCode
	 * @param jsonArg3DesXmlPara
	 * @param jsonArgeXtraPara
	 * @return
	 */
	public static Map<String, Object> convertCompensateRepaymentParams(ErrorInfo error,
			long platformId, long platformMemberId, String memberName,
			String argMerCode, JSONObject jsonArg3DesXmlPara,
			JSONObject jsonArgeXtraPara) {

		error.clear();

		// 必传字段校验
		String[] needsParams = {"pMerBillNo","pBidNo","pTransferType","pDetails","pMemo1","pMemo3","pWebUrl", "pS2SUrl"};
		String result = checkNeedsParams(needsParams, jsonArg3DesXmlPara,
				jsonArgeXtraPara);
		if (result != null) {
			error.code = -1;
			error.msg = "参数" + result + "为必传字段";

			return null;
		}
		
		JSONArray jsonArr = null;

		Object pDetails = jsonArg3DesXmlPara.get("pDetails");
		
		if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
			JSONObject pDetail = (JSONObject)pDetails; 
			JSONObject pRow = pDetail.getJSONObject("pRow"); 
	
			jsonArr = new JSONArray(); 
			jsonArr.add(pRow); 
		} else {
			jsonArr = jsonArg3DesXmlPara.getJSONArray("pDetails");
		}
		
		JSONObject pRow = jsonArr.getJSONObject(0);

		String loanAccount = pRow.getString("pFIpsAcctNo");
		String LoanOutMoneymoremore = findMoneymoremoreIdByAccount(error,platformId, loanAccount);

		List<Map<String, Object>> listmlib = new ArrayList<Map<String, Object>>();

		Map<String, Object> loanInfoBean = new HashMap<String, Object>();
		loanInfoBean.put("LoanOutMoneymoremore", LoanOutMoneymoremore);
		loanInfoBean.put("LoanInMoneymoremore", argMerCode);
		loanInfoBean.put("OrderNo", jsonArg3DesXmlPara.getString("pMerBillNo"));
		loanInfoBean.put("BatchNo", jsonArg3DesXmlPara.getString("pBidNo"));
		loanInfoBean.put("ExchangeBatchNo", "");
		loanInfoBean.put("AdvanceBatchNo", "");
		loanInfoBean.put("Amount", pRow.getString("pTrdAmt"));
		loanInfoBean.put("FullAmount", "");
		loanInfoBean.put("TransferName", "代偿还款");
		loanInfoBean.put("Remark", "代偿还款");
		loanInfoBean.put("SecondaryJsonList", "");
		listmlib.add(loanInfoBean);

		String LoanJsonList = LoanUtil.toJson(listmlib);

		String PlatformMoneymoremore = argMerCode;
		String TransferAction = "3"; // 其他
		String Action = "1"; // 手动转账
		String TransferType = "2"; // 直连
		String NeedAudit = "1"; // 无需审核，直接转账
		String RandomTimeStamp = LoanConstants.RANDOM_TIME_STAMP;

		String Remark1 = Codec.encodeBASE64(jsonArgeXtraPara.getString("pWebUrl"));
		String Remark2 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pS2SUrl"));
		String Remark3 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pMerBillNo")
				+ LoanConstants.TOKEN + jsonArg3DesXmlPara.getString("pTransferType")
				+ LoanConstants.TOKEN + jsonArg3DesXmlPara.getString("pMemo1")
				+ LoanConstants.TOKEN + jsonArg3DesXmlPara.getString("pMemo3")
				);
		Logger.info("Remark.length = %s", Remark1.length() + "," + Remark2.length() + "," + Remark3.length());

		String SubmitURL = LoanConstants.LOAN_URL[4]; // 请求双乾转账接口
		String ReturnURL = LoanReturnURL.COMPENSATER_REPAYMENT;
		String NotifyURL = LoanNotifyURL.COMPENSATER_REPAYMENT;

		String privatekey = LoanConstants.privateKeyPKCS8;

		String dataStr = LoanJsonList + PlatformMoneymoremore + TransferAction
				+ Action + TransferType + NeedAudit + RandomTimeStamp + Remark1
				+ Remark2 + Remark3 + ReturnURL + NotifyURL;
		// 签名
		RsaHelper rsa = RsaHelper.getInstance();
		String SignInfo = rsa.signData(dataStr, privatekey);

		LoanJsonList = LoanUtil.UrlEncoder(LoanJsonList, "utf-8");

		Map<String, Object> args = new HashMap<String, Object>();
		args.put("LoanJsonList", LoanJsonList);
		args.put("PlatformMoneymoremore", PlatformMoneymoremore);
		args.put("TransferAction", TransferAction);
		args.put("Action", Action);
		args.put("TransferType", TransferType);
		args.put("NeedAudit", NeedAudit);
		args.put("RandomTimeStamp", RandomTimeStamp);
		args.put("Remark1", Remark1);
		args.put("Remark2", Remark2);
		args.put("Remark3", Remark3);
		args.put("ReturnURL", ReturnURL);
		args.put("NotifyURL", NotifyURL);
		args.put("SignInfo", SignInfo);
		args.put("SubmitURL", SubmitURL);

		LoanUtil.printLoggerToLoan(args);

		//添加用户事件
		String serialNumber = jsonArg3DesXmlPara.getString("pMerBillNo");
		String frontUrl = jsonArgeXtraPara.getString("pWebUrl");
		String backgroundUrl = jsonArg3DesXmlPara.getString("pS2SUrl");
		DealDetail.addEvent(platformMemberId, LoanConstants.TRANSFER + 200, platformId, serialNumber, frontUrl, backgroundUrl, "", "");
		
		//添加交易记录
		double amount = Convert.strToDouble(pRow.getString("pTrdAmt").trim(), 0);
		String summary = "代偿还款";
		DealDetail.addDealDetail(platformMemberId, (int)platformId, serialNumber, LoanConstants.TRANSFER, amount, false, summary);
		
		return args;
	}

	/**
	 * 解冻保证金(审核：退回)
	 * 
	 * @param error
	 * @param platformId
	 * @param platformMemberId
	 * @param memberName
	 * @param argMerCode
	 * @param jsonArg3DesXmlPara
	 * @param jsonArgeXtraPara
	 * @return
	 */
	public static Map<String, Object> convertGuaranteeUnfreezeParams(
			ErrorInfo error, long platformId, long platformMemberId,
			String memberName, String argMerCode,
			JSONObject jsonArg3DesXmlPara, JSONObject jsonArgeXtraPara,String bidBillNo) {

		error.clear();

		// 必传字段校验
		String[] needsParams = {"pMerBillNo", "pTransferType","pMemo1","pWebUrl", "pS2SUrl" };
		String result = checkNeedsParams(needsParams, jsonArg3DesXmlPara,jsonArgeXtraPara);
		if (result != null) {
			error.code = -1;
			error.msg = "参数" + result + "为必传字段";

			return null;
		}

		// 对账（ws）
		String resultStr = loanOrderQuery(LoanConstants.queryTransfer, bidBillNo,null)[1];
		if(StringUtils.isNotBlank(resultStr)&&(resultStr.startsWith("[") || resultStr.startsWith("{"))){
			String actState = getLoanMap(resultStr).get("ActState").toString(); // 0.未操作1.已通过2.已退回3.自动通过
			if (!"0".equals(actState)) {
				// 解冻保证金已操作
				error.code = -100;
				error.msg = "解冻保证金已操作";
				
				return null;
			}
		}

		String LoanNoList = bidBillNo;
		String PlatformMoneymoremore = argMerCode;
		String AuditType = "2"; // 退回
		String RandomTimeStamp = LoanConstants.RANDOM_TIME_STAMP;

		String Remark1 = Codec.encodeBASE64(jsonArgeXtraPara.getString("pWebUrl"));
		String Remark2 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pS2SUrl"));
		String Remark3 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pMerBillNo") 
				+ LoanConstants.TOKEN +jsonArg3DesXmlPara.getString("pTransferType") 
				+ LoanConstants.TOKEN + platformMemberId);
		Logger.info("Remark.length = %s", Remark1.length() + "," + Remark2.length() + "," + Remark3.length());

		String SubmitURL = LoanConstants.LOAN_URL[8]; // 审核退回
		String ReturnURL = LoanReturnURL.GUARANTEE_UNFREEZE;
		String NotifyURL = LoanNotifyURL.GUARANTEE_UNFREEZE;
		String privatekey = LoanConstants.privateKeyPKCS8;

		String dataStr = LoanNoList + PlatformMoneymoremore + AuditType
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ReturnURL
				+ NotifyURL;
		// 签名
		RsaHelper rsa = RsaHelper.getInstance();
		String SignInfo = rsa.signData(dataStr, privatekey);

		Map<String, Object> args = new HashMap<String, Object>();
		args.put("LoanNoList", LoanNoList);
		args.put("PlatformMoneymoremore", PlatformMoneymoremore);
		args.put("AuditType", AuditType);
		args.put("RandomTimeStamp", RandomTimeStamp);
		args.put("Remark1", Remark1);
		args.put("Remark2", Remark2);
		args.put("Remark3", Remark3);
		args.put("ReturnURL", ReturnURL);
		args.put("NotifyURL", NotifyURL);
		args.put("SignInfo", SignInfo);
		args.put("SubmitURL", SubmitURL);

		LoanUtil.printLoggerToLoan(args);

		//添加用户事件
		String serialNumber = jsonArg3DesXmlPara.getString("pMerBillNo");
		String frontUrl = jsonArgeXtraPara.getString("pWebUrl");
		String backgroundUrl = jsonArg3DesXmlPara.getString("pS2SUrl");
		DealDetail.addEvent(platformMemberId, LoanConstants.GUARANTEE_UNFREEZE + 200, platformId, serialNumber, frontUrl, backgroundUrl, "", "");
		
		return args;
	}
	
	/**
	 * 还款,参数转化（转账：单笔，有二次分配，手动或自动，无需审核，成交汇款）
	 * 
	 * @param error
	 * @param platformId
	 * @param platformMemberId
	 * @param memberName
	 * @param argMerCode
	 * @param jsonArg3DesXmlPara
	 * @return
	 */
	public static Map<String, Object> convertRepaymentParams(ErrorInfo error,
			long platformId, long platformMemberId, String memberName,
			String argMerCode, JSONObject jsonArg3DesXmlPara,
			JSONObject jsonArgeXtraPara) {

		error.clear();

		// 必传字段校验
		String[] needsParams = {"pMerBillNo","pBidNo","period","pOutAcctNo","pRepayType","pDetails","pRepayType", "pS2SUrl" };
		String result = checkNeedsParams(needsParams, jsonArg3DesXmlPara,
				jsonArgeXtraPara);
		if (result != null) {
			error.code = -1;
			error.msg = "参数" + result + "为必传字段";

			return null;
		}

		JSONArray jsonArr = null;

		Object pDetails = jsonArg3DesXmlPara.get("pDetails");
		
		if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
			JSONObject pDetail = (JSONObject)pDetails; 
			JSONObject pRow = pDetail.getJSONObject("pRow"); 
			
			jsonArr = new JSONArray(); 
			jsonArr.add(pRow); 
		} else {
			jsonArr = jsonArg3DesXmlPara.getJSONArray("pDetails");
		} 
		
		JSONArray transfers = null;
		String period = jsonArgeXtraPara.getString("period");
		String bidNo = jsonArg3DesXmlPara.getString("pBidNo");
		//分批操作：bidBillNo为每一批的唯一标识，生成法则; 标的号  + 分隔符  + 还款期号
		String bidBillNo = bidNo + LoanConstants.BILLTOKEN + period;
		
		Map<String, Object> map = new HashMap<String, Object>();
		String batchId = jsonArg3DesXmlPara.containsKey("batchId")?jsonArg3DesXmlPara.getString("batchId"):"-1";
		int batchNo = jsonArg3DesXmlPara.containsKey("batchNo")?Integer.parseInt(jsonArg3DesXmlPara.getString("batchNo")):0;  //批次编号，0表示无分批
		map.put("batchId", batchId);
		map.put("batchNo", batchNo);
		map.put("transfers", transfers);
		
		//转账超过200笔处理
		if(jsonArr.size() > LoanConstants.TRANSFER_MAX_BILL){
			map = batchTransfer(error, bidBillNo, jsonArg3DesXmlPara, jsonArgeXtraPara, jsonArr, map);

			if(error.code < 0){
				return null;
			}
		}else{
			map.put("transfers", jsonArr);
			if(!"-1".equals(batchId)){  //修改分批处理状态
				TransferBatches.updateStatus(Long.parseLong(batchId),1,error);  //处理中
			}else{  //无需分批时，对账
				String OrderNo =  jsonArg3DesXmlPara.getString("pMerBillNo") + LoanConstants.BILLTOKEN + "0";
				String resultStr = loanOrderQuery(LoanConstants.queryTransfer, "",OrderNo)[1];
				if(StringUtils.isNotBlank(resultStr)&&(resultStr.startsWith("[") || resultStr.startsWith("{"))){
					String actState = getLoanMap(resultStr).get("ActState").toString(); // 0.未操作1.已通过2.已退回3.自动通过
					if (!"0".equals(actState)) {
						// 还款成功
						error.code = LoanConstants.TRANSFER_EXECUTED;
						return null;
					}
				}
			}
		}
		
		batchId = (String) map.get("batchId");
		batchNo = (Integer) map.get("batchNo");
		transfers = (JSONArray) map.get("transfers");
		
		List<Map<String, Object>> loanInfoBeans = new ArrayList<Map<String, Object>>();
		List<Map<String, String>> loanInfoSecondaryBeans = null;
		String SecondaryJsonList = null;
		JSONObject pRow = null;

		String loanOutAccount = jsonArg3DesXmlPara.get("pOutAcctNo").toString();
		String LoanOutMoneymoremore = findMoneymoremoreIdByAccount(error,platformId, loanOutAccount);

		String loanInAccount = "";
		String LoanInMoneymoremore = "";
		
		double amount = 0;
		for (int i = 0; i < transfers.size(); i++) {
			loanInfoSecondaryBeans = new ArrayList<Map<String, String>>();
			pRow = transfers.getJSONObject(i);
			
			Map<String, String> loanInfoSecondaryBean = new HashMap<String, String>();
			loanInfoSecondaryBean.put("LoanInMoneymoremore", argMerCode);
			loanInfoSecondaryBean.put("Amount", pRow.getString("pInFee"));
			loanInfoSecondaryBean.put("TransferName", "理财管理费");
			loanInfoSecondaryBean.put("Remark", "理财管理费");
			loanInfoSecondaryBeans.add(loanInfoSecondaryBean);
			SecondaryJsonList = LoanUtil.toJson(loanInfoSecondaryBeans);
			
			Logger.info("SecondaryJsonList"+i+" = %s", SecondaryJsonList);

			if(Convert.strToDouble(pRow.getString("pInFee").trim(), 0) == 0){
				SecondaryJsonList = "";
			}
			
			loanInAccount = pRow.get("pInAcctNo").toString();
			LoanInMoneymoremore = findMoneymoremoreIdByAccount(error,platformId, loanInAccount);
			
			Map<String, Object> loanInfoBean = new HashMap<String, Object>();
			loanInfoBean.put("LoanOutMoneymoremore",LoanOutMoneymoremore);
			loanInfoBean.put("LoanInMoneymoremore",LoanInMoneymoremore);
			//每一笔转账流水号生成法则：① 分批转账：还款流水号  + 分隔符 + 还款期号  + 批号 + 循环i；②一次转账：还款流水号  + 分隔符 + 循环i
			loanInfoBean.put("OrderNo", jsonArg3DesXmlPara.getString("pMerBillNo") + LoanConstants.BILLTOKEN + ("-1".equals(batchId)?"":(period + batchNo)) + i);
			loanInfoBean.put("BatchNo", jsonArg3DesXmlPara.getString("pBidNo"));
			loanInfoBean.put("ExchangeBatchNo", "");
			loanInfoBean.put("AdvanceBatchNo", "");
			loanInfoBean.put("Amount", pRow.getString("pInAmt"));
			loanInfoBean.put("FullAmount", "");
			loanInfoBean.put("TransferName", "还款");
			loanInfoBean.put("Remark", "转账给投资人");
			loanInfoBean.put("SecondaryJsonList", SecondaryJsonList);
			loanInfoBeans.add(loanInfoBean);
			
			amount = amount + Convert.strToDouble(pRow.getString("pInAmt").trim(), 0);
		}

		String LoanJsonList = LoanUtil.toJson(loanInfoBeans);
		String PlatformMoneymoremore = argMerCode;
		String TransferAction = "2"; //还款
		String Action = "1".equals(jsonArg3DesXmlPara.getString("pRepayType")) ? "1" : "2"; // 1手动转账,2自动转账
		String TransferType = "2"; // 直连
		String NeedAudit = "1"; // 无需审核，直接转账
		String RandomTimeStamp = LoanConstants.RANDOM_TIME_STAMP;

//		String Remark1 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pWebUrl"));
		String url = "1".equals(Action)?jsonArgeXtraPara.getString("pWSUrl"):jsonArg3DesXmlPara.getString("pWebUrl");
		
		String Remark1 = Codec.encodeBASE64(url);
		String Remark2 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pS2SUrl"));
		String Remark3 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pMerBillNo")
				+ LoanConstants.TOKEN + jsonArg3DesXmlPara.getString("pMemo3")
				+ LoanConstants.TOKEN + map.get("batchId")
				+ LoanConstants.TOKEN + platformId
				+ LoanConstants.TOKEN + bidNo
				+ LoanConstants.TOKEN + period
				+ LoanConstants.TOKEN + map.get("batchNo")
				);
		Logger.info("Remark.length = %s", Remark1.length() + "," + Remark2.length() + "," + Remark3.length());

		String SubmitURL = LoanConstants.LOAN_URL[4]; // 双乾转账接口
		String ReturnURL = LoanReturnURL.REPAYMENT;
		String NotifyURL = LoanNotifyURL.REPAYMENT;
		String privatekey = LoanConstants.privateKeyPKCS8;

		String dataStr = LoanJsonList + PlatformMoneymoremore + TransferAction
				+ Action + TransferType + NeedAudit + RandomTimeStamp + Remark1
				+ Remark2 + Remark3 + ReturnURL + NotifyURL;
		// 签名
		RsaHelper rsa = RsaHelper.getInstance();
		String SignInfo = rsa.signData(dataStr, privatekey);

		LoanJsonList = LoanUtil.UrlEncoder(LoanJsonList, "utf-8");

		Map<String, Object> args = new HashMap<String, Object>();
		
		args.put("LoanJsonList", LoanJsonList);
		args.put("PlatformMoneymoremore", PlatformMoneymoremore);
		args.put("TransferAction", TransferAction);
		args.put("Action", Action);
		args.put("TransferType", TransferType);
		args.put("NeedAudit", NeedAudit);
		args.put("RandomTimeStamp", RandomTimeStamp);
		args.put("Remark1", Remark1);
		args.put("Remark2", Remark2);
		args.put("Remark3", Remark3);
		args.put("ReturnURL", ReturnURL);
		args.put("NotifyURL", NotifyURL);
		args.put("SignInfo", SignInfo);
		args.put("SubmitURL", SubmitURL);

		LoanUtil.printLoggerToLoan(args);
		
		//添加用户事件
		String serialNumber = jsonArg3DesXmlPara.getString("pMerBillNo") + LoanConstants.BILLTOKEN + ("-1".equals(batchId)?"":batchId);
		String frontUrl = url;
		String backgroundUrl = jsonArg3DesXmlPara.getString("pS2SUrl");
		DealDetail.addEvent(platformMemberId, LoanConstants.REPAYMENT + 200, platformId, serialNumber, frontUrl, backgroundUrl, "", "");

		//添加交易记录
		String summary = "还款"+("-1".equals(batchId)?"":batchId);
		DealDetail.addDealDetail(platformMemberId, (int)platformId, serialNumber, LoanConstants.REPAYMENT, amount, false, summary);
						
		return args;
	}

	/**
	 * 转账超过200笔，进行分批处理
	 * @param error
	 * @param bidBillNo
	 * @param transfers
	 * @param jsonArg3DesXmlPara
	 * @param jsonArgeXtraPara
	 * @param jsonArr
	 * @param map
	 * @return
	 */
	private static Map<String, Object> batchTransfer(ErrorInfo error,String bidBillNo, JSONObject jsonArg3DesXmlPara, JSONObject jsonArgeXtraPara, JSONArray jsonArr, Map<String, Object> map) {
		String batchId = "";
		int batchNo = 0;
		JSONArray transfers = null;

		List<TransferBatches> list =  TransferBatches.queryByBidBillNo(bidBillNo);
		if(list != null){  //转账失败，再次转账
			boolean flag = true;  //是否完成转账
			for(TransferBatches tb : list){
				if(tb.status == 0){  //有未处理转账，继续处理
					batchId = tb.id + "";
					batchNo = tb.batchNo;
					jsonArg3DesXmlPara = JSONObject.fromObject(tb.transferBillNos);
					transfers = JSONArray.fromObject(jsonArg3DesXmlPara.get("pDetails"));
					TransferBatches.updateStatus(tb.id, 1, error);
					flag = false;
					break;
				}
			}
			
			if(flag){
				error.code = LoanConstants.TRANSFER_EXECUTED;  //完成转账
				return map;
			}
		}else{
			//保存分批信息，第一次操作时
			int batches = (int) Math.ceil(1.0 * jsonArr.size() / LoanConstants.TRANSFER_MAX_BILL);
			for(int i=1;i<=batches;i++){
				int begin = (i-1)*LoanConstants.TRANSFER_MAX_BILL;
				int end = i*LoanConstants.TRANSFER_MAX_BILL;
				end = end>jsonArr.size()?jsonArr.size():end;
				
				transfers = subTransfers(jsonArr,begin,end);
				jsonArg3DesXmlPara.put("pDetails", transfers);
				
				if(i==1){
					//保存并处理第一批
					TransferBatches tansferBaches = new TransferBatches(i,bidBillNo, jsonArg3DesXmlPara.toString(), 1,1);
					batchId = tansferBaches.create(error)+"";
					if(error.code < 0){
						error.code = LoanConstants.TRANSFER_ERROR;  
						return map;
					}
					batchNo = 1;
				}else{
					//保存其他批次
					TransferBatches tansferBaches = new TransferBatches(i,bidBillNo, jsonArg3DesXmlPara.toString(), 1,0);
					tansferBaches.create(error);
					if(error.code < 0){
						error.code = LoanConstants.TRANSFER_ERROR;
						return map;
					}
				}
			}
			//处理第一批
			transfers = subTransfers(jsonArr,0,LoanConstants.TRANSFER_MAX_BILL);
		}

		map.put("batchId", batchId);
		map.put("batchNo", batchNo);
		map.put("transfers", transfers);

		return map;
	}

	/**
	 * 拆分转账列表
	 * @param jsonArr
	 * @param begin
	 * @param end
	 * @return
	 */
	private static JSONArray subTransfers(JSONArray jsonArr, int begin, int end) {
		JSONArray jsonArray = new JSONArray();
		for(int i=begin; i<end; i++){
			jsonArray.add(jsonArr.getJSONObject(i));
		}
		
		return jsonArray;
	}

	/**
	 * 提现绑卡
	 * 
	 * @param error
	 * @param authSecond
	 * @param platformId
	 * @param platformMemberId
	 * @param memberName
	 * @param argMerCode
	 * @param jsonArg3DesXmlPara
	 * @param argIpsAccount
	 * @return
	 */
	public static Map<String, Object> convertBondCardNoParams(ErrorInfo error,
			long platformId, long platformMemberId, String memberName,
			String argMerCode, JSONObject jsonArg3DesXmlPara,
			JSONObject jsonArgeXtraPara) {
		error.clear();

		// 必传字段校验
		String[] needsParams = { "pIpsAcctNo", "pMerBillNo", "pTrdAmt",
				"pIpsFeeType","pMerFee", "pMemo3", "pWSUrl", "pS2SUrl" };
		String result = checkNeedsParams(needsParams, jsonArg3DesXmlPara,
				jsonArgeXtraPara);
		if (result != null) {
			error.code = -1;
			error.msg = "参数" + result + "为必传字段";

			return null;
		}

		String loanAccount = jsonArg3DesXmlPara.getString("pIpsAcctNo");
		String MoneymoremoreId = findMoneymoremoreIdByAccount(error,
				platformId, loanAccount);
		String PlatformMoneymoremore = argMerCode;
		String Action = LoanConstants.ACTION;
		String CardNo = "";
		String WithholdBeginDate = "";
		String WithholdEndDate = "";
		String SingleWithholdLimit = "";
		String TotalWithholdLimit = "";
		String RandomTimeStamp = LoanConstants.RANDOM_TIME_STAMP;

		String Remark1 = Codec.encodeBASE64(jsonArgeXtraPara.getString("pWSUrl"));
		String Remark2 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pS2SUrl"));
		String Remark3 = Codec.encodeBASE64(platformMemberId
				+ LoanConstants.TOKEN + jsonArg3DesXmlPara.getString("pMerBillNo")
				+ LoanConstants.TOKEN + jsonArg3DesXmlPara.getString("pTrdAmt")
				+ LoanConstants.TOKEN + jsonArg3DesXmlPara.getString("pIpsFeeType")
				+ LoanConstants.TOKEN + jsonArg3DesXmlPara.getString("pMemo3")
				+ LoanConstants.TOKEN + platformId
				+ LoanConstants.TOKEN + jsonArg3DesXmlPara.getString("pMerFee")
				);
		Logger.info("Remark.length = %s", Remark1.length() + "," + Remark2.length() + "," + Remark3.length());

		String SubmitURL = LoanConstants.LOAN_URL[5];
		String ReturnURL = LoanReturnURL.BOND_CRAD_NO;
		String NotifyURL = LoanNotifyURL.BOND_CRAD_NO;
		String privatekey = LoanConstants.privateKeyPKCS8;
		String publickey = LoanConstants.publicKey;

		String dataStr = MoneymoremoreId + PlatformMoneymoremore + Action
				+ CardNo + WithholdBeginDate + WithholdEndDate
				+ SingleWithholdLimit + TotalWithholdLimit + RandomTimeStamp
				+ Remark1 + Remark2 + Remark3 + ReturnURL + NotifyURL;
		// 签名
		RsaHelper rsa = RsaHelper.getInstance();
		String SignInfo = rsa.signData(dataStr, privatekey);

		if (StringUtils.isNotBlank(CardNo)) {
			CardNo = rsa.encryptData(CardNo, publickey);
		}

		Map<String, Object> args = new HashMap<String, Object>();
		args.put("MoneymoremoreId", MoneymoremoreId);
		args.put("PlatformMoneymoremore", PlatformMoneymoremore);
		args.put("Action", Action);
		args.put("CardNo", CardNo);
		args.put("WithholdBeginDate", WithholdBeginDate);
		args.put("WithholdEndDate", WithholdEndDate);
		args.put("SingleWithholdLimit", SingleWithholdLimit);
		args.put("TotalWithholdLimit", TotalWithholdLimit);
		args.put("RandomTimeStamp", RandomTimeStamp);
		args.put("Remark1", Remark1);
		args.put("Remark2", Remark2);
		args.put("Remark3", Remark3);
		args.put("ReturnURL", ReturnURL);
		args.put("NotifyURL", NotifyURL);
		args.put("SignInfo", SignInfo);
		args.put("SubmitURL", SubmitURL);

		LoanUtil.printLoggerToLoan(args);

		//添加用户事件
		String serialNumber = jsonArg3DesXmlPara.getString("pMerBillNo");
		String frontUrl = jsonArgeXtraPara.getString("pWSUrl");
		String backgroundUrl = jsonArg3DesXmlPara.getString("pS2SUrl");
		DealDetail.addEvent(platformMemberId, LoanConstants.BOND_CRAD_NO + 200, platformId, serialNumber, frontUrl, backgroundUrl, "", "");
		
		return args;
	}

	
	/**
	 * 提现,参数转化
	 * 
	 * @param error
	 * @param platformId
	 * @param platformMemberId
	 * @param memberName
	 * @param argMerCode
	 * @param jsonArg3DesXmlPara
	 * @return
	 */
	public static Map<String, Object> convertWithdrawParams(ErrorInfo error,
			long platformId, long platformMemberId, String memberName,
			String argMerCode, JSONObject jsonArg3DesXmlPara,
			JSONObject jsonArgeXtraPara, String CardNo, String MoneymoremoreId) {

		error.clear();

		// 必传字段校验
		String[] needsParams = {"pMerBillNo", "pTrdAmt","pIpsFeeType","pMerFee", "pMemo3", "pWSUrl", "pS2SUrl" };
		String result = checkNeedsParams(needsParams, jsonArg3DesXmlPara,jsonArgeXtraPara);
		if (result != null) {
			error.code = -1;
			error.msg = "参数" + result + "为必传字段";

			return null;
		}

		String WithdrawMoneymoremore = MoneymoremoreId;
		if (StringUtils.isBlank(MoneymoremoreId) || !MoneymoremoreId.trim().startsWith("m")) {
			/* 必传参数校验 */
			if (!jsonArg3DesXmlPara.containsKey("pIpsAcctNo")) {
				Logger.info("======校验参数时，%s", "参数pIpsAcctNo为必传字段");
				error.code = -1;
				error.msg = "参数pIpsAcctNo为必传字段";
				return null;
			}
			String pIpsAcctNo = jsonArg3DesXmlPara.getString("pIpsAcctNo");
			WithdrawMoneymoremore = findMoneymoremoreIdByAccount(error,
					platformId, pIpsAcctNo);
		}

		String PlatformMoneymoremore = argMerCode;
		String OrderNo = jsonArg3DesXmlPara.getString("pMerBillNo");
		String Amount = jsonArg3DesXmlPara.getString("pTrdAmt");
		String FeeMax = ""; // 不传
		String[] feePercentAndFeeRate = getFeePercentAndFeeRate(Amount, jsonArg3DesXmlPara.getString("pMerFee"));
		String FeePercent = feePercentAndFeeRate[0]; 
		String FeeRate = feePercentAndFeeRate[1];
		// String CardNo = CardNo;
		String CardType = "";
		String BankCode = "";
		String BranchBankName = "";
		String Province = "";
		String City = "";
		String RandomTimeStamp = LoanConstants.RANDOM_TIME_STAMP;

//		String Remark1 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pWebUrl"));
		String Remark1 = Codec.encodeBASE64(jsonArgeXtraPara.getString("pWSUrl"));
		String Remark2 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pS2SUrl"));
		String Remark3 = Codec.encodeBASE64(platformMemberId 
				+ LoanConstants.TOKEN + jsonArg3DesXmlPara.getString("pMemo3")
				);
		Logger.info("Remark.length = %s", Remark1.length() + "," + Remark2.length() + "," + Remark3.length());

		String SubmitURL = LoanConstants.LOAN_URL[6]; // 提现接口
		String ReturnURL = LoanReturnURL.WITHDRAWAL;
		String NotifyURL = LoanNotifyURL.WITHDRAWAL;
		String privatekey = LoanConstants.privateKeyPKCS8;
		String publickey = LoanConstants.publicKey;

		String dataStr = WithdrawMoneymoremore + PlatformMoneymoremore
				+ OrderNo + Amount + FeePercent + FeeMax + FeeRate + CardNo
				+ CardType + BankCode + BranchBankName + Province + City
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ReturnURL
				+ NotifyURL;
		// 签名
		RsaHelper rsa = RsaHelper.getInstance();
		String SignInfo = rsa.signData(dataStr, privatekey);

		if (StringUtils.isNotBlank(CardNo)) {
			CardNo = rsa.encryptData(CardNo, publickey);
		}

		Map<String, Object> args = new HashMap<String, Object>();
		args.put("WithdrawMoneymoremore", WithdrawMoneymoremore);
		args.put("PlatformMoneymoremore", PlatformMoneymoremore);
		args.put("OrderNo", OrderNo);
		args.put("FeePercent", FeePercent);
		args.put("FeeMax", FeeMax);
		args.put("FeeRate", FeeRate);
		args.put("Amount", Amount);
		args.put("CardNo", CardNo);
		args.put("CardType", CardType);
		args.put("BankCode", BankCode);
		args.put("BranchBankName", BranchBankName);
		args.put("Province", Province);
		args.put("City", City);
		args.put("RandomTimeStamp", RandomTimeStamp);
		args.put("Remark1", Remark1);
		args.put("Remark2", Remark2);
		args.put("Remark3", Remark3);
		args.put("ReturnURL", ReturnURL);
		args.put("NotifyURL", NotifyURL);
		args.put("SignInfo", SignInfo);
		args.put("SubmitURL", SubmitURL);

		LoanUtil.printLoggerToLoan(args);

		//添加用户事件
		String serialNumber = jsonArg3DesXmlPara.getString("pMerBillNo");
		String frontUrl = jsonArgeXtraPara.getString("pWSUrl");
		String backgroundUrl = jsonArg3DesXmlPara.getString("pS2SUrl");
		DealDetail.addEvent(platformMemberId, LoanConstants.WITHDRAWAL + 200, platformId, serialNumber, frontUrl, backgroundUrl, "", "");
		
		//添加交易记录
		double amount = Convert.strToDouble(Amount.trim(), 0);
		String summary = "提现";
		DealDetail.addDealDetail(platformMemberId, (int)platformId, serialNumber, LoanConstants.WITHDRAWAL, amount, false, summary);
						
		return args;
	}

	/**
	 * 余额查询,参数转化
	 * 
	 * @param error
	 * @param platformMemberId
	 * @param platformId
	 * @param argMerCode
	 * @param argIpsAccount
	 * @return
	 */
	public static Map<String, Object> convertAccountBalanceParams(
			ErrorInfo error, long platformMemberId, long platformId,
			String argMerCode, String argIpsAccount) {
		error.clear();

		String PlatformId = findMoneymoremoreIdByAccount(error, platformId,
				argIpsAccount);

		String PlatformMoneymoremore = argMerCode;

		String PlatformType = "";
		if (argIpsAccount.startsWith("p")) {
			PlatformType = "2";
		}

		String privatekey = LoanConstants.privateKeyPKCS8;

		String dataStr = PlatformId + PlatformType + PlatformMoneymoremore;

		// 签名
		RsaHelper rsa = RsaHelper.getInstance();
		String SignInfo = rsa.signData(dataStr, privatekey);

		Map<String, Object> args = new HashMap<String, Object>();
		args.put("PlatformId", PlatformId);
		args.put("PlatformMoneymoremore", PlatformMoneymoremore);
		args.put("PlatformType", PlatformType);
		args.put("SignInfo", SignInfo);

		LoanUtil.printLoggerToLoan(args);

		return args;
	}

	/**
	 * 交易查询，ws请求
	 * 
	 * @param error
	 * @param authSecond
	 * @param platformId
	 * @param platformMemberId
	 * @param memberName
	 * @param argMerCode
	 * @param jsonArg3DesXmlPara
	 * @param argIpsAccount
	 * @return
	 */
	public static Map<String, Object> convertOrderQueryParams(ErrorInfo error,
			String authSecond, long platformId, long platformMemberId,
			String memberName, String argMerCode,
			JSONObject jsonArg3DesXmlPara,JSONObject jsonArgeXtraPara, String argIpsAccount) {
		error.clear();
		
		Map<String, Object> resultToP2P = new HashMap<String, Object>();
		
		// 必传字段校验
		String[] needsParams = {"pMerBillNo","merBillNos","pTradeType"};
		String checkResult = checkNeedsParams(needsParams, jsonArg3DesXmlPara,jsonArgeXtraPara);
		if (checkResult != null) {
			error.code = -1;
			error.msg = "参数" + checkResult + "为必传字段";

			return null;
		}
		
		String pTradeType = jsonArg3DesXmlPara.getString("pTradeType");
		
		String Action = "";  //默认：转账
		if(RepairOperation.DO_DP_TRADE.equals(pTradeType.trim())){
			Action = "1";  //充值
		}
		if(RepairOperation.DO_DW_TRADE.equals(pTradeType.trim())){
			Action = "2";  //提现
		}
		
		String OrderNo = jsonArg3DesXmlPara.getString("pMerBillNo").trim();
		String LoanNo = "";
		/* 放款补单 */
		if(RepairOperation.TRANSFER_ONE.equals(pTradeType)){
			String[] merBillNoArray = jsonArgeXtraPara.getString("merBillNos").split(",");
			OrderNo = merBillNoArray[0];  //批量放款，要么都成功，要么都失败，所以只需查询其中一个的账单即可
		}
		/* 还款补单*/
		if(RepairOperation.REPAYMENT_NEW_TRADE.equals(pTradeType)){
			OrderNo = OrderNo + LoanConstants.BILLTOKEN + "0";  //批量还款，要么都成功，要么都失败，所以只需查询其中一个的账单即可
		}
		
		String[] resultArray = loanOrderQuery(Action, LoanNo, OrderNo);
		
		String resultStr = resultArray[1];
		
		String returncode = resultArray[0];
		if(!returncode.startsWith("2")){  //ws请求异常
			Logger.info("对账时：%s。 httpStatus = %s", "ws请求异常",returncode);
			
			return resultToP2P;
		}
		
		String pErrCode = "";
		String pErrMsg = "";
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("pTradeStatue", "4");  //默认值
		if (StringUtils.isNotBlank(resultStr)&& (resultStr.startsWith("[") || resultStr.startsWith("{"))) {
			//对账成功
			pErrCode = "MG00000F";
			pErrMsg = "成功";

			Map<String, String> resultFromLoan = getLoanMap(resultStr);
			
			/* p2p 对账接收状态：1#成功、2#失败、3#处理中、4#未查询到交易" */
			if(StringUtils.isBlank(Action.trim())){  //"":转账
				//操作状态：用于审核接口
				String actState = resultFromLoan.get("ActState").toString(); // 0.未操作,1.已通过,2.已退回,3.自动通过
				//转账状态：用于转账接口
				String transferState = resultFromLoan.get("TransferState").toString(); // 0.未转账,1.已转账
				
				 /*发标、投标，转账接口*/
				if(RepairOperation.REGISTER_SUBJECT.equals(pTradeType.trim()) || RepairOperation.REGISTER_CREDITOR.equals(pTradeType.trim())){ 
					result.put("pTradeStatue", "0".equals(transferState.trim())?"2":"1");
					//返回转账流水号
					result.put("pIpsBillNo",resultFromLoan.get("LoanNo").toString().trim());
					result.put("isPost","Y");
				}
				
				/*放款，审核接口*/
				if(RepairOperation.TRANSFER_ONE.equals(pTradeType)){
					result.put("pTradeStatue", "0".equals(actState.trim())?"2":"1");
				}
				
				/* 还款补单、债权转让，转账+审核接口  */
				if(RepairOperation.REPAYMENT_NEW_TRADE.equals(pTradeType) || RepairOperation.REGISTER_CRETANSFER.equals(pTradeType)){
					if("0".equals(transferState.trim())){  //未转账。失败
						result.put("pTradeStatue", "2");  
					}else if("1".equals(transferState.trim()) && "0".equals(actState.trim())){ //已转账，未审核。处理中
						result.put("pTradeStatue", "3");
					}else if("1".equals(transferState.trim()) &&  !"0".equals(actState.trim())){  //已转账，已审核。成功
						result.put("pTradeStatue", "1");
					}else{  //失败
						result.put("pTradeStatue", "2");
					}
				}
				
			}else if("1".equals(Action.trim())){  //"1":充值
				String RechargeState = resultFromLoan.get("RechargeState").toString(); // 0.未充值,1.成功,2.失败
				result.put("pTradeStatue", "0".equals(RechargeState.trim())?"2":RechargeState.trim());
				
			}else{  //"2":提现
				String WithdrawsState = resultFromLoan.get("WithdrawsState").toString(); // 0.已提交,1.成功,2.已退回
				result.put("pTradeStatue", "0".equals(WithdrawsState.trim())?"1":WithdrawsState.trim());
				result.put("serviceFee", resultFromLoan.get("FeeWithdraws").toString());
			}
		}else{
			//对账失败，或未查询到交易
			pErrCode = "MG00001F";
			pErrMsg = StringUtils.isNotBlank(resultStr)?resultStr:"未查询到交易";
			
			result.put("pTradeStatue", "4");
		}
		
		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		Logger.info("p3DesXmlPara", p3DesXmlPara);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);
		resultToP2P.put("p3DesXmlPara", p3DesXmlPara);
		
		resultToP2P.put("pErrCode", pErrCode);
		resultToP2P.put("pErrMsg", pErrMsg);
		
		return resultToP2P;
	}
	
	/**
	 * 对账接口(ws),供中间件使用
	 *  
	 * @param Action  查询类型
	 * @param LoanNo  乾多多流水号
	 * @param OrderNo  p2p流水号
	 * @return
	 */
	public static String[] loanOrderQuery(String Action, String LoanNo,
			String OrderNo) {

		String PlatformMoneymoremore = LoanConstants.argMerCode;

		if (Action == null) {
			Action = "";
		}

		if (LoanNo == null) {
			LoanNo = "";
		}

		if (OrderNo == null) {
			OrderNo = "";
		}

		String BatchNo = "";
		String BeginTime = "";
		String EndTime = "";

		String SubmitURL = LoanConstants.LOAN_URL[10];
		String privatekey = LoanConstants.privateKeyPKCS8;

		String dataStr = PlatformMoneymoremore + Action + LoanNo + OrderNo
				+ BatchNo + BeginTime + EndTime;
		// 签名
		RsaHelper rsa = RsaHelper.getInstance();
		String SignInfo = rsa.signData(dataStr, privatekey);

		Map<String, Object> req = new TreeMap<String, Object>();
		req.put("PlatformMoneymoremore", PlatformMoneymoremore);
		req.put("Action", Action);
		req.put("LoanNo", LoanNo);
		req.put("OrderNo", OrderNo);
		req.put("BatchNo", BatchNo);
		req.put("BeginTime", BeginTime);
		req.put("EndTime", EndTime);
		req.put("SignInfo", SignInfo);

		return LoanUtil.doPostQueryCmd(SubmitURL, req);
	}

	/**
	 * 根据p2p平台id和该平台用户id查询乾多多账户唯一标识
	 * 
	 * @param error
	 * @param platformId p2p平台id
	 * @param loanAccount 乾多多账户
	 * @return
	 */
	private static String findMoneymoremoreIdByAccount(ErrorInfo error,long platformId, String loanAccount) {
		if (loanAccount == null) {
			Logger.info("获取MoneymoremoreId时：%s", "乾多多帐号不能为空");
			error.code = -1;
			error.msg = "乾多多帐号不能为空";

			return "";
		}

		loanAccount = loanAccount.trim();

		t_member_of_platforms platformMember = t_member_of_platforms.find(
				"platform_member_account = ? and platform_id = ?", loanAccount, platformId).first();

		if (platformMember == null) {
			Logger.info("获取MoneymoremoreId时：%s", "该乾多多帐号没有绑定到当前平台");
			error.code = -2;
			error.msg = "该乾多多帐号没有绑定到当前平台";

			return "";
		}

		String platforMmemberAccountId = platformMember.platform_member_account_id;

		if (StringUtils.isBlank(platforMmemberAccountId)) {
			Logger.info("获取MoneymoremoreId时：%s","乾多多标识不存在");
			error.code = -3;
			error.msg = "乾多多标识不存在";

			return "";
		}

		return platforMmemberAccountId;
	}

	/**
	 * 必填参数校验
	 * 
	 * @param jsonArg3DesXmlPara
	 * @param jsonLoanXML
	 * @param memberName
	 * @param platformMemberId
	 * @return 未填字段，返回null表示校验通过
	 */
	private static String checkNeedsParams(String[] needsParams,
			JSONObject jsonArg3DesXmlPara, JSONObject jsonLoanXML,
			String... memberName) {

		for (String param : needsParams) {
			if (!(jsonArg3DesXmlPara.containsKey(param) || jsonLoanXML
					.containsKey(param))) {

				return param;
			}
		}

		if (memberName.length > 0 && StringUtils.isBlank(memberName[0])) {
			return "memberName";
		}
		return null;
	}

	/**
	 * 计算上浮费率和平台垫付百分比
	 * 
	 * @param pIpsFeeType
	 * @return
	 */
	private static String[] getFeePercentAndFeeRate(String Amount, String pMerFee) {
		String[] result = new String[2];
		double signRate = Convert.strToDouble(LoanConstants.signRate.trim(), 0);
		DecimalFormat df = new DecimalFormat("#0.0000");

		//平台收取用户手续费（不足1元按1元算）
		double platfromFee = Convert.strToDouble(pMerFee.trim(), 0);
		platfromFee = platfromFee<1.00?1.00:platfromFee;

		// 双乾收取的提现手续费（不足1元按1元算）
		double loanFee = Convert.strToDouble(Amount.trim(), 0) * signRate;
		loanFee = loanFee < 1.00 ? 1.00 : loanFee;
		
		if(platfromFee <= loanFee){  //平台需要承担一部分手续费
			double feePercent = (loanFee - platfromFee) * 100 / loanFee;
			result[0] = String.valueOf(feePercent);  //平台承担手续费百分比
			result[1] = "";  //不设上浮费率
		}else{
			double feeRate = (platfromFee - loanFee) / Convert.strToDouble(Amount.trim(), 1) + signRate;
			result[0] = "";  //平台有分润，平台承担手续费百分比为0
			result[1] = feeRate == 0 ? "" : String.valueOf(df.format(feeRate));  //上浮费率
		}
		
		return result;
	}


	/**
	 * 转账-用户转商户：
	 * @param error
	 * @param platformId
	 * @param platformMemberId
	 * @param memberName
	 * @param argMerCode
	 * @param jsonArg3DesXmlPara
	 * @param jsonArgeXtraPara
	 * @return
	 */
	public static Map<String, Object> convertTransferUserToMerParams(ErrorInfo error,
			long platformId, long platformMemberId, String memberName,
			String argMerCode, JSONObject jsonArg3DesXmlPara,
			JSONObject jsonArgeXtraPara) {

		error.clear();

		// 必传字段校验
		String[] needsParams = { "pMerBillNo", "TransAmt", "UsrCustId", "pWebUrl","pS2SUrl" };
		String result = checkNeedsParams(needsParams, jsonArg3DesXmlPara,
				jsonArgeXtraPara);
		if (result != null) {
			error.code = -1;
			error.msg = "参数" + result + "为必传字段";

			return null;
		}

		String loanAccount = jsonArgeXtraPara.getString("UsrCustId");
		String LoanOutMoneymoremore = findMoneymoremoreIdByAccount(error,platformId, loanAccount);

		List<Map<String, Object>> listmlib = new ArrayList<Map<String, Object>>();

		Map<String, Object> loanInfoBean = new HashMap<String, Object>();
		loanInfoBean.put("LoanOutMoneymoremore", LoanOutMoneymoremore);
		loanInfoBean.put("LoanInMoneymoremore", argMerCode);
		loanInfoBean.put("OrderNo", jsonArg3DesXmlPara.getString("pMerBillNo"));
		loanInfoBean.put("BatchNo", "b"+jsonArg3DesXmlPara.getString("pMerBillNo"));
		loanInfoBean.put("ExchangeBatchNo", "");
		loanInfoBean.put("AdvanceBatchNo", "");
		loanInfoBean.put("Amount", jsonArgeXtraPara.getString("TransAmt"));
		loanInfoBean.put("FullAmount", "");
		loanInfoBean.put("TransferName", "其他费用");
		loanInfoBean.put("Remark", "其他费用");
		loanInfoBean.put("SecondaryJsonList", "");
		listmlib.add(loanInfoBean);

		String LoanJsonList = LoanUtil.toJson(listmlib);

		String PlatformMoneymoremore = argMerCode;
		String TransferAction = "3"; // 其他
		String Action = "1"; // 手动转账
		String TransferType = "2"; // 直连
		String NeedAudit = "1"; // 无需审核，直接转账
		String RandomTimeStamp = LoanConstants.RANDOM_TIME_STAMP;

		String Remark1 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pWebUrl"));
		String Remark2 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pS2SUrl"));
		String Remark3 = "";
		Logger.info("Remark.length = %s", Remark1.length() + "," + Remark2.length() + "," + Remark3.length());

		String SubmitURL = LoanConstants.LOAN_URL[4]; // 请求双乾转账接口
		String ReturnURL = LoanReturnURL.TRANSFER_USER_TO_MER;
		String NotifyURL = LoanNotifyURL.TRANSFER_USER_TO_MER;
		String privatekey = LoanConstants.privateKeyPKCS8;

		String dataStr = LoanJsonList + PlatformMoneymoremore + TransferAction
				+ Action + TransferType + NeedAudit + RandomTimeStamp + Remark1
				+ Remark2 + Remark3 + ReturnURL + NotifyURL;
		// 签名
		RsaHelper rsa = RsaHelper.getInstance();
		String SignInfo = rsa.signData(dataStr, privatekey);

		LoanJsonList = LoanUtil.UrlEncoder(LoanJsonList, "utf-8");

		Map<String, Object> args = new HashMap<String, Object>();
		args.put("LoanJsonList", LoanJsonList);
		args.put("PlatformMoneymoremore", PlatformMoneymoremore);
		args.put("TransferAction", TransferAction);
		args.put("Action", Action);
		args.put("TransferType", TransferType);
		args.put("NeedAudit", NeedAudit);
		args.put("RandomTimeStamp", RandomTimeStamp);
		args.put("Remark1", Remark1);
		args.put("Remark2", Remark2);
		args.put("Remark3", Remark3);
		args.put("ReturnURL", ReturnURL);
		args.put("NotifyURL", NotifyURL);
		args.put("SignInfo", SignInfo);
		args.put("SubmitURL", SubmitURL);

		LoanUtil.printLoggerToLoan(args);

		//添加用户事件
				String serialNumber = jsonArg3DesXmlPara.getString("pMerBillNo");
				String frontUrl = jsonArg3DesXmlPara.getString("pWebUrl");
				String backgroundUrl = jsonArg3DesXmlPara.getString("pS2SUrl");
				DealDetail.addEvent(platformMemberId, LoanConstants.CREATE_ACCOUNT + 200, platformId, serialNumber, frontUrl, backgroundUrl, "", "");
		
		//添加交易记录
		double amount = Convert.strToDouble(jsonArgeXtraPara.getString("TransAmt").trim(), 0);
		String summary = "杂费收取";
		DealDetail.addDealDetail(platformMemberId, (int)platformId, serialNumber, LoanConstants.TRANSFER_USER_TO_MER, amount, false, summary);
		
		return args;
	}
	
	/**
	 * 商户转用户
	 * 
	 * @param error
	 * @param platformId
	 * @param platformMemberId
	 * @param memberName
	 * @param argMerCode
	 * @param jsonArg3DesXmlPara
	 * @return
	 */
	public static Map<String, Object> convertTransferMerToUserParams(ErrorInfo error,
			long platformId, long platformMemberId, String memberName,
			String argMerCode, JSONObject jsonArg3DesXmlPara,
			JSONObject jsonArgeXtraPara) {

		error.clear();

		// 必传字段校验
		String[] needsParams = {"pMerBillNo","pDetails","pWebUrl", "pS2SUrl" };
		String result = checkNeedsParams(needsParams, jsonArg3DesXmlPara,
				jsonArgeXtraPara);
		if (result != null) {
			error.code = -1;
			error.msg = "参数" + result + "为必传字段";

			return null;
		}

		JSONArray jsonArr = null;

		Object pDetails = jsonArgeXtraPara.get("pDetails");
		
		if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
			JSONObject pDetail = (JSONObject)pDetails; 
			JSONObject pRow = pDetail.getJSONObject("pRow"); 
	
			jsonArr = new JSONArray(); 
			jsonArr.add(pRow); 
		} else {
			jsonArr = jsonArgeXtraPara.getJSONArray("pDetails");
		} 
		
		List<Map<String, Object>> loanInfoBeans = new ArrayList<Map<String, Object>>();
		JSONObject pRow = null;

		String loanInAccount = "";
		String LoanInMoneymoremore = "";
		
		double amount = 0;
		for (int i = 0; i < jsonArr.size(); i++) {
			pRow = jsonArr.getJSONObject(i);
			
			loanInAccount = pRow.getString("inCustId").trim();
			LoanInMoneymoremore = findMoneymoremoreIdByAccount(error,platformId, loanInAccount);

			Map<String, Object> loanInfoBean = new HashMap<String, Object>();
			loanInfoBean.put("LoanOutMoneymoremore",argMerCode);
			loanInfoBean.put("LoanInMoneymoremore",LoanInMoneymoremore);
			loanInfoBean.put("OrderNo", jsonArg3DesXmlPara.getString("pMerBillNo") + LoanConstants.BILLTOKEN + i);
			loanInfoBean.put("BatchNo", "b" + jsonArg3DesXmlPara.getString("pMerBillNo"));
			loanInfoBean.put("ExchangeBatchNo", "");
			loanInfoBean.put("AdvanceBatchNo", "");
			loanInfoBean.put("Amount", pRow.getString("transAmt"));
			loanInfoBean.put("FullAmount", "");
			loanInfoBean.put("TransferName", "平台支出");
			loanInfoBean.put("Remark", "平台支出");
			loanInfoBean.put("SecondaryJsonList", "");
			loanInfoBeans.add(loanInfoBean);
			
			amount = amount + Convert.strToDouble(pRow.getString("transAmt").trim(), 0);
		}

		String LoanJsonList = LoanUtil.toJson(loanInfoBeans);
		String PlatformMoneymoremore = argMerCode;
		String TransferAction = "3"; //其他
		String Action = "1"; // 1手动转账
		String TransferType = "2"; // 直连
		String NeedAudit = "1"; // 无需审核，直接转账
		String RandomTimeStamp = LoanConstants.RANDOM_TIME_STAMP;

		String Remark1 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pWebUrl"));
		String Remark2 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pS2SUrl"));
		String Remark3 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pMerBillNo")
				+ LoanConstants.TOKEN + jsonArg3DesXmlPara.getString("pMemo1"));
		Logger.info("Remark.length = %s", Remark1.length() + "," + Remark2.length() + "," + Remark3.length());

		String SubmitURL = LoanConstants.LOAN_URL[4]; // 双乾转账接口
		String ReturnURL = LoanReturnURL.TRANSFER_MER_TO_USER;
		String NotifyURL = LoanNotifyURL.TRANSFER_MER_TO_USER;
		String privatekey = LoanConstants.privateKeyPKCS8;

		String dataStr = LoanJsonList + PlatformMoneymoremore + TransferAction
				+ Action + TransferType + NeedAudit + RandomTimeStamp + Remark1
				+ Remark2 + Remark3 + ReturnURL + NotifyURL;
		// 签名
		RsaHelper rsa = RsaHelper.getInstance();
		String SignInfo = rsa.signData(dataStr, privatekey);

		LoanJsonList = LoanUtil.UrlEncoder(LoanJsonList, "utf-8");

		Map<String, Object> args = new HashMap<String, Object>();
		args.put("LoanJsonList", LoanJsonList);
		args.put("PlatformMoneymoremore", PlatformMoneymoremore);
		args.put("TransferAction", TransferAction);
		args.put("Action", Action);
		args.put("TransferType", TransferType);
		args.put("NeedAudit", NeedAudit);
		args.put("RandomTimeStamp", RandomTimeStamp);
		args.put("Remark1", Remark1);
		args.put("Remark2", Remark2);
		args.put("Remark3", Remark3);
		args.put("ReturnURL", ReturnURL);
		args.put("NotifyURL", NotifyURL);
		args.put("SignInfo", SignInfo);
		args.put("SubmitURL", SubmitURL);

		LoanUtil.printLoggerToLoan(args);

		//添加用户事件
		String serialNumber = jsonArg3DesXmlPara.getString("pMerBillNo");
		String frontUrl = jsonArg3DesXmlPara.getString("pWebUrl");
		String backgroundUrl = jsonArg3DesXmlPara.getString("pS2SUrl");
		DealDetail.addEvent(platformMemberId, LoanConstants.CREATE_ACCOUNT + 200, platformId, serialNumber, frontUrl, backgroundUrl, "", "");
		
		//添加交易记录
		String summary = "商户转用户";
		DealDetail.addDealDetail(platformMemberId, (int)platformId, serialNumber, LoanConstants.TRANSFER_MER_TO_USER, amount, false, summary);
						
		
		return args;
	}


	/**
	 * 解冻投资金额。（审核回退）
	 * @param error
	 * @param platformId
	 * @param platformMemberId
	 * @param memberName
	 * @param argMerCode
	 * @param jsonArg3DesXmlPara
	 * @param jsonArgeXtraPara
	 * @return
	 */
	public static Map<String, Object> convertUnfreezeInvestAmountParams(
			ErrorInfo error, long platformId, long platformMemberId,
			String memberName, String argMerCode,
			JSONObject jsonArg3DesXmlPara, JSONObject jsonArgeXtraPara) {
		error.clear();

		// 必传字段校验
		String[] needsParams = {"pMerBillNo", "pP2PBillNo","pWebUrl", "pS2SUrl" };
		String result = checkNeedsParams(needsParams, jsonArg3DesXmlPara,jsonArgeXtraPara);
		if (result != null) {
			error.code = -1;
			error.msg = "参数" + result + "为必传字段";

			return null;
		}

		String bidBillNo = jsonArg3DesXmlPara.getString("pP2PBillNo");
		
		// 对账（ws）
		String resultStr = loanOrderQuery(LoanConstants.queryTransfer, bidBillNo,null)[1];
		if(StringUtils.isNotBlank(resultStr)&&(resultStr.startsWith("[") || resultStr.startsWith("{"))){
			String actState = getLoanMap(resultStr).get("ActState").toString(); // 0.未操作1.已通过2.已退回3.自动通过
			if (!"0".equals(actState)) {
				// 解冻投资金额已操作
				error.code = -100;
				error.msg = "解冻保证金已操作";
				
				return null;
			}
		}


		String LoanNoList = bidBillNo;
		String PlatformMoneymoremore = argMerCode;
		String AuditType = "2"; // 退回
		String RandomTimeStamp = LoanConstants.RANDOM_TIME_STAMP;

		String Remark1 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pWebUrl"));
		String Remark2 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pS2SUrl"));
		String Remark3 = Codec.encodeBASE64(jsonArg3DesXmlPara.getString("pMerBillNo"));
		Logger.info("Remark.length = %s", Remark1.length() + "," + Remark2.length() + "," + Remark3.length());

		String SubmitURL = LoanConstants.LOAN_URL[8]; // 审核退回
		String ReturnURL = LoanReturnURL.UNFREEZE_INVEST_AMOUNT;
		String NotifyURL = LoanNotifyURL.UNFREEZE_INVEST_AMOUNT;
		String privatekey = LoanConstants.privateKeyPKCS8;

		String dataStr = LoanNoList + PlatformMoneymoremore + AuditType
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ReturnURL
				+ NotifyURL;
		// 签名
		RsaHelper rsa = RsaHelper.getInstance();
		String SignInfo = rsa.signData(dataStr, privatekey);

		Map<String, Object> args = new HashMap<String, Object>();
		args.put("LoanNoList", LoanNoList);
		args.put("PlatformMoneymoremore", PlatformMoneymoremore);
		args.put("AuditType", AuditType);
		args.put("RandomTimeStamp", RandomTimeStamp);
		args.put("Remark1", Remark1);
		args.put("Remark2", Remark2);
		args.put("Remark3", Remark3);
		args.put("ReturnURL", ReturnURL);
		args.put("NotifyURL", NotifyURL);
		args.put("SignInfo", SignInfo);
		args.put("SubmitURL", SubmitURL);

		LoanUtil.printLoggerToLoan(args);

		//添加用户事件
		String serialNumber = jsonArg3DesXmlPara.getString("pMerBillNo");
		String frontUrl = jsonArg3DesXmlPara.getString("pWebUrl");
		String backgroundUrl = jsonArg3DesXmlPara.getString("pS2SUrl");
		DealDetail.addEvent(platformMemberId, LoanConstants.CREATE_ACCOUNT + 200, platformId, serialNumber, frontUrl, backgroundUrl, "", "");
		
		return args;
	}
	
	/**
	 * 将json转账信息转化成对象
	 * @param loanJsonList
	 * @return
	 */
	public static List<Map> getLoanList(String loanJsonList) {
		List<Map> list = null;

		if(loanJsonList.startsWith("[")){
			list = LoanUtil.toList(loanJsonList);
		}
		if(loanJsonList.startsWith("{")){
			list = new ArrayList<Map>();
			list.add(LoanUtil.toMap(loanJsonList));
		}
		return list;
	}
	
	/**
	 * 将响应的json信息转化成对象
	 * @param loanJsonList
	 * @return
	 */
	public static Map<String, String> getLoanMap(String response) {
		Map<String, String> resultFromLoan = null;
		
		if(response.startsWith("[")){
			resultFromLoan = LoanUtil.toList(response).get(0);
		}
		if(response.startsWith("{")){
			resultFromLoan = LoanUtil.toMap(response);
		}
		return resultFromLoan;
	}
}
