package com.ej.rovadiahyosefcalendar.classes

import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar
import com.kosherjava.zmanim.hebrewcalendar.JewishCalendar.Parsha
import java.util.Calendar

class MakamJCal {
    enum class AllBooks {
        BERESHIT,
        SHEMOT,
        VAYIKRA,
        BAMIDBAR,
        DEVARIM
    }

    enum class Makam {
        HOSENI,
        HIJAZ,
        HIJAZ_KAR,
        BAYAT,
        SABA,
        SIGAH,
        AJAM,
        RAST,
        SASGAR,
        IRAQ,
        MAHOUR,
        NAHWAND,
        NAWAH,
        MEHAYAR,
        BUSTANIGAR,
        ASHIRAN,
        GIRKA,
        OJ,
        RAHAWI,
        ARAZBAR,
        SHURI,
        KURD,
        ZANGIRAN,
        MOUHAYAR,

        // TURKISH
        HUSEYNI,
        ACEM_ASHIRAN,
        SEGAH,
        HICAZ,
        NAHOFT,
        DUGAH,
        MAHUR,
        NIHAVEND,
        ARABAN,
        BEYATI,
        USSAK,
        ISFAHAN,
        CARGAH,
        SEHNAZ,
        SEBAH,

        // GREEK
        FARAHNAQ,
        HUZAM,
        QARGIGAR,
        BUSALIQ,
        MUHAYER,
        SUZNIQ,
        BAYATI,

        // Questionable Makam
        Q_BUSTANIGAR,
        Q_FARAHNAQ,
        Q_HOSENI,
        Q_SIGAH,
        Q_QARGIGAR
    }

    /**
     * This method returns a string that contains the weekly Makam. The {@link JewishCalendar}
     * object passed into this method should be preset with the correct date.
     * @param jCal the JewishCalendar object set to Saturday
     * @return All the data fields
     */
    companion object {
        private fun getAllBooks(allBooks: AllBooks): List<String> {
            val allSefer = listOf(
                "ADES: 24793",
                "GABRIEL A SHREM 1964 SUHV", // AKA ARTSCROLL
                "TABBUSH Ms NLI 8*7622, Aleppo",
                "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905",
                "TEBELE Pre1888",
                "ELIE SHAUL COHEN FROM AINTAB, ~1880",
                "YAAQOB ABADI-PARSIYA",
                "YISHAQ YEQAR ARGENTINA",
                "Dibre Shelomo S KASSIN Pre1915",
                "Knis Betesh Geniza List, Aleppo",
                "ABRAHAM DWECK Pre1920",
                "IDELSOHN Pre1923",
                "S SAGIR Laniado",
                "M H Elias, SHIR HADASH, Jerusalem, 1930",
                "ASHEAR list",
                "ASHEAR NOTES 1936-1940",
                "ABRAHAM E SHREM ~1945",
                "Argentina 1947 & Ezra Mishanieh",
                "Shire Zimra H S ABOUD Jerusalem, 1950",
                "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO",
                "YOSEF YEHEZKEL Jerusalem 1975",
                "Ish Massliah \"Abia Renanot\" Tunisians",
                "Shaare Zimra YANANI Buenos Aires, 01",
                "BOZO, Ades, Shir Ushbaha 2005",
                "Yishaq Yeranen Halabi",
                "MOSHE AMASH (Shami)",
                "EZRA MASLATON TARAB (Shami)",
                "ABRAHAM SHAMRICHA (Shami)"
            )

            return when (allBooks) {
                AllBooks.BERESHIT -> allSefer + listOf(
                    "Victor Afya, Istanbul List",
                    "Izak Alaluf, Izmir List",
                    "Hallel VeZimrah, Salonika, 1928"
                )
                AllBooks.SHEMOT -> allSefer + listOf(
                    "Victor Afya, Istanbul List",
                    "Izak Alaluf, Izmir List",
                    "Hallel VeZimrah, Greece List, 1926"
                )
                AllBooks.VAYIKRA -> allSefer + listOf(
                    "Victor Afya, Istanbul List",
                    "Izak Alaluf, Izmir List",
                    "Hallel VeZimrah, Greece List, 1926"
                )
                AllBooks.BAMIDBAR -> allSefer + listOf(
                    "Hallel VeZimrah, Greece List, 1926"
                )
                AllBooks.DEVARIM -> allSefer + listOf(
                    "Hallel VeZimrah, Greece List, 1926"
                )
            }
        }

        fun <K, V> generateUniformMap(keys: List<K>, value: V, normal: Map<K, V>): Map<K, V> {
            return keys.associateWith { value } + normal
        }

        fun getMakamData(jCal: JewishCalendar): Map<String, List<Makam>> {
            if (jCal.isYomTov()) {
                when (jCal.getYomTovIndex()) {
                    JewishCalendar.ROSH_HASHANA ->
                        return if (jCal.jewishDayOfMonth == 1)
                            mapOf(
                                "Eliahou Yaaqob DWECK-KESAR" to listOf(Makam.HOSENI),
                                "YISHAQ YEQAR ARGENTINA" to listOf(Makam.HIJAZ),
                                "IDELSOHN Pre1923" to listOf(Makam.SABA),
                            )
                        else
                            mapOf(
                                "Eliahou Yaaqob DWECK-KESAR" to listOf(Makam.HOSENI),
                                "YISHAQ YEQAR ARGENTINA" to listOf(Makam.HIJAZ),
                                "IDELSOHN Pre1923" to listOf(Makam.BAYAT)
                            )

                    JewishCalendar.YOM_KIPPUR ->
                        return mapOf(
                            "Eliahou Yaaqob DWECK-KESAR" to listOf(Makam.HIJAZ, Makam.HOSENI),
                            "YISHAQ YEQAR ARGENTINA" to listOf(Makam.HIJAZ),
                            "IDELSOHN Pre1923" to listOf(Makam.HIJAZ),
                        )

                    JewishCalendar.SUCCOS ->
                        return if (jCal.jewishDayOfMonth == 15)
                            mapOf(
                                "SASSOON #647 Aleppo, 1850" to listOf(Makam.SIGAH),
                                "Eliahou Yaaqob DWECK-KESAR" to listOf(Makam.SIGAH),
                                "YAAQOB ABADI-PARSIYA" to listOf(Makam.SIGAH),
                                "YISHAQ YEQAR ARGENTINA" to listOf(Makam.SIGAH),
                                "ABRAHAM DWECK Pre1920" to listOf(Makam.SIGAH),
                                "IDELSOHN Pre1923" to listOf(Makam.SIGAH),
                                "S SAGIR Laniado" to listOf(Makam.SIGAH),
                                "ASHEAR list" to listOf(Makam.SIGAH),
                                "ASHEAR NOTES 1936-1940" to listOf(Makam.SIGAH),
                                "ABRAHAM E SHREM ~1945" to listOf(Makam.SIGAH),
                                "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.SIGAH),
                                "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.SIGAH),
                                "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.SIGAH),
                                "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.AJAM),
                                "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.SIGAH),
                                "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.SIGAH),
                                "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.SIGAH),
                                "MOSHE AMASH (Shami)" to listOf(Makam.RAST),
                                "EZRA MASLATON TARAB (Shami)" to listOf(Makam.RAST),
                                "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.RAST),
                                "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.SIGAH),
                            )
                        else
                            mapOf(
                                "SASSOON #647 Aleppo, 1850" to listOf(Makam.SASGAR),
                                "Eliahou Yaaqob DWECK-KESAR" to listOf(Makam.AJAM),
                                "YAAQOB ABADI-PARSIYA" to listOf(Makam.SASGAR),
                                "YISHAQ YEQAR ARGENTINA" to listOf(Makam.AJAM),
                                "ABRAHAM DWECK Pre1920" to listOf(Makam.SASGAR),
                                "S SAGIR Laniado" to listOf(Makam.AJAM),
                                "ASHEAR list" to listOf(Makam.AJAM),
                                "ASHEAR NOTES 1936-1940" to listOf(Makam.AJAM),
                                "ABRAHAM E SHREM ~1945" to listOf(Makam.AJAM),
                                "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.AJAM),
                                "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.AJAM),
                                "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.AJAM),
                                "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.AJAM),
                                "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.AJAM),
                                "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.AJAM),
                                "MOSHE AMASH (Shami)" to listOf(Makam.SIGAH),
                                "EZRA MASLATON TARAB (Shami)" to listOf(Makam.SIGAH),
                                "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.BAYAT),
                                "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.RAST),
                            )

                    JewishCalendar.CHOL_HAMOED_SUCCOS -> {
                        val cholHamoedSukMap = when (jCal.jewishDayOfMonth) {
                            16 -> mapOf(
                                "SASSOON #647 Aleppo, 1850" to listOf(Makam.SASGAR),
                                "Eliahou Yaaqob DWECK-KESAR" to listOf(Makam.AJAM),
                                "YAAQOB ABADI-PARSIYA" to listOf(Makam.SASGAR),
                                "YISHAQ YEQAR ARGENTINA" to listOf(Makam.AJAM),
                                "ABRAHAM DWECK Pre1920" to listOf(Makam.SASGAR),
                                "S SAGIR Laniado" to listOf(Makam.AJAM),
                                "ASHEAR list" to listOf(Makam.AJAM),
                                "ASHEAR NOTES 1936-1940" to listOf(Makam.AJAM),
                                "ABRAHAM E SHREM ~1945" to listOf(Makam.AJAM),
                                "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.AJAM),
                                "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.AJAM),
                                "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.AJAM),
                                "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.AJAM),
                                "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.AJAM),
                                "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.AJAM),
                                "MOSHE AMASH (Shami)" to listOf(Makam.SIGAH),
                                "EZRA MASLATON TARAB (Shami)" to listOf(Makam.SIGAH),
                                "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.BAYAT),
                                "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.RAST),
                            ) // Day 2 of Diaspora, but 1st day Hol Hamoed Israel
                            17 -> mapOf(
                                "SASSOON #647 Aleppo, 1850" to listOf(Makam.IRAQ),
                                "Eliahou Yaaqob DWECK-KESAR" to listOf(Makam.SABA),
                                "YAAQOB ABADI-PARSIYA" to listOf(Makam.IRAQ),
                                "YISHAQ YEQAR ARGENTINA" to listOf(Makam.SABA),
                                "ASHEAR list" to listOf(Makam.IRAQ),
                                "ASHEAR NOTES 1936-1940" to listOf(Makam.IRAQ),
                                "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.IRAQ),
                                "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.IRAQ),
                            )

                            18 -> mapOf(
                                "SASSOON #647 Aleppo, 1850" to listOf(Makam.SABA),
                                "Eliahou Yaaqob DWECK-KESAR" to listOf(Makam.BAYAT),
                                "YAAQOB ABADI-PARSIYA" to listOf(Makam.SABA),
                                "YISHAQ YEQAR ARGENTINA" to listOf(Makam.RAST),
                                "ASHEAR list" to listOf(Makam.RAST),
                                "ASHEAR NOTES 1936-1940" to listOf(Makam.RAST),
                                "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.RAST),
                                "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.RAST),
                            )

                            19 -> mapOf(
                                "SASSOON #647 Aleppo, 1850" to listOf(Makam.RAST),
                                "Eliahou Yaaqob DWECK-KESAR" to listOf(Makam.RAST),
                                "YAAQOB ABADI-PARSIYA" to listOf(Makam.RAST),
                                "YISHAQ YEQAR ARGENTINA" to listOf(Makam.BAYAT),
                                "ASHEAR list" to listOf(Makam.NAWAH),
                                "ASHEAR NOTES 1936-1940" to listOf(Makam.NAWAH),
                                "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.NAHWAND),
                                "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.NAHWAND),
                            )

                            20 -> mapOf(
                                "SASSOON #647 Aleppo, 1850" to listOf(Makam.HOSENI),
                                "Eliahou Yaaqob DWECK-KESAR" to listOf(Makam.NAWAH),
                                "YAAQOB ABADI-PARSIYA" to listOf(Makam.HOSENI),
                                "YISHAQ YEQAR ARGENTINA" to listOf(Makam.HOSENI),
                                "ASHEAR list" to listOf(Makam.BAYAT),
                                "ASHEAR NOTES 1936-1940" to listOf(Makam.BAYAT),
                                "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.BAYAT),
                                "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.HOSENI),
                            )

                            else -> mapOf()
                        }

                        return if (jCal.dayOfWeek == Calendar.SATURDAY) {
                            cholHamoedSukMap + mapOf(
                                "Eliahou Yaaqob DWECK-KESAR" to listOf(Makam.ASHIRAN),
                                "ASHEAR NOTES 1936-1940" to listOf(Makam.MAHOUR),
                                "ABRAHAM E SHREM ~1945" to listOf(Makam.BAYAT),
                                "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.BAYAT),
                                "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.BAYAT),
                                "MOSHE AMASH (Shami)" to listOf(Makam.SABA),
                                "EZRA MASLATON TARAB (Shami)" to listOf(Makam.BAYAT),
                                "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.Q_BUSTANIGAR)
                            )
                        } else cholHamoedSukMap
                    }

                    JewishCalendar.HOSHANA_RABBA -> return mapOf(
                        "Eliahou Yaaqob DWECK-KESAR" to listOf(Makam.HOSENI),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.SABA),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.ASHIRAN),
                        "ASHEAR list" to listOf(Makam.MEHAYAR),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.MEHAYAR),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.MEHAYAR),
                    )

