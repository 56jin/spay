package business;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.Query;

import org.apache.commons.lang.StringUtils;

import models.t_bids;
import models.t_supervisors;
import models.t_transfer_batches;
import play.Logger;
import play.db.helper.JpaHelper;
import play.db.jpa.JPA;
import utils.ErrorInfo;
import utils.RegexUtils;
import constants.SupervisorEvent;
import constants.Constants.Sex;
import constants.Constants.SupervisorLevel;

/**
 * 分批处理转账，业务类
 *
 * @author hys
 * @createDate  2015年1月5日 下午3:39:25
 *
 */
public class TransferBatches {

	public long id;
	
	public Date time;
	public int batchNo;  //分批编号
	public String bidBillNo;  //发标第三方流水号
	public String transferBillNos;  //转账流水号（第三方）
	public int transferType;  //交易类型。1、投标  2、还款
	public int status;  //0、未处理  1、处理中	2、转账成功  3、转账失败
	
	public TransferBatches(int batchNo,String bidBillNo, String transferBillNos,int transferType,int status) {
		this.time = new Date();
		this.batchNo = batchNo;
		this.bidBillNo = bidBillNo;
		this.transferBillNos = transferBillNos;
		this.transferType = transferType;
		this.status = status;
	}

	public TransferBatches() {
		super();
	}

	public long create(ErrorInfo error){
		error.clear();

		t_transfer_batches t_transfer_batch = new t_transfer_batches();

		fillEntity(t_transfer_batch);

		try {
			t_transfer_batch.save();
		} catch (Exception e) {
			Logger.error("添加转账批次时：%s",e.getMessage());
			error.code = -1;
			error.msg = "数据库异常";
			JPA.setRollbackOnly();

			return error.code;
		}
		
		error.code = 0;
		error.msg = "添加转账批次";

		return t_transfer_batch.id;
	}

	private void fillEntity(t_transfer_batches t_transfer_batches) {
		t_transfer_batches.time = this.time;
		t_transfer_batches.batch_no = this.batchNo;
		t_transfer_batches.bid_bill_no = this.bidBillNo;
		t_transfer_batches.transfer_bill_nos = this.transferBillNos;
		t_transfer_batches.transfer_type = this.transferType;
		t_transfer_batches.status = this.status;
	}

	public static TransferBatches pollRemainBill(String bidBillNo) {
		t_transfer_batches t_transfer_batch = t_transfer_batches.find("bid_bill_no = ? and status = 0", bidBillNo).first();
		
		if(t_transfer_batch == null){
			return null;
		}
		
		TransferBatches batch = new TransferBatches();
		batch.fillBusEntity(t_transfer_batch);
		
		return batch;
	}

	private void fillBusEntity(t_transfer_batches t_transfer_batch) {
		this.id = t_transfer_batch.id;
		this.time = t_transfer_batch.time;
		this.batchNo = t_transfer_batch.batch_no;
		this.bidBillNo = t_transfer_batch.bid_bill_no;
		this.transferBillNos = t_transfer_batch.transfer_bill_nos;
		this.transferType = t_transfer_batch.transfer_type;
		this.status = t_transfer_batch.status;
	}

	public static List<TransferBatches> queryByBidBillNo(String bidBillNo) {
		List<t_transfer_batches> list = t_transfer_batches.find("bid_bill_no = ?", bidBillNo).fetch();
		if(list == null || list.size()<=0 || list.get(0) == null){
			return null;
		}
		
		List<TransferBatches> transferBatches = new ArrayList<TransferBatches>();
		for(t_transfer_batches t_transfer_batch : list){
			TransferBatches batch = new TransferBatches();
			batch.fillBusEntity(t_transfer_batch);
			transferBatches.add(batch);
		}
		
		return transferBatches;
	}

	/**
	 * 更新分批转账状态
	 * @param id
	 * @param status
	 */
	public static void updateStatus(long id, int status, ErrorInfo error) {
		String sql = "update t_transfer_batches set status = ? where id = ?";

		try{
			JpaHelper.execute(sql,status,id).executeUpdate();
		}catch(Exception e){
			Logger.error("更新分批转账状态时：%s", e.getMessage());
			error.code = -1;
			error.msg = "数据库异常";
			return;
		}
		
		error.code = 1;
	}
}
