package com.example.ui.components

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Celebration
import androidx.compose.material.icons.filled.Checkroom
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Cookie
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.CurrencyRupee
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Diamond
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.LocalLaundryService
import androidx.compose.material.icons.filled.LocalParking
import androidx.compose.material.icons.filled.LocalPharmacy
import androidx.compose.material.icons.filled.LunchDining
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.SportsCricket
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TempleHindu
import androidx.compose.material.icons.filled.Train
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.R

/** A named group of icons in the category icon picker. */
data class CategoryIconGroup(@StringRes val title: Int, val icons: List<Pair<String, ImageVector>>)

/** Every icon a category can use, keyed by the name stored in CategoryEntity.iconName. */
val CategoryIconGroups = listOf(
    CategoryIconGroup(
        R.string.icon_group_food,
        listOf(
            "Restaurant" to Icons.Default.Restaurant, "Fastfood" to Icons.Default.Fastfood, "LunchDining" to Icons.Default.LunchDining,
            "LocalCafe" to Icons.Default.LocalCafe, "BakeryDining" to Icons.Default.BakeryDining, "Cookie" to Icons.Default.Cookie,
            "Icecream" to Icons.Default.Icecream, "SetMeal" to Icons.Default.SetMeal, "LocalDrink" to Icons.Default.LocalDrink
        )
    ),
    CategoryIconGroup(
        R.string.icon_group_groceries,
        listOf(
            "ShoppingCart" to Icons.Default.ShoppingCart, "Eco" to Icons.Default.Eco, "Grain" to Icons.Default.Grain,
            "Kitchen" to Icons.Default.Kitchen, "Store" to Icons.Default.Store, "ShoppingBag" to Icons.Default.ShoppingBag,
            "Checkroom" to Icons.Default.Checkroom, "Diamond" to Icons.Default.Diamond, "Devices" to Icons.Default.Devices
        )
    ),
    CategoryIconGroup(
        R.string.icon_group_home,
        listOf(
            "Home" to Icons.Default.Home, "ElectricBolt" to Icons.Default.ElectricBolt, "WaterDrop" to Icons.Default.WaterDrop,
            "Wifi" to Icons.Default.Wifi, "PhoneAndroid" to Icons.Default.PhoneAndroid, "Receipt" to Icons.Default.Receipt,
            "LocalLaundryService" to Icons.Default.LocalLaundryService, "Build" to Icons.Default.Build, "Pets" to Icons.Default.Pets
        )
    ),
    CategoryIconGroup(
        R.string.icon_group_travel,
        listOf(
            "DirectionsCar" to Icons.Default.DirectionsCar, "TwoWheeler" to Icons.Default.TwoWheeler, "LocalGasStation" to Icons.Default.LocalGasStation,
            "DirectionsBus" to Icons.Default.DirectionsBus, "Train" to Icons.Default.Train, "Flight" to Icons.Default.Flight,
            "LocalParking" to Icons.Default.LocalParking
        )
    ),
    CategoryIconGroup(
        R.string.icon_group_health,
        listOf(
            "LocalHospital" to Icons.Default.LocalHospital, "LocalPharmacy" to Icons.Default.LocalPharmacy, "FitnessCenter" to Icons.Default.FitnessCenter,
            "Spa" to Icons.Default.Spa, "ContentCut" to Icons.Default.ContentCut, "Shield" to Icons.Default.Shield
        )
    ),
    CategoryIconGroup(
        R.string.icon_group_people,
        listOf(
            "Person" to Icons.Default.Person, "ChildCare" to Icons.Default.ChildCare, "School" to Icons.Default.School,
            "MenuBook" to Icons.Default.MenuBook, "CardGiftcard" to Icons.Default.CardGiftcard, "Celebration" to Icons.Default.Celebration,
            "TempleHindu" to Icons.Default.TempleHindu, "VolunteerActivism" to Icons.Default.VolunteerActivism,
            "Movie" to Icons.Default.Movie, "MusicNote" to Icons.Default.MusicNote, "SportsCricket" to Icons.Default.SportsCricket
        )
    ),
    CategoryIconGroup(
        R.string.icon_group_money,
        listOf(
            "Payments" to Icons.Default.Payments, "CurrencyRupee" to Icons.Default.CurrencyRupee, "Savings" to Icons.Default.Savings,
            "AccountBalance" to Icons.Default.AccountBalance, "CreditCard" to Icons.Default.CreditCard, "SwapHoriz" to Icons.Default.SwapHoriz,
            "Work" to Icons.Default.Work
        )
    )
)

