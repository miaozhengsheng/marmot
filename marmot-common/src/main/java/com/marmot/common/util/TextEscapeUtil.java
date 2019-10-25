package com.marmot.common.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TextEscapeUtil {


    public static final char SHELTER = '*';

    private static final String EMAIL_REGEX = "[\\w[.-]]+@[\\w[.-]]+\\.[\\w]+";
    private static final Pattern EMAIL_PATTERN = Pattern.compile(EMAIL_REGEX);

    private static final String MOBILE_REGEX = "(?<!\\d)(?:(?:1[345678]\\d{9}))(?!\\d)";
    private static final Pattern MOBILE_PATTERN = Pattern.compile(MOBILE_REGEX);

    public static String emailEscape(String text) {
        StringBuilder builder = new StringBuilder(text);
        emailEscape(text, builder);
        return builder.toString();
    }

    private static void emailEscape(String text, final StringBuilder builder) {
        int pos = text.indexOf("@");
        if (pos == -1) {
            return;
        }
        try {
            Matcher matcher = EMAIL_PATTERN.matcher(text);
            while (matcher.find()) {
                int start = matcher.start();
                int middle = (pos - start) / 2;
                if (middle < pos) {
                    for (int i = start + middle; i < pos; i++) {
                        builder.setCharAt(i, SHELTER);
                    }
                }
                pos = text.indexOf("@", pos + 1);
            }
        } catch (Exception e) {
            // ignore
        }
    }

    public static String mobileEscape(String text) {
        StringBuilder builder = new StringBuilder(text);
        mobileEscape(text, builder);
        return builder.toString();
    }

    private static void mobileEscape(String text, final StringBuilder builder) {
        try {
            Matcher matcher = MOBILE_PATTERN.matcher(text);
            while (matcher.find()) {
                int pos = matcher.start();
                for (int i = pos + 3; i < pos + 7; i++) {
                    builder.setCharAt(i, SHELTER);
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * 全部过滤<br>
     * 目前包括：邮箱、手机号<br>
     * 
     * @param text
     * @return
     */
    public static String allEscape(String text) {
        StringBuilder builder = new StringBuilder(text);
        emailEscape(text, builder);
        mobileEscape(text, builder);
        return builder.toString();
    }


}
