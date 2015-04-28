package services;

import java.util.Date;
import java.util.HashMap;
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
import business.Member;

import com.shove.security.Encrypt;

import utils.Converter;
import utils.CryptTool;
import utils.DateUtil;
import utils.EmailUtil;
import utils.ErrorInfo;
import utils.PaymentUtil;
import constants.Constants;
import constants.IPSConstants;
import controllers.Application;

public class IPSTest {

	public static Map<String, String> entrance(int type, int platformId, long platformMemberId, String platformmembername, 
			String argMerCode, String arg3DesXmlPara, String argSign) {
		if(StringUtils.isBlank(arg3DesXmlPara)){
			Logger.info("arg3DesXmlPara出现空值");
			return null;
		}
		JSONObject json = (JSONObject)Converter.xmlToObj(arg3DesXmlPara);
		
		 Map<String, String> argsMap = new HashMap<String, String>();
		
		switch (type) {
		case IPSConstants.CREATE_ACCOUNT:
			argsMap = createAccount(argMerCode, json, argSign);
			break;
		case IPSConstants.REGISTER_SUBJECT:
			argsMap = registerSubject(argMerCode, json, argSign);
			break;
		case IPSConstants.REGISTER_CREDITOR:
			argsMap = registerCreditor(argMerCode, json, argSign);
			break;
		case IPSConstants.REGISTER_GUARANTOR:
			argsMap = registerGuarantor(argMerCode, json, argSign);
			break;
		case IPSConstants.REGISTER_CRETANSFER:
			argsMap = registerCretansfer(argMerCode, json, argSign);
			break;
		case IPSConstants.AUTO_SIGING:
			argsMap = autoSign(argMerCode, json, argSign);
			break;
		case IPSConstants.REPAYMENT_SIGNING:
			argsMap = repaymentSign(argMerCode, json, argSign);
			break;
		case IPSConstants.RECHARGE:
			argsMap = recharge(argMerCode, json, argSign);
			break;
		case IPSConstants.TRANSFER:
			argsMap = transfer(argMerCode, json, argSign);
			break;
		case IPSConstants.REPAYMENT:
			argsMap = repayment(argMerCode, json, argSign);
			break;
		case IPSConstants.UNFREEZE:
			argsMap = guaranteeUnfreeze(argMerCode, json, argSign);
			break;
		case IPSConstants.DEDUCT:
			argsMap = coDp(argMerCode, json, argSign);
			break;
		case IPSConstants.WITHDRAWAL:
			argsMap = withdrawal(argMerCode, json, argSign);
			break;
		default:
			break;
		}
		
        return argsMap;
	}
	
	/**
	 * 开户
	 * @param json
	 */
	public static boolean createAccount(int platformId, long platformMemberId, String platformmembername, JSONObject json) {
		Member.updateStatus(json.getString("pIdentNo"));
		Member.updateAccount(platformId, platformMemberId, json.getString("pIpsAcctNo"));
		
		return true;
	}
	
