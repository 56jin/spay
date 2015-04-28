package controllers.supervisor.managementHome;

import constants.Constants;
import controllers.supervisor.SupervisorController;
import play.mvc.Controller;
import utils.Arith;
import utils.ErrorInfo;

/**
 * 管理首页
 * @author zhs
 *
 */
public class HomeAction  extends SupervisorController {
	/**
	 * 管理首页
	 */
	public static void showHome() {
		render();
	}
}
