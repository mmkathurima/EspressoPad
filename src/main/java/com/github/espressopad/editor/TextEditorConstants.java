package com.github.espressopad.editor;

import java.util.regex.Pattern;

public class TextEditorConstants {
    private static final String[] properties = {
            "java.home", "java.vendor", "java.version", "sun.desktop", "os.name", "os.version",
            "os.arch", "user.name", "user.dir", "user.home", "user.language", "sun.cpu.isalist",
            "sun.arch.data.model", "java.io.tmpdir", "sun.jnu.encoding", "sun.boot.library.path",
            "java.class.version"
    };
    protected static final String sampleCode = "IntStream stream = IntStream.rangeClosed(0, 10)\n" +
            "stream.forEach(x -> System.out.println(x));";
    protected static final String[] KEYWORDS = {
            "abstract", "assert", "boolean", "break", "byte",
            "case", "catch", "char", "class", "const",
            "continue", "default", "do", "double", "else",
            "enum", "extends", "false", "final", "finally",
            "float", "for", "goto", "if", "implements", "import",
            "instanceof", "int", "interface", "long", "native",
            "new", "null", "package", "private", "protected",
            "public", "return", "short", "static", "strictfp",
            "super", "switch", "synchronized", "this", "throw",
            "throws", "transient", "true", "try", "var", "void",
            "volatile", "while"
    };
    private static final String KEYWORD_PATTERN = String.format("\\b(%s)\\b", String.join("|", KEYWORDS));
    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String BRACE_PATTERN = "\\{|\\}";
    private static final String BRACKET_PATTERN = "\\[|\\]";
    private static final String SEMICOLON_PATTERN = ";";
    private static final String STRING_PATTERN = "(\"([^\"\\\\]|\\\\.)*\"|'.?')";
    private static final String NUMBER_PATTERN = "\\b([-+]?((0[xX])(\\d*[a-fA-F0-9_]+\\d*))|((0[bB])[01_]+)|" +
            "([0-9]*\\.?_*[eE]?[0-9]+[dDfFLl]?))\\b";
    private static final String COMMENT_PATTERN = "//[^\n]*|/\\*(.|\\R)*?\\*/";
    protected static final Pattern PATTERN = Pattern.compile(
            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<PAREN>" + PAREN_PATTERN + ")"
                    + "|(?<BRACE>" + BRACE_PATTERN + ")"
                    + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
                    + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<NUMBER>" + NUMBER_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
    );
}

