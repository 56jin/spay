package business;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.Query;

import models.t_dict_ad_citys;
import models.t_member_of_platforms;
import models.t_members;
import models.t_platforms;
import models.v_member_details;
import models.v_member_events;
import net.sf.json.JSONObject;

import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang.StringUtils;

import play.Logger;
import play.cache.Cache;
import play.db.helper.JpaHelper;
import play.db.jpa.JPA;
import utils.CryptTool;
import utils.EmailUtil;
import utils.ErrorInfo;
import utils.PageBean;

import com.shove.security.Encrypt;

import constants.Constants;
import constants.ViewTemplete;

public class Member {

	public long id;
	public Date time;
	public String name;
	public String password;
	private String _password;
	
	public void setPassword(String password) {
		this._password = Encrypt.MD5(password+Constants.ENCRYPTION_KEY);
	}
	
	public String getPassword() {
		return this._password;
	}
	
	public String idNumber;
	private String _idNumber;
	
	public void setIdNumber(String idNumber) {
		t_members member = new t_members();
		
		member = t_members.find("id_number = ?", idNumber).first();
		
		if(member != null) {
			this.id = member.id;
			this.time = member.time;
			this.name = member.name;
			this._password = member.password;
			this.mobile = member.mobile;
			this.serialNumber = member.serial_number;
			this.status = member.status;
		}
		
		this._idNumber = idNumber;
	}
	
	public String getIdNumber() {
		return _idNumber;
	}
	
	public String mobile;
	public String serialNumber;
	public boolean status;
	
	public long memberId;
	public long platformId;
	public long platformMemberId;
	public String platformMembername;
	public String platformMemberAccount;
	public String authPaymentNumber;
	public String authInvestNumber;
	
	/**
	 * 开户时，需添加会员，会员和平台的关系（返回结果前，没有托管平台账号，开户状态为0）
	 * @return
	 */
	public Map<String, String> add() {
		Map<String, String> info = new HashedMap();
		
		info.put("name", CryptTool.getUniqueName());
		info.put("password", CryptTool.getPassword());
		
		this.name = info.get("name");
		this.password = info.get("password");
		
		t_members member = new t_members();
		
		member.time = new Date();
		member.name = this.name;
		member.password = this.password;
		member.id_number = this.idNumber;
		member.mobile = this.mobile;
		member.status = false;
		
		try {
			member.save();
		}catch(Exception e) {
			e.printStackTrace();
			Logger.info("保存用户信息时："+e.getMessage());
			
			return null;
		}
		
		t_member_of_platforms platformmember = new t_member_of_platforms();
		
		platformmember.member_id = member.id;
		platformmember.platform_id = this.platformId;
		platformmember.platform_member_id = this.platformMemberId;
		platformmember.platform_member_name = this.platformMembername;
		
		try {
			platformmember.save();
		}catch(Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("保存用户平台关系信息时："+e.getMessage());
			
			return null;
		}
		
		
		return info;
	}
	
	/**
	 * 会员已存在，添加平台和会员的关系
	 * @param error
	 */
	public void addPlatformmember(ErrorInfo error) {
		t_member_of_platforms platformmember = new t_member_of_platforms();
		
		platformmember.member_id = memberId;
		platformmember.platform_id = this.platformId;
		platformmember.platform_member_id = this.platformMemberId;
		platformmember.platform_member_name = this.platformMembername;
		
		try {
			platformmember.save();
		}catch(Exception e) {
			JPA.setRollbackOnly();
			e.printStackTrace();
			Logger.info("保存用户平台关系信息时："+e.getMessage());
			
			return ;
		}
		
		error.code = 0;
		error.msg = "开户成功";
	}
	
	public static long queryIdByIdNumber(String id_number) {
		String sql = "select id from t_members where id_number = ?";
		
		return t_members.find(sql, id_number).first();
	}
	
