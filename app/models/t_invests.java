package models;

import java.util.Date;

import javax.persistence.Entity;

import play.db.jpa.Model;

/**
 * 投资理财信息表
 * @author yx
 *	@create 2014年12月31日 上午11:39:35
 */
@Entity
public class t_invests extends Model{
	
	public long user_id;  //用户id(投资人)
	public Date time; //投资时间
	public long bid_id;  //借款标id
	public String ips_bill_no;  //第三方支付返回的订单号（资金托管）
	public double amount; //投资金额
	
	public String bid_no;  //借款标编号
	
	@Override
	public String toString() {
		return "t_invests [user_id=" + user_id + ", time=" + time + ", bid_id="
				+ bid_id + ", ips_bill_no=" + ips_bill_no + ", amount="
				+ amount + "]";
	}
	
	

}
