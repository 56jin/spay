package utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.activation.MimetypesFileTypeMap;

import org.apache.commons.lang.StringUtils;

import constants.Constants;
import constants.Constants.FileFormat;

import net.sf.json.JSONObject;
import play.Logger;
import play.Play;
import play.db.jpa.Blob;
import play.libs.Files;

/**
 * 文件操作
 * 
 * @author bsr
 * @version 6.0
 * @created 2014-5-5 上午11:47:05
 */
public class FileUtil {

	public static FileType uploadFile(File imgFile, ErrorInfo error) {
		error.clear();

		if (null == imgFile) {
			error.code = -1;
			error.msg = "上传文件为空";

			return null;
		}

		FileType fileType = new FileType();

		fileType.file = imgFile;

		if (fileType.file == null) {
			error.code = -1;
			error.msg = "上传文件为空";

			return null;
		}

		String type = fileType.fileType;

		if (type == null) {
			error.code = -2;
			error.msg = "上传文件类型有误";

			return null;
		}

		FileInputStream fis = null;

		try {
			fis = new FileInputStream(imgFile);
			Blob blob = new Blob();
			blob.set(fis, "image/png");
			fileType.filePath = "/images?uuid=" + blob.getUUID();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
			Logger.error("上传图片 复制文件 出现异常!" + e.getMessage());
			error.code = -4;
			error.msg = "上传图片 复制文件 出现异常";
		}

		error.msg = "上传图片成功";

		return fileType;
	}
	
	/**
	 * 上传文件
	 * @param file
	 * @param type(1 图片、2 文本、3 视频、4 音频、5 表格)
	 * @param error
	 * @return
	 */
	public static Map<String, Object> uploadFile(File file, int type, ErrorInfo error) {
		error.clear();
		 
		if (null == file) {
			error.code = -1;
			error.msg = "上传文件为空";

			return null;
		}
		
		if (type < FileFormat.IMG || type > FileFormat.XLS) {
			error.code = -1;
			error.msg = "上传文件格式有误";

			return null;
		}
		
		String fileName = file.getName();
		String fileExt = fileName.substring(fileName.lastIndexOf(".")+1);
		
		switch (type) {
		case FileFormat.IMG:
			if (!"GIF,JPG,JPEG,PNG,BMP".contains(fileExt.toUpperCase())) {
				error.code = -1;
				error.msg = "文件格式有误，请上传图片(gif,jpg,jpeg,png,bmp)文件";

				return null;
			}
			
			if(file.length() > Constants.PICTURE_SIZE){
				error.code = -1;
				error.msg = "图片文件过大!";

				return null;
			}
			
			break;
		case FileFormat.TXT:
			if (!"TXT".contains(fileExt.toUpperCase())) {
				error.code = -1;
				error.msg = "文件格式有误，请上传文本(txt)文件";

				return null;
			}
			break;
		case FileFormat.VIDEO:
			if (!"MP4,3GP,AVI,WMV,RM,RMVB".contains(fileExt.toUpperCase())) {
				error.code = -1;
				error.msg = "文件格式有误，请上传视频(mp4,3gp,avi,wmv,rm,rmvb)文件";

				return null;
			}
			break;
		case FileFormat.AUDIO:
			if (!"MP3,WAV,WMA".contains(fileExt.toUpperCase())) {
				error.code = -1;
				error.msg = "文件格式有误，请上传音频(mp3,wav,wma)文件";

				return null;
			}
			break;
		case FileFormat.XLS:
			if (!"XLS".contains(fileExt.toUpperCase())) {
				error.code = -1;
				error.msg = "文件格式有误，请上传表格(xls)文件";

				return null;
			}
			break;
		default:
			break;
		}
		
		FileInputStream fis = null;
		
		try {
			fis = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			Logger.error(e.getMessage());
			error.code = -4;
			error.msg = "找不到文件"+file.getName();
			
			return null;
		}
		
		Blob blob = new Blob();
		blob.set(fis, "");
		String filePre = (FileFormat.IMG == type) ? "/images?uuid=" : "";
		
		Map<String, Object> fileInfo = new HashMap<String, Object>();
		fileInfo.put("fileType", fileExt);
		fileInfo.put("size", Arith.div(file.length(), 1024, 2));
		fileInfo.put("fileName", filePre + blob.getUUID() + "." + fileExt);
		
		return fileInfo;
	}
	
