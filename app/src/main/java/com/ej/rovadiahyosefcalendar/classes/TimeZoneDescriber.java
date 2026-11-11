package com.ej.rovadiahyosefcalendar.classes;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.zone.ZoneOffsetTransition;
import java.time.zone.ZoneOffsetTransitionRule;
import java.time.zone.ZoneRules;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Describes a time zone in plain English or Hebrew: its offset from UTC, its daylight saving rules,
 * and the last and next clock change.
 * Never throws: a null, blank or unknown zone ID returns an error message instead.
 */
public final class TimeZoneDescriber {

    private final boolean hebrew;
    private final Locale locale;

    private TimeZoneDescriber(boolean hebrew) {
        this.hebrew = hebrew;
        this.locale = hebrew ? Locale.forLanguageTag("he") : Locale.ENGLISH;
    }

    public static String describe(String zoneId, boolean hebrew) {
        if (zoneId == null || zoneId.trim().isEmpty()) {
            return hebrew ? "שגיאה: מזהה אזור הזמן חסר." : "Error: the time zone ID is missing.";
        }
        ZoneId zone;
        try {
            zone = ZoneId.of(zoneId.trim());
        } catch (DateTimeException e) {
            return hebrew ? "שגיאה: אזור זמן לא מוכר: " + zoneId : "Error: unknown time zone: " + zoneId;
        }
        return describe(zone, hebrew);
    }

    public static String describe(TimeZone timeZone, boolean hebrew) {
        return describe(timeZone == null ? null : timeZone.toZoneId(), hebrew);
    }

    public static String describe(ZoneId zone, boolean hebrew) {
        if (zone == null) {
            return hebrew ? "שגיאה: מזהה אזור הזמן חסר." : "Error: the time zone ID is missing.";
        }
        try {
            return new TimeZoneDescriber(hebrew).build(zone);
        } catch (RuntimeException e) {
            return hebrew ? "שגיאה: לא ניתן לתאר את אזור הזמן " + zone.getId()
                    : "Error: could not describe time zone " + zone.getId();
        }
    }

    private String tr(String en, String he) {
        return hebrew ? he : en;
    }

    private String build(ZoneId zone) {
        ZoneRules rules = zone.getRules();
        Instant now = Instant.now();
        StringBuilder sb = new StringBuilder();

        String abbreviation = abbreviation(zone, now);
        sb.append(zone.getId()).append(abbreviation.equals(zone.getId()) ? "" : " (" + abbreviation + ")").append("\n\n");
        sb.append(tr("Right now: ", "כרגע: ")).append(offsetText(rules.getOffset(now)));

        if (rules.isFixedOffset()) {
            sb.append(".\n\n").append(tr("This zone does not use daylight saving time and has no recorded changes.",
                    "באזור זמן זה אין שעון קיץ ואין שינויים מתועדים.")).append('\n');
            return sb.toString();
        }

        sb.append(", ");
        if (rules.isDaylightSavings(now)) {
            sb.append(tr("daylight saving time (standard time is " + offsetShort(rules.getStandardOffset(now)) + ")",
                    "שעון קיץ (שעון חורף הוא " + offsetShort(rules.getStandardOffset(now)) + ")"));
        } else {
            sb.append(tr("standard time", "שעון חורף"));
        }
        sb.append(".\n\n");

        ZoneOffsetTransition previous = rules.previousTransition(now);
        ZoneOffsetTransition next = rules.nextTransition(now);

        List<ZoneOffsetTransitionRule> recurring = rules.getTransitionRules();
        if (recurring.isEmpty()) {
            sb.append(tr("Daylight saving time: ", "שעון קיץ: ")).append(next != null
                    ? tr("no fixed yearly pattern, changes are set one at a time.", "אין דפוס שנתי קבוע, השינויים נקבעים בנפרד.")
                    : tr("not currently used.", "אינו בשימוש כעת.")).append("\n\n");
        } else {
            sb.append(tr("Daylight saving time:", "שעון קיץ:")).append('\n');
            for (ZoneOffsetTransitionRule rule : recurring) {
                long change = rule.getOffsetAfter().getTotalSeconds() - rule.getOffsetBefore().getTotalSeconds();
                boolean starts = rule.getOffsetAfter().getTotalSeconds() > rule.getStandardOffset().getTotalSeconds();
                sb.append("• ").append(starts ? tr("Starts: ", "תחילה: ") : tr("Ends: ", "סיום: "))
                        .append(ruleDay(rule)).append(tr(" at ", " בשעה ")).append(ruleTime(rule))
                        .append(" – ").append(clocksText(change, false))
                        .append(tr(" (to ", " (ל-")).append(offsetShort(rule.getOffsetAfter())).append(")\n");
            }
            sb.append('\n');
        }

        sb.append(tr("Last change: ", "שינוי אחרון: "))
                .append(previous == null ? tr("none recorded", "לא מתועד") : changeLine(rules, previous, true)).append('\n');
        sb.append(tr("Next change: ", "השינוי הבא: "))
                .append(next == null ? tr("none scheduled", "אין שינוי מתוכנן") : changeLine(rules, next, false)).append('\n');

        return sb.toString();
    }

