$ErrorActionPreference = 'Stop'
Set-Location (Join-Path $PSScriptRoot '..')
if (-not (Test-Path '.env')) { throw 'Missing .env: copy .env.example to .env and configure it.' }
foreach ($line in Get-Content '.env') {
    if ($line -match '^\s*(#|$)') { continue }
    if ($line -notmatch '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') { throw 'Invalid .env assignment' }
    $key = $Matches[1]
    $value = $Matches[2]
    if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
        $value = $value.Substring(1, $value.Length - 2)
    }
    [Environment]::SetEnvironmentVariable($key, $value, 'Process')
}
# Core Tools needs JAVA_HOME even when Maven can find java on PATH.
if (-not $env:JAVA_HOME) {
    $javaCommand = Get-Command java -ErrorAction Stop
    $settings = & $javaCommand.Source -XshowSettings:properties -version 2>&1 | Out-String
    if ($LASTEXITCODE -ne 0 -or $settings -notmatch '(?m)^\s*java.home = (.+)$') {
        throw 'Cannot resolve the JDK. Set JAVA_HOME to JDK 25 in .env.'
    }
    $env:JAVA_HOME = $Matches[1].Trim()
}
$java = Join-Path $env:JAVA_HOME 'bin/java.exe'
$javac = Join-Path $env:JAVA_HOME 'bin/javac.exe'
if (-not (Test-Path $java) -or -not (Test-Path $javac)) {
    throw 'JAVA_HOME must point to a JDK directory containing bin/java.exe and bin/javac.exe.'
}
$settings = & $java -XshowSettings:properties -version 2>&1 | Out-String
if ($LASTEXITCODE -ne 0 -or $settings -notmatch '(?m)^\s*java.specification.version = 25\s*$') {
    throw 'Local Azure runtime requires JDK 25. Correct JAVA_HOME in .env.'
}
$env:PATH = (Join-Path $env:JAVA_HOME 'bin') + [IO.Path]::PathSeparator + $env:PATH
Write-Host "Using JDK 25: $env:JAVA_HOME"
if (-not (Get-Command func -ErrorAction SilentlyContinue)) { throw 'Azure Functions Core Tools 4.x (func) is required.' }
if ($env:AzureWebJobsStorage -eq 'UseDevelopmentStorage=true') {
    Write-Host 'Local storage requires Azurite running on ports 10000-10002 (see README).'
}
if (-not (Test-Path 'local.settings.json')) { Copy-Item 'local.settings.json.example' 'local.settings.json' }
& ./mvnw.cmd clean package azure-functions:run @args
exit $LASTEXITCODE
