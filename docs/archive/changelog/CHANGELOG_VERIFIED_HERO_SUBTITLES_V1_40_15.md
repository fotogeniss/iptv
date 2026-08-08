# v1.40.15 — TV Hero & Subtitle Size Reliability

## TV Home / Hero

- Διατηρείται αρχικό DPAD focus στο κουμπί Παρακολούθηση.
- Το `LazyColumn` επαναφέρεται ρητά στην κορυφή αφού ολοκληρωθεί το focus request, ώστε το focus bring-into-view να μη μετακινεί τον Hero κάτω από το επάνω chrome.
- Ο premium TV Hero απέκτησε μεγαλύτερο ασφαλές ύψος, top/bottom padding και line height.
- Ο legacy TV Hero απέκτησε επίσης ασφαλές top padding και αφαιρέθηκε το ανεξέλεγκτο auto-focus request.

## Μέγεθος υποτίτλων

- Η επιλογή παραμένει 70%–180%, ανά 10%, και αποθηκεύεται ανά προφίλ.
- Κάθε αλλαγή δείχνει άμεσα πάνω στο βίντεο ορατό δείγμα στο νέο μέγεθος.
- Οι εξωτερικοί SRT/OpenSubtitles κάνουν `requestLayout()`/`invalidate()` μετά την αλλαγή.
- Στον ExoPlayer απενεργοποιούνται μόνο τα embedded font sizes, ώστε το επιλεγμένο μέγεθος να μην αντικαθίσταται από το αρχείο υποτίτλων.
- Το μέγεθος επανεφαρμόζεται μετά από δημιουργία player, αλλαγή tracks, πρώτο rendered frame και αλλαγή video size.
- Στο VLC οι εξωτερικοί υπότιτλοι συνεχίζουν να χρησιμοποιούν το δικό μας overlay και αλλάζουν μέγεθος. Οι embedded VLC υπότιτλοι εμφανίζουν πλέον σαφή περιορισμό, επειδή η τρέχουσα ενσωμάτωση δεν διαθέτει live size setter.

## Version

- `versionName`: `1.40.15`
- `versionCode`: `59`
