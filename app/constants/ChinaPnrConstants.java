package constants;

import java.io.File;

import play.Play;
import services.ChinaPnrConfig;
import utils.DateUtil;
import utils.FileUtil;

/**
 * 汇付天下常量接口
 * 
 * @author yx
 * @create 2014年11月26日 下午9:07:13
 */
public interface ChinaPnrConstants {
	
	/**
	 * 日志根路径
	 */
	public final static String LOGFILEROOT = "data"+File.separator+"hftxlogs"+File.separator;
	

	/**
	 * 汇付天下正式环境
	 */
	public final static String PRODUCT_URL = "https://lab.chinapnr.com/muser/publicRequests";

	/**
	 * 汇付天下测试环境
	 */
	public final static String DEV_URL = ChinaPnrConfig.getProperty("chinapnr_url");
	
	/**
	 * 用户注册
	 */
	public final static String CMD_USERREGISTER = "UserRegister";
	
	/**
	 * 用户开户后台
	 */
	public final static String CMD_BGRRGISTER= "BgRegister";
	
	/**
	 * 用户绑卡
	 */
	public final static String CMD_USERBINDCARD = "UserBindCard";
	
	/**
	 * 用户登录
	 */
	public final static String CMD_USERLOGIN = "UserLogin";
	
	/**
	 * 网银充值
	 */
	public final static String CMD_NETSAVE = "NetSave";
	
	/**
	 * 自动扣款（放款）
	 */
	public final static String CMD_LOANS = "Loans";
	
	/**
	 * 自动扣款（还款）
	 */
	public final static String CMD_REPAYMENT = "Repayment";
	
	/**
	 * 前台用户间转账接口
	 */
	public final static String CMD_USRTRANSFER = "UsrTransfer";
	
	/**
	 * 转账（商户用）
	 */
	public final static String CMD_TRANSFER = "Transfer";
	
	/**
	 * 取现 
	 */
	public final static String CMD_CASH = "Cash";
	
	/**
	 * 余额查询(页面) 
	 */
	public final static String CMD_QUERYBALANCE = "QueryBalance";
	
	/**
	 * 余额查询(后台) 
	 */
	public final static String CMD_QUERYBALANCEBG = "QueryBalanceBg";
	
	/**
	 * 商户子账户信息查询 
	 */
	public final static String CMD_QUERYACCTS = "QueryAccts";
	
	/**
	 * 交易状态查询  
	 */
	public final static String CMD_QUERYTRANSSTAT = "QueryTransStat";
	
	/**
	 * 商户扣款对账 
	 */
	public final static String CMD_TRFRECONCILIATION = "TrfReconciliation";
	
	/**
	 * 投标对账(放款和还款对账) 、放还款对账
	 */
	public final static String CMD_RECONCILIATION = "Reconciliation";
	
	/**
	 * 取现对账
	 */
	public final static String CMD_CASHRECONCILIATION = "CashReconciliation";
	
	/**
	 * 充值对账
	 */
	public final static String CMD_SAVERECONCILIATION = "SaveReconciliation";
	
	/**
	 * 资金（货款）解冻
	 */
	public final static String CMD_USRUNFREEZE = "UsrUnFreeze";
	
	/**
	 *  标的信息录入接口
	 */
	public final static String CMD_ADDBIDINFO = "AddBidInfo";
	
	/**
	 * 债权转让接口
	 */
	public final static String CMD_CREDITASSIGN = "CreditAssign";
	
	/**
	 * 主动投标
	 */
	public final static String CMD_INITIATIVETENDER = "InitiativeTender";
	
	/**
	 * 自动投标
	 */
	public final static String CMD_AUTOTENDER = "AutoTender";
	
	/**
	 * 自动投标计划
	 */
	public final static String CMD_AUTOTENDERPLAN = "AutoTenderPlan";
	
	/**
	 * 自动投标计划关闭
	 */
	public final static String CMD_AUTOTENDERPLANCLOSE = "AutoTenderPlanClose";
	
	
	/**
	 * 标的撤销
	 */
	public final static String CMD_TENDERCANCLE = "TenderCancle";
	
	
	/**
	 * 标的撤销
	 */
	public final static String CMD_QUERYTRANSTYPE = "TenderCancle";
	
	/**
	 * 
	 */
	public final static String CMD_QUERYCARDINFO = "QueryCardInfo";
	
	/**
	 * 用户账户支付
	 */
	public final static String CMD_USRACCPAY = "UsrAcctPay";
	
	/**
	 * 用户信息查询
	 */
	public  final static String CMD_QUERYUSRINFO = "QueryUsrInfo";
	
