package controllers.loan;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import models.t_transfer_batches;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;

import org.apache.commons.lang.StringUtils;

import play.Logger;
import play.libs.Codec;
import play.libs.WS;
import play.libs.WS.WSRequest;
import services.LOAN;
import utils.Converter;
import utils.ErrorInfo;
import utils.loan.LoanUtil;
import utils.loan.RsaHelper;
import business.Bid;
import business.DealDetail;
import business.Member;
import business.TransferBatches;

import com.shove.Convert;
import com.shove.security.Encrypt;

import constants.Constants;
import constants.LoanConstants;
import controllers.Application;
import controllers.BaseController;

/**
 * 资金托管-双乾，核心控制器
 *
 * @author hys
 * @createDate  2015年1月5日 下午3:40:47
 *
 */
public class LoanController extends BaseController {

	//单例
	private static LoanController instance = null;
	
	public static LoanController getInstance(){
		if(instance == null){
            synchronized(LoanController.class){
                if(instance == null){
                    instance = new LoanController();
                }
            }
		}
		return instance;
	}
	
	/**
	 * 分发请求第三方（双乾）接口的业务方法
	 * @param type
	 * @param platformMemberId
	 * @param memberName
	 * @param platformId
	 * @param domain
	 * @param argMerCode
	 * @param arg3DesXmlPara
	 * @param argeXtraPara
	 * @param argIpsAccount
	 */
	public void loan(int type, long platformMemberId, String memberName,
			long platformId, String domain, String argMerCode,
			String arg3DesXmlPara,String argeXtraPara, String argIpsAccount) {
		ErrorInfo error = new ErrorInfo();				
		
		if(memberName == null){
			memberName = "";
		}
		
//		/* 接口类型参数限定在1~13 */
//		if (type <= 0 || type > 18) {
//			flash.error("没有此接口类型：type=%s" + type);
//			Application.error();
//		}

		/* 余额查询接口，还原arg3DesXmlPara参数*/
		if (type == LoanConstants.ACCOUNT_BALANCE) {
			arg3DesXmlPara = "";
		}

		/* 提现，先判断是否绑定了提现银行卡号 */
		String CardNo = "";
		if (type == LoanConstants.WITHDRAWAL) {
			CardNo = Member.findCardNo(error, platformMemberId, platformId);
			CardNo = Encrypt.decrypt3DES(CardNo, Constants.ENCRYPTION_KEY);

			if (StringUtils.isBlank(CardNo)) { // 转向提现绑卡接口
				type = LoanConstants.BOND_CRAD_NO;
				
				Logger.info("======请求中间件%s接口，执行开始========", type);
			}
		}

		JSONObject jsonArg3DesXmlPara = new JSONObject();
		if (!StringUtils.isBlank(arg3DesXmlPara)) {
			arg3DesXmlPara = Encrypt.decrypt3DES(arg3DesXmlPara,Constants.ENCRYPTION_KEY); // 参数解密
			try {
				jsonArg3DesXmlPara = (JSONObject) Converter.xmlToObj(arg3DesXmlPara);
			} catch (Exception e) {
				Logger.info("解析arg3DesXmlPara时：%s", e.getMessage());
			}
		}

		JSONObject jsonArgeXtraPara = new JSONObject();
		if (!StringUtils.isBlank(argeXtraPara)) {
			argeXtraPara = Encrypt.decrypt3DES(argeXtraPara,Constants.ENCRYPTION_KEY); // 参数解密
			
			if(!Converter.xmlToObj(argeXtraPara).getClass().isAssignableFrom(JSONNull.class)){
				try {
					jsonArgeXtraPara = (JSONObject) Converter.xmlToObj(argeXtraPara);
				} catch (Exception e) {
					Logger.info("解析argeXtraPara时：%s", e.getMessage());
				}
			}
		}

		/* 标的登记接口，必传参数校验 */
		if (type == 2 && !jsonArg3DesXmlPara.containsKey("pOperationType")) {
			Logger.info("校验参数时：%s", "参数pOperationType为必传字段");
			flash.error("参数pOperationType为必传字段");
			Application.error();
		}
		
		/* 转账接口，必传参数校验 */
		if (type == 9 && !jsonArg3DesXmlPara.containsKey("pTransferType")) {
			Logger.info("校验参数时：%s", "参数pTransferType为必传字段");
			flash.error("参数pTransferType为必传字段");
			Application.error();
		}

		/* 判断用户开户情况 */
		if (type == LoanConstants.CREATE_ACCOUNT) {
			Member.checkAccuntInfo(error, platformMemberId, memberName,platformId, domain, jsonArg3DesXmlPara);

			if (error.code < 0) {
				flash.error(error.msg);
				Application.error();
			}
		}

		boolean isPost = true;  //接口请求方式，post表单提交，WebService请求，默认post
		
		/* 将请求参数转换成双乾接口参数 */
		Map<String, Object> args = null;
		switch (type) {
		case LoanConstants.CREATE_ACCOUNT:  //1、开户
			args = LOAN.convertCreateAccountParams(error, platformId,
					platformMemberId, memberName, argMerCode,
					jsonArg3DesXmlPara, jsonArgeXtraPara); // 双乾开户接口
			break;
		case LoanConstants.REGISTER_SUBJECT: // 2、标的登记，冻结保证金
			if (LoanConstants.ADD_REGISTER_SUBJECT.equals(jsonArg3DesXmlPara.get("pOperationType"))) {
				args = LOAN.convertAddRegisterSubjectParams(error, platformId,
						platformMemberId, memberName, argMerCode,
						jsonArg3DesXmlPara, jsonArgeXtraPara); // 新增：双乾转账接口
			}
			if (LoanConstants.END_REGISTER_SUBJECT.equals(jsonArg3DesXmlPara.get("pOperationType"))) {
				args = LOAN.convertEndRegisterSubjectParams(error, platformId,
						platformMemberId, memberName, argMerCode,
						jsonArg3DesXmlPara, jsonArgeXtraPara); // 结束：双乾审核接口
			}
			break;
		case LoanConstants.REGISTER_CREDITOR: // 3、登记债权人，冻结投资金额
			isPost = "1".equals(jsonArg3DesXmlPara.getString("pRegType"));  //pRegType:1手动投标post，2自动投标ws
			args = LOAN.convertRegisterCreditorParams(error, platformId,
					platformMemberId, memberName, argMerCode,
					jsonArg3DesXmlPara, jsonArgeXtraPara); // 双乾转账接口
			break;
		case LoanConstants.REGISTER_CRETANSFER: // 4、登记债权转让，直接转账
			args = LOAN.convertRegisterCretansferParams(error, platformId,
					platformMemberId, memberName, argMerCode,jsonArg3DesXmlPara, jsonArgeXtraPara); // 双乾转账接口
			break;
		case LoanConstants.AUTO_SIGING: // 5、自动投标签约
			args = LOAN.convertSignParams(error, LoanConstants.AUTO_BID,
					platformId, platformMemberId, memberName, argMerCode,
					jsonArg3DesXmlPara, jsonArgeXtraPara); // 双乾授权接口
			break;
		case LoanConstants.REPAYMENT_SIGNING: // 6、自动还款签约
			args = LOAN.convertSignParams(error, LoanConstants.AUTO_PAY,
					platformId, platformMemberId, memberName, argMerCode,
					jsonArg3DesXmlPara, jsonArgeXtraPara); // 双乾授权接口
			break;
		case LoanConstants.RECHARGE: // 8、充值
			args = LOAN.convertRechargeParams(error, platformId,
					platformMemberId, memberName, argMerCode,
					jsonArg3DesXmlPara, jsonArgeXtraPara); // 双乾充值接口
			break;
		case LoanConstants.TRANSFER: // 9、转账
			if (LoanConstants.INVEST.equals(jsonArg3DesXmlPara.get("pTransferType"))) { // 转账：投资
				args = LOAN.convertTransferParams(error, platformId,
						platformMemberId, memberName, argMerCode,
						jsonArg3DesXmlPara, jsonArgeXtraPara); // 双乾审核接口
			}
			if (LoanConstants.COMPENSATE.equals(jsonArg3DesXmlPara.get("pTransferType"))) { // 转账：代偿
				args = LOAN.convertCompensateParams(error, platformId,
						platformMemberId, memberName, argMerCode,
						jsonArg3DesXmlPara, jsonArgeXtraPara); // 双乾审核接口
			}
			if (LoanConstants.COMPENSATEREPAYMENT.equals(jsonArg3DesXmlPara.get("pTransferType"))) { // 转账：代偿还款
				args = LOAN.convertCompensateRepaymentParams(error, platformId,
						platformMemberId, memberName, argMerCode,
						jsonArg3DesXmlPara, jsonArgeXtraPara); // 双乾审核接口
			}
			if (LoanConstants.CREDITOR_TANSFER.equals(jsonArg3DesXmlPara.get("pTransferType"))) { // 转账：债权转让
				flash.error("乾多多没有提供此接口类型：type = " + type + ",pTransferType = " + jsonArg3DesXmlPara.get("pTransferType"));
				Application.error();
			}
			break;
		case LoanConstants.REPAYMENT: // 10、还款
			isPost = "1".equals(jsonArg3DesXmlPara.getString("pRepayType"));  //pRepayType:1手动post，2自动ws
			args = LOAN.convertRepaymentParams(error, platformId,
					platformMemberId, memberName, argMerCode,
					jsonArg3DesXmlPara, jsonArgeXtraPara); // 双乾转账接口
			break;
		case LoanConstants.GUARANTEE_UNFREEZE: // 11、解冻保证金
			args = LOAN.convertGuaranteeUnfreezeParams(error, platformId,
					platformMemberId, memberName, argMerCode,
					jsonArg3DesXmlPara, jsonArgeXtraPara,argIpsAccount); // 双乾提现接口
			break;
		case LoanConstants.WITHDRAWAL: // 13、提现
			args = LOAN.convertWithdrawParams(error, platformId, platformMemberId,
							memberName, argMerCode, jsonArg3DesXmlPara,
							jsonArgeXtraPara, CardNo, argIpsAccount); // 双乾提现接口
			break;
		case LoanConstants.ACCOUNT_BALANCE: // 14、余额
			isPost = false;
			args = LOAN.convertAccountBalanceParams(error, platformMemberId,
					platformId, argMerCode, argIpsAccount); // 双乾余额查询接口
			break;
		case LoanConstants.BANK_LIST: // 15、商户端获取银行列表
			flash.error("乾多多没有提供此接口类型：type = " + type);
			Application.error();
			break;
		case LoanConstants.USER_INFO: // 16、账户信息查询
			flash.error("乾多多没有提供此接口类型：type = " + type);
			Application.error();
			break;
		case LoanConstants.QUERY_TRADE: // 17、交易查询
			isPost = false;
			args = LOAN.convertOrderQueryParams(error,
					LoanConstants.AUTH_SECOND, platformId, platformMemberId,
					memberName, argMerCode, jsonArg3DesXmlPara,jsonArgeXtraPara, argIpsAccount); // 双乾对账接口
			break;
		case LoanConstants.AUTHORIZE_SECONDARY: // 18、二次分配审核授权
			args = LOAN.convertAuthorizeSecondaryParams(error,
					LoanConstants.AUTH_SECOND, platformId, platformMemberId,
					memberName, argMerCode, jsonArg3DesXmlPara,jsonArgeXtraPara, argIpsAccount); // 双乾授权接口
			break;
		case LoanConstants.BOND_CRAD_NO: // 19、提现银行卡号绑定
			args = LOAN.convertBondCardNoParams(error, platformId,
					platformMemberId, memberName, argMerCode,
					jsonArg3DesXmlPara, jsonArgeXtraPara); // 双乾三口合一（提现绑卡）接口
			break;
		case LoanConstants.TRANSFER_USER_TO_MER: // 32、用户转商户
			args = LOAN.convertTransferUserToMerParams(error, platformId,
					platformMemberId, memberName, argMerCode,
					jsonArg3DesXmlPara, jsonArgeXtraPara); // 双乾转账接口
			break;
		case LoanConstants.TRANSFER_MER_TO_USER: // 33、商户转用户
			args = LOAN.convertTransferMerToUserParams(error, platformId,
					platformMemberId, memberName, argMerCode,
					jsonArg3DesXmlPara, jsonArgeXtraPara); // 双乾转账接口
			break;
		case LoanConstants.UNFREEZE_INVEST_AMOUNT: // 35、解冻投资金额
			args = LOAN.convertUnfreezeInvestAmountParams(error, platformId,
					platformMemberId, memberName, argMerCode,
					jsonArg3DesXmlPara, jsonArgeXtraPara); // 双乾转账接口
			break;
		default:
			break;
		}
		
		/* 直接返回p2p  */
		if(error.code < 0){
			if(isPost){
				returnP2PWithoutLoan(error, type, platformId, platformMemberId, memberName, argMerCode, jsonArg3DesXmlPara, jsonArgeXtraPara);
			}else{
				responseP2PWithoutLoan(error, type, platformMemberId, argMerCode, argIpsAccount, jsonArg3DesXmlPara, args);
			}
		}

		/* WS请求时，请求第三方接口，并转化参数 */
		String jsonResult = "";
		switch (type) {
		case LoanConstants.ACCOUNT_BALANCE:  //余额查询
			jsonResult = responeBalanceQuery(argMerCode, argIpsAccount,args);
			break;
		case LoanConstants.REPAYMENT:  //自动还款
			if("2".equals(jsonArg3DesXmlPara.getString("pRepayType"))){
				jsonResult = responseAutoRepayment(argMerCode, platformMemberId,args);
			}
			break;
		case LoanConstants.REGISTER_CREDITOR:  //自动投标
			if("2".equals(jsonArg3DesXmlPara.getString("pRegType"))){
				jsonResult = responseAutoRegisterCreditor(argMerCode, argIpsAccount,args);
			}
			break;
		case LoanConstants.QUERY_TRADE:  //交易查询
			jsonResult = args.isEmpty()?"":LoanUtil.toJson(args);
			break;
		default:
			break;
		}
		
		Logger.info("======请求中间件%s接口，执行结束========", type);
		
		if(isPost){
			/* 表单post请求第三方接口  */
			render("loan/LoanController/loan.html", args);
		}else{
			/* 响应p2p  */
			renderText(jsonResult);
		}

	}
	
	/**
	 * 响应p2p（ws）
	 * @param error
	 * @param type
	 * @param platformMemberId
	 * @param argMerCode
	 * @param argIpsAccount
	 * @param jsonArg3DesXmlPara
	 * @param args
	 */
	private void responseP2PWithoutLoan(ErrorInfo error, int type, long platformMemberId, String argMerCode, String argIpsAccount, JSONObject jsonArg3DesXmlPara, Map<String, Object> args) {
		String pMerCode = argMerCode;
		String pErrCode = ""; 
		String pErrMsg = "";   
		String p3DesXmlPara = "";
		String pSign = "";
		
		if(error.code > LoanConstants.ERROR){  //异常
			pErrCode = "MG00001F"; 
			pErrMsg = error.msg;   
		}
		
		if(error.code == LoanConstants.TRANSFER_REPAIR_SUCCESS && type == LoanConstants.REPAYMENT){  //已还款成功，直接返回
			pErrCode = "MG00000F"; 
			pErrMsg = "秒还成功";   
			LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
			result.put("pMerBillNo", jsonArg3DesXmlPara.getString("pMerBillNo"));
			result.put("pMemo3", jsonArg3DesXmlPara.getString("pMemo3")); 
			p3DesXmlPara = LoanUtil.parseMapToXml(result);
			p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);
		}
		
		pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pMerCode", pMerCode);
		jsonObj.put("pErrCode", pErrCode);
		jsonObj.put("pErrMsg", pErrMsg);
		jsonObj.put("p3DesXmlPara", p3DesXmlPara);
		jsonObj.put("pSign", pSign);
		renderText(jsonObj.toString());
	}

	/**
	 * 同步回调p2p(post),未经过第三方处理
	 * @param error
	 * @param type
	 * @param platformId
	 * @param platformMemberId
	 * @param memberName
	 * @param argMerCode
	 * @param jsonArg3DesXmlPara
	 * @param jsonArgeXtraPara
	 */
	private void returnP2PWithoutLoan(ErrorInfo error, int type, long platformId,
			long platformMemberId, String memberName, String argMerCode, JSONObject jsonArg3DesXmlPara, JSONObject jsonArgeXtraPara) {
		
		if(error.code > LoanConstants.ERROR){  //参数、程序异常
			flash.error(error.msg);
			Application.error();
		}
	
		switch (type) {
		case LoanConstants.TRANSFER :  //转账
			if (LoanConstants.INVEST.equals(jsonArg3DesXmlPara.get("pTransferType"))) { // 转账：投资
				handlerTransferException(error, platformMemberId, platformId, memberName, argMerCode, jsonArg3DesXmlPara, jsonArgeXtraPara);
			}
			if (LoanConstants.COMPENSATE.equals(jsonArg3DesXmlPara.get("pTransferType"))) { // 转账：代偿
				handlerCompensateException(error, platformMemberId, platformId, memberName, argMerCode, jsonArg3DesXmlPara, jsonArgeXtraPara);
			}
			break;
		case LoanConstants.REPAYMENT :  //手动还款
			handlerRepaymentException(error, platformMemberId, platformId, memberName, argMerCode, jsonArg3DesXmlPara, jsonArgeXtraPara);
			break;
		case LoanConstants.GUARANTEE_UNFREEZE :  //已解冻保证金，直接回调P2P
			transferReturnWithoutLoan(error, "MG00000F", "成功", platformMemberId,jsonArg3DesXmlPara,jsonArgeXtraPara);
			break;
		case LoanConstants.UNFREEZE_INVEST_AMOUNT :  //已解冻投资金额，直接回调P2P
			unfreezeInvestAmountReturnWithoutLoan(error, platformId,platformMemberId, memberName, argMerCode,jsonArg3DesXmlPara, jsonArgeXtraPara);
			break;
		default:
			break;
		}
	}

	/**
	 * 还款异常，直接回调p2p
	 * @param error
	 * @param jsonArg3DesXmlPara
	 * @param jsonArgeXtraPara
	 */
	private void handlerRepaymentException(ErrorInfo error, long platformMemberId, long platformId, String memberName, String argMerCode, JSONObject jsonArg3DesXmlPara, JSONObject jsonArgeXtraPara) {
		boolean flag = true;  //是否转账成功
		String merBillNo = jsonArg3DesXmlPara.getString("pMerBillNo");
		
		if(error.code == LoanConstants.TRANSFER_ERROR){  //转账异常
			flag = false; 
		}
		
		if(error.code == LoanConstants.TRANSFER_EXECUTED){  //转账已全部执行
			String bidNo = jsonArg3DesXmlPara.getString("pBidNo");
			String period = jsonArgeXtraPara.getString("period");
			//分批操作：bidBillNo为每一批的唯一标识，生成法则; 标的号  + 分隔符  + 还款期号
			String bidBillNo = bidNo + LoanConstants.BILLTOKEN + period;
			
			List<TransferBatches> list =  TransferBatches.queryByBidBillNo(bidBillNo);
			if(list != null){  //分批转账时
				//补单
				for(TransferBatches tb : list){
					if(tb.status == 1){  //掉单，补单
						//每一笔转账流水号生成法则：① 分批转账：还款流水号  + 分隔符 + 还款期号  + 批号 + 循环i；②一次转账：还款流水号  + 分隔符 + 循环i
						String OrderNo = merBillNo+ LoanConstants.BILLTOKEN + period + tb.batchNo + 0;
						String response = LOAN.loanOrderQuery("", "",OrderNo)[1];
						if (StringUtils.isNotBlank(response)&& (response.startsWith("[") || response.startsWith("{"))) {
							Map<String, String> resultFromLoan = LOAN.getLoanMap(response);
							
							String actState = resultFromLoan.get("ActState").toString(); // 0.未操作,1.已通过,2.已退回,3.自动通过
							if("0".equals(actState)){  //第三方失败或未处理
								flag = false;
								tb.status = 0;
								TransferBatches.updateStatus(tb.id,0,error);  //0.未处理，1.处理中，2.处理成功，3.处理失败
								
								jsonArg3DesXmlPara = JSONObject.fromObject(tb.transferBillNos);
								jsonArg3DesXmlPara.put("batchId", tb.id);
								jsonArg3DesXmlPara.put("batchNo", tb.batchNo);
							}else{
								tb.status = 2;
								TransferBatches.updateStatus(tb.id,2,error);  //0.未处理，1.处理中，2.处理成功，3.处理失败
							}
						}else{  //第三方无记录，重新转账
							flag = false;
							tb.status = 0;
							TransferBatches.updateStatus(tb.id,0,error);  //0.未处理，1.处理中，2.处理成功，3.处理失败
							
							jsonArg3DesXmlPara = JSONObject.fromObject(tb.transferBillNos);
							jsonArg3DesXmlPara.put("batchId", tb.id);
							jsonArg3DesXmlPara.put("batchNo", tb.batchNo);
						}
					}
				}

				for(TransferBatches tb : list){
					if(tb.status == 3){  //转账失败，重新转账
						TransferBatches.updateStatus(tb.id,0,error);  //重新转账
						flag = false;
					}
				}
			}
		}
		
//		String pWebUrl = jsonArg3DesXmlPara.getString("pWebUrl");
		String pWSUrl = jsonArgeXtraPara.getString("pWSUrl");
		String pMerCode = LoanConstants.argMerCode;
		String pErrCode = "";
		String pErrMsg = "";
		if(flag){
			pErrCode = "MG00000F";
			pErrMsg = "还款成功";
		}else{	
			pErrCode = "MG00001F";
			pErrMsg = "转账异常，请稍后再试";
			
			Map<String, Object> args = LOAN.convertRepaymentParams(error, platformId, platformMemberId, memberName, argMerCode, jsonArg3DesXmlPara, jsonArgeXtraPara);
			render("loan/LoanController/loan.html", args);
		}
		
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("pMerBillNo", merBillNo);
		result.put("pMemo3", jsonArg3DesXmlPara.getString("pMemo3")); 
		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);
		
		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);
		
		Map<String, Object> args = handlerService(pWSUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);
		
		render("loan/LoanController/loanReturnAfterWS.html", args);
	}

	/**
	 * 放款时，直接执行解冻或回调p2p
	 * @param error
	 * @param platformMemberId
	 * @param platformId
	 * @param argMerCode
	 * @param jsonArg3DesXmlPara
	 * @param jsonArgeXtraPara
	 */
	private void handlerTransferException(ErrorInfo error, long platformMemberId, long platformId, String memberName, String argMerCode, JSONObject jsonArg3DesXmlPara, JSONObject jsonArgeXtraPara) {
		boolean flag = true;  //是否转账成功
		
		if(error.code == LoanConstants.TRANSFER_ERROR){  //操作异常
			flag = false;
		}
		
		if(error.code == LoanConstants.TRANSFER_REPAIR_SUCCESS){  //操作成功
			flag = true;
		}
		
		if(error.code == LoanConstants.TRANSFER_EXECUTED){  //操作完成
			List<TransferBatches> list =  TransferBatches.queryByBidBillNo(jsonArgeXtraPara.getString("pBidBillNo"));
			if(list != null){  //分批转账时
				//补单
				for(TransferBatches tb : list){
					if(tb.status == 1){  //掉单，补单
						String LoanNo = tb.transferBillNos.split(",")[0].trim();
						String response = LOAN.loanOrderQuery("", LoanNo,"")[1];
						if (StringUtils.isNotBlank(response)&& (response.startsWith("[") || response.startsWith("{"))) {
							Map<String, String> resultFromLoan = LOAN.getLoanMap(response);
							
							String actState = resultFromLoan.get("ActState").toString(); // 0.未操作,1.已通过,2.已退回,3.自动通过
							if("0".equals(actState)){  //第三方已转账失败
								flag = false;
								tb.status = 0;  //修改状态为未处理，下次重新转账
								TransferBatches.updateStatus(tb.id,0,error);  //0.未处理，1.处理中，2.处理成功，3.处理失败
								
								//重新转账做准备
								jsonArgeXtraPara.put("batchId", tb.id);
								jsonArgeXtraPara.put("pBillNos", tb.transferBillNos);
							}else{
								tb.status = 2;
								TransferBatches.updateStatus(tb.id,2,error);  //0.未处理，1.处理中，2.处理成功，3.处理失败
							}
						}else{  //第三方无记录，重新转账
							flag = false;
							tb.status = 0;
							TransferBatches.updateStatus(tb.id,0,error);  //0.未处理，1.处理中，2.处理成功，3.处理失败
							
							//重新转账做准备
							jsonArgeXtraPara.put("batchId", tb.id);
							jsonArgeXtraPara.put("pBillNos", tb.transferBillNos);
						}
					}
				}
				
				//转账异常
				for(TransferBatches tb : list){
					if(tb.status == 3){  //转账失败，重新转账
						TransferBatches.updateStatus(tb.id,0,error);  //将状态改为未处理，重新转账
						flag = false;
					}
				}
			}
		}
		
		if(!flag){
//			transferReturnWithoutLoan(error, "MG00001F", "转账异常，请重试",platformMemberId,jsonArg3DesXmlPara,jsonArgeXtraPara);
			
			//重新转账
			Map<String, Object> args = LOAN.convertTransferParams(error, platformId, platformMemberId, memberName, argMerCode, jsonArg3DesXmlPara, jsonArgeXtraPara);
			render("loan/LoanController/loan.html", args);
		}else{  //放款成功，解冻
			String arg3DesXmlPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonArg3DesXmlPara.toString(), "pReq", null, null, null),Constants.ENCRYPTION_KEY); // 参数加密
			String argeXtraPara = Encrypt.encrypt3DES(Converter.jsonToXml(jsonArgeXtraPara.toString(), "pExtra", null, null, null),Constants.ENCRYPTION_KEY); // 参数加密
			
			LoanController.getInstance().loan(LoanConstants.GUARANTEE_UNFREEZE, platformMemberId, "", platformId, "", LoanConstants.argMerCode, arg3DesXmlPara,argeXtraPara, jsonArgeXtraPara.getString("pBidBillNo"));
		}
	}
	
	/**
	 * 代偿时，直接执行解冻或回调p2p
	 * @param error
	 * @param platformMemberId
	 * @param platformId
	 * @param argMerCode
	 * @param jsonArg3DesXmlPara
	 * @param jsonArgeXtraPara
	 */
	private void handlerCompensateException(ErrorInfo error, long  platformMemberId, long platformId, String memberName, String argMerCode, JSONObject jsonArg3DesXmlPara, JSONObject jsonArgeXtraPara) {
		boolean flag = true;  //是否转账成功
		String merBillNo = jsonArg3DesXmlPara.getString("pMerBillNo");
		
		if(error.code == LoanConstants.TRANSFER_ERROR){  //转账异常
			flag = false; 
		}
		
		if(error.code == LoanConstants.TRANSFER_EXECUTED){  //转账已全部执行
			String bidNo = jsonArg3DesXmlPara.getString("pBidNo");
			String period = jsonArgeXtraPara.getString("periods");
			//分批操作：bidBillNo为每一批的唯一标识，生成法则; 标的号  + 分隔符  + 还款期号
			String bidBillNo = bidNo + LoanConstants.BILLTOKEN + period;
			
			List<TransferBatches> list =  TransferBatches.queryByBidBillNo(bidBillNo);
			if(list != null){  //分批转账时
				//补单
				for(TransferBatches tb : list){
					if(tb.status == 1){  //掉单，补单
						//每一笔转账流水号生成法则：① 分批转账：还款流水号  + 分隔符 + 还款期号  + 批号 + 循环i；②一次转账：还款流水号  + 分隔符 + 循环i
						String OrderNo = merBillNo+ LoanConstants.BILLTOKEN + period + tb.batchNo + 0;
						String response = LOAN.loanOrderQuery("", "",OrderNo)[1];
						if (StringUtils.isNotBlank(response)&& (response.startsWith("[") || response.startsWith("{"))) {
							
							Map<String, String> resultFromLoan = LOAN.getLoanMap(response);
							
							String actState = resultFromLoan.get("ActState").toString(); // 0.未操作,1.已通过,2.已退回,3.自动通过
							if("0".equals(actState)){  //第三方已转账成功
								flag = false;
								tb.status = 0;
								TransferBatches.updateStatus(tb.id,0,error);  //0.未处理，1.处理中，2.处理成功，3.处理失败
								
								//重新转账准备
								jsonArg3DesXmlPara = JSONObject.fromObject(tb.transferBillNos);
								jsonArg3DesXmlPara.put("batchId", tb.id);
								jsonArg3DesXmlPara.put("batchNo", tb.batchNo);
							}else{
								tb.status = 2;
								TransferBatches.updateStatus(tb.id,2,error);  //0.未处理，1.处理中，2.处理成功，3.处理失败
							}
						}else{  //第三方无记录，重新转账
							flag = false;
							tb.status = 0;
							TransferBatches.updateStatus(tb.id,0,error);  //0.未处理，1.处理中，2.处理成功，3.处理失败
							
							//重新转账准备
							jsonArg3DesXmlPara = JSONObject.fromObject(tb.transferBillNos);
							jsonArg3DesXmlPara.put("batchId", tb.id);
							jsonArg3DesXmlPara.put("batchNo", tb.batchNo);
						}
					}
				}

				for(TransferBatches tb : list){
					if(tb.status == 3){  //转账失败，重新转账
						TransferBatches.updateStatus(tb.id,0,error);  //重新转账
						flag = false;
					}
				}
			}
		}
		
		String pWebUrl = jsonArgeXtraPara.getString("pWebUrl");
		String pMerCode = LoanConstants.argMerCode;
		String pErrCode = "";
		String pErrMsg = "";
		if(flag){
			pErrCode = "MG00000F";
			pErrMsg = "还款成功";
		}else{	
			pErrCode = "MG00001F";
			pErrMsg = "转账异常，请稍后再试";
			
			//重新转账
			Map<String, Object> args = LOAN.convertCompensateParams(error, platformId, platformMemberId, memberName, argMerCode, jsonArg3DesXmlPara, jsonArgeXtraPara);
			render("loan/LoanController/loan.html", args);
		}
		
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("pMerBillNo", merBillNo);
		result.put("pMemo1", jsonArg3DesXmlPara.getString("pMemo1")); 
		result.put("pMemo3", jsonArg3DesXmlPara.getString("pMemo3")); 
		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);
		
		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);
		
		render("loan/LoanController/loanReturn.html", pWebUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}

	/**
	 * 开户返回信息
	 * 
	 * @return
	 */
	public static void loanCreateAccountReturn(String AccountType,
			String AccountNumber, String Mobile, String Email, String RealName,
			String IdentificationNo, String LoanPlatformAccount,
			String MoneymoremoreId, String PlatformMoneymoremore,
			String AuthFee, String AuthState, String RandomTimeStamp,
			String Remark1, String Remark2, String Remark3, String ResultCode,
			String Message, String ReturnTimes, String SignInfo) {
		Logger.info("======第三方支付回调开户接口，执行开始========");

		LoanUtil.printLoggerFromLoan(ResultCode, Message,ReturnTimes, AccountType,
				AccountNumber, Mobile, Email, RealName, IdentificationNo,
				LoanPlatformAccount, MoneymoremoreId, PlatformMoneymoremore,
				AuthFee, AuthState, RandomTimeStamp, Remark1, Remark2, Remark3,
				ResultCode, Message, ReturnTimes, SignInfo);

		if (AccountType == null) {
			AccountType = "";
		}

		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}

		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}

		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}

		if (StringUtils.isBlank(Remark3)) {
			Logger.info("======Remark3存放的信息丢失========");
			Remark3 = "";
		}

		String publickey = LoanConstants.publicKey;

		String dataStr = AccountType + AccountNumber + Mobile + Email
				+ RealName + IdentificationNo + LoanPlatformAccount
				+ MoneymoremoreId + PlatformMoneymoremore + AuthFee + AuthState
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ResultCode;

		// 签名
		RsaHelper rsa = RsaHelper.getInstance();
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);

		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}

		String[] arrayRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String platformId = arrayRemark3[0];
		String platformMemberId = arrayRemark3[1];
		String merBillNo = arrayRemark3[2];

		String pWebUrl = new String(Codec.decodeBASE64(Remark1));
		String pS2SUrl = new String(Codec.decodeBASE64(Remark2));
		String pMerCode = PlatformMoneymoremore;

		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();

		String pErrCode = "";
		String pErrMsg = "";
		// 开户成功，继续请求双乾坤“二次分配审核授权”接口
		if ("88".equals(ResultCode) || "16".equals(ResultCode)) {
			result.put("pIpsAcctNo", AccountNumber);
			result.put("IdentificationNo", IdentificationNo);
			result.put("pMerBillNo", merBillNo);
			result.put("pWebUrl", pWebUrl);
			result.put("pS2SUrl", pS2SUrl);
			p3DesXmlPara = LoanUtil.parseMapToXml(result);
			Logger.info("开户后授权：p3DesXmlPara = %s", p3DesXmlPara);
			p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);

			getInstance().loan(LoanConstants.AUTHORIZE_SECONDARY,Long.parseLong(platformMemberId.trim()),
					LoanPlatformAccount,Long.parseLong(platformId.trim()), "",LoanConstants.argMerCode,p3DesXmlPara,"", MoneymoremoreId);
		} else {
			pErrCode = "MG00001F"; // 失败
			pErrMsg = Message;
			result.put("pStatus", "9"); // 失败
			result.put("pMemo1", platformMemberId.trim()); 
			
			p3DesXmlPara = LoanUtil.parseMapToXml(result);
			p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);
		}

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);

		LoanUtil.printLoggerToP2P(pWebUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);

		Logger.info("======第三方支付回调开户接口，结束========");
		
		DealDetail.updateEvent2(merBillNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		render("loan/LoanController/loanReturn.html", pWebUrl, pMerCode,
				pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}

	/**
	 * 开户返回信息(异步)
	 * 
	 * @return
	 */
	public static void loanCreateAccountNotify(String AccountType,
			String AccountNumber, String Mobile, String Email, String RealName,
			String IdentificationNo, String LoanPlatformAccount,
			String MoneymoremoreId, String PlatformMoneymoremore,
			String AuthFee, String AuthState, String RandomTimeStamp,
			String Remark1, String Remark2, String Remark3, String ResultCode,
			String Message, String ReturnTimes, String SignInfo) {
		Logger.info("======第三方支付（异步）回调开户接口，执行开始========");

		LoanUtil.printLoggerFromLoan(ResultCode, Message,ReturnTimes, AccountType,
				AccountNumber, Mobile, Email, RealName, IdentificationNo,
				LoanPlatformAccount, MoneymoremoreId, PlatformMoneymoremore,
				AuthFee, AuthState, RandomTimeStamp, Remark1, Remark2, Remark3,
				ResultCode, Message, ReturnTimes, SignInfo);

		if (AccountType == null) {
			AccountType = "";
		}

		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}

		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}

		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}

		if (StringUtils.isBlank(Remark3)) {
			Logger.info("======Remark3存放的信息丢失========");
			Remark3 = "";
		}

		String publickey = LoanConstants.publicKey;

		String dataStr = AccountType + AccountNumber + Mobile + Email
				+ RealName + IdentificationNo + LoanPlatformAccount
				+ MoneymoremoreId + PlatformMoneymoremore + AuthFee + AuthState
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ResultCode;

		// 签名
		RsaHelper rsa = RsaHelper.getInstance();
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);

		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}

		String[] arrayRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String platformMemberId = arrayRemark3[1];
		String merBillNo = arrayRemark3[2];
		
		String pWebUrl = new String(Codec.decodeBASE64(Remark1));
		String pS2SUrl = new String(Codec.decodeBASE64(Remark2));
		String pMerCode = PlatformMoneymoremore;

		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();

		String pErrCode = "";
		String pErrMsg = "";
		if ("88".equals(ResultCode) || "16".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 成功
		} else {
			pErrCode = "MG00001F"; // 失败
			pErrMsg = Message;
			result.put("pStatus", "9"); // 失败
			result.put("pMemo1", platformMemberId.trim()); 

			p3DesXmlPara = LoanUtil.parseMapToXml(result);
			p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);
		}

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);

		LoanUtil.printLoggerToP2P(pWebUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);

		Logger.info("======第三方支付（异步）回调开户接口，结束========");
		
		DealDetail.updateEvent2(merBillNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		if("MG00001F".equals(pErrCode)){
			//开户失败，通知p2p
			loanNotify(pS2SUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
		}else{
			// 开户成功，继续请求双乾“二次分配审核授权”接口,停止通知
			renderText("SUCCESS");
		}
		
	}
	
	/**
	 * 二次分配审核返回信息
	 * 
	 * @return
	 */
	public static void loanAuthorizeSecondaryReturn(String MoneymoremoreId,
			String PlatformMoneymoremore, String AuthorizeTypeOpen,
			String AuthorizeTypeClose, String AuthorizeType,
			String RandomTimeStamp, String Remark1, String Remark2,
			String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		Logger.info("======第三方支付回调二次分配审核接口，执行开始========");

		LoanUtil.printLoggerFromLoan(ResultCode, Message,ReturnTimes,  MoneymoremoreId,
				PlatformMoneymoremore, AuthorizeTypeOpen, AuthorizeTypeClose,
				AuthorizeType, RandomTimeStamp, Remark1, Remark2, Remark3,
				ResultCode, Message, ReturnTimes, SignInfo);

		if (AuthorizeTypeOpen == null) {
			AuthorizeTypeOpen = "";
		}
		if (AuthorizeTypeClose == null) {
			AuthorizeTypeClose = "";
		}

		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}

		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}

		if (StringUtils.isBlank(Remark3)) {
			Logger.info("======Remark3存放的信息丢失========");
			Remark3 = "";
		}

		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}

		String publickey = LoanConstants.publicKey;

		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = MoneymoremoreId + PlatformMoneymoremore
				+ AuthorizeTypeOpen + AuthorizeTypeClose + AuthorizeType
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ResultCode;

		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);

		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}

		String pWebUrl = new String(Codec.decodeBASE64(Remark1));
		String[] arrayRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String platformId = arrayRemark3[0];
		String platformMemberId = arrayRemark3[1];
		String pIpsAcctNo = arrayRemark3[2];
		String IdentificationNo = arrayRemark3[3];
		String pMerBillNo = arrayRemark3[4];

		String pMerCode = PlatformMoneymoremore;
		
		String pErrCode = "";
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		
		if ("88".equals(ResultCode) || "08".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 授权成功
			result.put("pIpsAcctNo", pIpsAcctNo.trim());
			
		} else {
			pErrCode = "MG00001F"; // 失败
		}
		result.put("pMemo1", platformMemberId.trim());
		result.put("pStatus", pErrCode.equals("MG00000F") ? "10" : "9");
		String pErrMsg = Message;

		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);

		LoanUtil.printLoggerToP2P(pWebUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);

		/* 开户成功 */
		if ("MG00000F".equals(pErrCode)) {
			Member.updateStatus(IdentificationNo.trim());
			Member.updateAccount(Integer.parseInt(platformId.trim()),Long.parseLong(platformMemberId.trim()),pIpsAcctNo.trim(),MoneymoremoreId);
		}

		Logger.info("======第三方支付回调二次分配审核接口，结束========");
		Logger.info("======第三方支付回调开户接口，结束========");
		
		DealDetail.updateEvent2(pMerBillNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		render("loan/LoanController/loanReturn.html", pWebUrl, pMerCode,
				pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}

	/**
	 * 二次分配审核（异步）返回信息
	 * 
	 * @return
	 */
	public static void loanAuthorizeSecondaryNotify(String MoneymoremoreId,
			String PlatformMoneymoremore, String AuthorizeTypeOpen,
			String AuthorizeTypeClose, String AuthorizeType,
			String RandomTimeStamp, String Remark1, String Remark2,
			String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		Logger.info("======第三方支付回调二次分配审核（异步）接口，执行开始========");

		LoanUtil.printLoggerFromLoan(ResultCode, Message,ReturnTimes, MoneymoremoreId,
				PlatformMoneymoremore, AuthorizeTypeOpen, AuthorizeTypeClose,
				AuthorizeType, RandomTimeStamp, Remark1, Remark2, Remark3,
				ResultCode, Message, ReturnTimes, SignInfo);

		if (AuthorizeTypeOpen == null) {
			AuthorizeTypeOpen = "";
		}
		if (AuthorizeTypeClose == null) {
			AuthorizeTypeClose = "";
		}

		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}

		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}

		if (StringUtils.isBlank(Remark3)) {
			Logger.info("======Remark3存放的信息丢失========");
			Remark3 = "";
		}

		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}

		String publickey = LoanConstants.publicKey;

		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = MoneymoremoreId + PlatformMoneymoremore
				+ AuthorizeTypeOpen + AuthorizeTypeClose + AuthorizeType
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ResultCode;

		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);

		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}

		String pS2SUrl = new String(Codec.decodeBASE64(Remark2));
		String[] arrayRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String platformId = arrayRemark3[0];
		String platformMemberId = arrayRemark3[1];
		String pIpsAcctNo = arrayRemark3[2];
		String IdentificationNo = arrayRemark3[3];
		String pMerBillNo = arrayRemark3[4];
		
		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		
		if ("88".equals(ResultCode) || "08".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 授权成功
			result.put("pIpsAcctNo", pIpsAcctNo.trim());
		} else {
			pErrCode = "MG00001F"; // 失败
		}
		result.put("pMemo1", platformMemberId.trim());
		result.put("pStatus", pErrCode.equals("MG00000F") ? "10" : "9");
		String pErrMsg = Message;

		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara
				+ Constants.ENCRYPTION_KEY);

		LoanUtil.printLoggerToP2P(pS2SUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);

		/* 开户成功 */
		if ("MG00000F".equals(pErrCode)) {
			Member.updateStatus(IdentificationNo.trim());
			Member.updateAccount(Integer.parseInt(platformId.trim()),Long.parseLong(platformMemberId.trim()),pIpsAcctNo.trim(),MoneymoremoreId);
		}

		Logger.info("======第三方支付回调二次分配审核（异步）接口，结束========");
		Logger.info("======第三方支付回调开户（异步）接口，结束========");
		
		DealDetail.updateEvent2(pMerBillNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		loanNotify(pS2SUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}
	
	/**
	 * 标的登记(新增)返回信息
	 * 
	 * @return
	 */
	public static void loanAddRegisterSubjectReturn(String LoanJsonList,
			String Action, String PlatformMoneymoremore,
			String RandomTimeStamp, String Remark1, String Remark2,
			String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		Logger.info("======第三方支付回调标的登记(新增)接口，执行开始========");

		LoanJsonList = LoanUtil.UrlDecoder(LoanJsonList, "utf-8");

		LoanUtil.printLoggerFromLoan(ResultCode, Message, ReturnTimes, LoanJsonList, Action,
				PlatformMoneymoremore, RandomTimeStamp, Remark1, Remark2,
				Remark3, ResultCode, Message, ReturnTimes, SignInfo);
		
		Map<String, Object> loanMap = LOAN.getLoanList(LoanJsonList).get(0);

		if (Action == null) {
			Action = "";
		}

		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}

		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}

		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}

		if (StringUtils.isBlank(Remark3)) {
			Logger.info("======Remark3存放的信息丢失========");
			Remark3 = "";
		}

		String publickey = LoanConstants.publicKey;

		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = LoanJsonList + PlatformMoneymoremore + Action
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ResultCode;

		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,publickey);

		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}

		String pWebUrl = new String(Codec.decodeBASE64(Remark1));
		String[] arrayRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
        String pOperationType = arrayRemark3[0];
        String platformMemberId = arrayRemark3[1];
        String pMemo3 = arrayRemark3[2];
		
		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		
		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 成功
			result.put("pIpsBillNo", loanMap.get("LoanNo").toString());

			//更新标的状态
			Bid.updateStatus(loanMap.get("BatchNo").toString(),true);
			
			DealDetail.updateStatus(loanMap.get("OrderNo").toString());

		} else {
			pErrCode = "MG00001F"; // 失败
			result.put("pIpsBillNo", "");
		}
		result.put("pMerBillNo", loanMap.get("OrderNo").toString());
		result.put("pOperationType", pOperationType);
		result.put("pBidNo", loanMap.get("BatchNo").toString());
		result.put("pMemo1", platformMemberId);
		result.put("pMemo3", pMemo3);
		String pErrMsg = Message;

		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);

		LoanUtil.printLoggerToP2P(pWebUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);

		Logger.info("======第三方支付回调标的登记（新增）接口，结束========");

		DealDetail.updateEvent2(loanMap.get("OrderNo").toString(), "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		render("loan/LoanController/loanReturn.html", pWebUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}
	
	/**
	 * 标的登记(新增)返回信息（异步）
	 * 
	 * @return
	 */
	public static void loanAddRegisterSubjectNotify(String LoanJsonList,
			String Action, String PlatformMoneymoremore,
			String RandomTimeStamp, String Remark1, String Remark2,
			String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		Logger.info("======第三方支付回调标的登记(新增)（异步）接口，执行开始========");

		LoanJsonList = LoanUtil.UrlDecoder(LoanJsonList, "utf-8");
		
		LoanUtil.printLoggerFromLoan(ResultCode, Message, ReturnTimes, LoanJsonList, Action,
				PlatformMoneymoremore, RandomTimeStamp, Remark1, Remark2,
				Remark3, ResultCode, Message, ReturnTimes, SignInfo);
		
		Map<String, Object> loanMap = LOAN.getLoanList(LoanJsonList).get(0);
		
		if (Action == null) {
			Action = "";
		}
		
		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}
		
		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}
		
		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}
		
		if (StringUtils.isBlank(Remark3)) {
			Logger.info("======Remark3存放的信息丢失========");
			Remark3 = "";
		}
		
		String publickey = LoanConstants.publicKey;
		
		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = LoanJsonList + PlatformMoneymoremore + Action
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ResultCode;
		
		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);
		
		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}
		
		String pS2SUrl = new String(Codec.decodeBASE64(Remark2));
		String[] arrayRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String pOperationType = arrayRemark3[0];
	    String platformMemberId = arrayRemark3[1];
	    String pMemo3 = arrayRemark3[2];
		
		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		
		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 成功
			result.put("pIpsBillNo", loanMap.get("LoanNo").toString());

			//更新标的状态
			Bid.updateStatus(loanMap.get("BatchNo").toString(),true);
			
			DealDetail.updateStatus(loanMap.get("OrderNo").toString());
			
		} else {
			pErrCode = "MG00001F"; // 失败
			result.put("pIpsBillNo", "");
		}
		result.put("pMerBillNo", loanMap.get("OrderNo").toString());
		result.put("pOperationType", pOperationType);
		result.put("pBidNo", loanMap.get("BatchNo").toString());
		result.put("pMemo1", platformMemberId);
		result.put("pMemo3", pMemo3);
		String pErrMsg = Message;

		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);

		LoanUtil.printLoggerToP2P(pS2SUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);

		Logger.info("======第三方支付回调标的登记（新增）（异步）接口，结束========");

		DealDetail.updateEvent2(loanMap.get("OrderNo").toString(), "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		loanNotify(pS2SUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}

	/**
	 * 标的登记(结束)返回信息
	 * 
	 * @return
	 */
	public static void loanEndRegisterSubjectReturn(String LoanNoList,
			String LoanNoListFail, String PlatformMoneymoremore,
			String AuditType, String RandomTimeStamp, String Remark1,
			String Remark2, String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		Logger.info("======第三方支付回调标的登记（结束）接口，执行开始========");
		
		LoanUtil.printLoggerFromLoan(ResultCode, Message, ReturnTimes, LoanNoList,
				LoanNoListFail, PlatformMoneymoremore, RandomTimeStamp,
				AuditType, Remark1, Remark2, Remark3, ResultCode, Message,
				ReturnTimes, SignInfo);

		if(StringUtils.isNotBlank(LoanNoListFail)){  //有问题的流水号
			Logger.info("======标的登记（结束）时，有问题的流水号：%s", LoanNoListFail);
		}
		
		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}

		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}

		if (Remark3 == null) {
			Remark3 = "";
		}

		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}

		String publickey = LoanConstants.publicKey;

		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = LoanNoList + LoanNoListFail + PlatformMoneymoremore
				+ AuditType + RandomTimeStamp + Remark1 + Remark2 + Remark3
				+ ResultCode;

		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);

		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}

		String pWebUrl = new String(Codec.decodeBASE64(Remark1));
		String[] arraryRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String pOperationType = arraryRemark3[0];
		String pMerBillNo = arraryRemark3[1];
		String pIpsBillNo = arraryRemark3[2];
		String pBidNo = arraryRemark3[3];
		String pMemo3 = arraryRemark3[4];
		
		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		
		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 成功

		} else {
			pErrCode = "MG00001F"; // 失败
		}
		
		result.put("pOperationType", pOperationType);
		result.put("pMerBillNo", pMerBillNo);
		result.put("pIpsBillNo", pIpsBillNo);
		result.put("pBidNo", pBidNo);
		result.put("pMemo3", pMemo3);
		String pErrMsg = Message;

		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);

		LoanUtil.printLoggerToP2P(pWebUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);

		Logger.info("======第三方支付回调标的登记（结束）接口，结束========");

		DealDetail.updateEvent2(pMerBillNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		render("loan/LoanController/loanReturn.html", pWebUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}
	
	/**
	 * 标的登记(结束)返回信息（异步）
	 * 
	 * @return
	 */
	public static void loanEndRegisterSubjectNotify(String LoanNoList,
			String LoanNoListFail, String PlatformMoneymoremore,
			String AuditType, String RandomTimeStamp, String Remark1,
			String Remark2, String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		Logger.info("======第三方支付回调标的登记（结束）（异步）接口，执行开始========");
		LoanUtil.printLoggerFromLoan(ResultCode, Message, ReturnTimes, LoanNoList,
				LoanNoListFail, PlatformMoneymoremore, RandomTimeStamp,
				AuditType, Remark1, Remark2, Remark3, ResultCode, Message,
				ReturnTimes, SignInfo);
		
		if(StringUtils.isNotBlank(LoanNoListFail)){  //有问题的流水号
			Logger.info("======标的登记（结束）（异步）时，有问题的流水号：%s", LoanNoListFail);
		}
		
		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}
		
		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}
		
		if (Remark3 == null) {
			Remark3 = "";
		}
		
		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}
		
		String publickey = LoanConstants.publicKey;
		
		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = LoanNoList + LoanNoListFail + PlatformMoneymoremore
				+ AuditType + RandomTimeStamp + Remark1 + Remark2 + Remark3
				+ ResultCode;
		
		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);
		
		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}

		String pS2SUrl = new String(Codec.decodeBASE64(Remark2));
		String[] arraryRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String pOperationType = arraryRemark3[0];
		String pMerBillNo = arraryRemark3[1];
		String pIpsBillNo = arraryRemark3[2];
		String pBidNo = arraryRemark3[3];
		String pMemo3 = arraryRemark3[4];
		
		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 成功

		} else {
			pErrCode = "MG00001F"; // 失败
		}
		
		result.put("pOperationType", pOperationType);
		result.put("pMerBillNo", pMerBillNo);
		result.put("pIpsBillNo", pIpsBillNo);
		result.put("pBidNo", pBidNo);
		result.put("pMemo3", pMemo3);
		
		String pErrMsg = Message;

		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);

		LoanUtil.printLoggerToP2P(pS2SUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);

		Logger.info("======第三方支付回调标的登记（结束）接口，结束========");

		DealDetail.updateEvent2(pMemo3, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		loanNotify(pS2SUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}

	/**
	 * 登记债权人返回信息
	 * 
	 * @return
	 */
	public static void loanRegisterCreditorReturn(String LoanJsonList,
			String Action, String PlatformMoneymoremore,
			String RandomTimeStamp, String Remark1, String Remark2,
			String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		Logger.info("======第三方支付回调登记债权人接口，执行开始========");

		LoanJsonList = LoanUtil.UrlDecoder(LoanJsonList, "utf-8");
		
		
		LoanUtil.printLoggerFromLoan(ResultCode, Message, ReturnTimes, LoanJsonList, Action,
				PlatformMoneymoremore, RandomTimeStamp, Remark1, Remark2,
				Remark3, ResultCode, Message, ReturnTimes, SignInfo);
		
		Map<String, Object> loanMap = LOAN.getLoanList(LoanJsonList).get(0);
		
		if (Action == null) {
			Action = "";
		}

		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}

		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}

		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}

		if (Remark3 == null) {
			Remark3 = "";
		}

		String publickey = LoanConstants.publicKey;

		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = LoanJsonList + PlatformMoneymoremore + Action
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ResultCode;

		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);

		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}

