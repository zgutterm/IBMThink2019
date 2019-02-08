#!/usr/bin/env bash
sudo iptables -A OUTPUT -p tcp --dport 3306 -j DROP
