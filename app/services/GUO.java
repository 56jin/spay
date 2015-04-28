package services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import models.t_guo_order_details;
import models.t_member_events;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
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
import utils.Converter;
import utils.DateUtil;
import utils.ErrorInfo;
import utils.GUOUtil;
import business.DealDetail;
import business.GuoOrderDetails;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.shove.security.Encrypt;

import constants.Constants;
import constants.GUOConstants;
import controllers.GUO.GPayment.CommonUtils;

/**
 * 国付宝业务类
 * @author cp
 *	@create 2015年1月7日 下午4:12:21
 */
public class GUO {

	public int type;
	public int platform;
	public long memberId;
	public String memberName;
	public String argMerCode;
	public String arg3DesXmlPara;
	public String argExtraPara;
	public JSONObject desPara;
	public JSONObject extraPara;
	
	public String pMerCode;
	public String pErrCode;
	public String pErrMsg;
	public String p3DesXmlPara;
	public JSONObject pDesJson;
	
	public JSONObject getDesPara() {
		return (JSONObject)Converter.xmlToObj(arg3DesXmlPara);
	}
	
	public void addParamTopDesJson(String key,String value){
		if(this.pDesJson == null ){
			pDesJson = (JSONObject)Converter.xmlToObj(argExtraPara);
		}
		this.pDesJson.put(key, value);
	}
	
	public void addParamToExtraPara(String key,String value){
		if(this.extraPara == null ){
			extraPara = (JSONObject)Converter.xmlToObj(arg3DesXmlPara);
		}
		this.extraPara.put(key, value);
	}
	
	public JSONObject getExtraPara() {
		return (JSONObject)Converter.xmlToObj(argExtraPara);
	}
	
	public String getP3DesXmlPara() {
		return null;
	}
	
	/**
	 * 开户
	 * @return
	 */
	public  Map<String, String> createAccount() {
		String tranCode = GUOConstants.CMD_CREATE_ACCOUNT;
		
		Map<String, String> args = new HashMap<String, String>();
		String tranDateTime = DateUtil.simple2(new Date());
		args.put("version",GUOConstants.VERSION);
		args.put("signType",GUOConstants.SIGN_TYPE);
		args.put("charset",GUOConstants.CHARSET);
		args.put("tranCode",tranCode);
		args.put("merId",GUOConstants.MER_ID);
		args.put("merName",GUOConstants.MER_NAME);
		args.put("merOrderNum",this.desPara.getString("pMerBillNo"));
		args.put("tranDateTime",tranDateTime);
		args.put("customerId",this.extraPara.getString("userId"));
		args.put("mobilePhone",this.desPara.getString("pMobileNo"));
		args.put("backgroundMerUrl",GUOConstants.IPS_ACCOUNT_S2S);
		args.put("tranIP",this.extraPara.getString("tranIP"));
		
		String signValue = "version=["+GUOConstants.VERSION+"]tranCode=["+tranCode+"]"
				+ "merId=["+GUOConstants.MER_ID+"]merOrderNum=["+this.desPara.getString("pMerBillNo")+"]"
						+ "tranDateTime=["+tranDateTime+"]customerId=["+this.extraPara.getString("userId")+"]"
								+ "tranIP=["+this.extraPara.getString("tranIP")+"]VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		Logger.debug("signValue 明文 :  "+signValue+"\n signValue 加密 : "+GUOUtil.md5(signValue));
		
		args.put("signValue", GUOUtil.md5(signValue));
		
		JsonObject obj = new JsonObject();
		obj.addProperty("platform", platform);
		obj.addProperty("memberId", this.memberId);
		obj.addProperty("pMemo1", this.memberId);
		
		DealDetail.addEvent(memberId, type, platform, this.desPara.getString("pMerBillNo"), null, this.desPara.getString("pS2SUrl"), obj.toString(), null);
		
		return args;
	}
	
	/**
	 * 登录
	 * @return
	 */
	public  Map<String, String> login( ) {
		String tranCode = GUOConstants.CMD_LOGIN;
		
		Map<String, String> args = new HashMap<String, String>();
		String merOrderNum = DateUtil.simple2(new Date());
		String tranDateTime = DateUtil.simple2(new Date());
		args.put("version",GUOConstants.VERSION);
		args.put("signType",GUOConstants.SIGN_TYPE);
		args.put("charset",GUOConstants.CHARSET);
		args.put("tranCode",tranCode);
		args.put("merId",GUOConstants.MER_ID);
		args.put("merName",GUOConstants.MER_NAME);
		args.put("merOrderNum",merOrderNum);
		args.put("tranDateTime",tranDateTime);
		String contractNo = this.extraPara.getString("contractNo") ;
		String mobilePhone =  this.extraPara.getString("mobilePhone");
		args.put("contractNo",contractNo);
		args.put("mobilePhone",mobilePhone);
		args.put("frontMerUrl",GUOConstants.GUO_ACCOUNT_WEB);
		args.put("tranIP","127.0.0.1");
		
		String signValue = "version=["+GUOConstants.VERSION+"]tranCode=["+tranCode+"]"
				+ "merId=["+GUOConstants.MER_ID+"]contractNo=["+contractNo+"]"
						+ "merOrderNum=["+merOrderNum+"]mobilePhone=["+mobilePhone+"]"
								+ "tranDateTime=["+tranDateTime+"]VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		Logger.debug("signValue 明文 :  "+signValue+"\n signValue 加密 :  "+GUOUtil.md5(signValue));
		
		args.put("signValue", GUOUtil.md5(signValue));
		
		return args;
	}
	
	/**
	 * 余额查询
	 * @return
	 */
	public Map<String, String> queryAccount(String argIpsAccount) {
		String tranCode = GUOConstants.CMD_QUERYACCOUNT;
		
		Map<String, String> args = new HashMap<String, String>();
		String merOrderNum = this.extraPara.getString("pMerBillNo");
		String tranDateTime = DateUtil.simple2(new Date());
		String mobilePhone = this.extraPara.getString("mobile");
		String tranIP = this.extraPara.getString("tranIP");
		args.put("version",GUOConstants.VERSION);
		args.put("signType",GUOConstants.SIGN_TYPE);
		args.put("charset",GUOConstants.CHARSET);
		args.put("tranCode",tranCode);
		args.put("merId",GUOConstants.MER_ID);
		args.put("merName",GUOConstants.MER_NAME);
		args.put("merOrderNum",merOrderNum);
		args.put("tranDateTime",tranDateTime);
		args.put("contractNo",argIpsAccount);
		args.put("mobilePhone",mobilePhone);
		args.put("tranIP",tranIP);
		
		String signValue = "version=["+GUOConstants.VERSION+"]tranCode=["+tranCode+"]"
				+ "merId=["+GUOConstants.MER_ID+"]merOrderNum=["+this.extraPara.getString("pMerBillNo")+"]"
						+ "mobilePhone=["+this.extraPara.getString("mobile")+"]contractNo=["+argIpsAccount+"]"
								+ "VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		Logger.debug("signValue 明文 :  "+signValue+"\n signValue 加密 :  "+GUOUtil.md5(signValue));
		
		args.put("signValue", GUOUtil.md5(signValue));
		
		return args;
	}
	
