package com.example.myapplication.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.myapplication.data.local.AcneDatabase
import com.example.myapplication.data.model.AcneAnalysis
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * İlerleme takibi ekranı
 */
@Composable
fun ProgressScreen(database: AcneDatabase) {
    val analyses by database.acneAnalysisDao().getAllAnalyses()
        .collectAsState(initial = emptyList())

    // Son 7 günlük verileri filtrele
    val sevenDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
    val recentAnalyses = analyses.filter { it.timestamp >= sevenDaysAgo }

    // İstatistikler
    val totalCount = analyses.size
    val acneCount = analyses.count { it.prediction == 1 }
    val noAcneCount = analyses.count { it.prediction == 0 }
    val recentAcneCount = recentAnalyses.count { it.prediction == 1 }

    // Trend hesapla
    val trend = calculateTrend(recentAnalyses)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "İlerleme Takibi",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Genel İstatistikler
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Genel İstatistikler",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    StatRow("Toplam Analiz", totalCount.toString())
                    StatRow("Akne Tespit Edildi", acneCount.toString())
                    StatRow("Akne Tespit Edilmedi", noAcneCount.toString())

                    if (totalCount > 0) {
                        val percentage = (acneCount * 100) / totalCount
                        StatRow("Akne Oranı", "%$percentage")
                    }
                }
            }
        }

        // Son 7 Gün
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Son 7 Gün",
                        style = MaterialTheme.typography.titleLarge
                    )

                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    StatRow("Analiz Sayısı", recentAnalyses.size.toString())
                    StatRow("Akne Tespit Edildi", recentAcneCount.toString())

                    // Trend
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Trend:",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = trend.text,
                            style = MaterialTheme.typography.titleMedium,
                            color = trend.color
                        )
                    }
                }
            }
        }

        // Günlük Dağılım
        if (recentAnalyses.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Günlük Dağılım (Son 7 Gün)",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        DailyChart(recentAnalyses)
                    }
                }
            }
        }

        // Mesaj
        item {
            if (analyses.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Henüz analiz yapılmadı.\nİlerleme takibi için analiz yapın.",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (recentAnalyses.size < 3) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Daha iyi trend analizi için en az 3 analiz gerekli.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun DailyChart(analyses: List<AcneAnalysis>) {
    // Günlere göre grupla
    val dateFormat = SimpleDateFormat("dd MMM", Locale("tr", "TR"))
    val grouped = analyses.groupBy { analysis ->
        dateFormat.format(Date(analysis.timestamp))
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grouped.forEach { (date, dayAnalyses) ->
            val acneCount = dayAnalyses.count { it.prediction == 1 }
            val totalCount = dayAnalyses.size

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = date,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(80.dp)
                )

                // Progress bar
                LinearProgressIndicator(
                    progress = if (totalCount > 0) acneCount.toFloat() / totalCount else 0f,
                    modifier = Modifier
                        .weight(1f)
                        .height(24.dp)
                        .padding(horizontal = 8.dp),
                    color = if (acneCount > 0)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "$acneCount/$totalCount",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.width(60.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

private data class TrendInfo(
    val text: String,
    val color: Color
)

private fun calculateTrend(analyses: List<AcneAnalysis>): TrendInfo {
    if (analyses.size < 3) {
        return TrendInfo("Yetersiz Veri", Color.Gray)
    }

    // İlk yarı ve son yarıyı karşılaştır
    val midPoint = analyses.size / 2
    val firstHalf = analyses.take(midPoint)
    val secondHalf = analyses.takeLast(midPoint)

    val firstHalfAcneRate = firstHalf.count { it.prediction == 1 }.toFloat() / firstHalf.size
    val secondHalfAcneRate = secondHalf.count { it.prediction == 1 }.toFloat() / secondHalf.size

    return when {
        secondHalfAcneRate < firstHalfAcneRate - 0.1f -> TrendInfo("İyileşiyor", Color(0xFF4CAF50))
        secondHalfAcneRate > firstHalfAcneRate + 0.1f -> TrendInfo("Kötüleşiyor", Color(0xFFF44336))
        else -> TrendInfo("Stabil", Color(0xFFFF9800))
    }
}