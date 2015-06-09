CREATE TABLE
    DEDUCT_REAL_PAY
    (
        id bigint NOT NULL AUTO_INCREMENT COMMENT '���',
        VERSION VARCHAR(7) COMMENT '�汾��',
        TRANS_CODE VARCHAR(8) COMMENT '������',
        TRANS_DATETIME TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP NULL COMMENT '����ʱ��',
        SERIAL_NO VARCHAR(16) COMMENT '�̻�������ˮ��',
        MER_ID VARCHAR(15) COMMENT '�̻���',
        MER_NAME VARCHAR(64) COMMENT '�̻���',
        TRANS_TYPE VARCHAR(4) COMMENT '�������� 0402-ʵʱ����',
        BIZ_TYPE VARCHAR(4) COMMENT 'ҵ�����ͣ�01- ���� 02- С�� 03- ���� 04- �ʹ� 05- ����',
        BIZ_OBJ_TYPE VARCHAR(2) COMMENT '00-�Թ���01-��˽',
        PAYER_ACC VARCHAR(32) COMMENT '�������˺�',
        PAYER_NAME VARCHAR(64) COMMENT '����������',
        CARD_TYPE VARCHAR(1) COMMENT '���۱�־��0-��ǿ���Ĭ�ϣ�1-����2-���ǿ�3-��˾�˺�',
        PAYER_BANK_CODE VARCHAR(12) COMMENT '�����п����к�',
        PAYER_BANK_NAME VARCHAR(50) COMMENT '�����˿�������',
        PAYER_BANK_NO VARCHAR(8) COMMENT '�����˿����б���',
        AMT DECIMAL(13,2) DEFAULT '0.00' COMMENT '���׽��',
        CERT_TYPE VARCHAR(2) COMMENT '����֤������',
        CERT_NO VARCHAR(20) COMMENT '����֤����',
        PROV_NO VARCHAR(6) COMMENT '����ʡ�ݱ���',
        CITY_NO VARCHAR(6) COMMENT '������б���',
        PURPOSE VARCHAR(64) COMMENT '��;˵��',
        POSTSCRIPT VARCHAR(100) COMMENT '����˵��',
        EXEC_CODE VARCHAR(6) COMMENT '��Ӧ����',
        EXEC_MSG VARCHAR(128) COMMENT '��Ӧ����',
        PAY_SERIAL_NO VARCHAR(16) COMMENT '��ͨƽ̨��ˮ��'
    )
    ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='ʵʱ����';