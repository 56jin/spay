package models;

import java.util.Date;

import javax.persistence.Entity;

import play.db.jpa.Model;
/**
 * 平台会员关系表
 * @author cp
 * @version 6.0
 * @created 2014年7月29日 下午7:38:33
 */
@Entity
public class t_member_of_platforms extends Model{

	public long member_id;
	public long platform_id;
	public long platform_member_id;
	public String platform_member_name;
	public String platform_member_account;
	public String platform_member_account_id;
	public String auth_payment_number;
	public String auth_invest_number;
	public String card_no;  //提现银行卡号  add by hys
	public int card_status;
}
