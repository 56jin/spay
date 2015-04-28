package models;

import java.util.Date;

import javax.persistence.Entity;

import play.db.jpa.Model;

/**
 * 平台，第三方支付关系表
 * @author cp
 * @version 6.0
 * @created 2014年7月30日 上午11:17:20
 */
@Entity
public class t_platforms extends Model{

	public Date time;
	public String name;
	public String domain;
	public int gateway_id;
	public String encryption;
	public boolean status;
	public int use_type;
	public boolean deal_status;
}
