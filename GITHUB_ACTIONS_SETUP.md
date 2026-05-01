# GitHub Actions CI/CD Pipeline - Implementation Guide

## Overview

The `sonar-scan.yml` workflow implements a **Dynamic Matrix CI/CD Pipeline** that efficiently runs tests and SonarQube scans for your Java Spring Boot microservices project. This workflow minimizes GitHub Actions minutes by only testing changed modules.

## Workflow Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     GitHub Event (Push/PR)                      │
└────────────────────────┬────────────────────────────────────────┘
                         │
                         ▼
        ┌────────────────────────────────┐
        │    Job 1: Detect Changes       │
        │    (dorny/paths-filter@v3)     │
        │                                │
        │  Detects which 10 modules      │
        │  have changes using path       │
        │  filtering                     │
        │                                │
        │  Output: JSON array of         │
        │  changed modules               │
        └────────┬──────────────┬────────┘
                 │              │
        ┌────────▼──┐      ┌────▼──────────┐
        │ Matrix: [] │      │ Matrix: [m1,] │
        │(No changes)       │(Has changes)  │
        │    │              │     │         │
        │    │              │     ▼         │
        │    │              │  Job 2: Build │
        │    │              │  & Test       │
        │    │              │  (Dynamic     │
        │    │              │   Matrix)     │
        │    │              │     │         │
        │    │              │     ▼         │
        │    │              │  Artifacts:   │
        │    │              │  jacoco.xml   │
        │    │              │     │         │
        │    └──────────┬───┘     │         │
        │               │         │         │
        │               ▼         ▼         │
        │       ┌────────────────────┐     │
        │       │  Job 3: SonarQube  │     │
        │       │  Scan (always())   │     │
        │       │                    │     │
        │       │  Download Jacoco   │     │
        │       │  reports           │     │
        │       │  Compile project   │     │
        │       │  Run SonarQube     │     │
        │       │  scan              │     │
        │       └────────────────────┘     │
        │               │                   │
        └───────────────┼───────────────────┘
                        ▼
        ┌────────────────────────────────┐
        │  Code Quality Analysis         │
        │  https://sonarcloud.io/        │
        └────────────────────────────────┘
```

## Job Descriptions

### Job 1: Detect Changes (`detect-changes`)

**Purpose:** Identify which of the 10 core modules have changes

**How it works:**
1. Uses `dorny/paths-filter@v3` to detect file changes in specific directory paths
2. Monitors these 10 modules:
   - `media`, `product`, `cart`, `location`, `order`, `customer`, `rating`, `inventory`, `tax`, `search`
3. Converts the filter output into a JSON array format for matrix strategy
4. Outputs two values:
   - `modules`: JSON array of changed modules (e.g., `["media", "product"]`)
   - `has-changes`: Boolean flag indicating if any changes detected

**Key Features:**
- Uses full git history (`fetch-depth: 0`) for accurate change detection
- Handles both push and pull request events
- Provides formatted output compatible with matrix strategy

**Configuration:**
```yaml
filters: |
  media:
    - 'media/**'
  product:
    - 'product/**'
  # ... (continues for all 10 modules)
```

### Job 2: Build & Test (`build-and-test`)

**Purpose:** Run unit tests for each changed module and generate Jacoco coverage reports

**Dependencies:**
- `needs: detect-changes` - Waits for change detection
- `if: fromJson(...) != null && ... > 0` - Only runs if modules changed

**Dynamic Matrix:**
```yaml
strategy:
  matrix:
    module: ${{ fromJson(needs.detect-changes.outputs.modules) }}
  max-parallel: 4
  fail-fast: false
