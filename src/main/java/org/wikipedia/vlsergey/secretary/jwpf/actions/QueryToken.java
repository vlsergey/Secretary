package org.wikipedia.vlsergey.secretary.jwpf.actions;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.json.JSONObject;
import org.wikipedia.vlsergey.secretary.jwpf.model.TokenType;
import org.wikipedia.vlsergey.secretary.jwpf.utils.ProcessException;

public class QueryToken extends AbstractApiAction {

	private Map<TokenType, String> tokens;

	public QueryToken(boolean bot, TokenType... tokenTypes) {
		super(bot);

		log.info("[action=query; meta=tokens]: " + ToStringBuilder.reflectionToString(this, ToStringStyle.SIMPLE_STYLE));

		HttpPost postMethod = new HttpPost("/api.php");
		MultipartEntity multipartEntity = new MultipartEntity();
		setMaxLag(multipartEntity);
		setParameter(multipartEntity, "format", "json");

		setParameter(multipartEntity, "action", "query");
		setParameter(multipartEntity, "meta", "tokens");
		if (tokenTypes != null) {
			setParameter(multipartEntity, "type", tokenTypes);
		}

		postMethod.setEntity(multipartEntity);

		msgs.add(postMethod);
	}

	public Map<TokenType, String> getTokens() {
		return tokens;
	}

	@Override
	protected void parseResult(String s) {
		JSONObject jsonObject = new JSONObject(s);

		if (jsonObject.has("error")) {
			throw new ProcessException(jsonObject.getJSONObject("error").getString("info"));
		}
		if (jsonObject.has("warnings")) {
			log.warn(jsonObject.get("warnings"));
		}

		if (!jsonObject.has("query")) {
			throw new ProcessException("No 'query' in response");
		}
		JSONObject query = jsonObject.getJSONObject("query");

		if (!query.has("tokens")) {
			throw new ProcessException("No 'tokens' in response");
		}

		JSONObject tokens = query.getJSONObject("tokens");
		this.tokens = new HashMap<>();
		for (TokenType tokenType : TokenType.values()) {
			String jsonKey = tokenType.name() + "token";
			if (tokens.has(jsonKey) && StringUtils.isNotBlank(tokens.getString(jsonKey))) {
				this.tokens.put(tokenType, tokens.getString(jsonKey));
			}
		}
	}

}
