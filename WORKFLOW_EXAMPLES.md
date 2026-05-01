# Workflow Examples & Expected Outputs

## Example 1: Single Module Change

### Scenario
Developer pushes changes to the `media` module only.

### GitHub Actions Workflow Output

```
✅ Workflow: SonarQube Cloud Scan with Dynamic Matrix

Jobs Summary:
├── ✅ detect-changes (2s)
│   └── Changed modules: ["media"]
│   └── has-changes: true
│
├── ✅ build-and-test [media] (45s)
│   ├── ✅ Test Module [media] (45s)
│   ├── Uploaded: jacoco-report-media
│   └── Coverage: 82.5%
│
└── ✅ sonarqube-scan (30s)
    ├── Downloaded: jacoco-report-media/jacoco.xml
    ├── Compiled project
    ├── Scanned with SonarQube
    └── Results: https://sonarcloud.io/...

Total Duration: ~1 min 17 sec
GitHub Actions Minutes: ~2 min
```

### Expected SonarCloud Output
```
Project: nashtech-garage_yas-yas-parent
Branch: feature/media-update

Quality Gate Status: ✅ PASSED

Coverage: 82.5%
  - New Coverage: 85.0%
  - Overall Coverage: 78.3%

Issues Found: 2
  - 1 Bug
  - 1 Code Smell

Duplicated Lines: 0.2%
```

---

## Example 2: Multiple Module Changes

### Scenario
PR with changes to `product`, `cart`, and `order` modules.

### GitHub Actions Workflow Output

```
✅ Workflow: SonarQube Cloud Scan with Dynamic Matrix

Jobs Summary:
├── ✅ detect-changes (2s)
│   └── Changed modules: ["product", "cart", "order"]
│   └── has-changes: true
│
├── ✅ build-and-test (90s) [Parallel Execution: max-parallel: 4]
│   ├── ✅ Test Module [product] (50s)
│   │   └── Uploaded: jacoco-report-product
│   │   └── Coverage: 79.2%
│   │
│   ├── ✅ Test Module [cart] (48s)
│   │   └── Uploaded: jacoco-report-cart
│   │   └── Coverage: 85.1%
│   │
│   └── ✅ Test Module [order] (52s)
│       └── Uploaded: jacoco-report-order
│       └── Coverage: 81.7%
│
└── ✅ sonarqube-scan (35s)
    ├── Downloaded: jacoco-report-product/jacoco.xml
    ├── Downloaded: jacoco-report-cart/jacoco.xml
    ├── Downloaded: jacoco-report-order/jacoco.xml
    ├── Compiled project
    ├── Scanned with SonarQube
    └── Results: https://sonarcloud.io/...

Total Duration: ~2 min 7 sec
GitHub Actions Minutes: ~4 min
```

### Expected SonarCloud Output
```
Project: nashtech-garage_yas-yas-parent
PR: #123

Quality Gate Status: ⚠️ FAILED

Overall Coverage: 82.0%
  - product: 79.2%
  - cart: 85.1%
  - order: 81.7%

Issues Found: 5
  - 2 Bugs
  - 3 Code Smells

Review Required:
  - Review with SonarQube findings
  - Address before merge
```

---

## Example 3: No Module Changes

### Scenario
Changes only to documentation or configuration files (not in the 10 modules).

### GitHub Actions Workflow Output

```
✅ Workflow: SonarQube Cloud Scan with Dynamic Matrix

Jobs Summary:
├── ✅ detect-changes (2s)
│   └── Changed modules: []
│   └── has-changes: false
│   └── ℹ️  No changes detected in monitored modules
│
├── ⏭️  build-and-test (SKIPPED)
│   └── Condition: matrix is empty
│   └── No matrix jobs created
│   └── No artifacts generated
│
└── ✅ sonarqube-scan (25s) [Runs always()]
    ├── No artifacts to download
    ├── Compiled project
    ├── Scanned entire project with SonarQube
    └── Results: https://sonarcloud.io/...
    └── ℹ️  SonarQube Cloud scan completed successfully

Total Duration: ~27 sec
GitHub Actions Minutes: ~1 min
```