                    JewishCalendar.SHEMINI_ATZERES -> return if (!jCal.inIsrael) {
                        mapOf(
                            "SASSOON #647 Aleppo, 1850" to listOf(Makam.AJAM),
                            "Eliahou Yaaqob DWECK-KESAR" to listOf(Makam.ASHIRAN),
                            "YAAQOB ABADI-PARSIYA" to listOf(Makam.BAYAT),
                            "ABRAHAM DWECK Pre1920" to listOf(Makam.BAYAT),
                            "ASHEAR list" to listOf(Makam.AJAM),
                            "ASHEAR NOTES 1936-1940" to listOf(Makam.AJAM),
                            "ABRAHAM E SHREM ~1945" to listOf(Makam.SABA),
                            "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.AJAM),
                            "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.HOSENI),
                            "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.SABA, Makam.SIGAH),
                            "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.RAST),
                            "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.AJAM),
                            "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.SABA),
                            "BOZO, Ades, Shir Ushbaha 2005" to listOf(
                                Makam.SABA,
                                Makam.SIGAH,
                                Makam.HOSENI
                            ),
                            "MOSHE AMASH (Shami)" to listOf(Makam.RAST),
                            "EZRA MASLATON TARAB (Shami)" to listOf(Makam.RAST),
                            "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.RAST),
                            "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.HIJAZ_KAR),
                        )
                    } else {
                        // This data is taken from the Parasha Page on VeZot Haberacha
                        mapOf(
                            "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.AJAM),
                            "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(Makam.AJAM),
                            "TEBELE Pre1888" to listOf(Makam.AJAM),
                            "YAAQOB ABADI-PARSIYA" to listOf(Makam.AJAM),
                            "YISHAQ YEQAR ARGENTINA" to listOf(Makam.AJAM),
                            "ADES: 24793" to listOf(Makam.AJAM),
                            "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.AJAM),
                            "ABRAHAM DWECK Pre1920" to listOf(Makam.AJAM),
                            "IDELSOHN Pre1923" to listOf(Makam.AJAM),
                            "S SAGIR Laniado" to listOf(Makam.AJAM),
                            "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.AJAM),
                            "ASHEAR list" to listOf(Makam.AJAM),
                            "ABRAHAM E SHREM ~1945" to listOf(Makam.AJAM),
                            "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.AJAM),
                            "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.AJAM),
                            "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.AJAM),
                            "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.AJAM),
                            "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.AJAM),
                            "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.AJAM),
                            "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.AJAM),
                            "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.AJAM),
                            "Yishaq Yeranen Halabi" to listOf(Makam.AJAM),
                            "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.BAYAT),
                        )
                    }

                    JewishCalendar.SIMCHAS_TORAH ->
                        // This data is taken from the Yom Tov Page
                        return mapOf(
                            "SASSOON #647 Aleppo, 1850" to listOf(Makam.BAYAT),
                            "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(Makam.AJAM),
                            "Eliahou Yaaqob DWECK-KESAR" to listOf(Makam.AJAM),
                            "YAAQOB ABADI-PARSIYA" to listOf(Makam.AJAM),
                            "YISHAQ YEQAR ARGENTINA" to listOf(Makam.AJAM),
                            "ADES: 24793" to listOf(Makam.AJAM),
                            "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.AJAM),
                            "ABRAHAM DWECK Pre1920" to listOf(Makam.AJAM),
                            "IDELSOHN Pre1923" to listOf(Makam.AJAM),
                            "S SAGIR Laniado" to listOf(Makam.AJAM),
                            "ASHEAR list" to listOf(Makam.SABA),
                            "ASHEAR NOTES 1936-1940" to listOf(Makam.SABA),
                            "ABRAHAM E SHREM ~1945" to listOf(Makam.AJAM),
                            "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.SABA),
                            "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.AJAM),
                            "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.AJAM),
                            "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.GIRKA),
                            "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.SIGAH),
                            "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.AJAM),
                            "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.AJAM),
                            "Yishaq Yeranen Halabi" to listOf(Makam.AJAM),
                            "MOSHE AMASH (Shami)" to listOf(Makam.BAYAT),
                            "EZRA MASLATON TARAB (Shami)" to listOf(Makam.BAYAT),
                            "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.BAYAT),
                        )

                    JewishCalendar.PURIM, JewishCalendar.SHUSHAN_PURIM ->
                        return mapOf(
                            "Eliahou Yaaqob DWECK-KESAR" to listOf(Makam.OJ, Makam.SABA),
                            "IDELSOHN Pre1923" to listOf(Makam.OJ, Makam.SIGAH),
                            "ASHEAR NOTES 1936-1940" to listOf(Makam.SIGAH),
                            "GABRIEL A SHREM 1964 SUHV" to listOf(if (jCal.getYomTovIndex() == JewishCalendar.PURIM) Makam.OJ else Makam.SIGAH),
                            "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.OJ, Makam.SIGAH),
                            "MOSHE AMASH (Shami)" to listOf(Makam.SIGAH),
                            "EZRA MASLATON TARAB (Shami)" to listOf(if (jCal.getYomTovIndex() == JewishCalendar.PURIM) Makam.SIGAH else Makam.RAST),
                            "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.SIGAH)
                        )

                    JewishCalendar.PESACH -> return when (jCal.jewishDayOfMonth) {
                        15 -> mapOf(
                            "Eliahou Yaaqob DWECK-KESAR" to listOf(Makam.SIGAH),
                            "YAAQOB ABADI-PARSIYA" to listOf(Makam.SIGAH),
                            "ABRAHAM DWECK Pre1920" to listOf(Makam.SIGAH),
                            "IDELSOHN Pre1923" to listOf(Makam.BAYAT),
                            "S SAGIR Laniado" to listOf(Makam.SIGAH),
                            "ASHEAR list" to listOf(Makam.SIGAH),
                            "ASHEAR NOTES 1936-1940" to listOf(Makam.SIGAH),
                            "ABRAHAM E SHREM ~1945" to listOf(Makam.SIGAH),
                            "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.SIGAH),
                            "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.SIGAH),
                            "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.SIGAH),
                            "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.SIGAH),
                            "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.SIGAH),
                            "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.SIGAH),
                            "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.SIGAH, Makam.BAYAT),
                            "MOSHE AMASH (Shami)" to listOf(Makam.RAST),
                            "EZRA MASLATON TARAB (Shami)" to listOf(Makam.RAST),
                            "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.RAST),
                            "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.SIGAH),
                        )

                        16 ->
                            mapOf(
                                "Eliahou Yaaqob DWECK-KESAR" to listOf(Makam.AJAM),
                                "YAAQOB ABADI-PARSIYA" to listOf(Makam.SASGAR),
                                "ABRAHAM DWECK Pre1920" to listOf(Makam.SASGAR),
                                "IDELSOHN Pre1923" to listOf(Makam.SASGAR),
                                "S SAGIR Laniado" to listOf(Makam.AJAM),
                                "ASHEAR list" to listOf(Makam.AJAM),
                                "ASHEAR NOTES 1936-1940" to listOf(Makam.AJAM),
                                "ABRAHAM E SHREM ~1945" to listOf(Makam.AJAM),
                                "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.AJAM),
                                "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.AJAM),
                                "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.AJAM),
                                "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.AJAM),
                                "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.AJAM),
                                "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.AJAM, Makam.SASGAR),
                                "MOSHE AMASH (Shami)" to listOf(Makam.SIGAH),
                                "EZRA MASLATON TARAB (Shami)" to listOf(Makam.SIGAH),
                                "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.SIGAH),
                                "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.AJAM),
                            )

                        21 ->
                            mapOf(
                                "Eliahou Yaaqob DWECK-KESAR" to listOf(Makam.AJAM),
                                "YAAQOB ABADI-PARSIYA" to listOf(Makam.AJAM),
                                "ABRAHAM DWECK Pre1920" to listOf(Makam.AJAM),
                                "IDELSOHN Pre1923" to listOf(Makam.AJAM),
                                "S SAGIR Laniado" to listOf(Makam.AJAM),
                                "ASHEAR list" to listOf(Makam.AJAM),
                                "ASHEAR NOTES 1936-1940" to listOf(Makam.AJAM),
                                "ABRAHAM E SHREM ~1945" to listOf(Makam.AJAM),
                                "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.AJAM),
                                "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.AJAM),
                                "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.AJAM),
                                "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.GIRKA),
                                "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.AJAM),
                                "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.AJAM),
                                "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.AJAM),
                                "MOSHE AMASH (Shami)" to listOf(Makam.AJAM),
                                "EZRA MASLATON TARAB (Shami)" to listOf(Makam.AJAM),
                                "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.AJAM),
                                "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.FARAHNAQ),
                            )

                        22 ->
                            mapOf(
                                "Eliahou Yaaqob DWECK-KESAR" to listOf(Makam.ASHIRAN),
                                "ABRAHAM DWECK Pre1920" to listOf(Makam.BAYAT),
                                "IDELSOHN Pre1923" to listOf(Makam.MEHAYAR),
                                "S SAGIR Laniado" to listOf(Makam.SABA),
                                "ASHEAR list" to listOf(Makam.SABA),
                                "ASHEAR NOTES 1936-1940" to listOf(Makam.SABA),
                                "ABRAHAM E SHREM ~1945" to listOf(Makam.SABA),
                                "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.SABA),
                                "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.SABA),
                                "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.SABA),
                                "BOZO, Ades, Shir Ushbaha 2005" to listOf(
                                    Makam.SABA,
                                    Makam.MEHAYAR
                                ),
                                "MOSHE AMASH (Shami)" to listOf(Makam.BAYAT),
                                "EZRA MASLATON TARAB (Shami)" to listOf(Makam.BAYAT),
                                "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.IRAQ),
                            )

                        else -> mapOf()
                    }

