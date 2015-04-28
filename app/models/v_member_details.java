package models;

import java.util.Date;
import javax.persistence.Entity;
import play.db.jpa.Model;

/**
 * 交易记录（视图）
 * @author cp
 * @version 6.0
 * @created 2014年9月15日 下午8:07:03
 */
@Entity
public class v_member_details extends Model {
	public int platform_id;
	public String platform_name;
	public long member_id;
	public String member_name;
	public String serial_number;
	public Date time;
	public long operation;
	public String name;
	public double amount;
	public boolean status;
	public String summary;
}
