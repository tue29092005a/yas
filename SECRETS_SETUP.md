# GitHub Secrets Configuration Checklist

## Quick Setup Guide

Follow these steps to get the `sonar-scan.yml` workflow running:

### Step 1: Generate SonarQube Token

1. Navigate to: https://sonarcloud.io/account/security
2. Click **"Generate Tokens"**
3. Name: `GitHub Actions YAS`
4. Click **"Generate"**
5. **Copy the token** (you'll need it in Step 2)

### Step 2: Add GitHub Secret

1. Go to your GitHub repository
2. Navigate to: **Settings** → **Secrets and variables** → **Actions**
3. Click **"New repository secret"**
4. Name: `SONAR_TOKEN`
5. Secret: *Paste the token from Step 1*
6. Click **"Add secret"**

### Step 3: Verify SonarQube Setup

1. Visit: https://sonarcloud.io/
2. Sign in with GitHub
3. Find your organization: `nashtech-garage`
4. Verify project key: `nashtech-garage_yas-yas-parent`
5. No additional setup needed if already configured

### Step 4: Test the Workflow

1. Push a change to any branch (except `main`):
   ```bash
   git checkout -b test-workflow
   echo "# Test" >> README.md
   git add .
   git commit -m "Test workflow trigger"
   git push origin test-workflow
   ```

2. Go to GitHub: **Actions** tab
3. Select **"SonarQube Cloud Scan with Dynamic Matrix"**
4. Watch the workflow execute

### Step 5: Monitor Results

After workflow completes:

1. **GitHub Actions**: Check status and logs
2. **SonarQube Cloud**: View at https://sonarcloud.io/projects
3. **Coverage**: Should show Jacoco coverage for changed modules

---

## Required Secrets Summary

| Secret Name | Value | Where to Get | Required |
|------------|-------|-------------|----------|
| `SONAR_TOKEN` | SonarQube authentication token | https://sonarcloud.io/account/security | ✅ Yes |
| `GITHUB_TOKEN` | Automatically available | GitHub provides by default | ✅ Yes (auto) |

---

## Troubleshooting

### ❌ Error: "No SONAR_TOKEN provided"
- **Solution**: Add `SONAR_TOKEN` secret (see Step 2)

### ❌ Error: "No modules changed - no matrix jobs"
- **Expected behavior**: Job 2 skips if only non-module files changed
- **Solution**: Change a file in one of the 10 modules

### ❌ Error: "Jacoco XML not found"
- **Check**: Tests must pass to generate coverage files
- **Solution**: Run locally: `mvn clean test` in a module

### ✅ Success Indicators
- ✅ Job 1 shows changed modules
- ✅ Job 2 runs matrix for each module
- ✅ Job 3 downloads artifacts and scans
- ✅ SonarCloud dashboard updates

---

## Useful Commands for Local Testing

Test the workflow locally before pushing:

```bash
# Build and test a specific module
mvn clean org.jacoco:jacoco-maven-plugin:prepare-agent test \
  org.jacoco:jacoco-maven-plugin:report -pl media -am

# Compile project
mvn clean compile

# Find generated coverage files
find . -name "jacoco.xml" -type f
```

---

## Dashboard Links

After setup, bookmark these:

- **SonarCloud Project**: https://sonarcloud.io/project/overview?id=nashtech-garage_yas-yas-parent
- **GitHub Actions**: https://github.com/tue29092005a/yas/actions
- **Repository Settings**: https://github.com/tue29092005a/yas/settings/secrets/actions
- **SonarCloud Tokens**: https://sonarcloud.io/account/security

---

## FAQ

**Q: Why does Job 3 (SonarQube) always run?**
A: By design, SonarQube scans run even if no modules changed, to ensure PR decorations and quality gates work on all PRs.

**Q: Can I run the workflow manually?**
A: Currently, it triggers automatically on push/PR. To add manual trigger:
   ```yaml
   on:
     workflow_dispatch:
   ```

**Q: How long does the workflow take?**
A: Typically 3-8 minutes depending on number of changed modules and cache hits.

**Q: What happens if a test fails?**
A: Job 2 continues with `fail-fast: false`, Job 3 still runs with available coverage data.

---

## Next Steps

1. ✅ Configure secrets (this checklist)
2. ✅ Create PR or push to test workflow
3. ✅ Monitor GitHub Actions and SonarCloud dashboards
4. ✅ Set up branch protection rules requiring status checks
5. ✅ Configure SonarCloud quality gates if needed

---

**Need help?** See `GITHUB_ACTIONS_SETUP.md` for detailed documentation.
