package constants;

import com.shove.Convert;
import play.Play;

/**
 * 常量值
 * 
 * @author bsr
 * @version 6.0
 * @created 2014-4-7 下午04:07:56
 */
public class Constants {
	public static String VERSION1 = "1.0";
	public static String VERSION2= "2.0";
	
	public static final String BASE_URL = Play.configuration.getProperty("test.application.baseUrl") + Play.configuration.getProperty("http.path");
	public static final String EMAIL = Play.configuration.getProperty("mail.smtp.user");
	public static final String ENCRYPTION_KEY = Play.configuration.getProperty("fixed.secret");//加密key

	public static final int ALREADY_RUN = -11; // 某个操作已成功执行
	public static int FAIL_CODE = -1315;	// 第三方处理失败
	
	/**
	 * 所有支付接口id
	 */
	public static final int PNR = 4;  //汇付

	/**
	 * 支付接口是否使用
	 */
	public static final boolean USE = true;
	public static final boolean NOT_USE = false;
	
	public static final String SQL_PATH = Play.configuration.getProperty("sql.path");					//数据库备份文件路径
	public static final String URL_IP_LOCATION = "http://int.dpool.sina.com.cn/iplookup/iplookup.php?format=json";//查询ip地址接口
	
	public static final String SUPERVISOR_INITIAL_PASSWORD = "123456";
	
	public static final String ERROR_PAGE_PATH_FRONT = "@Application.errorFront";
	public static final String ERROR_PAGE_PATH_SUPERVISOR = "@Application.errorSupervisor";
	public static final String ERROR_PAGE_PATH_INJECTION = "@Application.injection";
	public static final String COOKIE_KEY_SUPERVISOR_ID = "something";
	
	public static final int PAGE_SIZE = 10;
	public static final int PAGE_SIZE_EIGHT = 8;
	/**
	 * 校验手机验证码
	 */
	public static final boolean CHECK_CODE = Play.configuration.getProperty("application.mode").equals("dev") ? false : true;
	
	/**
	 * 加密串有效时间(s)
	 */
	public static final int VALID_TIME = 3600;
	
	/**
	 * 是否启用密码错误次数超限锁定(0不启用，1启用)
	 */
	public static final int CLOSE_LOCK = 0;
	public static final int OPEN_LOCK = 1;
	
	/**
	 * 缓存时间
	 */
	public static final String CACHE_TIME = "10min";
	
	/**
	 * 邮件发送标识
	 */
	public static final String ACTIVE = "active";
	public static final String PASSWORD = "resetPassword";
	public static final String SECRET_QUESTION = "secretQuestion";
	
	/**
	 * 部分加密action标识
	 */
	public static final String BID_ID_SIGN = "b"; // 标ID
	public static final String BILL_ID_SIGN = "bill"; // 标ID
	public static final String PRODUCT_ID_SIGN = "p"; // 产品ID
	public static final String USER_ID_SIGN = "u"; // 用户ID
	public static final String SUPERVISOR_ID_SIGN = "supervisor_id"; // 管理员ID
	public static final String ITEM_ID_SIGN = "i"; // 资料ID
	public static final String USER_ITEM_ID_SIGN = "ui"; // 用户资料ID
	
	/**
	 * ajax标识
	 */
	public static final String IS_AJAX = "1";
	
	/**
	 * 进行了加密的方法
	 */
	/*修改手机*/
	public static final String VERIFY_SAFE_QUESTION = "front.account.BasicInformation.verifySafeQuestion";
	public static final String SET_SAFE_QUESTION = "front.account.BasicInformation.resetSafeQuestion";
	public static final String PASSWORD_EMAIL = "front.account.LoginAndRegisterAction.resetSafeQuestion";
	
	/**
	 * 固定的路径
	 */
	public static final String LOGIN = BASE_URL + "login";
	public static final String QUICK_LOGIN = BASE_URL + "quick/login";
	public static final String RESET_PASSWORD_EMAIL = BASE_URL + "front/account/resetPassword?sign=";
	public static final String RESET_QUESTION_EMAIL = BASE_URL + "front/account/resetQuestion?sign=";
	public static final String ACTIVE_EMAIL = BASE_URL+"front/account/accountActivation?sign=";
	

	public static final int ONE = 1;
	public static final int TEN = 10;


	public static final int PICTURE_SIZE = 3000000; // 图片限制大小


	/**
	 * 分页主题
	 */
	public static final int PAGE_SIMPLE = 1;
	public static final int PAGE_ASYNCH = 2;
	
	/**
	 * 默认风格
	 */
	public static final int PAGE_STYLE_DEFAULT = 1;
	


	
	/**
	 * 系统管理员supervisor
	 * @author lzp
	 * @version 6.0
	 * @created 2014-5-27
	 */
	public static class SystemSupervisor {
		public static final long ID = 1;
	}
	
	/**
	 * 超级管理员组
	 * @author lzp
	 * @version 6.0
	 * @created 2014-5-27
	 */
	public class SystemSupervisorGroup {
		public static final long ID = 1;
		public static final String NAME = "超级管理员组";
	}