	/**
	 * 判断用户名是否存在
	 * @param name
	 * @return
	 */
	public static boolean isNameExist(String name) {
		
		if(StringUtils.isBlank(name)) {
			return true;
		}
		
		String sql = "select name from t_members where name = ?";
		
		try {
			return t_members.find(sql, name).fetch().size() == 0  ? false : true;
		} catch(Exception e) {
			e.printStackTrace();
			Logger.info("判断用户名是否存在时,根据用户名查询数据时："+e.getMessage());
			
			return true;
		}
	}
	
	/**
	 * 判断身份证是否存在
	 * @param name
	 * @return
	 */
	public static boolean isIdNumber(String idNumber) {
		
		if(StringUtils.isBlank(idNumber)) {
			return true;
		}
		
		String sql = "select id_number from t_members where id_number = ?";
		
		try {
			return t_members.find(sql, idNumber).fetch().size() == 0  ? false : true;
		} catch(Exception e) {
			e.printStackTrace();
			Logger.info("判断身份证是否存在时,根据身份证查询数据时："+e.getMessage());
			
			return true;
		}
	}
	
	/**
	 * 判断手机号码是否存在
	 * @param name
	 * @return
	 */
	public static boolean isMobile(String mobile) {
		
		if(StringUtils.isBlank(mobile)) {
			return true;
		}
		
		String sql = "select mobile from t_members where mobile = ?";
		
		try {
			return t_members.find(sql, mobile).fetch().size() == 0  ? false : true;
		} catch(Exception e) {
			e.printStackTrace();
			Logger.info("判断手机号码是否存在时,根据手机号码查询数据时："+e.getMessage());
			
			return true;
		}
	}
	
	/**
	 * 判断平台用户是否已开户
	 * @param idNumber
	 * @param error
	 * @return
	 */
	public static boolean isCreateAccount(String idNumber, String domain, ErrorInfo error) {
		t_members member = null;
		
		member = t_members.find("id_number = ?", idNumber).first();
		
		if(member == null) {
			return false;
		}
		
		long platformId = Platform.queryPlatformIdByDomain(domain, error);
		
		if(error.code < 0) {
			return false;
		}
		
		int gatewayId = Platform.queryGatewayByDomain(domain, error);
		
		if(error.code < 0) {
			return false;
		}
		
		t_member_of_platforms platformmember = null;
		
		platformmember = t_member_of_platforms.find("member_id = ? and platform_id = ?", member.id, platformId).first();
		
		/*在t_member中有记录，但t_member_of_platforms无记录，可能是直接注册而来*/
		if(platformmember == null) {

			t_member_of_platforms platformmember2 = null;
				
			platformmember2 = t_member_of_platforms.find("member_id = ?", member.id).first();
			
			if(platformmember2 == null) {
				error.code = 1;
				return true;
			}
			
			t_platforms platform = null;
			
			platform = t_platforms.findById(platformmember2.platform_id);
			
			/*t_member_of_platforms没有记录，但不同平台使用的支付相同*/
			if(StringUtils.isNotBlank(platformmember2.platform_member_account) && gatewayId == platform.gateway_id) {
				error.code = 2;
				return true;
			}
			
			error.code = 1;
			return true;
		}

		/*根据idNumber查询得开通账户的域名和当前域名相同，已开户*/
		if(StringUtils.isNotBlank(platformmember.platform_member_account) && platformId == platformmember.platform_id) {
			error.msg = "已开户";
			return true;
		}
		
		t_platforms platform = null;
		
		platform = t_platforms.findById(platformmember.platform_id);
		
		if(platform == null) {
			return false;
		}
		
		/*t_member_of_platforms已有记录，已开通，域名不同，但不同域名使用相同支付接口*/
		if(platform.gateway_id == gatewayId && StringUtils.isNotBlank(platformmember.platform_member_account)) {
			error.code = 3;
			return true;
		}
		
		/*t_member_of_platforms已有记录，同域名使用不同支付接口*/
		error.code = 4;
		return true;
	}
	