	/**
	 * 充值
	 * @return
	 */
	public Map<String, String> recharge() {
		String tranCode = GUOConstants.CMD_RECHARGE;
		
		Map<String, String> args = new HashMap<String, String>();
		String merOrderNum = this.desPara.getString("pMerBillNo");
		String tranDateTime = DateUtil.simple2(new Date());
		String amount = this.desPara.getString("pTrdAmt");
		String contractNo = this.desPara.getString("pIpsAcctNo");
		String mobilePhone = this.extraPara.getString("mobile");
		String frontMerUrl = this.desPara.getString("pWebUrl");
		String backgroundMerUrl = this.desPara.getString("pS2SUrl");
		String tranIP = this.extraPara.getString("tranIP");
		args.put("version",GUOConstants.VERSION);
		args.put("signType",GUOConstants.SIGN_TYPE);
		args.put("charset",GUOConstants.CHARSET);
		args.put("tranCode",tranCode);
		args.put("merId",GUOConstants.MER_ID);
		args.put("merName",GUOConstants.MER_NAME);
		args.put("merOrderNum",merOrderNum);
		args.put("tranDateTime",tranDateTime);
		args.put("tranAmt",amount);
		args.put("contractNo",contractNo);
		args.put("mobilePhone",mobilePhone);
		args.put("frontMerUrl",GUOConstants.GUO_ACCOUNT_WEB);
		args.put("backgroundMerUrl",GUOConstants.IPS_ACCOUNT_S2S);
		args.put("tranIP",tranIP);
		
		String signValue = "version=["+GUOConstants.VERSION+"]tranCode=["+tranCode+"]"
				+ "merId=["+GUOConstants.MER_ID+"]contractNo=["+contractNo+"]merOrderNum=["+merOrderNum+"]mobilePhone=["+mobilePhone+"]"
						+ "tranAmt=["+amount+"]tranDateTime=["+tranDateTime+"]"
								+ "VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		DealDetail detail = new DealDetail(platform, memberId, merOrderNum,
				GUOConstants.REGISTER_SUBJECT, Double.parseDouble(amount), false, "充值");
		
		detail.addDealDetail();
		
		String remark = "{\"userId\":\""+this.memberId+"\"}";
		DealDetail.addEvent(memberId, type+200, platform, merOrderNum, frontMerUrl, backgroundMerUrl, remark, null);
		
		Logger.debug("signValue 明文 :  "+ signValue+"\n signValue 加密 :  "+GUOUtil.md5(signValue));
		
		args.put("signValue", GUOUtil.md5(signValue));
		
