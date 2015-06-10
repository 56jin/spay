DROP TABLE IF EXISTS t_trade_result;
CREATE TABLE
    t_trade_result
    (
        id bigint NOT NULL AUTO_INCREMENT COMMENT '���',
        version VARCHAR(7) COMMENT '�汾��',
        trans_code VARCHAR(8) COMMENT '������',
        trans_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NULL COMMENT '����ʱ��',
        serial_no VARCHAR(16) COMMENT '�̻�������ˮ��',
        mer_id VARCHAR(15) COMMENT 'ʵ����֤�̻���:��ͬ�ڽ����̻��ţ���Ҫ��������',
        mer_name VARCHAR(64) COMMENT 'ʵ����֤�̻���',
        ori_payserial_no VARCHAR(16) COMMENT 'ԭƽ̨��ˮ��',
        ori_trans_date TIMESTAMP COMMENT 'ԭ���׷���������',
        trans_type VARCHAR(4) COMMENT 'ԭƽ̨��ˮ��',
        amt DECIMAL(13,2) DEFAULT '0.00' COMMENT '���׽��',
        status VARCHAR(2) COMMENT '����״̬',
        description VARCHAR(250) COMMENT '״̬����',
        exec_code VARCHAR(2) COMMENT '��Ӧ����',
        exec_msg VARCHAR(40) COMMENT '��Ӧ����',
        PRIMARY KEY (id)
    )
    ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='���׽������';