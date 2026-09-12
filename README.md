# WiseWays Machine Engine — Java / Spring Boot

Full conversion of `machine.py` (Flask + pandas + scikit-learn) to
**Java 17 + Spring Boot 3**.

---

## ML note

`machine.py` trained a `RandomForestRegressor` but the live `/recommend` route
**never called `model.predict()`** — it sorted colleges purely by rank difference
with an optional branch bonus.  This Java port replicates that exact logic.

If you want the ML-backed prediction from `model.py`, add **Smile**:

```xml
<dependency>
    <groupId>com.github.haifengl</groupId>
    <artifactId>smile-core</artifactId>
    <version>3.1.1</version>
</dependency>
```

…and call `smile.regression.RandomForest.fit(formula, dataFrame)` inside
`DataService.initData()`.

---

## Project structure

```
wiseway-java/
├── pom.xml
└── src/main/
    ├── java/com/wiseways/
    │   ├── MachineApplication.java          ← Spring Boot entry point
    │   ├── config/
    │   │   ├── CorsConfig.java              ← replaces flask_cors CORS(app)
    │   │   └── RestTemplateConfig.java      ← HTTP client bean
    │   ├── controller/
    │   │   └── ApiController.java           ← GET /  POST /ask  POST /recommend
    │   ├── model/
    │   │   ├── CollegeEntry.java            ← internal row (≈ df row)
    │   │   ├── CollegeResult.java           ← /recommend response element
    │   │   └── RecommendRequest.java        ← /recommend request body
    │   └── service/
    │       ├── AiService.java               ← NVIDIA API client
    │       └── DataService.java             ← CSV loading + recommendation
    └── resources/
        └── application.properties
```

---

## Running

### Prerequisites
- Java 17+
- Maven 3.8+
- CSV files (`uptac2.csv`, `Book2.csv`, …) in the **same directory** you run from

### Build & run

```bash
# Build fat-jar
mvn clean package -DskipTests

# Run  (CSV files must be in the current directory)
java -jar target/machine-engine-1.0.0.jar

# — or run directly without packaging —
mvn spring-boot:run
```

Server starts on **http://localhost:5000** — the same port as the Python version.

---

## API

```
GET  /

POST /ask
  Body   : { "query": "What is JEE Advanced?" }
  Returns: { "response": "..." }

POST /recommend
  Body:
  {
    "rank"        : 5000,
    "categoryRank": "",
    "branch"      : "Computer Science and Engineering",
    "area"        : "",
    "budget"      : "any",
    "counselling" : "any"
  }
  Returns:
  {
    "colleges": [
      {
        "college"      : "...",
        "branch"       : "...",
        "closing_rank" : 4823,
        "match_score"  : 91,
        "city"         : "...",
        "avg_package"  : "12.5 LPA",
        "fees"         : "8.2 Lakhs"
      },
      ...
    ]
  }
```

---

## Configuration

`src/main/resources/application.properties`

```properties
server.port=5000
nvidia.api.base-url=https://integrate.api.nvidia.com/v1
nvidia.api.key=YOUR_API_KEY_HERE
nvidia.api.model=meta/llama-3.1-8b-instruct
csv.primary=uptac2.csv
```

## API Testing & Validation

The WiseWays recommendation API was tested using Postman with automated post-response scripts.

### Tested Endpoint

```http
POST /recommend

### Postman Test Result Screenshot

![Postman API Test Result](assets/api-testing-result.png)

---

## Database

College cutoff data is stored in a relational database via **Spring Data JPA**:

- **Default — H2 (file-based, zero setup):** `jdbc:h2:file:./data/wiseways`
- **MySQL:** run with `--spring.profiles.active=mysql` (see `application-mysql.properties`)
- On first run the CSV datasets (`uptac2.csv`, `JEE_Rank_2016_2024.csv` — 24,000+ JoSAA & UPTAC cutoff records, 2016–2024) are parsed and seeded into the `colleges` table; afterwards the app loads straight from the database.
- The no-match fallback path uses a custom JPQL query (`ORDER BY ABS(closingRank - :rank)`) against an indexed `closing_rank` column.
