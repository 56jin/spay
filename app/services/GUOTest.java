package services;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import utils.DateUtil;
import utils.GUOUtil;
import constants.GUOConstants;

public class GUOTest {

	public static Map<String, String> entrance() {
		
		//把请求参数打包成数组 按照接口文档
		Map<String, String> args = new HashMap<String, String>();
		String merOrderNum = DateUtil.simple2(new Date());
		String tranDateTime = DateUtil.simple2(new Date());
		args.put("version",GUOConstants.VERSION);
		args.put("signType",GUOConstants.SIGN_TYPE);
		args.put("charset",GUOConstants.CHARSET);
		args.put("tranCode",GUOConstants.METHOD_TYPE[7]);
		args.put("merId",GUOConstants.MER_ID);
		args.put("merName",GUOConstants.MER_NAME);
		args.put("merOrderNum",merOrderNum);
		args.put("tranDateTime",tranDateTime);
		args.put("customerId","test");
		args.put("mobilePhone","13570808851");
		args.put("backgroundMerUrl",GUOConstants.GUO_ACCOUNT_WEB);
		args.put("tranIP","127.0.0.1");
		
		String signValue = "version=["+GUOConstants.VERSION+"]tranCode=["+GUOConstants.METHOD_TYPE[7]+"]"
				+ "merId=["+GUOConstants.MER_ID+"]merOrderNum=["+merOrderNum+"]"
						+ "tranDateTime=["+tranDateTime+"]customerId=[test]"
								+ "tranIP=[127.0.0.1]VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		
		args.put("signValue", GUOUtil.md5(signValue));
		
		return args;
	}
	
	public static Map<String, String> login() {
		//把请求参数打包成数组 按照接口文档
		Map<String, String> args = new HashMap<String, String>();
		String merOrderNum = DateUtil.simple2(new Date());
		String tranDateTime = DateUtil.simple2(new Date());
		args.put("version",GUOConstants.VERSION);
		args.put("signType",GUOConstants.SIGN_TYPE);
		args.put("charset",GUOConstants.CHARSET);
		args.put("tranCode",GUOConstants.METHOD_TYPE[8]);
		args.put("merId",GUOConstants.MER_ID);
		args.put("merName",GUOConstants.MER_NAME);
		args.put("merOrderNum",merOrderNum);
		args.put("tranDateTime",tranDateTime);
		args.put("contractNo",GUOConstants.CONTRACT_NO);
		args.put("mobilePhone",GUOConstants.MOBILE);
		args.put("frontMerUrl",GUOConstants.GUO_ACCOUNT_WEB_LOGIN);
		args.put("tranIP","127.0.0.1");
		
		String signValue = "version=["+GUOConstants.VERSION+"]tranCode=["+GUOConstants.METHOD_TYPE[8]+"]"
				+ "merId=["+GUOConstants.MER_ID+"]contractNo=["+GUOConstants.CONTRACT_NO+"]"
						+ "merOrderNum=["+merOrderNum+"]mobilePhone=["+GUOConstants.MOBILE+"]"
								+ "tranDateTime=["+tranDateTime+"]VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		
		args.put("signValue", GUOUtil.md5(signValue));
		
		return args;
	}
	
	public static Map<String, String> queryAccount() {
		//把请求参数打包成数组 按照接口文档
		Map<String, String> args = new HashMap<String, String>();
		String merOrderNum = DateUtil.simple2(new Date());
		String tranDateTime = DateUtil.simple2(new Date());
		args.put("version",GUOConstants.VERSION);
		args.put("signType",GUOConstants.SIGN_TYPE);
		args.put("charset",GUOConstants.CHARSET);
		args.put("tranCode",GUOConstants.METHOD_TYPE[6]);
		args.put("merId",GUOConstants.MER_ID);
		args.put("merName",GUOConstants.MER_NAME);
		args.put("merOrderNum",merOrderNum);
		args.put("tranDateTime",tranDateTime);
		args.put("contractNo",GUOConstants.CONTRACT_NO);
		args.put("mobilePhone",GUOConstants.MOBILE);
		args.put("tranIP","127.0.0.1");
		
		String signValue = "version=["+GUOConstants.VERSION+"]tranCode=["+GUOConstants.METHOD_TYPE[6]+"]"
				+ "merId=["+GUOConstants.MER_ID+"]merOrderNum=["+merOrderNum+"]"
						+ "mobilePhone=["+GUOConstants.MOBILE+"]contractNo=["+GUOConstants.CONTRACT_NO+"]"
								+ "VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		
		args.put("signValue", GUOUtil.md5(signValue));
		
		return args;
	}
	