**Note:** Job 3 still runs to ensure PR decorations and quality gates are applied.

---

## Example 4: Test Failure

### Scenario
Change in `media` module causes unit test to fail.

### GitHub Actions Workflow Output

```
⚠️  Workflow: SonarQube Cloud Scan with Dynamic Matrix

Jobs Summary:
├── ✅ detect-changes (2s)
│   └── Changed modules: ["media"]
│   └── has-changes: true
│
├── ⚠️  build-and-test [media] (50s)
│   ├── ⚠️  Test Module [media] (50s) - FAILED
│   │   ├── ✅ Compile successful
│   │   ├── ✅ Jacoco agent prepared
│   │   ├── ❌ Tests FAILED
│   │   │   └── Error: com.yas.media.controller.MediaControllerTest
│   │   │   └── Expected: "123", Got: "124"
│   │   ├── ⚠️  Jacoco report generated (partial)
│   │   └── Artifact uploaded: jacoco-report-media (with partial coverage)
│   │
│   └── Build Status: FAILURE
│
└── ✅ sonarqube-scan (28s) [Runs always()]
    ├── Downloaded: jacoco-report-media/jacoco.xml
    ├── Compiled project
    ├── Scanned with available coverage
    └── Results: https://sonarcloud.io/...

Total Duration: ~1 min 20 sec
GitHub Actions Minutes: ~2 min
```

### GitHub Status Check
```
❌ Status Check Failed

Check Details:
- Detect Changes: ✅ PASSED
- Test Module [media]: ❌ FAILED
- SonarQube Cloud Scan: ⚠️  PASSED (with warnings)

Blocking PR Merge: YES
Reason: Test failure in media module

Action Required:
1. Fix failing test in media module
2. Commit and push fix
3. Workflow runs again
```

---

## Example 5: Large PR - All 10 Modules

### Scenario
Major refactoring affecting all 10 modules.

### GitHub Actions Workflow Output

```
✅ Workflow: SonarQube Cloud Scan with Dynamic Matrix

Jobs Summary:
├── ✅ detect-changes (2s)
│   └── Changed modules: ["media", "product", "cart", "location", "order", 
│                         "customer", "rating", "inventory", "tax", "search"]
│   └── has-changes: true
│
├── ✅ build-and-test (120s) [Parallel Execution: max-parallel: 4]
│   │
│   ├── Batch 1 (parallel):
│   │   ├── ✅ Test Module [media] (50s) → jacoco-report-media
│   │   ├── ✅ Test Module [product] (52s) → jacoco-report-product
│   │   ├── ✅ Test Module [cart] (48s) → jacoco-report-cart
│   │   └── ✅ Test Module [location] (51s) → jacoco-report-location
│   │
│   ├── Batch 2 (parallel):
│   │   ├── ✅ Test Module [order] (49s) → jacoco-report-order
│   │   ├── ✅ Test Module [customer] (51s) → jacoco-report-customer
│   │   ├── ✅ Test Module [rating] (47s) → jacoco-report-rating
│   │   └── ✅ Test Module [inventory] (50s) → jacoco-report-inventory
│   │
│   └── Batch 3 (parallel):
│       ├── ✅ Test Module [tax] (46s) → jacoco-report-tax
│       └── ✅ Test Module [search] (55s) → jacoco-report-search
│
│   Total Artifacts: 10 jacoco.xml files
│   Average Coverage: 81.3%
│
└── ✅ sonarqube-scan (40s)
    ├── Downloaded: 10 jacoco.xml files
    ├── Coverage paths: /tmp/coverage-reports/*/jacoco.xml (10 files)
    ├── Compiled project
    ├── Scanned with all coverage data
    └── Results: https://sonarcloud.io/...

Total Duration: ~2 min 42 sec
GitHub Actions Minutes: ~5 min
```

