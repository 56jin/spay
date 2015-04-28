package business;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import models.t_loans_details;
import play.Logger;
import play.db.jpa.JPA;
import utils.ErrorInfo;

/**
 * 汇付自动放款业务类
 * @author yx
 *	@create 2014年12月11日 下午7:05:36
 */
public class Loans implements Serializable{
	
	public long id;
	public Date time;  //操作日期
	public String oriMerBillNo;  //原商户订单号
	public String trdAmt;  //转账金额
	public String fAcctType;  //转出方账户类型0#机构;1#个人
	public String fIpsAcctNo;  //转出方托管账户号.
	public String fTrdFee;  //转出方明细手续
	public String tAcctType;  //转入方账户类型
	public String tIpsAcctNo;  //转入方托管账户号
	public String tTrdFee;  //转入方明细手续
	public String ipsBillNo;  //冻结标志
	public String merBillNo;  //商户订单号
	public String bidNo;  //标的号
	public int status;  //1成功0失败 

	private long _id;
	private Date _time;  //操作日期
	private String _oriMerBillNo;  //原商户订单号
	private String _trdAmt;  //转账金额
	private String _fAcctType;  //转出方账户类型0#机构;1#个人
	private String _fIpsAcctNo;  //转出方托管账户号.
	private String _fTrdFee;  //转出方明细手续
	private String _tAcctType;  //转入方账户类型
	private String _tIpsAcctNo;  //转入方托管账户号
	private String _tTrdFee;  //转入方明细手续
	private String _ipsBillNo;  //冻结标志
	private String _merBillNo;  //商户订单号
	private String _bidNo;  //标的号
	private int _status;  //1成功0失败 
	
	public String getIpsBillNo() {
		return this._ipsBillNo;
	}

	public void setIpsBillNo(String ipsBillNo) {
		this._ipsBillNo = ipsBillNo;
	}

	public long getId() {
		return this._id;
	}

	public void setId(long id) {
		
		try{
	
			t_loans_details loans = t_loans_details.findById(id);
			this._id = loans.id;
			
		}catch(Exception e){
			
			Logger.error("汇付天下自动放款记录填充ID时 ", e.getMessage());
			
		}
		
	}

	public Date getTime() {
		return this._time;
	}

	public void setTime(Date time) {
		this._time = time;
	}

	public String getOriMerBillNo() {
		return this._oriMerBillNo;
	}

	public void setOriMerBillNo(String oriMerBillNo) {
		this._oriMerBillNo = oriMerBillNo;
	}

	public String getTrdAmt() {
		return this._trdAmt;
	}

	public void setTrdAmt(String trdAmt) {
		this._trdAmt = trdAmt;
	}

	public String getFAcctType() {
		return this._fAcctType;
	}

	public void setFAcctType(String fAcctType) {
		this._fAcctType = fAcctType;
	}

	public String getFIpsAcctNo() {
		return this._fIpsAcctNo;
	}

	public void setFIpsAcctNo(String fIpsAcctNo) {
		this._fIpsAcctNo = fIpsAcctNo;
	}

	public String getFTrdFee() {
		return this._fTrdFee;
	}

	public void setFTrdFee(String fTrdFee) {
		this._fTrdFee = fTrdFee;
	}

	public String getTAcctType() {
		return this._tAcctType;
	}

	public void setTAcctType(String tAcctType) {
		this._tAcctType = tAcctType;
	}

	public String getTIpsAcctNo() {
		return this._tIpsAcctNo;
	}

	public void setTIpsAcctNo(String tIpsAcctNo) {
		this._tIpsAcctNo = tIpsAcctNo;
	}

	public String getTTrdFee() {
		return this._tTrdFee;
	}

	public void setTTrdFee(String tTrdFee) {
		this._tTrdFee = tTrdFee;
	}

	public String getMerBillNo() {
		return this._merBillNo;
	}

	public void setMerBillNo(String merBillNo) {
		this._merBillNo = merBillNo;
	}

	public String getBidNo() {
		return this._bidNo;
	}

	public void setBidNo(String bidNo) {
		this._bidNo = bidNo;
	}

	public int getStatus() {
		return this._status;
	}

	public void setStatus(int status) {
		this._status = status;
	}
	
	private boolean isExist(String oriMerBillNom,String merBillNo){
		t_loans_details loans = null;
		try{
			
			loans = t_loans_details.find("oriMerBillNo=? and merBillNo=?", oriMerBillNo,merBillNo).first();
			
		}catch(Exception e){
			
			Logger.error("汇付支付放款查询记录时:", e.getMessage());
			return true;
		}
		return loans==null ? false:true;
		
		
	}

