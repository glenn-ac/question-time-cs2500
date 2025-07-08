import khoury.*

/*** Step 1: Questions ***/

// data class that represents a tagged question
data class TaggedQuestion(
    val question: String,
    val answer: String,
    val tags: List<String>,
) {
    /**
     * Checks if the question contains a specific tag (case-insensitive).
     */
    fun hasTag(tag: String): Boolean = tags.any { it.equals(tag, ignoreCase = true) }

    /**
     * Returns a string representation of the question.
     */
    fun format(): String = "$question|$answer|${tags.joinToString(",")}"
}

// test questions
val q1 = TaggedQuestion("What is the capital of France?", "Paris", listOf("geography", "history", "france"))
val q2 = TaggedQuestion("What is the capital of Spain?", "Madrid", listOf("geography", "spain"))

// test functions
@EnabledTest
fun testTaggedQuestion() {
    testSame(
        q1.hasTag("geography"),
        true,
        "q1 has geography tag",
    )

    testSame(
        q1.hasTag("spain"),
        false,
        "q1 does not have spain tag",
    )

    testSame(
        q2.hasTag("geography"),
        true,
        "q2 has geography tag",
    )

    testSame(
        q2.hasTag("spain"),
        true,
        "q2 has spain tag",
    )

    testSame(
        q1.format(),
        "What is the capital of France?|Paris|geography,history,france",
        "q1 format",
    )

    testSame(
        q2.format(),
        "What is the capital of Spain?|Madrid|geography,spain",
        "q2 format",
    )
}

/*** Step 2: Files of tagged questions ***/

// Function that converts a string to a TaggedQuestion
fun stringToQuestion(s: String): TaggedQuestion {
    val parts = s.split("|")
    return TaggedQuestion(parts[0], parts[1], parts[2].split(","))
}

// Function that reads a file and generates a question bank
fun readTaggedQuestionBank(filename: String): List<TaggedQuestion> {
    val lines = fileReadAsList(filename)
    return lines.map { stringToQuestion(it) }
}

// Test function to test the stringToQuestion function
@EnabledTest
fun testStringToQuestion() {
    testSame(
        stringToQuestion("What is the capital of France?|Paris|geography,history,france"),
        TaggedQuestion("What is the capital of France?", "Paris", listOf("geography", "history", "france")),
        "q1",
    )

    testSame(
        stringToQuestion("What is the capital of Spain?|Madrid|geography,spain"),
        TaggedQuestion("What is the capital of Spain?", "Madrid", listOf("geography", "spain")),
        "q2",
    )
}

// need to test readTaggedQuestionBank

/*** Step 3: Question bank design ***/

/**
 * The bank is either completed,
 * showing a question or showing
 * an answer
 */
enum class QuestionBankState { COMPLETED, QUESTIONING, ANSWERING }

/**
 * Basic functionality of any question bank
 */
interface IQuestionBank {
    /**
     * Returns the state of the question bank.
     */
    fun getState(): QuestionBankState

    /**
     * Returns the currently visible text, or null if the bank is completed.
     */
    fun getText(): String?

    /**
     * Returns the number of question-answer pairs.
     */
    fun getSize(): Int

    /**
     * Shifts from question to answer. If not QUESTIONING,
     * returns the same IQuestionBank.
     */
    fun show(): IQuestionBank

    /**
     * Shifts from an answer to the next question (or completion).
     * If the current question was answered correctly, it discards
     * it. Otherwise it cycles the question to the end.
     *
     * If not ANSWERING, returns the same IQuestionBank.
     */
    fun next(correct: Boolean): IQuestionBank

    /**
     * Returns a list of all TaggedQuestion objects in the question bank.
     */
    fun getAllQuestions(): List<TaggedQuestion>
}

/**
 * Notes (20.11.2024)
 * __________________
 *
 * Consider the following sample code:
 */

