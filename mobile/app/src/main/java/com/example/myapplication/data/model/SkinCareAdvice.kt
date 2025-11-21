package com.example.myapplication.data.model

/**
 * Cilt bakım önerileri
 */
object SkinCareAdvice {

    /**
     * Akne tespit edildiğinde öneriler
     */
    val acneDetectedAdvice = listOf(
        "Yüzünüzü günde 2 kez ılık su ve yüz temizleyici ile yıkayın.",
        "Sivilceleri sıkmayın veya patlatmayın, iz kalmasına neden olabilir.",
        "Yağsız (oil-free) nemlendirici kullanın.",
        "Güneş koruyucu kullanmayı ihmal etmeyin (SPF 30+).",
        "Stres seviyenizi azaltmaya çalışın, düzenli uyuyun.",
        "Bol su için, günde en az 2 litre.",
        "Şekerli ve yağlı yiyecekleri azaltın.",
        "Yüzünüze sık sık dokunmaktan kaçının.",
        "Makyaj malzemelerinizi düzenli temizleyin.",
        "Kalıcı sorun için dermatologa danışın."
    )

    /**
     * Akne tespit edilmediğinde öneriler
     */
    val noAcneAdvice = listOf(
        "Harika! Cildiniz temiz görünüyor.",
        "Günlük cilt bakım rutininizi sürdürün.",
        "Yüzünüzü günde 2 kez temizlemeye devam edin.",
        "Nemlendirici kullanmaya devam edin.",
        "Güneş koruyucu kullanmayı ihmal etmeyin.",
        "Bol su için, cildiniz için çok önemli.",
        "Sağlıklı beslenmeye devam edin.",
        "Düzenli uyku cildiniz için önemlidir.",
        "Yastık kılıfınızı düzenli değiştirin.",
        "Cildiniz için koruyucu bakıma devam edin."
    )

    /**
     * Genel cilt bakım ipuçları
     */
    val generalTips = listOf(
        " Nemlendirme", "Cildinizi her gün nemlendirin",
        "️ Güneş Koruması", "SPF 30+ güneş kremi kullanın",
        " Su Tüketimi", "Günde en az 8 bardak su için",
        " Temizlik", "Yüzünüzü günde 2 kez yıkayın",
        " Uyku", "Günde 7-8 saat uyuyun",
        " Beslenme", "Sebze ve meyve tüketin",
        " Stres", "Stresi azaltın, meditasyon yapın",
        " Egzersiz", "Haftada 3-4 kez egzersiz yapın"
    )
}