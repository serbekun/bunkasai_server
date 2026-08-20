package com.serbekun;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serbekun.bunkasai.BuildInfo;
import com.serbekun.bunkasai.http.handles.StaticRoutes;
import com.serbekun.bunkasai.http.handles.V0Health;

import io.javalin.Javalin;

import com.serbekun.bunkasai.http.InitHttp;
import com.serbekun.bunkasai.http.handles.HttpHandler;
import com.serbekun.bunkasai.resources.ResourceCache;
import com.serbekun.bunkasai.resources.ResourceLoader;
import com.serbekun.bunkasai.service.resource.ResourcesService;


public final class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        
        log.info("Bunkasai!");
        log.info("Ver: " + BuildInfo.version());

        /**
         * 3. Resource layer (loader -> cache -> service)
         */
        ResourceLoader resourceLoader = new ResourceLoader();
        ResourceCache resourceCache = new ResourceCache(resourceLoader);
        ResourcesService resourcesService = new ResourcesService(resourceLoader, resourceCache);

        /**
         * 4. HTTP layer (handlers with DI)
         */
        List<HttpHandler> handlers = List.of(
            new V0Health(),
            new StaticRoutes(resourcesService)
        );

        Javalin svr = Javalin.create();
        InitHttp initHttp = new InitHttp(svr, 8080, handlers);
        initHttp.initHttp();

    }
}
