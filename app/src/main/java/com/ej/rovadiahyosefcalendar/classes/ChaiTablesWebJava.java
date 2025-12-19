package com.ej.rovadiahyosefcalendar.classes;

import java.io.IOException;
import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.kosherjava.zmanim.util.GeoLocation;
import com.kosherjava.zmanim.hebrewcalendar.JewishDate;

/**
 * Java port of the ChaiTables helper used to build and scrape ChaiTables URLs.
 * This relies on GeoLocation and JewishDate from the KosherJava library
 * and on jsoup for HTML parsing.
 *
 * Note: ChaiTables uses Hebrew month indexing starting from Tishrei (1-12 or 1-13 in leap years),
 * whereas KosherJava uses indexing starting from Nissan (1-12 or 1-13 in leap years).
 * Conversion is handled automatically by convertMonthToChaiTablesIndex() and convertMonthFromChaiTablesIndex().
 */
public final class ChaiTablesWebJava {

	private static final List<String> SEARCH_RADII = List.of(
		"0", "0.1", "0.2", "0.3", "0.4", "0.5", "0.6", "0.7", "0.8", "0.9",
		"1", "1.1", "1.2", "1.3", "1.4", "1.5", "2", "3", "4", "5",
		"6", "7", "8", "9", "10", "11", "12", "13", "14", "15"
	);

	private final GeoLocation geoLocation;
	private final JewishDate baseCalendar;

	private String selectedCountry;
	private int indexOfMetroArea;

	public ChaiTablesWebJava(GeoLocation geoLocation, JewishDate jewishCalendar) {
		this.geoLocation = Objects.requireNonNull(geoLocation, "geoLocation");
		this.baseCalendar = Objects.requireNonNull(jewishCalendar, "jewishCalendar");
	}

	/**
	 * @param selectedCountry country name expected by ChaiTables (e.g. "Israel", "USA", "Eretz_Yisroel").
	 * @param indexOfMetroArea numeric metro area index used by ChaiTables.
	 */
	public void setOtherData(String selectedCountry, int indexOfMetroArea) {
		this.selectedCountry = Objects.requireNonNull(selectedCountry, "selectedCountry");
		this.indexOfMetroArea = indexOfMetroArea;
	}

	/**
	 * Builds the ChaiTables URL with the provided parameters.
	 */
	public URL getChaiTablesLink(String searchradius, int type, JewishDate jewishCalendar, int userId) {
		if (type < 0 || type > 5) {
			throw new IllegalArgumentException("type of tables must be between 0 and 5");
		}

		Map<String, String> urlParams = new LinkedHashMap<>();
		urlParams.put("country", selectedCountry);
		urlParams.put("USAcities2", "0");
		urlParams.put("eroshgt", "0.0");

		double geoTz = geoLocation.getTimeZone().getRawOffset() / (1000d * 60d * 60d);
		urlParams.put("geotz", Double.toString(geoTz));
		urlParams.put("DST", "ON");
		urlParams.put("exactcoord", "OFF");
		urlParams.put("types", Integer.toString(type));
		urlParams.put("RoundSecond", "1");
		urlParams.put("AddCushion", "2");
		urlParams.put("24hr", "");
		urlParams.put("typezman", "-1");
		urlParams.put("yrheb", Integer.toString(jewishCalendar.getJewishYear()));
		urlParams.put("optionheb", "1");
		urlParams.put("UserNumber", Integer.toString(userId));
		urlParams.put("Language", "English");
		urlParams.put("AllowShaving", "OFF");

		if ("Eretz_Yisroel".equals(selectedCountry)) {
			urlParams.put("TableType", "BY");
			urlParams.put("USAcities1", "1");
			urlParams.put("MetroArea", Integer.toString(indexOfMetroArea));
		} else {
			urlParams.put("searchradius", searchradius);
			urlParams.put("TableType", "Chai");
			urlParams.put("USAcities1", Integer.toString(indexOfMetroArea));
			urlParams.put("eroslatitude", formatCoord(geoLocation.getLatitude()));
			urlParams.put("eroslongitude", formatCoord(-geoLocation.getLongitude()));
			urlParams.put("MetroArea", "jerusalem");
		}

		StringBuilder builder = new StringBuilder("http://chaitables.com/cgi-bin/ChaiTables.cgi/");
		builder.append("?");
		String query = urlParams.entrySet().stream()
			.map(e -> "cgi_" + e.getKey() + "=" + e.getValue())
			.collect(Collectors.joining("&"));
		builder.append(query);

		try {
			return new URL(builder.toString());
		} catch (Exception e) {
			throw new IllegalStateException("Failed to build ChaiTables URL", e);
		}
	}

