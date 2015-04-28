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
public class t_member_event_types extends Model {
	public String name;
}
