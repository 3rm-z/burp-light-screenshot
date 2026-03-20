param(
    [Parameter(Mandatory = $true)]
    [string] $ImagePath
)
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Windows.Forms
Add-Type -AssemblyName System.Drawing
if (-not (Test-Path -LiteralPath $ImagePath)) {
    Write-Error "File not found: $ImagePath"
    exit 2
}
$img = [System.Drawing.Image]::FromFile($ImagePath)
try {
    [System.Windows.Forms.Clipboard]::SetImage($img)
    exit 0
}
finally {
    $img.Dispose()
}