//		String pWebUrl = new String(Codec.decodeBASE64(Remark1));
		String pWSUrl = new String(Codec.decodeBASE64(Remark1));
		String[] arrayRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String platformMemberId = arrayRemark3[0];
		String pFee = arrayRemark3[1].trim();
		String remainFee = arrayRemark3[2].trim();
		
		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 成功
			result.put("pP2PBillNo", loanMap.get("LoanNo").toString()); // 流水号

			// 更新标的的剩余借款管理费
			Bid.updateRemainFee(loanMap.get("BatchNo").toString(),remainFee);
			
			DealDetail.updateStatus(loanMap.get("OrderNo").toString());
			
		} else {
			pErrCode = "MG00001F"; // 失败
			result.put("pP2PBillNo", ""); // 流水号
		}
		result.put("pFee", pFee); // 手续费（登记而已）
		result.put("pMerBillNo", loanMap.get("OrderNo").toString()); 
		result.put("pMemo1", platformMemberId);  //platmemberid

		String pErrMsg = Message;

		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara
				+ Constants.ENCRYPTION_KEY);

//		LoanUtil.printLoggerToP2P(pWebUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);
		LoanUtil.printLoggerToP2P(pWSUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);

		Logger.info("======第三方支付回调登记债权人接口，结束========");

		DealDetail.updateEvent2(loanMap.get("OrderNo").toString(), "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		Map<String, Object> args = handlerService(pWSUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);
		
		render("loan/LoanController/loanReturnAfterWS.html", args);
	}
	
	/**
	 * 登记债权人返回信息（异步）
	 * 
	 * @return
	 */
	public static void loanRegisterCreditorNotify(String LoanJsonList,
			String Action, String PlatformMoneymoremore,
			String RandomTimeStamp, String Remark1, String Remark2,
			String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		Logger.info("======第三方支付回调登记债权人（异步）接口，执行开始========");

		LoanJsonList = LoanUtil.UrlDecoder(LoanJsonList, "utf-8");
		
		LoanUtil.printLoggerFromLoan(ResultCode, Message, ReturnTimes, LoanJsonList, Action,
				PlatformMoneymoremore, RandomTimeStamp, Remark1, Remark2,
				Remark3, ResultCode, Message, ReturnTimes, SignInfo);
		
		Map<String, Object> loanMap = LOAN.getLoanList(LoanJsonList).get(0);

		if (Action == null) {
			Action = "";
		}

		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}

		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}

		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}

		if (Remark3 == null) {
			Remark3 = "";
		}

		String publickey = LoanConstants.publicKey;

		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = LoanJsonList + PlatformMoneymoremore + Action
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ResultCode;

		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);

		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}

		String pS2SUrl = new String(Codec.decodeBASE64(Remark2));
		String[] arrayRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String platformMemberId = arrayRemark3[0];
		String pFee = arrayRemark3[1].trim();
		String remainFee = arrayRemark3[2].trim();
		
		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 成功
			result.put("pP2PBillNo", loanMap.get("LoanNo").toString()); // 流水号

			// 更新标的的剩余借款管理费
			Bid.updateRemainFee(loanMap.get("BatchNo").toString(),remainFee);
			
			DealDetail.updateStatus(loanMap.get("OrderNo").toString());
		} else {
			pErrCode = "MG00001F"; // 失败
			result.put("pP2PBillNo", ""); // 流水号
		}
		result.put("pFee", pFee); // 手续费（登记而已）
		result.put("pMerBillNo", loanMap.get("OrderNo").toString()); 
		result.put("pMemo1", platformMemberId);  //platmemberid
		
		String pErrMsg = Message;

		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);

		LoanUtil.printLoggerToP2P(pS2SUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);

		Logger.info("======第三方支付回调登记债权人（异步）接口，结束========");
		
		DealDetail.updateEvent2(loanMap.get("OrderNo").toString(), "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);

		loanNotify(pS2SUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}

	/**
	 * 登记债权转让返回信息
	 * 
	 * @return
	 */
	public static void loanCreditTransferReturn(String LoanJsonList,
			String Action, String PlatformMoneymoremore,
			String RandomTimeStamp, String Remark1, String Remark2,
			String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		Logger.info("======第三方支付回调登记债权转让接口，执行开始========");

		LoanJsonList = LoanUtil.UrlDecoder(LoanJsonList, "utf-8");
		
		LoanUtil.printLoggerFromLoan(ResultCode, Message, ReturnTimes, LoanJsonList, Action,
				PlatformMoneymoremore, RandomTimeStamp, Remark1, Remark2,
				Remark3, ResultCode, Message, ReturnTimes, SignInfo);
		
		Map<String, Object> loanMap = LOAN.getLoanList(LoanJsonList).get(0);

		if (Action == null) {
			Action = "";
		}

		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}

		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}

		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}

		if (Remark3 == null) {
			Remark3 = "";
		}

		String publickey = LoanConstants.publicKey;

		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = LoanJsonList + PlatformMoneymoremore + Action
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ResultCode;

		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);

		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}

		String pWebUrl = new String(Codec.decodeBASE64(Remark1));

		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 成功
			DealDetail.updateStatus(loanMap.get("OrderNo").toString());
		} else {
			pErrCode = "MG00001F"; // 失败
		}
		String pErrMsg = Message;

		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("pMerBillNo", loanMap.get("OrderNo").toString());
		result.put("pMemo1", new String(Codec.decodeBASE64(Remark3))); // platmemberid

		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,
				Constants.ENCRYPTION_KEY);

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara
				+ Constants.ENCRYPTION_KEY);

		LoanUtil.printLoggerToP2P(pWebUrl, pMerCode, pErrCode, pErrMsg,
				p3DesXmlPara, pSign);

		Logger.info("======第三方支付回调登记债权转让接口，结束========");

		DealDetail.updateEvent2(loanMap.get("OrderNo").toString(), "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		render("loan/LoanController/loanReturn.html", pWebUrl, pMerCode,
				pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}
	
	/**
	 * 登记债权转让返回信息（异步）
	 * 
	 * @return
	 */
	public static void loanCreditTransferNotify(String LoanJsonList,
			String Action, String PlatformMoneymoremore,
			String RandomTimeStamp, String Remark1, String Remark2,
			String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		Logger.info("======第三方支付回调登记债权转让（异步）接口，执行开始========");
		
		LoanJsonList = LoanUtil.UrlDecoder(LoanJsonList, "utf-8");
		
		LoanUtil.printLoggerFromLoan(ResultCode, Message, ReturnTimes, LoanJsonList, Action,
				PlatformMoneymoremore, RandomTimeStamp, Remark1, Remark2,
				Remark3, ResultCode, Message, ReturnTimes, SignInfo);
		
		Map<String, Object> loanMap = LOAN.getLoanList(LoanJsonList).get(0);
		
		if (Action == null) {
			Action = "";
		}
		
		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}
		
		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}
		
		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}
		
		if (Remark3 == null) {
			Remark3 = "";
		}
		
		String publickey = LoanConstants.publicKey;
		
		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = LoanJsonList + PlatformMoneymoremore + Action
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ResultCode;
		
		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);
		
		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}
		
		String pS2SUrl = new String(Codec.decodeBASE64(Remark2));
		
		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 成功
			
			DealDetail.updateStatus(loanMap.get("OrderNo").toString());
		} else {
			pErrCode = "MG00001F"; // 失败
		}
		String pErrMsg = Message;
		
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("pMerBillNo", loanMap.get("OrderNo").toString());
		result.put("pMemo1", new String(Codec.decodeBASE64(Remark3))); // platmemberid
		
		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,
				Constants.ENCRYPTION_KEY);
		
		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara
				+ Constants.ENCRYPTION_KEY);
		
		LoanUtil.printLoggerToP2P(pS2SUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);
		
		Logger.info("======第三方支付回调登记债权转让（异步）接口，结束========");
		
		DealDetail.updateEvent2(loanMap.get("OrderNo").toString(), "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		loanNotify(pS2SUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}
	
	/**
	 * 自动投标/还款返回信息
	 * 
	 * @return
	 */
	public static void loanSignReturn(String MoneymoremoreId,
			String PlatformMoneymoremore, String AuthorizeTypeOpen,
			String AuthorizeTypeClose, String AuthorizeType,
			String RandomTimeStamp, String Remark1, String Remark2,
			String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		Logger.info("======第三方支付回调自动投标/还款接口，执行开始========");

		LoanUtil.printLoggerFromLoan(ResultCode, Message, ReturnTimes, MoneymoremoreId,
				PlatformMoneymoremore, AuthorizeTypeOpen, AuthorizeTypeClose,
				AuthorizeType, RandomTimeStamp, Remark1, Remark2, Remark3,
				ResultCode, Message, ReturnTimes, SignInfo);

		if (AuthorizeTypeOpen == null) {
			AuthorizeTypeOpen = "";
		}
		if (AuthorizeTypeClose == null) {
			AuthorizeTypeClose = "";
		}

		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}

		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}

		if (Remark3 == null) {
			Remark3 = "";
		}

		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}

		String publickey = LoanConstants.publicKey;

		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = MoneymoremoreId + PlatformMoneymoremore
				+ AuthorizeTypeOpen + AuthorizeTypeClose + AuthorizeType
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ResultCode;

		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);

		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}

		String pWebUrl = new String(Codec.decodeBASE64(Remark1));
		String[] arrayRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String platformMemberId = arrayRemark3[0];
		String pMerBillNo = arrayRemark3[1];
		
		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		if ("88".equals(ResultCode) || "08".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 成功
		} else {
			pErrCode = "MG00001F"; // 失败
		}
		String pErrMsg = Message;

		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		String pIpsAuthNo = sdf.format(new Date()) + LoanUtil.getRandomNum(3);

		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("pMemo1", platformMemberId);
		result.put("pIpsAuthNo", pIpsAuthNo);

		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,
				Constants.ENCRYPTION_KEY);

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara
				+ Constants.ENCRYPTION_KEY);

		LoanUtil.printLoggerToP2P(pWebUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);

		Logger.info("======第三方支付回调自动投标/还款接口，结束========");

		DealDetail.updateEvent2(pMerBillNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		render("loan/LoanController/loanReturn.html", pWebUrl, pMerCode,
				pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}
	
	/**
	 * 自动投标/还款返回信息（异步）
	 * 
	 * @return
	 */
	public static void loanSignNotify(String MoneymoremoreId,
			String PlatformMoneymoremore, String AuthorizeTypeOpen,
			String AuthorizeTypeClose, String AuthorizeType,
			String RandomTimeStamp, String Remark1, String Remark2,
			String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		Logger.info("======第三方支付回调自动投标/还款（异步）接口，执行开始========");
		
		LoanUtil.printLoggerFromLoan(ResultCode, Message, ReturnTimes, MoneymoremoreId,
				PlatformMoneymoremore, AuthorizeTypeOpen, AuthorizeTypeClose,
				AuthorizeType, RandomTimeStamp, Remark1, Remark2, Remark3,
				ResultCode, Message, ReturnTimes, SignInfo);
		
		if (AuthorizeTypeOpen == null) {
			AuthorizeTypeOpen = "";
		}
		if (AuthorizeTypeClose == null) {
			AuthorizeTypeClose = "";
		}
		
		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}
		
		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}
		
		if (Remark3 == null) {
			Remark3 = "";
		}
		
		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}
		
		String publickey = LoanConstants.publicKey;
		
		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = MoneymoremoreId + PlatformMoneymoremore
				+ AuthorizeTypeOpen + AuthorizeTypeClose + AuthorizeType
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ResultCode;
		
		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);
		
		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}
		
		String pS2SUrl = new String(Codec.decodeBASE64(Remark2));
		String[] arrayRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String platformMemberId = arrayRemark3[0];
		String pMerBillNo = arrayRemark3[1];
		
		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		if ("88".equals(ResultCode) || "08".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 成功
		} else {
			pErrCode = "MG00001F"; // 失败
		}
		String pErrMsg = Message;
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
		String pIpsAuthNo = sdf.format(new Date()) + LoanUtil.getRandomNum(3);
		
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("pMemo1", platformMemberId);
		result.put("pIpsAuthNo", pIpsAuthNo);
		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,
				Constants.ENCRYPTION_KEY);
		
		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara
				+ Constants.ENCRYPTION_KEY);
		
		LoanUtil.printLoggerToP2P(pS2SUrl, pMerCode, pErrCode, pErrMsg,
				p3DesXmlPara, pSign);
		
		Logger.info("======第三方支付回调自动投标/还款（异步）接口，结束========");
		
		DealDetail.updateEvent2(pMerBillNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		loanNotify(pS2SUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}

	/**
	 * 充值返回信息
	 * 
	 * @return
	 */
	public static void loanRechargeReturn(String RechargeMoneymoremore,
			String PlatformMoneymoremore, String LoanNo, String OrderNo,
			String Amount, String Fee, String FeePlatform, String RechargeType,
			String FeeType, String CardNoList, String RandomTimeStamp,
			String Remark1, String Remark2, String Remark3, String ResultCode,
			String Message, String ReturnTimes, String SignInfo) {

		Logger.info("======第三方支付回调充值接口，执行开始========");

		LoanUtil.printLoggerFromLoan(ResultCode, Message,ReturnTimes, 
				RechargeMoneymoremore, PlatformMoneymoremore, LoanNo, OrderNo,
				Amount, Fee, FeePlatform, RechargeType, FeeType, CardNoList,
				RandomTimeStamp, Remark1, Remark2, Remark3, ResultCode,
				Message, ReturnTimes, SignInfo);

		if (Fee == null) {
			Fee = "";
		}

		if (RechargeType == null) {
			RechargeType = "";
		}

		if (FeeType == null) {
			FeeType = "";
		}

		if (CardNoList == null) {
			CardNoList = "";
		}

		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}

		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}

		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}

		if (StringUtils.isBlank(Remark3)) {
			Logger.info("======Remark3存放的信息丢失========");
			Remark3 = "";
		}

		String privatekey = LoanConstants.privateKeyPKCS8;
		String publickey = LoanConstants.publicKey;

		RsaHelper rsa = RsaHelper.getInstance();
		if (StringUtils.isNotBlank(CardNoList)) {
			CardNoList = rsa.decryptData(CardNoList, privatekey);
			if (StringUtils.isBlank(CardNoList)) {
				CardNoList = "";
			}
		}
		String dataStr = RechargeMoneymoremore + PlatformMoneymoremore + LoanNo
				+ OrderNo + Amount + Fee + RechargeType + FeeType + CardNoList
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ResultCode;

		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);

		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}

