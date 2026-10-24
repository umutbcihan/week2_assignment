# DevCluster: Donanım ve Sunucu Kaynak Yönetim Simülatörü
### Proje: week2_assignment

Bu proje, Google'ın modern standardı olan **Bildirimsel Arayüz (Declarative UI)** yaklaşımını, **Tek Yönlü Veri Akışı (Unidirectional Data Flow - UDF)** prensiplerini ve **RAM/CPU dostu bellek yönetimi** mekanizmalarını Jetpack Compose üzerinde simüle eden bir Android uygulamasıdır.

---

## 🛠 Mimari ve Teknik Gereksinimlerin Karşılanması

### A. Veri Katmanı (Data Layer) ve Derleyici Sözleşmesi
- **Değişmezlik (Immutability):** `HardwareItem` data class'ı oluşturulmuş ve çok iş parçacıklı (multithreaded) ortamlarda Race Condition hatalarını önlemek için tüm alanları istisnasız `val` olarak tanımlanmıştır.
- **Kararlılık Sözleşmesi:** Jetpack Compose derleyicisinin akıllı atlama (**Smart Recomposition Skipping**) motorunu tetiklemek adına sınıf `@Immutable` anotasyonu ile işaretlenmiştir.

### B. Durum Yönetimi ve Donanımsal Koruma (State Management)
- **Çalışma Zamanı Hafıza Kilidi (`remember`):** Toplam harcanan ödenek miktarı gibi ekranın dinamik durumları, Compose'un doğrusal hafıza yapısı olan **Slot Table** içerisine `remember` ile kilitlenmiştir.
- **Konfigürasyon Değişim Koruması (`rememberSaveable`):** Cihaz yan çevrildiğinde (Portrait → Landscape) Activity'nin baştan oluşturulması durumuna karşı, arama çubuğu metni (`searchQuery`) `rememberSaveable` ile RAM dışı geçici Bundle alanında muhafaza edilmiştir.
- **Turnike Mantığı (Memoization - `remember(searchQuery)`):** Arama ve kategori filtreleme algoritması, her recomposition'da CPU tüketmemesi için `remember(searchQuery)` önbellek filtresi arkasına alınmıştır. Algoritma yalnızca anahtar kelime değiştiğinde çalışır.

### C. Liste Mimarisi ve GPU Hücre Dönüşümü
- **Tembel Yükleme (`LazyColumn`):** Akıcı performans için standart `forEach` döngüleri yerine `LazyColumn` mimarisi kullanılmıştır.
- **Nokta Atışı Recomposition (`key = { it.id }`):** Eleman güncellemelerinde tüm listenin baştan çizilmesini engellemek için `items` bloğuna benzersiz kimlik parametresi `key = { it.id }` verilmiştir.

### D. Mimari Ayrışma Disiplini (Stateless UI & State Hoisting)
- **Saf Fonksiyon Tasarımı (Stateless UI):** `HardwareCard` ve `HardwareSearchBar` tamamen **Stateless** olarak kodlanmış; içlerinde hiçbir yerel `remember` hücresi barındırmamaktadır.
- **State Hoisting:** Durumlar yukarıdan parametre olarak alınmış (State Down), kullanıcı etkileşimleri ise Lambda Callbacks (Events Up) ile merkezi `DevClusterScreen` bileşenine iletilmiştir.
- **Modifier Sıralaması:** Kart bileşenlerinde görsel margin ve ripple taşmalarını önlemek amacıyla önce `.clickable()`, hemen ardından `.padding()` hiyerarşisi işletilmiştir.

---

## 📁 Proje Dosya Yapısı
Proje ana dosyası gereksinimde belirtildiği üzere:
`app/src/main/java/com/example/week2/MainActivity.kt`
