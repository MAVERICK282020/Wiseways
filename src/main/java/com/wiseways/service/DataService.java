package com.wiseways.service;

import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReaderBuilder;
import com.wiseways.model.CollegeEntity;
import com.wiseways.model.CollegeEntry;
import com.wiseways.model.CollegeResult;
import com.wiseways.model.RecommendRequest;
import com.wiseways.repository.CollegeEntityRepository;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class DataService {

    @Value("${csv.files}")
    private List<String> csvFiles;

    private final CollegeEntityRepository repo;

    private List<CollegeEntry> dataset = new ArrayList<>();
    private Map<String, Integer> branchMapping = new HashMap<>();

    public DataService(CollegeEntityRepository repo) {
        this.repo = repo;
    }

    // ================= LOAD DATA (CSV → DB on first run, DB afterwards) =================
    @PostConstruct
    public void initData() {
        try {
            if (repo.count() > 0) {
                loadFromDatabase();
                return;
            }

            List<CollegeEntry> raw = new ArrayList<>();

            for (String path : csvFiles) {
                int before = raw.size();
                loadCsvFile(path, raw);
                log.info("📄 {}: {} rows parsed", path, raw.size() - before);
            }

            if (raw.isEmpty()) {
                log.error("❌ NO DATA PARSED FROM ANY CSV!");
                return;
            }

            // Keep every category/quota/year row — the rank-difference sort picks
            // the cutoff closest to the user's (category) rank. Results are
            // de-duplicated per college+branch at the end.
            dataset = new ArrayList<>(raw);

            // Branch encoding
            List<String> branches = dataset.stream()
                    .map(CollegeEntry::getBranch)
                    .distinct().sorted()
                    .toList();

            for (int i = 0; i < branches.size(); i++)
                branchMapping.put(branches.get(i), i);

            dataset.forEach(e ->
                    e.setBranchCode(branchMapping.getOrDefault(e.getBranch(), -1)));

            seedDatabase(dataset);
            log.info("✅ DATA LOADED + SEEDED TO DATABASE: {}", dataset.size());

        } catch (Exception e) {
            log.error("❌ DATA ERROR", e);
        }
    }

    // ================= RECOMMEND =================
    public List<CollegeResult> recommend(RecommendRequest req) {

        int effectiveRank = (req.getCategoryRank() != null && !req.getCategoryRank().isBlank())
                ? parseCategoryRank(req.getCategoryRank())
                : req.getRank();

        String branchInput = safe(req.getBranch());
        String area = safe(req.getArea()).toLowerCase();
        String counselling = safe(req.getCounselling()).toLowerCase();
        String budget = mapBudget(safe(req.getBudget()));

        List<CollegeEntry> temp = new ArrayList<>(dataset);

        // ===== CITY FILTER (SMART) =====
        if (!area.isEmpty()) {
            temp = temp.stream()
                    .filter(e -> {
                        if (e.getCity() == null) return false;
                        String city = e.getCity().toLowerCase();

                        return city.contains(area)
                                || area.contains(city)
                                || city.contains("noida")
                                || city.contains("greater noida");
                    })
                    .collect(Collectors.toList());
        }

        log.info("After city filter: {}", temp.size());

        // ===== COUNSELLING FILTER =====
        if (!counselling.equals("any") && !counselling.isEmpty()) {
            temp = temp.stream()
                    .filter(e -> e.getCounsellingBoard() != null &&
                            e.getCounsellingBoard().toLowerCase().contains(counselling))
                    .collect(Collectors.toList());
        }

        log.info("After counselling filter: {}", temp.size());

        // ===== BUDGET FILTER =====
        if (!budget.equals("any")) {
            temp = applyBudgetFilter(temp, budget);
        }

        log.info("After budget filter: {}", temp.size());

        // ===== ELIGIBILITY =====
        double buffer = effectiveRank * 0.95;

        List<CollegeEntry> eligible = temp.stream()
                .filter(e -> e.getClosingRank() >= buffer)
                .collect(Collectors.toList());

        log.info("Eligible: {}", eligible.size());

        // ===== FALLBACK (DB query: nearest closing ranks) =====
        if (eligible.isEmpty()) {
            log.warn("⚠️ NO ELIGIBLE → FALLBACK (database query)");

            eligible = repo.findNearestByClosingRank(effectiveRank, PageRequest.of(0, 10))
                    .stream()
                    .map(this::toDomain)
                    .collect(Collectors.toList());
        }

        // ===== SORT =====
        Integer branchCode = branchMapping.get(branchInput);

        eligible.sort((a, b) -> {
            double diffA = Math.abs(a.getClosingRank() - effectiveRank);
            double diffB = Math.abs(b.getClosingRank() - effectiveRank);

            if (branchCode != null) {
                if (a.getBranchCode() != branchCode) diffA += 100000;
                if (b.getBranchCode() != branchCode) diffB += 100000;
            }

            return Double.compare(diffA, diffB);
        });

        // ===== RESULTS (best cutoff per college+branch, top 5) =====
        Set<String> seen = new HashSet<>();
        List<CollegeResult> results = new ArrayList<>();

        for (CollegeEntry e : eligible) {
            if (seen.add(e.getCollege() + "|" + e.getBranch())) {
                results.add(buildResult(e, effectiveRank));
                if (results.size() == 5) break;
            }
        }

        return results;
    }

    // ================= CSV PARSING =================

    private void loadCsvFile(String path, List<CollegeEntry> out) {
        try {
            List<String[]> rows = readCsv(path);
            if (rows.isEmpty()) return;

            Map<String, Integer> colIdx = buildColumnIndex(rows.get(0));

            int iInstitute = requireCol(colIdx, "Institute");
            int iProgram = requireCol(colIdx, "Program", "Academic Program Name");
            int iOpenRank = requireCol(colIdx, "Opening Rank");
            int iCloseRank = requireCol(colIdx, "Closing Rank");

            if (iInstitute < 0 || iProgram < 0 || iOpenRank < 0 || iCloseRank < 0) {
                log.warn("⚠️ {}: required columns missing, skipping", path);
                return;
            }

            int iYear = colIdx.getOrDefault(clean("Year"), -1);
            int iCity = colIdx.getOrDefault(clean("City"), -1);
            int iCounselling = colIdx.getOrDefault(clean("Counselling Board"), -1);
            int iFees = colIdx.getOrDefault(clean("Fees Lakhs"), -1);
            int iAvgPkg = colIdx.getOrDefault(clean("Avg Package"), -1);

            for (int i = 1; i < rows.size(); i++) {
                String[] row = rows.get(i);

                String college = cell(row, iInstitute);
                String branch = cell(row, iProgram);
                double opening = parseRank(cell(row, iOpenRank));
                double closing = parseRank(cell(row, iCloseRank));

                if (college.isEmpty() || branch.isEmpty()
                        || Double.isNaN(opening) || Double.isNaN(closing))
                    continue;

                CollegeEntry e = new CollegeEntry();
                e.setCollege(college);
                e.setBranch(branch);
                e.setOpeningRank(opening);
                e.setClosingRank(closing);

                if (iYear >= 0) {
                    String y = cell(row, iYear);
                    if (!y.isEmpty()) {
                        try { e.setYear(Integer.parseInt(y.trim())); }
                        catch (NumberFormatException ignored) { }
                    }
                }

                if (iCity >= 0) e.setCity(cell(row, iCity));
                if (iCounselling >= 0) e.setCounsellingBoard(cell(row, iCounselling));
                if (iFees >= 0) e.setFeesLakhs(parseDouble(cell(row, iFees)));
                if (iAvgPkg >= 0) e.setAvgPackage(parseDouble(cell(row, iAvgPkg)));

                out.add(e);
            }
        } catch (Exception e) {
            log.error("❌ CSV ERROR in {}", path, e);
        }
    }

    // ================= DATABASE MAPPING =================

    private void loadFromDatabase() {
        dataset = repo.findAll().stream().map(this::toDomain).collect(Collectors.toList());

        List<String> branches = dataset.stream()
                .map(CollegeEntry::getBranch)
                .distinct().sorted()
                .toList();

        for (int i = 0; i < branches.size(); i++)
            branchMapping.put(branches.get(i), i);

        dataset.forEach(e ->
                e.setBranchCode(branchMapping.getOrDefault(e.getBranch(), -1)));

        log.info("✅ DATA LOADED FROM DATABASE: {}", dataset.size());
    }

    private void seedDatabase(List<CollegeEntry> rows) {
        List<CollegeEntity> entities = rows.stream().map(e -> {
            CollegeEntity c = new CollegeEntity();
            c.setCollege(e.getCollege());
            c.setBranch(e.getBranch());
            c.setYear(e.getYear());
            c.setOpeningRank(e.getOpeningRank());
            c.setClosingRank(e.getClosingRank());
            c.setBranchCode(e.getBranchCode());
            c.setCity(e.getCity());
            c.setCounsellingBoard(e.getCounsellingBoard());
            c.setFeesLakhs(e.getFeesLakhs());
            c.setAvgPackage(e.getAvgPackage());
            return c;
        }).toList();
        repo.saveAll(entities);
    }

    private CollegeEntry toDomain(CollegeEntity c) {
        CollegeEntry e = new CollegeEntry();
        e.setCollege(c.getCollege());
        e.setBranch(c.getBranch());
        e.setYear(c.getYear());
        e.setOpeningRank(c.getOpeningRank());
        e.setClosingRank(c.getClosingRank());
        e.setBranchCode(branchMapping.getOrDefault(c.getBranch(), c.getBranchCode()));
        e.setCity(c.getCity());
        e.setCounsellingBoard(c.getCounsellingBoard());
        e.setFeesLakhs(c.getFeesLakhs());
        e.setAvgPackage(c.getAvgPackage());
        return e;
    }

    // ================= HELPERS =================

    private String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String mapBudget(String input) {
        return switch (input) {
            case "5 Lakh - 10 Lakh" -> "5-10";
            case "10 Lakh - 20 Lakh" -> "10-20";
            case "20 Lakh - 40 Lakh" -> "20-40";
            case "Above 40 Lakh" -> "40+";
            default -> "any";
        };
    }

    private List<CollegeEntry> applyBudgetFilter(List<CollegeEntry> list, String budget) {
        return list.stream().filter(e -> {
            if (e.getFeesLakhs() == null) return false;
            double f = e.getFeesLakhs();

            return switch (budget) {
                case "5-10" -> f >= 5 && f <= 10;
                case "10-20" -> f >= 10 && f <= 20;
                case "20-40" -> f >= 20 && f <= 40;
                case "40+" -> f > 40;
                default -> true;
            };
        }).collect(Collectors.toList());
    }

    private CollegeResult buildResult(CollegeEntry e, int rank) {
        double diff = Math.abs(e.getClosingRank() - rank);

        int score = (e.getClosingRank() >= rank)
                ? Math.max(50, 98 - (int)((diff / rank) * 80))
                : Math.max(30, 85 - (int)((diff / rank) * 100));

        return new CollegeResult(
                e.getCollege(),
                e.getBranch(),
                e.getClosingRank(),
                Math.min(score, 99),
                e.getCity() != null ? e.getCity() : "",
                e.getAvgPackage() != null ? e.getAvgPackage() + " LPA" : "-",
                e.getFeesLakhs() != null ? e.getFeesLakhs() + " Lakhs" : "-"
        );
    }

    private List<String[]> readCsv(String path) throws Exception {
        var parser = new CSVParserBuilder().withSeparator(',').build();
        try (var reader = new CSVReaderBuilder(new FileReader(path))
                .withCSVParser(parser).build()) {
            return reader.readAll();
        } catch (Exception e) {
            var stream = getClass().getClassLoader().getResourceAsStream(path);
            if (stream == null) throw e;
            try (var reader = new CSVReaderBuilder(new InputStreamReader(stream))
                    .withCSVParser(parser).build()) {
                return reader.readAll();
            }
        }
    }

    private Map<String, Integer> buildColumnIndex(String[] header) {
        Map<String, Integer> map = new HashMap<>();
        for (int i = 0; i < header.length; i++)
            map.put(clean(header[i]), i);
        return map;
    }

    private String clean(String s) {
        return s.replaceAll("[^a-zA-Z0-9\\s]", "").trim();
    }

    private int requireCol(Map<String, Integer> map, String... names) {
        for (String n : names) {
            Integer i = map.get(clean(n));
            if (i != null) return i;
        }
        return -1;
    }

    private double parseRank(String raw) {
        String s = raw.replaceAll("\\D", "");
        if (s.isEmpty()) return Double.NaN;
        return Double.parseDouble(s);
    }

    private Double parseDouble(String raw) {
        try {
            return raw == null ? null : Double.parseDouble(raw.trim());
        } catch (Exception e) {
            return null;
        }
    }

    private String cell(String[] row, int i) {
        return (i >= 0 && i < row.length) ? row[i].trim() : "";
    }

    private int parseCategoryRank(String val) {
        try {
            return Integer.parseInt(val.trim());
        } catch (Exception e) {
            return 0;
        }
    }
}
