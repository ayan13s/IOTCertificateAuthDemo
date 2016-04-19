
package com.ibm.bluemixmqtt;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.security.Key;
import java.util.Base64;
import java.util.Random;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;


public class IOTSecurityUtil {

	/**
	 * This method reads the properties from the config file
	 * @param filePath
	 * @return
	 */
	public static String getMACAdress(String dType, String dId) {
		 String strMACAdress = null;
	     InetAddress ip;
	        try {

	            ip = InetAddress.getLocalHost();
	            System.out.println("Current IP address : " + ip.getHostAddress());

	            NetworkInterface network = NetworkInterface.getByInetAddress(ip);

	            byte[] mac = network.getHardwareAddress();

	            System.out.print("Current MAC address : ");

	            StringBuilder sb = new StringBuilder();
	            for (int i = 0; i < mac.length; i++) {
	                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));        
	            }
	            strMACAdress = dType + sb.toString();
	            System.out.println("Mac address for " + dId + "is" + strMACAdress);
	            
	        } catch (UnknownHostException e) {
	            e.printStackTrace();
	        } catch (SocketException e){
	            e.printStackTrace();
	        }
	        return strMACAdress;
	}
	
	public static String encryptString(String strMsg, String strKey, String iv)
	{
        String strReturn = null;
        IvParameterSpec ivspec = null;
        SecretKeySpec keyspec;;
		try{
		// Create key and cipher
		ivspec = new IvParameterSpec(iv.getBytes());
		keyspec = new SecretKeySpec(strKey.getBytes(), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        
        // encrypt the text
        cipher.init(Cipher.ENCRYPT_MODE, keyspec, ivspec);
        byte[] encrypted = cipher.doFinal(strMsg.getBytes("UTF-8"));
        strReturn = Base64.getEncoder().encodeToString(encrypted);
        } catch(Exception e){
        	e.printStackTrace();
        }
		return strReturn;
	}
	
	public static String decryptString(byte[] strMsg, String strKey, String iv)
	{
        String strReturn = null;
        IvParameterSpec ivspec = null;
        SecretKeySpec keyspec;;
		try{
		// Create key and cipher
		ivspec = new IvParameterSpec(iv.getBytes());
		keyspec = new SecretKeySpec(strKey.getBytes(), "AES");        
		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        
        // decrypt the text
        cipher.init(Cipher.DECRYPT_MODE, keyspec, ivspec);
        strReturn = new String(cipher.doFinal(Base64.getDecoder().decode(strMsg)));
        //System.err.println(strReturn);
        } catch(Exception e){
        	e.printStackTrace();
        }
		return strReturn;
	}
	
	public static String generateOTP()
	{
		Random r = new Random();
		String otp = new String();
		for(int i=0 ; i < 8 ; i++) {
			otp += r.nextInt(10);
		}
		System.out.println("Generated OTP - " + otp);
		return otp;
	}
}
