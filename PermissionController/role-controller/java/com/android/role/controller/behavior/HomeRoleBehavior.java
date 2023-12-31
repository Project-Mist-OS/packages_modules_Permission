/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.role.controller.behavior;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PermissionInfo;
import android.content.pm.ResolveInfo;
import android.os.UserHandle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.role.controller.model.Permissions;
import com.android.role.controller.model.Role;
import com.android.role.controller.model.RoleBehavior;
import com.android.role.controller.util.UserUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Class for behavior of the home role.
 *
 * @see com.android.settings.applications.DefaultAppSettings
 * @see com.android.settings.applications.defaultapps.DefaultHomePreferenceController
 * @see com.android.settings.applications.defaultapps.DefaultHomePicker
 */
public class HomeRoleBehavior implements RoleBehavior {

    private static final List<String> AUTOMOTIVE_PERMISSIONS = Arrays.asList(
            android.Manifest.permission.READ_CALL_LOG,
            android.Manifest.permission.WRITE_CALL_LOG,
            android.Manifest.permission.READ_CONTACTS);

    @Override
    public boolean isAvailableAsUser(@NonNull Role role, @NonNull UserHandle user,
            @NonNull Context context) {
        return !UserUtils.isProfile(user, context);
    }

    /**
     * @see com.android.server.pm.PackageManagerService#getDefaultHomeActivity(int)
     */
    @Nullable
    @Override
    public String getFallbackHolder(@NonNull Role role, @NonNull Context context) {
        PackageManager packageManager = context.getPackageManager();
        Intent intent = role.getRequiredComponents().get(0).getIntentFilterData().createIntent();
        List<ResolveInfo> resolveInfos = packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY | PackageManager.MATCH_DIRECT_BOOT_AWARE
                | PackageManager.MATCH_DIRECT_BOOT_UNAWARE);

        String packageName = null;
        int priority = Integer.MIN_VALUE;
        int resolveInfosSize = resolveInfos.size();
        for (int i = 0; i < resolveInfosSize; i++) {
            ResolveInfo resolveInfo = resolveInfos.get(i);

            // Leave the fallback to PackageManagerService if there is only the fallback home in
            // Settings, because if we fallback to it here, we cannot fallback to a normal home
            // later, and user cannot see the fallback home in the UI anyway.
            if (isSettingsApplication(resolveInfo.activityInfo.applicationInfo, context)) {
                continue;
            }
            if (resolveInfo.priority > priority) {
                packageName = resolveInfo.activityInfo.packageName;
                priority = resolveInfo.priority;
            } else if (resolveInfo.priority == priority) {
                packageName = null;
            }
        }
        return packageName;
    }

    /**
     * Check if the application is a settings application
     */
    public static boolean isSettingsApplication(@NonNull ApplicationInfo applicationInfo,
            @NonNull Context context) {
        PackageManager packageManager = context.getPackageManager();
        ResolveInfo resolveInfo = packageManager.resolveActivity(new Intent(
                Settings.ACTION_SETTINGS), PackageManager.MATCH_DEFAULT_ONLY
                | PackageManager.MATCH_DIRECT_BOOT_AWARE
                | PackageManager.MATCH_DIRECT_BOOT_UNAWARE);
        if (resolveInfo == null || resolveInfo.activityInfo == null) {
            return false;
        }
        return Objects.equals(applicationInfo.packageName, resolveInfo.activityInfo.packageName);
    }

    @Override
    public void onHolderSelectedAsUser(@NonNull Role role, @NonNull String packageName,
            @NonNull UserHandle user, @NonNull Context context) {
        // Launch the new home app so the change is immediately visible even if the home button is
        // not pressed.
        Intent intent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_HOME)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public void grant(@NonNull Role role, @NonNull String packageName, @NonNull Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            Permissions.grant(packageName, AUTOMOTIVE_PERMISSIONS,
                    true, false, true, false, false, context);
        }

        // Before T, ALLOW_SLIPPERY_TOUCHES may either not exist, or may not be a role permission
        if (isRolePermission(android.Manifest.permission.ALLOW_SLIPPERY_TOUCHES, context)) {
            Permissions.grant(packageName,
                    Arrays.asList(android.Manifest.permission.ALLOW_SLIPPERY_TOUCHES),
                    true, false, true, false, false, context);
        }
    }

    @Override
    public void revoke(@NonNull Role role, @NonNull String packageName, @NonNull Context context) {
        if (context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE)) {
            Permissions.revoke(packageName, AUTOMOTIVE_PERMISSIONS, true, false, false, context);
        }

        // Before T, ALLOW_SLIPPERY_TOUCHES may either not exist, or may not be a role permission
        if (isRolePermission(android.Manifest.permission.ALLOW_SLIPPERY_TOUCHES, context)) {
            Permissions.revoke(packageName,
                    Arrays.asList(android.Manifest.permission.ALLOW_SLIPPERY_TOUCHES),
                    true, false, false, context);
        }
    }

    /**
     * Return true if the permission exists, and has 'role' protection level.
     * Return false otherwise.
     */
    private boolean isRolePermission(@NonNull String permissionName, @NonNull Context context) {
        PermissionInfo permissionInfo;
        try {
            permissionInfo = context.getPackageManager().getPermissionInfo(permissionName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
        final int flags = permissionInfo.getProtectionFlags();
        return (flags & PermissionInfo.PROTECTION_FLAG_ROLE) == PermissionInfo.PROTECTION_FLAG_ROLE;
    }
}
