package co.elastic.thumbnails4j.core;

public class ThumbnailingException extends Exception {

    public ThumbnailingException(Exception e) {
        super(e);
    }

    public ThumbnailingException(String msg) {
        super(msg);
    }
}