class ListBasedQuestionBank(
    private val questions: List<TaggedQuestion>,
    private val state: QuestionBankState,
    private val answeredQuestions: List<TaggedQuestion>,
) : IQuestionBank {
    override fun getState(): QuestionBankState =
        if (questions.isEmpty()) {
            QuestionBankState.COMPLETED
        } else {
            state
        }

    // Returns the size of the question bank
    override fun getSize(): Int = questions.size


    // Returns the current question or answer based on the state
    override fun show(): IQuestionBank =
        if (getState() == QuestionBankState.QUESTIONING) { // If the state is QUESTIONING
            ListBasedQuestionBank(questions, QuestionBankState.ANSWERING, answeredQuestions) // Change the state to ANSWERING
        } else {
            this
        }

    // Determines the next question or answer based on the state
    override fun next(correct: Boolean): IQuestionBank =
        if (state == QuestionBankState.ANSWERING) { // If the state is ANSWERING
            val currentQuestion = questions[0] // Get the current question
            if (correct) { //   If the answer is correct
                ListBasedQuestionBank( 
                    questions.drop(1),
                    QuestionBankState.QUESTIONING,
                    answeredQuestions + currentQuestion,
                ) // Remove the current question and add it to the answered questions
            } else {
                ListBasedQuestionBank(
                    questions.drop(1) + currentQuestion,
                    QuestionBankState.QUESTIONING,
                    answeredQuestions,
                ) // Remove the current question and add it to the end of the questions
            }
        } else {
            this
        }

    // Moved outside of the 'next' function
    override fun getAllQuestions(): List<TaggedQuestion> = questions

    // Returns the text of the current question or answer based on the state
    override fun getText(): String? =
        when (state) { // Check the state
            QuestionBankState.QUESTIONING -> questions[0].question
            QuestionBankState.ANSWERING -> questions[0].answer
            else -> null
        }
}

// Test function to test the ListBasedQuestionBank class
@EnabledTest
fun testQBank() {
    testSame(
        ListBasedQuestionBank(listOf(q1, q2), QuestionBankState.QUESTIONING, listOf()).getText(),
        "What is the capital of France?",
        "Initial question is q1",
    )
    testSame(
        ListBasedQuestionBank(listOf(q1, q2), QuestionBankState.QUESTIONING, listOf()).show().getState(),
        QuestionBankState.ANSWERING,
        "After show, state is ANSWERING",
    )
    testSame(
        ListBasedQuestionBank(listOf(q1, q2), QuestionBankState.QUESTIONING, listOf()).show().next(true).getSize(),
        1,
        "After correct answer, size is 1",
    )
    testSame(
        ListBasedQuestionBank(listOf(q1, q2), QuestionBankState.QUESTIONING, listOf()).show().next(false).getSize(),
        2,
        "After incorrect answer, size is 2",
    )
    testSame(
        ListBasedQuestionBank(listOf(q1, q2), QuestionBankState.QUESTIONING, listOf())
            .show()
            .next(false)
            .show()
            .next(true)
            .getText(),
        "What is the capital of France?",
        "After incorrect and correct answer, next question is q1",
    )
    testSame(
        ListBasedQuestionBank(listOf(q1, q2), QuestionBankState.QUESTIONING, listOf())
            .show()
            .next(true)
            .show()
            .next(true)
            .getState(),
        QuestionBankState.COMPLETED,
        "After answering all questions, state is COMPLETED",
    )
}

