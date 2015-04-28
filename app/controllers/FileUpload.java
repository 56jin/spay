package controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import constants.Constants;
import constants.Constants.FileFormat;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import play.Logger;
import play.Play;
import play.libs.Files;
import play.mvc.Controller;
import utils.ErrorInfo;
import utils.FileType;
import utils.FileUtil;

public class FileUpload extends BaseController{

	public static void upload(File attachment) {
		ErrorInfo error = new ErrorInfo();
		FileType type = FileUtil.uploadFile(attachment,error);
		
		JSONObject json = new JSONObject();
		
		if(error.code < 0) {
			json.put("error", error.msg);
			renderJSON(json);
		}
		
		json.put("type", type);
		
		renderJSON(type);
		
		
	}
	
	/**
	 * 上传文件
	 * @param file
	 * @param type(1 图片、2 文本、3 视频、4 音频、5 表格)
	 */
	public static void uploadFile(File file, int type) {
		ErrorInfo error = new ErrorInfo();
		Map<String, Object> fileInfo = FileUtil.uploadFile(file, type, error);
		
		if (error.code < 0) {
			renderText(JSONObject.fromObject(error).toString());
		}
		
		renderText(JSONObject.fromObject(fileInfo).toString());
	}
	
	/**
	 * 上传图片(用于编辑器)
	 * @param imgFile
	 */
	public static void uploadImage2(File imgFile) {
		ErrorInfo error = new ErrorInfo();
		FileType type = FileUtil.uploadFile(imgFile, error);
		if (error.code < 0) {
			JSONObject json = new JSONObject();
			json.put("error", error);
			
			renderText(json.toString());
		}
		
		String filename = type.filePath.replaceAll("\\\\", "/");
		
		JSONObject json = new JSONObject();
		 json.put("error", 0);
		 json.put("url",filename);
		
		
		renderText(json.toString());
	}
	
	/**
	 * 上传图片
	 * @param imgFile
	 */
	public static void uploadImage(File imgFile) {
		ErrorInfo error = new ErrorInfo();
		FileType type = FileUtil.uploadFile(imgFile, error);
		if (error.code < 0) {
			JSONObject json = new JSONObject();
			json.put("error", error);
			
			renderText(json.toString());
		}
		
		String filename = type.filePath.replaceAll("\\\\", "/");
		
		JSONObject json = new JSONObject();
		json.put("filename", filename);
		json.put("error", error);
		
		renderText(json.toString());
	}
	
	/**
	 * 上传图片
	 * @param imgFile
	 */
	public static void uploadImageReturnType(File imgFile) {
		ErrorInfo error = new ErrorInfo();
		FileType type = FileUtil.uploadFile(imgFile, error);
		if (error.code < 0) {
			JSONObject json = new JSONObject();
			json.put("error", error);
			
			renderText(json.toString());
		}
		
		type.filePath = type.filePath.replaceAll("\\\\", "/");
		type.size = type.size;
		type.resolution = type.resolution;
		
		JsonConfig config = new JsonConfig();  
		config.setExcludes(new String[]{"file"}); 
		JSONArray array = JSONArray.fromObject(type, config);
		
		renderText(array.toString());
	}
}
