$url = "http://localhost:8081/api/v1/customers/6d618517-51f3-4cc7-8a80-32e974bd4935"
$results = @()

for ($i = 1; $i -le 105; $i++) {
    try {
        $response = Invoke-WebRequest -Uri $url -UseBasicParsing -ErrorAction Stop
        $results += $response.StatusCode
    }
    catch {
        if ($_.Exception.Response.StatusCode.value__ -eq 429) {
            $results += 429
        }
        else {
            $results += "Error: $($_.Exception.Message)"
        }
    }
}

Write-Host "`nTotal requests: $($results.Count)"
Write-Host "200 OK: $(($results | Where-Object { $_ -eq 200 }).Count)"
Write-Host "429 Too Many Requests: $(($results | Where-Object { $_ -eq 429 }).Count)"
Write-Host "`nLast 10 responses:"
$results | Select-Object -Last 10