	public List<Long> extractTimes(Document domParsed, JewishDate jDate) {
		Element zmanTable = findZmanTable(domParsed);
		if (zmanTable == null) {
			Element inject = domParsed.getElementById("fetchURLInject");
			String originalUrl = inject != null ? inject.data() : "";
			throw new IllegalStateException("No zman table found: " + originalUrl);
		}

		JewishDate loopCal = (JewishDate) jDate.clone();
		ZoneId zoneId = geoLocation.getTimeZone().toZoneId();

		ZonedDateTime gregorianMonthStart = startOfMonth(loopCal, zoneId);
		JewishDate jewishMonthStartDate = (JewishDate) loopCal.clone();
		jewishMonthStartDate.setJewishDayOfMonth(1);
		ZonedDateTime jewishMonthStart = startOfDay(jewishMonthStartDate, zoneId);

		ZonedDateTime compareDate = gregorianMonthStart.isBefore(jewishMonthStart)
			? gregorianMonthStart
			: jewishMonthStart;

		List<Long> times = new ArrayList<>();

		Elements rows = zmanTable.getElementsByTag("tr");
		for (int rowIndex = 1; rowIndex < rows.size(); rowIndex++) {
			Element row = rows.get(rowIndex);
			Elements cells = row.children();
			for (int monthIndex = 1; monthIndex < cells.size() - 1; monthIndex++) {
				Element cell = cells.get(monthIndex);
				String zmanTime = cell.text().trim();
				if (zmanTime.isEmpty()) {
					continue;
				}

				int dayOfMonth = Integer.parseInt(cells.get(0).text().trim());

				// Convert ChaiTables month index (Tishrei=1) to KosherJava month index (Nissan=1)
				int kosherjavaMonth = convertMonthFromChaiTablesIndex(monthIndex, loopCal.isJewishLeapYear());
				loopCal.setJewishDate(loopCal.getJewishYear(), kosherjavaMonth, dayOfMonth);

				boolean underlined = cell.html().toLowerCase(Locale.US).contains("<u");
				ZonedDateTime time = toZonedDateTime(loopCal, zoneId, zmanTime);

				if (underlined && time.getDayOfWeek() != DayOfWeek.SATURDAY) {
					System.err.println("non-Shabbat underline. Something must be wrong: " + loopCal);
					continue;
				}

				if (time.isAfter(compareDate)) {
					times.add(time.toEpochSecond());
				}
			}
		}

		return times;
	}

	public ChaiTablesResult[] formatInterfacer() throws IOException {
		JewishDate calendar = (JewishDate) baseCalendar.clone();
		ChaiTablesResult[] results = new ChaiTablesResult[2];
		int i = 0;

		Map<String, Document> radiusData = new LinkedHashMap<>();

		Optional<String> smallestRadiusOpt = determineSmallestRadius(calendar, radiusData);
		if (smallestRadiusOpt.isEmpty()) {
			throw new IOException("No valid data within a 15km search radius was found");
		}

		String smallestRadius = smallestRadiusOpt.get();

		for (JewishDate yearLoop = (JewishDate) calendar.clone();
			 yearLoop.getJewishYear() != calendar.getJewishYear() + 2;
			 yearLoop.setJewishYear(yearLoop.getJewishYear() + 1)) {

			if (calendar.getJewishYear() != yearLoop.getJewishYear()) {
				yearLoop.setJewishMonth(JewishDate.TISHREI);
				yearLoop.setJewishDayOfMonth(1);
			}

			String cacheKey = smallestRadius + "-" + yearLoop.getJewishYear();
			Document ctDoc = radiusData.get(cacheKey);
			if (ctDoc == null) {
				URL ctLink = getChaiTablesLink(smallestRadius, 0, yearLoop, 413);
				ctDoc = fetchChaiTablesDocument(ctLink);
			}

			if (findZmanTable(ctDoc) == null) {
				continue;
			}

			List<Long> times = new ArrayList<>(extractTimes(ctDoc, yearLoop));
			results[i] = new ChaiTablesResult(getChaiTablesLink(smallestRadius, 0, yearLoop, 413).toString(), times);
			i++;
		}

		return results;
	}

