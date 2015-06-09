package services.goldway.util;

import java.security.Key; 
import java.security.KeyFactory; 
import java.security.KeyPair; 
import java.security.KeyPairGenerator; 
import java.security.PrivateKey; 
import java.security.PublicKey; 
import java.security.SecureRandom; 
import java.security.Signature; 
import java.security.spec.PKCS8EncodedKeySpec; 
import java.security.spec.X509EncodedKeySpec; 
import java.util.HashMap; 
import java.util.Map; 
   
import sun.misc.BASE64Decoder; 
import sun.misc.BASE64Encoder; 
   
/**
 * DSA-Digital Signature Algorithm ��Schnorr��ElGamalǩ���㷨�ı��֣�������NIST��ΪDSS(DigitalSignature Standard)��
 * �򵥵�˵������һ�ָ��߼�����֤��ʽ����������ǩ����������ֻ�й�Կ��˽Կ����������ǩ����˽Կ������������ǩ������Կ��֤���ݼ�ǩ����
 * ������ݺ�ǩ����ƥ������Ϊ��֤ʧ�ܣ��� �����е����� ���Բ��ټ��ܣ����շ�������ݺ��õ���Կ��ǩ�� ��֤�����Ƿ���Ч
 * 
 * @author stone
 * @date 2014-03-11 09:50:51
 */ 
public class DSAHelper { 
    //��������ʹ��DSA�㷨��ͬ��Ҳ����ʹ��RSA�㷨������ǩ�� 
    /*public static final String KEY_ALGORITHM = "RSA";
    public static final String SIGNATURE_ALGORITHM = "MD5withRSA";*/ 
       
    public static final String KEY_ALGORITHM = "DSA"; 
    public static final String SIGNATURE_ALGORITHM = "DSA"; 
       
    public static final String DEFAULT_SEED = "$%^*%^()(HJG8awfjas7"; //Ĭ������ 
    public static final String PUBLIC_KEY = "DSAPublicKey"; 
    public static final String PRIVATE_KEY = "DSAPrivateKey"; 
       
    public static void main(String[] args) throws Exception { 
        String str = "!@#$!#^$#&ZXVDF������·������*()_+"; 
        byte[] data = str.getBytes(); 
           
        Map<String, Object> keyMap = initKey();// ������Կ 
        PublicKey publicKey = (PublicKey) keyMap.get(PUBLIC_KEY); 
        PrivateKey privateKey = (PrivateKey) keyMap.get(PRIVATE_KEY); 
        System.out.println("˽Կformat��" + privateKey.getFormat()); 
        System.out.println("��Կformat��" + publicKey.getFormat()); 
           
           
        // ����ǩ�� 
        String sign = sign(data, getPrivateKey(keyMap)); 
           
        // ��֤ǩ��  
        boolean verify1 = verify("aaa".getBytes(), getPublicKey(keyMap), sign); 
        System.err.println("����֤ ���ݺ�ǩ��ƥ��:" + verify1);    
           
        boolean verify = verify(data, getPublicKey(keyMap), sign); 
        System.err.println("����֤ ���ݺ�ǩ��ƥ��:" + verify);    
    } 
       
    /**  
     * ������Կ  
     *   
     * @param seed ����  
     * @return ��Կ����  
     * @throws Exception  
     */ 
    public static Map<String, Object> initKey(String seed) throws Exception { 
        System.out.println("������Կ"); 
           
        KeyPairGenerator keygen = KeyPairGenerator.getInstance(KEY_ALGORITHM); 
        SecureRandom secureRandom = new SecureRandom();    
        secureRandom.setSeed(seed.getBytes());  
        //Modulus size must range from 512 to 1024 and be a multiple of 64 
        keygen.initialize(640, secureRandom);   
           
        KeyPair keys = keygen.genKeyPair(); 
        PrivateKey privateKey = keys.getPrivate(); 
        PublicKey publicKey = keys.getPublic(); 
           
        Map<String, Object> map = new HashMap<String, Object>(2); 
        map.put(PUBLIC_KEY, publicKey);    
        map.put(PRIVATE_KEY, privateKey); 
        return map; 
    } 
       
    /**  
     * ����Ĭ����Կ  
     *   
     * @return ��Կ����  
     * @throws Exception  
     */   
    public static Map<String, Object> initKey() throws Exception {    
        return initKey(DEFAULT_SEED);    
    } 
       
    /**  
     * ȡ��˽Կ  
     *   
     * @param keyMap  
     * @return  
     * @throws Exception  
     */   
    public static String getPrivateKey(Map<String, Object> keyMap) throws Exception {    
        Key key = (Key) keyMap.get(PRIVATE_KEY);    
        return encryptBASE64(key.getEncoded()); //base64����˽Կ 
    }    
     
    /**  
     * ȡ�ù�Կ  
     *   
     * @param keyMap  
     * @return  
     * @throws Exception  
     */   
    public static String getPublicKey(Map<String, Object> keyMap) throws Exception {    
        Key key = (Key) keyMap.get(PUBLIC_KEY);    
        return encryptBASE64(key.getEncoded()); //base64���ܹ�Կ 
    }    
       
    /**
     *  ��˽Կ����Ϣ��������ǩ��
     * @param data  ��������
     * @param privateKey ˽Կ-base64���ܵ�
     * @return 
     * @throws Exception
     */ 
    public static String sign(byte[] data, String privateKey) throws Exception { 
        System.out.println("��˽Կ����Ϣ��������ǩ��"); 
           
        byte[] keyBytes = decryptBASE64(privateKey); 
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes); 
        KeyFactory factory = KeyFactory.getInstance(KEY_ALGORITHM); 
        PrivateKey priKey = factory.generatePrivate(keySpec);//���� ˽Կ 
           
        //��˽Կ����Ϣ��������ǩ�� 
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM); 
        signature.initSign(priKey); 
        signature.update(data); 
        return encryptBASE64(signature.sign()); 
    } 
       
    /**
     * BASE64Encoder ����
     * @param data Ҫ���ܵ�����
     * @return ���ܺ���ַ���
     */ 
    private static String encryptBASE64(byte[] data) { 
        BASE64Encoder encoder = new BASE64Encoder(); 
        String encode = encoder.encode(data); 
        return encode; 
    } 
       
    /**
     * BASE64Decoder ����
     * @param data Ҫ���ܵ��ַ���
     * @return ���ܺ��byte[]
     * @throws Exception 
     */ 
    private static byte[] decryptBASE64(String data) throws Exception { 
        BASE64Decoder decoder = new BASE64Decoder(); 
        byte[] buffer = decoder.decodeBuffer(data); 
        return buffer; 
    } 
       
    /**
     * У������ǩ��
     * @param data ��������
     * @param publicKey
     * @param sign ����ǩ��
     * @return
     * @throws Exception
     */ 
    public static boolean verify(byte[] data, String publicKey, String sign) throws Exception { 
        byte[] keyBytes = decryptBASE64(publicKey);  
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes); 
        KeyFactory keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);   
        PublicKey pubKey = keyFactory.generatePublic(keySpec); 
           
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);    
        signature.initVerify(pubKey);  
        signature.update(data); 
           
        return signature.verify(decryptBASE64(sign)); //��֤ǩ�� 
    } 
       
}