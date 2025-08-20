package com.nervesparks.iris.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nervesparks.iris.data.search.SearchResult
import com.nervesparks.iris.ui.theme.ComponentStyles
import com.nervesparks.iris.ui.theme.SemanticColors

@Composable
fun SearchResultCard(
    result: SearchResult,
    onResultClick: (SearchResult) -> Unit = {}
) {
    val infiniteTransition = rememberInfiniteTransition(label = "search_result")
    
    // Subtle pulsing animation for the result card
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "result_alpha"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = ComponentStyles.smallPadding)
            .clickable { onResultClick(result) },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = alpha),
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = ComponentStyles.smallElevation),
        shape = ComponentStyles.smallCardShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(ComponentStyles.defaultPadding)
        ) {
            // Title with source indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = result.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.width(ComponentStyles.smallPadding))
                
                // Source badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(SemanticColors.Info.copy(alpha = 0.2f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = result.source,
                        style = MaterialTheme.typography.bodySmall,
                        color = SemanticColors.Info,
                        fontSize = 10.sp
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(ComponentStyles.smallSpacing))
            
            // Snippet
            Text(
                text = result.snippet,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            
            Spacer(modifier = Modifier.height(ComponentStyles.smallSpacing))
            
            // URL
            Text(
                text = result.url,
                style = MaterialTheme.typography.bodySmall,
                color = SemanticColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SearchResultsSection(
    results: List<SearchResult>,
    query: String,
    onResultClick: (SearchResult) -> Unit = {}
) {
    val context = LocalContext.current
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(ComponentStyles.defaultPadding)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🔍",
                fontSize = 20.sp
            )
            
            Spacer(modifier = Modifier.width(ComponentStyles.smallPadding))
            
            Text(
                text = "Search Results",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            Text(
                text = "${results.size} results",
                style = MaterialTheme.typography.bodySmall,
                color = SemanticColors.TextSecondary
            )
        }
        
        Spacer(modifier = Modifier.height(ComponentStyles.smallPadding))
        
        // Query display
        Text(
            text = "Query: \"$query\"",
            style = MaterialTheme.typography.bodySmall,
            color = SemanticColors.TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(ComponentStyles.defaultSpacing))
        
        // Results
        results.forEach { result ->
            SearchResultCard(
                result = result,
                onResultClick = onResultClick
            )
        }
        
        // Disclaimer
        Spacer(modifier = Modifier.height(ComponentStyles.defaultSpacing))
        
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = SemanticColors.Warning.copy(alpha = 0.1f)
            ),
            shape = ComponentStyles.smallCardShape
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(ComponentStyles.defaultPadding),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "⚠️",
                    fontSize = 16.sp
                )
                
                Spacer(modifier = Modifier.width(ComponentStyles.smallPadding))
                
                Text(
                    text = "Please verify important information from these search results.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SemanticColors.Warning
                )
            }
        }
    }
} 