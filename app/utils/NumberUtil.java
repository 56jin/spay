package utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

public class NumberUtil {

	public static Pattern patternInt = Pattern.compile("(^[+-]?([0-9]|([1-9][0-9]*)))");
//	public static Pattern patternInt = Pattern.compile("^[+-]?[0-9]+$");
	
	public static Pattern patternDouble = Pattern.compile("^[+-]?(([1-9]\\d*\\.?\\d+)|(0{1}\\.\\d+)|0{1})");//判断是否为小数
//	public static Pattern patternDouble = Pattern.compile("\\d+\\.\\d+$|-\\d+\\.\\d+$");//判断是否为小数

	public static boolean isNumeric(String str) { 
		if(StringUtils.isBlank(str)) {
			return false;
		}
		
		for (int i = str.length();--i>=0;){
			if (!Character.isDigit(str.charAt(i))){
				return false;    
				}   
		}   return true;  
	} 
	
	/**
	 * 判断是否是个整数（int,long等）
	 * @param str
	 * @return
	 */
	public static boolean isNumericInt(String str) {
		if(str == null) {
			return false;
		}
		
		return patternInt.matcher(str).matches();
	}
	
	/**
	 * 判断是否是个小数（double,float等）
	 * @param str
	 * @return
	 */
	public static boolean isNumericDouble(String str) {
		if(str == null) {
			return false;
		}
		
		return patternDouble.matcher(str).matches()||isNumericInt(str);
	}
	
	public static boolean isBoolean(String str) {
		if(str == null) {
			return false;
		}
		
		return str.equals("true") || str.equals("false");
	}
	
	public static boolean isDate(String str) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		
		try {
			format.parse(str);
		} catch (ParseException e) {
			return false;
		}
		
		return true;
	}
}
