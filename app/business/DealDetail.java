package business;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;
import javax.swing.JApplet;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.DateUtils;

import com.shove.security.Encrypt;

import constants.Constants;
import constants.DealType;
import constants.IPSConstants;
import constants.OptionKeys;
import constants.SupervisorEvent;
import constants.YEEConstants;
import constants.YEEConstants.Status;
import controllers.YEE.YEEPayment;
import controllers.YEE.SecureSign.SignUtil;
import play.Logger;
import play.db.helper.JpaHelper;
import play.db.jpa.JPA;
import play.libs.WS;
import utils.Arith;
import utils.Converter;
import utils.DataUtil;
import utils.DateUtil;
import utils.ErrorInfo;
import utils.NumberUtil;
import utils.PageBean;
import utils.YEEUtil;
import models.t_bids;
import models.t_member_details;
import models.t_member_events;
import models.t_platforms;
import models.t_supervisor_events;
import models.v_supervisor_events;
import models.v_supervisors;

/**
 * 交易记录实体类
 * @author cp
 * @version 6.0
 * @created 2014年9月15日 下午8:16:11
 */
public class DealDetail implements Serializable{
	public long id;
	public long _id;
	public int platformId;
	public long memberId;
	public Date time;
	public String serialNumber;
	public long operation;
	public double amount;
	public boolean status;
	public String summary;
	
	public DealDetail() {
		
	}
	
	public DealDetail(int platformId, long memberId, String serialNumber, int operation, 
			double amount, boolean status, String summary) {
		this.platformId = platformId;
		this.memberId = memberId;
		this.serialNumber = serialNumber;
		this.operation = operation;
		this.amount= amount;
		this.status = status;
		this.summary = summary;
	}
	
	/**
	 * 添加交易记录
	 * @param error
	 */
	public boolean addDealDetail() {
		t_member_details detail = new t_member_details();
		
		detail.member_id = this.memberId;
		detail.time = new Date();
		detail.platform_id = this.platformId;
		detail.serial_number = this.serialNumber;
		detail.operation = this.operation;
		detail.amount = this.amount;
		detail.status = this.status;
		detail.summary = this.summary;
		
		try {
			detail.save();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.info("添加交易记录时："+e.getMessage());
			
			return false;
		}
		
		return true;
	}
	
	/**
	 * 添加交易记录
	 * @param error
	 */
	public static  boolean addDealDetail(long memberId,int platformId,String serialNumber,long operation,double amount,boolean status,String summary) {
	    t_member_details detail = queryMemberdetail(platformId,serialNumber);
		if(detail != null){  //交易记录已存在
			Logger.info("======插入交易记录信息时，%s","交易记录已存在");
			return true;
		}
		
		detail = new t_member_details();
		detail.member_id = memberId;
		detail.time = new Date();
		detail.platform_id = platformId;
		detail.serial_number = serialNumber;
		detail.operation = operation;
		detail.amount = amount;
		detail.status = status;
		detail.summary = summary;
		
		try {
			detail.save();
		} catch (Exception e) {
			Logger.info("添加交易记录时："+e.getMessage());
			
			return false;
		}
			
		return true;
	}
	
	/**
	 * 查询交易记录
	 * @param platformId2
	 * @param serialNumber2
	 * @return
	 */
	private static t_member_details queryMemberdetail(int platformId,String serialNumber) {
		t_member_details detail = t_member_details.find("platform_id = ? and serial_number = ?", platformId,serialNumber).first();
		return detail==null?null:detail;
	}

	/**
	 * 更新交易状态为成功
	 * @param serialNumber
	 * @return
	 */
	public static boolean updateStatus(int platformId, String serialNumber) {
		System.out.println("===");
		try {
			JpaHelper.execute("update t_member_details set status = ? where platform_id = ? and serial_number like ? and status = 0", true, platformId, serialNumber+"%").executeUpdate();
		} catch (Exception e) {
			Logger.info("更新交易状态为成功时："+e.getMessage());
			
			return false;
		}
		
		return true;
	}
	
	/**
	 * 更新交易状态为成功
	 * @param serialNumber
	 * @return
	 */
	public static boolean updateStatus(String serialNumber) {
		try {
			JpaHelper.execute("update t_member_details set status = ? where serial_number like ? and status = 0", true,serialNumber+"%").executeUpdate();
		} catch (Exception e) {
			Logger.info("更新交易状态为成功时："+e.getMessage());
			
			return false;
		}
		
		return true;
	}
	
