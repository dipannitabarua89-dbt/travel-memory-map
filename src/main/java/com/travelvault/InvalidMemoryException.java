package com.travelvault;

public class InvalidMemoryException extends RuntimeException {
    public InvalidMemoryException(String message) {
        super(message);
    }
}