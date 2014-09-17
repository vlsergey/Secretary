package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.ApiSnak;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.EntityId;

@Component
public class GenderHelper extends AbstractHelper {

	private static final EntityId GENDER_FEMALE = EntityId.item(6581072);
	private static final EntityId GENDER_MALE = EntityId.item(6581097);

	public List<ApiSnak> parse(EntityId property, String strValue) {
		String value = strValue.trim();
		if (StringUtils.isBlank(value)) {
			return Collections.emptyList();
		}
		if (StringUtils.equalsIgnoreCase("м", value) || StringUtils.equalsIgnoreCase("мужской", value)) {
			return Collections.singletonList(ApiSnak.newSnak(property, GENDER_MALE));
		}
		if (StringUtils.equalsIgnoreCase("ж", value) || StringUtils.equalsIgnoreCase("женский", value)) {
			return Collections.singletonList(ApiSnak.newSnak(property, GENDER_FEMALE));
		}
		throw new UnsupportedParameterValue(value);
	}
}
