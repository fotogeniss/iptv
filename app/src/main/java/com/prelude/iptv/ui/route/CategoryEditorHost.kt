package com.prelude.iptv.ui.route

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.prelude.iptv.ui.MainViewModel
import com.prelude.iptv.ui.isTvDevice
import com.prelude.iptv.ui.mobile.settings.MobileEditCategoriesScreen
import com.prelude.iptv.ui.tv.settings.TvCategoryEditorScreen

/**
 * Ο ενιαίος επεξεργαστής κατηγοριών, πάνω από ό,τι φαίνεται.
 *
 * Ανοίγει από τις Ρυθμίσεις, από το φύλλο κατηγοριών της Live και από το μενού
 * της TV — όλα περνούν από το [MainViewModel.openCategoryEditor]. Ζει σε ένα
 * σημείο (MainActivity) ώστε να μην υπάρχουν δύο διαφορετικές οθόνες για το
 * ίδιο πράγμα, όπως πριν (διάλογος με κουτάκια + επεξεργαστής στις Ρυθμίσεις).
 */
@Composable
internal fun CategoryEditorHost(vm: MainViewModel) {
    val state by vm.categoryEditorState.collectAsStateWithLifecycle()
    if (!state.visible) return
    val modifier = Modifier.fillMaxSize().systemBarsPadding()
    if (isTvDevice()) {
        TvCategoryEditorScreen(
            state = state,
            onLayoutChange = vm::updateCategoryEditorLayout,
            onBack = vm::closeCategoryEditor,
            modifier = modifier,
        )
    } else {
        MobileEditCategoriesScreen(
            state = state,
            onLayoutChange = vm::updateCategoryEditorLayout,
            onBack = vm::closeCategoryEditor,
            modifier = modifier,
        )
    }
}