    private static boolean isStandardOffsetChange(ZoneRules rules, ZoneOffsetTransition t) {
        return !rules.getStandardOffset(t.getInstant().minusSeconds(1)).equals(rules.getStandardOffset(t.getInstant()));
    }

    /** E.g. "November 1, 2026 – daylight saving time ended (clocks went back 1 hour)". */
    private String changeLine(ZoneRules rules, ZoneOffsetTransition t, boolean past) {
        long change = t.getOffsetAfter().getTotalSeconds() - t.getOffsetBefore().getTotalSeconds();
        String what;
        if (isStandardOffsetChange(rules, t)) {
            String from = offsetShort(rules.getStandardOffset(t.getInstant().minusSeconds(1)));
            String to = offsetShort(rules.getStandardOffset(t.getInstant()));
            what = tr("standard time " + (past ? "changed" : "changes") + " from " + from + " to " + to,
                    "שעון החורף " + (past ? "השתנה" : "משתנה") + " מ-" + from + " ל-" + to);
        } else if (change > 0) {
            what = tr("daylight saving time " + (past ? "started" : "starts"), "שעון הקיץ " + (past ? "התחיל" : "מתחיל"));
        } else {
            what = tr("daylight saving time " + (past ? "ended" : "ends"), "שעון הקיץ " + (past ? "הסתיים" : "מסתיים"));
        }
        return dateText(t.getDateTimeBefore().toLocalDate()) + " – " + what + " (" + clocksText(change, past) + ")";
    }

    private String clocksText(long change, boolean past) {
        String amount = durationText(Math.abs(change));
        if (hebrew) {
            return "השעון " + (change > 0 ? (past ? "התקדם " : "מתקדם ") : (past ? "חזר אחורה " : "חוזר אחורה ")) + hebrewBe(amount);
        }
        return "clocks " + (past ? "went " : "go ") + (change > 0 ? "forward " : "back ") + amount;
    }

    /** "בשעה" / "בשעתיים" for words, "ב-5 שעות" when the amount starts with a digit. */
    private static String hebrewBe(String amount) {
        return Character.isDigit(amount.charAt(0)) ? "ב-" + amount : "ב" + amount;
    }

    /** E.g. "the second Sunday of March" / "יום ראשון השני של מרץ". */
    private String ruleDay(ZoneOffsetTransitionRule rule) {
        String month = rule.getMonth().getDisplayName(TextStyle.FULL, locale);
        int dom = rule.getDayOfMonthIndicator();
        DayOfWeek dow = rule.getDayOfWeek();
        if (dow == null) {
            if (dom == -1) {
                return tr("the last day of " + month, "היום האחרון של " + month);
            }
            if (dom < 0) {
                return tr("the " + ordinal(-dom) + "-to-last day of " + month, "היום ה-" + (-dom) + " מהסוף של " + month);
            }
            return tr(month + " " + dom, dom + " ב" + month);
        }
        String day = dow.getDisplayName(TextStyle.FULL, locale);
        if (dom == -1) {
            return tr("the last " + day + " of " + month, day + " האחרון של " + month);
        }
        if (dom > 0 && (dom - 1) % 7 == 0 && dom <= 22) {
            return tr("the " + ordinal((dom - 1) / 7 + 1) + " " + day + " of " + month,
                    day + " " + ordinal((dom - 1) / 7 + 1) + " של " + month);
        }
        return dom > 0
                ? tr("the " + day + " on or after " + month + " " + dom, day + " ב-" + dom + " ב" + month + " או אחריו")
                : tr("the " + day + " on or before the " + ordinal(-dom) + "-to-last day of " + month,
                day + " ביום ה-" + (-dom) + " מהסוף של " + month + " או לפניו");
    }

