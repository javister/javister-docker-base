#!/bin/bash

# Устанавливаем временную зону на Москву
TZ=${TZ:-Europe/Moscow}
ln -fs /usr/share/zoneinfo/$TZ /etc/localtime