```

**How it works:**
1. For each changed module, runs:
   ```bash
   mvn clean \
     org.jacoco:jacoco-maven-plugin:prepare-agent \
     test \
     org.jacoco:jacoco-maven-plugin:report \
     -pl <module> \
     -am
   ```

2. Maven flags explained:
   - `clean`: Remove previous build artifacts
   - `org.jacoco:jacoco-maven-plugin:prepare-agent`: Initialize Jacoco agent for coverage
   - `test`: Run unit tests
   - `org.jacoco:jacoco-maven-plugin:report`: Generate coverage XML report
   - `-pl <module>`: Only build the specified module
   - `-am`: Also build module's dependencies (ancestors)
   - `--batch-mode`: Non-interactive mode
   - `--fail-at-end`: Continue testing all modules even if one fails

3. Uploads generated `jacoco.xml` as artifact for Job 3:
   - Located at: `<module>/target/site/jacoco/jacoco.xml`
   - Artifact name: `jacoco-report-<module>`
   - Retention: 7 days

**Benefits:**
- Runs tests only for changed modules
- Parallel execution (max 4 jobs) reduces total time
- Non-blocking failures (fail-fast: false) ensure all modules are tested
- Coverage reports preserved for SonarQube analysis

### Job 3: SonarQube Cloud Scan (`sonarqube-scan`)

**Purpose:** Perform code quality analysis using SonarQube Cloud with coverage data

**Dependencies:**
- `needs: [detect-changes, build-and-test]` - Waits for both jobs
- `if: always()` - Runs regardless of Job 2 outcome

**How it works:**
1. Downloads all Jacoco XML artifacts from Job 2
2. Compiles the project to generate `.class` files:
   ```bash
   mvn clean compile --batch-mode --fail-at-end
   ```
   - SonarQube requires compiled binaries for accurate analysis

3. Locates all coverage reports dynamically:
   ```bash
   find coverage-reports -name "jacoco.xml" -type f | paste -sd "," -
   ```

4. Runs SonarQube Cloud scan with:
   ```
   -Dsonar.projectKey=nashtech-garage_yas-yas-parent
   -Dsonar.organization=nashtech-garage
   -Dsonar.coverage.jacoco.xmlReportPaths=<comma-separated paths>
   -Dsonar.java.binaries=**/target/classes
   ```

**Key Parameters:**
- `sonar.coverage.jacoco.xmlReportPaths`: Points to Jacoco coverage reports
- `sonar.java.binaries`: Points to compiled `.class` files for analysis

**Always Runs:**
- Even if Job 2 didn't run (no module changes) - ensures PR analysis
- Even if tests failed - captures available coverage

## Triggering the Workflow

The workflow triggers on:

### 1. Push Events (except main branch)
```yaml
on:
  push:
    branches:
      - '**'
      - '!main'
```
- Any push to any branch except `main`
- Useful for feature branches and development

### 2. Pull Requests to Main
```yaml
on:
  pull_request:
    branches:
      - 'main'
```
- Any PR targeting `main` branch
- For code review and quality gates

### 3. Concurrency Control
```yaml
concurrency:
  group: ${{ github.workflow }}-${{ github.ref }}
  cancel-in-progress: true
