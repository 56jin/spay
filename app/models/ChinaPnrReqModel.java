package models;

import java.io.Serializable;

import play.libs.Codec;

import com.shove.security.Encrypt;

/**
 * 汇付天下请求参数模型
 * 
 * @author yx
 * @create 2014年11月26日 下午7:35:52
 */
public class ChinaPnrReqModel implements Serializable {

	private String version; // 版本号 must
	private String cmdId; // 消息类型 must
	private String merCustId; // 商户客户号 must
	private String bgRetUrl; // 商户后台应答地址 must
	private String retUrl; // 页面返回URL option
	private String usrId; // 用户号 option
	private String usrName; // 真实姓名 option
	private String loginPwd;  //用户登录密码
	private String transPwd;  //用户交易密码
	private String idType; // 证件类型 option
	private String idNo; // 证件号码 option
	private String usrMp; // 手机号 option
	private String usrEmail; // 用户 Email option
	private String merPriv; // 商户私有域 option
	private String charSet; // 编码集 option
	private String chkValue; // 签名 option

	private String usrCustId; // 用户客户号 must
	private String ordId; // 订单号
	private String ordDate; // 订单日期
	private String gateBusiId; // 支付网关业务代号
	private String openBankId; // 开户银行代号
	private String dcFlag; // 借贷记标记
	private String transAmt; // 交易金额
	private String outCustId; // 出账客户号
	private String outAcctId; // 出账子账户

	private String inCustId; // 入账客户号
	private String inAcctId; // 入账子账户
	private String subOrdId;  //订单号 
	
	private String subOrdDate;  //订单日期
	private String fee;  //扣款手续费
	private String divDetails;  //分账账户串
	private String divAcctId;  //分账账户号 
	private String divAmt;  //分账金额 
	private String trxId; //本平台交易唯一标识
	private String queryTransType;  //交易查询类型
	private String feeObjFlag;  //续费收取对象标志I/O I--向入款客户号 InCustId 收取 O--向出款客户号 OutCustId 收取
	private String reqExt;  //入参扩展域
	private String proId;  //项目 ID
	
	private String divCustId;  //分账商户号
	
	
	private String beginDate;  //开始时间 
	private String endDate;  //开始时间 
	private String pageNum;  //开始时间 
	private String pageSize;  //开始时间 
	
	private String unFreezeOrdId;  //解冻订单号
	private String freezeTrxId;  //冻结标志
	private String maxTenderRate;  //最大投资手续费率
	private String borrowerDetails;  //借款人信息
	private String isFreeze;  //是否冻结
	private String freezeOrdId;  //冻结订单号
	private String borrCustId;  //借款人 ID
	private String borrTotAmt;  //借款人金额
	private String yearRate;
	
	private String retType;
	private String bidStartDate;
	private String bidEndDate;
	private String retAmt;
	private String retDate;
	private String guarAmt;  // p2p借款保证金,// 汇付担保金额
	private String servFee;
	private String servFeeAcctId;
	private String certId;
	
	
	public String getCertId() {
		return certId;
	}

	public void setCertId(String certId) {
		this.certId = certId;
	}

	public String getServFeeAcctId() {
		return servFeeAcctId;
	}

	public void setServFeeAcctId(String servFeeAcctId) {
		this.servFeeAcctId = servFeeAcctId;
	}

	public String getServFee() {
		return servFee;
	}

	public void setServFee(String servFee) {
		this.servFee = servFee;
	}

	public String getGuarAmt() {
		return guarAmt;
	}

	public void setGuarAmt(String guarAmt) {
		this.guarAmt = guarAmt;
	}

	public String getRetDate() {
		return retDate;
	}

	public void setRetDate(String retDate) {
		this.retDate = retDate;
	}

	public String getRetAmt() {
		return retAmt;
	}

	public void setRetAmt(String retAmt) {
		this.retAmt = retAmt;
	}

	public String getBidEndDate() {
		return bidEndDate;
	}

	public void setBidEndDate(String bidEndDate) {
		this.bidEndDate = bidEndDate;
	}

	public String getBidStartDate() {
		return bidStartDate;
	}

