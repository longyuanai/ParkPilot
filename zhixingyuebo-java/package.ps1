param([switch]$NoPause)
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'verification-tools.ps1')
Add-Type -AssemblyName System.IO.Compression.FileSystem
$project = (Resolve-Path -LiteralPath $PSScriptRoot).Path
$workspace = (Get-Item -LiteralPath $project).Parent.FullName
$acceptance = Join-Path $project 'build\acceptance'
$report = Join-Path $project 'docs\智行悦泊智慧停车系统实训设计与实现报告.docx'
$summary = Get-AcceptanceSummary (Join-Path $acceptance 'summary.properties')
$verifiedHash = (Get-Content -LiteralPath (Join-Path $acceptance 'source.sha256') -Raw).Trim()
if ($summary['test.passed'] -ne 'true' -or $summary['evidence.schema'] -ne $summary['test.schema'] -or $summary['production.unchanged'] -ne 'true' -or
    $summary['test.schema.deleted'] -ne 'true' -or $verifiedHash -ne (Get-ProjectFingerprint $project)) {
    throw '请先运行 verify_tests.ps1 -ExportEvidence，并确认本版代码验收通过。'
}
$shots = @(Get-ChildItem -LiteralPath (Join-Path $acceptance 'screenshots') -Filter '*.png' -File)
if ($shots.Count -lt 6 -or $shots.Count -gt 9) { throw '提交报告要求 6 到 9 张真实验收截图。' }

function Read-ZipText($Entry) {
    $reader = [IO.StreamReader]::new($Entry.Open())
    try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
}

$doc = [IO.Compression.ZipFile]::OpenRead($report)
try {
    $core = Read-ZipText ($doc.GetEntry('docProps/core.xml'))
    if (!$core.Contains($verifiedHash) -or !$core.Contains($summary['test.schema'])) {
        throw '报告需要与本轮验收结果对应，请重新生成。'
    }
    $docImages = @($doc.Entries | Where-Object { $_.FullName -match '^word/media/[^/]+\.png$' })
    $docHashes = foreach ($entry in $docImages) {
        $stream = $entry.Open()
        try { (Get-FileHash -InputStream $stream -Algorithm SHA256).Hash } finally { $stream.Dispose() }
    }
    $shotHashes = @($shots | Get-FileHash -Algorithm SHA256 | Select-Object -ExpandProperty Hash)
    if ($docImages.Count -ne $shots.Count -or (Compare-Object $shotHashes @($docHashes))) {
        throw '报告中的截图与本轮验收导出不一致。'
    }
} finally { $doc.Dispose() }

$target = Join-Path $workspace 'delivery'
$stage = Join-Path $project ('build\package_' + [Guid]::NewGuid().ToString('N'))
$source = Join-Path $stage '1.源代码工程'
$sql = Join-Path $stage '2.数据库脚本'
$docs = Join-Path $stage '3.课程设计报告'
$resources = Join-Path $source 'resources'
$lib = Join-Path $source 'lib'
$runConfigs = Join-Path $source '.idea\runConfigurations'
$evidence = Join-Path $docs '运行截图'
foreach ($directory in @($target,$source,$sql,$docs,$resources,$lib,$runConfigs,$evidence)) {
    New-Item -ItemType Directory -Path $directory -Force | Out-Null
}
Copy-Item -LiteralPath (Join-Path $project 'src') -Destination $source -Recurse
Copy-Item -LiteralPath (Join-Path $project 'sql') -Destination $source -Recurse
Copy-Item -LiteralPath (Join-Path $project 'resources\db.properties') -Destination $resources
Copy-Item -LiteralPath (Join-Path $project 'lib\mysql-connector-j-8.1.0.jar') -Destination $lib
foreach ($name in @('README.md','build.bat','build.ps1','run.bat','run.ps1','verify_tests.bat',
        'verify_tests.ps1','java-tools.ps1','verification-tools.ps1','zhixingyuebo-java.iml',
        '.idea\modules.xml','.idea\runConfigurations\MainApp.xml')) {
    Copy-Item -LiteralPath (Join-Path $project $name) -Destination (Join-Path $source $name)
}
foreach ($file in (Get-ChildItem -LiteralPath (Join-Path $project 'sql') -Filter '*.sql' -File)) {
    Copy-Item -LiteralPath $file.FullName -Destination $sql
}
Copy-Item -LiteralPath $report -Destination $docs
Copy-Item -LiteralPath (Join-Path $project '期末答辩与核心源码质询通关指南.md') -Destination $docs
Copy-Item -LiteralPath (Join-Path $acceptance 'results.tsv') -Destination $docs
foreach ($shot in $shots) { Copy-Item -LiteralPath $shot.FullName -Destination $evidence }
$proof = @(
    ('Source SHA256 ' + $verifiedHash)
    ('Passed cases ' + $summary['test.count'])
    ('Isolated test schema ' + $summary['test.schema'])
    ('Original database fingerprint match ' + $summary['production.unchanged'])
    ('Isolated schema removed ' + $summary['test.schema.deleted'])
)
[IO.File]::WriteAllLines((Join-Path $docs '验收摘要.txt'), $proof, [Text.Encoding]::UTF8)
if ((Get-ProjectFingerprint $source) -ne $verifiedHash) { throw '归档源码与已验收版本不一致。' }

