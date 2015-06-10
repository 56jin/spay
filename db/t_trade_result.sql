DROP TABLE IF EXISTS t_trade_result;
CREATE TABLE
    t_trade_result
    (
        id bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
        version VARCHAR(7) COMMENT '版本号',
        trans_code VARCHAR(8) COMMENT '交易码',
        trans_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NULL COMMENT '交易时间',
        serial_no VARCHAR(16) COMMENT '商户交易流水号',
        mer_id VARCHAR(15) COMMENT '实名认证商户号:不同于交易商户号，需要另外申请',
        mer_name VARCHAR(64) COMMENT '实名认证商户名',
        ori_payserial_no VARCHAR(16) COMMENT '原平台流水号',
        ori_trans_date TIMESTAMP COMMENT '原交易发生的日期',
        trans_type VARCHAR(4) COMMENT '原平台流水号',
        amt DECIMAL(13,2) DEFAULT '0.00' COMMENT '交易金额',
        status VARCHAR(2) COMMENT '交易状态',
        description VARCHAR(250) COMMENT '状态描述',
        exec_code VARCHAR(2) COMMENT '响应代码',
        exec_msg VARCHAR(40) COMMENT '响应描述',
        PRIMARY KEY (id)
    )
    ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='交易结果反馈';