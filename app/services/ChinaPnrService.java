package services;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import play.Logger;
import play.libs.WS;
import play.libs.WS.HttpResponse;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import business.DealDetail;
import constants.IPSConstants;
import constants.ChinaPnrConstants;
import controllers.PNR.ChinaPnrPayment.Random;
import models.ChinaPnrReqModel;
import utils.DateUtil;

/**
 * 汇付天下Service类
 * @author yx
 *	@create 2014年12月8日 下午7:41:26
 */
public class ChinaPnrService extends ChinaPnrBaseService implements Serializable{
	
	/**
	 * 登录
	 * @param model
	 * @return
	 */
	public String doUserLogin(ChinaPnrReqModel model){
		putValue("Version", model.getVersion())
		.putValue("CmdId", model.getCmdId())
		.putValue("MerCustId",  model.getMerCustId())
		.putValue("UsrCustId", model.getUsrCustId());
		
		return http();
	}
	
	/**
	 * 查询余额
	 * @return
	 */
	public String doQueryBalanceBg(ChinaPnrReqModel model) {
		putValue("Version", model.getVersion())
		.putValue("CmdId", model.getCmdId())
		.putValue("MerCustId", model.getMerCustId())
		.putValue("UsrCustId", model.getUsrCustId());
		
		return http();
	}
	
	/**
	 * 网银充值
	 * @return
	 */
	public String doNetSave(ChinaPnrReqModel model) {
		putValue("Version", model.getVersion())
		.putValue("CmdId", model.getCmdId())
		.putValue("MerCustId", model.getMerCustId())
		.putValue("UsrCustId", model.getUsrCustId())
		.putValue("OrdId", model.getOrdId())
		.putValue("OrdDate",DateUtil.getDate())
		.putValue("TransAmt", model.getTransAmt())
		.putValue("BgRetUrl", model.getBgRetUrl());
		
		return http();
	}
	
	/**
	 * 用户开户
	 * @param model
	 * @return
	 */
	public String doUserRegister(ChinaPnrReqModel model){
		putValue("Version", model.getVersion())
		.putValue("CmdId", model.getCmdId())
		.putValue("MerCustId", model.getMerCustId())
		.putValue("BgRetUrl", model.getBgRetUrl());
		
		return http();
	}
	
	/**
	 * 资金（货款）解冻
	 * @param model
	 * @return
	 */
	public String doUsrUnFreeze(ChinaPnrReqModel model){
		putValue("Version", ChinaPnrConfig.getProperty("chinapnr_version"))
		.putValue("CmdId", ChinaPnrConstants.CMD_USRUNFREEZE)
		.putValue("MerCustId", ChinaPnrConfig.getProperty("chinapnr_merCustId"))
		.putValue("OrdId", model.getOrdId())
		.putValue("OrdDate", DateUtil.getDate())
		.putValue("TrxId", model.getTrxId())
		.putValue("BgRetUrl",ChinaPnrConfig.getProperty("chinapnr_retUrlBg"));
		
		return  http();
	}
	
	/**
	 * 自动扣款(还款)2.0
	 * @return
	 */
	public String doRepayment(ChinaPnrReqModel model){
		putValue("Version", ChinaPnrConfig.getProperty("chinapnr_version_2.0"))
		.putValue("CmdId", ChinaPnrConstants.CMD_REPAYMENT)
		.putValue("MerCustId", ChinaPnrConfig.getProperty("chinapnr_merCustId"))
		.putValue("OrdId", model.getOrdId())
		.putValue("OrdDate",model.getOrdDate())
		.putValue("OutCustId", model.getOutCustId())
		.putValue("SubOrdId", model.getSubOrdId())
		.putValue("SubOrdDate", DateUtil.getDate())
		.putValue("TransAmt", model.getTransAmt())
		.putValue("Fee", "0".equals(model.getFee())?"0.00":model.getFee())
		.putValue("InCustId", model.getInCustId())
		.putValue("BgRetUrl", ChinaPnrConfig.getProperty("chinapnr_retUrlBg"))
		.putValue("ReqExt", model.getReqExt());
		if(!"0.00".equals(get("Fee"))){
			//修改收取投资人手续费 O--> I modify 20141225
			putValue("FeeObjFlag", "I")
			.putValue("DivDetails", model.getDivDetails());
		} 
		
		return http();
	}
	
