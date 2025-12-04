package com.example.bloodbank.data

import com.example.bloodbank.data.entity.Donor
import kotlin.random.Random

object SampleDataGenerator {
    
    private val firstNames = listOf(
        "Aarav", "Vivaan", "Aditya", "Vihaan", "Arjun", "Sai", "Reyansh", "Ayaan", "Krishna", "Ishaan",
        "Shaurya", "Atharv", "Advik", "Pranav", "Vedant", "Aadhya", "Ananya", "Diya", "Saanvi", "Pari",
        "Fatima", "Aisha", "Sara", "Zara", "Myra", "Priya", "Kavya", "Arya", "Navya", "Kiara",
        "Rajesh", "Suresh", "Ramesh", "Mahesh", "Dinesh", "Naresh", "Hitesh", "Ritesh", "Mukesh", "Rakesh",
        "Sunita", "Geeta", "Meera", "Seeta", "Rita", "Nita", "Lata", "Mamta", "Kavita", "Sangita",
        "Amit", "Sumit", "Rohit", "Mohit", "Lalit", "Ajit", "Vinit", "Ankit", "Nikit", "Ravi",
        "Pooja", "Sooja", "Neha", "Sneha", "Rekha", "Lekha", "Radha", "Sudha", "Vidya", "Divya",
        "Karan", "Varun", "Tarun", "Arun", "Shagun", "Argun", "Falgun", "Nirgun", "Gungun", "Simran",
        "Deepak", "Ashok", "Vinod", "Pramod", "Manoj", "Anoj", "Santosh", "Jagdish", "Harish", "Girish",
        "Anjali", "Sanjali", "Shweta", "Sweta", "Komal", "Vimal", "Kamal", "Nimal", "Bimal", "Himal"
    )
    
    private val lastNames = listOf(
        "Sharma", "Verma", "Gupta", "Singh", "Kumar", "Agarwal", "Jain", "Bansal", "Mittal", "Goel",
        "Shah", "Patel", "Mehta", "Desai", "Modi", "Joshi", "Trivedi", "Pandya", "Vyas", "Dave",
        "Reddy", "Rao", "Nair", "Menon", "Pillai", "Iyer", "Krishnan", "Raman", "Subramanian", "Venkat",
        "Khan", "Ahmed", "Ali", "Hussain", "Sheikh", "Malik", "Ansari", "Qureshi", "Siddiqui", "Rizvi",
        "Das", "Roy", "Ghosh", "Mukherjee", "Banerjee", "Chatterjee", "Bhattacharya", "Chakraborty", "Dutta", "Sen"
    )
    
    private val bloodGroups = listOf("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-")
    
    private val cities = listOf(
        "Mumbai", "Delhi", "Bangalore", "Hyderabad", "Ahmedabad", "Chennai", "Kolkata", "Surat",
        "Pune", "Jaipur", "Lucknow", "Kanpur", "Nagpur", "Indore", "Thane", "Bhopal",
        "Visakhapatnam", "Pimpri", "Patna", "Vadodara", "Ghaziabad", "Ludhiana", "Agra", "Nashik",
        "Faridabad", "Meerut", "Rajkot", "Kalyan", "Vasai", "Varanasi", "Srinagar", "Aurangabad",
        "Dhanbad", "Amritsar", "Navi Mumbai", "Allahabad", "Ranchi", "Howrah", "Coimbatore", "Jabalpur"
    )
    
    private val genders = listOf("Male", "Female", "Other")
    
    private val emergencyContacts = listOf(
        "+91-9876543210", "+91-8765432109", "+91-7654321098", "+91-6543210987", "+91-5432109876",
        "+91-4321098765", "+91-3210987654", "+91-2109876543", "+91-1098765432", "+91-9087654321"
    )
    
    private val bloodPressures = listOf("120/80", "110/70", "130/85", "115/75", "125/82", "118/78")
    private val weights = listOf("65kg", "70kg", "75kg", "60kg", "80kg", "55kg", "85kg", "68kg", "72kg", "58kg")
    