	/**
	 * 根据身份证和平台查询出资金账户
	 * @param idNumber
	 * @param domain
	 * @return
	 */
	public static String queryAccount(String idNumber, String domain) {
		String sql = "select platform_member_account from t_member_of_platforms where member_id = (select id from t_members where id_number = ?)" 
				+" and platform_Id = (select id from t_platforms where domain=?)";
		
		 Query query = JpaHelper.execute(sql, idNumber, domain);
		 if(query == null) {
			 return null;
		 }
			 
		 List<Object> accounts= query.getResultList();
		 if(accounts == null || accounts.size() == 0) {
			 return null;
		 }
		 
		 String memberAccount = accounts.get(0).toString();
		
		 return memberAccount;
	}
	
	/**
	 * 根据身份证和平台查询出资金账户
	 * @param idNumber
	 * @param domain
	 * @return
	 */
	public static String queryAccount(String idNumber, int platformId) {
		String sql = "select platform_member_account from t_member_of_platforms where member_id = (select id from t_members where id_number = ? and status = 1)" 
				+" and platform_Id = ?";
		
		 Query query = JpaHelper.execute(sql, idNumber, platformId);
		 if(query == null) {
			 return null;
		 }
			 
		 List<Object> accounts= query.getResultList();
		 if(accounts == null || accounts.size() == 0 || accounts.get(0) == null) {
			 return null;
		 }
		 
		 String memberAccount = accounts.get(0).toString();
		
		 return memberAccount;
	}
	
	/**
	 * 根据账户号和平台id查询使用平台的id
	 * @param pIpsAcctNo
	 * @param platformId
	 * @return
	 */
	public static long queryPlatMemberId(String pIpsAcctNo, long platformId) {
		String sql = "select platform_member_id from t_member_of_platforms where platform_id = ? and platform_member_account = ?";
		List<Long> ids = null;
		ids = t_member_of_platforms.find(sql, platformId, pIpsAcctNo).fetch();
		
		if(ids == null || ids.size() == 0) {
			
			return 0;
		}
		
		return ids.get(0);
	}
	
	
	/**
	 * 查询用户状态
	 * @return
	 */
	public static int querymemberStatus() {
		return 0;
	}
	
	/**
	 * 更新用户状态为使用（激活）
	 */
	public static void updateStatus(String idNumber) {
		
		JpaHelper.execute("update t_members set status = ? where id_number = ?", true, idNumber).executeUpdate();
	}
	
	/**
	 * 根据使用平台的id和用户名查询本平台的id
	 * @param platformId
	 * @param platformMemberId
	 * @return
	 */
	public static long queryMemberId(int platformId, long platformMemberId) {
		String sql = "select member_id from t_member_of_platforms where platform_id = ? and platform_member_id = ?";
		Logger.info("platformId:"+platformId+";platformMemberId:"+platformMemberId);
		return t_member_of_platforms.find(sql, (long)platformId, platformMemberId).first();
	}
	
	/**
	 * 添加用户资金账户
	 */
	public static void updateAccount(int platformId, long platformMemberId, String platformmemberAccount) {
//		String sql = "select platform_member_account from t_member_of_platforms where platform_id = ? and platform_member_id = ?";
//		
//		String memberAccount = t_member_of_platforms.find(sql, (long)platformId, platformMemberId).first();
//		
//		if(StringUtils.isBlank(memberAccount)) {
//			JpaHelper.execute("update t_member_of_platforms set platform_member_account = ? where platform_id = ? and platform_member_id = ?",
//					platformmemberAccount, (long)platformId, platformMemberId).executeUpdate();
//		}
		
		long memberId = queryMemberId(platformId, platformMemberId);
		
		JpaHelper.execute("update t_member_of_platforms set platform_member_account = ? where member_id = ?",
				platformmemberAccount, memberId).executeUpdate();
	}
	
