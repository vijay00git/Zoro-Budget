package com.example.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Overlays a subtle background-colored gradient at whichever edge(s) of a horizontally
 * scrolling row still have more content to reveal, so a row of chips clipped by the screen
 * edge doesn't look like it simply ends there. Wrap the scrolling row in a Box and add this as
 * a sibling with Modifier.matchParentSize().
 */
@Composable
fun ScrollEdgeFade(
    listState: LazyListState,
    modifier: Modifier = Modifier,
    edgeColor: Color = MaterialTheme.colorScheme.background
) {
    ScrollEdgeFade(
        canScrollBackward = listState.canScrollBackward,
        canScrollForward = listState.canScrollForward,
        modifier = modifier,
        edgeColor = edgeColor
    )
}

/** Overload for a plain `Modifier.horizontalScroll(ScrollState)` row instead of a LazyRow. */
@Composable
fun ScrollEdgeFade(
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
    edgeColor: Color = MaterialTheme.colorScheme.background
) {
    ScrollEdgeFade(
        canScrollBackward = scrollState.canScrollBackward,
        canScrollForward = scrollState.canScrollForward,
        modifier = modifier,
        edgeColor = edgeColor
    )
}

@Composable
private fun ScrollEdgeFade(
    canScrollBackward: Boolean,
    canScrollForward: Boolean,
    modifier: Modifier,
    edgeColor: Color
) {
    Box(modifier = modifier) {
        if (canScrollBackward) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .fillMaxHeight()
                    .width(20.dp)
                    .background(Brush.horizontalGradient(listOf(edgeColor, Color.Transparent)))
            )
        }
        if (canScrollForward) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxHeight()
                    .width(20.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, edgeColor)))
            )
        }
    }
}
