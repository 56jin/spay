package constants;

import play.Play;

import com.shove.Convert;

/**
 * 双乾接口常量
 *
 * @author hys
 * @createDate  2014年11月27日 下午8:27:14
 *
 */
public class LoanConstants {
//	static{
//    	Map<Long, GateWay> gateWays = GateWay.queryGateWay();
//    	GateWay gateWay = null;
//    	if(gateWays != null && !gateWays.isEmpty()) {
//    		/*双乾*/
//    		long loan = Constants.LOAN;
//    		gateWay = gateWays.get(loan) != null ? gateWays.get(loan) : null;
//    		
//    		if(gateWay != null) {
//    			LoanConstants.argMerCode = gateWay.keyInfo.get("argMerCode");
//    			LoanConstants.signRate = gateWay.keyInfo.get("signRate");
//    			LoanConstants.publicKey = gateWay.keyInfo.get("publicKey");
//    			LoanConstants.privateKeyPKCS8 = gateWay.keyInfo.get("privateKeyPKCS8");
//    			Logger.info("===================双乾支付常量赋值（LoanConstants）=================");
//    			Logger.info("argMerCode  = %s", LoanConstants.argMerCode==null?"null":"ok");
//    			Logger.info("signRate  = %s", LoanConstants.signRate==null?"null":"ok");
//    			Logger.info("publicKey  = %s", LoanConstants.publicKey==null?"null":"ok");
//    			Logger.info("privateKeyPKCS8 = %s", LoanConstants.privateKeyPKCS8==null?"null":"ok");
//    		}else{
//        		Logger.info("=======双乾支付常量赋值时，%s","没有添加双乾支付平台，或双乾支付平台为开启");
//        	}
//    	}else{
//    		Logger.info("=======双乾支付常量赋值时：%s","没有可使用的资金托管平台");
//    	}
//	}

	public static final String TOKEN = "KK;;";  //字符串分隔符
	
	public static final int TRANSFER_MAX_BILL = Convert.strToInt(Play.configuration.getProperty("loan.transfer.max.bill"), 200);  //转账、审核最大笔数
	
	/**
	 * 中间件向P2P平台提供的资金托管接口
	 */
	public static final int CREATE_ACCOUNT = 1;  //开户
	public static final int REGISTER_SUBJECT = 2;  //标的登记
	public static final int REGISTER_CREDITOR = 3; //登记债权人
	public static final int REGISTER_CRETANSFER = 5; //登记债权转让
	public static final int AUTO_SIGING = 6;  //自动投标签约
	public static final int REPAYMENT_SIGNING = 7; //自动还款签约
	public static final int RECHARGE = 8; //充值
	public static final int TRANSFER = 9;  //转账
	public static final int REPAYMENT = 10; //还款
	public static final int GUARANTEE_UNFREEZE = 11; //解冻保证金
	public static final int WITHDRAWAL = 13; //提现
	public static final int ACCOUNT_BALANCE = 14; //账户余额查询
	public static final int BANK_LIST = 15; //商户端获取银行列表
	public static final int USER_INFO = 16; //账户信息查询
	public static final int QUERY_TRADE = 17; //交易查询
                  
	public static final int AUTHORIZE_SECONDARY = 18; //双乾特有接口：二次分配审核授权
	public static final int BOND_CRAD_NO = 19; //双乾特有接口：提现银行卡
	
	public static final int TRANSFER_USER_TO_MER = 32;  //用户转商户
	public static final int TRANSFER_MER_TO_USER = 33;  //商户转用户
	public static final int UNFREEZE_INVEST_AMOUNT = 35;	//解冻投资金额
	
	/**
	 * 双乾支付
	 */
	public static final boolean LOAN_TEST_SHUANGQIAN = "true".equals(Play.configuration.getProperty("loan.test.shuangqian")) ? true : false;
	public static String privateKeyPKCS8 = null;
	public static String publicKey = null;
	public static String argMerCode = null;	
	public static String signRate = null;  //签约费率0.0025	
	
	/**
	 * 双乾接口地址
	 */
	public static String[] LOAN_URL_TEST = {"",  //0
		Play.configuration.getProperty("shuangqian.account.create"),  //1、开户	
		Play.configuration.getProperty("shuangqian.balance.query"),	 //2、余额查询
		Play.configuration.getProperty("shuangqian.recharge"),  //3、充值
		Play.configuration.getProperty("shuangqian.transfer"),	//4、转账
		Play.configuration.getProperty("shuangqian.fast.pay"),	//5、三口合一 （认证，提现绑卡，代扣授权）
		Play.configuration.getProperty("shuangqian.withdraws"),  //6、提现
		Play.configuration.getProperty("shuangqian.authorize"),	 //7、授权
		Play.configuration.getProperty("shuangqian.transfer.audit"),  //8、审核	
		Play.configuration.getProperty("shuangqian.release"),  //9、资金释放
		Play.configuration.getProperty("shuangqian.order.query")  //10、对账
	};  //测试地址
	
