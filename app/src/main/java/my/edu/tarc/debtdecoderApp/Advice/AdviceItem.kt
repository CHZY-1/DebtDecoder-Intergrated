sealed class AdviceItem {
    data class Title(val text: String? = null) : AdviceItem()

    data class Course(
        var title: String? = null,
        var description: String? = null,
        var imageUrl: String? = null,
        var isCompleted: Boolean = false,
        var url: String? = null,
        var titleId: String? = null,
        var courseId: String? = null
    )
 : AdviceItem()
}
