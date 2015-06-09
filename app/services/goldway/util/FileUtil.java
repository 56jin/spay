package services.goldway.util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;

/**
 * ������ FileUtil
 * ���ܣ��ļ�������
 * ŵ�ǽ�ͨ ��Ȩ����
 * @author zyc    2014-10-14
 * @version 1.0
 */
public class FileUtil {
	
	/**
	 * ���ܣ������ı��ļ�
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
        	throw new Exception("�����ļ����ݳ���δ�����쳣!!");
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
	 * ���ܣ�������Կ�ļ�
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
