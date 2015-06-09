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
 * RSAHelper - ��RSA ǩ��&��ǩ/�ֶμ���&�ֶν��� �İ�װ
 * ǩ���㷨: "SHA1withRSA", ˽Կ����ǩ��; ��Կ������ǩ.
 * �����㷨: "RSA/ECB/PKCS1Padding", ��Կ���м���; ˽Կ���н���.
 *
 * [localPrivKey]���Լ���˽Կ, �Լ��Ĺ�Կ��ͨ�ŶԷ�.
 * [peerPubKey]�ǶԷ��Ĺ�Կ, �Է���˽Կ�ڶԷ��Ǳ�.
 * Ϊ�˷���, ����ٶ�˫������Կ����һ��, ǩ���ͼ��ܵĹ���Ҳһ��.
 *
 * ��`Base64Str`��β�Ĳ�����ʾ������Base64������ַ���, �����������raw�ַ���.
 *
 * @author sangechen
 *
 */
public class RSAHelper {

	public static final String KEY_ALGORITHM = "RSA";
	public static final String SIGNATURE_ALGORITHM = "SHA1withRSA";
	public static final String CIPHER_ALGORITHM = "RSA/ECB/PKCS1Padding"; //����block��ҪԤ��11�ֽ�
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
	 * ��ʼ���Լ���˽Կ,�Է��Ĺ�Կ�Լ���Կ����.
	 * `openssl genrsa -out rsa_2048.key 2048` #ָ�����ɵ���Կ��λ��: 2048
	 * `openssl pkcs8 -topk8 -inform PEM -in rsa_2048.key -outform PEM -nocrypt -out pkcs8.txt` #for Java ת����PKCS#8����
	 * `openssl rsa -in rsa_2048.key -pubout -out rsa_2048_pub.key` #����pubkey
	 * @param localPrivKeyBase64Str Base64�����˽Կ,PKCS#8����. (ȥ��pem�ļ��е�ͷβ��ʶ)
	 * @param peerPubKeyBase64Str Base64����Ĺ�Կ. (ȥ��pem�ļ��е�ͷβ��ʶ)
	 * @param keysize ��Կ����, һ��2048
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
			//TODO decryptBlock��encryptBlock������Ҫ���¼���
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NoSuchPaddingException e) {
			e.printStackTrace();
		}
	}

	public String encrypt(String plaintext) throws UnsupportedEncodingException
	{
		//ת���õ��ֽ���
		byte[] data = getBytesDefault(plaintext); //FIXME UTF-8

		//����ֶμ��ܵ�block�� (����ȡ��)
		int nBlock = (data.length / encryptBlock);
		if ((data.length % encryptBlock) != 0) //������0block���ټ�1
		{
			nBlock += 1;
		}
		//for debug. System.out.printf("encryptBlock: %d/%d ~ %d\n", data.length, encryptBlock, nBlock);

		//���buffer, ��СΪnBlock��decryptBlock
		ByteArrayOutputStream outbuf = new ByteArrayOutputStream(nBlock * decryptBlock);

		try {
			cipher.init(Cipher.ENCRYPT_MODE, peerPubKey);
			//cryptedBase64Str = Base64.encodeBase64String(cipher.doFinal(plaintext.getBytes()));

			//�ֶμ���
			for (int offset = 0; offset < data.length; offset += encryptBlock)
			{
				//block��С: encryptBlock �� ʣ���ֽ���
				int inputLen = (data.length - offset);
				if (inputLen > encryptBlock)
				{
					inputLen = encryptBlock;
				}

				//�õ��ֶμ��ܽ��
				byte[] encryptedBlock = cipher.doFinal(data, offset, inputLen);
				//׷�ӽ�������buffer��
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
		//ת���õ��ֽ���
		byte[] data= Base64.decodeBase64(cryptedBase64Str);
		
		//����ֶν��ܵ�block�� (������Ӧ��������)
		int nBlock = (data.length / decryptBlock);
		//for debug. System.out.printf("decryptBlock: %d/%d ~ %d\n", data.length, decryptBlock, nBlock);

		//���buffer, , ��СΪnBlock��encryptBlock
		ByteArrayOutputStream outbuf = new ByteArrayOutputStream(nBlock * encryptBlock);

		try {
			cipher.init(Cipher.DECRYPT_MODE, localPrivKey);
			//plaintext = new String(cipher.doFinal(Base64.decodeBase64(cryptedBase64Str)));

			//�ֶν���
			for (int offset = 0; offset < data.length; offset += decryptBlock)
			{
				//block��С: decryptBlock �� ʣ���ֽ���
				int inputLen = (data.length - offset);
				if (inputLen > decryptBlock)
				{
					inputLen = decryptBlock;
				}

				//�õ��ֶν��ܽ��
				byte[] decryptedBlock = cipher.doFinal(data, offset, inputLen);
				//׷�ӽ�������buffer��
				outbuf.write(decryptedBlock);
			}
			outbuf.flush();//---д��ɺ���Ҫˢ�»����������ҹرջ���
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
			return outbuf.toString("GBK");//---����������Ҫ����Ϊ���ģ������Ҫת��ΪUTF-8��ʽ created by liu.zheng
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * @author Aaron.Wu
	 * @date 2014-7-1 ����10:04:17
	 * @description �޸��ַ����룬��Ϊ���շ���Ӳ���GBK��
	 */
	private byte[] getBytesDefault(String plaintext) throws UnsupportedEncodingException{
		return plaintext.getBytes("GBK");
	}
}
