# Rabbi Ovadiah Yosef Calendar (Android) App

<p align="center">
	<img src="https://is1-ssl.mzstatic.com/image/thumb/Purple211/v4/cc/4f/de/cc4fded5-598f-1f3a-eaa6-26405d119a93/AppIcon-0-0-1x_U007epad-0-11-0-85-220.png/217x0w.webp" width="200px" alt="logo">
</p>

<table align="center">
  <tr>
    <td align="center" width="33%"><strong>Google Play Store</strong></td>
    <td align="center" width="33%"><strong>App Store</strong></td>
    <td align="center" width="33%"><strong>Website</strong></td>
  </tr>
  <tr>
    <td align="center" width="33%">
      <a href="https://play.google.com/store/apps/details?id=com.EJ.ROvadiahYosefCalendar&amp;pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1">
        <img alt="Get it on Google Play" src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png" width="200px">
      </a>
    </td>
    <td align="center" width="33%">
      <a href="https://apps.apple.com/app/rabbi-ovadiah-yosef-calendar/id6448838987">
        <img alt="Get it on the App Store" src="https://ci6.googleusercontent.com/proxy/HrtBTHlFE3VpRkzLfRwnYbJjCLtCpmKOIV__qk9k9mj7e7PSZF2X0L7mzR63nCIfqbnUujbn-dhiq-LwYUqdcpSLg_ItRhdEQJ0wP438309hcA=s0-d-e1-ft#https://static.licdn.com/aero-v1/sc/h/76yzkd0h5kiv27lrd4yaenylk" width="200px">
      </a>
    </td>
    <td align="center" width="33%">
      <a href="https://royzmanim.com/">
        <img src="https://cdn-icons-png.flaticon.com/512/5602/5602732.png" width="100px" alt="Website">
      </a>
    </td>
  </tr>

  <tr>
    <td align="center" width="33%"><strong>Source Code</strong></td>
    <td align="center" width="33%"><strong>Source Code</strong></td>
    <td align="center" width="33%"><strong>Source Code</strong></td>
  </tr>
  <tr>
    <td align="center" width="33%">
      <a href="https://github.com/Zemaneh-Yosef/RabbiOvadiahYosefCalendarAndroidApp">
        <img src="https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png" width="50px" alt="GitHub">
      </a>
    </td>
    <td align="center" width="33%">
      <a href="https://github.com/Zemaneh-Yosef/RabbiOvadiahYosefCalendarIOSApp">
        <img src="https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png" width="50px" alt="GitHub">
      </a>
    </td>
    <td align="center" width="33%">
      <a href="https://github.com/Zemaneh-Yosef/royzmanimwebsite">
        <img src="https://github.githubassets.com/images/modules/logos_page/GitHub-Mark.png" width="50px" alt="GitHub">
      </a>
    </td>
  </tr>
</table>

# Goal of the project:
The goal original of this project was to recreate the "Luach HaMaor Ohr HaChaim" calendar that is widespread in Israel. This calendar is special because Rabbi Ovadiah Yosef ZT"L oversaw it's creation and used the calendar himself until he passed. It is considered to be the most accurate calendar for people who want to follow Rabbi Ovadia Yosef's practices:

<img src="https://i.imgur.com/QqGAtTB.jpg" height="750" alt="Picture of Ohr HaChaim calendar">

