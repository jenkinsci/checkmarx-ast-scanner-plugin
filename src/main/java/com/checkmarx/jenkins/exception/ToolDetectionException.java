package com.checkmarx.jenkins.exception;

import java.io.IOException;

/**
 * Exception thrown when there is an issue detecting tools in the Jenkins environment.
 * This exception is used to provide more specific error information for tool detection failures.
 */
public class ToolDetectionException extends IOException {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * Constructs a new ToolDetectionException with the specified cause.
     *
     * @param cause the cause of this exception
     */
    public ToolDetectionException(Throwable cause) {
        super(cause);
    }
    
    /**
     * Constructs a new ToolDetectionException with the specified detail message.
     *
     * @param message the detail message
     */
    public ToolDetectionException(String message) {
        super(message);
    }
    
    /**
     * Constructs a new ToolDetectionException with the specified detail message and cause.
     *
     * @param message the detail message
     * @param cause the cause of this exception
     */
    public ToolDetectionException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * Returns a string representation of this exception including its cause if available.
     *
     * @return a string representation of this exception
     */
    @Override
    public String toString() {
        if (getCause() != null) {
            return super.toString() + " [Caused by: " + getCause() + "]";
        }
        return super.toString();
    }
}
