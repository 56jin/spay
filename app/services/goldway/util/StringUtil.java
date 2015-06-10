package services.goldway.util;
/**
 * 类名： StringUtils
 * 功能：String工具
 * 诺亚金通 版权所有
 * @author zyc    2014-10-15
 * @version 1.0
 */
public class StringUtil {
	
	/** 左补0 */
	public static String leftAppendZero(String str, int strLength) {
		if(str == null) str = "";
		str = str.trim();
		int strLen = str.length();
		if (strLen < strLength) {
			while (strLen < strLength) {
				StringBuffer sb = new StringBuffer();
				sb.append("0").append(str);// 左补0
				str = sb.toString();
				strLen = str.length();
			}
		}
		return str;
	}
	
	
}
