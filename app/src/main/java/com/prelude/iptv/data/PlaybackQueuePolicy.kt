package com.prelude.iptv.data

/** Pure queue selection used before opening PlayerActivity. */
object PlaybackQueuePolicy {
    data class Result(val items: List<Channel>, val index: Int)

    fun prepare(
        target: Channel,
        requested: List<Channel>?,
        fallback: List<Channel>
    ): Result {
        val source = requested?.takeIf { it.isNotEmpty() }
            ?: fallback.takeIf { it.isNotEmpty() }
            ?: listOf(target)
        val targetKey = PlaybackQueue.favKey(target)
        val index = source.indexOfFirst { candidate ->
            val candidateKey = PlaybackQueue.favKey(candidate)
            if (targetKey.isNotBlank()) candidateKey == targetKey else candidate == target
        }

        // ΑΝ ΤΟ ΑΝΤΙΚΕΙΜΕΝΟ ΔΕΝ ΑΝΗΚΕΙ ΣΤΗ ΛΙΣΤΑ, Η ΛΙΣΤΑ ΔΕΝ ΕΙΝΑΙ ΤΟ ΠΛΑΙΣΙΟ ΤΟΥ.
        //
        // Πριν, εδώ υπήρχε «?: 0»: όταν το αντικείμενο δεν βρισκόταν, η ουρά έδειχνε
        // σιωπηλά στη ΘΕΣΗ 0. Έτσι έπαιζε το σωστό βίντεο (το URL περνά ξεχωριστά)
        // αλλά ο player έγραφε το όνομα του πρώτου της λίστας — εντελώς άσχετο.
        //
        // Συνέβαινε συστηματικά στο «Συνέχισε να βλέπεις»: το αντικείμενο έρχεται
        // από το ιστορικό, ενώ η ορατή λίστα είναι φιλτραρισμένη κατά ομάδα/τύπο,
        // οπότε σχεδόν ποτέ δεν το περιέχει.
        //
        // Σωστή απάντηση: ουρά ενός αντικειμένου. Καλύτερα χωρίς επόμενο/προηγούμενο
        // παρά με λάθος τίτλο και πλοήγηση σε άσχετα αντικείμενα.
        if (index < 0) return Result(listOf(target), 0)
        return Result(source, index)
    }
}
