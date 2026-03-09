param(
    [ValidateSet('dry-run', 'execute')]
    [string]$Mode = 'dry-run',
    [string]$BaseUrl = 'http://127.0.0.1:8080',
    [string]$Username = 'admin',
    [string]$Password = 'admin123'
)

$ErrorActionPreference = 'Stop'

$targets = @('RBAC自检任务', 'E2E演示任务')

function Invoke-JsonRequest {
    param(
        [string]$Method,
        [string]$Url,
        [hashtable]$Headers = @{},
        [object]$Body = $null
    )

    $params = @{
        Method      = $Method
        Uri         = $Url
        Headers     = $Headers
        ContentType = 'application/json'
    }
    if ($null -ne $Body) {
        $params.Body = ($Body | ConvertTo-Json -Depth 8)
    }
    Invoke-RestMethod @params
}

Write-Host "模式: $Mode"
Write-Host "服务: $BaseUrl"
Write-Host "目标标题: $($targets -join ' / ')"

$login = Invoke-JsonRequest -Method 'POST' -Url "$BaseUrl/api/auth/login" -Body @{
    username = $Username
    password = $Password
}

if ($login.code -ne 0 -or [string]::IsNullOrWhiteSpace($login.data.token)) {
    throw "登录失败，无法获取 token"
}

$headers = @{
    Authorization = "Bearer $($login.data.token)"
}

$tasksResp = Invoke-JsonRequest -Method 'GET' -Url "$BaseUrl/api/tasks" -Headers $headers
if ($tasksResp.code -ne 0) {
    throw "读取任务列表失败: $($tasksResp.message)"
}

$matched = @($tasksResp.data | Where-Object { $_.title -in $targets } | Sort-Object id)

if ($matched.Count -eq 0) {
    Write-Host '未找到需要清理的自检任务。'
    exit 0
}

Write-Host "命中的任务:"
$matched | ForEach-Object {
    Write-Host ("- id={0} title={1} status={2}" -f $_.id, $_.title, $_.status)
}

if ($Mode -eq 'dry-run') {
    Write-Host "dry-run 结束，共命中 $($matched.Count) 个任务。"
    exit 0
}

$deleted = New-Object System.Collections.Generic.List[string]
foreach ($task in $matched) {
    try {
        $resp = Invoke-JsonRequest -Method 'DELETE' -Url "$BaseUrl/api/tasks/$($task.id)" -Headers $headers
        if ($resp.code -eq 0) {
            $deleted.Add(("id={0} title={1}" -f $task.id, $task.title))
            Write-Host ("已删除: id={0} title={1}" -f $task.id, $task.title)
        } else {
            Write-Warning ("删除失败: id={0} title={1} message={2}" -f $task.id, $task.title, $resp.message)
        }
    } catch {
        Write-Warning ("删除异常: id={0} title={1} error={2}" -f $task.id, $task.title, $_.Exception.Message)
    }
}

Write-Host "执行完成，共删除 $($deleted.Count) 个任务。"
