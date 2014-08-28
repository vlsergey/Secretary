package org.wikipedia.vlsergey.secretary.dom;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.wikipedia.vlsergey.secretary.utils.StringUtils;

public class Template extends AbstractContainer {

	private static final long serialVersionUID = 1L;

	private List<TemplatePart> parts;

	private Content title;

	public Template(final Content templateName) {
		this.title = templateName;
		this.parts = new ArrayList<TemplatePart>(0);
	}

	public Template(final Content templateName, List<TemplatePart> parts) {
		this.title = templateName;
		this.parts = new ArrayList<TemplatePart>(parts);
	}

	public boolean format(boolean multiline, boolean spaceBeforeLine) {

		if (!(title instanceof Text))
			return false;

		TemplatePart lastOne = null;

		int maxParameterNameLength = -1;
		for (TemplatePart part : parts) {
			lastOne = part;

			if (part.getName() == null)
				return false;

			if (!(part.getName() instanceof Text))
				return false;

			if (!(part.getValue() instanceof Text))
				return false;

			maxParameterNameLength = java.lang.Math.max(maxParameterNameLength, part.getName().toString().trim()
					.length());
		}

		title = new Text(title.toString().trim() + (multiline ? "\n" : "") + (spaceBeforeLine ? " " : ""));

		for (TemplatePart parameter : parts) {
			if (multiline) {
				parameter.setName(new Text(" "
						+ StringUtils.rightPad(parameter.getName().toString().trim(), maxParameterNameLength) + " "));

				parameter.setValue(new Text(" " + parameter.getValue().toString().trim() + "\n"
						+ (spaceBeforeLine && (parameter != lastOne) ? " " : "")));

			} else {
				parameter.setName(new Text(parameter.getName().toString().trim()));

				parameter.setValue(new Text(parameter.getValue().toString().trim()
						+ (spaceBeforeLine && (parameter != lastOne) ? " " : "")));
			}
		}

		return true;
	}

	public String getCanonicalName() {
		if (title == null) {
			return null;
		}
		return title.toWiki(true).trim().toLowerCase();
	}

	@Override
	public List<Content> getChildren() {
		List<Content> result = new ArrayList<Content>();

		addToChildren(result, title);
		addToChildren(result, parts);

		return result;
	}

	public Content getName() {
		return title;
	}

	public TemplatePart getParameter(int index) {
		return parts.get(index);
	}

	public List<TemplatePart> getParameters() {
		return parts;
	}

	public List<TemplatePart> getParameters(String name) {
		List<TemplatePart> result = new LinkedList<TemplatePart>();

		for (TemplatePart parameter : parts) {
			final Content partName = parameter.getName();
			if (partName != null && StringUtils.equalsIgnoreCase(name, parameter.getCanonicalName().trim())) {
				result.add(parameter);
			}
		}

		return result;
	}

	public Content getParameterValue(String name) {
		final List<TemplatePart> matchedParameters = getParameters(name);
		for (TemplatePart parameter : matchedParameters) {
			final Content value = parameter.getValue();
			if (value != null) {
				if (StringUtils.trimToNull(value.toWiki(true)) != null) {
					return value;
				}
			}
		}
		if (!matchedParameters.isEmpty())
			return matchedParameters.get(0).getValue();

		return null;
	}

	public void removeParameter(String name) {
		final List<TemplatePart> children = parts;
		for (TemplatePart part : new ArrayList<TemplatePart>(parts)) {
			if (part.getName() != null && StringUtils.equalsIgnoreCase(name, part.getCanonicalName().trim())) {
				children.remove(part);
			}
		}
	}

	public void setParameterValue(String parameterName, Content value) {
		final List<TemplatePart> matchedParameters = getParameters(parameterName);

		if (matchedParameters.isEmpty()) {
			parts.add(new TemplatePart(new Text(parameterName), value));
			return;
		}

		if (matchedParameters.size() == 1) {
			TemplatePart part = matchedParameters.get(0);
			part.setValue(value);
			return;
		}

		TemplatePart first = matchedParameters.get(0);
		first.setValue(value);
		while (getParameters(parameterName).size() > 1)
			parts.remove(getParameters(parameterName).get(1));
	}

	public void setParts(List<TemplatePart> parts) {
		this.parts = parts;
	}

	public void setTitle(Content name) {
		this.title = name;
	}

	@Override
	public String toWiki(boolean removeComments) {
		StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append("{{");
		stringBuilder.append(super.toWiki(removeComments));
		stringBuilder.append("}}");

		return stringBuilder.toString();
	}

}
