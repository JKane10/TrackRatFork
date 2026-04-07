package com.trackrat.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.trackrat.android.data.models.OperationsSummaryResponse

/**
 * Collapsible card displaying a route operations summary.
 * Matches the iOS OperationsSummaryView: headline collapsed, body expanded.
 * Hides automatically when there is no data.
 */
@Composable
fun OperationsSummaryCard(
    summary: OperationsSummaryResponse?,
    modifier: Modifier = Modifier
) {
    if (summary == null || summary.body.isBlank()) return

    val hasHeadline = summary.headline.isNotBlank()

    if (hasHeadline) {
        // Collapsible variant: headline always visible, body on tap
        CollapsibleSummary(summary = summary, modifier = modifier)
    } else {
        // Simple variant: just show the body text
        GlassmorphicCard(
            modifier = modifier.fillMaxWidth(),
            cornerRadius = 10.dp,
            padding = 0.dp
        ) {
            Text(
                text = summary.body,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun CollapsibleSummary(
    summary: OperationsSummaryResponse,
    modifier: Modifier = Modifier
) {
    var isExpanded by rememberSaveable { mutableStateOf(false) }

    GlassmorphicCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 10.dp,
        padding = 0.dp
    ) {
        Column {
            // Header row (always visible, tappable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = summary.headline,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = if (isExpanded)
                        Icons.Default.KeyboardArrowUp
                    else
                        Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    tint = Color.White.copy(alpha = 0.5f)
                )
            }

            // Expanded body
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Divider(
                        color = Color.White.copy(alpha = 0.2f),
                        modifier = Modifier.padding(horizontal = 14.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = summary.body,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        lineHeight = MaterialTheme.typography.bodySmall.lineHeight,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 0.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}
