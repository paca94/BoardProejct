package com.example.jihwa.androidbluetoothwithbluecoveprac;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by jihwa on 2017-05-19.
 */

public class JHProtocolUnitTest {
    @Test
    public void arrayCombine_correct() throws Exception {
        byte[] bytes1 = {1,2,3};
        byte[] bytes2 = {4,5,6};

        byte[] byteActual = JHProtocol.arrayCombine(bytes1,bytes2);
        byte[] byteExpected = new byte[]{1,2,3,4,5,6};
        StringBuilder actual = new StringBuilder();
        StringBuilder expected = new StringBuilder();
        for(int i = 0; i < byteActual.length ; i++){
            actual.append(byteActual[i]);
            expected.append(byteExpected[i]);
        }
        assertEquals(expected.toString(), actual.toString());
    }
    @Test
    public void separateGetProtocol_correct() throws Exception{
        byte[] baseValue = "[SMSG]this is name".getBytes();
        byte[] bytesActual = JHProtocol.separateGetProtocolReturnByteArray(baseValue);
        byte[] bytesExpected = "[SMSG]".getBytes();
        StringBuilder actual = new StringBuilder();
        StringBuilder expected = new StringBuilder();
        for(int i = 0; i < bytesActual.length ; i++){
            actual.append(bytesActual[i]);
            if(i < bytesExpected.length)
                expected.append(bytesExpected[i]);
        }
        assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void separateGetBody_correct() throws Exception{
        byte[] baseValue = "[SMSG]this is name".getBytes();
        byte[] bytesActual = JHProtocol.separateGetBodyReturnByteArray(baseValue);
        byte[] bytesExpected = "this is name".getBytes();
        StringBuilder actual = new StringBuilder(new String(bytesActual,"UTF-8"));
        StringBuilder expected = new StringBuilder(new String(bytesExpected,"UTF-8"));

        assertEquals(expected.toString(), actual.toString());
    }
}