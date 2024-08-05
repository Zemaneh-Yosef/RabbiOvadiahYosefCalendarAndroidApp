package com.ej.rovadiahyosefcalendar.classes;

import java.util.HashMap;
import java.util.Map;

public class NumberToHebrew {

    private static final Map<Integer, String> hebrewUnits = new HashMap<>();
    private static final Map<Integer, String> hebrewTens = new HashMap<>();
    private static final Map<Integer, String> hebrewHundreds = new HashMap<>();
    private static final Map<Integer, String> hebrewThousands = new HashMap<>();

    static {
        hebrewUnits.put(1, "אֶחָד");
        hebrewUnits.put(2, "שְׁנַיִם");
        hebrewUnits.put(3, "שְׁלֹשָׁה");
        hebrewUnits.put(4, "אַרְבָּעָה");
        hebrewUnits.put(5, "חֲמִשָּׁה");
        hebrewUnits.put(6, "שִׁשָּׁה");
        hebrewUnits.put(7, "שִׁבְעָה");
        hebrewUnits.put(8, "שְׁמֹנָה");
        hebrewUnits.put(9, "תִּשְׁעָה");

        hebrewTens.put(10, "עָשָׂרָה");
        hebrewTens.put(20, "עֶשְׂרִים");
        hebrewTens.put(30, "שְׁלֹשִׁים");
        hebrewTens.put(40, "אַרְבָּעִים");
        hebrewTens.put(50, "חֲמִשִּׁים");
        hebrewTens.put(60, "שִׁשִּׁים");
        hebrewTens.put(70, "שִׁבְעִים");
        hebrewTens.put(80, "שְׁמֹנִים");
        hebrewTens.put(90, "תִּשְׁעִים");

        hebrewHundreds.put(100, "מֵאָה");
        hebrewHundreds.put(200, "מָאתַיִם");
        hebrewHundreds.put(300, "שְׁלֹשׁ מֵאוֹת");
        hebrewHundreds.put(400, "אַרְבַּע מֵאוֹת");
        hebrewHundreds.put(500, "חֲמֵשׁ מֵאוֹת");
        hebrewHundreds.put(600, "שֵׁשׁ מֵאוֹת");
        hebrewHundreds.put(700, "שֶׁבַע מֵאוֹת");
        hebrewHundreds.put(800, "שְׁמוֹנֶה מֵאוֹת");
        hebrewHundreds.put(900, "תְּשַׁע מֵאוֹת");

        hebrewThousands.put(1000, "אֶלֶף");
        hebrewThousands.put(2000, "אֲלָפַיִם");
        hebrewThousands.put(3000, "שְׁלֹשֶׁת אֲלָפִים");
        hebrewThousands.put(4000, "אַרְבָּעַת אֲלָפִים");
        hebrewThousands.put(5000, "חֲמֵשֶׁת אֲלָפִים");
        hebrewThousands.put(6000, "שֵׁשֶׁת אֲלָפִים");
        hebrewThousands.put(7000, "שִׁבְעַת אֲלָפִים");
        hebrewThousands.put(8000, "שְׁמוֹנַת אֲלָפִים");
        hebrewThousands.put(9000, "תִּשְׁעַת אֲלָפִים");
    }

    public static String numberToHebrew(int number) {
        StringBuilder hebrewSentence = new StringBuilder();

        if (number <= 0 || number > 9999) {
            return "מספר לא ידוע"; // Unknown number
        }

        if (number >= 1000) {
            int thousands = (number / 1000) * 1000;
            hebrewSentence.append(hebrewThousands.get(thousands));
            number %= 1000;
            if (number > 0) {
                hebrewSentence.append(" ו");
            }
        }

        if (number >= 100) {
            int hundreds = (number / 100) * 100;
            hebrewSentence.append(hebrewHundreds.get(hundreds));
            number %= 100;
            if (number > 0) {
                hebrewSentence.append(" ו");
            }
        }

        if (number >= 20) {
            int tens = (number / 10) * 10;
            hebrewSentence.append(hebrewTens.get(tens));
            number %= 10;
            if (number > 0) {
                hebrewSentence.append(" ו");
            }
        } else if (number >= 10) {
            hebrewSentence.append(hebrewUnits.get(number - 10)).append(" ").append("עשרה");
            number = 0;
        }

        if (number > 0) {
            hebrewSentence.append(hebrewUnits.get(number));
        }

        return hebrewSentence.toString();
    }
}


