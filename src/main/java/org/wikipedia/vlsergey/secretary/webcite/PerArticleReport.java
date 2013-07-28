package org.wikipedia.vlsergey.secretary.webcite;

import java.net.URI;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.wikipedia.vlsergey.secretary.webcite.lists.SkipReason;

public class PerArticleReport {

	static class ArchivingError {

		final String archiveUrl;
		final String originalUrl;
		final String status;

		ArchivingError(String originalUrl, String archiveUrl, String status) {
			this.originalUrl = originalUrl;
			this.archiveUrl = archiveUrl;
			this.status = status;
		}
	}

	public final Map<String, String> archived = new LinkedHashMap<String, String>();
	public final Map<String, String> dead = new LinkedHashMap<String, String>();
	public final List<ArchivingError> nonArchived = new LinkedList<ArchivingError>();
	public final Map<String, String> potentiallyDead = new LinkedHashMap<String, String>();

	public final Map<SkipReason, Set<URI>> skippedByUri = new TreeMap<SkipReason, Set<URI>>();

	public final Set<String> skippedIncorrectFormat = new LinkedHashSet<String>();
	public final Set<String> skippedMarkedArchived = new LinkedHashSet<String>();
	public final Set<String> skippedMarkedDead = new LinkedHashSet<String>();
	public final Set<String> skippedMissingTitle = new LinkedHashSet<String>();

	public final Map<String, String> skippedOnHttpCheck = new LinkedHashMap<String, String>();

	public final Set<String> skippedTooYoung = new LinkedHashSet<String>();

	private void appendLink(StringBuilder stringBuilder, Object url, boolean noWikiLinks) {
		if (noWikiLinks)
			stringBuilder.append("* <nowiki>" + url + "</nowiki>\n");
		else
			stringBuilder.append("* " + url + "\n");
	}

	private void appendLinks(StringBuilder stringBuilder, Collection<?> links, boolean noWikiLinks) {
		for (Object url : links)
			appendLink(stringBuilder, url, noWikiLinks);
		stringBuilder.append("\n");
	}

	private void appendLinks(StringBuilder stringBuilder, String intro, String tableHeader,
			List<ArchivingError> errors, boolean noWikiLinks) {
		if (!errors.isEmpty()) {
			stringBuilder.append(intro + ":\n");
			stringBuilder.append("{{{!}} class=\"wikitable sortable\" \n");
			stringBuilder.append(tableHeader + "\n");

			for (ArchivingError entry : errors) {
				stringBuilder.append("{{!-}}\n");
				stringBuilder.append("{{!}} ");

				if (noWikiLinks)
					stringBuilder.append("<nowiki>" + entry.originalUrl + "</nowiki>");
				else
					stringBuilder.append(entry.originalUrl);

				stringBuilder.append(" {{!}}{{!}} ");
				stringBuilder.append(entry.archiveUrl);
				stringBuilder.append("\n");

				stringBuilder.append(" {{!}}{{!}} ");
				stringBuilder.append(entry.status);
				stringBuilder.append("\n");
			}
			stringBuilder.append("{{!}}}\n");
			stringBuilder.append("\n");
		}
	}

	private void appendLinks(StringBuilder stringBuilder, String intro, String tableHeader, Map<String, String> dead,
			boolean noWikiLinks) {
		if (!dead.isEmpty()) {
			stringBuilder.append(intro + ":\n");
			stringBuilder.append("{{{!}} class=\"wikitable sortable\" \n");
			stringBuilder.append(tableHeader + "\n");

			for (Map.Entry<String, String> entry : dead.entrySet()) {
				stringBuilder.append("{{!-}}\n");
				stringBuilder.append("{{!}} ");

				if (noWikiLinks)
					stringBuilder.append("<nowiki>" + entry.getKey() + "</nowiki>");
				else
					stringBuilder.append(entry.getKey());

				stringBuilder.append(" {{!}}{{!}} ");
				stringBuilder.append(entry.getValue());
				stringBuilder.append("\n");
			}
			stringBuilder.append("{{!}}}\n");
			stringBuilder.append("\n");
		}
	}

	void archived(String url, String archiveUrl) {
		archived.put(url, archiveUrl);
	}

	void dead(String url, String reason) {
		dead.put(url, reason);
	}

	boolean hasChanges() {
		return !archived.isEmpty() || !dead.isEmpty();
	}

	void nonArchived(String url, String archiveUrl, String status) {
		nonArchived.add(new ArchivingError(url, archiveUrl, status));
	}

