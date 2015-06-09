package services.goldway.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * 类名： FileUtil
 * 功能：文件处理类
 * 诺亚金通 版权所有
 * @author zyc    2014-10-14
 * @version 1.0
 */
public class FileUtil {
	
	/**
	 * 功能：加载文本文件
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	public static String loadFileToStr(String fileName) throws Exception{  
        BufferedReader br = null;
        InputStreamReader reader= null;
        FileInputStream fileInputStream = null;
        String keyString = null;
		try {
		     fileInputStream = new FileInputStream(fileName);
			 reader = new InputStreamReader(fileInputStream);
			 br= new BufferedReader(reader);  
             String readLine= null;  
             StringBuilder sb= new StringBuilder();  
             while((readLine= br.readLine())!=null){  
            	 sb.append(readLine);  
             }
             keyString = sb.toString();
        } catch (Exception ex) {
        	throw new Exception("加载文件内容出现未处理异常!!");
        }finally {
        	if (fileInputStream != null) {
        		fileInputStream.close();
        	}
        	if (reader != null) {
        		reader.close();
        	}
        	if (br != null) {
        		br.close();
        	}
        }      
        return keyString; 
    }
	
	/**
	 * 功能：加载密钥文件
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	public static String loadKey(String keyfile) throws Exception{  
        BufferedReader br = null;
        InputStreamReader reader= null;
        FileInputStream fileInputStream = null;
        String keyString = null;
		try {
		     fileInputStream = new FileInputStream(keyfile);
			 reader = new InputStreamReader(fileInputStream);
			 br= new BufferedReader(reader);  
             String readLine= null;  
             StringBuilder sb= new StringBuilder();  
             while((readLine= br.readLine())!=null){  
                  if(readLine.charAt(0)=='-'){  
                     continue;  
                  }else{  
                     sb.append(readLine);  
                  }  
             }
             keyString = sb.toString();
        } catch (Exception ex) {
        	ex.printStackTrace();
        }finally {
        	if (fileInputStream != null) {
        		fileInputStream.close();
        	}
        	if (reader != null) {
        		reader.close();
        	}
        	if (br != null) {
        		br.close();
        	}
        }      
        return keyString; 
    }
}
