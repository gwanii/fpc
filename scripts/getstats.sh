#!/bin/bash
# ------------------------------------------------------------------
#  Copyright © 2016 Copyright (c) Sprint, Inc. and others.  All rights reserved.
#
#  This program and the accompanying materials are made available under the
#  terms of the Eclipse Public License v1.0 which accompanies this distribution,
#  and is available at http://www.eclipse.org/legal/epl-v10.html
# ------------------------------------------------------------------

echo ""
curl -i \
--header "Content-type: application/json" \
--request GET \
-u admin:admin \
http://localhost:8181/restconf/operational/fpc:tx-stats
echo ""
