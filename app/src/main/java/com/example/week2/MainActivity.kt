package com.example.week2

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

// =========================================================================
// A. VERİ KATMANI (DATA LAYER) VE DERLEYİCİ SÖZLEŞMESİ
// =========================================================================
// 1. Değişmezlik (Immutability): Çok iş parçacıklı asenkron süreçlerde Race Condition
//    hatalarını engellemek amacıyla sınıfın tüm alanları val anahtar kelimesiyle tanımlanmıştır.
// 2. Kararlılık Sözleşmesi: Jetpack Compose derleyicisinin akıllı atlama
//    (Smart Recomposition Skipping) motorunu tetiklemek için @Immutable ile işaretlenmiştir.
@Immutable
data class HardwareItem(
    val id: String,
    val name: String,
    val category: String,
    val cost: Double,
    val stockCount: Int
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Color(0xFF64B5F6),
                    surface = Color(0xFF1E1E2E),
                    background = Color(0xFF12121A)
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DevClusterScreen()
                }
            }
        }
    }
}

// =========================================================================
// MERKEZİ STATEFUL KAPSAYICI (STATE CONTAINER - STATE HOISTING)
// =========================================================================
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevClusterScreen() {
    // Sabit Test Veri Kümesi
    val initialHardwareList = remember {
        listOf(
            HardwareItem(id = "101", name = "MacBook Pro M3 Max", category = "Geliştirici Bilgisayarı", cost = 3499.0, stockCount = 5),
            HardwareItem(id = "102", name = "NVIDIA RTX 4090 Sunucu", category = "Yapay Zeka / GPU", cost = 1599.0, stockCount = 2),
            HardwareItem(id = "103", name = "Google Pixel 8 Pro", category = "Mobil Test Cihazı", cost = 999.0, stockCount = 8),
            HardwareItem(id = "104", name = "Dell UltraSharp 49\"", category = "Monitör", cost = 1199.0, stockCount = 4),
            HardwareItem(id = "105", name = "Sony WH-1000XM5 Kulaklık", category = "Odaklanma Ekipmanı", cost = 399.0, stockCount = 12),
            HardwareItem(id = "106", name = "Raspberry Pi 5 8GB", category = "IoT Geliştirme Kiti", cost = 85.0, stockCount = 25),
            HardwareItem(id = "107", name = "HHKB Professional Keyboard", category = "Ergonomik Klavye", cost = 299.0, stockCount = 0), // Stok sınırı test verisi
            HardwareItem(id = "108", name = "AMD Ryzen Threadripper", category = "Yapay Zeka / GPU", cost = 4999.0, stockCount = 1)
        )
    }

    // B.1 ÇALIŞMA ZAMANI HAFIZA KİLİDİ (remember):
    // Toplam harcanan ödenek miktarı gibi ekranın dinamik durumları,
    // Compose'un doğrusal hafıza yapısı olan Slot Table içerisine remember makrosuyla kilitlenmiştir.
    var totalSpentBudget by remember { mutableDoubleStateOf(0.0) }
    var lastAllocatedItemName by remember { mutableStateOf<String?>(null) }

    // B.2 KONFİGÜRASYON DEĞİŞİM KORUMASI (rememberSaveable):
    // Kullanıcının arama çubuğuna harf yazdığı esnada cihazı yan çevirmesi (Portrait -> Landscape)
    // durumunda arama durumu rememberSaveable ile RAM dışı geçici Bundle alanında korunur.
    var searchQuery by rememberSaveable { mutableStateOf("") }

    // B.3 TURNİKE MANTIĞI (MEMOIZATION - remember(searchQuery)):
    // Arama veya kategori filtreleme algoritmaları, her Recomposition tetiklenmesinde boş yere
    // CPU tüketmemesi için remember(searchQuery) önbellek filtresi arkasına alınmıştır.
    // Algoritma yalnızca searchQuery veya veri kümesi değiştiğinde çalışır.
    val filteredHardwareList = remember(searchQuery, initialHardwareList) {
        if (searchQuery.isBlank()) {
            initialHardwareList
        } else {
            val query = searchQuery.trim().lowercase(Locale.getDefault())
            initialHardwareList.filter { item ->
                item.name.lowercase(Locale.getDefault()).contains(query) ||
                item.category.lowercase(Locale.getDefault()).contains(query)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "DevCluster: Donanım ve Sunucu Kaynak Yönetimi",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = "Toplam Harcanan Ödenek: $%.2f".format(totalSpentBudget),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // D.2 STATE HOISTING (State Down, Events Up):
            // HardwareSearchBar tamamen Stateless tasarlanmıştır; durumu yukarıdan alır,
            // olayları lambda geri çağırımları (callbacks) ile merkeze fırlatır.
            HardwareSearchBar(
                query = searchQuery,
                onQueryChange = { newQuery -> searchQuery = newQuery },
                onClearClick = { searchQuery = "" },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (lastAllocatedItemName != null) {
                Text(
                    text = "Son Tahsis: $lastAllocatedItemName",
                    fontSize = 12.sp,
                    color = Color(0xFF81C784),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }

            // C.1 TEMBEL YÜKLEME (LazyColumn):
            // Donanımların ekranda akıcı listelenmesi için kesinlikle standart forEach döngüleri
            // kullanılmamış, LazyColumn mimarisi entegre edilmiştir.
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                // C.2 NOKTA ATIŞI RECOMPOSITION (key = { it.id }):
                // Listenin ortasından bir eleman güncellendiğinde ya da silindiğinde tüm ekranın
                // baştan çizilmesini önlemek amacıyla benzersiz kimlik (key = { it.id }) zorunlu geçilmiştir.
                items(
                    items = filteredHardwareList,
                    key = { it.id }
                ) { item ->
                    // D.1 & D.2 SAF FONKSİYON TASARIMI (Stateless UI):
                    // HardwareCard tamamen Stateless'tır ve hiçbir yerel remember hücresi barındırmaz.
                    HardwareCard(
                        item = item,
                        onItemClick = { clickedItem ->
                            // Stok kontrolü: Stok 0 ise ödenek eklenmez
                            if (clickedItem.stockCount > 0) {
                                totalSpentBudget += clickedItem.cost
                                lastAllocatedItemName = clickedItem.name
                            }
                        }
                    )
                }
            }
        }
    }
}

// =========================================================================
// D.1 STATELESS ARAMA ÇUBUĞU (SAF FONKSİYON)
// İçinde hiçbir yerel remember hücresi barındırmaz.
// =========================================================================
@Composable
fun HardwareSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClearClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Donanım veya kategori ara...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Ara"
            )
        },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = onClearClick) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Temizle"
                    )
                }
            }
        },
        singleLine = true,
        shape = RoundedCornerShape(12.dp)
    )
}

