package org.wikipedia.vlsergey.secretary.trust;

public enum StatisticsKey {

	FEATURED {
		@Override
		public String getStatisticsPageDescription(Month month, String pagesAnalyzed, String histsAnalyzed) {
			return "На данной странице делается попытка построить рейтинг редакторов избранных статей русской Википедии, "
					+ "основываясь на посещаемости статей в "
					+ month.getPrepositional()
					+ " и вкладе каждого редактора.";
		}

		@Override
		public String getStatisticsPageSuffix(Month month) {
			return "Рейтинг авторов избранных статей/" + month.getYearMinusMonth();
		}
	},

	GOOD {
		@Override
		public String getStatisticsPageDescription(Month month, String pagesAnalyzed, String histsAnalyzed) {
			return "На данной странице делается попытка построить рейтинг редакторов хороших статей русской Википедии, "
					+ "основываясь на посещаемости статей в "
					+ month.getPrepositional()
					+ " и вкладе каждого редактора.";
		}

		@Override
		public String getStatisticsPageSuffix(Month month) {
			return "Рейтинг авторов хороших статей/" + month.getYearMinusMonth();
		}
	},

	TOTAL {

		@Override
		public String getStatisticsPageDescription(Month month, String pagesAnalyzed, String histsAnalyzed) {
			return "Рейтинг авторов по посещаемости статей в " + month.getPrepositional() + ", основано на анализе "
					+ pagesAnalyzed + " статей (" + histsAnalyzed + " хитов).";
		}

		@Override
		public String getStatisticsPageSuffix(Month month) {
			return "WikiRaiting/" + month.getYearMinusMonth();
		}
	};

	public abstract String getStatisticsPageDescription(Month month, String pagesAnalyzed, String histsAnalyzed);

	public abstract String getStatisticsPageSuffix(Month month);
}
