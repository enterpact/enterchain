#!/usr/bin/env bash

targets="
OnChainPrivacyGroupManagementInterface
DefaultOnChainPrivacyGroupManagementContract
OnChainPrivacyGroupManagementProxy
"

for target in ${targets}; do

  solc --overwrite --bin --abi \
        -o build  \
        ${target}.sol

done

for target in ${targets}; do

    web3j solidity generate \
        -b build/${target}.bin \
        -a build/${target}.abi \
        -o ../java \
        -p org.enterchain.enter.privacy.contracts.generated

    solc --bin-runtime --overwrite -o . ./${target}.sol
done