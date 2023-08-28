package com.github.espressopad.editor;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public class BracketHighlighter {
    // constants
    private static final List<String> CLEAR_STYLE = Collections.singletonList("unmatchBracket");
    private static final List<String> MATCH_STYLE = Collections.singletonList("matchBracket");
    private static final String BRACKET_PAIRS = "(){}[]<>";
    // the code area
    private final CustomCodeArea codeArea;
    // the list of highlighted bracket pairs
    private final List<BracketPair> bracketPairs;

    /**
     * Parameterized constructor
     *
     * @param codeArea the code area
     */
    public BracketHighlighter(CustomCodeArea codeArea) {
        this.codeArea = codeArea;
        this.bracketPairs = new ArrayList<>();
        // listen for changes in text or caret position
        this.codeArea.addTextInsertionListener(new TextInsertionListener() {
            @Override
            public void codeInserted(int start, int end, String text) {
                BracketHighlighter.this.clearBracket();
            }
        });
        this.codeArea.caretPositionProperty().addListener(new ChangeListener<Integer>() {
            @Override
            public void changed(ObservableValue<? extends Integer> obs, Integer oldVal, Integer newVal) {
                Platform.runLater(new Runnable() {
                    @Override
                    public void run() {
                        BracketHighlighter.this.highlightBracket(newVal);
                    }
                });
            }
        });
    }

    /**
     * Highlight the matching bracket at current caret position
     */
    public void highlightBracket() {
        this.highlightBracket(codeArea.getCaretPosition());
    }

    /**
     * Highlight the matching bracket at new caret position
     *
     * @param newVal the new caret position
     */
    private void highlightBracket(int newVal) {
        // first clear existing bracket highlights
        this.clearBracket();

        // detect caret position both before and after bracket
        String prevChar;
        if (newVal > 0 && newVal <= codeArea.getLength())
            prevChar = codeArea.getText(newVal - 1, newVal);
        else prevChar = "";
        if (BRACKET_PAIRS.contains(prevChar)) newVal--;

        // get other half of matching bracket
        Integer other = this.getMatchingBracket(newVal);

        if (other != null) {
            // other half exists
            BracketPair pair = new BracketPair(newVal, other);
            // highlight pair
            this.styleBrackets(pair, MATCH_STYLE);
            // add bracket pair to list
            this.bracketPairs.add(pair);
        }
    }

    /**
     * Find the matching bracket location
     *
     * @param index to start searching from
     * @return null or position of matching bracket
     */
    private Integer getMatchingBracket(int index) {
        if (index < 0 || index >= this.codeArea.getLength()) return null;

        char initialBracket = this.codeArea.getText(index, index + 1).charAt(0);
        int bracketTypePosition = BRACKET_PAIRS.indexOf(initialBracket); // "(){}[]<>"
        if (bracketTypePosition < 0) return null;

        // even numbered bracketTypePositions are opening brackets, and odd positions are closing
        // if even (opening bracket) then step forwards, otherwise step backwards
        int stepDirection = (bracketTypePosition % 2 == 0) ? 1 : -1;

        // the matching bracket to look for, the opposite of initialBracket
        char match = BRACKET_PAIRS.charAt(bracketTypePosition + stepDirection);

        index += stepDirection;
        int bracketCount = 1;

        while (index > -1 && index < codeArea.getLength()) {
            char code = this.codeArea.getText(index, index + 1).charAt(0);
            if (code == initialBracket) bracketCount++;
            else if (code == match) bracketCount--;
            if (bracketCount == 0) return index;
            else index += stepDirection;
        }

        return null;
    }

    /**
     * Clear the existing highlighted bracket styles
     */
    public void clearBracket() {
        // get iterator of bracket pairs
        Iterator<BracketPair> iterator = this.bracketPairs.iterator();

        // loop through bracket pairs and clear all
        while (iterator.hasNext()) {
            // get next bracket pair
            BracketPair pair = iterator.next();
            // clear pair
            this.styleBrackets(pair, CLEAR_STYLE);
            // remove bracket pair from list
            iterator.remove();
        }
    }

    /**
     * Set a list of styles to a pair of brackets
     *
     * @param pair   pair of brackets
     * @param styles the style list to set
     */
    private void styleBrackets(BracketPair pair, List<String> styles) {
        this.styleBracket(pair.start, styles);
        this.styleBracket(pair.end, styles);
    }

    /**
     * Set a list of styles for a position
     *
     * @param pos    the position
     * @param styles the style list to set
     */
    private void styleBracket(int pos, List<String> styles) {
        if (pos < this.codeArea.getLength()) {
            String text = this.codeArea.getText(pos, pos + 1);
            if (BRACKET_PAIRS.contains(text))
                this.codeArea.setStyle(pos, pos + 1, styles);
        }
    }

    /**
     * Class representing a pair of matching bracket indices
     */
    static class BracketPair {
        private final int start;
        private final int end;

        public BracketPair(int start, int end) {
            this.start = start;
            this.end = end;
        }

        public int getStart() {
            return start;
        }

        public int getEnd() {
            return end;
        }

        @Override
        public String toString() {
            return "BracketPair{" + "start=" + start + ", end=" + end + '}';
        }
    }
}