```
- Cancels previous workflow runs on the same branch
- Saves GitHub Actions minutes

## Prerequisites & Setup

### 1. GitHub Secrets Configuration

You need to configure these secrets in your repository settings:

**Settings → Secrets and variables → Actions → New repository secret**

#### Required Secrets:

**`SONAR_TOKEN`**
1. Go to https://sonarcloud.io/account/security
2. Click "Generate Tokens" → "Generate"
3. Copy the token
4. Add as GitHub secret `SONAR_TOKEN`

**`GITHUB_TOKEN`** (Already provided by GitHub)
- Automatically available in GitHub Actions
- Used for PR decorations and authentication

### 2. SonarQube Cloud Setup

1. Visit: https://sonarcloud.io/
2. Sign up with GitHub account
3. Import repository: `nashtech-garage/yas`
4. Configure project:
   - **Project Key:** `nashtech-garage_yas-yas-parent` (already in workflow)
   - **Organization:** `nashtech-garage` (already in workflow)
5. Add quality gates if needed (Settings → Quality Gates)

### 3. Java & Maven Configuration

The workflow automatically handles:
- ✅ JDK 25 setup via `actions/setup-java@v4`
- ✅ Maven dependency caching for faster builds
- ✅ Project structure detection (Maven multi-module)

Ensure your `pom.xml` has:
- JDK 25 compiler configuration (already set in your pom.xml)
- Jacoco plugin (already configured in your pom.xml)

## GitHub Actions Minutes Optimization

### How This Workflow Saves Minutes:

1. **Dynamic Matrix Strategy**
   - Only tests changed modules (not entire project)
   - 10 modules monitored → typically 1-3 modules tested per PR

2. **Maven Parallel Execution**
   - `max-parallel: 4` limits concurrent jobs
   - Prevents GitHub Actions quota exhaustion

3. **Dependency Caching**
   - Maven dependencies cached across runs
   - Saves 30-60 seconds per build

4. **Efficient Artifact Management**
   - 7-day retention policy
   - Removes old artifacts automatically

### Estimated Savings:

| Scenario | Old (All modules) | New (Changed modules) | Savings |
|----------|------------------|----------------------|---------|
| Single module change | 15-20 min | 3-5 min | 70-80% |
| 3 modules changed | 15-20 min | 8-12 min | 40-60% |
| 10 modules changed | 15-20 min | 15-20 min | 0% |

## Monitoring & Troubleshooting

### View Workflow Runs

1. Go to: GitHub repository → Actions tab
2. Click: "SonarQube Cloud Scan with Dynamic Matrix"
3. Select: Latest workflow run

### Debug Mode

Add debug step if needed:
```yaml
- name: Debug outputs
  run: |
    echo "Changed modules: ${{ needs.detect-changes.outputs.modules }}"
    echo "Has changes: ${{ needs.detect-changes.outputs.has-changes }}"
```

### Common Issues

**Issue: No artifacts uploaded**
- Check if tests passed (`Job 2: Build & Test`)
- Verify Jacoco XML generated: `mvn clean test` locally

**Issue: SonarQube scan shows 0% coverage**
- Ensure Jacoco XML files downloaded successfully
- Check artifact names match pattern `jacoco-report-*`
- Verify `sonar.coverage.jacoco.xmlReportPaths` is set

**Issue: Tests failing**
- Run locally first: `mvn clean test`
- Check dependencies in `pom.xml`
- Verify JDK 25 compatibility

### Logs & Outputs

1. **Live logs:** View in GitHub Actions UI
2. **Workflow summary:** Shows job status and times
3. **SonarQube results:** https://sonarcloud.io/projects

## Customization

### Add More Modules

To monitor additional modules:

1. Add to `detect-changes` job filters:
```yaml
- name: Filter changed modules
  id: filter
  uses: dorny/paths-filter@v3
  with:
    filters: |
      newmodule:
        - 'newmodule/**'
```

### Adjust Matrix Parallelism

Modify `max-parallel` in `build-and-test` job:
```yaml
strategy:
  matrix:
    module: ${{ fromJson(needs.detect-changes.outputs.modules) }}
  max-parallel: 8  # Increase for faster execution
```

### Skip Job 3 on No Changes

Current behavior: Job 3 always runs for PR analysis
To skip Job 3 if no changes:
```yaml
sonarqube-scan:
  if: always() && needs.detect-changes.outputs.has-changes == 'true'
```

## References

- [GitHub Actions Documentation](https://docs.github.com/en/actions)
- [dorny/paths-filter@v3](https://github.com/dorny/paths-filter)
- [SonarCloud GitHub Action](https://github.com/SonarSource/sonarcloud-github-action)
- [Jacoco Maven Plugin](https://www.jacoco.org/jacoco/trunk/doc/maven.html)
- [Maven Multi-Module Projects](https://maven.apache.org/guides/mini/guide-multiple-modules.html)

## Support

For questions or issues:
1. Check GitHub Actions logs (Actions → Workflow run → Job)
2. Review SonarCloud project dashboard
3. Test locally with same Maven commands
4. Check your project's `.github/workflows/sonar-scan.yml` file
