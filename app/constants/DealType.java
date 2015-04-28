package constants;

public class DealType {
	public static final int REGISTER_SUBJECT = 2;			//标的登记
	public static final int REGISTER_CREDITOR = 3;			//登记债权人
	public static final int REGISTER_GUARANTOR = 4;			//登记担保方
	public static final int REGISTER_CRETANSFER = 5;		//登记债权转让
	public static final int DO_DP_TRADE = 8;				//充值
	public static final int TRANSFER = 9;					//转账(WS)
	public static final int REPAYMENT_NEW_TRADE = 10;		//还款
	public static final int GUARANTEE_UNFREEZE = 11;		//解冻保证金
	public static final int CO_DP = 12;						//自动代扣充值(WS)
	public static final int DO_DW_TRADE = 13;				//提现
}
