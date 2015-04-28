package business;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import net.sf.ehcache.Status;

import constants.Constants;
import constants.YEEConstants;
import play.Logger;
import play.db.jpa.JPA;
import utils.DateUtil;
import utils.ErrorInfo;
import models.t_member_of_platforms;

/**
 * 用户平台操作
 * @author zhs
 * @date 2014-12-15 下午03:31:46
 */
public class MemberOfPlatform {

	/**
	 * 查询用户的绑卡状态
	 * @param platformMemberId 用户在P2P的唯一标识（id）
	 * @param error
	 * @return
	 */
	public static int BindCardStatus(long platformMemberId, ErrorInfo error){
		error.clear();
		Integer status = null;
		
		String sql ="select card_status from t_member_of_platforms where platform_member_id = ?";
		
		try {
			status = t_member_of_platforms.find(sql, platformMemberId).first();
			
		} catch(Exception e) {
			e.printStackTrace();
			Logger.info("查询用户绑卡状态时："+e.getMessage());
			error.code = -1;
			error.msg = "还款出现异常，导致查询用户绑卡状态失败";
			JPA.setRollbackOnly();
			
	        return 0;
		}
		
		return status = null == status ? 0 : status.intValue();
	}
	
	/**
	 * 修改用户绑卡状态
	 * @param status 要修改的状态 1为受理成功 0为未绑定
	 * @param platformMemberId 用户在P2P的唯一标识（id）
	 * @param error
	 * @return
	 */
	public static boolean updateCardStatus(long platformMemberId, ErrorInfo error){
		error.clear();
		int rows = 0;
		EntityManager em = JPA.em();
		
		String sql = "update t_member_of_platforms set card_status = ? where platform_member_id = ?";
		
		Query query = em.createQuery(sql).setParameter(1, YEEConstants.CARD_SUBMIT_SUCCESS).setParameter(2, platformMemberId);
        
        try {
			rows = query.executeUpdate();
		} catch(Exception e) {
			e.printStackTrace();
			Logger.info("修改绑卡状态时："+e.getMessage());
			error.code = -1;
			error.msg = "数据库出现异常，导致修改绑卡状态失败";
			JPA.setRollbackOnly();
			
			return false;
		}
		
		if(rows == 0){
			error.code = -1;
			error.msg = "数据库出现异常，导致修改绑卡状态失败";
			JPA.setRollbackOnly();
			
			return false;
		}
		
		return true;
	}
}
