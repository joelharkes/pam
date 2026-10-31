#!/usr/bin/env bash
# Installs or updates PAM: Progressive Adventure Mod (Linux and macOS).
#
#   curl -fsSL https://raw.githubusercontent.com/joelharkes/pam/main/install.sh | bash
#   wget -qO- https://raw.githubusercontent.com/joelharkes/pam/main/install.sh | bash
#
# Other mods folder (a server, a launcher instance): add "-s -- /path/to/mods" after "bash",
# or set PAM_MODS_DIR.
#
# Downloads the newest GitHub release and the Fabric API version it was built with.
# Fabric Loader itself is not installed: https://fabricmc.net/use/

# Everything is in main, called on the last line, so a cut-off download can't run half a script.
main() {
	set -eu

	local repo="joelharkes/pam"
	local mods_dir releases jar_url jar_name tag properties mc_version api_version api_name
	mods_dir="$(find_mods_dir "${1:-${PAM_MODS_DIR:-}}")"
	# Not local: the trap runs after main has returned.
	tmp_dir="$(mktemp -d)"
	trap 'rm -rf "$tmp_dir"' EXIT

	# Not /releases/latest: it skips pre-releases, and alpha versions are published as pre-releases.
	releases="$(fetch "https://api.github.com/repos/$repo/releases?per_page=10")" \
		|| fail "Could not reach GitHub. Check your internet, or try again in an hour (GitHub limits requests)."
	jar_url="$(printf '%s' "$releases" \
		| grep -o "https://github.com/$repo/releases/download/[^\"]*/progressive_adventure-[^\"]*\.jar" \
		| grep -v -e '-sources\.jar$' | head -n 1 || true)"
	[ -n "$jar_url" ] || fail "No PAM release found on GitHub."
	jar_name="${jar_url##*/}"
	tag="${jar_url%/*}"
	tag="${tag##*/}"

	properties="$(fetch "https://raw.githubusercontent.com/$repo/$tag/gradle.properties")" \
		|| fail "Could not read the requirements of PAM $tag."
	mc_version="$(printf '%s\n' "$properties" | sed -n 's/^minecraft_version=//p')"
	api_version="$(printf '%s\n' "$properties" | sed -n 's/^fabric_api_version=//p')"
	[ -n "$mc_version" ] && [ -n "$api_version" ] || fail "Could not read the requirements of PAM $tag."

	echo "Mods folder: $mods_dir"
	echo "Latest PAM:  $tag (for Minecraft $mc_version)"

	if [ -f "$mods_dir/$jar_name" ]; then
		echo "PAM is already up to date."
	else
		download "$jar_url" "$tmp_dir/$jar_name"
		rm -f "$mods_dir"/progressive_adventure-*.jar
		mv "$tmp_dir/$jar_name" "$mods_dir/"
		echo "Installed $jar_name"
	fi

	# Any Fabric API for this Minecraft version will do. One for another version would stop the game
	# from starting, so turn it off (renamed, not deleted).
	local file have_api=""
	for file in "$mods_dir"/fabric-api-*.jar; do
		[ -e "$file" ] || continue
		case "$file" in
			*"+$mc_version.jar") have_api="$file" ;;
			*)
				mv "$file" "$file.disabled"
				echo "Turned off ${file##*/} (wrong Minecraft version)"
				;;
		esac
	done
	if [ -n "$have_api" ]; then
		echo "Fabric API is already installed (${have_api##*/})."
	else
		api_name="fabric-api-$api_version.jar"
		download "https://maven.fabricmc.net/net/fabricmc/fabric-api/fabric-api/$api_version/$api_name" "$tmp_dir/$api_name"
		mv "$tmp_dir/$api_name" "$mods_dir/"
		echo "Installed $api_name"
	fi

	# Only a game folder has versions/; a server or launcher instance manages the loader itself.
	local game_dir="${mods_dir%/mods}"
	if [ -d "$game_dir/versions" ] && ! ls -d "$game_dir/versions/fabric-loader-"*"-$mc_version" >/dev/null 2>&1; then
		echo
		echo "One more step: Fabric Loader for Minecraft $mc_version is not installed yet."
		echo "Get it from https://fabricmc.net/use/"
	fi

	echo
	echo "Done! Start Minecraft with the Fabric Loader $mc_version profile."
}

find_mods_dir() {
	if [ -n "$1" ]; then
		mkdir -p "$1"
		printf '%s\n' "${1%/}"
		return
	fi
	local game_dir
	for game_dir in \
		"$HOME/.minecraft" \
		"$HOME/Library/Application Support/minecraft" \
		"$HOME/.var/app/com.mojang.Minecraft/.minecraft"; do
		if [ -d "$game_dir" ]; then
			mkdir -p "$game_dir/mods"
			printf '%s\n' "$game_dir/mods"
			return
		fi
	done
	fail "Minecraft folder not found. Start Minecraft once, or pass your mods folder: ... | bash -s -- /path/to/mods"
}

fetch() {
	if command -v curl >/dev/null 2>&1; then
		curl -fsSL "$1"
	elif command -v wget >/dev/null 2>&1; then
		wget -qO- "$1"
	else
		fail "This script needs curl or wget."
	fi
}

download() {
	echo "Downloading ${2##*/}..."
	fetch "$1" >"$2" || fail "Download failed: $1"
	# Jars are zip files; anything else is an error page.
	[ "$(head -c 2 "$2")" = "PK" ] || fail "Download is not a jar: $1"
}

fail() {
	echo "Error: $1" >&2
	exit 1
}

main "$@"