private val iconsByName: Map<String, ImageVector> = CategoryIconGroups.flatMap { it.icons }.toMap()

/**
 * Words in a category's name that suggest an icon, for categories still on the plain tag icon
 * (every custom category created before the icon picker existed). Tamil and English words both.
 */
private val nameHints: List<Pair<List<String>, String>> = listOf(
    listOf("non-veg", "nonveg", "chicken", "mutton", "fish", "meen", "meat", "kozhi", "egg", "muttai") to "SetMeal",
    listOf("veg", "vegetable", "kaikari", "keerai", "fruit", "pazham") to "Eco",
    listOf("ulundai", "ulundu", "dal", "paruppu", "rice", "arisi", "grain", "atta", "flour", "maavu") to "Grain",
    listOf("snack", "biscuit", "sweet") to "Cookie",
    listOf("milk", "paal", "curd", "thayir", "water can") to "LocalDrink",
    listOf("tea", "coffee", "kaapi") to "LocalCafe",
    listOf("hotel", "restaurant", "food", "dining", "swiggy", "zomato", "unavu") to "Restaurant",
    listOf("grocery", "groceries", "provision", "supermarket", "kirana") to "ShoppingCart",
    listOf("rent", "house", "veedu") to "Home",
    listOf("eb", "electric", "current", "power") to "ElectricBolt",
    listOf("water") to "WaterDrop",
    listOf("internet", "wifi", "broadband") to "Wifi",
    listOf("mobile", "phone", "recharge", "airtel", "jio") to "PhoneAndroid",
    listOf("bill", "utilities") to "Receipt",
    listOf("petrol", "diesel", "fuel") to "LocalGasStation",
    listOf("bike", "scooter", "rapido", "uber", "ola") to "TwoWheeler",
    listOf("car", "cab", "taxi", "auto", "transport") to "DirectionsCar",
    listOf("bus") to "DirectionsBus",
    listOf("train", "irctc", "metro") to "Train",
    listOf("flight", "travel", "trip") to "Flight",
    listOf("medical", "hospital", "doctor", "health") to "LocalHospital",
    listOf("medicine", "pharmacy", "tablet") to "LocalPharmacy",
    listOf("gym", "fitness") to "FitnessCenter",
    listOf("salon", "haircut", "saloon") to "ContentCut",
    listOf("cosmetic", "beauty", "makeup") to "Spa",
    listOf("insurance", "lic") to "Shield",
    listOf("school", "fees", "college", "education", "tuition") to "School",
    listOf("book") to "MenuBook",
    listOf("gift") to "CardGiftcard",
    listOf("function", "festival", "party", "marriage", "kalyanam") to "Celebration",
    listOf("temple", "kovil", "pooja", "puja", "donation") to "TempleHindu",
    listOf("movie", "cinema", "entertainment", "ott", "netflix") to "Movie",
    listOf("cloth", "dress", "textile") to "Checkroom",
    listOf("gold", "jewel") to "Diamond",
    listOf("shopping", "amazon", "flipkart") to "ShoppingBag",
    listOf("salary", "income") to "Payments",
    listOf("saving", "investment", "sip", "rd", "fd") to "Savings",
    listOf("emi", "loan", "bank") to "AccountBalance",
    listOf("credit card", "card") to "CreditCard",
    listOf("transfer") to "SwapHoriz",
    listOf("kid", "baby", "child") to "ChildCare",
    listOf("wife", "husband", "amma", "appa", "family", "parents") to "Person",
    listOf("given", "lent", "help") to "VolunteerActivism",
    listOf("pet", "dog") to "Pets"
)

/** The icon a category's name points to, or null when nothing matches. */
fun suggestedIconName(categoryName: String): String? {
    val name = categoryName.lowercase()
    val words = name.split(Regex("[^a-z0-9-]+")).filter { it.isNotBlank() }.toSet()
    return nameHints.firstOrNull { (hints, _) ->
        hints.any { hint -> if (hint.contains(' ') || hint.length > 4) name.contains(hint) else hint in words || words.any { it.startsWith(hint) && hint.length >= 3 } }
    }?.second
}

/**
 * Resolves a CategoryEntity.iconName to its icon. A category still on the generic "Label" icon
 * gets one suggested from its name ([categoryName]) when possible.
 */
fun categoryIconFor(iconName: String, categoryName: String? = null): ImageVector {
    iconsByName[iconName]?.let { return it }
    val suggested = categoryName?.let { suggestedIconName(it) }
    return suggested?.let { iconsByName[it] } ?: Icons.Default.Label
}
