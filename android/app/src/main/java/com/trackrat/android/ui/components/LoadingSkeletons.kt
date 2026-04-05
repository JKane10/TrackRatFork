package com.trackrat.android.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.trackrat.android.utils.Constants

/**
 * Loading skeleton components for improved UX during data loading
 */

/**
 * Shimmer animation effect for loading states
 */
@Composable
private fun shimmerBrush(): Brush {
    val shimmerColors = listOf(
        Color.White.copy(alpha = 0.12f),
        Color.White.copy(alpha = 0.25f),
        Color.White.copy(alpha = 0.12f),
    )
    
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(Constants.LOADING_SKELETON_SHIMMER_DURATION_MS.toInt()),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    
    return Brush.linearGradient(
        colors = shimmerColors,
        start = Offset.Zero,
        end = Offset(x = translateAnim, y = translateAnim)
    )
}

/**
 * Generic shimmer box component
 */
@Composable
private fun ShimmerBox(
    modifier: Modifier = Modifier,
    cornerRadius: Int = Constants.BORDER_RADIUS_SMALL_DP
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(cornerRadius.dp))
            .background(shimmerBrush())
    )
}

/**
 * Train card loading skeleton — mirrors the compact single-row TrainCardContent layout.
 */
@Composable
fun TrainCardSkeleton(
    modifier: Modifier = Modifier
) {
    GlassmorphicCard(
        modifier = modifier.fillMaxWidth(),
        cornerRadius = 12.dp,
        padding = 10.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: train number + destination
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                ShimmerBox(modifier = Modifier.width(80.dp).height(18.dp))
                ShimmerBox(modifier = Modifier.width(120.dp).height(13.dp))
            }

            // Right: time range + track chip + status chip
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShimmerBox(modifier = Modifier.width(110.dp).height(16.dp))
                ShimmerBox(
                    modifier = Modifier.width(36.dp).height(20.dp),
                    cornerRadius = 6
                )
                ShimmerBox(
                    modifier = Modifier.width(56.dp).height(20.dp),
                    cornerRadius = Constants.BORDER_RADIUS_LARGE_DP
                )
            }
        }
    }
}

/**
 * Train list loading skeleton
 */
@Composable
fun TrainListSkeleton(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Constants.PADDING_MEDIUM_DP.dp),
        verticalArrangement = Arrangement.spacedBy(Constants.PADDING_SMALL_DP.dp)
    ) {
        items(Constants.SHIMMER_COUNT_TRAIN_LIST) {
            TrainCardSkeleton()
        }
    }
}

/**
 * Train detail stop skeleton
 */
@Composable
fun TrainStopSkeleton(
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Constants.PADDING_MEDIUM_DP.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Constants.PADDING_MEDIUM_DP.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status indicator
                ShimmerBox(
                    modifier = Modifier.size(12.dp),
                    cornerRadius = 6 // Circular
                )
                
                Column {
                    // Station name
                    ShimmerBox(
                        modifier = Modifier
                            .width(100.dp)
                            .height(18.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Time info
                    ShimmerBox(
                        modifier = Modifier
                            .width(80.dp)
                            .height(14.dp)
                    )
                }
            }
            
            // Track or time
            ShimmerBox(
                modifier = Modifier
                    .width(50.dp)
                    .height(16.dp)
            )
        }
    }
}

/**
 * Train detail loading skeleton
 */
@Composable
fun TrainDetailSkeleton(
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(Constants.PADDING_MEDIUM_DP.dp),
        verticalArrangement = Arrangement.spacedBy(Constants.PADDING_SMALL_DP.dp)
    ) {
        // Header section
        item {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(Constants.PADDING_MEDIUM_DP.dp)
                ) {
                    // Route
                    ShimmerBox(
                        modifier = Modifier
                            .width(200.dp)
                            .height(20.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Status
                    ShimmerBox(
                        modifier = Modifier
                            .width(120.dp)
                            .height(16.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Progress bar
                    ShimmerBox(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Progress text
                    ShimmerBox(
                        modifier = Modifier
                            .width(150.dp)
                            .height(14.dp)
                    )
                }
            }
        }
        
        // Stops list
        items(Constants.SHIMMER_COUNT_TRAIN_DETAIL_STOPS) {
            TrainStopSkeleton()
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TrainCardSkeletonPreview() {
    TrainCardSkeleton()
}

@Preview(showBackground = true)
@Composable
fun TrainDetailSkeletonPreview() {
    TrainDetailSkeleton(modifier = Modifier.height(400.dp))
}