#!/bin/bash

oc login -u developer -p redhat https://master.lab.example.com --insecure-skip-tls-verify=true
oc project review4-lab
oc delete route vendor-service-solution
