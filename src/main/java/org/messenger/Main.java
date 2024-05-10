package org.messenger;

import spark.debug.DebugScreen;

import java.net.CookieManager;
import java.net.CookiePolicy;

import static spark.Spark.*;

public class Main {
    public static void main(String[] args) {
        //String keyStoreLocation = "src/main/resources/keystore.jks";
        //String keyStorePassword = "qwerty";
        //secure(keyStoreLocation, keyStorePassword, null, null);
        port(2024);

        DebugScreen.enableDebugScreen();

        Routes.init();
    }
}