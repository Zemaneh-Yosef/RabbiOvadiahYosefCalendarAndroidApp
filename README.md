# Rabbi Ovadiah Yosef Calendar App
The goal of this app is to recreate the Ohr HaChaim calendar that is widespread in Israel:

<img src="https://i.imgur.com/QqGAtTB.jpg" height="750">

In order to create this app, I needed an API that would give me the times for sunrise and sunset everyday. I was recommended the well known [KosherJava](https://github.com/KosherJava/zmanim) Package, and that is the basis for this app's zmanim (time) calculations.

The app can display the zmanim/times in hebrew and english.

The only zman/time that could not be computed by the KosherJava API is the sunrise time that the Ohr HaChayim calendar uses. They explain in the calendar introduction that they take the sunrise times from a calendar called, "Luach Bechoray Yosef". That calendar calculates the time for sunrise by taking into account the geography of the land around that area and finding when the earliest time for sunrise is. This would take a toll on a mobile phone's processor, therefore, the app does not support it. However, I discovered that the creator of this calendar made a website [ChaiTables.com](http://chaitables.com) to help people use his algorithm for sunrise all over the world and create a 12 month table based on your input. I added the ability to download these times in the app with your own specific parameters. (I highly recommend  you see the introduction on chaitables.com.)

First view implemented was the daily view of the app:

![alt text](https://play-lh.googleusercontent.com/46VfUTuZLlA_ogFYMP0oLUbtgQtsj-D3lHNDnS5LvqVwwgXr4Qh0p8d0ZiJg-z69IEY=w720-h310-rw)

Since version 6.0, the weekly view of the original calendar has been impemented:

Click this link to download the app from the Google Play Store!: https://play.google.com/store/apps/details?id=com.EJ.ROvadiahYosefCalendar

# Explanation of how the zmanim are calculated:
Dawn - Alot HaShachar:

This time is calculated as 72 zmaniyot/seasonal minutes (according to the GR"A) before sunrise. Both sunrise and sunset have elevation included.

Misheyakir - Earliest Talit/Tefilin:

This time is calculated as 6 zmaniyot/seasonal minutes (according to the GR\"A) after Alot HaShachar (Dawn).






Introduction to the calendar in Israel:

<img src="https://i.imgur.com/udfwy3R.jpg" height="650">
<img src="https://i.imgur.com/ureV4p4.jpg" height="650">
<img src="https://i.imgur.com/HXEzXvr.jpg" height="650">
