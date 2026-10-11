package com.prelude.iptv.data

/**
 * Transient, source-scoped loading state. Catalog contents are never persisted;
 * this model only describes the currently running network/parse operation.
 */
data class SourceLoadProgress(
    val percent: Int? = null,
    val stage: String = "",
    val contentType: String = "live",
    val active: Boolean = false
)

typealias SourceProgressCallback = (percent: Int?, stage: String) -> Unit

/** Publishes the provider category id/title catalogue already fetched by a section load. */
typealias SourceCategoriesCallback = (categories: List<Pair<String, String>>) -> Unit

/** Publishes an immutable accumulated catalog while provider loading continues. */
typealias SourcePartialCallback = (items: List<Channel>) -> Unit

/**
 * ΠΟΣΑ ΣΤΟΙΧΕΙΑ ΕΧΕΙ ΠΡΑΓΜΑΤΙΚΑ ΚΑΘΕ ΚΑΤΗΓΟΡΙΑ, ΑΝΕΞΑΡΤΗΤΑ ΑΠΟ ΤΟ ΤΙ ΚΑΤΕΒΗΚΕ.
 *
 * ΓΙΑΤΙ ΧΩΡΙΣΤΟ ΑΠΟ ΤΟ [SourceCategoriesCallback]: εκείνο δίνει ονόματα, που τα
 * ξέρουμε από ΕΝΑ αίτημα· αυτό δίνει πλήθη, που το portal τα λέει μόνο μέσα στην
 * 1η σελίδα κάθε κατηγορίας (`total_items`). Μετρημένο σε πραγματικό portal:
 * `total_items=4282 · max_page_items=14` — δηλαδή το πλήθος είναι γνωστό αφού
 * κατέβουν 14 στοιχεία, όχι 4.282.
 *
 * ΤΙ ΞΕΚΛΕΙΔΩΝΕΙ: πλήθη κατηγοριών και σειρά ραγών που είναι ΣΩΣΤΑ όσο ο
 * κατάλογος κατεβαίνει ακόμη. Αυτά τα δύο, υπολογισμένα από τα μισοφορτωμένα,
 * ήταν η αιτία που το προοδευτικό μοντέλο απορρίφθηκε.
 *
 * Κενός χάρτης = ο πάροχος δεν δηλώνει σύνολα· τότε ισχύει η μέτρηση των
 * φορτωμένων, όπως πριν.
 */
typealias SourceCategoryTotalsCallback = (totals: Map<String, Int>, pageSize: Int) -> Unit
