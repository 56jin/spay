package constants;

/***
 * XXX_XXXX
 *
 * _前为业务类型
 * _后4位为数字：第一位：0(info) 1(warn) 2(error业务逻辑) 3(fatal系统异常)
 *             第二位：业务代码
 *                    0 公用部分
 *                    1 注册登录
 *                    2 账户中心
 *
 *                    4 交易
 *                    5 p2p
 */
public enum MsgCode {


    UER_REGISTER_SUCC("0601", "用户开户成功"),
    NET_SAVE_SUCC("0602", "充值成功"),
    INVEST_SUCC("0603", "投标成功"),


    UER_REGISTER_FALL("2601", "用户开户失败"),
    NET_SAVE_FALL("2602", "充值失败"),
    INVEST_FALL("2603", "投标失败"),



    ;





    private String code;
    private String message;
    private String detail;

    private MsgCode(String code, String message){
        this(code, message,"");
    }

    private MsgCode(String code, String message, String detail){
        this.code = code;
        this.message = message;
        this.detail = detail;
    }

    public String getCode(){
        return this.code;
    }

    public String getMessage(){
        return this.message;
    }

    public String getDetail(){
        return this.detail;
    }

    public static String getDescByCode(String code) {
        if(code == null) {
            return null;
        }
        for(MsgCode msgCode : MsgCode.values()) {
            if(code.equals(msgCode.getCode())) {
                return msgCode.getMessage();
            }
        }
        return null;
    }
}
