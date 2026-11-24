package com.harvesta.web;

/** Thrown from a controller to answer with an HTTP error page (403, 404, ...). */
public final class HttpError extends RuntimeException {

    public final int status;

    public HttpError(int status) {
        super("HTTP " + status, null, false, false);
        this.status = status;
    }

    public static HttpError notFound() {
        return new HttpError(404);
    }

    public static HttpError forbidden() {
        return new HttpError(403);
    }
}