	/**
	 * 添加用户事件记录
	 * @param member_id
	 * @param type
	 * @param platform_id
	 * @param descrption
	 * @param error
	 */
	public static void addEvent(long memberId, int type, long platformId, String serialNumber,
			String frontUrl, String backgroundUrl, String remark, String descrption) {
		
		t_member_events event = new t_member_events();
		
		event.member_id = memberId;
		event.time = new Date();
		event.serial_number = serialNumber;
		event.platform_id = platformId;
		event.type_id = type;
		event.front_url = frontUrl;
		event.background_url = backgroundUrl;
		event.remark = remark;
		event.descrption = descrption;
		
		try {
			event.save();
		} catch (Exception e) {
			Logger.error("增加用户事件记录时:" + e.getMessage());
		}
	}
	
	/**
	 * 回调时更新事件描述
	 * @param serialNumber
	 * @param descrption
	 * @return
	 */
	public static t_member_events updateEvent(String serialNumber, String descrption) {
		t_member_events event = t_member_events.find("serial_number = ?", serialNumber).first();
		
		if(event == null) {
			return null;
		}
		
		event.descrption = descrption;
		try {
			event.save();
		} catch (Exception e) {
			Logger.error("回调时更新事件描述时:" + e.getMessage());
		}
		
		return event;
	}
	
	/**
	 * 回调时更新事件描述，无返回值
	 * @param serialNumber
	 * @param descrption
	 * @return
	 */
	public static boolean updateEvent2(String serialNumber, String descrption) {
		try {
			JpaHelper.execute("update t_member_events set descrption = ? where serial_number = ?", descrption,serialNumber).executeUpdate();
		} catch (Exception e) {
			Logger.info("回调时更新事件描述时："+e.getMessage());
			
			return false;
		}
		
		return true;
	}
	
	/**
	 * 判断交易记录中流水号是否已存在
	 * @param serialNumber
	 * @return
	 */
	public static boolean isSerialNumberExist(int platformId, String serialNumber) {
		return t_member_details.count("platform_id = ? and serial_number = ?", platformId, serialNumber) == 0 ? false : true;
	}
	
	/**
	 * 添加管理员事件记录
	 * @param userId
	 * @param type  事件类型
	 * @param ip    ip
	 * @param descrption  描述
	 * @param error
	 */
	public static void supervisorEvent(long supervisorId, int type, String descrption, ErrorInfo error) {
		error.clear();
		
		t_supervisor_events supervisorEvent = new t_supervisor_events();
		
		supervisorEvent.supervisor_id = supervisorId;
		supervisorEvent.time = new Date();
		supervisorEvent.type_id = type;
		supervisorEvent.ip = DataUtil.getIp();
		supervisorEvent.type_id = type;
		supervisorEvent.descrption = descrption;
		
		try {
			supervisorEvent.save();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("增加管理员事件记录时:" + e.getMessage());
			
			error.code = -1;
			error.msg = "增加管理员事件记录失败!";
		}
	}
	
	/**
	 * 查询后台事件(操作日志)
	 * @param currPage
	 * @param pageSize
	 * @param keywordType
	 * @param keyword
	 * @param startTime
	 * @param endTime
	 * @param error
	 * @return
	 */
	public static PageBean<v_supervisor_events> querySupervisorEvents(int currPage, int pageSize,
			int keywordType, String keyword, Date beginTime, Date endTime, ErrorInfo error) {
		error.clear();
		
		if (currPage < 1) {
			currPage = 1;
		}

		if (pageSize < 1) {
			pageSize = 10;
		}
		
		if (keywordType < 0 || keywordType > 3) {
			keywordType = 0;
		}
		
		StringBuffer condition = new StringBuffer("(1 = 1)");
		List<Object> params = new ArrayList<Object>();
		
		if (StringUtils.isNotBlank(keyword)) {
			condition.append(Constants.QUERY_EVENT_KEYWORD[keywordType]);
			
			if (0 == keywordType) {
				params.add("%" + keyword + "%");
				params.add("%" + keyword + "%");
				params.add("%" + keyword + "%");
			} else {
				params.add("%" + keyword + "%");
			}
		}
		
		if(beginTime != null) {
			condition.append("and time > ? ");
			params.add(beginTime);
		}
		
		if(endTime != null) {
			condition.append("and time < ? ");
			params.add(endTime);
		}

		Date minDate = null;
		int count = 0;
		List<v_supervisor_events> page = null;

		try {
			minDate = v_supervisor_events.find("SELECT MIN(time) from v_supervisor_events").first();
			count = (int) v_supervisor_events.count(condition.toString(), params.toArray());
			page = v_supervisor_events.find(condition.toString(), params.toArray()).fetch(currPage, pageSize);
		} catch (Exception e) {
			Logger.error(e.getMessage());
			e.printStackTrace();
			error.code = -1;
			error.msg = "数据库异常";

			return null;
		}

		Map<String, Object> map = new HashMap<String, Object>();
		
		map.put("keywordType", keywordType);

		if (StringUtils.isNotBlank(keyword)) {
			map.put("keyword", keyword);
		}
		
		if(beginTime != null) {
			map.put("beginTime", beginTime);
		}
		
		if(endTime != null) {
			map.put("endTime", endTime);
		}
		
		map.put("days", (minDate==null)? 0 : DateUtil.daysBetween(minDate, new Date()));
		
		PageBean<v_supervisor_events> bean = new PageBean<v_supervisor_events>();
		bean.pageSize = pageSize;
		bean.currPage = currPage;
		bean.totalCount = count;
		bean.page = page;
		bean.conditions = map;
		
		error.code = 0;

		return bean;
	}
	
