package com.circlegate.liban.base;

public class Exceptions {

    public static class NotImplementedException extends RuntimeException {
        public NotImplementedException() {}

        public NotImplementedException(String message) {
            super(message);
        }

        public NotImplementedException(Throwable cause) {
            super(cause);
        }

        public NotImplementedException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
