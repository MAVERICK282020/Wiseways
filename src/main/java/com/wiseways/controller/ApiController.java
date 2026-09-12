package com.wiseways.controller;

import com.wiseways.model.AskRequest;
import com.wiseways.model.CollegeResult;
import com.wiseways.model.RecommendRequest;
import com.wiseways.service.ChatService;
import com.wiseways.service.DataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*") // CORS FIX
public class ApiController {

    @Autowired
    private DataService dataService;

    @Autowired
    private ChatService chatService;

    // ================= HEALTH CHECK =================
    @GetMapping("/")
    public String home() {
        return "WiseWays Machine Engine is Running 🚀";
    }

    // ================= ASSISTANT =================
    @PostMapping("/ask")
    public Map<String, Object> ask(@RequestBody AskRequest req) {
        return chatService.ask(req.getQuery() == null ? "" : req.getQuery());
    }

    // ================= MAIN API =================
    @PostMapping("/recommend")
    public Map<String, Object> recommend(@RequestBody RecommendRequest req) {

        System.out.println("🔥 REQUEST RECEIVED:");
        System.out.println("Rank: " + req.getRank());
        System.out.println("CategoryRank: " + req.getCategoryRank());
        System.out.println("Area: " + req.getArea());
        System.out.println("Budget: " + req.getBudget());
        System.out.println("Branch: " + req.getBranch());

        List<CollegeResult> results = dataService.recommend(req);

        Map<String, Object> response = new HashMap<>();
        response.put("colleges", results);

        return response;
    }
}