	public static File zipFiles(String[] files, String targetZipFile) {
		Blob blob = new Blob();
		File targetFile = new File(targetZipFile);
		FileOutputStream target = null;
		
		try {
			target = new FileOutputStream(targetFile);
		} catch (FileNotFoundException e1) {
		}
			
		ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(target));
		int BUFFER_SIZE = 1024;
		int count;
		byte buff[] = new byte[BUFFER_SIZE];
		File file = null;
		
		for (int i = 0; i < files.length; i++) {
			String[] arr = files[i].split("\\.");
			
			if (arr.length < 1) {
				continue;
			}
			
			file = new File(blob.getStore(), arr[0]);
			FileInputStream fi = null;
			
			try {
				fi = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				continue;
			}
			
			BufferedInputStream origin = new BufferedInputStream(fi);
			ZipEntry entry = new ZipEntry(file.getName() + "." + (arr.length<2 ? "png" : arr[1]));
			
			try {
				out.putNextEntry(entry);
			} catch (IOException e) {
				continue;
			}
			
			
			try {
				while ((count = origin.read(buff)) != -1) {
					out.write(buff, 0, count);
				}
			} catch (IOException e) {
				continue;
			}
			
			try {
				origin.close();
			} catch (IOException e) {
				continue;
			}
		}
		
		try {
			out.close();
		} catch (IOException e) {
		}

		return targetFile;
	}
	
	public static File zipImages(String[] images, String targetZipFile) {
		Blob blob = new Blob();
		File targetFile = new File(targetZipFile);
		FileOutputStream target = null;
		
		try {
			target = new FileOutputStream(targetFile);
		} catch (FileNotFoundException e1) {
		}
			
		ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(target));
		int BUFFER_SIZE = 1024;
		int count;
		byte buff[] = new byte[BUFFER_SIZE];
		File file = null;
		
		for (int i = 0; i < images.length; i++) {
			file = new File(blob.getStore(), images[i].split("\\.")[0]);
			FileInputStream fi = null;
			
			try {
				fi = new FileInputStream(file);
			} catch (FileNotFoundException e) {
				continue;
			}
			
			BufferedInputStream origin = new BufferedInputStream(fi);
			ZipEntry entry = new ZipEntry(file.getName() + ".png");
			
			try {
				out.putNextEntry(entry);
			} catch (IOException e) {
				continue;
			}
			
			
			try {
				while ((count = origin.read(buff)) != -1) {
					out.write(buff, 0, count);
				}
			} catch (IOException e) {
				continue;
			}
			
			try {
				origin.close();
			} catch (IOException e) {
				continue;
			}
		}
		
		try {
			out.close();
		} catch (IOException e) {
		}

		return targetFile;
	}
	
	public static File getStore(String path) {
        String name = path;
        File store = null;
        if(new File(name).isAbsolute()) {
            store = new File(name);
        } else {
            store = Play.getFile(name);
        }
        if(!store.exists()) {
            store.mkdirs();
        }
        return store;
    }
	
	
	/**
	 * 创建文件夹
	 * @param path
	 */
	public static void mkDir(String path){
		File file = new File(path);
		if(!file.exists()){
			file.mkdirs();
		}
	}
	
	/**
	 * 根据当前日期获取文件夹路径 eg:2015\1\3
	 * @return
	 */
	public static String getPathByCurrentDate(){
		String path = DateUtil.getYear() + File.separator + DateUtil.getMonth() + File.separator + DateUtil.getDay();
		return path;
	}
	
	
	
	
	public static void main(String[] args) {
//		FileUtil.getStore("d:/test/test/test/test/124.txt");
		String path = "c:/test/test/test/test";
		mkDir(path);
		Logger.info(getPathByCurrentDate());
	}
}
