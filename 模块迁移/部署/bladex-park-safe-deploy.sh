#!/usr/bin/env bash
set -euo pipefail

DEPLOY_DIR="${1:?missing deploy dir}"
TS="${2:-$(date +%Y%m%d_%H%M%S)}"

BASE="/data/bladex-park/bladex-park-test"
BACKEND="$BASE/backend"
FRONTEND="$BASE/frontend"
BACKUP="$BASE/backup"
NGX="$BASE/nginx-standalone"
DB_BACKUP_DIR="/data/bladex-park/db-backup"
MYSQL_CONTAINER="mysql"
MYSQL_PASSWORD="Root@260310"
MYSQL_DATABASE="wzjk"

JAR="$DEPLOY_DIR/blade-api.jar"
FRONTEND_TAR="$DEPLOY_DIR/bladex-park-frontend-dist.tar.gz"
SQL_DIR="$DEPLOY_DIR/sql"
FRONTEND_TMP="$BASE/frontend.tmp.$TS"
FRONTEND_OLD="$BASE/frontend.old.$TS"

echo "[0/9] safety checks"
test "$BASE" = "/data/bladex-park/bladex-park-test"
test "$BACKEND" = "$BASE/backend"
test "$FRONTEND" = "$BASE/frontend"
case "$DEPLOY_DIR" in
  /tmp/bladex-park-deploy-*) ;;
  *) echo "Unsafe DEPLOY_DIR: $DEPLOY_DIR" >&2; exit 1 ;;
esac
test -d "$DEPLOY_DIR"
test -f "$JAR"
test -f "$FRONTEND_TAR"
test -d "$SQL_DIR"
mkdir -p "$BACKEND" "$FRONTEND" "$BACKUP" "$DB_BACKUP_DIR"

df -h /
df -i /
df -Pk / | awk 'NR == 2 { if ($4 < 5242880) { print "Free disk is less than 5GB"; exit 1 } }'

echo "[1/9] artifact checksums"
cd "$DEPLOY_DIR"
sha256sum blade-api.jar bladex-park-frontend-dist.tar.gz sql/*.sql

echo "[2/9] database backup"
DB_BACKUP_FILE="$DB_BACKUP_DIR/wzjk_${TS}_before_35_40.sql.gz"
docker exec "$MYSQL_CONTAINER" sh -c "exec mysqldump --default-character-set=utf8mb4 -uroot -p$MYSQL_PASSWORD $MYSQL_DATABASE" | gzip > "$DB_BACKUP_FILE"
gzip -t "$DB_BACKUP_FILE"
ls -lh "$DB_BACKUP_FILE"

echo "[3/9] execute sql"
for sql in \
  35_add_smart_device_and_room_vacancy.mysql.sql \
  36_cleanup_acceptance_test_data.mysql.sql \
  37_add_embedded_smart_meter_tab.mysql.sql \
  38_add_rent_control_asset_ledger.mysql.sql \
  39_add_building_scene_images.mysql.sql \
  40_add_room_utility_vehicle.mysql.sql
do
  echo "Executing $sql"
  test -f "$SQL_DIR/$sql"
  docker exec -i "$MYSQL_CONTAINER" mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" < "$SQL_DIR/$sql"
done

echo "[4/9] backup current artifacts"
if test -f "$BACKEND/blade-api.jar"; then
  cp -a "$BACKEND/blade-api.jar" "$BACKUP/blade-api_$TS.jar"
fi
if test -d "$FRONTEND" && test -f "$FRONTEND/index.html"; then
  tar -czf "$BACKUP/frontend_$TS.tar.gz" -C "$FRONTEND" .
fi

echo "[5/9] prepare new frontend"
case "$FRONTEND_TMP" in
  /data/bladex-park/bladex-park-test/frontend.tmp.*) ;;
  *) echo "Unsafe FRONTEND_TMP: $FRONTEND_TMP" >&2; exit 1 ;;
esac
rm -rf "$FRONTEND_TMP"
mkdir -p "$FRONTEND_TMP"
tar -xzf "$FRONTEND_TAR" -C "$FRONTEND_TMP"
test -f "$FRONTEND_TMP/index.html"

echo "[6/9] switch backend and frontend"
cp -f "$JAR" "$BACKEND/blade-api.jar"
restore_frontend() {
  if test ! -d "$FRONTEND" && test -d "$FRONTEND_OLD"; then
    mv "$FRONTEND_OLD" "$FRONTEND"
  fi
}
trap restore_frontend ERR
rm -rf "$FRONTEND_OLD"
if test -d "$FRONTEND"; then
  mv "$FRONTEND" "$FRONTEND_OLD"
fi
mv "$FRONTEND_TMP" "$FRONTEND"
trap - ERR
rm -rf "$FRONTEND_OLD"

echo "[7/9] reload nginx and restart backend"
NGINX_BIN="$(command -v nginx || true)"
test -n "$NGINX_BIN"
"$NGINX_BIN" -p "$NGX/" -c conf/nginx.conf -t
"$NGINX_BIN" -p "$NGX/" -c conf/nginx.conf -s reload || "$NGINX_BIN" -p "$NGX/" -c conf/nginx.conf
docker restart bladex-park-api

echo "[8/9] sync public templates"
if test -d "$FRONTEND/系统所需材料"; then
  docker exec bladex-park-api mkdir -p /blade/saber3/public
  docker cp "$FRONTEND/系统所需材料" bladex-park-api:/blade/saber3/public/
fi

echo "[9/9] verify"
docker exec "$MYSQL_CONTAINER" mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE" -e "
SELECT COUNT(*) AS smart_device_table_count FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='biz_smart_device';
SELECT COUNT(*) AS asset_record_table_count FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='biz_asset_record';
SELECT COUNT(*) AS vacant_since_column_count FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='ics_room' AND column_name='vacant_since';
SELECT COUNT(*) AS scene_images_column_count FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='ics_building' AND column_name='scene_images';
SELECT COUNT(*) AS room_utility_record_table_count FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='biz_room_utility_record';
SELECT COUNT(*) AS room_vehicle_table_count FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='biz_room_vehicle';
SELECT COUNT(*) AS smart_meter_column_count FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='biz_smart_device' AND column_name IN ('payment_type','meter_type','purpose','current_reading','current_balance','multiplier','max_reading','warning_unit','warning_rules');
SELECT COUNT(*) AS standalone_smart_meter_menu_count FROM blade_menu WHERE code='smart_device';
"

curl -sS -o /dev/null -w "front_root_http=%{http_code}\n" http://127.0.0.1:2888/
curl -sS -o /dev/null -w "front_login_http=%{http_code}\n" http://127.0.0.1:2888/login
curl -sS -o /dev/null -w "api_doc_http=%{http_code}\n" http://127.0.0.1:8080/doc.html
docker logs --since 30s bladex-park-api 2>&1 | grep -E "APPLICATION FAILED TO START|ERROR|Exception" | tail -80 || true

echo "deployed_at=$TS"
echo "db_backup=$DB_BACKUP_FILE"
