#!/bin/bash

# Flutter Audio Output CI/CD Setup Script
# This script helps configure the CI/CD pipeline for automated publishing

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}[INFO]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

print_step() {
    echo -e "${BLUE}[STEP]${NC} $1"
}

echo "🚀 Flutter Audio Output CI/CD Setup"
echo "========================================"

# Check if we're in the right directory
if [ ! -f "pubspec.yaml" ]; then
    print_error "pubspec.yaml not found. Please run this script from the project root directory."
    exit 1
fi

if [ ! -f ".github/workflows/ci.yml" ]; then
    print_error "CI workflow not found. Please ensure .github/workflows/ci.yml exists."
    exit 1
fi

print_status "Found project files ✓"

# Step 1: Check Flutter installation
print_step "1. Checking Flutter installation..."
if command -v flutter &> /dev/null; then
    FLUTTER_VERSION=$(flutter --version | head -n 1 | cut -d ' ' -f 2)
    print_status "Flutter $FLUTTER_VERSION installed ✓"
else
    print_error "Flutter is not installed or not in PATH"
    echo "Please install Flutter: https://flutter.dev/docs/get-started/install"
    exit 1
fi

# Step 2: Check pub.dev credentials
print_step "2. Checking pub.dev credentials..."
if [ -f "$HOME/.pub-cache/credentials.json" ]; then
    print_status "pub.dev credentials found ✓"
    echo "Credentials location: $HOME/.pub-cache/credentials.json"
else
    print_warning "pub.dev credentials not found"
    echo "Run the following command to generate credentials:"
    echo "  flutter pub token"
    echo ""
    read -p "Do you want to generate credentials now? (y/n): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        flutter pub token
        if [ -f "$HOME/.pub-cache/credentials.json" ]; then
            print_status "Credentials generated successfully ✓"
        else
            print_error "Failed to generate credentials"
            exit 1
        fi
    else
        print_warning "Skipping credential generation"
    fi
fi

# Step 3: Display credentials for GitHub Secrets
if [ -f "$HOME/.pub-cache/credentials.json" ]; then
    print_step "3. GitHub Secrets Configuration"
    echo "Add the following secret to your GitHub repository:"
    echo ""
    echo "Secret Name: PUB_DEV_CREDENTIALS"
    echo "Secret Value:"
    echo "----------------------------------------"
    cat "$HOME/.pub-cache/credentials.json"
    echo "----------------------------------------"
    echo ""
    echo "To add this secret:"
    echo "1. Go to your GitHub repository"
    echo "2. Navigate to Settings → Secrets and variables → Actions"
    echo "3. Click 'New repository secret'"
    echo "4. Name: PUB_DEV_CREDENTIALS"
    echo "5. Value: Copy the JSON content above"
    echo ""
    read -p "Press Enter when you've added the secret to GitHub..."
fi

# Step 4: Validate package
print_step "4. Validating package for pub.dev..."
print_status "Running pub.dev validation..."

# Get dependencies
flutter pub get

# Run dry run publish
if flutter pub publish --dry-run; then
    print_status "Package validation passed ✓"
else
    print_error "Package validation failed"
    echo "Please fix the issues above before publishing"
    exit 1
fi

# Step 5: Check package analysis
print_step "5. Running package analysis..."
if command -v dart &> /dev/null; then
    if dart pub global list | grep -q "pana"; then
        print_status "PANA already installed"
    else
        print_status "Installing PANA..."
        dart pub global activate pana
    fi
    
    print_status "Running PANA analysis..."
    if dart pub global run pana --no-warning; then
        print_status "PANA analysis passed ✓"
    else
        print_warning "PANA analysis has suggestions"
        echo "Review the suggestions above to improve package score"
    fi
else
    print_warning "Dart not found, skipping PANA analysis"
fi

# Step 6: Check current version
print_step "6. Checking current version..."
CURRENT_VERSION=$(grep "version:" pubspec.yaml | cut -d' ' -f2)
print_status "Current version: $CURRENT_VERSION"

# Check if version exists on pub.dev
PACKAGE_NAME=$(grep "name:" pubspec.yaml | cut -d' ' -f2)
print_status "Package name: $PACKAGE_NAME"

if curl -s "https://pub.dev/api/packages/$PACKAGE_NAME" | grep -q "name"; then
    print_status "Package exists on pub.dev"
    
    # Check if current version exists
    if curl -s "https://pub.dev/api/packages/$PACKAGE_NAME/versions/$CURRENT_VERSION" | grep -q "version"; then
        print_warning "Current version $CURRENT_VERSION already exists on pub.dev"
        echo "You'll need to increment the version before publishing"
    else
        print_status "Current version $CURRENT_VERSION is available for publishing ✓"
    fi
else
    print_status "This will be a new package on pub.dev"
fi

# Step 7: Test CI workflow
print_step "7. Testing CI workflow..."
if [ -d ".github/workflows" ]; then
    print_status "GitHub Actions workflows found ✓"
    
    # List workflow files
    echo "Available workflows:"
    ls -la .github/workflows/
    
    echo ""
    echo "To test the CI workflow:"
    echo "1. Commit and push your changes"
    echo "2. Create a pull request"
    echo "3. Watch the CI workflow run in GitHub Actions"
else
    print_error "GitHub Actions workflows not found"
    exit 1
fi

# Step 8: Final setup summary
print_step "8. Setup Summary"
echo "✅ Setup completed successfully!"
echo ""
echo "📋 Next Steps:"
echo "1. Add PUB_DEV_CREDENTIALS secret to GitHub"
echo "2. Create a production environment in GitHub (optional)"
echo "3. Test the CI workflow by creating a pull request"
echo "4. Publish using one of these methods:"
echo "   - Create a GitHub release (recommended)"
echo "   - Run the publish workflow manually"
echo ""
echo "📚 Documentation:"
echo "- CI/CD Documentation: .github/CI_CD_DOCUMENTATION.md"
echo "- Publishing Guide: PUBLISHING.md"
echo ""
echo "🎉 Your Flutter Audio Output plugin is ready for automated publishing!"

# Optional: Open documentation
read -p "Would you like to open the publishing documentation? (y/n): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    if command -v open &> /dev/null; then
        open PUBLISHING.md
    elif command -v xdg-open &> /dev/null; then
        xdg-open PUBLISHING.md
    else
        print_status "Please open PUBLISHING.md manually"
    fi
fi
