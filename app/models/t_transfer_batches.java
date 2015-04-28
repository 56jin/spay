package models;

import java.util.Date;

import javax.persistence.Entity;

import play.db.jpa.Model;

/**
 * 分批处理转账，缓存表
 * @author hys
 * @version 6.0
 * @created 2014-12-15
 */
@Entity
public class t_transfer_batches extends Model {
	
	public Date time;
	public int batch_no;  //分批编号
	public String bid_bill_no;  //发标第三方流水号
	public String transfer_bill_nos;  //转账流水号（第三方）
	public int transfer_type;  //交易类型。1、投标  2、还款
	public int status;  //0、未处理  1、处理中	2、转账成功  3、转账失败	
}
