package business;

import java.util.Date;

import javax.persistence.Query;

import models.t_bids;
import play.Logger;
import play.db.helper.JpaHelper;
import play.db.jpa.JPA;
import utils.ErrorInfo;

/**
 * 标的业务类
 *
 * @author hys
 * @createDate  2015年1月5日 下午3:39:25
 *
 */
public class Bid {

	public long id;
	
	public Date time;
	public String bidNo;
	public double remainFee;
	public boolean isSuccess;
	
	//add by yangxuan 20141231
	public double amount;  //借款金额
	public double has_invested_amount;  //已投总额(冗余)
	public int version;  //版本--（用于控制并发）
	public long bid_id;  //标的ID
	//added
	
	public Bid(String bidNo, double remainFee) {
		this.bidNo = bidNo;
		this.remainFee = remainFee;
	}

	public Bid() {
		super();
	}

	/**
	 * 创建bid，保存借款管理费，标的状态为失败
	 * @param bidNo
	 * @param remainFee
	 * @param error
	 * @return
	 */
	public static long create(String bidNo, double remainFee, ErrorInfo error){
		error.clear();

		t_bids bid = t_bids.find("bid_no = ?", bidNo).first();
		if(bid != null){
			Logger.error("保存标的时：%s", "bid标号重复");
			return bid.id;
		}
		
		bid = new t_bids();

		bid.time = new Date();
		bid.bid_no = bidNo;
		bid.remain_fee = remainFee;	
		bid.is_success = false;	

		try {
			bid.save();
		} catch (Exception e) {
			Logger.error("添加标的时：%s",e.getMessage());
			error.code = -1;
			error.msg = "数据库异常";
			JPA.setRollbackOnly();

			return error.code;
		}
		
		error.code = 0;
		error.msg = "添加标的成功";

		return bid.id;
	}

	/**
	 * 查询剩余借款管理费
	 * @param bidNo
	 * @return
	 */
	public static double findRemainFee(String bidNo) {
        t_bids bid = t_bids.find("bid_no = ?", bidNo).first();

        if(bid == null){
        	Logger.info("查询剩余借款管理费时：%s", "标的不存在");
        	return 0;
        }
        
		return bid.remain_fee;
	}

	/**
	 * 更新剩余借款管理费
	 * @param bidNo
	 * @param fee
	 */
	public static void updateRemainFee(String bidNo, String fee) {
		JpaHelper.execute("update t_bids set remain_fee = ? where bid_no = ?",Double.parseDouble(fee.trim()),bidNo).executeUpdate();
	}

	/**
	 * 更新标的状态
	 * @param bidNo
	 * @param status
	 */
	public static void updateStatus(String bidNo, boolean status) {
		JpaHelper.execute("update t_bids set is_success = ? where bid_no = ?",status,bidNo).executeUpdate();
	}

	public static t_bids findBidByNo(String bidNo) {
        t_bids bid = t_bids.find("bid_no = ?", bidNo).first();

        if(bid == null){
        	Logger.debug("查询标的时：%s", "标的不存在");
        	return null;
        }
        
		return bid;
	}
	
	/**
	 * 通过bidNo查询标的信息
	 * @param bidNo
	 * @return
	 */
	private t_bids findBidByBidN0(String bidNo){
		
		t_bids bids = null;
		
		try{
			
			bids = t_bids.find("bid_no = ?", bidNo).first();
		
		}catch(Exception e){
			
			Logger.error("通过BidNo查询标的信息时：%s", e.getMessage());
			
		}
		return bids;
	}
	
	/**
	 * 保存标的信息
	 * @param error
	 */
	public void saveBid(ErrorInfo error){
		error.clear();
		
		t_bids bids = buildBid();
		t_bids bidsResult = findBidByBidN0(bids.bid_no);
		if(bidsResult==null){
			try{
				
				bids.save();
			
			}catch(Exception e){
				
				error.code = -1;
				error.msg = "保存标的信息失败";
				Logger.error("保存标的信息时： %s ", e.getMessage());
			}
		}
	}
	
