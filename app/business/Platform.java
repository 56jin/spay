package business;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;

import org.apache.commons.lang.StringUtils;

import constants.Constants;
import constants.ViewTemplete;
import models.t_payment_gateways;
import models.t_platforms;
import models.v_member_events;
import models.v_platforms;
import play.Logger;
import play.db.jpa.JPA;
import utils.ErrorInfo;
import utils.PageBean;

public class Platform {

	public long id;
	private long _id;
	
	public void setId(long id) {
		if(id <= 0) {
			this._id = -1;
			return ;
		}
		
		t_platforms platform = t_platforms.findById(id);
		
		if(platform == null) {
			this._id = id;
			return;
		}
		
		this._id = platform.id;
		this.time = platform.time;
		this.name = platform.name;
		this.domain = platform.domain;
		this.gatewayId = platform.gateway_id;
		this.encryption = platform.encryption;
		this.status = platform.status;
		this.useType = platform.use_type;
		this.account = t_payment_gateways.find("select account from t_payment_gateways where id = ?", this._id).first();
	}
	
	public long getId() {
		return this._id;
	}
	
	public Date time;
	public String name;
	public String domain;
	
	public void setDomain(String domain) {
		if(StringUtils.isBlank(domain)) {
			this.domain = null;
			return ;
		}
		
		t_platforms platform = t_platforms.find("domain = ?", domain).first();
		
		if(platform == null) {
			this.domain = domain;
			return;
		}
		
		this._id = platform.id;
		this.time = platform.time;
		this.name = platform.name;
		this.domain = domain;
		this.gatewayId = platform.gateway_id;
		this.encryption = platform.encryption;
		this.status = platform.status;
		this.useType = platform.use_type;
	}
	
	public int gatewayId;
	public String account;
	public String encryption;
	public boolean status;
	public int useType;
	public boolean dealStatus;
	
	public static int queryGatewayByDomain(String domain, ErrorInfo error) {
		error.clear();
		
		List<Integer> gateways = null;
		
		String sql = "select gateway_id from t_platforms where domain = ?";
		
		try{
			gateways = t_platforms.find(sql, domain).fetch();
		}catch(Exception e) {
			e.printStackTrace();
			Logger.info("通过域名查询支付接口时："+e.getMessage());
			
			error.code = -1;
			error.msg = "查询支付接口失败";
			
			return -1;
		}
		
		if(gateways == null || gateways.size() == 0) {
			error.code = -1;
			error.msg = "此域名未添加支付账号";
			
			return -1;
		}
		
		return gateways.get(0);
	}
	
	public static long queryPlatformIdByDomain(String domain, ErrorInfo error) {
		error.clear();
		
		List<Long> platformIds = null;
		
		String sql = "select id from t_platforms where domain = ?";
		
		try{
			platformIds = t_platforms.find(sql, domain).fetch();
		}catch(Exception e) {
			e.printStackTrace();
			Logger.info("通过域名查询支付接口时："+e.getMessage());
			
			error.code = -1;
			error.msg = "查询支付接口失败";
			
			return -1;
		}
		
		if(platformIds == null || platformIds.size() == 0) {
			error.code = -1;
			error.msg = "此域名未注册到托管平台";
			
			return -1;
		}
		
		return platformIds.get(0);
	}
	
	/**
	 * 保存接入平台设置信息
	 * @param error
	 */
	public void savePlatform(ErrorInfo error) {
		error.clear();
		
		Platform.isEncrytionExist(this.encryption, error);
		
		if(error.code < 0) {
			return ;
		}
		
		t_platforms platform = new t_platforms();
		
		platform.time = new Date();
		platform.name = this.name;
		platform.domain = this.domain;
		platform.gateway_id = this.gatewayId;
		platform.encryption = this.encryption;
		platform.status = true;
		platform.use_type = 2;
		
		try{
			platform.save();
		}catch(Exception e) {
			e.printStackTrace();
			Logger.info("保存接入平台设置时：" + e.getMessage());
			
			error.code = -1;
			error.msg = "保存接入平台设置失败";
			
			return ;
		}
		
		error.code = 0;
		error.msg = "接入平台设置保存成功";
	}
	