	/**
	 * 商户子账户信息查询
	 * @param model
	 * @return
	 */
	public String doQueryAccts(ChinaPnrReqModel model){
		putValue("Version", model.getVersion())
		.putValue("CmdId", model.getCmdId())
		.putValue("MerCustId", model.getMerCustId());
		
		return http();
	}
	
	/**
	 * 商户扣款对账
	 * @param model
	 * @return
	 */
	public String doTrfReconciliation(ChinaPnrReqModel model){
		putValue("Version", model.getVersion())
		.putValue("CmdId", model.getCmdId())
		.putValue("MerCustId", model.getMerCustId())
		.putValue("BeginDate", model.getBeginDate())
		.putValue("EndDate", model.getEndDate())
		.putValue("PageNum", model.getPageNum())
		.putValue("PageSize", model.getPageSize());
		
		return http();
	}
	
	/**
	 *  放还款对账
	 * @return
	 */
	public String doReconciliation(ChinaPnrReqModel model){
		putValue("Version",  ChinaPnrConfig.getProperty("chinapnr_version"))
		.putValue("CmdId", ChinaPnrConstants.CMD_RECONCILIATION)
		.putValue("MerCustId", ChinaPnrConfig.getProperty("chinapnr_merCustId"))
		.putValue("BeginDate", model.getBeginDate())
		.putValue("EndDate", model.getEndDate())
		.putValue("PageNum", model.getPageNum())
		.putValue("PageSize", model.getPageSize())
		.putValue("QueryTransType", model.getQueryTransType());
		
		return http();
	}
	
	/**
	 * 取现对账
	 */
	public String doCashReconciliation(ChinaPnrReqModel model){
		putValue("Version", model.getVersion())
		.putValue("CmdId", model.getCmdId())
		.putValue("MerCustId", model.getMerCustId())
		.putValue("BeginDate", model.getBeginDate())
		.putValue("EndDate", model.getEndDate())
		.putValue("PageNum", model.getPageNum())
		.putValue("PageSize", model.getPageSize());
		
		return http();
	}
	
	/**
	 * 充值对账
	 * @return
	 */
	public String doSaveReconciliation(ChinaPnrReqModel model){
		putValue("Version", model.getVersion())
		.putValue("CmdId", model.getCmdId())
		.putValue("MerCustId", model.getMerCustId())
		.putValue("BeginDate", model.getBeginDate())
		.putValue("EndDate", model.getEndDate())
		.putValue("PageNum", model.getPageNum())
		.putValue("PageSize", model.getPageSize());
		
		return http();
	}
	
	/**
	 * 交易状态查询
	 * @param model
	 * @return
	 */
	public String doQueryTransStat(ChinaPnrReqModel model){
		putValue("Version", ChinaPnrConfig.getProperty("chinapnr_version"))
		.putValue("CmdId", ChinaPnrConstants.CMD_QUERYTRANSSTAT)
		.putValue("MerCustId", ChinaPnrConfig.getProperty("chinapnr_merCustId"))
		.putValue("OrdId", model.getOrdId())
		.putValue("OrdDate", model.getOrdDate())
		.putValue("QueryTransType", model.getQueryTransType());
		
		return http();
	}
	
