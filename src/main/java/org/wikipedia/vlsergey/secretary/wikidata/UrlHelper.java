package org.wikipedia.vlsergey.secretary.wikidata;

import java.net.URI;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiSnak;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;

@Component
public class UrlHelper extends AbstractHelper {

	private static final Set<String> PROHIBITED_HOSTS = new HashSet<>(Arrays.asList("", "vk.com", "vkontakte.ru",
			"ok.ru", "odnoklassniki.ru", "facebook.com", "www.facebook.com"));

	public List<ValueWithQualifiers> parse(EntityId property, String strValue) {

		strValue = StringUtils.trimToEmpty(strValue);

		if (strValue.matches("^\\{\\{URL\\|[^\\{\\}\\|]*\\}\\}$")) {
			strValue = strValue.replaceAll("^\\{\\{URL\\|([^\\{\\}\\|]*)\\}\\}$", "$1");
		}

		if (strValue.matches("^\\[https?\\://[a-zA-Z\\./\\-]*/?\\s+[^\\]\\[\\{\\}]*\\]$")) {
			strValue = strValue.replaceAll("^\\[(https?\\://[a-zA-Z\\./\\-]*)\\s+[^\\]\\[\\{\\}]*\\]$", "$1");
		}

		if (strValue.matches("^\\[http\\://([^\\]\\s]*)\\]$")) {
			strValue = strValue.replaceAll("^\\[(http\\://[^\\]\\s]*)\\]$", "$1");
		}

		try {
			URI uri = URI.create(strValue);
			if (uri.getScheme().equals("http") || uri.getScheme().equals("https")) {
				if (!PROHIBITED_HOSTS.contains(uri.getHost())) {

					if (StringUtils.isBlank(uri.getPath())) {
						uri = new URI(uri.getScheme(), uri.getUserInfo(), uri.getHost(), uri.getPort(), "/",
								uri.getQuery(), uri.getFragment());
					}

					return ValueWithQualifiers.fromSnak(ApiSnak.newSnak(property, uri.toString()));
				}
			}

		} catch (Exception exc) {
		}

		throw new CantParseValueException(strValue);

	}
}
