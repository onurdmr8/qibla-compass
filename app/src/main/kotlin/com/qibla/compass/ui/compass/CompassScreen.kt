package com.qibla.compass.ui.compass

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qibla.compass.R
import com.qibla.compass.model.City
import com.qibla.compass.model.PredefinedCities
import com.qibla.compass.ui.components.AdBanner
import com.qibla.compass.util.QiblaCalculator
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompassScreen(
    heading: Float,
    isCalibrated: Boolean,
    currentCity: City,
    onCitySelected: (City) -> Unit,
    onRequestGps: () -> Unit
) {
    var showCitySheet by remember { mutableStateOf(false) }
    var showCalibrationDialog by remember { mutableStateOf(false) }

    val qiblaBearing = remember(currentCity) {
        QiblaCalculator.calculateQiblaBearing(currentCity.latitude, currentCity.longitude)
    }

    val distanceKm = remember(currentCity) {
        QiblaCalculator.calculateDistanceToKaaba(currentCity.latitude, currentCity.longitude)
    }

    // Kıble açısı ile telefon başlığı arasındaki fark
    var angleDiff = abs(heading - qiblaBearing)
    if (angleDiff > 180f) angleDiff = 360f - angleDiff
    val isAligned = angleDiff <= 3.0f

    val emeraldColor = Color(0xFF0F3B2C)
    val goldColor = Color(0xFFD4AF37)
    val brightGold = Color(0xFFFFD700)
    val alignedGreen = Color(0xFF10B981)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${currentCity.name}, ${currentCity.country}",
                            fontSize = 12.sp,
                            color = goldColor
                        )
                    }
                },
                actions = {
                    // Konum / Şehir değiştir
                    IconButton(onClick = { showCitySheet = true }) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = stringResource(R.string.select_city),
                            tint = Color.White
                        )
                    }
                    // GPS Konumunu Al
                    IconButton(onClick = onRequestGps) {
                        Icon(
                            imageVector = Icons.Default.MyLocation,
                            contentDescription = stringResource(R.string.gps_location),
                            tint = brightGold
                        )
                    }
                    // Kalibrasyon Yardımı
                    IconButton(onClick = { showCalibrationDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = stringResource(R.string.calibration_title),
                            tint = if (isCalibrated) Color.White.copy(alpha = 0.7f) else Color(0xFFF59E0B)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = emeraldColor)
            )
        },
        bottomBar = {
            AdBanner()
        },
        containerColor = Color(0xFF07140E)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Hizalama Durum Rozeti (Aligned Badge)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isAligned) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = alignedGreen),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.aligned_to_qibla),
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    Text(
                        text = stringResource(R.string.turn_until_aligned),
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Dönen 360° Pusula Kadranı
            QiblaCompassView(
                heading = heading,
                qiblaBearing = qiblaBearing,
                isAligned = isAligned,
                modifier = Modifier.size(310.dp)
            )

            // Alt Bilgi Kartları
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = emeraldColor.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, if (isAligned) alignedGreen else goldColor.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Kıble Açısı
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.qibla_angle),
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${qiblaBearing.toInt()}°",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = brightGold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = QiblaCalculator.getCardinalDirection(qiblaBearing),
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(40.dp)
                            .background(Color.White.copy(alpha = 0.15f))
                    )

                    // Kabe'ye Mesafe
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = stringResource(R.string.distance_to_kaaba),
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Text(
                            text = "${distanceKm.toInt()} km",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "Mekke-i Mükerreme",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // Şehir değiştirme butonu
            OutlinedButton(
                onClick = { showCitySheet = true },
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, goldColor.copy(alpha = 0.5f)),
                modifier = Modifier.padding(bottom = 6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = goldColor,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${stringResource(R.string.change_city)} (${currentCity.name})",
                    color = Color.White,
                    fontSize = 13.sp
                )
            }
        }
    }

    // Şehir Seçim Sheet'i
    if (showCitySheet) {
        CitySelectionSheet(
            cities = PredefinedCities.list,
            selectedCity = currentCity,
            onCitySelect = {
                onCitySelected(it)
                showCitySheet = false
            },
            onDismiss = { showCitySheet = false }
        )
    }

    // Kalibrasyon Yardım Penceresi
    if (showCalibrationDialog) {
        AlertDialog(
            onDismissRequest = { showCalibrationDialog = false },
            title = { Text(stringResource(R.string.calibration_title)) },
            text = {
                Column {
                    Text(
                        stringResource(R.string.calibration_desc),
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "İpucu: Telefonunuzu elinizde havada sekiz (8) çizecek şekilde birkaç kez hareket ettirmeniz manyetik sensörü anında kalibre eder.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showCalibrationDialog = false }) {
                    Text("Tamam")
                }
            }
        )
    }
}

