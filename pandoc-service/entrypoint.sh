#!/bin/bash

BUILD_TIMESTAMP="$(cat /opt/pandoc/.build_timestamp)"
export PANDOC_SERVICE_BUILD_TIMESTAMP=${BUILD_TIMESTAMP}

if ! pgrep -x 'dbus-daemon' > /dev/null; then
    if [ -f /run/dbus/pid ]; then
        rm /run/dbus/pid
    fi
    dbus_session_bus_address_filename="/tmp/dbus_session_bus_address";
    dbus-daemon --system --fork --print-address > ${dbus_session_bus_address_filename};
    BUS_ADDRESS=$(cat ${dbus_session_bus_address_filename});
    export DBUS_SESSION_BUS_ADDRESS=${BUS_ADDRESS};
fi

python3 app/PandocServiceApplication.py &

wait -n

exit $?
