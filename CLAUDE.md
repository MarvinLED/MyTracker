# Arbeitsanweisungen für Claude

## Verifikation

- **Keine Screenshots.** Nicht `adb exec-out screencap` o.ä. verwenden.
- **Keine Emulator-Tests, die Eingaben in Felder erfordern.** Also kein Durchklicken per
  `adb shell input tap/text/keyevent`, um einen Ablauf nachzuspielen.
- Verifikation stattdessen über: `./gradlew assembleDebug`, `./gradlew testDebugUnitTest` und  instrumentierte Tests (`connectedDebugAndroidTest`, z.B. `MigrationTest`).
- Wenn sich etwas nur am laufenden Gerät prüfen ließe: sagen, was ungeprüft bleibt, statt es  per Emulator-Eingaben nachzustellen.
- Schlage mir am Ende eine kurze Github Change Message vor. Nur einen Satz.  