    private val specialNotes = listOf(
        "Regular donor, very reliable",
        "Available for emergency calls",
        "Prefers morning appointments",
        "Has donated 10+ times",
        "First time donor",
        "Available on weekends only",
        "Works in healthcare",
        "Student volunteer",
        "Corporate donor program",
        "Community health advocate",
        "Blood bank volunteer",
        "Medical professional",
        "Emergency responder",
        "Social worker",
        "NGO volunteer"
    )
    
    // Generate sample profile image URLs (placeholder URLs)
    private fun generateProfileImageUrl(id: Long): String {
        return "https://picsum.photos/200/200?random=$id"
    }
    
    // Generate sample ID document URLs
    private fun generateIdDocumentUrl(id: Long): String {
        return "https://picsum.photos/400/300?random=${id + 1000}"
    }
    
    // Generate sample medical report URLs
    private fun generateMedicalReportUrl(id: Long): String {
        return "https://picsum.photos/600/400?random=${id + 2000}"
    }
    
    // Generate coordinates for Indian cities
    private fun getCityCoordinates(city: String): Pair<Double, Double> {
        return when (city) {
            "Mumbai" -> Pair(19.0760, 72.8777)
            "Delhi" -> Pair(28.7041, 77.1025)
            "Bangalore" -> Pair(12.9716, 77.5946)
            "Hyderabad" -> Pair(17.3850, 78.4867)
            "Ahmedabad" -> Pair(23.0225, 72.5714)
            "Chennai" -> Pair(13.0827, 80.2707)
            "Kolkata" -> Pair(22.5726, 88.3639)
            "Surat" -> Pair(21.1702, 72.8311)
            "Pune" -> Pair(18.5204, 73.8567)
            "Jaipur" -> Pair(26.9124, 75.7873)
            "Lucknow" -> Pair(26.8467, 80.9462)
            "Kanpur" -> Pair(26.4499, 80.3319)
            "Nagpur" -> Pair(21.1458, 79.0882)
            "Indore" -> Pair(22.7196, 75.8577)
            "Thane" -> Pair(19.2183, 72.9781)
            "Bhopal" -> Pair(23.2599, 77.4126)
            else -> Pair(20.5937 + Random.nextDouble(-5.0, 5.0), 78.9629 + Random.nextDouble(-10.0, 10.0))
        }
    }
    
    fun generateSampleDonors(count: Int = 120): List<Donor> {
        val donors = mutableListOf<Donor>()
        
        repeat(count) { index ->
            val id = (index + 1).toLong()
            val firstName = firstNames.random()
            val lastName = lastNames.random()
            val name = "$firstName $lastName"
            val city = cities.random()
            val coordinates = getCityCoordinates(city)
            
            // Generate phone number
            val phoneNumber = "+91-${Random.nextInt(7000000000L.toInt(), 9999999999L.toInt())}"
            
            val donor = Donor(
                id = id,
                name = name,
                bloodGroup = bloodGroups.random(),
                phone = phoneNumber,
                city = city,
                age = Random.nextInt(18, 65),
                gender = genders.random(),
                location = "$city, ${listOf("Sector", "Area", "Colony", "Nagar", "Road").random()} ${Random.nextInt(1, 50)}",
                latitude = coordinates.first + Random.nextDouble(-0.1, 0.1),
                longitude = coordinates.second + Random.nextDouble(-0.1, 0.1),
                profileImageUrl = generateProfileImageUrl(id),
                idDocumentUrl = generateIdDocumentUrl(id),
                medicalReportsUrl = generateMedicalReportUrl(id),
                isAvailable = Random.nextBoolean(),
                lastDonationDate = System.currentTimeMillis() - Random.nextLong(0, 365L * 24 * 60 * 60 * 1000),
                emergencyContact = emergencyContacts.random(),
                bloodPressure = bloodPressures.random(),
                weight = weights.random(),
                specialNotes = specialNotes.random(),
                voiceNoteUrl = if (Random.nextBoolean()) "https://www.soundjay.com/misc/sounds/bell-ringing-05.wav" else null,
                videoNoteUrl = if (Random.nextBoolean()) "https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_1mb.mp4" else null,
                registrationDate = System.currentTimeMillis() - Random.nextLong(0, 730L * 24 * 60 * 60 * 1000),
                verificationStatus = listOf("Verified", "Pending", "Verified", "Verified").random()
            )
            
            donors.add(donor)
        }
        
        return donors
    }
    
