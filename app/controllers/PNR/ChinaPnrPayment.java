package controllers.PNR;

import business.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.shove.security.Encrypt;
import constants.ChinaPnrConstants;
import constants.Constants;
import constants.MsgCode;
import controllers.Application;
import controllers.BaseController;
import models.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import play.Logger;
import play.libs.Codec;
import play.libs.WS;
import play.libs.WS.HttpResponse;
import play.mvc.Http;
import services.ChinaPnrBaseService;
import services.ChinaPnrConfig;
import services.ChinaPnrService;
import services.SignUtils;
import utils.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

/**
 * 汇付天下支付入口
 *
 * @author yx
 * @create 2014年11月27日 上午9:27:00
 */
public class ChinaPnrPayment extends BaseController {

    //add by printFlag yangxuan 2014-12-17  start
    private static final String TRXID = "TrxId";

    private static final String PROID = "ProId";

    private static final String  ORDID = "OrdId";
    //added

    private static final String MaxTenderRate = "0.10";

    private static final String BorrowerRate = "1.00";

    //在自动投标主动投标的时候，因为冻结订单号不能和投标订单号一致，所以需要在支付中间层给个标志 ，在交易状态查询的时候也需要加此标志
    private static final String frezeeFlag = "1";

    //成功
    private static final String PTradeStatue_S = "1";

    //失败
    private static final String PTradeStatue_F = "2";

    //处理中
    private static final String PTradeStatue_D = "3";

    //未找到
    private static final String PTradeStatue_N = "4";

    //返回至p2p的action路径key
    private static final String P2P_URL_KEY = "url_key";  //同步页面地址

    private static final String P2P_ASYN_URL_KEY  = "asyn_url_key";  //异步地址

    //返回至p2p的异步或者同步参数
    private static final String P2P_BASEPARAMS = "base_params";

    //ws,http返回成功状态码
    private static final Integer HTTP_STATUS_SUCCESS = 200;

    //模拟表单页面地址
    private static final String P2P_COMIT_PAGE = "/PNR/PNRPayment/p2pcommit.html";

    private static final String USER_BIND_CARD_FLAG = "bind_card";  //用户绑卡保存至数据库标志


    /**
     * 汇付天下请求主入口
     * @param domain  协议号,p2p通过与spay中对应的约定名称,做校验
     * @param type  接口类型
     * @param platform  平台id
     * @param memberId  p2p用户id
     * @param memberName  p2p用户名称
     * @param argMerCode  商户号
     * @param key  缓存中的arg3DesXmlPara，argeXtraPara的缓存key
     * @param argSign  md5加密之后的校验参数
     * @param argIpsAccount  第三方客户号
     */
    public void pnr(String domain, int type, int platform,
                    long memberId, String memberName, String argMerCode,
                    String arg3DesXmlPara,String extra, String argSign,
                    String argIpsAccount,String isWS)
    {

        ErrorInfo error = new ErrorInfo();

//		 接口类型参数限定在1~49 
        if (type <= 0 || type > 150) {
            flash.error("传入参数有误");
            Application.error();
        }

//		与使用平台的校验
        Logger.debug("argMerCode:%s"+argMerCode+"\n arg3DesXmlPara:%s"+arg3DesXmlPara+"\n extra:%s"+extra+"\n argSign:%s"+argSign);

        if(!PaymentUtil.expansionCheckSign(argMerCode+arg3DesXmlPara+extra, argSign)) {
            Logger.debug("------------------------资金托管平台校验失败-------------------------------");
            flash.error("sign校验失败");
            Application.error();
        }

        Logger.debug("------------------------资金托管平台校验成功-------------------------------");

        JSONObject json = null;
        JSONObject pnrparams = null;
        if(type!=ChinaPnrConstants.I_BANK_LIST){
            Logger.debug("-----json 原文: %s"+Encrypt.decrypt3DES(arg3DesXmlPara,Constants.ENCRYPTION_KEY +"\n-----pnrparams 原文: %s")+Encrypt.decrypt3DES(extra,Constants.ENCRYPTION_KEY));
            arg3DesXmlPara = Encrypt.decrypt3DES(arg3DesXmlPara,Constants.ENCRYPTION_KEY);
            Logger.debug("arg3DesXmlPara : %s", arg3DesXmlPara);
            if(arg3DesXmlPara!=null&&!"".equals(arg3DesXmlPara)){
                json = (JSONObject)Converter.xmlToObj(arg3DesXmlPara);
            }
            if(extra!=null&&!"".equals(extra)){
                pnrparams = (JSONObject)Converter.xmlToObj(Encrypt.decrypt3DES(extra,Constants.ENCRYPTION_KEY));
            }
            if (null == json || null == pnrparams) {
                Logger.debug("参数出现空值");
                flash.error("解析参数有误");
                Application.error();
            }
            Logger.debug("-----json:%s"+json==null?"":json.toString()+"\n-----pnrparams:%s"+pnrparams==null?"":pnrparams.toString());

        }

        ChinaPnrBaseService maps = null;
        Logger.debug("------------------------------[" + type+ "]start......................");
        switch (type) {
            case ChinaPnrConstants.I_USERLOGIN:
//			String usrCustId = "6000060000597572";
                String usrCustId = pnrparams.getString("contractNo");
                maps = caseUserLogin(usrCustId);
                break;
            // 开户
            case ChinaPnrConstants.I_CREATE_ACCOUNT:
                String idNumber = json.getString("pIdentNo");

                boolean flag = true;

//			身份证已存在
                if(Member.isCreateAccount(json.getString("pIdentNo"), domain, error)) {

//				用户平台关系表中不存在该身份证会员的信息
                    if(error.code == 1) {
                        Member member = new Member();
                        long id = Member.queryIdByIdNumber(json.getString("pIdentNo"));
                        member.memberId = id;
                        member.platformId = platform;
                        member.platformMemberId = memberId;
                        member.platformMembername = memberName;
                        member.addPlatformmember(error);

//				不同平台，使用相同的支付接口，表示身份证会员已开户，在平台关系表中插入数据	
                    }else if(error.code == 2) {
                        Member member = new Member();
                        member.idNumber = idNumber;
                        member.platformMemberId = memberId;
                        member.platformMembername = memberName;
                        member.platformMemberAccount = member.queryAccount(idNumber, platform);

                        member.addPlatformmember(error);
                    }else if(error.code == 3) {
                        Member.updateAccount(platform, memberId, Member.queryAccount(idNumber, platform));
                    }

                    String ipsAcctNo = Member.queryAccount(idNumber, platform);

                    if(ipsAcctNo != null) {
                        flag = false;
                    }

                    if(!flag) {
                        String pErrCode = ipsAcctNo == null ? "MG00001F" : "MY00000F";
                        String pErrMsg = ipsAcctNo == null ? "开户失败" : "已开户";
                        JSONObject jsonObj = new JSONObject();
                        jsonObj.put("pIpsAcctNo", ipsAcctNo);
                        jsonObj.put("pStatus", "0");
                        jsonObj.put("pMemo1", memberId);
                        String strXml = Converter.jsonToXml(jsonObj.toString(), "pReq", null, null, null);
                        String p3DesXmlPara = Encrypt.encrypt3DES(strXml, Constants.ENCRYPTION_KEY);

                        Map<String, String> args = new HashMap<String, String>();
                        args.put("url", json.getString("pWebUrl"));
                        args.put("pMerCode", argMerCode);
                        args.put("pErrCode", pErrCode);
                        args.put("pErrMsg", pErrMsg);
                        args.put("p3DesXmlPara", p3DesXmlPara);
                        args.put("pSign", Encrypt.MD5(argMerCode+pErrCode+pErrMsg+p3DesXmlPara+Constants.ENCRYPTION_KEY));

                        render("@IPS.IPayment.ipsCommit", args);
                    }

                }else {
//				身份证不存在，根据请求在用户表和用户平台关系表中添加记录
                    Member member = new Member();

                    member.idNumber = json.getString("pIdentNo");
                    member.mobile = json.getString("pMobileNo");
                    member.platformId = platform;

                    member.platformMemberId = memberId;
                    member.platformMembername = memberName;

                    Map<String, String> info = member.add();

                    String content = "您在资金托管平台注册的用户名：" + info.get("name") + "  密码：" + info.get("password");

                    EmailUtil.sendEmail(json.getString("pEmail"), "注册信息", content);
                }

                maps = caseUserRegister(memberId,type,platform,json);

                break;
            // 标的登记
            case ChinaPnrConstants.I_REGISTER_SUBJECT:case ChinaPnrConstants.I_FLOW_BID:
                String pOperationType = json.getString("pOperationType"); // 1:新增,2:结束
                if ("1".equals(pOperationType)) {
                    caseAddBidInfo(json,pnrparams,memberId,type,platform);
                    break;
                } else {
                    caseTenderCancleByBorrower(memberId,type,platform,json,pnrparams);
                }
                break;
            // 登记债权人
            case ChinaPnrConstants.I_REGISTER_CREDITOR:
                // 主动投标，自动投标
                maps = caseTender(memberId,type,platform,json,pnrparams);
                break;
            // 登记担保方
            case ChinaPnrConstants.I_REGISTER_GUARANTOR:
                //TODO
                break;
            // 登记债权转让
            case ChinaPnrConstants.I_REGISTER_CRETANSFER:
                maps = caseCreditAssign(memberId,type,platform,json,pnrparams);
                break;
            // 自动投标签约
            case ChinaPnrConstants.I_AUTO_SIGING:
                maps = caseAutoTenderPlan(memberId,type,platform,json);
                break;
            // 自动还款签约
            case ChinaPnrConstants.I_REPAYMENT_SIGNING:

                break;
            // 充值
            case ChinaPnrConstants.I_RECHARGE:
                maps = caseNetSave(memberId,type,platform,json);
                break;
            // 转账
            case ChinaPnrConstants.I_TRANSFER:
                //自动扣款(放款Loans)
                String pTransferType =  json.getString("pTransferType");
                if("1".equals(pTransferType)){
                    caseLoans(memberId,type,platform,argMerCode,json,pnrparams,error);
                }else if("2".equals(pTransferType)){  //代偿(商户转用户)
                    caseCompensatory(memberId,type,platform,json,pnrparams,error);
                }else if("3".equals(pTransferType)){  //代偿还款(用户转商户)
                    if("Y".equals(isWS)){
                        renderText("form_post");
                    }
                    maps = caseCompensatoryRepayment(memberId,type,platform,json,pnrparams);
                }
                break;
            case ChinaPnrConstants.I_REPAYMENT:
                caseRepayment(memberId,type,platform,json,error);
                break;
            // 解冻保证金
            case ChinaPnrConstants.I_UNFREEZE:
                maps = caseUsrUnFreeze(pnrparams);
                break;
            // 自动代扣充值
            case ChinaPnrConstants.I_DEDUCT:
                //TODO
                break;
            // 提现
            case ChinaPnrConstants.I_WITHDRAWAL:
                maps = caseCash(memberId,type,platform,json);
                break;
            // 账户余额查询 后台
            case ChinaPnrConstants.I_ACCOUNT_BALANCE:
                caseQueryBalanceBg(memberId,type,platform,argIpsAccount);
                break;
            case ChinaPnrConstants.I_USERBINDCARD:
                maps = caseUserBindCard(memberId,type,platform,pnrparams);
                break;
            // 账户余额查询 页面
            case ChinaPnrConstants.I_QUERYBALANCE:
                maps = caseQueryBalance(argIpsAccount);
                break;
            // 商户端获取银行列表
            case ChinaPnrConstants.I_BANK_LIST:
                caseBankList();
                break;
            // 账户信息查询
            case ChinaPnrConstants.I_USER_INFO:
                //TODO
                break;
            // 交易查询
            case ChinaPnrConstants.I_QUERY_TRADE:
                caseQueryTransStat(memberId,type,platform,json,pnrparams);
                break;
            //自动投标计划关闭
            case ChinaPnrConstants.I_AUTOTENDERPLANCLOSE:
                maps = caseAutoTenderPlanClose(pnrparams);
                break;
            //自动扣款转账（商户用）
            case ChinaPnrConstants.I_MTRANSFER:
//			caseTransfer(memberId,type,platform,json,pnrparams);
                break;
            //商户子账户信息查询
            case ChinaPnrConstants.I_QUERYACCTS:
                caseQueryAccts();
                break;
            //商户扣款对账
            case ChinaPnrConstants.I_TRFRECONCILIATION:
                caseTrfReconciliation(pnrparams);
                break;
            //放还款对账
            case ChinaPnrConstants.I_RECONCILIATION:
                caseReconciliation(pnrparams);
                break;
            //取现对账
            case ChinaPnrConstants.I_CASHRECONCILIATION:
                caseCashReconciliation(pnrparams);
                break;
            //充值对账
            case ChinaPnrConstants.I_SAVERECONCILIATION:
                caseSaveReconciliation(pnrparams);
                break;
            case ChinaPnrConstants.I_USRACCPAY:
                maps = caseUsrAcctPay(memberId,type,platform,json,pnrparams);
                break;
            case ChinaPnrConstants.I_MERCUSTTUSER:
//			caseTransfer(memberId,type,platform,json,pnrparams);
                caseTransferByBatch(memberId,type,platform,json,pnrparams);
                break;
            case ChinaPnrConstants.I_MERCUSTTUSERBYSINGLE:
                caseTransferBySingle(memberId,type,platform,json,pnrparams);
                break;
            case ChinaPnrConstants.I_USRUNFREEZE_SINGLE:
                caseUsrUnfreezeSingle(memberId,type,platform,json,pnrparams);
                break;
        }

        reqParams(maps); // 输出请求参数
        String action = ChinaPnrConstants.DEV_URL;
        Logger.debug("------------------------------[" + type
                + "]end,will submit form......................");
        render("/PNR/PNRPayment/pnr.html", action, maps);

    }

