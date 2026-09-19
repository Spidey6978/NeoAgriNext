package com.example.agrinext.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.agrinext.data.CropRoadmap
import com.example.agrinext.data.MarketInfo
import com.example.agrinext.data.RecommendedCrop
import com.example.agrinext.data.SatelliteData
import com.example.agrinext.util.LanguageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Shared Data Model
data class FarmLand(
    val id: Long,
    var name: String,
    val polygonPoints: List<Pair<Float, Float>>,
    val centroidLat: Double,
    val centroidLng: Double,
    var savedMarketInfo: MarketInfo? = null,
    var savedRoadmap: CropRoadmap? = null,
    var savedSatelliteData: SatelliteData? = null,
    var savedAnalysisText: String? = null,
    var savedRecommendedCrops: List<RecommendedCrop>? = null,
    var savedLocationName: String? = null,
    var imageUri: String? = null,
    var isFarming: Boolean = false,
    var farmingStartDate: Long? = null,
    var farmingCropName: String? = null
)

@Composable
fun FarmLandComponent(
    farmLand: FarmLand,
    onClick: () -> Unit,
    onEditClick: () -> Unit, // UPDATED: Re-added parameter
    modifier: Modifier = Modifier
) {
    val polygonFillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
    val polygonStrokeColor = MaterialTheme.colorScheme.primary
    val drawingAreaBackground = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)

    val containerColor = MaterialTheme.colorScheme.surfaceVariant
    val nameTextColor = if (containerColor.luminance() < 0.5f) Color.White else Color.DarkGray

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Polygon Drawing Area OR Image
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(drawingAreaBackground),
                contentAlignment = Alignment.Center
            ) {
                if (!farmLand.imageUri.isNullOrBlank()) {
                    val uri = try { Uri.parse(farmLand.imageUri) } catch(e: Exception) { null }
                    val bitmap = rememberBitmapFromUriForComponent(uri)

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = "Farm Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        PolygonCanvas(farmLand, polygonFillColor, polygonStrokeColor)
                    }
                } else {
                    PolygonCanvas(farmLand, polygonFillColor, polygonStrokeColor)
                }

                // Active Farming Indicator
                if (farmLand.isFarming) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Locked",
                                tint = MaterialTheme.colorScheme.onSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // Footer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = LanguageManager.get(farmLand.name),
                        style = MaterialTheme.typography.titleLarge,
                        color = nameTextColor
                    )

                    Text(
                        text = "${LanguageManager.get("Loc")}: ${String.format("%.2f", farmLand.centroidLat)}, ${String.format("%.2f", farmLand.centroidLng)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = nameTextColor.copy(alpha = 0.7f)
                    )
                }

                // UPDATED: Connected Edit Button
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(32.dp),
                    enabled = !farmLand.isFarming
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = LanguageManager.get("Edit Farm"),
                        tint = if (farmLand.isFarming) Color.Gray else MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun PolygonCanvas(farmLand: FarmLand, fillColor: Color, strokeColor: Color) {
    Box(modifier = Modifier.padding(16.dp).fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (farmLand.polygonPoints.isNotEmpty()) {
                val path = Path()
                val width = size.width
                val height = size.height

                val start = farmLand.polygonPoints.first()
                path.moveTo(start.first * width, start.second * height)

                for (i in 1 until farmLand.polygonPoints.size) {
                    val point = farmLand.polygonPoints[i]
                    path.lineTo(point.first * width, point.second * height)
                }
                path.close()

                drawPath(path = path, color = fillColor)
                drawPath(path = path, color = strokeColor, style = Stroke(width = 3.dp.toPx()))
            }
        }
    }
}

@Composable
fun rememberBitmapFromUriForComponent(uri: Uri?): ImageBitmap? {
    val context = LocalContext.current
    var bitmap by remember(uri) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(uri) {
        if (uri != null) {
            withContext(Dispatchers.IO) {
                try {
                    val inputStream = context.contentResolver.openInputStream(uri)
                    val androidBitmap = BitmapFactory.decodeStream(inputStream)
                    bitmap = androidBitmap?.asImageBitmap()
                } catch (e: Exception) {
                    e.printStackTrace()
                    bitmap = null
                }
            }
        } else {
            bitmap = null
        }
    }
    return bitmap
}