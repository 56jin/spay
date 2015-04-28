package models;


import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import constants.Constants;

import play.db.jpa.Model;
import utils.Security;

/**
 * 平台
 * @author cp
 * @version 6.0
 * @created 2014年11月14日 下午1:58:49
 */
@Entity
public class v_platforms extends Model {

	public Date time;
	public String name;
	public String domain;
	public long gateway_id;
	public String encryption;
	public boolean status;
	public boolean use_type;
	public boolean deal_status;
	public String gateway;
}
