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
    private String version = "1.0";         //�汾��
    @Column(name = "trans_code")
    private String transCode = "NCPS0010";  //������
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "trans_datetime")
    private Date transDatetime;             //����ʱ��
    @Column(name = "serial_no")
    private String serialNo;                //�̻�������ˮ��

    // req
    @Column(name = "mer_id")                //ʵ����֤�̻���:��ͬ�ڽ����̻��ţ���Ҫ��������
    private String merId;
    @Column(name = "mer_name")              //ʵ����֤�̻���
    private String merName;
    @Column(name = "acc_no")                //�����˺�
    private String accNo;
    @Column(name = "acc_name")              //���л���
    private String accName;
    @Column(name = "cert_type")             //֤������
    private String certType;
    @Column(name = "cert_no")               //֤������
    private String certNo;
    @Column(name = "mobile")                //�ֻ�����
    private String mobile;
    @Column(name = "bank_no")               //���б���
    private String bankNo;

    // ans
    @JsonProperty("Return_Code")
    @Column(name = "return_code")
    private String returnCode;             //��Ӧ����
    @JsonProperty("Return_Message")
    @Column(name = "return_message")
    private String returnMessage;          //��Ӧ����
    @JsonProperty("TradeCode")
    @Column(name = "trade_code")
    private String tradeCode;              //��֤Ӧ����
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
