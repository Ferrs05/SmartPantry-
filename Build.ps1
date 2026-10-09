param([string[]]$Tasks = @(':app:assembleDebug', ':domain:test', ':app:lintDebug'))
$ErrorActionPreference = 'Stop'
Push-Location $PSScriptRoot
try {
    $taskJdk = Get-ChildItem "$PSScriptRoot\.build-tools" -Directory -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($taskJdk) { $env:JAVA_HOME = $taskJdk.FullName }
    if (-not $env:JAVA_HOME) { throw 'Atur JAVA_HOME ke JDK 21, atau pilih Gradle JDK 21 di Android Studio.' }
    $env:GRADLE_USER_HOME = "$PSScriptRoot\.gradle-user-home"
    & "$PSScriptRoot\gradlew.bat" @Tasks --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw "Gradle gagal dengan exit code $LASTEXITCODE" }
} finally { Pop-Location }