	/**
	 * 充值
	 * @return
	 */
	public static Map<String, String> recharge() {
		//把请求参数打包成数组 按照接口文档
		Map<String, String> args = new HashMap<String, String>();
		String merOrderNum = DateUtil.simple2(new Date());
		String tranDateTime = DateUtil.simple2(new Date());
		String amount = String.format("%.2f", 1000f);
		args.put("version",GUOConstants.VERSION);
		args.put("signType",GUOConstants.SIGN_TYPE);
		args.put("charset",GUOConstants.CHARSET);
		args.put("tranCode",GUOConstants.METHOD_TYPE2[9]);
		args.put("merId",GUOConstants.MER_ID);
		args.put("merName",GUOConstants.MER_NAME);
		args.put("merOrderNum",merOrderNum);
		args.put("tranDateTime",tranDateTime);
		args.put("tranAmt",amount);
		args.put("contractNo",GUOConstants.CONTRACT_NO);
		args.put("mobilePhone",GUOConstants.MOBILE);
		args.put("frontMerUrl",GUOConstants.GUO_ACCOUNT_WEB);
		args.put("backgroundMerUrl",GUOConstants.IPS_ACCOUNT_S2S);
		args.put("tranIP","127.0.0.1");
		
		String signValue = "version=["+GUOConstants.VERSION+"]tranCode=["+GUOConstants.METHOD_TYPE2[9]+"]"
				+ "merId=["+GUOConstants.MER_ID+"]contractNo=["+GUOConstants.CONTRACT_NO+"]merOrderNum=["+merOrderNum+"]mobilePhone=["+GUOConstants.MOBILE+"]"
						+ "tranAmt=["+amount+"]tranDateTime=["+tranDateTime+"]"
								+ "VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		
		args.put("signValue", GUOUtil.md5(signValue));
		
		return args;
	}
	
	/**
	 * 提现
	 * @return
	 */
	public static Map<String, String> withdrawal() {
		//把请求参数打包成数组 按照接口文档
		Map<String, String> args = new HashMap<String, String>();
		String merOrderNum = DateUtil.simple2(new Date());
		String tranDateTime = DateUtil.simple2(new Date());
		String amount = String.format("%.2f", 1000f);
		String feeAmount = String.format("%.2f", 2f);
		args.put("version",GUOConstants.VERSION);
		args.put("signType",GUOConstants.SIGN_TYPE);
		args.put("charset",GUOConstants.CHARSET);
		args.put("tranCode",GUOConstants.METHOD_TYPE2[10]);
		args.put("merId",GUOConstants.MER_ID);
		args.put("merName",GUOConstants.MER_NAME);
		args.put("merOrderNum",merOrderNum);
		args.put("tranDateTime",tranDateTime);
		args.put("tranAmt",amount);
		args.put("mercFeeAmt",feeAmount);
		args.put("contractNo",GUOConstants.CONTRACT_NO);
		args.put("mobilePhone",GUOConstants.MOBILE);
		args.put("frontMerUrl",GUOConstants.GUO_ACCOUNT_WEB_LOGIN);
		args.put("backgroundMerUrl",GUOConstants.GUO_ACCOUNT_WEB);
		args.put("tranIP","127.0.0.1");
		
		String signValue = "version=["+GUOConstants.VERSION+"]tranCode=["+GUOConstants.METHOD_TYPE2[10]+"]"
				+ "merId=["+GUOConstants.MER_ID+"]contractNo=["+GUOConstants.CONTRACT_NO+"]merOrderNum=["+merOrderNum+"]mobilePhone=["+GUOConstants.MOBILE+"]"
						+ "tranAmt=["+amount+"]tranDateTime=["+tranDateTime+"]"
								+ "VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		
		args.put("signValue", GUOUtil.md5(signValue));
		
		return args;
	}
	
