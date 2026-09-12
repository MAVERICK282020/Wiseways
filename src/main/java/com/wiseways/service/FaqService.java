package com.wiseways.service;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Rule-based assistant for common counselling questions.
 * Works fully offline — no API key required. Serves /ask first;
 * unmatched queries fall through to the LLM (if configured) or to a
 * graceful fallback with suggested questions.
 */
@Service
public class FaqService {

    private record Faq(List<String> keywords, String answer) {}

    private static final List<String> SUGGESTIONS = List.of(
            "What is JoSAA?",
            "What is UPTAC?",
            "Which documents are required for counselling?",
            "What is category rank?",
            "How does the recommendation work?",
            "Which college can I get with my rank?"
    );

    private final List<Faq> faqs = buildFaqs();

    /**
     * @return the best-matching scripted answer, or null when nothing matches
     */
    public String answer(String query) {
        if (query == null || query.isBlank()) return null;

        String q = query.toLowerCase().trim();

        String best = null;
        int bestScore = 0;

        for (Faq faq : faqs) {
            int score = 0;
            for (String kw : faq.keywords()) {
                if (containsKeyword(q, kw)) score++;
            }
            if (score > bestScore) {
                bestScore = score;
                best = faq.answer();
            }
        }
        return best;
    }

    public List<String> getSuggestions() {
        return SUGGESTIONS;
    }

    // Single words match on word boundaries so "hi" doesn't hit "which";
    // multi-word phrases match as plain substrings.
    private boolean containsKeyword(String q, String kw) {
        if (kw.contains(" ")) return q.contains(kw);
        return Pattern.compile("\\b" + Pattern.quote(kw) + "\\b").matcher(q).find();
    }

    private List<Faq> buildFaqs() {
        return List.of(
            new Faq(List.of("josaa"),
                "JoSAA (Joint Seat Allocation Authority) conducts the common seat allocation for IITs, NITs, "
                + "IIITs and other government-funded technical institutes based on JEE Main and JEE Advanced ranks. "
                + "Candidates fill an ordered choice list and seats are allotted across multiple rounds."),

            new Faq(List.of("uptac", "upcet", "aktu counselling"),
                "UPTAC (Uttar Pradesh Technical Admission Counselling) is the state counselling process for "
                + "B.Tech and other courses in AKTU-affiliated colleges of Uttar Pradesh. Seats are allotted "
                + "based on JEE (Main) ranks through online choice filling across multiple rounds."),

            new Faq(List.of("csab"),
                "CSAB (Central Seat Allocation Board) runs the special and spot rounds after the JoSAA rounds "
                + "conclude, for vacant seats in the NIT+ system. If you missed JoSAA rounds, watch the CSAB "
                + "notifications on the official portal."),

            new Faq(List.of("document", "certificate required", "paperwork"),
                "Typical counselling documents: JEE rank card and admit card, Class 10 & 12 marksheets, "
                + "category/EWS certificate (if applicable), state domicile, a valid photo ID and passport-size "
                + "photographs. Always verify the exact checklist on the official counselling portal before your round."),

            new Faq(List.of("category rank", "categoryrank"),
                "Your category rank is your position among candidates of your reservation category (OBC-NCL, SC, "
                + "ST, EWS) rather than in the overall list. Cut-offs for reserved seats are declared per category, "
                + "so enter it in the 'categoryRank' field of /recommend for closer matches."),

            new Faq(List.of("opening rank", "closing rank", "cutoff", "cut off", "cut-off"),
                "The opening rank is the best (lowest) rank admitted to a program and the closing rank is the "
                + "worst (highest) rank admitted in a round. If your rank is within the closing rank, that seat was "
                + "historically reachable. WiseWays compares your rank against closing ranks from 2016-2024."),

            new Faq(List.of("home state", "homestate", "seat type", "what is quota", "quota meaning"),
                "Institutes split seats between quotas: NITs reserve about 50% for home-state candidates while "
                + "IITs have no home-state quota, and UPTAC seats are mainly for UP-domicile candidates. 'Seat type' "
                + "in the dataset is the reservation category a cut-off belongs to (OPEN, EWS, OBC-NCL, SC, ST)."),

            new Faq(List.of("which college", "which clg", "can i get", "mera rank", "my rank", "suggest college"),
                "Use POST /recommend with your rank (optionally branch, category rank, area and budget) — it "
                + "returns the 5 best-matched colleges from 24,000+ real cut-off records. You can try it right now "
                + "from the API or the web page."),

            new Faq(List.of("how does", "how do", "kaise kaam", "recommendation work", "how to use", "how this works"),
                "WiseWays stores 24,000+ real opening/closing rank records (JoSAA 2016-2024 and UPTAC) in a "
                + "database. Given your rank it filters eligible colleges, adds a bonus when your preferred branch "
                + "matches, and returns the top 5 colleges whose cut-offs are closest to your rank. If nothing is "
                + "eligible, it returns the nearest cut-offs from the database."),

            new Faq(List.of("which branch", "best branch", "branch should i", "cse or ", "cse ya "),
                "Pick a branch by interest first — you will spend four years with it. CSE/IT currently show the "
                + "broadest placement opportunities, while core branches (Mechanical, Civil, Electrical) lead to "
                + "strong specialist and higher-study paths. Compare branch-wise cut-offs for your rank with /recommend."),

            new Faq(List.of("fees", "fee structure", "hostel", "scholarship"),
                "Government B.Tech tuition is roughly 1-2 lakh rupees per year (IITs around 2-3 lakh, NITs around "
                + "1.5 lakh), with hostel and mess charges extra, plus tuition waivers and scholarships for eligible "
                + "students. Always confirm current fees on the institute's official website."),

            new Faq(List.of("help", "what can you", "kya kar sakte", "options"),
                "I can answer common questions about JoSAA, UPTAC, cut-offs, quotas, documents and branch selection, "
                + "and point you to the right API. For college suggestions, use POST /recommend with your rank."),

            new Faq(List.of("thank", "dhanyavad", "shukriya", "great job", "nice"),
                "You're welcome! Good luck with your counselling — /recommend is here whenever you want college suggestions."),

            new Faq(List.of("hello", "hi", "hey", "namaste", "hlo", "good morning", "good evening"),
                "Hello! I'm the WiseWays assistant. Ask me about JoSAA/UPTAC counselling, cut-offs, quotas, documents "
                + "or branch selection — or use POST /recommend with your rank to get college suggestions.")
        );
    }
}