		return args;
	}
	
	/**
	 * 提现
	 * @return
	 */
	public Map<String, String> withdrawal() {
		String tranCode = GUOConstants.CMD_WITHDRAWAL;
		
		Map<String, String> args = new HashMap<String, String>();
		String merOrderNum = this.desPara.getString("pMerBillNo");
		String tranDateTime = DateUtil.simple2(new Date());
		String amount = this.desPara.getString("pTrdAmt");
		String contractNo = this.desPara.getString("pIpsAcctNo");
		String mobilePhone = this.extraPara.getString("mobile");
		String frontMerUrl = this.desPara.getString("pWebUrl");
		String backgroundMerUrl = this.desPara.getString("pS2SUrl");
		String tranIP = this.extraPara.getString("tranIP");
		String feeAmount = this.desPara.getString("pMerFee");;
		args.put("version",GUOConstants.VERSION);
		args.put("signType",GUOConstants.SIGN_TYPE);
		args.put("charset",GUOConstants.CHARSET);
		args.put("tranCode",tranCode);
		args.put("merId",GUOConstants.MER_ID);
		args.put("merName",GUOConstants.MER_NAME);
		args.put("merOrderNum",merOrderNum);
		args.put("tranDateTime",tranDateTime);
		args.put("tranAmt",amount);
		args.put("mercFeeAmt",feeAmount);
		args.put("contractNo",contractNo);
		args.put("mobilePhone",mobilePhone);
		args.put("frontMerUrl",GUOConstants.GUO_ACCOUNT_WEB);
		args.put("backgroundMerUrl",GUOConstants.IPS_ACCOUNT_S2S);
		args.put("tranIP",tranIP);
		
		String signValue = "version=["+GUOConstants.VERSION+"]tranCode=["+tranCode+"]"
				+ "merId=["+GUOConstants.MER_ID+"]contractNo=["+contractNo+"]merOrderNum=["+merOrderNum+"]mobilePhone=["+mobilePhone+"]"
						+ "tranAmt=["+amount+"]tranDateTime=["+tranDateTime+"]"
								+ "VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		DealDetail detail = new DealDetail(platform, memberId, merOrderNum,
				GUOConstants.REGISTER_SUBJECT, Double.parseDouble(amount), false, "提现");
		
		detail.addDealDetail();
		
		JsonObject json = new JsonObject();
		json.addProperty("userId", this.memberId);
		json.addProperty("pMemo3",this.desPara.getString("pMemo3"));
		DealDetail.addEvent(memberId, type+200, platform, merOrderNum, frontMerUrl, backgroundMerUrl, json.toString(), null);
		
		Logger.debug("signValue 明文 : "+ signValue+"\n signValue 加密 :  "+GUOUtil.md5(signValue));
		
		args.put("signValue", GUOUtil.md5(signValue));
		
		return args;
	}
	
	/**
	 * 投标
	 * @return
	 */
	public Map<String, String> invest() {
		String tranCode = GUOConstants.CMD_INVEST;
		
		Map<String, String> args = new HashMap<String, String>();
		String merOrderNum = this.desPara.getString("pMerBillNo");
		String tranDateTime = DateUtil.simple2(new Date());
		String amount = this.desPara.getString("pTrdAmt");
		String investContractNo = this.desPara.getString("pAccount");
		String contractNo = this.extraPara.getString("bidContractNo");
		String mobilePhone = this.extraPara.getString("mobile");
		String bidId = this.desPara.getString("pBidNo");
		String frontMerUrl = this.desPara.getString("pWebUrl");
		String backgroundMerUrl = this.desPara.getString("pS2SUrl");
		String tranIP = this.extraPara.getString("tranIP");
		args.put("version",GUOConstants.VERSION);
		args.put("signType",GUOConstants.SIGN_TYPE);
		args.put("charset",GUOConstants.CHARSET);
		args.put("tranCode",tranCode);
		args.put("merId",GUOConstants.MER_ID);
		args.put("merName",GUOConstants.MER_NAME);
		args.put("merOrderNum",merOrderNum);
		args.put("tranDateTime",tranDateTime);
		args.put("tranAmt",amount);
		args.put("bidId",bidId);
		args.put("contractNo",contractNo);
		args.put("investContractNo",investContractNo);
		args.put("mobilePhone",mobilePhone);
		args.put("frontMerUrl",GUOConstants.GUO_ACCOUNT_WEB);
		args.put("backgroundMerUrl",GUOConstants.IPS_ACCOUNT_S2S);
		args.put("tranIP",tranIP);
		
		String signValue = "version=["+GUOConstants.VERSION+"]tranCode=["+tranCode+"]"
				+ "merId=["+GUOConstants.MER_ID+"]bidId=["+bidId+"]contractNo=["+contractNo+"]merOrderNum=["+merOrderNum+"]mobilePhone=["+mobilePhone+"]"
						+ "tranAmt=["+amount+"]tranDateTime=["+tranDateTime+"]"
								+ "VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		DealDetail detail = new DealDetail(platform, memberId, merOrderNum,
				GUOConstants.REGISTER_SUBJECT, Double.parseDouble(amount), false, "投标");
		
		detail.addDealDetail();
		
		String remark = "{\"pFee\":\""+this.desPara.getString("pFee")+"\",\"userId\":\""+this.memberId+"\"}";
		
		DealDetail.addEvent(memberId, type+200, platform, merOrderNum, frontMerUrl, backgroundMerUrl, remark, null);
		
		Logger.debug("signValue 明文 :  "+ signValue+"\n signValue 加密 : "+GUOUtil.md5(signValue));
		
		args.put("signValue", GUOUtil.md5(signValue));
		
		return args;
	}
	
	
	/**
	 * 发标
	 * @return
	 */
	public Map<String, String> createBid() {
		String tranCode = GUOConstants.CMD_CREATEBID;
		
		Map<String, String> args = new HashMap<String, String>();
		String merOrderNum = this.desPara.getString("pMerBillNo");
		String tranDateTime = DateUtil.simple2(new Date());
		String amount = this.desPara.getString("pLendAmt");
		String contractNo = this.desPara.getString("pIpsAcctNo");
		String mobilePhone = this.extraPara.getString("mobile");
		String bidId = this.desPara.getString("pBidNo");
		String interestRate = this.desPara.getString("pTrdLendRate");
		int pTrdCycleType = Integer.parseInt(this.desPara.getString("pTrdCycleType"));
		int pTrdCycleValue = Integer.parseInt(this.desPara.getString("pTrdCycleValue"));
		String borrowingDeadLine = pTrdCycleType == 1 ? DateUtil.simple2(DateUtil.dateAddDay(new Date(), pTrdCycleValue)) :
			DateUtil.simple2(DateUtil.dateAddMonth(new Date(), pTrdCycleValue));
		String pRepayMode = this.desPara.getString("pRepayMode");
		String tranIP = this.extraPara.getString("tranIP");
		String frontMerUrl = this.desPara.getString("pWebUrl");
		String backgroundMerUrl = this.desPara.getString("pS2SUrl");
		
		args.put("version",GUOConstants.VERSION);
		args.put("signType",GUOConstants.SIGN_TYPE);
		args.put("charset",GUOConstants.CHARSET);
		args.put("tranCode",tranCode);
		args.put("merId",GUOConstants.MER_ID);
		args.put("merName",GUOConstants.MER_NAME);
		args.put("merOrderNum",merOrderNum);
		args.put("tranDateTime",tranDateTime);
		args.put("bidId",bidId);
		args.put("contractNo",contractNo);
		args.put("mobilePhone",mobilePhone);
		args.put("customerName",GUOConstants.P2P_NAME);
		args.put("borrowingBalance",amount);
		args.put("interestRate",interestRate);
		args.put("borrowingDeadLine",borrowingDeadLine);
		args.put("repaymentType","99".equals(pRepayMode) ? "1" : "2");
		args.put("tranIP",tranIP);
		
		String signValue = "version=["+GUOConstants.VERSION+"]tranCode=["+tranCode+"]"
				+ "merId=["+GUOConstants.MER_ID+"]bidId=["+bidId+"]contractNo=["+contractNo+"]merOrderNum=["+merOrderNum+"]mobilePhone=["+mobilePhone+"]"
						+ "borrowingBalance=["+amount+"]tranDateTime=["+tranDateTime+"]"
								+ "VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		DealDetail detail = new DealDetail(platform, memberId, merOrderNum,
				GUOConstants.REGISTER_SUBJECT, Double.parseDouble(amount), false, "发标");
		
		detail.addDealDetail();
		
		String remark = "{\"operation\":\""+this.desPara.getString("pMemo3")+"\",\"userId\":\""+this.memberId+"\"}";
		
		DealDetail.addEvent(memberId, type+200, platform, merOrderNum, frontMerUrl, backgroundMerUrl, remark, null);
		
		Logger.debug("signValue 明文 :  "+ signValue+"\n signValue 加密 :  "+GUOUtil.md5(signValue));
		
		args.put("signValue", GUOUtil.md5(signValue));
		
		return args;
	}
	
	/**
	 * 流标
	 * @return
	 */
	public Map<String, String> flowBid() {
		String tranCode = GUOConstants.CMD_FLOWBID;
		
		Map<String, String> args = new HashMap<String, String>();
		String merOrderNum = this.desPara.getString("pMerBillNo")+this.desPara.getString("pMemo3");
		String bidId = this.desPara.getString("pBidNo");
		String tranDateTime = DateUtil.simple2(new Date());
		String amount = this.extraPara.getString("totalInvestAmount");
		String contractNo = this.desPara.getString("pIpsAcctNo");
		String frontMerUrl = this.desPara.getString("pWebUrl");
		String backgroundMerUrl = this.desPara.getString("pS2SUrl");
		String tranIP = this.extraPara.getString("tranIP");
		args.put("version",GUOConstants.VERSION);
		args.put("signType",GUOConstants.SIGN_TYPE);
		args.put("charset",GUOConstants.CHARSET);
		String pOperationType = this.desPara.getString("pOperationType");
		if(!"1".equals(pOperationType)){
			this.type = 0;
		}
		args.put("tranCode",tranCode);
		args.put("merId",GUOConstants.MER_ID);
		args.put("merName",GUOConstants.MER_NAME);
		args.put("merOrderNum",merOrderNum);
		args.put("tranDateTime",tranDateTime);
		args.put("bidId",bidId);
		args.put("repaymentAmt",amount );
		args.put("contractNo",contractNo);
		args.put("tranIP",tranIP);
		
		String signValue = "version=["+GUOConstants.VERSION+"]tranCode=["+tranCode+"]"
				+ "merId=["+GUOConstants.MER_ID+"]bidId=["+bidId+"]contractNo=["+contractNo+"]merOrderNum=["+merOrderNum+"]"
						+ "repaymentAmt=["+amount+"]tranDateTime=["+tranDateTime+"]"
								+ "VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		DealDetail detail = new DealDetail(platform, memberId, merOrderNum,
				GUOConstants.REGISTER_SUBJECT, Double.parseDouble(amount), false, "流标");
		
		detail.addDealDetail();
		
		String remark = "{\"operation\":\""+this.desPara.getString("pMemo3")+"\","
				+ "\"pOperationType\":\""+this.desPara.getString("pOperationType")+"\"}";
		
		DealDetail.addEvent(memberId, type+200, platform, merOrderNum, frontMerUrl, backgroundMerUrl, remark, "流标");
		
		Logger.debug("signValue 明文 : "+ signValue+"\n signValue 加密 :  "+GUOUtil.md5(signValue));
		
		args.put("signValue", GUOUtil.md5(signValue));
		
		return args;
	}
	
	/**
	 * 投标完成
	 * @return
	 */
	public Map<String, String> finishBid() {
		String tranCode = GUOConstants.CMD_FINISHBID;
		
		Map<String, String> args = new HashMap<String, String>();
		String merOrderNum = this.desPara.getString("pMerBillNo");
		String tranDateTime = DateUtil.simple2(new Date());
		String amount = this.extraPara.getString("amount");
		String contractNo = this.extraPara.getString("contractNo");
		String mobilePhone = this.extraPara.getString("mobile");
		String backgroundMerUrl = this.desPara.getString("pS2SUrl");
		String tranIP = this.extraPara.getString("tranIP");
		String bidId = this.desPara.getString("pBidNo");
		String mercFeeAmt = this.extraPara.getString("serviceFees");
		args.put("version",GUOConstants.VERSION);
		args.put("signType",GUOConstants.SIGN_TYPE);
		args.put("charset",GUOConstants.CHARSET);
		args.put("tranCode",tranCode);
		args.put("merId",GUOConstants.MER_ID);
		args.put("merName",GUOConstants.MER_NAME);
		args.put("merOrderNum",merOrderNum);
		args.put("tranDateTime",tranDateTime);
		args.put("bidId",bidId);
		args.put("tranAmt",amount);
		args.put("mercFeeAmt",mercFeeAmt);
		args.put("contractNo",contractNo);
		args.put("mobilePhone",mobilePhone);
		args.put("tranIP",tranIP);
		
		String signValue = "version=["+GUOConstants.VERSION+"]tranCode=["+tranCode+"]"
				+ "merId=["+GUOConstants.MER_ID+"]bidId=["+bidId+"]contractNo=["+contractNo+"]merOrderNum=["+merOrderNum+"]"
						+ "tranAmt=["+amount+"]mercFeeAmt=["+mercFeeAmt+"]tranDateTime=["+tranDateTime+"]"
								+ "VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		DealDetail detail = new DealDetail(platform, memberId, merOrderNum,
				GUOConstants.REGISTER_SUBJECT, Double.parseDouble(amount), false, "投标完成");
		
		detail.addDealDetail();
		
		JsonObject remark = new JsonObject();
		remark.addProperty("payment", this.desPara.getString("pMemo3"));
		remark.addProperty("pMemo1", this.memberId);
		
		DealDetail.addEvent(memberId, type+200, platform, merOrderNum, null, backgroundMerUrl, remark.toString(), null);
	
		Logger.debug("signValue 明文 :  "+ signValue+"\n signValue 加密 :  "+GUOUtil.md5(signValue));
		
		args.put("signValue", GUOUtil.md5(signValue));
		
		return args;
	}
	
	/**
	 * 还款
	 * @return
	 */
	public Map<String, String> repayment() {
		String tranCode = GUOConstants.CMD_REPAYMENT;
		
		Map<String, String> args = new HashMap<String, String>();
		String merOrderNum = this.desPara.getString("pMerBillNo");
		String bidId = this.desPara.getString("pBidNo");
		String tranDateTime = DateUtil.simple2(new Date());
		String contractNo = this.desPara.getString("pOutAcctNo");
		String mobilePhone = this.extraPara.getString("mobile");
		String amount = this.desPara.getString("pOutAmt");
		double amountPrice = Double.parseDouble(amount);
		double totalManageFeePrice = Double.parseDouble(this.extraPara.getString("totalManageFee"));
		String amountPriceValue = String.format("%.2f", amountPrice-totalManageFeePrice);
		String mercFeeAmt = this.extraPara.getString("totalManageFee");
		String frontMerUrl = this.desPara.getString("pWebUrl");
		String backgroundMerUrl = this.desPara.getString("pS2SUrl");
		String tranIP = this.extraPara.getString("tranIP");
		String repaymentType = "0"; //借款人还款
		String isInFull = Integer.parseInt(this.extraPara.getString("leftPayment")) > 0 ? "0" : "1";
		String investInfo = this.extraPara.getString("investInfo");
		
		args.put("version",GUOConstants.VERSION);
		args.put("signType",GUOConstants.SIGN_TYPE);
		args.put("charset",GUOConstants.CHARSET);
		args.put("tranCode",tranCode);
		args.put("merId",GUOConstants.MER_ID);
		args.put("merName",GUOConstants.MER_NAME);
		args.put("merOrderNum",merOrderNum);
		args.put("tranDateTime",tranDateTime);
		args.put("bidId",bidId);
		
		args.put("contractNo",contractNo);
		args.put("mobilePhone",mobilePhone);
		args.put("tranAmt",amountPriceValue);
		args.put("repaymentType",repaymentType);
		args.put("mercFeeAmt",mercFeeAmt);
		args.put("isInFull",isInFull);
		args.put("repaymentInfo",investInfo);
		args.put("frontMerUrl",GUOConstants.GUO_ACCOUNT_WEB);
		args.put("backgroundMerUrl",GUOConstants.IPS_ACCOUNT_S2S);
		args.put("tranIP",tranIP);
		
		String signValue = "version=["+GUOConstants.VERSION+"]tranCode=["+tranCode+"]"
				+ "merId=["+GUOConstants.MER_ID+"]bidId=["+bidId+"]contractNo=["+contractNo+"]merOrderNum=["+merOrderNum+"]"
						+ "mobilePhone=["+mobilePhone+"]mercFeeAmt=["+mercFeeAmt+"]repaymentType=["+repaymentType+"]tranAmt=["+amountPriceValue+"]"
								+ "isInFull=["+isInFull+"]VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		IPS.repaymentNewTrade(this.platform, this.memberId, this.desPara);
		
		JsonObject remarkJson = new JsonObject();
		remarkJson.addProperty("userId", this.memberId);
		remarkJson.addProperty("pMerBillNo",merOrderNum);
		remarkJson.addProperty("pMemo3",this.desPara.getString("pMemo3"));
		DealDetail.addEvent(memberId, type+200, platform, merOrderNum, frontMerUrl, backgroundMerUrl, remarkJson.toString(), "还款");
		
		Logger.debug("signValue 明文 : "+ signValue+"\n signValue 加密 :  "+GUOUtil.md5(signValue));
		
		args.put("signValue", GUOUtil.md5(signValue));
		
		return args;
	}
	
	/**
	 * 流标(webservice)
	 * @return
	 */
	public  String flowBidByWs(){
		String tranCode = GUOConstants.CMD_FLOWBID;
		
		Map<String, String> maps = new HashMap<String, String>();
		String merOrderNum = this.desPara.getString("pMerBillNo")+this.desPara.getString("pMemo3");
		String bidId = this.desPara.getString("pBidNo");
		String tranDateTime = DateUtil.simple2(new Date());
		String amount = this.extraPara.getString("totalInvestAmount");
		String contractNo = this.desPara.getString("pIpsAcctNo");
		String frontMerUrl = this.desPara.getString("pWebUrl");
		String backgroundMerUrl = this.desPara.getString("pS2SUrl");
		String tranIP = this.extraPara.getString("tranIP");
		maps.put("version",GUOConstants.VERSION);
		maps.put("signType",GUOConstants.SIGN_TYPE);
		maps.put("charset",GUOConstants.CHARSET);
		maps.put("tranCode",tranCode);
		maps.put("merId",GUOConstants.MER_ID);
		maps.put("merName",GUOConstants.MER_NAME);
		maps.put("merOrderNum",merOrderNum);
		maps.put("tranDateTime",tranDateTime);
		maps.put("bidId",bidId);
		maps.put("repaymentAmt",amount );
		maps.put("contractNo",contractNo);
		maps.put("tranIP",tranIP);
		
		String signValue = "version=["+GUOConstants.VERSION+"]tranCode=["+tranCode+"]"
				+ "merId=["+GUOConstants.MER_ID+"]bidId=["+bidId+"]contractNo=["+contractNo+"]merOrderNum=["+merOrderNum+"]"
						+ "repaymentAmt=["+amount+"]tranDateTime=["+tranDateTime+"]"
								+ "VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		DealDetail detail = new DealDetail(platform, memberId, merOrderNum,
				GUOConstants.REGISTER_SUBJECT, Double.parseDouble(amount), false, "流标");
		
		detail.addDealDetail();
		
		String remark = "{\"operation\":\""+this.desPara.getString("pMemo3")+"\","
				+ "\"pOperationType\":\""+this.desPara.getString("pOperationType")+"\"}";
		
		DealDetail.addEvent(memberId, type+200, platform, merOrderNum, frontMerUrl, backgroundMerUrl, remark, "流标");
		
		Logger.debug("signValue 明文 : "+ signValue+"\n signValue 加密 :  "+GUOUtil.md5(signValue));
		
		maps.put("signValue", GUOUtil.md5(signValue));
		
		return http(GUOConstants.GUO_URL,maps);
	}
	
	/**
	 * 查询交易记录
	 * @return
	 */
	public String queryTrade(){
		Map<String,String> maps = new HashMap<String, String>();
		maps.put("version",GUOConstants.VERSION);
		maps.put("signType",GUOConstants.SIGN_TYPE);
		maps.put("charset",GUOConstants.CHARSET);
		maps.put("tranCode", GUOConstants.CMD_QUERY_TRADE);
		String tranIp = this.extraPara.getString("tranIP");
		String merId =  GUOConstants.MER_ID;
		maps.put("merId",merId);
		maps.put("merName", GUOConstants.MER_NAME);
		 String merOrderNum = this.desPara.getString("pMerBillNo");
		maps.put("merOrderNum",merOrderNum);
		String tranDateTime  = DateUtil.simple2(new Date());
		maps.put("tranDateTime", tranDateTime);
		String pTradeType = this.desPara.getString("pTradeType");
		String orgTxnType = getOrgTxnType(pTradeType);
		maps.put("orgTxnType", orgTxnType);
		String orgOrderNum =  this.desPara.getString("pMerBillNo");
		maps.put("orgOrderNum",orgOrderNum);
		GuoOrderDetails orderDetails = new GuoOrderDetails();
		ErrorInfo error = new ErrorInfo();
		t_guo_order_details order = orderDetails.queryOrderByBillNo(orgOrderNum, error);
		String orgtranDateTime = DateUtil.simple2(new Date());
		if(order != null){
			orgtranDateTime = order.tranDateTime;
		}
		
		maps.put("orgtranDateTime", orgtranDateTime);
		maps.put("tranIP", tranIp);
		
		String signValue =  "version=["+GUOConstants.VERSION+"]tranCode=["+GUOConstants.CMD_QUERY_TRADE+"]"
				+ "merId=["+merId+"]merOrderNum=["+merOrderNum+"]"
				+ "tranDateTime=["+tranDateTime+"]orgOrderNum=["+orgOrderNum+"]orgtranDataTime=["+orgtranDateTime+"]"
						+ "VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		maps.put("signValue", Codec.hexMD5(signValue));
		
		String url = "http://mertest.gopay.com.cn/PGServer/Trans/P2pIndex.do";
		return http(url,maps);
	} 
	
	/**
	 * 交易记录查询
	 * @return
	 */
	public String analysisQueryTrade(){
		
		String result = queryTrade();
		Logger.debug("QueryTrade : %s", result);
		if(StringUtils.isBlank(result)){
			return null;
		}
		
		JSONObject json =  (JSONObject) Converter.xmlToObj(result);
		
		if(null == json){
			Logger.info("------------------------json参数解析有误-------------------------------");
			return null;
		}
		
		Set<String> set = json.keySet();
		for(String key : set){
			Logger.debug("%s : %s",key,json.getString(key));
		}
		
		String orgTxnStat = json.getString("orgTxnStat");
		String merCustId="MG00000F";
		String respCode="MG00000F";
		String respDesc="成功";
		String desValue = "";
		String pTradeStatue = "";
		if("0000".equals(orgTxnStat)){  //订单成功
			pTradeStatue ="1";
		}else if("5555".equals(orgTxnStat)){  //订单处理中
			pTradeStatue = "3";
		}else if("9999".equals(orgTxnStat)){  //订单失败
			pTradeStatue = "2";
		}else{  //订单不存在
			pTradeStatue = "4";
		}
		
		Set<String> desSet = this.desPara.keySet();
		JsonObject desJson = new JsonObject();
		for(String key : desSet ){
			desJson.addProperty(key, this.desPara.getString(key));
		}
		desJson.addProperty("pTradeStatue", pTradeStatue);
		desValue = buildP3DesXmlPara(desJson);
		String pSign = Codec.hexMD5(merCustId+respCode+respDesc+desValue+Constants.ENCRYPTION_KEY);
		JsonObject returnJson = new JsonObject();
		returnJson.addProperty("pMerCode", merCustId);
		returnJson.addProperty("pErrCode",respCode);
		returnJson.addProperty("pErrMsg", respDesc);
		returnJson.addProperty("p3DesXmlPara", desValue);
		returnJson.addProperty("pSign", pSign);
		
		return returnJson.toString();
	}
	
	
	private String getOrgTxnType(String queryTransType){
		String orgTxnType = null;
		if("02".equals(queryTransType)){  //标的登记
			orgTxnType = GUOConstants.CMD_CREATEBID;
		}else if("03".equals(queryTransType)){  //登记债权人接口
			orgTxnType = GUOConstants.CMD_INVEST;
		}else if("05".equals(queryTransType)){  //登记债权转让接口
			//不支持
		}else if("08".equals(queryTransType)){  //充值
			orgTxnType = GUOConstants.CMD_RECHARGE;
		}else if("11".equals(queryTransType)){  //还款
			orgTxnType = GUOConstants.CMD_REPAYMENT;
		}else if("09".equals(queryTransType)){  //提现
			orgTxnType = GUOConstants.CMD_WITHDRAWAL;
		}else if("14".equals(queryTransType)){  //转账-1放款(WS)
			orgTxnType = GUOConstants.CMD_FINISHBID;
		}
		
		return orgTxnType;
	}
	
	/**
	 * http请求
	 * @param url
	 * @param params
	 * @return
	 */
	private String http(String url,Map<String,String> params){
		Logger.debug("------------------http start-------------------");
		printParams(params); //打印DEBUG级别日志
		String tranCode = params.get("tranCode");
		String cmdName = CommonUtils.getTypeName(tranCode);
		Logger.info("-----------------[%s][%s]preSubmit>ReqestParams:%s-----------------",tranCode,cmdName
				,params.toString());
		
		
		
		String result = null;
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        Set<Entry<String, String>> entrySet = params.entrySet();
		
		if (entrySet != null) {
			
				for (Entry<String, String> e : entrySet) {
					nvps.add(new BasicNameValuePair(e.getKey(), e.getValue()));
				}
				
		}
        CloseableHttpClient httpclient = HttpClients.createDefault();
        EntityBuilder builder = EntityBuilder.create();
        
        try {
        	
            HttpPost httpPost = new HttpPost(url);
            builder.setParameters(nvps);
            httpPost.setEntity(builder.build());
            CloseableHttpResponse response = null;
            
			try {
				
				response = httpclient.execute(httpPost);
				
			} catch (ClientProtocolException e) {

				Logger.error("HttpClien : %s", e.getMessage());
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

						Logger.error("HttpClien : %s", e.getMessage());
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

					Logger.error("流关闭时 : %s", e.getMessage());
				}
            }
        } finally {
            try {
				httpclient.close();
			} catch (IOException e) {

				Logger.error("流关闭时 : %s", e.getMessage());
			}
        }
        Logger.info("RespResult: %s", result);
        return result;
	}
	
	/**
	 * 开户回调
	 * @return
	 */
	public static Map<String, String> createAccountCB(String version, String tranCode, String p2pUserId, String virCardNo,
			String merOrderNum, String tranDateTime, String signValue, String respCode, String msgExt, String merId,
			String merName, String contractNo, String mobilePhone, String customerId, String tranIP, String backgroundMerUrl) {
		String signValueMine = "version=["+version+"]tranCode=["+tranCode+"]respCode=["+respCode+"]"
				+ "merId=["+merId+"]merOrderNum=["+merOrderNum+"]tranDateTime=["+tranDateTime+"]"
						+ "customerId=["+customerId+"]tranIP=["+tranIP+"]"
								+ "VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		if(signValue == null || !signValue.equals(GUOUtil.md5(signValueMine))) {
			return null;
		}
		
		String pErrCode = "";
		String pErrMsg = "";
		
		
		JSONObject pDesJson = new JSONObject();
		pDesJson.put("pMerBillNo", merOrderNum);
		String pStatus = "10";
		t_member_events event = DealDetail.updateEvent(merOrderNum, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		if("0000".equals(respCode)) {
			pErrCode = "MG00000F";
			pErrMsg = "开户成功";
		}else {
			pErrCode = "MG00001F";
			pErrMsg = "开户失败";
			pStatus = "-1";
		}
		pDesJson.put("pStatus", pStatus);
		pDesJson.put("pIpsAcctNo", contractNo);
		
		Logger.info("pDesJson.toString() %s ", pDesJson.toString());
		JsonObject remark = new JsonParser().parse(event.remark).getAsJsonObject();
		Set<Entry<String, JsonElement>> set = remark.entrySet();
		for(Entry<String, JsonElement> entry : set){
			pDesJson.put(entry.getKey(), entry.getValue().getAsString());
		}
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(pDesJson.toString(), "pReq", null, null, null),Constants.ENCRYPTION_KEY);
		Map<String, String> args = new HashMap<String, String>();
		
		args.put("frontUrl", event.front_url);
		args.put("backgroundUrl", event.background_url);
		args.put("pMerCode", "");
		args.put("pErrCode", pErrCode);
		args.put("pErrMsg", pErrMsg);
		args.put("p3DesXmlPara", p3DesXmlPara);
		args.put("pSign", Encrypt.MD5(""+pErrCode+pErrMsg+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		return args;
	}
	
	/**
	 * 充值回调
	 * @return
	 */
	public static Map<String, String> rechargeCB(String version, String charset, String signType, String tranCode,
			String merOrderNum, String tranDateTime, String signValue, String respCode, String msgExt, String merId,
			String merName, String contractNo, String mobilePhone, String payType, String tranAmt, String feeAmt,
			String feePayer, String frontMerUrl, String backgroundMerUrl) {
		String signValueMine = "version=["+version+"]tranCode=["+tranCode+"]respCode=["+respCode+"]"
				+ "merId=["+merId+"]contractNo=["+contractNo+"]merOrderNum=["+merOrderNum+"]mobilePhone=["+mobilePhone+"]"
						+ "tranAmt=["+tranAmt+"]tranDateTime=["+tranDateTime+"]"
								+ "VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		if(signValue == null || !signValue.equals(GUOUtil.md5(signValueMine))) {
			return null;
		}
		
		String pErrCode = "";
		String pErrMsg = "";
		
		
		
		if("0000".equals(respCode)) {
			pErrCode = "MG00000F";
			pErrMsg = "充值成功";
		}else {
			pErrCode = "MG00001F";
			pErrMsg = "充值失败";
		}
		
		t_member_events event = DealDetail.updateEvent(merOrderNum, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		if(null == event) {
			return null;
		}
		
		JSONObject pDesJson = new JSONObject();
		pDesJson.put("pMerBillNo", merOrderNum);
		pDesJson.put("pMemo1", JSONObject.fromObject(event.remark).getString("userId"));
		pDesJson.put("pTrdAmt", tranAmt);
		
		String pDesXml = Converter.jsonToXml(pDesJson.toString(), "pReq", null, null, null);
		String p3DesXmlPara = Encrypt.encrypt3DES(pDesXml,Constants.ENCRYPTION_KEY);
		
		Map<String, String> args = new HashMap<String, String>();
		
		args.put("frontUrl", event.front_url);
		args.put("backgroundUrl", event.background_url);
		args.put("pMerCode", "");
		args.put("pErrCode", pErrCode);
		args.put("pErrMsg", pErrMsg);
		args.put("p3DesXmlPara", p3DesXmlPara);
		args.put("pSign", Encrypt.MD5(""+pErrCode+pErrMsg+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		return args;
	}
	
	/**
	 * 提现回调
	 * @return
	 */
	public static Map<String, String> withdrawalCB(String version, String charset, String signType, String tranCode,
			String merOrderNum, String tranDateTime, String signValue, String respCode, String msgExt, String merId,
			String merName, String contractNo, String mobilePhone, String tranAmt, String mercFeeAm, String feeAmt, 
			String feePayer, String frontMerUrl, String backgroundMerUrl) {
		String signValueMine = "version=["+version+"]tranCode=["+tranCode+"]"+"respCode=["+respCode+"]"
				+ "merId=["+merId+"]contractNo=["+contractNo+"]merOrderNum=["+merOrderNum+"]mobilePhone=["+mobilePhone+"]"
						+ "tranAmt=["+tranAmt+"]tranDateTime=["+tranDateTime+"]"
								+ "VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		Logger.info("signValueMine : %s"+ signValueMine+"\n--respCode--"+respCode+"\n--msgExt--"+ msgExt+"\n--手续费feePayer--"+ feePayer);
		
		if(signValue == null || !signValue.equals(GUOUtil.md5(signValueMine))) {
			return null;
		}
		
		String pErrCode = "";
		String pErrMsg = "";
		
		if("0000".equals(respCode)) {
			pErrCode = "MG00000F";
			pErrMsg = "提现成功";
		}else if("AAAA".equals(respCode)){
			pErrCode = "MG00010F";
			pErrMsg = "申请成功(待审核)";
		}
		else{
			pErrCode = "MG00001F";
			pErrMsg = "提现失败";
		}
		
		t_member_events event = DealDetail.updateEvent(merOrderNum, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		if(null == event) {
			return null;
		}
		
		JSONObject pDesJson = new JSONObject();
		pDesJson.put("pMerBillNo", merOrderNum);
		JsonObject remarkJson = new JsonParser().parse(event.remark).getAsJsonObject();
		
		pDesJson.put("pMemo1", remarkJson.get("userId").getAsString());
		pDesJson.put("pMemo3", remarkJson.get("pMemo3").getAsString());
		pDesJson.put("tranAmt", tranAmt);
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(pDesJson.toString(), "pReq", null, null, null),Constants.ENCRYPTION_KEY);
		
		Map<String, String> args = new HashMap<String, String>();
		
		args.put("frontUrl", event.front_url);
		args.put("backgroundUrl", event.background_url);
		args.put("pMerCode", "");
		args.put("pErrCode", pErrCode);
		args.put("pErrMsg", pErrMsg);
		args.put("p3DesXmlPara", p3DesXmlPara);
		args.put("pSign", Encrypt.MD5(""+pErrCode+pErrMsg+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		return args;
	}
	
	/**
	 * 发标回调
	 * @return
	 */
	public static Map<String, String> createBidCB(String infoXml) {
		if(StringUtils.isBlank(infoXml)){
			return null;
		}
		
		JSONObject info = (JSONObject)Converter.xmlToObj(infoXml);
		String signValueMine = "version=["+info.getString("version")+"]tranCode=["+info.getString("tranCode")+"]"
				+ "respCode=["+info.getString("respCode")+"]merId=["+info.getString("merId")+"]bidId=["+info.getString("bidId")+"]"
						+ "contractNo=["+info.getString("contractNo")+"]merOrderNum=["+info.getString("merOrderNum")+"]"
								+ "mobilePhone=["+info.getString("mobilePhone")+"]"
						+ "borrowingBalance=["+info.getString("borrowingBalance")+"]tranDateTime=["+info.getString("tranDateTime")+"]"
								+ "VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		if(info.getString("signValue") == null || !info.getString("signValue").equals(GUOUtil.md5(signValueMine))) {
			return null;
		}
		
		t_guo_order_details details = new t_guo_order_details();
		details.setVersion(info.getString("version"));
		details.setTranCode(info.getString("tranCode"));
		details.setRespCode(info.getString("respCode"));
		details.setMerId(info.getString("merId"));
		details.setBidId(info.getString("bidId"));
		details.setContractNo(info.getString("contractNo"));
		details.setMerOrderNum(info.getString("merOrderNum"));
		details.setMobilePhone(info.getString("mobilePhone"));
		details.setTranDateTime(info.getString("tranDateTime"));
		try{
		
		details.save();
		
		}catch(Exception e){
			Logger.error("发标回调保存记录信息时:%s", e.getMessage());
			
		}
		
		String pErrCode = "";
		String pErrMsg = "";
		
		if("0000".equals(info.getString("respCode"))) {
			pErrCode = "MG00000F";
			pErrMsg = "发标成功";
		}else {
			pErrCode = "MG00001F";
			pErrMsg = info.getString("msgExt");
		}
		
		t_member_events event = DealDetail.updateEvent(info.getString("merOrderNum"), "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		if(null == event) {
			return null;
		}
		
		JSONObject remark = JSONObject.fromObject(event.remark);
		
		JSONObject pDesJson = new JSONObject();
		pDesJson.put("pMerBillNo", info.getString("merOrderNum"));
		pDesJson.put("pIpsBillNo", info.getString("merOrderNum"));
		pDesJson.put("pBidNo", info.getString("bidId"));
		pDesJson.put("pOperationType", "1");//新增
		pDesJson.put("pMemo1", remark.getString("userId"));
		pDesJson.put("pMemo3", remark.getString("operation"));
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(pDesJson.toString(), "pReq", null, null, null),Constants.ENCRYPTION_KEY);
		
		Map<String, String> args = new HashMap<String, String>();
		
		args.put("frontUrl", event.front_url);
		args.put("backgroundUrl", event.background_url);
		args.put("pMerCode", "");
		args.put("pErrCode", pErrCode);
		args.put("pErrMsg", pErrMsg);
		args.put("p3DesXmlPara", p3DesXmlPara);
		args.put("pSign", Encrypt.MD5(""+pErrCode+pErrMsg+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		return args;
	}
	
	/**
	 * 投标回调
	 * @return
	 */
	public static Map<String, String> investCB(String version, String tranCode, String signType, String merId,
			String merName, String contractNo, String bidId, String mobilePhone, String merOrderNum, 
			 String tranAmt, String extantAmt, String tranDateTime, String tranIP, String respCode, String msgExt, 
			   String orderId, String tranFinishTime, String frontMerUrl, String backgroundMerUrl, String feeAmt,
			   String mercFeeAmt, String payType, String bankPayAmt, String vcardPayAmt, String curBal, String signValue) {
		String signValueMine = "version=["+version+"]tranCode=["+tranCode+"]respCode=["+respCode+"]"
				+ "merId=["+merId+"]bidId=["+bidId+"]contractNo=["+contractNo+"]merOrderNum=["+merOrderNum+"]mobilePhone=["+mobilePhone+"]"
						+ "tranAmt=["+tranAmt+"]tranDateTime=["+tranDateTime+"]"
								+ "VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		if(signValue == null || !signValue.equals(GUOUtil.md5(signValueMine))) {
			Logger.info("-------------校验失败------------");
			return null;
		}
		
		String pErrCode = "";
		String pErrMsg = "";
		
		if("0000".equals(respCode)) {
			pErrCode = "MG00000F";
			pErrMsg = "投标成功";
		}else {
			pErrCode = "MG00001F";
			pErrMsg = "投标失败";
		}
		
		t_member_events event = DealDetail.updateEvent(merOrderNum, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		if(null == event) {
			return null;
		}
		
		JSONObject remark = JSONObject.fromObject(event.remark);
		
		JSONObject pDesJson = new JSONObject();
		pDesJson.put("pMerBillNo", merOrderNum);
		pDesJson.put("pFee", remark.getString("pFee"));
		pDesJson.put("pMemo1", remark.getString("userId"));
		pDesJson.put("pP2PBillNo", merOrderNum);
		pDesJson.put("tranAmt", tranAmt);  //用户实际的投标金额用户实际的投标金额+留存金额=用户上送的交易金额
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(pDesJson.toString(), "pReq", null, null, null),Constants.ENCRYPTION_KEY);
		
		Map<String, String> args = new HashMap<String, String>();
		
		args.put("frontUrl", event.front_url);
		args.put("backgroundUrl", event.background_url);
		args.put("pMerCode", "");
		args.put("pErrCode", pErrCode);
		args.put("pErrMsg", pErrMsg);
		args.put("p3DesXmlPara", p3DesXmlPara);
		args.put("pSign", Encrypt.MD5(""+pErrCode+pErrMsg+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		return args;
	}
	
	/**
	 * 投标完成回调
	 * @return
	 */
	public static String finishBidCB(String infoXml) {
		JSONObject info = (JSONObject)Converter.xmlToObj(infoXml);
		
		if(null == info ){
			Logger.info("------------------------info参数解析有误，请重试-------------------------------");
			return null;
		}
		
		String signValueMine = "version=["+info.getString("version")+"]tranCode=["+info.getString("tranCode")+"]respCode=["+info.getString("respCode")+"]"
				+ "merId=["+info.getString("merId")+"]bidId=["+info.getString("bidId")+"]contractNo=["+info.getString("contractNo")+"]merOrderNum=["+info.getString("merOrderNum")+"]tranAmt=["+info.getString("tranAmt")+"]"
						+ "mercFeeAmt=["+info.getString("mercFeeAmt")+"]gopayFeeAmt=["+(info.getString("gopayFeeAmt").equals("[]") ? "" : info.getString("gopayFeeAmt"))+"]tranDateTime=["+info.getString("tranDateTime")+"]"
								+ "VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		if(info.getString("signValue") == null || !info.getString("signValue").equals(GUOUtil.md5(signValueMine))) {
			Logger.info("-------------校验失败------------");
			return null;
		}
		
		
		t_guo_order_details details = new t_guo_order_details();
		details.setVersion(info.getString("version"));
		details.setTranCode(info.getString("tranCode"));
		details.setRespCode(info.getString("respCode"));
		details.setMerId(info.getString("merId"));
		details.setBidId(info.getString("bidId"));
		details.setContractNo(info.getString("contractNo"));
		details.setMerOrderNum(info.getString("merOrderNum"));
		details.setTranAmt(info.getString("tranAmt"));
		details.setMercFeeAmt(info.getString("mercFeeAmt"));
		details.setTranDateTime(info.getString("tranDateTime"));
		try{
			
		
		details.save();
		
		}catch(Exception e){
			Logger.error("发标回调保存记录信息时:%s", e.getMessage());
			
		}
		
		String pErrCode = "";
		String pErrMsg = "";
		
		if("0000".equals(info.getString("respCode"))) {
			pErrCode = "MG00000F";
			pErrMsg = "投标完成成功";
		}else {
			pErrCode = "MG00001F";
			pErrMsg = "投标完成失败";
		}
		
		t_member_events event = DealDetail.updateEvent(info.getString("merOrderNum"), "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		if(null == event) {
			return null;
		}
		JsonObject remark = new JsonParser().parse(event.remark).getAsJsonObject();
		
		JSONObject pDesJson = new JSONObject();
		pDesJson.put("pMerBillNo", info.getString("merOrderNum"));
		pDesJson.put("pTransferType", "1");//转账（放款）
		pDesJson.put("pMemo1", remark.get("pMemo1").getAsString());
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(pDesJson.toString(), "pReq", null, null, null),Constants.ENCRYPTION_KEY);
		
		JSONObject args = new JSONObject();
		
		args.put("backgroundUrl", event.background_url);
		args.put("pMerCode", "");
		args.put("pErrCode", pErrCode);
		args.put("pErrMsg", pErrMsg);
		args.put("p3DesXmlPara", p3DesXmlPara);
		args.put("pSign", Encrypt.MD5(""+pErrCode+pErrMsg+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		return args.toString();
	}
	
	/**
	 * 流标回调
	 * @return
	 */
	public static Map<String, String> flowBidCB(String infoXml) {
		Logger.info("infoXml : %s", infoXml);
		JSONObject info = (JSONObject)Converter.xmlToObj(infoXml);
		
		if(StringUtils.isBlank(infoXml)){
			return null;
		}
		
		String signValueMine = "version=["+info.getString("version")+"]tranCode=["+info.getString("tranCode")+"]respCode=["+info.getString("respCode")+"]"
				+ "merId=["+info.getString("merId")+"]bidId=["+info.getString("bidId")+"]contractNo=["+info.getString("contractNo")+"]merOrderNum=["+info.getString("merOrderNum")+"]repaymentAmt=["+info.getString("repaymentAmt")+"]"
						+ "tranDateTime=["+info.getString("tranDateTime")+"]"
								+ "VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		if(info.getString("signValue") == null || !info.getString("signValue").equals(GUOUtil.md5(signValueMine))) {
			Logger.info("-------------校验失败------------");
			return null;
		}
		
		String pErrCode = "";
		String pErrMsg = "";
		
		if("0000".equals(info.getString("respCode"))) {
			pErrCode = "MG00000F"; //标的结束
			pErrMsg = "流标成功";
		}else {
			pErrCode = "MG00001F";
			pErrMsg = "流标失败";
		}
		
		t_member_events event = DealDetail.updateEvent(info.getString("merOrderNum"), "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		if(null == event) {
			return null;
		}
		
		JSONObject remark = JSONObject.fromObject(event.remark);
		
		JSONObject pDesJson = new JSONObject();
		pDesJson.put("pMerBillNo", info.getString("merOrderNum"));
		pDesJson.put("pIpsBillNo", info.getString("merOrderNum"));
		pDesJson.put("pBidNo", info.getString("bidId"));
		pDesJson.put("pOperationType", remark.getString("pOperationType"));
		pDesJson.put("pMemo3", remark.getString("operation"));
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(pDesJson.toString(), "pReq", null, null, null),Constants.ENCRYPTION_KEY);
		
		Map<String, String> args = new HashMap<String, String>();
		
		args.put("frontUrl", event.front_url);
		args.put("backgroundUrl", event.background_url);
		args.put("pMerCode", "");
		args.put("pErrCode", pErrCode);
		args.put("pErrMsg", pErrMsg);
		args.put("p3DesXmlPara", p3DesXmlPara);
		args.put("pSign", Encrypt.MD5(""+pErrCode+pErrMsg+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		
		return args;
	}
	
	/**
	 * 还款回调
	 * @return
	 */
	public static Map<String, String> repaymentCB(String version, String charset, String signType, String tranCode, String merOrderNum, 
			String tranDateTime, String signValue, String respCode, String msgExt, String merId, String merName, String contractNo, String mobilePhone, 
			String bid, String tranAmt, String repaymentType, String mercFeeAmt, String isInFull, String repaymentInfo, String repaymentChargeFeeAmt, 
			   String repaymentChargeFeePayer, String payType, String bankPayAmt, String vcardPayAmt, String curBal) {
		
		String signValueMine = "version=["+version+"]tranCode=["+tranCode+"]respCode=["+respCode+"]"
				+ "merId=["+merId+"]bidId=["+bid+"]contractNo=["+contractNo+"]merOrderNum=["+merOrderNum+"]mobilePhone=["+mobilePhone+"]"
				+ "mercFeeAmt=["+mercFeeAmt+"]repaymentType=["+repaymentType+"]tranAmt=["+tranAmt+"]"
						+ "isInFull=["+isInFull+"]VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		if(signValue == null || !signValue.equals(GUOUtil.md5(signValueMine))) {
			Logger.info("-------------校验失败------------");
			return null;
		}
		
		String pErrCode = "";
		String pErrMsg = "";
		
		if("0000".equals(respCode)) {
			pErrCode = "MG00000F";
			pErrMsg = "还款成功";
		}else {
			pErrCode = "MG00001F";
			pErrMsg = "还款失败";
		}
		
		t_member_events event = DealDetail.updateEvent(merOrderNum, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		if(null == event) {
			return null;
		}
		
		JSONObject remark = JSONObject.fromObject(event.remark);
		
		JSONObject pDesJson = new JSONObject();
		
		pDesJson.put("pMerBillNo", remark.get("pMerBillNo"));
		pDesJson.put("pMemo1", remark.get("userId"));
		pDesJson.put("pMemo3", remark.get("pMemo3"));
		
		String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(pDesJson.toString(), "pReq", null, null, null),Constants.ENCRYPTION_KEY);
		
		Map<String, String> args = new HashMap<String, String>();
		
		args.put("frontUrl", event.front_url);
		args.put("backgroundUrl", event.background_url);
		args.put("pMerCode", "");
		args.put("pErrCode", pErrCode);
		args.put("pErrMsg", pErrMsg);
		args.put("p3DesXmlPara", p3DesXmlPara);
		args.put("pSign", Encrypt.MD5(""+pErrCode+pErrMsg+p3DesXmlPara+Constants.ENCRYPTION_KEY));
		return args;
	}
	
	public static void printParams(Map<String,String> maps){
		Logger.debug("------------------printParams-------------------");
		Set<Entry<String, String>> set = maps.entrySet();
		for(Entry<String, String> entry : set){
			Logger.debug("%s : %s", entry.getKey(),entry.getValue());
		}
		Logger.debug("------------------printParams-------------------");
	}
	
	/**
	 * 构造buildP3DesXmlPara
	 * 
	 * @param jsonParams
	 * @return
	 */
	private static String buildP3DesXmlPara(JsonObject jsonParams) {
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
}
