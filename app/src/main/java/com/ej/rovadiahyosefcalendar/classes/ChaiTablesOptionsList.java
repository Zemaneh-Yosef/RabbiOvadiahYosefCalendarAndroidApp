package com.ej.rovadiahyosefcalendar.classes;

/**
 * This class is used to store the options for the ChaiTables website. Using this class, you can build all the options for the ChaiTables website.
 *
 * @author Elyahu Jacobi
 * @version 1.0
 */
public class ChaiTablesOptionsList {

    /**
     * This is the list of options for the main page of the ChaiTables website. Notice that I skipped "All Places" and "Airplane Times"
     * because they do not lead to any time tables. UPDATE: Use the ENUM class ChaiTablesCountries instead. I am leaving this here for
     * anyone who might want to use it and debugging reasons.
     * @see ChaiTablesCountries
     */
    public static String[] countries = new String[]{"Argentina", "Australia", "Austria", "Belgium", "Brazil",
            "Bulgaria", "Canada", "Chile", "China", "Colombia", "Czech-Republic", "Denmark",
            "Eretz_Yisroel (Cities)", //becomes Eretz_Yisroel in the link
            "Eretz_Yisroel (Neighborhoods)", //becomes Israel in the link
            "France", "Germany", "Greece", "Hungary", "Italy", "Mexico", "Netherlands", "Panama", "Poland", "Romania",
            "Russia", "South-Africa", "Spain", "Switzerland", "Turkey",
            "UK and Ireland", //becomes England in the link
            "Ukraine", "Uruguay", "USA", "Venezuela"};

    public static String[] countriesHebrew = new String[]{"ארגנטינה", "אוסטרליה", "אוסטריה", "בלגיה", "ברזיל",
            "בולגריה", "קנדה", "צ'ילה", "סין", "קולומביה", "רפובליקת הצ'כ", "דנמרק",
            "ארץ ישראל (ערים)",
            "ארץ ישראל (שכונות)",
            "צרפת", "גרמניה", "יוון", "הונגריה", "איטליה", "מקסיקו", "הולנד", "פנמה", "פולין", "רומניה",
            "רוסיה", "דרום אפריקה", "ספרד", "שוויץ", "טורקיה",
            "הממלכה המאוחדת ואירלנד",
            "אוקראינה", "אורוגוואי", "ארצות הברית", "וונצואלה"};

    /**
     * This is the selected country that will be passed into the link.
     * The string will look like this for Example: "USA" or "England"
     */
    private static String selectedCountry;

    /**
     * This is the list of options for all the metropolitan areas in the inner page of the ChaiTables website after you select a country.
     */
    private static String[] metropolitanAreas;

    /**
     * This is the selected MetroArea that will be passed into the link. Note: This will only used for Eretz_Yisroel (cities)
     */
    private static String selectedMetropolitanArea;

    /**
     * This is the index for the selected value of the metropolitan area in the String array.
     * This is needed for the parameter of the chaitables link called: cgi_USAcities1
     */
    public static int indexOfMetroArea = 0;