	/**
	 * 添加用户资金账户,包含账户和唯一标识
	 * @param platformId  平台id
	 * @param platformMemberId 平台用户id
	 * @param platformmemberAccount  平台用户资金托管账户
	 * @param platformmemberAccountId  平台用户资金托管账户id
	 */
	public static void updateAccount(int platformId, long platformMemberId, String platformmemberAccount, String platformmemberAccountId) {

		long memberId = queryMemberId(platformId, platformMemberId);
		
		JpaHelper.execute("update t_member_of_platforms set platform_member_account = ?, platform_member_account_id = ? where member_id = ?",
				platformmemberAccount, platformmemberAccountId, memberId).executeUpdate();
	}
	
	
	/**
	 * 添加自动还款授权号
	 */
	public static void updateAuthPaymentNumber(int platformId, long platformMemberId, String authPaymentNumber) {
//		String sql = "select auth_payment_number from t_member_of_platforms where platform_id = ? and platform_member_id = ?";
//		
//		String paymentNumber = t_member_of_platforms.find(sql, (long)platformId, platformMemberId).first();
//		
//		if(StringUtils.isBlank(paymentNumber)) {
//			JpaHelper.execute("update t_member_of_platforms set auth_payment_number = ? where platform_id = ? and platform_member_id = ?",
//					authPaymentNumber, (long)platformId, platformMemberId).executeUpdate();
//		}
		
		long memberId = queryMemberId(platformId, platformMemberId);
		
		JpaHelper.execute("update t_member_of_platforms set auth_payment_number = ? where member_id = ?",
				authPaymentNumber, memberId).executeUpdate();
	}
	
	/**
	 * 添加自动投标授权号
	 */
	public static void updateAuthInvestNumber(int platformId, long platformMemberId, String authInvestNumber) {
//		String sql = "select auth_invest_number from t_member_of_platforms where platform_id = ? and platform_member_id = ?";
//		
//		String investNumber = t_member_of_platforms.find(sql, (long)platformId, platformMemberId).first();
//		
//		if(StringUtils.isBlank(investNumber)) {
//			JpaHelper.execute("update t_member_of_platforms set auth_invest_number = ? where platform_id = ? and platform_member_id = ?",
//					authInvestNumber, (long)platformId, platformMemberId).executeUpdate();
//		}
		
		long memberId = queryMemberId(platformId, platformMemberId);
		
		JpaHelper.execute("update t_member_of_platforms set auth_invest_number = ? where member_id = ?",
				authInvestNumber, memberId).executeUpdate();
	}
	
	/**
	 * 判断用户中流水号是否已存在
	 * @param serialNumber
	 * @return
	 */
	public static boolean isSerialNumberExist(String serialNumber) {
		return t_members.count("serial_number = ?", serialNumber) == 0 ? false : true;
	}
	
	/**
	 * 根据provinceId查询所有的市
	 * @param citys
	 * @param cityId
	 * @return
	 */
	public static List<t_dict_ad_citys> queryCity(long provinceId) {
		
		List<t_dict_ad_citys> citys = (List<t_dict_ad_citys>) Cache.get("citys");
		
		if(citys == null) {
			citys = t_dict_ad_citys.findAll();
		}
		List<t_dict_ad_citys> cityList = new ArrayList<t_dict_ad_citys>();
		
		for(t_dict_ad_citys city : citys) {
			
			if(city.province_id == provinceId) {
				cityList.add(city);
			}
		}
		
		return cityList;
	}
	
