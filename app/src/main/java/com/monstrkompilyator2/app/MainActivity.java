package com.monstrkompilyator2.app;

import android.app.Activity;
import android.os.Bundle;
import android.widget.*;
import android.graphics.*;
import android.view.*;

1:46:41.7973783Z 	at org.gradle.internal.concurrent.AbstractManagedExecutor$1.run(AbstractManagedExecutor.java:47)
2026-10-24T21:46:41.7974695Z 
2026-10-24T21:46:41.7974841Z 
2026-10-24T21:46:41.7975152Z BUILD FAILED in 1m 15s
2026-10-24T21:46:42.2175611Z ##[error]Process completed with exit code 1.
2026-10-24T21:46:42.2312286Z Post job cleanup.
2026-10-24T21:46:42.4125525Z In post-action step
2026-10-24T21:46:42.4133512Z Cache is disabled: will not save state for later builds.
2026-10-24T21:46:42.4134977Z (node:2702) [DEP0040] DeprecationWarning: The `punycode` module is deprecated. Please use a userland alternative instead.
2026-10-24T21:46:42.4136067Z (Use `node --trace-deprecation ...` to show where the warning was created)
2026-10-24T21:46:42.4141159Z Generating Job Summary
2026-10-24T21:46:42.4157963Z Completed post-action step
2026-10-24T21:46:42.4291863Z Post job cleanup.
2026-10-24T21:46:42.5753081Z Post job cleanup.
2026-10-24T21:46:42.6622653Z [command]/usr/bin/git version
2026-10-24T21:46:42.6668544Z git version 2.55.0
2026-10-24T21:46:42.6713441Z Temporarily overriding HOME='/home/runner/work/_temp/8032c127-d578-4a5c-b2ec-c3be3d94c218' before making global git config changes
2026-10-24T21:46:42.6714394Z Adding repository directory to the temporary git global config as a safe directory
2026-10-24T21:46:42.6720767Z [command]/usr/bin/git config --global --add safe.directory /home/runner/work/binance-scanner-app/binance-scanner-app
2026-10-24T21:46:42.6759783Z [command]/usr/bin/git config --local --name-only --get-regexp core\.sshCommand
2026-10-24T21:46:42.6793474Z [command]/usr/bin/git submodule foreach --recursive sh -c "git config --local --name-only --get-regexp 'core\.sshCommand' && git config --local --unset-all 'core.sshCommand' || :"
2026-10-24T21:46:42.6993181Z [command]/usr/bin/git config --local --name-only --get-regexp http\.https\:\/\/github\.com\/\.extraheader
2026-10-24T21:46:42.7018543Z http.https://github.com/.extraheader
2026-10-24T21:46:42.7029323Z [command]/usr/bin/git config --local --unset-all http.https://github.com/.extraheader
2026-10-24T21:46:42.7059196Z [command]/usr/bin/git submodule foreach --recursive sh -c "git config --local --name-only --get-regexp 'http\.https\:\/\/github\.com\/\.extraheader' && git config --local --unset-all 'http.https://github.com/.extraheader' || :"
2026-10-24T21:46:42.7277832Z [command]/usr/bin/git config --local --name-only --get-regexp ^includeIf\.gitdir:
2026-10-24T21:46:42.7315603Z [command]/usr/bin/git submodule foreach --recursive git config --local --show-origin --name-only --get-regexp remote.origin.url
2026-10-24T21:46:42.7718147Z Cleaning up orphan processes
2026-10-24T21:46:42.7987932Z ##[warning]Node.js 20 is deprecated. The following actions target Node.js 20 but are being forced to run on Node.js 24: actions/checkout@v4, gradle/actions/setup-gradle@v3. For more information see: https://github.blog/changelog/2025-09-19-deprecation-of-node-20-on-github-actions-runners/
