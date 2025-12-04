package com.example.bloodbank.ui.chat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.bloodbank.databinding.FragmentChatbotBinding
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.bloodbank.data.entity.Donor
import com.example.bloodbank.data.repository.DonorRepository
import kotlinx.coroutines.launch

class ChatbotFragment : Fragment() {
    private var _binding: FragmentChatbotBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: ChatAdapter
    private lateinit var repository: DonorRepository

    // Simple session memory
    private var userAge: Int? = null
    private var userLocation: String? = null
    private var userBloodGroup: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatbotBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        repository = DonorRepository(requireContext())
        adapter = ChatAdapter(mutableListOf())
        binding.rvChat.layoutManager = LinearLayoutManager(requireContext()).apply { stackFromEnd = true }
        binding.rvChat.adapter = adapter

        // Greet
        postBot("Hi! I am your Blood Bank assistant. Ask me about blood groups, donors, requests, or nearby centers.")
        Toast.makeText(requireContext(), "Chatbot ready", Toast.LENGTH_SHORT).show()

        binding.btnSend.isEnabled = true
        binding.btnSend.setOnClickListener {
            val msg = binding.etMessage.text?.toString()?.trim().orEmpty()
            if (msg.isEmpty()) {
                Toast.makeText(requireContext(), "Please type a message", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            sendUserMessage(msg)
            binding.etMessage.setText("")
            try {
                respond(msg)
            } catch (_: Exception) {
                postBot("Sorry, I couldn't process that. Please try again.")
            }
        }
    }

    private fun sendUserMessage(text: String) {
        adapter.addMessage(ChatMessage(text, true))
        binding.rvChat.scrollToPosition(adapter.itemCount - 1)
    }

    private fun postBot(text: String) {
        adapter.addMessage(ChatMessage(text, false))
        binding.rvChat.scrollToPosition(adapter.itemCount - 1)
    }

    // Enhanced on-device rule-based responses leveraging local DB
    private fun respond(prompt: String) {
        val p = prompt.trim()
        val lower = p.lowercase()

        // 1) Handle age capture / eligibility
        extractAge(lower)?.let { age ->
            userAge = age
            val eligible = age in 18..65
            val msg = if (eligible) {
                "Your age is $age. You are eligible to donate blood based on age criteria (18-65)."
            } else {
                "Your age is $age. You are NOT eligible to donate blood based on age criteria (18-65)."
            }
            postBot(msg)
            return
        }

        // 2) Handle blood group capture
        extractBloodGroup(lower)?.let { bg ->
            userBloodGroup = bg
            postBot("Got it. Your blood group is $bg. Say 'show donors' to list donors, or tell me your city for nearby info.")
            return
        }

        // 3) Handle location capture (simple heuristic)
        if (lower.contains("location") || lower.contains("i am in ") || lower.contains("i'm in ")) {
            val loc = extractLocationGuess(p)
            if (!loc.isNullOrBlank()) {
                userLocation = loc
                postBot("Location set to $loc. Say 'show donors' to see matching donors${if (userBloodGroup!=null) " for $userBloodGroup" else ""}.")
            } else {
                postBot("Please tell me your city, e.g., 'My location is Mumbai'.")
            }
            return
        }

        // 4) FAQs and guidance
        when {
            listOf("hi", "hello", "hey").any { lower.startsWith(it) } -> {
                postBot("Hello! Tell me your age, location, and blood group. I can check eligibility, show donors and hospitals.")
                return
            }
            lower.contains("what is your age") -> {
                postBot("I'm your assistant, I don't have an age. Tell me your age and I'll check your eligibility.")
                return
            }
            lower.contains("blood group") && !lower.contains("show") -> {
                postBot("Common blood groups are A+, A-, B+, B-, AB+, AB-, O+, O-. O- is universal donor; AB+ is universal recipient.")
                return
            }
            lower.contains("eligible") || lower.contains("eligibility") -> {
                val age = userAge
                if (age == null) {
                    postBot("Please tell me your age (e.g., 'I am 22') and I'll check eligibility.")
                } else {
                    val eligible = age in 18..65
                    postBot(if (eligible) "Yes, at age $age you are eligible to donate." else "At age $age you are not eligible to donate.")
                }
                return
            }
            lower.contains("hospital") || lower.contains("hospitals") || lower.contains("centers") -> {
                showHospitals()
                return
            }
            lower.contains("donor") || lower.contains("donors") || lower.contains("show donors") -> {
                showDonors()
                return
            }
        }

        // Default
        postBot("I can help with: age eligibility, setting your location, detecting your blood group, listing donors and hospitals. Try: 'I am 25', 'My location is Mumbai', 'My blood group is B+', 'show donors'.")
    }

    private fun extractAge(text: String): Int? {
        val regex = Regex("(\\d{1,2})")
        val m = regex.find(text) ?: return null
        return m.groupValues[1].toIntOrNull()
    }

    private fun extractBloodGroup(text: String): String? {
        val regex = Regex("\\b(?:a|b|ab|o)[+-]\\b", RegexOption.IGNORE_CASE)
        val m = regex.find(text) ?: return null
        return m.value.uppercase()
    }

    private fun extractLocationGuess(original: String): String? {
        // Try to parse after 'in '
        val idx = original.lowercase().indexOf(" in ")
        return if (idx != -1 && idx + 4 < original.length) {
            original.substring(idx + 4).trim().replaceFirstChar { it.uppercase() }
        } else {
            // Fallback: take last word if it seems like a city
            original.split(" ").lastOrNull()?.trim()?.replaceFirstChar { it.uppercase() }
        }
    }

    private fun showHospitals() {
        val city = userLocation?.lowercase()
        if (city == null) {
            postBot("Tell me your city (e.g., 'My location is Mumbai') and I'll list hospitals. For now, here's Mumbai:")
        }
        val hospitals = listOf(
            Triple("KEM Hospital", "Parel, Mumbai", "19.07283, 72.88261"),
            Triple("Cooper Hospital", "JVPD Scheme, Mumbai", "19.0968, 72.8517"),
            Triple("Fortis Hospital Mulund", "Mulund, Mumbai", "19.2183, 72.9781"),
            Triple("Lilavati Hospital", "Bandra West, Mumbai", "19.0669, 72.8355"),
            Triple("Nanavati Max Hospital", "Vile Parle West, Mumbai", "19.1180, 72.8465"),
            Triple("Jaslok Hospital", "Pedder Rd, Mumbai", "18.9930, 72.8170"),
            Triple("SevenHills Hospital", "Marol, Andheri East", "19.2270, 72.8567"),
            Triple("Bhabha Hospital", "Bandra West, Mumbai", "19.0622, 72.8249")
        )
        val sb = StringBuilder()
        sb.append("Hospitals:")
        hospitals.forEachIndexed { i, h ->
            sb.append("\n${i+1}. ${h.first} - ${h.second}")
        }
        postBot(sb.toString())
    }

    private fun showDonors() {
        val bg = userBloodGroup
        if (bg == null) {
            postBot("Please tell me your blood group first (e.g., 'My blood group is O+').")
            return
        }
        lifecycleScope.launch {
            val all: List<Donor> = repository.getAllLocal()
            val filtered = all.filter { it.bloodGroup.equals(bg, ignoreCase = true) }
                .let { list ->
                    val city = userLocation
                    if (!city.isNullOrBlank()) list.filter { it.city.equals(city, ignoreCase = true) } else list
                }
                .take(10)
            if (filtered.isEmpty()) {
                postBot("No donors found for $bg${userLocation?.let { " in $it" } ?: ""}. Try 'Load Sample Data (100)' on the Donors screen or change city.")
            } else {
                val sb = StringBuilder("Top donors for $bg${userLocation?.let { " in $it" } ?: ""}:")
                filtered.forEachIndexed { i, d ->
                    sb.append("\n${i+1}. ${d.name} • ${d.bloodGroup} • ${d.city} • ${d.phone}")
                }
                postBot(sb.toString())
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
