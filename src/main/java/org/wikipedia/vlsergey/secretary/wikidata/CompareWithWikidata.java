package org.wikipedia.vlsergey.secretary.wikidata;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.wikipedia.vlsergey.secretary.cache.WikiCache;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.DataType;
import org.wikipedia.vlsergey.secretary.jwpf.wikidata.Properties;

@Component
public class CompareWithWikidata implements Runnable {

	@Autowired
	private CommonsCategoryHelper commonsCategoryHelper;

	@Autowired
	private CountriesHelper countriesHelper;

	@Autowired
	private GenderHelper genderHelper;

	@Autowired
	private PlacesHelper placesHelper;

	@Autowired
	private TimeHelper timeHelper;

	@Autowired
	private UrlHelper urlHelper;

	@Autowired
	@Qualifier("wikidataCache")
	WikiCache wikidataCache;

	@Autowired
	private CompareWithWikidataWorker worker;

	@Override
	public void run() {

		final TitleResolver titleResolver = new TitleResolver(wikidataCache, Locale.getDefault());
		final EntityByLinkResolver entityByLinkResolver = new EntityByLinkResolver(wikidataCache, titleResolver);

		worker.errorsReportClear();
		for (String templateName : Arrays.asList("Архитектор",
		// "Боксёр", "Киберспортсмен",
				"Кинематографист", "Персона", "Писатель", "Предприниматель",
				// "Снукерист",
				"Театральный деятель",
				// "Теннисист",
				"Учёный",
				// "Футболист",
				"Художник")) {
			worker.process(
					entityByLinkResolver,
					titleResolver,
					templateName,
					new SinglePropertyReconsiliationColumn(Arrays.asList("Дата рождения", "Дата_рождения"),
							DataType.TIME, Properties.DATE_OF_BIRTH, x -> timeHelper.parse(Properties.DATE_OF_BIRTH, x)) {
						@Override
						public ReconsiliationAction getAction(Collection<ValueWithQualifiers> wikipedia,
								Collection<ValueWithQualifiers> wikidata) {
							return timeHelper.getAction(wikipedia, wikidata);
						}
					},
					new SinglePropertyReconsiliationColumn(Arrays.asList("Место рождения", "Место_рождения"),
							DataType.WIKIBASE_ITEM, Properties.PLACE_OF_BIRTH, x -> placesHelper.parse(
									entityByLinkResolver, Properties.PLACE_OF_BIRTH, x)) {
						@Override
						public ReconsiliationAction getAction(Collection<ValueWithQualifiers> wikipedia,
								Collection<ValueWithQualifiers> wikidata) {
							return placesHelper.getAction(wikipedia, wikidata);
						}
					},
					new SinglePropertyReconsiliationColumn(Arrays.asList("Дата смерти", "Дата_смерти"), DataType.TIME,
							Properties.DATE_OF_DEATH, x -> timeHelper.parse(Properties.DATE_OF_DEATH, x)) {
						@Override
						public ReconsiliationAction getAction(Collection<ValueWithQualifiers> wikipedia,
								Collection<ValueWithQualifiers> wikidata) {
							return timeHelper.getAction(wikipedia, wikidata);
						}
					},
					new SinglePropertyReconsiliationColumn(Arrays.asList("Место смерти", "Место_смерти"),
							DataType.WIKIBASE_ITEM, Properties.PLACE_OF_DEATH, x -> placesHelper.parse(
									entityByLinkResolver, Properties.PLACE_OF_DEATH, x)) {
						@Override
						public ReconsiliationAction getAction(Collection<ValueWithQualifiers> wikipedia,
								Collection<ValueWithQualifiers> wikidata) {
							return placesHelper.getAction(wikipedia, wikidata);
						}
					},
					new SinglePropertyReconsiliationColumn(Arrays.asList("Подданство", "Гражданство"),
							DataType.WIKIBASE_ITEM, Properties.NATIONALITY, x -> countriesHelper.parse(
									entityByLinkResolver, Properties.NATIONALITY, x)) {
						@Override
						public ReconsiliationAction getAction(Collection<ValueWithQualifiers> wikipedia,
								Collection<ValueWithQualifiers> wikidata) {
							return countriesHelper.getAction(wikipedia, wikidata);
						}
					},
					new SinglePropertyReconsiliationColumn(Arrays.asList("Пол"), DataType.WIKIBASE_ITEM,
							Properties.GENDER, x -> genderHelper.parse(Properties.GENDER, x)) {
						@Override
						public ReconsiliationAction getAction(Collection<ValueWithQualifiers> wikipedia,
								Collection<ValueWithQualifiers> wikidata) {
							return genderHelper.getAction(wikipedia, wikidata);
						}
					},
					new SinglePropertyReconsiliationColumn(Arrays.asList("Сайт"), DataType.URL,
							Properties.OFFICIAL_WEBSITE, x -> urlHelper.parse(Properties.OFFICIAL_WEBSITE, x)) {
						@Override
						public ReconsiliationAction getAction(Collection<ValueWithQualifiers> wikipedia,
								Collection<ValueWithQualifiers> wikidata) {
							return urlHelper.getAction(wikipedia, wikidata);
						}
					},
					new SinglePropertyReconsiliationColumn(Arrays.asList("Викисклад"), DataType.STRING,
							Properties.COMMONS_CATEGORY, x -> commonsCategoryHelper.parse(Properties.COMMONS_CATEGORY,
									x)) {
						@Override
						public ReconsiliationAction getAction(Collection<ValueWithQualifiers> wikipedia,
								Collection<ValueWithQualifiers> wikidata) {
							return commonsCategoryHelper.getAction(wikipedia, wikidata);
						}
					});
		}
		worker.errorsReportDump();
		worker.errorsReportClear();
	}
}
