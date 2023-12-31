/*
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.permissioncontroller.role.model;

import com.android.permissioncontroller.R;
import com.android.role.controller.model.RoleParser;

/**
 * Initialize the function to retrieve the roles.xml resource from a context within
 * PermissionController APK
 */
public class RoleParserInitializer {

    /**
     * Initialize the function to retrieve the roles.xml resource from a context within
     * PermissionController APK
     */
    public static void initialize() {
        RoleParser.sGetRolesXml = context -> context.getResources().getXml(R.xml.roles);
    }
}