//		String pWebUrl = new String(Codec.decodeBASE64(Remark1));
		String pWSUrl = new String(Codec.decodeBASE64(Remark1));
		
		String[] arrayRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String platformMemberId = arrayRemark3[0];
		String pMerBillNo = arrayRemark3[1];
		
		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";

		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 成功

			DealDetail.updateStatus(pMerBillNo);
		} else {
			pErrCode = "MG00001F"; // 失败
		}
		String pErrMsg = Message;

		if(Convert.strToDouble(Amount, 0) <= 1){  //充值金额必须大于1元
			pErrMsg = "充值金额必须大于1元";
		}
		
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("pMerBillNo", OrderNo);
		result.put("pTrdAmt", Amount);
		result.put("pMemo1", platformMemberId);

		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,
				Constants.ENCRYPTION_KEY);

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara
				+ Constants.ENCRYPTION_KEY);

//		LoanUtil.printLoggerToP2P(pWebUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);
		LoanUtil.printLoggerToP2P(pWSUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);

		Logger.info("======第三方支付回调充值接口，结束========");

		DealDetail.updateEvent2(pMerBillNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		Map<String, Object> args = handlerService(pWSUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);
		
		render("loan/LoanController/loanReturnAfterWS.html", args);
	}
	
	/**
	 * 充值返回信息（异步）
	 * 
	 * @return
	 */
	public static void loanRechargeNotify(String RechargeMoneymoremore,
			String PlatformMoneymoremore, String LoanNo, String OrderNo,
			String Amount, String Fee, String FeePlatform, String RechargeType,
			String FeeType, String CardNoList, String RandomTimeStamp,
			String Remark1, String Remark2, String Remark3, String ResultCode,
			String Message, String ReturnTimes, String SignInfo) {
		
		Logger.info("======第三方支付回调充值（异步）接口，执行开始========");
		
		LoanUtil.printLoggerFromLoan(ResultCode, Message,ReturnTimes, 
				RechargeMoneymoremore, PlatformMoneymoremore, LoanNo, OrderNo,
				Amount, Fee, FeePlatform, RechargeType, FeeType, CardNoList,
				RandomTimeStamp, Remark1, Remark2, Remark3, ResultCode,
				Message, ReturnTimes, SignInfo);
		
		if (Fee == null) {
			Fee = "";
		}
		
		if (RechargeType == null) {
			RechargeType = "";
		}
		
		if (FeeType == null) {
			FeeType = "";
		}
		
		if (CardNoList == null) {
			CardNoList = "";
		}
		
		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}
		
		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}
		
		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}
		
		if (StringUtils.isBlank(Remark3)) {
			Logger.info("======Remark3存放的信息丢失========");
			Remark3 = "";
		}
		
		String privatekey = LoanConstants.privateKeyPKCS8;
		String publickey = LoanConstants.publicKey;
		
		RsaHelper rsa = RsaHelper.getInstance();
		if (StringUtils.isNotBlank(CardNoList)) {
			CardNoList = rsa.decryptData(CardNoList, privatekey);
			if (StringUtils.isBlank(CardNoList)) {
				CardNoList = "";
			}
		}
		String dataStr = RechargeMoneymoremore + PlatformMoneymoremore + LoanNo
				+ OrderNo + Amount + Fee + RechargeType + FeeType + CardNoList
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ResultCode;
		
		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);
		
		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}
		
		String pS2SUrl = new String(Codec.decodeBASE64(Remark2));
		
		String[] arrayRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String platformMemberId = arrayRemark3[0];
		String pMerBillNo = arrayRemark3[1];
		
		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 成功
			
			DealDetail.updateStatus(pMerBillNo);
		} else {
			pErrCode = "MG00001F"; // 失败
		}
		String pErrMsg = Message;
		
		if(Convert.strToDouble(Amount, 0) <= 1){  //充值金额必须大于1元
			pErrMsg = "充值金额必须大于1元";
		}
		
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("pMerBillNo", OrderNo);
		result.put("pTrdAmt", Amount);
		result.put("pMemo1", platformMemberId);
		
		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,
				Constants.ENCRYPTION_KEY);
		
		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara
				+ Constants.ENCRYPTION_KEY);
		
		LoanUtil.printLoggerToP2P(pS2SUrl, pMerCode, pErrCode, pErrMsg,
				p3DesXmlPara, pSign);
		
		Logger.info("======第三方支付回调充值（异步）接口，结束========");
		
		DealDetail.updateEvent2(pMerBillNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		loanNotify(pS2SUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}

	/**
	 * 转账：投资返回信息
	 * 
	 * @return
	 */
	public static void loanTransferReturn(String LoanNoList,
			String LoanNoListFail, String PlatformMoneymoremore,
			String AuditType, String RandomTimeStamp, String Remark1,
			String Remark2, String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		ErrorInfo error = new ErrorInfo();
		
		Logger.info("======第三方支付回调转账（投资）接口，执行开始========");

		LoanUtil.printLoggerFromLoan(ResultCode, Message, ReturnTimes, LoanNoList,
				LoanNoListFail, PlatformMoneymoremore, RandomTimeStamp,
				AuditType, Remark1, Remark2, Remark3, ResultCode, Message,
				ReturnTimes, SignInfo);

		if(StringUtils.isNotBlank(LoanNoListFail)){  //有问题的流水号
			Logger.info("======投资时，有问题的流水号：%s", LoanNoListFail);
		}

		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}

		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}

		if (Remark3 == null) {
			Remark3 = "";
		}
		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}

		String publickey = LoanConstants.publicKey;

		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = LoanNoList + LoanNoListFail + PlatformMoneymoremore
				+ AuditType + RandomTimeStamp + Remark1 + Remark2 + Remark3
				+ ResultCode;

		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,publickey);

		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}

		String pWebUrl = new String(Codec.decodeBASE64(Remark1));
		String pS2SUrl = new String(Codec.decodeBASE64(Remark2));
		String[] arrayRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String pMerBillNo = arrayRemark3[0];
		String pTransferType = arrayRemark3[1];
		String platformMemberId = arrayRemark3[2];
		String bidBillNo = arrayRemark3[3];
		String batchId = arrayRemark3[4];
		
		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		String pErrMsg = Message;
		
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("pMerBillNo", pMerBillNo);
		result.put("pTransferType", pTransferType);
		result.put("pMemo1", platformMemberId);
		
		if(!"-1".equals(batchId)){  //更新分批审核结果
			TransferBatches.updateStatus(Long.parseLong(batchId.trim()),"88".equals(ResultCode)?2:3,error);
			if(error.code < 1){
				return;
			}
		}
		
		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F";
			result.put("pBidBillNo", bidBillNo);  //发标流水号
			result.put("pWebUrl", pWebUrl);
			result.put("pS2SUrl", pS2SUrl);	
			
			if(!"-1".equals(batchId)){  //分批审核时
				batchTransferAuditCB(error, LoanConstants.TRANSFER, pMerCode, platformMemberId, batchId, bidBillNo, result, pWebUrl);
			}
			
			p3DesXmlPara = LoanUtil.parseMapToXml(result);
			Logger.info("放款后解冻保证金：p3DesXmlPara = %s", p3DesXmlPara);
			p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);
			
			//解冻保证金pBidBillNo  
			getInstance().loan(LoanConstants.GUARANTEE_UNFREEZE, Long.parseLong(platformMemberId.trim()), "", 0L, "",LoanConstants.argMerCode, p3DesXmlPara,p3DesXmlPara, bidBillNo);
		} else {
			pErrCode = "MG00001F";
		}


		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);
		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);

		LoanUtil.printLoggerToP2P(pWebUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);
		Logger.info("======第三方支付回调转账（投资）接口，结束========");

		DealDetail.updateEvent2(pMerBillNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		render("loan/LoanController/loanReturn.html", pWebUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}
	
	/**
	 * 审核成功后继续审核下一批转账
	 * @param error
	 * @param pMerCode
	 * @param platformMemberId
	 * @param batchId
	 * @param bidBillNo
	 * @param result
	 * @param pWebUrl
	 */
	private static void batchTransferAuditCB(ErrorInfo error, int type, String pMerCode, String platformMemberId, 
			String batchId, String bidBillNo, LinkedHashMap<String, String> result, String pWebUrl) {
		TransferBatches batch = TransferBatches.pollRemainBill(bidBillNo);  //
		if(batch != null){  //有未审核
			result.put("batchId", batch.id + "");  
			result.put("pBillNos", batch.transferBillNos); 
			
			String p3DesXmlPara = LoanUtil.parseMapToXml(result); 
			Logger.info("继续审核：p3DesXmlPara = %s", p3DesXmlPara);
			p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);
			
			//继续审核
			getInstance().loan(type, Long.parseLong(platformMemberId.trim()), "", 0L, "",LoanConstants.argMerCode, p3DesXmlPara,p3DesXmlPara, bidBillNo);
		}
		
		List<TransferBatches> list =  TransferBatches.queryByBidBillNo(bidBillNo);
		if(list != null){  //已全部处理过
			boolean flag = true;  //是否转账成功
			
			//补单
			for(TransferBatches tb : list){
				if(tb.status == 1){  //状态为：处理中。进行补单
					String LoanNo = tb.transferBillNos.split(",")[0].trim();
					String response = LOAN.loanOrderQuery("", LoanNo,"")[1];
					if (StringUtils.isNotBlank(response)&& (response.startsWith("[") || response.startsWith("{"))) {
						Map<String, String> resultFromLoan = LOAN.getLoanMap(response);

						String actState = resultFromLoan.get("ActState").toString(); // 0.未操作,1.已通过,2.已退回,3.自动通过
						if("0".equals(actState)){  //第三方未审核（或失败）
//							flag = false;
//							tb.status = 0;  //修改状态为未处理，下次转账
//							TransferBatches.updateStatus(tb.id,0,error);  //0.未处理，1.处理中，2.处理成功，3.处理失败
							//重新审核
							result.put("batchId", tb.id + "");  
							result.put("pBillNos", tb.transferBillNos); 
							
							String p3DesXmlPara = LoanUtil.parseMapToXml(result); 
							Logger.info("继续审核：p3DesXmlPara = %s", p3DesXmlPara);
							p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);
							
							//继续审核
							getInstance().loan(LoanConstants.TRANSFER, Long.parseLong(platformMemberId.trim()), "", 0L, "",LoanConstants.argMerCode, p3DesXmlPara,p3DesXmlPara, bidBillNo);
						}else{
							tb.status = 2;
							TransferBatches.updateStatus(tb.id,2,error);  //0.未处理，1.处理中，2.处理成功，3.处理失败
						}
					}else{  //第三方无记录，重新转账
						flag = false;
						tb.status = 0;
						TransferBatches.updateStatus(tb.id,0,error);  //0.未处理，1.处理中，2.处理成功，3.处理失败
					}
				}
			}
			
			//转账异常
			for(TransferBatches tb : list){
				if(tb.status == 3){  //审核失败，下次重新审核
					TransferBatches.updateStatus(tb.id,0,error);  
					flag = false;
				}
			}
			
			if(!flag){
				String pErrCode = "MG00001F";
				String pErrMsg = "转账异常，请重新转账";
				String p3DesXmlPara = LoanUtil.parseMapToXml(result);
				p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);
				String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);
				
				render("loan/LoanController/loanReturn.html", pWebUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
			}
		}
	}

	/**
	 * 转账：投资返回信息（异步）
	 * 
	 * @return
	 */
	public static void loanTransferNotify(String LoanNoList,
			String LoanNoListFail, String PlatformMoneymoremore,
			String AuditType, String RandomTimeStamp, String Remark1,
			String Remark2, String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		ErrorInfo error = new ErrorInfo();
		
		Logger.info("======第三方支付回调转账（投资）（异步）接口，执行开始========");
		
		LoanUtil.printLoggerFromLoan(ResultCode, Message, ReturnTimes, LoanNoList,
				LoanNoListFail, PlatformMoneymoremore, RandomTimeStamp,
				AuditType, Remark1, Remark2, Remark3, ResultCode, Message,
				ReturnTimes, SignInfo);

		if(StringUtils.isNotBlank(LoanNoListFail)){  //有问题的流水号
			Logger.info("======投资时，有问题的流水号：%s", LoanNoListFail);
		}

		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}

		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}

		if (Remark3 == null) {
			Remark3 = "";
		}
		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}

		String publickey = LoanConstants.publicKey;

		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = LoanNoList + LoanNoListFail + PlatformMoneymoremore
				+ AuditType + RandomTimeStamp + Remark1 + Remark2 + Remark3
				+ ResultCode;

		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,publickey);

		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}

		String pWebUrl = new String(Codec.decodeBASE64(Remark1));
		String pS2SUrl = new String(Codec.decodeBASE64(Remark2));
		String[] arrayRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String pMerBillNo = arrayRemark3[0];
		String pTransferType = arrayRemark3[1];
		String platformMemberId = arrayRemark3[2];
		String batchId = arrayRemark3[4];
		
		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		String pErrMsg = Message;
		
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("pMerBillNo", pMerBillNo);
		result.put("pTransferType", pTransferType);
		result.put("pMemo1", platformMemberId);
		
		// 如果是分批转账，则更新分批处理结果
		if(!"-1".equals(batchId)){
			TransferBatches.updateStatus(Long.parseLong(batchId.trim()),"88".equals(ResultCode)?2:3,error);
			if(error.code < 1){
				return;
			}
		}
		
		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F";
		} else {
			pErrCode = "MG00001F";
		}
		
		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);
		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);

		LoanUtil.printLoggerToP2P(pWebUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);
		Logger.info("======第三方支付回调转账（投资）（异步）接口，结束========");
		
		//转账失败时，通知P2P（异步）
		if("MG00001F".equals(pErrCode)){
			DealDetail.updateEvent2(pMerBillNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
			loanNotify(pS2SUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
		}else{
			//转账成功时，继续转账或解冻投资金额，停止第三方异步
			renderText("SUCCESS");
		}
		
	}
	
	/**
	 * 代偿返回信息
	 * 
	 * @return
	 */
	public static void loanCompensateReturn(String LoanJsonList,
			String Action, String PlatformMoneymoremore,
			String RandomTimeStamp, String Remark1, String Remark2,
			String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		ErrorInfo error = new ErrorInfo();
		
		Logger.info("======第三方支付回调代偿接口，执行开始========");

		LoanJsonList = LoanUtil.UrlDecoder(LoanJsonList, "utf-8");
		
		LoanUtil.printLoggerFromLoan(ResultCode, Message, ReturnTimes, LoanJsonList, Action,
				PlatformMoneymoremore, RandomTimeStamp, Remark1, Remark2,
				Remark3, ResultCode, Message, ReturnTimes, SignInfo);

		if (Action == null) {
			Action = "";
		}

		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}

		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}

		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}

		if (StringUtils.isBlank(Remark3)) {
			Logger.info("======Remark3存放的信息丢失========");
			Remark3 = "";
		}

		String publickey = LoanConstants.publicKey;

		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = LoanJsonList + PlatformMoneymoremore + Action
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ResultCode;

		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);

		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}

		String pWebUrl = new String(Codec.decodeBASE64(Remark1));
		String[] arrayRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String merBillNo = arrayRemark3[0];
		String memo1 = arrayRemark3[1];
		String memo3 = arrayRemark3[2];
		String batchId = arrayRemark3[3].trim();
		String platformId = arrayRemark3[4].trim();
		String bidNo = arrayRemark3[5].trim();
		String period = arrayRemark3[6].trim();
		String batchNo = arrayRemark3[7].trim();
		//分批操作：bidBillNo为每一批的唯一标识，生成法则; 标的号  + 分隔符  + 还款期号
		String bidBillNo = bidNo + LoanConstants.BILLTOKEN + period;

		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("pMerBillNo", merBillNo);
		result.put("pMemo1", memo1);
		result.put("pMemo3", memo3);
		
		if("18".equals(ResultCode)){  //重复订单，进行对账
			//每一笔转账流水号生成法则：① 分批转账：还款流水号  + 分隔符 + 还款期号  + 批号 + 循环i；②一次转账：还款流水号  + 分隔符 + 循环i
			String order =  merBillNo + LoanConstants.BILLTOKEN + ("-1".equals(batchId)? "" :(period + batchNo)) + 0;
			String resultStr = LOAN.loanOrderQuery(LoanConstants.queryTransfer, "",order)[1];
			if(StringUtils.isNotBlank(resultStr)&&(resultStr.startsWith("[") || resultStr.startsWith("{"))){
				String actState = LOAN.getLoanMap(resultStr).get("ActState").toString(); // 0.未操作1.已通过2.已退回3.自动通过
				if (!"0".equals(actState)) {
					// 还款成功
					ResultCode = "88";
				}
			}
		}
		
		if(!"-1".equals(batchId)){  //更新分批处理结果
			TransferBatches.updateStatus(Long.parseLong(batchId),"88".equals(ResultCode)?2:3,error);
			if(error.code <1){
				return;
			}
		}
		
		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 成功
			DealDetail.updateStatus(merBillNo + (!"-1".equals(batchId)?batchId:""));
			
			if(!"-1".equals(batchId)){	//分批转账			
				batchTransferCB(error, LoanConstants.TRANSFER, platformId, pMerCode, bidBillNo, period, merBillNo, result, pWebUrl);
			}
		} else {
			pErrCode = "MG00001F"; // 失败
		}
		String pErrMsg = Message;


		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);

		LoanUtil.printLoggerToP2P(pWebUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);

		Logger.info("======第三方支付回调代偿接口，结束========");

		DealDetail.updateEvent2(merBillNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		render("loan/LoanController/loanReturn.html", pWebUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}
	
	/**
	 * 代偿返回信息(异步)
	 * 
	 * @return
	 */
	public static void loanCompensateNotify(String LoanJsonList,
			String Action, String PlatformMoneymoremore,
			String RandomTimeStamp, String Remark1, String Remark2,
			String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		Logger.info("======第三方支付回调代偿接口(异步)，执行开始========");

		LoanJsonList = LoanUtil.UrlDecoder(LoanJsonList, "utf-8");
		
		LoanUtil.printLoggerFromLoan(ResultCode, Message, ReturnTimes, LoanJsonList, Action,
				PlatformMoneymoremore, RandomTimeStamp, Remark1, Remark2,
				Remark3, ResultCode, Message, ReturnTimes, SignInfo);

		if (Action == null) {
			Action = "";
		}

		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}

		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}

		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}

		if (StringUtils.isBlank(Remark3)) {
			Logger.info("======Remark3存放的信息丢失========");
			Remark3 = "";
		}

		String publickey = LoanConstants.publicKey;

		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = LoanJsonList + PlatformMoneymoremore + Action
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ResultCode;

		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);

		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}

		String pS2SUrl = new String(Codec.decodeBASE64(Remark2));
		String[] arrayRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String merBillNo = arrayRemark3[0];
		String memo1 = arrayRemark3[1];
		String memo3 = arrayRemark3[2];

		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		if("18".equals(ResultCode)){  //重复订单，进行对账
			String order = merBillNo + LoanConstants.BILLTOKEN + 0;
			String resultStr = LOAN.loanOrderQuery(LoanConstants.queryTransfer, "",order)[1];
			if(StringUtils.isNotBlank(resultStr)&&(resultStr.startsWith("[") || resultStr.startsWith("{"))){
				String actState = LOAN.getLoanMap(resultStr).get("ActState").toString(); // 0.未操作1.已通过2.已退回3.自动通过
				if (!"0".equals(actState)) {
					// 还款成功
					ResultCode = "88";
				}
			}
		}
		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 成功
			
			DealDetail.updateStatus(merBillNo);
		} else {
			pErrCode = "MG00001F"; // 失败
		}
		String pErrMsg = Message;

		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("pMerBillNo", merBillNo);
		result.put("pMemo1", memo1);
		result.put("pMemo3", memo3);

		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);

		LoanUtil.printLoggerToP2P(pS2SUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);

		Logger.info("======第三方支付回调代偿接口(异步)，结束========");

		DealDetail.updateEvent2(merBillNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		loanNotify(pS2SUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}
	
	/**
	 * 代偿还款返回信息
	 * 
	 * @return
	 */
	public static void loanCompensateRepaymentReturn(String LoanJsonList,
			String Action, String PlatformMoneymoremore,
			String RandomTimeStamp, String Remark1, String Remark2,
			String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		Logger.info("======第三方支付回调代偿还款接口，执行开始========");
		
		LoanJsonList = LoanUtil.UrlDecoder(LoanJsonList, "utf-8");
		
		LoanUtil.printLoggerFromLoan(ResultCode, Message, ReturnTimes, LoanJsonList, Action,
				PlatformMoneymoremore, RandomTimeStamp, Remark1, Remark2,
				Remark3, ResultCode, Message, ReturnTimes, SignInfo);
		
		if (Action == null) {
			Action = "";
		}
		
		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}
		
		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}
		
		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}
		
		if (StringUtils.isBlank(Remark3)) {
			Logger.info("======Remark3存放的信息丢失========");
			Remark3 = "";
		}
		
		String publickey = LoanConstants.publicKey;
		
		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = LoanJsonList + PlatformMoneymoremore + Action
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ResultCode;
		
		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);
		
		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}
		
		String pWebUrl = new String(Codec.decodeBASE64(Remark1));
		String[] arrayRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String merBillNo = arrayRemark3[0];
		String pTransferType = arrayRemark3[1];
		String memo1 = arrayRemark3[2];
		String memo3 = arrayRemark3[3];
		
		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		if("18".equals(ResultCode)){  //重复订单，进行对账
			String resultStr = LOAN.loanOrderQuery(LoanConstants.queryTransfer, "",merBillNo)[1];
			if(StringUtils.isNotBlank(resultStr)&&(resultStr.startsWith("[") || resultStr.startsWith("{"))){
				String actState = LOAN.getLoanMap(resultStr).get("ActState").toString(); // 0.未操作1.已通过2.已退回3.自动通过
				if (!"0".equals(actState)) {
					// 还款成功
					ResultCode = "88";
				}
			}
		}
		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 成功
			
			DealDetail.updateStatus(merBillNo);
		} else {
			pErrCode = "MG00001F"; // 失败
		}
		String pErrMsg = Message;
		
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("pMerBillNo", merBillNo);
		result.put("pTransferType", pTransferType);
		result.put("pMemo1", memo1);
		result.put("pMemo3", memo3);
		
		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);
		
		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);
		
		LoanUtil.printLoggerToP2P(pWebUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);
		
		Logger.info("======第三方支付回调代偿还款接口，结束========");
		
		DealDetail.updateEvent2(merBillNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		render("loan/LoanController/loanReturn.html", pWebUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}
	
	/**
	 * 代偿还款返回信息(异步)
	 * 
	 * @return
	 */
	public static void loanCompensateRepaymentNotify(String LoanJsonList,
			String Action, String PlatformMoneymoremore,
			String RandomTimeStamp, String Remark1, String Remark2,
			String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		Logger.info("======第三方支付回调代偿还款接口(异步)，执行开始========");
		
		LoanJsonList = LoanUtil.UrlDecoder(LoanJsonList, "utf-8");
		
		LoanUtil.printLoggerFromLoan(ResultCode, Message, ReturnTimes, LoanJsonList, Action,
				PlatformMoneymoremore, RandomTimeStamp, Remark1, Remark2,
				Remark3, ResultCode, Message, ReturnTimes, SignInfo);
		
		if (Action == null) {
			Action = "";
		}
		
		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}
		
		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}
		
		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}
		
		if (StringUtils.isBlank(Remark3)) {
			Logger.info("======Remark3存放的信息丢失========");
			Remark3 = "";
		}
		
		String publickey = LoanConstants.publicKey;
		
		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = LoanJsonList + PlatformMoneymoremore + Action
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ResultCode;
		
		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);
		
		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}
		
		String pS2SUrl = new String(Codec.decodeBASE64(Remark2));
		String[] arrayRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String merBillNo = arrayRemark3[0];
		String pTransferType = arrayRemark3[1];
		String memo1 = arrayRemark3[2];
		String memo3 = arrayRemark3[3];
		
		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		if("18".equals(ResultCode)){  //重复订单，进行对账
			String resultStr = LOAN.loanOrderQuery(LoanConstants.queryTransfer, "",merBillNo)[1];
			if(StringUtils.isNotBlank(resultStr)&&(resultStr.startsWith("[") || resultStr.startsWith("{"))){
				String actState = LOAN.getLoanMap(resultStr).get("ActState").toString(); // 0.未操作1.已通过2.已退回3.自动通过
				if (!"0".equals(actState)) {
					// 还款成功
					ResultCode = "88";
				}
			}
		}
		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 成功
			
			DealDetail.updateStatus(merBillNo);
		} else {
			pErrCode = "MG00001F"; // 失败
		}
		String pErrMsg = Message;
		
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("pMerBillNo", merBillNo);
		result.put("pTransferType", pTransferType);
		result.put("pMemo1", memo1);
		result.put("pMemo3", memo3);
		
		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);
		
		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);
		
		LoanUtil.printLoggerToP2P(pS2SUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);
		
		Logger.info("======第三方支付回调代偿还款接口(异步)，结束========");
		
		DealDetail.updateEvent2(merBillNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		loanNotify(pS2SUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}

	/**
	 * 解冻保证金返回信息
	 * 
	 * @return
	 */
	public static void loanGuaranteeUnfreezeReturn(String LoanNoList,
			String LoanNoListFail, String PlatformMoneymoremore,
			String AuditType, String RandomTimeStamp, String Remark1,
			String Remark2, String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {

		Logger.info("======第三方支付回调解冻保证金接口，执行开始========");

		LoanUtil.printLoggerFromLoan(ResultCode, Message, ReturnTimes, LoanNoList,
				LoanNoListFail, PlatformMoneymoremore, RandomTimeStamp,
				AuditType, Remark1, Remark2, Remark3, ResultCode, Message,
				ReturnTimes, SignInfo);

		if(StringUtils.isNotBlank(LoanNoListFail)){  //有问题的流水号
			Logger.info("======解冻保证金时，有问题的流水号：%s", LoanNoListFail);
		}
		
		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}

		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}

		if (Remark3 == null) {
			Remark3 = "";
		}
		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}

		String publickey = LoanConstants.publicKey;

		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = LoanNoList + LoanNoListFail + PlatformMoneymoremore
				+ AuditType + RandomTimeStamp + Remark1 + Remark2 + Remark3
				+ ResultCode;

		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);

		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}

		String pWebUrl = new String(Codec.decodeBASE64(Remark1));
		String[] arrayRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String pMerBillNo = arrayRemark3[0];
		String pTransferType = arrayRemark3[1];
		String platformMemberId = arrayRemark3[2];
		
		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		
		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F";

		} else {
			pErrCode = "MG00001F";
		}

		String pErrMsg = Message;
		
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("pMerBillNo", pMerBillNo);
		result.put("pTransferType", pTransferType);
		result.put("pMemo1", platformMemberId);
		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,
				Constants.ENCRYPTION_KEY);

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara
				+ Constants.ENCRYPTION_KEY);

		LoanUtil.printLoggerToP2P(pWebUrl, pMerCode, pErrCode, pErrMsg,
				p3DesXmlPara, pSign);

		Logger.info("======第三方支付回调解冻保证金接口，结束========");
		Logger.info("======第三方支付回调转账（投资）接口，结束========");

		DealDetail.updateEvent2(pMerBillNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		render("loan/LoanController/loanReturn.html", pWebUrl, pMerCode,
				pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}
	
	/**
	 * 解冻保证金返回信息（异步）
	 * 
	 * @return
	 */
	public static void loanGuaranteeUnfreezeNotify(String LoanNoList,
			String LoanNoListFail, String PlatformMoneymoremore,
			String AuditType, String RandomTimeStamp, String Remark1,
			String Remark2, String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		
		Logger.info("======第三方支付回调解冻保证金（异步）接口，执行开始========");
		
		LoanUtil.printLoggerFromLoan(ResultCode, Message, ReturnTimes, LoanNoList,
				LoanNoListFail, PlatformMoneymoremore, RandomTimeStamp,
				AuditType, Remark1, Remark2, Remark3, ResultCode, Message,
				ReturnTimes, SignInfo);
		
		if(StringUtils.isNotBlank(LoanNoListFail)){  //有问题的流水号
			Logger.info("======解冻保证金（异步）时，有问题的流水号：%s", LoanNoListFail);
		}
		
		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}
		
		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}
		
		if (Remark3 == null) {
			Remark3 = "";
		}
		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}
		
		String publickey = LoanConstants.publicKey;
		
		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = LoanNoList + LoanNoListFail + PlatformMoneymoremore
				+ AuditType + RandomTimeStamp + Remark1 + Remark2 + Remark3
				+ ResultCode;
		
		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);
		
		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}
		
		String pS2SUrl = new String(Codec.decodeBASE64(Remark2));
		String[] arrayRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String pMerBillNo = arrayRemark3[0];
		String pTransferType = arrayRemark3[1];
		String platformMemberId = arrayRemark3[2];
		
		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		
		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F";
			
		} else {
			pErrCode = "MG00001F";
		}
		
		String pErrMsg = Message;
		
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("pMerBillNo", pMerBillNo);
		result.put("pTransferType", pTransferType);
		result.put("pMemo1", platformMemberId);
		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,
				Constants.ENCRYPTION_KEY);
		
		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara
				+ Constants.ENCRYPTION_KEY);
		
		LoanUtil.printLoggerToP2P(pS2SUrl, pMerCode, pErrCode, pErrMsg,
				p3DesXmlPara, pSign);
		
		Logger.info("======第三方支付回调解冻保证金（异步）接口，结束========");
		Logger.info("======第三方支付回调转账（投资）（异步）接口，结束========");
		
		DealDetail.updateEvent2(pMerBillNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		loanNotify(pS2SUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}
	
	/**
	 * 还款返回信息
	 * 
	 * @return
	 */
	public static void loanRepaymentReturn(String LoanJsonList,
			String Action, String PlatformMoneymoremore,
			String RandomTimeStamp, String Remark1, String Remark2,
			String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		ErrorInfo error = new ErrorInfo();
		Logger.info("======第三方支付回调还款接口，执行开始========");

		LoanJsonList = LoanUtil.UrlDecoder(LoanJsonList, "utf-8");
		
		LoanUtil.printLoggerFromLoan(ResultCode, Message, ReturnTimes, LoanJsonList, Action,
				PlatformMoneymoremore, RandomTimeStamp, Remark1, Remark2,
				Remark3, ResultCode, Message, ReturnTimes, SignInfo);

		if (Action == null) {
			Action = "";
		}

		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}

		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}

		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}

		if (Remark3 == null) {
			Remark3 = "";
		}

		String publickey = LoanConstants.publicKey;

		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = LoanJsonList + PlatformMoneymoremore + Action
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ResultCode;

		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);

		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}

