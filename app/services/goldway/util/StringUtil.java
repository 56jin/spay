package services.goldway.util;
/**
 * ������ StringUtils
 * ���ܣ�String����
 * ŵ�ǽ�ͨ ��Ȩ����
 * @author zyc    2014-10-15
 * @version 1.0
 */
public class StringUtil {
	
	/** ��0 */
	public static String leftAppendZero(String str, int strLength) {
		if(str == null) str = "";
		str = str.trim();
		int strLen = str.length();
		if (strLen < strLength) {
			while (strLen < strLength) {
				StringBuffer sb = new StringBuffer();
				sb.append("0").append(str);// ��0
				str = sb.toString();
				strLen = str.length();
			}
		}
		return str;
	}
	
	
}
