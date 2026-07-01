package dev.alexissdev.kronos.koth.exception;

import dev.alexissdev.kronos.common.exception.HCFException;

public class KothNotFoundException extends HCFException {

    public KothNotFoundException(String name) {
        super("KOTH not found: " + name);
    }
}