### SonarCloud Report Summary
```
Overall Statistics:
├── Lines of Code: 45,230
├── Test Coverage: 81.3%
├── Duplicated Lines: 2.1%
├── Code Smells: 12
├── Bugs: 3
└── Security Hotspots: 1

Module Breakdown:
├── media: 82.5% (4,231 LOC)
├── product: 79.2% (4,891 LOC)
├── cart: 85.1% (3,456 LOC)
├── location: 81.0% (2,876 LOC)
├── order: 81.7% (5,123 LOC)
├── customer: 80.3% (4,234 LOC)
├── rating: 78.9% (2,345 LOC)
├── inventory: 82.1% (4,567 LOC)
├── tax: 79.8% (2,123 LOC)
└── search: 83.4% (3,384 LOC)

Quality Gate: ⚠️  FAILED
Reason: Coverage < 85% threshold
```

---

## Workflow Logs - Detailed Step-by-Step

### Job 1: Detect Changes - Full Log

```
2024-05-01T10:15:23.456Z Checkout code
2024-05-01T10:15:25.123Z ✅ Checked out to commit abc1234

2024-05-01T10:15:26.789Z Filter changed modules
2024-05-01T10:15:28.234Z Comparing: main...feature/update-media
2024-05-01T10:15:29.567Z Changed files:
2024-05-01T10:15:29.568Z   - media/src/main/java/com/yas/media/controller/MediaController.java
2024-05-01T10:15:29.569Z   - media/src/test/java/com/yas/media/controller/MediaControllerTest.java
2024-05-01T10:15:29.570Z   - media/pom.xml
2024-05-01T10:15:30.891Z ✅ Matched filter: media

2024-05-01T10:15:31.234Z Format module list for matrix
2024-05-01T10:15:31.567Z Input: "media"
2024-05-01T10:15:31.890Z Output: ["media"]

2024-05-01T10:15:32.123Z Set matrix output
2024-05-01T10:15:32.456Z matrix=["media"]

2024-05-01T10:15:32.789Z Check if there are changes
2024-05-01T10:15:33.012Z has-changes=true
2024-05-01T10:15:33.345Z ℹ️  Notice: Changes detected in modules: ["media"]
```

### Job 2: Build & Test - Full Log (Single Module)

```
2024-05-01T10:15:45.123Z Checkout code
2024-05-01T10:15:47.456Z ✅ Checked out to commit abc1234

2024-05-01T10:15:48.789Z Set up JDK 25
2024-05-01T10:15:52.123Z ✅ Java version: 25.0.0

2024-05-01T10:15:52.456Z Cache Maven packages
2024-05-01T10:15:53.789Z ✅ Cache key: linux-maven-abc123def456
2024-05-01T10:15:54.012Z ✅ Cache hit: Found existing cache

2024-05-01T10:15:54.345Z Run tests and generate Jacoco coverage for media
2024-05-01T10:16:00.789Z [INFO] Scanning for projects...
2024-05-01T10:16:01.234Z [INFO] ----- media [jar] -----
2024-05-01T10:16:01.567Z [INFO] clean
2024-05-01T10:16:02.123Z [INFO] Deleting /media/target
2024-05-01T10:16:02.456Z [INFO] Preparing Jacoco...
2024-05-01T10:16:03.789Z [INFO] Running tests: MediaControllerTest, MediaServiceTest, ...
2024-05-01T10:16:35.123Z [INFO] Tests: 45 run, 45 passed, 0 failed
2024-05-01T10:16:35.456Z [INFO] Building Jacoco report...
2024-05-01T10:16:36.789Z [INFO] Report generated: target/site/jacoco/jacoco.xml
2024-05-01T10:16:37.012Z ✅ BUILD SUCCESS

2024-05-01T10:16:37.345Z Upload Jacoco coverage report for media
2024-05-01T10:16:38.678Z Uploading artifact: jacoco-report-media
2024-05-01T10:16:39.123Z Files in artifact:
2024-05-01T10:16:39.234Z   - media/target/site/jacoco/jacoco.xml (12.4 KB)
2024-05-01T10:16:39.345Z ✅ Artifact uploaded successfully
```

### Job 3: SonarQube Scan - Full Log

