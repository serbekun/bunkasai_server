package com.serbekun.bunkasai.http.handles.statics;

import java.util.Locale;
import java.util.function.Function;

import com.serbekun.bunkasai.domain.http.dto.ErrorRes;
import com.serbekun.bunkasai.service.resource.ResourcesService;

import io.javalin.http.Context;
import io.javalin.http.HttpStatus;

/**
 * Serves static resources of the {@code /static/v0/*} family.
 * <p>
 * Every supported resource kind is described by {@link StaticResource}:
 * how to read a single file, how to list the whole directory and which
 * content type to answer with. The handler itself only decides between
 * "listing" and "single file" and maps a missing resource to 404.
 * </p>
 */
public class StaticV0Http {

    /** Content type used for every directory listing response. */
    private static final String LIST_CONTENT_TYPE = "application/json";

    private final ResourcesService resourcesService;

    public StaticV0Http(ResourcesService resourcesService) {
        this.resourcesService = resourcesService;
    }

    /** Serves the resource named by the {@code name} path parameter. */
    public void serve(Context ctx, StaticResource resource) {
        serve(ctx, ctx.pathParam("name"), resource);
    }

    /**
     * Serves a single resource, or the directory listing when {@code name} is
     * null/empty.
     *
     * @param ctx      the request context
     * @param name     the resource file name; null or empty means "list the directory"
     * @param resource the resource kind being served
     */
    public void serve(Context ctx, String name, StaticResource resource) {
        if (name == null || name.isEmpty()) {
            serveListing(ctx, resource);
            return;
        }

        try {
            if (resource.binary) {
                serveBinary(ctx, name, resource);
            } else {
                serveText(ctx, name, resource);
            }
        } catch (IllegalArgumentException e) {
            // Thrown by ResourcesBasePath.resolve on path traversal attempts.
            ctx.status(HttpStatus.BAD_REQUEST).json(new ErrorRes("Invalid resource name"));
        }
    }

    private void serveListing(Context ctx, StaticResource resource) {
        String files = resource.list(resourcesService);
        if (files == null) {
            ctx.status(HttpStatus.NOT_FOUND).json(new ErrorRes("Resource listing not available"));
            return;
        }

        ctx.contentType(LIST_CONTENT_TYPE);
        ctx.result(files);
    }

    private void serveBinary(Context ctx, String name, StaticResource resource) {
        byte[] data = resource.binaryReader.read(resourcesService, name);
        if (data == null) {
            ctx.status(HttpStatus.NOT_FOUND).json(new ErrorRes("Resource not found"));
            return;
        }

        ctx.contentType(resource.contentType(resourcesService, name));
        ctx.result(data);
    }

    private void serveText(Context ctx, String name, StaticResource resource) {
        String data = resource.textReader.read(resourcesService, name);
        if (data == null) {
            ctx.status(HttpStatus.NOT_FOUND).json(new ErrorRes("Resource not found"));
            return;
        }

        ctx.contentType(resource.contentType(resourcesService, name));
        ctx.result(data);
    }

    /**
     * Describes one kind of static resource served under {@code /static/v0/}.
     * The enum constant name (lower-cased) is also the URL segment.
     */
    public enum StaticResource {

        CSS(false, "text/css; charset=utf-8",
            ResourcesService::getCss, null, s -> s.getCss("")),
            
        HTML(false, "text/html; charset=utf-8",
            ResourcesService::getHtml, null, s -> s.getHtml("")),

        IMAGES(true, null,
            null, ResourcesService::getImage, ResourcesService::listImagesAsJson),

        JS(false, "application/javascript; charset=utf-8",
            ResourcesService::getJs, null, s -> s.getJs("")),

        JSON(false, "application/json; charset=utf-8",
            ResourcesService::getJson, null, s -> s.getJson("")),

        PDF(true, "application/pdf",
            null, ResourcesService::getPdf, ResourcesService::listPdfsAsJson),

        SVG(false, "image/svg+xml; charset=utf-8",
            ResourcesService::getSvg, null, s -> s.getSvg(""));

        private final boolean binary;
        private final String contentType;
        private final TextReader textReader;
        private final BinaryReader binaryReader;
        private final Function<ResourcesService, String> listReader;

        StaticResource(boolean binary,
                       String contentType,
                       TextReader textReader,
                       BinaryReader binaryReader,
                       Function<ResourcesService, String> listReader) {
            this.binary = binary;
            this.contentType = contentType;
            this.textReader = textReader;
            this.binaryReader = binaryReader;
            this.listReader = listReader;
        }

        /** The URL segment this resource is served under, e.g. {@code images}. */
        public String urlSegment() {
            return name().toLowerCase(Locale.ROOT);
        }

        /**
         * Returns the content type for a concrete file. Resource kinds with a
         * fixed type (CSS, HTML, ...) return it as is; kinds holding files of
         * mixed types (images) fall back to extension-based detection.
         */
        private String contentType(ResourcesService resourcesService, String name) {
            if (contentType != null) {
                return contentType;
            }
            return resourcesService.detectMimeType(name);
        }

        private String list(ResourcesService resourcesService) {
            return listReader.apply(resourcesService);
        }
    }

    @FunctionalInterface
    private interface TextReader {
        String read(ResourcesService resourcesService, String name);
    }

    @FunctionalInterface
    private interface BinaryReader {
        byte[] read(ResourcesService resourcesService, String name);
    }
}