$candidate = Join-Path $target ('.candidate_' + [Guid]::NewGuid().ToString('N') + '.zip')
$writer = [IO.Compression.ZipFile]::Open($candidate, [IO.Compression.ZipArchiveMode]::Create)
try {
    foreach ($file in (Get-ChildItem -LiteralPath $stage -Recurse -File -Force)) {
        $name = $file.FullName.Substring($stage.Length + 1).Replace('\','/')
        [IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
            $writer, $file.FullName, $name, [IO.Compression.CompressionLevel]::Optimal) | Out-Null
    }
} finally { $writer.Dispose() }
$zip = [IO.Compression.ZipFile]::OpenRead($candidate)
try {
    $names = @($zip.Entries | ForEach-Object { $_.FullName })
    foreach ($required in @('1.源代码工程/run.ps1','1.源代码工程/verify_tests.ps1',
            '2.数据库脚本/01_create_zhixingyuebo_database_and_tables.sql',
            '2.数据库脚本/02_upgrade_existing_database.sql',
            ('3.课程设计报告/' + [IO.Path]::GetFileName($report)))) {
        if ($required -notin $names) { throw ('归档缺少 ' + $required) }
    }
    if ($names -match '(?i)(^|/)(\.git|\.backups|bin|build|db\.local\.properties)(/|$)') {
        throw '归档含本机配置或构建目录。'
    }
    foreach ($entry in ($zip.Entries | Where-Object { $_.Name -like '*.properties' })) {
        $config = Read-ZipText $entry
        if ($config -match '(?im)^[ \t]*jdbc\.(username|password)[ \t]*[:=][ \t]*\S+' -or
            $config -match '(?im)^[ \t]*jdbc\.url[ \t]*[:=][^\r\n]*(?:[?&;](?:user|username|password)=|://[^/\s]*@)') {
            throw '归档配置包含实际凭据。'
        }
    }
} finally { $zip.Dispose() }

$final = Join-Path $target '黄家威.zip'
if (Test-Path -LiteralPath $final) {
    $backupRoot = Join-Path $workspace '.backups'
    New-Item -ItemType Directory -Path $backupRoot -Force | Out-Null
    $saved = Join-Path $backupRoot ('previous_delivery_' + [Guid]::NewGuid().ToString('N') + '.zip')
    [IO.File]::Replace($candidate, $final, $saved)
} else {
    Move-Item -LiteralPath $candidate -Destination $final
}
Write-Output ('PACKAGE=' + $final)
Write-Output ('SHA256=' + (Get-FileHash -LiteralPath $final -Algorithm SHA256).Hash)
if (-not $NoPause) { Read-Host '按回车结束' | Out-Null }
