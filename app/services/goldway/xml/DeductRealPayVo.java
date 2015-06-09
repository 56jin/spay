package services.goldway.xml;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Created by Yuan on 2015/6/8.
 */
@JacksonXmlRootElement(localName="package")
public class DeductRealPayVo {
    @JacksonXmlProperty(localName="Pub")
    public Pub pub;
    @JacksonXmlProperty(localName="Req")
    public Req req;

    public Pub getPub() {
        return pub;
    }

    public void setPub(Pub pub) {
        this.pub = pub;
    }

    public Req getReq() {
        return req;
    }

    public void setReq(Req req) {
        this.req = req;
    }

    public static class Pub {
        @JacksonXmlProperty(localName="Version")
        private String version = "1.0";
        @JacksonXmlProperty(localName="TransCode")
        private String transCode;
        @JacksonXmlProperty(localName="TransDate")
        private String transDate;
        @JacksonXmlProperty(localName="TransTime")
        private String transTime;
        @JacksonXmlProperty(localName="SerialNo")
        private String serialNo;

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

        public String getTransDate() {
            return transDate;
        }

        public void setTransDate(String transDate) {
            this.transDate = transDate;
        }

        public String getTransTime() {
            return transTime;
        }

        public void setTransTime(String transTime) {
            this.transTime = transTime;
        }

        public String getSerialNo() {
            return serialNo;
        }

        public void setSerialNo(String serialNo) {
            this.serialNo = serialNo;
        }
    }

    public static class Req {
        @JacksonXmlProperty(localName="MerId")
        private String merId;
        @JacksonXmlProperty(localName="MerName")
        private String merName;
        @JacksonXmlProperty(localName="TransType")
        private String transType;
        @JacksonXmlProperty(localName="BizType")
        private String bizType;
        @JacksonXmlProperty(localName="BizObjType")
        private String bizObjType;
        @JacksonXmlProperty(localName="PayerAcc")
        private String payerAcc;
        @JacksonXmlProperty(localName="PayerName")
        private String payerName;
        @JacksonXmlProperty(localName="CardType")
        private String cardType;
        @JacksonXmlProperty(localName="PayerBankCode")
        private String payerBankCode;
        @JacksonXmlProperty(localName="PayerBankName")
        private String payerBankName;
        @JacksonXmlProperty(localName="PayerBankNo")
        private String payerBankNo;
        @JacksonXmlProperty(localName="Amt")
        private String amt;
        @JacksonXmlProperty(localName="CertType")
        private String certType;
        @JacksonXmlProperty(localName="CertNo")
        private String certNo;
        @JacksonXmlProperty(localName="ProvNo")
        private String provNo;
        @JacksonXmlProperty(localName="CityNo")
        private String cityNo;
        @JacksonXmlProperty(localName="Purpose")
        private String purpose;
        @JacksonXmlProperty(localName="Postscript")
        private String postscript;

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

        public String getAmt() {
            return amt;
        }

        public void setAmt(String amt) {
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
    }

}

