package constants;

public class EventType {
	
	public static final int CREATE_ACCOUNT = 201; //开户
	public static final int REGISTER_SUBJECT = 202; //标的登记
	public static final int REGISTER_CREDITOR = 203; //登记债权人
	public static final int REGISTER_GUARANTOR = 204; //登记担保方
	public static final int REGISTER_CRETANSFER = 205; //登记债权转让
	public static final int AUTO_SIGING = 206;  //自动投标签约
	public static final int REPAYMENT_SIGNING = 207; //自动还款签约
	public static final int RECHARGE = 208; //充值
	public static final int TRANSFER = 209;  //转账
	public static final int TRANSFER_RECEIVED = 210;  //收到转账
	public static final int REPAYMENT = 211; //还款
	public static final int REPAYMENT_RECEIVED = 212; //还款
	public static final int UNFREEZE = 213; //解冻保证金
	public static final int DEDUCT = 214; //自动代扣充值
	public static final int WITHDRAWAL = 215; //提现
	public static final int ACCOUNT_BALANCE = 216; //账户余额查询
	public static final int BANK_LIST = 217; //商户端获取银行列表
	public static final int USER_INFO = 218; //账户信息查询
}