	public static String[] LOAN_URL_FORMAL = null;  //正式地址
	
	public static String[] LOAN_URL = LOAN_TEST_SHUANGQIAN ? LOAN_URL_TEST : LOAN_URL_FORMAL;
	
	/**
	 * 中间件向双乾提供的回调接口地址
	 */
	public static class LoanReturnURL {   
		public static final String CREATE_ACCOUNT = Constants.BASE_URL + "/loan/loanCreateAccountReturn";  //1、开户 
		public static final String ADD_REGISTER_SUBJECT = Constants.BASE_URL + "/loan/loanAddRegisterSubjectReturn";  //2、标的登记（新增）
		public static final String END_REGISTER_SUBJECT = Constants.BASE_URL + "/loan/loanEndRegisterSubjectReturn";  //2、标的登记(结束)
		public static final String REGISTER_CREDITOR = Constants.BASE_URL + "/loan/loanRegisterCreditorReturn";  //3、登记债权人
		public static final String REGISTER_CRETANSFER = Constants.BASE_URL + "/loan/loanCreditTransferReturn";  //5、登记债权转让
		public static final String SIGING = Constants.BASE_URL + "/loan/loanSignReturn";  //6、自动投标签约,7、自动还款签约
		public static final String RECHARGE = Constants.BASE_URL + "/loan/loanRechargeReturn"; //8、充值
		public static final String TRANSFER = Constants.BASE_URL + "/loan/loanTransferReturn" ;   //9、转账：投资
		public static final String COMPENSATER = Constants.BASE_URL + "/loan/loanCompensateReturn" ;   //9、转账：代偿
		public static final String COMPENSATER_REPAYMENT = Constants.BASE_URL + "/loan/loanCompensateRepaymentReturn" ;   //9、转账：代偿还款
		public static final String REPAYMENT = Constants.BASE_URL + "/loan/loanRepaymentReturn";  //10、还款
		public static final String GUARANTEE_UNFREEZE = Constants.BASE_URL + "/loan/loanGuaranteeUnfreezeReturn";  //11、解冻保证金
		public static final String WITHDRAWAL = Constants.BASE_URL + "/loan/loanWithdrawReturn";  //13、提现
		public static final String ACCOUNT_BALANCE = Constants.BASE_URL + "/loan/loanAccountBalanceReturn";  //14、账户余额查询
		public static final String QUERY_TRADE = Constants.BASE_URL + "/loan/loanQueryTradeReturn";  //17、交易查询
		public static final String AUTHORIZE_SECONDARY = Constants.BASE_URL + "/loan/loanAuthorizeSecondaryReturn";  //18、二次分配审核授权
		public static final String BOND_CRAD_NO = Constants.BASE_URL + "/loan/loanBondCardNoReturn";  //19、提现银行卡号绑定
		public static final String TRANSFER_USER_TO_MER = Constants.BASE_URL + "/loan/loanTransferUserToMerReturn";  //32、用户转商户
		public static final String TRANSFER_MER_TO_USER = Constants.BASE_URL + "/loan/loanTransferMerToUserReturn";  //33、商户转用户
		public static final String UNFREEZE_INVEST_AMOUNT = Constants.BASE_URL + "/loan/loanUnfreezeInvestAmountReturn";  //35、解冻投资金额
	}

	/**
	 * 中间件向双乾提供的后台通知接口地址
	 */
	public static class LoanNotifyURL {   
		public static final String CREATE_ACCOUNT = Constants.BASE_URL + "/loan/loanCreateAccountNotify";  //1、开户 
		public static final String ADD_REGISTER_SUBJECT = Constants.BASE_URL + "/loan/loanAddRegisterSubjectNotify";  //2、标的登记（新增）
		public static final String END_REGISTER_SUBJECT = Constants.BASE_URL + "/loan/loanEndRegisterSubjectNotify";  //2、标的登记(结束)
		public static final String REGISTER_CREDITOR = Constants.BASE_URL + "/loan/loanRegisterCreditorNotify";  //3、登记债权人
		public static final String REGISTER_CRETANSFER = Constants.BASE_URL + "/loan/loanCreditTransferNotify";  //5、登记债权转让
		public static final String SIGING = Constants.BASE_URL + "/loan/loanSignNotify";  //6、自动投标签约,7、自动还款签约
		public static final String RECHARGE = Constants.BASE_URL + "/loan/loanRechargeNotify"; //8、充值
		public static final String TRANSFER = Constants.BASE_URL + "/loan/loanTransferNotify" ;   //9、转账：投资
		public static final String COMPENSATER = Constants.BASE_URL + "/loan/loanCompensateNotify" ;   //9、转账：代偿
		public static final String COMPENSATER_REPAYMENT = Constants.BASE_URL + "/loan/loanCompensateRepaymentNotify" ;   //9、转账：代偿还款
		public static final String REPAYMENT = Constants.BASE_URL + "/loan/loanRepaymentNotify";  //10、还款
		public static final String GUARANTEE_UNFREEZE = Constants.BASE_URL + "/loan/loanGuaranteeUnfreezeNotify";  //11、解冻保证金
		public static final String WITHDRAWAL = Constants.BASE_URL + "/loan/loanWithdrawNotify";  //13、提现
		public static final String ACCOUNT_BALANCE = Constants.BASE_URL + "/loan/loanAccountBalanceNotify";  //14、账户余额查询
		public static final String QUERY_TRADE = Constants.BASE_URL + "/loan/loanQueryTradeNotify";  //17、交易查询
		public static final String AUTHORIZE_SECONDARY = Constants.BASE_URL + "/loan/loanAuthorizeSecondaryNotify";  //18、二次分配审核授权
		public static final String BOND_CRAD_NO = Constants.BASE_URL + "/loan/loanBondCardNoNotify";  //19、提现银行卡号绑定
		public static final String TRANSFER_USER_TO_MER = Constants.BASE_URL + "/loan/loanTransferUserToMerNotify";  //32、用户转商户
		public static final String TRANSFER_MER_TO_USER = Constants.BASE_URL + "/loan/loanTransferMerToUserNotify";  //33、商户转用户
		public static final String UNFREEZE_INVEST_AMOUNT = Constants.BASE_URL + "/loan/loanUnfreezeInvestAmountNotify";  //35、商户转用户
	}
	