	/**
	 * 修改标的投资金额(每次投标进行累加投资总额并且进行版本更新,版本更新用于并发处理)
	 * @param investPrice
	 */
	public static void modifyHasInvestedAmount(String bidNo,String investPrice,int version ,ErrorInfo error){
		error.clear();
		
		String sql ="update t_bids set  has_invested_amount =  has_invested_amount +?,version = version+1  WHERE bid_no =? and version = ?";
		Query query = JPA.em().createNativeQuery(sql).setParameter(1, investPrice).setParameter(2, bidNo).setParameter(3, version);
		int excute = query.executeUpdate();
		if(excute < 1){
			
			JPA.setRollbackOnly();
			Logger.info("修改标的投资金额时 ：%s", "失败");
			error.code = -1;
			error.msg = "修改标的投资金额时失败";
		}
	}
	
	/**
	 * 修改标的投资金额(每次投标进行累加投资总额,投资总额不能超过满标金额)
	 * @param investPrice
	 */
	public static void modifyHasInvestedAmount(String bidNo, double investAmount, ErrorInfo error){
		error.clear();
		
		String sql ="update t_bids set  has_invested_amount = has_invested_amount +? WHERE bid_no =? and has_invested_amount <= amount-? ";
		Query query = JPA.em().createNativeQuery(sql).setParameter(1, investAmount).setParameter(2, bidNo).setParameter(3, investAmount);
		int excute = query.executeUpdate();
		if(excute < 1){
			
			JPA.setRollbackOnly();
			Logger.info("修改标的投资金额时： %s", "已标满");
			error.code = -100;
			error.msg = "修改标的投资金额时已标满";
		}
	}
	
	/**
	 * 修改标的投资金额
	 * @param investPrice
	 */
	public static void modifyHasInvestedAmount(String bidNo,double hasInvestAmount) {
		String sql ="update t_bids set has_invested_amount = ? WHERE bid_no = ?";
		JPA.em().createNativeQuery(sql).setParameter(1, hasInvestAmount).setParameter(2, bidNo).executeUpdate();
	}
	
	/**
	 * 修改标的投资金额
	 * @param investPrice
	 */
	public static void modifyHasInvestedAmountByCallBack(String bidNo,double investPrice ,ErrorInfo error){
		error.clear();
		
		String sql ="update t_bids set  has_invested_amount =  has_invested_amount +?   WHERE bid_no =?  ";
		Logger.info("investPrice : %s", investPrice);
		Query query = JPA.em().createNativeQuery(sql).setParameter(1, investPrice).setParameter(2, bidNo);
		int excute = query.executeUpdate();
		if(excute < 1){
			
			JPA.setRollbackOnly();
			Logger.info("修改标的投资金额时 ：%s", "失败");
			error.code = -1;
			error.msg = "修改标的投资金额时失败";
		}
	}
	
	/**
	 * 修改标的投资金额(针对在投标时,t_bids中的已投资金额与t_invest表中的标的投资总额数据更新,但不进行version版本更新)
	 * @param investPrice
	 */
	public static void modifyHasInvestedAmount(String bidNo,String price,ErrorInfo error){
		error.clear();
		Logger.debug("投标之前t_bids信息与投资列表信息不符合，进行数据同步: price : %s", price);
		
		String sql ="update t_bids set  has_invested_amount = ? WHERE bid_no =?  ";
		Query query = JPA.em().createNativeQuery(sql).setParameter(1, price).setParameter(2, bidNo);
		int excute = query.executeUpdate();
		if(excute < 1){
			
			JPA.setRollbackOnly();
			Logger.error("修改标的投资金额时： %s", "失败");
			error.code = -1;
			error.msg = "修改标的投资金额时失败";
		}
	}
	
	/**
	 * 构造标
	 * @return
	 */
	public t_bids buildBid(){
		t_bids bids = new t_bids();
		bids.time = new Date();
		bids.bid_no = this.bidNo;
		bids.amount = this.amount;
		bids.has_invested_amount = this.has_invested_amount;
		bids.version = this.version;
		bids.bid_id = this.bid_id;
		return bids;
	}

}
