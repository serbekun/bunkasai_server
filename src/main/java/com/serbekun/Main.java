package com.serbekun;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serbekun.bunkasai.BuildInfo;

public final class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        
        log.info("Bunkasai!");
        log.info("Ver: " + BuildInfo.version());
    }
}