	/**
	 * 数据库操作类型
	 * @author lzp
	 * @version 6.0
	 * @created 2014-7-22
	 */
	public class DBOperationType {
		public static final int CLEAR = 0;
		public static final int RESET = 1;
		public static final int RECOVER = 2;
		public static final int BACKUP = 3;
	}

	/**
	 * 消息查询关键字类型
	 * @author lzp
	 * @version 6.0
	 * @created 2014-5-27
	 */
	public class MessageKeywordType {
		public static final int Title = 1; //标题
		public static final int SenderName = 2; //发信人
	}
	

	/**
	 * 管理员等级
	 * @author lzp
	 * @version 6.0
	 * @created 2014-5-27
	 */
	public class SupervisorLevel {
		public static final int Normal = 0; //普通管理员
		public static final int Super = 1;  //超级管理员
	}
	
	/**
	 * 性别
	 * @author lzp
	 * @version 6.0
	 * @created 2014-5-27
	 */
	public class Sex {
		public static final int Man = 1;
		public static final int Woman = 2;
		public static final int Unknown = 3;
	}
	

	/**
	 * 查询客服关键字
	 */
	public static final String[] QUERY_CUSTOMER_KEYWORD_TYPE = {//全部，编号，姓名，手机，邮箱
		" and (customer_num like ? or reality_name like ? or mobile1 like ? or email like ?) ",
		" and (customer_num like ?) ",
		" and (reality_name like ?) ",
		" and (mobile1 like ?) ",
		" and (email like ?) "};
	
	/**
	 * 查询事件关键字
	 * 0 全部, 1 ip地址, 2 操作内容, 3 管理员名字
	 */
	public static final String[] QUERY_EVENT_KEYWORD = {
		" and (ip like ? or descrption like ? or supervisor_name like ?) ",
		" and (ip like ?) ",
		" and (descrption like ?) ",
		" and (supervisor_name like ?) "
		};
	

	
	/**
	 * 文件格式
	 * @author lzp
	 * @version 6.0
	 * @created 2014-8-16
	 */
	public class FileFormat {
		public static final int IMG = 1;	//图片
		public static final int TXT = 2;	//文本
		public static final int VIDEO = 3;	//视频
		public static final int AUDIO = 4;	//音频
		public static final int XLS = 5;	//表格
	}
	


	/**
	 * 平台交易记录排序
	 */
	public static final String[] MEMBER_ORDER = 
		{
			" ",
			" order by t_member_details.id desc ",
			" order by t_member_details.amount asc ",
			" order by t_member_details.amount desc ",
			" order by t_member_details.time asc ",
			" order by t_member_details.time desc "
		};
	
	/**
	 * 平台事件记录排序
	 */
	public static final String[] EVENT_ORDER = 
		{
			" ",
			" order by t_member_events.id desc ",
			" order by t_member_events.time asc ",
			" order by t_member_events.time desc "
		};
	
	/**
	 * 平台交易记录排序
	 */
	public static final String[] MEMBER_TYPE = 
		{
			" and (t_platforms.name like ? or t_member_of_platforms.platform_member_name like ? or t_member_details.serial_number like ?) ",
			" and t_platforms.name like ? ",
			" and t_member_of_platforms.platform_member_name like ? ",
			" and t_member_details.serial_number like ? ",
		};
	
	/**
	 * 平台交易记录排序
	 */
	public static final String[] PLATFORM_TYPE = 
		{
			" and (name like ? or gateway like ? ) ",
			" and name like ? ",
			" and gateway like ? ",
		};
	
	/**
	 * 平台交易记录排序
	 */
	public static final String[] PLATFORM_ORDER = 
		{
		" ",
		" order by id desc ",
		" order by time asc ",
		" order by time desc "
		};

	/**
	 * 云盾校验签名
	 */
	public static final String CLOUD_SHIELD_SIGN_FAULT = "-3";
	
	/**
	 * 云盾校验服务器时间
	 */
	public static final String CLOUD_SHIELD_SERVICE_TIME = "-4";
	
	/**
	 * 校验当前云盾没有插入
	 */
	public static final String CLOUD_SHIELD_NOT_EXIST = "-1";
	
	/**
	 * 校验云盾不属于当前管理员
	 */
	public static final String CLOUD_SHIELD_SUPERVISOR = "-2";

	public static final String GOLD_WAY_SERVICE_URL = Play.configuration.getProperty("jintong.service.url");
	public static final String GOLD_WAY_PRIVATE_KEY = Play.applicationPath + Play.configuration.getProperty("jintong.privateKey");
	public static final String GOLD_WAY_PUB_KEY = Play.applicationPath +  Play.configuration.getProperty("jintong.pubKey");
	public static final String DEDUCT_REAL_PAY_SERVICE_CODE = Play.configuration.getProperty("jintong.deductRealPay.serviceCode");
	public static final String DEDUCT_REAL_PAYEE_SERVICE_CODE = Play.configuration.getProperty("jintong.deductRealPayee.serviceCode");

}