//		String pWebUrl = new String(Codec.decodeBASE64(Remark1));
		String pWSUrl = new String(Codec.decodeBASE64(Remark1));
		String[] arrayRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String merBillNo = arrayRemark3[0].trim();
		String memo3 = arrayRemark3[1].trim();
		String batchId = arrayRemark3[2].trim();
		String platformId = arrayRemark3[3].trim();
		String bidNo = arrayRemark3[4].trim();
		String period = arrayRemark3[5].trim();
		String batchNo = arrayRemark3[6].trim();
		//分批操作：bidBillNo为每一批的唯一标识，生成法则; 标的号  + 分隔符  + 还款期号
		String bidBillNo = bidNo + LoanConstants.BILLTOKEN + period;

		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		String pErrMsg = Message;
		
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("pMerBillNo", merBillNo);
		result.put("pMemo3", memo3); 
		
		if("18".equals(ResultCode)){  //重复订单，进行对账
			//每一笔转账流水号生成法则：① 分批转账：还款流水号  + 分隔符 + 还款期号  + 批号 + 循环i；②一次转账：还款流水号  + 分隔符 + 循环i
			String OrderNo =  merBillNo + LoanConstants.BILLTOKEN + ("-1".equals(batchId)? "" :(period + batchNo)) + 0;
			String resultStr = LOAN.loanOrderQuery(LoanConstants.queryTransfer, "",OrderNo)[1];
			if(StringUtils.isNotBlank(resultStr)&&(resultStr.startsWith("[") || resultStr.startsWith("{"))){
				String actState = LOAN.getLoanMap(resultStr).get("ActState").toString(); // 0.未操作1.已通过2.已退回3.自动通过
				if (!"0".equals(actState)) {
					// 还款成功
					ResultCode = "88";
				}
			}
		}
		
		if(!"-1".equals(batchId)){  //更新分批处理结果
			TransferBatches.updateStatus(Long.parseLong(batchId),"88".equals(ResultCode)?2:3,error);
			if(error.code <1){
				return;
			}
		}
		
		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 成功
			DealDetail.updateStatus(merBillNo + (!"-1".equals(batchId)?batchId:""));
			
			if(!"-1".equals(batchId)){	//分批转账			
				batchTransferCB(error, LoanConstants.REPAYMENT, platformId, pMerCode, bidBillNo, period, merBillNo, result, pWSUrl);
			}
			
		} else {
			pErrCode = "MG00001F"; // 失败
		}

		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara+ Constants.ENCRYPTION_KEY);

