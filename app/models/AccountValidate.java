package models;

import com.fasterxml.jackson.annotation.JsonProperty;
import play.db.jpa.Model;

import javax.persistence.*;
import java.util.Date;

/**
 * Created by Yuan on 2015/6/10.
 */
@Entity
@Table(name = "t_account_validate")
public class AccountValidate extends Model {
    // pub


    @Column(name = "version")
    private String version = "1.0";         //版本号
    @Column(name = "trans_code")
    private String transCode = "NCPS0010";  //交易码
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "trans_datetime")
    private Date transDatetime;             //交易时间
    @Column(name = "serial_no")
    private String serialNo;                //商户交易流水号

    // req
    @Column(name = "mer_id")                //实名认证商户号:不同于交易商户号，需要另外申请
    private String merId;
    @Column(name = "mer_name")              //实名认证商户名
    private String merName;
    @Column(name = "acc_no")                //银行账号
    private String accNo;
    @Column(name = "acc_name")              //银行户名
    private String accName;
    @Column(name = "cert_type")             //证件类型
    private String certType;
    @Column(name = "cert_no")               //证件号码
    private String certNo;
    @Column(name = "mobile")                //手机号码
    private String mobile;
    @Column(name = "bank_no")               //银行编码
    private String bankNo;

    // ans
    @JsonProperty("Return_Code")
    @Column(name = "return_code")
    private String returnCode;             //响应代码
    @JsonProperty("Return_Message")
    @Column(name = "return_message")
    private String returnMessage;          //响应描述
    @JsonProperty("TradeCode")
    @Column(name = "trade_code")
    private String tradeCode;              //认证应答码
    @JsonProperty("TradeDesc")
    @Column(name = "trade_desc")
    private String tradeDesc;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getTransCode() {
        return transCode;
    }

    public void setTransCode(String transCode) {
        this.transCode = transCode;
    }

    public Date getTransDatetime() {
        return transDatetime;
    }

    public void setTransDatetime(Date transDatetime) {
        this.transDatetime = transDatetime;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
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

    public String getAccNo() {
        return accNo;
    }

    public void setAccNo(String accNo) {
        this.accNo = accNo;
    }

    public String getAccName() {
        return accName;
    }

    public void setAccName(String accName) {
        this.accName = accName;
    }

    public String getCertType() {
        return certType;
    }

    public void setCertType(String certType) {
        this.certType = certType;
    }

    public String getCertNo() {
        return certNo;
    }

    public void setCertNo(String certNo) {
        this.certNo = certNo;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getBankNo() {
        return bankNo;
    }

    public void setBankNo(String bankNo) {
        this.bankNo = bankNo;
    }

    public String getReturnCode() {
        return returnCode;
    }

    public void setReturnCode(String returnCode) {
        this.returnCode = returnCode;
    }

    public String getReturnMessage() {
        return returnMessage;
    }

    public void setReturnMessage(String returnMessage) {
        this.returnMessage = returnMessage;
    }

    public String getTradeCode() {
        return tradeCode;
    }

    public void setTradeCode(String tradeCode) {
        this.tradeCode = tradeCode;
    }

    public String getTradeDesc() {
        return tradeDesc;
    }

    public void setTradeDesc(String tradeDesc) {
        this.tradeDesc = tradeDesc;
    }
}
