DROP TABLE IF EXISTS t_account_validate;
CREATE TABLE
    t_account_validate
    (
        id bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
        version VARCHAR(7) COMMENT '版本号',
        trans_code VARCHAR(8) COMMENT '交易码',
        trans_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NULL COMMENT '交易时间',
        serial_no VARCHAR(16) COMMENT '商户交易流水号',
        mer_id VARCHAR(15) COMMENT '实名认证商户号:不同于交易商户号，需要另外申请',
        mer_name VARCHAR(64) COMMENT '实名认证商户名',
        acc_no VARCHAR(32) COMMENT '银行账号',
        acc_name VARCHAR(64) COMMENT '银行户名',
        cert_type VARCHAR(2) COMMENT '证件类型',
        cert_no VARCHAR(20) COMMENT '证件号码',
        mobile VARCHAR(20) COMMENT '手机号码',
        bank_no VARCHAR(8) COMMENT '银行编码',
        return_code VARCHAR(6) COMMENT '响应代码',
        return_message VARCHAR(128) COMMENT '响应描述',
        trade_code VARCHAR(4) COMMENT '认证应答码',
        trade_desc VARCHAR(128) COMMENT '认证处理描述',
        PRIMARY KEY (id)
    )
    ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='实名认证';