	public void setBidStartDate(String bidStartDate) {
		this.bidStartDate = bidStartDate;
	}

	public String getRetType() {
		return retType;
	}

	public void setRetType(String retType) {
		this.retType = retType;
	}

	public String getYearRate() {
		return yearRate;
	}

	public void setYearRate(String yearRate) {
		this.yearRate = yearRate;
	}

	public String getBorrTotAmt() {
		return borrTotAmt;
	}

	public void setBorrTotAmt(String borrTotAmt) {
		this.borrTotAmt = borrTotAmt;
	}

	public String getBorrCustId() {
		return borrCustId;
	}

	public void setBorrCustId(String borrCustId) {
		this.borrCustId = borrCustId;
	}

	public String getFreezeOrdId() {
		return freezeOrdId;
	}

	public void setFreezeOrdId(String freezeOrdId) {
		this.freezeOrdId = freezeOrdId;
	}

	public String getIsFreeze() {
		return isFreeze;
	}

	public void setIsFreeze(String isFreeze) {
		this.isFreeze = isFreeze;
	}

	public String getBorrowerDetails() {
		return borrowerDetails;
	}

	public void setBorrowerDetails(String borrowerDetails) {
		this.borrowerDetails = borrowerDetails;
	}

	public String getMaxTenderRate() {
		return maxTenderRate;
	}

	public void setMaxTenderRate(String maxTenderRate) {
		this.maxTenderRate = maxTenderRate;
	}

	public String getFreezeTrxId() {
		return freezeTrxId;
	}

	public void setFreezeTrxId(String freezeTrxId) {
		this.freezeTrxId = freezeTrxId;
	}

	public String getUnFreezeOrdId() {
		return unFreezeOrdId;
	}

	public void setUnFreezeOrdId(String unFreezeOrdId) {
		this.unFreezeOrdId = unFreezeOrdId;
	}

	public String getBeginDate() {
		return beginDate;
	}

