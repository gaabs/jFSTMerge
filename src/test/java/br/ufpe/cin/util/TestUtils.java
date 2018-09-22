package br.ufpe.cin.util;

import java.io.OutputStream;
import java.io.PrintStream;

public class TestUtils {
    public static void hideSystemOutput() {
        PrintStream hideStream = new PrintStream(new OutputStream() {
            public void write(int b) {
            }
        });
        System.setOut(hideStream);
    }
}
