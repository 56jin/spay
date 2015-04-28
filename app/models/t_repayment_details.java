package models;

import java.util.Date;

import javax.persistence.Entity;

import play.db.jpa.Model;

/**
 * 汇付天下还款记录
 * @author yx
 *	@create 2014年12月12日 下午5:12:06
 */
@Entity
public class t_repayment_details extends Model{
	
	public Date time;  //操作时间
	public String ordId;  //订单号
	public String ordDate;  //订单日期
	public String outCustId;  //出账客户号
	public String subOrdId;  //订单号
	public String subOrdDate;  //订单日期
	public String transAmt;  //交易金额
	public String fee;  //扣款手续费
	public String inCustId;  //入账客户号
	public String reqExt;  //入参扩展域
	public String memberId;  //互联网金融平台用户id
	public String pBidNo;  //标的号
	public String pMerBillNo;  //商户还款订单号
	public int status;  //状态
	@Override
	public String toString() {
		return "t_repayment_details [time=" + time + ", ordId=" + ordId
				+ ", ordDate=" + ordDate + ", outCustId=" + outCustId
				+ ", subOrdId=" + subOrdId + ", subOrdDate=" + subOrdDate
				+ ", transAmt=" + transAmt + ", fee=" + fee + ", inCustId="
				+ inCustId + ", reqExt=" + reqExt + ", memberId=" + memberId
				+ ", pBidNo=" + pBidNo + ", pMerBillNo=" + pMerBillNo
				+ ", status=" + status + "]";
	}
	

	
	
}
