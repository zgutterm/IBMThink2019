#!/usr/bin/env bash
sudo iptables -D OUTPUT -p tcp --dport 3306 -j DROP
echo "Database connectivity un-blocked!"
