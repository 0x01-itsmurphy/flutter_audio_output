# Publishing to pub.dev

This document explains how to publish the Flutter Audio Output plugin to pub.dev using the automated CI/CD pipeline.

## Prerequisites

Before publishing, ensure you have:

1. **GitHub Repository Access**: Admin access to configure secrets
2. **pub.dev Account**: Account with publishing permissions
3. **Updated Code**: All changes committed and pushed to main branch

## Setup Process

### 1. Configure pub.dev Credentials

First, generate your pub.dev credentials:

```bash
# Generate credentials (this will open a browser)
flutter pub token

# The credentials will be saved to ~/.pub-cache/credentials.json
```

Copy the credentials content:

```bash
cat ~/.pub-cache/credentials.json
```

### 2. Add GitHub Secrets

1. Go to your GitHub repository
2. Navigate to **Settings** → **Secrets and variables** → **Actions**
3. Add the following repository secret:

   - **Name**: `PUB_DEV_CREDENTIALS`
   - **Value**: The entire content of `credentials.json`

### 3. Configure Production Environment

1. Go to **Settings** → **Environments**
2. Create a new environment named `production`
3. (Optional) Add protection rules:
   - Required reviewers
   - Deployment branches (e.g., only `main`)

## Publishing Methods

### Method 1: Automated Release Publishing (Recommended)

This is the easiest and most reliable method:

1. **Update Version**: Update the version in `pubspec.yaml`
   ```yaml
   version: 1.2.3
   ```

2. **Update Changelog**: Add changes to `CHANGELOG.md`
   ```markdown
   ## 1.2.3
   - Fixed iOS receiver switching issue
   - Updated Android for Gradle 8.8 compatibility
   - Added comprehensive CI/CD pipeline
   ```

3. **Commit Changes**:
   ```bash
   git add pubspec.yaml CHANGELOG.md
   git commit -m "Bump version to 1.2.3"
   git push origin main
   ```

4. **Create GitHub Release**:
   - Go to **Releases** → **Create a new release**
   - **Tag**: `v1.2.3` (or `1.2.3`)
   - **Title**: `Release 1.2.3`
   - **Description**: Copy from CHANGELOG.md
   - Click **Publish release**

5. **Automated Publishing**: The workflow will automatically:
   - Run all tests and validations
   - Publish to pub.dev
   - Verify publication
   - Create status summary

### Method 2: Manual Workflow Dispatch

For more control over the publishing process:

1. **Update Version**: Update `pubspec.yaml` as above

2. **Go to Actions**:
   - Navigate to **Actions** → **Publish to pub.dev**
   - Click **Run workflow**

3. **Configure Options**:
   - **Use workflow from**: `main`
   - **Version**: `1.2.3`
   - **Dry run**: ✅ (recommended for first test)

4. **Test First**: Always run a dry run first to validate everything

5. **Publish**: Run again with dry run unchecked

## Validation Process

The automated pipeline includes comprehensive validation:

### Pre-publish Checks
- ✅ Package structure validation
- ✅ Version conflict detection
- ✅ pubspec.yaml validation
- ✅ PANA score validation

### Full Test Suite
- ✅ Dart analysis and formatting
- ✅ Unit tests with coverage
- ✅ Android build and integration tests
- ✅ iOS build and integration tests
- ✅ Security scanning
- ✅ Documentation checks

### Publication Process
- ✅ Credential validation
- ✅ Dry run testing
- ✅ Actual publication
- ✅ Post-publish verification

## Monitoring and Verification

### GitHub Actions
Monitor the workflow progress:
- Go to **Actions** tab
- Watch the "Publish to pub.dev" workflow
- Check individual job logs for details

### pub.dev Verification
After successful publication:
- Visit: https://pub.dev/packages/flutter_audio_output
- Verify new version is listed
- Check package score and documentation

### Publication Status
The workflow provides detailed summaries:
- Pre-publish validation results
- Test execution status
- Publication confirmation
- Post-publish verification

## Troubleshooting

### Common Issues

#### 1. Version Already Exists
```
❌ Version 1.2.3 already exists on pub.dev
```
**Solution**: Increment version number in `pubspec.yaml`

#### 2. Package Validation Failed
```
❌ Package validation failed with issues
```
**Solution**: 
- Run `flutter pub publish --dry-run` locally
- Fix reported issues
- Re-run workflow

#### 3. Test Failures
```
❌ Android/iOS tests failed
```
**Solution**:
- Check test logs in Actions tab
- Fix failing tests
- Re-run workflow

#### 4. Credential Issues
```
❌ Invalid pub.dev credentials
```
**Solution**:
- Regenerate credentials: `flutter pub token`
- Update `PUB_DEV_CREDENTIALS` secret

#### 5. Permission Denied
```
❌ You do not have permission to publish
```
**Solution**:
- Verify you're a publisher on pub.dev
- Check package ownership
- Contact existing publishers

### Debug Steps

1. **Check Workflow Logs**:
   ```
   Actions → Publish to pub.dev → [Latest run] → [Failed job]
   ```

2. **Test Locally**:
   ```bash
   flutter pub publish --dry-run
   dart pub global activate pana
   pana --no-warning
   ```

3. **Verify Credentials**:
   ```bash
   flutter pub token
   # Check if credentials work
   ```

4. **Check Package Status**:
   ```bash
   curl -s https://pub.dev/api/packages/flutter_audio_output
   ```

## Best Practices

### Version Management
- Use semantic versioning (major.minor.patch)
- Update CHANGELOG.md with each release
- Tag releases consistently

### Testing
- Always run dry run first
- Test locally before publishing
- Monitor CI/CD pipeline health

### Documentation
- Keep README.md updated
- Document breaking changes
- Update example app

### Security
- Rotate credentials periodically
- Use production environment protection
- Monitor security scan results

## Rollback Process

If you need to rollback a published version:

1. **Publish New Version**: pub.dev doesn't support unpublishing
2. **Deprecate Bad Version**: Mark version as deprecated
3. **Update Documentation**: Warn users about the problematic version

```bash
# Deprecate a version (not available via automated pipeline)
flutter pub global activate pub_api_client
# Use pub_api_client to deprecate version
```

## Support

For issues with the publishing process:

1. **Check GitHub Issues**: Search for similar problems
2. **Review Documentation**: Re-read this guide
3. **Contact Maintainers**: Create GitHub issue with details
4. **pub.dev Support**: For pub.dev-specific issues

## Automated vs Manual

### Automated Publishing (Recommended)
- ✅ Comprehensive validation
- ✅ Consistent process
- ✅ Full test coverage
- ✅ Audit trail
- ✅ Rollback capability

### Manual Publishing
- ❌ No validation
- ❌ Human error prone
- ❌ Inconsistent process
- ❌ No test coverage

**Always use the automated pipeline for production releases.**
