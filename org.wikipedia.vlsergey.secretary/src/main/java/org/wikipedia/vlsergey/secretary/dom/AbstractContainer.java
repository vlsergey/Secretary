package org.wikipedia.vlsergey.secretary.dom;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

public abstract class AbstractContainer extends Content {

    private static final long serialVersionUID = 5245387328136551791L;

    protected static void addToChildren(List<Content> result, Content content) {
        if (content != null) {
            // if (content instanceof ArticleFragment) {
            // result.addAll(((ArticleFragment) content).getChildren());
            // } else {
            result.add(content);
            // }
        }
    }

    protected static void addToChildren(List<Content> result,
            List<Content> content) {
        if (content != null) {
            result.addAll(content);
        }
    }

    protected static String toWiki(List<? extends Content> toOutput) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Content content : toOutput) {
            stringBuilder.append(content.toWiki());
        }
        return stringBuilder.toString();
    }

    public LinkedHashMap<String, List<Template>> getAllTemplates() {
        final LinkedHashMap<String, List<Template>> result = new LinkedHashMap<String, List<Template>>();

        for (Content content : getChildren()) {
            if (content instanceof Template) {
                final Template template = (Template) content;
                final String templateName = template.getCanonicalName();

                if (!result.containsKey(templateName))
                    result.put(templateName, new ArrayList<Template>());

                result.get(templateName).add(template);
            }

            if (content instanceof AbstractContainer) {
                AbstractContainer abstractContainer = (AbstractContainer) content;
                final LinkedHashMap<String, List<Template>> childTemplates = abstractContainer.getAllTemplates();

                for (Entry<String, List<Template>> entry : childTemplates.entrySet()) {
                    final String templateName = entry.getKey();
                    final List<Template> templates = entry.getValue();

                    if (result.containsKey(templateName)) {
                        result.get(templateName).addAll(templates);
                    } else {
                        result.put(templateName, new ArrayList<Template>(
                                templates));
                    }
                }
            }
        }

        return result;
    }

    public List<Text> getAllTexts() {
        final List<Text> result = new LinkedList<Text>();

        for (Content content : getChildren()) {
            if (content instanceof Text) {
                result.add((Text) content);
            }

            if (content instanceof AbstractContainer) {
                AbstractContainer abstractContainer = (AbstractContainer) content;
                result.addAll(abstractContainer.getAllTexts());
            }
        }

        return result;
    }

    public abstract List<? extends Content> getChildren();

    public boolean hasTemplate(String templateName) {
        templateName = templateName.toLowerCase();
        LinkedHashMap<String, List<Template>> allTemplates = getAllTemplates();
        return allTemplates.containsKey(templateName)
                && allTemplates.get(templateName).size() != 0;
    }

    @Override
    public String toWiki() {
        return toWiki(getChildren());
    }
}
