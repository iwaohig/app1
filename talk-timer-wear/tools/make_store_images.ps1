# Usage: run with -OutDir fastlane/metadata/android/ja-JP/images (from talk-timer-wear) to redraw the store images.
param([string]$OutDir)
Add-Type -AssemblyName System.Drawing
$ErrorActionPreference = 'Stop'
New-Item -ItemType Directory -Force $OutDir | Out-Null

function C([string]$hex) { [System.Drawing.ColorTranslator]::FromHtml($hex) }
$bg     = C '#15181D'
$track  = C '#262B33'
$green  = C '#4CD17A'
$white  = [System.Drawing.Color]::White
$grey   = C '#A6ABB3'
$dim    = C '#8A8F98'

function New-Canvas($w, $h) {
    $bmp = New-Object System.Drawing.Bitmap($w, $h, [System.Drawing.Imaging.PixelFormat]::Format32bppArgb)
    $g = [System.Drawing.Graphics]::FromImage($bmp)
    $g.SmoothingMode = 'AntiAlias'
    $g.TextRenderingHint = 'AntiAliasGridFit'
    $g.InterpolationMode = 'HighQualityBicubic'
    $g.PixelOffsetMode = 'HighQuality'
    return @($bmp, $g)
}

# Ring: two thirds left, from 12 o'clock counter-clockwise (same as ic_launcher.xml).
function Draw-Ring($g, [float]$cx, [float]$cy, [float]$r, [float]$stroke) {
    $rect = New-Object System.Drawing.RectangleF(($cx - $r), ($cy - $r), (2 * $r), (2 * $r))
    $pt = New-Object System.Drawing.Pen($track, $stroke)
    $g.DrawEllipse($pt, $rect)
    $pg = New-Object System.Drawing.Pen($green, $stroke)
    $pg.StartCap = 'Round'; $pg.EndCap = 'Round'
    $g.DrawArc($pg, $rect, 30, 240)
    $pt.Dispose(); $pg.Dispose()
}

# ---------- Icon 512 x 512 (full bleed; Play applies its own mask) ----------
$c = New-Canvas 512 512; $bmp = $c[0]; $g = $c[1]
$g.Clear($bg)
$s = 512 / 48.0
Draw-Ring $g 256 256 (16 * $s) (4 * $s)
$b = New-Object System.Drawing.SolidBrush($white)
$g.FillRectangle($b, [float](22.5 * $s), [float](14 * $s), [float](3 * $s), [float](11 * $s))
$g.FillRectangle($b, [float](22.5 * $s), [float](23 * $s), [float](9 * $s), [float](3 * $s))
$b.Dispose(); $g.Dispose()
$bmp.Save((Join-Path $OutDir 'icon.png'), [System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()

# ---------- Feature graphic 1024 x 500 ----------
$c = New-Canvas 1024 500; $bmp = $c[0]; $g = $c[1]
$g.Clear($bg)
# Watch face mock-up on the left
$cx = 260; $cy = 250; $faceR = 172
$face = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::Black)
$g.FillEllipse($face, ($cx - $faceR), ($cy - $faceR), (2 * $faceR), (2 * $faceR)); $face.Dispose()
Draw-Ring $g $cx $cy 156 15
$fmt = New-Object System.Drawing.StringFormat; $fmt.Alignment = 'Center'; $fmt.LineAlignment = 'Center'
$jp = 'Yu Gothic UI'
$fLabel = New-Object System.Drawing.Font($jp, 26, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$fTime  = New-Object System.Drawing.Font('Segoe UI', 86, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$fSmall = New-Object System.Drawing.Font($jp, 22, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$g.DrawString('残り', $fLabel, (New-Object System.Drawing.SolidBrush($dim)), $cx, ($cy - 70), $fmt)
$g.DrawString('19:53', $fTime, (New-Object System.Drawing.SolidBrush($white)), $cx, ($cy + 2), $fmt)
$g.DrawString('現在 14:12 → 終了 14:32', $fSmall, (New-Object System.Drawing.SolidBrush($grey)), $cx, ($cy + 70), $fmt)

# Text on the right
$left = New-Object System.Drawing.StringFormat; $left.Alignment = 'Near'; $left.LineAlignment = 'Near'
$fTitle = New-Object System.Drawing.Font($jp, 64, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$fSub   = New-Object System.Drawing.Font($jp, 34, [System.Drawing.FontStyle]::Regular, [System.Drawing.GraphicsUnit]::Pixel)
$fTag   = New-Object System.Drawing.Font($jp, 24, [System.Drawing.FontStyle]::Bold, [System.Drawing.GraphicsUnit]::Pixel)
$x = 490
$g.DrawString('プレゼンタイマー', $fTitle, (New-Object System.Drawing.SolidBrush($white)), $x, 140, $left)
$g.DrawString('残り時間を、手首の振動で。', $fSub, (New-Object System.Drawing.SolidBrush($grey)), $x, 232, $left)
$g.DrawString('Wear OS ・ 時計だけで動く発表用タイマー', $fTag, (New-Object System.Drawing.SolidBrush($green)), $x, 300, $left)
$g.Dispose()
$bmp.Save((Join-Path $OutDir 'featureGraphic.png'), [System.Drawing.Imaging.ImageFormat]::Png); $bmp.Dispose()

Get-ChildItem $OutDir -Filter *.png | ForEach-Object {
    $i = [System.Drawing.Image]::FromFile($_.FullName); "{0} {1}x{2} {3} bytes" -f $_.Name, $i.Width, $i.Height, $_.Length; $i.Dispose()
}
