package utils;

import org.apache.commons.lang.StringUtils;

import constants.Constants.SystemSupervisor;

public class RegexUtils {
	
	/**
	 * 用户名是否符合规范（^[\u4E00-\u9FA5A-Za-z0-9_]+$）
	 * @return
	 */
	public static boolean isValidUsername(String username) {
		if (StringUtils.isBlank(username)) {
			return false;
		}
		
		return username.matches("^[\u4E00-\u9FA5A-Za-z0-9_]{3,10}$");
	}
	
	/**
	 * 密码是否符合规范（[a-zA-Z\d]{6,20}）
	 * @return
	 */
	public static boolean isValidPassword(String password) {
		if (null == password) {
			return false;
		}
		
		return password.matches("^([^\\s'‘’]{6,20})$");
	}
//	public static boolean isValidPassword(String password) {
//		if (null == password) {
//			return false;
//		}
//		
//		return password.matches("[a-zA-Z\\d]{6,20}");
//	}
	/**
	 * 是否有效手机号码
	 * @param mobileNum
	 * @return
	 */
	public static boolean isMobileNum(String mobileNum) {
		if (null == mobileNum) {
			return false;
		}
		
		return mobileNum.matches("^((13[0-9])|(14[4,7])|(15[^4,\\D])|(17[6-8])|(18[0-9]))(\\d{8})$");
	}
	
	/**
	 * 是否有效邮箱
	 * @param email
	 * @return
	 */
	public static boolean isEmail(String email) {
		if (null == email) {
			return false;
		}
		
		return email.matches("^([a-zA-Z0-9_-])+@([a-zA-Z0-9_-])+((\\.[a-zA-Z0-9_-]{2,3}){1,2})$");
	}
	
	/**
	 * 是否是QQ邮箱
	 */
	public static boolean isQQEmail(String email){
		if(null == email)
			return false;
		
		int index = email.indexOf("@") + 1;

		if(-1 == index)
			return false;
		
		String qq = null;
		
		try {
			qq = email.substring(index, index + 2);
		} catch (Exception e) {
			return false;
		}
		
		if(qq.equals("qq") || qq.equals("QQ"))
			return true;
		
		return false;
	}
	
	/**
	 * 是否数字(小数||整数)
	 * @param number
	 * @return
	 */
	public static boolean isNumber(String number) {
		if (null == number) {
			return false;
		}
		
		return number.matches("^[+-]?(([1-9]{1}\\d*)|([0]{1}))(\\.(\\d)+)?$");
	}
	
	/**
	 * 是否整数
	 * @param number
	 * @return
	 */
	public static boolean isInt(String number) {
		if (null == number) {
			return false;
		}
		
		return number.matches("^[+-]?(([1-9]{1}\\d*)|([0]{1}))$");
	}
	
	/**
	 * 是否正整数
	 * @param number
	 * @return
	 */
	public static boolean isPositiveInt(String number) {
		if (null == number) {
			return false;
		}
		
		return number.matches("^[+-]?(([1-9]{1}\\d*)|([0]{1}))$");
	}
	
	/**
	 * 是否日期yyyy-mm-dd(yyyy/mm/dd)
	 * @param date
	 * @return
	 */
	public static boolean isDate(String date) {
		if (null == date) {
			return false;
		}
		return date.matches("^([1-2]\\d{3})[\\/|\\-](0?[1-9]|10|11|12)[\\/|\\-]([1-2]?[0-9]|0[1-9]|30|31)$");
	}
	
	/**
	 * 逗号分隔的正则表达式
	 * @param str
	 * @return
	 */
	public static String getCommaSparatedRegex(String str) {
		if (str == null) {
			return null;
		}
		
		return "^("+str+")|([\\s\\S]*,"+str+")|("+str+",[\\s\\S]*)|([\\s\\S]*,"+str+",[\\s\\S]*)$";
	}
	
	/**
	 * 字符串包含
	 * @return
	 */
	public static boolean contains(String str, String regex) {
		if (str == null || regex == null) {
			return false;
		}
		
		return str.matches(regex);
	}
	
	public static void main(String[] args) {
	}
}
