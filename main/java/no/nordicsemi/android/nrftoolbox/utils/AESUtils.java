package no.nordicsemi.android.nrftoolbox.utils;

import android.util.Base64;

import java.security.Key;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class AESUtils {

	public static final String KEY_ALGORITHM = "AES";
	public static String charset = "utf-8";
	public static final String CIPHER_ALGORITHM = "AES/ECB/PKCS5Padding";

	public static Key toKey(byte[] key) throws Exception {
		return new SecretKeySpec(key, KEY_ALGORITHM);
	}

	public static String encrypt(String data, String key) throws Exception {
		Key k = toKey(key.getBytes(charset));
		Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
		cipher.init(Cipher.ENCRYPT_MODE, k);
		return Base64.encodeToString(cipher.doFinal(data.getBytes()), Base64.DEFAULT);
	}  

	public static String decrypt(String data, String key) throws Exception {
		Key k = toKey(key.getBytes(charset));
		Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
		cipher.init(Cipher.DECRYPT_MODE, k);
		return new String(cipher.doFinal(Base64.decode(data, Base64.DEFAULT)));
	}  
}