// 3.2 implement IQuestionBank interface
class AutoGeneratedQuestionBank(
    private val questions: List<TaggedQuestion>,
    private val state: QuestionBankState,
    private val answeredQuestions: List<TaggedQuestion>,
) : IQuestionBank {
    // Generates an AutoGeneratedQuestionBank with n questions (cubed function)
    fun generateBank(n: Int): AutoGeneratedQuestionBank =
        AutoGeneratedQuestionBank(
            questions =
                List(n) {
                    TaggedQuestion(
                        "What is ${it + 1} cubed?",
                        "${(it + 1) * (it + 1) * (it + 1)}",
                        listOf("Cubes", "Math", "Easy"),
                    )
                },
            state = QuestionBankState.QUESTIONING,
            answeredQuestions = listOf(),
        )


    // Returns the state of the question bank
    override fun getState(): QuestionBankState =
        if (questions.isEmpty()) { // If there are no questions left
            QuestionBankState.COMPLETED // The state is COMPLETED
        } else {
            state
        }

    override fun getSize(): Int = questions.size // Returns the size of the question bank

    // Returns the current question or answer based on the state
    override fun show(): IQuestionBank =
        if (getState() == QuestionBankState.QUESTIONING) { // If the state is QUESTIONING
            AutoGeneratedQuestionBank(questions, QuestionBankState.ANSWERING, answeredQuestions) // Change the state to ANSWERING
        } else {
            this
        }

    // Determines the next question or answer based on the state
    override fun next(correct: Boolean): IQuestionBank =
        if (state == QuestionBankState.ANSWERING) { // If the state is ANSWERING
            val currentQuestion = questions[0]
            if (correct) { // If the answer is correct
                AutoGeneratedQuestionBank(
                    questions.drop(1),
                    QuestionBankState.QUESTIONING,
                    answeredQuestions + currentQuestion,
                ) // Remove the current question and add it to the answered questions
            } else {
                AutoGeneratedQuestionBank(
                    questions.drop(1) + currentQuestion,
                    QuestionBankState.QUESTIONING,
                    answeredQuestions,
                ) // Remove the current question and add it to the end of the questions
            }
        } else {
            this
        }

    // Moved outside of the 'next' function
    override fun getAllQuestions(): List<TaggedQuestion> = questions

    // Returns the text of the current question or answer based on the state
    override fun getText(): String? =
        when (state) {
            QuestionBankState.QUESTIONING -> questions[0].question
            QuestionBankState.ANSWERING -> questions[0].answer
            else -> null
        } 
}

// Test functions for the AutoGeneratedQuestionBank functionality
@EnabledTest
fun testAutoQBank() {
    testSame(
        AutoGeneratedQuestionBank(listOf(), QuestionBankState.QUESTIONING, listOf()).generateBank(3).getText(),
        "What is 1 cubed?",
        "Initial question is 1 cubed",
    )
    testSame(
        AutoGeneratedQuestionBank(listOf(), QuestionBankState.QUESTIONING, listOf()).generateBank(3).show().getState(),
        QuestionBankState.ANSWERING,
        "After show, state is ANSWERING",
    )
    testSame(
        AutoGeneratedQuestionBank(listOf(), QuestionBankState.QUESTIONING, listOf())
            .generateBank(3)
            .show()
            .next(true)
            .getSize(),
        2,
        "After correct answer, size is 2",
    )
    testSame(
        AutoGeneratedQuestionBank(listOf(), QuestionBankState.QUESTIONING, listOf())
            .generateBank(3)
            .show()
            .next(false)
            .getSize(),
        3,
        "After incorrect answer, size is 3",
    )
    testSame(
        AutoGeneratedQuestionBank(
            listOf(),
            QuestionBankState.QUESTIONING,
            listOf(),
        ).generateBank(3).show().next(false).show().next(true).getText(),
        "What is 3 cubed?",
        "After incorrect and correct answer, next question is 3 cubed",
    )
    testSame(
        AutoGeneratedQuestionBank(
            listOf(),
            QuestionBankState.QUESTIONING,
            listOf(),
        ).generateBank(3).show().next(true).show().next(true).show().next(true).getState(),
        QuestionBankState.COMPLETED,
        "After answering all questions, state is COMPLETED",
    )
}

/*** Step 4: Menu design  ***/
// Interface for menu options. Each option needs a title.
interface IMenuOption {
    // Get the title of the menu option.
    fun getTitle(): String
}

