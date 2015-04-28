package constants;

public class EventTypes {

	/**
	 * 1注册2登录3注销4改密5密码错误6修改安全问题7；201发标202借款撤单；401所有以理财人操作的；601所有会员操作的；
	 * 1001管理员的操作
	 * 2001系统的操作 
	 */
	
	/**
	 * 用户
	 */
	public static final long USER_LOGIN = 1;  //登录
	public static final long USER_REGIST = 2; //注册
	public static final long USER_LOGOUT = 3; //注销
	public static final long USER_ADD_SECRET_QUESTION = 4; //添加安全问题
	public static final long USER_EDIT_SECRET_QUESTION = 5; //修改安全问题
	public static final long USER_VERIFY_SECRET_QUESTION = 6; //修改安全问题

	/**
	 * 管理员
	 */
	public static final long LOGIN = 1001;
	public static final long REGIST = 1002;
	public static final long LOGOUT = 1003;
	
	public static final long ADD_SECRET_QUESTION = 1101;
	public static final long EDIT_SECRET_QUESTION = 1102;
	public static final long DELETE_SECRET_QUESTION = 1104;
	
	public static final long ADD_BID_PRODUCT = 1201; //添加借款标产品
	public static final long EDIT_BID_PRODUCT = 1202; //编辑借款标产品
	public static final long DETELE_BID_PRODUCT = 1203; //删除借款标产品
	public static final long ADD_AUDIT_ITEM = 1211;  //添加审核资料
	public static final long EDIT_AUDIT_ITEM = 1212;  //编辑
	public static final long DELETE_AUDIT_ITEM = 1213; //修改
	
	public static final long OVERDUE_MARK = 1310;  //账单标记逾期
	public static final long BADBILL_MARK = 1311;  //标记坏账
	
	
	/**
	 * 1注册2登录3注销4改密5密码错误6修改安全问题7；201发标202借款撤单；401所有以理财人操作的；601所有会员操作的；
	 * 1001管理员的操作
	 * 2001系统的操作 
	 */
	
	
	
}
