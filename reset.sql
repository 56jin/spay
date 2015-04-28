/*!40101 SET NAMES utf8 */;

/*!40101 SET SQL_MODE=''*/;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
CREATE DATABASE /*!32312 IF NOT EXISTS*/`spay` /*!40100 DEFAULT CHARACTER SET utf8 */;

USE `spay`;
/*清空数据表数据*/
truncate table t_bids;
truncate table t_consumers;
truncate table t_loans_details;
truncate table t_member_detail_sequences;
truncate table t_member_details;
truncate table t_member_events;
truncate table t_member_of_platforms;
truncate table t_members;
truncate table t_payment_gateways;
truncate table t_platforms;
truncate table t_repayment_details;
truncate table t_supervisor_events;
truncate table t_untreated_imformation;

-- ----------------------------
-- Records of t_payment_gateways
-- ----------------------------
INSERT INTO `t_payment_gateways` VALUES ('1', '国付宝', 'test001', '0000049847', '1111aaaa', '{\"t3\":\"t3\",\"t2\":\"t2\",\"t1\":\"t1\"}', '');
INSERT INTO `t_payment_gateways` VALUES ('2', '环迅支付', '808801', 'IPS', '123456', '{\"DES_IV\":\"2EDxsEfp\",\"CERT_MD5\":\"GPhKt7sh4dxQQZZkINGFtefRKNPyAj8S00cgAwtRyy0ufD7alNC28xCBKpa6IU7u54zzWSAv4PqUDKMgpOnM7fucO1wuwMi4RgPAnietmqYIhHXZ3TqTGKNzkxA55qYH\",\"PUB_KEY\":\"-----BEGIN PUBLIC KEY-----#MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCMWwKD0u90z1K8WvtG6cZ3SXHL#UqmQCWxbT6JURy5BVwgsTdsaGmr22HT4jfEBQHEjmTtyUWC5Ag9Cwgef0VFrDB7T#qyhWfVA7n8SvV6b1eDbQlY/qhUb50+3SCpN7HxdPzdMDkJjy6i6syh7RtH0QfoAp#HS6TLY4DjPvbGgdXhwIDAQAB#-----END PUBLIC KEY-----\",\"DES_KEY\":\"ICHuQplJ0YR9l7XeVNKi6FMn\"}', '');
INSERT INTO `t_payment_gateways` VALUES ('3', '双乾', '333888888', 'loan', '123456', null, '');
INSERT INTO `t_payment_gateways` VALUES ('4', '汇付天下', '6000060000918949', 'chinapnr', '123456', null, '');
INSERT INTO `t_payment_gateways` VALUES ('5', '易宝', null, null, null, '10040011137', '');
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;