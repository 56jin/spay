package business;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import models.t_guo_order_details;
import play.Logger;
import play.db.jpa.JPA;
import utils.ErrorInfo;

/**
 * 国付宝订单详情业务类
 * @author yx
 *	@create 2014年12月31日 上午10:03:41
 */
public class GuoOrderDetails implements Serializable{
	
	/**
	 * 保存交易记录
	* @param version 网关版本号
	 * @param charset 字符集
	 * @param signType 加密方式
	 * @param tranCode 交易代码
	 * @param merId 商户代码
	 * @param merName 商户名称
	 * @param tranAmt 投资金额
	 * @param payType 支付方式
	 * @param feeAmt 国付宝手续费
	 * @param feePayer 国付宝手续承担方
	 * @param frontMerUrl 商户前台通知地址
	 * @param tranDateTime 交易时间
	 * @param contractNo 提现专属账户的签约协议号
	 * @param p2pUserId P2p用户在国付宝平台的用户ID
	 * @param virCardNo 国付宝虚拟账号
	 * @param merOrderNum 订单号
	 * @param mercFeeAm P2P平台佣金
	 * @param backgroundMerUrl 商户后台通知地址
	 * @param respCode 响应码
	 * @param msgExt 响应结果信息
	 * @param customerId P2P平台用户ID
	 * @param mobilePhone 开通用户的手机号
	 * @param extantAmt 留存金额
	 * @param orderId 国付宝内部订单号
	 * @param bidId 标号
	 * @param tranFinishTime 交易完成时间
	 * @param mercFeeAmt P2P平台佣金
	 * @param bankPayAmt 银行卡支付金额
	 * @param vcardPayAmt 国付宝虚拟卡支付金额
	 * @param curBal 投资人国付宝虚拟卡可用余额
	 * @param repaymentType 还款类型
	 * @param isInFull 是否全额还款
	 * @param repaymentInfo 还款信息
	 * @param repaymentChargeFeeAmt 还款充值手续费
	 * @param repaymentChargeFeePayer 还款充值手续费承担方
	 * @param tranIP 用户浏览器IP
	 * @param signValue 加密串
	 * @param error 错误信使
	 */
	public void saveOrder(String version, String charset, String signType, String tranCode, String merId, 
			String merName,String tranAmt, String payType, String feeAmt, String feePayer, String frontMerUrl,
			String tranDateTime, String contractNo, String p2pUserId, String virCardNo, String merOrderNum, String mercFeeAm,
			String backgroundMerUrl, String respCode, String msgExt, String customerId, String mobilePhone, 
			String extantAmt, String orderId, String bidId, String tranFinishTime, String mercFeeAmt, String bankPayAmt,
			String vcardPayAmt, String curBal, String repaymentType, String isInFull, String repaymentInfo, String repaymentChargeFeeAmt,
			String repaymentChargeFeePayer, String tranIP, String signValue,ErrorInfo error){
		error.clear();
		
		t_guo_order_details details = reflectOrder(version,  charset,  signType,  tranCode,  merId, 
				 merName, tranAmt,  payType,  feeAmt,  feePayer,  frontMerUrl,
				 tranDateTime,  contractNo,  p2pUserId,  virCardNo,  merOrderNum,  mercFeeAm,
				 backgroundMerUrl,  respCode,  customerId,  mobilePhone, 
				 extantAmt,  orderId,  bidId,  tranFinishTime,  mercFeeAmt,  bankPayAmt,
				 vcardPayAmt,  curBal,  repaymentType,  isInFull,  repaymentInfo,  repaymentChargeFeeAmt,
				 repaymentChargeFeePayer,  tranIP,  signValue);
		
		try{
			
			details.save();
		
		}catch(Exception e){
			
			JPA.setRollbackOnly();
			error.code = -1;
			error.msg = "保存国付宝交易记录日志异常";
			Logger.error("保存国付宝交易记录日志时：%s", e.getMessage());
		}
	}
	
	/**
	 * 参数为全额参数,不可缺一
	 * @param args
	 * @return
	 */
	private t_guo_order_details reflectOrder(String ... args){
		String[] keys = {"version","  charset","  signType","  tranCode","  merId"," merName"," tranAmt","  payType","  feeAmt","  feePayer","  frontMerUrl",
				" tranDateTime","  contractNo","  p2pUserId","  virCardNo","  merOrderNum","  mercFeeAm","backgroundMerUrl","  respCode",
				"  customerId","  mobilePhone"," extantAmt","  orderId","  bidId","  tranFinishTime","  mercFeeAmt","  bankPayAmt"," vcardPayAmt",
				"  curBal","  repaymentType","  isInFull","  repaymentInfo","  repaymentChargeFeeAmt"," repaymentChargeFeePayer","  tranIP","  signValue"};
		int keySize = keys.length;
		Class clz = null;
		t_guo_order_details order = null;
		try {
			clz = Class.forName("models.t_guo_order_details");
		} catch (ClassNotFoundException e1) {

			Logger.error("反射t_guo_order_details类时 : %s", e1.getMessage());
		}
		try {
			order = (t_guo_order_details)clz.newInstance();
		} catch (InstantiationException e1) {
			
			Logger.error("Class实例化t_guo_order_details时 : %s", e1.getMessage());
		} catch (IllegalAccessException e1) {

			Logger.error("Class实例化t_guo_order_details时 : %s", e1.getMessage());
		}
		Method method = null;
		for(int i = 0; i<keySize;i++){
			try {
				method = clz.getMethod("set"+toUpLowcase(keys[i]), String.class);
			} catch (SecurityException e1) {

				Logger.error("Class反射获取Method时 : %s", e1.getMessage());
			} catch (NoSuchMethodException e1) {

				Logger.error("Class反射获取Method时 : %s", e1.getMessage());
			}
			try {
				String value = args[i]==null?"":args[i];
				method.invoke(order, value);  
			} catch (IllegalArgumentException e) {
				
				Logger.error("获取 args参数时 : %s", e.getMessage());
			} catch (IllegalAccessException e) {

				Logger.error("获取 args参数时 : %s", e.getMessage());
			} catch (InvocationTargetException e) {

				Logger.error("Method Invocation 时 : %s", e.getMessage());
			}
			
		}
		return order;
	}
	
	/**
	 *  通过订单号查询订单信息
	 * @param billNo
	 * @param error
	 * @return
	 */
	public t_guo_order_details queryOrderByBillNo(String billNo,ErrorInfo error){
		error.clear();
		
		t_guo_order_details order  = null;
		
		try{
			
		order = t_guo_order_details.find("merOrderNum = ?", billNo).first();
		
		}catch(Exception e){
			
			JPA.setRollbackOnly();
			Logger.error("通过订单号查询订单信息时:%s", e.getMessage());
			error.code = -1;
			error.msg = "通过订单号查询订单信息时异常";
		}
		
		return order;
	}
	
	/**
	 * 第一个字母大小转化
	 * @param value
	 * @return
	 */
	private   String toUpLowcase(String value){
		value = value.trim();
		String one = String.valueOf(value.charAt(0)).toUpperCase();
		return one+value.substring(1);
	}

}
