DROP TABLE IF EXISTS t_deduct_real_payee;
CREATE TABLE
    t_deduct_real_payee
    (
        id bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
        version VARCHAR(7) COMMENT '版本号',
        trans_code VARCHAR(8) COMMENT '交易码',
        trans_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NULL COMMENT '交易时间',
        serial_no VARCHAR(16) COMMENT '商户交易流水号',
        mer_id VARCHAR(15) COMMENT '商户号',
        mer_name VARCHAR(64) COMMENT '商户号',
        trans_type VARCHAR(4) COMMENT '交易类型 0401-实时代付',
--         biz_type VARCHAR(4) COMMENT '业务类型，01- 基金 02- 小贷 03- 保险 04- 资管 05- 其他',
        biz_obj_type VARCHAR(2) COMMENT '00-对公，01-对私',
        payee_acc VARCHAR(32) COMMENT '收款人账号',
        payee_name VARCHAR(64) COMMENT '收款人名称',
        bank_acc_type VARCHAR(1) COMMENT '收款人账号类型：0-借记卡（默认）1-存折2-贷记卡3-公司账号',
        payee_bank_code VARCHAR(12) COMMENT '收款行开户行号',
        payee_bank_name VARCHAR(50) COMMENT '收款人开户行名',
        payee_bank_no VARCHAR(8) COMMENT '收款人开户行编码',
        amt DECIMAL(13,2) DEFAULT '0.00' COMMENT '交易金额',
        cert_type VARCHAR(2) COMMENT '开户证件类型',
        cert_no VARCHAR(20) COMMENT '开户证件号',
        mobile VARCHAR(20) COMMENT '手机号',
        prov_no VARCHAR(6) COMMENT '收款省份编码',
        city_no VARCHAR(6) COMMENT '收款城市编码',
        purpose VARCHAR(64) COMMENT '用途说明',
        postscript VARCHAR(100) COMMENT '附言说明',
        exec_code VARCHAR(6) COMMENT '响应代码',
        exec_msg VARCHAR(128) COMMENT '响应描述',
        pay_serial_no VARCHAR(16) COMMENT '金通平台流水号',
        PRIMARY KEY (id)
    )
    ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='实时代付';