@Composable
fun QiblaCompassView(
    heading: Float,
    qiblaBearing: Float,
    isAligned: Boolean,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        // Sabit Tepe Yön Oku (Telefonun baktığı yön)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val arrowTop = 6.dp.toPx()
            val arrowWidth = 14.dp.toPx()
            val arrowHeight = 18.dp.toPx()

            val arrowPath = Path().apply {
                moveTo(center.x, arrowTop)
                lineTo(center.x - arrowWidth / 2, arrowTop + arrowHeight)
                lineTo(center.x + arrowWidth / 2, arrowTop + arrowHeight)
                close()
            }
            drawPath(
                path = arrowPath,
                color = if (isAligned) Color(0xFF10B981) else Color(0xFFFFD700)
            )
        }

        // Dönen Kadran (Heading'e göre döner)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
                .rotate(-heading)
        ) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2 - 8.dp.toPx()

            // Dış Altın Halka
            drawCircle(
                color = Color(0xFF1E3A2B),
                radius = radius,
                style = Stroke(width = 4.dp.toPx())
            )
            drawCircle(
                color = Color(0xFFD4AF37).copy(alpha = 0.4f),
                radius = radius - 6.dp.toPx(),
                style = Stroke(width = 1.dp.toPx())
            )

            // Derece Çizgileri
            for (deg in 0 until 360 step 5) {
                val isMajor = deg % 30 == 0
                val tickLength = if (isMajor) 14.dp.toPx() else 7.dp.toPx()
                val tickColor = when {
                    deg == 0 -> Color(0xFFEF4444) // Kuzey Kırmızı
                    isMajor -> Color(0xFFD4AF37)
                    else -> Color.White.copy(alpha = 0.2f)
                }
                val strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx()

                val rad = Math.toRadians(deg.toDouble())
                val startX = center.x + (radius - tickLength) * sin(rad).toFloat()
                val startY = center.y - (radius - tickLength) * cos(rad).toFloat()
                val endX = center.x + radius * sin(rad).toFloat()
                val endY = center.y - radius * cos(rad).toFloat()

                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = strokeWidth
                )
            }

            // Kabe Yönü İşaretçisi (Kadran üzerinde tam Kıble açısında yeşil/altın Kabe küpü)
            val qiblaRad = Math.toRadians(qiblaBearing.toDouble())
            val kaabaDistance = radius - 30.dp.toPx()
            val kaabaX = center.x + kaabaDistance * sin(qiblaRad).toFloat()
            val kaabaY = center.y - kaabaDistance * cos(qiblaRad).toFloat()

            // Kıble yönü çizgisi
            drawLine(
                color = Color(0xFF10B981).copy(alpha = 0.7f),
                start = center,
                end = Offset(kaabaX, kaabaY),
                strokeWidth = 2.dp.toPx()
            )

            // Kabe İkonu Dairesi
            drawCircle(
                color = Color(0xFF10B981),
                radius = 16.dp.toPx(),
                center = Offset(kaabaX, kaabaY)
            )
            drawCircle(
                color = Color(0xFF0F1713),
                radius = 12.dp.toPx(),
                center = Offset(kaabaX, kaabaY)
            )
            // Kabe altın bant
            drawCircle(
                color = Color(0xFFFFD700),
                radius = 4.dp.toPx(),
                center = Offset(kaabaX, kaabaY)
            )
        }

        // Merkez Pusula Göbeği
        Box(
            modifier = Modifier
                .size(70.dp)
                .shadow(12.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF2C5E48),
                            Color(0xFF0F261C)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${heading.toInt()}°",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CitySelectionSheet(
    cities: List<City>,
    selectedCity: City,
    onCitySelect: (City) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = stringResource(R.string.select_city),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(cities) { city ->
                    val isSelected = city.name == selectedCity.name
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCitySelect(city) },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = city.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = city.country,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            Text(
                                text = "${QiblaCalculator.calculateQiblaBearing(city.latitude, city.longitude).toInt()}°",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