	public void setBeginDate(String beginDate) {
		this.beginDate = beginDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public String getPageNum() {
		return pageNum;
	}

	public void setPageNum(String pageNum) {
		this.pageNum = pageNum;
	}

	public String getPageSize() {
		return pageSize;
	}

	public void setPageSize(String pageSize) {
		this.pageSize = pageSize;
	}

	public String getDivCustId() {
		return divCustId;
	}

	public void setDivCustId(String divCustId) {
		this.divCustId = divCustId;
	}

	public String getReqExt() {
		return reqExt;
	}

	public void setReqExt(String reqExt) {
		this.reqExt = reqExt;
	}

	public String getProId() {
		return proId;
	}

	public void setProId(String proId) {
		this.proId = proId;
	}

	public String getFeeObjFlag() {
		return feeObjFlag;
	}

	public void setFeeObjFlag(String feeObjFlag) {
		this.feeObjFlag = feeObjFlag;
	}

	public String getLoginPwd() {
		return loginPwd;
	}

	public void setLoginPwd(String loginPwd) {
		this.loginPwd = loginPwd;
	}

	public String getTransPwd() {
		return transPwd;
	}

	public void setTransPwd(String transPwd) {
		this.transPwd = transPwd;
	}

	public String getQueryTransType() {
		return queryTransType;
	}

	public void setQueryTransType(String queryTransType) {
		this.queryTransType = queryTransType;
	}

	public String getTrxId() {
		return trxId;
	}

	public void setTrxId(String trxId) {
		this.trxId = trxId;
	}

	public String getDivAmt() {
		return divAmt;
	}

	public void setDivAmt(String divAmt) {
		this.divAmt = divAmt;
	}

	public String getDivAcctId() {
		return divAcctId;
	}

	public void setDivAcctId(String divAcctId) {
		this.divAcctId = divAcctId;
	}

	public String getDivDetails() {
		return divDetails;
	}

	public void setDivDetails(String divDetails) {
		this.divDetails = divDetails;
	}

	public String getFee() {
		return fee;
	}

	public void setFee(String fee) {
		this.fee = fee;
	}

	public String getSubOrdDate() {
		return subOrdDate;
	}

	public void setSubOrdDate(String subOrdDate) {
		this.subOrdDate = subOrdDate;
	}

	public String getSubOrdId() {
		return subOrdId;
	}

	public void setSubOrdId(String subOrdId) {
		this.subOrdId = subOrdId;
	}

	public String getInAcctId() {
		return inAcctId;
	}

	public void setInAcctId(String inAcctId) {
		this.inAcctId = inAcctId;
	}

	public String getInCustId() {
		return inCustId;
	}

	public void setInCustId(String inCustId) {
		this.inCustId = inCustId;
	}

	public String getOutAcctId() {
		return outAcctId;
	}

	public void setOutAcctId(String outAcctId) {
		this.outAcctId = outAcctId;
	}

	public String getOutCustId() {
		return outCustId;
	}

	public void setOutCustId(String outCustId) {
		this.outCustId = outCustId;
	}

	public String getTransAmt() {
		return transAmt;
	}

	public void setTransAmt(String transAmt) {
		this.transAmt = transAmt;
	}

	public String getDcFlag() {
		return dcFlag;
	}

	public void setDcFlag(String dcFlag) {
		this.dcFlag = dcFlag;
	}

	public String getOpenBankId() {
		return openBankId;
	}

	public void setOpenBankId(String openBankId) {
		this.openBankId = openBankId;
	}

	public String getGateBusiId() {
		return gateBusiId;
	}

	public void setGateBusiId(String gateBusiId) {
		this.gateBusiId = gateBusiId;
	}

	public String getOrdDate() {
		return ordDate;
	}

	public void setOrdDate(String ordDate) {
		this.ordDate = ordDate;
	}

	public String getOrdId() {
		return ordId;
	}

	public void setOrdId(String ordId) {
		this.ordId = ordId;
	}

	public ChinaPnrReqModel() {
		super();
	}

	public String getUsrCustId() {
		return usrCustId;
	}

	public void setUsrCustId(String usrCustId) {
		this.usrCustId = usrCustId;
	}

	public ChinaPnrReqModel(String version, String cmdId, String merCustId,
			String bgRetUrl) {
		super();
		this.version = version;
		this.cmdId = cmdId;
		this.merCustId = merCustId;
		this.bgRetUrl = bgRetUrl;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version = version;
	}

	public String getCmdId() {
		return cmdId;
	}

	public void setCmdId(String cmdId) {
		this.cmdId = cmdId;
	}

	public String getMerCustId() {
		return merCustId;
	}

	public void setMerCustId(String merCustId) {
		this.merCustId = merCustId;
	}

	public String getBgRetUrl() {
		return bgRetUrl;
	}

	public void setBgRetUrl(String bgRetUrl) {
		this.bgRetUrl = bgRetUrl;
	}

	public String getRetUrl() {
		return retUrl;
	}

	public void setRetUrl(String retUrl) {
		this.retUrl = retUrl;
	}

	public String getUsrId() {
		return usrId;
	}

	public void setUsrId(String usrId) {
		this.usrId = usrId;
	}

	public String getUsrName() {
		return usrName;
	}

	public void setUsrName(String usrName) {
		this.usrName = usrName;
	}

	public String getIdType() {
		return idType;
	}

	public void setIdType(String idType) {
		this.idType = idType;
	}

	public String getIdNo() {
		return idNo;
	}

	public void setIdNo(String idNo) {
		this.idNo = idNo;
	}

	public String getUsrMp() {
		return usrMp;
	}

	public void setUsrMp(String usrMp) {
		this.usrMp = usrMp;
	}

	public String getUsrEmail() {
		return usrEmail;
	}

	public void setUsrEmail(String usrEmail) {
		this.usrEmail = usrEmail;
	}

	public String getMerPriv() {
		return merPriv;
	}

	public void setMerPriv(String merPriv) {
		this.merPriv = Codec.encodeBASE64(merPriv);
	}

	public String getCharSet() {
		return charSet;
	}

	public void setCharSet(String charSet) {
		this.charSet = charSet;
	}

	public String getChkValue() {
		return chkValue;
	}

	public void setChkValue(String chkValue) {
		this.chkValue = chkValue;
	}

}
