package library;

public final class LibraryException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public LibraryException(String message, Throwable cause) {
        super(message, cause);
    }
}