	/**
	 * 开户相关常量
	 */
	public static final String REGISTER_TYPE = "2";  //固定注册类型为：半自动
	public static final String ACCOUNT_TYPE = "";  //固定账户类型为：个人
	public static final String IMAGE1 = "";  //去掉Image1可选项
	public static final String IMAGE2 = "";  //去掉Image2可选项
	public static final String RANDOM_TIME_STAMP = "";  //不启用防抵赖功能
	
	/**
	 * 充值相关插常量
	 */
	public static final String RECHARGE_TYPE = "";  // 固定充值类型：网银充值
	public static final String FEE_TYPE = "";  // 去掉FeeType（手续费类型）可选项
	public static final String CARD_NO = "";  // 去掉CardNo（银行卡号）可选项
	
	/**
	 * 授权相关常量
	 */
	public static final String AUTO_BID = "1";  //自动投标授权
	public static final String AUTO_PAY = "2";  //自动还款授权
	public static final String AUTH_SECOND = "3";  //二次分配审核授权
	
	/**
	 * 标的登记相关常量
	 */
	public static final String ADD_REGISTER_SUBJECT = "1";  //标的操作类型：新增
	public static final String END_REGISTER_SUBJECT = "2";  //自动还款授权：结束
	
	/**
	 * 转账相关常量
	 */
	public static final String INVEST = "1";  //转账类型：投资
	public static final String COMPENSATE = "2";  //转账类型：代偿
	public static final String COMPENSATEREPAYMENT = "3";  //转账类型：代偿还款
	public static final String CREDITOR_TANSFER = "4";  //转账类型：债权转让
	public static final String BILLTOKEN = "k";  //转账流水号分隔符
	

	public static final int ERROR = -99;  //参数、程序错误
	public static final int TRANSFER_EXECUTED = -100;  //转账执行完成
	public static final int TRANSFER_REPAIR_SUCCESS = -101;  //转账成功
	public static final int TRANSFER_ERROR = -102;  //转账异常
	
	/**
	 * 提现绑卡相关常量
	 */
	public static final String ACTION = "2";  //操作类型：2.提现银行卡绑定
	
	/**
	 * 提现相关常量
	 */
	public static final String FeeFromPlatform = "1";  //平台垫付
	public static final String FeeFromMember = "2";  //用户支付
	
	/**
	 * 对账相关常量
	 */
	public static final String queryTransfer = "";  //转账
	public static final String queryRecharge = "1";  //充值
	public static final String queryWithdraw = "2";  //提现
	
	public static final boolean isTest = true;
	
	public static final String URLENCODE = "utf-8";  //字符串分隔符

	
	/**
	 * 补单常量
	 *
	 * @author hys
	 * @createDate  2014年12月25日 下午2:07:00
	 *
	 */
	public class RepairOperation {
		public static final String REGISTER_SUBJECT = "02";		//标的登记,冻结保证金
		public static final String REGISTER_CREDITOR = "03";	//登记债权人接口，冻结投资金额
		public static final String REGISTER_CRETANSFER = "05";	//登记债权转让接口，直接转账，是否审核通过
		public static final String DO_DP_TRADE = "08";			//充值
		public static final String REPAYMENT_NEW_TRADE = "11";	//还款，直接转账，是否审核通过
		public static final String DO_DW_TRADE = "09";		//提现
		public static final String TRANSFER_ONE = "14";		//转账-1放款(WS)，是否审核通过
		public static final String TRANSFER_FOUR = "15";	//转账-4债权转让(WS)，直接转账是否审核通过
	}
}
