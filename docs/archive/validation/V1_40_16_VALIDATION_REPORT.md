# Validation report — v1.40.16

## Scope

Στοχευμένη διόρθωση της μετάβασης:

`Details → Αναπαραγωγή → PlayerActivity`

σε κινητό και Android TV.

## Αιτία

Το κοινό callback του `DetailHost` στο `BrowseRoute.kt` εκτελούσε `detailChannel = null` πριν από το ασύγχρονο `openChannel(...)`. Το `openChannel` μπορεί να περιμένει URL resolution και metadata/EPG, οπότε η Αρχική γινόταν ορατή πριν ξεκινήσει ο player.

## Αλλαγή

Αφαιρέθηκε μόνο το πρόωρο `detailChannel = null`. Το Details κλείνει ακόμη κανονικά με το δικό του Back και από τις επιλογές του mobile navigation.

## Έλεγχοι που εκτελέστηκαν

- Kotlin PSI syntax validation production: `FILES=139 ERRORS=0`
- Kotlin PSI syntax validation tests: `FILES=20 ERRORS=0`
- Regression contracts:
  - το Details δεν κλείνει πριν από το `openChannel`: OK
  - το explicit Back του Details παραμένει: OK
  - το `PlayerActivity` launch contract παραμένει: OK
  - queue και subtitle requests συνεχίζουν να προωθούνται: OK
  - versionCode/versionName: OK
- ZIP integrity test: εκτελείται κατά τη συσκευασία.

## Περιορισμός validation

Το πλήρες `:app:compileDebugKotlin` δεν ξεκίνησε στο παρόν περιβάλλον. Ο Gradle wrapper προσπάθησε να κατεβάσει το Gradle 8.9, αλλά σταμάτησε με `UnresolvedAddressException` πριν από το Android/Kotlin compilation. Το σχετικό log περιλαμβάνεται ως `gradle_compile_attempt_v40_16.txt`.
