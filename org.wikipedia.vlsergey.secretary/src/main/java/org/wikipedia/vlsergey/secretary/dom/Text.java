package org.wikipedia.vlsergey.secretary.dom;

/**
 * 
 * 
 * @author SEVL0904
 */
public class Text extends Content {
    private String text;

    public Text(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toWiki() {
        return text;
    }

}