	/**
	 * 保存汇付天下交易记录
	 * @param error
	 */
	public void saveLoans(ErrorInfo error){
		
		error.clear();
		
		t_loans_details loans = buildLoans();
		if(isExist(loans.oriMerBillNo,loans.merBillNo))
			return;
		
		try{
			
			loans.save();
		
		}catch(Exception e){
			
			Logger.error("保存汇付放款交易记录时: %s",e.getMessage());
			error.code = -1;
			error.msg = "数据库异常";
			
			JPA.setRollbackOnly();
		}
	}
	
	/**
	 * 修改执行状态,汇付响应成功后,修改status字段
	 * @param error
	 * @return
	 */
	public int modifyStatus(ErrorInfo error){
		Logger.info("放款修改Status字段 start oriMerBillNo:%s ,merBillNo:%s",this._oriMerBillNo,this._merBillNo);
		
		t_loans_details loans = queryLoansByOriMerBillNo(this._oriMerBillNo,this._merBillNo);
		
		if(loans==null){
			Logger.info("loans==null");
				return 0;
		}
		if(loans.status ==1){
			Logger.info("loans.status ==1");
				return 0;
		}
		String sql = "update t_loans_details set status = ? where oriMerBillNo=? ";
		int result = 0;
		try{
			Logger.info("执行upadte set status =1 start ");
			result = JPA.em().createNativeQuery(sql).setParameter(1, 1).setParameter(2, this._oriMerBillNo).executeUpdate();
			Logger.info("执行upadte set status =1 end result : %s",result);
		}catch(Exception e){
			
			Logger.error("修改回复放款交易记录状态Status时:", e.getMessage());
			error.code = -1;
			error.msg = "数据库异常.";
			
			JPA.setRollbackOnly();
			return 0;
		}
		
		return result;
		
	}
	
	/**
	 * 查询一笔放款中,失败的交易记录条数
	 * @return
	 */
	public int queryLoansFailByMerBillNo(ErrorInfo error){
		
		error.clear();
		
		List<t_loans_details> list = null;
		
		try{
			
			list = t_loans_details.find("merBillNo=? and status=?", this._merBillNo,0).fetch();
		
		}catch(Exception e){
			
			Logger.error("查询一笔放款中,失败的交易记录条数时：", e.getMessage());
			error.code = -1;
			error.msg = "数据库异常.";
			JPA.setRollbackOnly();
			return -1;
			
		}
		
		if(list!=null)
				return list.size();
		return -1;
	}
	
	/**
	 * 通过oriMerBillNo和订单号查询放款记录
	 * @param oriMerBillNo
	 * @return
	 */
	private t_loans_details queryLoansByOriMerBillNo(String oriMerBillNo,String merBillNo){
		
		t_loans_details loans = null;
		
		try{
			
			loans = t_loans_details.find("oriMerBillNo = ? and merBillNo=?", oriMerBillNo,merBillNo).first();
		
		}catch(Exception e){
			
			Logger.error("通过oriMerBillNo查询放款记录时：", e.getMessage());
			JPA.setRollbackOnly();
			return null;
			
		}
		
		return loans;
	}
	
	/**
	 * 通过订单号查询放款批量信息
	 * @param billNo
	 * @param error
	 * @return
	 */
	public static List<t_loans_details> queryLoanListByBillNo(String billNo,ErrorInfo error){
		error.clear();
		
		List<t_loans_details> list = null;
		
		try{
			
			list = t_loans_details.find("merBillNo  = ?", billNo).fetch();
		
		}catch(Exception e){
			
			error.code = -1;
			error.msg = "通过订单号查询批量信息时异常";
			Logger.error("通过订单号查询批量信息时:%s", e.getMessage());
		}
		
		return list;
	}
	
	/**
	 * 构造汇付放款记录
	 * @return
	 */
	public  t_loans_details buildLoans(){
		t_loans_details loans = new t_loans_details();
		loans.time = new Date();
		loans.oriMerBillNo = this._oriMerBillNo;
		loans.trdAmt = this._trdAmt;
		loans.fAcctType = this._fAcctType;
		loans.fIpsAcctNo = this._fIpsAcctNo;
		loans.fTrdFee = this._fTrdFee;
		loans.tAcctType = this._tAcctType;
		loans.tIpsAcctNo = this._tIpsAcctNo;
		loans.tTrdFee=  this._tTrdFee;
		loans.ipsBillNo = this._ipsBillNo; 
		loans.merBillNo = this._merBillNo;
		loans.status = this._status;
		loans.bidNo = this._bidNo;
		return loans;
	}
	
	
	
	

}
