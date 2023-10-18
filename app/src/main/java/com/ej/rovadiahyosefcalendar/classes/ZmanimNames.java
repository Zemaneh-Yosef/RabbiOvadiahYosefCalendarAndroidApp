package com.ej.rovadiahyosefcalendar.classes;

public class ZmanimNames {

    private final boolean mIsZmanimInHebrew;
    private final boolean mIsZmanimEnglishTranslated;

    public ZmanimNames(boolean isZmanimInHebrew, boolean isZmanimEnglishTranslated) {
        mIsZmanimInHebrew = isZmanimInHebrew;
        mIsZmanimEnglishTranslated = isZmanimEnglishTranslated;
    }

    public String getChatzotLaylaString() {
        if (mIsZmanimInHebrew) {
            return "חצות הלילה";
        } else if (mIsZmanimEnglishTranslated) {
            return "Midnight";
        } else {
            return "Chatzot Layla";
        }
    }

    public String getLChumraString() {
        if (mIsZmanimInHebrew) {
            return "לחומרא";
        } else if (mIsZmanimEnglishTranslated) {
            return "(Stringent)";
        } else {
            return "L'Chumra";
        }
    }

    public String getTaanitString() {
        if (mIsZmanimInHebrew) {
            return "תענית";
        } else if (mIsZmanimEnglishTranslated) {
            return "Fast";
        } else {
            return "Taanit";
        }
    }

    public String getTzaitHacochavimString() {
        if (mIsZmanimInHebrew) {
            return "צאת הכוכבים";
        } else if (mIsZmanimEnglishTranslated) {
            return "Nightfall";
        } else {
            return "Tzait Hacochavim";
        }
    }

    public String getSunsetString() {
        if (mIsZmanimInHebrew) {
            return "שקיעה";
        } else if (mIsZmanimEnglishTranslated) {
            return "Sunset";
        } else {
            return "Shkia";
        }
    }

    public String getRTString() {
        if (mIsZmanimInHebrew) {
            return "רבינו תם";
        } else {
            return "Rabbeinu Tam";
        }
    }

    public String getMacharString() {
        if (mIsZmanimInHebrew) {
            return " (מחר) ";
        } else {
            return " (Tom) ";
        }
    }

    public String getStartsString() {
        if (mIsZmanimInHebrew) {
            return " מתחיל";
        } else {
            return " Starts";
        }
    }

    public String getEndsString() {
        if (mIsZmanimEnglishTranslated) {
            return " Ends";
        } else {
            return "";
        }
    }

    public String getTzaitString() {
        if (mIsZmanimInHebrew) {
            return "צאת ";
        } else if (!mIsZmanimEnglishTranslated) {
            return "Tzait ";
        } else {
            return "";//if we are translating to English, we don't want to show the word Tzait first, just {Zman} Ends
        }
    }

    public String getCandleLightingString() {
        if (mIsZmanimInHebrew) {
            return "הדלקת נרות";
        } else {
            return "Candle Lighting";
        }
    }

    public String getYalkutYosefString() {
        if (mIsZmanimInHebrew) {
            return "ילקוט יוסף";
        } else {
            return "Yalkut Yosef";
        }
    }

    public String getHalachaBerurahString() {
        if (mIsZmanimInHebrew) {
            return "הלכה ברורה";
        } else {
            return "Halacha Berurah";
        }
    }

    public String getAbbreviatedYalkutYosefString() {
        if (mIsZmanimInHebrew) {
            return "י\"י";
        } else {
            return "Y\"Y";
        }
    }

    public String getAbbreviatedHalachaBerurahString() {
        if (mIsZmanimInHebrew) {
            return "ה\"ב";
        } else {
            return "H\"B";
        }
    }

    public String getPlagHaminchaString() {
        if (mIsZmanimInHebrew) {
            return "פלג המנחה";
        } else {
            return "Plag HaMincha";
        }
    }

    public String getMinchaKetanaString() {
        if (mIsZmanimInHebrew) {
            return "מנחה קטנה";
        } else {
            return "Mincha Ketana";
        }
    }

    public String getMinchaGedolaString() {
        if (mIsZmanimInHebrew) {
            return "מנחה גדולה";
        } else if (mIsZmanimEnglishTranslated) {
            return "Earliest Mincha";
        } else {
            return "Mincha Gedola";
        }
    }

    public String getChatzotString() {
        if (mIsZmanimInHebrew) {
            return "חצות";
        } else if (mIsZmanimEnglishTranslated) {
            return "Mid-day";
        } else {
            return "Chatzot";
        }
    }

    public String getBiurChametzString() {
        if (mIsZmanimInHebrew) {
            return "סוף זמן ביעור חמץ";
        } else if (mIsZmanimEnglishTranslated) {
            return "Latest time to burn Chametz";
        } else {
            return "Sof Zman Biur Chametz";
        }
    }

    public String getBrachotShmaString() {
        if (mIsZmanimInHebrew) {
            return "סוף זמן ברכות שמע";
        } else if (mIsZmanimEnglishTranslated) {
            return "Latest Brachot Shma";
        } else {
            return "Sof Zman Brachot Shma";
        }
    }

    public String getAchilatChametzString() {
        if (mIsZmanimInHebrew) {
            return "סוף זמן אכילת חמץ";
        } else if (mIsZmanimEnglishTranslated) {
            return "Latest time to eat Chametz";
        } else {
            return "Sof Zman Achilat Chametz";
        }
    }

    public String getShmaGraString() {
        if (mIsZmanimInHebrew) {
            return "סוף זמן שמע גר\"א";
        } else if (mIsZmanimEnglishTranslated) {
            return "Latest Shma GR\"A";
        } else {
            return "Sof Zman Shma GR\"A";
        }
    }

    public String getShmaMgaString() {
        if (mIsZmanimInHebrew) {
            return "סוף זמן שמע מג\"א";
        } else if (mIsZmanimEnglishTranslated) {
            return "Latest Shma MG\"A";
        } else {
            return "Sof Zman Shma MG\"A";
        }
    }

    public String getMishorString() {
        if (mIsZmanimInHebrew) {
            return "מישור";
        } else if (mIsZmanimEnglishTranslated) {
            return "Sea Level";
        } else {
            return "Mishor";
        }
    }

    public String getElevatedString() {
        if (mIsZmanimInHebrew) {
            return "(גבוה)";
        } else {
            return "(Elevated)";
        }
    }

    public String getHaNetzString() {
        if (mIsZmanimInHebrew) {
            return "הנץ";
        } else if (mIsZmanimEnglishTranslated) {
            return "Sunrise";
        } else {
            return "HaNetz";
        }
    }

    public String getIsInString() {
        if (mIsZmanimInHebrew) {
            return " ב...";
        } else {
            return " is in...";
        }
    }

    public String getTalitTefilinString() {
        if (mIsZmanimInHebrew) {
            return "טלית ותפילין";
        } else {
            return "Earliest Talit/Tefilin";
        }
    }

    public String getAlotString() {
        if (mIsZmanimInHebrew) {
            return "עלות השחר";
        } else if (mIsZmanimEnglishTranslated) {
            return "Dawn";
        } else {
            return "Alot Hashachar";
        }
    }
}
