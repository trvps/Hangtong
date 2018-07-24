package com.htdz.liteguardian.util;

import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.KeyPair;  
import java.security.KeyPairGenerator;  
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;  
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.BadPaddingException;  
import javax.crypto.Cipher;  
import javax.crypto.IllegalBlockSizeException;  
import javax.crypto.NoSuchPaddingException;

import sun.misc.BASE64Decoder;
import sun.misc.BASE64Encoder;  
  
public class EncryptRSA {  
      
    public static byte[] decryptBASE64(String key) throws Exception {               

        return (new BASE64Decoder()).decodeBuffer(key);               

    }                                 

               

    public static String encryptBASE64(byte[] key) throws Exception {               

        return (new BASE64Encoder()).encodeBuffer(key);               

    }
	
    /** 
     * 得到公钥 
     * @param key 密钥字符串（经过base64编码） 
     * @throws Exception 
     */  
    public static PublicKey getPublicKey(String key) throws Exception {  
          byte[] keyBytes;  
          keyBytes = (new BASE64Decoder()).decodeBuffer(key);  
          X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);  
          KeyFactory keyFactory = KeyFactory.getInstance("RSA");  
          PublicKey publicKey = keyFactory.generatePublic(keySpec);  
          return publicKey;  
    }  
    
    /** 
     * 得到私钥 
     * @param key 密钥字符串（经过base64编码） 
     * @throws Exception 
     */  
    public static PrivateKey getPrivateKey(String key) throws Exception {  
          byte[] keyBytes;  
          keyBytes = (new BASE64Decoder()).decodeBuffer(key);  
          PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);  
          KeyFactory keyFactory = KeyFactory.getInstance("RSA");  
          PrivateKey privateKey = keyFactory.generatePrivate(keySpec);  
          return privateKey;  
    }  
    
    
    /** 
     * 加密 
     * @param publicKey 
     * @param srcBytes 
     * @return 
     * @throws NoSuchAlgorithmException 
     * @throws NoSuchPaddingException 
     * @throws InvalidKeyException 
     * @throws IllegalBlockSizeException 
     * @throws BadPaddingException 
     */  
    public byte[] encrypt(RSAPublicKey publicKey,byte[] srcBytes) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{  
        if(publicKey!=null){  
            //Cipher负责完成加密或解密工作，基于RSA  
            Cipher cipher = Cipher.getInstance("RSA");  
            //根据公钥，对Cipher对象进行初始化  
            cipher.init(Cipher.ENCRYPT_MODE, publicKey);  
            byte[] resultBytes = cipher.doFinal(srcBytes);  
            return resultBytes;  
        }  
        return null;  
    }  
      
    /** 
     * 解密  
     * @param privateKey 
     * @param srcBytes 
     * @return 
     * @throws NoSuchAlgorithmException 
     * @throws NoSuchPaddingException 
     * @throws InvalidKeyException 
     * @throws IllegalBlockSizeException 
     * @throws BadPaddingException 
     */  
    public byte[] decrypt(RSAPrivateKey privateKey,byte[] srcBytes) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException{  
        if(privateKey!=null){  
            //Cipher负责完成加密或解密工作，基于RSA  
            Cipher cipher = Cipher.getInstance("RSA");  
            //根据公钥，对Cipher对象进行初始化  
            cipher.init(Cipher.DECRYPT_MODE, privateKey);  
            byte[] resultBytes = cipher.doFinal(srcBytes);  
            return resultBytes;  
        }  
        return null;  
    }  
  
    /** 
     * @param args 
     * @throws Exception 
     */  
    public static void main(String[] args) throws Exception {  
        EncryptRSA rsa = new EncryptRSA();  
       // String msg = "{\"orderNum\" : \"0000001\",\"insured\" : \"Tom\",\"email\" : \"Tom@castelbds.com\",\"amount\" : \"100000\",\"orderTime\" : \"2018-01-03 17:10:10\"}";  
        String msg = "insured";
        //KeyPairGenerator类用于生成公钥和私钥对，基于RSA算法生成对象  
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");  
        //初始化密钥对生成器，密钥大小为1024位  
        keyPairGen.initialize(1024);  
        //生成一个密钥对，保存在keyPair中  
        KeyPair keyPair = keyPairGen.generateKeyPair();  
        //得到私钥  
        RSAPrivateKey privateKey = (RSAPrivateKey)keyPair.getPrivate();               
        //得到公钥  
        RSAPublicKey publicKey = (RSAPublicKey)keyPair.getPublic();  
          
        Key key = (Key) publicKey; 
     
		String publicKeyStr = encryptBASE64(key.getEncoded());

		key = (Key) privateKey; 
				
		String priKeyStr = encryptBASE64(key.getEncoded());
		
		//PublicKey publicKey1 = getPublicKey(publicKeyStr);
        
        //用公钥加密  
        byte[] srcBytes = msg.getBytes();  
       // byte[] resultBytes = rsa.encrypt(publicKey, srcBytes);  
        byte[] resultBytes = rsa.encrypt((RSAPublicKey)getPublicKey(publicKeyStr), srcBytes);   
        
        byte[] resultBytes1 = decryptBASE64(encryptBASE64(resultBytes));
        
        //用私钥解密  
        byte[] decBytes = rsa.decrypt(privateKey, resultBytes1);  
        
        System.out.println("publicKeyStr:" + publicKeyStr); 
        System.out.println("priKeyStr:" + priKeyStr); 
        System.out.println("明文是:" + msg);  
        //System.out.println("加密后是:" + new String(resultBytes)); 
        System.out.println("加密后是:" + encryptBASE64(resultBytes)); 
        
        System.out.println("解密后是:" + new String(decBytes));  
    }  
  
}  

