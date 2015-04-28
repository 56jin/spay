package constants;

import play.Play;

public class YEEConstants {

	/**
	 * 接口操作类型
	 */
	public static final int CREATE_ACCOUNT = 1; //开户
	public static final int REGISTER_SUBJECT = 2;  //标的登记(ws)(标的第一个投资的时候开始登记)
	public static final int REGISTER_CREDITOR = 3;  //登记债权人（投标）
	public static final int REGISTER_GUARANTOR = 4;  //登记担保方--易宝用不上
	public static final int REGISTER_CRETANSFER = 5;  //登记债权转让（债权转让）
	public static final int AUTO_SIGING = 6;  //自动投标签约
	public static final int REPAYMENT_SIGNING = 7;  //自动还款签约
	public static final int RECHARGE = 8;  //充值
	public static final int TRANSFER = 9;  //转账
	public static final int REPAYMENT = 10;  //还款
	public static final int UNFREEZE = 11;  //解冻保证金
	public static final int DEDUCT = 12;  //自动代扣充值
	public static final int WITHDRAWAL = 13;  //提现
	public static final int ACCOUNT_BALANCE = 14;  //账户余额查询(ws)
	public static final int BANK_LIST = 15;  //商户端获取银行列表
	public static final int USER_INFO = 16;  //账户信息查询(ws)
	public static final int QUERY_TRADE = 17;  //交易查询(ws)
	public static final int BOUND_CARD = 18;  //绑定银行卡
	public static final int TRANSFER_USER_TO_MER = 32;  //用户转商户,通过此接口，可以将资金任意转出给其他多人。用户输入交易密码确认后，资金仅冻结，需要之后调用通用转账确认接口来真正进行转账或者取消
	public static final int TRANSFER_MER_TO_USERS = 33;  //商户转用户(发送投标奖励post)
	public static final int TRANSFER_MER_TO_USER = 34;  //商户转用户（发放cps奖励，单笔ws ）
	public static final int UNFREZZ_AMOUNT = 36;  //解除冻结资金
	public static final int AUTO_INVEST_BID = 37;  //自动投标
	
	public static String[] IPS_URL_TEST = {"",
		Play.configuration.getProperty("yee.register"),  //开户
		"",
		Play.configuration.getProperty("yee.invest.bid"),  //登记债权人
		"",
		Play.configuration.getProperty("yee.transfer"),  //登记债权转让
		Play.configuration.getProperty("yee.auto.invest"),  //自动投标签约
		"http://119.161.147.110:8088/member/bha/toAuthorizeAutoRepayment",  //自动还款签约
		Play.configuration.getProperty("yee.recharge"),  //充值
		Play.configuration.getProperty("yee.money.transfer"),  //转账
		Play.configuration.getProperty("yee.repayment"),  //还款
		"http://119.161.147.110:8088/member/bha/toAuthorizeAutoRepayment",  //自动还款
		"http://119.161.147.110:8088/member/bha/toAuthorizeAutoRepayment",  //解冻保证金
		Play.configuration.getProperty("yee.withdraw"),	//提现
		"","","","",	
		"http://119.161.147.110:8088/member/bha/toBindBankCard",	//绑卡
		"","","","","","","","","","","","","",
		Play.configuration.getProperty("yee.user.to.mar"),  //用户转商户
		"",
		Play.configuration.getProperty("yee.mar.to.user"),  //商户转用户
	};
	
	public static final String BID_FLOWS = "flow";
	public static final String AUTO_INVEST = "autoInvest";
	public static final String AUTO_PAYMENT = "autoPayment";
	public static final boolean isTest = true;
	
	public static String[] QUERY_DEDAIL = {"","","",
		"PAYMENT_RECORD",  //标的投资放款记录
		"","","","",
		"RECHARGE_RECORD",  //充值记录
		"WITHDRAW_RECORD",  //提现记录
		"",
		"REPAYMENT_RECORD",  //标的还款记录
		"","",
		"PAYMENT_RECORD",  //标的投资放款记录
		};
	
	public static final String YEE_SIGN_URL = Play.configuration.getProperty("yee.fix");  //证书路径
	public static final String YEE_SIGN_PASS = Play.configuration.getProperty("yee.fix.pass");  //证书密钥
	public static final String YEE_URL_REDICT = Play.configuration.getProperty("yee.url.redict");  //直接接口访问路径
	
	public static final int P2P_LOAN = 1;  //转账---1放款
	public static final int COMPENSATE = 2;  //代偿（线下收款，本金垫付--商户转用户）
	public static final int COMPENSATE_REPAYMENT = 3;  //代偿还款（本金垫付后借款人还款 -- 用户转商户）
	public static final int P2P_TRANSFER = 4;  //转账---4债权转让
	
	/**
	 * 保存在数据库中绑卡状态
	 */
	public static final int CARD_NO_BANG = 0;  //未绑定
	public static final int CARD_SUBMIT_SUCCESS = 1;  //受理成功
	
	/**
	 * 资金托管返回的绑卡状态
	 */
	public static final String CARD_HANDDLE = "VERIFYING";  //认证中
	public static final String CARD_SUCCESS = "VERIFIED";  //已认证
	
	/**
	 * 资金托管返回的会员激活状态
	 */
	public static final String USER_ACTIVATED = "ACTIVATED";  //已激活
	public static final String USER_DEACTIVATED = "DEACTIVATED";  //未激活
	
	/**
	 * 标的还款方式
	 */
	public static final String BID_ERPATTYPE = "99";  //秒还还款
	
	/**
	 * 登记标的操作方式
	 */
	public static final String BID_OPERRATION_TYPE = "2";  //流标
	
	/**
	 * 登记债权人操作方式
	 */
	public static final String REGISTER_CREDITOR_TYPE = "2";  //
	
	/**
	 * 易宝在数据库的domain标识号（用来查找商户号号）
	 */
	public static final String PLATFORM_NO = Play.configuration.getProperty("yee.platform.no");  
	
	/**
	 * WS请求P2P
	 */
	public static final String YEE_CALLBACK = Play.configuration.getProperty("yee.callback");
	
	/**
	 * 查询补单的类型
	 */
	public class QueryType{
		public static final int QUERY_INVEST_TYPE = 3;  //登记债权人
		public static final int QUERY_CREATE_BID = 2;  //发标
		public static final int QUERY_TRANSFERS_TYPE = 5;  //登记债权转让
		public static final int QUERY_RECAHARGE_TYPE = 8;  //充值
		public static final int QUERY_WITHDRAW_TYPE = 9;  //提现
		public static final int QUERY_REPAYMENT_TYPE = 11;  //还款
		public static final int QUERY_MONEY_TRANSFER_TYPE = 14;  //转账(放款)
	}
	
	public class Status {
		public static final String SUCCESS = "1";		//成功
		public static final String FAIL = "2";			//失败
		public static final String HANDLING = "3";		//处理中
	}
}