	/**
	 * 开户
	 * @param argMerCode
	 * @param json
	 * @param argSign
	 * @return
	 */
	public static Map<String, String> createAccount(String argMerCode, JSONObject json, String argSign) {
		String pMerCode = argMerCode;
		String pErrCode = "0000";
		String pErrMsg = "开户成功";
		
		JSONObject jsonObj = new JSONObject();
		
		jsonObj.put("pStatus", "0000");
		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonObj.put("pSmDate", json.getString("pSmDate"));
		jsonObj.put("pEmail", json.getString("pEmail"));
		jsonObj.put("pIdentNo", json.getString("pIdentNo"));
		jsonObj.put("pRealName", json.getString("pEmail"));
		jsonObj.put("pMobileNo", json.getString("pEmail"));
		
		jsonObj.put("pBankName", "");
		jsonObj.put("pBkAccName", "");
		jsonObj.put("pBkAccNo", "");
		jsonObj.put("pCardStatus", "Y");
		jsonObj.put("pPhStatus", "Y");
		jsonObj.put("pIpsAcctNo", CryptTool.getRandom(10, false, true));
		jsonObj.put("pIpsAcctDate", DateUtil.simple(new Date()));
		
		jsonObj.put("pMemo1", json.getString("pMemo1"));
		jsonObj.put("pMemo2", json.getString("pMemo2"));
		jsonObj.put("pMemo3", json.getString("pMemo3"));
		
		System.out.println("开户："+jsonObj.toString());
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null), Constants.ENCRYPTION_KEY);
		
		String pSign = Encrypt.MD5(pMerCode+pErrCode+pErrMsg+p3DesXmlPara+Constants.ENCRYPTION_KEY);
	
		Map<String, String> args = new HashMap<String, String>();
		
		args.put("url", json.getString("pWebUrl"));
		args.put("pMerCode", pMerCode);
		args.put("pErrCode", pErrCode);
		args.put("pErrMsg", pErrMsg);
		args.put("p3DesXmlPara", p3DesXmlPara);
		args.put("pSign", pSign);
		
		return args;
	}
	
	/**
	 * 注册表
	 * @param argMerCode
	 * @param json
	 * @param argSign
	 * @return
	 */
	public static Map<String, String> registerSubject(String argMerCode, JSONObject json, String argSign) {
		String pMerCode = argMerCode;
		String pErrCode = "0000";
		String pErrMsg = "MG00000F";
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pBidlNo", json.getString("pBidNo"));
		jsonObj.put("pRegDate", json.getString("pRegDate"));
		jsonObj.put("pTrdAmt", json.getString("pLendAmt"));
		jsonObj.put("pGuaranteesAmt", json.getString("pGuaranteesAmt"));
		jsonObj.put("pTrdLendRate", json.getString("pTrdLendRate"));
		jsonObj.put("pTrdCycleType", json.getString("pTrdCycleType"));
		jsonObj.put("pTrdCycleValue", json.getString("pTrdCycleValue"));
		jsonObj.put("pLendPurpose", json.getString("pLendPurpose"));
		jsonObj.put("pRepayMode", json.getString("pRepayMode"));
		jsonObj.put("pOperationType", json.getString("pOperationType"));
		jsonObj.put("pLendFee", json.getString("pLendFee"));
		jsonObj.put("pAcctType", json.getString("pAcctType"));
		jsonObj.put("pIdentNo", json.getString("pIdentNo"));
		jsonObj.put("pRealName", json.getString("pRealName"));
		jsonObj.put("pIpsAcctNo", json.getString("pIpsAcctNo"));
		jsonObj.put("pP2PBillN", "L-"+CryptTool.getRandom(10, false, true));
		jsonObj.put("pIpsTime", DateUtil.simple(new Date()));
		jsonObj.put("pBidStatus", "1");
		jsonObj.put("pRealFreezenAmt", Double.parseDouble(json.getString("pGuaranteesAmt"))+ Double.parseDouble(json.getString("pLendFee")));
		
		jsonObj.put("pMemo1", json.getString("pMemo1"));
		jsonObj.put("pMemo2", json.getString("pMemo2"));
		jsonObj.put("pMemo3", json.getString("pMemo3"));
		
		System.out.println("标的登记："+jsonObj.toString());
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null), Constants.ENCRYPTION_KEY);
		
		String pSign = Encrypt.MD5(pMerCode+pErrCode+pErrMsg+p3DesXmlPara+Constants.ENCRYPTION_KEY);

		Map<String, String> args = new HashMap<String, String>();
		
		args.put("url", json.getString("pWebUrl"));
		args.put("pMerCode", pMerCode);
		args.put("pErrCode", pErrCode);
		args.put("pErrMsg", pErrMsg);
		args.put("p3DesXmlPara", p3DesXmlPara);
		args.put("pSign", pSign);
		
		return args;
	}
	
	/**
	 * 登记债权人
	 * @param argMerCode
	 * @param json
	 * @param argSign
	 * @return
	 */
	public static Map<String, String> registerCreditor(String argMerCode, JSONObject json, String argSign) {
		String pMerCode = argMerCode;
		String pErrCode = "MG00000F";
		String pErrMsg = "MG00000F";
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonObj.put("pMerDate", json.getString("pMerDate"));
		jsonObj.put("pAccountDealNo", "P-"+CryptTool.getRandom(10, false, true));
		jsonObj.put("pBidDealNo", "B-"+CryptTool.getRandom(10, false, true));
		jsonObj.put("pBidNo", json.getString("pBidNo"));
		jsonObj.put("pContractNo", json.getString("pContractNo"));
		jsonObj.put("pBusiType", 1);
		jsonObj.put("pAuthAmt", json.getString("pAuthAmt"));
		jsonObj.put("pTrdAmt", json.getString("pTrdAmt"));
		jsonObj.put("pFee", json.getString("pFee"));
		jsonObj.put("pTransferAmt", Double.parseDouble(json.getString("pTrdAmt"))+Double.parseDouble(json.getString("pFee")));
		jsonObj.put("pAccount", json.getString("pAccount"));
		jsonObj.put("pStatus", 1);
		jsonObj.put("pP2PBillNo", "WS-"+CryptTool.getRandom(10, false, true));
		jsonObj.put("pIpsTime", DateUtil.simple2(new Date()));
		
		jsonObj.put("pMemo1", json.getString("pMemo1"));
		jsonObj.put("pMemo2", json.getString("pMemo2"));
		jsonObj.put("pMemo3", json.getString("pMemo3"));
		
		System.out.println("登记债权人："+jsonObj.toString());
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null), Constants.ENCRYPTION_KEY);
		
		String pSign = Encrypt.MD5(pMerCode+pErrCode+pErrMsg+p3DesXmlPara+Constants.ENCRYPTION_KEY);

		Map<String, String> args = new HashMap<String, String>();
		
		args.put("url", json.getString("pWebUrl"));
		args.put("pMerCode", pMerCode);
		args.put("pErrCode", pErrCode);
		args.put("pErrMsg", pErrMsg);
		args.put("p3DesXmlPara", p3DesXmlPara);
		args.put("pSign", pSign);
		
		return args;
	}
	
	/**
	 * 登记担保方
	 * @param argMerCode
	 * @param json
	 * @param argSign
	 * @return
	 */
	public static Map<String, String> registerGuarantor(String argMerCode, JSONObject json, String argSign) {
		String pMerCode = argMerCode;
		String pErrCode = "MG00000F";
		String pErrMsg = "MG00000F";
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonObj.put("pMerDate", json.getString("pMerDate"));
		jsonObj.put("pP2PBillNo", "PB-"+CryptTool.getRandom(10, false, true));
		jsonObj.put("pBidNo", json.getString("pBidNo"));
		jsonObj.put("pAmount", json.getString("pAmount"));
		jsonObj.put("pMarginAmt", json.getString("pMarginAmt"));
		jsonObj.put("pProFitAmt", json.getString("pProFitAmt"));
		double freeze = Double.parseDouble(json.getString("pAmount"))+Double.parseDouble(json.getString("pMarginAmt"))+Double.parseDouble(json.getString("pProFitAmt"));
		jsonObj.put("pRealFreezeAmt", freeze);
		jsonObj.put("pAcctType", 1);
		jsonObj.put("pAccountName", json.getString("pAccountName"));
		jsonObj.put("pAccount", freeze);
		jsonObj.put("pCompenAmt", json.getString("pAccount"));
		jsonObj.put("pIpsTime", DateUtil.simple2(new Date()));
		jsonObj.put("pStatus", 0);
		
		jsonObj.put("pMemo1", json.getString("pMemo1"));
		jsonObj.put("pMemo2", json.getString("pMemo2"));
		jsonObj.put("pMemo3", json.getString("pMemo3"));
		
		System.out.println("登记担保方："+jsonObj.toString());
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null), Constants.ENCRYPTION_KEY);
		
		String pSign = Encrypt.MD5(pMerCode+pErrCode+pErrMsg+p3DesXmlPara+Constants.ENCRYPTION_KEY);

		Map<String, String> args = new HashMap<String, String>();
		
		args.put("url", json.getString("pWebUrl"));
		args.put("pMerCode", pMerCode);
		args.put("pErrCode", pErrCode);
		args.put("pErrMsg", pErrMsg);
		args.put("p3DesXmlPara", p3DesXmlPara);
		args.put("pSign", pSign);
		
		return args;
	}
	
	/**
	 * 登记债权转让
	 * @param argMerCode
	 * @param json
	 * @param argSign
	 * @return
	 */
	public static Map<String, String> registerCretansfer(String argMerCode, JSONObject json, String argSign) {
		String pMerCode = argMerCode;
		String pErrCode = "MG00000F";
		String pErrMsg = "MG00000F";
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonObj.put("pMerDate", json.getString("pMerDate"));
		jsonObj.put("pBidNo", json.getString("pBidNo"));
		jsonObj.put("pContractNo", json.getString("pContractNo"));
		jsonObj.put("pFromAccountType", json.getString("pFromAccountType"));
		jsonObj.put("pFromName", json.getString("pFromName"));
		jsonObj.put("pFromAccount", json.getString("pFromAccount"));
		jsonObj.put("pFromIdentType", json.getString("pFromIdentType"));
		jsonObj.put("pFromIdentNo", json.getString("pFromIdentNo"));
		jsonObj.put("pToAccountType", json.getString("pToAccountType"));
		jsonObj.put("pToAccountName", json.getString("pToAccountName"));
		jsonObj.put("pFromIdentType", json.getString("pFromIdentType"));
		jsonObj.put("pToAccount", json.getString("pToAccount"));
		jsonObj.put("pToIdentType", json.getString("pToIdentType"));
		jsonObj.put("pToIdentNo", json.getString("pToIdentNo"));
		jsonObj.put("pCreMerBillNo", json.getString("pCreMerBillNo"));
		jsonObj.put("pFromFee", json.getString("pFromFee"));
		jsonObj.put("pToFee", json.getString("pToFee"));
		jsonObj.put("pCretType", json.getString("pCretType"));
		jsonObj.put("pWebUrl", json.getString("pWebUrl"));
		jsonObj.put("pS2SUrl", json.getString("pS2SUrl"));
		
		jsonObj.put("pMemo1", json.getString("pMemo1"));
		jsonObj.put("pMemo2", json.getString("pMemo2"));
		jsonObj.put("pMemo3", json.getString("pMemo3"));
		
		System.out.println("登记债权转让："+jsonObj.toString());
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null), Constants.ENCRYPTION_KEY);
		
		String pSign = Encrypt.MD5(pMerCode+pErrCode+pErrMsg+p3DesXmlPara+Constants.ENCRYPTION_KEY);

		Map<String, String> args = new HashMap<String, String>();
		
		args.put("url", json.getString("pWebUrl"));
		args.put("pMerCode", pMerCode);
		args.put("pErrCode", pErrCode);
		args.put("pErrMsg", pErrMsg);
		args.put("p3DesXmlPara", p3DesXmlPara);
		args.put("pSign", pSign);
		
		return args;
	}
	
	/**
	 * 自动投标签约
	 * @param argMerCode
	 * @param json
	 * @param argSign
	 * @return
	 */
	public static Map<String, String> autoSign(String argMerCode, JSONObject json, String argSign) {
		String pMerCode = argMerCode;
		String pErrCode = "MG00000F";
		String pErrMsg = "MG00000F";
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonObj.put("pSigningDate", json.getString("pSigningDate"));
		jsonObj.put("pIpsAuthNo", "invest-"+CryptTool.getRandom(10, false, true));
		String type = json.getString("pValidType");
		
		System.out.println("有效日期类型："+type);
		Date validDate = null;
		
		if("D".equals(type)) {
			int day = Integer.parseInt(json.getString("pValidDate"));
			validDate = DateUtil.dateAddDay(new Date(), day);
		}else if("M".equals(type)) {
			int month = Integer.parseInt(json.getString("pValidDate"));
			validDate = DateUtil.dateAddMonth(new Date(), month);
		}
		
		jsonObj.put("pValidDate", DateUtil.simple(validDate));
			
		jsonObj.put("pSAmtQuota", json.getString("pSAmtQuota"));
		jsonObj.put("pEAmtQuota", json.getString("pEAmtQuota"));
		jsonObj.put("pSIRQuota", json.getString("pSIRQuota"));
		jsonObj.put("pEIRQuota", json.getString("pEIRQuota"));
		jsonObj.put("pIpsTime", DateUtil.simple2(new Date()));
		
		jsonObj.put("pMemo1", json.getString("pMemo1"));
		jsonObj.put("pMemo2", json.getString("pMemo2"));
		jsonObj.put("pMemo3", json.getString("pMemo3"));
		
		System.out.println("自动投标签约："+jsonObj.toString());
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null), Constants.ENCRYPTION_KEY);
		
		String pSign = Encrypt.MD5(pMerCode+pErrCode+pErrMsg+p3DesXmlPara+Constants.ENCRYPTION_KEY);

		Map<String, String> args = new HashMap<String, String>();
		
		args.put("url", json.getString("pWebUrl"));
		args.put("pMerCode", pMerCode);
		args.put("pErrCode", pErrCode);
		args.put("pErrMsg", pErrMsg);
		args.put("p3DesXmlPara", p3DesXmlPara);
		args.put("pSign", pSign);
		
		return args;
	}
	
	/**
	 * 自动还款签约
	 * @param argMerCode
	 * @param json
	 * @param argSign
	 * @return
	 */
	public static Map<String, String> repaymentSign(String argMerCode, JSONObject json, String argSign) {
		String pMerCode = argMerCode;
		String pErrCode = "MG00000F";
		String pErrMsg = "MG00000F";
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonObj.put("pSigningDate", json.getString("pSigningDate"));
		jsonObj.put("pP2PBillNo", "P"+CryptTool.getRandom(10, false, true));
			
		jsonObj.put("pIdentType", json.getString("pIdentType"));
		jsonObj.put("pIdentNo", json.getString("pIdentNo"));
		jsonObj.put("pRealName", json.getString("pRealName"));
		jsonObj.put("pIpsAcctNo", json.getString("pIpsAcctNo"));
		jsonObj.put("pIpsAuthNo", "repayment-"+CryptTool.getRandom(10, false, true));
		jsonObj.put("pIpsTime", DateUtil.simple2(new Date()));
		
		jsonObj.put("pMemo1", json.getString("pMemo1"));
		jsonObj.put("pMemo2", json.getString("pMemo2"));
		jsonObj.put("pMemo3", json.getString("pMemo3"));
		
		System.out.println("自动还款签约："+jsonObj.toString());
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null), Constants.ENCRYPTION_KEY);
		
		String pSign = Encrypt.MD5(pMerCode+pErrCode+pErrMsg+p3DesXmlPara+Constants.ENCRYPTION_KEY);

		Map<String, String> args = new HashMap<String, String>();
		
		args.put("url", json.getString("pWebUrl"));
		args.put("pMerCode", pMerCode);
		args.put("pErrCode", pErrCode);
		args.put("pErrMsg", pErrMsg);
		args.put("p3DesXmlPara", p3DesXmlPara);
		args.put("pSign", pSign);
		
		return args;
	}
	
	/**
	 * 充值
	 * @param argMerCode
	 * @param json
	 * @param argSign
	 * @return
	 */
	public static Map<String, String> recharge(String argMerCode, JSONObject json, String argSign) {
		String pMerCode = argMerCode;
		String pErrCode = "MG00000F";
		String pErrMsg = "MG00000F";
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonObj.put("pAcctType", json.getString("pAcctType"));
		jsonObj.put("pIdentNo", json.getString("pIdentNo"));
		jsonObj.put("pRealName", json.getString("pRealName"));
		jsonObj.put("pIpsAcctNo", json.getString("pIpsAcctNo"));
		jsonObj.put("pTrdDate", json.getString("pTrdDate"));
		jsonObj.put("pTrdAmt", json.getString("pTrdAmt"));
		jsonObj.put("pTrdBnkCode", json.getString("pTrdBnkCode"));
		jsonObj.put("pIpsBillNo", "RECHARGE-"+CryptTool.getRandom(10, false, true));
		
		jsonObj.put("pMemo1", json.getString("pMemo1"));
		jsonObj.put("pMemo2", json.getString("pMemo2"));
		jsonObj.put("pMemo3", json.getString("pMemo3"));
		
		System.out.println("充值："+jsonObj.toString());
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null), Constants.ENCRYPTION_KEY);
		
		String pSign = Encrypt.MD5(pMerCode+pErrCode+pErrMsg+p3DesXmlPara+Constants.ENCRYPTION_KEY);

		Map<String, String> args = new HashMap<String, String>();
		
		args.put("url", json.getString("pWebUrl"));
		args.put("pMerCode", pMerCode);
		args.put("pErrCode", pErrCode);
		args.put("pErrMsg", pErrMsg);
		args.put("p3DesXmlPara", p3DesXmlPara);
		args.put("pSign", pSign);
		
		return args;
	}
	
	/**
	 * 转账
	 * @param argMerCode
	 * @param json
	 * @param argSign
	 * @return
	 */
	public static Map<String, String> transfer(String argMerCode, JSONObject json, String argSign) {
		String pMerCode = argMerCode;
		String pErrCode = "MG00000F";
		String pErrMsg = "MG00000F";
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonObj.put("pBidNo", json.getString("pBidNo"));
		jsonObj.put("pDate", json.getString("pDate"));
		jsonObj.put("pTransferType", json.getString("pTransferType"));
		jsonObj.put("pTransferMode", json.getString("pTransferMode"));
		jsonObj.put("pIpsBillNo", "T-"+CryptTool.getRandom(10, false, true));
		
		JSONArray pDetails = new JSONArray();
		
		JSONArray argsDetails = json.getJSONArray("pDetails");
		
		for (int i = 0; i < argsDetails.size(); i++) {
			JSONObject pRow = argsDetails.getJSONObject(i);
			
			pRow.put("pOriMerBillNo", pRow.getString("pOriMerBillNo"));
			pRow.put("pTrdAmt", pRow.getString("pTrdAmt"));
			pRow.put("pFAcctType", pRow.getString("pFAcctType"));
			pRow.put("pFIpsAcctNo", pRow.getString("pFIpsAcctNo"));
			pRow.put("pFTrdFee", pRow.getString("pFTrdFee"));
			pRow.put("pTAcctType", pRow.getString("pTAcctType"));
			pRow.put("pTIpsAcctNo", pRow.getString("pTIpsAcctNo"));
			pRow.put("pTTrdFee", pRow.getString("pTTrdFee"));
			
			pRow.put("pIpsDetailBillNo", "TD-"+CryptTool.getRandom(10, false, true));
			pRow.put("pIpsDetailTime", DateUtil.simple2(new Date()));
			pRow.put("pIpsFee", 0);
			pRow.put("pStatus", "Y");
			pRow.put("pMessage", "nothing");
			
			pDetails.add(pRow);
		}
		
		jsonObj.put("pDetails", pDetails);
		
		jsonObj.put("pMemo1", json.getString("pMemo1"));
		jsonObj.put("pMemo2", json.getString("pMemo2"));
		jsonObj.put("pMemo3", json.getString("pMemo3"));
		
		System.out.println("转账："+jsonObj.toString());
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		
		String pSign = Encrypt.MD5(pMerCode+pErrCode+pErrMsg+p3DesXmlPara+Constants.ENCRYPTION_KEY);

		Map<String, String> args = new HashMap<String, String>();
		
		args.put("url", json.getString("pS2SUrl"));
		args.put("pMerCode", pMerCode);
		args.put("pErrCode", pErrCode);
		args.put("pErrMsg", pErrMsg);
		args.put("p3DesXmlPara", p3DesXmlPara);
		args.put("pSign", pSign);
		
		return args;
	}
	
	/**
	 * 还款
	 * @param argMerCode
	 * @param json
	 * @param argSign
	 * @return
	 */
	public static Map<String, String> repayment(String argMerCode, JSONObject json, String argSign) {
		String pMerCode = argMerCode;
		String pErrCode = "MG00000F";
		String pErrMsg = "MG00000F";
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pBidNo", json.getString("pBidNo"));
		jsonObj.put("pRepaymentDate", json.getString("pRepaymentDate"));
		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonObj.put("pIpsBillNo", "P-"+CryptTool.getRandom(10, false, true));
		jsonObj.put("pIpsDate", DateUtil.simple(new Date()));
		
		JSONArray pDetails = new JSONArray();
		
		JSONArray argsDetails = json.getJSONArray("pDetails");
		
		for (int i = 0; i < argsDetails.size(); i++) {
			JSONObject pRow = argsDetails.getJSONObject(i);
			
			pRow.put("pCreMerBillNo", pRow.getString("pCreMerBillNo"));
			pRow.put("pInAcctNo", pRow.getString("pInAcctNo"));
			pRow.put("pInFee", pRow.getString("pInFee"));
			pRow.put("pStatus", "Y");
			pRow.put("pMessage", "恭喜你，成功了！");
			
			pDetails.add(pRow);
		}
		
		jsonObj.put("pDetails", pDetails);
		
		jsonObj.put("pMemo1", json.getString("pMemo1"));
		jsonObj.put("pMemo2", json.getString("pMemo2"));
		jsonObj.put("pMemo3", json.getString("pMemo3"));
		
		System.out.println("还款："+jsonObj.toString());
		
		String pp = Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null);
		
		System.out.println(pp);
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
		
		String pSign = Encrypt.MD5(pMerCode+pErrCode+pErrMsg+p3DesXmlPara+Constants.ENCRYPTION_KEY);

		Map<String, String> args = new HashMap<String, String>();
		
		args.put("url", json.getString("pWebUrl"));
		args.put("pMerCode", pMerCode);
		args.put("pErrCode", pErrCode);
		args.put("pErrMsg", pErrMsg);
		args.put("p3DesXmlPara", p3DesXmlPara);
		args.put("pSign", pSign);
		
		return args;
	}
	
	/**
	 * 解冻保证金
	 * @param argMerCode
	 * @param json
	 * @param argSign
	 * @return
	 */
	public static Map<String, String> guaranteeUnfreeze(String argMerCode, JSONObject json, String argSign) {
		String pMerCode = argMerCode;
		String pErrCode = "MG00000F";
		String pErrMsg = "MG00000F";
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonObj.put("pBidNo", json.getString("pBidNo"));
		jsonObj.put("pUnfreezeDate", json.getString("pUnfreezeDate"));
		jsonObj.put("pUnfreezeAmt", json.getString("pUnfreezeAmt"));
		jsonObj.put("pUnfreezenType", json.getString("pUnfreezenType"));
		jsonObj.put("pAcctType", json.getString("pAcctType"));
		jsonObj.put("pIdentNo", json.getString("pIdentNo"));
		jsonObj.put("pRealName", json.getString("pRealName"));
		jsonObj.put("pIpsAcctNo", json.getString("pIpsAcctNo"));
		jsonObj.put("pIpsBillNo", "P-"+CryptTool.getRandom(10, false, true));
		jsonObj.put("pIpsTime", DateUtil.simple2(new Date()));
		
		jsonObj.put("pMemo1", json.getString("pMemo1"));
		jsonObj.put("pMemo2", json.getString("pMemo2"));
		jsonObj.put("pMemo3", json.getString("pMemo3"));
		
		System.out.println("解冻保证金："+jsonObj.toString());
		
		String pp = Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null);
		
		System.out.println(pp);
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", "null", null, null), Constants.ENCRYPTION_KEY);
		
		String pSign = Encrypt.MD5(pMerCode+pErrCode+pErrMsg+p3DesXmlPara+Constants.ENCRYPTION_KEY);

		Map<String, String> args = new HashMap<String, String>();
		
		args.put("url", json.getString("pS2SUrl"));
		args.put("pMerCode", pMerCode);
		args.put("pErrCode", pErrCode);
		args.put("pErrMsg", pErrMsg);
		args.put("p3DesXmlPara", p3DesXmlPara);
		args.put("pSign", pSign);
		
		return args;
	}
	
	/**
	 * 自动代扣充值
	 * @param argMerCode
	 * @param json
	 * @param argSign
	 * @return
	 */
	public static Map<String, String> coDp(String argMerCode, JSONObject json, String argSign) {
		String pMerCode = argMerCode;
		String pErrCode = "MG00000F";
		String pErrMsg = "MG00000F";
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonObj.put("pTrdDate", json.getString("pTrdDate"));
		jsonObj.put("pTrdAmt", json.getString("pTrdAmt"));
		jsonObj.put("pMerFee", json.getString("pMerFee"));
		jsonObj.put("pIpsFeeType", json.getString("pIpsFeeType"));
		jsonObj.put("pAcctType", json.getString("pAcctType"));
		jsonObj.put("pIdentNo", json.getString("pIdentNo"));
		jsonObj.put("pRealName", json.getString("pRealName"));
		jsonObj.put("pIpsAcctNo", json.getString("pIpsAcctNo"));
		jsonObj.put("pIpsBillNo", "P-"+CryptTool.getRandom(10, false, true));
		jsonObj.put("pIpsTime", DateUtil.simple2(new Date()));
		jsonObj.put("pTrdRealAmt", json.getString("pTrdAmt"));
		jsonObj.put("pIpsFee", Double.parseDouble(json.getString("pTrdAmt"))*0.01);
		jsonObj.put("pTrdBnkCode", "201");
		
		jsonObj.put("pMemo1", json.getString("pMemo1"));
		jsonObj.put("pMemo2", json.getString("pMemo2"));
		jsonObj.put("pMemo3", json.getString("pMemo3"));
		
		System.out.println("自动代扣充值："+jsonObj.toString());
		
		String pp = Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null);
		
		System.out.println(pp);
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", "null", null, null), Constants.ENCRYPTION_KEY);
		
		String pSign = Encrypt.MD5(pMerCode+pErrCode+pErrMsg+p3DesXmlPara+Constants.ENCRYPTION_KEY);

		Map<String, String> args = new HashMap<String, String>();
		
		args.put("url", json.getString("pS2SUrl"));
		args.put("pMerCode", pMerCode);
		args.put("pErrCode", pErrCode);
		args.put("pErrMsg", pErrMsg);
		args.put("p3DesXmlPara", p3DesXmlPara);
		args.put("pSign", pSign);
		
		return args;
	}
	
	/**
	 * 提现
	 * @param argMerCode
	 * @param json
	 * @param argSign
	 * @return
	 */
	public static Map<String, String> withdrawal(String argMerCode, JSONObject json, String argSign) {
		String pMerCode = argMerCode;
		String pErrCode = "MG00000F";
		String pErrMsg = "MG00000F";
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pMerBillNo", json.getString("pMerBillNo"));
		jsonObj.put("pAcctType", json.getString("pAcctType"));
		jsonObj.put("pIdentNo", json.getString("pIdentNo"));
		jsonObj.put("pRealName", json.getString("pRealName"));
		jsonObj.put("pIpsAcctNo", json.getString("pIpsAcctNo"));
		jsonObj.put("pDwDate", json.getString("pDwDate"));
		jsonObj.put("pTrdAmt", json.getString("pTrdAmt"));
		jsonObj.put("pIpsBillNo", "P-"+CryptTool.getRandom(10, false, true));
		
		jsonObj.put("pMemo1", json.getString("pMemo1"));
		jsonObj.put("pMemo2", json.getString("pMemo2"));
		jsonObj.put("pMemo3", json.getString("pMemo3"));
		
		System.out.println("提现："+jsonObj.toString());
		
		String pp = Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null);
		
		System.out.println(pp);
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonObj.toString(), "pReq", "null", null, null), Constants.ENCRYPTION_KEY);
		
		String pSign = Encrypt.MD5(pMerCode+pErrCode+pErrMsg+p3DesXmlPara+Constants.ENCRYPTION_KEY);
		
		Map<String, String> args = new HashMap<String, String>();
		
		args.put("url", json.getString("pWebUrl"));
		args.put("pMerCode", pMerCode);
		args.put("pErrCode", pErrCode);
		args.put("pErrMsg", pErrMsg);
		args.put("p3DesXmlPara", p3DesXmlPara);
		args.put("pSign", pSign);
		
		return args;
	}
	
	/**
	 * 账户余额查询
	 * @param argMerCode
	 * @param json
	 * @param argSign
	 * @return
	 */
	public static String queryBalance(String argMerCode, String argIpsAccount) {
		String pMerCode = argMerCode;
		String pErrCode = "00000";
		String pErrMsg = "00000";
		double pBalance = 1523000.00;
		double pLock = 5520.00;
		double pNeedstl = 100.00;
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pMerCode", pMerCode);
		jsonObj.put("pErrCode", pErrCode);
		jsonObj.put("pErrMsg", pErrMsg);
		jsonObj.put("pIpsAcctNo", argIpsAccount);
		jsonObj.put("pBalance", 1523000.00);
		jsonObj.put("pLock", 5520.00);
		jsonObj.put("pNeedstl", 100.00);
		jsonObj.put("pSign", Encrypt.MD5(pMerCode+pErrCode+pErrMsg+pBalance+pLock+pNeedstl+Constants.ENCRYPTION_KEY));
		
		System.out.println("账户余额查询："+jsonObj.toString());
		
		String argXmlPara = Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null);
		
		System.out.println(argXmlPara);
		
		return argXmlPara;
	}
	
	/**
	 * 银行列表查询
	 * @param argMerCode
	 * @return
	 */
	public static String bankList(String argMerCode) {
		String pMerCode = argMerCode;
		String pErrCode = "MG00000F";
		String pErrMsg = "MG00000F";
		String pBankList =	"中国农业银行|中农|2001#中国工商银行|中商|2002";
		String pSign = "<pMerCode>"+pMerCode+"</pMerCode>"+"<pErrCode>"+pErrCode+"</pErrCode>"
				+ "<pErrMsg>"+pErrMsg+"</pErrMsg>"+"<pBankList>"+pBankList+"</pBankList>";
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pMerCode", pMerCode);
		jsonObj.put("pErrCode", pErrCode);
		jsonObj.put("pErrMsg", pErrMsg);
		jsonObj.put("pBankList", pBankList);
		jsonObj.put("pSign", Encrypt.MD5(pSign+Constants.ENCRYPTION_KEY));
		
		System.out.println("银行列表查询："+jsonObj.toString());
		
		String argXmlPara = Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null);
		
		System.out.println(argXmlPara);
		
		return argXmlPara;
	}
	
	/**
	 * 账户信息查询
	 * @param argMerCode
	 * @param argIpsAccount
	 * @param argMemo
	 * @return
	 */
	public static String queryAccount(String argMerCode, String argIpsAccount, String argMemo) {
		String pEmail = "luozhiping@eims.com.cn";
		String pStatus = "04";
		String pUCardStatus = "02";
		String pBankName = "中国农业银行";
		String pBCardStatus = "02";
		String pSignStatus ="04";
		String pSign = "<pMerCode>"+argMerCode+"</pMerCode>"+"<pIpsAcctNo>"+argIpsAccount+"</pIpsAcctNo>"
				+ "<pEmail>"+pEmail+"<pEmail/>"+"<pStatus>"+pStatus+"<pStatus/>"
				+ "<pUCardStatus>"+pUCardStatus+"<pUCardStatus/>"+"<pBankName>"+pBankName+"<pBankName/>"
				+ "<pBCardStatus>"+pBCardStatus+"<pBCardStatus/>"+"<pSignStatus>"+pSignStatus+"<pSignStatus/>";
				
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pMerCode", argMerCode);
		jsonObj.put("pIpsAcctNo", argIpsAccount);
		jsonObj.put("pEmail", "luozhiping@eims.com.cn");
		jsonObj.put("pStatus", "04");
		jsonObj.put("pUCardStatus", "02");
		jsonObj.put("pBankName", "中国农业银行");
		jsonObj.put("pBCardStatus", "02");
		jsonObj.put("pSignStatus", "04");
		jsonObj.put("pSign", Encrypt.MD5(pSign+Constants.ENCRYPTION_KEY));
		jsonObj.put("pMemo", argMemo);
		jsonObj.put("pErrCode", "0000");
		
		System.out.println("账户信息查询："+jsonObj.toString());
		
		String argXmlPara = Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null);
		
		System.out.println(argXmlPara);
		
		return argXmlPara;
	}
	
	
}