	/**
	 * 自动扣款(放款)2.0
	 * @param model
	 * @return
	 */
	public String doLoans(ChinaPnrReqModel model){
		putValue("Version", ChinaPnrConfig.getProperty("chinapnr_version_2.0"))
		.putValue("CmdId", ChinaPnrConstants.CMD_LOANS)
		.putValue("MerCustId",ChinaPnrConfig.getProperty("chinapnr_merCustId"))
		.putValue("OrdId", model.getOrdId())
		.putValue("OrdDate", model.getOrdDate())
		.putValue("OutCustId", model.getOutCustId())
		.putValue("TransAmt", model.getTransAmt())
		.putValue("Fee",  "0".equals(model.getFee())?"0.00":model.getFee())
		.putValue("SubOrdId", model.getSubOrdId())
		.putValue("SubOrdDate", model.getSubOrdDate())
		.putValue("InCustId", model.getInCustId())
		.putValue("IsDefault", "N")
		.putValue("IsUnFreeze", "Y")
		.putValue("UnFreezeOrdId", System.currentTimeMillis()+"")
		.putValue("FreezeTrxId",model.getFreezeTrxId())
		.putValue("BgRetUrl", ChinaPnrConfig.getProperty("chinapnr_retUrlBg"))
		.putValue("ReqExt", model.getReqExt());
		if(!"0.00".equals(get("Fee"))){
			putValue("FeeObjFlag", "I")
			.putValue("DivDetails", model.getDivDetails());
			
		} 
		return http();
	}
	
	/**
	 * 主动投标、自动投标
	 * @return
	 */
	public String doTender(ChinaPnrReqModel model){
		putValue("Version", ChinaPnrConfig.getProperty("chinapnr_version_2.0"))
		.putValue("CmdId",model.getCmdId())
		.putValue("MerCustId",ChinaPnrConfig.getProperty("chinapnr_merCustId"))
		.putValue("OrdId", model.getOrdId())
		.putValue("OrdDate", model.getOrdDate())
		.putValue("TransAmt", model.getTransAmt())
		.putValue("UsrCustId", model.getUsrCustId())
		.putValue("MaxTenderRate", model.getMaxTenderRate())
		.putValue("BorrowerDetails", model.getBorrowerDetails())
		.putValue("IsFreeze", model.getIsFreeze())
		.putValue("FreezeOrdId", model.getFreezeOrdId())
		.putValue("BgRetUrl", ChinaPnrConfig.getProperty("chinapnr_retUrlBg"));
		
		return http();
	}
	
	/**
	 * 标的信息录入
	 * @param model
	 * @return
	 */
	public String doAddBidInfo(ChinaPnrReqModel model){
		putValue("Version", ChinaPnrConfig.getProperty("chinapnr_version"))
		.putValue("CmdId",ChinaPnrConstants.CMD_ADDBIDINFO)
		.putValue("MerCustId",ChinaPnrConfig.getProperty("chinapnr_merCustId"))
		.putValue("ProId", model.getProId())
		.putValue("BorrCustId", model.getBorrCustId())
		.putValue("BorrTotAmt", model.getBorrTotAmt())
		.putValue("YearRate", model.getYearRate())
		.putValue("RetType", model.getRetType())
		.putValue("BidStartDate", model.getBidStartDate())
		.putValue("BidEndDate", model.getBidEndDate())
		.putValue("RetAmt", model.getRetAmt())
		.putValue("RetDate", model.getRetDate())
		.putValue("ProArea", "1100")
		.putValue("BgRetUrl",  ChinaPnrConfig.getProperty("chinapnr_retUrlBg"))
		.putValue("ReqExt", model.getReqExt());
		if(!"0.00".equals(model.getGuarAmt())){
			putValue("GuarAmt", model.getGuarAmt());
		}
		return http();
	}
	 
	/**
	 * 银行卡查询接口
	 * @param model
	 * @return
	 */
	public String doQueryCardInfo(ChinaPnrReqModel model){
		putValue("Version", ChinaPnrConfig.getProperty("chinapnr_version"))
		.putValue("CmdId", ChinaPnrConstants.CMD_QUERYCARDINFO)
		.putValue("MerCustId", ChinaPnrConfig.getProperty("chinapnr_merCustId"))
		.putValue("UsrCustId", model.getUsrCustId());
		return http();
	}
	
