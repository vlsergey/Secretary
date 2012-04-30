package org.wikipedia.vlsergey.secretary.laws;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.client.params.CookiePolicy;
import org.apache.http.impl.client.AbstractHttpClient;
import org.wikipedia.vlsergey.secretary.http.HttpManager;
import org.wikipedia.vlsergey.secretary.utils.IoUtils;
import org.wikipedia.vlsergey.secretary.utils.StringUtils;

public class Images {

	static final Map<String, String> replaces = new HashMap<String, String>();
	static {
		replaces.put("/RXZ5wF64WLe7EXfflgarQ==", "<sup>nd</sup>");
		replaces.put("0lSVD33B1Dyf9VM2XcPnDQ==", "<sup>12</sup>");
		replaces.put("1GZku2wUCnt8pimhGzneSg==", "ω");
		replaces.put("21zs8xwvCUKtYoCOzIblIQ==", "±");
		replaces.put("2sj+J4/TKF9Ni99WqQ5QBw==", "<sub>4</sub>");
		replaces.put("7EHJu8HezGHn3dsz9BGR6Q==", "<sup>-8</sup>");
		replaces.put("9UcbExE1jCGZzH9vqS2NDg==", "<sup>5</sup>");
		replaces.put("ba5mLncpHjdOdRlllmTYMA==", "<sup>8</sup>");
		replaces.put("cqHAtIejdSYFUYV5v9WL/g==", "<sup>20</sup>");
		replaces.put("cS7Ui8czfE6NE6TqYm7suQ==", "<math>2 / 3</math>");
		replaces.put("csV7AMVXzAS3fF95aC2CKw==", "λ");
		replaces.put("f2SQ1yOZ1+r42//E9UN3Dg==", "³");
		replaces.put("FNFWWtCB9Gg8+j9YVobe5A==", "<sub>2</sub>");
		replaces.put("fNnqXo93EFkLBvzIfXg5Uw==", "<sup>-4</sup>");
		replaces.put("g4INy88kuFA47lcZ4JCD1w==", "<sup>6</sup>");
		replaces.put("iCc7B1+SSYeMl6n7UBbaPA==", "<sup>-3</sup>");
		replaces.put("iXPHjTWM1cRFbPLpSct/QA==", "<sup>C</sup>");
		replaces.put("j2FcM4AU+GMgevrbsb8xGw==", "³");
		replaces.put("jHA3GAQz/8xF/BtL13QqCA==",
				"<math>\\sqrt {\\left( {a^2  + \\left( {b \\times d} \\right)^2 } \\right)}</math>");
		replaces.put("K3gtYqIk4gSb4+/2QH/yjA==", "<math>g</math>");
		replaces.put("miNZKFcf5mRxyNNelqpSEA==", "<math>g^2</math>");
		replaces.put("n3XGUMZU4ayy4D0YTSKs1Q==", "±");
		replaces.put("NpXplDu4YbXaL1mZei9iOw==", "<sup>-6</sup>");
		replaces.put("OmcqesK5kMS6R/8K4Ni2TA==", "<sup>-1</sup>");
		replaces.put("PuADQlLXRG6JE81jP6X8jg==", "<sup>14</sup>");
		replaces.put("TPC6Rff9Z5jeYe/X4DIH5g==", "<sup>18</sup>");
		replaces.put("UiBN4eVyKtRbY1VWGlFqSA==", "<sup>-5</sup>");
		replaces.put("Vz8tuodUAuOKeKTu1wKDGQ==", "¹");
		replaces.put("WtdZQ7DIEv61nOmfH+c+1Q==", "²");
		replaces.put("xmiktQPShEvmGefjrYgvqw==", "²");
		replaces.put("XpYM3prHFVRJorNndmZDYA==", "<sup>4</sup>");
		replaces.put("yf92ndOxJK0h7kVcNDb5ug==", "α");
		replaces.put("z8k2Yc/0Mnti4AhTMvSDpQ==", "<sub>3</sub>");
	}

	public static void main(String[] args) throws Exception {
		new Images().start();
	}

	String process(final AbstractHttpClient client, String wiki) throws IOException, ClientProtocolException,
			NoSuchAlgorithmException {
		Pattern pattern = Pattern.compile("<img (class=\"doc-image\")? src=\"([^\"]*)\" width=[0-9]+ height=[0-9]+ />");
		Matcher matcher = pattern.matcher(wiki);

		Map<String, String> toReplace = new HashMap<String, String>();
		while (matcher.find()) {
			final String imageRelativeUrl = matcher.group(2);
			HttpGet get = new HttpGet("http://docs.kodeks.ru" + imageRelativeUrl);
			byte[] data = client.execute(get, new ResponseHandler<byte[]>() {
				@Override
				public byte[] handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
					return IOUtils.toByteArray(response.getEntity().getContent());
				}
			});

			final String hashcode = IoUtils.getHashcode(data);
			if (!replaces.containsKey(hashcode)) {
				System.out.println(imageRelativeUrl + " \t" + data.length + " \t" + hashcode);
			} else {
				toReplace.put(matcher.group(), replaces.get(hashcode));
			}
		}

		String result = wiki;
		for (Map.Entry<String, String> replacement : toReplace.entrySet()) {
			result = StringUtils.replace(result, replacement.getKey(), replacement.getValue());
		}
		return result;
	}

	private void start() throws Exception {
		HttpManager httpManager = new HttpManager();
		httpManager.afterPropertiesSet();
		final AbstractHttpClient client = httpManager.getClient(HttpManager.DEFAULT_CLIENT);

		client.getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.BROWSER_COMPATIBILITY);

		String wiki = IoUtils.readToString(Images.class.getResourceAsStream("images.txt"), "utf-8");

		String result = process(client, wiki);
		// System.out.println(result);
		final Writer fileWriter = new OutputStreamWriter(new FileOutputStream("result.txt"), "utf-8");
		fileWriter.write(result);
		fileWriter.close();
	}
}
