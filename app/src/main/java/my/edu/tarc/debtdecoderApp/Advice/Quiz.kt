package my.edu.tarc.debtdecoderApp.Advice

data class Quiz(
    val testName: String? = null,
    val duration: String? = null,
    val url: String? = null,
    val difficulty: String? = null,
    val numOfMCQ: Int? = null,
    var quizId: String? = null
)


data class Question(
    val question: String? = null,
    val options: List<String>? = null,
    val answer: String? = null
)

data class QuizResult(
    val id: String? = null,
    val quizTitle: String? = null,
    val totalQuestions: Int? = null,
    val correctAnswers: Int? = null,
    val timeUsed: String? = null,
    val date: String? = null
)

