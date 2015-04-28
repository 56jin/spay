package business;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.Query;

import models.t_invests;
import play.Logger;
import play.db.jpa.JPA;
import utils.ErrorInfo;

/**
 * 投资理财业务类
 * @author yx
 *	@create 2014年12月31日 上午11:41:08
 */
public class Invests implements Serializable{
	
	public long id;
	public long user_id;  //用户id(投资人)
	public Date time; //投资时间
	public long bid_id;  //借款标id
	public String ips_bill_no;  //第三方支付返回的订单号（资金托管）
	public double amount; //投资金额
	
	public String bid_no;  //借款标编号
	
	private long _id;
	private long _user_id;  //用户id(投资人)
	private Date _time; //投资时间
	private long _bid_id;  //借款标id
	private String _ips_bill_no;  //第三方支付返回的订单号（资金托管）
	private double _amount; //投资金额
	
	private String _bid_no; //借款标编号
	
	public String getBid_no() {
		return this._bid_no;
	}
	public void setBid_no(String bid_no) {
		this._bid_no = bid_no;
	}
	
	public long getId() {
		return this._id;
	}
	public void setId(long id) {
		this._id = id;
	}
	public long getUser_id() {
		return this._user_id;
	}
	public void setUser_id(long user_id) {
		this._user_id = user_id;
	}
	public Date getTime() {
		return this._time;
	}
	public void setTime(Date time) {
		this._time = time;
	}
	public long getBid_id() {
		return this._bid_id;
	}
	public void setBid_id(long bid_id) {
		this._bid_id = bid_id;
	}
	public String getIps_bill_no() {
		return this._ips_bill_no;
	}
	public void setIps_bill_no(String ips_bill_no) {
		this._ips_bill_no = ips_bill_no;
	}
	public double getAmount() {
		return this._amount;
	}
	public void setAmount(double amount) {
		this._amount = amount;
	}
	
	/**
	 * 保存投资信息
	 * @param error
	 */
	public void saveInvest(ErrorInfo error){
		error.clear();
		
		t_invests invests = buildInvest();
		
		t_invests investResult = findInvestByBillNo(invests.ips_bill_no);
		if(investResult == null){
			try{
				
				invests.save();
				
			}catch(Exception e){
				
				error.code = -1;
				error.msg = "保存投资信息失败";
				Logger.error("保存投资信息时:%s", e.getMessage());
			}
		}
	}
	
	/**
	 * 通过订单号查询投资信息
	 * @param billNo
	 * @return
	 */
	private t_invests findInvestByBillNo(String billNo){
		
		t_invests invests = null;
		try{
			
			invests = t_invests.find("ips_bill_no = ?", billNo).first();
		
		}catch(Exception e){
			
			Logger.error("通过订单号查询投资信息时 %s", e.getMessage());
		}
		return invests;
	}
	
	public static double getAmountByBidId(String bidId){
		String sql = "SELECT SUM(amount) FROM t_invests WHERE bid_id = ?";
		Query query = JPA.em().createNativeQuery(sql).setParameter(1, bidId);
		List resultList = query.getResultList();
		if(resultList!=null&&resultList.size()>0 && resultList.get(0) != null){
			return Double.valueOf( resultList.get(0).toString());
		}
		return 0.00;
	}
	
	public static double getAmountByBidNo(String bidNo){
		String sql = "SELECT SUM(amount) FROM t_invests WHERE bid_no = ?";
		Query query = JPA.em().createNativeQuery(sql).setParameter(1, bidNo);
		List resultList = query.getResultList();
		if(resultList!=null&&resultList.size()>0 && resultList.get(0) != null){
			return Double.valueOf( resultList.get(0)==null?"0.00":resultList.get(0).toString());
		}
		return 0.00;
	}
	
	public t_invests buildInvest(){
		t_invests invests = new t_invests();
		invests.user_id = this._user_id;
		invests.time = new Date();
		invests.bid_id = this._bid_id;
		invests.ips_bill_no = this._ips_bill_no;
		invests.amount = this._amount;
		
		invests.bid_no = this._bid_no;
		
		return invests;
	}
	
	
	

}
