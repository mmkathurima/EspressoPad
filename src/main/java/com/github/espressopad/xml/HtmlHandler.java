package com.github.espressopad.xml;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HtmlHandler {

    public static String convertJavaDoc(String input) {
        // Define regular expressions for JavaDoc tags
        // You can add more patterns for other JavaDoc tags as needed
        /*input = input.replaceAll("&", "&amp")
                .replaceAll("<", "&lt;")
                .replaceAll(">", "&gt;")
                .replaceAll("'", "&#39;")
                .replaceAll("\"", "&quot;");*/

        Pattern pattern = Pattern.compile("@author\\s+(.*?)");
        Matcher matcher = pattern.matcher(input);
        input = matcher.replaceAll("\n\t\t<div><b>Author: </b>$1</div>");

        pattern = Pattern.compile("@apiNote\\s+(.*?)");
        matcher = pattern.matcher(input);
        input = matcher.replaceAll("\n\t\t<div><b>API Note: </b>$1</div>");

        pattern = Pattern.compile("\\{\\@code\\s+(.*?)\\}");
        // Replace {@code ...} with <code>...</code>
        matcher = pattern.matcher(input);
        input = matcher.replaceAll("<code>$1</code>");

        pattern = Pattern.compile("<pre>\\{@code\\s*([^}]*)\\s*}</pre>");
        // Replace {@code ...} with <code>...</code>
        matcher = pattern.matcher(input);
        input = matcher.replaceAll("<pre><code>$1</code></pre>");

        pattern = Pattern.compile("@deprecated\\s+(.*?)");
        matcher = pattern.matcher(input);
        input = matcher.replaceAll("\n\t\t<div><b>Deprecated: </b>$1</div>");

        pattern = Pattern.compile("\\{*\\@(exception|throws)\\s+(.*?)\\}*");
        matcher = pattern.matcher(input);
        while (matcher.find())
            input = matcher.replaceAll("\n\t\t<div><b>Exception:</b> $2</div>");

        pattern = Pattern.compile("<div><b>Exception:</b>\\s*</div>\\b(\\w+)\\b");
        matcher = pattern.matcher(input);
        input = matcher.replaceAll("<div><b>Exception:</b>&nbsp;<span class='red'>$1</span>");

        pattern = Pattern.compile("@implNote\\s+(.*?)");
        matcher = pattern.matcher(input);
        input = matcher.replaceAll("\n\t\t<div><b>Implementation Note: </b>$1</div>");

        pattern = Pattern.compile("@implSpec\\s+(.*?)");
        matcher = pattern.matcher(input);
        input = matcher.replaceAll("\n\t\t<div><b>Implementation Spec: </b>$1</div>");

        pattern = Pattern.compile("@jls\\s+(.*?)");
        matcher = pattern.matcher(input);
        input = matcher.replaceAll("\n\t\t<div><b>jls: </b>$1</div>");

        // Replace {@link ...} with <a href="...">...</a>
        pattern = Pattern.compile("\\{\\@(link|linkplain)\\s+(.*?)\\}");
        matcher = pattern.matcher(input);
        input = matcher.replaceAll("<a href=\"$2\">$2</a>");

        pattern = Pattern.compile("\\{*@param\\s+(.*?)\\}*");
        matcher = pattern.matcher(input);
        while (matcher.find())
            input = matcher.replaceAll("\n\t\t<div><b>Parameter:</b>&nbsp;$1</div>");

        //"\n\t\t<div><b>Params:</b>&nbsp;$1</div>"
        pattern = Pattern.compile("<b>Parameter:</b>&nbsp;</div>((\\b(\\w+)\\b)|(&lt;\\w+&gt;))");
        matcher = pattern.matcher(input);
        input = matcher.replaceAll("<b>Parameter:</b>&nbsp;</div><span class='red'>$1</span>&nbsp;-");

        pattern = Pattern.compile("\\{*\\@return\\s+(.*?)\\}*");
        matcher = pattern.matcher(input);
        input = matcher.replaceAll("\n\t\t<div><b>Returns:</b> $1</div>");

        pattern = Pattern.compile("\\{*\\@see\\s+(.*?)\\}*");
        matcher = pattern.matcher(input);
        input = matcher.replaceAll("\n\t\t<div><b>See Also:</b> $1</div>");

        pattern = Pattern.compile("\\{*\\@serial\\s+(.*?)\\}*");
        matcher = pattern.matcher(input);
        input = matcher.replaceAll("\n\t\t<div><b>Serial:</b> $1</div>");

        pattern = Pattern.compile("\\{*\\@serialData\\s+(.*?)\\}*");
        matcher = pattern.matcher(input);
        input = matcher.replaceAll("\n\t\t<div><b>Serial Data:</b> $1</div>");

        pattern = Pattern.compile("\\{*\\@serialField\\s+(.*?)\\}*");
        matcher = pattern.matcher(input);
        input = matcher.replaceAll("\n\t\t<div><b>Serial Field:</b> $1</div>");

        pattern = Pattern.compile("\\{*\\@since\\s+(.*?)\\}*");
        matcher = pattern.matcher(input);
        input = matcher.replaceAll("\n\t\t<div><b>Since:</b> $1</div>");

        pattern = Pattern.compile("@spec\\s+(.*?)");
        matcher = pattern.matcher(input);
        input = matcher.replaceAll("\n\t\t<div><b>Spec: </b>$1</div>");

        pattern = Pattern.compile("\\{\\@value\\s+(.*?)\\}");
        matcher = pattern.matcher(input);
        input = matcher.replaceAll("\n\t\t<div><pre><code>$1</code></pre></div>");

        pattern = Pattern.compile("\\{*\\@version\\s+(.*?)\\}*");
        matcher = pattern.matcher(input);
        input = matcher.replaceAll("\n\t\t<div><b>Version:</b> $1</div>");

        return String.format("%s\n\t\t<style>body { font-family: sans-serif; font-size: 11pt; padding-right: 20px; }" +
                " .red { color: indianred; } </style>", input);
    }
}
