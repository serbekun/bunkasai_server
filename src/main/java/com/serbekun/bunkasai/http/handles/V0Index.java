package com.serbekun.bunkasai.http.handles;

import com.serbekun.bunkasai.domain.http.dto.ErrorRes;
import com.serbekun.bunkasai.service.resource.ResourcesService;

import io.javalin.Javalin;
import io.javalin.http.HttpStatus;

/**
 * <p>Handles the GET request to the <code>/</code> endpoint.</p>
 *
 * <p>Serves the site entry point — <code>html/index.html</code> — so the
 * frontend is reachable at the root instead of the deep static URL
 * <code>/static/v0/html/index.html</code>.</p>
 *
 * <p>The page itself is read through {@link ResourcesService}, i.e. the same
 * cached resource layer the <code>/static/v0/*</code> routes use.</p>
 */
public class V0Index implements HttpHandler {

    /** Page served at the root of the site. */
    private static final String INDEX_PAGE = "index.html";

    private static final String CONTENT_TYPE = "text/html; charset=utf-8";

    private final ResourcesService resourcesService;

    public V0Index(ResourcesService resourcesService) {
        this.resourcesService = resourcesService;
    }

    @Override
    public void register(Javalin svr) {
        svr.get("/", ctx -> {
            String page = resourcesService.getHtml(INDEX_PAGE);
            if (page == null) {
                ctx.status(HttpStatus.NOT_FOUND).json(new ErrorRes("Index page not found"));
                return;
            }

            ctx.contentType(CONTENT_TYPE);
            ctx.result(page);
        });
    }
}
