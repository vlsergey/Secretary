package org.wikipedia.vlsergey.secretary.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;

public class Template extends AbstractContainer {

	private Content name;

	private ArticleFragment parameters;

	public Template(final Content templateName, ArticleFragment parameters) {
		this.name = templateName;
		this.parameters = parameters;
	}

	public boolean format(boolean multiline, boolean spaceBeforeLine) {

		if (!(name instanceof Text))
			return false;

		Parameter lastOne = null;

		int maxParameterNameLength = -1;
		for (Content content : parameters.getChildren()) {
			Parameter parameter = (Parameter) content;
			lastOne = parameter;

			if (parameter.getName() == null)
				return false;

			if (!(parameter.getName() instanceof Text))
				return false;

			if (!(parameter.getValue() instanceof Text))
				return false;

			maxParameterNameLength = java.lang.Math.max(maxParameterNameLength,
					parameter.getName().toString().trim().length());
		}

		name = new Text(name.toString().trim() + (multiline ? "\n" : "")
				+ (spaceBeforeLine ? " " : ""));

		for (Content content : parameters.getChildren()) {
			Parameter parameter = (Parameter) content;

			if (multiline) {
				parameter.setName(new Text(" "
						+ StringUtils.rightPad(parameter.getName().toString()
								.trim(), maxParameterNameLength) + " "));

				parameter
						.setValue(new Text(
								" "
										+ parameter.getValue().toString()
												.trim()
										+ "\n"
										+ (spaceBeforeLine
												&& (parameter != lastOne) ? " "
												: "")));

			} else {
				parameter.setName(new Text(parameter.getName().toString()
						.trim()));

				parameter
						.setValue(new Text(
								parameter.getValue().toString().trim()
										+ (spaceBeforeLine
												&& (parameter != lastOne) ? " "
												: "")));
			}

		}

		return true;
	}

	public String getCanonicalName() {
		StringBuilder stringBuilder = new StringBuilder();
		List<Content> children = name instanceof ArticleFragment ? ((ArticleFragment) name)
				.getChildren() : Collections.singletonList(name);
		for (Content content : children) {
			if (content instanceof Comment) {
				// ignore
			} else {
				stringBuilder.append(content.toWiki());
			}
		}
		return stringBuilder.toString().trim().toLowerCase();
	}

	@Override
	public List<Content> getChildren() {
		List<Content> result = new ArrayList<Content>();

		addToChildren(result, name);
		addToChildren(result, parameters);

		return result;
	}

	public Content getName() {
		return name;
	}

	public Parameter getParameter(int index) {
		return (Parameter) parameters.getChildren().get(index);
	}

	public Parameter getParameter(String name) {
		for (Content content : parameters.getChildren()) {
			Parameter parameter = (Parameter) content;
			if (parameter.getName() != null
					&& StringUtils.equalsIgnoreCase(name, parameter
							.getCanonicalName().trim()))
				return parameter;
		}
		return null;
	}

	public ArticleFragment getParameters() {
		return parameters;
	}

	public void removeParameter(String name) {
		final List<Content> children = parameters.getChildren();
		for (Content content : new ArrayList<Content>(children)) {
			Parameter parameter = (Parameter) content;
			if (parameter.getName() != null
					&& StringUtils.equalsIgnoreCase(name, parameter
							.getCanonicalName().trim())) {
				children.remove(parameter);
			}
		}
	}

	public void setName(Content name) {
		this.name = name;
	}

	public void setParameters(ArticleFragment parameters) {
		this.parameters = parameters;
	}

	@Override
	public String toWiki() {
		StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append("{{");
		stringBuilder.append(super.toWiki());
		stringBuilder.append("}}");

		return stringBuilder.toString();
	}

}
