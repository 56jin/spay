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
 * 未处理成功的请求的信息
 * @author cp
 * @version 6.0
 * @created 2014年7月30日 上午11:16:27
 */
@Entity
public class t_untreated_imformation extends Model {
	
	public Date time;
	public String information;
	public int gateway_id;
	public int count;
	public boolean status;
}
