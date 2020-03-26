#!/bin/bash

[ "$ADVANCED_SCRIPT" ] && trace "Scripting Enabled by: $ADVANCED_SCRIPT"
[ -f /config/userscript.sh ] && (chmod +x /config/userscript.sh && trace "Userscript Provided")
[ "$ADVANCED_SCRIPT" ] && [ -x /config/userscript.sh ] && /config/userscript.sh

exit 0