    // Generate voice prompt samples
    fun getVoicePrompts(): List<String> {
        return listOf(
            "Urgent blood request! O negative blood needed at City Hospital immediately.",
            "Emergency alert! Multiple accident victims need A positive blood donors right now.",
            "Critical shortage! AB negative blood required for surgery in 2 hours.",
            "Blood drive reminder! Your scheduled donation appointment is in 30 minutes.",
            "Thank you for being a life saver! Your last donation helped 3 patients.",
            "New blood request in your area! B positive donors needed within 5 kilometers.",
            "Emergency blood camp! Mobile unit arriving at your location in 15 minutes.",
            "Congratulations! You've successfully completed 10 blood donations.",
            "Health reminder! Stay hydrated and eat iron-rich foods before donating.",
            "Blood bank alert! Your blood group is critically needed this weekend."
        )
    }
    
    // Generate audio note samples
    fun getAudioNoteSamples(): List<Pair<String, String>> {
        return listOf(
            Pair("Emergency Case #001", "Patient requires immediate O negative transfusion for emergency surgery."),
            Pair("Special Request #002", "Pediatric patient needs A positive blood with CMV negative status."),
            Pair("Urgent Appeal #003", "Cancer patient undergoing chemotherapy requires platelet donation."),
            Pair("Critical Case #004", "Accident victim with multiple injuries needs B negative blood urgently."),
            Pair("Medical Note #005", "Pregnant woman with complications requires AB positive blood immediately."),
            Pair("Emergency Alert #006", "Thalassemia patient needs regular O positive blood transfusion."),
            Pair("Special Case #007", "Rare blood group AB negative needed for organ transplant surgery."),
            Pair("Urgent Request #008", "Hemophilia patient requires fresh frozen plasma donation."),
            Pair("Critical Alert #009", "Sickle cell patient needs A negative blood for exchange transfusion."),
            Pair("Emergency Case #010", "Burn victim requires multiple blood products including platelets.")
        )
    }
    
    // Generate video note samples
    fun getVideoNoteSamples(): List<Triple<String, String, String>> {
        return listOf(
            Triple("Doctor Appeal #001", "Dr. Sarah explains urgent need for O negative blood", "https://sample-videos.com/zip/10/mp4/SampleVideo_640x360_1mb.mp4"),
            Triple("Patient Story #002", "Recovery story of accident survivor thanks to blood donors", "https://sample-videos.com/zip/10/mp4/SampleVideo_640x360_2mb.mp4"),
            Triple("Emergency Room #003", "Live from ER - critical patient needs immediate blood", "https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_1mb.mp4"),
            Triple("Blood Drive #004", "Community blood drive event highlights and donor testimonials", "https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_2mb.mp4"),
            Triple("Medical Procedure #005", "How blood donation saves lives - medical explanation", "https://sample-videos.com/zip/10/mp4/SampleVideo_1920x1080_1mb.mp4"),
            Triple("Donor Recognition #006", "Honoring 100-time blood donor and community hero", "https://sample-videos.com/zip/10/mp4/SampleVideo_1920x1080_2mb.mp4"),
            Triple("Emergency Appeal #007", "Hospital director's urgent appeal for rare blood type", "https://sample-videos.com/zip/10/mp4/SampleVideo_640x360_5mb.mp4"),
            Triple("Success Story #008", "Child's life saved through timely blood donation", "https://sample-videos.com/zip/10/mp4/SampleVideo_1280x720_5mb.mp4"),
            Triple("Awareness Campaign #009", "Importance of regular blood donation in community", "https://sample-videos.com/zip/10/mp4/SampleVideo_1920x1080_5mb.mp4"),
            Triple("Medical Training #010", "Blood bank procedures and safety protocols", "https://sample-videos.com/zip/10/mp4/SampleVideo_1920x1080_10mb.mp4")
        )
    }
}
