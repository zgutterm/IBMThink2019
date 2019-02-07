#!/bin/sh
echo 'Preparing test folder:'
echo '  Cleaning test folder...'
rm -rf orders
mkdir -p orders/incoming
echo '  Copying sample data files...'
cp ../orders/order-1.xml orders/incoming/noop-1.xml
cp ../orders/order-[2-6].xml orders/incoming/
echo 'Preparation complete!'
