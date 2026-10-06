#!/bin/sh
set -eu

usage() {
    printf '%s\n' 'Uso: sh scripts/install.sh [--prefix PASTA]' \
        'Instala em ~/.local por padrão. Compile antes com: mvn verify'
}

prefix="${HOME}/.local"
if [ "$#" -gt 0 ]; then
    case "$1" in
        --help|-h) usage; exit 0 ;;
        --prefix)
            if [ "$#" -ne 2 ] || [ -z "$2" ]; then usage >&2; exit 1; fi
            prefix=$2
            ;;
        *) usage >&2; exit 1 ;;
    esac
fi

project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
jar="$project_dir/target/java-cli-manager.jar"
if [ ! -f "$jar" ]; then
    printf '%s\n' 'JAR não encontrado. Execute mvn verify antes de instalar.' >&2
    exit 1
fi

mkdir -p "$prefix/bin" "$prefix/share/java-cli-manager"
cp "$jar" "$prefix/share/java-cli-manager/java-cli-manager.jar"
cat > "$prefix/bin/java-cli-manager" <<'LAUNCHER'
#!/bin/sh
set -eu
install_bin=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
exec java -jar "$install_bin/../share/java-cli-manager/java-cli-manager.jar" "$@"
LAUNCHER
chmod 755 "$prefix/bin/java-cli-manager"
printf 'Instalado em %s/bin/java-cli-manager\n' "$prefix"
printf 'Inclua %s/bin no PATH para usar o comando java-cli-manager.\n' "$prefix"
