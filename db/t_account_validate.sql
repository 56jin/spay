DROP TABLE IF EXISTS t_account_validate;
CREATE TABLE
    t_account_validate
    (
        id bigint NOT NULL AUTO_INCREMENT COMMENT '���',
        version VARCHAR(7) COMMENT '�汾��',
        trans_code VARCHAR(8) COMMENT '������',
        trans_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NULL COMMENT '����ʱ��',
        serial_no VARCHAR(16) COMMENT '�̻�������ˮ��',
        mer_id VARCHAR(15) COMMENT 'ʵ����֤�̻���:��ͬ�ڽ����̻��ţ���Ҫ��������',
        mer_name VARCHAR(64) COMMENT 'ʵ����֤�̻���',
        acc_no VARCHAR(32) COMMENT '�����˺�',
        acc_name VARCHAR(64) COMMENT '���л���',
        cert_type VARCHAR(2) COMMENT '֤������',
        cert_no VARCHAR(20) COMMENT '֤������',
        mobile VARCHAR(20) COMMENT '�ֻ�����',
        bank_no VARCHAR(8) COMMENT '���б���',
        return_code VARCHAR(6) COMMENT '��Ӧ����',
        return_message VARCHAR(128) COMMENT '��Ӧ����',
        trade_code VARCHAR(4) COMMENT '��֤Ӧ����',
        trade_desc VARCHAR(128) COMMENT '��֤��������',
        PRIMARY KEY (id)
    )
    ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='ʵ����֤';