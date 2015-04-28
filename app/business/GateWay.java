package business;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.Query;

import net.sf.json.JSONObject;
import constants.Constants;
import constants.IPSConstants;
//import constants.LoanConstants;
import constants.SupervisorEvent;
import constants.UserEvent;
import play.Logger;
import play.db.helper.JpaHelper;
import play.db.jpa.JPA;
import utils.ErrorInfo;
import models.t_payment_gateways;

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
	
	public static Map<Long, GateWay> queryGateWay() {
		List<t_payment_gateways> ways = new ArrayList<t_payment_gateways>();
		
		try{
			ways = t_payment_gateways.find("is_use = ?", Constants.USE).fetch();
		}catch (Exception e) {
			e.printStackTrace();
			Logger.info("查询当前所有使用的支付方式时：" + e.getMessage());
			
//			error.code = -1;
//			error.msg = "查询当前所有使用的支付方式失败";
			
			return null;
		}
		
		Map<Long, GateWay> gateWays = new HashMap<Long, GateWay>();
		GateWay gateWay = null;
		
		for(t_payment_gateways way : ways) {
			gateWay = new GateWay();
			
			gateWay._id = way.id;
			gateWay.name = way.name;
			gateWay.account = way.account;
			gateWay.pid = way.pid;
			gateWay.key = way._key;
			gateWay.isUse = way.is_use;
			gateWay.information = way.information;
			
//			JSONObject json = JSONObject.fromObject(gateWay.information);
//			Iterator<String> iterator = json.keys();
//			gateWay.keyInfo = new HashMap<String, String>();
//			
//			while(iterator.hasNext()) {
//				String key = iterator.next();
//				String value = json.getString(key);
//				
//				if("PUB_KEY".equals(key)) {
//					value = value.replace("#", "\n");
//				}
//				
//				gateWay.keyInfo.put(key, value);
//			}
			
			gateWays.put(gateWay._id,gateWay);
		}
		
		return gateWays;
	}
	
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
		
		/*如果对支付账户进行了编辑，那么需要对对应的常量重新赋值*/
		switch((int)gateWayId) {
		case Constants.IPS:
			IPSConstants.CERT_MD5 = info.getString("CERT_MD5");
			IPSConstants.PUB_KEY = info.getString("PUB_KEY").replace("#", "\n");
			IPSConstants.DES_KEY = info.getString("DES_KEY");
			IPSConstants.DES_IV = info.getString("DES_IV");
			break;
//		case Constants.LOAN:
//			LoanConstants.argMerCode = info.getString("argMerCode");
//			LoanConstants.signRate = info.getString("signRate");
//			LoanConstants.publicKey = info.getString("publicKey");
//			LoanConstants.privateKeyPKCS8 = info.getString("privateKeyPKCS8");
//			Logger.info("===================双乾支付常量更新=================");
//			Logger.info("argMerCode = %s", LoanConstants.argMerCode==null?"null":"ok");
//			Logger.info("signRate = %s", LoanConstants.signRate==null?"null":"ok");
//			Logger.info("publicKey = %s", LoanConstants.publicKey==null?"null":"ok");
//			Logger.info("privateKeyPKCS8 = %s", LoanConstants.privateKeyPKCS8==null?"null":"ok");
//			break;
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
