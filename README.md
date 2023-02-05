# Rabbi Ovadiah Yosef Calendar App

<a href="https://play.google.com/store/apps/details?id=com.EJ.ROvadiahYosefCalendar&amp;pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1"><img class="android" alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" width="250px"></a>

<img src="http://1radionews.com/wp-content/uploads/app_store_coming_soon.png" alt="Download on the App Store" width="240px">

<a href="https://elyahu41.github.io/RabbiOvadiahYosefCalendar/index.html"><img src="https://www.kindpng.com/picc/m/36-363991_www-icon-png-transparent-background-website-icon-png.png" width="124px"></a>

The goal of this app is to recreate the Ohr HaChaim calendar that is widespread in Israel:

<img src="https://i.imgur.com/QqGAtTB.jpg" height="750">

In order to create this app, I needed an API that would give me the times for sunrise and sunset everyday. I was recommended the well known [KosherJava](https://github.com/KosherJava/zmanim) Package, and that is the basis for this app's zmanim (time) calculations.

The app can display the zmanim/times in hebrew and english.

The only zman/time that could not be computed by the KosherJava API is the sunrise time that the Ohr HaChayim calendar uses. They explain in the calendar introduction that they take the sunrise times from a calendar called, "Luach Bechoray Yosef". That calendar calculates the time for sunrise by taking into account the geography of the land around that area and finding when the earliest time for sunrise is. This would take a toll on a mobile phone's processor, therefore, the app does not support it. However, I discovered that the creator of this calendar made a website [ChaiTables.com](http://chaitables.com) to help people use his algorithm for sunrise all over the world and create a 12 month table based on your input. I added the ability to download these times in the app with your own specific parameters. (I highly recommend  you see the introduction on chaitables.com.)

First view implemented was the daily view of the app:

![alt text](https://play-lh.googleusercontent.com/46VfUTuZLlA_ogFYMP0oLUbtgQtsj-D3lHNDnS5LvqVwwgXr4Qh0p8d0ZiJg-z69IEY=w2560-h1440-rw)

Since version 6.0, the weekly view of the original calendar has been impemented:

![alt text](https://play-lh.googleusercontent.com/NbOtQFdOia2iwFb1GQJk68j_WvLwhnMOzRnE-sbLzfuqZTrybFcuM1cCypHgL8odg4N8=w2560-h1440-rw)

Click this link to download the app from the [Google Play Store!](https://play.google.com/store/apps/details?id=com.EJ.ROvadiahYosefCalendar)

# Explanation of how the zmanim are calculated:
Dawn - Alot HaShachar:

This time is calculated as 72 zmaniyot/seasonal minutes (according to the GR"A) before sunrise. Both sunrise and sunset have elevation included.

Misheyakir - Earliest Talit/Tefilin:

This time is calculated as 6 zmaniyot/seasonal minutes (according to the GR\"A) after Alot HaShachar (Dawn).

Sunrise - HaNetz:

Explained above how the Luach B'Choray Yosef calculates the time for sunrise, however, if the user does not download the times from the website, the app defaults to Mishor/Sea Level Sunrise provided by the KosherJava API.

Eating Chametz - Achilat Chametz:

This is calculated as 4 zmaniyot/seasonal hours, according to the Magen Avraham, after Alot HaShachar (Dawn) with elevation included.

Burning Chametz - Biur Chametz:

This is calculated as 5 zmaniyot/seasonal hours, according to the MG"A, after Alot HaShachar (Dawn) with elevation included.

Latest time for Shma (MG"A):

The Magen Avraham/Terumat HeDeshen calculates this time as 3 zmaniyot/seasonal hours after Alot HaShachar (Dawn). They calculate a zmaniyot/seasonal hour by taking the time between Alot HaShachar (Dawn) and Tzeit Hachocavim (Nightfall) of Rabbeinu Tam and dividing it into 12 equal parts.

Latest time for Shma (GR\"A):

The GR"A calculates this time as 3 zmaniyot/seasonal hours after sunrise (elevation included). The GR"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and divides it into 12 equal parts.

Brachot Shma:

The GR"A calculates this time as 4 zmaniyot/seasonal hours after sunrise (elevation included). The GR"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and divides it into 12 equal parts.

Mid-Day - Chatzot:

This time is calculated as 6 zmaniyot/seasonal hours after sunrise. The GR"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and divides it into 12 equal parts.

Earliest Mincha - Mincha Gedolah:

This time is calculated as 30 regular minutes after Chatzot (Mid-Day). However, if the zmaniyot/seasonal minutes are longer, we use those minutes instead to be stringent. The GR"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and divides it into 12 equal parts. Then we divide one of those 12 parts into 60 to get a zmaniyot/seasonal minute."

Mincha Ketana:

This time is calculated as 9 and a half zmaniyot/seasonal hours after sunrise. The GR"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and divides it into 12 equal parts. Then we divide one of those 12 parts into 60 to get a zmaniyot/seasonal minute.

Plag HaMincha:

This time is usually calculated as 10 and 3/4th zmaniyot/seasonal hours after sunrise, however, yalkut yosef writes to calculate it as 1 hour and 15 zmaniyot/seasonal minutes before tzeit. The GR"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and divides it into 12 equal parts. Then we divide one of those 12 parts into 60 to get a zmaniyot/seasonal minute.

Candle Lighting:

This time is calculated as 20 regular minutes before sunset (elevation included) by default. You can change this in the settings.

Sunset - Shkia:

Halachic sunset is defined as the moment when the top edge of the sun disappears on the horizon while setting (elevation included).

Nightfall - Tzeit Hacochavim:

This time is calculated as 13 and a half zmaniyot/seasonal minutes after sunset (elevation included). The GR"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and divides it into 12 equal parts. Then we divide one of those 12 parts into 60 to get a zmaniyot/seasonal minute. NOTE: This time is very early in the winter and especially in the far north or south. This zman should NOT be used to decide when shabbat ends or any other serious matters without consolidating a rabbi first!

Fast Ends - Tzeit Taanit:

This time is displayed twice, the first time is calculated as 20 regular minutes after sunset (elevation included) and the second time is calculated as 30 minutes afterwards.

Shabbat/Chag Ends - Tzeit Shabbat/Chag:

Note that there are many customs on when shabbat ends, by default, it is set to 40 regular minutes after sunset (elevation included), however, you can change the time in the settings. This time is calculated as 40 regular minutes after sunset (elevation included).

Rabbeinu Tam:

This time is calculated as 72 zmaniyot/seasonal minutes after sunset (elevation included). The GR"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and divides it into 12 equal parts. Then we divide one of those 12 parts into 60 to get a zmaniyot/seasonal minute in order to calculate 72 minutes. Another way of calculating this time is by calculating how many minutes are between sunrise and sunset. Take that number and divide it by 10, and then add the result to sunset.

Midnight - Chatzot Layla:

This time is calculated as 6 zmaniyot/seasonal hours after sunset. The GR"A calculates a zmaniyot/seasonal hour by taking the time between sunrise and sunset (elevation included) and divides it into 12 equal parts. In this case, we use sunrise for the next day.




Introduction to the calendar in Israel:

<img src="https://i.imgur.com/udfwy3R.jpg" height="650">
<img src="https://i.imgur.com/ureV4p4.jpg" height="650">
<img src="https://i.imgur.com/HXEzXvr.jpg" height="650">
