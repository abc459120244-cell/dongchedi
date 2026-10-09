<#
.SYNOPSIS
    校验本地仓库与远程仓库（origin）是否完全同步：提交哈希、文件清单、提交历史、分支结构。

.EXAMPLE
    powershell -ExecutionPolicy Bypass -File scripts/verify-sync.ps1
    powershell -ExecutionPolicy Bypass -File scripts/verify-sync.ps1 -Remote origin -Branch main
#>
[CmdletBinding()]
param(
    [string]$Remote = 'origin',
    [string]$Branch = 'main'
)

$ErrorActionPreference = 'Stop'
$script:failed = $false

function Write-Section([string]$Title) {
    Write-Host ''
    Write-Host ('=' * 68) -ForegroundColor DarkGray
    Write-Host " $Title" -ForegroundColor Cyan
    Write-Host ('=' * 68) -ForegroundColor DarkGray
}

function Write-Ok([string]$Message) {
    Write-Host "  [一致] $Message" -ForegroundColor Green
}

function Write-Bad([string]$Message) {
    Write-Host "  [不一致] $Message" -ForegroundColor Red
    $script:failed = $true
}

function Invoke-Git([string[]]$GitArgs) {
    $output = & git @GitArgs 2>&1
    if ($LASTEXITCODE -ne 0) {
        throw "git $($GitArgs -join ' ') 执行失败：$output"
    }
    return $output
}

Write-Section '0. 仓库信息'
if (-not (Test-Path '.git')) {
    Write-Host '  当前目录不是 Git 仓库，请在仓库根目录执行本脚本。' -ForegroundColor Red
    exit 2
}

try {
    $remoteUrl = (Invoke-Git @('remote', 'get-url', $Remote)) -join ''
} catch {
    Write-Host "  未找到名为 $Remote 的远程仓库，请先执行：" -ForegroundColor Red
    Write-Host "      git remote add $Remote <远程仓库地址>" -ForegroundColor Yellow
    exit 2
}
$localHead = (Invoke-Git @('rev-parse', 'HEAD')) -join ''
Write-Host "  远程仓库    : $Remote ($remoteUrl)"
Write-Host "  本地 HEAD   : $localHead"

Write-Section '1. 拉取远程最新引用'
Invoke-Git @('fetch', $Remote, '--prune') | Out-Null
$remoteRef = "$Remote/$Branch"
$remoteHead = (Invoke-Git @('rev-parse', $remoteRef)) -join ''
Write-Host "  远端 $remoteRef : $remoteHead"

Write-Section '2. 提交哈希比对'
if ($localHead -eq $remoteHead) {
    Write-Ok "本地 $Branch 与 $remoteRef 指向同一个提交"
} else {
    Write-Bad "本地 $localHead 与远端 $remoteHead 不一致"
}

Write-Section '3. 工作区状态'
$status = (Invoke-Git @('status', '--porcelain')) -join ''
if ([string]::IsNullOrWhiteSpace($status)) {
    Write-Ok '工作区干净，没有未提交的改动'
} else {
    Write-Bad '工作区存在未提交的改动：'
    Write-Host $status -ForegroundColor Yellow
}

Write-Section '4. 未推送 / 未拉取的提交'
$ahead = @(Invoke-Git @('log', '--oneline', "$remoteRef..HEAD"))
$behind = @(Invoke-Git @('log', '--oneline', "HEAD..$remoteRef"))
if ($ahead.Count -eq 0) { Write-Ok '没有未推送的提交' } else {
    Write-Bad "存在未推送的提交（$($ahead.Count) 个）："; $ahead | ForEach-Object { Write-Host "    $_" }
}
if ($behind.Count -eq 0) { Write-Ok '没有未拉取的提交' } else {
    Write-Bad "存在未拉取的提交（$($behind.Count) 个）："; $behind | ForEach-Object { Write-Host "    $_" }
}

Write-Section '5. 文件清单比对（是否有文件遗漏）'
$localFiles = @(Invoke-Git @('ls-tree', '-r', '--name-only', 'HEAD'))
$remoteFiles = @(Invoke-Git @('ls-tree', '-r', '--name-only', $remoteRef))
$added = @(Compare-Object $remoteFiles $localFiles | Where-Object { $_.SideIndicator -eq '=>' })
$missing = @(Compare-Object $remoteFiles $localFiles | Where-Object { $_.SideIndicator -eq '<=' })
if ($added.Count -eq 0 -and $missing.Count -eq 0) {
    Write-Ok "本地与远端文件清单完全一致，共 $($localFiles.Count) 个文件"
} else {
    if ($missing.Count -gt 0) {
        Write-Bad "本地缺少远程已有的 $($missing.Count) 个文件："
        $missing | ForEach-Object { Write-Host "    仅在远端存在: $($_.InputObject)" -ForegroundColor Yellow }
    }
    if ($added.Count -gt 0) {
        Write-Bad "远程缺少本地已有的 $($added.Count) 个文件（可能尚未推送）："
        $added | ForEach-Object { Write-Host "    仅在本地存在: $($_.InputObject)" -ForegroundColor Yellow }
    }
}
# 逐字节比对工作树内容（防止同名文件内容不同）
$treeDiff = (Invoke-Git @('diff', '--stat', 'HEAD', $remoteRef)) -join ''
if ([string]::IsNullOrWhiteSpace($treeDiff)) {
    Write-Ok '本地与远端文件内容完全相同（git diff 无差异）'
} else {
    Write-Bad '本地与远端存在内容差异：'
    Write-Host $treeDiff -ForegroundColor Yellow
}

Write-Section '6. 提交历史比对'
$localLog = @(Invoke-Git @('log', '--format=%H', 'HEAD'))
$remoteLog = @(Invoke-Git @('log', '--format=%H', $remoteRef))
if (($localLog -join ',') -eq ($remoteLog -join ',')) {
    Write-Ok "提交历史逐条一致，共 $($localLog.Count) 个提交"
} else {
    Write-Bad "提交历史不一致（本地 $($localLog.Count) 个，远端 $($remoteLog.Count) 个）"
}

Write-Section '7. 分支结构比对'
$localBranches = @(Invoke-Git @('for-each-ref', '--format=%(refname:short)', 'refs/heads'))
$remoteBranches = @(Invoke-Git @('for-each-ref', '--format=%(refname:short)', "refs/remotes/$Remote") |
    Where-Object { $_ -notmatch '/HEAD$' } |
    ForEach-Object { $_ -replace "^$Remote/", '' })
Write-Host "  本地分支: $($localBranches -join ', ')"
Write-Host "  远端分支: $($remoteBranches -join ', ')"
$branchDiff = @(Compare-Object $remoteBranches $localBranches)
if ($branchDiff.Count -eq 0) {
    Write-Ok '本地与远端分支结构一致'
} else {
    Write-Bad '分支结构存在差异：'
    $branchDiff | ForEach-Object {
        $side = if ($_.SideIndicator -eq '=>') { '仅存在于本地' } else { '仅存在于远端' }
        Write-Host "    ${side}: $($_.InputObject)" -ForegroundColor Yellow
    }
}

Write-Section '校验结论'
if ($script:failed) {
    Write-Host '  ✗ 本地与远程仓库尚未完全同步，请根据上面的提示处理。' -ForegroundColor Red
    exit 1
}

Write-Host '  ✓ 本地与远程仓库完全同步：文件、提交历史、分支结构均一致，无文件遗漏。' -ForegroundColor Green
exit 0
