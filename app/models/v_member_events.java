package models;

import java.util.Date;

import javax.persistence.Entity;

import play.db.jpa.Model;

/**
 * 事件类型
 * @author cp
 * @version 6.0
 * @created 2014年9月15日 上午9:29:40
 */
@Entity
public class v_member_events extends Model {
	public long platform_id;
	public String platform_name;
	public long member_id;
	public String member_name;
	public Date time;
	public long type_id;
	public String name;
	public String descrption;
}