//		LoanUtil.printLoggerToP2P(pWebUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);
		LoanUtil.printLoggerToP2P(pWSUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);

		Logger.info("======第三方支付回调还款接口，结束========");

		DealDetail.updateEvent2(merBillNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		Map<String, Object> args = handlerService(pWSUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);
		
		render("loan/LoanController/loanReturnAfterWS.html", args);
	}
	
	/**
	 * 转账成功后继续转账
	 * @param error
	 * @param platformId
	 * @param pMerCode
	 * @param bidBillNo
	 * @param period
	 * @param merBillNo
	 * @param result
	 * @param pWSUrl
	 */
	private static void batchTransferCB(ErrorInfo error, int type, String platformId, String pMerCode, String bidBillNo,
			String period, String merBillNo, LinkedHashMap<String, String> result, String url) {
		TransferBatches batch = TransferBatches.pollRemainBill(bidBillNo);  
		if(batch != null){  //分批处理
		
			JSONObject jsonObj = JSONObject.fromObject(batch.transferBillNos);
			jsonObj.put("batchId", batch.id);
			jsonObj.put("batchNo", batch.batchNo);
			jsonObj.put("period", period);  //还款
			jsonObj.put("periods", period);  //代偿
			jsonObj.put("pWSUrl", url);  //还款，p2p业务接口地址
			jsonObj.put("pWebUrl", url);  //代偿
			
			String strXml = Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null);
			Logger.info("继续转账：p3DesXmlPara = %s", strXml);
			String arg3DesXmlPara = Encrypt.encrypt3DES(strXml, Constants.ENCRYPTION_KEY);

			//继续转账
			getInstance().loan(type, 0L, "", Long.parseLong(platformId), "",LoanConstants.argMerCode, arg3DesXmlPara,arg3DesXmlPara, "");
		}
		
		List<TransferBatches> list =  TransferBatches.queryByBidBillNo(bidBillNo);
		if(list != null){  //分批转账时
			boolean flag = true;  //是否转账成功
			//补单
			for(TransferBatches tb : list){
				if(tb.status == 1){  //掉单，补单
					String OrderNo = merBillNo+ LoanConstants.BILLTOKEN + period + tb.batchNo + 0;
					String response = LOAN.loanOrderQuery("", "",OrderNo)[1];
					if (StringUtils.isNotBlank(response)&& (response.startsWith("[") || response.startsWith("{"))) {
						
						Map<String, String> resultFromLoan = LOAN.getLoanMap(response);

						String actState = resultFromLoan.get("ActState").toString(); // 0.未操作,1.已通过,2.已退回,3.自动通过
						if("0".equals(actState)){  //第三方未转账或转账失败
							flag = false;
							tb.status = 0;  //修该状态为未处理，下次继续转账
							TransferBatches.updateStatus(tb.id,0,error);  //0.未处理，1.处理中，2.处理成功，3.处理失败
							
							JSONObject jsonObj = JSONObject.fromObject(tb.transferBillNos);
							jsonObj.put("batchId", tb.id);
							jsonObj.put("batchNo", tb.batchNo);
							jsonObj.put("period", period);  //还款
							jsonObj.put("periods", period);  //代偿
							jsonObj.put("pWSUrl", url);  //还款，p2p业务接口地址
							jsonObj.put("pWebUrl", url);  //代偿，p2p业务接口地址
							
							String strXml = Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null);
							Logger.info("重新转账：p3DesXmlPara = %s", strXml);
							String arg3DesXmlPara = Encrypt.encrypt3DES(strXml, Constants.ENCRYPTION_KEY);

							//重新转账
							getInstance().loan(type, 0L, "", Long.parseLong(platformId), "",LoanConstants.argMerCode, arg3DesXmlPara,arg3DesXmlPara, "");
						}else{
							tb.status = 2;
							TransferBatches.updateStatus(tb.id,2,error);  //0.未处理，1.处理中，2.处理成功，3.处理失败
						}
					}else{  //第三方无记录，重新转账
						flag = false;
						tb.status = 0;
						TransferBatches.updateStatus(tb.id,0,error);  //0.未处理，1.处理中，2.处理成功，3.处理失败
					}
				}
			}
			
			//转账异常
			for(TransferBatches tb : list){
				if(tb.status == 3){  //转账失败，下次重新转账
					TransferBatches.updateStatus(tb.id,0,error);  //0.未处理，1.处理中，2.处理成功，3.处理失败
					flag = false;
				}
			}
			
			if(!flag){
				String pErrCode = "MG00001F";
				String pErrMsg = "转账异常，请重新转账";
				String p3DesXmlPara = LoanUtil.parseMapToXml(result);
				p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);
				String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);
				
				if(type == LoanConstants.TRANSFER){
					render("loan/LoanController/loanReturn.html", url, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
				}
				
				if(type == LoanConstants.REPAYMENT){
					Map<String, Object> args = handlerService(url, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);
					render("loan/LoanController/loanReturnAfterWS.html", args);
				}	
			}
		}
	}

	/**
	 * 还款返回信息（异步）
	 * 
	 * @return
	 */
	public static void loanRepaymentNotify(String LoanJsonList,
			String Action, String PlatformMoneymoremore,
			String RandomTimeStamp, String Remark1, String Remark2,
			String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		ErrorInfo error = new ErrorInfo();
		Logger.info("======第三方支付回调还款（异步）接口，执行开始========");
		
		LoanJsonList = LoanUtil.UrlDecoder(LoanJsonList, "utf-8");
		
		LoanUtil.printLoggerFromLoan(ResultCode, Message, ReturnTimes, LoanJsonList, Action,
				PlatformMoneymoremore, RandomTimeStamp, Remark1, Remark2,
				Remark3, ResultCode, Message, ReturnTimes, SignInfo);

		if (Action == null) {
			Action = "";
		}

		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}

		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}

		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}

		if (Remark3 == null) {
			Remark3 = "";
		}

		String publickey = LoanConstants.publicKey;

		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = LoanJsonList + PlatformMoneymoremore + Action
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ResultCode;

		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);

		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}

		String pS2SUrl = new String(Codec.decodeBASE64(Remark2));
		String[] arrayRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String merBillNo = arrayRemark3[0].trim();
		String memo3 = arrayRemark3[1].trim();
		String batchId = arrayRemark3[2].trim();
		String bidNo = arrayRemark3[4].trim();
		String period = arrayRemark3[5].trim();
		String batchNo = arrayRemark3[6].trim();
		//分批操作：bidBillNo为每一批的唯一标识，生成法则; 标的号  + 分隔符  + 还款期号
		String bidBillNo = bidNo + LoanConstants.BILLTOKEN + period;

		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		String pErrMsg = Message;
		
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("pMerBillNo", merBillNo);
		result.put("pMemo3", memo3); 
		
		if("18".equals(ResultCode)){  //重复订单，进行对账
			//每一笔转账流水号生成法则：① 分批转账：还款流水号  + 分隔符 + 还款期号  + 批号 + 循环i；②一次转账：还款流水号  + 分隔符 + 循环i
			String OrderNo =  merBillNo + LoanConstants.BILLTOKEN + ("-1".equals(batchId)? "" :(period + batchNo)) + 0;
			String resultStr = LOAN.loanOrderQuery(LoanConstants.queryTransfer, "",OrderNo)[1];
			if(StringUtils.isNotBlank(resultStr)&&(resultStr.startsWith("[") || resultStr.startsWith("{"))){
				String actState = LOAN.getLoanMap(resultStr).get("ActState").toString(); // 0.未操作1.已通过2.已退回3.自动通过
				if (!"0".equals(actState)) {
					// 还款成功
					ResultCode = "88";
				}
			}
		}
		
		if(!"-1".equals(batchId)){  //更新分批处理结果
			TransferBatches.updateStatus(Long.parseLong(batchId),"88".equals(ResultCode)?2:3,error);
			if(error.code <1){
				return;
			}
		}
		
		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 成功
			DealDetail.updateStatus(merBillNo + (!"-1".equals(batchId)?batchId:""));
			
		} else {
			pErrCode = "MG00001F"; // 失败
		}

		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara+ Constants.ENCRYPTION_KEY);

		LoanUtil.printLoggerToP2P(pS2SUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);
		
		Logger.info("======第三方支付回调还款（异步）接口，结束========");
		
		DealDetail.updateEvent2(merBillNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		//分批还款,成功时
		if(!"-1".equals(batchId) && "MG00000F".equals(pErrCode)){
			List<t_transfer_batches> list = t_transfer_batches.find("bid_bill_no = ? and status != 2", bidBillNo).fetch();
			if(list == null || list.size()<=0 || list.get(0) == null){  //  所有转账都成功，通知p2p
				loanNotify(pS2SUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
			}else{
				renderText("SUCCESS");  //停止第三方异步通知
			}
		}
		
		//非分批转账，正常通知
		loanNotify(pS2SUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
		
	}

	/**
	 * 提现银行卡绑定返回信息
	 * 
	 * @return
	 */
	public static void loanBondCardNoReturn(String MoneymoremoreId,
			String PlatformMoneymoremore, String Action, String CardType,
			String BankCode, String CardNo, String BranchBankName,
			String Province, String City, String WithholdBeginDate,
			String WithholdEndDate, String SingleWithholdLimit,
			String TotalWithholdLimit, String RandomTimeStamp, String Remark1,
			String Remark2, String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		Logger.info("======第三方支付回调提现银行卡绑定接口，执行开始========");

		LoanUtil.printLoggerFromLoan(ResultCode, Message,ReturnTimes, MoneymoremoreId,
				PlatformMoneymoremore, Action, BankCode, CardNo,
				BranchBankName, Province, City, WithholdBeginDate,
				WithholdEndDate, SingleWithholdLimit, TotalWithholdLimit,
				RandomTimeStamp, Remark1, Remark2, Remark3, ResultCode,
				Message, ReturnTimes, SignInfo);

		if (WithholdBeginDate == null) {
			WithholdBeginDate = "";
		}
		if (WithholdEndDate == null) {
			WithholdEndDate = "";
		}

		if (SingleWithholdLimit == null) {
			SingleWithholdLimit = "";
		}
		if (TotalWithholdLimit == null) {
			WithholdEndDate = "";
		}

		if (Remark1 == null) {
			Remark1 = "";
		}

		if (Remark2 == null) {
			Remark2 = "";
		}

		if (Remark3 == null) {
			Remark3 = "";
		}

		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}

		String publickey = LoanConstants.publicKey;
		String privatekey = LoanConstants.privateKeyPKCS8;

		RsaHelper rsa = RsaHelper.getInstance();

		if (StringUtils.isNotBlank(CardNo)) {
			CardNo = rsa.decryptData(CardNo, privatekey);
			if (StringUtils.isBlank(CardNo)) {
				CardNo = "";
			}
		}

		String dataStr = MoneymoremoreId + PlatformMoneymoremore + Action
				+ CardType + BankCode + CardNo + BranchBankName + Province
				+ City + WithholdBeginDate + WithholdEndDate
				+ SingleWithholdLimit + TotalWithholdLimit + RandomTimeStamp
				+ Remark1 + Remark2 + Remark3 + ResultCode;

		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);

		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}

