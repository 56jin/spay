package controllers.interceptor;

import java.io.UnsupportedEncodingException;
import com.shove.security.License;
import business.BackstageSet;
import business.Supervisor;
import constants.Constants;
import controllers.Application;
import controllers.BaseController;
import controllers.supervisor.login.LoginAction;
import controllers.supervisor.systemSettings.SoftwareLicensAction;
import play.Logger;
import play.Play;
import play.mvc.Before;
import play.mvc.Controller;
import utils.ErrorInfo;
import utils.Security;

public class FInterceptor extends BaseController{
	
	@Before
	public static void checkLogin(){
		/*try{
			License.update(BackstageSet.getCurrentBackstageSet().registerCode);
			if(!(License.getDomainNameAllow() && License.getAdminPagesAllow())) {
				flash.put("error", "此版本非正版授权，请联系晓风软件购买正版授权！");
				SoftwareLicensAction.notRegister();
			}
		}catch (Exception e) {
			e.printStackTrace();
			Logger.info("进行正版校验时：" + e.getMessage());
			flash.put("error", "此版本非正版授权，请联系晓风软件购买正版授权！");
			SoftwareLicensAction.notRegister();
		}*/
		
		if (Supervisor.isLogin()) {
			return;
		}
		
		LoginAction.loginInit();
	}
	
}