```
2024-05-01T10:16:52.123Z Checkout code
2024-05-01T10:16:54.456Z ✅ Checked out to commit abc1234 (full history)

2024-05-01T10:16:55.789Z Set up JDK 25
2024-05-01T10:16:59.123Z ✅ Java version: 25.0.0

2024-05-01T10:16:59.456Z Cache Maven packages
2024-05-01T10:17:00.789Z ✅ Cache hit: Found existing cache

2024-05-01T10:17:01.012Z Create coverage output directory
2024-05-01T10:17:01.345Z ✅ Created: coverage-reports/

2024-05-01T10:17:01.678Z Download Jacoco coverage reports
2024-05-01T10:17:03.123Z Downloading: jacoco-report-media
2024-05-01T10:17:03.456Z ✅ Downloaded to: coverage-reports/jacoco-report-media/

2024-05-01T10:17:03.789Z List downloaded coverage reports
2024-05-01T10:17:04.012Z Coverage reports structure:
2024-05-01T10:17:04.123Z coverage-reports/jacoco-report-media/jacoco.xml

2024-05-01T10:17:04.456Z Compile project for SonarQube
2024-05-01T10:17:08.789Z [INFO] Compiling...
2024-05-01T10:17:25.123Z [INFO] Compilation successful
2024-05-01T10:17:25.456Z ✅ Generated: target/classes/ (10.2 MB)

2024-05-01T10:17:25.789Z Prepare SonarQube parameters
2024-05-01T10:17:26.012Z Coverage files found: coverage-reports/jacoco-report-media/jacoco.xml
2024-05-01T10:17:26.345Z ℹ️  Notice: Using coverage reports: coverage-reports/jacoco-report-media/jacoco.xml

2024-05-01T10:17:26.678Z SonarQube Cloud scan
2024-05-01T10:17:27.123Z [INFO] SonarCloud analysis started...
2024-05-01T10:17:30.456Z [INFO] Found 1,234 Java files
2024-05-01T10:17:35.789Z [INFO] Processing coverage data: 82.5%
2024-05-01T10:17:40.123Z [INFO] Analyzing code smells...
2024-05-01T10:17:42.456Z [INFO] Analyzing security issues...
2024-05-01T10:17:47.789Z [INFO] Upload to SonarCloud...
2024-05-01T10:17:52.123Z ✅ SonarCloud analysis completed

2024-05-01T10:17:52.456Z SonarQube scan completed
2024-05-01T10:17:52.789Z ℹ️  Notice: SonarQube Cloud scan completed successfully
2024-05-01T10:17:52.890Z View results at: https://sonarcloud.io/project/overview?id=nashtech-garage_yas-yas-parent
```

---

## Performance Comparison: Before vs After

### Before (All Modules, Every Push)
```
Scenario: Push to feature/update-media

Build Time: 18 minutes
├── media: 2 min
├── product: 2 min
├── cart: 2 min
├── location: 2 min
├── order: 2 min
├── customer: 2 min
├── rating: 2 min
├── inventory: 2 min
├── tax: 2 min
├── search: 2 min

GitHub Actions Minutes Used: 18 minutes
Cost: $0.36/month (at $0.008/min)
```

### After (Only Changed Modules, Dynamic Matrix)
```
Scenario: Push to feature/update-media

Build Time: 2 minutes 30 seconds
├── detect-changes: 5 sec
├── Test [media]: 45 sec
├── SonarQube scan: 40 sec

GitHub Actions Minutes Used: ~2.5 minutes
Cost: $0.02/month (at $0.008/min)

SAVINGS: 87% reduction in GitHub Actions minutes! 🎉
```

---

## Branch Protection Rules

Recommended configuration in GitHub:

```
Settings → Branches → Branch protection rules

Rule Name: Protect main

Require status checks to pass before merging:
  ✅ detect-changes
  ✅ build-and-test (any matrix job)
  ✅ sonarqube-scan

Require branches to be up to date before merging: ✅

Require approval from code owners: ✅

Require status checks from a code owner: ✅
```

---

**For more details, see:**
- `GITHUB_ACTIONS_SETUP.md` - Complete documentation
- `SECRETS_SETUP.md` - Secrets configuration
- `.github/workflows/sonar-scan.yml` - Workflow source code
