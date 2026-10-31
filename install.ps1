# Installs or updates PAM: Progressive Adventure Mod (Windows).
#
#   irm https://raw.githubusercontent.com/joelharkes/pam/main/install.ps1 | iex
#
# Other mods folder (a server, a launcher instance): first run $env:PAM_MODS_DIR = "C:\path\to\mods"
#
# Downloads the newest GitHub release and the Fabric API version it was built with.
# Fabric Loader itself is not installed: https://fabricmc.net/use/

# Everything is in a function, called on the last line, so a cut-off download can't run half a script.
# It runs inside the player's own terminal through iex, so errors use throw, not exit.
function Install-Pam {
	$ErrorActionPreference = 'Stop'
	$ProgressPreference = 'SilentlyContinue' # the progress bar makes downloads very slow in Windows PowerShell
	[Net.ServicePointManager]::SecurityProtocol = [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12

	$repo = 'joelharkes/pam'

	if ($env:PAM_MODS_DIR) {
		$modsDir = $env:PAM_MODS_DIR
	} else {
		$gameDir = Join-Path $env:APPDATA '.minecraft'
		if (-not (Test-Path $gameDir)) {
			throw 'Minecraft folder not found. Start Minecraft once, or set PAM_MODS_DIR to your mods folder.'
		}
		$modsDir = Join-Path $gameDir 'mods'
	}
	New-Item -ItemType Directory -Force -Path $modsDir | Out-Null

	# Not /releases/latest: it skips pre-releases, and alpha versions are published as pre-releases.
	$releases = Invoke-RestMethod "https://api.github.com/repos/$repo/releases?per_page=10"
	$release = $null
	$asset = $null
	foreach ($candidate in $releases) {
		$asset = $candidate.assets | Where-Object { $_.name -match '^progressive_adventure-.*\.jar$' -and $_.name -notmatch '-sources\.jar$' } | Select-Object -First 1
		if ($asset) {
			$release = $candidate
			break
		}
	}
	if (-not $asset) {
		throw 'No PAM release found on GitHub.'
	}
	$tag = $release.tag_name

	$properties = (Invoke-WebRequest -UseBasicParsing "https://raw.githubusercontent.com/$repo/$tag/gradle.properties").Content
	$mcMatch = [regex]::Match($properties, '(?m)^minecraft_version=(\S+)')
	$apiMatch = [regex]::Match($properties, '(?m)^fabric_api_version=(\S+)')
	if (-not $mcMatch.Success -or -not $apiMatch.Success) {
		throw "Could not read the requirements of PAM $tag."
	}
	$mcVersion = $mcMatch.Groups[1].Value
	$apiVersion = $apiMatch.Groups[1].Value

	Write-Host "Mods folder: $modsDir"
	Write-Host "Latest PAM:  $tag (for Minecraft $mcVersion)"

	if (Test-Path -LiteralPath (Join-Path $modsDir $asset.name)) {
		Write-Host 'PAM is already up to date.'
	} else {
		$jar = Get-Jar $asset.browser_download_url $asset.name
		Get-ChildItem -Path $modsDir -Filter 'progressive_adventure-*.jar' | Remove-Item -Force
		Move-Item -LiteralPath $jar -Destination $modsDir
		Write-Host "Installed $($asset.name)"
	}

	# Any Fabric API for this Minecraft version will do. One for another version would stop the game
	# from starting, so turn it off (renamed, not deleted).
	$haveApi = $null
	foreach ($file in Get-ChildItem -Path $modsDir -Filter 'fabric-api-*.jar') {
		if ($file.Name.EndsWith("+$mcVersion.jar")) {
			$haveApi = $file.Name
		} else {
			Rename-Item -LiteralPath $file.FullName -NewName "$($file.Name).disabled" -Force
			Write-Host "Turned off $($file.Name) (wrong Minecraft version)"
		}
	}
	if ($haveApi) {
		Write-Host "Fabric API is already installed ($haveApi)."
	} else {
		$apiName = "fabric-api-$apiVersion.jar"
		$jar = Get-Jar "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/$apiVersion/$apiName" $apiName
		Move-Item -LiteralPath $jar -Destination $modsDir
		Write-Host "Installed $apiName"
	}

	# Only a game folder has versions/; a server or launcher instance manages the loader itself.
	$versionsDir = Join-Path (Split-Path $modsDir) 'versions'
	if ((Test-Path $versionsDir) -and -not (Get-ChildItem -Path $versionsDir -Filter "fabric-loader-*-$mcVersion")) {
		Write-Host ''
		Write-Host "One more step: Fabric Loader for Minecraft $mcVersion is not installed yet."
		Write-Host 'Get it from https://fabricmc.net/use/'
	}

	Write-Host ''
	Write-Host "Done! Start Minecraft with the Fabric Loader $mcVersion profile."
}

# Downloads to the temp folder so a failed download never ends up in the mods folder.
function Get-Jar($url, $name) {
	Write-Host "Downloading $name..."
	$dir = Join-Path ([IO.Path]::GetTempPath()) "pam-$([guid]::NewGuid())"
	New-Item -ItemType Directory -Path $dir | Out-Null
	$path = Join-Path $dir $name
	Invoke-WebRequest -UseBasicParsing -Uri $url -OutFile $path
	$stream = [IO.File]::OpenRead($path)
	$first = $stream.ReadByte()
	$second = $stream.ReadByte()
	$stream.Close()
	# Jars are zip files; anything else is an error page.
	if ($first -ne 0x50 -or $second -ne 0x4B) {
		Remove-Item -LiteralPath $path -Force
		throw "Download is not a jar: $url"
	}
	return $path
}

Install-Pam
