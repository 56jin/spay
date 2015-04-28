package models;

import javax.persistence.Entity;

import play.db.jpa.Model;

/**
 * 国付宝异步回调记录表
 * 
 * @author yx
 * @create 2014年12月31日 上午9:56:13
 */
@Entity
public class t_guo_order_details extends Model {
	
	public String version;  //网关版本号
	public String charset;  //字符集
	public String signType;  //加密方式
	public String tranCode;  //交易代码
	public String merId;  //商户代码
	public String merName;  //商户名称
	public String tranAmt;  //投资金额
	public String payType;  //支付方式
	public String feeAmt;  //国付宝手续费
	public String feePayer;  //国付宝手续承担方
	public String frontMerUrl;  //商户前台通知地址
	public String tranDateTime;  //交易时间
	public String contractNo;  //提现专属账户的签约协议号
	public String p2pUserId;  //P2p用户在国付宝平台的用户ID
	public String virCardNo;  //国付宝虚拟账号
	public String merOrderNum;  //订单号
	public String mercFeeAm;  //P2P平台佣金
	public String backgroundMerUrl;  //商户后台通知地址
	public String respCode;  //响应码
//	public String msgExt;  //响应结果信息
	public String customerId;  //P2P平台用户ID
	public String mobilePhone;  //开通用户的手机号
	public String extantAmt;  //留存金额
	public String orderId;  //国付宝内部订单号
	public String bidId;  //标号
	public String tranFinishTime;  //交易完成时间
	public String mercFeeAmt;  //P2P平台佣金
	public String bankPayAmt;  //银行卡支付金额
	public String vcardPayAmt;  //国付宝虚拟卡支付金额
	public String curBal;  //投资人国付宝虚拟卡可用余额
	public String repaymentType;  //还款类型
	public String isInFull;  //是否全额还款
	public String repaymentInfo;  //还款信息
	public String repaymentChargeFeeAmt;  //还款充值手续费
	public String repaymentChargeFeePayer;  //还款充值手续费承担方
	public String tranIP;  //用户浏览器IP
	public String signValue;  //加密串
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public String getCharset() {
		return charset;
	}
	public void setCharset(String charset) {
		this.charset = charset;
	}
	public String getSignType() {
		return signType;
	}
	public void setSignType(String signType) {
		this.signType = signType;
	}
	public String getTranCode() {
		return tranCode;
	}
	public void setTranCode(String tranCode) {
		this.tranCode = tranCode;
	}
	public String getMerId() {
		return merId;
	}
	public void setMerId(String merId) {
		this.merId = merId;
	}
	public String getMerName() {
		return merName;
	}
	public void setMerName(String merName) {
		this.merName = merName;
	}
	public String getTranAmt() {
		return tranAmt;
	}
	public void setTranAmt(String tranAmt) {
		this.tranAmt = tranAmt;
	}
	public String getPayType() {
		return payType;
	}
	public void setPayType(String payType) {
		this.payType = payType;
	}
	public String getFeeAmt() {
		return feeAmt;
	}
	public void setFeeAmt(String feeAmt) {
		this.feeAmt = feeAmt;
	}
	public String getFeePayer() {
		return feePayer;
	}
	public void setFeePayer(String feePayer) {
		this.feePayer = feePayer;
	}
	public String getFrontMerUrl() {
		return frontMerUrl;
	}
	public void setFrontMerUrl(String frontMerUrl) {
		this.frontMerUrl = frontMerUrl;
	}
	public String getTranDateTime() {
		return tranDateTime;
	}
	public void setTranDateTime(String tranDateTime) {
		this.tranDateTime = tranDateTime;
	}
	public String getContractNo() {
		return contractNo;
	}
	public void setContractNo(String contractNo) {
		this.contractNo = contractNo;
	}
	public String getP2pUserId() {
		return p2pUserId;
	}
	public void setP2pUserId(String p2pUserId) {
		this.p2pUserId = p2pUserId;
	}
	public String getVirCardNo() {
		return virCardNo;
	}
	public void setVirCardNo(String virCardNo) {
		this.virCardNo = virCardNo;
	}
	public String getMerOrderNum() {
		return merOrderNum;
	}
	public void setMerOrderNum(String merOrderNum) {
		this.merOrderNum = merOrderNum;
	}
	public String getMercFeeAm() {
		return mercFeeAm;
	}
	public void setMercFeeAm(String mercFeeAm) {
		this.mercFeeAm = mercFeeAm;
	}
	public String getBackgroundMerUrl() {
		return backgroundMerUrl;
	}
	public void setBackgroundMerUrl(String backgroundMerUrl) {
		this.backgroundMerUrl = backgroundMerUrl;
	}
	public String getRespCode() {
		return respCode;
	}
	public void setRespCode(String respCode) {
		this.respCode = respCode;
	}
	public String getCustomerId() {
		return customerId;
	}
	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}
	public String getMobilePhone() {
		return mobilePhone;
	}
	public void setMobilePhone(String mobilePhone) {
		this.mobilePhone = mobilePhone;
	}
	public String getExtantAmt() {
		return extantAmt;
	}
	public void setExtantAmt(String extantAmt) {
		this.extantAmt = extantAmt;
	}
	public String getOrderId() {
		return orderId;
	}
	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}
	public String getBidId() {
		return bidId;
	}
	public void setBidId(String bidId) {
		this.bidId = bidId;
	}
	public String getTranFinishTime() {
		return tranFinishTime;
	}
	public void setTranFinishTime(String tranFinishTime) {
		this.tranFinishTime = tranFinishTime;
	}
	public String getMercFeeAmt() {
		return mercFeeAmt;
	}
	public void setMercFeeAmt(String mercFeeAmt) {
		this.mercFeeAmt = mercFeeAmt;
	}
	public String getBankPayAmt() {
		return bankPayAmt;
	}
	public void setBankPayAmt(String bankPayAmt) {
		this.bankPayAmt = bankPayAmt;
	}
	public String getVcardPayAmt() {
		return vcardPayAmt;
	}
	public void setVcardPayAmt(String vcardPayAmt) {
		this.vcardPayAmt = vcardPayAmt;
	}
	public String getCurBal() {
		return curBal;
	}
	public void setCurBal(String curBal) {
		this.curBal = curBal;
	}
	public String getRepaymentType() {
		return repaymentType;
	}
	public void setRepaymentType(String repaymentType) {
		this.repaymentType = repaymentType;
	}
	public String getIsInFull() {
		return isInFull;
	}
	public void setIsInFull(String isInFull) {
		this.isInFull = isInFull;
	}
	public String getRepaymentInfo() {
		return repaymentInfo;
	}
	public void setRepaymentInfo(String repaymentInfo) {
		this.repaymentInfo = repaymentInfo;
	}
	public String getRepaymentChargeFeeAmt() {
		return repaymentChargeFeeAmt;
	}
	public void setRepaymentChargeFeeAmt(String repaymentChargeFeeAmt) {
		this.repaymentChargeFeeAmt = repaymentChargeFeeAmt;
	}
	public String getRepaymentChargeFeePayer() {
		return repaymentChargeFeePayer;
	}
	public void setRepaymentChargeFeePayer(String repaymentChargeFeePayer) {
		this.repaymentChargeFeePayer = repaymentChargeFeePayer;
	}
	public String getTranIP() {
		return tranIP;
	}
	public void setTranIP(String tranIP) {
		this.tranIP = tranIP;
	}
	public String getSignValue() {
		return signValue;
	}
	public void setSignValue(String signValue) {
		this.signValue = signValue;
	}
	@Override
	public String toString() {
		return "t_guo_order_details [version=" + version + ", charset="
				+ charset + ", signType=" + signType + ", tranCode=" + tranCode
				+ ", merId=" + merId + ", merName=" + merName + ", tranAmt="
				+ tranAmt + ", payType=" + payType + ", feeAmt=" + feeAmt
				+ ", feePayer=" + feePayer + ", frontMerUrl=" + frontMerUrl
				+ ", tranDateTime=" + tranDateTime + ", contractNo="
				+ contractNo + ", p2pUserId=" + p2pUserId + ", virCardNo="
				+ virCardNo + ", merOrderNum=" + merOrderNum + ", mercFeeAm="
				+ mercFeeAm + ", backgroundMerUrl=" + backgroundMerUrl
				+ ", respCode=" + respCode + ", customerId=" + customerId
				+ ", mobilePhone=" + mobilePhone + ", extantAmt=" + extantAmt
				+ ", orderId=" + orderId + ", bidId=" + bidId
				+ ", tranFinishTime=" + tranFinishTime + ", mercFeeAmt="
				+ mercFeeAmt + ", bankPayAmt=" + bankPayAmt + ", vcardPayAmt="
				+ vcardPayAmt + ", curBal=" + curBal + ", repaymentType="
				+ repaymentType + ", isInFull=" + isInFull + ", repaymentInfo="
				+ repaymentInfo + ", repaymentChargeFeeAmt="
				+ repaymentChargeFeeAmt + ", repaymentChargeFeePayer="
				+ repaymentChargeFeePayer + ", tranIP=" + tranIP
				+ ", signValue=" + signValue + "]";
	}
	 
	
	
	
	
	
}
