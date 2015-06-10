package services.goldway.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.apache.commons.codec.binary.Base64;

/**
 * RSAHelper - 对RSA 签名&验签/分段加密&分段解密 的包装
 * 签名算法: "SHA1withRSA", 私钥进行签名; 公钥进行验签.
 * 加密算法: "RSA/ECB/PKCS1Padding", 公钥进行加密; 私钥进行解密.
 *
 * [localPrivKey]是自己的私钥, 自己的公钥给通信对方.
 * [peerPubKey]是对方的公钥, 对方的私钥在对方那边.
 * 为了方便, 这里假定双方的密钥长度一致, 签名和加密的规则也一致.
 *
 * 以`Base64Str`结尾的参数表示内容是Base64编码的字符串, 其他情况都是raw字符串.
 *
 * @author sangechen
 *
 */
public class RSAHelper {

	public static final String KEY_ALGORITHM = "RSA";
	public static final String SIGNATURE_ALGORITHM = "SHA1withRSA";
	public static final String CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding"; //加密block需要预留11字节
	public static final int KEYBIT = 2048;
	public static final int RESERVEBYTES = 11;

	private KeyFactory keyFactory;
	private Signature signature;
	private Cipher cipher;

	private PrivateKey localPrivKey;
	private PublicKey peerPubKey;
	private int encryptBlock;
	private int decryptBlock;

	public RSAHelper() {
		try {
			keyFactory = KeyFactory.getInstance(KEY_ALGORITHM);
			signature = Signature.getInstance(SIGNATURE_ALGORITHM);
			cipher = Cipher.getInstance(CIPHER_ALGORITHM);

			localPrivKey = null;
			peerPubKey = null;
			decryptBlock = KEYBIT / 8; //256 bytes
			encryptBlock = decryptBlock - RESERVEBYTES; //245 bytes
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * 初始化自己的私钥,对方的公钥以及密钥长度.
	 * `openssl genrsa -out rsa_2048.key 2048` #指定生成的密钥的位数: 2048
	 * `openssl pkcs8 -topk8 -inform PEM -in rsa_2048.key -outform PEM -nocrypt -out pkcs8.txt` #for Java 转换成PKCS#8编码
	 * `openssl rsa -in rsa_2048.key -pubout -out rsa_2048_pub.key` #导出pubkey
	 * @param localPrivKeyBase64Str Base64编码的私钥,PKCS#8编码. (去掉pem文件中的头尾标识)
	 * @param peerPubKeyBase64Str Base64编码的公钥. (去掉pem文件中的头尾标识)
	 * @param keysize 密钥长度, 一般2048
	 */
	public void initKey(String localPrivKeyBase64Str, String peerPubKeyBase64Str, int keysize)
	{
		try {
			localPrivKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(Base64.decodeBase64(localPrivKeyBase64Str)));
			peerPubKey = keyFactory.generatePublic(new X509EncodedKeySpec(Base64.decodeBase64(peerPubKeyBase64Str)));
			decryptBlock = keysize / 8;
			encryptBlock = decryptBlock - RESERVEBYTES;
		} catch (InvalidKeySpecException e) {
			e.printStackTrace();
		}
	}

	public void initSignature(String algorithm)
	{
		try {
			signature = Signature.getInstance(algorithm);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}

	public String sign(String plaintext) throws UnsupportedEncodingException
	{
		String signBase64Str = "";

		try {
			signature.initSign(localPrivKey);
			signature.update(getBytesDefault(plaintext));
			signBase64Str = Base64.encodeBase64String(signature.sign());
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (SignatureException e) {
			e.printStackTrace();
		}

		return signBase64Str;
	}

	public boolean verify(String plaintext, String signBase64Str) throws UnsupportedEncodingException
	{
		boolean isValid = false;

		try {
			signature.initVerify(peerPubKey);
			signature.update(getBytesDefault(plaintext));
			isValid = signature.verify(Base64.decodeBase64(signBase64Str));
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (SignatureException e) {
			e.printStackTrace();
		}

		return isValid;
	}

	public void initCipher(String transformation)
	{
		try {
			cipher = Cipher.getInstance(transformation);
			//TODO decryptBlock和encryptBlock可能需要重新计算
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		}
	}

	public String encrypt(String plaintext) throws UnsupportedEncodingException
	{
		//转换得到字节流
		byte[] data = getBytesDefault(plaintext); //FIXME UTF-8

		//计算分段加密的block数 (向上取整)
		int nBlock = (data.length / encryptBlock);
		if ((data.length % encryptBlock) != 0) //余数非0block数再加1
		{
			nBlock += 1;
		}
		//for debug. System.out.printf("encryptBlock: %d/%d ~ %d\n", data.length, encryptBlock, nBlock);

		//输出buffer, 大小为nBlock个decryptBlock
		ByteArrayOutputStream outbuf = new ByteArrayOutputStream(nBlock * decryptBlock);

		try {
			cipher.init(Cipher.ENCRYPT_MODE, peerPubKey);
			//cryptedBase64Str = Base64.encodeBase64String(cipher.doFinal(plaintext.getBytes()));

			//分段加密
			for (int offset = 0; offset < data.length; offset += encryptBlock)
			{
				//block大小: encryptBlock 或 剩余字节数
				int inputLen = (data.length - offset);
				if (inputLen > encryptBlock)
				{
					inputLen = encryptBlock;
				}

				//得到分段加密结果
				byte[] encryptedBlock = cipher.doFinal(data, offset, inputLen);
				//追加结果到输出buffer中
				outbuf.write(encryptedBlock);
			}
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return Base64.encodeBase64String(outbuf.toByteArray()); //ciphertext
	}

	public String decrypt(String cryptedBase64Str) 
	{
		//转换得到字节流
		byte[] data= Base64.decodeBase64(cryptedBase64Str);
		
		//计算分段解密的block数 (理论上应该能整除)
		int nBlock = (data.length / decryptBlock);
		//for debug. System.out.printf("decryptBlock: %d/%d ~ %d\n", data.length, decryptBlock, nBlock);

		//输出buffer, , 大小为nBlock个encryptBlock
		ByteArrayOutputStream outbuf = new ByteArrayOutputStream(nBlock * encryptBlock);

		try {
			cipher.init(Cipher.DECRYPT_MODE, localPrivKey);
			//plaintext = new String(cipher.doFinal(Base64.decodeBase64(cryptedBase64Str)));

			//分段解密
			for (int offset = 0; offset < data.length; offset += decryptBlock)
			{
				//block大小: decryptBlock 或 剩余字节数
				int inputLen = (data.length - offset);
				if (inputLen > decryptBlock)
				{
					inputLen = decryptBlock;
				}

				//得到分段解密结果
				byte[] decryptedBlock = cipher.doFinal(data, offset, inputLen);
				//追加结果到输出buffer中
				outbuf.write(decryptedBlock);
			}
			outbuf.flush();//---写完成后，需要刷新缓冲区，并且关闭缓冲
			outbuf.close();
		} catch (InvalidKeyException e) {
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			e.printStackTrace();
		} catch (BadPaddingException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			return outbuf.toString("GBK");//---返回数据需要编码为中文，因此需要转码为UTF-8格式 created by liu.zheng
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * @author Aaron.Wu
	 * @date 2014-7-1 上午10:04:17
	 * @description 修改字符编码，因为接收方是硬编成GBK的
	 */
	private byte[] getBytesDefault(String plaintext) throws UnsupportedEncodingException{
		return plaintext.getBytes("GBK");
	}
}
