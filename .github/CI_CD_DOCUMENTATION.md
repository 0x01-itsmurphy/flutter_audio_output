# CI/CD Pipeline Documentation

This document describes the comprehensive CI/CD pipeline for the Flutter Audio Output plugin, including automated testing, validation, and publishing to pub.dev.

## Overview

The CI/CD pipeline consists of two main workflows:

1. **CI/CD Pipeline** (`ci.yml`) - Continuous Integration and quality checks
2. **Publish to pub.dev** (`publish.yml`) - Automated publishing to pub.dev

## CI/CD Pipeline (`ci.yml`)

### Triggers
- Push to `main` or `develop` branches
- Pull requests to `main` branch
- Manual workflow dispatch

### Jobs

#### 1. 🔍 Code Analysis
- **Purpose**: Code quality and static analysis
- **Actions**:
  - Dart formatting validation
  - Flutter analyzer
  - Unit tests with coverage
  - Coverage upload to Codecov

#### 2. 🤖 Android Build & Test
- **Purpose**: Android platform validation
- **Matrix**: API levels 21, 29, 33
- **Actions**:
  - Build debug APK
  - Run unit tests
  - Run integration tests on Android emulator

#### 3. 🍎 iOS Build & Test
- **Purpose**: iOS platform validation
- **Matrix**: iPhone 15 and iPad Air (5th gen) with iOS 17.2
- **Actions**:
  - Build iOS app with Xcode
  - Run unit tests
  - Run integration tests on iOS simulator

#### 4. 🏗️ Build Example Apps
- **Purpose**: Build example applications
- **Matrix**: Ubuntu (Android) and macOS (iOS)
- **Actions**:
  - Build release APK for Android
  - Build release iOS app (no code signing)
  - Upload build artifacts

#### 5. 📦 Validate Package
- **Purpose**: Package validation for pub.dev
- **Actions**:
  - Dry run publish
  - PANA package analysis
  - Score validation

#### 6. 🔒 Security Scan
- **Purpose**: Security vulnerability scanning
- **Actions**:
  - Trivy filesystem scan
  - SARIF report upload to GitHub

#### 7. 📚 Documentation Check
- **Purpose**: Documentation completeness
- **Actions**:
  - Generate documentation
  - Check for missing documentation

#### 8. ✅ CI Success
- **Purpose**: Final status aggregation
- **Actions**:
  - Check all job statuses
  - Provide final CI result

## Publish Pipeline (`publish.yml`)

### Triggers
- GitHub releases (when published)
- Manual workflow dispatch with version input

### Jobs

#### 1. 🔍 Pre-publish Validation
- **Purpose**: Comprehensive pre-publish checks
- **Actions**:
  - Package structure validation
  - Version existence check on pub.dev
  - pubspec.yaml version validation
  - PANA score validation

#### 2. 🧪 Full Test Suite
- **Purpose**: Complete test suite execution
- **Actions**:
  - Calls the main CI workflow
  - Ensures all tests pass before publishing

#### 3. 🚀 Publish to pub.dev
- **Purpose**: Actual publishing to pub.dev
- **Actions**:
  - Setup pub.dev credentials
  - Perform dry run or actual publish
  - Generate publication summary

#### 4. ✅ Verify Publication
- **Purpose**: Post-publish verification
- **Actions**:
  - Wait for pub.dev propagation
  - Verify package availability
  - Create verification summary

#### 5. 📢 Notify on Failure
- **Purpose**: Failure notification and guidance
- **Actions**:
  - Create failure summary
  - Provide troubleshooting guidance

## Setup Requirements

### Repository Secrets

The following secrets need to be configured in your GitHub repository:

#### Required for CI/CD
- `CODECOV_TOKEN` - Token for coverage reporting (optional)

#### Required for Publishing
- `PUB_DEV_CREDENTIALS` - pub.dev credentials JSON

### Environment Setup

#### Production Environment
- Create a "production" environment in GitHub repository settings
- Configure required reviewers if needed for additional security
- Add protection rules as needed