	/**
	 * 交易明细查询
	 */
	public final static String CMD_QUERYTRANSDETAIL = "QueryTransDetail";
	
	
	
	
	
	/**
	 *开户
	 */
	public static final int I_CREATE_ACCOUNT = 1; //开户
	
	/**
	 * 标的登记
	 */
	public static final int I_REGISTER_SUBJECT = 2; //标的登记
	
	/**
	 * 标的流标
	 */
	public static final int I_FLOW_BID = 22; //标的流标
	
	/**
	 * 登记债权人
	 */
	public static final int I_REGISTER_CREDITOR = 3; //登记债权人
	
	/**
	 * 登记担保方
	 */
	public static final int I_REGISTER_GUARANTOR = 4; //登记担保方
	
	/**
	 * 登记债权转让
	 */
	public static final int I_REGISTER_CRETANSFER = 5; //登记债权转让
	
	/**
	 * 自动投标签约
	 */
	public static final int I_AUTO_SIGING = 6;  //自动投标签约
	
	/**
	 * 自动还款签约
	 */
	public static final int I_REPAYMENT_SIGNING = 7; //自动还款签约
	
	/**
	 * 充值
	 */
	public static final int I_RECHARGE = 8; //充值
	
	/**
	 * 转账
	 */
	public static final int I_TRANSFER = 9;  //转账
	
	/**
	 * 还款
	 */
	public static final int I_REPAYMENT = 10; //还款
	
	/**
	 * 解冻保证金
	 */
	public static final int I_UNFREEZE = 11; //解冻保证金
	
	/**
	 * 自动代扣充值
	 */
	public static final int I_DEDUCT = 12; //自动代扣充值
	
	/**
	 * 提现
	 */
	public static final int I_WITHDRAWAL = 13; //提现
	
	/**
	 * 账户余额查询
	 */
	public static final int I_ACCOUNT_BALANCE = 14; //账户余额查询
	
	/**
	 * 商户端获取银行列表
	 */
	public static final int I_BANK_LIST = 15; //商户端获取银行列表
	
	/**
	 * 账户信息查询
	 */
	public static final int I_USER_INFO = 16; //账户信息查询
	
	/**
	 * 交易查询
	 */
	public static final int I_QUERY_TRADE = 17; //交易查询
	
	/**
	 * 登录
	 */
	public static final int I_USERLOGIN = 100; //登录
	
	/**
	 * 用户绑卡
	 */
	public static final int I_USERBINDCARD = 19; //用户绑卡
	
	/**
	 * 余额查询（页面）
	 */
	public static final int I_QUERYBALANCE = 20; //余额查询（页面）
	
	/**
	 * 主动投标
	 */
	public static final int I_INITIATIVETENDER = 21; //主动投标
	
	/**
	 * 自动投标
	 */
	public static final int I_AUTOTENDER  = 22;  //自动投标
	
	/**
	 * 自动投标计划
	 */
	public static final int I_AUTOTENDERPLAN  = 23;  //自动投标计划
	
	/**
	 * 自动投标计划关闭
	 */
	public static final int I_AUTOTENDERPLANCLOSE  = 24;  //自动投标计划关闭
	
	/**
	 * 自动扣款(放款)
	 */
	public static final int I_LOANS  = 25;  //自动扣款(放款)
	
	/**
	 * 自动扣款转账（商户用）
	 */
	public static final int I_MTRANSFER = 26;  //自动扣款转账（商户用）
	
	
	/**
	 * 商户子账户信息查询
	 */
	public static final int I_QUERYACCTS = 27;  //商户子账户信息查询
	
	/**
	 * 商户扣款对账
	 */
	public static final int I_TRFRECONCILIATION = 28;  //商户扣款对账
	
	/**
	 * 放还款对账
	 */
	public static final int I_RECONCILIATION = 29;  //放还款对账
	
	/**
	 * 取现对账
	 */
	public static final int I_CASHRECONCILIATION = 30;  //取现对账
	
	/**
	 * 充值对账
	 */
	public static final int I_SAVERECONCILIATION = 31;  //充值对账
	
	/**
	 * 用户账户支付
	 */
	public static final int I_USRACCPAY = 32;  
	
	/**
	 * 商户转用户(多比)
	 */
	public static final int I_MERCUSTTUSER = 33;
	
	/**
	 * 商户转用户(单笔)
	 */
	public static final int I_MERCUSTTUSERBYSINGLE = 34;
	
	/**
	 * 单笔资金解冻
	 */
	public static final int I_USRUNFREEZE_SINGLE = 36;
	
	
}