	void potentiallyDead(String url, String reason) {
		potentiallyDead.put(url, reason);
	}

	void skippedByUri(URI uri, SkipReason skipReason) {
		if (!skippedByUri.containsKey(skipReason)) {
			skippedByUri.put(skipReason, new TreeSet<URI>());
		}
		skippedByUri.get(skipReason).add(uri);
	}

	void skippedIncorrectFormat(String url) {
		skippedIncorrectFormat.add(url);
	}

	void skippedMarkedArchived(String url) {
		// was is our?
		if (archived.containsKey(url))
			return;

		skippedMarkedArchived.add(url);
	}

	void skippedMarkedDead(String url) {
		// was is our?
		if (dead.containsKey(url))
			return;

		skippedMarkedDead.add(url);
	}

	void skippedMissingTitle(String url) {
		skippedMissingTitle.add(url);
	}

	void skippedOnHttpCheck(String url, String reason) {
		skippedOnHttpCheck.put(url, reason);
	}

	void skippedTooYoung(String url) {
		skippedTooYoung.add(url);
	}

	public String toWiki(String title, String anchor, boolean noWikiLinks) {
		StringBuilder stringBuilder = new StringBuilder();

		stringBuilder.append("\n\n== ");
		stringBuilder.append(title);
		stringBuilder.append(" ==\n");
		stringBuilder.append("<div id='" + anchor + "'></div>");
		if (archived.size() != 0)
			stringBuilder
					.append(archived.size()
							+ " шаблонов {{tl|citeweb}} были дополнены ссылками на только что созданную архивную копию материала. ");
		if (dead.size() != 0)
			stringBuilder.append(dead.size() + " шаблонов {{tl|citeweb}} были помечены как «мёртвые». ");
		stringBuilder.append("Оставшиеся ссылки были пропущены по различным причинам. ");
		stringBuilder.append("Далее приведена детальная информация.\n\n");

		if (!skippedIncorrectFormat.isEmpty()) {
			stringBuilder
					.append("Следующие ссылки были пропущены, так как их формат не смог быть корректно воспринят ботом:\n");
			appendLinks(stringBuilder, skippedIncorrectFormat, noWikiLinks);
		}

		if (!skippedMarkedArchived.isEmpty()) {
			stringBuilder.append("Следующие ссылки были пропущены, так как уже имеют ссылку на архивную версию:\n");
			appendLinks(stringBuilder, skippedMarkedArchived, noWikiLinks);
		}

		if (!skippedMarkedDead.isEmpty()) {
			stringBuilder.append("Следующие ссылки были пропущены, так как уже помечены как «мёртвые»:\n");
			appendLinks(stringBuilder, skippedMarkedDead, noWikiLinks);
		}

		for (SkipReason skipReason : SkipReason.values()) {
			if (!skippedByUri.containsKey(skipReason)) {
				continue;
			}

			stringBuilder.append(skipReason.getReportDescription());
			stringBuilder.append(":\n");
			appendLinks(stringBuilder, skippedByUri.get(skipReason), noWikiLinks);
		}

		if (!skippedTooYoung.isEmpty()) {
			stringBuilder.append("Следующие ссылки были пропущены, " + "так как они появились слишком недавно:\n");
			appendLinks(stringBuilder, skippedTooYoung, noWikiLinks);
		}

		appendLinks(stringBuilder, "Следующие ссылки были помечены как «мёртвые»", "! Ссылка !! Причина", this.dead,
				noWikiLinks);

		appendLinks(stringBuilder, "Следующие ссылки были пропущены, "
				+ "так как они являются потенциально «мёртвыми» и недоступны в настоящий момент",
				"! Ссылка !! Причина", this.potentiallyDead, noWikiLinks);

		appendLinks(stringBuilder, "Следующие ссылки были пропущены, "
				+ "так как проверка их статуса дала неоднозначный для бота результат", "! Ссылка !! Причина",
				this.skippedOnHttpCheck, noWikiLinks);

		appendLinks(stringBuilder, "Следующие ссылки были успешно архивированы",
				"! Ссылка !! Ссылка на архивную копию", this.archived, noWikiLinks);

		appendLinks(stringBuilder, "Следующие ссылки были добавлены в WebCite, но их архивация завершилась с ошибкой",
				"! Ссылка !! Ссылка на архивную копию !! Тип ошибки", this.nonArchived, noWikiLinks);

		stringBuilder.append("Спасибо за внимание! ~~~~");

		return stringBuilder.toString();
	}
}
