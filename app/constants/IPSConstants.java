package constants;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import business.BackstageSet;
import business.Supervisor;
import play.Play;
import utils.Security;

/**
 * 常量值
 * 
 * @author bsr
 * @version 6.0
 * @created 2014-4-7 下午04:07:56
 */
public class IPSConstants {
	/**
	 * 环迅支付
	 */
	public static final boolean IPS_TEST_HX = "true".equals(Play.configuration.getProperty("IPS.TEST.HX")) ? true : false;
	public static String CERT_MD5 = null;
	public static String PUB_KEY = null;
	public static String DES_KEY = null;
	public static String DES_IV = null;

	/**
	 * 环迅地址
	 */
	
	public static String[] IPS_URL_FORMAL = null;
	public static String[] IPS_URL_TEST = {"",
		Play.configuration.getProperty("hx.account.create"),	//标的登记
		Play.configuration.getProperty("hx.subject.register"),	//登记债权人
		Play.configuration.getProperty("hx.creditor.register"), //登记担保方
		Play.configuration.getProperty("hx.guarantor.register"),//登记债权转让
		Play.configuration.getProperty("hx.cretansfer.register"),//自动投标签约
		Play.configuration.getProperty("hx.auto.sign"),		//自动还款签约
		Play.configuration.getProperty("hx.repayment.sign"),//充值
		Play.configuration.getProperty("hx.do.trade"),		//还款
		Play.configuration.getProperty("hx.transfer"),		//转账
		Play.configuration.getProperty("hx.repayment.trade"),//还款
		Play.configuration.getProperty("hx.unfreeze.guarantee"),//解冻保证金
		Play.configuration.getProperty("hx.co.trade"),		//自动代扣充值
		Play.configuration.getProperty("hx.dw.trade"),		//提现
		Play.configuration.getProperty("hx.balance.query"), //账户余额查询
		Play.configuration.getProperty("hx.bank.list"),		//商户端获取银行列表
		Play.configuration.getProperty("hx.user.info"),		//账户信息查询
		Play.configuration.getProperty("hx.query.trade"),		//账户信息查询
	};
	public static String[] IPS_URL = IPS_TEST_HX ? IPS_URL_TEST : IPS_URL_FORMAL;
	
	public static String IPS_ACCOUNT_WEB = Constants.BASE_URL + "/ips/ipsCommit"; 
	public static String IPS_ACCOUNT_S2S = Constants.BASE_URL + "/ips/ipsCommitAsynch";
	public static String WS_URL = Play.configuration.getProperty("ws.url");
	public static String WS_URL_QUERY = Play.configuration.getProperty("ws.url.query");
	/**
	 * 接口操作类型
	 */
	public static final int CREATE_ACCOUNT = 1; //开户
	public static final int REGISTER_SUBJECT = 2; //标的登记
	public static final int REGISTER_CREDITOR = 3; //登记债权人
	public static final int REGISTER_GUARANTOR = 4; //登记担保方
	public static final int REGISTER_CRETANSFER = 5; //登记债权转让
	public static final int AUTO_SIGING = 6;  //自动投标签约
	public static final int REPAYMENT_SIGNING = 7; //自动还款签约
	public static final int RECHARGE = 8; //充值
	public static final int TRANSFER = 9;  //转账
	public static final int REPAYMENT = 10; //还款
	public static final int UNFREEZE = 11; //解冻保证金
	public static final int DEDUCT = 12; //自动代扣充值
	public static final int WITHDRAWAL = 13; //提现
	public static final int ACCOUNT_BALANCE = 14; //账户余额查询
	public static final int BANK_LIST = 15; //商户端获取银行列表
	public static final int USER_INFO = 16; //账户信息查询
	public static final int QUERY_TRADE = 17; //交易查询
	
	public static final String BID_FLOWS = "flow";
	public static final String AUTO_INVEST = "autoInvest";
	public static final String AUTO_PAYMENT = "autoPayment";
	public static final boolean isTest = true;
}
