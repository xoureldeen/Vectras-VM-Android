package com.vectras.vm.utils;

import java.util.Random;

public class TextUtils {
    public static String randomALetter() {
        String addAdb;
        Random random = new Random();
        int randomAbc = random.nextInt(26);
        if (randomAbc == 0) {
            addAdb = "a";
        } else if (randomAbc == 1) {
            addAdb = "b";
        } else if (randomAbc == 2) {
            addAdb = "c";
        } else if (randomAbc == 3) {
            addAdb = "d";
        } else if (randomAbc == 4) {
            addAdb = "e";
        } else if (randomAbc == 5) {
            addAdb = "f";
        } else if (randomAbc == 6) {
            addAdb = "g";
        } else if (randomAbc == 7) {
            addAdb = "h";
        } else if (randomAbc == 8) {
            addAdb = "i";
        } else if (randomAbc == 9) {
            addAdb = "j";
        } else if (randomAbc == 10) {
            addAdb = "k";
        } else if (randomAbc == 11) {
            addAdb = "l";
        } else if (randomAbc == 12) {
            addAdb = "m";
        } else if (randomAbc == 13) {
            addAdb = "n";
        } else if (randomAbc == 14) {
            addAdb = "o";
        } else if (randomAbc == 15) {
            addAdb = "p";
        } else if (randomAbc == 16) {
            addAdb = "q";
        } else if (randomAbc == 17) {
            addAdb = "r";
        } else if (randomAbc == 18) {
            addAdb = "s";
        } else if (randomAbc == 19) {
            addAdb = "t";
        } else if (randomAbc == 20) {
            addAdb = "u";
        } else if (randomAbc == 21) {
            addAdb = "v";
        } else if (randomAbc == 22) {
            addAdb = "w";
        } else if (randomAbc == 23) {
            addAdb = "x";
        } else if (randomAbc == 24) {
            addAdb = "y";
        } else {
            addAdb = "z";
        }
        return addAdb;
    }
}
