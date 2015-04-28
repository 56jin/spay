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
public class GUOConstants {

	public static final String CONTRACT_NO = "P2P141128000000498"; //协议号
	public static final String CONTRACT_NO2 = "P2P141128000000499"; //协议号
	public static String P2P_NAME = null; //P2P平台用户名
	public static final String MOBILE = "15019412176"; //手机号码
	public static final String MOBILE2 = "13570808851"; //手机号码
	public static String MER_ID = null; //签约商户id
	public static String MER_NAME = null; //签约商户名称
	public static final String VERFICATION_CODE = Play.configuration.getProperty("verfication_code"); //身份识别码
	public static final String VERSION = "2.0"; //网关版本号,必须为2.0
	public static final String SIGN_TYPE = "1"; //加密方式 1 MD5 2 SHA
	public static final String CHARSET = "2"; //字符编码 1 GBK 2 UTF-8
	
	public static final String REPAYMENT_TYPE = "2"; //还款方式 1 全额  2 分期
	
	public static final String [] METHOD_TYPE2 = {
		"","P000","P001","P002","P003","P004","P006","P007","P008","P009","P010"
	};
	
	public static final String [] METHOD_TYPE = {
		"P002","P007","P000","P001","","","","","P008","P003","P004","","P008","P010","P006"
	};
	
	public static String GUO_ACCOUNT_WEB = Constants.BASE_URL + "/guo/guoCommit";
	public static String GUO_ACCOUNT_WEB_LOGIN = Constants.BASE_URL + "/guo/guoCommitLogin"; 
	public static String IPS_ACCOUNT_S2S = Constants.BASE_URL + "/guo/guoCommitAsynch";
	
	public static String GUO_URL = Play.configuration.getProperty("guo_url"); 
	
	/**
	 * 开户
	 */
	public static final String CMD_CREATE_ACCOUNT = "P007";
	
	/**
	 * 登入
	 */
	public static final String CMD_LOGIN = "P008";
	
	/**
	 * 专属账户余额查询
	 */
	public static final String CMD_QUERYACCOUNT = "P006";
	
	/**
	 * 专属账户充值
	 */
	public static final String CMD_RECHARGE = "P009";
	
	/**
	 * 专属账户提现
	 */
	public static final String CMD_WITHDRAWAL = "P010";
	
	/**
	 * 投标接口
	 */
	public static final String CMD_INVEST = "P001"; 
	
	/**
	 * 发标
	 */
	public static final String CMD_CREATEBID = "P000";
	
	/**
	 * 流标
	 */
	public static final String CMD_FLOWBID = "P002";
	
	/**
	 * 投标完成
	 */
	public static final String CMD_FINISHBID = "P003";
	
	/**
	 * 还款
	 */
	public static final String CMD_REPAYMENT = "P004";
	
	/**
	 * 查询单笔交易状态
	 */
	public static final String CMD_QUERY_TRADE = "P013";
	
	
	
	
	/**
	 * 开户
	 */
	public static final int CREATE_ACCOUNT = 1; //开户
	
	/**
	 * 标的登记
	 */
	public static final int REGISTER_SUBJECT = 2; //标的登记
	
	/**
	 * 登记债权人
	 */
	public static final int REGISTER_CREDITOR = 3; //登记债权人
	
	/**
	 * 登记担保方
	 */
	public static final int REGISTER_GUARANTOR = 4; //登记担保方
	
	/**
	 * 登记债权转让
	 */
	public static final int REGISTER_CRETANSFER = 5; //登记债权转让
	
	/**
	 * 自动投标签约
	 */
	public static final int AUTO_SIGING = 6;  //自动投标签约
	
	/**
	 * 自动还款签约
	 */
	public static final int REPAYMENT_SIGNING = 7; //自动还款签约
	
	/**
	 * 充值
	 */
	public static final int RECHARGE = 8; //充值
	
	/**
	 * 转账
	 */
	public static final int TRANSFER = 9;  //转账
	
	/**
	 * 还款
	 */
	public static final int REPAYMENT = 10; //还款
	
	/**
	 * 解冻保证金
	 */
	public static final int UNFREEZE = 11; //解冻保证金
	
	/**
	 * 自动代扣充值
	 */
	public static final int DEDUCT = 12; //自动代扣充值
	
	/**
	 * 提现
	 */
	public static final int WITHDRAWAL = 13; //提现
	
	/**
	 * 账户余额查询
	 */
	public static final int ACCOUNT_BALANCE = 14; //账户余额查询
	
	/**
	 * 商户端获取银行列表
	 */
	public static final int BANK_LIST = 15; //商户端获取银行列表
	
	/**
	 * 账户信息查询
	 */
	public static final int USER_INFO = 16; //账户信息查询
	
	/**
	 * 交易查询
	 */
	public static final int QUERY_TRADE = 17; //交易查询
	
	/**
	 * 登录
	 */
	public static final int USER_LOGIN = 100; //登录
}
