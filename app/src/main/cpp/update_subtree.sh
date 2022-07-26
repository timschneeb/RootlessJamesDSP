#!/bin/bash
cd "$(git rev-parse --show-toplevel)" || exit
git subtree pull --prefix app/src/main/cpp/libjamesdsp-subtree https://github.com/james34602/JamesDSPManager.git master --squash
