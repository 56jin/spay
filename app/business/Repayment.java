package business;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import models.t_loans_details;
import models.t_repayment_details;
import play.Logger;
import play.db.jpa.JPA;
import utils.ErrorInfo;

/**
 * 汇付天下自动扣款(还款)记录业务类
 * @author yx
 *	@create 2014年12月12日 下午5:17:59
 */
public class Repayment implements Serializable{
	public long id;
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
	
	private long _id;
	private Date _time;  //操作时间
	private String _ordId;  //订单号
	private String _ordDate;  //订单日期
	private String _outCustId;  //出账客户号
	private String _subOrdId;  //订单号
	private String _subOrdDate;  //订单日期
	private String _transAmt;  //交易金额
	private String _fee;  //扣款手续费
	private String _inCustId;  //入账客户号
	private String _reqExt;  //入参扩展域
	private String _memberId;  //互联网金融平台用户id
	private String _pBidNo;  //标的号
	private String _pMerBillNo;  //商户还款订单号
	private int _status;  //状态
	
	public Date getTime() {
		return this._time;
	}
	public void setTime(Date time) {
		this._time = time;
	}
	public String getOrdId() {
		return this._ordId;
	}
	public void setOrdId(String ordId) {
		this._ordId = ordId;
	}
	public String getOrdDate() {
		return this._ordDate;
	}
	public void setOrdDate(String ordDate) {
		this._ordDate = ordDate;
	}
	public String getOutCustId() {
		return this._outCustId;
	}
	public void setOutCustId(String outCustId) {
		this._outCustId = outCustId;
	}
	public String getSubOrdId() {
		return this._subOrdId;
	}
	public void setSubOrdId(String subOrdId) {
		this._subOrdId = subOrdId;
	}
	public String getSubOrdDate() {
		return this._subOrdDate;
	}
	public void setSubOrdDate(String subOrdDate) {
		this._subOrdDate = subOrdDate;
	}
	public String getTransAmt() {
		return this._transAmt;
	}
	public void setTransAmt(String transAmt) {
		this._transAmt = transAmt;
	}
	public String getFee() {
		return this._fee;
	}
	public void setFee(String fee) {
		this._fee = fee;
	}
	public String getInCustId() {
		return this._inCustId;
	}
	public void setInCustId(String inCustId) {
		this._inCustId = inCustId;
	}
	public String getReqExt() {
		return this._reqExt;
	}
	public void setReqExt(String reqExt) {
		this._reqExt = reqExt;
	}
	public String getMemberId() {
		return this._memberId;
	}
	public void setMemberId(String memberId) {
		this._memberId = memberId;
	}
	public String getPBidNo() {
		return this._pBidNo;
	}
	public void setPBidNo(String pBidNo) {
		this._pBidNo = pBidNo;
	}
	public String getPMerBillNo() {
		return this._pMerBillNo;
	}
	public void setPMerBillNo(String pMerBillNo) {
		this._pMerBillNo = pMerBillNo;
	}
	public int getStatus() {
		return this._status;
	}
	public void setStatus(int status) {
		this._status = status;
	}
	
	/**
	 * 根据ordId,memberId查询是否存在交易记录
	 * @param ordId
	 * @param memberId
	 * @return
	 */
	public boolean isExist(String ordId,String pMerBillNo){
		t_repayment_details repayment = null;
		
		try{
			
			repayment = t_repayment_details.find("ordId=? and pMerBillNo=?", ordId,pMerBillNo).first();
			
		}catch(Exception e){
			
			Logger.error("根据ordId,memberId查询是否存在交易记录时:", e.getMessage());
			return true;
		}
		
		return repayment==null?false:true;
	}
	
	/**
	 * 保存汇付还款记录
	 * @param error
	 */
	public void saveRepayment(ErrorInfo error){
		error.clear();
		
		t_repayment_details repayment = buildRepayment();
		
		if(isExist(this._ordId,this._pMerBillNo))
				return;
		
		try{
			
			repayment.save();
		
		}catch(Exception e){
			
			Logger.error("保存汇付还款记录时: %s",e.getMessage());
			error.code = -1;
			error.msg = "数据库异常";
			
			JPA.setRollbackOnly();
		}
	}
	
	/**
	 *  构造还款记录
	 * @return
	 */
	private t_repayment_details buildRepayment(){
		t_repayment_details repayment = new t_repayment_details();
		repayment.time = new Date();
		repayment.ordId = this._ordId;
		repayment.ordDate = this._ordDate;
		repayment.outCustId = this._outCustId;
		repayment.subOrdId = this._subOrdId;
		repayment.subOrdDate = this._subOrdDate;
		repayment.transAmt = this._transAmt;
		repayment.fee = this._fee;
		repayment.inCustId = this._inCustId;
		repayment.reqExt = this._reqExt;
		repayment.memberId = this._memberId;
		repayment.pBidNo = this._pBidNo;
		repayment.pMerBillNo = this._pMerBillNo;
		repayment.status = 0;
		
		return repayment;
	}
	
	/**
	 * 修改汇付还款交易记录状态Status
	 * @param error
	 * @return
	 */
	public int modifyStatus(ErrorInfo error) {
		
		t_repayment_details repayment = queryRepaymentByOriMerBillNo(this._ordId);
		
		if(repayment==null)
				return 0;
		
		if(repayment.status ==1)
				return 0;
		
		String sql = "update t_repayment_details set status = ? where ordId=? ";
		int result = 0;
		try{
			
			result = JPA.em().createNativeQuery(sql).setParameter(1, 1).setParameter(2, this._ordId).executeUpdate();
		
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
	 * 查询一笔还款中,失败的交易记录条数
	 * @return
	 */
	public int queryRepaymentFailByMerBillNo(ErrorInfo error){
		
		error.clear();
		
		List<t_repayment_details> list = null;
		
		try{
			
			list = t_repayment_details.find("pMerBillNo=? and status=?", this._pMerBillNo,0).fetch();
		
		}catch(Exception e){
			
			Logger.error("查询一笔还款中,失败的交易记录条数时：", e.getMessage());
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
	 * 通过ordId查询还款交易记录
	 * @param _ordId
	 * @return
	 */
	private t_repayment_details queryRepaymentByOriMerBillNo(String _ordId) {
		t_repayment_details repayment = null;
		
		try{
			
			repayment = t_repayment_details.find("ordId = ?", _ordId).first();
		
		}catch(Exception e){
			
			Logger.error("通过ordId查询还款记录时：", e.getMessage());
			JPA.setRollbackOnly();
			return null;
			
		}
		
		return repayment;
	}
	
	
	/**
	 * 通过订单号查询还款批量信息
	 * @param billNo
	 * @param error
	 * @return
	 */
	public static List<t_repayment_details> queryRepaymentListByBillNo(String billNo,ErrorInfo error){
		error.clear();
		
		List<t_repayment_details> list = null;
		
		try{
			
			list = t_repayment_details.find("pMerBillNo  like ?", billNo+"%").fetch();
		
		}catch(Exception e){
			
			error.code = -1;
			error.msg = "通过订单号查询还款批量信息时异常";
			Logger.error("通过订单号查询还款批量信息时:%s", e.getMessage());
		}
		
		return list;
	}
	
	
	
	

}
