<#
.SYNOPSIS
    按车型名称从 360 图片搜索抓取真实车型照片，保存到本地供开发环境使用。

.DESCRIPTION
    照片仅用于本机个人学习/演示，版权归原图站所有，默认不提交到 Git 仓库
    （照片目录已在 .gitignore 中忽略）。脚本会同时生成 photos-credits.json，
    记录每张照片的来源站点与原始链接。

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts/fetch-car-photos.ps1
    powershell -ExecutionPolicy Bypass -File scripts/fetch-car-photos.ps1 -Force -Width 800
#>
[CmdletBinding()]
param(
    [string]$DataFile = 'src/main/resources/data/cars.json',
    [string]$OutDir = 'src/main/resources/static/img/cars/photos',
    [int]$Width = 640,
    [int]$PerPage = 24,
    [int]$DelayMs = 200,
    [switch]$Force
)

$ErrorActionPreference = 'Stop'

$userAgent = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36'
$referer = 'https://image.so.com/'

function Test-Jpeg([string]$Path) {
    if (-not (Test-Path $Path)) { return $false }
    $stream = [System.IO.File]::OpenRead($Path)
    try {
        if ($stream.Length -lt 5120) { return $false }
        $b1 = $stream.ReadByte()
        $b2 = $stream.ReadByte()
        return ($b1 -eq 0xFF -and $b2 -eq 0xD8)
    } finally {
        $stream.Dispose()
    }
}

function Get-ResizedUrl([string]$ThumbUrl, [int]$Width) {
    if ($ThumbUrl -match '^(https?://[^/]+)/(.+)$') {
        return "$($Matches[1])/sdr/_${Width}_/$($Matches[2])"
    }
    return $ThumbUrl
}

function Get-SearchCandidates([string]$Name, [string]$Brand, [int]$Count) {
    # 依次尝试多种查询词，避免个别车型命中率过低
    $queries = @($Name, ($Name -replace '\s+', ''), "$Name 实车", "$Brand 汽车")
    $payload = $null
    foreach ($query in ($queries | Select-Object -Unique)) {
        $encoded = [Uri]::EscapeDataString($query)
        $url = "https://image.so.com/j?q=$encoded&pn=0&rn=$Count"
        $raw = & curl.exe -s -L --max-time 25 -A $userAgent -e $referer $url
        if (-not $raw) { continue }
        try {
            $candidate = $raw | ConvertFrom-Json
        } catch {
            continue
        }
        if ($candidate.list -and $candidate.list.Count -gt 0) {
            $payload = $candidate
            break
        }
        Start-Sleep -Milliseconds 150
    }
    if (-not $payload -or -not $payload.list) { return @() }

    # 从车型名里取出用于比对的关键词（去空格后的型号片段）
    $modelPart = $Name
    if ($Brand -and $Name.StartsWith($Brand)) {
        $modelPart = $Name.Substring($Brand.Length).Trim()
    }
    $tokens = [regex]::Matches($modelPart, '[A-Za-z0-9][A-Za-z0-9\-\.]{1,}') |
        ForEach-Object { $_.Value.ToUpperInvariant() } | Select-Object -Unique

    $scored = @()
    # 标题里出现这些词通常是内饰/细节/资讯图，作为封面观感较差，降权处理
    $interiorPattern = '内饰|座椅|中控|后排|前排|后备箱|仪表|方向盘|车门|细节|图解|拆解|配置表|报价|价格|优惠|销量|试驾|评测'
    # 出现这些词说明是外观图，优先使用
    $exteriorPattern = '外观|前脸|侧面|车身|车尾|官图|实拍|图赏|上市|首发|亮相|申报图|车展'

    foreach ($item in $payload.list) {
        if (-not $item.thumb -or -not $item.imgkey) { continue }
        $w = [int]$item.thumbWidth
        $h = [int]$item.thumbHeight
        if ($w -le 0 -or $h -le 0) { continue }
        if ($w -lt $h) { continue }              # 只要横向照片
        $ratio = [double]$w / [double]$h
        if ($ratio -lt 1.05 -or $ratio -gt 2.6) { continue }

        $title = "$($item.title) $($item.litetitle)"
        $score = 0
        if ($Brand -and $title -match [regex]::Escape($Brand)) { $score += 2 }
        $hitTokens = 0
        foreach ($token in $tokens) {
            if ($title.ToUpperInvariant().Contains($token)) { $hitTokens++ }
        }
        $score += [Math]::Min($hitTokens, 3)
        if ($title -match $exteriorPattern) { $score += 3 }
        if ($title -match $interiorPattern) { $score -= 6 }
        if ($score -eq 0) { $score = 1 }         # 名称为中文时标题往往不含型号串，保留为候选
        $scored += [pscustomobject]@{
            Item  = $item
            Score = $score
            Width = $w
            Ratio = [Math]::Round($ratio, 2)
        }
    }
    return ($scored | Sort-Object -Property @{Expression = 'Score'; Descending = $true},
        @{Expression = 'Width'; Descending = $true} | Select-Object -First 6)
}

