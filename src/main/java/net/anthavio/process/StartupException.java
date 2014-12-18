package net.anthavio.process;

public class StartupException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public StartupException(String message, Throwable arg1) {
        super(message, arg1);
    }

    public StartupException(String message) {
        super(message);
    }

    public StartupException(Throwable arg0) {
        super(arg0);
    }

}
