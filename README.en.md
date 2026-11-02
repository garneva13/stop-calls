# CallGuard

[![Support the author on Boosty](https://img.shields.io/badge/Boosty-support%20the%20author-FF6B35?style=for-the-badge)](https://boosty.to/garneva13)

An Android app that rejects incoming calls from numbers you do not have in your
contacts, and keeps a journal of those calls.

**Download:** [latest release](https://github.com/garneva13/stop-calls/releases/latest)
· **Website:** [garneva13.github.io/stop-calls](https://garneva13.github.io/stop-calls/en/)
· **Русская версия:** [README.md](README.md)

Android 10 and newer (minSdk 29), APK ~275 KB, MIT license.

> [!TIP]
> **The app is free: no ads, no subscriptions, no paid tiers.** One person builds it in
> their spare time — if CallGuard saves you some nerves, you can
> [support the author on Boosty](https://boosty.to/garneva13).

## What it does

| Feature | Where |
|---|---|
| Rejects calls from numbers not in contacts, before the first ring | switch in the state block |
| Journal of every screened call with the reason | “Full journal” |
| Pause for 30 minutes, 1, 2 or 3 hours | “Pause” tile |
| Weekly schedule | “Schedule” tile |
| Let the next unknown call through | “Let next call through” tile |
| Allow list of numbers that always get through | “Allow list” |
| Statistics: day, week, month, year — chart and call times | “Statistics” row |
| Status notification in the shade: on / paused | appears automatically |
| “Call filter” Quick Settings tile — toggle with one tap | shade → edit tiles |
| English and Russian UI with an in-app switch | About → Language |
| Diagnostics: do calls reach the app, what was decided | “Diagnostics” |

The app does **not use the internet**: the APK has no `INTERNET` permission. No update
checks, no analytics, no ads. The journal, allow list and settings stay on the phone;
contacts are read only to compare the caller number and never leave the device.

## Requirements

- Android 10+ with the “Call screening” role available (Pixel and most stock
  Android builds).
- Contacts access, so numbers can be compared.
- One app can hold the call screening role: assigning it to CallGuard turns off
  the built-in Google caller ID (switch back in “Caller ID & spam app”).

### Limitations

- Hidden and private numbers are never passed to screening apps by Android.
  Use the Phone app’s “Block unknown callers” for those.
- STIR/SHAKEN verification is rarely sent in Russia, so the “block without
  marker” option is off by default.
- Numbers are compared by the last 10 digits, so `+7 916 123-45-67`,
  `8 (916) 123-45-67` and `9161234567` are the same number.
- iOS is not supported: the platform gives third-party apps neither the caller
  number nor the right to reject a call — see [docs/ios.md](docs/ios.md).

## Build

No Gradle and no network are required: JDK 17 and Android SDK build-tools.

```powershell
cd callguard
.\build-apk.ps1 -Clean -Version 1.12 -VersionCode 13
```

The signed APK is copied to `dist/`. Self-tests for the pure logic:

```powershell
$jdk = 'C:\Program Files (x86)\Android\openjdk\jdk-17.0.14\bin'
& "$jdk\javac.exe" -encoding UTF-8 -d tools\out `
    app\src\main\java\com\callguard\PhoneNumbers.java tools\PhoneNumbersSelfTest.java
& "$jdk\java.exe" -cp tools\out com.callguard.PhoneNumbersSelfTest
```

## Releasing

```powershell
.\build-apk.ps1 -Version 1.13 -VersionCode 14
git push
```

Then create a GitHub release with tag `v1.13` and attach `dist/CallGuard-1.13.apk`.

There is no in-app updater: the app has no network permission. Users download the new
APK and install it over the old one; data is kept as long as the signing key is the same.
In RuStore the version is updated through the store console.

Never commit `keystore/` — anyone with the signing key could sign an update your phone
would accept. Changing the key means users must uninstall first.

## License

MIT — see [LICENSE](LICENSE).
