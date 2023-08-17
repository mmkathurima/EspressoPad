package com.example.jshelleditor.streams;

import javax.swing.JOptionPane;
import java.io.IOException;
import java.io.InputStream;

public class ConsoleInputStream extends InputStream {
    private final StringBuilder buffer = new StringBuilder();

    @Override
    public int read() throws IOException {
        if (this.buffer.length() == 0) {
            this.buffer.append(JOptionPane.showInputDialog(null, "Enter your input:"));
            this.buffer.append("\n");
        }

        char charToRead = this.buffer.charAt(0);
        this.buffer.deleteCharAt(0);
        System.out.print(charToRead);
        return charToRead;
    }
}