// Show a menu with options and let the user pick one.
// Returns the chosen option or null if the user quits.
fun <T : IMenuOption> chooseMenu(options: List<T>): T? {
    while (true) {
        // Show the menu options.
        println("Please choose an option:")
        options.forEachIndexed { index, option ->
            println("${index + 1}. ${option.getTitle()}")
        }
        println("0. Quit")

        // Ask the user to enter a choice.
        print("Enter your choice: ")
        val input = readLine()?.trim()

        // Check if the input is valid.
        val choice =
            when {
                input == null -> {
                    println("Invalid input. Please try again.")
                    continue
                }
                input.equals("0", ignoreCase = true) || input.equals("O", ignoreCase = true) -> {
                    return null
                }
                input.toIntOrNull() in 1..options.size -> {
                    input.toInt()
                }
                else -> {
                    println("Invalid choice. Please enter a number between 0 and ${options.size}.")
                    continue
                }
            }

        // Return the selected option.
        if (choice != null && choice in 1..options.size) {
            return options[choice - 1]
        } else {
            println("Invalid choice. Please try again.")
        }
    }
}


/*** Step 5: Sentiment analysis ***/

typealias Classifier = (String) -> Boolean

data class LabeledExample<E, L>(
    val example: E,
    val label: L,
)

val dataset: List<LabeledExample<String, Boolean>> =
    listOf(
        // Some positive examples
        LabeledExample("yes", true),
        LabeledExample("y", true),
        LabeledExample("indeed", true),
        LabeledExample("aye", true),
        LabeledExample("oh yes", true),
        LabeledExample("affirmative", true),
        LabeledExample("roger", true),
        LabeledExample("uh huh", true),
        LabeledExample("true", true),
        // Some negative examples
        LabeledExample("no", false),
        LabeledExample("n", false),
        LabeledExample("nope", false),
        LabeledExample("negative", false),
        LabeledExample("nay", false),
        LabeledExample("negatory", false),
        LabeledExample("uh uh", false),
        LabeledExample("absolutely not", false),
        LabeledExample("false", false),
    )

// Heuristically determines if the supplied string
//is positive based on the first letter being Y
fun naiveClassifier(s: String): Boolean = s.uppercase().startsWith("Y")

/**
 * Tests whether our classifier returns the expected result
 * for an element of our data set (at given index).
 */
fun testOne(
    index: Int,
    expected: Boolean,
    classifier: Classifier,
) {
    val sample = dataset[index]
    testSame(
        classifier(sample.example),
        when (expected) {
            true -> sample.label
            false -> !sample.label
        },
        // Label for this test
        when (expected) {
            true -> "${sample.example}"
            false -> "${sample.example} Error"
        },
    )
}

fun <T> topK(
    items: List<T>,
    metric: (T) -> Int,
    k: Int,
): List<T> = items.sortedByDescending(metric).take(k)

// Test function to test the topK function
@EnabledTest
fun testTopK() {
    testSame(
        topK(listOf("a", "bb", "ccc", "dddd"), { it.length }, 2),
        listOf("dddd", "ccc"),
        "top 2 by length",
    )

    testSame(
        topK(listOf("a", "bb", "ccc", "dddd"), { it.length }, 1),
        listOf("dddd"),
        "top 1 by length",
    )

    testSame(
        topK(listOf("a", "bb", "ccc", "dddd"), { it.length }, 3),
        listOf("dddd", "ccc", "bb"),
        "top 3 by length",
    )
}

// Calculate the Levenshtein Distance between two strings without using mutable state.
fun levenshteinDistance(word1: String, word2: String): Int {
    val length2 = word2.length

    // Initialize the first row of the distance matrix as an immutable list
    val initialRow = (0..length2).toList()

    // Iterate through each character of word1, updating the distance matrix immutably
    val finalRow = word1.fold(initialRow) { previousRow, char1 ->
        buildList {
            // The first element of the current row is the row index (insertion cost)
            add(previousRow.first() + 1)

            // Compute the rest of the current row
            for (i in 1..length2) {
                val char2 = word2[i - 1]
                val cost = if (char1 == char2) 0 else 1

                val insertion = this[i - 1] + 1          // Cost of insertion
                val deletion = previousRow[i] + 1        // Cost of deletion
                val substitution = previousRow[i - 1] + cost // Cost of substitution

                // Add the minimum of the three possible operations to the current row
                add(minOf(insertion, deletion, substitution))
            }
        }
    }

    // The last element of the final row contains the Levenshtein distance
    return finalRow.last()
}

