function Get-ProjectFingerprint {
    param([Parameter(Mandatory=$true)][string]$ProjectRoot)
    $project = (Resolve-Path -LiteralPath $ProjectRoot).Path
    $files = @()
    foreach ($directory in @('src','sql','lib')) {
        $files += @(Get-ChildItem -LiteralPath (Join-Path $project $directory) -Recurse -File)
    }
    foreach ($file in @('build.bat','build.ps1','run.bat','run.ps1','demo.bat','verify_tests.bat','verify_tests.ps1',
            'java-tools.ps1','verification-tools.ps1','resources\db.properties',
            'zhixingyuebo-java.iml','.idea\modules.xml','.idea\runConfigurations\MainApp.xml')) {
        $files += Get-Item -LiteralPath (Join-Path $project $file)
    }
    $rows = foreach ($file in ($files | Sort-Object FullName)) {
        $relative = $file.FullName.Substring($project.Length + 1).Replace('\','/')
        (Get-FileHash -LiteralPath $file.FullName -Algorithm SHA256).Hash + ' ' + $relative
    }
    $sha = [System.Security.Cryptography.SHA256]::Create()
    try {
        return [BitConverter]::ToString($sha.ComputeHash([Text.Encoding]::UTF8.GetBytes($rows -join [char]10))).Replace('-','')
    } finally { $sha.Dispose() }
}

function Get-AcceptanceSummary {
    param([Parameter(Mandatory=$true)][string]$Path)
    $summary = @{}
    foreach ($line in (Get-Content -LiteralPath $Path)) {
        if ($line -match '^([a-z.]+)=(.*)$') { $summary[$Matches[1]] = $Matches[2] }
    }
    return $summary
}
