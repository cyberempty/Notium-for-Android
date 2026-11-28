# ======================================================================================
#  NOTIUM BUILD SYSTEM
#  Builds "src" and produces Notium-android-APK\Notium.apk
#  Portable: everything needed is downloaded into build-tools\tools the first time and
#  reused afterwards (later builds also work without internet while that folder is kept).
# ======================================================================================
$ErrorActionPreference = 'Stop'
$ProgressPreference    = 'SilentlyContinue'
try { [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12 } catch { }
try { [Console]::OutputEncoding = New-Object System.Text.UTF8Encoding($false) } catch { }

$APP_VERSION = '1.0.0'
$btools = Split-Path -Parent $MyInvocation.MyCommand.Path
$main    = Split-Path -Parent $btools
$pdir    = Join-Path $main 'src'
$tools   = Join-Path $btools 'tools'
$jdkDir  = Join-Path $tools 'jdk'
$sdk     = Join-Path $tools 'android-sdk'
$signDir = Join-Path $btools 'signing'
$keystore = Join-Path $signDir 'notium.keystore'
$logPath = Join-Path $btools 'build.log'
$outDir  = Join-Path (Split-Path -Parent $main) 'Notium-android-APK'
$marker  = Join-Path $tools '.pronto-release'
$estFile = Join-Path $tools '.task-estimate'

# ----------------------------------------------------------------------- user interface
$ESC = [char]27
$R = "$ESC[0m"; $DIM = "$ESC[90m"; $BOLD = "$ESC[1m"; $WHITE = "$ESC[97m"
$script:UseVT = $false
$script:T0 = Get-Date
$script:Percent = 0.0
$script:PhaseNo = 0
$script:PhaseNames = @('Checking environment', 'JDK 17 (Temurin)', 'Android command-line tools', 'Android SDK 35 and build-tools', 'Compiling and optimizing (R8)', 'Finalizing')
$ACC = "$ESC[96m"; $FILLC = [string][char]0x2588; $EMPTYC = [string][char]0x2591
if (Test-Path $marker) { $subtitle = 'components already present: no network needed' } else { $subtitle = 'first build: internet connection required' }
$script:PhaseW    = @(3, 10, 4, 15, 60, 8)
$script:PhaseBase = @(0, 3, 13, 17, 32, 92)
$script:Status = ''
$script:Mode = 'Checking'
$script:Spin = 0
$script:LastTick = [DateTime]::MinValue
$script:LogW = $null
$script:HeaderRows = 8
$script:Errors = New-Object System.Collections.ArrayList
$script:Warnings = 0
$script:TaskCount = 0
$script:TaskEstimate = 60
$script:WentWrong = New-Object System.Collections.ArrayList
$script:CaptureWrong = $false
$script:LastLogged = ''

function Enable-VT {
    try {
        Add-Type -Namespace NotiumBuild -Name Con -ErrorAction Stop -MemberDefinition @'
[DllImport("kernel32.dll")] public static extern System.IntPtr GetStdHandle(int n);
[DllImport("kernel32.dll")] public static extern bool GetConsoleMode(System.IntPtr h, out uint m);
[DllImport("kernel32.dll")] public static extern bool SetConsoleMode(System.IntPtr h, uint m);
'@
        $h = [NotiumBuild.Con]::GetStdHandle(-11)
        $m = [uint32]0
        if (-not [NotiumBuild.Con]::GetConsoleMode($h, [ref]$m)) { return $false }
        if (($m -band 4) -ne 0) { return $true }
        return [NotiumBuild.Con]::SetConsoleMode($h, ($m -bor 4))
    } catch { return $false }
}

function Get-W { try { $w = [Console]::WindowWidth } catch { $w = 100 }; if ($w -lt 64) { return 64 }; return ($w - 1) }
function Get-H { try { $h = [Console]::WindowHeight } catch { $h = 30 }; if ($h -lt 18) { return 18 }; return $h }
function Get-Elapsed { $t = (Get-Date) - $script:T0; return ('{0:00}:{1:00}' -f [int][Math]::Floor($t.TotalMinutes), $t.Seconds) }
function Fit([string]$s, [int]$w) { if ($s.Length -gt $w) { return $s.Substring(0, [Math]::Max(0, $w - 1)) + '…' }; return $s }

function Put-Row([int]$row, [string]$text) {
    [Console]::Write(("{0}7{0}[{1};1H{0}[2K{2}{0}8" -f $ESC, $row, $text))
}

function Draw-Header {
    if (-not $script:UseVT) { return }
    $w = Get-W
    $pct = [int][Math]::Floor($script:Percent)
    $barW = [Math]::Max(20, $w - 30)
    $filled = [int][Math]::Floor($barW * $script:Percent / 100)
    $bar = ($FILLC * $filled) + ($EMPTYC * ($barW - $filled))
    $spin = @('|', '/', '-', '\')[$script:Spin % 4]
    if ($script:PhaseNo -gt 0) { $phase = ('{0}/{1}   {2}' -f $script:PhaseNo, 6, $script:PhaseNames[$script:PhaseNo - 1]) } else { $phase = 'Starting' }
    $title = Fit (' NOTIUM  BUILD SYSTEM   ' + $subtitle) ($w - 10)
    $ver = ('v' + $APP_VERSION + ' ')
    Put-Row 1 ($BOLD + $WHITE + $title.PadRight($w - $ver.Length) + $ver + $R)
    Put-Row 2 ($DIM + ('─' * $w) + $R)
    Put-Row 3 (' ' + $DIM + 'Project     ' + $R + 'src')
    Put-Row 4 (' ' + $DIM + 'Mode        ' + $R + (Fit $script:Mode ($w - 16)))
    Put-Row 5 (' ' + $DIM + 'Phase       ' + $R + (Fit $phase ($w - 16)))
    Put-Row 6 (' ' + $DIM + 'Status      ' + $R + (Fit $script:Status ($w - 16)))
    Put-Row 7 (' ' + $ACC + $bar + $R + ('  {0,3}%   ' -f $pct) + $DIM + (Get-Elapsed) + $R + '   ' + $spin)
    Put-Row 8 ($DIM + ('─' * $w) + $R)
}

function Start-UI {
    $script:UseVT = Enable-VT
    if ($script:UseVT) {
        $h = Get-H
        [Console]::Write("$ESC[2J$ESC[H")
        Draw-Header
        [Console]::Write(("{0}[{1};{2}r{0}[{1};1H" -f $ESC, ($script:HeaderRows + 1), $h))
    } else {
        Write-Host ''
        Write-Host ' NOTIUM  BUILD SYSTEM' -ForegroundColor White
        Write-Host (' ' + $subtitle) -ForegroundColor Cyan
        Write-Host (' ' + ('-' * 60)) -ForegroundColor DarkGray
    }
}

function Stop-UI {
    if ($script:UseVT) {
        [Console]::Write(("{0}[r{0}[{1};1H" -f $ESC, (Get-H)))
        [Console]::WriteLine('')
    }
}

function Tick {
    $now = [DateTime]::UtcNow
    if (($now - $script:LastTick).TotalMilliseconds -ge 250) {
        $script:LastTick = $now
        $script:Spin++
        Draw-Header
    }
}

function Log([string]$level, [string]$msg) {
    $stamp = Get-Elapsed
    if ($script:LogW) { $script:LogW.WriteLine(('{0}  {1}  {2}' -f $stamp, $level.PadRight(5), $msg)) }
    if ($script:UseVT) {
        switch ($level) { 'OK' { $col = "$ESC[32m" } 'WARN' { $col = "$ESC[33m" } 'ERROR' { $col = "$ESC[31m" } 'STEP' { $col = "$ESC[36m" } default { $col = "$ESC[37m" } }
        [Console]::WriteLine($DIM + $stamp + $R + '  ' + $col + $level.PadRight(5) + $R + '  ' + $msg)
    } else {
        switch ($level) { 'OK' { $c = 'Green' } 'WARN' { $c = 'Yellow' } 'ERROR' { $c = 'Red' } 'STEP' { $c = 'Cyan' } default { $c = 'Gray' } }
        Write-Host ('[{0,3}%] {1}  {2}  {3}' -f [int]$script:Percent, $stamp, $level.PadRight(5), $msg) -ForegroundColor $c
    }
    Tick
}

function Set-Phase([int]$n) {
    $script:PhaseNo = $n
    if ($script:Percent -lt $script:PhaseBase[$n - 1]) { $script:Percent = [double]$script:PhaseBase[$n - 1] }
    $script:Status = ''
    Log 'STEP' ('Phase {0}/6  {1}' -f $n, $script:PhaseNames[$n - 1])
    Draw-Header
}
function Set-Frac([double]$f) {
    if ($f -gt 1) { $f = 1 }
    if ($f -lt 0) { $f = 0 }
    $p = $script:PhaseBase[$script:PhaseNo - 1] + $script:PhaseW[$script:PhaseNo - 1] * $f
    if ($p -gt $script:Percent) { $script:Percent = $p }
    Tick
}
function Set-Status([string]$s) { $script:Status = $s; Tick }
function End-Phase { Set-Frac 1; $script:LastTick = [DateTime]::MinValue; Tick }

# ----------------------------------------------------------------------- utilities
function Format-Size([double]$b) {
    if ($b -ge 1GB) { return ('{0:N2} GB' -f ($b / 1GB)) }
    return ('{0:N1} MB' -f ($b / 1MB))
}

function Get-File([string]$url, [string]$out, [string]$label) {
    for ($try = 1; $try -le 3; $try++) {
        $fs = $null; $in = $null; $res = $null
        try {
            $req = [System.Net.HttpWebRequest]::Create($url)
            $req.UserAgent = 'NotiumBuildSystem'
            $req.AllowAutoRedirect = $true
            $req.Timeout = 60000
            $req.ReadWriteTimeout = 60000
            $res = $req.GetResponse()
            $total = $res.ContentLength
            $in = $res.GetResponseStream()
            $fs = [System.IO.File]::Create($out)
            $buf = New-Object byte[] 262144
            $done = 0
            while (($n = $in.Read($buf, 0, $buf.Length)) -gt 0) {
                $fs.Write($buf, 0, $n)
                $done += $n
                if ($total -gt 0) {
                    Set-Frac ($done / $total)
                    Set-Status ('{0}  {1} / {2}  ({3}%)' -f $label, (Format-Size $done), (Format-Size $total), [int](100 * $done / $total))
                } else {
                    Set-Status ('{0}  {1}' -f $label, (Format-Size $done))
                }
            }
            $fs.Close(); $in.Close(); $res.Close()
            return
        } catch {
            if ($fs) { $fs.Close() }; if ($in) { $in.Close() }; if ($res) { $res.Close() }
            if ($try -eq 3) { throw ('Download failed: {0}  ({1})' -f $url, $_.Exception.Message) }
            Log 'WARN' ('Download interrupted, retrying {0}/3' -f ($try + 1))
            Start-Sleep -Seconds 3
        }
    }
}

function Expand-Zip([string]$zip, [string]$dest) {
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    if (Test-Path $dest) { Remove-Item $dest -Recurse -Force }
    [System.IO.Compression.ZipFile]::ExtractToDirectory($zip, $dest)
}

# Runs a program and passes every output line (stdout and stderr) to $onLine, updating the interface.
function Invoke-Streamed([string]$file, [string[]]$arguments, [string]$workDir, [scriptblock]$onLine, [string]$stdin) {
    $psi = New-Object System.Diagnostics.ProcessStartInfo
    $psi.FileName = $file
    $psi.Arguments = (($arguments | ForEach-Object { if ($_ -match '\s') { '"' + $_ + '"' } else { $_ } }) -join ' ')
    $psi.WorkingDirectory = $workDir
    $psi.UseShellExecute = $false
    $psi.RedirectStandardOutput = $true
    $psi.RedirectStandardError = $true
    $psi.RedirectStandardInput = $true
    $psi.CreateNoWindow = $true
    $p = [System.Diagnostics.Process]::Start($psi)
    # Input must be written without a BOM (the default encoding would corrupt the first character).
    if ($stdin) {
        try {
            $w = New-Object System.IO.StreamWriter($p.StandardInput.BaseStream, (New-Object System.Text.UTF8Encoding($false)))
            $w.Write($stdin); $w.Flush(); $w.Close()
        } catch { }
    }
    try { $p.StandardInput.Close() } catch { }
    $so = $p.StandardOutput.ReadLineAsync()
    $se = $p.StandardError.ReadLineAsync()
    while ($true) {
        $moved = $false
        if ($null -ne $so -and $so.IsCompleted) {
            $line = $so.Result
            if ($null -eq $line) { $so = $null } else { $null = & $onLine $line; $so = $p.StandardOutput.ReadLineAsync() }
            $moved = $true
        }
        if ($null -ne $se -and $se.IsCompleted) {
            $line = $se.Result
            if ($null -eq $line) { $se = $null } else { $null = & $onLine $line; $se = $p.StandardError.ReadLineAsync() }
            $moved = $true
        }
        if ($null -eq $so -and $null -eq $se) { break }
        if (-not $moved) { Start-Sleep -Milliseconds 80; Tick }
    }
    $p.WaitForExit()
    return $p.ExitCode
}

function Short-Path([string]$s) {
    return ($s -replace 'file:///[A-Za-z]:/.*?/src/', '' -replace '^file:///', '')
}

# ----------------------------------------------------------------------- execution
$exitCode = 0
$failed = $false
try {
    $script:LogW = New-Object System.IO.StreamWriter($logPath, $false, (New-Object System.Text.UTF8Encoding($false)))
    $script:LogW.AutoFlush = $true
    Start-UI

    # ---- Phase 1: environment
    Set-Phase 1
    if (-not (Test-Path (Join-Path $pdir 'gradlew.bat'))) { throw ('Folder "src" is missing or incomplete: ' + $pdir) }
    if ($main -match '\\Temp\\' -or $main -match '\.zip') { Log 'WARN' 'The project seems to be running from inside an archive: extract the zip into a normal folder first.' }
    New-Item -ItemType Directory -Force -Path $tools, $signDir | Out-Null
    $offline = Test-Path $marker
    if ($offline) { $script:Mode = 'Components already present (no download needed)' } else { $script:Mode = 'First build: downloading components' }
    if (Test-Path $estFile) { $v = 0; if ([int]::TryParse((Get-Content $estFile -Raw).Trim(), [ref]$v) -and $v -gt 20) { $script:TaskEstimate = $v } }
    Log 'INFO' ('Project       {0}' -f $pdir)
    Log 'INFO' ('Resources     {0}' -f $tools)
    Log 'INFO' ('Mode          {0}' -f $script:Mode)
    End-Phase

    # ---- Phase 2: JDK
    Set-Phase 2
    if (-not (Test-Path (Join-Path $jdkDir 'bin\java.exe'))) {
        $zip = Join-Path $tools 'jdk.zip'
        Log 'INFO' 'Download Eclipse Temurin JDK 17 (x64)'
        Get-File 'https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse' $zip 'Temurin JDK 17'
        Set-Status 'Extracting JDK archive...'
        $tmp = Join-Path $tools 'jdk_tmp'
        Expand-Zip $zip $tmp
        $inner = Get-ChildItem $tmp -Directory | Select-Object -First 1
        if (Test-Path $jdkDir) { Remove-Item $jdkDir -Recurse -Force }
        Move-Item $inner.FullName $jdkDir
        Remove-Item $tmp -Recurse -Force; Remove-Item $zip -Force
        Log 'OK' 'JDK 17 installed'
    } else { Log 'OK' 'JDK 17 already present' }
    $env:JAVA_HOME = $jdkDir
    $env:Path = "$jdkDir\bin;$env:Path"
    End-Phase

    # ---- Phase 3: command-line tools
    Set-Phase 3
    $sdkmanager = Join-Path $sdk 'cmdline-tools\latest\bin\sdkmanager.bat'
    if (-not (Test-Path $sdkmanager)) {
        $zip = Join-Path $tools 'cmdline-tools.zip'
        Log 'INFO' 'Download Android command-line tools 11076708'
        Get-File 'https://dl.google.com/android/repository/commandlinetools-win-11076708_latest.zip' $zip 'Android command-line tools'
        Set-Status 'Extracting archive...'
        $tmp = Join-Path $tools 'cmd_tmp'
        Expand-Zip $zip $tmp
        New-Item -ItemType Directory -Force -Path (Join-Path $sdk 'cmdline-tools') | Out-Null
        Move-Item (Join-Path $tmp 'cmdline-tools') (Join-Path $sdk 'cmdline-tools\latest')
        Remove-Item $tmp -Recurse -Force; Remove-Item $zip -Force
        Log 'OK' 'Command-line tools installed'
    } else { Log 'OK' 'Command-line tools already present' }
    $env:ANDROID_HOME = $sdk
    $env:ANDROID_SDK_ROOT = $sdk
    End-Phase

    # ---- Phase 4: SDK
    Set-Phase 4
    if (-not (Test-Path (Join-Path $sdk 'platforms\android-35')) -or -not (Test-Path (Join-Path $sdk 'build-tools\34.0.0'))) {
        Log 'INFO' 'Accepting Android SDK licenses'
        $licDir = Join-Path $sdk 'licenses'
        New-Item -ItemType Directory -Force -Path $licDir | Out-Null
        $enc = New-Object System.Text.UTF8Encoding($false)
        [System.IO.File]::WriteAllText((Join-Path $licDir 'android-sdk-license'), "`n24333f8a63b6825ea9c5514f83c2829b004d1fee`n84831b9409646a918e30573bab4c9c91346d8abd`nd56f5187479451eabf01fb78af6dfcb131a6481e`n", $enc)
        [System.IO.File]::WriteAllText((Join-Path $licDir 'android-sdk-preview-license'), "`n84831b9409646a918e30573bab4c9c91346d8abd`n504667f4c0de7af1a06de9f4b1727b84351f2910`n", $enc)
        Log 'INFO' 'Installing platforms;android-35 and build-tools;34.0.0'
        $onSdk = {
            param($l)
            if ($script:LogW) { $script:LogW.WriteLine($l) }
            if ($l -match '(\d+)%\s*(.*)$') { Set-Frac ([int]$Matches[1] / 100.0); Set-Status ('SDK  {0}%  {1}' -f $Matches[1], $Matches[2].Trim()) }
            elseif ($l -match '^\s*\[?\s*(Warning|Error)' ) { Log 'WARN' $l.Trim() }
        }
        $code = Invoke-Streamed $sdkmanager @("--sdk_root=$sdk", 'platforms;android-35', 'build-tools;34.0.0') $tools $onSdk ''
        if ($code -ne 0) { throw ('SDK installation failed (code {0})' -f $code) }
        if (-not (Test-Path (Join-Path $sdk 'platforms\android-35')) -or -not (Test-Path (Join-Path $sdk 'build-tools\34.0.0'))) {
            throw 'SDK components not installed: license not accepted or incomplete download. Try again.'
        }
        Log 'OK' 'Android SDK installed'
    } else { Log 'OK' 'Android SDK already present' }
    ('sdk.dir=' + ($sdk -replace '\\', '/')) | Out-File (Join-Path $pdir 'local.properties') -Encoding ascii
    End-Phase

    # ---- local signing (stable key: app updates remain installable)
    if (-not (Test-Path $keystore)) {
        Log 'INFO' 'Creating the local signing key (one time only)'
        $keytool = Join-Path $jdkDir 'bin\keytool.exe'
        $code = Invoke-Streamed $keytool @('-genkeypair', '-keystore', $keystore, '-storetype', 'PKCS12', '-alias', 'notium', '-keyalg', 'RSA', '-keysize', '2048', '-validity', '10000', '-storepass', 'notium-local', '-keypass', 'notium-local', '-dname', 'CN=Notium, O=Notium, C=IT') $tools { param($l) $null = $l } ''
        if ($code -ne 0 -or -not (Test-Path $keystore)) { throw 'Signing key creation failed' }
    }
    $env:NOTIUM_KEYSTORE = $keystore
    $env:NOTIUM_STORE_PASSWORD = 'notium-local'
    $env:NOTIUM_KEY_ALIAS = 'notium'
    $env:NOTIUM_KEY_PASSWORD = 'notium-local'

    # ---- Phase 5: Gradle
    Set-Phase 5
    $env:GRADLE_USER_HOME = Join-Path $tools 'gradle-home'
    $gradle = Join-Path $pdir 'gradlew.bat'
    $major = '(compile.*Kotlin|minify.*R8|dexBuilder|mergeDex|package|assemble|validateSigning|processReleaseResources|extractProguard|optimizeResources)'
    $onGradle = {
        param($l)
        if ($script:LogW) { $script:LogW.WriteLine($l) }
        if ($l -match '^> Task (\S+)') {
            $script:TaskCount++
            Set-Frac ([Math]::Min(0.97, $script:TaskCount / [double]$script:TaskEstimate))
            $t = $Matches[1]
            Set-Status ('Gradle  {0}   ({1} tasks)' -f $t, $script:TaskCount)
            if ($t -match $major -and $t -ne $script:LastLogged) { $script:LastLogged = $t; Log 'INFO' ('Task  ' + $t) }
        }
        elseif ($l -match '^e: (.+)$') { $null = $script:Errors.Add((Short-Path $Matches[1])) }
        elseif ($l -match '^w: ') { $script:Warnings++ }
        elseif ($l -match '^\* What went wrong:') { $script:CaptureWrong = $true }
        elseif ($script:CaptureWrong) {
            if (($l -match '^\* Try:') -or (($l -match '^\s*$') -and ($script:WentWrong.Count -gt 0))) { $script:CaptureWrong = $false }
            elseif ($script:WentWrong.Count -lt 8) { $null = $script:WentWrong.Add($l.Trim()) }
        }
    }
    $gargs = @('assembleRelease', '--parallel', '--build-cache', '--console=plain')
    if ($offline) { $gargs += '--offline'; Log 'INFO' 'Gradle without network access (--offline)' } else { Log 'INFO' 'Gradle: downloading Gradle and libraries when missing' }
    Log 'INFO' 'Starting release build (optimized with R8)'
    $code = Invoke-Streamed $gradle $gargs $pdir $onGradle ''
    if ($code -ne 0 -and $offline -and $script:Errors.Count -eq 0) {
        Log 'WARN' 'Components missing from the local cache: retrying with network access'
        $script:TaskCount = 0; $script:WentWrong.Clear()
        $code = Invoke-Streamed $gradle @('assembleRelease', '--parallel', '--build-cache', '--console=plain') $pdir $onGradle ''
    }
    if ($code -ne 0) { $failed = $true; throw ('Build failed (code {0})' -f $code) }
    Log 'OK' ('Build finished  ({0} tasks, {1} compiler warnings)' -f $script:TaskCount, $script:Warnings)
    'ok' | Out-File $marker -Encoding ascii
    if ($script:TaskCount -gt 20) { [string]$script:TaskCount | Out-File $estFile -Encoding ascii }
    End-Phase

    # ---- Phase 6: finalization
    Set-Phase 6
    $apk = Get-ChildItem (Join-Path $pdir 'app\build\outputs\apk\release') -Filter *.apk -ErrorAction SilentlyContinue | Where-Object { $_.Name -notmatch 'unaligned' } | Select-Object -First 1
    if (-not $apk) { throw 'APK not found in the Gradle output folder' }
    New-Item -ItemType Directory -Force -Path $outDir | Out-Null
    $final = Join-Path $outDir 'Notium.apk'
    Copy-Item $apk.FullName $final -Force
    if (-not (Test-Path $final)) { throw ('Copying the APK failed: ' + $final) }
    $sha = (Get-FileHash $final -Algorithm SHA256).Hash.ToLower()
    $size = (Get-Item $final).Length
    Set-Frac 0.5
    Log 'INFO' 'Stopping the Gradle daemon and removing temporary files'
    $null = Invoke-Streamed $gradle @('--stop') $pdir { param($l) $null = $l } ''
    foreach ($p in @('app\build', 'build', '.gradle', '.kotlin', 'local.properties')) {
        Remove-Item (Join-Path $pdir $p) -Recurse -Force -ErrorAction SilentlyContinue
    }
    End-Phase
    $script:Percent = 100.0
    $script:Status = 'Done'
    $script:Mode = 'Build complete'
    Draw-Header
    Log 'OK' 'Build completed successfully'

    Stop-UI
    $line = '─' * 64
    Write-Host ''
    Write-Host (' ' + $line) -ForegroundColor DarkGray
    Write-Host '  BUILD COMPLETE' -ForegroundColor Green
    Write-Host (' ' + $line) -ForegroundColor DarkGray
    Write-Host ('  Output       ' + $final)
    Write-Host ('  Size         ' + (Format-Size $size))
    Write-Host ('  SHA-256      ' + $sha)
    Write-Host ('  Version      ' + $APP_VERSION)
    Write-Host ('  Duration     ' + (Get-Elapsed))
    Write-Host '  Signing      local key in build-tools\signing'
    Write-Host (' ' + $line) -ForegroundColor DarkGray
    Write-Host ''
    Write-Host '  Copy Notium.apk to your phone and open it. If a version signed with a different key is already installed,'
    Write-Host '  uninstall it first (Android does not allow overwriting an app signed with a different key).'
    Write-Host ''
    $ans = Read-Host '  Delete the downloaded components in build-tools\tools (about 3 GB)? They are reused by the next builds  [y/N]'
    if ($ans -match '^[yY]') { Remove-Item $tools -Recurse -Force -ErrorAction SilentlyContinue; Write-Host '  build-tools\tools deleted.' }
    try { Start-Process explorer.exe ('/select,"' + $final + '"') } catch { }
}
catch {
    $msg = $_.Exception.Message
    Log 'ERROR' $msg
    Stop-UI
    $line = '─' * 64
    Write-Host ''
    Write-Host (' ' + $line) -ForegroundColor DarkGray
    Write-Host ('  BUILD FAILED   (phase {0}/6 - {1})' -f $script:PhaseNo, $(if ($script:PhaseNo -gt 0) { $script:PhaseNames[$script:PhaseNo - 1] } else { 'startup' })) -ForegroundColor Red
    Write-Host (' ' + $line) -ForegroundColor DarkGray
    Write-Host ('  {0}' -f $msg) -ForegroundColor Red
    if ($script:Errors.Count -gt 0) {
        Write-Host ''
        Write-Host ('  Compilation errors ({0}):' -f $script:Errors.Count) -ForegroundColor Yellow
        foreach ($e in $script:Errors) { Write-Host ('    ' + $e) }
    } elseif ($script:WentWrong.Count -gt 0) {
        Write-Host ''
        Write-Host '  Gradle details:' -ForegroundColor Yellow
        foreach ($e in $script:WentWrong) { Write-Host ('    ' + $e) }
    }
    Write-Host ''
    Write-Host ('  Full log: ' + $logPath)
    Write-Host ''
    $exitCode = 1
}
finally {
    if ($script:LogW) { try { $script:LogW.Close() } catch { } }
}
exit $exitCode
