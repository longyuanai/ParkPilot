function Get-JavaTools {
    $compiler = $null
    if ($env:JAVA_HOME -and (Test-Path -LiteralPath (Join-Path $env:JAVA_HOME 'bin\javac.exe'))) {
        $compiler = Join-Path $env:JAVA_HOME 'bin\javac.exe'
    } else {
        $available = @(Get-Command javac.exe -All -ErrorAction SilentlyContinue)
        $preferred = $available | Where-Object { $_.Source -notmatch '\\javapath\\' } | Select-Object -First 1
        if (-not $preferred) { $preferred = $available | Select-Object -First 1 }
        if ($preferred) { $compiler = $preferred.Source }
    }
    if (-not $compiler) { throw 'Install JDK 8 or later, or set JAVA_HOME.' }
    $runtime = Join-Path (Split-Path -Parent $compiler) 'java.exe'
    return @{ Compiler = $compiler; Runtime = $runtime }
}
