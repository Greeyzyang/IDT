package com.ryeex.sdkwatch.utils;

import android.text.TextUtils;


public class FwVerUtil {
    public static boolean isValid(String version) {
        if (TextUtils.isEmpty(version)) {
            return false;
        }

        String[] sep = version.split("\\.");

        if (sep == null || sep.length != 3) {
            return false;
        }

        try {
            int first = Integer.parseInt(sep[0]);
            int second = Integer.parseInt(sep[1]);
            int third = Integer.parseInt(sep[2]);
        } catch (Exception e) {
            return false;
        }

        return true;
    }

    //调用前请确保版本号格式有效
    public static int compare(String firstVer, String secondVer) {
        try {
            String[] firstSep = firstVer.split("\\.");
            String[] secondSep = secondVer.split("\\.");

            if (firstSep.length == secondSep.length) {
                for (int i = 0; i < firstSep.length; i++) {
                    int firstVer1 = Integer.parseInt(firstSep[i]);
                    int secondVer1 = Integer.parseInt(secondSep[i]);
                    if (firstVer1 > secondVer1) {
                        return 1;
                    } else if (firstVer1 < secondVer1) {
                        return -1;
                    }

                    if (i == (firstSep.length - 1)) {
                        return 0;
                    }
                }
            }
        } catch (Exception e) {
            return -1;
        }
        return -1;
    }

}