	/**
	 * 查询交易记录
	 * @param userId
	 * @param type 类别
	 * @param beginTime 开始时间
	 * @param endTime   结束时间
	 * @param currPage
	 * @param pageSize
	 * @return
	 */
	public static PageBean<v_member_details> queryMemberDetails(long memberId, int type, String keyword, Date beginTime,
			Date endTime, int orderStatus, int currPage, int pageSize, ErrorInfo error) {
		error.clear();
		
		if(currPage == 0) {
			currPage = 1;
		}
		
		if(pageSize == 0) {
			pageSize = Constants.PAGE_SIZE;
		}
		
		StringBuffer conditions = new StringBuffer(" 1=1 ");
		List<Object> values = new ArrayList<Object>();
		Map<String,Object> conditionMap = new HashMap<String, Object>();
		
		conditionMap.put("condition", type);
		conditionMap.put("keyword", keyword);
		conditionMap.put("startDate", beginTime);
		conditionMap.put("endDate", endTime);
		conditionMap.put("orderStatus", orderStatus);
		
		if(type >= 0 && type <=3 && StringUtils.isNotBlank(keyword)) {
			conditions.append(Constants.MEMBER_TYPE[type]);
			if(type == 0) {
				values.add("%"+keyword+"%");
				values.add("%"+keyword+"%");
			}
			values.add("%"+keyword+"%");
		}
		
		if(beginTime != null) {
			conditions.append("and t_member_details.time > ? ");
			values.add(beginTime);
		}
		
		if(endTime != null) {
			conditions.append("and t_member_details.time < ? ");
			values.add(endTime);
		}
		
		if(memberId != 0) {
			conditions.append("and t_member_details.member_id = ? ");
			values.add(memberId);
		}
		
		if(orderStatus > 0 && orderStatus <= 5) {
			conditions.append(Constants.MEMBER_ORDER[orderStatus]);
		}
		
		PageBean<v_member_details> page = new PageBean<v_member_details>();
		page.currPage = currPage;
		page.pageSize = pageSize;
		page.conditions = conditionMap;
		
		List<v_member_details> userDetails = new ArrayList<v_member_details>();
		
		Query query = JPA.em().createNativeQuery(ViewTemplete.V_MEMBER_DETAILS+" where "+conditions.toString()+" limit ?,?",v_member_details.class); 
		Query queryCount = JPA.em().createNativeQuery("SELECT FOUND_ROWS()"); 

		for(int i=0; i<values.size(); i++){ 
			query.setParameter(i+1, values.get(i)); 
		}
		query.setParameter(values.size()+1, currPage); 
		query.setParameter(values.size()+2, page.pageSize);
		
		try {
//			page.totalCount =   (int) v_member_details.count(conditions.toString(), values.toArray());
//			page.totalCount = (int) v_member_details.count();
			userDetails = query.getResultList(); 
			page.totalCount = ((BigInteger) queryCount.getSingleResult()).intValue();
//			userDetails = v_member_details.find(conditions.toString(), values.toArray()).fetch(currPage, page.pageSize);
			
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询交易记录时："+e.getMessage());

			error.code = -1;
			error.msg = "查询平台交易记录失败";
			return null;
		}
		
		page.page = userDetails;
		error.code = 0;
		
		return page;
	}
	
	/**
	 * 查询用户事件
	 * @param userId
	 * @param type 类别
	 * @param beginTime 开始时间
	 * @param endTime   结束时间
	 * @param currPage
	 * @param pageSize
	 * @return
	 */
	public static PageBean<v_member_events> queryMemberEvents(int type, String keyword, Date beginTime,
			Date endTime, int orderStatus, int currPage, int pageSize, ErrorInfo error) {
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
		conditionMap.put("startDate", beginTime);
		conditionMap.put("endDate", endTime);
		conditionMap.put("orderStatus", orderStatus);
		
		if(type >= 0 && type <=2 && StringUtils.isNotBlank(keyword)) {
			conditions.append(Constants.MEMBER_TYPE[type]);
			if(type == 0) {
				values.add("%"+keyword+"%");
				values.add("%"+keyword+"%");
			}
			values.add("%"+keyword+"%");
		}
		
		if(beginTime != null) {
			conditions.append("and t_member_events.time > ? ");
			values.add(beginTime);
		}
		
		if(endTime != null) {
			conditions.append("and t_member_events.time < ? ");
			values.add(endTime);
		}
		
		if(orderStatus > 0 && orderStatus <= 3) {
			conditions.append(Constants.EVENT_ORDER[orderStatus]);
		}
		
		PageBean<v_member_events> page = new PageBean<v_member_events>();
		page.currPage = currPage;
		page.pageSize = pageSize;
		page.conditions = conditionMap;
		
		List<v_member_events> userDetails = new ArrayList<v_member_events>();
		
		Query query = JPA.em().createNativeQuery(ViewTemplete.V_MEMBER_EVENTS+" where "+conditions.toString()+" limit ?,?",v_member_events.class); 
		Query queryCount = JPA.em().createNativeQuery("SELECT FOUND_ROWS()"); 
		
		for(int i=0; i<values.size(); i++){ 
			query.setParameter(i+1, values.get(i)); 
		}
		query.setParameter(values.size()+1, currPage); 
		query.setParameter(values.size()+2, page.pageSize); 
		
		try {
			userDetails = query.getResultList(); 
			page.totalCount = ((BigInteger) queryCount.getSingleResult()).intValue();
		} catch (Exception e) {
			e.printStackTrace();
			Logger.error("查询用户事件时："+e.getMessage());

			error.code = -1;
			error.msg = "查询用户事件失败";
			return null;
		}
		
		page.page = userDetails;
		error.code = 0;
		
		return page;
	}
	