	/**
	 * 查询删除操作日志记录
	 * @param currPage
	 * @param pageSize
	 * @param error
	 * @return
	 */
	public static PageBean<v_supervisor_events> querySupervisorDeleteEvents(int currPage, int pageSize, ErrorInfo error) {
		error.clear();
		
		if (currPage < 1) {
			currPage = 1;
		}

		if (pageSize < 1) {
			pageSize = 10;
		}
		
		int count = 0;
		List<v_supervisor_events> page = null;

		try {
			count = (int) v_supervisor_events.count("type_id = ?", SupervisorEvent.DELETE_EVENT);
			page = v_supervisor_events.find("type_id = ?", SupervisorEvent.DELETE_EVENT).fetch(currPage, pageSize);
		} catch (Exception e) {
			Logger.error(e.getMessage());
			e.printStackTrace();
			error.code = -1;
			error.msg = "数据库异常";

			return null;
		}
		
		PageBean<v_supervisor_events> bean = new PageBean<v_supervisor_events>();
		bean.pageSize = pageSize;
		bean.currPage = currPage;
		bean.totalCount = count;
		bean.page = page;
		bean.conditions = null;
		
		error.code = 0;

		return bean;
	}
	
	/**
	 * 删除操作日志
	 * @param type 0 全部、 1 一周前、 2 一月前 
	 * @param error
	 */
	public static int deleteEvents(int type, ErrorInfo error) {
		error.clear();

		if (type < 0 || type > 2) {
			error.code = -1;
			error.msg = "删除操作日志,参数有误";
			
			return error.code;
		}
		
		Date date = null;
		String description = null;
		
		if (1 == type) {
			date = DateUtils.addWeeks(new Date(), -1);
			description = "删除一周前操作日志";
		} else if (2 == type) {
			date = DateUtils.addMonths(new Date(), -1);
			description = "删除一个月前操作日志";
		} else {
			description = "删除全部操作日志";
		}
		
		try {
			if (0 == type) {
				t_supervisor_events.deleteAll();
			} else {
				t_supervisor_events.delete("time < ?", date);
			}
		} catch (Exception e) {
			Logger.error(e.getMessage());
			e.printStackTrace();
			error.code = -1;
			error.msg = "数据库异常";

			return error.code;
		}
		
		supervisorEvent(Supervisor.currSupervisor().id, SupervisorEvent.DELETE_EVENT, description, error);
		
		if (error.code < 0) {
			return error.code;
		}
		
		error.code = 0;
		error.msg = "删除操作日志成功";
		
		return error.code;
	}
	
	/**
	 * 判断事件记录中流水号是否已存在
	 * @param serialNumber
	 * @return
	 */
	public static boolean isExistOfEvent(int platformId, String serialNumber) {
		return t_member_events.count("platform_id = ? and serial_number = ?", (long)platformId, serialNumber) == 0 ? false : true;
	}
	
