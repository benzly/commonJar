/**
 * =====================================================================
 *
 * @file JZip.java
 * @Module Name com.joysee.common.utils
 * @author benz
 * @OS version 1.0
 * @Product type: JoySee
 * @date 2013-12-11
 * @brief This file is the http **** implementation.
 * @This file is responsible by ANDROID TEAM.
 * @Comments: ===================================================================== Revision
 *            History:
 *
 *            Modification Tracking
 *
 *            Author Date OS version Reason ---------- ------------ ------------- ----------- benz
 *            2013-12-11 1.0 Check for NULL, 0 h/w
 *            =====================================================================
 **/


package com.letv.commonjar.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import android.os.SystemClock;

import com.letv.commonjar.CLog;


public class JZip {

    private static final int BUFF_SIZE = 1024 * 1024;


    public static boolean zipFile(File zipFile) throws ZipException {
        boolean ret = false;
        String folderPath = zipFile.getAbsolutePath();
        long begin = SystemClock.currentThreadTimeMillis();
        CLog.d("JZip", "------parser zip : " + folderPath);
        InputStream in = null;
        OutputStream out = null;
        try {
            ZipFile zf = new ZipFile(zipFile);
            for (Enumeration<?> entries = zf.entries(); entries.hasMoreElements();) {
                ZipEntry entry = ((ZipEntry) entries.nextElement());
                in = zf.getInputStream(entry);
                String str = folderPath + File.separator + entry.getName();
                str = new String(str.getBytes("8859_1"), "GB2312");
                File desFile = new File(str);
                if (!desFile.exists()) {
                    File fileParentDir = desFile.getParentFile();
                    if (!fileParentDir.exists()) {
                        fileParentDir.mkdirs();
                    }
                    desFile.createNewFile();
                }
                out = new FileOutputStream(desFile);
                byte buffer[] = new byte[BUFF_SIZE];
                int realLength;
                while ((realLength = in.read(buffer)) > 0) {
                    out.write(buffer, 0, realLength);
                }
                ret = true;
                in.close();
                out.close();
            }
        } catch (IOException e) {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            ret = false;
        } finally {
            if (zipFile.exists()) {
                zipFile.deleteOnExit();
            }
        }
        StringBuffer sb = new StringBuffer();
        sb.append("------parser zip over------ ret=");
        sb.append(ret);
        sb.append("----time = " + (SystemClock.currentThreadTimeMillis() - begin));
        CLog.d("JZip", sb.toString());
        return ret;
    }

}
