package com.fm.zhangzhengchao.fmpufdeom.utils;

import android.util.Log;

/**
 * Created by zhangzhengchao on 2017/8/24.
 */

public class HexstringAndBytesConvert {

    private static final String TAG = "Hexstring";

    public static String bytesToHex(byte[] src){
        if (src == null || src.length <= 0) {
            return null;
        }

        StringBuilder stringBuilder = new StringBuilder("");
        for (int i = 0; i < src.length; i++) {
            // 之所以用byte和0xff相与，是因为int是32位，与0xff相与后就舍弃前面的24位，只保留后8位
            String str = Integer.toHexString(src[i] & 0xff);
            if (str.length() < 2) { // 不足两位要补0
                stringBuilder.append(0);
            }
            stringBuilder.append(str);
        }
        return stringBuilder.toString();
    }


    public static byte[] hexToBytes(String hexString) {
        if (hexString == null || hexString.equals("")) {
            return null;
        }
        hexString= hexString.toLowerCase();
        Log.i(TAG,hexString);
        int length = hexString.length() / 2;
        char[] hexChars = hexString.toCharArray();
        byte[] bytes = new byte[length];
        String hexDigits = "0123456789abcdef";
        for (int i = 0; i < length; i++) {
            int pos = i * 2; // 两个字符对应一个byte
            int h = hexDigits.indexOf(hexChars[pos]) << 4; // 注1
            int l = hexDigits.indexOf(hexChars[pos + 1]); // 注2
            if(h == -1 || l == -1) { // 非16进制字符
                return null;
            }
            bytes[i] = (byte) (h | l);
        }
        return bytes;
    }
}
