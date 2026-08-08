# v1.40.16 — Direct Details-to-Player transition

## Διόρθωση

Στη ροή αναπαραγωγής από την οθόνη πληροφοριών αφαιρέθηκε το πρόωρο κλείσιμο του Details overlay.

Πριν:

1. Το `detailChannel` γινόταν `null` αμέσως μετά το πάτημα Αναπαραγωγή.
2. Η εφαρμογή εμφάνιζε την υποκείμενη Αρχική οθόνη.
3. Στο μεταξύ γινόταν resolve του playable URL και λήψη metadata/EPG, έως 2,5 δευτερόλεπτα.
4. Μετά άνοιγε το `PlayerActivity`.

Τώρα:

- Το Details παραμένει στην οθόνη όσο ετοιμάζεται η αναπαραγωγή.
- Το `PlayerActivity` ανοίγει χωρίς ενδιάμεσο Home flash.
- Η ίδια κοινή ροή καλύπτει κινητό και Android TV.
- Με Back από τον player ο χρήστης επιστρέφει στις πληροφορίες της ταινίας ή του επεισοδίου.
- Δεν άλλαξαν queue, subtitle request, URL resolution, resume ή provider behavior.
