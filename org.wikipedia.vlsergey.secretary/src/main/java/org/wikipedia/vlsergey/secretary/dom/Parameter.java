package org.wikipedia.vlsergey.secretary.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Parameter extends AbstractContainer {
    private Content name;

    private Content value;

    public Parameter(Content parameterName, Content value) {
	this.name = parameterName;
	this.value = value;
    }

    public String getCanonicalName() {
	if (name == null)
	    return null;

	StringBuilder stringBuilder = new StringBuilder();
	List<? extends Content> toStr = name instanceof ArticleFragment ? ((ArticleFragment) name)
		.getChildren()
		: Collections.<Content> singletonList(name);
	for (Content content : toStr) {
	    if (content instanceof Text) {
		stringBuilder.append(content.toWiki());
	    } else if (content instanceof Comment) {
		// ignore
	    } else {
		throw new UnsupportedOperationException(
			"Unsupported content in template name: "
				+ content.getClass().getName());
	    }
	}
	return stringBuilder.toString().trim().toLowerCase();
    }

    @Override
    public List<Content> getChildren() {
	List<Content> result = new ArrayList<Content>();

	addToChildren(result, name);
	addToChildren(result, value);

	return result;
    }

    public Content getName() {
	return name;
    }

    public Content getValue() {
	return value;
    }

    public void setName(Content name) {
	this.name = name;
    }

    public void setValue(Content value) {
	this.value = value;
    }

    @Override
    public String toWiki() {
	if (name == null)
	    return "|" + value.toWiki();

	return "|" + name.toWiki() + "=" + value.toWiki();
    }
}