    private String ruleTime(ZoneOffsetTransitionRule rule) {
        String time = rule.isMidnightEndOfDay() ? tr("midnight (end of day)", "חצות (סוף היום)") : timeText(rule.getLocalTime());
        return switch (rule.getTimeDefinition()) {
            case UTC -> time + " UTC";
            case STANDARD -> time + tr(" standard time", " (שעון חורף)");
            default -> time;
        };
    }

    private String timeText(LocalTime t) {
        if (hebrew) {
            return String.format(Locale.ROOT, "%02d:%02d", t.getHour(), t.getMinute());
        }
        int h12 = t.getHour() % 12 == 0 ? 12 : t.getHour() % 12;
        return String.format(Locale.ROOT, "%d:%02d %s", h12, t.getMinute(), t.getHour() < 12 ? "AM" : "PM");
    }

    private String dateText(LocalDate date) {
        return DateTimeFormatter.ofPattern(hebrew ? "d 'ב'MMMM yyyy" : "MMMM d, yyyy", locale).format(date);
    }

    private String ordinal(int n) {
        return switch (n) {
            case 1 -> tr("first", "הראשון");
            case 2 -> tr("second", "השני");
            case 3 -> tr("third", "השלישי");
            case 4 -> tr("fourth", "הרביעי");
            default -> hebrew ? "ה-" + n : n + "th";
        };
    }

    /** "UTC-5 (5 hours behind UTC)". */
    private String offsetText(ZoneOffset offset) {
        int minutes = offset.getTotalSeconds() / 60;
        if (minutes == 0) {
            return tr("UTC (the same as UTC time)", "UTC (זהה ל-UTC)");
        }
        String amount = durationText(Math.abs(minutes) * 60L);
        String relation = hebrew
                ? (minutes < 0 ? "מפגר אחרי UTC " : "מקדים את UTC ") + hebrewBe(amount)
                : amount + (minutes < 0 ? " behind" : " ahead of") + " UTC";
        return offsetShort(offset) + " (" + relation + ")";
    }

    /** "UTC-5" or "UTC+5:30" (seconds from old local-mean-time offsets are dropped). */
    private static String offsetShort(ZoneOffset offset) {
        int minutes = offset.getTotalSeconds() / 60;
        if (minutes == 0) {
            return "UTC";
        }
        int abs = Math.abs(minutes);
        return "UTC" + (minutes < 0 ? "-" : "+") + abs / 60 + (abs % 60 == 0 ? "" : String.format(Locale.ROOT, ":%02d", abs % 60));
    }

    private String durationText(long seconds) {
        long h = seconds / 3600, m = (seconds % 3600) / 60, s = seconds % 60;
        StringBuilder sb = new StringBuilder();
        if (h > 0) {
            sb.append(hebrew ? (h == 1 ? "שעה" : h == 2 ? "שעתיים" : h + " שעות") : h + (h == 1 ? " hour" : " hours"));
        }
        if (m > 0) {
            sb.append(sb.length() > 0 ? " " : "")
                    .append(hebrew ? (m == 1 ? "דקה" : m == 2 ? "שתי דקות" : m + " דקות") : m + (m == 1 ? " minute" : " minutes"));
        }
        if (s > 0) {
            sb.append(sb.length() > 0 ? " " : "")
                    .append(hebrew ? (s == 1 ? "שנייה" : s + " שניות") : s + (s == 1 ? " second" : " seconds"));
        }
        return sb.length() == 0 ? tr("0 minutes", "0 דקות") : sb.toString();
    }

    private String abbreviation(ZoneId zone, Instant at) {
        return ZonedDateTime.ofInstant(at, zone).format(DateTimeFormatter.ofPattern("zzz", locale));
    }
}