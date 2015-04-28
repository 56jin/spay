package models;

import java.util.Date;

import javax.persistence.Entity;

import play.db.jpa.Model;

/**
 * 汇付天下自动放款交易记录存储
 * @author yx
 *	@create 2014年12月11日 下午6:59:33
 */
@Entity
public class t_loans_details extends DataModel{
	
	public Date time;  //操作日期
	public String oriMerBillNo;  //原商户订单号
	public String trdAmt;  //转账金额
	public String fAcctType;  //转出方账户类型0#机构;1#个人
	public String fIpsAcctNo;  //转出方托管账户号.
	public String fTrdFee;  //转出方明细手续
	public String tAcctType;  //转入方账户类型
	public String tIpsAcctNo;  //转入方托管账户号
	public String tTrdFee;  //转入方明细手续
	public String ipsBillNo;  //冻结标识
	public String merBillNo;  //商户订单号
	public String bidNo;  //标的号
	public int status;  //1成功0失败
	
	@Override
	public String toString() {
		return "t_loans_details [time=" + time + ", oriMerBillNo="
				+ oriMerBillNo + ", trdAmt=" + trdAmt + ", fAcctType="
				+ fAcctType + ", fIpsAcctNo=" + fIpsAcctNo + ", fTrdFee="
				+ fTrdFee + ", tAcctType=" + tAcctType + ", tIpsAcctNo="
				+ tIpsAcctNo + ", tTrdFee=" + tTrdFee + ", ipsBillNo="
				+ ipsBillNo + ", merBillNo=" + merBillNo + ", bidNo=" + bidNo
				+ ", status=" + status + "]";
	}
	
	

}
