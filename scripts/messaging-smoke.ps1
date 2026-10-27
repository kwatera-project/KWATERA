param(
  [string]$Gateway = 'http://localhost:8090',
  [string]$ConversationId = $env:CONVERSATION_ID
)
$ErrorActionPreference = 'Stop'
if (!$env:GUEST_TOKEN -or !$env:OWNER_TOKEN -or !$ConversationId) {
  throw 'Set GUEST_TOKEN, OWNER_TOKEN and CONVERSATION_ID environment variables.'
}
$timeout = [System.Threading.CancellationTokenSource]::new([TimeSpan]::FromSeconds(30))
function Send-Frame($Socket, [string]$Frame) {
  $bytes = [Text.Encoding]::UTF8.GetBytes($Frame + [char]0)
  $Socket.SendAsync([ArraySegment[byte]]::new($bytes), [System.Net.WebSockets.WebSocketMessageType]::Text, $true, $timeout.Token).GetAwaiter().GetResult()
}
function Receive-Frame($Socket) {
  $buffer = [byte[]]::new(32768)
  $stream = [IO.MemoryStream]::new()
  try {
    do {
      $result = $Socket.ReceiveAsync([ArraySegment[byte]]::new($buffer), $timeout.Token).GetAwaiter().GetResult()
      if ($result.MessageType -eq [System.Net.WebSockets.WebSocketMessageType]::Close) { throw 'WebSocket closed' }
      $stream.Write($buffer, 0, $result.Count)
    } until ($result.EndOfMessage)
    $frame = [Text.Encoding]::UTF8.GetString($stream.ToArray()).TrimStart("`n", "`r")
    if ($frame.StartsWith('ERROR')) { throw "STOMP rejected request: $frame" }
    return $frame
  } finally { $stream.Dispose() }
}
function Open-Participant([string]$Token) {
  $socket = [System.Net.WebSockets.ClientWebSocket]::new()
  $socket.Options.SetRequestHeader('Origin', 'http://localhost:5173')
  $uri = $Gateway -replace '^http', 'ws'
  $socket.ConnectAsync([uri]"$uri/ws/conversations", $timeout.Token).GetAwaiter().GetResult()
  Send-Frame $socket "CONNECT`naccept-version:1.2`nhost:localhost`nAuthorization:Bearer $Token`n`n"
  if (!(Receive-Frame $socket).StartsWith('CONNECTED')) { throw 'Expected CONNECTED' }
  Send-Frame $socket "SUBSCRIBE`nid:conversation`ndestination:/user/queue/conversations/$ConversationId/messages`nack:auto`nreceipt:subscribed`n`n"
  if (!(Receive-Frame $socket).StartsWith('RECEIPT')) { throw 'Expected subscription RECEIPT' }
  return $socket
}
$guest = $null
$owner = $null
try {
  $guest = Open-Participant $env:GUEST_TOKEN
  $owner = Open-Participant $env:OWNER_TOKEN
  $body = @{content="Gateway smoke test $([DateTimeOffset]::UtcNow.ToString('O'))"} | ConvertTo-Json -Compress
  Send-Frame $guest "SEND`ndestination:/app/conversations/$ConversationId/messages`ncontent-type:application/json`n`n$body"
  $guestFrame = Receive-Frame $guest
  $ownerFrame = Receive-Frame $owner
  $a = (($guestFrame -split "`n`n",2)[1].TrimEnd([char]0)) | ConvertFrom-Json
  $b = (($ownerFrame -split "`n`n",2)[1].TrimEnd([char]0)) | ConvertFrom-Json
  if (!$a.id -or $a.id -ne $b.id -or $a.content -ne $b.content) { throw 'Participant delivery mismatch' }
  $cursor = $null
  $found = $false
  do {
    $url = "$Gateway/api/v1/conversations/$ConversationId/messages?limit=100"
    if ($cursor) { $url += '&cursor=' + [uri]::EscapeDataString($cursor) }
    $page = Invoke-RestMethod $url -Headers @{Authorization="Bearer $env:GUEST_TOKEN"}
    if ($page.items.id -contains $a.id) { $found = $true; break }
    $cursor = $page.nextCursor
  } while ($page.hasMore)
  if (!$found) { throw 'Delivered message missing from REST history' }
  Write-Host "PASS: both participants received persisted message $($a.id); REST history contains it."
} finally {
  if ($guest) { $guest.Dispose() }
  if ($owner) { $owner.Dispose() }
  $timeout.Dispose()
}
