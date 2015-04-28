package models;

import javax.persistence.Entity;

import play.db.jpa.Model;
/**
 * 第三方支付信息
 * @author cp
 * @version 6.0
 * @created 2014年7月30日 上午11:16:35
 */
@Entity
public class t_payment_gateways extends Model{

	public String name;
	public String account;
	public String pid;
	public String _key;
	public String information;
	public boolean is_use;
	
	public t_payment_gateways() {
		
	}
	
	public t_payment_gateways(long id, String name) {
		this.id = id;
		this.name = name;
	}
}
