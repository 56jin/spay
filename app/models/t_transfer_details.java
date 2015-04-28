package models;

import java.util.Date;

import javax.persistence.Entity;

import play.db.jpa.Model;

/**
 * 转账实体
 * @author yx
 *	@create 2015年1月5日 上午9:01:33
 */
@Entity
public class t_transfer_details extends Model{

	public Date time;  //创建时间
	public String pMerBillNo;  //流水号
	public String orderId ;  //订单号
	public double transAmt;  //金额
	public String inCustId;  //入账号-资金托管平台账号
	public String outCustId;  //出账号-资金托管平台账号
	public int status;  //交易状态0:失败、1：成功
	
	
}
