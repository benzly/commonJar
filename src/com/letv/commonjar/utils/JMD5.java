/**
 * =====================================================================
 *
 * @file  JMD5.java
 * @Module Name   com.joysee.common.utils
 * @author Benz
 * @OS version  1.0
 * @Product type: JoySee
 * @date   2013年10月29日
 * @brief  This file is the http **** implementation.
 * @This file is responsible by ANDROID TEAM.
 * @Comments: 
 * =====================================================================
 * Revision History:
 *
 *                   Modification  Tracking
 *
 * Author            Date            OS version        Reason 
 * ----------      ------------     -------------     -----------
 * Benz         2013年12月09日            1.0          Check for NULL, 0 h/w
 * =====================================================================
 **/

package com.letv.commonjar.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


public class JMD5 {

	public static char hexDigits[] = { '0', '1', '2', '3', '4', '5', '6','7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	
	
	public static boolean check(File targetFile, String password){
		MessageDigest messagedigest = null;
		String targetMD5 = null;
		try {
			messagedigest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		if(null==messagedigest){
			return false;
		}
		
		if(messagedigest!=null && targetFile!=null){
			InputStream fis;
		    try {
				fis = new FileInputStream(targetFile);
				byte[] buffer = new byte[1024];
				int numRead = 0;
				while ((numRead = fis.read(buffer)) > 0) {
					messagedigest.update(buffer, 0, numRead);
				}
				fis.close();
				targetMD5 = bufferToHex(messagedigest.digest());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if(null!=targetMD5 && !"".equals(targetMD5)){
			return targetMD5.equals(password);
		}
		return false;
	}
	
	
	public static boolean check(String targetJson, String password){
		MessageDigest messagedigest = null;
		String targetMD5 = null;
		try {
			messagedigest = MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		if(null==messagedigest){
			return false;
		}
		
		if(messagedigest!=null && targetJson!=null && !"".equals(targetJson)){
			messagedigest.update(targetJson.getBytes());
			targetMD5 = bufferToHex(messagedigest.digest());
		}
		if(null!=targetMD5 && !"".equals(targetMD5)){
			return targetMD5.equals(password);
		}
		return false;
	}
	
	
	private static String bufferToHex(byte bytes[]) {
		int lenth = bytes.length;
		int start = 0;
		StringBuffer stringbuffer = new StringBuffer(2 * lenth);
		int k = start + lenth;
		for (int l = start; l < k; l++) {
			char c0 = hexDigits[(bytes[l] & 0xf0) >> 4];
			char c1 = hexDigits[bytes[l] & 0xf];
			stringbuffer.append(c0);
			stringbuffer.append(c1);
		}
		return stringbuffer.toString();
	}
}
