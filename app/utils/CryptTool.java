package utils;

import org.apache.commons.lang.RandomStringUtils;

import business.Member;

/**
 * 生成用户名、密码等随机串
 * @author cp
 * @version 6.0
 * @created 2014年7月29日 下午2:42:16
 */
public class CryptTool {

	/**
	 * 产生随机字符串
	 * @param count 字符串长度
	 * @param letters 是否包含字母
	 * @param numbers 是否包含数字
	 * @return
	 */
	public static String getRandom(int count, boolean letters, boolean numbers) { 
        return RandomStringUtils.random(count, letters, numbers); 
    }
	
	/**
	 * 生成长度为6包含字母和数字的随机密码
	 * @return
	 */
	public static String getPassword() {
		return getRandom(6, true, true);
	}
	
	/**
	 * 生成格式为xxxxxx_dd的用户名（x小写字母，d数字）
	 * @return
	 */
	public static String getUsername() {
		return getRandom(6, true, false).toLowerCase() + "_" + getRandom(2, false, true);
	}
	
	/**
	 * 生成 生成格式为xxxxxx_dd且唯一的用户名（x小写字母，d数字）
	 * @return
	 */
	public static String getUniqueName() {
		String name = null;
		do{
			name = getUsername();
		}while(Member.isNameExist(name));
		
		return name;
	}
	
	public static void main(String[]args) {
	}
}
