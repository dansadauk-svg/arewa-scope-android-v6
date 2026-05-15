# Arewa Scope Android App v6 Notifications

This version is based on the working v5 branded app and adds Firebase Cloud Messaging.

## Important

The included `app/google-services.json` is a placeholder so the project can build. Replace it with your real Firebase file before expecting real notifications.

## Firebase Android app package name

Use this package name in Firebase:

`com.arewascope.app`

## Topic

The app automatically subscribes to this FCM topic:

`news`

Your WordPress push plugin must also send to the same topic: `news`.

## Build

Upload the extracted project files to GitHub root, then run:

Actions → Build Arewa Scope APK v6 Notifications → Run workflow

## Notification behaviour

When a notification contains a `url` data field, tapping it opens that post/page inside the app.
