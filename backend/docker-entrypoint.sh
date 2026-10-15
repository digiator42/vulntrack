#!/bin/sh
# Vercel container entrypoint.
#
# Vercel/Neon provide the database as DATABASE_URL
# (postgresql://user:pass@host:port/db?params), but Spring Boot reads
# neither DATABASE_URL nor a libpq-style URL. Translate it to the
# SPRING_DATASOURCE_* properties the app expects, unless they were
# already set explicitly (then they win).
set -e

if [ -z "$SPRING_DATASOURCE_URL" ] && [ -n "$DATABASE_URL" ]; then
    raw="$DATABASE_URL"
    scheme="${raw%%://*}"

    case "$scheme" in
        postgres|postgresql)
            rest="${raw#*://}"

            # Split user:pass @ hostpart (credentials may contain '@').
            creds="${rest%@*}"
            hostpart="${rest##*@}"
            user="${creds%%:*}"
            pass="${creds#*:}"

            # Split host:port/db from ?query.
            hostdb="${hostpart%%\?*}"
            query=""
            case "$hostpart" in
                *\?*) params="${hostpart#*\?}" ;;
                *) params="" ;;
            esac

            # Rebuild the query with JDBC-friendly parameter names.
            # libpq's channel_binding has no underscore equivalent before
            # pgjdbc 42.7, and Neuton poolers need server-side prepared
            # statements disabled (prepareThreshold=0).
            if [ -n "$params" ]; then
                newparams=""
                oldIFS="$IFS"
                IFS='&'
                for p in $params; do
                    case "$p" in
                        channel_binding=*) p="channelBinding=${p#channel_binding=}" ;;
                        sslmode=*) p="sslmode=${p#sslmode=}" ;;
                    esac
                    if [ -n "$newparams" ]; then
                        newparams="$newparams&$p"
                    else
                        newparams="$p"
                    fi
                done
                IFS="$oldIFS"
                # pgjdbc switches to server-side prepared statements after 5
                # uses of one statement; those do not survive PgBouncer's
                # transaction pooling (Neon's pooler endpoint). Disable them.
                if [ -n "$newparams" ]; then
                    query="?$newparams&prepareThreshold=0"
                else
                    query="?prepareThreshold=0"
                fi
            fi

            # pgjdbc needs an explicit port; Neon's pooler default is 5432.
            # Split host from /dbname first so the port lands on the host.
            hostonly="${hostdb%%/*}"
            dbpart="/${hostdb#*/}"
            case "$hostonly" in
                *:[0-9]*) ;;
                *) hostonly="$hostonly:5432" ;;
            esac
            hostdb="$hostonly$dbpart"

            export SPRING_DATASOURCE_URL="jdbc:postgresql://${hostdb}${query}"
            export SPRING_DATASOURCE_USERNAME="$user"
            export SPRING_DATASOURCE_PASSWORD="$pass"
            echo "[entrypoint] DATABASE_URL -> $SPRING_DATASOURCE_URL (user: $user)"
            ;;
    esac
fi

# Neon (free tier) auto-suspends its compute after a few idle minutes.
# If the container boots while the DB sleeps, Hikari (1ms init timeout)
# fails the datasource and the whole app fails to start. Rather than
# tuning pool properties (Hikari's config is sealed once started, and
# those properties cannot be env-bound after the fact), wait here until
# the database answers a real query, then start the JVM.
if [ -n "$SPRING_DATASOURCE_URL" ] && command -v psql >/dev/null 2>&1; then
    WAIT_DB_URL="$DATABASE_URL"
    if [ -z "$WAIT_DB_URL" ]; then
        # Rebuild a libpq URI from the already-parsed Spring properties.
        WAIT_DB_URL="postgresql://${SPRING_DATASOURCE_USERNAME}:${SPRING_DATASOURCE_PASSWORD}@${SPRING_DATASOURCE_URL#jdbc:postgresql://}"
        WAIT_DB_URL="${WAIT_DB_URL%\?*}"
        [ "${WAIT_DB_URL#*\?}" != "$WAIT_DB_URL" ] || WAIT_DB_URL="${WAIT_DB_URL}?sslmode=require"
    fi
    echo "[entrypoint] waiting for database to accept connections..."
    waited=0
    until psql "$WAIT_DB_URL" -tAc 'SELECT 1' >/dev/null 2>&1; do
        waited=$((waited + 1))
        if [ "$waited" -ge 40 ]; then
            echo "[entrypoint] database still unreachable after 200s; starting anyway"
            break
        fi
        sleep 5
    done
    [ "$waited" -ge 40 ] || echo "[entrypoint] database reachable"
fi

# Vercel injects PORT; Spring reads SERVER_PORT, so translate it here too.
exec java $JAVA_OPTS -jar /app/vuluntrack-backend.jar --server.port="${PORT:-8080}"
