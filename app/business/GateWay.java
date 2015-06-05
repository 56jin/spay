package business;

import constants.SupervisorEvent;
import models.t_payment_gateways;
import net.sf.json.JSONObject;
import play.Logger;
import play.db.jpa.JPA;
import utils.ErrorInfo;

import javax.persistence.Query;
import java.util.*;
import java.util.Map.Entry;


public class GateWay {

	public long id;
	private long _id;
	
	public void setId(long id) {
		if(id <= 0) {
			return ;
		}
		
		t_payment_gateways gateway = t_payment_gateways.findById(id);
		
		if(gateway == null) {
			this._id  = -1;
			
			return;
		}
		
		this._id = gateway.id;
		this.name = gateway.name;
		this.account = gateway.account;
		this.pid = gateway.pid;
		this.key = gateway._key;
		this.isUse = gateway.is_use;
		this.information = gateway.information;
	}
	
	public long getId() {
		return _id;
	}
	
	public String name;
	public String account;
	public String pid;
	public String key;
	public String information;
	public Map<String, String> keyInfo;
	private Map<String, String> _keyInfo;
	
	public void setKeyInfo(Map<String, String> keyInfo) {
		this._keyInfo = keyInfo;
	}
	
	public Map<String, String> getKeyInfo() {
		this.keyInfo = new HashMap<String, String>();
		
		if(this.information == null) {
			return this.keyInfo;
		}
		
		JSONObject json = JSONObject.fromObject(this.information);
		Iterator<String> iterator = json.keys();
		
		while(iterator.hasNext()) {
			String key = iterator.next();
			String value = json.getString(key);
			
//			if("PUB_KEY".equals(key)) {
//				value = value.replace("#", "\n");
//			}
			
			this.keyInfo.put(key, value);
		}
		
		return keyInfo;
	}
	
	public boolean isUse;
	

	public static List<t_payment_gateways> queryAll(ErrorInfo error) {
		error.clear();
		List<t_payment_gateways> ways = new ArrayList<t_payment_gateways>();
		
		String sql = "select new t_payment_gateways(id, name) from t_payment_gateways";
		
		try{
			ways = t_payment_gateways.find(sql).fetch();
		}catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询当前所有使用的支付方式时：" + e.getMessage());
			
//			error.code = -1;
//			error.msg = "查询当前所有使用的支付方式失败";
			
			return null;
		}
		
		return ways;
	}
	
	/**
	 * 更新
	 * @param gateWayId
	 * @param error
	 */
	public void update(long gateWayId, ErrorInfo error) {
		error.clear();
		
		if(this._keyInfo == null) {
			error.code = -1;
			error.msg = "请传入有效参数";
			
			return ;
		}
		
		JSONObject info = new JSONObject();
		Set<Entry<String, String>> entrySet =  this._keyInfo.entrySet();
		
		for(Entry<String, String> entry : entrySet) {
			info.put(entry.getKey(), entry.getValue());
		}
		
		String sql = "update t_payment_gateways set name = ?, account = ?, pid = ?, _key = ?, information = ?, is_use = ? where id = ?";
		Query query = JPA.em().createQuery(sql).setParameter(1, this.name).setParameter(2, this.account).setParameter(3, this.pid)
						.setParameter(4, this.key).setParameter(5, info.toString()).setParameter(6, this.isUse).setParameter(7, gateWayId);
		
		int rows= 0;
		
		try {
			rows = query.executeUpdate();
		}catch(Exception e) {
			e.printStackTrace();
			Logger.info("保存修改的资金托管设置时：" + e.getMessage());
			
			return;
		}
		
		if(rows == 0) {
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "数据未更新";
			
			return ;
		}
		
		DealDetail.supervisorEvent(Supervisor.currSupervisor().id, SupervisorEvent.GATEWAY_SET, "资金托管设置", error);
		
		if(error.code < 0) {
			JPA.setRollbackOnly();
			return ;
		}
		
		error.code = 0;
		error.msg = "资金托管设置保存成功！";
	}
	
	/**
	 * @author yangxuan
	 *   通过ID查询网关信息
	 * @param id
	 * @return
	 */
	public static  t_payment_gateways queryGateWayById(long id){
		
		t_payment_gateways gayGateways = null;
		
		try{
			
			gayGateways =  t_payment_gateways.findById(id);
		
		}catch(Exception e){
			
			Logger.error("查询网关时:", e.getMessage());
			JPA.setRollbackOnly();
		}
				
		return gayGateways;
	}
	
	public static void main(String[] args) {
		GateWay g = new GateWay();
		ErrorInfo error = new ErrorInfo();
		Map<String, String> keyInfo = new HashMap<String, String>();
		keyInfo.put("t1", "t1");
		keyInfo.put("t2", "t2");
		keyInfo.put("t3", "t3");
		g.setKeyInfo(keyInfo);
		
//		g.add(error);
	}
}