                    JewishCalendar.CHOL_HAMOED_PESACH -> {
                        val pesachHHReturnData = when (jCal.jewishDayOfMonth) {
                            16 ->
                                mapOf(
                                    "Eliahou Yaaqob DWECK-KESAR" to listOf(Makam.AJAM),
                                    "YAAQOB ABADI-PARSIYA" to listOf(Makam.SASGAR),
                                    "ABRAHAM DWECK Pre1920" to listOf(Makam.SASGAR),
                                    "IDELSOHN Pre1923" to listOf(Makam.SASGAR),
                                    "S SAGIR Laniado" to listOf(Makam.AJAM),
                                    "ASHEAR list" to listOf(Makam.AJAM),
                                    "ASHEAR NOTES 1936-1940" to listOf(Makam.AJAM),
                                    "ABRAHAM E SHREM ~1945" to listOf(Makam.AJAM),
                                    "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.AJAM),
                                    "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.AJAM),
                                    "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.AJAM),
                                    "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.AJAM),
                                    "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.AJAM),
                                    "BOZO, Ades, Shir Ushbaha 2005" to listOf(
                                        Makam.AJAM,
                                        Makam.SASGAR
                                    ),
                                    "MOSHE AMASH (Shami)" to listOf(Makam.SIGAH),
                                    "EZRA MASLATON TARAB (Shami)" to listOf(Makam.SIGAH),
                                    "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.SIGAH),
                                    "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.AJAM),
                                )

                            17 ->
                                mapOf(
                                    "Eliahou Yaaqob DWECK-KESAR" to listOf(Makam.SABA),
                                    "YAAQOB ABADI-PARSIYA" to listOf(Makam.IRAQ),
                                    "IDELSOHN Pre1923" to listOf(Makam.IRAQ),
                                    "ASHEAR list" to listOf(Makam.IRAQ),
                                    "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.IRAQ),
                                    "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.IRAQ),
                                )

                            18 ->
                                mapOf(
                                    "Eliahou Yaaqob DWECK-KESAR" to listOf(Makam.BAYAT),
                                    "YAAQOB ABADI-PARSIYA" to listOf(Makam.RAHAWI),
                                    "IDELSOHN Pre1923" to listOf(Makam.RAHAWI),
                                    "ASHEAR list" to listOf(Makam.SABA),
                                    "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.SABA),
                                    "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.RAHAWI),
                                )

                            19 ->
                                mapOf(
                                    "Eliahou Yaaqob DWECK-KESAR" to listOf(Makam.RAST),
                                    "YAAQOB ABADI-PARSIYA" to listOf(Makam.RAST),
                                    "IDELSOHN Pre1923" to listOf(Makam.RAST),
                                    "ASHEAR list" to listOf(Makam.RAST),
                                    "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.RAST),
                                    "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.RAST),
                                )

                            20 ->
                                mapOf(
                                    "Eliahou Yaaqob DWECK-KESAR" to listOf(Makam.RAHAWI),
                                    "YAAQOB ABADI-PARSIYA" to listOf(Makam.SABA),
                                    "IDELSOHN Pre1923" to listOf(Makam.SABA),
                                    "ASHEAR list" to listOf(Makam.BAYAT),
                                    "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.BAYAT),
                                    "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.BAYAT),
                                )

                            else -> mapOf()
                        }

                        return if (jCal.dayOfWeek == Calendar.SATURDAY)
                            pesachHHReturnData + mapOf("GABRIEL A SHREM 1964 SUHV" to listOf(Makam.BAYAT))
                        else pesachHHReturnData
                    }

