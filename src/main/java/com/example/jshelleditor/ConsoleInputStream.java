package com.example.jshelleditor;

import javax.swing.JOptionPane;
import java.io.IOException;
import java.io.InputStream;

public class ConsoleInputStream extends InputStream {
    private StringBuilder buffer = new StringBuilder();

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
