package com.iiordanov.util;

import java.util.Random;

public class RandomString {
    private Random r;

    public RandomString() {
        r = new Random();
    }

    public String randomString(final int length, int maxValue) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char c = (char) (r.nextInt(maxValue) + 32);
            sb.append(c);
        }
        return sb.toString();
    }

    public String randomString(final int length) {
        return randomString(length, 95);
    }

    public String randomLowerCaseString(final int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            char c = (char) (r.nextInt(25) + 97); // Get only a-z
            sb.append(c);
        }
        return sb.toString();
    }
}