	/**
	 * 判断用户开户情况
	 * 
	 * @param error
	 * @param platformMemberId
	 * @param memberName
	 * @param platformId
	 * @param domain
	 * @param jsonArg3DesXmlPara
	 */
	public static void checkAccuntInfo(ErrorInfo error, long platformMemberId,
			String memberName, long platformId, String domain,
			JSONObject jsonArg3DesXmlPara) {
		error.clear();

		String idNumber = jsonArg3DesXmlPara.getString("pIdentNo").trim();
		String mobile = jsonArg3DesXmlPara.getString("pMobileNo").trim();
		String email = jsonArg3DesXmlPara.getString("pEmail").trim();

		t_members t_member = t_members.find("id_number = ?", idNumber).first();

		/* 省份证不存在，没有注册资金托管平台账户 */
		if (t_member == null) {
			/* 身份证不存在，根据请求在用户表和用户平台关系表中添加记录 */
			Member member = new Member();

			member.idNumber = idNumber;
			member.mobile = mobile;
			member.platformId = platformId;
			member.platformMemberId = platformMemberId;
			member.platformMembername = memberName;

			Map<String, String> info = member.add();

			String content = "您在资金托管平台注册的用户名：" + info.get("name") + "  密码："
					+ info.get("password");

			EmailUtil.sendEmail(email, "注册信息", content);
		} else {

			t_member_of_platforms platformMember = t_member_of_platforms.find(
					"member_id = ? and platform_id = ?", t_member.id,
					platformId).first();

			/* 资金托管平台账户未与当前p2p平台绑定关系 */
			if (platformMember == null) {
				/* 绑定资金托管平台账户与当前平台的关系 */
				Member member = new Member();
				member.memberId = Member.queryIdByIdNumber(idNumber);
				member.platformId = platformId;
				member.platformMemberId = platformMemberId;
				member.platformMembername = memberName;
				member.addPlatformmember(error);
			} else {
				/* 资金托管平台账户已经与当前p2p平台绑定关系 */
				if (StringUtils.isNotBlank(platformMember.platform_member_account)) {
					// 已经在第三方支付平台开户成功
					Logger.info("======开户时：%s", "已开户");
					error.code = -1;
					error.msg = "已开户";
					
				} else {
					// 没有在第三方成功开户，continue
				}
			}
		}
	}
	/**
	 * 查询提现银行卡号
	 */
	public static String findCardNo(ErrorInfo error,long platformMemberId, long platformId) {
		error.clear();
		t_member_of_platforms platformMember = t_member_of_platforms.find("platform_member_id = ? and platform_id = ?", platformMemberId,platformId).first();
		if(platformMember == null){
			Logger.info("获取提现银行卡号时：%s","该乾多多帐号没有绑定到当前平台");
			error.code = -1;
			error.msg = "该乾多多帐号没有绑定到当前平台";

			return "";
		}
		
		return platformMember.card_no;
	}

	/**
	 * 保存提现银行卡号
	 * @param encrypt3des
	 */
	public static void updateCardNo(String cardNo,long platformMemberId, long platformId) {
		t_member_of_platforms platformMember = t_member_of_platforms.find(
				"platform_member_id = ? and platform_id = ?", platformMemberId,platformId).first();
		if(platformMember == null){
			Logger.info("更新提现银行卡号时：%s","该乾多多帐号没有绑定到当前平台");
			Logger.info("更新提现银行卡号失败");
			
			return;
		}
		
		JpaHelper.execute("update t_member_of_platforms set card_no = ? where member_id = ? ",
				cardNo, platformMember.member_id).executeUpdate();
	}
	
	
}
