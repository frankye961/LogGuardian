package com.logguardian.util;

public class Utils {

    public static boolean checkIfJson(String message){
        return message.startsWith("{") && message.endsWith("}");
    }
}
