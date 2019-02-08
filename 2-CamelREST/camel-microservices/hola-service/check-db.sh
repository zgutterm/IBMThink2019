#!/usr/bin/env bash
sudo iptables -L -n -v | grep 3306
