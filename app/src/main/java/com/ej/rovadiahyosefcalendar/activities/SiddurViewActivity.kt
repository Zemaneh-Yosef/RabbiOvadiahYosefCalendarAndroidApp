package com.ej.rovadiahyosefcalendar.activities

import android.os.Bundle
import android.util.TypedValue
import android.view.WindowManager
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import com.ej.rovadiahyosefcalendar.R
import com.ej.rovadiahyosefcalendar.activities.MainFragmentManagerActivity.SHARED_PREF
import com.ej.rovadiahyosefcalendar.classes.HighlightString
import com.ej.rovadiahyosefcalendar.classes.JewishDateInfo
import com.ej.rovadiahyosefcalendar.classes.SiddurMaker
import com.ej.rovadiahyosefcalendar.classes.SiddurScreenEntry

class SiddurViewActivity : AppCompatActivity() {

	private var scrollToPosition: ((Int) -> Unit)? = null
	private var pendingScrollPosition: Int? = null

	override fun onCreate(savedInstanceState: Bundle?) {
		enableEdgeToEdge()
		super.onCreate(savedInstanceState)

		val sharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE)

		// --- 1. PRAYER TITLE ---
		val siddurTitle = intent.getStringExtra("prayer") ?: ""

		// --- 2. SETUP JEWISH DATE ---
		val mJewishDateInfo = JewishDateInfo(sharedPreferences.getBoolean("inIsrael", false)).apply {
			jewishCalendar.setJewishDate(
				intent.getIntExtra("JewishYear", jewishCalendar.jewishYear),
				intent.getIntExtra("JewishMonth", jewishCalendar.jewishMonth),
				intent.getIntExtra("JewishDay", jewishCalendar.jewishDayOfMonth)
			)
			setCalendar(jewishCalendar.gregorianCalendar)
			jewishCalendar.isMukafChoma = sharedPreferences.getBoolean("isMukafChoma", false)
			jewishCalendar.isSafekMukafChoma = sharedPreferences.getBoolean("isSafekMukafChoma", false)
		}

		val typedValue = TypedValue()
		theme.resolveAttribute(android.R.attr.textColorPrimary, typedValue, true)
		val textColor = ContextCompat.getColor(this, typedValue.resourceId)

		// --- 3. LOAD PRAYERS ---
		val siddurMaker = SiddurMaker(mJewishDateInfo, textColor, this)
		val prayers: ArrayList<HighlightString> = when (siddurTitle) {
			"סליחות" -> siddurMaker.getSelichotPrayers(
				intent.getBooleanExtra("isAfterChatzot", false),
				intent.getBooleanExtra("isAfterSunrise", false)
			)
			"שחרית" -> siddurMaker.shacharitPrayers
			"מוסף" -> siddurMaker.musafPrayers
			"מנחה" -> siddurMaker.minchaPrayers
			"ערבית" -> siddurMaker.arvitPrayers
			"ספירת העומר" -> siddurMaker.sefiratHaOmerPrayers
			"הדלקת נרות חנוכה" -> siddurMaker.hadlakatNeirotChanukaPrayers
			"הבדלה" -> siddurMaker.havdalahPrayers
			"סדר סיום מסכת" -> siddurMaker.getSiyumMasechetPrayer(
				requireNotNull(intent.getStringArrayExtra("masechtas"))
			)
			"ברכת המזון" -> siddurMaker.birchatHamazonPrayers
			"תפלת הדרך" -> siddurMaker.tefilatHaderechPrayer
			"ברכת הלבנה" -> siddurMaker.birchatHalevanaPrayers
			"תיקון חצות" -> siddurMaker.getTikkunChatzotPrayers(
				intent.getBooleanExtra("isNightTikkunChatzot", true)
			)
			"ק״ש שעל המיטה" -> siddurMaker.getKriatShemaShealHamitaPrayers(
				!intent.getBooleanExtra("isAfterChatzot", false)
			)
			"ברכת מעין שלוש" -> siddurMaker.getBirchatMeeyinShaloshPrayers(
				requireNotNull(intent.getStringArrayExtra("itemsForMeyinShalosh"))
			)
			"תהילים" -> siddurMaker.tehilimPrayers
			else -> ArrayList()
		}

		// --- 4. BUILD CATEGORY JUMP LIST ---
		val categoryNames = mutableListOf<String>()
		val categoryPositions = mutableListOf<Int>()
		prayers.forEachIndexed { index, highlightString ->
			if (highlightString.type == HighlightString.StringType.CATEGORY) {
				categoryNames.add(highlightString.toString())
				categoryPositions.add(index)
			}
		}

		// --- 5. KEEP SCREEN-ON ---
		if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean("siddurAlwaysOn", false)) {
			window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
		}

		// --- 6. HANDLE TEHILIM DEEP LINK ---
		val tehilimIndex = intent.getIntExtra("tehilimIndex", -1)
		if (tehilimIndex != -1 && tehilimIndex - 1 in categoryPositions.indices) {
			requestScroll(categoryPositions[tehilimIndex - 1])
		}

		// --- 7. SET COMPOSE CONTENT ---
		// Toolbar, category menu, and siddur content are all handled inside Compose.
		setContent {
			SiddurScreenEntry(
				siddurContent = prayers,
				jewishDateInfo = mJewishDateInfo,
				siddurTitle = siddurTitle.ifEmpty { getString(R.string.show_siddur) },
				categoryNames = categoryNames,
				categoryPositions = categoryPositions,
				onNavigateBack = { finish() },
				onScrollRequested = { scrollFn ->
					scrollToPosition = scrollFn
					pendingScrollPosition?.let { pending ->
						scrollFn(pending)
						pendingScrollPosition = null
					}
				}
			)
		}
	}

	private fun requestScroll(position: Int) {
		scrollToPosition?.invoke(position) ?: run {
			pendingScrollPosition = position
		}
	}
}