	/**
	 * 判断域名是否已存在
	 * @param error
	 */
	public static void isEncrytionExist(String encryption, ErrorInfo error) {
		error.clear();
		
		long rows = 0;
		
		try {
			rows = t_platforms.count("encryption = ?", encryption);
		}catch(Exception e) {
			e.printStackTrace();
			Logger.info("判断域名是否存在时：" + e.getMessage());
			
			error.code = -1;
			error.msg = "判断域名是否存在失败";
			
			return;
		}
		
		if(rows > 0) {
			error.code = -1;
			error.msg = "对不起，该绑定域名已存在";
			
			return;
		}
		
		error.code = 0;
	}
	
	public static PageBean<v_platforms> queryPlatform(int type, String keyword, Date startDate,
			Date endDate, int orderStatus, int currPage, int pageSize, ErrorInfo error) {
		error.clear();
		
		if(currPage == 0) {
			currPage = 1;
		}
		
		if(pageSize == 0) {
			pageSize = Constants.PAGE_SIZE;
		}
		
		StringBuffer conditions = new StringBuffer("1=1 ");
		List<Object> values = new ArrayList<Object>();
		Map<String,Object> conditionMap = new HashMap<String, Object>();
		
		conditionMap.put("condition", type);
		conditionMap.put("keyword", keyword);
		conditionMap.put("startDate", startDate);
		conditionMap.put("endDate", endDate);
		conditionMap.put("orderStatus", orderStatus);
		
		if(type >= 0 && type <=2 && StringUtils.isNotBlank(keyword)) {
			conditions.append(Constants.PLATFORM_TYPE[type]);
			if(type == 0) {
				values.add("%"+keyword+"%");
			}
			values.add("%"+keyword+"%");
		}
		
		if(startDate != null) {
			conditions.append("and time >= ? ");
			values.add(startDate);
		}
		
		if(endDate != null) {
			conditions.append("and time <= ? ");
			values.add(endDate);
		}
		
		if(orderStatus > 0 && orderStatus <= 3) {
			conditions.append(Constants.PLATFORM_ORDER[orderStatus]);
		}
		
		List<v_platforms> platforms = new ArrayList<v_platforms>();
		int count = 0;
		
		try {
			count = (int) v_platforms.count(conditions.toString(), values.toArray()); 
			platforms = v_platforms.find(conditions.toString(), values.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询平台接入详情时："+e.getMessage());

			error.code = -1;
			error.msg = "查询平台接入详情失败";
			return null;
		}
		
		PageBean<v_platforms> page = new PageBean<v_platforms>();
		page.pageSize = pageSize;
		page.currPage = currPage;
		page.totalCount = count;
		page.conditions = conditionMap;
		
		page.page = platforms;
		
		return page;
	}
	
	/**
	 * 重新绑定接入平台
	 * @param error
	 */
	public void updatePlatform(long id, ErrorInfo error) {
		error.clear();
		
		if(this.dealStatus) {
			error.code = -1;
			error.msg = "对不起，绑定的接入平台已发生了交易，不能再进行重新绑定";
			
			return ;
		}
		
		t_platforms platform = t_platforms.findById(id);
		
		if(platform == null) {
			error.code = -1;
			error.msg = "原绑定关系不存在，无法进行重新绑定";
			
			return ;
		}
		
		if(!this.encryption.equals(platform.encryption)) {
			isEncrytionExist(this.encryption, error);
			
			if(error.code < 0) {
				
				return ;
			}
		}
		
		platform.id = this.id;
		platform.time = new Date();
		platform.name = this.name;
		platform.domain = this.domain;
		platform.gateway_id = this.gatewayId;
		platform.encryption = this.encryption;
		platform.status = true;
		platform.use_type = 2;
		
		try {
			platform.save();
		}catch(Exception e) {
			e.printStackTrace();
			Logger.info("重新绑定接入平台失败" + e.getMessage());
			
			error.code = -1;
			error.msg = "重新绑定接入平台失败";
			
			return;
		}
		
		error.code = 0;
		error.msg = "重新绑定接入平台成功";
	}
	
	/**
	 * 用于第一笔交易后改变建议状态
	 * @param domain
	 */
	public static void updateDealStatus(long platformId) {
		String sql = "update t_platforms set deal_status = 1 where id = ? and deal_status = 0";
		
		try {
			JPA.em().createQuery(sql).setParameter(1, platformId).executeUpdate();
		}catch(Exception e) {
			e.printStackTrace();
			Logger.info("更新可编辑状态失败" + e.getMessage());
		}
	}
}
