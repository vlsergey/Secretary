package org.wikipedia.vlsergey.secretary.webcite;

import java.util.Locale;

import org.wikipedia.vlsergey.secretary.dom.Content;
import org.wikipedia.vlsergey.secretary.dom.Template;

enum WikiConstants {

	RU {
		@Override
		String[] accessDate() {
			return new String[] { "accessdate" };
		}

		@Override
		String[] archiveDate() {
			return new String[] { "archivedate" };
		}

		@Override
		String[] archiveUrl() {
			return new String[] { "archiveurl" };
		}

		@Override
		String[] author() {
			return new String[] { "author" };
		}

		@Override
		String[] date() {
			return new String[] { "date" };
		}

		@Override
		String[] deadlink() {
			return new String[] { "deadlink" };
		}

		@Override
		String[] title() {
			return new String[] { "title" };
		}

		@Override
		String[] url() {
			return new String[] { "url" };
		}
	},

	UK {
		@Override
		String[] accessDate() {
			return new String[] { "accessdate", "дата-доступу" };
		}

		@Override
		String[] archiveDate() {
			return new String[] { "archivedate", "дата-архіву" };
		}

		@Override
		String[] archiveUrl() {
			return new String[] { "archiveurl", "url-архіву" };
		}

		@Override
		String[] author() {
			return new String[] {};
		}

		@Override
		String[] date() {
			return new String[] { "date", "дата" };
		}

		@Override
		String[] deadlink() {
			return new String[] { "deadurl", "мертвий-url" };
		}

		@Override
		String[] title() {
			return new String[] { "title", "назва" };
		}

		@Override
		String[] url() {
			return new String[] { "url" };
		}

	};

	static final String TEMPLATE_CITE_WEB = "cite web";

	static WikiConstants get(Locale locale) {
		if ("ru".equals(locale.getLanguage())) {
			return RU;
		} else if ("uk".equals(locale.getLanguage())) {
			return UK;
		} else {
			throw new UnsupportedOperationException("Language '" + locale.getLanguage() + "' not supported");
		}
	}

	static Content getParameterValue(Template template, String[] parameterNames) {
		for (String possibleParameterName : parameterNames) {
			Content value = template.getParameterValue(possibleParameterName);
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	static void removeParameter(Template template, String[] parameterNames) {
		for (String possibleParameterName : parameterNames) {
			template.removeParameter(possibleParameterName);
		}
	}

	abstract String[] accessDate();

	abstract String[] archiveDate();

	abstract String[] archiveUrl();

	Content archiveUrl(Template template) {
		return getParameterValue(template, archiveUrl());
	}

	abstract String[] author();

	abstract String[] date();

	abstract String[] deadlink();

	Content deadlink(Template template) {
		return getParameterValue(template, deadlink());
	}

	String template() {
		return "cite web";
	}

	abstract String[] title();

	abstract String[] url();

	Content url(Template template) {
		return getParameterValue(template, url());
	}

}
