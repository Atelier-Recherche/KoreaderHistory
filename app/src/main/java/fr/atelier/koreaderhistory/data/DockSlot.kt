package fr.atelier.koreaderhistory.data

data class DockSlot(
    val id: String,
    val label: String,
    val iconKey: String,
    val actionType: DockActionType,
    val param: String = "",
    val param2: String = "",
)
