package com.prelude.iptv.category

/**
 * Ομαδοποίηση κατηγοριών ανά «χώρα» από το πρόθεμα του παρόχου.
 *
 * Οι πάροχοι γράφουν «GR | Αθλητικά», «UK: News», «[US] Movies», «IT - Sport».
 * Το πρόθεμα (2–3 κεφαλαία γράμματα πριν από διαχωριστικό) γίνεται ομάδα, ώστε
 * ένα πάτημα να ανάβει/σβήνει όλες τις κατηγορίες της χώρας μαζί.
 *
 * Καθαρός κώδικας χωρίς Android: τον μοιράζονται κινητό, TV και τα tests.
 */
object CategoryCountryPolicy {
    /** Ομάδα κατηγοριών· `code == null` σημαίνει «Άλλα» (χωρίς πρόθεμα ή σπάνιο πρόθεμα). */
    data class CountryGroup(val code: String?, val ids: List<String>)

    /** Πόσες ομάδες χωρούν στο πλέγμα πριν από τα «Άλλα». */
    const val MAX_GROUPS = 7

    private val PREFIX = Regex("""^\s*[\[(|]?\s*(\p{Lu}{2,3})\s*[\])|:\-–—•·▎┃»/]""")

    // Ετικέτες ποιότητας/τύπου που μοιάζουν με χώρα αλλά δεν είναι.
    private val NOT_COUNTRIES = setOf(
        "VOD", "PPV", "HD", "FHD", "UHD", "SD", "TV", "XXX", "NEW", "ALL", "VIP", "HEVC", "MIX", "24",
    )

    fun countryOf(title: String): String? {
        val code = PREFIX.find(title)?.groupValues?.get(1) ?: return null
        return code.takeUnless { it in NOT_COUNTRIES }
    }

    /**
     * Οι ομάδες με σειρά μεγέθους. Ένα πρόθεμα μετρά ως χώρα μόνο αν το έχουν
     * τουλάχιστον δύο κατηγορίες. Αν βγαίνουν λιγότερες από δύο χώρες, το πλέγμα
     * δεν βοηθά σε τίποτα και επιστρέφεται κενή λίστα.
     */
    fun groups(options: List<CategoryOption>): List<CountryGroup> {
        val byCode = options.groupBy { countryOf(it.title) }
        val countries = byCode
            .filterKeys { it != null }
            .filterValues { it.size >= 2 }
            .entries
            .sortedWith(compareByDescending<Map.Entry<String?, List<CategoryOption>>> { it.value.size }.thenBy { it.key })
        if (countries.size < 2) return emptyList()
        val shown = countries.take(MAX_GROUPS)
        val shownCodes = shown.map { it.key }.toSet()
        val others = options.filter { countryOf(it.title) !in shownCodes }
        return shown.map { CountryGroup(it.key, it.value.map(CategoryOption::id)) } +
            listOfNotNull(others.takeIf { it.isNotEmpty() }?.let { CountryGroup(null, it.map(CategoryOption::id)) })
    }
}