	/**
	 * 投标
	 * @return
	 */
	public static Map<String, String> invest() {
		//把请求参数打包成数组 按照接口文档
		Map<String, String> args = new HashMap<String, String>();
		String merOrderNum = DateUtil.simple2(new Date());
		String tranDateTime = DateUtil.simple2(new Date());
		String bidId = "bid20141129100857";
		String amount = String.format("%.2f", 900f);
		args.put("version",GUOConstants.VERSION);
		args.put("signType",GUOConstants.SIGN_TYPE);
		args.put("charset",GUOConstants.CHARSET);
		args.put("tranCode",GUOConstants.METHOD_TYPE[2]);
		args.put("merId",GUOConstants.MER_ID);
		args.put("merName",GUOConstants.MER_NAME);
		args.put("merOrderNum",merOrderNum);
		args.put("tranDateTime",tranDateTime);
		args.put("tranAmt",amount);
		args.put("bidId",bidId);
		args.put("contractNo",GUOConstants.CONTRACT_NO);
		args.put("investContractNo",GUOConstants.CONTRACT_NO2);
		args.put("mobilePhone",GUOConstants.MOBILE2);
		args.put("frontMerUrl",GUOConstants.GUO_ACCOUNT_WEB_LOGIN);
		args.put("backgroundMerUrl",GUOConstants.GUO_ACCOUNT_WEB);
		args.put("tranIP","127.0.0.1");
		
		String signValue = "version=["+GUOConstants.VERSION+"]tranCode=["+GUOConstants.METHOD_TYPE[2]+"]"
				+ "merId=["+GUOConstants.MER_ID+"]bidId=["+bidId+"]contractNo=["+GUOConstants.CONTRACT_NO+"]merOrderNum=["+merOrderNum+"]mobilePhone=["+GUOConstants.MOBILE2+"]"
						+ "tranAmt=["+amount+"]tranDateTime=["+tranDateTime+"]"
								+ "VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		
		args.put("signValue", GUOUtil.md5(signValue));
		
		return args;
	}
	
	/**
	 * 发标
	 * @return
	 */
	public static Map<String, String> createBid() {
		//把请求参数打包成数组 按照接口文档
		Map<String, String> args = new HashMap<String, String>();
		String merOrderNum = DateUtil.simple2(new Date());
		String tranDateTime = DateUtil.simple2(new Date());
		String bidId = "bid"+DateUtil.simple2(new Date());
		String amount = String.format("%.2f", 100f);
		String rate = String.format("%.2f", 10f);
		String borrowingDeadLine = "20151129094626";
		args.put("version",GUOConstants.VERSION);
		args.put("signType",GUOConstants.SIGN_TYPE);
		args.put("charset",GUOConstants.CHARSET);
		args.put("tranCode",GUOConstants.METHOD_TYPE[1]);
		args.put("merId",GUOConstants.MER_ID);
		args.put("merName",GUOConstants.MER_NAME);
		args.put("merOrderNum",merOrderNum);
		args.put("tranDateTime",tranDateTime);
		args.put("tranAmt",amount);
		args.put("bidId",bidId);
		args.put("contractNo",GUOConstants.CONTRACT_NO);
		args.put("mobilePhone",GUOConstants.MOBILE);
		args.put("customerName",GUOConstants.P2P_NAME);
		args.put("borrowingBalance",amount);
		args.put("interestRate",rate);
		args.put("borrowingDeadLine",borrowingDeadLine);
		args.put("repaymentType",GUOConstants.REPAYMENT_TYPE);
		args.put("tranIP","127.0.0.1");
		
		String signValue = "version=["+GUOConstants.VERSION+"]tranCode=["+GUOConstants.METHOD_TYPE[1]+"]"
				+ "merId=["+GUOConstants.MER_ID+"]bidId=["+bidId+"]contractNo=["+GUOConstants.CONTRACT_NO+"]merOrderNum=["+merOrderNum+"]mobilePhone=["+GUOConstants.MOBILE+"]"
						+ "borrowingBalance=["+amount+"]tranDateTime=["+tranDateTime+"]"
								+ "VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		
		args.put("signValue", GUOUtil.md5(signValue));
		
		return args;
	}
	
	/**
	 * 流标
	 * @return
	 */
	public static Map<String, String> flowBid() {
		//把请求参数打包成数组 按照接口文档
		Map<String, String> args = new HashMap<String, String>();
		String merOrderNum = DateUtil.simple2(new Date());
		String tranDateTime = DateUtil.simple2(new Date());
		String bidId = "bid20141129103735";
		String amount = String.format("%.2f", 0f);
		args.put("version",GUOConstants.VERSION);
		args.put("signType",GUOConstants.SIGN_TYPE);
		args.put("charset",GUOConstants.CHARSET);
		args.put("tranCode",GUOConstants.METHOD_TYPE[3]);
		args.put("merId",GUOConstants.MER_ID);
		args.put("merName",GUOConstants.MER_NAME);
		args.put("merOrderNum",merOrderNum);
		args.put("tranDateTime",tranDateTime);
		args.put("tranAmt",amount);
		args.put("bidId",bidId);
		args.put("repaymentAmt",amount);
		args.put("contractNo",GUOConstants.CONTRACT_NO);
		args.put("tranIP","127.0.0.1");
		
		String signValue = "version=["+GUOConstants.VERSION+"]tranCode=["+GUOConstants.METHOD_TYPE[3]+"]"
				+ "merId=["+GUOConstants.MER_ID+"]bidId=["+bidId+"]contractNo=["+GUOConstants.CONTRACT_NO+"]merOrderNum=["+merOrderNum+"]"
						+ "repaymentAmt=["+amount+"]tranDateTime=["+tranDateTime+"]"
								+ "VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		
		args.put("signValue", GUOUtil.md5(signValue));
		
		return args;
	}
	