	/**
	 * 根据流水号从事件里面查出P2P请求过来的备注信息
	 * @param serialNumber 流水号
	 * @param error
	 * @return 
	 */
	public static Map<String, String> queryEvents(String serialNumber, ErrorInfo error){
		String infoSql = "select remark from t_member_events where serial_number = ?";
		
		String remark = null;
		
		try {
			remark = t_member_events.find(infoSql, serialNumber).first();
		}catch(Exception e) {
			e.printStackTrace();
			Logger.info("根据商户号和流水号查询时："+e.getMessage());
			error.code = -1;
			error.msg = "查询事件记录的备注信息失败";
			
			return null;
		}
		
		if(null == remark){
			error.code = -2;
			error.msg = "事件记录里面没有这一条记录";
			
			return null;
		}
		
	    JSONObject json = JSONObject.fromObject(remark);
		
	    Map<String, String> map = new HashMap<String, String>();
	    
	    map.put("pWebUrl", json.getString("pWebUrl"));
	    map.put("pS2SUrl", json.getString("pS2SUrl"));
	    map.put("type", json.getString("type"));
	    map.put("memberId", json.getString("memberId"));
	    map.put("pMerBillNo", json.getString("pMerBillNo"));
	    map.put("platformId", json.getString("platformId"));
	    map.put("pMemo1", json.getString("pMemo1"));
	    map.put("pMemo2", json.getString("pMemo2"));
	    map.put("pMemo3", json.getString("pMemo3"));
	    map.put("domain", json.getString("domain"));
	    
		return map;
	}
	
	/**
	 * 根据流水号从交易记录里面查出P2P请求过来的备注信息
	 * @param serialNumber 流水号
	 * @param error
	 * @return 
	 */
	public static Map<String, String> queryDetails(String serialNumber, ErrorInfo error){
		
		String infoSql = "select summary from t_member_details where serial_number = ?";
		
		String remark = null;
		
		try {
			remark = t_member_details.find(infoSql, serialNumber).first();
		}catch(Exception e) {
			e.printStackTrace();
			Logger.info("根据商户号和流水号查询时："+e.getMessage());
			error.code = -1;
			error.msg = "查询交易记录的备注信息失败";
			
			return null;
		}
		
		if(null == remark){
			error.code = -2;
			error.msg = "交易记录里面没有这一条记录";
			
			return null;
		}
		JSONObject json = JSONObject.fromObject(remark);
		
	    Map<String, String> map = new HashMap<String, String>();
	    
	    map.put("pWebUrl", json.getString("pWebUrl"));
	    map.put("pS2SUrl", json.getString("pS2SUrl"));
	    map.put("type", json.getString("type"));
	    map.put("memberId", json.getString("memberId"));
	    map.put("pMerBillNo", json.getString("pMerBillNo"));
	    map.put("platformId", json.getString("platformId"));
	    map.put("pMemo1", json.getString("pMemo1"));
	    map.put("domain", json.getString("domain"));
	    map.put("pMemo2", json.getString("pMemo2"));
	    map.put("pMemo3", json.getString("pMemo3"));
	    
//	    if(json.getInt("type") != YEEConstants.TRANSFER_USER_TO_MER && json.getInt("type") 
//	    		!= YEEConstants.TRANSFER_MER_TO_USERS && json.getInt("type") != YEEConstants.TRANSFER_MER_TO_USER){
//	    	map.put("pMemo2", json.getString("pMemo2"));
//		    map.put("pMemo3", json.getString("pMemo3"));
//	    }
	    
	    //充值用到这个字段
	    if(json.size() == 11){
	    	map.put("amount", json.getString("amount"));
	    }
		
		return map;
	}
	
	/**
	 * 执行定时任务时更改提现放款成功与失败后的查看状态
	 * @param serialNumber  流水号
	 * @param error
	 * @return
	 */
	public int updateWithdrawStatus(String serialNumber, ErrorInfo error){
		error.clear();
		
		String sql = "update t_member_details set status = true where serial_number = ?";
		
		Query query = JPA.em().createQuery(sql).setParameter(1, serialNumber);
		int rows = 0;
		
		try {
			rows = query.executeUpdate();
		} catch(Exception e) {
			Logger.info(e.getMessage());
			error.code = -1;
			error.msg = "数据库异常";
			JPA.setRollbackOnly();
			
			return error.code;
		}
		
		if (rows < 1) {
			error.code = -1;
			error.msg = "数据未更新";
			
			return error.code;
		}
		
		return error.code;
	} 
	
