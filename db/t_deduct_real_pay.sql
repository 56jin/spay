DROP TABLE IF EXISTS t_deduct_real_pay;
CREATE TABLE
    t_deduct_real_pay
    (
        id bigint NOT NULL AUTO_INCREMENT COMMENT '���',
        version VARCHAR(7) COMMENT '�汾��',
        trans_code VARCHAR(8) COMMENT '������',
        trans_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NULL COMMENT '����ʱ��',
        serial_no VARCHAR(16) COMMENT '�̻�������ˮ��',
        mer_id VARCHAR(15) COMMENT '�̻���',
        mer_name VARCHAR(64) COMMENT '�̻���',
        trans_type VARCHAR(4) COMMENT '�������� 0402-ʵʱ����',
        biz_type VARCHAR(4) COMMENT 'ҵ�����ͣ�01- ���� 02- С�� 03- ���� 04- �ʹ� 05- ����',
        biz_obj_type VARCHAR(2) COMMENT '00-�Թ���01-��˽',
        payer_acc VARCHAR(32) COMMENT '�������˺�',
        payer_name VARCHAR(64) COMMENT '����������',
        card_type VARCHAR(1) COMMENT '���۱�־��0-��ǿ���Ĭ�ϣ�1-����2-���ǿ�3-��˾�˺�',
        payer_bank_code VARCHAR(12) COMMENT '�����п����к�',
        payer_bank_name VARCHAR(50) COMMENT '�����˿�������',
        payer_bank_no VARCHAR(8) COMMENT '�����˿����б���',
        amt DECIMAL(13,2) DEFAULT '0.00' COMMENT '���׽��',
        cert_type VARCHAR(2) COMMENT '����֤������',
        cert_no VARCHAR(20) COMMENT '����֤����',
        prov_no VARCHAR(6) COMMENT '����ʡ�ݱ���',
        city_no VARCHAR(6) COMMENT '������б���',
        purpose VARCHAR(64) COMMENT '��;˵��',
        postscript VARCHAR(100) COMMENT '����˵��',
        exec_code VARCHAR(6) COMMENT '��Ӧ����',
        exec_msg VARCHAR(128) COMMENT '��Ӧ����',
        pay_serial_no VARCHAR(16) COMMENT '��ͨƽ̨��ˮ��',
        PRIMARY KEY (id)
    )
    ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='ʵʱ����';