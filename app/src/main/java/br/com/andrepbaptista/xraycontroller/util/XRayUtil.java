package br.com.andrepbaptista.xraycontroller.util;

public class XRayUtil {

    private static final String HEX_STRING_PREFIX = "00050503";
    private static final String HEX_STRING_SUFIX = "00";

    public static String timeToHexString(String time) {

        String value;

        switch (time) {
            case "0,1":
                value = "0,1";
                break;
            default:
                value = "0,1";
        }

        return HEX_STRING_PREFIX + value + HEX_STRING_SUFIX;

    }

}
