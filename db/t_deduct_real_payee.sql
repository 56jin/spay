DROP TABLE IF EXISTS t_deduct_real_payee;
CREATE TABLE
    t_deduct_real_payee
    (
        id bigint NOT NULL AUTO_INCREMENT COMMENT '���',
        version VARCHAR(7) COMMENT '�汾��',
        trans_code VARCHAR(8) COMMENT '������',
        trans_datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NULL COMMENT '����ʱ��',
        serial_no VARCHAR(16) COMMENT '�̻�������ˮ��',
        mer_id VARCHAR(15) COMMENT '�̻���',
        mer_name VARCHAR(64) COMMENT '�̻���',
        trans_type VARCHAR(4) COMMENT '�������� 0401-ʵʱ����',
--         biz_type VARCHAR(4) COMMENT 'ҵ�����ͣ�01- ���� 02- С�� 03- ���� 04- �ʹ� 05- ����',
        biz_obj_type VARCHAR(2) COMMENT '00-�Թ���01-��˽',
        payee_acc VARCHAR(32) COMMENT '�տ����˺�',
        payee_name VARCHAR(64) COMMENT '�տ�������',
        bank_acc_type VARCHAR(1) COMMENT '�տ����˺����ͣ�0-��ǿ���Ĭ�ϣ�1-����2-���ǿ�3-��˾�˺�',
        payee_bank_code VARCHAR(12) COMMENT '�տ��п����к�',
        payee_bank_name VARCHAR(50) COMMENT '�տ��˿�������',
        payee_bank_no VARCHAR(8) COMMENT '�տ��˿����б���',
        amt DECIMAL(13,2) DEFAULT '0.00' COMMENT '���׽��',
        cert_type VARCHAR(2) COMMENT '����֤������',
        cert_no VARCHAR(20) COMMENT '����֤����',
        mobile VARCHAR(20) COMMENT '�ֻ���',
        prov_no VARCHAR(6) COMMENT '�տ�ʡ�ݱ���',
        city_no VARCHAR(6) COMMENT '�տ���б���',
        purpose VARCHAR(64) COMMENT '��;˵��',
        postscript VARCHAR(100) COMMENT '����˵��',
        exec_code VARCHAR(6) COMMENT '��Ӧ����',
        exec_msg VARCHAR(128) COMMENT '��Ӧ����',
        pay_serial_no VARCHAR(16) COMMENT '��ͨƽ̨��ˮ��',
        PRIMARY KEY (id)
    )
    ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='ʵʱ����';