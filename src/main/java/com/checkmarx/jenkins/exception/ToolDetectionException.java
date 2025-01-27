package com.checkmarx.jenkins.exception;

import java.io.IOException;

public class ToolDetectionException extends IOException {

    public ToolDetectionException(Throwable cause) {
        super(cause);
    }

    public ToolDetectionException(String message) {
        super(message);
    }

    public ToolDetectionException(String message, Throwable innerException) {
        super(message, innerException);
    }
}
