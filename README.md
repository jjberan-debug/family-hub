# Family Hub

A wall-tablet dashboard for the family, showing:

- **Family calendar (top):** this week's Google Calendar events, Monday to Sunday. Use the arrows to change week, and tap a day to add an event.
- **Kids (bottom):** a big card per kid showing **today** in large, easy-to-read text, with **tomorrow** underneath for getting ready the night before. Items are things like *Sport uniform*, *Library bag* or *Tennis 4 pm*. They can repeat every week or be one-offs, with or without a time. Tap **Week** on a card to see and edit that kid's whole week.
- **Calendar events:** tap **Event** to add one to Google Calendar, or tap an event to see it, open it in the Calendar app, or delete it.
- **Shopping and To do:** simple checklists. Tap an item to tick it, and use **Clear ticked** to tidy up.

Kids' activities and lists are stored only on the tablet. Calendar events live in Google Calendar, and the app reaches them through the Google account on the tablet.

## Installing on the tablet

1. On the tablet, open the repository's **Releases** page on GitHub (sign in to GitHub in the browser the first time).
2. Download the newest `FamilyHub-N.apk`.
3. Open the downloaded file. Android asks you to allow installs from your browser once: choose **Settings → Allow from this source**, go back, then tap **Install**.
4. Open **Family Hub** and tap **Allow** when it asks for calendar access.
5. Open **Settings** (the gear icon), then enter the kids' names and choose their colours and the calendars to show.

**Updating:** install a newer APK the same way. It installs over the old one and keeps all your data.

## Tablet set-up tips

- Sign the tablet in to the Google account that holds the family calendar. In the Google Calendar app, tick each calendar you want (including any Donna shares) and turn **Sync** on for it.
- For a wall display, keep the tablet plugged in. **Keep screen on** in Settings is on by default.
- Swipe in from the top or bottom edge to show Android's navigation bars.
- To keep a backup of the app's data, turn on Google backup in the tablet's settings (*Settings → Google → Backup*).

## Building

Every push to `main` builds the APK with GitHub Actions (`.github/workflows/build.yml`) and publishes it as a release.

To build locally, install Android Studio, open this folder and run `assembleRelease`.