In order to recreate the calendar, we needed use an API that would give the times for sunrise and sunset everyday (since all the other zemanim (times) are based on sunrise/sunset). I was recommended the well known [KosherJava](https://github.com/KosherJava/zmanim) package for it's accuracy and transparency, and that is the basis for all of the app's calculations.

The app was originally made for primarily english speakers, however, it has been localized for both english and hebrew speakers.

The only zeman that could not be computed by the KosherJava API is the sunrise time that the Ohr HaChaim calendar uses. They explain in the calendar introduction that they take the sunrise times from a calendar called, "Luach Bechoray Yosef". That calendar calculates the time for sunrise by taking into account the geography of the land around that area and finding when is the first time the sun is seen in that area (based on the introduction to Chaitables.com). While not impossible, this would take a massive toll on a mobile phone's processor and memory, therefore, the app does not support it. However, I discovered that the creator of this calendar made a website [ChaiTables.com](http://chaitables.com) to help people use his algorithm for sunrise/sunset all over the world and create a 12 month table based on your input. I added the ability to download these times in the app with your own specific parameters. (It is highly recommended that you see the introduction on chaitables.com.)

After the Ohr HaChaim calendar was implemented and fully functional, we implemented Rabbi Leeor Dahan's calendar for areas outside of Israel. The Ohr HaChaim calendar was made for Israel and it needed a few adjustments to be applicable for outside of Israel. We confirmed this with both of Rabbi Ovadia Yosef's sons (Rabbi Yitzhak Yosef and Rabbi David Yosef).

Rabbi Meir Gavriel Elbaz and Rabbi Leeor Dahan themselves have given [haskamot](https://royzmanim.com/) for this project, and with their help we were able to even receive a [haskama](https://royzmanim.com/assets/haskamah-rishon-letzion.pdf) from Rabbi Yitzhak Yosef.

# App Screenshots:
| <img src="https://play-lh.googleusercontent.com/zVHLQfLFEFi042B5fs8aospA9rEHQYI9lhEpBDl5WJtO7DXpWf5KHOyJwGcJt6FL6g"> | <img src="https://play-lh.googleusercontent.com/qqzs-vMfkBYOb7MhE8GSSDrgxnT3SHPnvOUVZ5-hpmPKNRmGDlKk_ZfWgFPRjoyS9Zk"> | <img src="https://play-lh.googleusercontent.com/M2rFetNy9kZJoVpi6EV0w07TagYgV7pdCaSH-d46p_tjjaydrbrjCA-8ytfBJ57BSzI"> | <img src="https://play-lh.googleusercontent.com/M5ngIENjzFAu3Jr_6uPMPR7ZL5axZa7wXjXj-l3T2UR1NwWHnDhgK5cwymG92_dH9g"> |
| ---------------------------------------------- | -------------------------------------------- | ------------------------------------------ | ------------------------------------------- |

| <img src="https://play-lh.googleusercontent.com/x9-dPIKcVofd0Bzd1Pncp7YTKItC9dPtP0ZU8swd3Q-ee2ySTkyiscXTssF_eNPfgnk"> | <img src="https://play-lh.googleusercontent.com/4_ieNYmeCYv75lGsuOg95fT5-1v7vK8zuvSbOV78MtMWWKLS-PIDS2dTT9MCTz4vX4FP"> | <img src="https://play-lh.googleusercontent.com/ZiT9dliscrbOqqwxdpzivXE77Hpt-CMpgUcT-8x1MVnDPZp6jVOTpdes5Dgg9l4Okg"> | <img src="https://play-lh.googleusercontent.com/goFEBByE3neELxuURq_G7LtO_QkuQCs8WPnu5Ltx57Elx62k5FgCWqWDuIfobqagMs2u"> |
| ---------------------------------------------- | -------------------------------------------- | ------------------------------------------ | ------------------------------------------- |

# Explanation of how the zemanim are calculated:
- For an in-depth explanation on each specific time, please look at the descriptions for each individual time, found in the app-itself.
- For an overall explanation, please visit our organization's ReadME on GitHub.
- For an explanation on the differences between outside of Israel and inside of Israel or whether or not to use elevation, please visit our [FAQ](https://royzmanim.com/FAQ)

# Introduction and haskama to the calendar in Israel:

| <img src="https://royzmanim.com/assets/images/sources/OHhaskama.png" height="250" alt="haskama"> | <img src="https://royzmanim.com/assets/images/sources/intro1.png" height="250" alt="intro 1"> | <img src="https://royzmanim.com/assets/images/sources/intro2.png" height="250" alt="intro 2"> | <img src="https://royzmanim.com/assets/images/sources/intro3.png" height="250" alt="intro 3"> |
| ---------------------------------------------- | -------------------------------------------- | ------------------------------------------- | ------------------------------------------- |