$cars = Get-Content -Raw -Encoding UTF8 $DataFile | ConvertFrom-Json
New-Item -ItemType Directory -Force -Path $OutDir | Out-Null

$creditsPath = Join-Path $OutDir 'photos-credits.json'
# 读取已有的来源清单，按 id 合并，避免每次运行都覆盖历史记录
$credits = @()
if (Test-Path $creditsPath) {
    try {
        $creditSource = Get-Content -Raw -Encoding UTF8 $creditsPath | ConvertFrom-Json
        $credits = @($creditSource)
    } catch {
        $credits = @()
    }
}
$downloaded = 0
$skipped = 0
$failed = @()

Write-Host "共 $($cars.Count) 款车型，开始抓取照片（宽度 $Width px）…" -ForegroundColor Cyan

foreach ($car in $cars) {
    $target = Join-Path $OutDir "$($car.id).jpg"
    if ((Test-Path $target) -and -not $Force) {
        $skipped++
        continue
    }

    $candidates = Get-SearchCandidates -Name $car.name -Brand $car.brand -Count $PerPage
    $saved = $false
    $temp = "$target.new"
    foreach ($candidate in $candidates) {
        # 先试指定宽度的缩放图，失败再退回原始缩略图地址
        $urls = @($(Get-ResizedUrl -ThumbUrl $candidate.Item.thumb -Width $Width), $candidate.Item.thumb) |
            Select-Object -Unique
        $savedUrl = $null
        foreach ($url in $urls) {
            & curl.exe -s -L --max-time 30 -A $userAgent -e $referer -o $temp $url
            if (Test-Jpeg $temp) {
                $savedUrl = $url
                break
            }
            Remove-Item $temp -ErrorAction SilentlyContinue
        }
        if ($savedUrl) {
            # 下载成功才替换已有文件，避免 -Force 重跑时把好图删掉
            Move-Item -Force $temp $target
            $entry = [pscustomobject]@{
                id       = $car.id
                name     = $car.name
                title    = $candidate.Item.title
                site     = $candidate.Item.site
                pageUrl  = $candidate.Item.link
                imageUrl = $savedUrl
            }
            $credits = @($credits | Where-Object { $_.id -ne $car.id }) + $entry
            $saved = $true
            $downloaded++
            Write-Host ("  [{0,2}] {1}  <- {2}" -f $car.id, $car.name, $candidate.Item.site)
            break
        }
    }

    if (-not $saved) {
        $failed += $car.name
        Write-Host ("  [{0,2}] {1}  未找到可用图片" -f $car.id, $car.name) -ForegroundColor Yellow
    }
    if ($DelayMs -gt 0) { Start-Sleep -Milliseconds $DelayMs }
}

$credits = @($credits | Sort-Object id)
$credits | ConvertTo-Json -Depth 4 | Set-Content -Encoding UTF8 $creditsPath

Write-Host ''
Write-Host "完成：新增 $downloaded 张，跳过 $skipped 张，失败 $($failed.Count) 款" -ForegroundColor Green
Write-Host "照片目录：$OutDir"
Write-Host "来源清单：$creditsPath"
if ($failed.Count -gt 0) {
    Write-Host "未获取到照片的车型：" -ForegroundColor Yellow
    $failed | ForEach-Object { Write-Host "  - $_" }
}
