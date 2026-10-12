package com.prelude.iptv.ui.components.details

/**
 * Η διάρκεια όπως τη δίνουν οι πάροχοι είναι ασυνεπής: «101», «101 min»,
 * «01:41:00», «6060» (δευτερόλεπτα). Εδώ γίνεται λεπτά, ώστε το UI να γράφει
 * «1ώ 41′» αντί για ένα γυμνό «101» που μοιάζει με αριθμό επεισοδίου.
 */
object DetailDurationPolicy {
    private val CLOCK = Regex("""^\s*(\d{1,2}):(\d{2})(?::(\d{2}))?\s*$""")
    private val HOURS_MINUTES = Regex("""(\d+)\s*h(?:ours?)?\s*(\d+)?\s*m?""", RegexOption.IGNORE_CASE)
    private val NUMBER = Regex("""^\s*(\d+)\s*(min|mins|minutes|')?\s*$""", RegexOption.IGNORE_CASE)

    /** Λεπτά, ή null αν η τιμή δεν αναγνωρίζεται (τότε το UI δείχνει την αρχική). */
    fun minutes(raw: String): Int? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        CLOCK.find(text)?.let { match ->
            val first = match.groupValues[1].toInt()
            val second = match.groupValues[2].toInt()
            // «1:41:00» = ώρες:λεπτά:δευτ. · «1:41» = ώρες:λεπτά
            return (first * 60 + second).takeIf { it > 0 }
        }
        NUMBER.find(text)?.let { match ->
            val value = match.groupValues[1].toInt()
            if (value <= 0) return null
            // Πάνω από 10 ώρες σε λεπτά δεν έχει νόημα: είναι δευτερόλεπτα.
            return if (match.groupValues[2].isEmpty() && value > 600) (value + 30) / 60 else value
        }
        HOURS_MINUTES.find(text)?.let { match ->
            val hours = match.groupValues[1].toInt()
            val mins = match.groupValues[2].toIntOrNull() ?: 0
            return (hours * 60 + mins).takeIf { it > 0 }
        }
        return null
    }
}
