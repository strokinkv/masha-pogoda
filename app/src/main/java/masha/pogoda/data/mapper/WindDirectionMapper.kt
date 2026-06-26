package masha.pogoda.data.mapper

fun degreesToRu(deg: Int): String {
    val directions = listOf("С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ")
    return directions[(((deg % 360) + 360) % 360 + 22) / 45 % 8]
}

