package io.github.renatolsjf.chassis.integration;

public class IOErrorOperationException extends OperationException {

    protected IOErrorOperationException() { super(); }
    protected IOErrorOperationException(String message) { super(message); }
    protected IOErrorOperationException(Throwable cause) { super(cause); }
    protected IOErrorOperationException(String message, Throwable cause) { super(message, cause); }

}