                    JewishCalendar.SHAVUOS -> return if (jCal.jewishDayOfMonth == 7)
                        mapOf(
                            "Eliahou Yaaqob DWECK-KESAR" to listOf(Makam.AJAM),
                            "ABRAHAM DWECK Pre1920" to listOf(Makam.SASGAR),
                            "S SAGIR Laniado" to listOf(Makam.AJAM),
                            "ASHEAR list" to listOf(Makam.AJAM),
                            "ASHEAR NOTES 1936-1940" to listOf(Makam.AJAM),
                            "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.SASGAR),
                            "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.HOSENI),
                            "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.AJAM),
                            "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.HOSENI),
                            "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.SIGAH),
                            "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.AJAM),
                            "MOSHE AMASH (Shami)" to listOf(Makam.BAYAT),
                            "EZRA MASLATON TARAB (Shami)" to listOf(Makam.BAYAT),
                            "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.BAYAT),
                            "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.BUSTANIGAR),
                        )
                    else
                        mapOf(
                            "Eliahou Yaaqob DWECK-KESAR" to listOf(Makam.SIGAH),
                            "ABRAHAM DWECK Pre1920" to listOf(Makam.SIGAH),
                            "IDELSOHN Pre1923" to listOf(Makam.SIGAH),
                            "S SAGIR Laniado" to listOf(Makam.SIGAH),
                            "ASHEAR list" to listOf(Makam.SIGAH),
                            "ASHEAR NOTES 1936-1940" to listOf(Makam.SIGAH),
                            "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.SIGAH),
                            "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.AJAM),
                            "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.SIGAH),
                            "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.BAYAT, Makam.AJAM),
                            "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.AJAM),
                            "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.AJAM),
                            "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.SIGAH),
                            "MOSHE AMASH (Shami)" to listOf(Makam.RAST),
                            "EZRA MASLATON TARAB (Shami)" to listOf(Makam.RAST),
                            "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.RAST),
                            "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.RAST),
                        )
                }
            }

            // Combine the two Parashiot entries into one
            val mergeParashaMap = mapOf(
                Parsha.VAYAKHEL_PEKUDEI to Parsha.VAYAKHEL,
                Parsha.TAZRIA_METZORA to Parsha.TAZRIA,
                Parsha.ACHREI_MOS_KEDOSHIM to Parsha.ACHREI_MOS,
                Parsha.BEHAR_BECHUKOSAI to Parsha.BEHAR,
                Parsha.CHUKAS_BALAK to Parsha.CHUKAS,
                Parsha.MATOS_MASEI to Parsha.MATOS,
                Parsha.NITZAVIM_VAYEILECH to Parsha.NITZAVIM
            )
            val thisWeekParsha = if (mergeParashaMap.contains(jCal.parshah))
                mergeParashaMap[jCal.parshah]
            else
                jCal.parshah

            val data = when (thisWeekParsha) {
                Parsha.BERESHIS ->
                    generateUniformMap(
                        getAllBooks(AllBooks.BERESHIT),
                        listOf(Makam.RAST),
                        mapOf(
                            "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(
                                Makam.RAST,
                                Makam.BAYAT
                            ),
                            "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(
                                Makam.RAST,
                                Makam.BAYAT
                            ),
                            "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.RAST, Makam.BAYAT),
                            "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(
                                Makam.BAYAT,
                                Makam.RAST
                            ),
                            "Victor Afya, Istanbul List" to listOf(Makam.SEGAH),
                            "Izak Alaluf, Izmir List" to listOf(Makam.SEGAH),
                        )
                    )

                Parsha.NOACH ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.IRAQ),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(
                            Makam.IRAQ,
                            Makam.SABA
                        ),
                        "TEBELE Pre1888" to listOf(Makam.IRAQ),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.IRAQ, Makam.SABA),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.BAYAT),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.IRAQ),
                        "ADES: 24793" to listOf(Makam.IRAQ, Makam.SABA),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.IRAQ, Makam.SABA),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.IRAQ, Makam.SIGAH),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.SIGAH),
                        "IDELSOHN Pre1923" to listOf(Makam.IRAQ),
                        "S SAGIR Laniado" to listOf(Makam.BAYAT),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.SIGAH),
                        "ASHEAR list" to listOf(Makam.IRAQ),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.IRAQ, Makam.SIGAH),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.SIGAH),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.IRAQ),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.IRAQ, Makam.BAYAT),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.SIGAH),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.SIGAH),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.HIJAZ),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.HIJAZ),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.BAYAT),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.BAYAT, Makam.SIGAH),
                        "Yishaq Yeranen Halabi" to listOf(Makam.BAYAT),
                        "MOSHE AMASH (Shami)" to listOf(Makam.NAWAH),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.NAWAH),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.NAWAH),
                        "Victor Afya, Istanbul List" to listOf(Makam.HICAZ),
                        "Izak Alaluf, Izmir List" to listOf(Makam.HICAZ),
                        //"Hallel VeZimrah, Salonika, 1928": "Nibah"
                    )
                // Parsha.VZOS_HABERACHA is unused
                Parsha.LECH_LECHA -> generateUniformMap(
                    getAllBooks(AllBooks.BERESHIT),
                    listOf(Makam.SABA),
                    mapOf(
                        "MOSHE AMASH (Shami)" to listOf(Makam.SIGAH, Makam.IRAQ),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.SIGAH, Makam.IRAQ),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.SIGAH),
                        "Victor Afya, Istanbul List" to listOf(Makam.HUSEYNI),
                        "Izak Alaluf, Izmir List" to listOf(Makam.HUSEYNI),
                        "Hallel VeZimrah, Salonika, 1928" to listOf(Makam.NAHOFT),
                    )
                )

                Parsha.VAYERA ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.NAWAH),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(Makam.NAWAH),
                        "TEBELE Pre1888" to listOf(Makam.NAWAH),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.NAWAH),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.NAWAH),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.NAWAH),
                        "ADES: 24793" to listOf(Makam.NAWAH),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.NAWAH),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.NAWAH),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.HOSENI),
                        "IDELSOHN Pre1923" to listOf(Makam.NAWAH),
                        "S SAGIR Laniado" to listOf(Makam.NAWAH),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.BAYAT),
                        "ASHEAR list" to listOf(Makam.NAWAH),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.NAWAH, Makam.RAST),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.BAYAT),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.NAHWAND),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.NAWAH),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(
                            Makam.RAHAWI,
                            Makam.NAWAH
                        ), // Original Pizmonim.com entry: Just Nawah
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.BAYAT),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.HIJAZ),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.NAHWAND),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.NAWAH),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.NAWAH),
                        "Yishaq Yeranen Halabi" to listOf(Makam.NAWAH),
                        "MOSHE AMASH (Shami)" to listOf(Makam.AJAM),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.BAYAT),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.BAYAT),
                        "Victor Afya, Istanbul List" to listOf(Makam.ARABAN),
                        "Izak Alaluf, Izmir List" to listOf(Makam.NIHAVEND),
                        "Hallel VeZimrah, Salonika, 1928" to listOf(Makam.HIJAZ),
                    )

                Parsha.CHAYEI_SARA -> generateUniformMap(
                    getAllBooks(AllBooks.BERESHIT),
                    listOf(Makam.HIJAZ),
                    mapOf(
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.HOSENI),
                        "ADES: 24793" to listOf(Makam.ARAZBAR),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.HIJAZ, Makam.ARAZBAR),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.NAHWAND),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.HIJAZ_KAR),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.HIJAZ_KAR),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.NAHWAND),
                        "MOSHE AMASH (Shami)" to listOf(Makam.SABA),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.SABA),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.SABA),
                        "Victor Afya, Istanbul List" to listOf(Makam.DUGAH),
                        "Izak Alaluf, Izmir List" to listOf(Makam.DUGAH),
                        "Hallel VeZimrah, Salonika, 1928" to listOf(Makam.BUSTANIGAR),
                    )
                )

                Parsha.TOLDOS -> generateUniformMap(
                    getAllBooks(AllBooks.BERESHIT),
                    listOf(Makam.MAHOUR),
                    mapOf(
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.RAST),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.RAST),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.RAST),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.RAST),
                        "MOSHE AMASH (Shami)" to listOf(Makam.RAST),
                        "Victor Afya, Istanbul List" to listOf(Makam.MAHUR),
                        "Izak Alaluf, Izmir List" to listOf(Makam.ARABAN),
                        "Hallel VeZimrah, Salonika, 1928" to listOf(Makam.IRAQ),
                    )
                )

                Parsha.VAYETZEI ->
                    mapOf(
                        //"TABBUSH Ms NLI 8*7622, Aleppo": ["Sharga"],
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(Makam.GIRKA),
                        "TEBELE Pre1888" to listOf(Makam.GIRKA),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.GIRKA),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.GIRKA),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.GIRKA),
                        "ADES: 24793" to listOf(Makam.GIRKA),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.GIRKA, Makam.AJAM),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.SABA),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.SABA),
                        "IDELSOHN Pre1923" to listOf(Makam.GIRKA),
                        "S SAGIR Laniado" to listOf(Makam.AJAM),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.SABA),
                        //"ASHEAR list": ["Sharga"],
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.AJAM),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.SABA),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.GIRKA/*, "Sharga" */),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.AJAM),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.AJAM),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.SABA),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.AJAM),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.AJAM),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.AJAM),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.AJAM),
                        "Yishaq Yeranen Halabi" to listOf(Makam.AJAM),
                        "MOSHE AMASH (Shami)" to listOf(Makam.AJAM),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.AJAM),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.AJAM),
                        "Victor Afya, Istanbul List" to listOf(Makam.NIHAVEND),
                        "Izak Alaluf, Izmir List" to listOf(Makam.BEYATI),
                        "Hallel VeZimrah, Salonika, 1928" to listOf(Makam.SABA),
                    )

                Parsha.VAYISHLACH ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.IRAQ),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(
                            Makam.IRAQ,
                            Makam.SABA
                        ),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.IRAQ, Makam.SABA),
                        "TEBELE Pre1888" to listOf(Makam.IRAQ),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.BAYAT),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.SIGAH),
                        "ADES: 24793" to listOf(Makam.IRAQ, Makam.SABA),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.IRAQ, Makam.SABA),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.IRAQ),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.BAYAT),
                        "IDELSOHN Pre1923" to listOf(Makam.IRAQ),
                        "S SAGIR Laniado" to listOf(Makam.BAYAT),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.HOSENI),
                        "ASHEAR list" to listOf(Makam.IRAQ),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.SIGAH, Makam.IRAQ),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.HOSENI),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.IRAQ),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.SABA, Makam.SIGAH),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.SABA, Makam.SIGAH),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.SIGAH),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.SABA),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.SABA),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.BAYAT),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.BAYAT, Makam.SIGAH),
                        "Yishaq Yeranen Halabi" to listOf(Makam.BAYAT),
                        "MOSHE AMASH (Shami)" to listOf(Makam.BAYAT),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.BAYAT),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.BAYAT),
                        "Victor Afya, Istanbul List" to listOf(Makam.ACEM_ASHIRAN),
                        "Izak Alaluf, Izmir List" to listOf(Makam.ACEM_ASHIRAN),
                        "Hallel VeZimrah, Salonika, 1928" to listOf(Makam.HUZAM),
                    )

                Parsha.VAYESHEV ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.RAHAWI),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(Makam.RAHAWI),
                        "TEBELE Pre1888" to listOf(Makam.RAHAWI),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.RAHAWI),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.RAHAWI),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.RAHAWI),
                        "ADES: 24793" to listOf(Makam.RAHAWI),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.RAHAWI),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.RAHAWI),
                        "IDELSOHN Pre1923" to listOf(Makam.RAHAWI),
                        "S SAGIR Laniado" to listOf(Makam.NAWAH),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.RAHAWI),
                        "ASHEAR list" to listOf(Makam.RAHAWI),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.NAWAH, Makam.NAHWAND),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.RAHAWI, Makam.NAHWAND),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.RAHAWI),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.RAHAWI),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.RAHAWI, Makam.NAHWAND),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.NAWAH),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.NAHWAND),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.NAHWAND),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.NAWAH),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.RAHAWI, Makam.NAWAH),
                        "Yishaq Yeranen Halabi" to listOf(Makam.NAWAH),
                        "MOSHE AMASH (Shami)" to listOf(Makam.NAWAH),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.NAWAH),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.NAWAH),
                        "Victor Afya, Istanbul List" to listOf(Makam.USSAK),
                        "Izak Alaluf, Izmir List" to listOf(Makam.ISFAHAN),
                        "Hallel VeZimrah, Salonika, 1928" to listOf(Makam.SIGAH),
                    )

                Parsha.MIKETZ -> generateUniformMap(
                    getAllBooks(AllBooks.BERESHIT) - "Victor Afya, Istanbul List",
                    listOf(Makam.SIGAH),
                    mapOf(
                        "MOSHE AMASH (Shami)" to listOf(Makam.RAHAWI),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.RAHAWI),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.RAHAWI),
                        "Izak Alaluf, Izmir List" to listOf(Makam.USSAK),
                        "Hallel VeZimrah, Salonika, 1928" to listOf(Makam.USSAK),
                    )
                )

                Parsha.VAYIGASH ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.BAYAT),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(Makam.BAYAT),
                        "TEBELE Pre1888" to listOf(Makam.BAYAT),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(
                            Makam.IRAQ,
                            Makam.SABA,
                            Makam.BAYAT,
                            Makam.RAST
                        ),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.SABA),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.BAYAT),
                        "ADES: 24793" to listOf(Makam.BAYAT),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.BAYAT),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.SABA),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.SABA),
                        "IDELSOHN Pre1923" to listOf(Makam.BAYAT),
                        "S SAGIR Laniado" to listOf(Makam.SABA),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.MAHOUR),
                        "ASHEAR list" to listOf(Makam.BAYAT),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.BAYAT),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.MAHOUR),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.BAYAT),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.BAYAT, Makam.SABA),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.BAYAT),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.BAYAT),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.BAYAT),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.BAYAT),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.BAYAT),
                        "Yishaq Yeranen Halabi" to listOf(Makam.BAYAT, Makam.SABA),
                        "MOSHE AMASH (Shami)" to listOf(Makam.SIGAH),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.SIGAH),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.SIGAH),
                        "Victor Afya, Istanbul List" to listOf(Makam.SEGAH),
                        "Izak Alaluf, Izmir List" to listOf(Makam.NIHAVEND),
                        "Hallel VeZimrah, Salonika, 1928" to listOf(Makam.MUHAYER),
                    )

                Parsha.VAYECHI -> generateUniformMap(
                    getAllBooks(AllBooks.BERESHIT),
                    listOf(Makam.HIJAZ),
                    mapOf(
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.HIJAZ, Makam.SABA),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.NAHWAND),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.SHURI),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.NAHWAND),
                        "MOSHE AMASH (Shami)" to listOf(Makam.SABA),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.SABA),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.SABA),
                        "Victor Afya, Istanbul List" to listOf(Makam.BEYATI),
                        "Izak Alaluf, Izmir List" to listOf(Makam.CARGAH),
                        "Hallel VeZimrah, Salonika, 1928" to listOf(Makam.AJAM),
                    )
                )

                Parsha.SHEMOS ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.RAST),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(
                            Makam.BAYAT,
                            Makam.RAST
                        ),
                        "TEBELE Pre1888" to listOf(Makam.BAYAT),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.BAYAT, Makam.RAST),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.BAYAT),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.RAST),
                        "ADES: 24793" to listOf(Makam.BAYAT, Makam.RAST),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.BAYAT, Makam.RAST),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.RAST),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.RAST),
                        "IDELSOHN Pre1923" to listOf(Makam.RAST),
                        "S SAGIR Laniado" to listOf(Makam.BAYAT),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.HOSENI),
                        "ASHEAR list" to listOf(Makam.BAYAT, Makam.RAST),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.BAYAT, Makam.RAST),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.HOSENI),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.BAYAT, Makam.RAST),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.BAYAT, Makam.RAST),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.BAYAT, Makam.RAST),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.RAST),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.RAST),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.RAST),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.RAST),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.BAYAT, Makam.RAST),
                        "Yishaq Yeranen Halabi" to listOf(Makam.BAYAT),
                        "MOSHE AMASH (Shami)" to listOf(Makam.RAST),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.RAST),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.RAST),
                        "Victor Afya, Istanbul List" to listOf(Makam.NIHAVEND),
                        "Izak Alaluf, Izmir List" to listOf(Makam.NIHAVEND),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.BAYATI),
                    )

                Parsha.VAERA ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.SABA),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(
                            Makam.HOSENI,
                            Makam.NAWAH
                        ),
                        "TEBELE Pre1888" to listOf(Makam.HOSENI),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(
                            Makam.IRAQ,
                            Makam.HOSENI,
                            Makam.NAWAH
                        ),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.IRAQ, Makam.HOSENI),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.SIGAH),
                        "ADES: 24793" to listOf(Makam.HOSENI, Makam.NAWAH),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.HOSENI, Makam.NAWAH),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.BAYAT),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.BAYAT),
                        "IDELSOHN Pre1923" to listOf(Makam.HOSENI, Makam.NAWAH),
                        "S SAGIR Laniado" to listOf(Makam.SIGAH),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.SABA),
                        "ASHEAR list" to listOf(Makam.SIGAH),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.NAHWAND, Makam.BAYAT),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.SABA),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.SIGAH),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(
                            Makam.HOSENI,
                            Makam.SIGAH
                        ),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.HOSENI, Makam.RAST),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.BAYAT),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.HOSENI),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.BAYAT),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.SIGAH),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.NAWAH, Makam.HOSENI),
                        "Yishaq Yeranen Halabi" to listOf(Makam.NAWAH),
                        "MOSHE AMASH (Shami)" to listOf(Makam.BAYAT),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.BAYAT),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.BAYAT),
                        "Victor Afya, Istanbul List" to listOf(Makam.HICAZ),
                        "Izak Alaluf, Izmir List" to listOf(Makam.HICAZ),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.BUSALIQ),
                    )

                Parsha.BO ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.IRAQ),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(
                            Makam.IRAQ,
                            Makam.RAST
                        ),
                        "TEBELE Pre1888" to listOf(Makam.IRAQ),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.IRAQ, Makam.RAST),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.SIGAH, Makam.RAST),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.BAYAT),
                        "ADES: 24793" to listOf(Makam.IRAQ, Makam.RAST),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.IRAQ, Makam.RAST),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.SABA),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.RAHAWI),
                        "IDELSOHN Pre1923" to listOf(Makam.IRAQ),
                        "S SAGIR Laniado" to listOf(Makam.RAST),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.BAYAT),
                        "ASHEAR list" to listOf(Makam.RAST),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.NAWAH, Makam.NAHWAND),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.NAHWAND),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.RAST),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.SIGAH, Makam.RAST),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.SIGAH),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.SIGAH),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.SIGAH),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.SIGAH),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.NAWAH),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.SIGAH),
                        "Yishaq Yeranen Halabi" to listOf(Makam.SIGAH),
                        "MOSHE AMASH (Shami)" to listOf(Makam.RAST),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.RAHAWI),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.NAWAH),
                        "Victor Afya, Istanbul List" to listOf(Makam.HUSEYNI),
                        "Izak Alaluf, Izmir List" to listOf(Makam.HUSEYNI),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.QARGIGAR),
                    )

                Parsha.BESHALACH -> generateUniformMap(
                    getAllBooks(AllBooks.SHEMOT),
                    listOf(Makam.AJAM),
                    mapOf(
                        "Victor Afya, Istanbul List" to listOf(Makam.ACEM_ASHIRAN),
                        "Izak Alaluf, Izmir List" to listOf(Makam.ACEM_ASHIRAN),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.Q_FARAHNAQ)
                    )
                )

                Parsha.YISRO -> generateUniformMap(
                    getAllBooks(AllBooks.SHEMOT),
                    listOf(Makam.HOSENI),
                    mapOf(
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.Q_HOSENI),
                        "IDELSOHN Pre1923" to listOf(Makam.SIGAH),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.BAYAT),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.KURD),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(
                            Makam.KURD,
                            Makam.HOSENI
                        ),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.BAYAT),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.HOSENI, Makam.SIGAH),
                        "MOSHE AMASH (Shami)" to listOf(Makam.BAYAT),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.BAYAT),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.BAYAT),
                        "Victor Afya, Istanbul List" to listOf(Makam.MAHUR, Makam.SEHNAZ),
                        "Izak Alaluf, Izmir List" to listOf(Makam.MAHUR),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.RAST),
                    )
                )

                Parsha.MISHPATIM ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.NAWAH),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(
                            Makam.RAST,
                            Makam.SABA
                        ),
                        "TEBELE Pre1888" to listOf(Makam.SABA),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.NAWAH, Makam.SABA),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.NAWAH, Makam.SABA),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.NAWAH),
                        "ADES: 24793" to listOf(Makam.SABA),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.SABA),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.NAWAH),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.NAHWAND),
                        "IDELSOHN Pre1923" to listOf(Makam.SABA),
                        "S SAGIR Laniado" to listOf(Makam.NAWAH),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.RAST, Makam.SABA),
                        "ASHEAR list" to listOf(Makam.NAWAH),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.BAYAT, Makam.NAWAH),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.RAST),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.NAHWAND),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.SABA),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.SABA),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.SABA),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.SABA, Makam.ISFAHAN),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.SABA),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.RAST),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.SABA),
                        "Yishaq Yeranen Halabi" to listOf(Makam.SABA),
                        "MOSHE AMASH (Shami)" to listOf(Makam.SIGAH),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.SIGAH),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.SIGAH),
                        "Victor Afya, Istanbul List" to listOf(Makam.ARABAN),
                        "Izak Alaluf, Izmir List" to listOf(Makam.BEYATI),
                        //"Hallel VeZimrah, Greece List, 1926": "Nibah"
                    )

                Parsha.TERUMAH -> generateUniformMap(
                    getAllBooks(AllBooks.SHEMOT),
                    listOf(Makam.SABA),
                    mapOf(
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.SABA, Makam.RAST),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.SABA, Makam.RAST),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.SABA, Makam.MOUHAYAR),
                        "IDELSOHN Pre1923" to listOf(Makam.SIGAH),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(
                            Makam.SABA,
                            Makam.HOSENI
                        ),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.BAYAT),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.BAYAT, Makam.SABA),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.HOSENI),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.RAST),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.BAYAT, Makam.HOSENI),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.BAYAT),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.BAYAT, Makam.SIGAH),
                        "Yishaq Yeranen Halabi" to listOf(Makam.BAYAT),
                        "MOSHE AMASH (Shami)" to listOf(Makam.RAST),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.BAYAT),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.BAYAT),
                        "Victor Afya, Istanbul List" to listOf(Makam.BEYATI),
                        "Izak Alaluf, Izmir List" to listOf(Makam.SEGAH),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.NAHWAND),
                    )
                )

                Parsha.TETZAVEH -> generateUniformMap(
                    getAllBooks(AllBooks.SHEMOT),
                    listOf(Makam.SABA),
                    mapOf(
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.SABA, Makam.RAST),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.SABA, Makam.RAST),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.SABA, Makam.MOUHAYAR),
                        "IDELSOHN Pre1923" to listOf(Makam.SIGAH),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(
                            Makam.SABA,
                            Makam.HOSENI
                        ),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.BAYAT),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.BAYAT, Makam.SABA),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.HOSENI),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.RAST),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.BAYAT, Makam.HOSENI),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.BAYAT),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.BAYAT, Makam.SIGAH),
                        "Yishaq Yeranen Halabi" to listOf(Makam.BAYAT),
                        "MOSHE AMASH (Shami)" to listOf(Makam.RAST),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.BAYAT),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.BAYAT),
                        "Victor Afya, Istanbul List" to listOf(Makam.BEYATI),
                        "Izak Alaluf, Izmir List" to listOf(Makam.SEGAH),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.NAHWAND),
                    )
                )

                Parsha.KI_SISA -> generateUniformMap(
                    getAllBooks(AllBooks.SHEMOT) - "Hallel VeZimrah, Greece List, 1926",
                    listOf(Makam.HIJAZ),
                    mapOf(
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.NAHWAND),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.SHURI),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.NAHWAND),
                        "MOSHE AMASH (Shami)" to listOf(Makam.SABA),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.SABA),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.SABA),
                        "Victor Afya, Istanbul List" to listOf(Makam.HICAZ),
                        "Izak Alaluf, Izmir List" to listOf(Makam.ARABAN),
                        //"Hallel VeZimrah, Greece List, 1926": "Haqaqordo?"
                    )
                )

                Parsha.VAYAKHEL -> generateUniformMap(
                    getAllBooks(AllBooks.SHEMOT)
                            - "M H Elias, SHIR HADASH, Jerusalem, 1930"
                            - "YOSEF YEHEZKEL Jerusalem 1975"
                            - "ABRAHAM SHAMRICHA (Shami)"
                            - "Victor Afya, Istanbul List"
                            - "Izak Alaluf, Izmir List",
                    listOf(Makam.HOSENI),

                    mapOf(
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.SABA),
                        "S SAGIR Laniado" to listOf(Makam.BAYAT),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.BAYAT),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.BAYAT),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.BAYAT),
                        "MOSHE AMASH (Shami)" to listOf(Makam.AJAM),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.MAHOUR),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.BUSTANIGAR),
                    )
                )

                Parsha.PEKUDEI -> generateUniformMap(
                    getAllBooks(AllBooks.SHEMOT)
                            - "YOSEF YEHEZKEL Jerusalem 1975"
                            - "Victor Afya, Istanbul List"
                            - "Izak Alaluf, Izmir List",
                    listOf(Makam.NAWAH),
                    mapOf(
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.RAST, Makam.SABA),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.SABA),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.NAHWAND),
                        //"GABRIEL A SHREM 1964 SUHV" to listOf(Makam.NAWAH), // Artscroll: Nawah/Nahwand; Shir Ushbacha: Rahaw/Nawah
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.BAYAT),
                        "MOSHE AMASH (Shami)" to listOf(Makam.BAYAT),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.BAYAT),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.BAYAT),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.HUZAM),
                    )
                )

                Parsha.VAYIKRA ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.RAST),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(Makam.RAST),
                        "TEBELE Pre1888" to listOf(Makam.RAST),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.RAST),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.RAST),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.RAST),
                        "ADES: 24793" to listOf(Makam.RAST),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.RAST),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.RAST),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.RAST),
                        "IDELSOHN Pre1923" to listOf(Makam.RAST),
                        "S SAGIR Laniado" to listOf(Makam.RAST),
                        "ASHEAR list" to listOf(Makam.RAST),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.RAST),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.RAST),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.RAST),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.RAST),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.RAST),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.RAST),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.RAST),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.RAST),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.RAST),
                        "Yishaq Yeranen Halabi" to listOf(Makam.RAST),
                        "MOSHE AMASH (Shami)" to listOf(Makam.RAST),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.RAST),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.RAST),
                        "Victor Afya, Istanbul List" to listOf(Makam.SEBAH),
                        "Izak Alaluf, Izmir List" to listOf(Makam.HICAZ),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.SABA),
                    )

                Parsha.TZAV ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.IRAQ),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(Makam.IRAQ),
                        "TEBELE Pre1888" to listOf(Makam.IRAQ),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.RAHAWI, Makam.IRAQ),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.RAHAWI, Makam.IRAQ),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.RAHAWI),
                        "ADES: 24793" to listOf(Makam.IRAQ),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.IRAQ),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.RAHAWI),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.RAHAWI),
                        "IDELSOHN Pre1923" to listOf(Makam.IRAQ),
                        "S SAGIR Laniado" to listOf(Makam.NAWAH),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.RAHAWI),
                        "ASHEAR list" to listOf(Makam.RAHAWI),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.BAYAT),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.RAHAWI),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.IRAQ),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.RAHAWI, Makam.NAWAH),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.BAYAT),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.IRAQ),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.NAHWAND),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.SIGAH),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.NAWAH),
                        "Yishaq Yeranen Halabi" to listOf(Makam.NAWAH),
                        "MOSHE AMASH (Shami)" to listOf(Makam.NAWAH),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.BAYAT),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.BAYAT),
                        "Victor Afya, Istanbul List" to listOf(Makam.ISFAHAN, Makam.ACEM_ASHIRAN),
                        "Izak Alaluf, Izmir List" to listOf(Makam.SEGAH),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.Q_HOSENI)
                    )

                Parsha.SHMINI ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.HOSENI),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(Makam.HOSENI),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.HOSENI),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.HOSENI),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.HOSENI),
                        "ADES: 24793" to listOf(Makam.HOSENI),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.HOSENI),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.HOSENI),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.HOSENI),
                        "IDELSOHN Pre1923" to listOf(Makam.HOSENI),
                        "S SAGIR Laniado" to listOf(Makam.HOSENI),
                        "ASHEAR list" to listOf(Makam.HOSENI),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.NAHWAND),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.HOSENI),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.HOSENI),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.HOSENI),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.RAST),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.HOSENI),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.HOSENI),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.BAYAT),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.HOSENI),
                        "Yishaq Yeranen Halabi" to listOf(Makam.HOSENI),
                        "MOSHE AMASH (Shami)" to listOf(Makam.SABA),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.SABA),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.SABA),
                        "Victor Afya, Istanbul List" to listOf(Makam.HICAZ),
                        "Izak Alaluf, Izmir List" to listOf(Makam.HICAZ),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.BAYATI),
                    )

                Parsha.TAZRIA ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.BAYAT),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(Makam.BAYAT),
                        "TEBELE Pre1888" to listOf(Makam.BAYAT),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.BAYAT),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.BAYAT),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.MEHAYAR),
                        "ADES: 24793" to listOf(Makam.BAYAT),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.BAYAT),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.BAYAT),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.BAYAT),
                        "IDELSOHN Pre1923" to listOf(Makam.BAYAT),
                        "S SAGIR Laniado" to listOf(Makam.SIGAH),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.BAYAT),
                        "ASHEAR list" to listOf(Makam.SIGAH),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.BAYAT, Makam.SIGAH),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.SIGAH),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.BAYAT, Makam.RAST),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.BAYAT, Makam.SABA),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.BAYAT),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.MEHAYAR),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.SIGAH),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.SABA),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.BAYAT, Makam.SABA),
                        "Yishaq Yeranen Halabi" to listOf(Makam.SABA),
                        "MOSHE AMASH (Shami)" to listOf(Makam.BAYAT),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.SIGAH),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.BAYAT),
                        "Victor Afya, Istanbul List" to listOf(Makam.MAHUR),
                        "Izak Alaluf, Izmir List" to listOf(Makam.ACEM_ASHIRAN),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.SUZNIQ),
                    )

                Parsha.METZORA ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.IRAQ),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(Makam.IRAQ),
                        "TEBELE Pre1888" to listOf(Makam.IRAQ),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.RAHAWI),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.NAWAH),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.SIGAH),
                        "ADES: 24793" to listOf(Makam.IRAQ),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.IRAQ),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.RAHAWI),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.NAHWAND),
                        "IDELSOHN Pre1923" to listOf(Makam.IRAQ),
                        "S SAGIR Laniado" to listOf(Makam.NAWAH),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.NAHWAND, Makam.SIGAH),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.SIGAH),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.SABA, Makam.SIGAH),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.NAHWAND, Makam.SIGAH),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.SIGAH),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.RAST),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.NAWAH),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.SABA, Makam.SIGAH),
                        "Yishaq Yeranen Halabi" to listOf(Makam.SABA),
                        "MOSHE AMASH (Shami)" to listOf(Makam.RAST),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.BAYAT),
                        "Izak Alaluf, Izmir List" to listOf(Makam.SEGAH),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.Q_SIGAH)
                    )

                Parsha.ACHREI_MOS -> generateUniformMap(
                    getAllBooks(AllBooks.VAYIKRA)
                            - "Izak Alaluf, Izmir List"
                            - "Victor Afya, Istanbul List",
                    listOf(Makam.HIJAZ),
                    mapOf(
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.NAHWAND),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.NAHWAND),
                        "MOSHE AMASH (Shami)" to listOf(Makam.SABA),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.SABA),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.BAYAT),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.NAHOFT),
                    )
                )

                Parsha.KEDOSHIM ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.SABA),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(Makam.SABA),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.IRAQ, Makam.SABA),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.IRAQ),
                        "ADES: 24793" to listOf(Makam.SABA),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.SABA),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.SABA),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.SABA),
                        "IDELSOHN Pre1923" to listOf(Makam.SABA),
                        "S SAGIR Laniado" to listOf(Makam.SABA),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.RAST),
                        "ASHEAR list" to listOf(Makam.IRAQ),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.SABA),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.IRAQ),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.SABA),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.SABA),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.RAST),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.SABA),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.SABA),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.BAYAT),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.SABA),
                        "Yishaq Yeranen Halabi" to listOf(Makam.SABA),
                        "MOSHE AMASH (Shami)" to listOf(Makam.AJAM),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.SIGAH),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.SIGAH),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.HIJAZ),
                    )

                Parsha.EMOR ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.ASHIRAN),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(Makam.ASHIRAN),
                        "TEBELE Pre1888" to listOf(Makam.ASHIRAN),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.ASHIRAN, Makam.SABA),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.ASHIRAN),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.ASHIRAN),
                        "ADES: 24793" to listOf(Makam.ASHIRAN),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.ASHIRAN),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.ASHIRAN),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.SIGAH),
                        "IDELSOHN Pre1923" to listOf(Makam.ASHIRAN),
                        "S SAGIR Laniado" to listOf(Makam.ASHIRAN),
                        "ASHEAR list" to listOf(Makam.ASHIRAN),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.SIGAH, Makam.HOSENI),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.ASHIRAN),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(
                            Makam.SIGAH,
                            Makam.HOSENI
                        ),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.SIGAH, Makam.HOSENI),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.SIGAH),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.OJ),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.HOSENI),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.SIGAH),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.SIGAH, Makam.HOSENI),
                        "Yishaq Yeranen Halabi" to listOf(Makam.SIGAH),
                        "MOSHE AMASH (Shami)" to listOf(Makam.RAST),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.RAST),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.RAST),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.BUSALIQ),
                    )

                Parsha.BEHAR ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.NAWAH),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(Makam.NAWAH),
                        "TEBELE Pre1888" to listOf(Makam.NAWAH),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.NAWAH),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.SABA),
                        "ADES: 24793" to listOf(Makam.NAWAH),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.NAWAH),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.NAWAH),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.NAWAH),
                        "IDELSOHN Pre1923" to listOf(Makam.NAWAH),
                        "S SAGIR Laniado" to listOf(Makam.NAWAH),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.SABA),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.NAWAH),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.SABA),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.NAWAH),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.NAWAH, Makam.SABA),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.BAYAT),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.NAHWAND),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.NAWAH),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.NAWAH),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.NAWAH, Makam.SABA),
                        "Yishaq Yeranen Halabi" to listOf(Makam.NAWAH),
                        "MOSHE AMASH (Shami)" to listOf(Makam.SIGAH),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.AJAM),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.Q_QARGIGAR)
                    )

                Parsha.BECHUKOSAI ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.NAWAH),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(Makam.NAWAH),
                        "TEBELE Pre1888" to listOf(Makam.NAWAH),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.NAWAH),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.NAWAH),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.NAWAH),
                        "ADES: 24793" to listOf(Makam.NAWAH),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.RAHAWI),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.BAYAT),
                        "IDELSOHN Pre1923" to listOf(Makam.NAWAH, Makam.BAYAT),
                        "S SAGIR Laniado" to listOf(Makam.NAHWAND),
                        "ASHEAR list" to listOf(Makam.NAWAH),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.NAHWAND),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.BAYAT),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.NAHWAND), // Original pizmonim.com: Nawah/Nahwand
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.NAHWAND),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.KURD),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.SABA),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.RAST),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.NAWAH, Makam.BAYAT),
                        "Yishaq Yeranen Halabi" to listOf(Makam.BAYAT),
                        "MOSHE AMASH (Shami)" to listOf(Makam.BAYAT),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.AJAM),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.IRAQ),
                    )

                Parsha.BAMIDBAR ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.MEHAYAR),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(Makam.MEHAYAR),
                        "TEBELE Pre1888" to listOf(Makam.MEHAYAR),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.NAWAH),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.RAST),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.RAST),
                        "ADES: 24793" to listOf(Makam.MEHAYAR),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.MEHAYAR, Makam.RAST),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.RAST),
                        "IDELSOHN Pre1923" to listOf(Makam.MEHAYAR),
                        "S SAGIR Laniado" to listOf(Makam.RAST),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.RAST),
                        "ASHEAR list" to listOf(Makam.MEHAYAR),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.HOSENI, Makam.RAST),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.RAST),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.MEHAYAR),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.SABA, Makam.RAST),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.RAST),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.RAST),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.RAST),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.RAST),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.BAYAT),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.HOSENI, Makam.RAST),
                        "Yishaq Yeranen Halabi" to listOf(Makam.BAYAT),
                        "MOSHE AMASH (Shami)" to listOf(Makam.RAST, Makam.AJAM),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.RAST),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.RAST),
                        //"Hallel VeZimrah, Greece List, 1926": "Haqqordo?"
                    )

                Parsha.NASSO -> generateUniformMap(
                    getAllBooks(AllBooks.BAMIDBAR) - "M H Elias, SHIR HADASH, Jerusalem, 1930",
                    listOf(Makam.SABA),
                    mapOf(
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.HOSENI, Makam.SABA),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(
                            Makam.RAST,
                            Makam.SABA
                        ), // Original Pizmonim.com: Saba
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.ZANGIRAN),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.HOSENI),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.SABA, Makam.RAST),
                        "Yishaq Yeranen Halabi" to listOf(Makam.HOSENI, Makam.SABA),
                        "MOSHE AMASH (Shami)" to listOf(Makam.SIGAH),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.BAYAT),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.BAYAT),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.HIJAZ),
                    )
                )

                Parsha.BEHAALOSCHA ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.HOSENI),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(Makam.OJ),
                        "TEBELE Pre1888" to listOf(Makam.SIGAH),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.SIGAH, Makam.HOSENI),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.SIGAH),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.SIGAH),
                        "ADES: 24793" to listOf(Makam.OJ),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.HOSENI),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.SIGAH),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.SIGAH),
                        "IDELSOHN Pre1923" to listOf(Makam.SIGAH),
                        "S SAGIR Laniado" to listOf(Makam.SIGAH),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.SIGAH),
                        "ASHEAR list" to listOf(Makam.SIGAH),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.SIGAH),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.SIGAH),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.SIGAH),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.SIGAH),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.SIGAH),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.SIGAH),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.SIGAH),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.SIGAH),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.SIGAH),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.SIGAH),
                        "Yishaq Yeranen Halabi" to listOf(Makam.SIGAH),
                        "MOSHE AMASH (Shami)" to listOf(Makam.NAWAH),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.RAHAWI),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.NAWAH),
                        // "Hallel VeZimrah, Greece List, 1926": "Ushaq"
                    )

                Parsha.SHLACH ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.IRAQ),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(
                            Makam.IRAQ,
                            Makam.NAWAH
                        ),
                        "TEBELE Pre1888" to listOf(Makam.IRAQ),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.IRAQ, Makam.NAWAH),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.NAWAH),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.HIJAZ),
                        "ADES: 24793" to listOf(Makam.IRAQ, Makam.NAWAH),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.IRAQ),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.IRAQ),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.HIJAZ),
                        "IDELSOHN Pre1923" to listOf(Makam.IRAQ),
                        "S SAGIR Laniado" to listOf(Makam.NAWAH),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.HIJAZ),
                        "ASHEAR list" to listOf(Makam.IRAQ),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.HIJAZ),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.HIJAZ),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.IRAQ),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.IRAQ, Makam.NAWAH),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.HIJAZ),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.NAHWAND),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.SHURI),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.NAWAH),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.NAHWAND),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.HIJAZ, Makam.NAHWAND),
                        "Yishaq Yeranen Halabi" to listOf(Makam.NAHWAND),
                        "MOSHE AMASH (Shami)" to listOf(Makam.SABA),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.SABA),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.SABA),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.HUZAM),
                    )

                Parsha.KORACH ->
                    mapOf(
                        //"TABBUSH Ms NLI 8*7622, Aleppo": "Ḥoseni combined",
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(Makam.HOSENI),
                        "TEBELE Pre1888" to listOf(Makam.HOSENI),
                        //"ELIE SHAUL COHEN FROM AINTAB, ~1880": "Ḥoseni Bayat combined",
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.HOSENI),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.HOSENI),
                        "ADES: 24793" to listOf(Makam.HOSENI),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.HOSENI),
                        //"Knis Betesh Geniza List, Aleppo": "Ḥoseni combined",
                        //"ABRAHAM DWECK Pre1920": "Ḥoseni combined",
                        "IDELSOHN Pre1923" to listOf(Makam.HOSENI),
                        "S SAGIR Laniado" to listOf(Makam.HOSENI),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.HOSENI),
                        "ASHEAR list" to listOf(Makam.HOSENI),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.BAYAT),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.NAHWAND),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.HOSENI),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.HOSENI),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.NAHWAND),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.BAYAT),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.HOSENI),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.NAHWAND),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.BAYAT),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.NAHWAND, Makam.HOSENI),
                        "Yishaq Yeranen Halabi" to listOf(Makam.HOSENI),
                        "MOSHE AMASH (Shami)" to listOf(Makam.BAYAT),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.SIGAH),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.SIGAH),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.IRAQ),
                    )

                Parsha.CHUKAS ->
                    mapOf(
                        //"TABBUSH Ms NLI 8*7622, Aleppo": "Ḥoseni combined",
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(Makam.HOSENI),
                        "TEBELE Pre1888" to listOf(Makam.HOSENI),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.BAYAT),
                        "ADES: 24793" to listOf(Makam.HOSENI),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.RAST),
                        "IDELSOHN Pre1923" to listOf(Makam.RAST),
                        "S SAGIR Laniado" to listOf(Makam.MAHOUR),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.ASHIRAN),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.BAYAT),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.HOSENI),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.HOSENI),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.RAST),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.HOSENI),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.AJAM),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.RAST),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.RAST, Makam.HOSENI),
                        "Yishaq Yeranen Halabi" to listOf(Makam.RAST),
                        "MOSHE AMASH (Shami)" to listOf(Makam.AJAM),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.BAYAT),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.AJAM),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.SUZNIQ),
                    )

                Parsha.BALAK ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.BAYAT),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(Makam.BAYAT),
                        "TEBELE Pre1888" to listOf(Makam.BAYAT),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.BAYAT),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.RAST),
                        "ADES: 24793" to listOf(Makam.BAYAT),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.BAYAT),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.BAYAT),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.BAYAT),
                        "IDELSOHN Pre1923" to listOf(Makam.BAYAT),
                        "S SAGIR Laniado" to listOf(Makam.BAYAT),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.RAST),
                        "ASHEAR list" to listOf(Makam.NAHWAND),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.NAHWAND),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.MAHOUR),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.NAHWAND),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.BAYAT),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.MAHOUR),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.BAYAT),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.BAYAT),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.RAST),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.BAYAT, Makam.MAHOUR),
                        "Yishaq Yeranen Halabi" to listOf(Makam.BAYAT),
                        "MOSHE AMASH (Shami)" to listOf(Makam.SIGAH),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.BAYAT),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.BAYAT),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.SIGAH),
                    )

                Parsha.PINCHAS -> generateUniformMap(
                    getAllBooks(AllBooks.BAMIDBAR),
                    listOf(Makam.SABA),
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.BAYAT),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.SABA, Makam.BAYAT),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.BUSTANIGAR),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.MUHAYER),
                    )
                )

                Parsha.MATOS ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.NAWAH),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(Makam.NAWAH),
                        "TEBELE Pre1888" to listOf(Makam.NAWAH),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.NAWAH),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.NAWAH),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.BAYAT),
                        "ADES: 24793" to listOf(Makam.NAWAH),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.NAWAH),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.NAWAH),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.NAWAH),
                        "IDELSOHN Pre1923" to listOf(Makam.NAWAH),
                        "S SAGIR Laniado" to listOf(Makam.NAWAH, Makam.NAHWAND),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.RAHAWI),
                        "ASHEAR list" to listOf(Makam.NAWAH),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.BAYAT),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.NAHWAND),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.NAHWAND),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.NAWAH),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(
                            Makam.RAHAWI,
                            Makam.NAHWAND
                        ),// Pizmonim.com[Makam.NAWAH, Makam.NAHWAND),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.SABA),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.NAHWAND),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.SABA),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.NAWAH),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.NAWAH, Makam.NAHWAND),
                        "Yishaq Yeranen Halabi" to listOf(Makam.NAWAH),
                        "MOSHE AMASH (Shami)" to listOf(Makam.RAST),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.MAHOUR),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.BAYAT),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.SABA),
                    )

                Parsha.MASEI ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.SABA),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(Makam.SABA),
                        "TEBELE Pre1888" to listOf(Makam.SABA),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.SABA),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.NAHWAND),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.NAHWAND),
                        "ADES: 24793" to listOf(Makam.SABA),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.MEHAYAR),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.SABA),
                        "IDELSOHN Pre1923" to listOf(Makam.SABA),
                        "S SAGIR Laniado" to listOf(Makam.NAWAH, Makam.NAHWAND),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.SABA),
                        "ASHEAR list" to listOf(Makam.SABA),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.NAWAH),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.SABA),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.SABA),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.SABA),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.SABA),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.NAHWAND),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.SABA, Makam.ISFAHAN),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.NAHWAND),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.NAHWAND),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.BAYAT, Makam.SABA),
                        "Yishaq Yeranen Halabi" to listOf(Makam.SABA),
                        "MOSHE AMASH (Shami)" to listOf(Makam.BAYAT),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.BAYAT),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.BAYAT),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.NAHWAND),
                    )

                Parsha.DEVARIM -> generateUniformMap(
                    getAllBooks(AllBooks.DEVARIM),
                    listOf(Makam.HIJAZ),
                    mapOf(
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.ZANGIRAN),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.AJAM),
                    )
                )

                Parsha.VAESCHANAN -> generateUniformMap(
                    getAllBooks(AllBooks.DEVARIM),
                    listOf(Makam.HOSENI),
                    mapOf(
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(
                            Makam.HOSENI,
                            Makam.ASHIRAN
                        ),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.ASHIRAN),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.ASHIRAN),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.BAYAT),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.BAYAT),
                        "MOSHE AMASH (Shami)" to listOf(Makam.RAST),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.RAST),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.RAST),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.RAST),
                    )
                )

                Parsha.EIKEV -> generateUniformMap(
                    getAllBooks(AllBooks.DEVARIM),
                    listOf(Makam.SIGAH),
                    mapOf(
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.RAST),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.RAST),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.SIGAH, Makam.IRAQ),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.RAST),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.SIGAH, Makam.IRAQ),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.QARGIGAR),
                    )
                )

                Parsha.REEH ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.IRAQ),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(
                            Makam.IRAQ,
                            Makam.RAST
                        ),
                        "TEBELE Pre1888" to listOf(Makam.ASHIRAN),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.IRAQ),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.RAST),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.ASHIRAN),
                        "ADES: 24793" to listOf(Makam.IRAQ),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.IRAQ),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.SABA),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.SABA),
                        "IDELSOHN Pre1923" to listOf(Makam.IRAQ),
                        "S SAGIR Laniado" to listOf(Makam.NAHWAND),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.NAWAH),
                        "ASHEAR list" to listOf(Makam.ASHIRAN),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.BAYAT),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.BAYAT),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.ASHIRAN),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.SIGAH, Makam.RAST),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.RAST),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.RAST),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.RAST),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.RAST),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.BAYAT),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.RAST),
                        "Yishaq Yeranen Halabi" to listOf(Makam.RAST),
                        "MOSHE AMASH (Shami)" to listOf(Makam.BAYAT),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.NAWAH),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.NAWAH),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.Q_HOSENI)
                    )

                Parsha.SHOFTIM ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.SABA),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(
                            Makam.SABA,
                            Makam.SIGAH
                        ),
                        "TEBELE Pre1888" to listOf(Makam.SABA),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.SIGAH, Makam.SABA),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.SABA),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.AJAM),
                        "ADES: 24793" to listOf(Makam.SABA, Makam.SIGAH),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.SASGAR),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.AJAM),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.AJAM),
                        "IDELSOHN Pre1923" to listOf(Makam.SIGAH),
                        "S SAGIR Laniado" to listOf(Makam.AJAM),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.AJAM),
                        "ASHEAR list" to listOf(Makam.SABA),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.AJAM),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.AJAM),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.SABA),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.SABA),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.AJAM),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.AJAM),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.GIRKA),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.SABA, Makam.AJAM),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.AJAM),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.SIGAH, Makam.AJAM),
                        "Yishaq Yeranen Halabi" to listOf(Makam.AJAM),
                        "MOSHE AMASH (Shami)" to listOf(Makam.AJAM),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.AJAM),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.AJAM),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.HIJAZ),
                    )

                Parsha.KI_SEITZEI ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.SABA),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(
                            Makam.SABA,
                            Makam.RAST
                        ),
                        "TEBELE Pre1888" to listOf(Makam.SABA),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(
                            Makam.RAST,
                            Makam.IRAQ,
                            Makam.NAWAH
                        ),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.RAST),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.SABA),
                        "ADES: 24793" to listOf(Makam.SABA, Makam.RAST),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.SABA),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.RAST),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.NAHWAND),
                        "IDELSOHN Pre1923" to listOf(Makam.SABA),
                        "S SAGIR Laniado" to listOf(Makam.RAST),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.SABA),
                        "ASHEAR list" to listOf(Makam.NAWAH),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.SABA, Makam.SIGAH),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.SABA),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.NAHWAND),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.RAST),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.SABA),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.SABA),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.BAYAT, Makam.AJAM),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.RAST),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.SABA),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.SABA),
                        "Yishaq Yeranen Halabi" to listOf(Makam.SABA),
                        "MOSHE AMASH (Shami)" to listOf(Makam.SIGAH),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.SIGAH),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.SIGAH),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.BAYATI),
                    )

                Parsha.KI_SAVO ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.IRAQ),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(Makam.IRAQ),
                        "TEBELE Pre1888" to listOf(Makam.IRAQ),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.IRAQ),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.IRAQ),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.NAWAH),
                        "ADES: 24793" to listOf(Makam.IRAQ),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.IRAQ),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.IRAQ),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.RAST),
                        "IDELSOHN Pre1923" to listOf(Makam.IRAQ),
                        "S SAGIR Laniado" to listOf(Makam.SIGAH),
                        "ASHEAR list" to listOf(Makam.IRAQ),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.NAWAH, Makam.SABA, Makam.SIGAH),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.SIGAH),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.IRAQ),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.SIGAH),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.SIGAH),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.SIGAH),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.OJ),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.HIJAZ),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.SIGAH),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.SIGAH, Makam.IRAQ),
                        "Yishaq Yeranen Halabi" to listOf(Makam.SIGAH),
                        "MOSHE AMASH (Shami)" to listOf(Makam.SABA),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.SABA),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.BAYAT),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.Q_SIGAH)
                    )

                Parsha.NITZAVIM ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.NAWAH),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(Makam.NAWAH),
                        "TEBELE Pre1888" to listOf(Makam.NAWAH),
                        "ELIE SHAUL COHEN FROM AINTAB, ~1880" to listOf(Makam.NAWAH),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.NAWAH),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.RAST),
                        "ADES: 24793" to listOf(Makam.NAWAH),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.NAWAH),
                        "Knis Betesh Geniza List, Aleppo" to listOf(Makam.NAWAH),
                        "ABRAHAM DWECK Pre1920" to listOf(Makam.HIJAZ),
                        "IDELSOHN Pre1923" to listOf(Makam.NAWAH),
                        "S SAGIR Laniado" to listOf(Makam.NAWAH),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.RAST),
                        "ASHEAR list" to listOf(Makam.NAWAH),
                        //"ASHEAR NOTES 1936-1940": "Ḥijaz, H/H",
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.NAHWAND),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.NAHWAND),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.NAWAH),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.NAHWAND), //Original Pizmonim.com: ["Nawa", Makam.NAHWAND),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.RAST),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.RAST),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.NAHWAND),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.BAYAT),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.NAWAH, Makam.NAHWAND),
                        "Yishaq Yeranen Halabi" to listOf(Makam.NAWAH),
                        "MOSHE AMASH (Shami)" to listOf(Makam.AJAM),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.BAYAT),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.SIGAH),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.SABA),
                    )

                Parsha.VAYEILECH ->
                    mapOf(
                        "TABBUSH Ms NLI 8*7622, Aleppo" to listOf(Makam.NAWAH),
                        "R COHEN \"SHIR USHBAHA\" Jerusalem, 1905" to listOf(Makam.NAWAH),
                        "YAAQOB ABADI-PARSIYA" to listOf(Makam.RAST),
                        "YISHAQ YEQAR ARGENTINA" to listOf(Makam.HIJAZ),
                        "ADES: 24793" to listOf(Makam.NAWAH),
                        "Dibre Shelomo S KASSIN Pre1915" to listOf(Makam.NAWAH),
                        "S SAGIR Laniado" to listOf(Makam.NAWAH),
                        "M H Elias, SHIR HADASH, Jerusalem, 1930" to listOf(Makam.HOSENI),
                        "ASHEAR NOTES 1936-1940" to listOf(Makam.HOSENI),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.BAYAT, Makam.HOSENI),
                        "Argentina 1947 & Ezra Mishanieh" to listOf(Makam.AJAM),
                        "Shire Zimra H S ABOUD Jerusalem, 1950" to listOf(Makam.RAST),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.HOSENI),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.BAYAT),
                        "Ish Massliah \"Abia Renanot\" Tunisians" to listOf(Makam.RAST),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.BAYAT),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.RAST, Makam.HOSENI),
                        "Yishaq Yeranen Halabi" to listOf(Makam.RAST),
                        "MOSHE AMASH (Shami)" to listOf(Makam.BAYAT),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.AJAM),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.HOSENI),
                    )

                Parsha.HAAZINU -> generateUniformMap(
                    getAllBooks(AllBooks.DEVARIM) - "M H Elias, SHIR HADASH, Jerusalem, 1930" - "ASHEAR NOTES 1936-1940",
                    listOf(Makam.HOSENI),
                    mapOf(
                        "S SAGIR Laniado" to listOf(Makam.BAYAT),
                        "ABRAHAM E SHREM ~1945" to listOf(Makam.BAYAT),
                        "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.MEHAYAR),
                        "D KASSIN/ ISAAC CAIN; RODFE SEDEQ; MEXICO" to listOf(Makam.BAYAT),
                        "YOSEF YEHEZKEL Jerusalem 1975" to listOf(Makam.SHURI),
                        "Shaare Zimra YANANI Buenos Aires, 01" to listOf(Makam.BAYAT),
                        "BOZO, Ades, Shir Ushbaha 2005" to listOf(Makam.HOSENI, Makam.MEHAYAR),
                        "MOSHE AMASH (Shami)" to listOf(Makam.AJAM),
                        "EZRA MASLATON TARAB (Shami)" to listOf(Makam.AJAM),
                        "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.BAYAT),
                        "Hallel VeZimrah, Greece List, 1926" to listOf(Makam.NAHWAND),
                    )
                )

                else -> mapOf()
            }

            var currentData = when (jCal.parshah) {
                Parsha.VAYAKHEL_PEKUDEI -> data + mapOf("GABRIEL A SHREM 1964 SUHV" to listOf(Makam.BAYAT))
                Parsha.TAZRIA_METZORA -> data + mapOf("GABRIEL A SHREM 1964 SUHV" to listOf(Makam.SABA))
                Parsha.ACHREI_MOS_KEDOSHIM -> data + mapOf(
                    "GABRIEL A SHREM 1964 SUHV" to listOf(
                        Makam.BAYAT,
                        Makam.HIJAZ
                    )
                )

                Parsha.BEHAR_BECHUKOSAI -> data + mapOf("GABRIEL A SHREM 1964 SUHV" to listOf(Makam.SABA))
                Parsha.CHUKAS_BALAK -> data + mapOf("GABRIEL A SHREM 1964 SUHV" to listOf(Makam.HOSENI))
                Parsha.MATOS_MASEI -> data + mapOf("GABRIEL A SHREM 1964 SUHV" to listOf(Makam.SABA))
                Parsha.NITZAVIM_VAYEILECH -> data + mapOf(
                    "GABRIEL A SHREM 1964 SUHV" to listOf(
                        Makam.HOSENI
                    )
                )

                else -> data
            }

            if (jCal.jewishMonth == JewishCalendar.TISHREI && jCal.jewishDayOfMonth > 10 && jCal.parshah == Parsha.HAAZINU) {
                currentData = currentData + mapOf("ASHEAR NOTES 1936-1940" to listOf(Makam.HOSENI))
            }

            if (jCal.dayOfWeek == Calendar.SATURDAY) {
                var shavuotInUpcoming = false
                val restoreDate = jCal.getGregorianCalendar()
                for (i in 1..7) {
                    jCal.forward(Calendar.DATE, 1)
                    if (jCal.isShavuos()) {
                        shavuotInUpcoming = true
                        break
                    }
                }
                jCal.setDate(restoreDate)

                if (shavuotInUpcoming)
                    currentData =
                        currentData + mapOf("GABRIEL A SHREM 1964 SUHV" to listOf(Makam.HOSENI))

                return currentData
            }

            if (jCal.dayOfChanukah != -1) {
                return when (jCal.dayOfChanukah) {
                    1 ->
                        mapOf(
                            "SASSOON #647 Aleppo, 1850" to listOf(Makam.RAHAWI),
                            "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.RAHAWI),
                            "ABRAHAM SHAMRICHA (Shami)" to listOf(Makam.NAWAH),
                        )

                    2 ->
                        mapOf(
                            "SASSOON #647 Aleppo, 1850" to listOf(Makam.IRAQ),
                            "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.IRAQ),
                        )

                    3 ->
                        mapOf(
                            "SASSOON #647 Aleppo, 1850" to listOf(Makam.SIGAH),
                            "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.SIGAH),
                        )

                    4 ->
                        mapOf(
                            "SASSOON #647 Aleppo, 1850" to listOf(Makam.SABA),
                            "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.SABA),
                        )

                    5 ->
                        mapOf(
                            "SASSOON #647 Aleppo, 1850" to listOf(Makam.RAST),
                            "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.RAST),
                        )

                    6 ->
                        mapOf(
                            "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.NAHWAND),
                        )

                    7 ->
                        mapOf(
                            "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.SIGAH),
                        )

                    8 ->
                        mapOf(
                            "GABRIEL A SHREM 1964 SUHV" to listOf(Makam.BAYAT),
                        )

                    else -> mapOf()
                }
            }

            return mapOf()
        }
    }
}