	/**
	 * 定时任务查询提现订单回退记录
	 * @param error
	 */
	public void checkWithdraw(ErrorInfo error){
		error.clear();
		
		int platFormId = (int) Platform.queryPlatformIdByDomain(YEEConstants.PLATFORM_NO, error);
		
		List list = new ArrayList();
		String sql = "select serial_number from t_member_details where operation = ? and platform_id = ? and status = 0";
		
		try {
			list = t_member_details.find(sql, (long)YEEConstants.WITHDRAWAL, platFormId).fetch();
			
		} catch(Exception e) {
			e.printStackTrace();
			Logger.info("查询投资账单信息时："+e.getMessage());
			error.code = -3;
			error.msg = "数据库异常，导致查询提现记录失败";
			JPA.setRollbackOnly();
			
			return;
		}
		
		Map<String, String> userMap = null;
		JSONObject jsonOb = null; 
		Map<String, Object> map = null;
		String resultarr = null;
		
		for(int i = 0; i <list.size(); i++){
			userMap = new HashMap<String, String>();
			jsonOb = new JSONObject();
			map = new HashMap<String, Object>();
			
			userMap.put("requestNo", list.get(i)+"");  //补单的请求流水号
			userMap.put("mode", "WITHDRAW_RECORD");  //查询模式
			jsonOb.putAll(userMap);
			
			String req = Converter.jsonToXml(jsonOb.toString(), "request", null, null, null);
			
			Platform platform = new Platform();
			platform.domain = YEEConstants.PLATFORM_NO;
			
			GateWay gateWay = new GateWay();
			gateWay.id = (long)platform.gatewayId;
			String argMerCode = gateWay.pid;
			
			req = YEEUtil.addAttribute(argMerCode, req);
			map.put("sign",SignUtil.sign(req, YEEConstants.YEE_SIGN_URL, YEEConstants.YEE_SIGN_PASS));
			map.put("req", req);
			map.put("service", "QUERY");
			
			resultarr = YEEUtil.doPostQueryCmd(YEEConstants.YEE_URL_REDICT, map);
			
			//判断通信是否有异常，如果有异常，回调P2P失败值
			if(null == resultarr){
				String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
				jsonOb.put("pMerCode", list.get(i)+"");
				jsonOb.put("pErrMsg", "");
				jsonOb.put("pErrCode", "");
				jsonOb.put("p3DesXmlPara", p3DesXmlPara);
				jsonOb.put("pSign", Encrypt.MD5( list.get(i)+""+p3DesXmlPara+Constants.ENCRYPTION_KEY));
				
				Logger.info("-----------------------单笔业务查询请求易宝异常-------------------------------");
				
				return;
			}
			
			JSONObject jsonResult = (JSONObject)Converter.xmlToObj(resultarr);
			
			Logger.info("------------------------提现查询第三方返回结果jsonResult=:"+jsonResult+"-------------------------------");
			JSONArray jsonArr = null;
			Object pDetails = jsonResult.get("records");//节点数组
			
			if(null == pDetails){
				updateWithdrawStatus(list.get(i).toString(), error);
				return;
		     }
			
			if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
				JSONObject pDetail = (JSONObject)pDetails; 
				JSONObject record = pDetail.getJSONObject("record"); 
		
				jsonArr = new JSONArray(); 
				jsonArr.add(record); 
				
			} else {
				jsonArr = jsonResult.getJSONArray("records");
			} 
			
			JSONObject pRow = jsonArr.getJSONObject(0);
			
			//提现成功，改变该条记录的状态，后面就不需要再来查
			if(pRow.getString("status").equals("SUCCESS") && pRow.getString("remitStatus").equals("REMIT_SUCCESS")){
				updateWithdrawStatus(list.get(i).toString(), error);
				
				return;
				
				//提现失败，对调P2P
			}else if(pRow.getString("status").equals("SUCCESS") && pRow.getString("remitStatus").equals("REMIT_FAILURE")){
				updateWithdrawStatus(list.get(i).toString(), error);
				
				Map<String, String> remarkMap = DealDetail.queryDetails(list.get(i).toString(), error);  //查询备份数据库的数据
				Map<String, String> args = new HashMap<String, String>();
				
				jsonOb.clear();
				jsonOb.put("pMerBillNo", list.get(i)+"");
				jsonOb.put("pMemo1", remarkMap.get("memberId"));
				jsonOb.put("pMemo3", remarkMap.get("pMemo3"));
				 
				String p3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonOb.toString(), "pReq", "pRow", null, null), Constants.ENCRYPTION_KEY);
				args.put("pMerCode", YEEConstants.PLATFORM_NO);
				args.put("pErrMsg", null);
				args.put("pErrCode", "MG00020F");
				args.put("p3DesXmlPara", p3DesXmlPara);
				args.put("pSign", Encrypt.MD5( YEEConstants.PLATFORM_NO+"MG00020F"+"null"+p3DesXmlPara+Constants.ENCRYPTION_KEY));

				WS.url(YEEConstants.YEE_CALLBACK).setParameters(args).post().getString();
			}
		}
	}
}
