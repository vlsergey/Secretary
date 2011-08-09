package org.wikipedia.vlsergey.secretary.dom;

public class NoWiki extends Content {
    private final String escapedText;

    public NoWiki(String escapedText) {
        this.escapedText = escapedText;
    }

    @Override
    public String toWiki() {
        return "<nowiki>" + escapedText + "</nowiki>";
    }

}