// Test the Levenshtein Distance function with sample data.
@EnabledTest
fun testLevenshteinDistance() {
    testSame(
        levenshteinDistance("arsenal", "liverpool"),
        7,
        "Distance between 'arsenal' and 'liverpool'."
    )

    testSame(
        levenshteinDistance("chelsea", "leeds"),
        5,
        "Distance between 'chelsea' and 'leeds'."
    )

    testSame(
        levenshteinDistance("", "milan"),
        5,
        "Distance between '' and 'milan'."
    )

    testSame(
        levenshteinDistance("roma", "roma"),
        0,
        "Distance between 'roma' and 'roma'."
    )
}

// Predict a label for the query string using k-Nearest Neighbors.
fun nnLabel(
    query: String,
    dataset: List<LabeledExample<String, Boolean>>,
    distFn: (String, String) -> Int,
    k: Int
): Pair<Boolean, Int> {
    // Sort the dataset by distance to the query.
    val sortedDataset = dataset.sortedBy { distFn(query, it.example) }

    // Pick the top k closest neighbors.
    val topK = sortedDataset.take(k)

    // Count the votes for each label.
    val votes = topK.groupingBy { it.label }.eachCount()

    // Find the label with the most votes.
    val predictedLabel = votes.maxByOrNull { it.value }?.key ?: false
    val voteCount = votes[predictedLabel] ?: 0

    return Pair(predictedLabel, voteCount) // Return the label and vote count.
}

// Test the nnLabel function with sample data.
@EnabledTest
fun testNNLabel() {
    val (label1, votes1) = nnLabel("y", dataset, { a, b -> levenshteinDistance(a, b) }, 3)
    testSame(
        label1,
        true,
        "NN Label for 'y' should be true."
    )
    testSame(
        votes1,
        2,
        "NN Label votes for 'y' should be 2."
    )

    val (label2, votes2) = nnLabel("no", dataset, { a, b -> levenshteinDistance(a, b) }, 3)
    testSame(
        label2,
        false,
        "NN Label for 'no' should be false."
    )

    val (label3, votes3) = nnLabel("maybe", dataset, { a, b -> levenshteinDistance(a, b) }, 3)
    testSame(
        label3,
        false,
        "NN Label for 'maybe' should be false."
    )
    testSame(
        votes3,
        2,
        "NN Label votes for 'maybe' should be 2."
    )
}

// Classify an input string using the dataset or nearest neighbors.
fun classifier(input: String): Boolean {
    // Check if the input is already in the dataset.
    val existing = dataset.find { it.example.equals(input, ignoreCase = true) }
    if (existing != null) {
        return existing.label
    }

    // Use 3-Nearest Neighbors with Levenshtein Distance if not found.
    val (predictedLabel, _) = nnLabel(input, dataset, ::levenshteinDistance, 3)
    return predictedLabel
}

// Test the classifier function with sample data.
@EnabledTest
fun testClassifier() {
    testSame(
        classifier("yes"),
        true,
        "Classifier for 'yes' should be true."
    )

    testSame(
        classifier("no"),
        false,
        "Classifier for 'no' should be false."
    )

    testSame(
        classifier("maybe"),
        false,
        "Classifier for 'maybe' should be false."
    )

    testSame(
        classifier("unknown"),
        false,
        "Classifier for 'unknown' should be false." // Assuming closest labels are negative.
    )
}


/*** Step 6: Putting it all together ***/

// Data class to store the result of a study session.
data class StudyQuestionBankResult(
    val questions: Int, // Total number of questions in the session.
    val attempts: Int   // Total attempts made by the user.
)

