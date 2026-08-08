package com.prelude.iptv.ui.mobile.settings

internal enum class MobileLegalTab(val label: String) {
    PRIVACY("Απόρρητο"),
    TERMS("Όροι"),
    SERVICES("Υπηρεσίες"),
}

internal data class MobileLegalDisclosure(
    val id: String,
    val title: String,
    val summary: String,
    val details: String,
    val icon: MobileLegalIcon,
)

internal data class MobileLegalService(
    val id: String,
    val badge: String,
    val title: String,
    val description: String,
    val status: String,
)

internal data class MobileLegalTerm(
    val title: String,
    val body: String,
)

internal enum class MobileLegalIcon {
    STORAGE,
    FAVORITES,
    CACHE,
    NETWORK,
}

/**
 * Single in-app source for the approved mobile legal presentation.
 *
 * The publishable, long-form sources remain in docs/PRIVACY_POLICY.md and
 * docs/TERMS_OF_USE.md. Any behavior change must update both places before a
 * store release.
 */
internal object MobileLegalContent {
    const val POLICY_VERSION = "1.1-draft"
    const val EFFECTIVE_DATE = "2 Αυγούστου 2026"
    const val TMDB_ATTRIBUTION = "This product uses the TMDB API but is not endorsed or certified by TMDB."

    // Must be replaced with the entity and address used by the Play listing.
    const val PUBLISHER_LEGAL_NAME = ""
    const val PRIVACY_EMAIL = ""

    val identityConfigured: Boolean
        get() = PUBLISHER_LEGAL_NAME.isNotBlank() && PRIVACY_EMAIL.isNotBlank()

    val localDisclosures = listOf(
        MobileLegalDisclosure(
            id = "sources",
            title = "Λίστες και πηγές",
            summary = "URL, στοιχεία σύνδεσης και ρυθμίσεις παρόχου",
            details = "Τα ευαίσθητα στοιχεία σύνδεσης αποθηκεύονται κρυπτογραφημένα με κλειδιά του Android Keystore. Δεν συγχρονίζονται σε server του PRELUDE+.",
            icon = MobileLegalIcon.STORAGE,
        ),
        MobileLegalDisclosure(
            id = "preferences",
            title = "Προτιμήσεις προβολής",
            summary = "Αγαπημένα, ιστορικό, πρόοδος και ρυθμίσεις",
            details = "Χρησιμοποιούνται μόνο για λειτουργίες στη συσκευή, όπως συνέχιση προβολής και εξατομίκευση. Διαγράφονται από τις αντίστοιχες ρυθμίσεις, με εκκαθάριση δεδομένων Android ή με απεγκατάσταση.",
            icon = MobileLegalIcon.FAVORITES,
        ),
        MobileLegalDisclosure(
            id = "cache",
            title = "Cache μεταδεδομένων",
            summary = "Αφίσες, περιγραφές, EPG και προσωρινά αρχεία",
            details = "Η cache κρατά προσωρινά δεδομένα για ταχύτερη λειτουργία και μπορεί να καθαριστεί χωρίς να διαγραφούν οι αποθηκευμένες πηγές.",
            icon = MobileLegalIcon.CACHE,
        ),
    )

    val networkDisclosures = listOf(
        MobileLegalDisclosure(
            id = "network",
            title = "Αιτήματα που ξεκινάς εσύ",
            summary = "IPTV, TMDB, OpenSubtitles και Google Play",
            details = "Η εφαρμογή συνδέεται απευθείας στις πηγές που προσθέτεις. Για μεταδεδομένα ή υπότιτλους αποστέλλονται στοιχεία αναζήτησης, όπως τίτλος, έτος, σεζόν και επεισόδιο, στις αντίστοιχες υπηρεσίες.",
            icon = MobileLegalIcon.NETWORK,
        ),
        MobileLegalDisclosure(
            id = "diagnostics",
            title = "Προαιρετικά διαγνωστικά σταθερότητας",
            summary = "Crash και ANR reports μόνο μετά από δική σου επιλογή",
            details = "Η συλλογή είναι κλειστή εξ αρχής. Αν την ενεργοποιήσεις ή επιλέξεις αποστολή μία φορά, μπορεί να σταλούν στο Firebase Crashlytics τεχνικά στοιχεία όπως exception, stack trace, έκδοση εφαρμογής, μοντέλο συσκευής και έκδοση Android. Δεν προσθέτουμε URLs λιστών, credentials, τίτλους media, προφίλ ή analytics events.",
            icon = MobileLegalIcon.NETWORK,
        ),
    )

