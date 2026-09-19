package com.example.agrinext.ui.components

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize // Added missing import
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke // Added missing import
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.agrinext.Config
import com.example.agrinext.data.AgriNextApi
import com.example.agrinext.data.NewsItem
import com.example.agrinext.util.LanguageManager
import com.google.gson.GsonBuilder
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Shared memory cache to persist news items between screen transitions
 * and avoid repeated AI generation calls.
 */
object NewsSessionCache {
    var cachedItems: List<NewsItem>? = null
}

@Composable
fun NewsCarouselComponent(
    onNewsClick: (Int) -> Unit = {}
) {
    val context = LocalContext.current

    // --- State Management ---
    var newsItems by remember { mutableStateOf(NewsSessionCache.cachedItems ?: emptyList()) }
    var isLoading by remember { mutableStateOf(NewsSessionCache.cachedItems == null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Expanded item state - handles the "expand in length" logic
    var expandedNewsItem by remember { mutableStateOf<NewsItem?>(null) }

    // Setup Retrofit with high timeout for AI content generation
    val retrofit = remember {
        val client = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
        val gson = GsonBuilder().setLenient().create()
        Retrofit.Builder()
            .baseUrl(Config.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(AgriNextApi::class.java)
    }

    // Effect to fetch data if cache is empty
    LaunchedEffect(Unit) {
        if (NewsSessionCache.cachedItems == null) {
            isLoading = true
            errorMessage = null
            try {
                val response = retrofit.getAgriNews()
                if (response.isSuccessful && response.body() != null) {
                    val fetched = response.body()!!
                    NewsSessionCache.cachedItems = fetched
                    newsItems = fetched
                } else {
                    errorMessage = "Unable to fetch news (Error ${response.code()})"
                }
            } catch (e: Exception) {
                Log.e("NewsCarousel", "Network Error: ${e.message}")
                errorMessage = "Network connection failed."
            } finally {
                isLoading = false
            }
        }
    }

    // Handle back button to collapse the detail view
    BackHandler(enabled = expandedNewsItem != null) {
        expandedNewsItem = null
    }

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val cardWidth = 300.dp

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = spring()) // Makes height change smooth
    ) {
        // 1. HEADER
        Text(
            text = LanguageManager.get("Local Agri-News"),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 12.dp, start = 20.dp)
        )

        // 2. LOADING / CAROUSEL AREA
        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 3.dp)
            }
        } else if (newsItems.isNotEmpty()) {
            // Pager implementation with crash-guards
            val safeItemCount = newsItems.size
            key(safeItemCount) {
                val pagerState = rememberPagerState(initialPage = safeItemCount * 100) { Int.MAX_VALUE }

                HorizontalPager(
                    state = pagerState,
                    contentPadding = PaddingValues(horizontal = (screenWidth - cardWidth) / 2),
                    pageSpacing = 16.dp,
                    pageSize = PageSize.Fixed(cardWidth),
                    modifier = Modifier.graphicsLayer { clip = false }
                ) { pageIndex ->
                    val item = newsItems[pageIndex % safeItemCount]
                    NewsItemCard(
                        news = item,
                        isCurrentlyExpanded = expandedNewsItem?.id == item.id,
                        onClick = {
                            // Toggle expansion: if clicking same item, close it; otherwise open it.
                            expandedNewsItem = if (expandedNewsItem?.id == item.id) null else item
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Indicator Dots
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    repeat(safeItemCount) { iteration ->
                        val isSelected = (pagerState.currentPage % safeItemCount) == iteration
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                .size(if (isSelected) 10.dp else 8.dp)
                        )
                    }
                }
            }
        } else {
            // Error State
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp).height(100.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = errorMessage ?: "No news available.", color = Color.Gray)
                }
            }
        }

        // 3. INLINE EXPANDED CONTENT
        // This expands the length of the component "there and there only"
        AnimatedVisibility(
            visible = expandedNewsItem != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            expandedNewsItem?.let { news ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Header with Close Button
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = LanguageManager.get(news.category),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            IconButton(
                                onClick = { expandedNewsItem = null },
                                modifier = Modifier.size(32.dp).background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                            ) {
                                Icon(Icons.Default.Close, "Close", modifier = Modifier.size(16.dp))
                            }
                        }

                        // Full Content Text
                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                            Text(
                                text = LanguageManager.get(news.title),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = LanguageManager.get(news.content ?: "Detailed content currently unavailable."),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                lineHeight = 26.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${LanguageManager.get("Source")}: ${LanguageManager.get(news.source)} • ${news.timeAgo}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsItemCard(
    news: NewsItem,
    isCurrentlyExpanded: Boolean,
    onClick: () -> Unit
) {
    val categoryColor = when(news.category.lowercase()) {
        "weather" -> Color(0xFF1565C0)
        "market" -> Color(0xFFE65100)
        "policy" -> Color(0xFF2E7D32)
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(280.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isCurrentlyExpanded) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        ),
        border = if (isCurrentlyExpanded) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!news.imageUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(news.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Default.Image, "News", tint = Color.Gray, modifier = Modifier.size(40.dp))
                }
            }

            Column(
                modifier = Modifier.padding(16.dp).fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(categoryColor.copy(alpha = 0.1f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = LanguageManager.get(news.category),
                            style = MaterialTheme.typography.labelSmall,
                            color = categoryColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = LanguageManager.get(news.timeAgo),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }

                Text(
                    text = LanguageManager.get(news.title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = if (isCurrentlyExpanded) LanguageManager.get("Showing Details")
                    else "${LanguageManager.get("Source")}: ${LanguageManager.get(news.source)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCurrentlyExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isCurrentlyExpanded) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
    }
}