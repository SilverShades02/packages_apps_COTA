/*
 * Copyright 2014 ParanoidAndroid Project
 *
 * This file is part of Paranoid OTA.
 *
 * Paranoid OTA is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Paranoid OTA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Paranoid OTA.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.paranoid.paranoidhub.updater;

import com.paranoid.paranoidhub.updater.Updater.PackageInfo;
import com.paranoid.paranoidhub.utils.Version;

import org.json.JSONObject;

import java.util.List;

public interface Server {

    String getUrl(String device, Version version);

    List<PackageInfo> createPackageInfoList(JSONObject response) throws Exception;

    String getError();
}