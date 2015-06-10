package models;

import play.db.jpa.Model;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.Date;

/**
 * Created by Yuan on 2015/6/10.
 * 交易结果反馈表
 */
@Entity
@Table(name = "t_trade_result")
public class TradeResult extends Model {
    // pub
    @Column(name = "version")
    private String version = "1.0";         //版本号
    @Column(name = "trans_code")
    private String transCode = "NCPS1002";  //交易码
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "trans_datetime")
    private Date transDatetime;             //交易时间
    @Column(name = "serial_no")
    private String serialNo;                //原发起方流水号

    // req
    @Column(name = "mer_id")
    private String merId;                   //商户号
    @Column(name = "ori_payserial_no")
    private String oriPaySerialNo;          //原平台流水号
    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "ori_trans_date")
    private String oriTransDate;               //交易类型:0401-实时代付,0402-实时代收
    @Column(name = "trans_type")
    private String transType;               //交易类型:0401-实时代付,0402-实时代收
    @Column(name = "amt")
    private BigDecimal amt;                 //交易金额
    @Column(name = "status")
    private BigDecimal status;              //交易状态
    @Column(name = "description")
    private String description;              //状态描述

    // ans
    @Column(name = "exec_code")
    private String execCode;              //响应代码
    @Column(name = "exec_msg")
    private String execMsg;              //响应描述

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

    public String getOriPaySerialNo() {
        return oriPaySerialNo;
    }

    public void setOriPaySerialNo(String oriPaySerialNo) {
        this.oriPaySerialNo = oriPaySerialNo;
    }

    public String getTransType() {
        return transType;
    }

    public void setTransType(String transType) {
        this.transType = transType;
    }

    public BigDecimal getAmt() {
        return amt;
    }

    public void setAmt(BigDecimal amt) {
        this.amt = amt;
    }

    public BigDecimal getStatus() {
        return status;
    }

    public void setStatus(BigDecimal status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getOriTransDate() {
        return oriTransDate;
    }

    public void setOriTransDate(String oriTransDate) {
        this.oriTransDate = oriTransDate;
    }
}