//		String pWebUrl = new String(Codec.decodeBASE64(Remark1));
		String pWSUrl = new String(Codec.decodeBASE64(Remark1));
		String pS2SUrl = new String(Codec.decodeBASE64(Remark2));
		String[] arrayRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String platformMemberId = arrayRemark3[0];
		String pMerBillNo = arrayRemark3[1];
		String pTrdAmt = arrayRemark3[2];
		String pIpsFeeType = arrayRemark3[3];
		String pMemo3 = arrayRemark3[4];
		String platformId = arrayRemark3[5];
		String pMerFee = arrayRemark3[6];
		
		String pMerCode = PlatformMoneymoremore;

		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("pMemo1", platformMemberId); 
		result.put("pMemo3", pMemo3);
		result.put("pMerBillNo", pMerBillNo);  //提现流水号

		if ("88".equals(ResultCode) || "18".equals(ResultCode)) { // 绑卡成功，请求提现接口
			// 保存银行卡号3DS+密钥加密
			Member.updateCardNo(
					Encrypt.encrypt3DES(CardNo, Constants.ENCRYPTION_KEY),
					Long.parseLong(platformMemberId.trim()),
					Long.parseLong(platformId.trim()));

			Logger.info("======提现银行卡绑定成功========");
			Logger.info("======第三方支付回调提现银行卡绑定接口，结束========");

			result.put("pTrdAmt", pTrdAmt);  //提现金额
			result.put("pIpsFeeType", pIpsFeeType);  //手续费承担类型
			result.put("pMerFee", pMerFee);  //手续费

//			result.put("pWebUrl", pWebUrl);
			result.put("pWSUrl", pWSUrl);
			result.put("pS2SUrl", pS2SUrl);
			
			p3DesXmlPara = LoanUtil.parseMapToXml(result);
			Logger.info("绑卡后提现：p3DesXmlPara = %s", p3DesXmlPara);
			p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);

			getInstance().loan(LoanConstants.WITHDRAWAL,
					Long.parseLong(platformMemberId.trim()), "", Long.parseLong(platformId.trim()), "",
					LoanConstants.argMerCode, p3DesXmlPara,p3DesXmlPara, MoneymoremoreId);
		}

		// 绑卡失败，则提示提现失败
		String pErrCode = "MG00001F";
		Logger.info("======提现银行卡绑定失败========");
		Logger.info("======第三方支付回调提现银行卡绑定接口，结束========");

		String pErrMsg = Message;

		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);

		LoanUtil.printLoggerToP2P(pWSUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);

		Logger.info("======第三方支付回调提现接口，结束========");

		DealDetail.updateEvent2(pMerBillNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		Map<String, Object> args = handlerService(pWSUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);
		
		render("loan/LoanController/loanReturnAfterWS.html", args);
	}
	
	/**
	 * 提现银行卡绑定返回信息（异步）
	 * 
	 * @return
	 */
	public static void loanBondCardNoNotify(String MoneymoremoreId,
			String PlatformMoneymoremore, String Action, String CardType,
			String BankCode, String CardNo, String BranchBankName,
			String Province, String City, String WithholdBeginDate,
			String WithholdEndDate, String SingleWithholdLimit,
			String TotalWithholdLimit, String RandomTimeStamp, String Remark1,
			String Remark2, String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		Logger.info("======第三方支付回调提现银行卡绑定（异步）接口，执行开始========");
		
		LoanUtil.printLoggerFromLoan(ResultCode, Message,ReturnTimes, MoneymoremoreId,
				PlatformMoneymoremore, Action, BankCode, CardNo,
				BranchBankName, Province, City, WithholdBeginDate,
				WithholdEndDate, SingleWithholdLimit, TotalWithholdLimit,
				RandomTimeStamp, Remark1, Remark2, Remark3, ResultCode,
				Message, ReturnTimes, SignInfo);
		
		if (WithholdBeginDate == null) {
			WithholdBeginDate = "";
		}
		if (WithholdEndDate == null) {
			WithholdEndDate = "";
		}
		
		if (SingleWithholdLimit == null) {
			SingleWithholdLimit = "";
		}
		if (TotalWithholdLimit == null) {
			WithholdEndDate = "";
		}
		
		if (Remark1 == null) {
			Remark1 = "";
		}
		
		if (Remark2 == null) {
			Remark2 = "";
		}
		
		if (Remark3 == null) {
			Remark3 = "";
		}
		
		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}
		
		String publickey = LoanConstants.publicKey;
		String privatekey = LoanConstants.privateKeyPKCS8;
		
		RsaHelper rsa = RsaHelper.getInstance();
		
		if (StringUtils.isNotBlank(CardNo)) {
			CardNo = rsa.decryptData(CardNo, privatekey);
			if (StringUtils.isBlank(CardNo)) {
				CardNo = "";
			}
		}
		
		String dataStr = MoneymoremoreId + PlatformMoneymoremore + Action
				+ CardType + BankCode + CardNo + BranchBankName + Province
				+ City + WithholdBeginDate + WithholdEndDate
				+ SingleWithholdLimit + TotalWithholdLimit + RandomTimeStamp
				+ Remark1 + Remark2 + Remark3 + ResultCode;
		
		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);
		
		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}
		
//		String pWebUrl = new String(Codec.decodeBASE64(Remark1));
		String pS2SUrl = new String(Codec.decodeBASE64(Remark2));
		String[] arrayRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String platformMemberId = arrayRemark3[0];
		String pMerBillNo = arrayRemark3[1];
		String pMemo3 = arrayRemark3[4];
		String platformId = arrayRemark3[5];
		
		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("pMemo1", platformMemberId); 
		result.put("pMemo3", pMemo3);
		result.put("pMerBillNo", pMerBillNo);  //提现流水号

		if ("88".equals(ResultCode) || "18".equals(ResultCode)) { // 绑卡成功，请求提现接口
			pErrCode = "MG00000F";
			// 保存银行卡号3DS+密钥加密
			Member.updateCardNo(
					Encrypt.encrypt3DES(CardNo, Constants.ENCRYPTION_KEY),
					Long.parseLong(platformMemberId.trim()),
					Long.parseLong(platformId.trim()));
		}else{
			pErrCode = "MG00001F";
		}

		String pErrMsg = Message;
		
		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);
		
		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara
				+ Constants.ENCRYPTION_KEY);
		
		LoanUtil.printLoggerToP2P(pS2SUrl, pMerCode, pErrCode, pErrMsg,
				p3DesXmlPara, pSign);
		
		Logger.info("======第三方支付回调提现（异步）接口，结束========");
		
		DealDetail.updateEvent2(pMerBillNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		if("MG00001F".equals(pErrCode)){
			//绑卡失败，直接通知p2p
			loanNotify(pS2SUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
		}else{
			//绑卡成功，请求提现，停止异步通知
			renderText("SUCCESS");
		}
	}

	/**
	 * 提现返回信息
	 * 
	 * @return
	 */
	public static void loanWithdrawReturn(String WithdrawMoneymoremore,
			String PlatformMoneymoremore, String LoanNo, String OrderNo,
			String Amount, String FeeMax, String FeeWithdraws,
			String FeePercent, String Fee, String FreeLimit, String FeeRate,
			String FeeSplitting, String RandomTimeStamp, String Remark1,
			String Remark2, String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		Logger.info("======第三方支付回调提现接口，执行开始========");

		LoanUtil.printLoggerFromLoan(ResultCode, Message,ReturnTimes,
				WithdrawMoneymoremore, PlatformMoneymoremore, LoanNo, OrderNo,
				Amount, FeeMax, FeeWithdraws, FeePercent, Fee, FreeLimit,
				FeeRate, FeeSplitting, RandomTimeStamp, Remark1, Remark2,
				Remark3, ResultCode, Message, ReturnTimes, SignInfo);

		if (FeeMax == null) {
			FeeMax = "";
		}

		if (FeeRate == null) {
			FeeRate = "";
		}

		if (FeeSplitting == null) {
			FeeSplitting = "";
		}

		if (Remark1 == null) {
			Remark1 = "";
		}

		if (Remark2 == null) {
			Remark2 = "";
		}

		if (Remark3 == null) {
			Remark3 = "";
		}

		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}

		String publickey = LoanConstants.publicKey;

		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = WithdrawMoneymoremore + PlatformMoneymoremore + LoanNo
				+ OrderNo + Amount + FeeMax + FeeWithdraws + FeePercent + Fee
				+ FreeLimit + FeeRate + FeeSplitting + RandomTimeStamp
				+ Remark1 + Remark2 + Remark3 + ResultCode;

		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,publickey);

		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}

//		String pWebUrl = new String(Codec.decodeBASE64(Remark1));
		String pWSUrl = new String(Codec.decodeBASE64(Remark1));
		String[] arraryRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String platformMemberId = arraryRemark3[0];
		String pMemo3 = arraryRemark3[1];
		
		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 成功
			result.put("serviceFee", FeeWithdraws);
			DealDetail.updateStatus(OrderNo);
		} else if("89".equals(ResultCode)){
			pErrCode = "MG00020F"; //提现回退
		} else{
			pErrCode = "MG00001F"; // 失败
		}
		
		String pErrMsg = Message;
		
		if(Convert.strToDouble(Amount, 0) <= 1){  //提现金额必须大于1元
			pErrMsg = "提现金额必须大于1元";
		}

		result.put("pMemo1", platformMemberId);
		result.put("pMemo3", pMemo3);
		result.put("pMerBillNo", OrderNo);

		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);

