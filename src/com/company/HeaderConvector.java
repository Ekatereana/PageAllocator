package com.company;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public enum  HeaderConvector {
    CONVECTOR;

    public byte[] intToArray(int number) {
        byte[] arr = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putInt(number).array();
        return arr;
    }

    public Integer charArrayToInt( byte[] arr){
        return ByteBuffer.wrap(arr).order(ByteOrder.LITTLE_ENDIAN).getInt();


    }
}

