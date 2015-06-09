package models;

import play.db.jpa.Model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by Yuan on 2015/6/9.
 */
@Entity
@Table(name = "DEDUCT_REAL_PAY")
public class DeductRealPay extends Model {
    // pub
    @Column(name = "VERSION")
    private String version = "1.0";
    @Column(name = "TRANS_CODE")
    private String transCode = "NCPS0002";
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "TRANS_DATETIME")
    private Date transDatetime;
    @Column(name = "SERIAL_NO")
    private String serialNo;

    // req
    @Column(name = "MER_ID")
    private String merId;
    @Column(name = "MER_NAME")
    private String merName;
    @Column(name = "TRANS_TYPE")
    private String transType;
    @Column(name = "BIZ_TYPE")
    private String bizType;
    @Column(name = "BIZ_OBJ_TYPE")
    private String bizObjType;
    @Column(name = "PAYER_ACC")
    private String payerAcc;
    @Column(name = "PAYER_NAME")
    private String payerName;
    @Column(name = "CARD_TYPE")
    private String cardType;
    @Column(name = "PAYER_BANK_CODE")
    private String payerBankCode;
    @Column(name = "PAYER_BANK_NAME")
    private String payerBankName;
    @Column(name = "PAYER_BANK_NO")
    private String payerBankNo;
    @Column(name = "AMT")
    private BigDecimal amt;
    @Column(name = "CERT_TYPE")
    private String certType;
    @Column(name = "CERT_NO")
    private String certNo;
    @Column(name = "PROV_NO")
    private String provNo;
    @Column(name = "CITY_NO")
    private String cityNo;
    @Column(name = "PURPOSE")
    private String purpose;
    @Column(name = "POSTSCRIPT")
    private String postscript;

    // ans
    @Column(name = "EXEC_CODE")
    private String execCode;
    @Column(name = "EXEC_MSG")
    private String execMsg;
    @Column(name = "PAY_SERIAL_NO")
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

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public String getBizObjType() {
        return bizObjType;
    }

    public void setBizObjType(String bizObjType) {
        this.bizObjType = bizObjType;
    }

    public String getPayerAcc() {
        return payerAcc;
    }

    public void setPayerAcc(String payerAcc) {
        this.payerAcc = payerAcc;
    }

    public String getPayerName() {
        return payerName;
    }

    public void setPayerName(String payerName) {
        this.payerName = payerName;
    }

    public String getCardType() {
        return cardType;
    }

    public void setCardType(String cardType) {
        this.cardType = cardType;
    }

    public String getPayerBankCode() {
        return payerBankCode;
    }

    public void setPayerBankCode(String payerBankCode) {
        this.payerBankCode = payerBankCode;
    }

    public String getPayerBankName() {
        return payerBankName;
    }

    public void setPayerBankName(String payerBankName) {
        this.payerBankName = payerBankName;
    }

    public String getPayerBankNo() {
        return payerBankNo;
    }

    public void setPayerBankNo(String payerBankNo) {
        this.payerBankNo = payerBankNo;
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