// Data class to represent the state during a study session.
data class StudyQuestionBankState(
    val remainingQuestions: List<TaggedQuestion>, // Questions left to answer.
    val currentQuestion: TaggedQuestion? = null, // The current question being shown.
    val stage: Int = 0, // 0: Show question, 1: Show answer, 2: Get feedback.
    val attempts: Int = 0 // Number of attempts so far.
)

// Generate text to display based on the current state.
fun stateToText(state: StudyQuestionBankState): String =
    when (state.stage) {
        0 -> "Question: ${state.currentQuestion?.question}\nPress Enter to view the answer..."
        1 -> "Answer: ${state.currentQuestion?.answer}. Were you right? (Y/N)"
        2 -> "Attempts: ${state.attempts}\nProceeding to next question..."
        else -> ""
    }

// Determine the next state based on the current state and user input.
fun whatsNext(
    state: StudyQuestionBankState,
    input: String,
    classifier: (String) -> Boolean
): StudyQuestionBankState =
    when (state.stage) {
        0 -> state.copy(stage = 1) // Move to showing the answer.
        1 -> {
            val isCorrect = classifier(input)
            val updatedRemaining =
                if (isCorrect) {
                    state.remainingQuestions.drop(1)
                } else {
                    state.remainingQuestions.drop(1) + state.currentQuestion!!
                }
            val nextQuestion = updatedRemaining.firstOrNull()
            state.copy(
                remainingQuestions = updatedRemaining,
                currentQuestion = nextQuestion,
                stage = if (updatedRemaining.isNotEmpty()) 0 else 2,
                attempts = state.attempts + 1
            )
        }
        else -> state // Final stage remains unchanged.
    }

// Check if the study session should end.
fun terminate(state: StudyQuestionBankState): Boolean = state.stage == 2

// Generate text to display when the session ends.
fun terminalStateToText(state: StudyQuestionBankState): String =
    "Study session completed!\nQuestions: ${state.attempts}, Attempts: ${state.attempts}"

// Global variable to store the current classifier for nextState function
private var currentClassifier: ((String) -> Boolean)? = null

// Wrapper function for whatsNext that can be used with function reference
fun nextState(state: StudyQuestionBankState, input: String): StudyQuestionBankState =
    whatsNext(state, input, currentClassifier ?: { false })

// Run a study session with a question bank and classifier.
fun studyQuestionBank(
    questionBank: IQuestionBank,
    classifier: (String) -> Boolean
): StudyQuestionBankResult {
    val initialState = StudyQuestionBankState(
        remainingQuestions = questionBank.getAllQuestions(),
        currentQuestion = questionBank.getAllQuestions().firstOrNull(),
        stage = 0,
        attempts = 0
    )

    // Set the global classifier for this session
    currentClassifier = classifier

    val finalState = reactConsole(
        initialState,
        ::stateToText,
        ::nextState,
        ::terminate,
        ::terminalStateToText
    )

    return StudyQuestionBankResult(
        questions = questionBank.getSize(),
        attempts = finalState.attempts
    )
}

// Get the list of all questions from a question bank.
fun readTaggedQuestionBankFromBank(questionBank: IQuestionBank): List<TaggedQuestion> {
    return emptyList() // Placeholder for actual implementation.
}

// Get the current question from the question bank.
fun getCurrentQuestion(questionBank: IQuestionBank): TaggedQuestion? {
    return null // Placeholder for actual implementation.
}

// Interface for menu options related to question banks.
interface IQuestionBankOption : IMenuOption {
    fun getQuestionBank(): IQuestionBank
}

// Option for a file-based question bank.
class FileBasedQuestionBankOption(private val filename: String) : IQuestionBankOption {
    override fun getTitle(): String = "Question Bank from File ($filename)"

    override fun getQuestionBank(): IQuestionBank {
        val questions = readTaggedQuestionBank(filename)
        return ListBasedQuestionBank(
            questions = questions,
            state = QuestionBankState.QUESTIONING,
            answeredQuestions = listOf()
        )
    }
}

