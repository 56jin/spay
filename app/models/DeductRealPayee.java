package models;

import com.fasterxml.jackson.annotation.JsonProperty;
import play.db.jpa.Model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by Yuan on 2015/6/9.
 * ÊµÊ±´ú¸¶
 */
@Entity
@Table(name = "t_deduct_real_payee")
public class DeductRealPayee extends Model {
    // pub
    @Column(name = "version")
    private String version = "1.0";
    @Column(name = "trans_code")
    private String transCode = "NCPS0001";
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "trans_datetime")
    private Date transDatetime;
    @Column(name = "serial_no")
    private String serialNo;

    // req
    @Column(name = "mer_id")
    private String merId;
    @Column(name = "mer_name")
    private String merName;
    @Column(name = "trans_type")
    private String transType;
//    @Column(name = "biz_type")
//    private String bizType;
    @Column(name = "biz_obj_type")
    private String bizObjType;
    @Column(name = "payee_acc")
    private String payeeAcc;
    @Column(name = "payee_name")
    private String payeeName;
    @Column(name = "bank_acc_type")
    private String bankAccType;
    @Column(name = "payee_bank_code")
    private String payeeBankCode;
    @Column(name = "payee_bank_name")
    private String payeeBankName;
    @Column(name = "payee_bank_no")
    private String payeeBankNo;
    @Column(name = "amt")
    private BigDecimal amt;
    @Column(name = "cert_type")
    private String certType;
    @Column(name = "cert_no")
    private String certNo;
    @Column(name = "mobile")
    private String mobile;
    @Column(name = "prov_no")
    private String provNo;
    @Column(name = "city_no")
    private String cityNo;
    @Column(name = "purpose")
    private String purpose;
    @Column(name = "postscript")
    private String postscript;

    // ans
    @JsonProperty("ExecCode")
    @Column(name = "exec_code")
    private String execCode;
    @JsonProperty("ExecMsg")
    @Column(name = "exec_msg")
    private String execMsg;
    @JsonProperty("PaySerialNo")
    @Column(name = "pay_serial_no")
    private String paySerialNo;

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

    public String getTransType() {
        return transType;
    }

    public void setTransType(String transType) {
        this.transType = transType;
    }

//    public String getBizType() {
//        return bizType;
//    }
//
//    public void setBizType(String bizType) {
//        this.bizType = bizType;
//    }

    public String getBizObjType() {
        return bizObjType;
    }

    public void setBizObjType(String bizObjType) {
        this.bizObjType = bizObjType;
    }

    public String getPayeeAcc() {
        return payeeAcc;
    }

    public void setPayeeAcc(String payeeAcc) {
        this.payeeAcc = payeeAcc;
    }

    public String getPayeeName() {
        return payeeName;
    }

    public void setPayeeName(String payeeName) {
        this.payeeName = payeeName;
    }

    public String getBankAccType() {
        return bankAccType;
    }

    public void setBankAccType(String bankAccType) {
        this.bankAccType = bankAccType;
    }

    public String getPayeeBankCode() {
        return payeeBankCode;
    }

    public void setPayeeBankCode(String payeeBankCode) {
        this.payeeBankCode = payeeBankCode;
    }

    public String getPayeeBankName() {
        return payeeBankName;
    }

    public void setPayeeBankName(String payeeBankName) {
        this.payeeBankName = payeeBankName;
    }

    public String getPayeeBankNo() {
        return payeeBankNo;
    }

    public void setPayeeBankNo(String payeeBankNo) {
        this.payeeBankNo = payeeBankNo;
    }

    public BigDecimal getAmt() {
        return amt;
    }

    public void setAmt(BigDecimal amt) {
        this.amt = amt;
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

    public String getProvNo() {
        return provNo;
    }

    public void setProvNo(String provNo) {
        this.provNo = provNo;
    }

    public String getCityNo() {
        return cityNo;
    }

    public void setCityNo(String cityNo) {
        this.cityNo = cityNo;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public String getPostscript() {
        return postscript;
    }

    public void setPostscript(String postscript) {
        this.postscript = postscript;
    }

    public String getExecCode() {
        return execCode;
    }

    public void setExecCode(String execCode) {
        this.execCode = execCode;
    }

    public String getExecMsg() {
        return execMsg;
    }

    public void setExecMsg(String execMsg) {
        this.execMsg = execMsg;
    }

    public String getPaySerialNo() {
        return paySerialNo;
    }

    public void setPaySerialNo(String paySerialNo) {
        this.paySerialNo = paySerialNo;
    }
}
