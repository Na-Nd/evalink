package ru.nand.authservice.util.exception;

public class WrongPasswordException extends AuthException{
    public WrongPasswordException(String message) {
        super("Wrong password" + message);
    }
}