// =========================================================================
// D.1 & D.3 STATELESS DONANIM KARTI (SAF FONKSİYON VE MODIFIER SIRALAMASI)
// İçinde hiçbir yerel remember hücresi barındırmaz.
// =========================================================================
@Composable
fun HardwareCard(
    item: HardwareItem,
    onItemClick: (HardwareItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val isOutOfStock = item.stockCount == 0

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isOutOfStock) Color(0xFF262633) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        // D.3 MODIFIER SIRALAMASI:
        // Kart bileşenlerinde önce .clickable(), ardından .padding() hiyerarşisi işletilerek
        // görsel margin hatalarının ve ripple taşmalarının önüne geçilmiştir.
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = !isOutOfStock) {
                onItemClick(item)
            }
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (isOutOfStock) Color.Gray else Color.White
                )
                Text(
                    text = "$${item.cost}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 15.sp,
                    color = if (isOutOfStock) Color.Gray else MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF2C2C40),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = item.category,
                        fontSize = 11.sp,
                        color = Color(0xFFB0B0C8),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = if (isOutOfStock) "Tükendi (Stok Sınırı)" else "Stok: ${item.stockCount} adet",
                    fontSize = 12.sp,
                    fontWeight = if (isOutOfStock) FontWeight.Bold else FontWeight.Normal,
                    color = if (isOutOfStock) Color(0xFFEF5350) else Color(0xFFA5D6A7)
                )
            }
        }
    }
}