    /**
     * 代偿还款
     * @param memberId p2p平台用户id
     * @param type 接口类型
     * @param platfrom 平台id
     * @param json 参数
     * @param pnrparams 参数2
     * @return
     */
    private static ChinaPnrBaseService caseCompensatoryRepayment(long memberId,
                                                                 int type, int platform, JSONObject json, JSONObject pnrparams) {
        Object pDetails = json.get("pDetails");
        JSONArray jsonArr = null;
        if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
            JSONObject pDetail = (JSONObject)pDetails;
            JSONObject pRow = pDetail.getJSONObject("pRow");

            jsonArr = new JSONArray();
            jsonArr.add(pRow);
        } else {
            jsonArr = json.getJSONArray("pDetails");
        }
        double countAmt = 0.00;
        String usrCustId = "";
        for (int i = 0;i<jsonArr.size();i++) {
            JSONObject pRow = (JSONObject)jsonArr.get(i);
            countAmt += Double.parseDouble(pRow.getString("pTrdAmt"));
            usrCustId = pRow.getString("pFIpsAcctNo");
        }
        if(pnrparams==null)
            pnrparams = new JSONObject();
        pnrparams.put("UsrCustId", usrCustId);
        pnrparams.put("TransAmt", String.format("%.2f", countAmt));
        json.put("pWebUrl", pnrparams.getString("pWebUrl"));
        return caseUsrAcctPay(memberId, type, platform, json, pnrparams);
    }

    /**
     * 代偿(商户转用户)
     * @param memberId p2p平台用户id
     * @param type 接口类型
     * @param platfrom 平台id
     * @param json 参数
     * @param pnrParams 参数2
     */
    private static void caseCompensatory(long memberId, int type, int platform,
                                         JSONObject json, JSONObject pnrParams,ErrorInfo error) {
        addEvent(memberId, type+200, platform, json.getString("pMerBillNo"),null,null, null, null);

        Object pDetails = json.get("pDetails");
        JSONArray jsonArr = null;
        if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
            JSONObject pDetail = (JSONObject)pDetails;
            JSONObject pRow = pDetail.getJSONObject("pRow");

            jsonArr = new JSONArray();
            jsonArr.add(pRow);
        } else {
            jsonArr = json.getJSONArray("pDetails");
        }

        Transfer transfer = null;
        String result ="";
        JsonObject resultObj = null;
        String pmerBillNo = json.getString("pMerBillNo");
        String orderId = "";
        double transAmt = 0.00;
        String inCustId = "";
        String outCustId = ChinaPnrConfig.getProperty("chinapnr_merCustId");
        for (int i = 0;i<jsonArr.size();i++) {
            JSONObject pRow = (JSONObject)jsonArr.get(i);
            transfer = new Transfer();
            transfer.pmerBillNo = pmerBillNo ;
            orderId = pRow.getString("pOriMerBillNo");
//			orderId = pmerBillNo;

            transfer.orderId =orderId ;
            transAmt = Double.parseDouble(pRow.getString("pTrdAmt"))-Double.parseDouble(pRow.getString("pTTrdFee"));
            transfer.transAmt = transAmt;
            inCustId = pRow.getString("pTIpsAcctNo");
            transfer.inCustId = inCustId;
            transfer.outCustId =outCustId ;

            transfer.saveTransfer(error);

            if(error.code<0){
                error();
            }

            result = caseTransfer(transfer.orderId, String.format("%.2f",  transfer.transAmt), transfer.inCustId);
            resultObj  = new JsonParser().parse(result).getAsJsonObject();
            //放款成功,修改数据库状态
            if("000".equals(resultObj.get("RespCode").getAsString())|"355".equals(resultObj.get("RespCode").getAsString())){
                transfer.modifyStatus(1, transfer.orderId, error);
                if(error.code<0){
                    error();
                }
            }
        }
        int failNum = 0;
        List<t_transfer_details> list  = transfer.findTransferByStatus(0, pmerBillNo, error);
        JsonObject respJson = new JsonObject();
        respJson.addProperty("pMerCode", outCustId);
        json.put("pMemo1", memberId);
        String arg3DesXmlPara = buildP3DesXmlPara(json);
        if(list != null && list.size()<=1){
            respJson.addProperty("pErrCode", "MG00000F");
            respJson.addProperty("pErrMsg", "操作成功");
        }else{
            respJson.addProperty("pErrCode", "MG00001F");
            respJson.addProperty("pErrMsg", failNum+"比记录操作失败");
        }
        respJson.addProperty("p3DesXmlPara", arg3DesXmlPara);
        String pSign  = Codec.hexMD5(respJson.get("pMerCode").getAsString()+respJson.get("pErrCode").getAsString()
                +respJson.get("pErrMsg").getAsString()+respJson.get("p3DesXmlPara").getAsString()+Constants.ENCRYPTION_KEY);
        respJson.addProperty("pSign", pSign);

        Logger.debug(respJson.toString());
        renderJSON(respJson.toString());
    }

    /**
     *  单笔资金解冻
     * @param memberId p2p平台用户id
     * @param type 接口类型
     * @param platfrom 平台id
     * @param json 参数
     * @param pnrparams 参数2
     */
    private static void caseUsrUnfreezeSingle(long memberId, int type,
                                              int platform, JSONObject json, JSONObject pnrparams) {
        System.out.println("============="+json);
        String ordId = json.getString("pMerBillNo");
        String trxId = json.getString("pP2PBillNo");
        ChinaPnrReqModel model = new ChinaPnrReqModel();
        model.setOrdId(ordId);
        model.setTrxId(trxId);
        ChinaPnrService service = new ChinaPnrService();
        String result =service.doUsrUnFreeze(model);
        JsonObject resultObj = new JsonParser().parse(result).getAsJsonObject();

        String pMerCode = resultObj.get("MerCustId").getAsString();
        String pErrCode = "999";
        String pErrMsg = "失败";
        if("000".equals(resultObj.get("RespCode").getAsString())|"107".equals(resultObj.get("RespCode").getAsString())){
            pErrCode = "MG00000F";
            pErrMsg = "成功";
        }

        JsonObject returnObj = new JsonObject();
        returnObj.addProperty("pMerCode", resultObj.get("MerCustId").getAsString());
        String arg3DesXmlPara = buildP3DesXmlPara(json);
        returnObj.addProperty("pErrCode", pErrCode);
        returnObj.addProperty("pErrMsg", pErrMsg);
        returnObj.addProperty("p3DesXmlPara", arg3DesXmlPara);
        String pSign = hasMD5(pMerCode,pErrCode,pErrMsg,arg3DesXmlPara);
        returnObj.addProperty("pSign", pSign);
        Logger.debug("解冻返回 json : %s", returnObj.toString());
//		renderJSON(returnObj);
        renderText(returnObj.toString());
		 
		/*Map<String,String> maps = new HashMap<String, String>();
		maps.put("pMerCode",pMerCode);
		maps.put("pErrCode",pErrCode);
		maps.put("pErrMsg", pErrMsg);
		maps.put("p3DesXmlPara", arg3DesXmlPara);
		String pSign = hasMD5(pMerCode,pErrCode,pErrMsg,arg3DesXmlPara);
		
		maps.put("pSign", pSign);
		String action = json.getString("pWebUrl");
		render("/PNR/PNRPayment/pnr.html",action,maps);*/

    }

    /**
     * 用户账户支付
     * @param memberId p2p平台用户id
     * @param type 接口类型
     * @param platfrom 平台id
     * @param json 参数
     * @param pnrparams 参数2
     * @return ChinaPnrBaseService(继承Map)
     */
    private static ChinaPnrBaseService caseUsrAcctPay(long memberId,int type , int platfrom,JSONObject json ,JSONObject pnrparams) {

        String ordId = json.getString("pMerBillNo");
        String UsrCustId  = pnrparams.getString("UsrCustId");
        String TransAmt = pnrparams.getString("TransAmt");

        JsonObject remark = new JsonObject();
        remark.addProperty("platfrom", platfrom);
        Set<String> set = json.keySet();
        for(String key : set){
            remark.addProperty(key,json.getString(key));
        }

        addEvent(memberId, type, platfrom, ordId, json.getString("pWebUrl"), json.getString("pS2SUrl"), remark.toString(), "汇付天下-用户账户支付");
        addDealDetail(memberId, platfrom, ordId, type, Double.parseDouble(TransAmt), false, "汇付天下-用户账户支付");

        ChinaPnrService service = new ChinaPnrService();
        service.putValue("Version", ChinaPnrConfig.getProperty("chinapnr_version"))
                .putValue("CmdId", ChinaPnrConstants.CMD_USRACCPAY)
                .putValue("OrdId", ordId)
                .putValue("UsrCustId", UsrCustId)
                .putValue("MerCustId", ChinaPnrConfig.getProperty("chinapnr_merCustId"))
                .putValue("TransAmt", TransAmt)
                .putValue("InAcctId", ChinaPnrConfig.getProperty("chinapnr_fee"))
                .putValue("InAcctType", "MERDT")
                .putValue("RetUrl", ChinaPnrConfig.getProperty("chinapnr_retUrl"))
                .putValue("BgRetUrl", ChinaPnrConfig.getProperty("chinapnr_retUrlBg"))
                .setChkValue();

        return service;
    }


    /**
     * 标的撤销(借款人撤销),p2p中的撤标就是借款人撤销,汇付通过调用解冻接口
     * @param memberId p2p平台用户id
     * @param type 接口类型
     * @param platfrom 平台id
     * @param json 参数
     * @param pnrparams 参数2
     */
    private static void caseTenderCancleByBorrower(long memberId,int type,int platform,JSONObject json,JSONObject pnrparams){

        String investInfo = pnrparams.getString("investInfo");
        Map<String,String> maps = new HashMap<String, String>();
        String pMerCode  = "";
        String pErrCode = "";
        String pErrMsg= "";

        String pSign = "";
        JsonObject desjson = new JsonObject();
        desjson.addProperty("pMerBillNo", json.getString("pMerBillNo"));
        desjson.addProperty("pOperationType",json.getString("pOperationType"));
        desjson.addProperty("pIpsBillNo",json.getString("pMerBillNo"));
        desjson.addProperty("pBidNo",json.getString("pBidNo"));
        desjson.addProperty("pMemo1", memberId);
        desjson.addProperty("pMemo3", json.getString("pMemo3"));
        String p3DesXmlPara=  buildP3DesXmlPara(desjson);
        //汇付天下请求成功000、107次数统计
        int i = 0 ;
        //流标需要ws返回,其他需要表单提交
        if("flowI".equals(json.getString("pMemo3"))){
            if(investInfo!=null&&!"".equals(investInfo)&&!"[]".equals(investInfo)){
                String[] invests = investInfo.split(",");
                ChinaPnrService service = new ChinaPnrService();
                ChinaPnrReqModel model = new ChinaPnrReqModel();
                String result = "";
                JsonObject resultJson = null;
                String respCode = "";
                int investSize = invests.length;
                for(String trxId : invests){
                    model.setOrdId(Random.randomNum());
                    model.setTrxId(trxId);
                    result = service.doUsrUnFreeze(model);
                    resultJson = new JsonParser().parse(result).getAsJsonObject();
                    respCode = resultJson.get("RespCode").getAsString();
                    if("000".equals(respCode)|"107".equals(respCode)){
                        i++;
                    }
                }
                if(investSize == i){
                    pMerCode = resultJson.get("MerCustId").getAsString();
                    pErrCode = "000".equals(resultJson.get("RespCode").getAsString()) |"107".equals(resultJson.get("RespCode").getAsString())? "MG00000F"
                            : params.get("RespCode");
                    pErrMsg = resultJson.get("RespDesc").getAsString();
                    pSign = hasMD5(pMerCode,pErrCode,pErrMsg,p3DesXmlPara);
                }else{
                    pMerCode = "999";
                    pErrCode = "999";
                    pErrMsg ="失败";
                }
            }else{
                pMerCode = "MG00000F";
                pErrCode = "MG00000F";
                pErrMsg ="成功";
            }
            pSign = hasMD5(pMerCode,pErrCode,pErrMsg,p3DesXmlPara);
            JsonObject returnJson = new JsonObject();
            returnJson.addProperty("pMerCode", pMerCode);
            returnJson.addProperty("pErrCode", pErrCode);
            returnJson.addProperty("pErrMsg", pErrMsg);
            returnJson.addProperty("p3DesXmlPara", p3DesXmlPara);
            returnJson.addProperty("pSign", pSign);
            renderJSON(returnJson.toString());
        }else{
            pMerCode = "MG00000F";
            pErrCode = "MG00000F";
            pErrMsg ="成功";
            pSign = hasMD5(pMerCode,pErrCode,pErrMsg,p3DesXmlPara);
            JsonObject returnJson = new JsonObject();
            returnJson.addProperty("pMerCode", pMerCode);
            returnJson.addProperty("pErrCode", pErrCode);
            returnJson.addProperty("pErrMsg", pErrMsg);
            returnJson.addProperty("p3DesXmlPara", p3DesXmlPara);
            returnJson.addProperty("pSign", pSign);
            renderJSON(returnJson.toString());
        }
    }



    /**
     * 标的撤销(投资人撤销)
     * @param memberId p2p平台用户id
     * @param type 接口类型
     * @param platfrom 平台id
     * @param json 参数
     * @param pnrparams 参数2
     * @return ChinaPnrBaseService(继承Map)
     */
    private static ChinaPnrBaseService caseTenderCancle(long memberId,int type,long platform,JSONObject json,
                                                        JSONObject pnrparams) {

        String version = ChinaPnrConfig.getProperty("chinapnr_version_2.0");
        String cmdId = ChinaPnrConstants.CMD_TENDERCANCLE;
        String ordId = json.getString("pMerBillNo");
        String ordDate = json.getString("pRegDate");
        String transAmt = json.getString("pLendAmt");
        String UsrCustId = json.getString("pIpsAcctNo");
        String IsUnFreeze = "Y";  //需要参数
        String RetUrl = ChinaPnrConfig.getProperty("chinapnr_retUrl");
        String BgRetUrl = ChinaPnrConfig.getProperty("chinapnr_retUrlBg");
        String merCustId = ChinaPnrConfig.getProperty("chinapnr_merCustId");

        ChinaPnrService maps = new ChinaPnrService();

        maps.putValue("Version", version).putValue("CmdId", cmdId)
                .putValue("MerCustId", merCustId)
                .putValue("OrdId", ordId).putValue("OrdDate", ordDate)
                .putValue("TransAmt", transAmt)
                .putValue("UsrCustId", UsrCustId)
                .putValue("RetUrl", RetUrl)
                .putValue("BgRetUrl", BgRetUrl)
                .putValue("IsUnFreeze", IsUnFreeze)
                .putValue("UnFreezeOrdId",Random.randomNum())
                .putValue("FreezeTrxId", pnrparams.getString("freezeTrxId"));
        JsonObject reqExt = new JsonObject();
        reqExt.addProperty("pMerBillNo",  json.getString("pMerBillNo"));
        reqExt.addProperty("pIpsBillNo",  json.getString("pMerBillNo"));
        reqExt.addProperty("pOperationType",  json.getString("pOperationType"));
        reqExt.addProperty("pMemo3",  json.getString("pMemo3"));
        reqExt.addProperty("pBidNo",  json.getString("pBidNo"));
        reqExt.addProperty("pMemo1", memberId);
        reqExt.addProperty("pRegDate",  json.getString("pRegDate"));
        reqExt.addProperty("pTrdLendRate",
                json.getString("pTrdLendRate")); // 在回调时返回至p2p平台需要利率
        reqExt.addProperty("pRepayMode", json.getString("pRepayMode")); // 环讯之前开通接口跟汇付类型之不一样,在此存储pRepayMode值
        maps.setChkValue();

        addEvent(memberId, type+200, platform, json.getString("pMerBillNo"), json.getString("pWebUrl"), json.getString("pS2SUrl"),reqExt.toString(), null);

        return maps;
    }

    /**
     * 标的信息录入(发标)
     * @param memberId p2p平台用户id
     * @param type 接口类型
     * @param platfrom 平台id
     * @param json 参数
     * @param pnrparams 参数2
     */
    private static void caseAddBidInfo(JSONObject json,
                                       JSONObject pnrparams,long memberId,int type,int platform) {

        String proId = json.getString("pBidNo");
        String borrCustId = json.getString("pIpsAcctNo");
        String borrTotAmt = json.getString("pLendAmt");
        String yearRate = json.getString("pTrdLendRate");
        String pRepayMode = json.getString("pRepayMode");
        String retType = "";
        if ("1".equals(pRepayMode)) {
            retType = "01";
        } else if ("2".equals(pRepayMode)) {
            retType = "03";
        }else if("3".equals(pRepayMode)){
            retType = "02";
        }else if("99".equals(pRepayMode)){
            retType = "99";
        }

        addDealDetail(memberId, platform, json.getString("pBidNo"), type, Double.parseDouble(borrTotAmt), false, "汇付天下-标的信息入录");

        String now = DateUtil.simple2(new Date());  //标的开始时间
        int pTrdCycleType = Integer.parseInt(json.getString("pTrdCycleType"));  //借款周期类型
        int pTrdCycleValue = Integer.parseInt(json.getString("pTrdCycleValue"));
        int bidday = Integer.parseInt(pnrparams.getString("bidEndDate"));  //p2p传来的只是天数,具体投标时间需要计算
        String bidEndDate = DateUtil.simple2(DateUtil.dateAddDay(new Date(), bidday));
        String retAmt = json.getString("pLendAmt"); // 应还款总金额//  
        String retDate =  pTrdCycleType == 1 ? DateUtil.simple(DateUtil.dateAddDay(new Date(), pTrdCycleValue)) :
                DateUtil.simple(DateUtil.dateAddMonth(new Date(), pTrdCycleValue));; // 应还款日期//  
        String guarAmt = json.getString("pGuaranteesAmt"); // p2p借款保证金,// 汇付担保金额

        ChinaPnrReqModel model = new ChinaPnrReqModel();
        model.setProId(proId);
        model.setBorrCustId(borrCustId);
        model.setBorrTotAmt(borrTotAmt);
        model.setYearRate(yearRate);
        model.setRetType(retType);
        model.setBidStartDate(now);
        model.setBidEndDate(bidEndDate);
        model.setRetAmt(retAmt);
        model.setRetDate(retDate);
        model.setGuarAmt(guarAmt);
        JsonObject reqExt = new JsonObject();
        reqExt.addProperty("pMerBillNo",  json.getString("pMerBillNo"));
        reqExt.addProperty("pRegDate",  json.getString("pRegDate"));
        reqExt.addProperty("pTrdLendRate",
                json.getString("pTrdLendRate")); // 在回调时返回至p2p平台需要利率
        reqExt.addProperty("pRepayMode", json.getString("pRepayMode")); // 环讯之前开通接口跟汇付类型之不一样,在此存储pRepayMode值
        reqExt.addProperty("pMemo3", json.getString("pMemo3"));
        reqExt.addProperty("pMemo1",memberId);
        reqExt.addProperty("platform",platform);

        model.setReqExt("");

        addEvent(memberId, type+200, platform, json.getString("pBidNo"), json.getString("pWebUrl"), json.getString("pS2SUrl"),reqExt.toString(), null);

        ChinaPnrService service = new ChinaPnrService();
        String result = service.doAddBidInfo(model);

        JsonObject respJson = null;
        respJson  = new JsonParser().parse(result).getAsJsonObject();

        String pMerCode = respJson.get("MerCustId").getAsString();
        //modify 20141221 get params . params -->respJson
        String pErrCode = "000".equals(respJson.get("RespCode").getAsString()) ? "MG00000F"
                : respJson.get("RespCode").getAsString();
        //modifyed

        if("MG00000F".equals(pErrCode)){
            saveBids(model);
        }

        String pErrMsg = respJson.get("RespDesc").getAsString();
        JsonObject desjson = new JsonObject();
        desjson.addProperty("pMerBillNo", json.getString("pMerBillNo"));
        desjson.addProperty("pBidNo", respJson.get("ProId").getAsString());
        desjson.addProperty("pRegDate", json.getString("pRegDate"));
        desjson.addProperty("pLendAmt", respJson.get("BorrTotAmt").getAsString());
        desjson.addProperty("pGuaranteesAmt", params.get("GuarAmt"));
        desjson.addProperty("pTrdLendRate", json.getString("pTrdLendRate"));
        desjson.addProperty("pTrdLendRate", "");
        desjson.addProperty("pTrdCycleType", "");
        desjson.addProperty("pTrdCycleValue", "");
        desjson.addProperty("pLendPurpose", "");
        desjson.addProperty("pRepayMode", json.getString("pRepayMode"));
        desjson.addProperty("pOperationType", "1");
        desjson.addProperty("pLendFee", "");
        desjson.addProperty("pAcctType", "");
        desjson.addProperty("pIdentNo", "");
        desjson.addProperty("pRealName", "");
        desjson.addProperty("pIpsAcctNo", respJson.get("BorrCustId").getAsString());
        desjson.addProperty("pIpsBillNo", json.getString("pMerBillNo"));
        desjson.addProperty("pIpsTime", DateUtil.simple2(new Date()));
        desjson.addProperty("pBidStatus", "1");
        desjson.addProperty("pRealFreezenAmt", respJson.get("GuarAmt").getAsString());
        desjson.addProperty("pMemo1", memberId);
        desjson.addProperty("pMemo3", json.getString("pMemo3"));

        String p3DesXmlPara = buildP3DesXmlPara(desjson);
        String pSign = hasMD5(pMerCode,pErrCode,pErrMsg,p3DesXmlPara);

        JsonObject returnJson = new JsonObject();
        returnJson.addProperty("pMerCode", pMerCode);
        returnJson.addProperty("pErrCode", pErrCode);
        returnJson.addProperty("pErrMsg", pErrMsg);
        returnJson.addProperty("p3DesXmlPara", p3DesXmlPara);
        returnJson.addProperty("pSign", pSign);

        Map<String,String> maps = new HashMap<String, String>();
        maps.put("pMerCode", pMerCode);
        maps.put("pErrCode", pErrCode);
        maps.put("pErrMsg", pErrMsg);
        maps.put("p3DesXmlPara", p3DesXmlPara);
        maps.put("pSign", pSign);
        String action = json.getString("pWebUrl");
        render("/PNR/PNRPayment/pnr.html",action,maps);

    }

    /**
     * 投标
     * @param memberId p2p平台用户id
     * @param type 接口类型
     * @param platfrom 平台id
     * @param json 参数
     * @param pnrparams 参数2
     * @return ChinaPnrBaseService(继承Map)
     */
    private static ChinaPnrBaseService caseTender(long memberId,int type,int platform,JSONObject json,
                                                  JSONObject pnrparams) {
        String pRegType = json.get("pRegType") + ""; // 登记方式 1:手动投标 2:自动投标
        if ("1".equals(pRegType)) {
            return caseInitiativeTender(memberId, type, platform, json, pnrparams);
        } else if ("2".equals(pRegType)) {
            caseAutoTender(memberId, type, platform, json, pnrparams);
        }
        return null;
    }

    private static ChinaPnrBaseService caseInitiativeTender(long memberId,int type,int platform,JSONObject json,
                                                            JSONObject pnrparams){

        JsonObject p2pExtra = new JsonObject();
        p2pExtra.addProperty("pRegType", json.getString("pRegType"));
        p2pExtra.addProperty("pBidNo", json.getString("pBidNo"));
        p2pExtra.addProperty("pFee", json.getString("pFee"));
        p2pExtra.addProperty("pMemo1",memberId);
        p2pExtra.addProperty("platform",platform);

        addEvent(memberId, type+200, platform, json.getString("pMerBillNo"), json.getString("pWebUrl"), json.getString("pS2SUrl"), p2pExtra.toString(), null);
        addDealDetail(memberId, platform, json.getString("pMerBillNo"), type, Double.parseDouble( json.getString("pAuthAmt")), false, "汇付天下-主动投标");
//		 modifyHasInvestedAmount(json.getString("pBidNo"), json.getString("pAuthAmt"),json,false);

        ChinaPnrService maps = new ChinaPnrService();

        String borrowerDetails =  "[{\"BorrowerCustId\":\""+pnrparams.getString("bidContractNo")+"\","
                + "\"BorrowerAmt\":\""+json.getString("pTrdAmt")+"\","
                + "\"BorrowerRate\":\""+BorrowerRate+"\","
                + "\"ProId\":\""+json.getString("pBidNo")+"\"}]";;
        String retUrl = ChinaPnrConfig.getProperty("chinapnr_retUrl");
        maps.putValue("Version", ChinaPnrConfig.getProperty("chinapnr_version_2.0")).putValue("CmdId", ChinaPnrConstants.CMD_INITIATIVETENDER)
                .putValue("MerCustId", ChinaPnrConfig.getProperty("chinapnr_merCustId") ).putValue("OrdId", json.getString("pMerBillNo"))
                .putValue("OrdDate", DateUtil.getDate())
                .putValue("TransAmt",  json.getString("pAuthAmt"))
                .putValue("UsrCustId", json.getString("pAccount"))
                .putValue("MaxTenderRate", MaxTenderRate)
                .putValue("BorrowerDetails", borrowerDetails)
                .putValue("BgRetUrl", ChinaPnrConfig.getProperty("chinapnr_retUrlBg"))
                .putValue("RetUrl", retUrl)
                .putValue("IsFreeze", "Y")
                .putValue("FreezeOrdId",  json.getString("pMerBillNo")+frezeeFlag);
//		maps.put("ReqExt", ReqExt);
        maps.setChkValue();

        return maps;
    }


    /**
     * 自动投标(ws)
     * @param memberId p2p平台用户id
     * @param type 接口类型
     * @param platfrom 平台id
     * @param json 参数
     * @param pnrparams 参数2
     */
    private static void caseAutoTender(long memberId,int type,int platform,JSONObject json,
                                       JSONObject pnrparams){

        JsonObject p2pExtra = new JsonObject();
        p2pExtra.addProperty("pRegType", json.getString("pRegType"));
        p2pExtra.addProperty("pBidNo", json.getString("pBidNo"));
        p2pExtra.addProperty("pFee", json.getString("pFee"));
        p2pExtra.addProperty("pMemo1",memberId);
        p2pExtra.addProperty("platform", platform);

        addEvent(memberId, type+200, platform, json.getString("pMerBillNo"), json.getString("pWebUrl"), json.getString("pS2SUrl"), p2pExtra.toString(), null);
        addDealDetail(memberId, platform, json.getString("pMerBillNo"), type, Double.parseDouble(json.getString("pAuthAmt")), false, "汇付天下-自动投标");
//		modifyHasInvestedAmount(json.getString("pBidNo"), json.getString("pAuthAmt"),json,true);

        ChinaPnrService service = new ChinaPnrService();
        ChinaPnrReqModel model = new ChinaPnrReqModel();
        model.setIsFreeze("Y");
        model.setFreezeOrdId( json.getString("pMerBillNo")+frezeeFlag);
        model.setOrdId( json.getString("pMerBillNo"));
        model.setOrdDate(DateUtil.getDate());
        model.setTransAmt( json.getString("pAuthAmt"));
        model.setUsrCustId(json.getString("pAccount"));
        model.setMaxTenderRate(MaxTenderRate);

        String borrowerDetails =  "[{\"BorrowerCustId\":\""+pnrparams.getString("bidContractNo")+"\","
                + "\"BorrowerAmt\":\""+json.getString("pTrdAmt")+"\","
                + "\"BorrowerRate\":\""+BorrowerRate+"\","
                + "\"ProId\":\""+json.getString("pBidNo")+"\"}]";
        model.setBorrowerDetails(borrowerDetails);

        String result = service.doAutoTender(model);

        JsonObject resultObj  = new JsonParser().parse(result).getAsJsonObject();
        String pMerCode = resultObj.get("MerCustId").getAsString();
        String pErrCode = "000".equals(resultObj.get("RespCode").getAsString()) ? "MG00000F"
                : resultObj.get("RespCode").getAsString();
        String pErrMsg = resultObj.get("RespDesc").getAsString();
        String p3DesXmlPara = "";
        JsonObject desjson = new JsonObject();
        desjson.addProperty("pMerBillNo", resultObj.get("OrdId").getAsString());
        desjson.addProperty("pMerDate", resultObj.get("OrdDate").getAsString());
        desjson.addProperty("pAccountDealNo", resultObj.get("UsrCustId").getAsString());
        desjson.addProperty("pBidDealNo", resultObj.get("OrdId").getAsString());
        desjson.addProperty("pBidNo", "");
        desjson.addProperty("pContractNo", "");
        desjson.addProperty("pBusiType", "");
        desjson.addProperty("pFee", json.getString("pFee"));
        desjson.addProperty("pAuthAmt", "");
        desjson.addProperty("pTrdAmt", resultObj.get("TransAmt").getAsString());
        desjson.addProperty("pTransferAmt", "");
        desjson.addProperty("pAccount", resultObj.get("UsrCustId").getAsString());
        desjson.addProperty("pStatus", "0");
        desjson.addProperty("pP2PBillNo",resultObj.get("FreezeTrxId").getAsString());
        desjson.addProperty("pIpsTime", "");
        desjson.addProperty("pMemo1",memberId);
        p3DesXmlPara = buildP3DesXmlPara(desjson);
        Map<String,String> maps = new HashMap<String, String>();
        String pSign = Codec.hexMD5(pMerCode+pErrCode+pErrMsg+p3DesXmlPara+Constants.ENCRYPTION_KEY);
        maps.put("pMerCode", pMerCode);
        maps.put("pErrCode", pErrCode);
        maps.put("pErrMsg", pErrMsg);
        maps.put("p3DesXmlPara", p3DesXmlPara);
        maps.put("pSign", pSign);
        JsonObject returnJson = new JsonObject();
        returnJson.addProperty("pMerCode", pMerCode);
        returnJson.addProperty("pErrCode", pErrCode);
        returnJson.addProperty("pErrMsg", pErrMsg);
        returnJson.addProperty("p3DesXmlPara", p3DesXmlPara);
        returnJson.addProperty("pSign", pSign);
        renderJSON(returnJson.toString());
    }

    /**
     * 债权转让(登记债权转让)
     * @param memberId p2p平台用户id
     * @param type 接口类型
     * @param platfrom 平台id
     * @param json 参数
     * @param pnrparams 参数2
     * @return ChinaPnrBaseService(继承Map)
     */
    private static ChinaPnrBaseService caseCreditAssign(long memberId,int type,int platform,JSONObject json,
                                                        JSONObject pnrparams) {


        String BorrowerCreditAmt = pnrparams.getString("BorrowerCreditAmt");

        pnrparams.put("bidDetails",  "{\"BidDetails\":[{\"BidOrdId\":\""+json.getString("pCreMerBillNo")+"\","
                + "\"BidOrdDate\":\""+pnrparams.getString("orderDate")+"\",\"BidCreditAmt\":\""+BorrowerCreditAmt+"\","
                + "\"BorrowerDetails\":[{\"BorrowerCustId\":\""+pnrparams.getString("pBidAccount")+"\","
                + "\"BorrowerCreditAmt\":\""+BorrowerCreditAmt+"\",\"PrinAmt\":\""+pnrparams.getString("printAmt")+"\","
                + "\"ProId\":\""+json.getString("pBidNo")+"\"}]}]}");

        String version = ChinaPnrConfig.getProperty("chinapnr_version");
        String cmdId = ChinaPnrConstants.CMD_CREDITASSIGN;
        String merCustId = ChinaPnrConfig.getProperty("chinapnr_merCustId");
        String sellCustId = json.getString("pFromAccount");
        String creditAmt = BorrowerCreditAmt;
        String creditDealAmt = json.getString("pPayAmt");
        String bidDetails = pnrparams.getString("bidDetails");
        String fee = json.getString("pFromFee");
        String buyCustId = json.getString("pToAccount");
        String ordId = json.getString("pMerBillNo");
        String ordDate = DateUtil.getDate();
        String bgRetUrl = ChinaPnrConfig.getProperty("chinapnr_retUrlBg");
        String retUrl = ChinaPnrConfig.getProperty("chinapnr_retUrl");

        JsonObject p2pExtra = new JsonObject();
        p2pExtra.addProperty("pMemo1", memberId);
        p2pExtra.addProperty("pWebUrl", json.getString("pWebUrl"));
        p2pExtra.addProperty("pS2SUrl", json.getString("pS2SUrl"));
        p2pExtra.addProperty("platform", platform);

        double fee1=Double.valueOf(fee);
        ChinaPnrService maps = new ChinaPnrService();
        if (fee1 != 0) {
            maps.putValue("Version", version)
                    .putValue("CmdId", cmdId)
                    .putValue("MerCustId", merCustId)
                    .putValue("SellCustId", sellCustId)
                    .putValue("CreditAmt", creditAmt)
                    .putValue("CreditDealAmt", creditDealAmt)
                    .putValue("BidDetails", bidDetails)
                    .putValue("DivDetails", "[{\"DivAcctId\":\"MDT000001\",\"DivAmt\":\""+fee+"\"}]")
                    .putValue("Fee", fee)
                    .putValue("BuyCustId", buyCustId)
                    .putValue("OrdId", ordId)
                    .putValue("OrdDate", ordDate)
                    .putValue("BgRetUrl", bgRetUrl)
                    .putValue("RetUrl", retUrl)
                    .setChkValue();
        }else{

            maps.putValue("Version", version)
                    .putValue("CmdId", cmdId)
                    .putValue("MerCustId", merCustId)
                    .putValue("SellCustId", sellCustId)
                    .putValue("CreditAmt", creditAmt)
                    .putValue("CreditDealAmt", creditDealAmt)
                    .putValue("BidDetails", bidDetails)
                    .putValue("Fee", "0.00"/*fee*/)
                    .putValue("BuyCustId", buyCustId)
                    .putValue("OrdId", ordId)
                    .putValue("OrdDate", ordDate)
                    .putValue("BgRetUrl", bgRetUrl)
                    .putValue("RetUrl", retUrl)
                    .setChkValue();
        }

        addEvent(memberId, type+200, platform, json.getString("pMerBillNo"),json.getString("pWebUrl"),json.getString("pS2SUrl"), p2pExtra.toString(), null);
        addDealDetail(memberId, platform,  json.getString("pMerBillNo"), type, Double.parseDouble(creditAmt), false, "汇付天下-债权转让");

        return maps;
    }

    /**
     *自动投标计划(开启)
     * @param memberId p2p平台用户id
     * @param type 接口类型
     * @param platfrom 平台id
     * @param json 参数
     * @param pnrparams 参数2
     * @return ChinaPnrBaseService(继承Map)
     */
    private static ChinaPnrBaseService caseAutoTenderPlan(long memberId,int type,int platform,JSONObject json) {

        String version = ChinaPnrConfig.getProperty("chinapnr_version");
        String cmdId = ChinaPnrConstants.CMD_AUTOTENDERPLAN;
        String merCustId = ChinaPnrConfig.getProperty("chinapnr_merCustId");
        String usrCustId = json.getString("pIpsAcctNo"); // "6000060000597572";
        // 投标计划类型：汇付申请开通的是完全授权
        String tenderPlanType = ChinaPnrConfig
                .getProperty("tenderPlanType"); // P--部分授权W--完全授权
        String retUrl = ChinaPnrConfig.getProperty("chinapnr_retUrl");
        String merPriv = json.getString("pMerBillNo"); // 商户流水号

        ChinaPnrService maps = new ChinaPnrService();

        maps.putValue("Version", version).putValue("CmdId", cmdId)
                .putValue("MerCustId", merCustId)
                .putValue("UsrCustId", usrCustId)
                .putValue("TenderPlanType", tenderPlanType)
                .putValue("RetUrl", retUrl).putValue("MerPriv", merPriv);
        maps.setChkValue();

        JsonObject extra = new JsonObject();
        extra.addProperty("pMemo1", memberId);
        extra.addProperty("pMerBillNo", json.getString("pMerBillNo"));
        //add by 20141220
        extra.addProperty("platform", platform);
        //added

        addEvent(memberId, type+200, platform, json.getString("pMerBillNo"),json.getString("pWebUrl"),json.getString("pS2SUrl"),extra.toString(), null);
        addDealDetail(memberId, platform,  json.getString("pMerBillNo"), type, 0.00, false, "汇付天下-自动投标计划");

        return maps;
    }

    /**
     *用户绑卡
     * @param memberId p2p平台用户id
     * @param type 接口类型
     * @param platfrom 平台id
     * @param json 参数
     * @param pnrparams 参数2
     * @return ChinaPnrBaseService(继承Map)
     */
    private static ChinaPnrBaseService caseUserBindCard(long memberId,int type , int platform,JSONObject pnrparams) {

        String usrCustId = pnrparams.getString("pIpsAcctNo");
        ChinaPnrService maps = new ChinaPnrService();
        String version = ChinaPnrConfig.getProperty("chinapnr_version");
        String cmdId = ChinaPnrConstants.CMD_USERBINDCARD;
        String merCustId = ChinaPnrConfig.getProperty("chinapnr_merCustId");
        String bgRetUrl = ChinaPnrConfig.getProperty("chinapnr_retUrlBg");
        String serialNumber = pnrparams.getString("pMerBillNo")+USER_BIND_CARD_FLAG;

        maps.putValue("Version", version).putValue("CmdId", cmdId).putValue("MerCustId", merCustId)
                .putValue("UsrCustId", usrCustId).putValue("BgRetUrl", bgRetUrl).putValue("MerPriv", serialNumber).setChkValue();

        addEvent(memberId, type, platform, serialNumber, pnrparams.getString("pWebUrl"),pnrparams.getString("pS2SUrl"), null, "汇付天下-用户绑卡");

        return maps;
    }

    /**
     * 网银充值
     * @param memberId p2p平台用户id
     * @param type 接口类型
     * @param platfrom 平台id
     * @param json 参数
     * @param pnrparams 参数2
     * @return ChinaPnrBaseService(继承Map)
     */
    private static ChinaPnrBaseService caseNetSave(long memberId,int type,int platform,JSONObject json) {

        String version = ChinaPnrConfig.getProperty("chinapnr_version");
        String cmdId = ChinaPnrConstants.CMD_NETSAVE;
        String merCustId = ChinaPnrConfig.getProperty("chinapnr_merCustId");
        String usrCustId = json.getString("pIpsAcctNo");
//		String usrCustId = ChinaPnrConfig.getProperty("chinapnr_merCustId");
        String ordId = json.getString("pMerBillNo");
        String transAmt = json.getString("pTrdAmt");
        String pChannelType = json.getString("pChannelType");
        String gateBusiId = "2".equals(pChannelType) ? "WH" : "B2B";
        String bgRetUrl = ChinaPnrConfig.getProperty("chinapnr_retUrlBg");
        String retUrl = ChinaPnrConfig.getProperty("chinapnr_retUrl");

        String now = json.getString("pTrdDate");
        JsonObject p2pExtra = new JsonObject();
        p2pExtra.addProperty("pMerBillNo", ordId);
        p2pExtra.addProperty("pMemo1", memberId);
        p2pExtra.addProperty("platform", platform);
        ChinaPnrService maps = new ChinaPnrService();

        maps.putValue("Version", version).putValue("CmdId", cmdId)
                .putValue("MerCustId", merCustId)
                .putValue("UsrCustId", usrCustId).putValue("OrdId", ordId)
                .putValue("OrdDate", now).putValue("TransAmt", transAmt)
                .putValue("BgRetUrl", bgRetUrl)
                .putValue("GateBusiId", gateBusiId)
                .putValue("RetUrl", retUrl);
        maps.setChkValue();

        addEvent(memberId, type+200, platform, json.getString("pMerBillNo"),json.getString("pWebUrl"),json.getString("pS2SUrl"), p2pExtra.toString(), null);
        addDealDetail(memberId, platform, json.getString("pMerBillNo"), type, Double.parseDouble(transAmt), false, "网银充值");

        return maps;
    }

    /**
     * 还款(p2p是采用单笔交易,还款给多个用户, 在汇付这边是针对单笔单笔操作,所以先将p2p穿过来的数组记录保存至数据库
     * ,然后发请求到汇付,如果执行成功,则修改数据库状态字段,批量全部执行完之后,在查询数据库失败记录条数,如果有失败记录
     * ,则返回失败结果至p2p)
     * @param memberId p2p平台用户id
     * @param type 接口类型
     * @param platfrom 平台id
     * @param json 参数
     * @param pnrparams 参数2
     * @return ChinaPnrBaseService(继承Map)
     */
    private static void caseRepayment(long memberId,int type,long platform,JSONObject json,ErrorInfo error) {

        addEvent(memberId, type+200, platform, json.getString("pMerBillNo"),null,null, null, null);

        Object pDetails = json.get("pDetails");
        JSONArray jsonArr = null;
        if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
            JSONObject pDetail = (JSONObject)pDetails;
            JSONObject pRow = pDetail.getJSONObject("pRow");

            jsonArr = new JSONArray();
            jsonArr.add(pRow);
        } else {
            jsonArr = json.getJSONArray("pDetails");
        }
        ChinaPnrReqModel model =null;
        ChinaPnrService service = null;
        String nowDate = null;
        Repayment repayment = null;
        JsonObject resultObj = null;
        int i = 0;
        for(Object obj : jsonArr){
            i++;
            nowDate = DateUtil.getDate();
            JSONObject detail = (JSONObject)obj;

            repayment = new Repayment();
            repayment.ordId =json.getString("pMerBillNo")+i;
            repayment.ordDate = nowDate;
            repayment.outCustId = json.getString("pOutAcctNo");
            repayment.subOrdId = detail.getString("pCreMerBillNo");
            repayment.subOrdDate = nowDate;
            repayment.transAmt = detail.getString("pInAmt");
            repayment.fee = detail.getString("pInFee");
            repayment.inCustId = detail.getString("pInAcctNo");
            String reqExtParams = "{\"ProId\":\""+json.getString("pBidNo")+"\"}";
            repayment.reqExt = reqExtParams;
            repayment.memberId = json.getString("pMerBillNo");
            repayment.pBidNo = json.getString("pBidNo");
            repayment.pMerBillNo = json.getString("pMerBillNo");
			
			/*repayment.saveRepayment(error);
			if(error.code<0){
				error();
			}*/

            model = new ChinaPnrReqModel();
            model.setOrdId(json.getString("pMerBillNo")+i);
            model.setOrdDate(nowDate);
            model.setOutCustId(json.getString("pOutAcctNo"));
            model.setSubOrdId(detail.getString("pCreMerBillNo"));
            model.setSubOrdDate(nowDate);
            model.setTransAmt(detail.getString("pInAmt"));
            model.setFee(detail.getString("pInFee"));
            model.setInCustId(detail.getString("pInAcctNo"));
            model.setReqExt(reqExtParams);
            //add  20141225
            JsonArray array = new JsonArray();
            JsonObject divDetails = new JsonObject();
            divDetails.addProperty("DivCustId", ChinaPnrConfig.getProperty("chinapnr_merCustId"));
            divDetails.addProperty("DivAcctId", "MDT000001");
            divDetails.addProperty("DivAmt", detail.getString("pInFee"));
            array.add(divDetails);
            model.setDivDetails(array.toString());

            service = new ChinaPnrService();
            String result = service.doRepayment(model);

            resultObj  = new JsonParser().parse(result).getAsJsonObject();
            //放款成功,修改数据库状态
            //add by 20141220 添加351重复订单为成功
			/*if("000".equals(resultObj.get("RespCode").getAsString())|"351".equals(resultObj.get("RespCode").getAsString())){
				repayment.modifyStatus(error);
				if(error.code<0){
					error();
				}
			}*/
            if("000".equals(resultObj.get("RespCode").getAsString())){
                repayment.saveRepayment(error);

                if(error.code<0){
                    Logger.error("保存还款记录失败");
                    error();
                }

            }

            if("000".equals(resultObj.get("RespCode").getAsString())|"351".equals(resultObj.get("RespCode").getAsString())){
                repayment.modifyStatus(error);
                if(error.code<0){
                    error();
                }
            }
        }

        int failNum = repayment.queryRepaymentFailByMerBillNo(error);
        JsonObject returnObj = new JsonObject();
        returnObj.addProperty("pMerCode", resultObj.get("MerCustId").getAsString());

        if(failNum == 0){
            returnObj.addProperty("pErrCode", "MG00000F");
            returnObj.addProperty("pErrMsg", "操作成功");
        }else{
            returnObj.addProperty("pErrCode", "MG00001F");
            returnObj.addProperty("pErrMsg", " 记录操作失败");
        }
        JsonObject desJson = new JsonObject();
        desJson.addProperty("pMemo1",memberId);
        desJson.addProperty("pMemo3", json.getString("pMemo3"));
        desJson.addProperty("pMerBillNo", json.getString("pMerBillNo"));
        String arg3DesXmlPara = buildP3DesXmlPara(desJson);
        returnObj.addProperty("p3DesXmlPara", arg3DesXmlPara);
        String pSign = Codec.hexMD5(returnObj.get("pMerCode").getAsString()+returnObj.get("pErrCode").getAsString()
                +returnObj.get("pErrMsg").getAsString()+arg3DesXmlPara+Constants.ENCRYPTION_KEY);
        returnObj.addProperty("pSign", pSign);
        Map<String,String> maps = new HashMap<String, String>();
        maps.put("pMerCode", returnObj.get("pMerCode").getAsString());
        maps.put("pErrCode", returnObj.get("pErrCode").getAsString());
        maps.put("pErrMsg", returnObj.get("pErrMsg").getAsString());
        maps.put("p3DesXmlPara", arg3DesXmlPara);
        maps.put("pSign", pSign);
        String action = json.getString("pWebUrl");
        String asynAction = json.getString("pS2SUrl");
        Map<String,String> front_params_temp = null;
        Map<String,Object> front_params = new HashMap<String, Object>();
        if(P2pCommonUtils.isAsynchSoapNames(ChinaPnrConstants.CMD_REPAYMENT)){
            Logger.debug("接口%s支持异步回调",ChinaPnrConstants.CMD_REPAYMENT);
            Map<String,Object> params = buildSubmitParams(action,asynAction, maps);
            String result = submitByAsynch(params);  //异步提交至P2P
            JsonObject wsJson = new JsonParser().parse(result).getAsJsonObject();
            front_params.put(P2P_URL_KEY, wsJson.get("pPostUrl").getAsString());
            String value = buildP3DesXmlPara(wsJson);
            front_params_temp = new HashMap<String, String>();
            front_params_temp.put("result", value);
            front_params.put(P2P_BASEPARAMS, front_params_temp);
        }
        submitByFront(front_params);
    }




    /**
     * 放款(p2p是采用单笔交易,放款给多个用户, 在汇付这边是针对单笔单笔操作,所以先将p2p穿过来的数组记录保存至数据库
     * ,然后发请求到汇付,如果执行成功,则修改数据库状态字段,批量全部执行完之后,在查询数据库失败记录条数,如果有失败记录
     * ,则返回失败结果至p2p。注意：在提交至汇付时,因为冻结订单号在spay,p2p都没有做存储,所以在这里约定俗成,将订单号+“1”)
     * @param memberId p2p平台用户id
     * @param type 接口类型
     * @param platfrom 平台id
     * @param json 参数
     * @param pnrparams 参数2
     * @return ChinaPnrBaseService(继承Map)
     */
    private static void caseLoans(long memberId,int type,long platform,String argMerCode,JSONObject json,JSONObject pnrParams,ErrorInfo error) {

        addEvent(memberId, type+200, platform, json.getString("pMerBillNo"),null,null, null, null);

        Object pDetails = json.get("pDetails");
        Object extraPDetails = pnrParams.get("pDetails");
        JSONArray jsonArr = null;
        if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
            JSONObject pDetail = (JSONObject)pDetails;
            JSONObject pRow = pDetail.getJSONObject("pRow");

            jsonArr = new JSONArray();
            jsonArr.add(pRow);
        } else {
            jsonArr = json.getJSONArray("pDetails");
        }
        JSONArray extraPDetailsjsonArr = null;
        if (extraPDetails.getClass().isAssignableFrom(JSONObject.class)) {
            JSONObject pDetail = (JSONObject)extraPDetails;
            JSONObject pRow = pDetail.getJSONObject("pRow2");

            extraPDetailsjsonArr = new JSONArray();
            extraPDetailsjsonArr.add(pRow);
        } else {
            extraPDetailsjsonArr = pnrParams.getJSONArray("pDetails");
        }

        Loans loans = null;
        String result ="";
        JsonObject resultObj = null;
        for (int i = 0;i<jsonArr.size();i++) {
            JSONObject pRow = (JSONObject)jsonArr.get(i);
            JSONObject pnrRow = (JSONObject)extraPDetailsjsonArr.get(i);
            loans = new Loans();
            loans.oriMerBillNo = pRow.getString("pOriMerBillNo");
            loans.trdAmt = pRow.getString("pTrdAmt");
            loans.fAcctType = pRow.getString("pFAcctType");
            loans.fIpsAcctNo = pRow.getString("pFIpsAcctNo");
            loans.fTrdFee = pRow.getString("pTTrdFee");
            loans.tAcctType = pRow.getString("pTAcctType");
            loans.tIpsAcctNo = pRow.getString("pTIpsAcctNo");
            loans.tTrdFee = pRow.getString("pTTrdFee");
            loans.ipsBillNo = pnrRow.getString("ipsBillNo");
            loans.merBillNo = json.getString("pMerBillNo");
            loans.bidNo = json.getString("pBidNo");
			/*loans.saveLoans(error);
			if(error.code<0){
				error();
			}*/
            ChinaPnrService service = new ChinaPnrService();
            ChinaPnrReqModel model = new ChinaPnrReqModel();
            model.setOrdId(pRow.getString("pOriMerBillNo"));
            model.setOrdDate(json.getString("pDate"));
            model.setOutCustId(pRow.getString("pFIpsAcctNo"));
            model.setTransAmt(pRow.getString("pTrdAmt"));
            model.setFee(pRow.getString("pTTrdFee"));
            model.setSubOrdId(pRow.getString("pOriMerBillNo"));
            model.setSubOrdDate(json.getString("pDate"));
            model.setInCustId(pRow.getString("pTIpsAcctNo"));
            model.setUnFreezeOrdId(pRow.getString("pOriMerBillNo")+1);
            model.setFreezeTrxId(pnrRow.getString("ipsBillNo"));
            String reqExt =  "{\"ProId\":\""+json.getString("pBidNo")+"\"}";
            model.setReqExt(reqExt);

            JsonArray arr = new JsonArray();
            JsonObject divDetail = new JsonObject();
            divDetail.addProperty("DivCustId", ChinaPnrConfig.getProperty("chinapnr_merCustId"));
            divDetail.addProperty("DivAcctId", "MDT000001");
            divDetail.addProperty("DivAmt", pRow.getString("pTTrdFee"));
            arr.add(divDetail);
            model.setDivDetails(arr.toString());


            result = service.doLoans(model);
            resultObj  = new JsonParser().parse(result).getAsJsonObject();
            //放款成功,修改数据库状态
			/*if("000".equals(resultObj.get("RespCode").getAsString())|"345".equals(resultObj.get("RespCode").getAsString())){
				loans.modifyStatus(error);
				if(error.code<0){
					error();
				}
			}*/

            if("000".equals(resultObj.get("RespCode").getAsString())){
                loans.saveLoans(error);

                if(error.code<0){
                    Logger.error("保存放款记录失败");
                    error();
                }

            }

            if("000".equals(resultObj.get("RespCode").getAsString())|"351".equals(resultObj.get("RespCode").getAsString())){
                loans.modifyStatus(error);

                if(error.code<0){
                    error();
                }
            }
        }

        int failNum = loans.queryLoansFailByMerBillNo(error);
        JsonObject respJson = new JsonObject();
        respJson.addProperty("pMerCode", argMerCode);
        JsonObject arg3DesXmlParaJson = new JsonObject();
        arg3DesXmlParaJson.addProperty("pMerBillNo", json.getString("pMerBillNo"));
        arg3DesXmlParaJson.addProperty("pTransferType", json.getString("pTransferType"));
        arg3DesXmlParaJson.addProperty("pMemo1", memberId);
        String arg3DesXmlPara = buildP3DesXmlPara(arg3DesXmlParaJson);
        if(failNum==0){
            respJson.addProperty("pErrCode", "MG00000F");
            respJson.addProperty("pErrMsg", "操作成功");
        }else{
            respJson.addProperty("pErrCode", "MG00001F");
            respJson.addProperty("pErrMsg", failNum+"比记录操作失败");
        }
        respJson.addProperty("p3DesXmlPara", arg3DesXmlPara);
        String pSign  = Codec.hexMD5(respJson.get("pMerCode").getAsString()+respJson.get("pErrCode").getAsString()
                +respJson.get("pErrMsg").getAsString()+respJson.get("p3DesXmlPara").getAsString()+Constants.ENCRYPTION_KEY);
        respJson.addProperty("pSign", pSign);

        Logger.debug(respJson.toString());
        renderJSON(respJson.toString());
    }


    /**
     * 资金解冻(该接口已开通, p2p目前没有需求调用)
     * @param pnrparams 参数
     * @return ChinaPnrBaseService(继承Map)
     */
    private static ChinaPnrBaseService caseUsrUnFreeze(JSONObject pnrparams) {
        //测试数据
        pnrparams = new JSONObject();
        pnrparams.put("ordId", Random.randomNum());
        pnrparams.put("trxId", "201411291234567890");
        //测试数据

        ChinaPnrService maps = new ChinaPnrService();
        String version = ChinaPnrConfig.getProperty("chinapnr_version");
        String cmdId = ChinaPnrConstants.CMD_USRUNFREEZE;
        String merCustId = ChinaPnrConfig.getProperty("chinapnr_merCustId");
        String ordId = pnrparams.getString("ordId");
        String trxId = pnrparams.getString("trxId");
        String bgRetUrl = ChinaPnrConfig.getProperty("chinapnr_retUrlBg");
        maps.put("Version", version);
        maps.put("CmdId", cmdId);
        maps.put("MerCustId", merCustId);
        maps.put("OrdId", ordId);
        maps.put("TrxId", trxId);
        maps.put("BgRetUrl", bgRetUrl);
        maps.setChkValue();
        return maps;
    }

    private static boolean  isUserBindCard(long memberId,int type, int platform,JSONObject json){
        Logger.debug("--------银行卡查询 start-----------------");
        ChinaPnrService service = new ChinaPnrService();
        ChinaPnrReqModel model = new ChinaPnrReqModel();
        model.setUsrCustId( json.getString("pIpsAcctNo"));
        String result = service.doQueryCardInfo(model);
        JsonObject resultJson  = new JsonParser().parse(result).getAsJsonObject();
        String code = resultJson.get("RespCode").getAsString();
        if("000".equals(code)){  //当前用户无可用的取现银行卡,取现失败
            int size = resultJson.get("UsrCardInfolist").getAsJsonArray().size();
            if(size<1){
                Logger.debug("--------该用户为绑定银行卡,将进行绑卡操作 start-----------------");
                String action = ChinaPnrConstants.DEV_URL;
                ChinaPnrBaseService maps = caseUserBindCard(memberId,type,platform,json);
                render("/PNR/PNRPayment/pnr.html", action, maps);
            }
        }
        return true;
    }

    /**
     *  提现、取现
     * @param memberId p2p平台用户id
     * @param type 接口类型
     * @param platfrom 平台id
     * @param json 参数
     * @return ChinaPnrBaseService(继承Map)
     */
    private static ChinaPnrBaseService caseCash(Long memberId,int type,int platform,JSONObject json) {
        isUserBindCard(memberId,type,platform,json);
        String version = ChinaPnrConfig.getProperty("chinapnr_version_2.0");
        String cmdId = ChinaPnrConstants.CMD_CASH;
        String merCustId = ChinaPnrConfig.getProperty("chinapnr_merCustId");
        String ordId = json.getString("pMerBillNo");
        String usrCustId = json.getString("pIpsAcctNo");
        //内扣
        double transAmtTemp = (Double.parseDouble(json.getString("pTrdAmt"))-Double.parseDouble(json.getString("pMerFee")));
        String transAmt = String.format("%.2f", transAmtTemp);
        String retUrl = ChinaPnrConfig.getProperty("chinapnr_retUrl");
        String bgRetUrl = ChinaPnrConfig.getProperty("chinapnr_retUrlBg");
        String pWebUrl = json.getString("pWebUrl");
        String pS2SUrl = json.getString("pS2SUrl");
        String servFee = json.getString("pMerFee");
        String servFeeAcctId = ChinaPnrConfig.getProperty("chinapnr_fee");
        JsonObject p2pExtra = new JsonObject();
        p2pExtra.addProperty("pMemo1", memberId);
        p2pExtra.addProperty("pMemo3",  json.getString("pMemo3"));
        p2pExtra.addProperty("platform", platform);

        ChinaPnrService maps = new ChinaPnrService();
        maps.putValue("Version", version)
                .putValue("CmdId", cmdId)
                .putValue("MerCustId", merCustId)
                .putValue("OrdId", ordId)
                .putValue("UsrCustId", usrCustId)
                .putValue("TransAmt", transAmt)
                .putValue("ServFee", servFee)
                .putValue("ServFeeAcctId", servFeeAcctId)
                .putValue("RetUrl", retUrl).putValue("BgRetUrl", bgRetUrl) ;
        maps.setChkValue();

        addEvent(memberId, type+200, platform, json.getString("pMerBillNo"),pWebUrl,pS2SUrl, p2pExtra.toString(), null);
        addDealDetail(memberId, platform, ordId, type, Double.parseDouble(transAmt), false, "提现");

        return maps;
    }

    /**
     *  后台查询余额(ws)
     * @param memberId p2p平台用户id
     * @param type 接口类型
     * @param platfrom 平台id
     * @param argIpsAccount 支付平台账号
     */
    private static void caseQueryBalanceBg(long memberId,int type,long platform,String argIpsAccount) {

        addEvent(memberId, type+200, platform, null,null,null, null, "汇付天下-后台余额查询");

        String version = ChinaPnrConfig.getProperty("chinapnr_version");
        String cmdId = ChinaPnrConstants.CMD_QUERYBALANCEBG;
        String merCustId = ChinaPnrConfig.getProperty("chinapnr_merCustId");

        ChinaPnrReqModel model = new ChinaPnrReqModel();
        ChinaPnrService service = new ChinaPnrService();
        model.setVersion(version);
        model.setCmdId(cmdId);
        model.setMerCustId(merCustId);
        model.setUsrCustId(argIpsAccount);
        String results = service.doQueryBalanceBg(model);
        Logger.debug("汇付天下-后台余额查询:" + results);
        JsonParser parser = new JsonParser();
        JsonObject resultJson = parser.parse(results).getAsJsonObject();
        String pMerCode = resultJson.get("MerCustId").getAsString();
        String pErrCode = "000".equals(resultJson.get("RespCode")
                .getAsString()) ? "MG00000F" : resultJson.get("RespCode")
                .getAsString();
        String pErrMsg = resultJson.get("RespDesc").getAsString();
        JSONObject json = new JSONObject();
        json.put("pMerCode", pMerCode);
        json.put("pErrCode", pErrCode);
        json.put("pErrMsg", pErrMsg);
        json.put("pIpsAcctNo", resultJson.get("UsrCustId").getAsString());
        json.put("pBalance", resultJson.get("AvlBal").getAsString());
        json.put("pLock", resultJson.get("FrzBal").getAsString());
        json.put("pNeedstl", " ");
        json.put("pAccBalance", resultJson.get("AcctBal").getAsString());
        StringBuffer pSignbuBuffer = new StringBuffer();
        pSignbuBuffer.append(pMerCode).append(pErrCode).append(pErrMsg)
                .append(params.get("UsrCustId"))
                .append(params.get("AvlBal")).append(params.get("FrzBal"))
                .append(" ").append(Constants.ENCRYPTION_KEY);
        String pSign = Codec.hexMD5(pSignbuBuffer.toString());
        json.put("pSign", pSign);
        renderJSON(json.toString());
    }

    /**
     *  查询余额(页面) [p2p未有业务调用、此接口开通]
     * @param argIpsAccount 支付平台账号
     */
    private static ChinaPnrBaseService caseQueryBalance(String argIpsAccount) {
        ChinaPnrService maps = new ChinaPnrService();
        String version = ChinaPnrConfig.getProperty("chinapnr_version");
        String cmdId = ChinaPnrConstants.CMD_QUERYBALANCE;
        String merCustId = ChinaPnrConfig.getProperty("chinapnr_merCustId");
        maps.putValue("Version", version).putValue("CmdId", cmdId)
                .putValue("MerCustId", merCustId)
                .putValue("UsrCustId", argIpsAccount);
        maps.setChkValue();
        return maps;
    }

    /**
     * 放款交易状态查询(在放款接口中,spay将放款记录存入了数据库中,所以在放款交易状态查询的时候,直接根据p2p传过来的流水号去查询
     * 该批放款记录信息,获取流水号,遍历提交到汇付,查询结果成功后累加变量,最后将变量与数据库条数比较,相等,则提示成功,反之失败)
     * @param memberId p2p平台用户id
     * @param type 接口类型
     * @param platfrom 平台id
     * @param json 参数
     * @param pnrparams 参数2
     */
    private static void caseQueryTransStatByLoans(long memberId,int type,long platform,JSONObject json,JSONObject pnrparams){
        String queryTransType = QueryTransType.LOANS;
        ChinaPnrReqModel model = new ChinaPnrReqModel();
        ChinaPnrService service = new ChinaPnrService();
        String ordId = json.getString("pMerBillNo");
        ErrorInfo error = new ErrorInfo();
        List<t_loans_details> list = Loans.queryLoanListByBillNo(ordId, error);
        if( error.code<0){
            Application.error(error.msg);
        }
        if(list !=null && list.size()>0){
            int count = list.size();
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
            String OrdDate = format.format(new Date());
            String result  = "";
            int i = 0;
            for(t_loans_details details : list ){
                model.setOrdId(details.oriMerBillNo);
                OrdDate = format.format(details.time);
                model.setOrdDate(OrdDate);
                model.setQueryTransType(queryTransType);
                result = service.doQueryTransStat(model);
                JsonObject resultJson = new JsonParser().parse(result).getAsJsonObject();
                String respCode = resultJson.get("RespCode").getAsString();
                if("000".equals(respCode)){
                    i++;
                }
            }
            String merCustId="MG00000F";
            String respCode="MG00000F";
            String respDesc="成功";
            JsonObject desJson = new JsonObject();
            desJson.addProperty("pTradeStatue",PTradeStatue_S);
            String desValue = buildP3DesXmlPara(desJson);
            String pSign = hasMD5(merCustId,respCode,respDesc,desValue);
            if(count != i){

                merCustId="999";
                respCode="999";
                respDesc="失败";
                desJson.addProperty("pTradeStatue",PTradeStatue_F);
                desValue = buildP3DesXmlPara(desJson);
                pSign = hasMD5(merCustId,respCode,respDesc,desValue);

            }
            JsonObject returnJson = new JsonObject();
            returnJson.addProperty("pMerCode", merCustId);
            returnJson.addProperty("pErrCode",respCode);
            returnJson.addProperty("pErrMsg", respDesc);
            returnJson.addProperty("p3DesXmlPara", desValue);
            returnJson.addProperty("pSign", pSign);
            renderJSON(returnJson.toString());

        }
    }

    /**
     * 还款交易状态查询(在还款接口中,spay将还款记录存入了数据库中,所以在还款交易状态查询的时候,直接根据p2p传过来的流水号去查询
     * 该批还款记录信息,获取流水号,遍历提交到汇付,查询结果成功后累加变量,最后将变量与数据库条数比较,相等,则提示成功,反之失败)
     * @param memberId p2p平台用户id
     * @param type 接口类型
     * @param platfrom 平台id
     * @param json 参数
     * @param pnrparams 参数2
     */
    private static void caseQueryTransStatByRepayment(long memberId,int type,long platform,JSONObject json,JSONObject pnrparams){
        String queryTransType = QueryTransType.REPAYMENT;
        ChinaPnrReqModel model = new ChinaPnrReqModel();
        ChinaPnrService service = new ChinaPnrService();
        String ordId = json.getString("pMerBillNo");
        ErrorInfo error = new ErrorInfo();
        List<t_repayment_details> list = Repayment.queryRepaymentListByBillNo(ordId, error);
        if( error.code<0){
            Application.error(error.msg);
        }
        if(list !=null && list.size()>0){
            int count = list.size();
            SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
            String OrdDate = format.format(new Date());
            String result  = "";
            int i = 0;
            for(t_repayment_details details : list ){
                model.setOrdId(details.ordId/*details.subOrdId*/);
                OrdDate = format.format(details.time);
                model.setOrdDate(OrdDate);
                model.setQueryTransType(queryTransType);
                result = service.doQueryTransStat(model);
                JsonObject resultJson = new JsonParser().parse(result).getAsJsonObject();
                String respCode = resultJson.get("RespCode").getAsString();
                if("000".equals(respCode)){
                    i++;
                }
            }
            String merCustId="MG00000F";
            String respCode="MG00000F";
            String respDesc="成功";
            JsonObject desJson = new JsonObject();
            desJson.addProperty("pTradeStatue", PTradeStatue_S);
            String desValue = buildP3DesXmlPara(desJson);
            String pSign = hasMD5(merCustId,respCode,respDesc,desValue);
            if(count != i){

                merCustId="999";
                respCode="999";
                respDesc="失败";
                desJson.addProperty("pTradeStatue", PTradeStatue_F);
                desValue = buildP3DesXmlPara(desJson);

                pSign = hasMD5(merCustId,respCode,respDesc,desValue);

            }
            JsonObject returnJson = new JsonObject();
            returnJson.addProperty("pMerCode", merCustId);
            returnJson.addProperty("pErrCode",respCode);
            returnJson.addProperty("pErrMsg", respDesc);
            returnJson.addProperty("p3DesXmlPara", desValue);
            returnJson.addProperty("pSign", pSign);
            renderJSON(returnJson.toString());
        }
    }

    /**
     * 充值交易状态查询(汇付端只提供对账信息,不是针对某个订单号做单笔查询,在p2p传入一个订单号时,spay根据开始日期以及结束日期去
     * 汇付查询对账,建议数据条数不要设置太多,一次100条, 如果为查询,系统自动查询下一个100条,直到查询到该订单信息后结束,或者在
     * 开始日期与结束日期记录全部遍历完之后才结束)
     * @param memberId p2p平台用户id
     * @param type 接口类型
     * @param platfrom 平台id
     * @param json 参数
     * @param pnrparams 参数2
     */
    private static void caseQueryTransStatByCash(long memberId,int type,long platform,JSONObject json,JSONObject pnrparams){

        ChinaPnrReqModel model = new ChinaPnrReqModel();
        ChinaPnrService service = new ChinaPnrService();
        model.setVersion(ChinaPnrConfig.getProperty("chinapnr_version"));
        model.setCmdId(ChinaPnrConstants.CMD_SAVERECONCILIATION);
        model.setMerCustId(ChinaPnrConfig.getProperty("chinapnr_merCustId"));
        model.setBeginDate(pnrparams.getString("BeginDate"));
        model.setEndDate(pnrparams.getString("EndDate"));

        String target = json.getString("pMerBillNo");
        boolean flag = false;
        int i = 1;
        int count = 1000;
        String result = "";
        JsonObject resultJson = null;
        JsonArray array = null;
        while(((i)*100)<(count+100)){
            model.setPageNum(i+"");
            model.setPageSize("100");
            result = service.doSaveReconciliation(model);
            resultJson = new JsonParser().parse(result).getAsJsonObject();
            array = resultJson.get("SaveReconciliationDtoList").getAsJsonArray();
            for(JsonElement detail : array){
                String OrdId = detail.getAsJsonObject().get("OrdId").getAsString();
                String state  = detail.getAsJsonObject().get("TransStat").getAsString();
                if(target.equals(OrdId)&&"S".equals(state)){
                    Logger.debug("第"  +i +"次查询.查询到订单号:"+target+"记录");
                    flag = true;
                    break;
                }
            }
            count  = Integer.valueOf(resultJson.get("TotalItems").getAsString());
            i++;
        }

        String merCustId="MG00000F";
        String respCode="MG00000F";
        String respDesc="成功";
        String desValue = "";
        JsonObject desJson = new JsonObject();
        desJson.addProperty("pTradeStatue", PTradeStatue_S);
        String pSign = hasMD5(merCustId,respCode,respDesc,desValue);
        if(!flag){
            merCustId="999";
            respCode="999";
            respDesc="失败";
            desValue = "";
            desJson.addProperty("pTradeStatue", PTradeStatue_F);
            pSign = hasMD5(merCustId,respCode,respDesc,desValue);

        }
        JsonObject returnJson = new JsonObject();
        returnJson.addProperty("pMerCode", merCustId);
        returnJson.addProperty("pErrCode",respCode);
        returnJson.addProperty("pErrMsg", respDesc);
        returnJson.addProperty("p3DesXmlPara", desValue);
        returnJson.addProperty("pSign", pSign);
        renderJSON(returnJson.toString());
    }

    /**
     * 充值交易状态查询
     * @param memberId p2p平台用户id
     * @param type 接口类型
     * @param platfrom 平台id
     * @param json 参数
     * @param pnrparams 参数2
     */
    private static void caseQueryTransDetailBySave(long memberId,int type,long platform,JSONObject json,JSONObject pnrparams){

        ChinaPnrReqModel model = new ChinaPnrReqModel();
        ChinaPnrService service = new ChinaPnrService();
        model.setOrdId( json.getString("pMerBillNo"));
        model.setQueryTransType(QueryTransType.SAVE);
        String result = service.doQueryTransDetail(model);
        JsonObject resultJson = new JsonParser().parse(result).getAsJsonObject();
        String merCustId="MG00000F";
        String respCode="MG00000F";
        String respDesc="成功";
        String desValue = "";
        JsonObject desJson = new JsonObject();
        if("000".equals(resultJson.get("RespCode").getAsString())){
            String transStat = resultJson.get("TransStat").getAsString();
            String stat  = TransStat.getStat(transStat, QueryTransType.SAVE);
            desJson.addProperty("pTradeStatue", stat);
        }else{
            merCustId="999";
            respCode="999";
            respDesc="失败";
            desValue = "";
            desJson.addProperty("pTradeStatue", PTradeStatue_F);
        }
        desValue = buildP3DesXmlPara(desJson);
        String pSign = hasMD5(merCustId,respCode,respDesc,desValue);

        JsonObject returnJson = new JsonObject();
        returnJson.addProperty("pMerCode", merCustId);
        returnJson.addProperty("pErrCode",respCode);
        returnJson.addProperty("pErrMsg", respDesc);
        returnJson.addProperty("p3DesXmlPara", desValue);
        returnJson.addProperty("pSign", pSign);
        renderJSON(returnJson.toString());
    }

    /**
     * 交易状态查询
     * @param memberId p2p平台用户id
     * @param type 接口类型
     * @param platfrom 平台id
     * @param json 参数
     * @param pnrparams 参数2
     */
    private static void caseQueryTransStat(long memberId,int type,long platform,JSONObject json,JSONObject pnrparams) {

        ChinaPnrReqModel model = new ChinaPnrReqModel();
        ChinaPnrService service = new ChinaPnrService();
        String ordId = json.getString("pMerBillNo");
        //01开户、02登记标的、03登记投资人、04登记担
        //保方、05登记债权转让、06自动投标签约、07自
        //动还款签约、08充值（含自动代扣充值）、09提现、
        //10解冻保证金、11还款、12转账-代偿、13转账-代偿还款、14转账-投资、15转账-债权转让、16
        //转账-结算担保收益
        String queryTransType = json.getString("pTradeType");

        Logger.debug("queryTransType : %s", queryTransType);

        if("03".equals(queryTransType)|"12".equals(queryTransType)|"11".equals(queryTransType)|"09".equals(queryTransType)|"08".equals(queryTransType)|"14".equals(queryTransType)){
            //02对应汇付天下标的交易查询
            if("03".equals(queryTransType)){
                queryTransType = QueryTransType.TENDER;
            }else if("14".equals(queryTransType)){

                caseQueryTransStatByLoans( memberId, type, platform, json, pnrparams);

            }else if("11".equals(queryTransType)){  //还款

                caseQueryTransStatByRepayment(memberId, type, platform, json, pnrparams);

            }else if("09".equals(queryTransType)){  //提现
                queryTransType = QueryTransType.CASH;
            }else if("08".equals(queryTransType)){ //充值

//				caseQueryTransStatByCash(memberId, type, platform, json, pnrparams);
                caseQueryTransDetailBySave(memberId, type, platform, json, pnrparams);

            }

            addEvent(memberId, type, platform, ordId, null, null, queryTransType, "汇付天下-交易状态查询");

            model.setOrdId(ordId);
            model.setQueryTransType(queryTransType);
            model.setOrdDate(pnrparams.getString("ordDate"));
            String result=  service.doQueryTransStat(model);
            JsonObject resultJson = new JsonParser().parse(result).getAsJsonObject();
            String respCode = resultJson.get("RespCode").getAsString();
            respCode = "000".equals(respCode)?"MG00000F":respCode;
            String respDesc = resultJson.get("RespDesc").getAsString();
            String merCustId = resultJson.get("MerCustId").getAsString();
            JsonObject desJson = new JsonObject();
            desJson.addProperty("pMerBillNo", ordId);
            desJson.addProperty("pTradeType",  json.getString("pTradeType"));
            desJson.addProperty("pMerDate",  pnrparams.getString("ordDate"));
            String pTradeStatue = TransStat.getStat(resultJson.get("TransStat").getAsString(),queryTransType);
            desJson.addProperty("pTradeStatue", pTradeStatue);

            String desValue = buildP3DesXmlPara(desJson);
            String pSign = hasMD5(merCustId,respCode,respDesc,desValue);
            JsonObject returnJson = new JsonObject();
            returnJson.addProperty("pMerCode", merCustId);
            returnJson.addProperty("pErrCode",respCode);
            returnJson.addProperty("pErrMsg", respDesc);
            returnJson.addProperty("p3DesXmlPara", desValue);
            returnJson.addProperty("pSign", pSign);
            renderJSON(returnJson.toString());

        }
    }

    /**
     * 自动投标计划关闭(p2p未有此业务,该接口已通,p2p如果需要,直接调用该接口)
     * @param pnrparams 参数
     * @return ChinaPnrBaseService(继承Map)
     */
    private static ChinaPnrBaseService caseAutoTenderPlanClose(
            JSONObject pnrparams) {
        //测试数据
        pnrparams = new JSONObject();
        pnrparams.put("usrCustId", "6000060000580857");
        //测试数据

        ChinaPnrService maps = new ChinaPnrService();
        String version = ChinaPnrConfig.getProperty("chinapnr_version");
        String cmdId = ChinaPnrConstants.CMD_AUTOTENDERPLANCLOSE;
        String merCustId = ChinaPnrConfig.getProperty("chinapnr_merCustId");
        String usrCustId = pnrparams.getString("usrCustId");
        String retUrl = ChinaPnrConfig.getProperty("chinapnr_retUrl");
        maps.putValue("Version", version).putValue("CmdId", cmdId)
                .putValue("MerCustId", merCustId)
                .putValue("UsrCustId", usrCustId)
                .putValue("RetUrl", retUrl);
        maps.setChkValue();
        return maps;
    }

    /**
     * 批量转账(商户转用户)
     * @param memberId p2p平台用户id
     * @param type 接口类型
     * @param platfrom 平台id
     * @param json 参数
     * @param pnrparams 参数2
     */
    private static void caseTransferByBatch(long memberId,int type,int platform,JSONObject json,JSONObject pnrparams){
		/*json.put("pMerBillNo", "12304568");
		json.put("pWebUrl", "http://www.baidu.com");
    	
    	JSONArray arra = new JSONArray();
    	
		JSONObject row = new JSONObject();
		row.put("transAmt", "11.15");
		row.put("inCustId", "6000060000594067");
		arra.add(row);
    	
    		
		JSONObject row1 = new JSONObject();
		row1.put("transAmt", "12.21");
		row1.put("inCustId", "6000060000582329");
		arra.add(row1);
    	pnrparams.put("pDetails", arra);*/

        String billNo = json.getString("pMerBillNo");
        addEvent(memberId, type+200,platform, json.getString("pMerBillNo"),json.getString("pWebUrl"), json.getString("pS2SUrl"), json.toString(), "汇付天下-商户转账");


        Object pDetails = pnrparams.get("pDetails");
        JSONArray jsonArr = null;

        if (pDetails.getClass().isAssignableFrom(JSONObject.class)) {
            JSONObject pDetail = (JSONObject)pDetails;
            JSONObject pRow = pDetail.getJSONObject("pRow");

            jsonArr = new JSONArray();
            jsonArr.add(pRow);
        } else {
            jsonArr = pnrparams.getJSONArray("pDetails");
        }
        int i = 0;
        String merId = ChinaPnrConfig.getProperty("chinapnr_merCustId");
        ErrorInfo error = new ErrorInfo();
        String result = "";
        int count = jsonArr.size();
        JsonObject desJson = new JsonObject();
        desJson.addProperty("pMerBillNo", billNo);
        JsonArray detailArr = new JsonArray();

        JsonObject temp = null;
        JsonObject returnDetail  = null;
        double transAmt = 0.00;
        for(Object obj : jsonArr){
            i++;
            JSONObject detail = (JSONObject)obj;

            temp = new JsonObject();
            returnDetail = new JsonObject();
            returnDetail.addProperty("transAmt", detail.getString("transAmt"));
            returnDetail.addProperty("inCustId", detail.getString("inCustId"));
            temp.add("pRow", returnDetail);
            detailArr.add(temp);

            transAmt  += Double.valueOf(detail.getString("transAmt"));

            Transfer transfer = new Transfer();
            transfer.pmerBillNo = billNo;
            transfer.orderId = billNo+i;
            transfer.transAmt = Double.valueOf(detail.getString("transAmt"));
            transfer.inCustId = detail.getString("inCustId");
            transfer.outCustId = merId;
            transfer.saveTransfer(error);
            result = caseTransfer(transfer.orderId,detail.getString("transAmt"),transfer.inCustId);
            JsonObject resultJson = new JsonParser().parse(result).getAsJsonObject();
            if("000".equals(resultJson.get("RespCode").getAsString())|"355".equals(resultJson.get("RespCode").getAsString())){
                transfer.modifyStatus(1, transfer.orderId, error);
            }
        }

        addDealDetail(memberId, platform, json.getString("pMerBillNo"), type, transAmt, false, "汇付天下-商户转账");

        desJson.add("pDetails", detailArr);
        desJson.addProperty("pMemo1", json.getString("pMemo1"));

        result = Converter.jsonToXml(desJson.toString(), "pReq", null,
                null, null);
        result =result.replaceAll("<e>", "").replaceAll("</e>", "");
        result = Encrypt.encrypt3DES(result, Constants.ENCRYPTION_KEY);

        Transfer transfer = new Transfer();
        List<t_transfer_details> list = transfer.findTransferByStatus(1, billNo, error);
        String merCustId="MG00000F";
        String respCode="MG00000F";
        String respDesc="成功";
        String desValue = result;
        if(list!=null) {
            if(list.size()!= count) {
                merCustId="MG00000F";
                respCode="999";
                respDesc="失败";
            }
        }
        ChinaPnrBaseService maps = new ChinaPnrBaseService();
        maps.put("pMerCode", merCustId);
        maps.put("pErrCode", respCode);
        maps.put("pErrMsg",respDesc);
        maps.put("p3DesXmlPara", desValue);
        maps.setPsign(new String[]{"pMerCode","pErrCode","pErrMsg","p3DesXmlPara"});
        String action = json.getString("pWebUrl");

        render("/PNR/PNRPayment/pnr.html", action, maps);
    }

    /**
     * 单笔转账(商户转用户)
     * @param memberId p2p平台用户id
     * @param type 接口类型
     * @param platfrom 平台id
     * @param json 参数
     * @param pnrparams 参数2
     */
    private static void caseTransferBySingle(long memberId, int type,
                                             int platform, JSONObject json, JSONObject pnrparams) {


        addEvent(memberId, type+200,platform, json.getString("pMerBillNo"),json.getString("pWebUrl"), json.getString("pS2SUrl"), json.toString(), "汇付天下-商户转账");
        addDealDetail(memberId, platform, json.getString("pMerBillNo"), type, Double.parseDouble(pnrparams.getString("transAmt")), false, "汇付天下-商户转账");

        String result=caseTransfer(json.getString("pMerBillNo"), pnrparams.getString("transAmt"), pnrparams.getString("inCustId"));
        JsonObject resultJson = new JsonParser().parse(result).getAsJsonObject();
        Transfer transfer = new Transfer();
        ErrorInfo error = new ErrorInfo();
        transfer.pmerBillNo = json.getString("pMerBillNo");
        transfer.orderId = json.getString("pMerBillNo");
        transfer.transAmt = Double.valueOf(pnrparams.getString("transAmt"));
        transfer.inCustId = pnrparams.getString("inCustId");
        String merId = ChinaPnrConfig.getProperty("chinapnr_merCustId");
        transfer.outCustId = merId;
        transfer.saveTransfer(error);
        String respCode = resultJson.get("RespCode").getAsString();
        if("000".equals(respCode)|"355".equals(respCode)){
            respCode = "MG00000F";
        }

        if("MG00000F".equals(respCode)){
            transfer.modifyStatus(1, transfer.orderId, error);
        }
        String respDesc = resultJson.get("RespDesc").getAsString();
        String merCustId = resultJson.get("OutCustId").getAsString();
        String desValue = "";
        if("MG00000F".equals(respCode)){
            JsonObject desJson = new JsonObject();
            Set<String> keySet = json.keySet();
            for(String key : keySet ){
                desJson.addProperty(key, json.getString(key));
            }
            desJson.addProperty("pMerBillNo", json.getString("pMerBillNo"));
            desJson.addProperty("transAmt",  pnrparams.getString("transAmt"));
            desJson.addProperty("inCustId",  pnrparams.getString("inCustId"));
            desValue = buildP3DesXmlPara(desJson);
        }
        String pSign = hasMD5(merCustId,respCode,respDesc,desValue);
        JsonObject returnJson = new JsonObject();
        returnJson.addProperty("pMerCode", merCustId);
        returnJson.addProperty("pErrCode",respCode);
        returnJson.addProperty("pErrMsg", respDesc);
        returnJson.addProperty("p3DesXmlPara", desValue);
        returnJson.addProperty("pSign", pSign);
        renderJSON(returnJson.toString());

    }

    /**
     * 转账(基类方法)
     * @param ordId 订单号
     * @param transAmt 转账金额
     * @param inCustId 入账号
     * @return 汇付返回结果
     */
    private static String caseTransfer(String ordId,String transAmt,String inCustId) {

        ChinaPnrService service = new ChinaPnrService();
        ChinaPnrReqModel model = new ChinaPnrReqModel();
        model.setOrdId(ordId);
        model.setTransAmt(transAmt);
        model.setInCustId(inCustId);

        String result= service.doTransfer(model);
        return result;
    }

    /**
     * 查询商户子账户信息(p2p未有此需求,接口以开放,如需就调用此接口)
     */
    private static void caseQueryAccts() {
        ChinaPnrService service = new ChinaPnrService();
        ChinaPnrReqModel model = new ChinaPnrReqModel();
        model.setVersion(ChinaPnrConfig.getProperty("chinapnr_version"));
        model.setCmdId(ChinaPnrConstants.CMD_QUERYACCTS);
        model.setMerCustId(ChinaPnrConfig.getProperty("chinapnr_merCustId"));
        renderJSON(service.doQueryAccts(model));
    }

    /**
     * 商户扣款对账(接口已开放,p2p未使用)
     * @param pnrparams 参数
     */
    private static void caseTrfReconciliation(JSONObject pnrparams) {
        pnrparams = new JSONObject();
        pnrparams.put("BeginDate", "20141001");
        pnrparams.put("EndDate", "20141230");
        pnrparams.put("PageNum", "1");
        pnrparams.put("PageSize", "1000");

        ChinaPnrReqModel model = new ChinaPnrReqModel();
        ChinaPnrService service = new ChinaPnrService();
        model.setVersion(ChinaPnrConfig.getProperty("chinapnr_version"));
        model.setCmdId(ChinaPnrConstants.CMD_TRFRECONCILIATION);
        model.setMerCustId(ChinaPnrConfig.getProperty("chinapnr_merCustId"));
        model.setBeginDate(pnrparams.getString("BeginDate"));
        model.setEndDate(pnrparams.getString("EndDate"));
        model.setPageNum(pnrparams.getString("PageNum"));
        model.setPageSize(pnrparams.getString("PageSize"));

        renderJSON(service.doTrfReconciliation(model));
    }

    /**
     * 投标对账(放款和还款对账)[待p2p业务调用]
     * @param pnrparams
     */
    private static void caseReconciliation(JSONObject pnrparams) {
        pnrparams = new JSONObject();
        //测试数据
        pnrparams.put("BeginDate", "20141001");
        pnrparams.put("EndDate", "20141230");
        pnrparams.put("PageNum", "1");
        pnrparams.put("PageSize", "1000");
        pnrparams.put("QueryTransType", "REPAYMENT");  //LOANS
        //测试数据

        ChinaPnrService service = new ChinaPnrService();
        ChinaPnrReqModel model = new ChinaPnrReqModel();
        model.setVersion(ChinaPnrConfig.getProperty("chinapnr_version"));
        model.setCmdId(ChinaPnrConstants.CMD_RECONCILIATION);
        model.setMerCustId(ChinaPnrConfig.getProperty("chinapnr_merCustId"));
        model.setBeginDate(pnrparams.getString("BeginDate"));
        model.setEndDate(pnrparams.getString("EndDate"));
        model.setPageNum(pnrparams.getString("PageNum"));
        model.setPageSize(pnrparams.getString("PageSize"));
        model.setQueryTransType(pnrparams.getString("QueryTransType"));

        renderJSON(service.doReconciliation(model));
    }

    /**
     * 取现对账(待p2p调用此接口)
     * @param pnrparams 参数
     */
    private static void caseCashReconciliation(JSONObject pnrparams) {
        //测试数据
        pnrparams = new JSONObject();
        pnrparams.put("BeginDate", "20141001");
        pnrparams.put("EndDate", "20141230");
        pnrparams.put("PageNum", "1");
        pnrparams.put("PageSize", "1000");
        //测试数据

        ChinaPnrReqModel model = new ChinaPnrReqModel();
        ChinaPnrService service = new ChinaPnrService();
        model.setVersion(ChinaPnrConfig.getProperty("chinapnr_version"));
        model.setCmdId(ChinaPnrConstants.CMD_CASHRECONCILIATION);
        model.setMerCustId(ChinaPnrConfig.getProperty("chinapnr_merCustId"));
        model.setBeginDate(pnrparams.getString("BeginDate"));
        model.setEndDate(pnrparams.getString("EndDate"));
        model.setPageNum(pnrparams.getString("PageNum"));
        model.setPageSize(pnrparams.getString("PageSize"));

        renderJSON(service.doCashReconciliation(model));
    }

    /**
     * 充值对账(待p2p调用此接口)
     * @param pnrparams 参数
     */
    private static void caseSaveReconciliation(JSONObject pnrparams) {
        //测试数据
        pnrparams = new JSONObject();
        pnrparams.put("BeginDate", "20141001");
        pnrparams.put("EndDate", "20141230");
        pnrparams.put("PageNum", "1");
        pnrparams.put("PageSize", "1000");
        //测试数据

        ChinaPnrService service = new ChinaPnrService();
        ChinaPnrReqModel model = new ChinaPnrReqModel();
        model.setVersion(ChinaPnrConfig.getProperty("chinapnr_version"));
        model.setCmdId(ChinaPnrConstants.CMD_SAVERECONCILIATION);
        model.setMerCustId(ChinaPnrConfig.getProperty("chinapnr_merCustId"));
        model.setBeginDate(pnrparams.getString("BeginDate"));
        model.setEndDate(pnrparams.getString("EndDate"));
        model.setPageNum(pnrparams.getString("PageNum"));
        model.setPageSize(pnrparams.getString("PageSize"));

        renderJSON(service.doSaveReconciliation(model));
    }

    /**
     * 获取银行列表
     */
    private  static void caseBankList() {

        List<String> bankList = ChinaPnrConfig.getBankList();
        StringBuffer bankBuffer = new StringBuffer();
        for (String bank : bankList) {
            bankBuffer.append(bank + "|" + " #");
        }
        bankBuffer.delete(bankBuffer.length() - 1, bankBuffer.length());
        JSONObject json = new JSONObject();
        String pMerCode = ChinaPnrConfig.getProperty("chinapnr_merCustId");
        json.put("pMerCode", pMerCode);
        String pErrCode = "0000 ";
        json.put("pErrCode", "0000 ");
        String pErrMsg = "成功";
        json.put("pErrMsg", pErrMsg);
        json.put("pIpsAcctNo", " ");
        String pBankList = bankBuffer.toString();
        json.put("pBankList", pBankList);
        StringBuffer pSignbuBuffer = new StringBuffer();
        pSignbuBuffer.append(pMerCode).append(pErrCode).append(pErrMsg)
                .append(" ").append(pBankList)
                .append(Constants.ENCRYPTION_KEY);
        String pSign = Codec.hexMD5(pSignbuBuffer.toString());
        json.put("pSign", pSign);

        renderJSON(json.toString());
    }

    /**
     * 开户
     * @param memberId p2p平台用户id
     * @param type 接口类型
     * @param platfrom 平台id
     * @param json 参数
     * @return ChinaPnrBaseService(继承Map)
     */
    private static ChinaPnrBaseService caseUserRegister(long memberId,int type,int platform,JSONObject json) {

        ChinaPnrService maps = new ChinaPnrService();
        String version = ChinaPnrConfig.getProperty("chinapnr_version");
        String cmdId = ChinaPnrConstants.CMD_USERREGISTER;
        String merCustId = ChinaPnrConfig.getProperty("chinapnr_merCustId");
        String bgRetUrl = ChinaPnrConfig.getProperty("chinapnr_retUrlBg");
        String retUrl = ChinaPnrConfig.getProperty("chinapnr_retUrl");
        String pMerBillNo = json.getString("pMerBillNo");
        String idNo = json.getString("pIdentNo");
        String usrName = json.getString("pRealName");
        String usrMp = json.getString("pMobileNo");
        String usrEmail = json.getString("pEmail");
        String pSmDate = json.getString("pSmDate");
        String pWebUrl = json.getString("pWebUrl");
        String pS2SUrl = json.getString("pS2SUrl");
        JsonObject p2pExtra = new JsonObject();
        p2pExtra.addProperty("pMerBillNo", pMerBillNo);
        p2pExtra.addProperty("pSmDate", pSmDate);
        p2pExtra.addProperty("pMemo1", memberId);
        p2pExtra.addProperty("platform", platform);
        p2pExtra.addProperty("pIdentNo", idNo);
        p2pExtra.addProperty("memberId", memberId);
        String MerPriv = pMerBillNo;
        maps.putValue("Version", version).putValue("CmdId", cmdId)
                .putValue("MerCustId", merCustId)
                .putValue("BgRetUrl", bgRetUrl).putValue("RetUrl", retUrl)
                .putValue("IdNo", idNo)
                .putValue("UsrName", usrName).putValue("UsrMp", usrMp)
                .putValue("UsrEmail", usrEmail)
                .putValue("UsrId", usrMp)
                .putValue("MerPriv", MerPriv)
                .setChkValue();
        addEvent(memberId, type + 200, platform, json.getString("pMerBillNo"), pWebUrl, pS2SUrl, p2pExtra.toString(), null);
        addDealDetail(memberId,platform,json.getString("pMerBillNo"),type,0.00,false,"用户开户");
        return maps;
    }

    /**
     * 请求参数输出
     * @param maps
     */
    private static void reqParams(Map<String, String> maps) {
        Set<Entry<String, String>> set = maps.entrySet();
        StringBuffer buffer = new StringBuffer();
        Logger.debug("------------------------------------请求参数 start----------------------------" );
        for (Entry<String, String> entry : set) {
            buffer.append(entry.getKey() + "=" + entry.getValue() + "&");
            Logger.debug(" %s :%s" ,entry.getKey(), entry.getValue());
        }
        buffer = buffer.delete(buffer.length() - 1, buffer.length());
        Logger.info("ReqParams  " + buffer.toString());

//		ChinaPnrBaseService.writeReqParams(buffer.toString());

        Logger.debug("------------------------------------请求参数 end----------------------------" );
    }

    /**
     * 汇付响应参数解析存入Map<String,String>
     * @param params
     * @return
     */
    private static Map<String, String> getRequestParam(String params) {
        try {
            params = URLDecoder.decode(URLDecoder.decode(params,"UTF-8"),"UTF-8");
        } catch (UnsupportedEncodingException e1) {

            Logger.error("UrlDecode解码时 ：%s",e1.getMessage());
        }
        Map paramMap = null;
        if (null != params) {
            paramMap = new HashMap();
            String param[] = params.split("&");
            for (int i = 0; i < param.length; i++) {
                String content = param[i];
                String key = content.substring(0, content.indexOf("="));
                String value = content.substring(content.indexOf("=") + 1,
                        content.length());
                if ("BgRetUrl".equals(key) | "RetUrl".equals(key)) {
                    try {
                        value = URLDecoder.decode(value, "UTF-8");
                    } catch (UnsupportedEncodingException e) {

                        Logger.error("UrlDecode解码时 ：%s",e.getMessage());
                    }
                }
                paramMap.put(key, value);

                Logger.debug("%s : %s", key,value);

            }
        }

        return paramMap;
    }

    /**
     * 用于响应p2p中的pSign加密方法
     * @param args 字符串数组
     * @return 加密后value
     */
    private static String hasMD5(String... args){
        if(args != null){
            StringBuffer buffer = new StringBuffer();
            for(String value : args){
                buffer.append(value);
            }
            buffer.append(Constants.ENCRYPTION_KEY);
            return Codec.hexMD5(buffer.toString());
        }
        Logger.error("hasMD5加密时,缺少参数");
        return "";
    }

    /**
     * 回调
     */
    public static void doCallBack1() {
        InputStream is = request.body;
        String str = null;
        try {
            str = IOUtils.toString(is, "utf-8");
        } catch (IOException e) {

            Logger.error("回调函数读取流错误");
            e.printStackTrace();
        }
        str = URLDecoder.decode(str);
        str = URLDecoder.decode(str);
        Logger.info("回调参数:" + str);
        Map<String, String> params = getRequestParam(str);
        if (!validResp(params)){

            Logger.error("汇付天下回调签名校验失败");
        }
        try {
            String result = "订单:RECV_ORD_ID_" + params.get("TrxId");
            response.out.write(result.getBytes());
        } catch (IOException e) {

            Logger.error("读取流时 : %s " ,e.getMessage());
        }
        excute(params);
    }

    /**
     * 用户登录(接口已开放)
     * @param usrCustId 用户客户号
     * @return
     */
    private static ChinaPnrService caseUserLogin(String usrCustId){
        ChinaPnrService maps = new ChinaPnrService();
        String version = ChinaPnrConfig.getProperty("chinapnr_version");
        String cmdId = ChinaPnrConstants.CMD_USERLOGIN;
        String merCustId = ChinaPnrConfig.getProperty("chinapnr_merCustId");
        maps.put("Version", version);
        maps.put("CmdId", cmdId);
        maps.put("MerCustId", merCustId);
        maps.put("UsrCustId", usrCustId);
        maps.setChkValue();
        return maps;
    }

    /**
     * 回调
     */
    public static void doCallBack() {
        Logger.info("-------------------------doCallBack start---------------------------");

        Map paramMap = new HashMap();;
        String reqparams = null;

        //判断 是否 是app端调用
        Http.Header userAgentHeader = request.headers.get("User-Agent");
        if (userAgentHeader == null) {
            userAgentHeader = request.headers.get("user-agent");
        }

        if (userAgentHeader != null && StringUtils.isNotEmpty(userAgentHeader.value())){
            String userAgent = userAgentHeader.value();
            Logger.info("User-Agent:" + userAgent);
            if (userAgent.contains("Mobile") && userAgent.contains("jindoujialicai")) {
                paramMap.put("fromApp", "Y");
            }
        }

        try {
            reqparams = URLDecoder.decode(URLDecoder.decode(params.urlEncode(),"UTF-8"));

//			ChinaPnrBaseService.writeRespParams(reqparams);

        } catch (UnsupportedEncodingException e1) {

            Logger.error("回调UrlDecode时 : %s " ,e1.getMessage());
        }

        if (null != reqparams) {

            String param[] = reqparams.split("&");
            for (int i = 0; i < param.length; i++) {
                String content = param[i];
                String key = content.substring(0, content.indexOf("="));
                String value = content.substring(content.indexOf("=") + 1,
                        content.length());
                try {
                    paramMap.put(key, URLDecoder.decode(value,"UTF-8"));
                } catch (UnsupportedEncodingException e1) {

                    Logger.error("汇付天下回调构造参数UrlDecode时%s", e1.getMessage());
                }

                try {
                    Logger.debug("~~~%s : %s", key,URLDecoder.decode(value,"UTF-8"));
                } catch (UnsupportedEncodingException e) {

                    Logger.error("回调UrlDecode时 : %s " ,e.getMessage());
                }

            }
        }
        Logger.info("RespParams  " + paramMap.toString());
        Logger.info("-------------------------doCallBack end---------------------------");
        excute(paramMap);
    }




    /**
     * 汇付响应结果进行验签,防止篡改
     * @param params
     * @return
     */
    private static boolean validResp(Map<String, String> params) {

        Logger.info("------------------validResp start------------------");

        String cmdId = params.get("CmdId");

        if (cmdId == null | "".equals(cmdId))
            return false;

        StringBuffer buffer = new StringBuffer();
        String[] keys = ChinaPnrConfig.getRespChkValueKeys(cmdId);



        String value = "";
        for (String key : keys) {
            value = params.get(key) == null ? "" : params.get(key);
            buffer.append(value);
        }
        Logger.debug("%s ----> Resp ChkValue：%s"+ cmdId+"====="+buffer.toString()+"\n---Resp 本地构造ChkValue明文：%s"+buffer.toString());

        String chkValue = params.get("ChkValue");
        boolean flag = false;
        try {

            flag = SignUtils.verifyByRSA(buffer.toString(), chkValue);

        } catch (Exception e) {

            e.printStackTrace();

        }
        if (!flag) {

            Logger.error("汇付天下回调签名验证失败");

        }

        Logger.debug("------------------validResp end------------------");

        return true;
    }

    private static void excute(Map<String, String> params) {
        String cmdId = params.get("CmdId");
        boolean fromApp = "Y".equals(params.get("fromApp")) ? true : false;
        //add by yangxuan 2014-12-17 start
        //汇付开发文档可能会存在打印标志错误，所以把三个字段全部输出到界面

        String appValue = null;
        if (!fromApp) {
            printFlag(params.get(TRXID));
            printFlag(params.get(ORDID));
            printFlag(params.get(PROID));
        }else{
            String trxId = "订单:RECV_ORD_ID_" + params.get(TRXID);
            String ordId = "订单:RECV_ORD_ID_" + params.get(ORDID);
            String proId = "订单:RECV_ORD_ID_" + params.get(PROID);
            appValue = trxId + ordId + proId;
        }

        //added

        Map<String, Object> maps = null;
        boolean flag = false;
        if (ChinaPnrConstants.CMD_USERREGISTER.equals(cmdId)) {  //用户注册

            maps = doUserRegister(params);
            flag = true;
        } else if (ChinaPnrConstants.CMD_NETSAVE.equals(cmdId)) {  //网银充值

            maps = doNetSave(params);
            flag = true;
        } else if (ChinaPnrConstants.CMD_CASH.equals(cmdId)) {  //取现

            maps = doCash(params);
            flag = true;
        } else if (ChinaPnrConstants.CMD_USERBINDCARD.equals(cmdId)) {  //用户绑卡

            maps = doUserBindCard(params);
            flag = true;
        }else if (ChinaPnrConstants.CMD_ADDBIDINFO.equals(cmdId)) {  //标的信息入录

            maps = doAddBidInfo(params);
        } else if (ChinaPnrConstants.CMD_TENDERCANCLE.equals(cmdId)) {  //标的撤销(投标撤销)

            maps = doTenderCancle(params);
            flag = true;
        } else if (ChinaPnrConstants.CMD_AUTOTENDERPLAN.equals(cmdId)) {  //自动投标签约计划

            maps = doAutoTenderPlan(params);
            flag = true;
        }else if(ChinaPnrConstants.CMD_INITIATIVETENDER.equals(cmdId)|ChinaPnrConstants.CMD_AUTOTENDER.equals(cmdId)){  //主动投标、自动投标

            maps = doInitiativeTender(params);
            flag = true;
        }else if(ChinaPnrConstants.CMD_CREDITASSIGN.equals(cmdId)){  //债权转让

            maps = doCreditAssign(params);
            flag = true;
        }else if(ChinaPnrConstants.CMD_TRANSFER.equals(cmdId)){  //商户转账

            maps = doTransfer(params);
            flag = true;
        }else if(ChinaPnrConstants.CMD_USRACCPAY.equals(cmdId)){  //用户账户支付

            maps = doUsrAcctPay(params);
            flag = true;
        }

        String resultAsy = null;
        if(flag){
            Map<String,String> front_params = null;
            if(P2pCommonUtils.isAsynchSoapNames(cmdId)){
                Logger.debug("接口%s支持异步回调",cmdId);
                resultAsy = submitByAsynch(maps);  //异步提交至P2P
                JsonObject json = new JsonParser().parse(resultAsy).getAsJsonObject();
                String code = json.get("code").getAsString();
                if("-10".equals(code)&&(ChinaPnrConstants.CMD_INITIATIVETENDER.equals(cmdId)|
                        ChinaPnrConstants.CMD_AUTOTENDER.equals(cmdId))){  //投标超额，调用资金解冻
                    String ordId = json.get("pMerBillNo").getAsString();
                    String trxId = json.get("pP2PBillNo").getAsString();
                    ChinaPnrReqModel model = new ChinaPnrReqModel();
                    model.setOrdId(ordId);
                    model.setTrxId(trxId);
                    ChinaPnrService service = new ChinaPnrService();
                    String excuteResult = service.doUsrUnFreeze(model);
                    JsonObject excuteJson = new JsonParser().parse(excuteResult).getAsJsonObject();
                    String respCode = excuteJson.get("RespCode").getAsString();
                    String pMerCode = "999";
                    String pErrCode= "999";
                    String pErrMsg = "解冻资金失败";

                    if("000".equals(respCode)|"107".equals(respCode)){
                        pMerCode = "MG00000F";
                        pErrCode = "MG00000F";
                        pErrMsg = "解冻资金成功";

                    }
                    String p3DesXmlPara = buildP3DesXmlPara(json);
                    String pSign = hasMD5(pMerCode,pErrCode,pErrMsg,p3DesXmlPara);
                    json.addProperty("pMerCode", pMerCode);
                    json.addProperty("pErrCode", pErrCode);
                    json.addProperty("pErrMsg", pErrMsg);
                    json.addProperty("p3DesXmlPara", p3DesXmlPara);
                    json.addProperty("pSign", pSign);

                }

                maps.put(P2P_URL_KEY, json.get("pPostUrl").getAsString());
                front_params = new HashMap<String, String>();
                String value = buildP3DesXmlPara(json);
                front_params.put("result", value);
                maps.put(P2P_BASEPARAMS, front_params);
		
					/*maps.put(P2P_URL_KEY, json.get("pPostUrl").getAsString());
					front_params = new HashMap<String, String>();
					Logger.info("异步提交至P2P明文: %s", json.toString());
					String value = buildP3DesXmlPara(json);
					front_params.put("result", value);
					maps.put(P2P_BASEPARAMS, front_params);*/
            }else{
                resultAsy = submitByAsynch(maps);  //异步提交至P2P
            }

            if (fromApp){
                //作用与submitByFront一致 不render html 返回json
                String url = String.valueOf(maps.get(P2P_URL_KEY));
                Map<String,String> baseParams = (Map<String, String>) maps.get(P2P_BASEPARAMS);
                WS.url(url).setParameters(baseParams).post();

                renderAppMsg(cmdId, resultAsy, appValue);
            }else{
                submitByFront(maps);
            }

        }

    }

    private static void renderAppMsg(String cmdId, String result, String appValue){
        Logger.info(">>cmdId:" + cmdId);
        Logger.info(">>result:" + result);
        JsonObject resultJson = new JsonParser().parse(result).getAsJsonObject();
        String code = resultJson.get("code").getAsString();
        String msg = resultJson.get("msg") == null ? "" : resultJson.get("msg").getAsString();

        boolean successFlag = "0".equals(code) ? true : false;

        Map<String, Object> messageMap= new HashMap<String, Object>();
        messageMap.put("severity", successFlag ? 0 : 1);
        messageMap.put("detail", "");

        if (ChinaPnrConstants.CMD_USERREGISTER.equals(cmdId)) {//开户
            messageMap.put("code", successFlag ? MsgCode.UER_REGISTER_SUCC.getCode() : MsgCode.UER_REGISTER_FALL.getCode());
            messageMap.put("summary", successFlag ? MsgCode.UER_REGISTER_SUCC.getMessage() : msg);
        }else if (ChinaPnrConstants.CMD_NETSAVE.equals(cmdId)) {//充值
            messageMap.put("code", successFlag ? MsgCode.NET_SAVE_SUCC.getCode() : MsgCode.NET_SAVE_FALL.getCode());
            messageMap.put("summary", successFlag ? MsgCode.NET_SAVE_SUCC.getMessage() : msg);
        }else if (ChinaPnrConstants.CMD_INITIATIVETENDER.equals(cmdId)) {//投标
            messageMap.put("code", successFlag ? MsgCode.INVEST_SUCC.getCode() : MsgCode.INVEST_FALL.getCode());
            messageMap.put("summary", successFlag ? MsgCode.INVEST_SUCC.getMessage() : msg);
        }

        Map<String, Object> jsonMap= new HashMap<String, Object>();
        jsonMap.put("message", messageMap);
        jsonMap.put("value", appValue);

        JSONObject json = JSONObject.fromObject(jsonMap);

        Logger.info("回调方法返回：" + json.toString());

        renderJSON(json);
    }

    /**
     * 异步提交至P2P
     * @param params
     */
    private static String submitByAsynch(Map<String,Object> params){
        Logger.debug("--------------异步提交至P2Pstart-------------");
        String result = "";
        String url = String.valueOf(params.get(P2P_ASYN_URL_KEY));
        Logger.info("url:" + url);

        Map<String,String> baseParams = (Map<String, String>) params.get(P2P_BASEPARAMS);
        Logger.info("CallBack to P2p Params : %s", params.toString());
        HttpResponse response = WS.url(url).setParameters(baseParams).post();
        if(response.getStatus().intValue() == HTTP_STATUS_SUCCESS.intValue()){
            result = response.getString();
        }
        Logger.info("(Asyn)p2p return results:%s",result);

        return result;

    }

    /**
     * 同步提交至P2P
     * @param params
     */
    private static void submitByFront(Map<String,Object> params){
        String action = String.valueOf(params.get(P2P_URL_KEY));
        Logger.info("action:" + action);

        Map<String,String> maps = (Map<String, String>) params.get(P2P_BASEPARAMS);
        Logger.debug("同步提交至P2P参数： %s", maps.toString());

        render(P2P_COMIT_PAGE,action,maps);
    }



    /**
     * 用户开户回调处理
     *
     * @param params
     * @return
     */
    private static Map<String, Object> doUserRegister(Map<String, String> params) {
        ChinaPnrBaseService maps = new ChinaPnrBaseService();
        String pMerCode = params.get("MerCustId");
        String pErrCode = "000".equals(params.get("RespCode")) ? "MG00000F"
                : params.get("RespCode");
        String pErrMsg = params.get("RespDesc");
        JsonObject json = new JsonObject();
        t_member_events event = DealDetail.updateEvent(params.get("MerPriv"), "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);

        String p3DesXmlPara = "";
        if(pErrCode.equals("MG00000F")){
            JsonObject extra  = new JsonParser().parse(event.remark).getAsJsonObject();
            Member.updateStatus(params.get("IdNo"));
            Member.updateAccount(extra.get("platform").getAsInt(), extra.get("memberId").getAsLong(),params.get("UsrCustId"));



            updateStatus(extra.get("platform").getAsInt(), extra.get("pMerBillNo").getAsString());

            String pMerBillNo = extra.get("pMerBillNo").getAsString();
            String pSmDate = extra.get("pSmDate").getAsString();
            json.addProperty("pStatus",
                    "000".equals(params.get("RespCode")) ? "10" : "9");
            json.addProperty("pMerBillNo", pMerBillNo); // 汇付天下没有流水号,所以需要在p2p提交时,将pMerBillNo保存至MerPriv中以便获取
            json.addProperty("pSmDate", pSmDate);
            json.addProperty("pEmail", params.get("UsrEmail"));
            json.addProperty("pIdentNo", params.get("IdNo"));
            json.addProperty("pRealName", params.get("UsrName") == null ? ""
                    : params.get("UsrName"));
            json.addProperty("pMobileNo", params.get("UsrMp"));
            json.addProperty("pBankName", "");
            json.addProperty("pBkAccName", params.get("UsrName") == null ? ""
                    : params.get("UsrName"));
            json.addProperty("pBkAccNo", "");
            json.addProperty("pCardStatus", "Y");
            json.addProperty("pPhStatus", "Y");
            json.addProperty("pIpsAcctNo", params.get("UsrCustId") == null ? ""
                    : params.get("UsrCustId"));
            json.addProperty("pIpsAcctDate", DateUtil.getDate());
            json.addProperty("pMemo1", extra.get("pMemo1").getAsString());  //存放memberId

            p3DesXmlPara = buildP3DesXmlPara(json);

        }
        maps.putValue("pMerCode", pMerCode).putValue("pErrCode", pErrCode)
                .putValue("pErrMsg", pErrMsg);
        maps.putValue("p3DesXmlPara", p3DesXmlPara);
        String[] keys = { "pMerCode", "pErrCode", "pErrMsg", "p3DesXmlPara" };
        maps.setPsign(keys);
        String action = event.front_url;
        String aysnAction = event.background_url;

//        String action = "http://127.0.0.1:9000/front/PaymentAction/createAcctCB";
//        String aysnAction = "http://127.0.0.1:9000/front/PaymentAction/createAcctCBSys";

        return buildSubmitParams(action,aysnAction,maps);
    }

    /**
     * 网银充值回调处理
     *
     * @param params
     * @return
     */
    private static Map<String, Object> doNetSave(Map<String, String> params) {
        ChinaPnrBaseService maps = new ChinaPnrBaseService();
        String pMerCode = params.get("MerCustId");
        String pErrCode = "000".equals(params.get("RespCode")) ? "MG00000F"
                : params.get("RespCode");
        String pErrMsg = params.get("RespDesc");
        JsonObject json = new JsonObject();
        t_member_events event = DealDetail.updateEvent(params.get("OrdId"), "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
        String p3DesXmlPara = "";
        if("MG00000F".equals(pErrCode)){
            JsonObject extra  = new JsonParser().parse(event.remark).getAsJsonObject();

            updateStatus(extra.get("platform").getAsInt(), params.get("OrdId"));

            String pMerBillNo = params.get("OrdId");
            json.addProperty("pMerBillNo", pMerBillNo);
            json.addProperty("pAcctType", "");
            json.addProperty("pIdentNo","");
            json.addProperty("pRealName","");
            json.addProperty("pIpsAcctNo", params.get("UsrCustId"));
            json.addProperty("pTrdDate", params.get("OrdDate"));
            json.addProperty("pTrdAmt", params.get("TransAmt"));
            json.addProperty("pTrdBnkCode", params.get("GateBankId"));
            json.addProperty("pIpsBillNo", params.get("TrxId"));
            json.addProperty("pMemo1", extra.get("pMemo1").getAsString());
            p3DesXmlPara= buildP3DesXmlPara(json);
        }

        maps.putValue("pMerCode", pMerCode).putValue("pErrCode", pErrCode)
                .putValue("pErrMsg", pErrMsg)
                .putValue("p3DesXmlPara", p3DesXmlPara);
        maps.setPsign(new String[] { "pMerCode", "pErrCode", "pErrMsg",
                "p3DesXmlPara" });

        String action = event.front_url;
        String aysnAction = event.background_url;
        return buildSubmitParams(action,aysnAction,maps);
    }

    /**
     * 用户绑卡
     *
     * @param params
     * @return
     */
    private static Map<String, Object> doUserBindCard(Map<String, String> params) {
        ChinaPnrBaseService maps = new ChinaPnrBaseService();
        JsonObject json = new JsonObject();
        String respCode = params.get("RespCode");
        String respDesc = params.get("RespDesc");
        String openAcctId = params.get("OpenAcctId");
        String openBankId = params.get("OpenBankId");
        String usrCustId = params.get("UsrCustId");
        String trxId = params.get("TrxId");
        json.addProperty("RespCode", respCode);
        json.addProperty("RespDesc", respDesc);
        json.addProperty("OpenAcctId", openAcctId);
        json.addProperty("OpenBankId", openBankId);
        json.addProperty("UsrCustId", usrCustId);
        json.addProperty("TrxId", trxId);
        StringBuffer buffer = new StringBuffer(respCode).append(respDesc)
                .append(openAcctId).append(openBankId).append(usrCustId)
                .append(trxId);
        String chkValue = Codec.hexMD5(buffer.toString());
        json.addProperty("ChkValue", chkValue);

        maps.putValue("results", json.toString());

        String serialNumber = params.get("MerPriv");  //将p2p的流水号保存至此字段,回调后查询数据库,查询到返回至p2p的同步异步地址
        t_member_events event = DealDetail.updateEvent(serialNumber, "pErrCode:"+0+";pErrMsg:"+0);
        String action = event.front_url;
        String aysnAction = event.background_url;
        return buildSubmitParams(action,aysnAction,maps);
    }

    /**
     * 取现
     * @param params
     * @return
     */
    private static Map<String, Object> doCash(Map<String, String> params) {
        ChinaPnrBaseService maps = new ChinaPnrBaseService();
        String pMerCode = params.get("MerCustId");
        String respCode = params.get("RespCode");
        String pErrCode = "";
        String pErrMsg = params.get("RespDesc");
        JsonObject json = new JsonObject();
        t_member_events event = DealDetail.updateEvent(params.get("OrdId"), "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
        String p3DesXmlPara = "";
        JsonObject extra  = new JsonParser().parse(event.remark).getAsJsonObject();
        Logger.info("提现:%s", respCode);
        if("000".equals(respCode)){
            pErrCode  = "MG00000F";
            updateStatus(extra.get("platform").getAsInt(), params.get("OrdId"));
            json.addProperty("pAcctType", " ");
            json.addProperty("pIdentNo", " ");
            json.addProperty("pRealName", " ");
            json.addProperty("pIpsAcctNo", params.get("UsrCustId"));
            json.addProperty("pDwDate", " ");
            json.addProperty("pTrdAmt", params.get("TransAmt"));
            json.addProperty("pTrdBnkCode", params.get("GateBankId"));
            json.addProperty("pIpsBillNo", params.get("TrxId"));

        }else if("999".equals(respCode)){ //处理中
            pErrCode  = "MG00010F";
        }else {  //失败
            pErrCode  = "MG00001F";
        }
        String pMerBillNo = params.get("OrdId");
        json.addProperty("pMerBillNo", pMerBillNo);
        json.addProperty("pMemo1",extra.get("pMemo1").getAsString());
        json.addProperty("pMemo3", extra.get("pMemo3").getAsString());
        p3DesXmlPara= buildP3DesXmlPara(json);
        maps.putValue("pMerCode", pMerCode).putValue("pErrCode", pErrCode)
                .putValue("pErrMsg", pErrMsg)
                .putValue("p3DesXmlPara", p3DesXmlPara);

        maps.setPsign(new String[] { "pMerCode", "pErrCode","pErrMsg","p3DesXmlPara" });
        String action = event.front_url;
        String aysnAction = event.background_url;
        return buildSubmitParams(action,aysnAction,maps);
    }

    /**
     * 标的信息入录
     * @param params
     * @return
     */
    private static Map<String, Object> doAddBidInfo(Map<String, String> params) {
        ChinaPnrBaseService maps = new ChinaPnrBaseService();
        String pMerCode = params.get("MerCustId");
        String pErrCode = "000".equals(params.get("RespCode")) ? "MG00000F"
                : params.get("RespCode");
        String pErrMsg = params.get("RespDesc");
        String p3DesXmlPara = "";

        t_member_events event = DealDetail.updateEvent(params.get("ProId"), "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);

        if("MG00000F".equals(pErrCode)){
            JsonObject extra  = new JsonParser().parse(event.remark).getAsJsonObject();

            updateStatus(extra.get("platform").getAsInt(), params.get("ProId"));

            JsonObject json = new JsonObject();
            json.addProperty("pMerBillNo", extra.get("pMerBillNo").getAsString());
            json.addProperty("pBidNo", params.get("ProId"));
            json.addProperty("pRegDate", extra.get("pRegDate").getAsString());
            json.addProperty("pLendAmt", params.get("BorrTotAmt"));
            json.addProperty("pGuaranteesAmt", params.get("GuarAmt"));
            json.addProperty("pTrdLendRate", extra.get("pTrdLendRate")
                    .getAsString());
            json.addProperty("pTrdLendRate", "");
            json.addProperty("pTrdCycleType", "");
            json.addProperty("pTrdCycleValue", "");
            json.addProperty("pLendPurpose", "");
            json.addProperty("pRepayMode", extra.get("pRepayMode").getAsString());
            json.addProperty("pOperationType", "1");
            json.addProperty("pLendFee", "");
            json.addProperty("pAcctType", "");
            json.addProperty("pIdentNo", "");
            json.addProperty("pRealName", "");
            json.addProperty("pIpsAcctNo", params.get("BorrCustId"));
            json.addProperty("pIpsBillNo", params.get("pMerBillNo"));
            json.addProperty("pIpsTime", DateUtil.simple2(new Date()));
            json.addProperty("pBidStatus", "1");
            json.addProperty("pRealFreezenAmt", params.get("GuarAmt"));
            json.addProperty("pMemo1", extra.get("pMemo1").getAsString());
            json.addProperty("pMemo3", extra.get("pMemo3").getAsString());

            p3DesXmlPara = buildP3DesXmlPara(json);
        }

        maps.putValue("pMerCode", pMerCode).putValue("pErrCode", pErrCode)
                .putValue("pErrMsg", pErrMsg)
                .putValue("p3DesXmlPara", p3DesXmlPara);
        maps.setPsign(new String[]{"pMerCode", "pErrCode", "pErrMsg",
                "p3DesXmlPara"});

        String action = event.front_url;
        String aysnAction = event.background_url;
        return buildSubmitParams(action,aysnAction,maps);
    }

    /**
     * 标的撤销
     * @param params
     * @return
     */
    private static Map<String, Object> doTenderCancle(Map<String, String> params) {
        ChinaPnrBaseService maps = new ChinaPnrBaseService();
        String pMerCode = params.get("MerCustId");
        String pErrCode = "000".equals(params.get("RespCode")) ? "MG00000F"
                : params.get("RespCode");
        String pErrMsg = params.get("RespDesc");
        String p3DesXmlPara = "";
        JsonObject json = new JsonObject();

        t_member_events event = DealDetail.updateEvent(params.get("OrdId"), "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);

        JsonObject extra  = new JsonParser().parse(event.remark).getAsJsonObject();
        json.addProperty("pMerBillNo", extra.get("pMerBillNo")
                .getAsString());
        json.addProperty("pRegDate", extra.get("pRegDate").getAsString());
        json.addProperty("pLendAmt", params.get("BorrTotAmt"));
        json.addProperty("pGuaranteesAmt", params.get("GuarAmt"));
        json.addProperty("pTrdLendRate", extra.get("pTrdLendRate")
                .getAsString());
        json.addProperty("pTrdLendRate", "");
        json.addProperty("pTrdCycleType", "");
        json.addProperty("pTrdCycleValue", "");
        json.addProperty("pLendPurpose", "");
        json.addProperty("pRepayMode", extra.get("pRepayMode")
                .getAsString());
        json.addProperty("pLendFee", "");
        json.addProperty("pAcctType", "");
        json.addProperty("pIdentNo", "");
        json.addProperty("pRealName", "");
        json.addProperty("pIpsAcctNo", params.get("BorrCustId"));
        json.addProperty("pP2PBillN", params.get("ProId"));
        json.addProperty("pIpsTime", DateUtil.simple2(new Date()));
        json.addProperty("pBidStatus", "1");
        json.addProperty("pOperationType", extra.get("pOperationType").getAsString());
        json.addProperty("pIpsBillNo",  extra.get("pIpsBillNo").getAsString());
        json.addProperty("pMemo3",  extra.get("pMemo3").getAsString());
        json.addProperty("pBidNo",  extra.get("pBidNo").getAsString());
        json.addProperty("pMemo1",  extra.get("pMemo1").getAsString());
        p3DesXmlPara = buildP3DesXmlPara(json);
        maps.putValue("pMerCode", pMerCode).putValue("pErrCode", pErrCode)
                .putValue("pErrMsg", pErrMsg)
                .putValue("p3DesXmlPara", p3DesXmlPara);
        maps.setPsign(new String[] { "pMerCode", "pErrCode", "pErrMsg",
                "p3DesXmlPara" });

        String action = event.front_url;
        String aysnAction = event.background_url;
        return buildSubmitParams(action,aysnAction,maps);
    }

    /**
     * 自动投标计划(支持实时返回或者页面浏览器,不支持异步)
     *
     * @param params
     * @return
     */
    private static Map<String, Object> doAutoTenderPlan(Map<String, String> params) {
        ChinaPnrBaseService maps = new ChinaPnrBaseService();
        String pMerCode = params.get("MerCustId");
        String pErrCode = "000".equals(params.get("RespCode")) ? "MG00000F"
                : params.get("RespCode");
        String pErrMsg = params.get("RespDesc");
        t_member_events event = DealDetail.updateEvent(params.get("MerPriv"), "pErrCode:" + pErrCode + ";pErrMsg:" + pErrMsg);
        JsonObject extra  = new JsonParser().parse(event.remark).getAsJsonObject();
        String p3DesXmlPara = "";
        if("MG00000F".equals(pErrCode)){
            updateStatus(extra.get("platform").getAsInt(), params.get("MerPriv"));

            JsonObject json = new JsonObject();
            json.addProperty("pMerBillNo", extra.get("pMerBillNo").getAsString());
            json.addProperty("pSigningDate", DateUtil.getDate());
            json.addProperty("pP2PBillNo", "");
            json.addProperty("pIpsTime", DateUtil.simple2(new Date()));
            json.addProperty("pIpsAuthNo", Random.randomNum());
            json.addProperty("pValidDate", "");
            json.addProperty("pSAmtQuota", "");
            json.addProperty("pEAmtQuota", "");
            json.addProperty("pSIRQuota", "");
            json.addProperty("pEIRQuota", "");
            json.addProperty("pMemo1", extra.get("pMemo1").getAsString());

            p3DesXmlPara = buildP3DesXmlPara(json);

        }
        maps.putValue("pMerCode", pMerCode).putValue("pErrCode", pErrCode)
                .putValue("pErrMsg", pErrMsg)
                .putValue("p3DesXmlPara", p3DesXmlPara);
        maps.setPsign(new String[]{"pMerCode", "pErrCode", "pErrMsg",
                "p3DesXmlPara"});

        String action = event.front_url;
        String aysnAction = event.background_url;
        return buildSubmitParams(action,aysnAction,maps);
    }


    /**
     * 主动投标回调
     * @param params
     * @return
     */
    private static Map<String,Object> doInitiativeTender(Map<String, String> params) {

        ChinaPnrBaseService maps = new ChinaPnrBaseService();
        String pMerCode = params.get("MerCustId");
        String pErrCode = "000".equals(params.get("RespCode")) ? "MG00000F"
                : params.get("RespCode");
        String pErrMsg = params.get("RespDesc");
        String p3DesXmlPara = "";
        t_member_events event = DealDetail.updateEvent(params.get("OrdId"), "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
        JsonObject extra  = new JsonParser().parse(event.remark).getAsJsonObject();
        JsonObject json = new JsonObject();
        if("MG00000F".equals(pErrCode)){
            updateStatus(extra.get("platform").getAsInt(), params.get("OrdId"));
            long userId = Long.valueOf(extra.get("pMemo1").getAsString());
            long bidId = Long.valueOf(extra.get("pBidNo").getAsString());
            String billNo = params.get("OrdId");
            double amount = Double.valueOf(params.get("TransAmt"));
            saveInvest(userId, bidId, billNo, amount);
//			modifyHasInvestedAmountByCallBack(bidId+"", amount);

            json.addProperty("pMerDate", params.get("OrdDate"));
            json.addProperty("pAccountDealNo", params.get("UsrCustId"));
            json.addProperty("pBidDealNo", params.get("OrdId"));
            json.addProperty("pBidNo", "");
            json.addProperty("pContractNo", "");
            json.addProperty("pBusiType", "");
            json.addProperty("pAuthAmt", "");
            json.addProperty("pTrdAmt", params.get("TransAmt"));
            json.addProperty("pFee",extra.get("pFee").getAsString());
            json.addProperty("pTransferAmt", "");
            json.addProperty("pAccount", params.get("UsrCustId"));
            json.addProperty("pStatus", "0");
            json.addProperty("pP2PBillNo",params.get("FreezeTrxId"));
            json.addProperty("pIpsTime", "");
            json.addProperty("pMemo1",extra.get("pMemo1").getAsString());

        }
        json.addProperty("pMemo2", "Y");
        json.addProperty("pMerBillNo", params.get("OrdId"));
        p3DesXmlPara = buildP3DesXmlPara(json);
        maps.putValue("pMerCode", pMerCode).putValue("pErrCode", pErrCode)
                .putValue("pErrMsg", pErrMsg)
                .putValue("p3DesXmlPara", p3DesXmlPara);
        maps.setPsign(new String[]{"pMerCode", "pErrCode", "pErrMsg",
                "p3DesXmlPara"});

        String action = event.front_url;
        String aysnAction = event.background_url;
        return buildSubmitParams(action,aysnAction,maps);
    }

    /**
     * 债权转让
     * @param params
     * @return
     */
    private static Map<String, Object> doCreditAssign(Map<String, String> params) {

        ChinaPnrBaseService maps = new ChinaPnrBaseService();
        String pMerCode = params.get("MerCustId");
        String pErrCode = "000".equals(params.get("RespCode")) ? "MG00000F"
                : params.get("RespCode");
        String pErrMsg = URLDecoder.decode(params.get("RespDesc"));
        String p3DesXmlPara = "";
        JsonObject json = new JsonObject();
        t_member_events event = DealDetail.updateEvent(params.get("OrdId"), "pErrCode:"+pErrCode+";pErrMsg:"+pErrMsg);
        JsonObject extra  = new JsonParser().parse(event.remark).getAsJsonObject();
        if("MG00000F".equals(pErrCode)){

            updateStatus(extra.get("platform").getAsInt(), params.get("OrdId"));

            json.addProperty("pMerBillNo", params.get("OrdId"));
            json.addProperty("pMerDate", params.get("OrdDate"));
            json.addProperty("pP2PBillNo", params.get("OrdId"));
            json.addProperty("pIpsTime", "");
            json.addProperty("pBidNo", "");
            json.addProperty("pBussType", "1");
            json.addProperty("pFromAccountType","");
            json.addProperty("pFromName", "");
            json.addProperty("pFromAccount", params.get("SellCustId"));
            json.addProperty("pFromIdentType", "");
            json.addProperty("pFromIdentNo", "");
            json.addProperty("pToAccountType", "");
            json.addProperty("pToAccountName","");
            json.addProperty("pToAccount", params.get("BuyCustId"));
            json.addProperty("pToIdentType", "");
            json.addProperty("pToIdentNo", "");
            json.addProperty("pCreMerBillNo",params.get("OrdId"));
            json.addProperty("pCretAmt", params.get("CreditAmt"));
            json.addProperty("pPayAmt", params.get("CreditDealAmt"));
            json.addProperty("pFromFee", params.get("Fee"));
            json.addProperty("pToFee", "0.00");
            json.addProperty("pCretType", "");
            json.addProperty("pStatus", "");
            json.addProperty("pMemo1", extra.get("pMemo1").getAsString());

            p3DesXmlPara = buildP3DesXmlPara(json);
        }
        maps.putValue("pMerCode", pMerCode).putValue("pErrCode", pErrCode)
                .putValue("pErrMsg", pErrMsg)
                .putValue("p3DesXmlPara", p3DesXmlPara);
        maps.setPsign(new String[] { "pMerCode", "pErrCode", "pErrMsg",
                "p3DesXmlPara" });

        String action = event.front_url;
        String aysnAction = event.background_url;
        return buildSubmitParams(action,aysnAction,maps);
    }

    /**
     * 商户转账回调
     * @param params
     * @return
     */
    private static Map<String, Object> doTransfer(Map<String,String> params){
        String respCode = params.get("RespCode");
        respCode = "000".equals(respCode)?"MG00000F":respCode;
        String respDesc = params.get("RespDesc");
        String merCustId = params.get("OutCustId");
        String desValue = "";
        t_member_events event = DealDetail.updateEvent(params.get("OrdId"), "pErrCode:"+respCode+";pErrMsg:"+respDesc);
        if("MG00000F".equals(respCode)){
            JsonObject desJson = new JsonObject();
            JsonObject remark = new JsonParser().parse(event.remark).getAsJsonObject();
            Set<Entry<String,JsonElement>> set = remark.entrySet();
            for(Entry<String,JsonElement> entry : set){
                desJson.addProperty(entry.getKey(), entry.getValue().getAsString());
            }


            desJson.addProperty("pMerBillNo", params.get("OrdId"));
            desJson.addProperty("transAmt",  params.get("TransAmt"));
            desJson.addProperty("inCustId",  params.get("InCustId"));
            desValue = buildP3DesXmlPara(desJson);
        }
        String pSign = hasMD5(merCustId,respCode,respDesc,desValue);
        Map<String,String> maps = new HashMap<String, String>();
        maps.put("pMerCode", merCustId);
        maps.put("pErrCode", respCode);
        maps.put("pErrMsg", respDesc);
        maps.put("p3DesXmlPara", desValue);
        maps.put("pSign", pSign);

        String action = event.front_url;
        String aysnAction = event.background_url;
        return buildSubmitParams(action,aysnAction,maps);
    }

    /**
     * 用户账户支付回调
     * @param params
     * @return
     */
    private static Map<String, Object> doUsrAcctPay(Map<String,String> params){
        String respCode = params.get("RespCode");
        respCode = "000".equals(respCode)?"MG00000F":respCode;
        String respDesc = params.get("RespDesc");
        String UsrCustId = params.get("UsrCustId");
        String desValue = "";

        t_member_events event = DealDetail.updateEvent(params.get("OrdId"), "pErrCode:" + respCode + ";pErrMsg:" + respDesc);
        JsonObject extra  = new JsonParser().parse(event.remark).getAsJsonObject();
        if("MG00000F".equals(respCode)){

            updateStatus(extra.get("platfrom").getAsInt(), params.get("OrdId"));

            JsonObject desJson = new JsonObject();
            desJson.addProperty("pMerBillNo", params.get("OrdId"));
            desJson.addProperty("TransAmt",  params.get("TransAmt"));
            desJson.addProperty("UsrCustId",  params.get("UsrCustId"));
            Set<Entry< String, JsonElement>> set  = extra.entrySet();
            for(Entry<String, JsonElement> entry : set){
                desJson.addProperty(entry.getKey(), entry.getValue().getAsString());

            }
            desValue = buildP3DesXmlPara(desJson);
        }
        String pSign = hasMD5(UsrCustId,respCode,respDesc,desValue);
        Map<String,String> maps = new HashMap<String, String>();
        maps.put("pMerCode", UsrCustId);
        maps.put("pErrCode", respCode);
        maps.put("pErrMsg", respDesc);
        maps.put("p3DesXmlPara", desValue);
        maps.put("pSign", pSign);

        String action = event.front_url;
        String aysnAction = event.background_url;
        return buildSubmitParams(action,aysnAction,maps);
    }

    /**
     * 构造请求数据(返回至P2p,支持同步异步)
     * @return
     */
    private static Map<String,Object> buildSubmitParams(String frontAction,String asynAction ,Map<String,String> baseParams){
        Map<String,Object> maps = new HashMap<String,Object>();
        maps.put(P2P_URL_KEY, frontAction);
        maps.put(P2P_ASYN_URL_KEY, asynAction);
        maps.put(P2P_BASEPARAMS, baseParams);
        return maps;
    }

    /**
     * 输出流打印标志
     * @param flag
     */
    private static void printFlag(String flag){
        String result = "订单:RECV_ORD_ID_" + flag;
        try {
            response.out.write(result.getBytes());
        } catch (IOException e) {

            Logger.error("解析流时 : %s " ,e.getMessage());
        }
    }

    /**
     * 构造buildP3DesXmlPara
     *
     * @param jsonParams
     * @return
     */
    private static String buildP3DesXmlPara(JsonObject jsonParams) {
        String result = "";
        try {

            result = Converter.jsonToXml(jsonParams.toString(), "pReq", null,
                    null, null);

        } catch (Exception e) { // 手动捕获异常,可能会存在Xtream转化出现异常

            Logger.error("buildP3DesXmlPara 时 %s", e.getMessage());
            return "{\"RespCode\":\"999\",\"RespDesc\":\"buildP3DesXmlPara异常\"}";
        }
        result = Encrypt.encrypt3DES(result, Constants.ENCRYPTION_KEY);
        return result;
    }

    /**
     * 构造buildP3DesXmlPara
     *
     * @param jsonParams
     * @return
     */
    private static String buildP3DesXmlPara(JSONObject jsonParams) {
        String result = "";
        try {

            result = Converter.jsonToXml(jsonParams.toString(), "pReq", null,
                    null, null);

        } catch (Exception e) { // 手动捕获异常,可能会存在Xtream转化出现异常

            Logger.error("buildP3DesXmlPara 时 %s", e.getMessage());
            return "{\"RespCode\":\"999\",\"RespDesc\":\"buildP3DesXmlPara异常\"}";
        }
        result = Encrypt.encrypt3DES(result, Constants.ENCRYPTION_KEY);
        return result;
    }

    /**
     * 添加操作事件
     * @param memberId
     * @param type
     * @param platformId
     * @param serialNumber
     * @param frontUrl
     * @param backgroundUrl
     * @param remark
     * @param descrption
     */
    private static void addEvent(long memberId, int type, long platformId, String serialNumber,
                                 String frontUrl, String backgroundUrl, String remark, String descrption){
        DealDetail.addEvent(memberId, type, platformId, serialNumber, frontUrl, backgroundUrl, remark, descrption);
    }

    /**
     * 添加操作详情
     * @param memberId
     * @param platformId
     * @param serialNumber
     * @param operation
     * @param amount
     * @param status
     * @param summary
     * @return
     */
    private static boolean addDealDetail(long memberId,int platformId,String serialNumber,int operation,double amount,boolean status,String summary) {
        return DealDetail.addDealDetail(memberId, platformId, serialNumber, operation, amount, status, summary);
    }

    /**
     * 更新操作详情状态
     * @param platformId
     * @param serialNumber
     * @return
     */
    private static boolean updateStatus(int platformId, String serialNumber){
        return DealDetail.updateStatus(platformId, serialNumber);
    }

    /**
     * 标的信息录入保存标的信息至数据库
     * @param chinaPnrService
     */
    private static void saveBids(ChinaPnrReqModel model){
        ErrorInfo error = new ErrorInfo();

        Bid bid = new Bid();
        bid.bidNo =  model.getProId();
        bid.amount =Double.valueOf(model.getBorrTotAmt());
        bid.has_invested_amount = 0.00;
        bid.version = 0;
        bid.bid_id = Long.valueOf(model.getProId());
        bid.saveBid(error);
        if(error.code<0){
            Application.error(error.msg);
        }
    }

    /**
     * 修改标的投资金额
     * @param bidNo
     * @param investPrice
     * @param json
     * @param auto 是否是自动投标
     */
	/*private static void modifyHasInvestedAmount(String bidNo,String investPrice,JSONObject json,boolean auto){
		ErrorInfo error = new ErrorInfo();
		
		Bid bid = new Bid();
		t_bids tbids = bid.findBidByNo(bidNo);
		
		double amountCount = Invests.getAmountByBidId(bidNo);
		
		Logger.debug("标的号%s = "+bidNo+" 投资金额 %s = "+amountCount+"\n----t_bids -->bid:%s :="+bidNo+"----info %s :="+tbids.toString());
		
		if(amountCount!=tbids.amount){
			Bid.modifyHasInvestedAmount(bidNo,String.valueOf( amountCount), error);
			if(error.code<0){
				Application.error(error.msg);
			}
		}
		
		t_bids tbidsquery = bid.findBidByNo(bidNo);
		
		
		double amount = tbidsquery.amount;
		double hasAmount =tbidsquery.has_invested_amount;
		hasAmount = hasAmount + Double.valueOf(investPrice);
		if(amount < hasAmount ){
				if(!auto){
				ChinaPnrBaseService maps = new ChinaPnrBaseService();
				String pMerCode = "999";
				String pErrCode = "999";
				String pErrMsg = "投标失败,本次投资金额已超上限";
				String p3DesXmlPara =buildP3DesXmlPara(json);
				maps.putValue("pMerCode", pMerCode).putValue("pErrCode", pErrCode)
				.putValue("pErrMsg", pErrMsg)
				.putValue("p3DesXmlPara", p3DesXmlPara);
				maps.setPsign(new String[] { "pMerCode", "pErrCode", "pErrMsg",
						"p3DesXmlPara" });
				String action =json.getString("pWebUrl");
			
				render("/PNR/PNRPayment/p2pcommit.html", action, maps);
			}else{
				String pMerCode = "999";
				String pErrCode = "999";
				String pErrMsg = "投标失败,本次投资金额已超上限";
				String p3DesXmlPara =buildP3DesXmlPara(json);
				String pSign = hasMD5(pMerCode,pErrCode,pErrMsg,p3DesXmlPara);
				JsonObject returnJson = new JsonObject();
				returnJson.addProperty("pMerCode", pMerCode);
				returnJson.addProperty("pErrCode", pErrCode);
				returnJson.addProperty("pErrMsg", pErrMsg);
				returnJson.addProperty("p3DesXmlPara", p3DesXmlPara);
				returnJson.addProperty("pSign", pSign);
				renderJSON(returnJson.toString());
			}
		}
		
		Bid.modifyHasInvestedAmount(bidNo, investPrice,tbidsquery.version, error);
		if(error.code<0){
			Application.error(error.msg);
		}
	}*/
	
	/*
	*//**
     * 回调修改投资金额
     * @param bidNo
     * @param investPrice
     *//*
	private static void modifyHasInvestedAmountByCallBack(String bidNo,double investPrice){
		ErrorInfo error = new ErrorInfo();
		Bid.modifyHasInvestedAmountByCallBack(bidNo, investPrice, error);
		if(error.code<0){
			Application.error(error.msg);
		}
	}*/

    /**
     * 保存投资信息
     * @param userId
     * @param bidId
     * @param billNo
     * @param amount
     */
    private static void saveInvest(long userId,long bidId,String billNo,double amount){
        ErrorInfo error = new ErrorInfo();

        Invests invests = new Invests();
        invests.user_id = userId;
        invests.bid_id = bidId;
        invests.ips_bill_no = billNo;
        invests.amount = amount;
        invests.saveInvest(error);
        if(error.code<0){
            Application.error(error.msg);
        }
    }

    public static class P2pCommonUtils{

        /**
         * P2p支持的ws回调接口名称
         */
        private static List<String> AsynchSoapNames = null;

        static {
            if(AsynchSoapNames == null)
                initAsynchSoapNames();
        }

        private static void initAsynchSoapNames(){
            AsynchSoapNames = new ArrayList<String>();
            AsynchSoapNames.add(ChinaPnrConstants.CMD_INITIATIVETENDER);
            AsynchSoapNames.add(ChinaPnrConstants.CMD_CASH);
            AsynchSoapNames.add(ChinaPnrConstants.CMD_REPAYMENT);
            AsynchSoapNames.add(ChinaPnrConstants.CMD_NETSAVE);
        }

        /**
         * 是否支持ws异步接口
         * @param cmdId
         * @return
         */
        public static boolean isAsynchSoapNames(String cmdId){
            return AsynchSoapNames.contains(cmdId);
        }

    }

    /**
     * 时间戳生成内部类
     * @author yx
     *	@create 2014年12月14日 下午1:32:18
     */
    public static class Random {

        public static String randomNum() {
            return String.valueOf(System.currentTimeMillis());
        }
    }
    /**
     * 查询状态内部类
     */
    public  interface QueryTransType{

        /**
         * 放款交易查询类型
         */
        public static final String LOANS = "LOANS";

        /**
         * 还款交易查询类型
         */
        public static final String REPAYMENT = "REPAYMENT";

        /**
         * 投标交易查询类型
         */
        public static final String TENDER = "TENDER";

        /**
         * 取现交易查询类型
         */
        public static final String CASH = "CASH";

        /**
         * 解冻交易查询类型
         */
        public static final String FREEZE = "FREEZE";

        /**
         * 充值交易查询
         */
        public static final String SAVE = "SAVE";

    }

    /**
     * 查询状态转化类
     * @author yx
     *	@create 2014年12月15日 下午4:36:31
     */
    public static class TransStat{

        public static String getStat(String status,String queryTransType){
            if(QueryTransType.CASH.equals(queryTransType)){
                if(status.equals("S")){

                    return PTradeStatue_S;
                }else if(status.equals("F")){

                    return PTradeStatue_F;
                }else if(status.equals("H")){

                    return PTradeStatue_D;
                }else if(status.equals("R")){

                    return PTradeStatue_N;
                }else{

                    return PTradeStatue_N;
                }
            }else if(QueryTransType.FREEZE.equals(queryTransType)){
                if(status.equals("F")|status.equals("U")){

                    return PTradeStatue_S;
                }else if(status.equals("N")){

                    return PTradeStatue_F;
                }else{

                    return PTradeStatue_N;
                }
            }else if(QueryTransType.TENDER.equals(queryTransType)){
                if(status.equals("N")){

                    return PTradeStatue_S;
                }else if(status.equals("C")){

                    return PTradeStatue_F;
                }else{

                    return PTradeStatue_N;
                }
            }else if(QueryTransType.SAVE.equals(queryTransType)){
                if(status.equals("I")){

                    return PTradeStatue_D;
                }else if(status.equals("F")){

                    return PTradeStatue_F;
                }else if(status.equals("S")){

                    return PTradeStatue_S;
                }
            }

            return PTradeStatue_N;
        }
    }

}
