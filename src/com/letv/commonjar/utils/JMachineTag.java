/**
 * =====================================================================
 *
 * @file  JMachineTag.java
 * @Module Name   com.joysee.common.utils
 * @author benz
 * @OS version  1.0
 * @Product type: JoySee
 * @date   2013-12-17
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
 * benz          2013-12-17           1.0         Check for NULL, 0 h/w
 * =====================================================================
 **/


package com.letv.commonjar.utils;

import java.net.NetworkInterface;

public class JMachineTag {

	/**
	 * @return MAC
	 */
	public static String getMACAddress(){
		byte address[];
		String mac = "";
		try {
			address = NetworkInterface.getByName("eth0").getHardwareAddress();
			for(int i=0; i<6; i++){
				mac+=String.format("%02X",address[i]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return mac;
	}
}
