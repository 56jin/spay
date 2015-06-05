package controllers;

import business.Platform;
import com.shove.security.Encrypt;
import constants.Constants;
import controllers.PNR.ChinaPnrPayment;
import org.apache.commons.lang.StringUtils;
import play.Logger;
import utils.ErrorInfo;

/**
 * 中间件支付控制器入口
 *
 * @author Administrator
 */
public class Payment extends BaseController {

    /**
     * 支付接口主入口
     *
     * @param version    接口版本
     * @param type       接口类型
     * @param memberId   会员id
     * @param memberName 会员名称
     * @param domain     约定密钥
     */
    public static void spay(String version, int type, long memberId, String memberName, String domain) {
        if (StringUtils.isBlank(version)) {
            flash.error("请传入晓风资金托管版本");
            Application.error();
        }

        if (type <= 0) {
            flash.error("传入参数有误");
            Application.error();
        }

        if (StringUtils.isBlank(domain)) {
            Logger.info("domain不允许为空");
            return;
        }

        ErrorInfo error = new ErrorInfo();
        Platform platform = new Platform();
        platform.domain = Encrypt.decrypt3DES(domain, Constants.ENCRYPTION_KEY);

        if (error.code < 0) {
            flash.error(error.msg);
            Application.error();
        }

        Logger.info("------------version = " + version + "\n" + "------------请求的支付平台：" + platform.gatewayId + "\n------------请求的接口：" + type + "--");

        if (Constants.VERSION2.equals(version)) {
            switch (platform.gatewayId) {
                //汇付
                case Constants.PNR: {
                    String argMerCode = params.get("argMerCode");  //商户号
                    String arg3DesXmlPara = params.get("arg3DesXmlPara");  //xml通过3des加密的参数
                    String argSign = params.get("argSign");  //md5加密之后的校验参数
                    String argIpsAccount = params.get("argIpsAccount");  //第三方客户号
                    String extra = params.get("argeXtraPara");  //xml通过3des加密的参数2, 针对版本2.0添加的所需参数
                    Logger.info("------------>>>>>>>>>>extra " + extra);
                    ChinaPnrPayment chinaPnrPayment = new ChinaPnrPayment();
                    String isWS = params.get("isWS");
                    chinaPnrPayment.pnr(platform.domain, type, (int) platform.id, memberId, memberName, argMerCode, arg3DesXmlPara, extra, argSign, argIpsAccount, isWS);
                    break;
                }

            }
        }
    }
}
