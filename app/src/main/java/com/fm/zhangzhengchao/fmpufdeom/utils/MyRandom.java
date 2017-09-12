package com.fm.zhangzhengchao.fmpufdeom.utils;

import java.util.Random;

/**
 * Created by zhangzhengchao on 2017/8/30.
 */

public class MyRandom {
    public static String generate4ByteRandomNumber(){
        byte[] byterandom=new byte[4];
        Random random=new Random();
        random.nextBytes(byterandom);
        String randomNumber = HexstringAndBytesConvert.bytesToHex(byterandom);
        return randomNumber;
    }
}
