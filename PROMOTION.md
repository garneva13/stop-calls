# Продвижение CallGuard

Практические тексты и чек-лист. Ничего не публиковать «во все площадки сразу» —
лучше по одной, с интервалом в несколько дней, чтобы видеть, откуда пришли люди.

## 1. Чек-лист репозитория (до любых анонсов)

- [ ] **About** на главной странице: description, website, topics.
      Не в Settings → General, а в блоке About справа → шестерёнка.
- [ ] **Social preview**: Settings → General → Social preview → загрузить
      `docs/cover-1200x630.png`.
- [ ] Последний релиз не черновик и не Pre-release, APK прикреплён.
- [ ] Репозиторий закреплён в профиле (Customize your pins).
- [ ] Ссылка на сайт стоит в bio профиля.

### Значения для About

Description:

```
Android call filter: rejects calls from numbers not in your contacts. Offline, no ads, MIT, Android 10+.
```

Website: `https://garneva13.github.io/stop-calls/`

Topics (по одному, Enter после каждого):

```
android, android-app, call-blocker, call-screening, spam-blocker,
unknown-callers, telephony, privacy, offline, java, no-ads,
open-source, mit-license, android10
```

## 2. Reddit — r/androidapps

Title:

```
CallGuard — open-source Android filter that rejects calls from numbers not in your contacts (offline, no ads)
```

Body:

```
I got tired of spam calls and did not want a cloud service reading my contacts, so I built a small
local filter.

How it works: it uses CallScreeningService + the "Call screening" role, so Android hands the call
to the app before the phone rings. The app checks the number against your contacts and rejects it
if the number is not there. Everything happens on the device.

What is inside:
- journal of every decision with the reason (not in contacts / in contacts / paused / scheduled)
- pause for 30 minutes, 1, 2 or 3 hours, plus a weekly schedule
- one-shot pass for the next unknown call
- allow list for clinics and couriers
- statistics: day/week/month/year, chart by day and by hour
- silent status notification, English and Russian UI
- long-press a journal row to copy the number

Size: 275 KB, Android 10+, no AndroidX, built with aapt2/d8 directly. MIT license.

APK: https://github.com/garneva13/stop-calls/releases/latest
Source: https://github.com/garneva13/stop-calls
Site: https://garneva13.github.io/stop-calls/en/

Known limits: hidden/private numbers are never passed to screening apps, and only one app can hold
the screening role (assigning it to CallGuard turns off Google's built-in caller ID).
```

## 3. Reddit — r/privacy или r/degoogle

Короткая версия, другой акцент:

```
Made a local-only call filter for Android — no account, no cloud, no analytics

It rejects calls from numbers that are not in your contacts, using the system CallScreeningService.
The only network request the app ever makes is the update check against api.github.com; there is no
telemetry and no contact upload. Journal and settings live in SQLite/shared prefs on the device.

Open source, MIT, 275 KB: https://github.com/garneva13/stop-calls
```

## 4. Hacker News — Show HN

Title:

```
Show HN: CallGuard – offline Android call filter that blocks unknown numbers
```

Первый комментарий (технические детали — HN любит именно это):

```
Author here. The app uses CallScreeningService with the ROLE_CALL_SCREENING role, so the call is
handed over before the phone rings and can be rejected without a ringtone.

Notes on the implementation:
- no AndroidX, no Gradle: a PowerShell script runs aapt2 → javac → d8 → zipalign → apksigner
- phone numbers are canonicalised through PhoneNumberUtils (ICU) with the country from SIM/network,
  so it works outside Russia too
- contacts are cached for 5 minutes; provider errors fail open (call is allowed)
- charts are drawn on a plain Canvas view, no chart library
- targetSdk 34, minSdk 29, APK 275 KB

The app is deliberately narrow: it does not try to be a spam database, it just answers "is this
number in my contacts".
```

## 5. 4PDA

Заголовок темы:

```
Стоп-звонки (CallGuard) — фильтр звонков с незнакомых номеров
```

Описание:

```
Приложение для Android 10+, которое сбрасывает входящие звонки с номеров, которых нет в адресной
книге, и ведёт журнал решений.

Возможности:
• сброс до первого гудка через системную роль «Фильтрация вызовов»;
• журнал: номер, время, причина (нет в контактах / расписание / пауза);
• пауза на 30 минут, 1, 2 или 3 часа; расписание на будни;
• «пропустить следующий звонок» — выключается после первого прошедшего незнакомого;
• белый список для больниц и доставок;
• статистика: день/неделя/месяц/год, график по дням и по часам;
• тихое уведомление о работе фильтра;
• русский и английский интерфейс.

Без рекламы, без аккаунта, без интернета (кроме проверки обновлений на GitHub).
Размер 275 КБ, лицензия MIT, исходники открыты.

Скачать: https://github.com/garneva13/stop-calls/releases/latest
Сайт: https://garneva13.github.io/stop-calls/
```

## 6. Хабр

Заголовок:

```
Фильтр звонков на чистом Android: CallScreeningService, ICU-номера и сборка без Gradle
```

Структура статьи:

1. **Проблема.** Спам-звонки, почему готовые решения требуют облака и читают контакты.
2. **Почему CallScreeningService.** Роль `ROLE_CALL_SCREENING`, вызов приходит до гудка,
   ограничение в 5 секунд на ответ, `setDisallowCall(true)`.
3. **Ловушка с ролью.** Пользователь выбирает «Телефон» вместо приложения — фильтр молчит;
   как это лечится диагностикой и текстом в диалоге.
4. **Номера.** `PhoneNumberUtils.formatNumberToE164` + страна из SIM/сети; почему эвристика
   «последние 10 цифр» ломается в Норвегии и Бразилии.
5. **Сборка без Gradle.** aapt2 → javac → d8 → zipalign → apksigner, 275 КБ, почему это
   быстрее и прозрачнее обычного шаблона.
6. **Статистика на Canvas.** Свой `BarChartView` без библиотек: сетка, шаг оси, подписи.
7. **Что не получилось.** Скрытые номера недоступны, iOS невозможен, маркер STIR/SHAKEN
   в России почти не передаётся.
8. **Ссылки.** Репозиторий, APK, страница.

## 7. Telegram

```
Обновил «Стоп-звонки» до 1.12: статистика с графиками, выбор времени паузы, английский интерфейс.

Приложение сбрасывает звонки с номеров, которых нет в контактах, и ничего не отправляет в сеть.
Android 10+, 275 КБ, бесплатно.

https://github.com/garneva13/stop-calls/releases/latest
```

## 8. Awesome-списки

PR в `awesome-android` / `awesome-privacy` имеет смысл после первых 20–30 звёзд и
нескольких живых issues — иначе выглядит как самореклама. Формат добавления: одна
строка с ссылкой и коротким описанием в существующий раздел.

## Чего не делать

- не покупать звёзды и не просить «поставить звезду за звезду» — GitHub это ловит;
- не постить один и тот же текст в десяток Telegram-чатов подряд;
- не публиковать на всех площадках в один день: не будет понятно, что сработало.

## Метрики

- **Insights → Traffic**: просмотры, уникальные посетители, клоны, откуда перешли.
- **Releases**: счётчик скачиваний у APK.
- **Google Search Console**: после пуша отправить `sitemap.xml`, смотреть запросы.
- Раз в неделю — одна строка в заметках: дата, площадка, просмотры, скачивания.
