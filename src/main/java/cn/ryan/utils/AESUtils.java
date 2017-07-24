package cn.ryan.utils;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

import org.apache.tomcat.util.codec.binary.Base64;


/***
 * AES对称加密
 * 
 * @author HuRui
 *
 */
public class AESUtils {

	private static final String KEY = "U2pfBiKv/AEh6w==";

	// 加密
	public static String encrypt(String sSrc) {
		if (KEY == null) {
			System.out.print("Key为空null");
			return null;
		}
		// 判断Key是否为16位
		if (KEY.length() != 16) {
			System.out.print("Key长度不是16位");
			return null;
		}
		byte[] encrypted = null;
		try {
			byte[] raw = KEY.getBytes("utf-8");
			SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");// "算法/模式/补码方式"
			cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
			encrypted = cipher.doFinal(sSrc.getBytes("utf-8"));
		} catch (UnsupportedEncodingException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException | BadPaddingException
				| InvalidKeyException e) {
			e.printStackTrace();
		}
		return new Base64().encodeToString(encrypted);// 此处使用BASE64做转码功能，同时能起到2次加密的作用。
	}

	// 解密
	public static String decrypt(String sSrc) {
		try {
			// 判断Key是否正确
			if (KEY == null) {
				System.out.print("Key为空null");
				return null;
			}
			// 判断Key是否为16位
			if (KEY.length() != 16) {
				System.out.print("Key长度不是16位");
				return null;
			}
			byte[] raw = KEY.getBytes("utf-8");
			SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, skeySpec);
			byte[] encrypted1 = new Base64().decode(sSrc);// 先用base64解密
			try {
				byte[] original = cipher.doFinal(encrypted1);
				String originalString = new String(original, "utf-8");
				return originalString;
			} catch (Exception e) {
				System.out.println(e.toString());
				return null;
			}
		} catch (Exception ex) {
			System.out.println(ex.toString());
			return null;
		}
	}
	public static void main(String[] args) {
	}
}
