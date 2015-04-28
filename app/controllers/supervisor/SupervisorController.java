package controllers.supervisor;

import controllers.BaseController;
import controllers.interceptor.SupervisorInterceptor;
import play.mvc.Controller;
import play.mvc.With;

/**
 * 后台控制器基类
 * @author lzp
 * @version 6.0
 * @created 2014-7-1
 */
@With(SupervisorInterceptor.class)
public class SupervisorController extends BaseController {

}
