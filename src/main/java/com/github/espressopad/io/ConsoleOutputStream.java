package com.github.espressopad.io;

import com.github.espressopad.views.components.MessageConsole;

import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/*
 *	Class to intercept output from a PrintStream and add it to a Document.
 *  The output can optionally be redirected to a different PrintStream.
 *  The text displayed in the Document can be color coded to indicate
 *  the output source.
 */
public class ConsoleOutputStream extends ByteArrayOutputStream {
    private final String EOL = System.lineSeparator();
    private SimpleAttributeSet attributes;
    private final PrintStream printStream;
    private final StringBuffer buffer = new StringBuffer(80);
    private boolean isFirstLine;
    private final MessageConsole messageConsole;

    /*
     *  Specify the option text color and PrintStream
     */
    public ConsoleOutputStream(MessageConsole messageConsole, Color textColor, PrintStream printStream) {
        this.messageConsole = messageConsole;
        if (textColor != null) {
            this.attributes = new SimpleAttributeSet();
            StyleConstants.setForeground(this.attributes, textColor);
        }

        this.printStream = printStream;

        if (this.messageConsole.isAppend())
            this.isFirstLine = true;
    }

    /*
     *  Override this method to intercept the output text. Each line of text
     *  output will actually involve invoking this method twice:
     *
     *  a) for the actual text message
     *  b) for the newLine string
     *
     *  The message will be treated differently depending on whether the line
     *  will be appended or inserted into the Document
     */
    @Override
    public void flush() {
        String message = this.toString();

        if (message.isEmpty()) return;

        if (this.messageConsole.isAppend())
            this.handleAppend(message);
        else
            this.handleInsert(message);

        this.reset();
    }

    /*
     *	We don't want to have blank lines in the Document. The first line
     *  added will simply be the message. For additional lines it will be:
     *
     *  newLine + message
     */
    private void handleAppend(String message) {
        //  This check is needed in case the text in the Document has been
        //	cleared. The buffer may contain the EOL string from the previous
        //  message.

        if (this.messageConsole.getDocument().getLength() == 0)
            this.buffer.setLength(0);

        if (this.EOL.equals(message))
            this.buffer.append(message);
        else {
            this.buffer.append(message);
            this.clearBuffer();
        }
    }

    /*
     *  We don't want to merge the new message with the existing message
     *  so the line will be inserted as:
     *
     *  message + newLine
     */
    private void handleInsert(String message) {
        this.buffer.append(message);

        if (this.EOL.equals(message))
            this.clearBuffer();
    }

    /*
     *  The message and the newLine have been added to the buffer in the
     *  appropriate order so we can now update the Document and send the
     *  text to the optional PrintStream.
     */
    private void clearBuffer() {
        //  In case both the standard out and standard err are being redirected
        //  we need to insert a newline character for the first line only

        if (this.isFirstLine && this.messageConsole.getDocument().getLength() != 0)
            this.buffer.insert(0, "\n");

        this.isFirstLine = false;
        String line = this.buffer.toString();

        try {
            if (this.messageConsole.isAppend()) {
                int offset = this.messageConsole.getDocument().getLength();
                this.messageConsole.getDocument().insertString(offset, line, this.attributes);
                this.messageConsole.getTextComponent().setCaretPosition(this.messageConsole.getDocument().getLength());
            } else {
                this.messageConsole.getDocument().insertString(0, line, this.attributes);
                this.messageConsole.getTextComponent().setCaretPosition(0);
            }
        } catch (BadLocationException e) {
        }

        if (this.printStream != null)
            this.printStream.print(line);

        this.buffer.setLength(0);
    }
}
