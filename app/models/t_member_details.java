package models;

import java.util.Date;
import javax.persistence.Entity;
import play.db.jpa.Model;

/**
 * 交易记录
 * @author lzp
 * @version 6.0
 * @created 2014-7-30
 */
@Entity
public class t_member_details extends Model {
	public long member_id;
	public Date time;
	public int platform_id;
	public String serial_number;
	public long operation;
	public double amount;
	public boolean status;
	public String summary;
}
