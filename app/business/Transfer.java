package business;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.persistence.Query;

import models.t_loans_details;
import models.t_transfer_details;
import play.Logger;
import play.db.jpa.JPA;
import utils.ErrorInfo;

/**
 *  转账业务实体类
 * @author yx
 *	@create 2015年1月5日 上午9:05:07
 */
public class Transfer implements Serializable{
	
	public long id;
	public Date time;  //创建时间
	public String pmerBillNo;  //流水号
	public String orderId;  //订单号
	public double transAmt;  //金额
	public String inCustId;  //入账号-资金托管平台账号
	public String outCustId;  //出账号-资金托管平台账号
	public int status;  //交易状态0:失败、1：成功
	
	
	private long _id;
	private Date _time;  //创建时间
	private String _pmerBillNo;  //流水号
	private String _orderId;  //订单号
	private double _transAmt;  //金额
	private String _inCustId;  //入账号-资金托管平台账号
	private String _outCustId;  //出账号-资金托管平台账号
	private int _status;  //交易状态0:失败、1：成功
	
	public long getId() {
		Logger.info("getId");
		return this._id;
	}
	public void setId(long id) {
		Logger.info("setId");
		this._id = id;
	}
	
 
	public String getOrderId() {
		return this._orderId;
	}
	public void setOrderId(String orderId) {
		this._orderId = orderId;
	}
	public String getPmerBillNo() {
		return this._pmerBillNo;
	}
	public void setPmerBillNo(String pmerBillNo) {
		this._pmerBillNo = pmerBillNo;
	}
	public double getTransAmt() {
		return this._transAmt;
	}
	public void setTransAmt(double transAmt) {
		this._transAmt = transAmt;
	}
	public String getInCustId() {
		return this._inCustId;
	}
	public void setInCustId(String inCustId) {
		this._inCustId = inCustId;
	}
	public String getOutCustId() {
		return this._outCustId;
	}
	public void setOutCustId(String outCustId) {
		this._outCustId = outCustId;
	}
	public int getStatus() {
		return this._status;
	}
	public void setStatus(int status) {
		this._status = status;
	}
	public Date getTime() {
		return this._time;
	}
	public void setTime(Date time) {
		this._time = time;
	}
	
	/**
	 * 修改状态
	 * @param error
	 */
	public void modifyStatus(int status ,String orderId,ErrorInfo error){
		error.clear();
		
		String sql = "update t_transfer_details set status =? where orderId= ?";
		Query query = JPA.em().createNativeQuery(sql).setParameter(1, status).setParameter(2, orderId);
		int result = query.executeUpdate();
		if(result<1){
			
			Logger.error("修改转账状态失败");
			error.code = -1;
			error.msg = "修改转账状态失败";
			
		}
	}
	
	/**
	 * 通过订单号、状态查询转账信息
	 * @param status
	 * @param pmerBillNo
	 * @param error
	 * @return
	 */
	public List<t_transfer_details> findTransferByStatus(int status,String pmerBillNo , ErrorInfo error){
		error.clear();
		
		List<t_transfer_details> list = null;
		
		try{
			
			list = t_transfer_details.find("pmerBillNo  = ? and status = ?", pmerBillNo,status).fetch();
		
		}catch(Exception e){
			
			error.code = -1;
			error.msg = "通过订单号、状态查询转账信息时异常";
			Logger.error("通过订单号、状态查询转账信息时:%s", e.getMessage());
		}
		return list;
	}
	
	/**
	 * 保存转账信息
	 * @param error
	 */
	public void saveTransfer(ErrorInfo error){
		error.clear();
		
		t_transfer_details details = buildTransfer();
		t_transfer_details transfer = findTransferByBillNo(details.orderId);
		
		if(transfer==null){
			
			try{
				details.save();
			}catch(Exception e){
				
				JPA.setRollbackOnly();
				Logger.error("保存转账信息时%s", e.getMessage());
				error.code = -1;
				error.msg = "保存转账信息时异常";
			}
		}
		
	}
	
	/**
	 * 通过流水号查询转账信息
	 * @return
	 */
	private t_transfer_details findTransferByBillNo(String orderId){
		
		t_transfer_details details = null;
		
		try{
		
			details = t_transfer_details.find("orderId= ?", orderId).first();
			
		}catch(Exception e){
			
			Logger.error(" 通过流水号查询转账信息时:%s", e.getMessage());
			
		}
		
		return details;
	}
	
	/**
	 * 构造转账实体
	 * @return
	 */
	private t_transfer_details buildTransfer(){
		t_transfer_details transfer = new t_transfer_details();
		transfer.time = new Date();
		transfer.pMerBillNo = this._pmerBillNo;
		transfer.orderId = this._orderId;
		transfer.transAmt = this._transAmt;
		transfer.inCustId = this._inCustId;
		transfer.outCustId = this._outCustId;
		transfer.status = 0;
		return transfer;
	}
	 
	
	

}
