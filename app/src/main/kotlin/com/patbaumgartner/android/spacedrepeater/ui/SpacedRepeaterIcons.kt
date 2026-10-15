package com.patbaumgartner.android.spacedrepeater.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.patbaumgartner.android.spacedrepeater.R

/**
 * The Material icons this app draws, vendored as vector drawables (same policy as the watchdog reference).
 */
internal object SpacedRepeaterIcons {
    val ArrowBack: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_arrow_back)

    val Add: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_add)

    val Stats: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_stats)

    val Settings: ImageVector
        @Composable get() = ImageVector.vectorResource(R.drawable.ic_settings)
}
