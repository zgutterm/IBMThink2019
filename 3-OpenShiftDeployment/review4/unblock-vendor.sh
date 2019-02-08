#!/bin/bash

oc login -u developer -p redhat https://master.lab.example.com --insecure-skip-tls-verify=true
oc project review4-lab
oc expose svc vendor-service --hostname=vendor.apps.lab.example.com
