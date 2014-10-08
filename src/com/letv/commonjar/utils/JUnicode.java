/**
 * =====================================================================
 *
 * @file  JUnicode.java
 * @Module Name   com.joysee.common.utils
 * @author YueLiang
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
 * YueLiang         2013年10月29日            1.0          Check for NULL, 0 h/w
 * =====================================================================
 **/

package com.letv.commonjar.utils;

public class JUnicode {

    private JUnicode() {
    }

    public static String decode(String str) throws Exception {
        if (str == null) {
            throw new IllegalAccessError("Invalid param, str is null.");
        }
        char[] in = str.toCharArray();
        int off = 0;
        char c;
        char[] out = new char[in.length];
        int outLen = 0;
        while (off < in.length) {
            c = in[off++];
            if (c == '\\') {
                if (in.length > off) { // 是否有下一个字符
                    c = in[off++]; // 取出下一个字符
                } else {
                    out[outLen++] = '\\'; // 末字符为'\'，返回
                    break;
                }
                if (c == 'u') { // 如果是"\\u"
                    int value = 0;
                    if (in.length > off + 4) { // 判断"\\u"后边是否有四个字符
                        boolean isUnicode = true;
                        for (int i = 0; i < 4; i++) { // 遍历四个字符
                            c = in[off++];
                            switch (c) {
                                case '0':
                                case '1':
                                case '2':
                                case '3':
                                case '4':
                                case '5':
                                case '6':
                                case '7':
                                case '8':
                                case '9':
                                    value = (value << 4) + c - '0';
                                    break;
                                case 'a':
                                case 'b':
                                case 'c':
                                case 'd':
                                case 'e':
                                case 'f':
                                    value = (value << 4) + 10 + c - 'a';
                                    break;
                                case 'A':
                                case 'B':
                                case 'C':
                                case 'D':
                                case 'E':
                                case 'F':
                                    value = (value << 4) + 10 + c - 'A';
                                    break;
                                default:
                                    isUnicode = false; // 判断是否为unicode码
                            }
                        }
                        if (isUnicode) { // 是unicode码转换为字符
                            out[outLen++] = (char) value;
                        } else { // 不是unicode码把"\\uXXXX"填入返回值
                            off = off - 4;
                            out[outLen++] = '\\';
                            out[outLen++] = 'u';
                            out[outLen++] = in[off++];
                        }
                    } else { // 不够四个字符则把"\\u"放入返回结果并继续
                        out[outLen++] = '\\';
                        out[outLen++] = 'u';
                        continue;
                    }
                } else {
                    switch (c) { // 判断"\\"后边是否接特殊字符，回车，tab一类的
                        case 't':
                            c = '\t';
                            out[outLen++] = c;
                            break;
                        case 'r':
                            c = '\r';
                            out[outLen++] = c;
                            break;
                        case 'n':
                            c = '\n';
                            out[outLen++] = c;
                            break;
                        case 'f':
                            c = '\f';
                            out[outLen++] = c;
                            break;
                        default:
                            out[outLen++] = '\\';
                            out[outLen++] = c;
                            break;
                    }
                }
            } else {
                out[outLen++] = (char) c;
            }
        }
        return new String(out, 0, outLen);
    }

}
