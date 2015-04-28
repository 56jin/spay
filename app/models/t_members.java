package models;

import java.math.BigDecimal;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;

import static javax.persistence.GenerationType.IDENTITY;

import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import play.data.validation.Email;
import play.data.validation.Match;
import play.data.validation.MaxSize;
import play.data.validation.Min;
import play.data.validation.MinSize;
import play.data.validation.Required;
import play.db.jpa.Model;

/**
 * 用户
 * @author cp
 * @version 6.0
 * @created 2014年7月30日 上午11:18:30
 */
@Entity
public class t_members extends Model {
	
	public Date time;
	public String name;
	public String password;
	public String id_number;
	public String mobile;
	public String serial_number;
	public boolean status;
}
