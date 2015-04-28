package models;

import javax.persistence.Entity;
import play.db.jpa.Model;

/**
 * 交易记录类型
 * @author lzp
 * @version 6.0
 * @created 2014-7-30
 */
@Entity
public class t_member_detail_types extends Model {
	public String name;
	public int type;
	public String description;
}
