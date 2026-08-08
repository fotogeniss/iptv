# v1.40.9 — View All & Subtitle Matching

## Mobile Home — «Προβολή όλων»

- Το label είναι πλέον πραγματικό clickable action με άνετο touch target.
- Κάθε rail κρατά preview έως 20 στοιχεία, αλλά και την πλήρη deduplicated λίστα του.
- Προστέθηκε πλήρης οθόνη κατηγορίας δύο στηλών με Back και κοινό mobile bottom navigation.
- Το Back επιστρέφει στο ίδιο Home rail state.
- Άνοιγμα κάρτας οδηγεί στα Details, όπως και στις υπόλοιπες VOD/Series ροές.

## OpenSubtitles matching

- Προστέθηκε pure `SubtitleSearchPolicy`.
- Καθαρίζονται provider prefixes, quality/release tags, έτη και άσχετα decorations.
- Ταινίες αναζητούνται με καθαρό τίτλο + έτος + `type=movie`.
- Επεισόδια αναζητούνται με τίτλο σειράς + έτος + season + episode + `type=episode`.
- Η ακριβής ταυτότητα επεισοδίων διατηρείται και στο prev/next/autoplay queue.
- Τα αποτελέσματα επαναταξινομούνται με title/year/season/episode match πριν από τα downloads.
- Η χειροκίνητη αναζήτηση αναγνωρίζει `S01E02`, `S01 E02`, `1x02` και Season/Episode labels.

## Version

- `versionName`: 1.40.9
- `versionCode`: 53