    /**
     * STEP 1
     * This is the first method that should be called.
     *
     * It will initialize the values of the selected country into the metropolitanAreas array.
     * Then it will return that array.
     *
     * Choose a metro area from the array and pass it into the next method called {@link #selectMetropolitanArea(String)}
     */
    public static String[] selectCountry(ChaiTablesCountries country) {
        switch (country) {
            case ARGENTINA:
                initMetropolitanAreaArgentina();
                break;
            case AUSTRALIA:
                initMetropolitanAreaAustralia();
                break;
            case AUSTRIA:
                initMetropolitanAreaAustria();
                break;
            case BELGIUM:
                initMetropolitanAreaBelgium();
                break;
            case BRAZIL:
                initMetropolitanAreaBrazil();
                break;
            case BULGARIA:
                initMetropolitanAreaBulgaria();
                break;
            case CANADA:
                initMetropolitanAreaCanada();
                break;
            case CHILE:
                initMetropolitanAreaChile();
                break;
            case CHINA:
                initMetropolitanAreaChina();
                break;
            case COLOMBIA:
                initMetropolitanAreaColombia();
                break;
            case CZECH_REPUBLIC:
                initMetropolitanAreaCzechRepublic();
                break;
            case DENMARK:
                initMetropolitanAreaDenmark();
                break;
            case ERETZ_YISROEL_NEIGHBORHOODS:
                initMetropolitanAreaIsrael();
                break;
            case ERETZ_YISROEL_CITIES:
                initMetropolitanAreaEretzYisroel();
                break;
            case FRANCE:
                initMetropolitanAreaFrance();
                break;
            case GERMANY:
                initMetropolitanAreaGermany();
                break;
            case GREECE:
                initMetropolitanAreaGreece();
                break;
            case HUNGARY:
                initMetropolitanAreaHungary();
                break;
            case ITALY:
                initMetropolitanAreaItaly();
                break;
            case MEXICO:
                initMetropolitanAreaMexico();
                break;
            case NETHERLANDS:
                initMetropolitanAreaNetherlands();
                break;
            case PANAMA:
                initMetropolitanAreaPanama();
                break;
            case POLAND:
                initMetropolitanAreaPoland();
                break;
            case ROMANIA:
                initMetropolitanAreaRomania();
                break;
            case RUSSIA:
                initMetropolitanAreaRussia();
                break;
            case SOUTH_AFRICA:
                initMetropolitanAreaSouthAfrica();
                break;
            case SPAIN:
                initMetropolitanAreaSpain();
                break;
            case SWITZERLAND:
                initMetropolitanAreaSwitzerland();
                break;
            case TURKEY:
                initMetropolitanAreaTurkey();
                break;
            case UK_AND_IRELAND:
                initMetropolitanAreaUKandIreland();
                break;
            case UKRAINE:
                initMetropolitanAreaUkraine();
                break;
            case URUGUAY:
                initMetropolitanAreaUruguay();
                break;
            case USA:
                initMetropolitanAreaUSA();
                break;
            case VENEZUELA:
                initMetropolitanAreaVenezuela();
                break;
            default://Not found
                break;
        }
        selectedCountry = country.label;
        return metropolitanAreas;
    }

    /**
     * STEP 2
     * This method is used to set the metropolitan area that the user selected.
     * If a country has not been selected, the method throws an exception.
     * @param metropolitanArea The string that represents the metropolitan area.
     * @see #selectCountry(ChaiTablesCountries)
     */
    public static void selectMetropolitanArea(String metropolitanArea) {
        if (metropolitanAreas == null) {
            throw new NullPointerException("metropolitanAreas is null. Please call selectCountry() first.");
        }

        selectedMetropolitanArea = metropolitanArea;

        for (int i = 0; i < metropolitanAreas.length; i++) {
            if (metropolitanAreas[i].equals(metropolitanArea)) {
                indexOfMetroArea = i + 1;//since we start at 0 and the arrays from javascript start at 1
                break;
            }
        }
    }

