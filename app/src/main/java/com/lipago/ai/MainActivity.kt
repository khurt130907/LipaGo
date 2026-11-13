
package com.lipago.ai

import android.os.Bundle
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect

import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

class MainActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    private val blue = Color.rgb(25, 75, 140)
    private val background = Color.rgb(245, 248, 255)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseAppCheck.getInstance()
            .installAppCheckProviderFactory(
                DebugAppCheckProviderFactory.getInstance()
            )

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        if (auth.currentUser != null) {
            showDashboard()
        } else {
            showWelcome()
        }
    }

    private fun createLayout(): LinearLayout {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setPadding(30, 30, 30, 30)
        layout.setBackgroundColor(background)
        return layout
    }

    private fun addTitle(layout: LinearLayout, text: String) {
        val title = TextView(this)
        title.text = text
        title.textSize = 32f
        title.setTextColor(blue)
        title.gravity = Gravity.CENTER
        title.setTypeface(null, android.graphics.Typeface.BOLD)
        title.setPadding(0, 10, 0, 25)
        layout.addView(title)
    }

    private fun createInput(hint: String, type: Int): EditText {
        val input = EditText(this)
        input.hint = hint
        input.inputType = type
        input.textSize = 16f

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, 8, 0, 8)
        input.layoutParams = params

        return input
    }

    private fun createButton(text: String, layout: LinearLayout): Button {
        val button = Button(this)
        button.text = text
        layout.addView(button)
        return button
    }

    private fun showWelcome() {
        val layout = createLayout()

        addTitle(layout, "LipaGo")

        val subtitle = TextView(this)
        subtitle.text = "Discover Lipa City with ease."
        subtitle.textSize = 18f
        subtitle.setTextColor(Color.DKGRAY)
        subtitle.gravity = Gravity.CENTER
        subtitle.setPadding(0, 10, 0, 30)

        layout.addView(subtitle)

        createButton("GET STARTED", layout).setOnClickListener {
            showOptions()
        }

        setContentView(layout)
    }

    private fun showOptions() {
        val layout = createLayout()

        addTitle(layout, "Welcome to LipaGo")

        createButton("LOGIN", layout).setOnClickListener {
            showLogin()
        }

        createButton("REGISTER", layout).setOnClickListener {
            showRegister()
        }

        setContentView(layout)
    }

    private fun showLogin() {
        val layout = createLayout()

        addTitle(layout, "Login")

        val email = createInput(
            "Email Address",
            android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        )

        val password = createInput(
            "Password",
            android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        )

        layout.addView(email)
        layout.addView(password)

        createButton("LOGIN", layout).setOnClickListener {
            val userEmail = email.text.toString().trim()
            val userPassword = password.text.toString()

            if (userEmail.isEmpty() || userPassword.isEmpty()) {
                Toast.makeText(
                    this,
                    "Please fill in all fields.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                auth.signInWithEmailAndPassword(userEmail, userPassword)
                    .addOnSuccessListener {
                        showDashboard()
                    }
                    .addOnFailureListener { error ->
                        Toast.makeText(
                            this,
                            error.localizedMessage ?: "Login failed.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        }

        createButton("BACK", layout).setOnClickListener {
            showOptions()
        }

        setContentView(layout)
    }

    private fun showRegister() {
        val layout = createLayout()

        addTitle(layout, "Create Account")

        val name = createInput(
            "Full Name",
            android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS
        )

        val email = createInput(
            "Email Address",
            android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        )

        val password = createInput(
            "Password (at least 6 characters)",
            android.text.InputType.TYPE_CLASS_TEXT or
                    android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        )

        layout.addView(name)
        layout.addView(email)
        layout.addView(password)

        createButton("REGISTER", layout).setOnClickListener {
            val userName = name.text.toString().trim()
            val userEmail = email.text.toString().trim()
            val userPassword = password.text.toString()

            if (userName.isEmpty() || userEmail.isEmpty() ||
                userPassword.isEmpty()
            ) {
                Toast.makeText(
                    this,
                    "Please fill in all fields.",
                    Toast.LENGTH_SHORT
                ).show()

            } else if (userPassword.length < 6) {
                Toast.makeText(
                    this,
                    "Password must be at least 6 characters.",
                    Toast.LENGTH_SHORT
                ).show()

            } else {
                auth.createUserWithEmailAndPassword(
                    userEmail,
                    userPassword
                )
                    .addOnSuccessListener { result ->
                        val userId = result.user?.uid

                        if (userId != null) {
                            val userProfile = hashMapOf(
                                "userId" to userId,
                                "fullName" to userName,
                                "email" to userEmail,
                                "createdAt" to FieldValue.serverTimestamp()
                            )

                            db.collection("users")
                                .document(userId)
                                .set(userProfile)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        "Registration successful!",
                                        Toast.LENGTH_SHORT
                                    ).show()

                                    showDashboard()
                                }
                                .addOnFailureListener { error ->
                                    Toast.makeText(
                                        this,
                                        "Profile saving failed: ${error.localizedMessage}",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    showDashboard()
                                }
                        }
                    }
                    .addOnFailureListener { error ->
                        Toast.makeText(
                            this,
                            error.localizedMessage ?: "Registration failed.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            }
        }

        createButton("BACK", layout).setOnClickListener {
            showOptions()
        }

        setContentView(layout)
    }

    private fun showDashboard() {
        val layout = createLayout()

        addTitle(layout, "LipaGo Dashboard")

        val welcome = TextView(this)
        welcome.text =
            "Welcome to LipaGo!\nDiscover the beautiful destinations of Lipa City."
        welcome.textSize = 18f
        welcome.setTextColor(Color.DKGRAY)
        welcome.gravity = Gravity.CENTER
        welcome.setPadding(0, 10, 0, 30)

        layout.addView(welcome)

        createButton("TOURIST DESTINATIONS", layout).setOnClickListener {
            showDestinations()
        }

        createButton("AI CHATBOT", layout).setOnClickListener {
            showChatbot()
        }

        createButton("LOGOUT", layout).setOnClickListener {
            auth.signOut()
            showWelcome()

            Toast.makeText(
                this,
                "Logged out successfully.",
                Toast.LENGTH_SHORT
            ).show()
        }

        setContentView(layout)
    }

    private fun showChatbot() {
        val model = Firebase.ai(
            backend = GenerativeBackend.googleAI()
        ).generativeModel("gemini-3.8-flash")

        val chat = model.startChat()

        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(16, 12, 16, 12)
        root.setBackgroundColor(background)

        val header = TextView(this)
        header.text = "LipaGo AI Assistant"
        header.textSize = 25f
        header.setTextColor(blue)
        header.gravity = Gravity.CENTER
        header.setTypeface(null, android.graphics.Typeface.BOLD)
        header.setPadding(4, 12, 4, 16)
        root.addView(header)

        val subtitle = TextView(this)
        subtitle.text = "Your local tourism guide in Lipa City"
        subtitle.textSize = 13f
        subtitle.setTextColor(Color.DKGRAY)
        subtitle.gravity = Gravity.CENTER
        subtitle.setPadding(0, 0, 0, 12)
        root.addView(subtitle)

        val chatScroll = ScrollView(this)
        chatScroll.isFillViewport = true
        chatScroll.setBackgroundColor(Color.WHITE)

        val messagesLayout = LinearLayout(this)
        messagesLayout.orientation = LinearLayout.VERTICAL
        messagesLayout.setPadding(12, 14, 12, 14)
        chatScroll.addView(messagesLayout)

        val scrollParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            0,
            1f
        )
        root.addView(chatScroll, scrollParams)

        addChatMessage(
            messagesLayout,
            "Hello! I'm LipaGo AI. Ask me about tourist destinations, activities, and places to visit in Lipa City!",
            false
        )

        val inputRow = LinearLayout(this)
        inputRow.orientation = LinearLayout.HORIZONTAL
        inputRow.gravity = Gravity.CENTER_VERTICAL
        inputRow.setPadding(0, 10, 0, 6)

        val messageInput = EditText(this)
        messageInput.hint = "Ask about Lipa City..."
        messageInput.textSize = 15f
        messageInput.setSingleLine(true)
        messageInput.setPadding(14, 10, 14, 10)
        messageInput.background = roundedBackground(Color.WHITE, 24f)

        val inputParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
        inputParams.setMargins(0, 0, 8, 0)
        inputRow.addView(messageInput, inputParams)

        val sendButton = Button(this)
        sendButton.text = "SEND"
        sendButton.isAllCaps = false
        inputRow.addView(sendButton)
        root.addView(inputRow)

        val backButton = Button(this)
        backButton.text = "BACK TO DASHBOARD"
        backButton.isAllCaps = false
        root.addView(backButton)
        backButton.setOnClickListener { showDashboard() }

        sendButton.setOnClickListener {
            val userMessage = messageInput.text.toString().trim()
            if (userMessage.isEmpty()) return@setOnClickListener

            addChatMessage(messagesLayout, userMessage, true)
            messageInput.text.clear()

            sendButton.isEnabled = false
            sendButton.text = "WAIT..."
            messageInput.isEnabled = false

            val responseBubble = addChatMessage(
                messagesLayout,
                "LipaGo AI:\n",
                false
            )

            chatScroll.post { chatScroll.fullScroll(View.FOCUS_DOWN) }

            val prompt = """
                You are LipaGo AI, a friendly tourism assistant for Lipa City,
                Batangas, Philippines.

                Help users with tourist destinations, travel suggestions,
                activities, food, culture, and general information about Lipa City.
                Focus on Lipa City and nearby Batangas destinations when relevant.

                Respond in the same language used by the user.
                You can speak Filipino, English, or Taglish.
                Keep answers clear, friendly, and easy to understand.
                Use short paragraphs or simple bullet points when useful.
                Do not invent exact prices, schedules, opening hours, or addresses.
                If you are unsure, clearly say that the user should verify the details.

                User question: $userMessage
            """.trimIndent()

            lifecycleScope.launch {
                try {
                    var receivedText = false

                    chat.sendMessageStream(prompt).collect { chunk ->
                        val textPart = chunk.text.orEmpty()
                        if (textPart.isNotEmpty()) {
                            receivedText = true
                            responseBubble.append(textPart)
                            chatScroll.post {
                                chatScroll.fullScroll(View.FOCUS_DOWN)
                            }
                        }
                    }

                    if (!receivedText) {
                        responseBubble.text =
                            "LipaGo AI:\nSorry, I couldn't generate a response. Please try again."
                    }
                } catch (error: Exception) {
                    responseBubble.text =
                        "LipaGo AI:\nI couldn't connect to the AI service right now. Please try again in a moment.\n\nDetails: ${error.localizedMessage ?: "Unknown error"}"
                } finally {
                    sendButton.isEnabled = true
                    sendButton.text = "SEND"
                    messageInput.isEnabled = true
                    chatScroll.post {
                        chatScroll.fullScroll(View.FOCUS_DOWN)
                    }
                }
            }
        }

        setContentView(root)
    }

    private fun addChatMessage(
        layout: LinearLayout,
        message: String,
        isUser: Boolean
    ): TextView {
        val row = LinearLayout(this)
        row.orientation = LinearLayout.HORIZONTAL
        row.gravity = if (isUser) Gravity.END else Gravity.START

        val textView = TextView(this)
        textView.text = if (isUser) "You:\n$message" else message
        textView.textSize = 15f
        textView.setTextColor(Color.rgb(35, 42, 55))
        textView.setPadding(16, 12, 16, 12)
        textView.maxWidth =
            (resources.displayMetrics.widthPixels * 0.76f).toInt()

        textView.background = roundedBackground(
            if (isUser) Color.rgb(220, 235, 255)
            else Color.rgb(242, 245, 250),
            18f
        )

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        params.setMargins(0, 6, 0, 6)
        textView.layoutParams = params

        row.addView(textView)
        layout.addView(row)
        return textView
    }

    private fun roundedBackground(
        color: Int,
        radiusDp: Float
    ): GradientDrawable {
        val drawable = GradientDrawable()
        drawable.setColor(color)
        drawable.cornerRadius =
            radiusDp * resources.displayMetrics.density
        return drawable
    }

    // TOURIST DESTINATIONS

    private fun showDestinations() {
        val scrollView = ScrollView(this)
        scrollView.setBackgroundColor(background)

        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.setPadding(25, 25, 25, 25)

        addTitle(layout, "Tourist Destinations")

        addDestination(
            layout,
            "1. San Sebastian Cathedral",
            "A historic church in the heart of Lipa City known for its religious significance and beautiful architecture.",
            "San Sebastian Cathedral is one of the well-known landmarks of Lipa City. Visitors can appreciate its history, religious importance, and architecture."
        )

        addDestination(
            layout,
            "2. Casa de Segunda",
            "A heritage house that showcases the history, culture, and traditional lifestyle of old Lipa.",
            "Casa de Segunda is a historical house associated with the heritage of Lipa. It gives visitors an idea of traditional Filipino home life and local history."
        )

        addDestination(
            layout,
            "3. Mount Malarayat",
            "A mountain destination known for its natural scenery, fresh air, and outdoor recreational activities.",
            "Mount Malarayat is known for its mountain views and natural surroundings. Visitors can enjoy the scenery and outdoor activities in the area."
        )

        addDestination(
            layout,
            "4. Milea Bee Farm",
            "A local bee farm where visitors can learn about beekeeping, honey production, and nature.",
            "Milea Bee Farm introduces visitors to beekeeping and the importance of bees. It is also a place to learn about honey production and environmental awareness."
        )

        val backButton = Button(this)
        backButton.text = "BACK TO DASHBOARD"
        backButton.isAllCaps = false

        backButton.setOnClickListener {
            showDashboard()
        }

        layout.addView(backButton)

        scrollView.addView(layout)
        setContentView(scrollView)
    }

    private fun addDestination(
        layout: LinearLayout,
        name: String,
        description: String,
        fullDescription: String
    ) {
        val card = LinearLayout(this)
        card.orientation = LinearLayout.VERTICAL
        card.setPadding(22, 22, 22, 22)
        card.background = roundedBackground(Color.WHITE, 16f)
        card.elevation = 5f

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )

        params.setMargins(0, 10, 0, 15)
        card.layoutParams = params

        val title = TextView(this)
        title.text = name
        title.textSize = 20f
        title.setTextColor(blue)
        title.setTypeface(null, android.graphics.Typeface.BOLD)

        val details = TextView(this)
        details.text = description
        details.textSize = 15f
        details.setTextColor(Color.DKGRAY)
        details.setPadding(0, 10, 0, 10)

        val tapText = TextView(this)
        tapText.text = "TAP TO VIEW DETAILS  →"
        tapText.textSize = 13f
        tapText.setTextColor(blue)
        tapText.setTypeface(null, android.graphics.Typeface.BOLD)
        tapText.setPadding(0, 8, 0, 0)

        card.addView(title)
        card.addView(details)
        card.addView(tapText)

        card.setOnClickListener {
            showDestinationDetails(name, fullDescription)
        }

        layout.addView(card)
    }

    private fun showDestinationDetails(
        name: String,
        description: String
    ) {
        val layout = createLayout()

        addTitle(layout, name)

        val detailText = TextView(this)
        detailText.text = description
        detailText.textSize = 18f
        detailText.setTextColor(Color.DKGRAY)
        detailText.gravity = Gravity.CENTER
        detailText.setPadding(15, 20, 15, 30)

        layout.addView(detailText)

        createButton("BACK TO DESTINATIONS", layout).setOnClickListener {
            showDestinations()
        }

        createButton("BACK TO DASHBOARD", layout).setOnClickListener {
            showDashboard()
        }

        setContentView(layout)
    }
}
