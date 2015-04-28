package models;

import java.util.Date;

import javax.persistence.Entity;

import play.db.jpa.Model;

/**
 * 标的记录
 * @author hys
 * @version 6.0
 * @created 2014-12-15
 */
@Entity
public class t_bids extends Model {
	
	public Date time;
	public String bid_no;  //标的号
	public double remain_fee;  //剩余借款管理费
	public boolean is_success;  //是否发标成功，1是，0否
	//add by yangxuan 20141231
	public double amount;  //借款金额
	public double has_invested_amount;  //已投总额(冗余)
	public int version;  //版本--（用于控制并发）
	public long bid_id;  //标的ID
	//added
	
	@Override
	public String toString() {
		return "t_bids [time=" + time + ", bid_no=" + bid_no + ", remain_fee="
				+ remain_fee + ", is_success=" + is_success + ", amount="
				+ amount + ", has_invested_amount=" + has_invested_amount
				+ ", version=" + version + ", bid_id=" + bid_id + "]";
	}
	
	
}
