package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.cookie.Cookie;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.StringBody;
import org.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.wikipedia.vlsergey.secretary.jwpf.MediaWikiBot;
import org.wikipedia.vlsergey.secretary.jwpf.actions.AbstractApiXmlAction.ListAdapter;
import org.wikipedia.vlsergey.secretary.jwpf.model.Namespace;
import org.wikipedia.vlsergey.secretary.jwpf.utils.CookieException;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

public abstract class AbstractApiAction implements ContentProcessable {

	public static final int MAXLAG = 0;

	protected static final SimpleDateFormat timestampDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
	static {
		timestampDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	protected static String encode(String string) {
		try {
			String result = URLEncoder.encode(string, StandardCharsets.UTF_8.name());
			return result;
		} catch (UnsupportedEncodingException e) {
			throw new Error("MediaWiki '" + StandardCharsets.UTF_8.name() + "' charset not supported by Java VM");
		}

	}

	protected static synchronized String format(Date date) {
		return timestampDateFormat.format(date);
	}

	protected static String getText(Element element) {
		StringBuilder content = new StringBuilder();
		for (Node child : new ListAdapter<Node>(element.getChildNodes())) {
			if (child instanceof Text) {
				Text text = (Text) child;
				content.append(text.getTextContent());
			}
		}
		return content.toString();
	}

	protected static synchronized Date parseDate(String timestamp) throws ParseException {
		try {
			return timestampDateFormat.parse(timestamp);
		} catch (NumberFormatException exc) {
			NumberFormatException exc2 = new NumberFormatException("Unable to parse '" + timestamp + "': "
					+ exc.getMessage());
			exc2.initCause(exc);
			throw exc2;
		}
	}

	protected static void setParameter(MultipartEntity multipartEntity, String name, Boolean value) {
		if (value != null && value.booleanValue()) {
			setParameter(multipartEntity, name, "1");
		}
	}

	protected static void setParameter(MultipartEntity multipartEntity, String name, Date value) {
		if (value != null) {
			try {
				multipartEntity.addPart(name, new StringBody(format(value), StandardCharsets.UTF_8));
			} catch (UnsupportedEncodingException e) {
				throw new Error("MediaWiki '" + MediaWikiBot.ENCODING + "' charset not supported by Java VM");
			}
		}
	}

	protected static void setParameter(MultipartEntity multipartEntity, String name, Enum<?> value) {
		if (value != null) {
			setParameter(multipartEntity, name, value.toString());
		}
	}

	protected static void setParameter(MultipartEntity multipartEntity, String name, int value) {
		try {
			multipartEntity.addPart(name, new StringBody(Integer.toString(value)));
		} catch (UnsupportedEncodingException e) {
			throw new Error("MediaWiki '" + MediaWikiBot.ENCODING + "' charset not supported by Java VM");
		}
	}

	protected static void setParameter(MultipartEntity multipartEntity, String name, JSONObject value) {
		if (value != null) {
			setParameter(multipartEntity, name, value.toString());
		}
	}

	protected static void setParameter(MultipartEntity multipartEntity, String name, Namespace value) {
		if (value != null) {
			setParameter(multipartEntity, name, value.id);
		}
	}

	protected static void setParameter(MultipartEntity multipartEntity, String name, Namespace[] values) {
		if (values != null && values.length > 0) {
			setParameter(multipartEntity, name, toStringParameters(values));
		}
	}

	protected static void setParameter(MultipartEntity multipartEntity, String name, Number value) {
		if (value != null) {
			setParameter(multipartEntity, name, value.toString());
		}
	}

	protected static void setParameter(MultipartEntity multipartEntity, String name, String value) {
		if (value != null) {
			try {
				multipartEntity.addPart(name, new StringBody(value, StandardCharsets.UTF_8));
			} catch (UnsupportedEncodingException e) {
				throw new Error("MediaWiki '" + StandardCharsets.UTF_8.name() + "' charset not supported by Java VM");
			}
		}
	}

	protected static void setParameter(MultipartEntity multipartEntity, String name, String[] values) {
		if (values != null && values.length > 0) {
			setParameter(multipartEntity, name, toStringParameters(values));
		}
	}

	protected static <T extends Enum<T>> void setParameter(MultipartEntity multipartEntity, String name, T[] values) {
		if (values != null && values.length > 0) {
			setParameter(multipartEntity, name, toStringParameters(values));
		}
	}

	protected static String toLog(String text) {
		if (text == null) {
			return "null";
		}
		return "«" + StringUtils.substring(text, 0, 32).replace("\t", "\\t").replace("\r", "\\r").replace("\n", "\\n")
				+ "…»";
	}

	private static String toStringParameters(Namespace[] namespaces) {
		StringBuilder stringBuilder = new StringBuilder();

		boolean first = true;
		for (Namespace namespace : namespaces) {
			if (!first)
				stringBuilder.append("|");
			stringBuilder.append(namespace.id);

			first = false;
		}

		return stringBuilder.toString();
	}

	protected static String toStringParameters(Object[] parameters) {
		StringBuilder stringBuilder = new StringBuilder();

		boolean first = true;
		for (Object l : parameters) {
			if (!first)
				stringBuilder.append("|");
			stringBuilder.append(l.toString());

			first = false;
		}

		return stringBuilder.toString();
	}

	protected final boolean bot;

	protected final Log log = LogFactory.getLog(getClass());

	protected List<HttpRequestBase> msgs = new ArrayList<HttpRequestBase>(1);

	public AbstractApiAction(boolean bot) {
		this.bot = bot;
	}

	protected void appendParameters(StringBuilder stringBuilder, Iterable<? extends Object> parameters) {
		try {
			String params = toStringParameters(parameters);
			params = URLEncoder.encode(params, StandardCharsets.UTF_8.name());

			stringBuilder.append(params);
		} catch (UnsupportedEncodingException exc) {
			log.error("MediaWiki encoding not supported: '" + StandardCharsets.UTF_8.name() + "': " + exc.getMessage(),
					exc);
			throw new Error(exc.getMessage(), exc);
		}
	}

	@Override
	public boolean followRedirects() {
		return true;
	}

	@Override
	public final List<HttpRequestBase> getMessages() {
		return msgs;
	}

	protected boolean isBot() {
		return bot;
	}

	protected abstract void parseResult(String s);

	@Override
	public final void processReturningText(final HttpRequestBase hm, final String s) throws ProcessException {
		parseResult(s);
	}

	public void reset() {
		this.msgs = new ArrayList<HttpRequestBase>(1);
	}

	protected void setMaxLag(MultipartEntity multipartEntity) {
		setParameter(multipartEntity, "maxlag", MAXLAG);
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this, ToStringStyle.SHORT_PREFIX_STYLE);
	}

	protected String toStringParameters(int[] parameters) {
		StringBuilder stringBuilder = new StringBuilder();

		boolean first = true;
		for (Object l : parameters) {
			if (!first)
				stringBuilder.append("|");
			stringBuilder.append(l.toString());

			first = false;
		}

		return stringBuilder.toString();
	}

	protected String toStringParameters(Iterable<? extends Object> parameters) {
		return toStringParameters(parameters, Integer.MAX_VALUE, Integer.MAX_VALUE);
	}

	protected String toStringParameters(Iterable<? extends Object> parameters, int maxForNonBots, int maxForBots) {
		StringBuilder stringBuilder = new StringBuilder();

		int counter = 0;
		boolean first = true;
		for (Object l : parameters) {

			if (isBot() ? (counter == maxForBots) : (counter == maxForNonBots)) {
				throw new IllegalArgumentException("Too many values supplied");
			}

			if (!first)
				stringBuilder.append("|");
			stringBuilder.append(l.toString());

			first = false;
			counter++;
		}

		return stringBuilder.toString();
	}

	@Override
	public void validateReturningCookies(List<Cookie> cs, HttpRequestBase hm) throws CookieException {
		// no op
	}

}