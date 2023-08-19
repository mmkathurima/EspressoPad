package com.example.jshelleditor.streams;

import javax.swing.JDialog;
import javax.swing.JOptionPane;
import java.io.IOException;
import java.io.InputStream;

public class ConsoleInputStream extends InputStream {
    private final StringBuilder buffer = new StringBuilder();

    @Override
    public int read() throws IOException {
        if (this.buffer.length() == 0) {
            JOptionPane optionPane = new JOptionPane("Enter input:", JOptionPane.QUESTION_MESSAGE,
                    JOptionPane.OK_CANCEL_OPTION);
            Object initial = optionPane.getInputValue();
            optionPane.setWantsInput(true);
            JDialog dialog = optionPane.createDialog("Awaiting input");
            dialog.setAlwaysOnTop(true);
            dialog.setVisible(true);
            dialog.requestFocus();
            dialog.dispose();
            Object val = optionPane.getInputValue();
            if (val == initial)
                val = "";
            this.buffer.append(val);
            this.buffer.append("\n");
        }

        char charToRead = this.buffer.charAt(0);
        this.buffer.deleteCharAt(0);
        System.out.print(charToRead);
        return charToRead;
    }
}
