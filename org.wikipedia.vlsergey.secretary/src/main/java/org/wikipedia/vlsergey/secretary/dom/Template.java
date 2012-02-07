package org.wikipedia.vlsergey.secretary.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.wikipedia.vlsergey.secretary.utils.StringUtils;

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

	public ArticleFragment getParameters() {
		return parameters;
	}

	public List<Parameter> getParameters(String name) {
		List<Parameter> result = new LinkedList<Parameter>();

		for (Content content : parameters.getChildren()) {
			Parameter parameter = (Parameter) content;
			if (parameter.getName() != null
					&& StringUtils.equalsIgnoreCase(name, parameter
							.getCanonicalName().trim())) {
				result.add(parameter);
			}
		}

		return result;
	}

	public Content getParameterValue(String name) {
		final List<Parameter> matchedParameters = getParameters(name);
		for (Parameter parameter : matchedParameters) {
			if (StringUtils.trimToNull(parameter.getValue().toWiki()) != null) {
				return parameter.getValue();
			}
		}
		if (!matchedParameters.isEmpty())
			return matchedParameters.get(0).getValue();

		return null;
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

	public void setParameterValue(String parameterName, Content value) {
		final List<Parameter> matchedParameters = getParameters(parameterName);

		if (matchedParameters.isEmpty()) {
			parameters.getChildren().add(
					new Parameter(new Text(parameterName), value));
			return;
		}

		if (matchedParameters.size() == 1) {
			Parameter parameter = matchedParameters.get(0);
			parameter.setValue(value);
			return;
		}

		Parameter first = matchedParameters.get(0);
		first.setValue(value);
		while (getParameters(parameterName).size() > 1)
			parameters.getChildren()
					.remove(getParameters(parameterName).get(1));
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
