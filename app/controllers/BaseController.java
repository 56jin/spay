package controllers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import constants.Constants;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;
import play.Logger;
import play.Play;
import play.libs.Files;
import play.mvc.Before;
import play.mvc.Controller;
import utils.ErrorInfo;
import utils.FileType;
import utils.FileUtil;

public class BaseController extends Controller{

//	@Before
	private static void injectionInterceptor() throws Exception {
		String injectionVal = new com.shove.web.security.InjectionInterceptor().run();
		if(injectionVal == null || injectionVal.length() > 0){
			render(Constants.ERROR_PAGE_PATH_INJECTION,injectionVal);
		}
	}
}