    /**
     * STEP 3
     * Returns the full chaitables url after everything has been setup. See {@link #selectCountry(ChaiTablesCountries)} and {@link #selectMetropolitanArea(String)}
     * @return the full link directly to the chai tables for the chosen neighborhood
     *
     * @param latitude The latitude of the location
     * @param longitude The longitude of the location
     * @param searchRadius The search radius in kilometers.
     * @param timezone The timezone of the location in UTC format.
     * @param type the type of table you want.
     *             0 is visible sunrise,
     *             1 is visible sunset,
     *             2 is mishor sunrise,
     *             3 is astronomical sunrise,
     *             4 is mishor sunset,
     *             5 is astronomical sunset.
     * @param year the desired hebrew year for the chaitable link
     * @param userId the user id for the chaitables link
     */
    public static String getChaiTablesLink(double latitude, double longitude, int timezone, int searchRadius, int type, int year, int userId) {
        if (type < 0 || type > 5) {
            throw new IllegalArgumentException("type of tables must be between 0 and 5");
        }

        longitude = -longitude;

        if (selectedCountry.equals("Eretz_Yisroel")) {
            return getChaiTablesEretzYisroelLink(type, year, userId);
        }

        if (selectedCountry.equals("Israel")) {
            searchRadius = 2;//recommended radius for Israel only for some reason. Everywhere else the site defaults to 8(km).
        }

        return "http://www.chaitables.com/cgi-bin/ChaiTables.cgi/?cgi_TableType=Chai" +
                "&cgi_country=" + selectedCountry +
                "&cgi_USAcities1=" + indexOfMetroArea +
                "&cgi_USAcities2=0" +//not used if we're using our own coordinates
                "&cgi_searchradius=" + searchRadius +
                "&cgi_Placename=?" + //not needed
                "&cgi_eroslatitude=" + latitude +
                "&cgi_eroslongitude=" + longitude +
                "&cgi_eroshgt=0.0" + //only used for astronomical sunrise and sunset (I think)
                "&cgi_geotz=" + timezone + //tz = timezone
                "&cgi_exactcoord=OFF" + //not needed
                "&cgi_MetroArea=jerusalem" + //this only changes when using Eretz Yisroel (cities)
                "&cgi_types=" + type + //this parameter defines what type of table to produce.
                "&cgi_RoundSecond=1" + //1 = Round to nearest second (default/-1 = round to nearest 5 seconds)
                "&cgi_AddCushion=0" + //cushion for obstructions, leave it at default
                "&cgi_24hr=" + //whether we want the times in 24h format, default is off
                "&cgi_typezman=-1" + //whether to calculate extra zmanim, -1 is none because we do not need it
                "&cgi_yrheb=" + year +
                "&cgi_optionheb=1" + //change language to hebrew parameter, default is off (for the english website)
                "&cgi_UserNumber=" + userId +//the number of the user is tracked for some reason, we should probably randomize it a little.
                "&cgi_Language=English" +
                "&cgi_AllowShaving=OFF";
    }