    val terms = listOf(
        MobileLegalTerm(
            "1. Ανεξάρτητος media player",
            "Το PRELUDE+ δεν παρέχει, δεν φιλοξενεί και δεν πωλεί τηλεοπτικά κανάλια, λίστες, συνδρομές ή οπτικοακουστικό περιεχόμενο.",
        ),
        MobileLegalTerm(
            "2. Πηγές του χρήστη",
            "Προσθέτεις μόνο πηγές και περιεχόμενο για τα οποία έχεις νόμιμη άδεια πρόσβασης. Εσύ ευθύνεσαι για τη νομιμότητα, τη διαθεσιμότητα και τους όρους του παρόχου σου.",
        ),
        MobileLegalTerm(
            "3. Ασφάλεια στοιχείων",
            "Εσύ ευθύνεσαι για την ασφάλεια των κωδικών παρόχου, των PIN και των password-protected backup που δημιουργείς ή κοινοποιείς.",
        ),
        MobileLegalTerm(
            "4. Υπηρεσίες τρίτων",
            "Οι IPTV providers, το TMDB, το OpenSubtitles, το Google Play και το προαιρετικό Firebase Crashlytics είναι ανεξάρτητες υπηρεσίες. Η διαθεσιμότητα και οι πρακτικές τους διέπονται από τους δικούς τους όρους.",
        ),
        MobileLegalTerm(
            "5. Αγορές Premium",
            "Οι αγορές διεκπεραιώνονται από το Google Play. Τιμή, φόροι, αποδείξεις, επιστροφές χρημάτων και λογαριασμός πληρωμών ελέγχονται από τη Google και την εφαρμοστέα νομοθεσία.",
        ),
        MobileLegalTerm(
            "6. Διαθεσιμότητα",
            "Streams, EPG, υπότιτλοι και metadata μπορεί να είναι ελλιπή ή μη διαθέσιμα λόγω δικτύου, συσκευής ή τρίτου παρόχου. Δεν εγγυόμαστε αδιάλειπτη λειτουργία υπηρεσιών τρίτων.",
        ),
        MobileLegalTerm(
            "7. Αλλαγές",
            "Κάθε ουσιώδης αλλαγή στις πρακτικές δεδομένων ή στους όρους θα συνοδεύεται από νέα ημερομηνία ισχύος και ενημερωμένη έκδοση πολιτικής.",
        ),
    )

    val services = listOf(
        MobileLegalService(
            id = "iptv",
            badge = "IPTV",
            title = "Ο πάροχος της λίστας σου",
            description = "Λήψη καταλόγου, EPG και ροής. Τα δεδομένα πηγαίνουν απευθείας στον server που επέλεξες.",
            status = "ΕΛΕΓΧΕΤΑΙ ΑΠΟ ΤΟΝ ΧΡΗΣΤΗ",
        ),
        MobileLegalService(
            id = "tmdb",
            badge = "TMDB",
            title = "The Movie Database",
            description = "Αναζήτηση αφισών, βαθμολογιών, περιγραφών, ηθοποιών και στοιχείων επεισοδίων.",
            status = "ΠΡΟΑΙΡΕΤΙΚΟ API KEY",
        ),
        MobileLegalService(
            id = "opensubtitles",
            badge = "OS",
            title = "OpenSubtitles",
            description = "Αυτόματη ή χειροκίνητη αναζήτηση και λήψη υποτίτλων με βάση την ταυτότητα του τίτλου.",
            status = "ΠΡΟΑΙΡΕΤΙΚΗ ΣΥΝΔΕΣΗ",
        ),
        MobileLegalService(
            id = "google_play",
            badge = "PLAY",
            title = "Google Play",
            description = "Διεκπεραίωση αγοράς, αποκατάσταση αγοράς και κατάσταση Premium.",
            status = "ΜΟΝΟ ΓΙΑ ΑΓΟΡΕΣ",
        ),
        MobileLegalService(
            id = "firebase_crashlytics",
            badge = "CRASH",
            title = "Firebase Crashlytics",
            description = "Προαιρετική αποστολή τεχνικών crash και ANR reports για διάγνωση προβλημάτων σταθερότητας.",
            status = "OPT-IN · ΧΩΡΙΣ ANALYTICS",
        ),
    )
}