### pub.dev Credentials Setup

1. **Generate pub.dev credentials**:
   ```bash
   flutter pub token
   ```

2. **Copy credentials**:
   ```bash
   cat ~/.pub-cache/credentials.json
   ```

3. **Add to GitHub Secrets**:
   - Go to repository Settings → Secrets and variables → Actions
   - Add new repository secret named `PUB_DEV_CREDENTIALS`
   - Paste the entire JSON content

## Usage

### Automated Publishing (Recommended)

1. **Update version in pubspec.yaml**
2. **Create a GitHub release**:
   - Tag format: `v1.2.3` or `1.2.3`
   - The pipeline will automatically extract the version
   - Publishing will be triggered automatically

### Manual Publishing

1. **Go to Actions tab**
2. **Select "Publish to pub.dev" workflow**
3. **Click "Run workflow"**
4. **Enter version number** (e.g., `1.2.3`)
5. **Choose dry run or live publish**

### Dry Run Testing

Always test with dry run first:
- Manual trigger with "Perform a dry run" checked
- Validates everything without actually publishing
- Shows what would be published

## Monitoring and Troubleshooting

### Common Issues

#### 1. Version Already Exists
- **Error**: Version already exists on pub.dev
- **Solution**: Increment version in pubspec.yaml

#### 2. Package Validation Failed
- **Error**: PANA score too low or validation issues
- **Solution**: Fix issues reported by `flutter pub publish --dry-run`

#### 3. Test Failures
- **Error**: Unit or integration tests failing
- **Solution**: Fix failing tests before publishing

#### 4. Credentials Issues
- **Error**: Invalid pub.dev credentials
- **Solution**: Regenerate and update `PUB_DEV_CREDENTIALS` secret

#### 5. Build Failures
- **Error**: Android/iOS build failures
- **Solution**: Check build logs and fix platform-specific issues

### Monitoring

#### GitHub Actions
- Monitor workflow runs in the Actions tab
- Check job logs for detailed information
- Review artifact uploads

#### pub.dev
- Monitor package page: https://pub.dev/packages/flutter_audio_output
- Check version availability
- Monitor download statistics

#### Coverage
- Check coverage reports in Codecov
- Monitor coverage trends over time

## Best Practices

### Version Management
- Use semantic versioning (semver)
- Update CHANGELOG.md before releasing
- Keep pubspec.yaml version in sync

### Testing
- Ensure all tests pass before publishing
- Add integration tests for new features
- Maintain good test coverage

### Documentation
- Keep README.md updated
- Document all public APIs
- Update example app for new features

### Security
- Regularly update dependencies
- Monitor security scan results
- Review and rotate credentials periodically

## Workflow Files

### Main CI/CD Pipeline
```yaml
# .github/workflows/ci.yml
# Comprehensive CI/CD pipeline with testing and validation
```

### Publishing Pipeline
```yaml
# .github/workflows/publish.yml
# Automated publishing to pub.dev with validation
```

## Integration with Development Workflow

### Pull Request Workflow
1. Developer creates feature branch
2. Creates pull request to main
3. CI pipeline runs automatically
4. All checks must pass for merge

### Release Workflow
1. Merge to main branch
2. Update version in pubspec.yaml
3. Update CHANGELOG.md
4. Create GitHub release
5. Publish pipeline runs automatically
6. Package is live on pub.dev

### Hotfix Workflow
1. Create hotfix branch from main
2. Fix issue and update version
3. Create pull request
4. After merge, create release
5. Automated publishing

## Maintenance

### Regular Tasks
- Update Flutter version in workflows
- Update GitHub Actions versions
- Review and update test matrices
- Monitor workflow performance

### Quarterly Reviews
- Review security scan results
- Update documentation
- Optimize workflow performance
- Update CI/CD best practices

This comprehensive CI/CD setup ensures high-quality, secure, and reliable publishing of the Flutter Audio Output plugin to pub.dev.
