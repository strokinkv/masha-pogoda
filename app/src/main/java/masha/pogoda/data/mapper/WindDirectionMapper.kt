package masha.pogoda.data.mapper

fun yandexWindToRu(direction: String): String = when (direction.uppercase()) {
    "NORTH" -> "С"
    "NORTH_EAST" -> "СВ"
    "EAST" -> "В"
    "SOUTH_EAST" -> "ЮВ"
    "SOUTH" -> "Ю"
    "SOUTH_WEST" -> "ЮЗ"
    "WEST" -> "З"
    "NORTH_WEST" -> "СЗ"
    "CALM" -> "штиль"
    else -> "—"
}

fun degreesToRu(deg: Int): String {
    val directions = listOf("С", "СВ", "В", "ЮВ", "Ю", "ЮЗ", "З", "СЗ")
    return directions[(((deg % 360) + 360) % 360 + 22) / 45 % 8]
}

