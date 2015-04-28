package models;

import java.util.Date;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Transient;
import org.junit.Test;
import com.shove.security.Encrypt;
import constants.Constants;
import play.Logger;
import play.db.jpa.Model;

/**
 * 管理员事件
 * @author lzp
 * @version 6.0
 * @created 2014-6-18
 */
@Entity
public class v_supervisor_events extends Model {
	public long supervisor_id;
	public Date time;
	public String ip;
	public int type_id;
	public String descrption;
	public String content;
	public String supervisor_name;
	public int supervisor_level;
	public String type_name;
	public String type_description;
	public String ukey;
}