//		LoanUtil.printLoggerToP2P(pWebUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);
		LoanUtil.printLoggerToP2P(pWSUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);

		Logger.info("======第三方支付回调提现接口，结束========");

		DealDetail.updateEvent2(OrderNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		Map<String, Object> args = handlerService(pWSUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);
		
		render("loan/LoanController/loanReturnAfterWS.html", args);
	}
	
	/**
	 * 提现返回信息（异步）
	 * 
	 * @return
	 */
	public static void loanWithdrawNotify(String WithdrawMoneymoremore,
			String PlatformMoneymoremore, String LoanNo, String OrderNo,
			String Amount, String FeeMax, String FeeWithdraws,
			String FeePercent, String Fee, String FreeLimit, String FeeRate,
			String FeeSplitting, String RandomTimeStamp, String Remark1,
			String Remark2, String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		Logger.info("======第三方支付回调提现（异步）接口，执行开始========");
		
		LoanUtil.printLoggerFromLoan(ResultCode, Message,ReturnTimes,
				WithdrawMoneymoremore, PlatformMoneymoremore, LoanNo, OrderNo,
				Amount, FeeMax, FeeWithdraws, FeePercent, Fee, FreeLimit,
				FeeRate, FeeSplitting, RandomTimeStamp, Remark1, Remark2,
				Remark3, ResultCode, Message, ReturnTimes, SignInfo);
		
		if (FeeMax == null) {
			FeeMax = "";
		}
		
		if (FeeRate == null) {
			FeeRate = "";
		}
		
		if (FeeSplitting == null) {
			FeeSplitting = "";
		}
		
		if (Remark1 == null) {
			Remark1 = "";
		}
		
		if (Remark2 == null) {
			Remark2 = "";
		}
		
		if (Remark3 == null) {
			Remark3 = "";
		}
		
		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}
		
		String publickey = LoanConstants.publicKey;
		
		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = WithdrawMoneymoremore + PlatformMoneymoremore + LoanNo
				+ OrderNo + Amount + FeeMax + FeeWithdraws + FeePercent + Fee
				+ FreeLimit + FeeRate + FeeSplitting + RandomTimeStamp
				+ Remark1 + Remark2 + Remark3 + ResultCode;
		
		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);
		
		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}

		String pS2SUrl = new String(Codec.decodeBASE64(Remark2));
		String[] arraryRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String platformMemberId = arraryRemark3[0];
		String pMemo3 = arraryRemark3[1];
		
		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 成功
			result.put("serviceFee", FeeWithdraws);
			DealDetail.updateStatus(OrderNo);
		} else if("89".equals(ResultCode)){
			pErrCode = "MG00020F"; //提现回退
		} else{
			pErrCode = "MG00001F"; // 失败
		}
		String pErrMsg = Message;
		
		if(Convert.strToDouble(Amount, 0) <= 1){  //提现金额必须大于1元
			pErrMsg = "提现金额必须大于1元";
		}
		
		result.put("pMemo1", platformMemberId);
		result.put("pMemo3", pMemo3);
		result.put("pMerBillNo", OrderNo);
		
		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);
		
		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara
				+ Constants.ENCRYPTION_KEY);
		
		LoanUtil.printLoggerToP2P(pS2SUrl, pMerCode, pErrCode, pErrMsg,
				p3DesXmlPara, pSign);
		
		Logger.info("======第三方支付回调提现（异步）接口，结束========");
		
		DealDetail.updateEvent2(OrderNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		loanNotify(pS2SUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}
	
	/**
	 * 用户转商户返回信息
	 * 
	 * @return
	 */
	public static void loanTransferUserToMerReturn(String LoanJsonList,
			String Action, String PlatformMoneymoremore,
			String RandomTimeStamp, String Remark1, String Remark2,
			String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		Logger.info("======第三方支付回调用户转商户接口，执行开始========");
	
		LoanJsonList = LoanUtil.UrlDecoder(LoanJsonList, "utf-8");

		LoanUtil.printLoggerFromLoan(ResultCode, Message, ReturnTimes, LoanJsonList, Action,
				PlatformMoneymoremore, RandomTimeStamp, Remark1, Remark2,
				Remark3, ResultCode, Message, ReturnTimes, SignInfo);
		
		Map<String, Object> loanMap = LOAN.getLoanList(LoanJsonList).get(0);

		if (Action == null) {
			Action = "";
		}

		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}

		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}

		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}

		if (Remark3 == null) {
			Remark3 = "";
		}

		String publickey = LoanConstants.publicKey;

		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = LoanJsonList + PlatformMoneymoremore + Action
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ResultCode;

		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,publickey);

		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}

		String pWebUrl = new String(Codec.decodeBASE64(Remark1));

		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		
		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 成功
			
			result.put("pMerBillNo", loanMap.get("OrderNo").toString());
		} else {
			pErrCode = "MG00001F"; // 失败
		}
		String pErrMsg = Message;

		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);

		LoanUtil.printLoggerToP2P(pWebUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);

		Logger.info("======第三方支付回调用户转商户接口，结束========");

		DealDetail.updateEvent2(loanMap.get("OrderNo").toString(), "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		render("loan/LoanController/loanReturn.html", pWebUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}
	
	/**
	 * 用户转商户返回信息
	 * 
	 * @return
	 */
	public static void loanTransferUserToMerNotify(String LoanJsonList,
			String Action, String PlatformMoneymoremore,
			String RandomTimeStamp, String Remark1, String Remark2,
			String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		Logger.info("======第三方支付回调用户转商户（异步）接口，执行开始========");

		LoanJsonList = LoanUtil.UrlDecoder(LoanJsonList, "utf-8");

		LoanUtil.printLoggerFromLoan(ResultCode, Message, ReturnTimes, LoanJsonList, Action,
				PlatformMoneymoremore, RandomTimeStamp, Remark1, Remark2,
				Remark3, ResultCode, Message, ReturnTimes, SignInfo);
		
		Map<String, Object> loanMap = LOAN.getLoanList(LoanJsonList).get(0);

		if (Action == null) {
			Action = "";
		}

		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}

		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}

		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}

		if (Remark3 == null) {
			Remark3 = "";
		}

		String publickey = LoanConstants.publicKey;

		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = LoanJsonList + PlatformMoneymoremore + Action
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ResultCode;

		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,publickey);

		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}

		String pS2SUrl = new String(Codec.decodeBASE64(Remark2));

		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		
		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 成功
			
			result.put("pMerBillNo", loanMap.get("OrderNo").toString());
		} else {
			pErrCode = "MG00001F"; // 失败
		}
		String pErrMsg = Message;

		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);

		LoanUtil.printLoggerToP2P(pS2SUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);

		Logger.info("======第三方支付回调用户转商户（异步）接口，结束========");

		DealDetail.updateEvent2(loanMap.get("OrderNo").toString(), "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		loanNotify(pS2SUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}
	
	/**
	 * 商户转用户返回信息
	 * 
	 * @return
	 */
	public static void loanTransferMerToUserReturn(String LoanJsonList,
			String Action, String PlatformMoneymoremore,
			String RandomTimeStamp, String Remark1, String Remark2,
			String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		Logger.info("======第三方支付回调商户转用户接口，执行开始========");

		LoanJsonList = LoanUtil.UrlDecoder(LoanJsonList, "utf-8");
		
		LoanUtil.printLoggerFromLoan(ResultCode, Message, ReturnTimes, LoanJsonList, Action,
				PlatformMoneymoremore, RandomTimeStamp, Remark1, Remark2,
				Remark3, ResultCode, Message, ReturnTimes, SignInfo);

		if (Action == null) {
			Action = "";
		}

		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}

		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}

		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}

		if (StringUtils.isBlank(Remark3)) {
			Logger.info("======Remark3存放的信息丢失========");
			Remark3 = "";
		}

		String publickey = LoanConstants.publicKey;

		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = LoanJsonList + PlatformMoneymoremore + Action
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ResultCode;

		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);

		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}

		String pWebUrl = new String(Codec.decodeBASE64(Remark1));
		String[] arrayRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String pMerBillNo = arrayRemark3[0];
		String pMemo1 = arrayRemark3[1];
		
		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 成功
			
			DealDetail.updateStatus(pMerBillNo);
		} else {
			pErrCode = "MG00001F"; // 失败
		}
		String pErrMsg = Message;

		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("pMerBillNo", pMerBillNo);
		result.put("pMemo1", pMemo1);

		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);

		LoanUtil.printLoggerToP2P(pWebUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);

		Logger.info("======第三方支付回调商户转用户接口，结束========");

		DealDetail.updateEvent2(pMerBillNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		render("loan/LoanController/loanReturn.html", pWebUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}
	
	/**
	 * 商户转用户返回信息(异步)
	 * 
	 * @return
	 */
	public static void loanTransferMerToUserNotify(String LoanJsonList,
			String Action, String PlatformMoneymoremore,
			String RandomTimeStamp, String Remark1, String Remark2,
			String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {
		Logger.info("======第三方支付回调商户转用户接口(异步)，执行开始========");
		
		LoanJsonList = LoanUtil.UrlDecoder(LoanJsonList, "utf-8");
		
		LoanUtil.printLoggerFromLoan(ResultCode, Message, ReturnTimes, LoanJsonList, Action,
				PlatformMoneymoremore, RandomTimeStamp, Remark1, Remark2,
				Remark3, ResultCode, Message, ReturnTimes, SignInfo);
		
		if (Action == null) {
			Action = "";
		}
		
		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}
		
		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}
		
		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}
		
		if (StringUtils.isBlank(Remark3)) {
			Logger.info("======Remark3存放的信息丢失========");
			Remark3 = "";
		}
		
		String publickey = LoanConstants.publicKey;
		
		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = LoanJsonList + PlatformMoneymoremore + Action
				+ RandomTimeStamp + Remark1 + Remark2 + Remark3 + ResultCode;
		
		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);
		
		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}
		
		String pS2SUrl = new String(Codec.decodeBASE64(Remark2));
		String[] arrayRemark3 = new String(Codec.decodeBASE64(Remark3)).split(LoanConstants.TOKEN);
		String pMerBillNo = arrayRemark3[0];
		String pMemo1 = arrayRemark3[1];
		
		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F"; // 成功
			
			DealDetail.updateStatus(pMerBillNo);
		} else {
			pErrCode = "MG00001F"; // 失败
		}
		String pErrMsg = Message;
		
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("pMerBillNo", pMerBillNo);
		result.put("pMemo1", pMemo1);
		
		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);
		
		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);
		
		LoanUtil.printLoggerToP2P(pS2SUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);
		
		Logger.info("======第三方支付回调商户转用户接口(异步)，结束========");
		
		DealDetail.updateEvent2(pMerBillNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		loanNotify(pS2SUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}

	/**
	 * 解冻投资金额返回信息
	 * 
	 * @return
	 */
	public static void loanUnfreezeInvestAmountReturn(String LoanNoList,
			String LoanNoListFail, String PlatformMoneymoremore,
			String AuditType, String RandomTimeStamp, String Remark1,
			String Remark2, String Remark3, String ResultCode, String Message,
			String ReturnTimes, String SignInfo) {

		Logger.info("======第三方支付回调解冻投资金额接口，执行开始========");

		LoanUtil.printLoggerFromLoan(ResultCode, Message, ReturnTimes, LoanNoList,
				LoanNoListFail, PlatformMoneymoremore, RandomTimeStamp,
				AuditType, Remark1, Remark2, Remark3, ResultCode, Message,
				ReturnTimes, SignInfo);

		if(StringUtils.isNotBlank(LoanNoListFail)){  //有问题的流水号
			Logger.info("======解冻投资金额时，有问题的流水号：%s", LoanNoListFail);
		}
		
		if (StringUtils.isBlank(Remark1)) {
			Logger.info("======Remark1存放的信息丢失========");
			Remark1 = "";
		}

		if (StringUtils.isBlank(Remark2)) {
			Logger.info("======Remark2存放的信息丢失========");
			Remark2 = "";
		}

		if (Remark3 == null) {
			Remark3 = "";
		}
		if (RandomTimeStamp == null) {
			RandomTimeStamp = "";
		}

		String publickey = LoanConstants.publicKey;

		RsaHelper rsa = RsaHelper.getInstance();
		String dataStr = LoanNoList + LoanNoListFail + PlatformMoneymoremore
				+ AuditType + RandomTimeStamp + Remark1 + Remark2 + Remark3
				+ ResultCode;

		// 签名
		boolean verifySignature = rsa.verifySignature(SignInfo, dataStr,
				publickey);

		if (!verifySignature) {
			Logger.info("数据签名验证:%s", verifySignature);
			flash.error("数据签名验证失败");
			Application.error();
		}

		String pWebUrl = new String(Codec.decodeBASE64(Remark1));
		Remark3 = new String(Codec.decodeBASE64(Remark3));

		String pMerCode = PlatformMoneymoremore;
		String pErrCode = "";
		
		if ("88".equals(ResultCode)) {
			pErrCode = "MG00000F";

		} else {
			pErrCode = "MG00001F";
		}

		String pErrMsg = Message;
		
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("pMerBillNo", Remark3);
		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);

		LoanUtil.printLoggerToP2P(pWebUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);

		Logger.info("======第三方支付回调解冻投资金额接口，结束========");

		DealDetail.updateEvent2(Remark3, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		render("loan/LoanController/loanReturn.html", pWebUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}
	
	/**
	 * 余额查询,WS请求
	 * 
	 * @param argMerCode
	 * @param argIpsAccount
	 * @param args
	 * @return
	 */
	private static String responeBalanceQuery(String argMerCode, String argIpsAccount,
			Map<String, Object> args) {

		String SubmitURL = LoanConstants.LOAN_URL[2];
		String[] resultarr = LoanUtil.doPostQueryCmd(SubmitURL, args);

		Logger.info("======余额查询结果========");
		Logger.info("status：%s", resultarr[0]);
		Logger.info("result：%s", resultarr[1]);

		Map<String, Object> result = new HashMap<String, Object>();

		if (StringUtils.isNotBlank(resultarr[1])) {
			String[] balance = resultarr[1].split("\\|");
			if (balance != null && balance.length > 2) {
				result.put("pMerCode", argMerCode);
				result.put("pErrCode", "0000");
				result.put("pErrMsg", "成功");
				result.put("pIpsAcctNo", argIpsAccount);
				result.put("pBalance", balance[0]);
				result.put("pLock", balance[2]);
				result.put("pNeedstl", "0.00");

			}
		} else {
			result.put("pMerCode", argMerCode);
			result.put("pErrCode", "9999");
			result.put("pErrMsg", "查询余额失败");
			result.put("pIpsAcctNo", argIpsAccount);
			result.put("pBalance", "0.00");
			result.put("pLock", "0.00");
			result.put("pNeedstl", "0.00");
		}

		String sign = result.get("pMerCode").toString()
				+ result.get("pErrCode").toString()
				+ result.get("pErrMsg").toString()
				+ result.get("pIpsAcctNo").toString()
				+ result.get("pBalance").toString()
				+ result.get("pLock").toString()
				+ result.get("pNeedstl").toString();
		String pSign = Encrypt.MD5(sign + Constants.ENCRYPTION_KEY);
		result.put("pSign", pSign);

		Logger.info("======返回给p2p的查询结果========");
		Logger.info("pErrCode：%s", result.get("pErrCode").toString());
		// Logger.info("pSign：%s", pSign);

		Logger.info("pSign：%s", sign);

		return LoanUtil.toJson(result);
	}
	
	/**
	 * 转账：投资，直接返回P2P
	 * 
	 * @return
	 */
	private static void transferReturnWithoutLoan(ErrorInfo error,String pErrCode, String pErrMsg,
			long platformMemberId,JSONObject jsonArg3DesXmlPara,JSONObject jsonArgeXtraPara) {

		String pWebUrl = jsonArgeXtraPara.getString("pWebUrl");
		String pMerCode = LoanConstants.argMerCode;
		
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("pMerBillNo", jsonArg3DesXmlPara.getString("pMerBillNo"));
		result.put("pTransferType", LoanConstants.INVEST);
		result.put("pMemo1", platformMemberId + "");


		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);

		LoanUtil.printLoggerToP2P(pWebUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);

		DealDetail.updateEvent2(jsonArg3DesXmlPara.getString("pMerBillNo"), "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		render("loan/LoanController/loanReturn.html", pWebUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
	}

	/**
	 * 投资金额已解冻，直接返回p2p
	 * @param error
	 * @param platformId
	 * @param platformMemberId
	 * @param memberName
	 * @param argMerCode
	 * @param jsonArg3DesXmlPara
	 * @param jsonArgeXtraPara
	 */
	private static void unfreezeInvestAmountReturnWithoutLoan(ErrorInfo error,
			long platformId, long platformMemberId, String memberName,
			String argMerCode, JSONObject jsonArg3DesXmlPara,
			JSONObject jsonArgeXtraPara) {
		String pWebUrl = jsonArg3DesXmlPara.getString("pWebUrl");

		String pMerCode = argMerCode;
		String pErrCode = "MG00000F";
		String pErrMsg = "解冻成功";
		
		String p3DesXmlPara = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		result.put("pMerBillNo", jsonArg3DesXmlPara.getString("pMerBillNo"));
		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara + Constants.ENCRYPTION_KEY);

		LoanUtil.printLoggerToP2P(pWebUrl, pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);

		Logger.info("======第三方支付回调解冻投资金额接口，结束========");

		DealDetail.updateEvent2(jsonArg3DesXmlPara.getString("pMerBillNo"), "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		render("loan/LoanController/loanReturn.html", pWebUrl, pMerCode,pErrCode, pErrMsg, p3DesXmlPara, pSign);
		
	}

	/**
	 * 异步回调p2p
	 * @param pS2SUrl
	 * @param pMerCode
	 * @param pErrCode
	 * @param pErrMsg
	 * @param p3DesXmlPara
	 * @param pSign
	 */
	private static void loanNotify(String pS2SUrl, String pMerCode, String pErrCode, String pErrMsg, String p3DesXmlPara, String pSign){
		Map<String, Object> req = new HashMap<String, Object>();
		req.put("pMerCode", pMerCode);
		req.put("pErrCode", pErrCode);
		req.put("pErrMsg", pErrMsg);
		req.put("p3DesXmlPara", p3DesXmlPara);
		req.put("pSign", pSign);
		String respone = LoanUtil.doPostQueryCmd(pS2SUrl, req)[1];
		if(StringUtils.isNotBlank(respone)){
			JSONObject jsonObj = null;
			try{
				jsonObj = JSONObject.fromObject(respone);	
			}catch(Exception e){
				Logger.error("异步通知，json解析时，%s", e.getMessage());
				
				renderText("");
			}
			
			int code = Convert.strToInt(jsonObj.getString("code").trim(),-1);
			
			if(code >= 0 || code == Constants.ALREADY_RUN || code == Constants.FAIL_CODE){
				renderText("SUCCESS");
			}
		}
		renderText("");
	}
	

	/**
	 * 处理p2p业务
	 * @param pWSUrl
	 * @param pMerCode
	 * @param pErrCode
	 * @param pErrMsg
	 * @param p3DesXmlPara
	 * @param pSign
	 * @return
	 */
	private static Map<String, Object> handlerService(String pWSUrl, String pMerCode, String pErrCode, String pErrMsg, String p3DesXmlPara, String pSign) {
		Map<String, String> map = new HashMap<String, String>();
		map.put("pMerCode", pMerCode);
		map.put("pErrCode", pErrCode);
		map.put("pErrMsg", pErrMsg);
		map.put("p3DesXmlPara", p3DesXmlPara);
		map.put("pSign", pSign);
		
		String response = "";
		try{
			WSRequest request = null;
			request= WS.url(pWSUrl);
			request.timeout = 180;  //设置为180秒
			
			response = request.setParameters(map).post().getString();
		}catch(Exception e){
			Logger.info("执行p2p业务时：%s",e.getMessage());
		}
		
		Logger.info("执行p2p业务，result：%s", response);
		
		JSONObject jsonReturn = null;
		try{
			jsonReturn = JSONObject.fromObject(response);	
		}catch(Exception e){
			Logger.error("执行p2p业务，json解析时，%s", e.getMessage());
		}
		
		String returnResult = Encrypt.encrypt3DES(Converter.jsonToXml(jsonReturn.toString(), null, null, null, null), Constants.ENCRYPTION_KEY);
		
		Map<String, Object> args = new HashMap<String, Object>();
		args.put("url", jsonReturn.getString("pPostUrl"));
		args.put("result", returnResult);
		
		return args;
	}
	
	/**
	 * 自动还款,WS请求
	 * 
	 * @param argMerCode
	 * @param argIpsAccount
	 * @param args
	 * @return
	 */
	private static String responseAutoRepayment(String argMerCode, long platmemberid,Map<String, Object> args) {

		String SubmitURL = LoanConstants.LOAN_URL[4];
		String[] resultarr = LoanUtil.doPostQueryCmd(SubmitURL, args);

		String pMerCode = argMerCode;
		String pErrCode = ""; 
		String pErrMsg = "";   
		String p3DesXmlPara = "";
		String OrderNo = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		if(StringUtils.isNotBlank(resultarr[1])&&(resultarr[1].startsWith("[") || resultarr[1].startsWith("{"))){
			
			JSONObject jsonFromLoan = null;
			if(resultarr[1].startsWith("[")){
				jsonFromLoan = (JSONObject) JSONArray.fromObject(resultarr[1]).get(0);
			}
			
			if(resultarr[1].startsWith("{")){
				jsonFromLoan = JSONObject.fromObject(resultarr[1]);
			}

			String[] arrayRemark3 = new String(Codec.decodeBASE64(jsonFromLoan.getString("Remark3"))).split(LoanConstants.TOKEN);
			String merBillNo = arrayRemark3[0].trim();
			String memo3 = arrayRemark3[1].trim();
			String batchId = arrayRemark3[2].trim();
			String platformId = arrayRemark3[3].trim();
			String bidNo = arrayRemark3[4].trim();
			String period = arrayRemark3[5].trim();
			String bidBillNo = bidNo + period;
			
			String resultCode = jsonFromLoan.getString("ResultCode");

			result.put("pMerBillNo", merBillNo);
			result.put("pMemo3", memo3); 
			
			if(!"-1".equals(batchId)){  //更新分批处理结果
				TransferBatches.updateStatus(Long.parseLong(batchId),"88".equals(resultCode)?2:3,new ErrorInfo());
			}
			
			if ("88".equals(resultCode)) {
				pErrCode = "MG00000F";

				TransferBatches batch = TransferBatches.pollRemainBill(bidBillNo);  
				if(!"-1".equals(batchId) && batch != null){  //分批处理
				
					JSONObject jsonObj = JSONObject.fromObject(batch.transferBillNos);
					jsonObj.put("batchId", batch.id);
					jsonObj.put("batchNo", batch.batchNo);
					jsonObj.put("period", period);
					
					String strXml = Converter.jsonToXml(jsonObj.toString(), "pReq", "pRow", null, null);
					Logger.info("继续还款：p3DesXmlPara = %s", strXml);
					String arg3DesXmlPara = Encrypt.encrypt3DES(strXml, Constants.ENCRYPTION_KEY);

					//继续还款 
					LoanController.getInstance().loan(LoanConstants.REPAYMENT, 0L, "", Long.parseLong(platformId), "",LoanConstants.argMerCode, arg3DesXmlPara,arg3DesXmlPara, "");
				}

				DealDetail.updateStatus(merBillNo + (!"-1".equals(batchId)?batchId:""));
				
			} else {
				pErrCode = "MG00001F";
			}
			
			pErrMsg = jsonFromLoan.getString("Message"); 
			OrderNo = arrayRemark3[2];  
			
		}else{
			pErrCode = "MG00001F";
			pErrMsg = StringUtils.isNotBlank(resultarr[1])?resultarr[1]:"ws请求响应结果为空"; 
			result.put("pMemo1", platmemberid + "");
		}

		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara
				+ Constants.ENCRYPTION_KEY);

		LoanUtil.printLoggerToP2P("", pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);

		
		Map<String, Object> resultToP2P = new HashMap<String, Object>();
		resultToP2P.put("pMerCode", pMerCode);
		resultToP2P.put("pErrCode", pErrCode);
		resultToP2P.put("pErrMsg", pErrMsg);
		resultToP2P.put("p3DesXmlPara", p3DesXmlPara);
		resultToP2P.put("pSign", pSign);
		
		Logger.info("======第三方支付响应自动还款接口，结束========");
		
		DealDetail.updateEvent(OrderNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		return LoanUtil.toJson(resultToP2P);
	}
	
	/**
	 * 登记债权人（自动投标）,WS请求
	 * 
	 * @param argMerCode
	 * @param argIpsAccount
	 * @param args
	 * @return
	 */
	private static String responseAutoRegisterCreditor(String argMerCode, String argIpsAccount,Map<String, Object> args) {

		String SubmitURL = LoanConstants.LOAN_URL[4];
		String[] resultarr = LoanUtil.doPostQueryCmd(SubmitURL, args);

		String pMerCode = argMerCode;
		String pErrCode = ""; 
		String pErrMsg = "";   
		String p3DesXmlPara = "";
		String OrderNo = "";
		LinkedHashMap<String, String> result = new LinkedHashMap<String, String>();
		if(StringUtils.isNotBlank(resultarr[1])&&(resultarr[1].startsWith("[") || resultarr[1].startsWith("{"))){
			
			JSONObject jsonFromLoan = null;
			if(resultarr[1].startsWith("[")){
				jsonFromLoan = (JSONObject) JSONArray.fromObject(resultarr[1]).get(0);
			}
			
			if(resultarr[1].startsWith("{")){
				jsonFromLoan = JSONObject.fromObject(resultarr[1]);
			}
			
			String[] arrayRemark3 = new String(Codec.decodeBASE64(jsonFromLoan.getString("Remark3"))).split(LoanConstants.TOKEN);
				
			String LoanJsonList = LoanUtil.UrlDecoder(jsonFromLoan.getString("LoanJsonList"), "utf-8");
			
	        Map<String, Object> loanMap = LOAN.getLoanList(LoanJsonList).get(0);
			
			if ("88".equals(jsonFromLoan.getString("ResultCode"))) {
				pErrCode = "MG00000F";
				
				result.put("pFee", arrayRemark3[1]);
				result.put("pP2PBillNo", loanMap.get("LoanNo").toString());
				
				DealDetail.updateStatus(loanMap.get("OrderNo").toString());
				
			} else {
				pErrCode = "MG00001F";
			}
			
			pErrMsg = jsonFromLoan.getString("Message"); 
			OrderNo = loanMap.get("OrderNo").toString(); 
			
		}else{
			pErrCode = "MG00001F";
			pErrMsg = StringUtils.isNotBlank(resultarr[1])?resultarr[1]:"ws请求响应结果为空"; 
		}

		p3DesXmlPara = LoanUtil.parseMapToXml(result);
		p3DesXmlPara = Encrypt.encrypt3DES(p3DesXmlPara,Constants.ENCRYPTION_KEY);

		String pSign = Encrypt.MD5(pMerCode + pErrCode + pErrMsg + p3DesXmlPara
				+ Constants.ENCRYPTION_KEY);

		LoanUtil.printLoggerToP2P("", pMerCode, pErrCode, pErrMsg,p3DesXmlPara, pSign);

		Map<String, Object> resultToP2P = new HashMap<String, Object>();
		resultToP2P.put("pMerCode", pMerCode);
		resultToP2P.put("pErrCode", pErrCode);
		resultToP2P.put("pErrMsg", pErrMsg);
		resultToP2P.put("p3DesXmlPara", p3DesXmlPara);
		resultToP2P.put("pSign", pSign);
		
		Logger.info("======第三方支付响应自动投标接口，结束========");
		
		DealDetail.updateEvent(OrderNo, "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
		
		return LoanUtil.toJson(resultToP2P);
	}
}