	private Optional<String> determineSmallestRadius(JewishDate calendar, Map<String, Document> radiusData)
		throws IOException {

		if ("Israel".equals(selectedCountry)) {
			return Optional.of("2");
		}

		if ("USA".equals(selectedCountry) && indexOfMetroArea == 32) {
			if (doubleEquals(geoLocation.getLatitude(), 34.09777065545882)
				&& doubleEquals(geoLocation.getLongitude(), -118.42699812743257)) {
				return Optional.of("14");
			}
			return Optional.of("8");
		}

		String largestRadius = SEARCH_RADII.get(SEARCH_RADII.size() - 1);
		URL ctBiggestRadius = getChaiTablesLink(largestRadius, 0, calendar, 413);
		Document ctBiggestDoc = fetchChaiTablesDocument(ctBiggestRadius);

		if (findZmanTable(ctBiggestDoc) == null) {
			return Optional.empty();
		}

		radiusData.put(largestRadius + "-" + calendar.getJewishYear(), ctBiggestDoc);

		for (String radius : SEARCH_RADII) {
			if (radius.equals(largestRadius)) {
				return Optional.of(radius);
			}

			URL ctLink = getChaiTablesLink(radius, 0, calendar, 413);
			Document ctDoc = fetchChaiTablesDocument(ctLink);

			if (findZmanTable(ctDoc) != null) {
				radiusData.put(radius + "-" + calendar.getJewishYear(), ctDoc);
				return Optional.of(radius);
			}
		}

		return Optional.of(largestRadius);
	}

	private Document fetchChaiTablesDocument(URL url) throws IOException {
		return Jsoup.connect(url.toString()).get();
	}

	private static Element findZmanTable(Document doc) {
		for (Element table : doc.getElementsByTag("table")) {
			Element firstRow = table.selectFirst("tr");
			if (firstRow == null) {
				continue;
			}
			int cells = firstRow.childrenSize();
			if (cells == 14 || cells == 15) {
				return table;
			}
		}
		return null;
	}

	private static String formatCoord(double value) {
		return String.format(Locale.US, "%.6f", value);
	}

	private ZonedDateTime toZonedDateTime(JewishDate date, ZoneId zoneId, String hhmmss) {
		String[] parts = hhmmss.split(":");
		int hour = Integer.parseInt(parts[0]);
		int minute = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
		int second = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;

		LocalDate localDate = LocalDate.of(
			date.getGregorianYear(),
			date.getGregorianMonth() + 1,
			date.getGregorianDayOfMonth()
		);

		return ZonedDateTime.of(localDate, LocalTime.of(hour, minute, second), zoneId);
	}

	private ZonedDateTime startOfDay(JewishDate date, ZoneId zoneId) {
		LocalDate localDate = LocalDate.of(
			date.getGregorianYear(),
			date.getGregorianMonth() + 1,
			date.getGregorianDayOfMonth()
		);
		return localDate.atStartOfDay(zoneId);
	}

	private ZonedDateTime startOfMonth(JewishDate date, ZoneId zoneId) {
		LocalDate localDate = LocalDate.of(
			date.getGregorianYear(),
			date.getGregorianMonth() + 1,
			1
		);
		return localDate.atStartOfDay(zoneId);
	}

	private static boolean doubleEquals(double a, double b) {
		return Math.abs(a - b) < 1e-9;
	}

	/**
	 * Converts a Hebrew month index from ChaiTables format (Tishrei=1) to KosherJava format (Nissan=1).
	 *
	 * KosherJava indexing: Nissan=1, Iyar=2, ..., Shevat=11, Adar=12, Adar_II=13 (leap year only)
	 * ChaiTables indexing: Tishrei=1, Cheshvan=2, ..., Elul=12 or 13 (depending on leap year)
	 *
	 * @param chaiTablesMonth month in ChaiTables format (1-12 or 1-13 in leap years)
	 * @param isLeapYear whether the year is a Hebrew leap year
	 * @return month in KosherJava format (1-12 or 1-13 in leap years)
	 */
	private static int convertMonthFromChaiTablesIndex(int chaiTablesMonth, boolean isLeapYear) {
		// ChaiTables: Tishrei=1, Cheshvan=2, Kislev=3, Teves=4, Shevat=5, Adar/Adar_I=6, Adar_II=7 (leap), Nissan=8 or 7, ..., Elul=12 or 13
		// KosherJava: Nissan=1, Iyar=2, Sivan=3, Tammuz=4, Av=5, Elul=6, Tishrei=7, Cheshvan=8, Kislev=9, Teves=10, Shevat=11, Adar=12, Adar_II=13 (leap)

		if (chaiTablesMonth >= 1 && chaiTablesMonth <= (isLeapYear ? 7 : 6)) {
			return chaiTablesMonth + 6;
		} else {
			return chaiTablesMonth - (isLeapYear ? 7 : 6);
		}
	}

	public record ChaiTablesResult(String url, List<Long> times) {

	}
}