// Option for an auto-generated question bank.
class AutoGeneratedQuestionBankOption(private val numberOfQuestions: Int) : IQuestionBankOption {
    override fun getTitle(): String = "Auto-Generated Question Bank ($numberOfQuestions questions)"

    override fun getQuestionBank(): IQuestionBank =
        AutoGeneratedQuestionBank(
            questions = listOf(),
            state = QuestionBankState.QUESTIONING,
            answeredQuestions = listOf()
        ).generateBank(numberOfQuestions)
}

// Option for a filtered question bank based on a tag.
class FilteredQuestionBankOption(
    private val sourceQuestionBank: IQuestionBank,
    private val tag: String
) : IQuestionBankOption {
    override fun getTitle(): String = "Filtered Question Bank (Tag: \"$tag\")"

    override fun getQuestionBank(): IQuestionBank {
        val filteredQuestions = sourceQuestionBank.getAllQuestions().filter { it.hasTag(tag) }
        return ListBasedQuestionBank(
            questions = filteredQuestions,
            state = QuestionBankState.QUESTIONING,
            answeredQuestions = listOf()
        )
    }
}

// Interface for menu options related to classifiers.
interface IClassifierOption : IMenuOption {
    fun getClassifier(): Classifier
}

// Option for a naive classifier.
class NaiveClassifierOption : IClassifierOption {
    override fun getTitle(): String = "Naive Classifier (Starts with 'Y' or 'y')"

    override fun getClassifier(): Classifier = { s -> naiveClassifier(s) }
}

// Option for an advanced classifier.
class AdvancedClassifierOption : IClassifierOption {
    override fun getTitle(): String = "Advanced Classifier (k-NN with Levenshtein Distance)"

    override fun getClassifier(): Classifier = { s -> classifier(s) }
}

// Main function to run the study program.
fun study() {
    val fileQuestionBankOptions = listOf(FileBasedQuestionBankOption("questions.txt"))
    val autoGeneratedOptions = listOf(
        AutoGeneratedQuestionBankOption(5),
        AutoGeneratedQuestionBankOption(10)
    )

    val sourceQuestionBank =
        if (fileQuestionBankOptions.isNotEmpty()) {
            fileQuestionBankOptions[0].getQuestionBank()
        } else {
            AutoGeneratedQuestionBankOption(5).getQuestionBank()
        }

    val tagForFiltering = "subtraction"
    val filteredQuestionBankOptions =
        listOf(FilteredQuestionBankOption(sourceQuestionBank, tagForFiltering))

    val allQuestionBankOptions: List<IQuestionBankOption> =
        fileQuestionBankOptions + autoGeneratedOptions + filteredQuestionBankOptions

    val classifierOptions: List<IClassifierOption> = listOf(NaiveClassifierOption(), AdvancedClassifierOption())

    while (true) {
        println("\n--- Study Session ---")

        val selectedQuestionBankOption = chooseMenu(allQuestionBankOptions)
        if (selectedQuestionBankOption == null) {
            println("Exiting the study program. Goodbye!")
            break
        }
        val selectedQuestionBank = selectedQuestionBankOption.getQuestionBank()

        val selectedClassifierOption = chooseMenu(classifierOptions)
        if (selectedClassifierOption == null) {
            println("Exiting the study program. Goodbye!")
            break
        }
        val selectedClassifier = selectedClassifierOption.getClassifier()

        println(
            "\nStarting study session with '${selectedQuestionBankOption.getTitle()}' using '${selectedClassifierOption.getTitle()}'.\n"
        )
        val result = studyQuestionBank(selectedQuestionBank, selectedClassifier)
        println("\nStudy session completed. Questions: ${result.questions}, Attempts: ${result.attempts}\n")

        println("Would you like to start another study session? (Y/N)")
        val continueInput = readLine()?.trim() ?: "N"
        if (!continueInput.uppercase().startsWith("Y")) {
            println("Exiting the study program. Goodbye!")
            break
        }
    }
}

runEnabledTests(this)

fun main() {
    study()
}

main()