	/**
	 * 投标完成
	 * @return
	 */
	public static Map<String, String> finishBid() {
		//把请求参数打包成数组 按照接口文档
		Map<String, String> args = new HashMap<String, String>();
		String merOrderNum = DateUtil.simple2(new Date());
		String tranDateTime = DateUtil.simple2(new Date());
		String bidId = "bid20141129100857";
		String amount = String.format("%.2f", 1000f);
		String mercFeeAmt = String.format("%.2f", 10f);
		args.put("version",GUOConstants.VERSION);
		args.put("signType",GUOConstants.SIGN_TYPE);
		args.put("charset",GUOConstants.CHARSET);
		args.put("tranCode",GUOConstants.METHOD_TYPE[4]);
		args.put("merId",GUOConstants.MER_ID);
		args.put("merName",GUOConstants.MER_NAME);
		args.put("merOrderNum",merOrderNum);
		args.put("tranDateTime",tranDateTime);
		args.put("bidId",bidId);
		args.put("tranAmt",amount);
		args.put("mercFeeAmt",mercFeeAmt);
		args.put("contractNo",GUOConstants.CONTRACT_NO);
		args.put("mobilePhone",GUOConstants.MOBILE);
		args.put("tranIP","127.0.0.1");
		
		String signValue = "version=["+GUOConstants.VERSION+"]tranCode=["+GUOConstants.METHOD_TYPE[4]+"]"
				+ "merId=["+GUOConstants.MER_ID+"]bidId=["+bidId+"]contractNo=["+GUOConstants.CONTRACT_NO+"]merOrderNum=["+merOrderNum+"]"
						+ "tranAmt=["+amount+"]mercFeeAmt=["+mercFeeAmt+"]tranDateTime=["+tranDateTime+"]"
								+ "VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		
		args.put("signValue", GUOUtil.md5(signValue));
		
		return args;
	}
	
	/**
	 * 还款
	 * @return
	 */
	public static Map<String, String> repayment() {
		//把请求参数打包成数组 按照接口文档
		Map<String, String> args = new HashMap<String, String>();
		String merOrderNum = DateUtil.simple2(new Date());
		String tranDateTime = DateUtil.simple2(new Date());
		String bidId = "bid20141129100857";
		String amount = String.format("%.2f", 100f);
		String mercFeeAmt = String.format("%.2f", 10f);
		String repaymentType = "0";
		String isInFull = "0";
		args.put("version",GUOConstants.VERSION);
		args.put("signType",GUOConstants.SIGN_TYPE);
		args.put("charset",GUOConstants.CHARSET);
		args.put("tranCode",GUOConstants.METHOD_TYPE[5]);
		args.put("merId",GUOConstants.MER_ID);
		args.put("merName",GUOConstants.MER_NAME);
		args.put("merOrderNum",merOrderNum);
		args.put("tranDateTime",tranDateTime);
		args.put("bidId",bidId);
		
		args.put("contractNo",GUOConstants.CONTRACT_NO);
		args.put("mobilePhone",GUOConstants.MOBILE);
		args.put("tranAmt",amount);
		args.put("repaymentType",repaymentType);
		args.put("mercFeeAmt",mercFeeAmt);
		args.put("isInFull","0");
		args.put("repaymentInfo",GUOConstants.MOBILE2+":"+amount);
		args.put("frontMerUrl",GUOConstants.GUO_ACCOUNT_WEB_LOGIN);
		args.put("backgroundMerUrl",GUOConstants.GUO_ACCOUNT_WEB);
		args.put("tranIP","127.0.0.1");
		
		String signValue = "version=["+GUOConstants.VERSION+"]tranCode=["+GUOConstants.METHOD_TYPE[5]+"]"
				+ "merId=["+GUOConstants.MER_ID+"]bidId=["+bidId+"]contractNo=["+GUOConstants.CONTRACT_NO+"]merOrderNum=["+merOrderNum+"]"
						+ "mobilePhone=["+GUOConstants.MOBILE+"]mercFeeAmt=["+mercFeeAmt+"]repaymentType=["+repaymentType+"]tranAmt=["+amount+"]"
								+ "isInFull=["+isInFull+"]VerficationCode=["+GUOConstants.VERFICATION_CODE+"]";
		
		
		args.put("signValue", GUOUtil.md5(signValue));
		
		return args;
	}
}
