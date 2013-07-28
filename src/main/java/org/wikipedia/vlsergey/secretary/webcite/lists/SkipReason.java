package org.wikipedia.vlsergey.secretary.webcite.lists;

import java.net.URI;
import java.util.Collection;

public enum SkipReason {

	ARCHIVES {
		@Override
		public String botSubpageName() {
			return "IgnoreArchives";
		}

		@Override
		public Collection<String> collection() {
			return Archives.HOSTS;
		}

		@Override
		public String getConsoleDebugLogString(URI uri) {
			return "URL " + uri + " skipped because URL is archived version already";
		}

		@Override
		public String getReportDescription() {
			return "Следующие ссылки были пропущены, так как уже являются ссылками на архивные копии. "
					+ "Возможно стоит изменить их оформление, "
					+ "поместив ссылку на архивную копию в параметр archiveurl";
		}

		@Override
		public boolean skip(URI uri) {
			return Archives.contains(uri);
		}
	},

	BLACKLISTED {
		@Override
		public String botSubpageName() {
			return "IgnoreBlacklisted";
		}

		@Override
		public Collection<String> collection() {
			return Blacklisted.HOSTS;
		}

		@Override
		public String getConsoleDebugLogString(URI uri) {
			return "URL " + uri + " skipped because copyright holder has asked WebCite not to display the material";
		}

		@Override
		public String getReportDescription() {
			return "Следующие ссылки были пропущены, " + "так как они указывают на сайты, "
					+ "которые часто запрещены к показу на WebCite правообладателем";
		}

		@Override
		public boolean skip(URI uri) {
			return Blacklisted.contains(uri);
		}
	},

	ERRORS {
		@Override
		public String botSubpageName() {
			return "IgnoreErrors";
		}

		@Override
		public Collection<String> collection() {
			return Erros.HOSTS;
		}

		@Override
		public String getConsoleDebugLogString(URI uri) {
			return "Ignoring original URL '" + uri + "' because WebCite incorrectly archives this site";
		}

		@Override
		public String getReportDescription() {
			return "Следующие ссылки были пропущены, " + "так как они указывают на сайты, "
					+ "некорректно архивируемые WebCite";
		}

		@Override
		public boolean skip(URI uri) {
			return Erros.contains(uri);
		}
	},

	FAILURE_404 {
		@Override
		public String botSubpageName() {
			return "IgnoreFailure404";
		}

		@Override
		public Collection<String> collection() {
			return Failure404.HOSTS;
		}

		@Override
		public String getConsoleDebugLogString(URI uri) {
			return "Ignoring original URL '" + uri + "' because WebCite always failure with 404";
		}

		@Override
		public String getReportDescription() {
			return "Следующие ссылки были пропущены, "
					+ "так как обычно WebCite не может сархивировать страницы с данных сайтов (failure_404)";
		}

		@Override
		public boolean skip(URI uri) {
			return Failure404.contains(uri);
		}
	},

	NOCACHE {
		@Override
		public String botSubpageName() {
			return "IgnoreNoCache";
		}

		@Override
		public Collection<String> collection() {
			return NoCache.HOSTS;
		}

		@Override
		public String getConsoleDebugLogString(URI uri) {
			return "Ignoring original URL '" + uri + "' because site usually has no-cache tag on pages";
		}

		@Override
		public String getReportDescription() {
			return "Следующие ссылки были пропущены, " + "так как они указывают на сайты, "
					+ "которые часто используют тег no-cache";
		}

		@Override
		public boolean skip(URI uri) {
			return NoCache.contains(uri);
		}
	},

	SENCE {
		@Override
		public String botSubpageName() {
			return "IgnoreSence";
		}

		@Override
		public Collection<String> collection() {
			return Sence.HOSTS;
		}

		@Override
		public String getConsoleDebugLogString(URI uri) {
			return "URL " + uri + " skipped because it doesn't make sence to archive it";
		}

		@Override
		public String getReportDescription() {
			return "Следующие ссылки были пропущены, " + "так как не имеет смысла их архивировать";
		}

		@Override
		public boolean skip(URI uri) {
			return Sence.contains(uri);
		}
	},

	TECH_LIMITS {
		@Override
		public String botSubpageName() {
			return "IgnoreTechLimits";
		}

		@Override
		public Collection<String> collection() {
			return TechLimits.HOSTS;
		}

		@Override
		public String getConsoleDebugLogString(URI uri) {
			return "Ignoring original URL '" + uri
					+ "' because WebCite has technical troubles archiving links from this website";
		}

		@Override
		public String getReportDescription() {
			return "Следующие ссылки были пропущены, " + "так как они указывают на сайты, "
					+ "архивирование которых службой WebCite имеет технические сложности";
		}

		@Override
		public boolean skip(URI uri) {
			return TechLimits.contains(uri);
		}
	},

	WIKISOURCE_TARGET {
		@Override
		public String botSubpageName() {
			return "IgnoreWikisourceTarget";
		}

		@Override
		public Collection<String> collection() {
			return WikisourceTarget.HOSTS;
		}

		@Override
		public String getConsoleDebugLogString(URI uri) {
			return "Ignoring original URL '" + uri + "' because wikisource shall be used instead";
		}

		@Override
		public String getReportDescription() {
			return "Следующие ссылки были пропущены, "
					+ "потому что вместо их использования необходимо использовать ссылку на Викитеку "
					+ "(добавить указанный материал на Викитеку либо найти, если он уже там присутствует)";
		}

		@Override
		public boolean skip(URI uri) {
			return WikisourceTarget.contains(uri);
		}
	},

	WRONG_TEMPLATE {
		@Override
		public String botSubpageName() {
			return "IgnoreWrongTemplate";
		}

		@Override
		public Collection<String> collection() {
			return WrongTemplate.HOSTS;
		}

		@Override
		public String getConsoleDebugLogString(URI uri) {
			return "Ignoring original URL '" + uri + "' because it shall not appear in webcite template";
		}

		@Override
		public String getReportDescription() {
			return "Следующие ссылки были пропущены, "
					+ "так как они должны быть оформлены без использования шаблона {{tl|cite web}} "
					+ "(например, это ссылка на книгу или журнал)";
		}

		@Override
		public boolean skip(URI uri) {
			return WrongTemplate.contains(uri);
		}
	},

	;

	public static SkipReason getSkipReason(URI uri) {
		for (SkipReason skipReason : values()) {
			if (skipReason.skip(uri)) {
				return skipReason;
			}
		}
		return null;
	}

	public abstract String botSubpageName();

	public abstract Collection<String> collection();

	public abstract String getConsoleDebugLogString(URI uri);

	public abstract String getReportDescription();

	public abstract boolean skip(URI uri);

}