	/**
	 * 自动投标
	 * @return
	 */
	public String doAutoTender(ChinaPnrReqModel model){
		putValue("Version", ChinaPnrConfig.getProperty("chinapnr_version_2.0")).
		putValue("CmdId", ChinaPnrConstants.CMD_AUTOTENDER)
		.putValue("MerCustId", ChinaPnrConfig.getProperty("chinapnr_merCustId") )
		.putValue("OrdId", model.getOrdId())
		.putValue("OrdDate", DateUtil.getDate())
		.putValue("TransAmt", model.getTransAmt())
		.putValue("UsrCustId", model.getUsrCustId())
		.putValue("MaxTenderRate", model.getMaxTenderRate())
		.putValue("BorrowerDetails", model.getBorrowerDetails())
		.putValue("BgRetUrl", ChinaPnrConfig.getProperty("chinapnr_retUrlBg")) 
		.putValue("IsFreeze", "Y")
		.putValue("FreezeOrdId",  model.getFreezeOrdId());
		
		return http();
	}
	
	/**
	 * 转账（商户用）
	 * @return
	 */
	public String doTransfer(ChinaPnrReqModel model){
		putValue("Version", ChinaPnrConfig.getProperty("chinapnr_version"))
		.putValue("CmdId", ChinaPnrConstants.CMD_TRANSFER)
		.putValue("OrdId", model.getOrdId())
		.putValue("OutCustId", ChinaPnrConfig.getProperty("chinapnr_merCustId"))
		.putValue("OutAcctId", ChinaPnrConfig.getProperty("chinapnr_cfb"))
		.putValue("TransAmt", model.getTransAmt())
		.putValue("InCustId", model.getInCustId())
		.putValue("BgRetUrl", ChinaPnrConfig.getProperty("chinapnr_retUrlBg"));
		
		return http();
	}
	
	/**
	 * 交易明细查询
	 */
	public String doQueryTransDetail(ChinaPnrReqModel model){
		putValue("Version", ChinaPnrConfig.getProperty("chinapnr_version"))
		.putValue("CmdId", ChinaPnrConstants.CMD_QUERYTRANSDETAIL)
		.putValue("MerCustId", ChinaPnrConfig.getProperty("chinapnr_merCustId") )
		.putValue("OrdId", model.getOrdId())
		.putValue("QueryTransType", model.getQueryTransType());
		
		return http();
	}
	
	/**
	 * 用户信息查询
	 * @param model
	 * @return
	 */
	public String doQueryUsrInfo(ChinaPnrReqModel model ){
		putValue("Version", ChinaPnrConfig.getProperty("chinapnr_version"))
		.putValue("CmdId", ChinaPnrConstants.CMD_QUERYUSRINFO)
		.putValue("MerCustId", ChinaPnrConfig.getProperty("chinapnr_merCustId") )
		.putValue("CertId", model.getCertId());
		
		return http();
	}
	
	/**
	 * 查询交易状态
	 */
	@Test
	public void testdoQueryTransStat(){
		
		ChinaPnrReqModel model = new ChinaPnrReqModel();
		ChinaPnrService service =new ChinaPnrService();
		model.setVersion("10");
/*		String[] ordId = {"681131422588179577",
				"681131422588284488",
				"681131422588371965",
				"681131422588392246",
				"681131422588424636",
				"681131422588483667",
				"681131422588897026",
				"681131422589223747",
				"681131422589645199",
				"681131422590671059",
				"681131422591368596",
				"681131422591603710",
				"681131422591730478",
				"681131422592122376",
				"681131422593999169",
				"681131422594054444",
				"681131422594067133"};
		for(int i=0; i<ordId.length; i++) {
			model.setOrdId(ordId[i]); //取现回调的订单号
		}*/             
		model.setOrdId("6000060018816347"); 
		model.setOrdDate("20150130"); //时间
		model.setQueryTransType("CASH");  //取现  CASH
		//model.setQueryTransType("TENDER");  //投标  TENDER
		//model.setQueryTransType("CASH");  //放款  LOANS
		//model.setQueryTransType("CASH");  //还款  REPAYMENT
		//model.setQueryTransType("CASH");  //取现  FREEZE
		service.doQueryTransStat(model);
	}
	
	// 解冻
	@Test
	public void testdoUsrUnFreeze() {
		ChinaPnrReqModel model = new ChinaPnrReqModel();
		ChinaPnrService service = new ChinaPnrService();
		model.setOrdId(System.currentTimeMillis() + "");
		model.setTrxId("201501060005654902");
		service.doUsrUnFreeze(model);
	}
	
}
