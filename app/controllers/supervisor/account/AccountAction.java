package controllers.supervisor.account;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.lang.StringUtils;

import controllers.interceptor.SupervisorInterceptor;
import controllers.supervisor.SupervisorController;

import business.Supervisor;
import play.Logger;
import play.mvc.Controller;
import play.mvc.With;
import utils.ErrorInfo;
import utils.RegexUtils;

/**
 * 账户
 * @author lzp
 * @version 6.0
 * @created 2014-5-30
 */
public class AccountAction extends SupervisorController {
	/**
	 * 用户中心
	 */
	public static void home() {
		render();
	}
	
	/**
	 * 编辑管理员
	 */
	public static void editSupervisor(String oldPassword, String password, String realityName, int sex,
			String birthday, String mobile1, String mobile2, String email) {
		ErrorInfo error = new ErrorInfo();
		
		if (StringUtils.isBlank(birthday)) {
			error.code = -1;
			error.msg = "出生日期不能为空";
			
			renderJSON(error);
		}
		
		birthday = birthday.replaceAll("\\s+", "");
		
		if (!RegexUtils.isDate(birthday)) {
			error.code = -1;
			error.msg = "出生日期格式不正确，正确的格式如：2008-08-08";
			
			renderJSON(error);
		}
		
		Date date = null;
		
		try {
			date = new SimpleDateFormat("yyyy-MM-dd").parse(birthday);
		} catch (ParseException e) {
			Logger.error(e.getMessage());
			e.printStackTrace();
			error.code = -1;
			error.msg = "数据异常，请重试";
			
			renderJSON(error);
		}
		
		Supervisor supervisor = Supervisor.currSupervisor();
		
		if (null == supervisor) {
			redirect("/supervisor");
		}
		
		if (StringUtils.isNotBlank(supervisor.password) && !supervisor.isMyPassword(oldPassword)) {
			error.code = -1;
			error.msg = "原始密码不正确";
			
			renderJSON(error);
		}
		
		supervisor.password = password;
		supervisor.realityName = realityName;
		supervisor.sex = sex;
		supervisor.birthday = date;
		supervisor.mobile1 = mobile1;
		supervisor.mobile2 = mobile2;
		supervisor.email = email;
		supervisor.edit(error);
		
		if (error.code < 0) {
			renderJSON(error);
		}
		
		Supervisor.setCurrSupervisor(supervisor);
		renderJSON(error);
	}
}
