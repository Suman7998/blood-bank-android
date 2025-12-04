# Blood Bank App — Presentation Deck

Note: Copy each section into individual Google Slides/PowerPoint slides. Speaker Notes are included under each slide.

---

## Slide 1: Title — Blood Bank App
- Modern Android app to connect donors, recipients, and hospitals
- Find donors, request blood, donate, and locate centers
- Smart AI notifications, chatbot, multimedia, maps, and BERT

Speaker Notes:
- Introduce the app vision: faster, smarter, community-driven blood support.

---

## Slide 2: Problem & Vision
- Problem: Delays in matching donors with urgent needs
- Gaps: Unstructured outreach, low engagement, limited awareness
- Vision: Intelligent, location-aware, multimedia-enabled blood bank platform

Speaker Notes:
- Emphasize reducing response time and improving coordination.

---

## Slide 3: Architecture Overview
- MVVM + Repository, offline-first caching
- Local: Room/SQLite; Remote: Firebase (Auth, Firestore, Storage, FCM)
- Services: Maps & Location, Multimedia, AI/BERT, Notifications
- Tech: Kotlin, Coroutines/Flow, Hilt, Retrofit/OkHttp, Glide

Speaker Notes:
- Highlight separation of concerns and reliability.

---

## Slide 4: UI/UX Highlights
- 50+ gradient palettes; modern cards (25dp, 12dp elevation)
- Gradient toolbar, bottom navigation, styled drawer
- Redesigned Login, Home quick actions, donor cards with badges
- Accessibility: contrast, large touch targets, consistent icons

Speaker Notes:
- Show screenshots of Login, Home, Donor Card (see Screens section).

---

## Slide 5: Firebase Platform
- Auth: Email/Password (extensible for Google Sign-In)
- Firestore: donors, requests, centers, alerts, media_meta
- Storage: images, audio, video; metadata linking
- FCM: AI notifications in background
- Security: rules for PII and path validation

Speaker Notes:
- Stress realtime updates and secure data handling.

---

## Slide 6: Maps & Location
- Google Maps SDK; Fused Location Provider
- Nearby donors within 10km; sorted by distance; blood-group filters
- Permissions with fallbacks; API key via local.properties

Speaker Notes:
- Show map and nearby donors screenshot.

---

## Slide 7: Multimedia System
- Images: Camera capture, compression, thumbnails, Glide
- Audio: Record notes, playback, TTS prompts
- Video: 30s video notes with thumbnails
- Firebase Storage integration; robust permissions and errors

Speaker Notes:
- Demonstrate capture-to-upload flow.

---

## Slide 8: AI Chatbot
- Use-cases: donor search, request guidance, FAQs
- NLP: BERT-based intent classification + entity extraction
- Features: context-aware replies, quick actions, TTS

Speaker Notes:
- Example: “Need O+ donors near me” → results + action buttons.

---

## Slide 9: ML — BERT Model
- Lightweight on-device BERT, quantized
- Pipeline: tokenization → inference → label mapping
- Performance: <150ms on mid-range devices
- Fallback: rule-based intents if model unavailable

Speaker Notes:
- Emphasize privacy and offline capability.

---

## Slide 10: AI Notifications System
- Alert Types: Emergency, Partnership, Campaigns, Mobile Camps
- Intelligence: time-of-day, weekday, seasonal, critical shortages
- Delivery: in-app banners, color-coded notifications, FCM push
- Storage: Firestore alerts with TTL and read states

Speaker Notes:
- Show color-coded notification examples.

---

## Slide 11: Data, Security, Performance
- Data Model: donors, requests, centers, alerts, media_meta
- Security: Firebase rules, minimal PII, secrets via local.properties
- Performance: caching, thumbnails, Firestore offline, background sync
- Testing: Unit/UI tests, lint, CI checks

Speaker Notes:
- Briefly cover reliability and compliance.

---

## Slide 12: Demo & Roadmap
- Demo Flow: Login → Home actions → Multimedia note → Chatbot → Notifications → Maps
- Roadmap: social sharing, donation badges, advanced analytics, multi-language
- Call to Action: pilot with partner hospitals

Speaker Notes:
- Invite questions and feedback.
