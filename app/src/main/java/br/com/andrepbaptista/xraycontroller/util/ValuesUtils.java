package br.com.andrepbaptista.xraycontroller.util;

import java.util.ArrayList;
import java.util.List;

public class ValuesUtils {

    private static final String HEX_STRING_PREFIX = "00050503";
    private static final String HEX_STRING_SUFIX = "00";
    final protected static char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len/2];

        for(int i = 0; i < len; i+=2){
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }

        return data;
    }

    public static String byteArrayToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length*2];
        int v;

        for(int j=0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j*2] = hexArray[v>>>4];
            hexChars[j*2 + 1] = hexArray[v & 0x0F];
        }

        return new String(hexChars);
    }

    public static String timeToHexString(String time) {

        String value;

        switch (time) {
            case "0,05":
                value = "01";
                break;
            case "0,10":
                value = "02";
                break;
            case "0,20":
                value = "03";
                break;
            case "0,30":
                value = "04";
                break;
            case "0,50":
                value = "05";
                break;
            case "0,80":
                value = "06";
                break;
            case "1,00":
                value = "07";
                break;
            case "1,50":
                value = "08";
                break;
            case "2,00":
                value = "09";
                break;
            case "3,00":
                value = "0A";
                break;
            case "4,00":
                value = "0B";
                break;
            case "5,00":
                value = "0C";
                break;
            case "6,00":
                value = "0D";
                break;
            case "8,00":
                value = "0E";
                break;
            case "10,0":
                value = "0F";
                break;
            default:
                value = "01";
        }

        return HEX_STRING_PREFIX + value + HEX_STRING_SUFIX;

    }

    public static int hexStringToTimeListIndex(String hexString) {

        int beginIndex = HEX_STRING_PREFIX.length();
        int endIndex = HEX_STRING_PREFIX.length() + 2;
        String time = hexString.substring(beginIndex, endIndex);

        switch (time) {
            case "01":
                return 0;
            case "02":
                return 1;
            case "03":
                return 2;
            case "04":
                return 3;
            case "05":
                return 4;
            case "06":
                return 5;
            case "07":
                return 6;
            case "08":
                return 7;
            case "09":
                return 8;
            case "0A":
                return 9;
            case "0B":
                return 10;
            case "0C":
                return 11;
            case "0D":
                return 12;
            case "0E":
                return 13;
            case "0F":
                return 14;
            default:
                return 0;
        }

    }

    public static List<String> getTimesList() {
        List<String> times = new ArrayList<>();

        times.add(0 ,"0,05");
        times.add(1 ,"0,10");
        times.add(2 ,"0,20");
        times.add(3 ,"0,30");
        times.add(4 ,"0,50");
        times.add(5 ,"0,80");
        times.add(6 ,"1,00");
        times.add(7 ,"1,50");
        times.add(8 ,"2,00");
        times.add(9 ,"3,00");
        times.add(10 ,"4,00");
        times.add(11 ,"5,00");
        times.add(12 ,"6,00");
        times.add(13 ,"8,00");
        times.add(14 ,"10,0");

        return times;
    }

}
