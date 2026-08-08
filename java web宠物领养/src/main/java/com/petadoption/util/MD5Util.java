package com.petadoption.util;

import org.apache.commons.codec.digest.DigestUtils;

public class MD5Util {
    public static String md5(String input) {
        return DigestUtils.md5Hex(input);
    }
}
