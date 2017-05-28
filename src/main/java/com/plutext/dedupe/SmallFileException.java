package com.plutext.dedupe;

public class SmallFileException  extends Exception {
	
    public SmallFileException(String message) {
        super(message);
    }
    public SmallFileException(String message, Throwable throwable) {
        super(message, throwable);
    }

}