    private static String getChaiTablesEretzYisroelLink(int type, int year, int userId) {
        return "http://www.chaitables.com/cgi-bin/ChaiTables.cgi/?cgi_TableType=BY" +
                "&cgi_country=" + selectedCountry +
                "&cgi_USAcities1=1"  +
                "&cgi_USAcities2=0" +//not used if we're using our own coordinates
                "&cgi_searchradius=" +
                "&cgi_Placename=?" + //not needed
                "&cgi_eroslatitude=0.0" +
                "&cgi_eroslongitude=0.0" +
                "&cgi_eroshgt=0.0" + //only used for astronomical sunrise and sunset (I think)
                "&cgi_geotz=2" + //tz = timezone
                "&cgi_exactcoord=OFF" + //not needed
                "&cgi_MetroArea=" + selectedMetropolitanArea + //this only changes when using Eretz Yisroel (cities)
                "&cgi_types=" + type + //this parameter defines what type of table to produce.
                "&cgi_RoundSecond=-1" + //-1 = OFF
                "&cgi_AddCushion=0" + //cushion for obstructions, leave it at default
                "&cgi_24hr=" + //whether we want the times in 24h format, default is off
                "&cgi_typezman=-1" + //whether to calculate extra zmanim, -1 is none because we do not need it
                "&cgi_yrheb=" + year +
                "&cgi_optionheb=1" + //change language to hebrew parameter, default is off (for the english website)
                "&cgi_UserNumber=" + userId +//the number of the user is tracked for some reason, we should probably randomize it a little.
                "&cgi_Language=English" +
                "&cgi_AllowShaving=OFF";
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaArgentina() {
        metropolitanAreas = new String[] {
                "Buenos-Aires_area_Argentina",
                "Concordia_area_Argentina",
                "Salto_area_Argentina"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaAustralia() {
        metropolitanAreas = new String[] {
                "Melbourne_area_Australia",
                "Sydney_area_Australia"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaAustria() {
        metropolitanAreas = new String[] {
                "Vienna_area_Austria"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaBelgium() {
        metropolitanAreas = new String[] {
                "Antwerpen_area_Belgium",
                "Bruxelles_area_Belgium"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaBrazil() {
        metropolitanAreas = new String[] {
                "Rio-de-Janeiro_area_Brazil",
                "Sao_Paulo_area_Brazil"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaBulgaria() {
        metropolitanAreas = new String[] {
                "Sofia_area_Bulgaria"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaCanada() {
        metropolitanAreas = new String[] {
                "Calgary_area_AB",
                "Edmonton_area_AB",
                "Fredericton_area_NB",
                "Halifax_area_NS",
                "Hamilton_area_ON",
                "Kitchener_area_ON",
                "London_area_ON",
                "Montreal_area_QC",
                "Ottawa_area_ON",
                "Regina_area_SK",
                "Toronto_area_ON",
                "Vancouver_area_BC",
                "Windsor_area_ON",
                "Winnipeg_area_MB"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaChile() {
        metropolitanAreas = new String[] {
                "Santiago_area_Chile"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaChina() {
        metropolitanAreas = new String[] {
                "Hong-Kong_area_China"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaColombia() {
        metropolitanAreas = new String[] {
                "Bogota_area_Colombia",
                "Cali_area_Colombia"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaCzechRepublic() {
        metropolitanAreas = new String[] {
                "Prague_area_Czech-Republic"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaDenmark() {
        metropolitanAreas = new String[] {
                "Copenhagen_area_Denmark"
        };
    }

    /**
     * Also known as MetroArea in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaEretzYisroel() {
        metropolitanAreas = new String[] {
                "acco",
                "achiezer",
                "achisamach",
                "achituv",
                "achuzam",
                "aderet",
                "adiad",
                "afulah",
                "agur",
                "airport",
                "alefei_menasheh",
                "almah",
                "almon",
                "alonei_habashan",
                "alon_moreh",
                "alon_shvut",
                "aluma",
                "alumim",
                "amazia",
                "amichai",
                "amkah",
                "amukah",
                "amuna",
                "arad",
                "ariel",
                "ashdod",
                "ashkolon",
                "ateret",
                "atlit",
                "aviezer",
                "avnei_chafetz",
                "avnei_eiton",
                "azrikam",
                "barak",
                "bareket",
                "barkan-beit_abah",
                "bat_iyen B",
                "bat_shlomo",
                "beerot_yitzhak",
                "beersheva",
                "beer_tuviah",
                "beer_yaakov",
                "beetar_eilit",
                "beit_ariyeh",
                "beit_chagai",
                "beit_chelkiah",
                "beit_choron",
                "beit_dagan",
                "beit_el",
                "beit_gamliel",
                "beit_hagedi",
                "beit_hillel",
                "beit_meir",
                "beit_rimon",
                "beit_shemes_(old-part)",
                "beit_shean",
                "beit_shemes_combined",
                "beit_uziel",
                "beit_yehoshuah",
                "beit_yosef",
                "ben_zakai",
                "berechiah-hodayah",
                "binyaminah",
                "birea",
                "bnei_ayish",
                "bnei_atarot",
                "bnei_brak_Zuriel",
                "bnei_brak-ramat_gan-givatiyim",
                "bnei_darom",
                "bnei_dekalim",
                "bnei_netzarim",
                "bnei_reem",
                "brosh",
                "burgatah",
                "caesarea",
                "calanit",
                "carmel",
                "carmiel",
                "chaderah",
                "chadid",
                "chalamish",
                "chamat_geder",
                "chasmonaim",
                "chavat_hashomer",
                "chazon",
                "chazor_haglilit-rosh_pinah",
                "chemed",
                "cherev-laat_elyachin-kfar_haraah",
                "chermash",
                "chevel_shalom",
                "chofetz_chaim",
                "chofit",
                "chof_kinar",
                "chorasha",
                "dalton",
                "dimonah",
                "dolev",
                "dovev",
                "efrata",
                "eilat",
                "einav",
                "ein_ayalah",
                "ein_bokek",
                "ein_gedi_(kibbutz)",
                "ein_gev",
                "ein_hanaziv",
                "ein_yaakov",
                "ein_zurim",
                "eitan",
                "eitanim",
                "elad",
                "elazar",
                "eli",
                "eliav",
                "elipelet",
                "elkanah",
                "elyakim",
                "emanuel",
                "eshbol",
                "eshtaohl",
                "etz_efraim",
                "even_menachem",
                "even_shmuel",
                "even_yehuda",
                "gaderah",
                "gadid",
                "gamzu",
                "gan_ohr",
                "gan_yavneh",
                "gefen",
                "geulei_teman",
                "gevah_binyamin",
                "gibolim-melilot",
                "gilat",
                "ginton",
                "gitit",
                "givat_washington",
                "givat_yitzhar",
                "givat_yaarim",
                "givat_zeev",
                "givat_zeev_agan_haayalot",
                "givat_zeev_agan_haayalot_combined",
                "givat_adah",
                "givon_hachadasha",
                "gush_chizpin",
                "gush_dan",
                "haifa",
                "har_bracha",
                "har_gilo",
                "har_shmuel",
                "har_yona",
                "Harish",
                "hazorim",
                "hebron-kirat_arbah",
                "Herzliah_A-kfar_smaryahu",
                "Herzliah_B-ramat_hasharon",
                "hinanit-shaked-tal_menashe",
                "hosen",
                "hoshayahu",
                "itamar",
                "jerusalem",
                "kadima",
                "karmei_katif",
                "karmei_zur",
                "karnei_shomron",
                "kazrin",
                "kedumim",
                "kerem_ben_zimrah",
                "kesalon",
                "keshet",
                "kever_schmuel_hanavi_roof",
                "kever_rachel-beit_lechem",
                "kever_rebbi_meir_baal_hanes",
                "kever_yonatan_ben_uziel",
                "kfar_adumim",
                "kfar_chanannia",
                "kfar_chabad",
                "kfar_chasidim-rechesim",
                "kfar_etzion",
                "kfar_gidon",
                "kfar_menachem",
                "kfar_rosh_hanikra",
                "kfar_sabah-raananah",
                "kfar_sirkin",
                "kfar_shamai",
                "kfar_tapuach",
                "kfar_tavor",
                "kfar_veradim",
                "kfar_yonah",
                "kfar_yaabez-ezriel",
                "kfar_zeitim",
                "kibutz_yavneh",
                "kiriat_anavim",
                "kiriat_atah",
                "kiriat_ekron",
                "kiriat_gat",
                "kiriat_malachi",
                "kiriat_netafim",
                "kiriat_ohno-sabion-yehud",
                "kiriat_sefer",
                "kiriat_sefer_water_tower",
                "kiriat_shemonah",
                "kiriat_tivon",
                "kiriat_yaarim-camp",
                "kiriat-yam-mozkin-bialik",
                "kochav_hashachar",
                "kochav_yair",
                "komemiut",
                "lachish",
                "lavi",
                "lod",
                "lod_Ben_Gurion_airport",
                "maaleh_gilboah",
                "maaleh_michmash",
                "maaleh_amos",
                "maaleh_adumim-mizpeh_navoh",
                "maaleh_efraim",
                "maaleh_levonah",
                "maaleh_shomron",
                "maalot_tarsicha",
                "macabim",
                "macheneh_yatir",
                "machsiah",
                "malchishuah",
                "manof",
                "maon",
                "Marchavei_David",
                "margaliyyot",
                "masada",
                "maskiot",
                "masuot_yizhak",
                "matah",
                "matityahu",
                "mazkeret_batiah",
                "mazor",
                "mecholah",
                "megadim",
                "meiron",
                "meitar",
                "menachemiah",
                "menucha",
                "meoz_zion",
                "mercaz_shapirah",
                "metulah_top",
                "metulah_bottom",
                "metzad",
                "mevaseret_zion",
                "mevaseret_zion_area_(combined)",
                "mevoh_modyim",
                "mevoh_charon",
                "mevoh_dotan",
                "mevoot_yericho",
                "meyrav",
                "migdal",
                "migdalim",
                "migdal_oz",
                "migdal_haemek",
                "misgav",
                "mishmar_hayarden",
                "mizadot_yehudah",
                "mizpeh_netofa",
                "mizpeh_nevoh",
                "mizpeh_ramon",
                "mizpeh_yericho",
                "modiin",
                "modiin_illit",
                "morag",
                "moreshet",
                "mozah",
                "mozah_elit",
                "naaleh",
                "nachalim",
                "nacham",
                "nachliel",
                "nahariah",
                "nataf",
                "natanyah",
                "Naveh",
                "nazaret_eilit",
                "nechushah",
                "neriah",
                "nes_harim",
                "nes_zionah",
                "neta",
                "netivot",
                "Netua",
                "neveh_daniel",
                "neveh_ziv",
                "nili",
                "nirit",
                "nir_etzion",
                "nir_galim",
                "nizan",
                "noam",
                "noga",
                "nokdim",
                "ofakim",
                "ofra",
                "ohel_nachum_tiberias",
                "ohra",
                "ohr_akibah",
                "ohr_haganuz",
                "ohr_yehudah-ramat_pinkas-neveh_efraim",
                "omer",
                "otniel",
                "ozem",
                "paamei_tashas",
                "pakiin_chadashah",
                "pardes_channah",
                "patish",
                "peduel",
                "pesagot",
                "petach_tikvah",
                "pnei_chever",
                "porat",
                "poriah_naveh_oved",
                "poriah_eilit",
                "raananah-kfar_sabah",
                "rachov",
                "ramat_beit_shemes",
                "ramat_beit_shemes_gimmel",
                "ramat_raziel",
                "ramat_yishai",
                "ramat_magshimim",
                "ramlah",
                "ramon_airbase",
                "ramot_sesh",
                "rechovot",
                "reut",
                "revacha",
                "revava",
                "revayah",
                "rimonim",
                "rishon_lezion-nachalat_yehudah",
                "rosh_haeiin",
                "rosh_pinah-chazor_haglilit",
                "rosh_zurim",
                "saad",
                "sadeh_eliyahu",
                "sadeh_trumot",
                "sadeh_yaakov",
                "sadeh_elan",
                "safed",
                "sah_nor",
                "schem",
                "schlomi",
                "sedei_chemed",
                "sederot",
                "shaalvim",
                "shaarei_avraham",
                "shaarei_tikvah",
                "Shaar_Yeshuv",
                "shadmot_mechulah",
                "shagav",
                "shalvah",
                "sharsheret",
                "shavei_shomron",
                "shebolim",
                "shiloh",
                "shimah",
                "shlomit",
                "shluchot",
                "shoeva",
                "shoham",
                "shokdah",
                "Shomera",
                "shomriya",
                "shvut_rachel",
                "susiah",
                "talmon",
                "taoz",
                "tarom",
                "tefachot",
                "tekoa",
                "tekumah",
                "telz_stone_ravshulman",
                "telz_stone_all_mountains",
                "tel_aviv-bat_yam-cholon",
                "tel_zion_kochav_yaakov",
                "tenah",
                "tiberias_eilit",
                "tiberias_old",
                "tifrach",
                "tirat_yehudah",
                "tirat_zvi",
                "tirat_carmel",
                "tirosh",
                "toshiah-kfar_mimon",
                "vered_yericho",
                "yaara",
                "yad_binyamin",
                "yad_rambam",
                "yakir",
                "yavneel",
                "yavneh",
                "yeruchom",
                "yesodot",
                "yesod_hamaalah",
                "yishai",
                "yitzhar",
                "yokneam_illit",
                "yonatan",
                "yoshiveah",
                "yotvatah",
                "zafriah",
                "zanoach",
                "Zarit",
                "zavdiel",
                "zealim",
                "zecharya",
                "zerachia",
                "zeruah",
                "zichron_yaakov",
                "zimrat-shuvah",
                "zipori",
                "zohar",
                "zufim",
                "zuriel",
                "zur_hadassah"
        };
    }


    /**
     * Also known as USAcities1 in the link for the ChaiTables website. Erez Yisroel (Neighborhoods)
     */
    private static void initMetropolitanAreaIsrael() {
        metropolitanAreas = new String[] {
                "Beit-Shemes_area_Israel",
                "Haifa_area_Israel",
                "Jerusalem_area_Israel",
                "Safed_area_Israel",
                "Tiberias_area_Israel"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaFrance() {
        metropolitanAreas = new String[] {
                "Aix-En-Provence_area_France",
                "Aix-Les-Bains_area_France",
                "Colmar_area_France",
                "Grenoble_area_France",
                "Lille_area_France",
                "Lyon_area_France",
                "Marseille_area_France",
                "Metz_area_France",
                "Mulhouse_area_France",
                "Nantes_area_France",
                "Nice_area_France",
                "Nimes_area_France",
                "Paris_area_France",
                "Strasbourg_area_France",
                "Toulon_area_France",
                "Toulouse_area_France",
                "Troyes_area_France",
                "Vichy_area_France"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaGermany() {
        metropolitanAreas = new String[] {
                "Bad-Nauheim_area_Germany",
                "Berlin_area_Germany",
                "Frankfurt_area_Germany",
                "Hamburg_area_Germany",
                "Hannover_area_Germany",
                "Munich_area_Germany"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaGreece() {
        metropolitanAreas = new String[] {
                "Athens_area_Greece"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaHungary() {
        metropolitanAreas = new String[] {
                "Budapest_area_Hungary"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaItaly() {
        metropolitanAreas = new String[] {
                "Bologna_area_Italy",
                "Florence_area_Italy",
                "Milan_area_Italy",
                "Rome_area_Italy"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaMexico() {
        metropolitanAreas = new String[] {
                "Cuernevaca_Morelos_area",
                "Mexico-City_area_Mexico"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaNetherlands() {
        metropolitanAreas = new String[] {
                "Amsterdam_area_Netherlands"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaPanama() {
        metropolitanAreas = new String[] {
                "Panama-City_area_Panama"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaPoland() {
        metropolitanAreas = new String[] {
                "Krakow_area_Poland"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaRomania() {
        metropolitanAreas = new String[] {
                "Bucharest_area_Romania"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaRussia() {
        metropolitanAreas = new String[] {
                "Moscow_area_Russia"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaSouthAfrica() {
        metropolitanAreas = new String[] {
                "Cape_Town_area_South-Africa",
                "Johannesburg_area_South-Africa"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaSpain() {
        metropolitanAreas = new String[] {
                "Barcelona_area_Spain",
                "Madrid_area_Spain",
                "Malaga_area_Spain",
                "Marbella_area_Spain",
                "Melilla_area_Spain"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaSwitzerland() {
        metropolitanAreas = new String[] {
                "Basel_area_Switzerland",
                "Einsiedeln_area_Switzerland",
                "Geneve_area_Switzerland",
                "Lausanne_area_Switzerland",
                "Lugano_area_Switzerland",
                "Luzern_area_Switzerland",
                "Zurich_area_Switzerland"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaTurkey() {
        metropolitanAreas = new String[] {
                "Antalya_area_Turkey",
                "Istanbul_area_Turkey"
        };
    }


    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaUKandIreland() {
        metropolitanAreas = new String[] {
                "Belfast_area_Northern-Ireland",
                "Birmingham_area_UK",
                "Cardiff_area_UK",
                "Dublin_area_Ireland",
                "Edinburgh_area_UK",
                "Gateshead-Newcastle_area_UK",
                "Glasgow_area_UK",
                "Leeds_area_UK",
                "Leicester_area_UK",
                "Liverpool_area_UK",
                "London_area_UK",
                "Manchester_area_UK",
                "Southend-On-Sea_area_UK",
                "Southport_area_UK",
                "Sunderland_area_UK"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaUkraine() {
        metropolitanAreas = new String[] {
                "Berdychiv_area_Ukraine",
                "Bogorodichin_area_Ukraine",
                "Kiev_area_Ukraine",
                "Kolomyya_area_Ukraine",
                "Odessa_area_Ukraine",
                "Stanislau_area_Ukraine",
                "Uman_area_Ukraine",
                "Zhytomyr_area_Ukraine"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaUruguay() {
        metropolitanAreas = new String[] {
                "Montevideo_area_Uruguay",
                "San_Jose_area_Uruguay"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaUSA() {
        metropolitanAreas = new String[] {
                "Albuquerque_area_NM",
                "Atlanta_area_GA",
                "Austin_area_TX",
                "Baltimore_area_MD",
                "Bethlehem_area_NH",
                "Binghamton_area_NY",
                "Boston_area_MA",
                "Buffalo_area_NY",
                "Catskill_village_area_NY",
                "Chicago_area_IL",
                "Cincinnati_area_OH",
                "Cleveland_area_OH",
                "Columbus_area_OH",
                "Dallas_area_TX",
                "Deal_area_NJ",
                "Denver_area_CO",
                "Detroit_area_MI",
                "El_Paso_area_TX",
                "Fort_Worth_area_TX",
                "Franconia_area_NH",
                "Harrisburg_area_PA",
                "Hartford_area_CT",
                "Houston_area_TX",
                "Howell_area_NJ",
                "Indianapolis_area_IN",
                "Ithaca_area_NY",
                "Jackson_area_NJ",
                "Kansas_City_area_MO",
                "Lakewood_area_NJ",
                "Liberty_area_NY",
                "Long_Island_area_NY",
                "Los_Angeles_area_CA",
                "Memphis_area_TN",
                "Miami_area_FL",
                "Milwaukee_area_WI",
                "Minneapolis_area_MN",
                "Monroe_area_NY",
                "Monsey_area_NY",
                "Monticello_area_NY",
                "New_Haven_area_CT",
                "New_York_City_area_NY",
                "North_East_New_Jersey_area_NJ",
                "Philadelphia_area_PA",
                "Phoenix_area_AZ",
                "Pittsburgh_area_PA",
                "Providence_area_RI",
                "Richmond_area_VA",
                "Rochester_area_NY",
                "San_Antonio_area_TX",
                "San_Diego_area_CA",
                "San_Francisco_area_CA",
                "Santa-Fe_area_NM",
                "Scranton_area_PA",
                "Seattle_area_WA",
                "Sierra-Nevada_area_CA",
                "South_Bend_area_IN",
                "South_Fallsburg_area_NY",
                "South_Haven_area_MI",
                "Spring_Valley_area_NY",
                "St_Louis_area_MO",
                "Stamford_area_CT",
                "Toms-River_area_NJ",
                "Tuscon_area_AZ",
                "WashingtonDC_area_MD",
                "Waterbury_area_CT",
                "Williams_area_AZ",
                "Woodridge_area_NY"
        };
    }

    /**
     * Also known as USAcities1 in the link for the ChaiTables website.
     */
    private static void initMetropolitanAreaVenezuela() {
        metropolitanAreas = new String[] {
                "Caracas_area_Venezuela"
        };
    }

}